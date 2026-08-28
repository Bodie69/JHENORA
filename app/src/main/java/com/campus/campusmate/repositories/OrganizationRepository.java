package com.campus.campusmate.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.campus.campusmate.models.Organization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        enqueue(dataApi.insertOrganization(organization, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Failed to create organization");
            }
        });
    }

    public void updateOrganization(Organization organization, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + organization.getId());
        Map<String, Object> fields = buildOrgPatch(organization);
        enqueue(dataApi.patchOrganization(filter, fields, "return=minimal"), new OnResult<Void>() {
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

    public void deleteOrganization(String id, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        enqueue(dataApi.deleteOrganization(filter), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Delete failed");
            }
        });
    }

    public void approveOrganization(String id, OnSimpleListener listener) {
        Map<String, String> filter = new HashMap<>();
        filter.put("id", "eq." + id);
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", Organization.STATUS_APPROVED);
        enqueue(dataApi.patchOrganization(filter, fields, "return=minimal"), new OnResult<Void>() {
            @Override
            public void onSuccess(Void body) {
                listener.onSuccess();
            }

            @Override
            public void onError(String message) {
                listener.onError("Approval failed");
            }
        });
    }

    public void observeAllOrganizations(MutableLiveData<List<Organization>> liveData) {
        enqueue(dataApi.getOrganizations(new HashMap<>()), new OnResult<List<Organization>>() {
            @Override
            public void onSuccess(List<Organization> body) {
                liveData.postValue(body);
            }

            @Override
            public void onError(String message) {
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
