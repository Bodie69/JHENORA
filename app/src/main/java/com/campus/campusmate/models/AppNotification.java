package com.campus.campusmate.models;

public class AppNotification {

    public static final String TYPE_ANNOUNCEMENT = "announcement";
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_APPROVAL = "approval";
    public static final String TYPE_REGISTRATION = "registration";

    private String id;
    private String title;
    private String message;
    private String type;
    private String referenceId;
    private String userId;
    private boolean isRead;
    private long createdAt;

    public AppNotification() {}

    public AppNotification(String userId, String title, String message, String type, String referenceId) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.referenceId = referenceId;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
