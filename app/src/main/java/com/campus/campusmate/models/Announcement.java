package com.campus.campusmate.models;

import com.google.gson.annotations.SerializedName;

public class Announcement {

    public static final String VISIBILITY_GENERAL = "general";
    public static final String VISIBILITY_DEPARTMENT = "department";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_ARCHIVED = "archived";

    public static final String PRIORITY_NORMAL = "normal";
    public static final String PRIORITY_HIGH = "high";
    public static final String PRIORITY_URGENT = "urgent";

    private String id;
    private String title;
    private String what;
    private String who;
    @SerializedName("when_info")
    private String when;
    @SerializedName("where_info")
    private String where;
    private String why;
    private String additionalInfo;
    private String imageUrl;
    private String authorId;
    private String authorName;
    private String authorRole;
    private String department;
    private String visibility;
    private String status;
    private String priority;
    private String category;
    private String rejectionReason;
    private String approvedBy;
    private long createdAt;
    private long updatedAt;
    private long approvedAt;

    public Announcement() {}

    public Announcement(String title, String authorId, String authorName, String authorRole) {
        this.title = title;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.status = STATUS_PENDING;
        this.priority = PRIORITY_NORMAL;
        this.visibility = VISIBILITY_GENERAL;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
    public boolean isUrgent() { return PRIORITY_URGENT.equals(priority); }
    public boolean isHighPriority() { return PRIORITY_HIGH.equals(priority); }
    public boolean isGeneralVisibility() { return VISIBILITY_GENERAL.equals(visibility); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getWhat() { return what; }
    public void setWhat(String what) { this.what = what; }
    public String getWho() { return who; }
    public void setWho(String who) { this.who = who; }
    public String getWhen() { return when; }
    public void setWhen(String when) { this.when = when; }
    public String getWhere() { return where; }
    public void setWhere(String where) { this.where = where; }
    public String getWhy() { return why; }
    public void setWhy(String why) { this.why = why; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public long getApprovedAt() { return approvedAt; }
    public void setApprovedAt(long approvedAt) { this.approvedAt = approvedAt; }
}
