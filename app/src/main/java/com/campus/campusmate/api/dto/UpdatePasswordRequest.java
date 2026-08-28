package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class UpdatePasswordRequest {
    @SerializedName("password") public final String password;

    public UpdatePasswordRequest(String password) {
        this.password = password;
    }
}
