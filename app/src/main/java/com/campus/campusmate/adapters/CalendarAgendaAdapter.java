package com.campus.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a day's agenda — a mix of {@link CalendarAgendaItem}s backed by either
 * Announcements or Events — as colorful cards, cycling through a pastel palette
 * per card position. A small leading icon distinguishes the content type.
 */
public class CalendarAgendaAdapter extends RecyclerView.Adapter<CalendarAgendaAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CalendarAgendaItem item);
    }

    private static final int[] PALETTE = {
            R.color.cal_pastel_purple,
            R.color.cal_pastel_blue,
            R.color.cal_pastel_green,
            R.color.cal_pastel_coral,
            R.color.cal_pastel_yellow,
            R.color.cal_pastel_gray,
    };

    private final Context context;
    private List<CalendarAgendaItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public CalendarAgendaAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CalendarAgendaItem> newList) {
        items = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_calendar_agenda, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarAgendaItem item = items.get(position);

        holder.card.setCardBackgroundColor(ContextCompat.getColor(context, PALETTE[position % PALETTE.length]));
        holder.tvTitle.setText(item.title);
        holder.tvTime.setText(item.subtitle);
        holder.ivType.setImageResource(item.type == CalendarAgendaItem.Type.ANNOUNCEMENT
                ? R.drawable.ic_announcements : R.drawable.ic_calendar);
        holder.tvAvatar.setText(getInitials(item.authorName));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            if (!parts[i].isEmpty()) initials.append(parts[i].charAt(0));
        }
        return initials.toString().toUpperCase();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView ivType;
        final TextView tvTitle;
        final TextView tvTime;
        final TextView tvAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            ivType = itemView.findViewById(R.id.iv_type);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
        }
    }
}
