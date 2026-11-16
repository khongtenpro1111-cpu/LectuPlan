package com.tkyl.lectuplan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class StagingEventAdapter extends RecyclerView.Adapter<StagingEventAdapter.ViewHolder> {

    private List<EventModel> eventList;
    private OnItemClickListener listener;
    private OnSelectionChangeListener selectionChangeListener;

    public interface OnItemClickListener {
        void onItemClick(int position, EventModel event);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged();
    }

    public StagingEventAdapter(List<EventModel> eventList, OnItemClickListener listener, OnSelectionChangeListener selectionChangeListener) {
        this.eventList = eventList;
        this.listener = listener;
        this.selectionChangeListener = selectionChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staging_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventModel event = eventList.get(position);

        holder.tvTitle.setText(event.getTitle() != null ? event.getTitle() : "Sự kiện chưa có tên");
        holder.tvDate.setText(event.getDate() != null && !event.getDate().isEmpty() ? event.getDate() : "???");

        String timeStr = "";
        if (event.getStartTime() != null && !event.getStartTime().isEmpty()) {
            timeStr = event.getStartTime() + " - " + event.getEndTime();
        } else {
            timeStr = "???";
        }
        holder.tvTime.setText(timeStr);

        String location = "";
        if (event.getSchoolCode() != null && !event.getSchoolCode().isEmpty()) {
            location += event.getSchoolCode();
        }
        if (event.getRoom() != null && !event.getRoom().isEmpty()) {
            location += (location.isEmpty() ? "" : " - ") + event.getRoom();
        }
        holder.tvLocation.setText(location);

        // Warning and Card Style
        if (event.hasMissingDateTime()) {
            holder.tvWarning.setVisibility(View.VISIBLE);
            holder.cardView.setStrokeColor(Color.parseColor("#FFA000")); // Orange warning
            holder.cardView.setStrokeWidth(3);
        } else {
            holder.tvWarning.setVisibility(View.GONE);
            holder.cardView.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardView.setStrokeWidth(1);
        }

        // CheckBox
        holder.checkBox.setOnCheckedChangeListener(null); // Prevent trigger
        holder.checkBox.setChecked(event.isSelectedForImport());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            event.setSelectedForImport(isChecked);
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position, event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        CheckBox checkBox;
        TextView tvTitle, tvDate, tvTime, tvLocation, tvWarning;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardStagingEvent);
            checkBox = itemView.findViewById(R.id.cbStagingItem);
            tvTitle = itemView.findViewById(R.id.tvStagingTitle);
            tvDate = itemView.findViewById(R.id.tvStagingDate);
            tvTime = itemView.findViewById(R.id.tvStagingTime);
            tvLocation = itemView.findViewById(R.id.tvStagingLocation);
            tvWarning = itemView.findViewById(R.id.tvStagingWarning);
        }
    }
}