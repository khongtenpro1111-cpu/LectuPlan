package com.tkyl.lectuplan;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onItemClick(NotificationModel item);
        void onSelectionChanged(int selectedCount);
    }

    private List<NotificationModel> list;
    private OnNotificationClickListener listener;
    private boolean isMultiSelectMode = false;
    private Set<Integer> selectedIds = new HashSet<>();

    public NotificationAdapter(List<NotificationModel> list, OnNotificationClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateData(List<NotificationModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        this.isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    public Set<Integer> getSelectedIds() {
        return selectedIds;
    }

    public void selectAll() {
        if (list == null) return;
        selectedIds.clear();
        for (NotificationModel item : list) {
            selectedIds.add(item.getId());
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel item = list.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTimestamp.setText(item.getTimestamp());

        // Hiển thị trạng thái chưa xem / đã xem
        if (item.isRead()) {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvTitle.setTextColor(Color.parseColor("#475569"));
        } else {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvTitle.setTextColor(Color.parseColor("#0F172A"));
        }

        // Chế độ Chọn Nhiều
        if (isMultiSelectMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setChecked(selectedIds.contains(item.getId()));
        } else {
            holder.cbSelect.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                toggleSelection(item.getId());
            } else {
                if (listener != null) listener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                setMultiSelectMode(true);
                toggleSelection(item.getId());
                return true;
            }
            return false;
        });

        holder.cbSelect.setOnClickListener(v -> toggleSelection(item.getId()));
    }

    private void toggleSelection(int id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTimestamp;
        View viewUnreadDot;
        CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTimestamp = itemView.findViewById(R.id.tvNotifTimestamp);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            cbSelect = itemView.findViewById(R.id.cbSelectNotif);
        }
    }
}
