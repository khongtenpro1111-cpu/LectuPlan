package com.tkyl.lectuplan;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TimelineSlotAdapter extends RecyclerView.Adapter<TimelineSlotAdapter.ViewHolder> {

    public interface OnSlotClickListener {
        void onFreeSlotClick(TimelineSlot slot);
        void onBusyEventClick(EventModel event);
    }

    private List<TimelineSlot> slotList;
    private OnSlotClickListener listener;

    public TimelineSlotAdapter(List<TimelineSlot> slotList, OnSlotClickListener listener) {
        this.slotList = slotList;
        this.listener = listener;
    }

    public void updateData(List<TimelineSlot> newSlots) {
        this.slotList = newSlots;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineSlot slot = slotList.get(position);

        holder.tvTimeDisplay.setText(slot.getTimeDisplay());
        holder.tvTitle.setText(slot.getTitle());
        holder.tvSubtitle.setText(slot.getSubtitle());
        holder.tvSuggestion.setText(slot.getSuggestion());
        holder.tvBadge.setText(slot.getBadgeText());

        boolean isNight = (holder.itemView.getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int mainColor = Color.parseColor(slot.getColorHex());
        holder.viewLeftStrip.setBackgroundColor(mainColor);

        // Nền card nhẹ nhàng theo màu chủ đạo (hoặc nền tối nếu ở Dark Mode)
        if (isNight) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1E293B"));
            holder.cardView.setStrokeColor(Color.parseColor("#334155"));
            holder.tvTimeDisplay.setTextColor(Color.WHITE);
            holder.tvTitle.setTextColor(Color.WHITE);
            holder.tvSubtitle.setTextColor(Color.parseColor("#E2E8F0"));
            holder.tvSuggestion.setTextColor(Color.parseColor("#CBD5E1"));

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(16);
            badgeBg.setColor(Color.parseColor("#0F172A"));
            badgeBg.setStroke(2, mainColor);
            holder.tvBadge.setBackground(badgeBg);
            holder.tvBadge.setTextColor(mainColor);
        } else {
            try {
                int bgColor = Color.parseColor(slot.getBgHex());
                holder.cardView.setCardBackgroundColor(bgColor);
            } catch (Exception e) {
                holder.cardView.setCardBackgroundColor(Color.WHITE);
            }
            holder.cardView.setStrokeColor(Color.parseColor("#E2E8F0"));
            holder.tvTimeDisplay.setTextColor(Color.parseColor("#0F172A"));
            holder.tvTitle.setTextColor(Color.parseColor("#0F172A"));
            holder.tvSubtitle.setTextColor(Color.parseColor("#475569"));
            holder.tvSuggestion.setTextColor(Color.parseColor("#64748B"));

            // Tùy biến badge
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(16);
            badgeBg.setColor(Color.WHITE);
            badgeBg.setStroke(2, mainColor);
            holder.tvBadge.setBackground(badgeBg);
            holder.tvBadge.setTextColor(mainColor);
        }

        // Hiển thị nút "+ Chèn lịch" cho các khe giờ rảnh
        boolean isFree = slot.getSlotType() == TimelineSlot.SlotType.DEEP_FREE
                || slot.getSlotType() == TimelineSlot.SlotType.START_DAY_FREE
                || slot.getSlotType() == TimelineSlot.SlotType.END_DAY_FREE
                || slot.getSlotType() == TimelineSlot.SlotType.MICRO_FREE;

        if (isFree) {
            holder.btnQuickAdd.setVisibility(View.VISIBLE);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setCornerRadius(14);
            btnBg.setColor(mainColor);
            holder.btnQuickAdd.setBackground(btnBg);
            holder.btnQuickAdd.setOnClickListener(v -> {
                if (listener != null) listener.onFreeSlotClick(slot);
            });
            holder.cardView.setOnClickListener(v -> {
                if (listener != null) listener.onFreeSlotClick(slot);
            });
        } else {
            holder.btnQuickAdd.setVisibility(View.GONE);
            if (slot.getSourceEvent() != null) {
                holder.cardView.setOnClickListener(v -> {
                    if (listener != null) listener.onBusyEventClick(slot.getSourceEvent());
                });
            } else {
                holder.cardView.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return slotList != null ? slotList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        View viewLeftStrip;
        TextView tvTimeDisplay;
        TextView tvBadge;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvSuggestion;
        TextView btnQuickAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardTimelineSlot);
            viewLeftStrip = itemView.findViewById(R.id.viewSlotLeftStrip);
            tvTimeDisplay = itemView.findViewById(R.id.tvSlotTimeDisplay);
            tvBadge = itemView.findViewById(R.id.tvSlotBadge);
            tvTitle = itemView.findViewById(R.id.tvSlotTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSlotSubtitle);
            tvSuggestion = itemView.findViewById(R.id.tvSlotSuggestion);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAddSlot);
        }
    }
}

