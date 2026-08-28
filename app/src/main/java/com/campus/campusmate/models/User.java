package com.campus.campusmate.models;

import com.google.gson.annotations.SerializedName;

/**
 * User model class representing the 'users' table in Supabase.
 * Uses SerializedName to ensure exact mapping with database columns.
 */
public class User {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_FACULTY = "faculty";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ORGANIZATION = "organization";
    public static final String ROLE_LIVE_DISPLAY = "live_display";

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    @SerializedName("uid")
    private String uid;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("middle_name")
    private String middleName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("department")
    private String department;

    @SerializedName("profile_image_url")
    private String profileImageUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("email_verified")
    private boolean emailVerified;

    @SerializedName("created_at")
    private long createdAt;

    @SerializedName("updated_at")
    private Long updatedAt; // Use Long to allow nulls from DB

    @SerializedName("block")
    private String block;

    @SerializedName("year_level")
    private String yearLevel;

    @SerializedName("address")
    private String address;

    @SerializedName("profession")
    private String profession;

    @SerializedName("organization_name")
    private String organizationName;

    @SerializedName("organization_description")
    private String organizationDescription;

    @SerializedName("organization_id")
    private String organizationId;

    public User() {}

    public String getFullName() {
        StringBuilder name = new StringBuilder(firstName != null ? firstName : "");
        if (middleName != null && !middleName.isEmpty()) {
            name.append(" ").append(middleName);
        }
        if (lastName != null) {
            name.append(" ").append(lastName);
        }
        return name.toString().trim();
    }

    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
    public boolean isFaculty() { return ROLE_FACULTY.equals(role); }
    public boolean isStudent() { return ROLE_STUDENT.equals(role); }
    public boolean isOrganization() { return ROLE_ORGANIZATION.equals(role); }
    public boolean isLiveDisplay() { return ROLE_LIVE_DISPLAY.equals(role); }
    public boolean isApproved() { return STATUS_APPROVED.equals(status); }
    public boolean isPending() { return STATUS_PENDING.equals(status); }
    public boolean canPost() { return isFaculty() || isOrganization() || isAdmin(); }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }
    public String getYearLevel() { return yearLevel; }
    public void setYearLevel(String yearLevel) { this.yearLevel = yearLevel; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public String getOrganizationDescription() { return organizationDescription; }
    public void setOrganizationDescription(String organizationDescription) { this.organizationDescription = organizationDescription; }
    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
}
