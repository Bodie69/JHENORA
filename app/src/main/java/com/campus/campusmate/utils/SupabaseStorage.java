package com.campus.campusmate.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

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
     * The upload is performed asynchronously on a background thread.
     *
     * @param context The application context.
     * @param imageUri The Uri of the image to be uploaded.
     * @param folder The folder path within the bucket where the image should be stored.
     * @param callback The callback to receive the upload result.
     */
    public static void uploadImage(Context context, Uri imageUri, String folder, UploadCallback callback) {
        executor.execute(() -> {
            try {
                byte[] bytes = readBytes(context, imageUri);
                if (bytes == null) {
                    postFailure(callback, "Failed to read image file");
                    return;
                }

                String fileName = UUID.randomUUID().toString() + ".jpg";
                String path = folder + "/" + fileName;
                String uploadUrl = Constants.SUPABASE_URL
                        + "/storage/v1/object/"
                        + Constants.STORAGE_BUCKET + "/"
                        + path;

                SessionManager session = new SessionManager(context);
                String token = session.getAccessToken();
                String authHeader = token != null
                        ? "Bearer " + token
                        : "Bearer " + Constants.SUPABASE_ANON_KEY;

                OkHttpClient client = SupabaseManager.getInstance(context).getHttpClient();
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .addHeader("Authorization", authHeader)
                        .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
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
                }
            } catch (IOException e) {
                postFailure(callback, e.getMessage());
            }
        });
    }

    /**
     * Reads the byte content from a given Uri.
     *
     * @param context The application context.
     * @param uri The Uri of the file to read.
     * @return A byte array containing the file content, or null if reading fails.
     */
    private static byte[] readBytes(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            return null;
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
