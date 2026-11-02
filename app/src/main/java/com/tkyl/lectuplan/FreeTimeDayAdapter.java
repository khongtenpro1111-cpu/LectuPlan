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

    private List<FreeTimeDayModel> dayList;

    public FreeTimeDayAdapter(List<FreeTimeDayModel> dayList) {
        this.dayList = dayList;
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
            holder.tvBadge.setTextColor(Color.parseColor(item.getStatusColor()));
            holder.tvBadge.setBackgroundColor(Color.parseColor(item.getStatusBgColor()));
        } catch (Exception ignored) {}
        holder.tvDetail.setText(item.getDetailText());
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBadge, tvDetail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFreeDayTitle);
            tvBadge = itemView.findViewById(R.id.tvFreeStatusBadge);
            tvDetail = itemView.findViewById(R.id.tvFreeTimeDetail);
        }
    }
}
