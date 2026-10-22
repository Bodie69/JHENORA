package com.campus.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.AvatarLoader;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.DepartmentLogos;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final Context context;
    private List<User> items = new ArrayList<>();
    private OnUserClickListener listener;

    public UserAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<User> newList) {
        items = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitials;
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvRole;
        private final TextView tvDept;
        private final TextView tvStatus;
        private final TextView tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tv_initials);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvDept = itemView.findViewById(R.id.tv_dept);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
        }

        void bind(User u) {
            String name = u.getFullName();
            tvName.setText(name.isEmpty() ? u.getEmail() : name);
            if (tvEmail != null) tvEmail.setText(u.getEmail() != null ? u.getEmail() : "");
            if (tvRole != null) tvRole.setText(u.getRole() != null ? capitalize(u.getRole()) : "");
            if (tvDept != null) {
                tvDept.setText(u.getDepartment() != null ? u.getDepartment() : "");
                DepartmentLogos.bind(tvDept, u.getDepartment());
            }
            if (tvDate != null) tvDate.setText(DateUtils.formatDate(u.getCreatedAt()));

            // Initials avatar
            if (tvInitials != null) {
                String initials = getInitials(name, u.getEmail());
                tvInitials.setText(initials);
                tvInitials.setBackgroundResource(R.drawable.bg_avatar);
                if (ivAvatar != null) AvatarLoader.load(ivAvatar, tvInitials, u.getProfileImageUrl());
            }

            // Status badge
            if (tvStatus != null) {
                String status = u.getStatus() != null ? u.getStatus() : "pending";
                switch (status) {
                    case User.STATUS_APPROVED:
                        tvStatus.setText(R.string.status_approved);
                        tvStatus.setTextColor(context.getColor(R.color.success));
                        tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                        break;
                    case User.STATUS_REJECTED:
                        tvStatus.setText(R.string.status_rejected);
                        tvStatus.setTextColor(context.getColor(R.color.danger));
                        tvStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                        break;
                    default:
                        tvStatus.setText(R.string.status_pending);
                        tvStatus.setTextColor(context.getColor(R.color.warning));
                        tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                        break;
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(u);
            });
        }

        private String getInitials(String name, String email) {
            if (!name.isEmpty()) {
                String[] parts = name.trim().split("\\s+");
                if (parts.length >= 2) {
                    return String.valueOf(parts[0].charAt(0)).toUpperCase()
                            + String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
                }
                return String.valueOf(name.charAt(0)).toUpperCase();
            }
            if (email != null && !email.isEmpty()) {
                return String.valueOf(email.charAt(0)).toUpperCase();
            }
            return "?";
        }

        private String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
