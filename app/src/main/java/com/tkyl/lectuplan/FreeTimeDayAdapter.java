package com.tkyl.lectuplan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FreeTimeDayAdapter extends RecyclerView.Adapter<FreeTimeDayAdapter.ViewHolder> {

    public interface OnFreeDayClickListener {
        void onDayClick(FreeTimeDayModel item);
    }

    private List<FreeTimeDayModel> dayList;
    private OnFreeDayClickListener listener;

    public FreeTimeDayAdapter(List<FreeTimeDayModel> dayList) {
        this.dayList = dayList;
    }

    public FreeTimeDayAdapter(List<FreeTimeDayModel> dayList, OnFreeDayClickListener listener) {
        this.dayList = dayList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_free_time_day_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FreeTimeDayModel item = dayList.get(position);
        holder.tvTitle.setText(item.getDayTitle());
        holder.tvBadge.setText(item.getStatusBadge());
        try {
            boolean isNight = (holder.itemView.getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            if (isNight) {
                String colorStr = item.getStatusColor();
                if ("#15803D".equalsIgnoreCase(colorStr)) {
                    holder.tvBadge.setTextColor(Color.parseColor("#6EE7B7"));
                    holder.tvBadge.setBackgroundColor(Color.parseColor("#064E3B"));
                } else if ("#B91C1C".equalsIgnoreCase(colorStr)) {
                    holder.tvBadge.setTextColor(Color.parseColor("#FCA5A5"));
                    holder.tvBadge.setBackgroundColor(Color.parseColor("#450A0A"));
                } else if ("#B45309".equalsIgnoreCase(colorStr)) {
                    holder.tvBadge.setTextColor(Color.parseColor("#FCD34D"));
                    holder.tvBadge.setBackgroundColor(Color.parseColor("#451A03"));
                } else {
                    holder.tvBadge.setTextColor(Color.parseColor("#93C5FD"));
                    holder.tvBadge.setBackgroundColor(Color.parseColor("#1E3A8A"));
                }
            } else {
                holder.tvBadge.setTextColor(Color.parseColor(item.getStatusColor()));
                holder.tvBadge.setBackgroundColor(Color.parseColor(item.getStatusBgColor()));
            }
        } catch (Exception ignored) {}

        if (holder.tvMorningDetail != null) {
            String morning = item.getMorningDetail();
            holder.tvMorningDetail.setText(morning != null && !morning.isEmpty() ? morning : "🟢 Trống cả buổi sáng (07:00 - 11:30)");
        }

        if (holder.tvAfternoonDetail != null) {
            String afternoon = item.getAfternoonDetail();
            holder.tvAfternoonDetail.setText(afternoon != null && !afternoon.isEmpty() ? afternoon : "🟢 Trống cả buổi chiều (13:00 - 17:00)");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return dayList != null ? dayList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBadge, tvMorningDetail, tvAfternoonDetail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFreeDayTitle);
            tvBadge = itemView.findViewById(R.id.tvFreeStatusBadge);
            tvMorningDetail = itemView.findViewById(R.id.tvMorningDetail);
            tvAfternoonDetail = itemView.findViewById(R.id.tvAfternoonDetail);
        }
    }
}
