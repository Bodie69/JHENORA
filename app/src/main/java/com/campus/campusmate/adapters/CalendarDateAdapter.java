package com.campus.campusmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.campus.campusmate.R;

import java.util.List;

/**
 * Horizontal date-strip adapter for the Calendar agenda screen (Today, Tomorrow,
 * then upcoming dates), each optionally carrying a small event-count badge.
 */
public class CalendarDateAdapter extends RecyclerView.Adapter<CalendarDateAdapter.ViewHolder> {

    public interface OnDateClickListener {
        void onDateClick(int position);
    }

    public static class DateItem {
        public final long timestamp;
        public final String label;
        public final int eventCount;

        public DateItem(long timestamp, String label, int eventCount) {
            this.timestamp = timestamp;
            this.label = label;
            this.eventCount = eventCount;
        }
    }

    private final Context context;
    private List<DateItem> items;
    private int selectedPosition = 0;
    private OnDateClickListener listener;

    public CalendarDateAdapter(Context context, List<DateItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setListener(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    public void submitList(List<DateItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_calendar_date, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DateItem item = items.get(position);
        boolean isSelected = position == selectedPosition;

        holder.tvLabel.setText(item.label);
        holder.tvLabel.setTextColor(ContextCompat.getColor(context,
                isSelected ? R.color.m3_on_surface : R.color.m3_on_surface_variant));
        holder.tvLabel.setTypeface(holder.tvLabel.getTypeface(), isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        if (item.eventCount > 0) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(item.eventCount > 9 ? "9+" : String.valueOf(item.eventCount));
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDateClick(holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvLabel;
        final TextView tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLabel = itemView.findViewById(R.id.tv_label);
            tvBadge = itemView.findViewById(R.id.tv_badge);
        }
    }
}
