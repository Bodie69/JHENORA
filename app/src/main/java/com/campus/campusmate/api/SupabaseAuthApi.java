package com.campus.campusmate.api;

import com.campus.campusmate.api.dto.AuthRequest;
import com.campus.campusmate.api.dto.AuthResponse;
import com.campus.campusmate.api.dto.EmailRequest;
import com.campus.campusmate.api.dto.RefreshTokenRequest;
import com.campus.campusmate.api.dto.SupabaseUser;
import com.campus.campusmate.api.dto.UpdatePasswordRequest;
import com.campus.campusmate.api.dto.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body AuthRequest request);

    @POST("auth/v1/token")
    Call<AuthResponse> signIn(
            @Query("grant_type") String grantType,
            @Body AuthRequest request);

    /** Exchanges a refresh token for a new access/refresh token pair. Used by the
     *  401 {@link okhttp3.Authenticator} in SupabaseManager once RLS makes an
     *  expired access token actually matter. */
    @POST("auth/v1/token")
    Call<AuthResponse> refreshToken(
            @Query("grant_type") String grantType,
            @Body RefreshTokenRequest request);

    @GET("auth/v1/user")
    Call<SupabaseUser> getUser(@Header("Authorization") String bearerToken);

    @POST("auth/v1/logout")
    Call<Void> logout(@Header("Authorization") String bearerToken);

    /**
     * Triggers the "Reset Password" recovery email. With the recovery email template
     * configured to include {{ .Token }}, this delivers a 6-digit OTP instead of a link.
     */
    @POST("auth/v1/recover")
    Call<Void> recover(@Body EmailRequest request);

    /** Verifies the recovery OTP and returns a short-lived session for the password update call. */
    @POST("auth/v1/verify")
    Call<AuthResponse> verifyOtp(@Body VerifyOtpRequest request);

    @PUT("auth/v1/user")
    Call<SupabaseUser> updateUser(@Body UpdatePasswordRequest request);
}
