package com.campus.campusmate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.models.Organization;
import com.campus.campusmate.models.User;
import com.campus.campusmate.repositories.OrganizationRepository;
import com.campus.campusmate.repositories.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminViewModel extends AndroidViewModel {

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;

    public final MutableLiveData<List<User>> allUsers = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<User>> pendingUsers = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<User>> displayAccounts = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<Organization>> organizations = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<String> successMessage = new MutableLiveData<>();

    // Dashboard stats
    public final MutableLiveData<Integer> totalUsers = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> totalFaculty = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> totalStudents = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> totalOrganizations = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> pendingCount = new MutableLiveData<>(0);

    public AdminViewModel(@NonNull Application application) {
        super(application);
        userRepo = UserRepository.getInstance();
        userRepo.init(application);
        orgRepo = OrganizationRepository.getInstance();
        orgRepo.init(application);

        allUsers.observeForever(users -> {
            if (users != null) {
                totalUsers.postValue(users.size());
                int faculty = 0, students = 0, pending = 0;
                List<User> displays = new ArrayList<>();
                for (User u : users) {
                    if (User.ROLE_FACULTY.equals(u.getRole())) faculty++;
                    if (User.ROLE_STUDENT.equals(u.getRole())) students++;
                    if (User.STATUS_PENDING.equals(u.getStatus())) pending++;
                    if (User.ROLE_LIVE_DISPLAY.equals(u.getRole())) displays.add(u);
                }
                totalFaculty.postValue(faculty);
                totalStudents.postValue(students);
                pendingCount.postValue(pending);
                displayAccounts.postValue(displays);
            }
        });
    }

    public void createDisplayAccount(String email, String password, String label) {
        isLoading.setValue(true);
        userRepo.createDisplayAccount(email, password, label, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                loadAllUsers();
                isLoading.postValue(false);
                successMessage.postValue("Display account created");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void loadAllUsers() {
        userRepo.fetchAllUsers(allUsers);
    }

    public void loadPendingUsers() {
        userRepo.fetchPendingUsers(pendingUsers);
    }

    public void loadOrganizations() {
        orgRepo.observeAllOrganizations(organizations);
        organizations.observeForever(orgs -> {
            if (orgs != null) totalOrganizations.postValue(orgs.size());
        });
    }

    public void approveUser(String uid) {
        isLoading.setValue(true);
        userRepo.updateUserStatus(uid, User.STATUS_APPROVED, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                loadAllUsers();
                loadPendingUsers();
                isLoading.postValue(false);
                successMessage.postValue("User approved");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void rejectUser(String uid) {
        isLoading.setValue(true);
        userRepo.updateUserStatus(uid, User.STATUS_REJECTED, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                loadAllUsers();
                loadPendingUsers();
                isLoading.postValue(false);
                successMessage.postValue("User rejected");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void approveOrganization(String orgId) {
        isLoading.setValue(true);
        orgRepo.approveOrganization(orgId, new OrganizationRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                loadOrganizations();
                successMessage.postValue("Organization approved");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void createOrganization(Organization org, String leaderId, String leaderName) {
        isLoading.setValue(true);
        org.setStatus(Organization.STATUS_APPROVED);
        org.setLeaderId(leaderId);
        org.setLeaderName(leaderName);
        org.setCreatedAt(System.currentTimeMillis());

        orgRepo.createOrganization(org, new OrganizationRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                assignLeaderThen(org, leaderId, null, "created");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void updateOrganization(Organization org, String leaderId, String leaderName) {
        isLoading.setValue(true);
        String previousLeaderId = org.getLeaderId();
        // "" rather than null when removing: the org-table patch map omits null values
        // entirely (leaving the columns untouched), so an explicit "" is what actually clears
        // them. assignLeaderThen still receives the original (possibly null) leaderId below,
        // since that's what tells it to unlink the previous leader instead of assigning a new one.
        org.setLeaderId(leaderId != null ? leaderId : "");
        org.setLeaderName(leaderName != null ? leaderName : "");

        orgRepo.updateOrganization(org, new OrganizationRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                assignLeaderThen(org, leaderId, previousLeaderId, "updated");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    /**
     * Links the assigned student to the organization as its leader, promoting their role to
     * 'organization' and taking on the organization's department (so their posts and
     * announcements are correctly scoped to it), unlinks the previous leader if they're being
     * replaced (reverting their role and department back to 'student'), then reports success.
     */
    private void assignLeaderThen(Organization org, String leaderId, String previousLeaderId, String verb) {
        boolean leaderChanged = previousLeaderId != null && !previousLeaderId.equals(leaderId);

        if (leaderId == null) {
            if (leaderChanged) unlinkOrgLeader(previousLeaderId);
            loadOrganizations();
            isLoading.postValue(false);
            successMessage.postValue("Organization " + verb);
            return;
        }

        Map<String, String> filter = new HashMap<>();
        filter.put("uid", "eq." + leaderId);
        Map<String, Object> fields = new HashMap<>();
        fields.put("organization_name", org.getName());
        fields.put("organization_id", org.getId());
        fields.put("status", User.STATUS_APPROVED);
        fields.put("role", User.ROLE_ORGANIZATION);
        fields.put("department", org.getDepartment() != null ? org.getDepartment() : "");

        userRepo.updateUserFields(filter, fields, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (leaderChanged) unlinkOrgLeader(previousLeaderId);
                loadOrganizations();
                loadAllUsers();
                isLoading.postValue(false);
                successMessage.postValue("Organization " + verb + " and student assigned");
            }

            @Override
            public void onError(String message) {
                loadOrganizations();
                isLoading.postValue(false);
                errorMessage.postValue("Org " + verb + ", but failed to assign student: " + message);
            }
        });
    }

    /**
     * Clears a former leader's organization link and reverts their role back to 'student'.
     * Also clears the department that was taken on from the organization, since it no longer
     * applies once they're demoted; an admin can reassign it via the user's profile if needed.
     * Uses "" rather than null for these fields: the shared Gson instance omits null map
     * values from the PATCH body entirely (so the column would be left untouched), but an
     * empty string is sent as a real value that actually clears the column.
     */
    private void unlinkOrgLeader(String uid) {
        Map<String, String> filter = new HashMap<>();
        filter.put("uid", "eq." + uid);
        Map<String, Object> fields = new HashMap<>();
        fields.put("organization_name", "");
        fields.put("organization_id", "");
        fields.put("role", User.ROLE_STUDENT);
        fields.put("department", "");
        userRepo.updateUserFields(filter, fields, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                loadAllUsers();
            }

            @Override
            public void onError(String message) {
                // Best-effort cleanup; the new assignment already succeeded.
            }
        });
    }

    public void deleteOrganization(String id) {
        isLoading.setValue(true);
        orgRepo.deleteOrganization(id, new OrganizationRepository.OnSimpleListener() {
            @Override
            public void onSuccess() {
                loadOrganizations();
                isLoading.postValue(false);
                successMessage.postValue("Organization deleted");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public List<User> filterUsers(List<User> all, String role, String query) {
        if (all == null) return new ArrayList<>();
        List<User> result = new ArrayList<>();
        String q = query != null ? query.toLowerCase().trim() : "";
        for (User u : all) {
            // Pending users are handled on the Approvals page until approved/rejected.
            if (User.STATUS_PENDING.equals(u.getStatus())) continue;
            boolean matchRole = role == null || role.isEmpty() || role.equals(u.getRole());
            boolean matchQuery = q.isEmpty()
                    || (u.getFullName().toLowerCase().contains(q))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                    || (u.getDepartment() != null && u.getDepartment().toLowerCase().contains(q));
            if (matchRole && matchQuery) result.add(u);
        }
        return result;
    }
}
