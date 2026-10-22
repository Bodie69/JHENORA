package com.campus.campusmate.models;

public class Department {

    private String id;
    private String name;
    private String code;
    private String description;
    private String colorHex;
    /** Public URL of the department logo in Supabase Storage; {@code null} or empty if none. */
    private String logoUrl;
    private transient int colorRes;
    private long createdAt;

    public Department() {}

    public Department(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public int getColorRes() { return colorRes; }
    public void setColorRes(int colorRes) { this.colorRes = colorRes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
