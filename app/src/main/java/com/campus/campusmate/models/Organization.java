package com.campus.campusmate.models;

public class Organization {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    private String id;
    private String name;
    private String description;
    private String leaderId;
    private String leaderName;
    private String department;
    private String imageUrl;
    private String status;
    private long createdAt;

    public Organization() {}

    public Organization(String name, String description, String leaderId) {
        this.name = name;
        this.description = description;
        this.leaderId = leaderId;
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isPending() { return STATUS_PENDING.equals(status); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
