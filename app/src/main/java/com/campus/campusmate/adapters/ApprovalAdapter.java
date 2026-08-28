package com.campus.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.models.Event;
import com.campus.campusmate.models.User;
import com.campus.campusmate.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ApprovalAdapter extends RecyclerView.Adapter<ApprovalAdapter.ViewHolder> {

    public interface OnApprovalListener {
        void onApprove(Object item);
        void onReject(Object item);
        void onView(Object item);
    }

    private final Context context;
    private List<Object> items = new ArrayList<>();
    private OnApprovalListener listener;

    public ApprovalAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnApprovalListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Object> newList) {
        items = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_approval, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvAuthor;
        private final TextView tvDate;
        private final TextView tvDept;
        private final TextView tvType;
        private final View btnApprove;
        private final View btnReject;
        private final View btnView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDept = itemView.findViewById(R.id.tv_dept);
            tvType = itemView.findViewById(R.id.tv_type);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnView = itemView.findViewById(R.id.btn_view);
        }

        void bind(Object item) {
            boolean isEvent = item instanceof Event;
            boolean isUser = item instanceof User;

            String title;
            String authorName;
            String department;
            String visibility;
            long createdAt;

            if (isUser) {
                User u = (User) item;
                title = u.getFullName().isEmpty() ? u.getEmail() : u.getFullName();
                authorName = u.getEmail();
                department = u.getDepartment();
                visibility = null;
                createdAt = u.getCreatedAt();
            } else if (isEvent) {
                Event e = (Event) item;
                title = e.getTitle();
                authorName = e.getAuthorName();
                department = e.getDepartment();
                visibility = e.getVisibility();
                createdAt = e.getCreatedAt();
            } else {
                Announcement a = (Announcement) item;
                title = a.getTitle();
                authorName = a.getAuthorName();
                department = a.getDepartment();
                visibility = a.getVisibility();
                createdAt = a.getCreatedAt();
            }

            tvTitle.setText(title);
            if (tvAuthor != null) tvAuthor.setText(authorName != null ? authorName : "");
            if (tvDate != null) tvDate.setText(DateUtils.getRelativeTime(createdAt));
            if (tvDept != null) {
                tvDept.setText(Announcement.VISIBILITY_GENERAL.equals(visibility)
                        ? context.getString(R.string.label_general)
                        : department != null ? department : "");
            }
            if (tvType != null) {
                tvType.setText(isUser ? "User" : isEvent ? "Event" : "Announcement");
                tvType.setBackgroundResource(isUser ? R.drawable.bg_chip_user
                        : isEvent ? R.drawable.bg_chip_event : R.drawable.bg_chip_selected);
                tvType.setTextColor(context.getColor(R.color.white));
            }

            if (btnView != null) btnView.setVisibility(isUser ? View.GONE : View.VISIBLE);

            if (btnApprove != null) {
                btnApprove.setOnClickListener(v -> {
                    if (listener != null) listener.onApprove(item);
                });
            }
            if (btnReject != null) {
                btnReject.setOnClickListener(v -> {
                    if (listener != null) listener.onReject(item);
                });
            }
            if (btnView != null) {
                btnView.setOnClickListener(v -> {
                    if (listener != null) listener.onView(item);
                });
            }
        }
    }
}
