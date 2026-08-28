package com.campus.campusmate.repositories;

import android.content.Context;
import android.net.Uri;

import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.SupabaseManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Repository class for handling image uploads to Supabase Storage.
 */
public class ImageStorageRepository extends BaseRepository {

    private static ImageStorageRepository instance;
    private OkHttpClient client;
    private Context context;

    private ImageStorageRepository() {}

    public static synchronized ImageStorageRepository getInstance() {
        if (instance == null) instance = new ImageStorageRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        this.context = context.getApplicationContext();
        this.client = SupabaseManager.getInstance(context).getHttpClient();
    }

    /**
     * @param folder Path within the shared {@link Constants#STORAGE_BUCKET} bucket, not a
     *               bucket name itself — the project only provisions one public bucket
     *               ("campusmate"; see schema.sql), so per-feature uploads are separated by
     *               folder instead.
     */
    public void uploadImage(Uri uri, String folder, String fileName, OnUploadListener listener) {
        if (client == null || context == null) {
            listener.onError("Repository not initialized");
            return;
        }

        new Thread(() -> {
            try {
                File file = uriToFile(uri, fileName);
                if (file == null) {
                    listener.onError("Failed to process image");
                    return;
                }

                String path = folder + "/" + fileName;
                String url = Constants.SUPABASE_URL + "/storage/v1/object/" + Constants.STORAGE_BUCKET + "/" + path;

                RequestBody requestBody = RequestBody.create(file, MediaType.parse("image/jpeg"));

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .header("x-upsert", "true")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String publicUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/" + Constants.STORAGE_BUCKET + "/" + path;
                        listener.onSuccess(publicUrl);
                    } else {
                        listener.onError("Upload failed: " + response.code());
                    }
                }
            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        }).start();
    }

    private File uriToFile(Uri uri, String fileName) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            File file = new File(context.getCacheDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    public interface OnUploadListener {
        void onSuccess(String url);
        void onError(String message);
    }
}
