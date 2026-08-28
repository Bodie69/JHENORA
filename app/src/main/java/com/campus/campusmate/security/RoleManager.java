package com.campus.campusmate.security;

import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.Constants;

public final class RoleManager {

    private RoleManager() {}

    public static boolean canCreateAnnouncement(User user) {
        if (user == null || !user.isApproved()) return false;
        return user.isFaculty() || user.isOrganization() || user.isAdmin();
    }

    public static boolean canCreateEvent(User user) {
        if (user == null || !user.isApproved()) return false;
        return user.isFaculty() || user.isOrganization() || user.isAdmin();
    }

    public static boolean canEditPost(User user, String postAuthorId) {
        if (user == null) return false;
        if (user.isAdmin()) return true;
        return user.getUid() != null && user.getUid().equals(postAuthorId);
    }

    public static boolean canDeletePost(User user, String postAuthorId) {
        if (user == null) return false;
        if (user.isAdmin()) return true;
        return user.getUid() != null && user.getUid().equals(postAuthorId);
    }

    public static boolean canApprove(User user) {
        return user != null && user.isAdmin();
    }

    public static boolean canManageUsers(User user) {
        return user != null && user.isAdmin();
    }

    public static boolean canManageDepartments(User user) {
        return user != null && user.isAdmin();
    }

    public static boolean canManageOrganizations(User user) {
        return user != null && user.isAdmin();
    }

    public static boolean canViewAdminDashboard(User user) {
        return user != null && user.isAdmin();
    }

    public static boolean canPostToDepartment(User user, String department) {
        if (user == null) return false;
        if (user.isAdmin()) return true;
        if (user.isOrganization() || user.isFaculty()) {
            // Can post to own department or general
            return Constants.DEPT_GENERAL.equals(department)
                    || department.equals(user.getDepartment());
        }
        return false;
    }

    public static boolean canPostToGeneral(User user) {
        if (user == null) return false;
        return user.isAdmin() || user.isFaculty() || user.isOrganization();
    }
}
