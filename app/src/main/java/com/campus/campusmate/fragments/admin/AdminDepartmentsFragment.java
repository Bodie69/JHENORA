package com.campus.campusmate.fragments.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.campus.campusmate.databinding.FragmentAdminDepartmentsBinding;
import com.campus.campusmate.models.Department;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.UiUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminDepartmentsFragment extends Fragment {

    private FragmentAdminDepartmentsBinding binding;
    private DeptAdapter adapter;
    private String currentQuery = "";

    private static final List<Department> DEPARTMENTS = Arrays.asList(
            createDept("bsit", "BSIT", "Bachelor of Science in Information Technology"),
            createDept("btled", "BTLED", "Bachelor of Technology and Livelihood Education"),
            createDept("educ", "EDUC", "Bachelor of Education")
    );

    private static Department createDept(String id, String code, String name) {
        Department d = new Department(id, name, code);
        return d;
    }

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

        adapter = new DeptAdapter(DEPARTMENTS);
        binding.rvDepartments.setAdapter(adapter);
        
        setupSearch();
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
            adapter.update(DEPARTMENTS);
            return;
        }

        List<Department> filtered = new ArrayList<>();
        for (Department d : DEPARTMENTS) {
            if ((d.getName() != null && d.getName().toLowerCase().contains(currentQuery))
                    || (d.getCode() != null && d.getCode().toLowerCase().contains(currentQuery))) {
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

    private static class DeptAdapter extends RecyclerView.Adapter<DeptAdapter.VH> {
        private List<Department> items;

        DeptAdapter(List<Department> items) { this.items = items; }

        void update(List<Department> newItems) {
            this.items = newItems;
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
            if (holder.tvDesc != null) {
                holder.tvDesc.setText(d.getDescription() != null ? d.getDescription() : "");
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCode, tvName, tvDesc;

            VH(@NonNull View v) {
                super(v);
                tvCode = v.findViewById(R.id.tv_code);
                tvName = v.findViewById(R.id.tv_name);
                tvDesc = v.findViewById(R.id.tv_description);
            }
        }
    }
}
