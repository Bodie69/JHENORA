package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class AuthRequest {
    @SerializedName("email") public final String email;
    @SerializedName("password") public final String password;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
