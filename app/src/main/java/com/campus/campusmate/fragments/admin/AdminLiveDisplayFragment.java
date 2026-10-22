package com.campus.campusmate.fragments.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.campus.campusmate.activities.LiveDisplayActivity;
import com.campus.campusmate.databinding.DialogCreateDisplayAccountBinding;
import com.campus.campusmate.databinding.FragmentAdminLiveDisplayBinding;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.utils.ValidationUtils;
import com.campus.campusmate.viewmodels.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AdminLiveDisplayFragment extends Fragment {

    private FragmentAdminLiveDisplayBinding binding;
    private AdminViewModel viewModel;
    private DisplayAccountAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminLiveDisplayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        adapter = new DisplayAccountAdapter(new ArrayList<>());
        binding.rvDisplayAccounts.setAdapter(adapter);

        viewModel.displayAccounts.observe(getViewLifecycleOwner(), this::refreshList);

        binding.btnPreviewDisplay.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LiveDisplayActivity.class)));

        binding.fabCreateDisplay.setOnClickListener(v -> showCreateDialog());

        // Defensive re-fetch, matching AdminOrganizationsFragment: guarantees the list is
        // loaded for this screen even if the activity-level initial load hasn't finished yet.
        viewModel.loadAllUsers();
    }

    private void refreshList(List<User> accounts) {
        adapter.update(accounts);
        boolean empty = accounts == null || accounts.isEmpty();
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showCreateDialog() {
        DialogCreateDisplayAccountBinding dBinding = DialogCreateDisplayAccountBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dBinding.getRoot())
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String label = dBinding.etDisplayLabel.getText().toString().trim();
            String email = dBinding.etDisplayEmail.getText().toString().trim();
            String password = dBinding.etDisplayPassword.getText().toString();

            if (label.isEmpty()) {
                UiUtils.showToast(requireContext(), "Label is required");
                return;
            }
            if (!ValidationUtils.isValidEmail(email)) {
                UiUtils.showToast(requireContext(), "A valid email is required");
                return;
            }
            if (!ValidationUtils.isValidPassword(password)) {
                UiUtils.showToast(requireContext(), "Password must be at least 8 characters");
                return;
            }

            viewModel.createDisplayAccount(email, password, label);
            dialog.dismiss();
            showCredentialsDialog(label, email, password);
        }));
        dialog.show();
    }

    /**
     * Shown once, right after creation: there's no way to view the password again later, and
     * whoever sets up the physical kiosk device needs to type these credentials in.
     */
    private void showCredentialsDialog(String label, String email, String password) {
        String message = "Label: " + label + "\nEmail: " + email + "\nPassword: " + password
                + "\n\nSave these now — the password can't be shown again.";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Display Account Created")
                .setMessage(message)
                .setPositiveButton("Done", null)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class DisplayAccountAdapter extends RecyclerView.Adapter<DisplayAccountAdapter.VH> {
        private List<User> items;

        DisplayAccountAdapter(List<User> items) {
            this.items = items;
        }

        void update(List<User> newItems) {
            items = newItems != null ? newItems : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_display_account, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User account = items.get(position);
            holder.tvName.setText(account.getFullName());
            holder.tvEmail.setText(account.getEmail());
            holder.tvCreated.setText("Created " + DateUtils.formatDate(account.getCreatedAt()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final android.widget.TextView tvName;
            final android.widget.TextView tvEmail;
            final android.widget.TextView tvCreated;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_name);
                tvEmail = itemView.findViewById(R.id.tv_email);
                tvCreated = itemView.findViewById(R.id.tv_created);
            }
        }
    }
}
