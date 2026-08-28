package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {
    @SerializedName("email") public final String email;
    @SerializedName("token") public final String token;
    @SerializedName("type") public final String type;

    public VerifyOtpRequest(String email, String token) {
        this.email = email;
        this.token = token;
        this.type = "recovery";
    }
}
