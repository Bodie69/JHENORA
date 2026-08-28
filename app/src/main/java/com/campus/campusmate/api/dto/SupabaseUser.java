package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class SupabaseUser {
    @SerializedName("id") public String id;
    @SerializedName("email") public String email;
    @SerializedName("email_confirmed_at") public String emailConfirmedAt;
    @SerializedName("created_at") public String createdAt;

    public boolean isEmailConfirmed() {
        return emailConfirmedAt != null && !emailConfirmedAt.isEmpty();
    }
}
