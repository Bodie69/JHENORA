package com.campus.campusmate.utils;

import android.content.Context;

import com.campus.campusmate.BuildConfig;
import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.api.SupabaseAuthApi;
import com.campus.campusmate.api.dto.AuthResponse;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton manager for Supabase services.
 * It initializes and provides access to Retrofit-based APIs for authentication and data,
 * as well as the underlying OkHttpClient.
 */
public class SupabaseManager {

    private static SupabaseManager instance;

    private final OkHttpClient httpClient;
    private final Retrofit retrofit;
    private final SupabaseAuthApi authApi;
    private final SupabaseApi dataApi;

    /**
     * Private constructor to initialize the singleton instance.
     * Sets up the OkHttpClient with interceptors for API keys and authentication tokens,
     * and configures Retrofit with Gson.
     *
     * @param context The application context.
     */
    private SupabaseManager(Context context) {
        SessionManager session = new SessionManager(context);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        // Bare client (no auth interceptor/authenticator of its own) used only to
        // exchange a refresh token for a new session when a request 401s. Now that
        // RLS is enforced, an expired access token actually breaks requests instead
        // of silently falling back to the anon key, so this has to recover in place.
        OkHttpClient refreshClient = new OkHttpClient.Builder().build();

        httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = session.getAccessToken();
                    Request.Builder builder = chain.request().newBuilder()
                            .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                            .addHeader("Content-Type", "application/json");
                    if (token != null) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    } else {
                        builder.addHeader("Authorization", "Bearer " + Constants.SUPABASE_ANON_KEY);
                    }
                    return chain.proceed(builder.build());
                })
                .addInterceptor(logging)
                .authenticator((route, response) -> {
                    if (responseCount(response) >= 2) return null; // already retried once, give up

                    String refreshToken = session.getRefreshToken();
                    if (refreshToken == null || refreshToken.isEmpty()) return null;

                    synchronized (SupabaseManager.class) {
                        String failedAuthHeader = response.request().header("Authorization");
                        String currentAccessToken = session.getAccessToken();
                        if (currentAccessToken != null
                                && !("Bearer " + currentAccessToken).equals(failedAuthHeader)) {
                            // Another request already refreshed the session while this one waited.
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + currentAccessToken)
                                    .build();
                        }

                        String bodyJson = "{\"refresh_token\":\"" + refreshToken + "\"}";
                        Request refreshRequest = new Request.Builder()
                                .url(Constants.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                                .addHeader("Content-Type", "application/json")
                                .post(RequestBody.create(MediaType.parse("application/json"), bodyJson))
                                .build();

                        try (Response refreshResponse = refreshClient.newCall(refreshRequest).execute()) {
                            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                                session.saveTokens(null, null);
                                return null;
                            }
                            AuthResponse auth = gson.fromJson(refreshResponse.body().string(), AuthResponse.class);
                            if (auth == null || !auth.isSuccess()) {
                                session.saveTokens(null, null);
                                return null;
                            }
                            session.saveTokens(auth.accessToken, auth.refreshToken);
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + auth.accessToken)
                                    .build();
                        } catch (IOException e) {
                            return null;
                        }
                    }
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.SUPABASE_URL + "/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        authApi = retrofit.create(SupabaseAuthApi.class);
        dataApi = retrofit.create(SupabaseApi.class);
    }

    /**
     * Returns the singleton instance of SupabaseManager.
     *
     * @param context The application context.
     * @return The SupabaseManager instance.
     */
    public static SupabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Returns the Supabase authentication API.
     *
     * @return The SupabaseAuthApi interface.
     */
    public SupabaseAuthApi authApi() {
        return authApi;
    }

    /**
     * Returns the Supabase data API.
     *
     * @return The SupabaseApi interface.
     */
    public SupabaseApi dataApi() {
        return dataApi;
    }

    /**
     * Returns the underlying OkHttpClient used for network requests.
     *
     * @return The OkHttpClient instance.
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /** Counts this response plus any chained prior responses, to cap 401-retry attempts. */
    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
