package com.campus.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.campus.campusmate.R;
import com.campus.campusmate.models.Announcement;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DepartmentLogos;
import com.campus.campusmate.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Announcement announcement);
        void onItemLongClick(Announcement announcement, View anchor);
    }

    private final Context context;
    private List<Announcement> items = new ArrayList<>();
    private OnItemClickListener listener;

    public AnnouncementAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Announcement> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }

            @Override
            public int getNewListSize() { return newList != null ? newList.size() : 0; }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                String oldId = items.get(oldPos).getId();
                String newId = newList.get(newPos).getId();
                return oldId != null && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Announcement a = items.get(oldPos);
                Announcement b = newList.get(newPos);
                return a.getUpdatedAt() == b.getUpdatedAt()
                        && a.getStatus().equals(b.getStatus());
            }
        });
        items = newList != null ? newList : new ArrayList<>();
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement a = items.get(position);
        holder.bind(a);
    }

    @Override
    public int getItemCount() { return items.size(); }

    public Announcement getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView card;
        private final TextView tvTitle;
        private final TextView tvAuthor;
        private final TextView tvDate;
        private final TextView tvDept;
        private final TextView tvPriority;
        private final TextView tvStatus;
        private final TextView tvPreview;
        private final View priorityBar;
        private final ImageView ivThumbnail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDept = itemView.findViewById(R.id.tv_dept);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvPreview = itemView.findViewById(R.id.tv_preview);
            priorityBar = itemView.findViewById(R.id.priority_bar);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
        }

        void bind(Announcement a) {
            tvTitle.setText(a.getTitle());
            tvAuthor.setText(a.getAuthorName() != null ? a.getAuthorName() : "");
            tvDate.setText(DateUtils.getRelativeTime(a.getCreatedAt()));
            tvPreview.setText(a.getWhat() != null ? a.getWhat() : "");

            // Department tag
            if (tvDept != null) {
                boolean isGeneral = Announcement.VISIBILITY_GENERAL.equals(a.getVisibility());
                String dept = isGeneral ? context.getString(R.string.label_general) : a.getDepartment();
                tvDept.setText(dept != null ? dept : "");
                DepartmentLogos.bind(tvDept, isGeneral ? Constants.DEPT_GENERAL : a.getDepartment());
            }

            // Priority badge
            if (tvPriority != null) {
                UiUtils.applyPriorityBadge(tvPriority, a.getPriority() != null ? a.getPriority() : "normal");
            }

            // Status badge (for author view)
            if (tvStatus != null) {
                UiUtils.applyStatusBadge(tvStatus, a.getStatus() != null ? a.getStatus() : "pending");
                tvStatus.setVisibility(View.VISIBLE);
            }

            // Priority accent bar
            if (priorityBar != null) {
                if (Announcement.PRIORITY_URGENT.equals(a.getPriority())) {
                    priorityBar.setBackgroundColor(context.getColor(R.color.danger));
                    priorityBar.setVisibility(View.VISIBLE);
                } else if (Announcement.PRIORITY_HIGH.equals(a.getPriority())) {
                    priorityBar.setBackgroundColor(context.getColor(R.color.warning));
                    priorityBar.setVisibility(View.VISIBLE);
                } else {
                    priorityBar.setVisibility(View.INVISIBLE);
                }
            }

            if (ivThumbnail != null) {
                if (a.getImageUrl() != null && !a.getImageUrl().isEmpty()) {
                    ivThumbnail.setVisibility(View.VISIBLE);
                    Glide.with(context).load(a.getImageUrl()).centerCrop().into(ivThumbnail);
                } else {
                    ivThumbnail.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(a);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onItemLongClick(a, v);
                return true;
            });
        }
    }
}
