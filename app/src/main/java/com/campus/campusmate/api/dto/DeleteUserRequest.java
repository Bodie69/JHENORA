package com.campus.campusmate.api.dto;

import com.google.gson.annotations.SerializedName;

public class DeleteUserRequest {
    @SerializedName("uid") public final String uid;

    public DeleteUserRequest(String uid) {
        this.uid = uid;
    }
}
