package com.campus.campusmate.models;

public class AuditLog {

    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_APPROVE = "approve";
    public static final String ACTION_REJECT = "reject";
    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_LOGOUT = "logout";

    public static final String TARGET_ANNOUNCEMENT = "announcement";
    public static final String TARGET_EVENT = "event";
    public static final String TARGET_USER = "user";
    public static final String TARGET_ORGANIZATION = "organization";
    public static final String TARGET_DEPARTMENT = "department";

    private String id;
    private String userId;
    private String userEmail;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private long timestamp;

    public AuditLog() {}

    public AuditLog(String userId, String userEmail, String action,
                    String targetType, String targetId, String details) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
