package com.tkyl.lectuplan;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomCalendarAdapter extends RecyclerView.Adapter<CustomCalendarAdapter.ViewHolder> {

    public static class DayCell {
        public Date date;
        public int dayNumber;
        public boolean isCurrentMonth;
        public boolean isToday;
        public String dateIso; // "yyyy-MM-dd"

        public DayCell(Date date, int dayNumber, boolean isCurrentMonth, boolean isToday, String dateIso) {
            this.date = date;
            this.dayNumber = dayNumber;
            this.isCurrentMonth = isCurrentMonth;
            this.isToday = isToday;
            this.dateIso = dateIso;
        }
    }

    public interface OnDayClickListener {
        void onDayClick(DayCell dayCell);
    }

    private List<DayCell> days;
    private String selectedDateIso;
    private Map<String, Set<Integer>> eventTypesPerDate; // dateIso -> Set of types: 1: work, 2: meeting, 3: handwritten
    private OnDayClickListener listener;

    public CustomCalendarAdapter(List<DayCell> days, String selectedDateIso, Map<String, Set<Integer>> eventTypesPerDate, OnDayClickListener listener) {
        this.days = days;
        this.selectedDateIso = selectedDateIso;
        this.eventTypesPerDate = eventTypesPerDate;
        this.listener = listener;
    }

    public void updateData(List<DayCell> newDays, String selectedDateIso, Map<String, Set<Integer>> eventTypesPerDate) {
        this.days = newDays;
        this.selectedDateIso = selectedDateIso;
        this.eventTypesPerDate = eventTypesPerDate;
        notifyDataSetChanged();
    }

    public void setSelectedDateIso(String selectedDateIso) {
        this.selectedDateIso = selectedDateIso;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day_cell, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayCell cell = days.get(position);
        holder.tvDayNumber.setText(String.valueOf(cell.dayNumber));

        boolean isSelected = cell.dateIso != null && cell.dateIso.equals(selectedDateIso);

        if (isSelected) {
            holder.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_selected_day);
            holder.tvDayNumber.setTextColor(Color.WHITE);
            holder.tvDayNumber.setTypeface(null, Typeface.BOLD);
        } else if (cell.isToday) {
            holder.tvDayNumber.setBackground(null);
            holder.tvDayNumber.setTextColor(Color.parseColor("#7C3AED"));
            holder.tvDayNumber.setTypeface(null, Typeface.BOLD);
        } else if (cell.isCurrentMonth) {
            holder.tvDayNumber.setBackground(null);
            holder.tvDayNumber.setTextColor(Color.parseColor("#1E293B"));
            holder.tvDayNumber.setTypeface(null, Typeface.NORMAL);
        } else {
            // Ngày của tháng trước / tháng sau (mờ đi)
            holder.tvDayNumber.setBackground(null);
            holder.tvDayNumber.setTextColor(Color.parseColor("#CBD5E1"));
            holder.tvDayNumber.setTypeface(null, Typeface.NORMAL);
        }

        // Hiển thị các chấm tròn màu sắc phân loại lịch công việc
        Set<Integer> types = (eventTypesPerDate != null && cell.dateIso != null) ? eventTypesPerDate.get(cell.dateIso) : null;
        if (types != null && !types.isEmpty() && cell.isCurrentMonth) {
            holder.dotWork.setVisibility(types.contains(1) ? View.VISIBLE : View.GONE);
            holder.dotMeeting.setVisibility(types.contains(2) ? View.VISIBLE : View.GONE);
            holder.dotHandwritten.setVisibility(types.contains(3) ? View.VISIBLE : View.GONE);
        } else {
            holder.dotWork.setVisibility(View.GONE);
            holder.dotMeeting.setVisibility(View.GONE);
            holder.dotHandwritten.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(cell);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        View dotWork, dotMeeting, dotHandwritten;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            dotWork = itemView.findViewById(R.id.dotWork);
            dotMeeting = itemView.findViewById(R.id.dotMeeting);
            dotHandwritten = itemView.findViewById(R.id.dotHandwritten);
        }
    }
}
