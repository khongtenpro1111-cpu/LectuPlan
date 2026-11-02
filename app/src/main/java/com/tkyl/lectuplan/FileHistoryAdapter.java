package com.tkyl.lectuplan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class FileHistoryAdapter extends RecyclerView.Adapter<FileHistoryAdapter.ViewHolder> {

    private List<FileHistoryModel> historyList;
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onItemClick(FileHistoryModel item);
        void onViewContentClick(FileHistoryModel item);
        void onDownloadClick(FileHistoryModel item);
        void onDeleteClick(FileHistoryModel item);
    }

    public FileHistoryAdapter(List<FileHistoryModel> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    public void updateList(List<FileHistoryModel> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_file_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileHistoryModel item = historyList.get(position);
        holder.tvFileName.setText(item.getFileName());
        holder.tvTime.setText("🕒 " + item.getTimestamp());

        String typeStr = item.getFileType() != null ? item.getFileType() : "NHẬP FILE";
        String lowerType = typeStr.toLowerCase(Locale.getDefault());
        String lowerName = item.getFileName() != null ? item.getFileName().toLowerCase(Locale.getDefault()) : "";

        boolean isExport = lowerType.contains("xuất");

        if (isExport) {
            holder.tvIcon.setText(lowerName.endsWith(".pdf") ? "📄" : "📊");
            holder.tvBadge.setText("📊 " + typeStr + " • " + item.getRecordCount() + " mục");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#DBEAFE"));
            holder.tvBadge.setTextColor(Color.parseColor("#1D4ED8"));
        } else if (lowerType.contains("viết tay") || lowerType.contains("ghi tay")) {
            holder.tvIcon.setText("✍️");
            holder.tvBadge.setText("✍️ " + typeStr + " • " + item.getRecordCount() + " mục");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#EDE9FE"));
            holder.tvBadge.setTextColor(Color.parseColor("#6D28D9"));
        } else {
            if (lowerName.endsWith(".pdf")) {
                holder.tvIcon.setText("📄");
            } else if (lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt")) {
                holder.tvIcon.setText("📽️");
            } else if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc")) {
                holder.tvIcon.setText("📝");
            } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
                holder.tvIcon.setText("🖼️");
            } else {
                holder.tvIcon.setText("📊");
            }
            holder.tvBadge.setText("📥 " + typeStr + " • " + item.getRecordCount() + " mục");
            holder.tvBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
            holder.tvBadge.setTextColor(Color.parseColor("#15803D"));
        }

        // Bấm vào toàn bộ thẻ hoặc nút "📄 Xem nội dung" sẽ mở trình xem trực tiếp trong ứng dụng (không bị che chữ)
        View.OnClickListener viewContentListener = v -> {
            if (listener != null) listener.onViewContentClick(item);
        };
        holder.itemView.setOnClickListener(viewContentListener);
        if (holder.btnViewContent != null) {
            holder.btnViewContent.setOnClickListener(viewContentListener);
        }

        // Bấm nút "👁️ Mở file" sẽ mở bằng ứng dụng đọc file bên ngoài
        if (holder.btnOpenFile != null) {
            holder.btnOpenFile.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        // Bấm nút "⬇️ Tải về máy" sẽ tải/lưu file trực tiếp về thư mục Download của điện thoại
        if (holder.btnDownloadFile != null) {
            holder.btnDownloadFile.setOnClickListener(v -> {
                if (listener != null) listener.onDownloadClick(item);
            });
        }

        if (holder.btnDeleteHistory != null) {
            holder.btnDeleteHistory.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvFileName, tvBadge, tvTime, btnViewContent, btnOpenFile, btnDownloadFile;
        ImageButton btnDeleteHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvHistoryIcon);
            tvFileName = itemView.findViewById(R.id.tvHistoryFileName);
            tvBadge = itemView.findViewById(R.id.tvHistoryBadge);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
            btnViewContent = itemView.findViewById(R.id.btnViewContent);
            btnOpenFile = itemView.findViewById(R.id.btnOpenFile);
            btnDownloadFile = itemView.findViewById(R.id.btnDownloadFile);
            btnDeleteHistory = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}
