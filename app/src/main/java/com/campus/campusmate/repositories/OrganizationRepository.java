package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.api.SupabaseApi;
import com.campus.campusmate.models.Organization;
import com.campus.campusmate.utils.SupabaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrganizationRepository extends BaseRepository {

    private static OrganizationRepository instance;

    private OrganizationRepository() {}

    public static synchronized OrganizationRepository getInstance() {
        if (instance == null) instance = new OrganizationRepository();
        return instance;
    }

    @Override
    public void init(Context context) {
        super.init(context);
    }

    public void createOrganization(Organization organization, OnSimpleListener listener) {
        String id = UUID.randomUUID().toString();
        organization.setId(id);
        dataApi.insertOrganization(organization, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) listener.onSuccess();
                else listener.onError("Failed to create organization");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void updateOrganization(Organization organization, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + organization.getId());
        Map<String, Object> fields = buildOrgPatch(organization);
        dataApi.patchOrganization(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) listener.onSuccess();
                else listener.onError("Update failed");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void deleteOrganization(String id, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        dataApi.deleteOrganization(filter).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) listener.onSuccess();
                else listener.onError("Delete failed");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void approveOrganization(String id, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Organization.STATUS_APPROVED);
        dataApi.patchOrganization(filter, fields, "return=minimal").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) listener.onSuccess();
                else listener.onError("Approval failed");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void observeAllOrganizations(MutableLiveData<List<Organization>> liveData) {
        dataApi.getOrganizations(new HashMap<>()).enqueue(new Callback<List<Organization>>() {
            @Override
            public void onResponse(Call<List<Organization>> call, Response<List<Organization>> response) {
                liveData.postValue(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<List<Organization>> call, Throwable t) {
                liveData.postValue(null);
            }
        });
    }

    private Map<String, Object> buildOrgPatch(Organization o) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", o.getName());
        m.put("description", o.getDescription());
        m.put("leader_id", o.getLeaderId());
        m.put("leader_name", o.getLeaderName());
        m.put("image_url", o.getImageUrl());
        m.put("status", o.getStatus());
        m.put("department", o.getDepartment());
        return m;
    }

    public interface OnSimpleListener {
        void onSuccess();
        void onError(String message);
    }
}
