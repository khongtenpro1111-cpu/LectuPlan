package com.tkyl.lectuplan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SmartSlotAdapter extends RecyclerView.Adapter<SmartSlotAdapter.ViewHolder> {

    public static class FoundSlot {
        public String dateIso;
        public String dayDisplay;
        public int startMin;
        public int endMin;
        public String timeRangeStr;
        public int durationMinutes;
        public String durationFormatted;
        public String contextDesc;
        
        // AI fields
        public int score;
        public String aiRecommendation;

        public FoundSlot(String dateIso, String dayDisplay, int startMin, int endMin, String timeRangeStr, int durationMinutes, String durationFormatted, String contextDesc) {
            this.dateIso = dateIso;
            this.dayDisplay = dayDisplay;
            this.startMin = startMin;
            this.endMin = endMin;
            this.timeRangeStr = timeRangeStr;
            this.durationMinutes = durationMinutes;
            this.durationFormatted = durationFormatted;
            this.contextDesc = contextDesc;
            this.score = 100; // Default base score
            this.aiRecommendation = "";
        }
    }

    public interface OnSlotScheduleListener {
        void onSchedule(FoundSlot slot);
        void onShare(FoundSlot slot); // Thêm event Share
    }

    private List<FoundSlot> list;
    private OnSlotScheduleListener listener;

    public SmartSlotAdapter(List<FoundSlot> list, OnSlotScheduleListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateData(List<FoundSlot> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_found_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoundSlot slot = list.get(position);
        holder.tvDayTitle.setText(slot.dayDisplay);
        holder.tvTimeRange.setText("🟢 " + slot.timeRangeStr + " (" + slot.durationFormatted + ")");
        
        String ctxText = slot.contextDesc != null ? slot.contextDesc : "Khả dụng nhận việc";
        if (slot.aiRecommendation != null && !slot.aiRecommendation.isEmpty()) {
            ctxText += "\n💡 AI: " + slot.aiRecommendation;
        }
        holder.tvContext.setText(ctxText);

        if (slot.score > 0) {
            holder.tvAiScoreBadge.setVisibility(View.VISIBLE);
            holder.tvAiScoreBadge.setText("✨ " + slot.score + "đ");
            if (slot.score >= 120) {
                holder.tvAiScoreBadge.setBackgroundResource(R.drawable.bg_ai_badge); // Vàng
            } else if (slot.score < 80) {
                // Đỏ hoặc xám
                holder.tvAiScoreBadge.setBackgroundColor(android.graphics.Color.parseColor("#EF4444"));
            } else {
                holder.tvAiScoreBadge.setBackgroundColor(android.graphics.Color.parseColor("#10B981")); // Xanh
            }
        } else {
            holder.tvAiScoreBadge.setVisibility(View.GONE);
        }

        holder.btnSchedule.setOnClickListener(v -> {
            if (listener != null) listener.onSchedule(slot);
        });
        
        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(v -> {
                if (listener != null) listener.onShare(slot);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayTitle;
        TextView tvTimeRange;
        TextView tvContext;
        TextView tvAiScoreBadge;
        Button btnSchedule;
        Button btnShare;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayTitle = itemView.findViewById(R.id.tvFoundDayTitle);
            tvTimeRange = itemView.findViewById(R.id.tvFoundTimeRange);
            tvContext = itemView.findViewById(R.id.tvFoundContext);
            tvAiScoreBadge = itemView.findViewById(R.id.tvAiScoreBadge);
            btnSchedule = itemView.findViewById(R.id.btnScheduleSlot);
            btnShare = itemView.findViewById(R.id.btnShareSlot);
        }
    }
}

