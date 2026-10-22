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
import com.campus.campusmate.models.Event;
import com.campus.campusmate.utils.DateUtils;
import com.campus.campusmate.utils.Constants;
import com.campus.campusmate.utils.DepartmentLogos;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Event event);
        void onItemLongClick(Event event, View anchor);
    }

    private final Context context;
    private List<Event> items = new ArrayList<>();
    private OnItemClickListener listener;

    public EventAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Event> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }

            @Override
            public int getNewListSize() { return newList != null ? newList.size() : 0; }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                String a = items.get(oldPos).getId();
                String b = newList.get(newPos).getId();
                return a != null && a.equals(b);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).getStatus().equals(newList.get(newPos).getStatus());
            }
        });
        items = newList != null ? newList : new ArrayList<>();
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    public Event getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView card;
        private final TextView tvDay;
        private final TextView tvMonth;
        private final TextView tvTitle;
        private final TextView tvWhere;
        private final TextView tvAuthor;
        private final TextView tvDept;
        private final TextView tvStatus;
        private final View statusDot;
        private final ImageView ivThumbnail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvMonth = itemView.findViewById(R.id.tv_month);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvWhere = itemView.findViewById(R.id.tv_where);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvDept = itemView.findViewById(R.id.tv_dept);
            tvStatus = itemView.findViewById(R.id.tv_status);
            statusDot = itemView.findViewById(R.id.status_dot);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
        }

        void bind(Event e) {
            tvTitle.setText(e.getTitle());

            if (tvDay != null && e.getStartDate() > 0) {
                tvDay.setText(DateUtils.getDayOfMonth(e.getStartDate()));
            }
            if (tvMonth != null && e.getStartDate() > 0) {
                tvMonth.setText(DateUtils.getShortMonthName(e.getStartDate()));
            }

            if (tvWhere != null) tvWhere.setText(e.getWhere() != null ? e.getWhere() : "");
            if (tvAuthor != null) tvAuthor.setText(e.getAuthorName() != null ? e.getAuthorName() : "");

            if (tvDept != null) {
                boolean isGeneral = Event.VISIBILITY_GENERAL.equals(e.getVisibility());
                String dept = isGeneral ? context.getString(R.string.label_general) : e.getDepartment();
                tvDept.setText(dept != null ? dept : "");
                DepartmentLogos.bind(tvDept, isGeneral ? Constants.DEPT_GENERAL : e.getDepartment());
            }

            if (tvStatus != null) {
                String status = e.getStatus() != null ? e.getStatus() : "";
                switch (status) {
                    case Event.STATUS_APPROVED:
                        if (e.isUpcoming()) {
                            tvStatus.setText(R.string.event_upcoming);
                            tvStatus.setTextColor(context.getColor(R.color.success));
                        } else if (e.isOngoing()) {
                            tvStatus.setText(R.string.event_ongoing);
                            tvStatus.setTextColor(context.getColor(R.color.primary));
                        } else {
                            tvStatus.setText(R.string.event_past);
                            tvStatus.setTextColor(context.getColor(R.color.text_secondary));
                        }
                        break;
                    case Event.STATUS_PENDING:
                        tvStatus.setText(R.string.status_pending);
                        tvStatus.setTextColor(context.getColor(R.color.warning));
                        break;
                    case Event.STATUS_REJECTED:
                        tvStatus.setText(R.string.status_rejected);
                        tvStatus.setTextColor(context.getColor(R.color.danger));
                        break;
                }
            }

            if (ivThumbnail != null) {
                if (e.getImageUrl() != null && !e.getImageUrl().isEmpty()) {
                    ivThumbnail.setVisibility(View.VISIBLE);
                    Glide.with(context).load(e.getImageUrl()).centerCrop().into(ivThumbnail);
                } else {
                    ivThumbnail.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(e);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onItemLongClick(e, v);
                return true;
            });
        }
    }
}
