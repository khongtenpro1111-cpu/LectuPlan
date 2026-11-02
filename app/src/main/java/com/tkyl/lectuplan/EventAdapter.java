package com.tkyl.lectuplan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private static final int COLOR_FREE_STRIP = Color.parseColor("#22C55E");
    private static final int COLOR_FREE_BG = Color.parseColor("#F0FDF4");
    private static final int COLOR_FREE_STROKE = Color.parseColor("#86EFAC");
    private static final int COLOR_FREE_BADGE_BG = Color.parseColor("#DCFCE7");
    private static final int COLOR_FREE_TEXT = Color.parseColor("#15803D");
    private static final int COLOR_FREE_SUBTEXT = Color.parseColor("#166534");

    private static final int COLOR_MEETING_STRIP = Color.parseColor("#F59E0B");
    private static final int COLOR_MEETING_BADGE_BG = Color.parseColor("#FEF3C7");
    private static final int COLOR_MEETING_TEXT = Color.parseColor("#B45309");

    private static final int COLOR_WORK_STRIP = Color.parseColor("#059669");
    private static final int COLOR_WORK_BADGE_BG = Color.parseColor("#D1FAE5");
    private static final int COLOR_WORK_TEXT = Color.parseColor("#047857");

    private static final int COLOR_HANDWRITTEN_STRIP = Color.parseColor("#7C3AED");
    private static final int COLOR_HANDWRITTEN_BADGE_BG = Color.parseColor("#EDE9FE");
    private static final int COLOR_HANDWRITTEN_TEXT = Color.parseColor("#6D28D9");

    private static final int COLOR_CARD_WHITE = Color.parseColor("#FFFFFF");
    private static final int COLOR_CARD_STROKE = Color.parseColor("#CBD5E1");
    private static final int COLOR_DARK_TEXT = Color.parseColor("#0F172A");

    private List<EventModel> eventList;
    private OnEventClickListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<Integer> selectedEventIds = new HashSet<>();
    private final Map<String, String> formattedDateCache = new HashMap<>();

    public interface OnEventClickListener {
        void onDeleteClick(EventModel event);
        void onEditClick(EventModel event);
        void onSelectionChanged(int selectedCount);
    }

    public EventAdapter(List<EventModel> eventList, OnEventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (eventList == null || position < 0 || position >= eventList.size()) return position;
        EventModel item = eventList.get(position);
        return item.getId() != 0 ? item.getId() : -(position + 10000L);
    }

    public void updateList(List<EventModel> newList) {
        this.eventList = newList;
        notifyDataSetChanged();
    }

    public void setMultiSelectMode(boolean enable) {
        this.isMultiSelectMode = enable;
        if (!enable) {
            selectedEventIds.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void selectAll() {
        selectedEventIds.clear();
        for (EventModel e : eventList) {
            if (e.getId() > 0) {
                selectedEventIds.add(e.getId());
            }
        }
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedEventIds.size());
    }

    public void clearSelection() {
        selectedEventIds.clear();
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(0);
    }

    public List<Integer> getSelectedEventIds() {
        return new ArrayList<>(selectedEventIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_event_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventModel event = eventList.get(position);

        View viewLeftAccentStrip = holder.viewLeftAccentStrip;

        boolean isFreeSlot = event.getId() < 0 || (event.getOrganizer() != null && event.getOrganizer().contains("THỜI GIAN TRỐNG"));
        String titleLower = event.getTitle().toLowerCase(Locale.getDefault());
        boolean isMeetingOrExam = titleLower.contains("họp") || titleLower.contains("thi") || titleLower.contains("hội đồng") || titleLower.contains("chấm");

        if (isFreeSlot) {
            if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(COLOR_FREE_STRIP);
            if (holder.cardEventItem != null) {
                holder.cardEventItem.setCardBackgroundColor(COLOR_FREE_BG);
                holder.cardEventItem.setStrokeColor(COLOR_FREE_STROKE);
            }
            holder.tvSemesterBadge.setText("💡 Lịch trống");
            holder.tvSemesterBadge.setBackgroundColor(COLOR_FREE_BADGE_BG);
            holder.tvSemesterBadge.setTextColor(COLOR_FREE_TEXT);

            holder.tvTitle.setText(event.getTitle());
            holder.tvTitle.setTextColor(COLOR_FREE_TEXT);

            holder.tvDate.setText(formatFullDate(event.getDate(), event.getWeek()));
            if (holder.tvOrganizerLabel != null) {
                holder.tvOrganizerLabel.setText("Khung giờ:");
            }
            String orgText = event.getOrganizer() != null ? event.getOrganizer() : "Trống rảnh cả ngày";
            holder.tvOrganizer.setText(orgText);
            holder.tvOrganizer.setTextColor(COLOR_FREE_SUBTEXT);

            if (holder.cbSelect != null) holder.cbSelect.setVisibility(View.GONE);
            if (holder.btnEdit != null) holder.btnEdit.setVisibility(View.GONE);
            if (holder.btnDelete != null) holder.btnDelete.setVisibility(View.GONE);

        } else if (isMeetingOrExam) {
            if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(COLOR_MEETING_STRIP);
            if (holder.cardEventItem != null) {
                holder.cardEventItem.setCardBackgroundColor(COLOR_CARD_WHITE);
                holder.cardEventItem.setStrokeColor(COLOR_CARD_STROKE);
            }
            if (holder.tvOrganizerLabel != null) {
                holder.tvOrganizerLabel.setText("Tổ chức:");
            }
            String orgText = event.getOrganizer() != null ? event.getOrganizer() : "";
            String badgeSuffix = extractSourceBadgeSuffix(orgText);
            holder.tvSemesterBadge.setText("• Lịch họp / Hội đồng" + badgeSuffix);
            holder.tvSemesterBadge.setBackgroundColor(COLOR_MEETING_BADGE_BG);
            holder.tvSemesterBadge.setTextColor(COLOR_MEETING_TEXT);

            holder.tvTitle.setText(event.getTitle());
            holder.tvTitle.setTextColor(COLOR_DARK_TEXT);

            holder.tvDate.setText(formatFullDate(event.getDate(), event.getWeek()));
            holder.tvOrganizer.setText(orgText);
            holder.tvOrganizer.setTextColor(COLOR_DARK_TEXT);
            if (holder.tvOrganizerLabel != null) {
                holder.tvOrganizerLabel.setText("Tổ chức:");
            }

            bindInteractiveButtons(holder, event, position);

        } else {
            if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(COLOR_WORK_STRIP);
            if (holder.cardEventItem != null) {
                holder.cardEventItem.setCardBackgroundColor(COLOR_CARD_WHITE);
                holder.cardEventItem.setStrokeColor(COLOR_CARD_STROKE);
            }
            String orgText = event.getOrganizer() != null ? event.getOrganizer() : "";
            String badgeSuffix = extractSourceBadgeSuffix(orgText);
            if (orgText.contains("Viết tay") || orgText.contains("Ghi tay")) {
                if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(COLOR_HANDWRITTEN_STRIP);
                holder.tvSemesterBadge.setText("✍️ Tài liệu ghi tay" + badgeSuffix);
                holder.tvSemesterBadge.setBackgroundColor(COLOR_HANDWRITTEN_BADGE_BG);
                holder.tvSemesterBadge.setTextColor(COLOR_HANDWRITTEN_TEXT);
            } else {
                holder.tvSemesterBadge.setText("• Lịch dạy / Công việc" + badgeSuffix);
                holder.tvSemesterBadge.setBackgroundColor(COLOR_WORK_BADGE_BG);
                holder.tvSemesterBadge.setTextColor(COLOR_WORK_TEXT);
            }

            holder.tvTitle.setText(event.getTitle());
            holder.tvTitle.setTextColor(COLOR_DARK_TEXT);

            holder.tvDate.setText(formatFullDate(event.getDate(), event.getWeek()));
            holder.tvOrganizer.setText(orgText);
            holder.tvOrganizer.setTextColor(COLOR_DARK_TEXT);
            if (holder.tvOrganizerLabel != null) {
                holder.tvOrganizerLabel.setText("Tổ chức:");
            }

            bindInteractiveButtons(holder, event, position);
        }
    }

    private String extractSourceBadgeSuffix(String organizer) {
        if (organizer == null) return "";
        String lower = organizer.toLowerCase(Locale.getDefault());
        if (lower.contains("excel") || lower.contains(".xlsx") || lower.contains(".xls")) {
            return " • 📊 Excel";
        } else if (lower.contains("pdf")) {
            return " • 📄 PDF";
        } else if (lower.contains("powerpoint") || lower.contains("pptx") || lower.contains("slide")) {
            return " • 📽️ PPTX";
        } else if (lower.contains("word") || lower.contains("docx")) {
            return " • 📝 Word";
        } else if (lower.contains("ocr") || lower.contains("ảnh")) {
            return " • 🖼️ Ảnh OCR";
        } else if (lower.contains("txt") || lower.contains("csv")) {
            return " • 📃 Text/CSV";
        }
        return "";
    }

    private void bindInteractiveButtons(ViewHolder holder, EventModel event, int position) {
        if (holder.cbSelect != null) {
            holder.cbSelect.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
            holder.cbSelect.setChecked(selectedEventIds.contains(event.getId()));
        }

        if (holder.btnEdit != null) {
            holder.btnEdit.setVisibility(isMultiSelectMode ? View.GONE : View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(event);
            });
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setVisibility(isMultiSelectMode ? View.GONE : View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(event);
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                if (selectedEventIds.contains(event.getId())) {
                    selectedEventIds.remove(event.getId());
                } else {
                    selectedEventIds.add(event.getId());
                }
                notifyItemChanged(position);
                if (listener != null) listener.onSelectionChanged(selectedEventIds.size());
            } else {
                if (listener != null) listener.onEditClick(event);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode) {
                setMultiSelectMode(true);
                selectedEventIds.add(event.getId());
                if (listener != null) listener.onSelectionChanged(selectedEventIds.size());
                return true;
            }
            return false;
        });
    }

    private String formatFullDate(String dateStr, int fallbackWeek) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        String key = dateStr.trim();
        String cached = formattedDateCache.get(key);
        if (cached != null) return cached;
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfInput.parse(key);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                String dayOfWeek = "";
                int day = cal.get(Calendar.DAY_OF_WEEK);
                switch (day) {
                    case Calendar.MONDAY: dayOfWeek = "Thứ Hai"; break;
                    case Calendar.TUESDAY: dayOfWeek = "Thứ Ba"; break;
                    case Calendar.WEDNESDAY: dayOfWeek = "Thứ Tư"; break;
                    case Calendar.THURSDAY: dayOfWeek = "Thứ Năm"; break;
                    case Calendar.FRIDAY: dayOfWeek = "Thứ Sáu"; break;
                    case Calendar.SATURDAY: dayOfWeek = "Thứ Bảy"; break;
                    case Calendar.SUNDAY: dayOfWeek = "Chủ Nhật"; break;
                }

                int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String formattedDate = sdfOutput.format(date);

                String result = dayOfWeek + ", " + formattedDate + " (Tuần " + weekOfYear + ")";
                formattedDateCache.put(key, result);
                return result;
            }
        } catch (Exception ignored) {
        }
        String fallback = key + " (Tuần " + fallbackWeek + ")";
        formattedDateCache.put(key, fallback);
        return fallback;
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardEventItem;
        View viewLeftAccentStrip;
        TextView tvStt, tvTitle, tvSemesterBadge, tvDate, tvOrganizer, tvOrganizerLabel;
        ImageButton btnDelete, btnEdit;
        CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEventItem = itemView.findViewById(R.id.cardEventItem);
            viewLeftAccentStrip = itemView.findViewById(R.id.viewLeftAccentStrip);
            tvStt = itemView.findViewById(R.id.tvStt);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSemesterBadge = itemView.findViewById(R.id.tvSemesterBadge);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvOrganizerLabel = itemView.findViewById(R.id.tvOrganizerLabel);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}
