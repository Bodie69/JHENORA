package com.campus.campusmate.fragments.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.campus.campusmate.base.BaseFragment;
import com.campus.campusmate.databinding.FragmentAdminHomeBinding;

public class AdminHomeFragment extends BaseFragment<FragmentAdminHomeBinding> {

    private OnModuleSelectedListener listener;

    public interface OnModuleSelectedListener {
        void onModuleSelected(int position);
    }

    public void setOnModuleSelectedListener(OnModuleSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected FragmentAdminHomeBinding inflateBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAdminHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        binding.cardStats.setOnClickListener(v -> notifySelected(0));
        binding.cardApprovals.setOnClickListener(v -> notifySelected(1));
        binding.cardUsers.setOnClickListener(v -> notifySelected(2));
        binding.cardOrgs.setOnClickListener(v -> notifySelected(3));
        binding.cardBulletin.setOnClickListener(v -> notifySelected(4));
        binding.cardEvents.setOnClickListener(v -> notifySelected(5));
        binding.cardDepts.setOnClickListener(v -> notifySelected(6));
    }

    @Override
    protected void setupObservers() {
        // No observers
    }

    private void notifySelected(int position) {
        if (listener != null) {
            listener.onModuleSelected(position);
        }
    }
}
