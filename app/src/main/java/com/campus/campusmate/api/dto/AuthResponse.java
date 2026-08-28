package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    // Session fields (returned if email confirmation is OFF)
    @SerializedName("access_token") public String accessToken;
    @SerializedName("refresh_token") public String refreshToken;
    @SerializedName("token_type") public String tokenType;
    @SerializedName("user") public SupabaseUser user;

    // User fields (returned directly at root if email confirmation is ON)
    @SerializedName("id") public String id;
    @SerializedName("email") public String email;

    @SerializedName("error") public String error;
    @SerializedName("error_description") public String errorDescription;

    public boolean isSuccess() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Helper to get the user ID regardless of how Supabase returned it.
     */
    public String getEffectiveUid() {
        if (user != null && user.id != null) return user.id;
        return id;
    }
}
