package com.campus.campusmate.models;

import com.google.gson.annotations.SerializedName;

public class Event {

    public static final String VISIBILITY_GENERAL = "general";
    public static final String VISIBILITY_DEPARTMENT = "department";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_ARCHIVED = "archived";

    private String id;
    private String title;
    private String what;
    private String who;
    @SerializedName("when_info")
    private String when;
    @SerializedName("where_info")
    private String where;
    private String why;
    private String registrationLink;
    private String imageUrl;
    private String authorId;
    private String authorName;
    private String authorRole;
    private String department;
    private String visibility;
    private String status;
    private String category;
    private String rejectionReason;
    private String approvedBy;
    private long startDate;
    private long endDate;
    private long createdAt;
    private long updatedAt;
    private long approvedAt;

    public Event() {}

    public Event(String title, String authorId, String authorName, String authorRole) {
        this.title = title;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.status = STATUS_PENDING;
        this.visibility = VISIBILITY_GENERAL;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isRejected() { return STATUS_REJECTED.equals(status); }
    public boolean isUpcoming() { return startDate > System.currentTimeMillis(); }
    public boolean isOngoing() {
        long now = System.currentTimeMillis();
        return startDate <= now && (endDate == 0 || endDate >= now);
    }
    public boolean isPast() { return endDate > 0 && endDate < System.currentTimeMillis(); }

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
    public String getRegistrationLink() { return registrationLink; }
    public void setRegistrationLink(String registrationLink) { this.registrationLink = registrationLink; }
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
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public long getApprovedAt() { return approvedAt; }
    public void setApprovedAt(long approvedAt) { this.approvedAt = approvedAt; }
}
