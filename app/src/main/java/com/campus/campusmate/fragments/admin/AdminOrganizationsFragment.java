package com.campus.campusmate.fragments.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.campus.campusmate.databinding.DialogCreateOrganizationBinding;
import com.campus.campusmate.databinding.FragmentAdminOrganizationsBinding;
import com.campus.campusmate.models.Organization;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminOrganizationsFragment extends Fragment {

    private FragmentAdminOrganizationsBinding binding;
    private AdminViewModel viewModel;
    private List<User> studentList = new ArrayList<>();
    private String currentQuery = "";
    private OrgAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminOrganizationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        adapter = new OrgAdapter(new ArrayList<>());
        binding.rvOrganizations.setAdapter(adapter);

        setupSearch();

        viewModel.organizations.observe(getViewLifecycleOwner(), orgs -> {
            refreshList(orgs);
        });

        viewModel.allUsers.observe(getViewLifecycleOwner(), users -> {
            studentList.clear();
            if (users != null) {
                for (User u : users) {
                    if (User.ROLE_STUDENT.equals(u.getRole())) {
                        studentList.add(u);
                    }
                }
            }
        });

        binding.fabCreateOrg.setOnClickListener(v -> showCreateOrgDialog());

        // Defensive re-fetch: guarantees the student list is loaded for this screen
        // even if the activity-level initial load hasn't completed yet.
        viewModel.loadAllUsers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // This tab's ViewPager2 page is kept alive (not recreated) while another tab is
        // shown, and organizations/students are otherwise only loaded once at Activity
        // startup, so without this, orgs created/renamed elsewhere would sit stale here
        // until the app is force-restarted.
        if (viewModel != null) {
            viewModel.loadOrganizations();
            viewModel.loadAllUsers();
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                refreshList(viewModel.organizations.getValue());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(v);
                refreshList(viewModel.organizations.getValue());
                return true;
            }
            return false;
        });
    }

    private void refreshList(List<Organization> all) {
        if (all == null) return;
        List<Organization> filtered = new ArrayList<>();
        for (Organization org : all) {
            boolean matches = currentQuery.isEmpty() ||
                    (org.getName() != null && org.getName().toLowerCase().contains(currentQuery)) ||
                    (org.getLeaderName() != null && org.getLeaderName().toLowerCase().contains(currentQuery)) ||
                    (org.getDepartment() != null && org.getDepartment().toLowerCase().contains(currentQuery));
            if (matches) filtered.add(org);
        }
        adapter.update(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static final String NO_LEADER_OPTION = "— No leader —";

    /** Display text shown for a student in the Assign Leader dropdown. */
    private String displayName(User s) {
        return s.getFullName() + " (" + s.getEmail() + ")";
    }

    /** Looks up a student by their dropdown display text against the *current* student list. */
    private User findStudentByDisplayName(String text) {
        for (User s : studentList) {
            if (displayName(s).equals(text)) return s;
        }
        return null;
    }

    private void showCreateOrgDialog() {
        DialogCreateOrganizationBinding dBinding = DialogCreateOrganizationBinding.inflate(getLayoutInflater());

        String[] depts = getResources().getStringArray(R.array.departments);
        dBinding.acOrgDept.setSimpleItems(depts);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dBinding.getRoot())
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();

        // Refresh right as the dialog appears (not just at build time) so a still-loading
        // student list has a chance to arrive before the dropdown is populated.
        dialog.setOnShowListener(d -> {
            String[] names = new String[studentList.size()];
            for (int i = 0; i < studentList.size(); i++) names[i] = displayName(studentList.get(i));
            dBinding.acOrgLeader.setSimpleItems(names);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = dBinding.etOrgName.getText().toString().trim();
                String desc = dBinding.etOrgDesc.getText().toString().trim();
                String dept = dBinding.acOrgDept.getText().toString();
                User selected = findStudentByDisplayName(dBinding.acOrgLeader.getText().toString());

                if (name.isEmpty()) {
                    UiUtils.showToast(requireContext(), "Name required");
                    return;
                }
                if (dept.isEmpty()) {
                    UiUtils.showToast(requireContext(), "Department required");
                    return;
                }
                if (selected == null) {
                    UiUtils.showToast(requireContext(), studentList.isEmpty()
                            ? "No student accounts found. Register a student account first."
                            : "Please select a student to lead this organization");
                    return;
                }

                Organization org = new Organization();
                org.setName(name);
                org.setDescription(desc);
                org.setDepartment(dept);

                viewModel.createOrganization(org, selected.getUid(), selected.getFullName());
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class OrgAdapter extends RecyclerView.Adapter<OrgAdapter.VH> {
        private List<Organization> items;

        OrgAdapter(List<Organization> items) { this.items = items; }

        void update(List<Organization> newItems) {
            items = newItems != null ? newItems : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_organization, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Organization org = items.get(position);
            holder.tvName.setText(org.getName());
            holder.tvDesc.setText(org.getDescription() != null ? org.getDescription() : "");
            holder.tvStatus.setText(org.getStatus() != null ? org.getStatus() : "pending");
            boolean hasLeader = org.getLeaderName() != null && !org.getLeaderName().isEmpty();
            holder.tvLeader.setText(hasLeader ? "Leader: " + org.getLeaderName() : "No leader assigned");
            
            UiUtils.applyStatusBadge(holder.tvStatus, org.getStatus());

            if (!Organization.STATUS_APPROVED.equals(org.getStatus())) {
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnApprove.setOnClickListener(v ->
                        viewModel.approveOrganization(org.getId()));
            } else {
                holder.btnApprove.setVisibility(View.GONE);
            }

            holder.btnOptions.setOnClickListener(v -> showOptionsMenu(v, org));
        }

        private void showOptionsMenu(View v, Organization org) {
            boolean hasLeader = org.getLeaderId() != null && !org.getLeaderId().isEmpty();

            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.getMenu().add("Edit");
            if (hasLeader) menu.getMenu().add("Remove Leader");
            menu.getMenu().add("Delete");
            menu.setOnMenuItemClickListener(item -> {
                if ("Edit".equals(item.getTitle())) {
                    showEditOrgDialog(org);
                } else if ("Remove Leader".equals(item.getTitle())) {
                    showRemoveLeaderConfirmDialog(org);
                } else if ("Delete".equals(item.getTitle())) {
                    showDeleteConfirmDialog(org);
                }
                return true;
            });
            menu.show();
        }

        private void showRemoveLeaderConfirmDialog(Organization org) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove Leader")
                    .setMessage("Remove " + org.getLeaderName() + " as the leader of " + org.getName() + "?")
                    .setPositiveButton("Remove", (dialog, which) -> viewModel.updateOrganization(org, null, null))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showEditOrgDialog(Organization org) {
            DialogCreateOrganizationBinding dBinding = DialogCreateOrganizationBinding.inflate(getLayoutInflater());
            dBinding.etOrgName.setText(org.getName());
            dBinding.etOrgDesc.setText(org.getDescription());

            String[] depts = getResources().getStringArray(R.array.departments);
            dBinding.acOrgDept.setSimpleItems(depts);
            dBinding.acOrgDept.setText(org.getDepartment(), false);

            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Edit Organization")
                    .setView(dBinding.getRoot())
                    .setPositiveButton("Update", null)
                    .setNegativeButton("Cancel", null)
                    .create();

            boolean hasExistingLeader = org.getLeaderId() != null && !org.getLeaderId().isEmpty();

            // Refresh right as the dialog appears (not just at build time) so a still-loading
            // student list has a chance to arrive before the dropdown is populated.
            dialog.setOnShowListener(d -> {
                String[] names = new String[studentList.size() + 1];
                names[0] = NO_LEADER_OPTION;
                User currentLeader = null;
                for (int i = 0; i < studentList.size(); i++) {
                    User s = studentList.get(i);
                    names[i + 1] = displayName(s);
                    if (s.getUid() != null && s.getUid().equals(org.getLeaderId())) currentLeader = s;
                }
                dBinding.acOrgLeader.setSimpleItems(names);

                // Whether the currently assigned leader shows up in this dropdown: once assigned,
                // their role is promoted to 'organization' so they no longer appear in the
                // student-only list above. Leave the field blank and surface who it is via helper
                // text instead of falsely pointing to "No leader" or forcing a re-pick.
                boolean currentLeaderMissingFromList = hasExistingLeader && currentLeader == null;

                if (currentLeader != null) {
                    dBinding.acOrgLeader.setText(displayName(currentLeader), false);
                } else if (!hasExistingLeader) {
                    dBinding.acOrgLeader.setText(NO_LEADER_OPTION, false);
                } else {
                    dBinding.tilOrgLeader.setHelperTextEnabled(true);
                    dBinding.tilOrgLeader.setHelperText("Currently assigned: " + org.getLeaderName()
                            + " — pick a student to reassign, or \"" + NO_LEADER_OPTION + "\" to remove");
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = dBinding.etOrgName.getText().toString().trim();
                    String leaderText = dBinding.acOrgLeader.getText().toString().trim();

                    if (name.isEmpty()) {
                        UiUtils.showToast(requireContext(), "Name required");
                        return;
                    }
                    if (dBinding.acOrgDept.getText().toString().trim().isEmpty()) {
                        UiUtils.showToast(requireContext(), "Department required");
                        return;
                    }

                    String leaderId;
                    String leaderName;
                    if (leaderText.equals(NO_LEADER_OPTION)) {
                        leaderId = null;
                        leaderName = null;
                    } else if (leaderText.isEmpty() && currentLeaderMissingFromList) {
                        // Untouched: keep the existing (not-in-list) leader assigned.
                        leaderId = org.getLeaderId();
                        leaderName = org.getLeaderName();
                    } else {
                        User selected = findStudentByDisplayName(leaderText);
                        if (selected == null) {
                            UiUtils.showToast(requireContext(), studentList.isEmpty()
                                    ? "No student accounts found. Register a student account first."
                                    : "Please select a student, or \"" + NO_LEADER_OPTION + "\" to remove the leader");
                            return;
                        }
                        leaderId = selected.getUid();
                        leaderName = selected.getFullName();
                    }

                    org.setName(name);
                    org.setDescription(dBinding.etOrgDesc.getText().toString().trim());
                    org.setDepartment(dBinding.acOrgDept.getText().toString());

                    viewModel.updateOrganization(org, leaderId, leaderName);
                    dialog.dismiss();
                });
            });
            dialog.show();
        }

        private void showDeleteConfirmDialog(Organization org) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Organization")
                    .setMessage("Are you sure you want to delete " + org.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteOrganization(org.getId()))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvStatus, tvLeader;
            View btnApprove, btnOptions;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvDesc = v.findViewById(R.id.tv_description);
                tvStatus = v.findViewById(R.id.tv_status);
                tvLeader = v.findViewById(R.id.tv_leader);
                btnApprove = v.findViewById(R.id.btn_approve);
                btnOptions = v.findViewById(R.id.btn_options);
            }
        }
    }
}
