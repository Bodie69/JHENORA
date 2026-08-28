package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class CreateAccountResponse {
    @SerializedName("success") public boolean success;
    @SerializedName("uid") public String uid;
    @SerializedName("error") public String error;
}
