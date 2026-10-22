package com.campus.campusmate.fragments.admin;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.campus.campusmate.R;
import com.campus.campusmate.databinding.FragmentAdminDepartmentsBinding;
import com.campus.campusmate.models.Department;
import com.campus.campusmate.utils.UiUtils;
import com.campus.campusmate.viewmodels.AdminViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin list of departments. Each row shows the department's logo (or its code badge when no
 * logo has been uploaded) and lets the admin upload, replace, or remove that logo.
 */
public class AdminDepartmentsFragment extends Fragment {

    private FragmentAdminDepartmentsBinding binding;
    private AdminViewModel viewModel;
    private DeptAdapter adapter;
    private String currentQuery = "";
    private final List<Department> allDepartments = new ArrayList<>();

    private final ActivityResultLauncher<String> pickLogoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    viewModel.uploadPendingDepartmentLogo(uri);
                } else {
                    viewModel.setPendingLogoDepartment(null);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDepartmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AdminViewModel.class);

        adapter = new DeptAdapter(this::onLogoClicked);
        binding.rvDepartments.setAdapter(adapter);

        setupSearch();
        setupObservers();
        viewModel.loadDepartments();
    }

    private void setupObservers() {
        viewModel.departments.observe(getViewLifecycleOwner(), departments -> {
            if (departments == null) {
                UiUtils.showSnackbar(binding.getRoot(), getString(R.string.dept_load_failed));
                return;
            }
            allDepartments.clear();
            allDepartments.addAll(departments);
            filter();
        });

        viewModel.uploadingDepartmentId.observe(getViewLifecycleOwner(), adapter::setUploadingId);
    }

    private void onLogoClicked(Department department) {
        if (viewModel.uploadingDepartmentId.getValue() != null) return;

        if (TextUtils.isEmpty(department.getLogoUrl())) {
            pickLogo(department);
            return;
        }

        String[] options = {
                getString(R.string.dept_logo_option_change),
                getString(R.string.dept_logo_option_remove)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dept_logo_dialog_title, department.getCode()))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) pickLogo(department);
                    else viewModel.removeDepartmentLogo(department);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void pickLogo(Department department) {
        viewModel.setPendingLogoDepartment(department);
        pickLogoLauncher.launch("image/*");
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                filter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                UiUtils.hideKeyboard(v);
                filter();
                return true;
            }
            return false;
        });
    }

    private void filter() {
        if (currentQuery.isEmpty()) {
            adapter.update(new ArrayList<>(allDepartments));
            return;
        }

        List<Department> filtered = new ArrayList<>();
        for (Department d : allDepartments) {
            if ((d.getName() != null && d.getName().toLowerCase().contains(currentQuery))
                    || (d.getCode() != null && d.getCode().toLowerCase().contains(currentQuery))
                    || (d.getDescription() != null && d.getDescription().toLowerCase().contains(currentQuery))) {
                filtered.add(d);
            }
        }
        adapter.update(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface OnLogoClickListener {
        void onLogoClick(Department department);
    }

    private static class DeptAdapter extends RecyclerView.Adapter<DeptAdapter.VH> {
        private List<Department> items = new ArrayList<>();
        private final OnLogoClickListener listener;
        private String uploadingId;

        DeptAdapter(OnLogoClickListener listener) { this.listener = listener; }

        void update(List<Department> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        void setUploadingId(@Nullable String id) {
            this.uploadingId = id;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_department, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Department d = items.get(position);
            holder.tvCode.setText(d.getCode());
            holder.tvName.setText(d.getName());
            holder.tvDesc.setText(d.getDescription() != null ? d.getDescription() : "");

            boolean hasLogo = !TextUtils.isEmpty(d.getLogoUrl());
            if (hasLogo) {
                holder.ivLogo.setVisibility(View.VISIBLE);
                holder.badge.setVisibility(View.INVISIBLE);
                Glide.with(holder.ivLogo)
                        .load(d.getLogoUrl())
                        .centerCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        @NonNull Target<Drawable> target,
                                                        boolean isFirstResource) {
                                // Broken/deleted file: fall back to the code badge rather
                                // than an empty circle.
                                holder.ivLogo.setVisibility(View.GONE);
                                holder.badge.setVisibility(View.VISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Drawable resource,
                                                           @NonNull Object model,
                                                           Target<Drawable> target,
                                                           @NonNull DataSource dataSource,
                                                           boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(holder.ivLogo);
            } else {
                Glide.with(holder.ivLogo).clear(holder.ivLogo);
                holder.ivLogo.setVisibility(View.GONE);
                holder.badge.setVisibility(View.VISIBLE);
            }

            boolean uploading = d.getId() != null && d.getId().equals(uploadingId);
            holder.progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
            holder.editBadge.setVisibility(uploading ? View.GONE : View.VISIBLE);
            holder.frameLogo.setEnabled(uploadingId == null);
            holder.frameLogo.setOnClickListener(v -> listener.onLogoClick(d));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvCode, tvName, tvDesc;
            final ImageView ivLogo;
            final View badge, frameLogo;
            final ProgressBar progress;
            final ImageView editBadge;

            VH(@NonNull View v) {
                super(v);
                tvCode = v.findViewById(R.id.tv_code);
                tvName = v.findViewById(R.id.tv_name);
                tvDesc = v.findViewById(R.id.tv_description);
                ivLogo = v.findViewById(R.id.iv_logo);
                badge = v.findViewById(R.id.layout_code_badge);
                frameLogo = v.findViewById(R.id.frame_logo);
                progress = v.findViewById(R.id.progress_logo);
                editBadge = v.findViewById(R.id.iv_logo_edit);
            }
        }
    }
}
