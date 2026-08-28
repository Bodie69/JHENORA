package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class EmailRequest {
    @SerializedName("email") public final String email;

    public EmailRequest(String email) {
        this.email = email;
    }
}
