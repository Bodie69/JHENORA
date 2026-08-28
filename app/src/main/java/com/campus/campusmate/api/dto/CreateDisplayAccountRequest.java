package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class CreateDisplayAccountRequest {
    @SerializedName("email") public final String email;
    @SerializedName("password") public final String password;
    @SerializedName("full_name") public final String fullName;

    public CreateDisplayAccountRequest(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }
}
