package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.api.SupabaseAuthApi;
import com.campus.campusmate.api.dto.AuthRequest;
import com.campus.campusmate.api.dto.AuthResponse;
import com.campus.campusmate.api.dto.CreateAccountResponse;
import com.campus.campusmate.api.dto.CreateDisplayAccountRequest;
import com.campus.campusmate.api.dto.EmailRequest;
import com.campus.campusmate.api.dto.SupabaseUser;
import com.campus.campusmate.api.dto.UpdatePasswordRequest;
import com.campus.campusmate.api.dto.VerifyOtpRequest;
import com.campus.campusmate.models.AuditLog;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.InputSanitizer;
import com.campus.campusmate.utils.SessionManager;
import com.campus.campusmate.utils.SupabaseManager;

import timber.log.Timber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository extends BaseRepository {

    private static UserRepository instance;

    private UserRepository() {}

    public static synchronized UserRepository getInstance() {
        if (instance == null) instance = new UserRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
    }

    // --- Authentication ---

    public void registerUser(String email, String password, User userModel, OnCompleteListener listener) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);
        long now = System.currentTimeMillis();

        // Ensure no stale session interferes with the sign-up call.
        session.saveTokens(null, null);

        Timber.d("Registering new account via Supabase Auth");

        authApi.signUp(new AuthRequest(sanitizedEmail, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    String errorBody = "No details";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception e) {
                        Timber.e(e, "Failed to read sign-up error body");
                    }

                    Timber.e("Auth sign-up FAILED " + response.code() + ": " + errorBody);

                    if (response.code() == 422 || errorBody.toLowerCase(java.util.Locale.ROOT).contains("already registered")) {
                        listener.onError("Email address is already registered");
                    } else {
                        listener.onError("Registration failed: " + errorBody);
                    }
                    return;
                }

                AuthResponse auth = response.body();
                String uid = auth.getEffectiveUid();
                // The profile insert below is authenticated as the new user (the
                // users_insert_self RLS policy requires auth.uid() = uid), so the
                // session must be saved before making that call.
                session.saveTokens(auth.accessToken, auth.refreshToken);

                Map<String, Object> userData = new HashMap<>();
                userData.put("uid", uid);
                userData.put("email", sanitizedEmail);
                userData.put("role", userModel.getRole());
                userData.put("first_name", userModel.getFirstName());
                userData.put("middle_name", userModel.getMiddleName());
                userData.put("last_name", userModel.getLastName());
                userData.put("department", userModel.getDepartment());
                userData.put("status", User.STATUS_PENDING);
                userData.put("email_verified", false);
                userData.put("created_at", now);
                userData.put("block", userModel.getBlock());
                userData.put("year_level", userModel.getYearLevel());
                userData.put("address", userModel.getAddress());
                userData.put("profession", userModel.getProfession());
                userData.put("organization_name", userModel.getOrganizationName());
                userData.put("organization_description", userModel.getOrganizationDescription());
                userData.put("organization_id", userModel.getOrganizationId());

                dataApi.insertUserMap(userData, "return=minimal").enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            logAudit(uid, sanitizedEmail, AuditLog.ACTION_CREATE,
                                    AuditLog.TARGET_USER, uid, "User registered");
                            listener.onSuccess();
                        } else {
                            String errorBody = "No details";
                            try {
                                if (response.errorBody() != null) errorBody = response.errorBody().string();
                            } catch (Exception e) {
                                Timber.e(e, "Failed to read profile-insert error body");
                            }
                            Timber.e("Profile insert FAILED " + response.code() + ": " + errorBody);
                            session.saveTokens(null, null);
                            listener.onError("Registration failed: " + errorBody);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        session.saveTokens(null, null);
                        listener.onError(t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void loginUser(String email, String password, OnLoginListener listener) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);

        // Ensure no stale tokens interfere with the sign-in call itself.
        session.saveTokens(null, null);

        Timber.d("Attempting login");

        authApi.signIn("password", new AuthRequest(sanitizedEmail, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Timber.w("Sign-in failed: " + response.code());
                    listener.onError("invalid_credentials");
                    return;
                }

                AuthResponse auth = response.body();
                String uid = auth.getEffectiveUid();
                session.saveTokens(auth.accessToken, auth.refreshToken);

                Map<String, String> filter = new HashMap<>();
                filter.put("uid", "eq." + uid);
                dataApi.getUsers(filter).enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            User user = response.body().get(0);
                            Timber.d("Login profile loaded");
                            finishLogin(user, listener);
                        } else {
                            Timber.e("No profile row for authenticated user");
                            session.saveTokens(null, null);
                            listener.onError("server_error");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        Timber.e("Profile fetch network failure: " + t.getMessage());
                        session.saveTokens(null, null);
                        listener.onError("network_error");
                    }
                });
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Timber.e("Login network failure: " + t.getMessage());
                listener.onError("network_error");
            }
        });
    }

    private void finishLogin(User user, OnLoginListener listener) {
        if (User.STATUS_REJECTED.equals(user.getStatus())) {
            listener.onError("account_rejected");
        } else if (User.ROLE_ADMIN.equals(user.getRole())) {
            // Admins are always allowed if not explicitly rejected
            listener.onSuccess(user);
        } else if (!User.STATUS_APPROVED.equals(user.getStatus())
                && !User.ROLE_LIVE_DISPLAY.equals(user.getRole())) {
            listener.onError("account_pending");
        } else {
            listener.onSuccess(user);
        }
    }

    // --- Forgot Password (Email OTP) ---

    public void sendPasswordResetOtp(String email, OnCompleteListener listener) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);

        // Verify this email actually belongs to a registered account before triggering
        // Supabase Auth's recovery email, which otherwise silently no-ops (HTTP 200, no
        // email sent) for any address with no matching auth.users row and would leave the
        // UI falsely reporting "code sent".
        Map<String, String> filter = new HashMap<>();
        filter.put("email", "ilike." + sanitizedEmail);
        dataApi.getUsers(filter).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    listener.onError("No account found with this email");
                    return;
                }
                enqueue(authApi.recover(new EmailRequest(sanitizedEmail)), new OnResult<Void>() {
                    @Override
                    public void onSuccess(Void body) {
                        listener.onSuccess();
                    }

                    @Override
                    public void onError(String message) {
                        listener.onError("Failed to send verification code");
                    }
                });
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void verifyPasswordResetOtp(String email, String otp, OnCompleteListener listener) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);
        VerifyOtpRequest req = new VerifyOtpRequest(sanitizedEmail, otp.trim());
        enqueue(authApi.verifyOtp(req), new OnResult<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse auth) {
                if (auth == null || !auth.isSuccess()) {
                    listener.onError("Invalid or expired code");
                    return;
                }
                // Temporary recovery session; used only to authorize the password update below,
                // then cleared in resetPassword() so it never becomes a real logged-in session.
                session.saveTokens(auth.accessToken, auth.refreshToken);
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Invalid or expired code");
            }
        });
    }

    public void resetPassword(String email, String newPassword, OnCompleteListener listener) {
        enqueue(authApi.updateUser(new UpdatePasswordRequest(newPassword)), new OnResult<SupabaseUser>() {
            @Override
            public void onSuccess(SupabaseUser body) {
                session.clearSession();
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                session.clearSession();
                listener.onError("Failed to reset password");
            }
        });
    }

    public void logout() {
        String userId = session.getUserId();
        String userEmail = session.getUserEmail();
        if (userId != null) {
            logAudit(userId, userEmail, AuditLog.ACTION_LOGOUT,
                    AuditLog.TARGET_USER, userId, "User logged out");
        }
        session.clearSession();
    }

    // --- Profile ---

    public void fetchUserProfile(String uid, OnUserFetchedListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("uid", "eq." + uid);

        Timber.d("Fetching user profile");

        dataApi.getUsers(filter).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (!response.body().isEmpty()) {
                        User user = response.body().get(0);
                        Timber.d("Profile found");
                        listener.onFetched(user);
                    } else {
                        Timber.w("Profile not found for requested user");
                        listener.onError("user_not_found");
                    }
                } else {
                    String error = "Unknown";
                    try {
                        if (response.errorBody() != null) error = response.errorBody().string();
                    } catch (Exception e) {
                        Timber.e(e, "Failed to read profile-fetch error body");
                    }
                    Timber.e("Fetch failed: " + error);
                    listener.onError(error);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Timber.e("Network Failure: " + t.getMessage());
                listener.onError(t.getMessage());
            }
        });
    }

    public void observeUserProfile(String uid, MutableLiveData<User> liveData) {
        fetchUserProfile(uid, new OnUserFetchedListener() {
            @Override public void onFetched(User user) { liveData.postValue(user); }
            @Override public void onError(String message) { liveData.postValue(null); }
        });
    }

    public void updateUserProfile(String uid, User user, OnCompleteListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("uid", "eq." + uid);
        Map<String, Object> fields = buildUserPatch(user);

        Timber.d("Updating user profile");

        dataApi.patchUserMap(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    listener.onSuccess();
                } else {
                    String errorBody = "Update failed";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                            Timber.e("Update Error " + response.code() + ": " + errorBody);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Failed to read profile-update error body");
                    }
                    listener.onError(errorBody);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void updateUserStatus(String uid, String status, OnCompleteListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("uid", "eq." + uid);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", status);
        updateUserFields(filter, fields, listener);
    }

    public void updateUserFields(Map<String, String> filter, Map<String, Object> fields, OnCompleteListener listener) {
        enqueue(dataApi.patchUser(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Update failed");
            }
        });
    }

    // --- Admin: Display accounts ---

    /**
     * Creates a dedicated "live_display" login for a signage kiosk device via the
     * create-display-account Edge Function, which runs with the service-role key so this
     * doesn't authenticate the caller as the new account (unlike {@link #registerUser}).
     */
    public void createDisplayAccount(String email, String password, String label, OnCompleteListener listener) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(email);
        dataApi.createDisplayAccount(new CreateDisplayAccountRequest(sanitizedEmail, password, label))
                .enqueue(new Callback<CreateAccountResponse>() {
                    @Override
                    public void onResponse(Call<CreateAccountResponse> call, Response<CreateAccountResponse> response) {
                        CreateAccountResponse body = response.body();
                        if (response.isSuccessful() && body != null && body.success) {
                            listener.onSuccess();
                            return;
                        }

                        String message = "Failed to create display account";
                        try {
                            if (response.errorBody() != null) {
                                String raw = response.errorBody().string();
                                String parsed = new org.json.JSONObject(raw).optString("error", null);
                                if (parsed != null && !parsed.isEmpty()) message = parsed;
                            }
                        } catch (Exception e) {
                            Timber.e(e, "Failed to read create-display-account error body");
                        }
                        Timber.e("Create display account failed (" + response.code() + "): " + message);
                        listener.onError(message);
                    }

                    @Override
                    public void onFailure(Call<CreateAccountResponse> call, Throwable t) {
                        listener.onError(t.getMessage());
                    }
                });
    }

    // --- Admin: Fetch all users ---

    public void fetchAllUsers(MutableLiveData<List<User>> liveData) {
        enqueue(dataApi.getUsers(new HashMap<>()), new OnResult<List<User>>() {
            @Override
            public void onSuccess(List<User> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    public void fetchPendingUsers(MutableLiveData<List<User>> liveData) {
        Map<String, String> filter = new HashMap<>();
        filter.put("status", "eq." + User.STATUS_PENDING);
        enqueue(dataApi.getUsers(filter), new OnResult<List<User>>() {
            @Override
            public void onSuccess(List<User> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
                liveData.postValue(null);
            }
        });
    }

    // --- Audit ---


    private Map<String, Object> buildUserPatch(User user) {
        Map<String, Object> m = new HashMap<>();
        m.put("first_name", user.getFirstName());
        m.put("last_name", user.getLastName());
        m.put("middle_name", user.getMiddleName());
        m.put("department", user.getDepartment());
        m.put("profile_image_url", user.getProfileImageUrl());
        m.put("role", user.getRole());
        m.put("status", user.getStatus());
        m.put("block", user.getBlock());
        m.put("year_level", user.getYearLevel());
        m.put("address", user.getAddress());
        m.put("profession", user.getProfession());
        m.put("organization_name", user.getOrganizationName());
        m.put("organization_description", user.getOrganizationDescription());
        m.put("organization_id", user.getOrganizationId());
        m.put("created_at", user.getCreatedAt());
        m.put("updated_at", System.currentTimeMillis());
        return m;
    }

    // --- Listeners ---

    public interface OnCompleteListener {
        void onSuccess();
        void onError(String message);
    }

    public interface OnLoginListener {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface OnUserFetchedListener {
        void onFetched(User user);
        void onError(String message);
    }
}
