package com.tkyl.lectuplan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileHistoryAdapter extends RecyclerView.Adapter<FileHistoryAdapter.ViewHolder> {

    private List<FileHistoryModel> historyList;
    private OnHistoryClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<Integer> selectedHistoryIds = new HashSet<>();

    public interface OnHistoryClickListener {
        void onItemClick(FileHistoryModel item);
        void onViewContentClick(FileHistoryModel item);
        void onDownloadClick(FileHistoryModel item);
        void onDeleteClick(FileHistoryModel item);
        void onSelectionChanged(int selectedCount);
    }

    public FileHistoryAdapter(List<FileHistoryModel> historyList, OnHistoryClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    public void updateList(List<FileHistoryModel> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    public void setMultiSelectMode(boolean enable) {
        this.isMultiSelectMode = enable;
        if (!enable) {
            selectedHistoryIds.clear();
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedHistoryIds.size());
        }
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void selectAll() {
        if (historyList == null || historyList.isEmpty()) return;
        boolean allSelected = (selectedHistoryIds.size() == historyList.size());
        selectedHistoryIds.clear();
        if (!allSelected) {
            for (FileHistoryModel item : historyList) {
                selectedHistoryIds.add(item.getId());
            }
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedHistoryIds.size());
        }
    }

    public void clearSelection() {
        selectedHistoryIds.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(0);
        }
    }

    public List<Integer> getSelectedHistoryIds() {
        return new ArrayList<>(selectedHistoryIds);
    }

    public List<FileHistoryModel> getSelectedItems() {
        List<FileHistoryModel> selected = new ArrayList<>();
        if (historyList != null) {
            for (FileHistoryModel item : historyList) {
                if (selectedHistoryIds.contains(item.getId())) {
                    selected.add(item);
                }
            }
        }
        return selected;
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
        boolean isNight = (holder.itemView.getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isExport) {
            holder.tvIcon.setText(lowerName.endsWith(".pdf") ? "📄" : "📊");
            holder.tvBadge.setText("📊 " + typeStr + " • " + item.getRecordCount() + " mục");
            holder.tvBadge.setBackgroundColor(isNight ? Color.parseColor("#1E3A8A") : Color.parseColor("#DBEAFE"));
            holder.tvBadge.setTextColor(isNight ? Color.parseColor("#93C5FD") : Color.parseColor("#1D4ED8"));
        } else if (lowerType.contains("viết tay") || lowerType.contains("ghi tay")) {
            holder.tvIcon.setText("✍️");
            holder.tvBadge.setText("✍️ " + typeStr + " • " + item.getRecordCount() + " mục");
            holder.tvBadge.setBackgroundColor(isNight ? Color.parseColor("#3B0764") : Color.parseColor("#EDE9FE"));
            holder.tvBadge.setTextColor(isNight ? Color.parseColor("#C4B5FD") : Color.parseColor("#6D28D9"));
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
            holder.tvBadge.setBackgroundColor(isNight ? Color.parseColor("#064E3B") : Color.parseColor("#DCFCE7"));
            holder.tvBadge.setTextColor(isNight ? Color.parseColor("#6EE7B7") : Color.parseColor("#15803D"));
        }

        // Cập nhật trạng thái chọn nhiều (Multi-Select)
        if (holder.ivSelectHistory != null) {
            holder.ivSelectHistory.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
            boolean isSelected = selectedHistoryIds.contains(item.getId());
            holder.ivSelectHistory.setImageResource(isSelected ? R.drawable.ic_check_box_checked : R.drawable.ic_check_box_unchecked);
        }
        if (holder.btnDeleteHistory != null) {
            holder.btnDeleteHistory.setVisibility(isMultiSelectMode ? View.GONE : View.VISIBLE);
        }
        if (holder.layoutHistoryActions != null) {
            holder.layoutHistoryActions.setVisibility(isMultiSelectMode ? View.GONE : View.VISIBLE);
        }

        // Highlight thẻ khi được tích chọn
        if (holder.itemView instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) holder.itemView;
            float density = holder.itemView.getResources().getDisplayMetrics().density;
            if (isMultiSelectMode && selectedHistoryIds.contains(item.getId())) {
                card.setStrokeColor(Color.parseColor("#2563EB"));
                card.setStrokeWidth(Math.round(2 * density));
                card.setCardBackgroundColor(isNight ? Color.parseColor("#1E293B") : Color.parseColor("#EFF6FF"));
            } else {
                card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.border_default));
                card.setStrokeWidth(Math.round(1 * density));
                card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.surface_card));
            }
        }

        // Tùy biến nút hành động phù hợp Dark Mode & Light Mode
        if (holder.btnViewContent != null) {
            holder.btnViewContent.setBackgroundColor(isNight ? Color.parseColor("#3B0764") : Color.parseColor("#EDE9FE"));
            holder.btnViewContent.setTextColor(isNight ? Color.parseColor("#C4B5FD") : Color.parseColor("#6D28D9"));
        }
        if (holder.btnOpenFile != null) {
            holder.btnOpenFile.setBackgroundColor(isNight ? Color.parseColor("#1E3A8A") : Color.parseColor("#EFF6FF"));
            holder.btnOpenFile.setTextColor(isNight ? Color.parseColor("#93C5FD") : Color.parseColor("#2563EB"));
        }
        if (holder.btnDownloadFile != null) {
            holder.btnDownloadFile.setBackgroundColor(isNight ? Color.parseColor("#064E3B") : Color.parseColor("#DCFCE7"));
            holder.btnDownloadFile.setTextColor(isNight ? Color.parseColor("#6EE7B7") : Color.parseColor("#047857"));
        }

        // Xử lý sự kiện nhấn vào thẻ
        View.OnClickListener clickListener = v -> {
            if (isMultiSelectMode) {
                if (selectedHistoryIds.contains(item.getId())) {
                    selectedHistoryIds.remove(item.getId());
                } else {
                    selectedHistoryIds.add(item.getId());
                }
                notifyItemChanged(position);
                if (listener != null) listener.onSelectionChanged(selectedHistoryIds.size());
            } else {
                if (listener != null) listener.onViewContentClick(item);
            }
        };
        holder.itemView.setOnClickListener(clickListener);

        // Nhấn giữ để kích hoạt chế độ chọn nhiều
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                setMultiSelectMode(true);
                selectedHistoryIds.add(item.getId());
                notifyItemChanged(position);
                if (listener != null) listener.onSelectionChanged(selectedHistoryIds.size());
                return true;
            }
            return false;
        });

        if (holder.btnViewContent != null) {
            holder.btnViewContent.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    clickListener.onClick(v);
                } else if (listener != null) {
                    listener.onViewContentClick(item);
                }
            });
        }

        // Bấm nút "👁️ Mở file" sẽ mở bằng ứng dụng đọc file bên ngoài
        if (holder.btnOpenFile != null) {
            holder.btnOpenFile.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    clickListener.onClick(v);
                } else if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        // Bấm nút "⬇️ Tải về máy" sẽ tải/lưu file trực tiếp về thư mục Download của điện thoại
        if (holder.btnDownloadFile != null) {
            holder.btnDownloadFile.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    clickListener.onClick(v);
                } else if (listener != null) {
                    listener.onDownloadClick(item);
                }
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
        return historyList != null ? historyList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvFileName, tvBadge, tvTime, btnViewContent, btnOpenFile, btnDownloadFile;
        ImageButton btnDeleteHistory;
        ImageView ivSelectHistory;
        View layoutHistoryActions;

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
            ivSelectHistory = itemView.findViewById(R.id.ivSelectHistory);
            layoutHistoryActions = itemView.findViewById(R.id.layoutHistoryActions);
        }
    }
}
