package com.campus.campusmate.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utility class for handling file uploads to Supabase Storage.
 * It provides methods to upload images asynchronously and handle callbacks on the main thread.
 */
public class SupabaseStorage {

    /**
     * Callback interface for monitoring the status of a file upload.
     */
    public interface UploadCallback {
        /**
         * Called when the upload is successfully completed.
         *
         * @param publicUrl The public URL of the uploaded file.
         */
        void onSuccess(String publicUrl);

        /**
         * Called when the upload fails.
         *
         * @param errorMessage A message describing the reason for failure.
         */
        void onFailure(String errorMessage);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Uploads an image from a given Uri to a specified folder in Supabase Storage.
     * The image is downsampled/re-encoded first (see {@link #compressImage}), then the upload
     * runs asynchronously on a background thread.
     *
     * @param context The application context.
     * @param imageUri The Uri of the image to be uploaded.
     * @param folder The folder path within the bucket where the image should be stored.
     * @param callback The callback to receive the upload result.
     */
    public static void uploadImage(Context context, Uri imageUri, String folder, UploadCallback callback) {
        executor.execute(() -> {
            byte[] bytes = compressImage(context, imageUri);
            if (bytes == null) {
                postFailure(callback, "Failed to read or process the selected image");
                return;
            }

            String fileName = UUID.randomUUID().toString() + ".jpg";
            String path = folder + "/" + fileName;
            String uploadUrl = Constants.SUPABASE_URL
                    + "/storage/v1/object/"
                    + Constants.STORAGE_BUCKET + "/"
                    + path;

            // apikey/Authorization come from SupabaseManager's shared client interceptor —
            // adding them here too used to send duplicate headers on this request, which
            // Cloudflare's edge in front of Supabase rejected with a bare 400 before it ever
            // reached Supabase's storage service.
            OkHttpClient client = SupabaseManager.getInstance(context).getHttpClient();
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("x-upsert", "true")
                    .post(RequestBody.create(bytes, MediaType.parse("image/jpeg")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String publicUrl = Constants.SUPABASE_URL
                            + "/storage/v1/object/public/"
                            + Constants.STORAGE_BUCKET + "/"
                            + path;
                    mainHandler.post(() -> callback.onSuccess(publicUrl));
                } else {
                    String msg = response.body() != null ? response.body().string() : "Upload failed";
                    postFailure(callback, msg);
                }
            } catch (IOException e) {
                postFailure(callback, e.getMessage());
            }
        });
    }

    /** Side length, in px, of the square PNG stored for logos (department logos). */
    private static final int LOGO_SIZE = 512;
    /** Max per-channel difference for a pixel to count as the logo's background colour. */
    private static final int LOGO_BG_TOLERANCE = 24;

    /**
     * Uploads {@code imageUri} as a logo: the image is trimmed of its blank margins (transparent,
     * white or any flat border colour), centred on a square canvas so the artwork reaches the
     * edges of a circular frame, and stored as a {@value #LOGO_SIZE}px PNG so transparency
     * survives (JPEG would turn transparent corners black).
     *
     * @param folder The folder path within the bucket, e.g. {@link Constants#STORAGE_DEPARTMENTS}.
     */
    public static void uploadLogo(Context context, Uri imageUri, String folder, UploadCallback callback) {
        executor.execute(() -> {
            byte[] bytes = processLogo(context, imageUri);
            if (bytes == null) {
                postFailure(callback, "Failed to read or process the selected image");
                return;
            }
            upload(context, bytes, folder + "/" + UUID.randomUUID() + ".png", "image/png", callback);
        });
    }

    /**
     * Uploads {@code imageUri} as {@code uid}'s profile photo: downsampled, rotated upright and
     * re-encoded as JPEG (see {@link #compressImage}), then stored at
     * {@code profiles/<uid>/<random>.jpg}. Storage policies only let a user write inside their
     * own folder (supabase/restrict_profile_photo_storage.sql), and the fresh name per upload
     * gives every photo a new URL, so image caches never show a replaced photo.
     */
    public static void uploadProfilePhoto(Context context, Uri imageUri, String uid, UploadCallback callback) {
        executor.execute(() -> {
            byte[] bytes = compressImage(context, imageUri);
            if (bytes == null) {
                postFailure(callback, "Failed to read or process the selected image");
                return;
            }
            upload(context, bytes, Constants.STORAGE_PROFILES + "/" + uid + "/" + UUID.randomUUID() + ".jpg",
                    "image/jpeg", callback);
        });
    }

    /**
     * Best-effort removal of a previously uploaded file, given its public URL. Used to clean up
     * a replaced or removed logo or profile photo; failures are ignored (an orphaned file is
     * harmless).
     */
    public static void deleteByPublicUrl(Context context, String publicUrl) {
        String prefix = Constants.SUPABASE_URL + "/storage/v1/object/public/" + Constants.STORAGE_BUCKET + "/";
        if (publicUrl == null || !publicUrl.startsWith(prefix)) return;
        String path = publicUrl.substring(prefix.length());
        executor.execute(() -> {
            Request request = new Request.Builder()
                    .url(Constants.SUPABASE_URL + "/storage/v1/object/" + Constants.STORAGE_BUCKET + "/" + path)
                    .delete()
                    .build();
            try (Response ignored = SupabaseManager.getInstance(context).getHttpClient()
                    .newCall(request).execute()) {
                // Result intentionally ignored.
            } catch (IOException ignored) {
                // Best effort.
            }
        });
    }

    private static void upload(Context context, byte[] bytes, String path, String contentType,
                               UploadCallback callback) {
        String uploadUrl = Constants.SUPABASE_URL + "/storage/v1/object/" + Constants.STORAGE_BUCKET + "/" + path;
        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("x-upsert", "true")
                .post(RequestBody.create(bytes, MediaType.parse(contentType)))
                .build();
        try (Response response = SupabaseManager.getInstance(context).getHttpClient()
                .newCall(request).execute()) {
            if (response.isSuccessful()) {
                String publicUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/"
                        + Constants.STORAGE_BUCKET + "/" + path;
                mainHandler.post(() -> callback.onSuccess(publicUrl));
            } else {
                String msg = response.body() != null ? response.body().string() : "Upload failed";
                postFailure(callback, msg);
            }
        } catch (IOException e) {
            postFailure(callback, e.getMessage());
        }
    }

    /** Decode -> upright -> trim blank margins -> centre on a square -> PNG bytes. */
    private static byte[] processLogo(Context context, Uri uri) {
        Bitmap source = decodeUpright(context, uri, LOGO_SIZE * 2);
        if (source == null) return null;
        Bitmap square = squareLogo(source, LOGO_SIZE);
        source.recycle();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        square.compress(Bitmap.CompressFormat.PNG, 100, output);
        square.recycle();
        return output.toByteArray();
    }

    /**
     * Trims {@code source}'s blank margins and centres what's left on a {@code size} x
     * {@code size} canvas, scaled so its longer side spans the full canvas. Pads (never crops)
     * the shorter side with the logo's own background, so nothing of the logo is cut off and
     * transparent backgrounds stay transparent. Does not recycle {@code source}.
     */
    static Bitmap squareLogo(Bitmap source, int size) {
        int corner = source.getPixel(0, 0);
        Bitmap trimmed = trimUniformBorder(source);
        int side = Math.max(trimmed.getWidth(), trimmed.getHeight());
        int background = isTransparent(corner) ? Color.TRANSPARENT : corner;

        Bitmap square = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(square);
        canvas.drawColor(background);
        float scale = (float) size / side;
        float drawW = trimmed.getWidth() * scale;
        float drawH = trimmed.getHeight() * scale;
        RectF dest = new RectF((size - drawW) / 2f, (size - drawH) / 2f,
                (size + drawW) / 2f, (size + drawH) / 2f);
        canvas.drawBitmap(trimmed, null, dest, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));

        if (trimmed != source) trimmed.recycle();
        return square;
    }

    /** Decodes {@code uri} at roughly {@code maxDimension}, EXIF-corrected, as ARGB_8888. */
    private static Bitmap decodeUpright(Context context, Uri uri, int maxDimension) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, bounds);
        } catch (IOException e) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension);
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } catch (IOException e) {
            return null;
        }
        return bitmap == null ? null : applyExifRotation(context, uri, bitmap);
    }

    /**
     * Crops away the outer border whose pixels all match the top-left corner's colour (or are
     * transparent, if the corner is). Returns {@code bitmap} itself if there's nothing to trim
     * or the whole image is background.
     */
    private static Bitmap trimUniformBorder(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int bg = bitmap.getPixel(0, 0);
        int[] row = new int[w];
        int[] col = new int[h];

        int top = 0;
        while (top < h && lineIsBackground(bitmap, row, 0, top, w, 1, bg)) top++;
        if (top == h) return bitmap; // blank image: leave as-is
        int bottom = h - 1;
        while (bottom > top && lineIsBackground(bitmap, row, 0, bottom, w, 1, bg)) bottom--;
        int left = 0;
        while (left < w && lineIsBackground(bitmap, col, left, 0, 1, h, bg)) left++;
        int right = w - 1;
        while (right > left && lineIsBackground(bitmap, col, right, 0, 1, h, bg)) right--;

        if (left == 0 && top == 0 && right == w - 1 && bottom == h - 1) return bitmap;
        return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1);
    }

    private static boolean lineIsBackground(Bitmap bitmap, int[] buffer, int x, int y,
                                            int width, int height, int bg) {
        bitmap.getPixels(buffer, 0, width, x, y, width, height);
        int count = width * height;
        for (int i = 0; i < count; i++) {
            if (!matchesBackground(buffer[i], bg)) return false;
        }
        return true;
    }

    private static boolean matchesBackground(int pixel, int bg) {
        if (isTransparent(bg)) return isTransparent(pixel);
        if (Color.alpha(pixel) < 200) return false;
        return Math.abs(Color.red(pixel) - Color.red(bg)) <= LOGO_BG_TOLERANCE
                && Math.abs(Color.green(pixel) - Color.green(bg)) <= LOGO_BG_TOLERANCE
                && Math.abs(Color.blue(pixel) - Color.blue(bg)) <= LOGO_BG_TOLERANCE;
    }

    private static boolean isTransparent(int pixel) {
        return Color.alpha(pixel) < 16;
    }

    /**
     * Decodes {@code uri} downsampled to at most {@link Constants#MAX_IMAGE_DIMENSION} on its
     * longest side, corrects for EXIF orientation (camera photos are otherwise stored sideways —
     * {@link BitmapFactory} ignores EXIF, and re-encoding drops the original tag anyway), and
     * re-encodes as a JPEG at {@link Constants#IMAGE_QUALITY}.
     * <p>
     * Without this, a raw multi-megabyte camera photo was uploaded byte-for-byte: slow enough
     * over campus wifi to look like a stuck upload, and decoding those same raw bytes into a
     * full-size {@link Bitmap} elsewhere risked {@link OutOfMemoryError} on low-RAM devices.
     *
     * @return the re-encoded JPEG bytes, or {@code null} if the image couldn't be read/decoded.
     */
    private static byte[] compressImage(Context context, Uri uri) {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream boundsStream = context.getContentResolver().openInputStream(uri)) {
            if (boundsStream == null) return null;
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        } catch (IOException e) {
            return null;
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null;

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = sampleSize(
                boundsOptions.outWidth, boundsOptions.outHeight, Constants.MAX_IMAGE_DIMENSION);

        Bitmap bitmap;
        try (InputStream decodeStream = context.getContentResolver().openInputStream(uri)) {
            if (decodeStream == null) return null;
            bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        } catch (IOException e) {
            return null;
        }
        if (bitmap == null) return null;

        bitmap = applyExifRotation(context, uri, bitmap);

        // inSampleSize only halves, so the decode above can still overshoot the target by up
        // to 2x — scale down the rest of the way to actually land at MAX_IMAGE_DIMENSION.
        int longestSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longestSide > Constants.MAX_IMAGE_DIMENSION) {
            float scale = (float) Constants.MAX_IMAGE_DIMENSION / longestSide;
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                    Math.round(bitmap.getWidth() * scale), Math.round(bitmap.getHeight() * scale), true);
            if (scaled != bitmap) bitmap.recycle();
            bitmap = scaled;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_QUALITY, output);
        bitmap.recycle();
        return output.toByteArray();
    }

    /** Largest power-of-two sample size that keeps the decoded bitmap at or above
     *  {@code maxDimension} on its longest side (the final precise resize happens after decode). */
    private static int sampleSize(int width, int height, int maxDimension) {
        int sampleSize = 1;
        int longestSide = Math.max(width, height);
        while (longestSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    /** Rotates {@code bitmap} to upright according to the source image's EXIF orientation tag,
     *  recycling the pre-rotation bitmap. Returns {@code bitmap} unchanged if EXIF is missing,
     *  unreadable, or already upright. */
    private static Bitmap applyExifRotation(Context context, Uri uri, Bitmap bitmap) {
        try (InputStream exifStream = context.getContentResolver().openInputStream(uri)) {
            if (exifStream == null) return bitmap;
            ExifInterface exif = new ExifInterface(new BufferedInputStream(exifStream));
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float degrees = orientationToDegrees(orientation);
            if (degrees == 0f) return bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (IOException e) {
            return bitmap;
        }
    }

    private static float orientationToDegrees(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return 90f;
            case ExifInterface.ORIENTATION_ROTATE_180: return 180f;
            case ExifInterface.ORIENTATION_ROTATE_270: return 270f;
            default: return 0f;
        }
    }

    /**
     * Posts a failure message to the provided callback on the main thread.
     *
     * @param callback The callback to notify.
     * @param message The failure message.
     */
    private static void postFailure(UploadCallback callback, String message) {
        mainHandler.post(() -> callback.onFailure(message));
    }
}
