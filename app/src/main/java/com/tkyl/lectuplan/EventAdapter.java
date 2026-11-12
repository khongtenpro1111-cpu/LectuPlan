package com.tkyl.lectuplan;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Set<Integer> conflictedEventIds = new HashSet<>();
    private final Map<String, String> formattedDateCache = new HashMap<>();

    public interface OnEventClickListener {
        void onDeleteClick(EventModel event);
        void onEditClick(EventModel event);
        void onSelectionChanged(int selectedCount);
    }

    public EventAdapter(List<EventModel> eventList, OnEventClickListener listener) {
        if (eventList != null) {
            EventModel.sortChronologically(eventList);
            markConflicts(eventList);
        }
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
        if (newList != null) {
            EventModel.sortChronologically(newList);
            markConflicts(newList);
        }
        this.eventList = newList;
        notifyDataSetChanged();
    }

    public List<EventModel> getEventList() {
        return eventList;
    }
    
    private void markConflicts(List<EventModel> list) {
        conflictedEventIds.clear();
        if (list == null) return;
        
        for (int i = 0; i < list.size(); i++) {
            EventModel e1 = list.get(i);
            if (e1.getId() <= 0) continue; 
            for (int j = i + 1; j < list.size(); j++) {
                EventModel e2 = list.get(j);
                if (e2.getId() <= 0) continue;
                if (e1.getDate() == null || e2.getDate() == null) continue;
                if (!e1.getDate().equals(e2.getDate())) break; 
                
                if (e1.getStartMin() < e2.getEndMin() && e1.getEndMin() > e2.getStartMin()) {
                    conflictedEventIds.add(e1.getId());
                    conflictedEventIds.add(e2.getId());
                }
            }
        }
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

        // Kiểm tra xem lịch này có cùng ngày với lịch liền trước không (Nếu cùng ngày thì chỉ cần 1 badge thời gian phía trên)
        boolean isFirstOfDay = true;
        if (position > 0 && eventList != null) {
            EventModel prevEvent = eventList.get(position - 1);
            if (prevEvent != null && prevEvent.getDate() != null && event.getDate() != null) {
                if (prevEvent.getDate().trim().equals(event.getDate().trim())) {
                    isFirstOfDay = false;
                }
            }
        }

        // Thiết lập Badge Ngày chi tiết: Chỉ hiển thị 1 lần duy nhất trên đầu nhóm các lịch cùng ngày
        String fullDateText = formatFullDate(event.getDate(), event.getWeek());
        if (holder.layoutDateHeaderBadge != null && holder.tvDateHeaderBadge != null) {
            if (isFirstOfDay && fullDateText != null && !fullDateText.trim().isEmpty()) {
                holder.layoutDateHeaderBadge.setVisibility(View.VISIBLE);
                holder.tvDateHeaderBadge.setText(fullDateText);
            } else {
                holder.layoutDateHeaderBadge.setVisibility(View.GONE);
            }
        }
        if (holder.layoutDateRow != null) {
            // Ẩn dòng ngày trùng lặp bên trong thẻ vì đã có badge ngày chi tiết ở ngay trên đầu thẻ
            holder.layoutDateRow.setVisibility(View.GONE);
        }

        View viewLeftAccentStrip = holder.viewLeftAccentStrip;

        boolean isFreeSlot = event.getId() < 0 || (event.getOrganizer() != null && event.getOrganizer().contains("THỜI GIAN TRỐNG"));
        String titleLower = event.getTitle().toLowerCase(Locale.getDefault());
        boolean isMeetingOrExam = titleLower.contains("họp") || titleLower.contains("thi") || titleLower.contains("hội đồng") || titleLower.contains("chấm");

        boolean isNight = (holder.itemView.getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int cardBgColor = isNight ? Color.parseColor("#1E293B") : Color.parseColor("#FFFFFF");
        int cardStrokeColor = isNight ? Color.parseColor("#334155") : Color.parseColor("#CBD5E1");
        int titleTextColor = isNight ? Color.parseColor("#FFFFFF") : Color.parseColor("#0F172A");
        int descTextColor = isNight ? Color.parseColor("#E2E8F0") : Color.parseColor("#64748B");

        EventFieldBreakdown bd = parseEventDetails(event);

        if (isFreeSlot) {
            if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(COLOR_FREE_STRIP);
            if (holder.cardEventItem != null) {
                holder.cardEventItem.setCardBackgroundColor(isNight ? Color.parseColor("#064E3B") : COLOR_FREE_BG);
                holder.cardEventItem.setStrokeColor(COLOR_FREE_STROKE);
            }
            if (holder.tvTimeBadge != null) {
                holder.tvTimeBadge.setVisibility(View.GONE);
            }
            holder.tvSemesterBadge.setText("💡 Lịch trống");
            holder.tvSemesterBadge.setBackgroundColor(isNight ? Color.parseColor("#065F46") : COLOR_FREE_BADGE_BG);
            holder.tvSemesterBadge.setTextColor(isNight ? Color.parseColor("#6EE7B7") : COLOR_FREE_TEXT);

            if (holder.layoutRowTitle != null) {
                holder.layoutRowTitle.setVisibility(View.VISIBLE);
                if (holder.tvTitleLabel != null) {
                    holder.tvTitleLabel.setText("Trạng thái:");
                    holder.tvTitleLabel.setTextColor(isNight ? Color.parseColor("#6EE7B7") : COLOR_FREE_TEXT);
                }
                if (holder.tvTitle != null) {
                    holder.tvTitle.setText(bd.title);
                    holder.tvTitle.setTextColor(isNight ? Color.parseColor("#A7F3D0") : COLOR_FREE_TEXT);
                }
            }

            if (holder.layoutRowPeriod != null) holder.layoutRowPeriod.setVisibility(View.GONE);
            if (holder.layoutRowTime != null) holder.layoutRowTime.setVisibility(View.GONE);
            if (holder.layoutRowGroup != null) holder.layoutRowGroup.setVisibility(View.GONE);
            if (holder.layoutRowRoom != null) holder.layoutRowRoom.setVisibility(View.GONE);

            if (holder.layoutRowOrganizer != null) {
                holder.layoutRowOrganizer.setVisibility(View.VISIBLE);
                if (holder.tvOrganizerLabel != null) {
                    holder.tvOrganizerLabel.setText("Chi tiết:");
                    holder.tvOrganizerLabel.setTextColor(isNight ? Color.parseColor("#6EE7B7") : COLOR_FREE_TEXT);
                }
                if (holder.tvOrganizer != null) {
                    holder.tvOrganizer.setText(bd.organizer);
                    holder.tvOrganizer.setTextColor(isNight ? Color.parseColor("#D1FAE5") : COLOR_FREE_SUBTEXT);
                }
            }

            if (holder.cbSelect != null) holder.cbSelect.setVisibility(View.GONE);
            if (holder.btnEdit != null) holder.btnEdit.setVisibility(View.GONE);
            if (holder.btnDelete != null) holder.btnDelete.setVisibility(View.GONE);

            if (holder.tvConflictBadge != null) holder.tvConflictBadge.setVisibility(View.GONE);
            if (holder.layoutConflictDetails != null) holder.layoutConflictDetails.setVisibility(View.GONE);

        } else {
            if (holder.cardEventItem != null) {
                holder.cardEventItem.setCardBackgroundColor(cardBgColor);
                holder.cardEventItem.setStrokeColor(cardStrokeColor);
            }

            if (holder.tvTimeBadge != null) {
                holder.tvTimeBadge.setVisibility(View.GONE);
            }

            String orgText = bd.organizer;
            String badgeSuffix = extractSourceBadgeSuffix(orgText);
            SchoolProfile.SchoolType school = SchoolProfile.detectSchool(event.getTitle() + " " + orgText);
            String category = event.getScheduleCategory();
            boolean isTeaching = EventModel.CAT_TEACHING.equals(category);

            // Phân loại chuẩn 4 nhóm: Lịch dạy/học, Lịch sự kiện, Lịch phát sinh, Lịch khác(tự đặt tên)
            if (EventModel.CAT_TEACHING.equals(category)) {
                String schoolTag = (school != SchoolProfile.SchoolType.DEFAULT) ? ("[" + school.getCode() + "] ") : "";
                int blueColor = isNight ? Color.parseColor("#60A5FA") : Color.parseColor("#1D4ED8");
                if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(blueColor);
                holder.tvSemesterBadge.setText("👨‍🏫 " + schoolTag + EventModel.CAT_TEACHING + badgeSuffix);
                holder.tvSemesterBadge.setBackgroundColor(isNight ? Color.parseColor("#172554") : Color.parseColor("#DBEAFE"));
                holder.tvSemesterBadge.setTextColor(blueColor);

            } else if (EventModel.CAT_EVENT.equals(category)) {
                int greenColor = isNight ? Color.parseColor("#34D399") : Color.parseColor("#059669");
                if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(greenColor);
                holder.tvSemesterBadge.setText("🏛️ " + EventModel.CAT_EVENT + badgeSuffix);
                holder.tvSemesterBadge.setBackgroundColor(isNight ? Color.parseColor("#064E3B") : Color.parseColor("#D1FAE5"));
                holder.tvSemesterBadge.setTextColor(greenColor);

            } else if (EventModel.CAT_INCIDENT.equals(category)) {
                int amberColor = isNight ? Color.parseColor("#FBBF24") : Color.parseColor("#D97706");
                if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(amberColor);
                holder.tvSemesterBadge.setText("⚡ " + EventModel.CAT_INCIDENT + badgeSuffix);
                holder.tvSemesterBadge.setBackgroundColor(isNight ? Color.parseColor("#451A03") : Color.parseColor("#FEF3C7"));
                holder.tvSemesterBadge.setTextColor(amberColor);

            } else {
                // Nhóm Lịch khác hoặc tự đặt tên tùy chỉnh
                int purpleColor = isNight ? Color.parseColor("#C084FC") : Color.parseColor("#9333EA");
                if (viewLeftAccentStrip != null) viewLeftAccentStrip.setBackgroundColor(purpleColor);
                String displayCat = (category != null && !category.trim().isEmpty() && !EventModel.LEGACY_CAT_MANUAL_OTHER.equals(category) && !EventModel.CAT_MANUAL_OTHER.equals(category))
                        ? category.trim()
                        : "Lịch khác";
                holder.tvSemesterBadge.setText("📝 " + displayCat + badgeSuffix);
                holder.tvSemesterBadge.setBackgroundColor(isNight ? Color.parseColor("#3B0764") : Color.parseColor("#F3E8FF"));
                holder.tvSemesterBadge.setTextColor(purpleColor);
            }

            // Dòng 2: Tiêu đề
            if (holder.layoutRowTitle != null) {
                holder.layoutRowTitle.setVisibility(View.VISIBLE);
                if (holder.tvTitleLabel != null) {
                    holder.tvTitleLabel.setText("Tiêu đề:");
                    holder.tvTitleLabel.setTextColor(descTextColor);
                }
                if (holder.tvTitle != null) {
                    holder.tvTitle.setText(bd.title);
                    holder.tvTitle.setTextColor(titleTextColor);
                }
            }

            // Dòng 3: Tiết - CHỈ HIỂN THỊ NẾU LÀ SỰ KIỆN GIẢNG DẠY HOẶC PHÁT SINH CÓ TIẾT
            if (holder.layoutRowPeriod != null) {
                boolean canHavePeriod = EventModel.CAT_TEACHING.equals(category) || 
                        (EventModel.CAT_INCIDENT.equals(category) && bd.period != null && !bd.period.isEmpty());
                if (canHavePeriod && bd.period != null && !bd.period.isEmpty()) {
                    holder.layoutRowPeriod.setVisibility(View.VISIBLE);
                    if (holder.tvPeriodLabel != null) {
                        holder.tvPeriodLabel.setText("Tiết:");
                        holder.tvPeriodLabel.setTextColor(descTextColor);
                    }
                    if (holder.tvPeriod != null) {
                        holder.tvPeriod.setText(bd.period);
                        holder.tvPeriod.setTextColor(titleTextColor);
                    }
                } else {
                    holder.layoutRowPeriod.setVisibility(View.GONE);
                }
            }

            // Dòng 4: Thời gian
            if (holder.layoutRowTime != null) {
                if (bd.time != null && !bd.time.isEmpty()) {
                    holder.layoutRowTime.setVisibility(View.VISIBLE);
                    if (holder.tvTimeLabel != null) {
                        holder.tvTimeLabel.setText("Thời gian:");
                        holder.tvTimeLabel.setTextColor(descTextColor);
                    }
                    if (holder.tvTime != null) {
                        holder.tvTime.setText(bd.time);
                        holder.tvTime.setTextColor(isNight ? Color.parseColor("#38BDF8") : Color.parseColor("#2563EB"));
                    }
                } else {
                    holder.layoutRowTime.setVisibility(View.GONE);
                }
            }

            // Dòng 5: Nhóm
            if (holder.layoutRowGroup != null) {
                if (bd.group != null && !bd.group.isEmpty()) {
                    holder.layoutRowGroup.setVisibility(View.VISIBLE);
                    if (holder.tvGroupLabel != null) {
                        holder.tvGroupLabel.setText("Nhóm:");
                        holder.tvGroupLabel.setTextColor(descTextColor);
                    }
                    if (holder.tvGroup != null) {
                        holder.tvGroup.setText(bd.group);
                        holder.tvGroup.setTextColor(titleTextColor);
                    }
                } else {
                    holder.layoutRowGroup.setVisibility(View.GONE);
                }
            }

            // Dòng 6: Phòng / Địa điểm
            if (holder.layoutRowRoom != null) {
                if (bd.room != null && !bd.room.trim().isEmpty()) {
                    holder.layoutRowRoom.setVisibility(View.VISIBLE);
                    String cleanRoom = bd.room.replaceAll("(?i)^(?:phòng|room|địa\\s*điểm)\\s*[:=–-]?\\s*", "").trim();
                    if (cleanRoom.isEmpty()) cleanRoom = bd.room.trim();

                    if (holder.tvRoomLabel != null) {
                        boolean isClassroom = EventModel.CAT_TEACHING.equals(category)
                                || cleanRoom.matches("(?i).*[ERABCUL]\\d?[-–.0-9/]+.*");
                        holder.tvRoomLabel.setText(isClassroom ? "Phòng:" : "Địa điểm:");
                        holder.tvRoomLabel.setTextColor(descTextColor);
                    }
                    if (holder.tvRoom != null) {
                        holder.tvRoom.setText(cleanRoom);
                        holder.tvRoom.setTextColor(titleTextColor);
                    }
                } else {
                    holder.layoutRowRoom.setVisibility(View.GONE);
                }
            }

            // Dòng 7: Tổ chức / Cơ sở
            if (holder.layoutRowOrganizer != null) {
                if (bd.organizer != null && !bd.organizer.isEmpty()) {
                    holder.layoutRowOrganizer.setVisibility(View.VISIBLE);
                    if (holder.tvOrganizerLabel != null) {
                        holder.tvOrganizerLabel.setText("Tổ chức:");
                        holder.tvOrganizerLabel.setTextColor(descTextColor);
                    }
                    if (holder.tvOrganizer != null) {
                        holder.tvOrganizer.setText(bd.organizer);
                        holder.tvOrganizer.setTextColor(titleTextColor);
                    }
                } else {
                    holder.layoutRowOrganizer.setVisibility(View.GONE);
                }
            }

            // Dòng 8: Dòng hiển thị cảnh báo trùng lịch trực tiếp
            if (holder.layoutConflictDetails != null) {
                boolean hasConflict = conflictedEventIds.contains(event.getId());
                if (hasConflict) {
                    List<String> conflicts = new ArrayList<>();
                    for (EventModel other : eventList) {
                        if (other.getId() != event.getId() && other.getId() > 0 
                            && event.getDate() != null && event.getDate().equals(other.getDate())) {
                            if (event.getStartMin() < other.getEndMin() && event.getEndMin() > other.getStartMin()) {
                                String cleanOtherTitle = EventModel.cleanDisplayTitle(other.getTitle());
                                conflicts.add(cleanOtherTitle);
                            }
                        }
                    }
                    if (!conflicts.isEmpty()) {
                        holder.layoutConflictDetails.setVisibility(View.VISIBLE);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < conflicts.size(); i++) {
                            sb.append(conflicts.get(i));
                            if (i < conflicts.size() - 1) sb.append(", ");
                        }
                        if (holder.tvConflictDetails != null) {
                            holder.tvConflictDetails.setText("Trùng thời gian với: " + sb.toString());
                        }
                    } else {
                        holder.layoutConflictDetails.setVisibility(View.GONE);
                    }
                } else {
                    holder.layoutConflictDetails.setVisibility(View.GONE);
                }
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
        if (holder.tvConflictBadge != null) {
            boolean hasConflict = conflictedEventIds.contains(event.getId());
            holder.tvConflictBadge.setVisibility(hasConflict ? View.VISIBLE : View.GONE);
        }
        
        if (holder.cbSelect != null) {
            holder.cbSelect.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
            holder.cbSelect.setChecked(selectedEventIds.contains(event.getId()));
        }

        if (holder.btnEdit != null) {
            boolean isNight = (holder.itemView.getContext().getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            holder.btnEdit.setColorFilter(isNight ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
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

        View.OnClickListener clickListener = v -> {
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
        };

        View.OnLongClickListener longClickListener = v -> {
            if (!isMultiSelectMode) {
                setMultiSelectMode(true);
                selectedEventIds.add(event.getId());
                if (listener != null) listener.onSelectionChanged(selectedEventIds.size());
                return true;
            }
            return false;
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.itemView.setOnLongClickListener(longClickListener);
        if (holder.cardEventItem != null) {
            holder.cardEventItem.setOnClickListener(clickListener);
            holder.cardEventItem.setOnLongClickListener(longClickListener);
        }
    }

    private String formatFullDate(String dateStr, int fallbackWeek) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        String key = dateStr.trim() + "_w" + fallbackWeek;
        String cached = formattedDateCache.get(key);
        if (cached != null) return cached;
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdfInput.parse(dateStr.trim());
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

                int week = fallbackWeek > 0 ? fallbackWeek : cal.get(Calendar.WEEK_OF_YEAR);
                SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String formattedDate = sdfOutput.format(date);

                String result = dayOfWeek + ", " + formattedDate + " (Tuần " + week + ")";
                formattedDateCache.put(key, result);
                return result;
            }
        } catch (Exception ignored) {
        }
        String fallback = dateStr + " (Tuần " + fallbackWeek + ")";
        formattedDateCache.put(key, fallback);
        return fallback;
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventFieldBreakdown {
        public String title = "";
        public String period = "";
        public String time = "";
        public String group = "";
        public String room = "";
        public String organizer = "";
    }

    public static EventFieldBreakdown parseEventDetails(EventModel event) {
        EventFieldBreakdown bd = new EventFieldBreakdown();
        if (event == null) return bd;
        EventModel.EventFieldBreakdown modelBd = EventModel.parseEventDetails(event);
        bd.title = modelBd.title;
        bd.period = modelBd.period;
        bd.time = modelBd.time;
        bd.group = modelBd.group;
        bd.room = modelBd.room;
        bd.organizer = modelBd.organizer;
        return bd;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutDateHeaderBadge;
        TextView tvDateHeaderBadge;
        View viewTimelineBranch;
        MaterialCardView cardEventItem;
        View viewLeftAccentStrip;
        TextView tvSemesterBadge, tvTimeBadge, tvConflictBadge;
        ImageButton btnDelete, btnEdit;
        CheckBox cbSelect;

        LinearLayout layoutRowTitle, layoutRowPeriod, layoutRowTime, layoutRowGroup, layoutRowRoom, layoutRowOrganizer, layoutConflictDetails;
        TextView tvTitleLabel, tvTitle;
        TextView tvPeriodLabel, tvPeriod;
        TextView tvTimeLabel, tvTime;
        TextView tvGroupLabel, tvGroup;
        TextView tvRoomLabel, tvRoom;
        TextView tvOrganizerLabel, tvOrganizer, tvConflictDetails;

        LinearLayout layoutDateRow;
        TextView tvDate, tvDateLabel, tvStt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutDateHeaderBadge = itemView.findViewById(R.id.layoutDateHeaderBadge);
            tvDateHeaderBadge = itemView.findViewById(R.id.tvDateHeaderBadge);
            viewTimelineBranch = itemView.findViewById(R.id.viewTimelineBranch);
            cardEventItem = itemView.findViewById(R.id.cardEventItem);
            viewLeftAccentStrip = itemView.findViewById(R.id.viewLeftAccentStrip);
            tvSemesterBadge = itemView.findViewById(R.id.tvSemesterBadge);
            tvTimeBadge = itemView.findViewById(R.id.tvTimeBadge);
            tvConflictBadge = itemView.findViewById(R.id.tvConflictBadge);
            
            layoutRowTitle = itemView.findViewById(R.id.layoutRowTitle);
            tvTitleLabel = itemView.findViewById(R.id.tvTitleLabel);
            tvTitle = itemView.findViewById(R.id.tvTitle);

            layoutRowPeriod = itemView.findViewById(R.id.layoutRowPeriod);
            tvPeriodLabel = itemView.findViewById(R.id.tvPeriodLabel);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);

            layoutRowTime = itemView.findViewById(R.id.layoutRowTime);
            tvTimeLabel = itemView.findViewById(R.id.tvTimeLabel);
            tvTime = itemView.findViewById(R.id.tvTime);

            layoutRowGroup = itemView.findViewById(R.id.layoutRowGroup);
            tvGroupLabel = itemView.findViewById(R.id.tvGroupLabel);
            tvGroup = itemView.findViewById(R.id.tvGroup);

            layoutRowRoom = itemView.findViewById(R.id.layoutRowRoom);
            tvRoomLabel = itemView.findViewById(R.id.tvRoomLabel);
            tvRoom = itemView.findViewById(R.id.tvRoom);

            layoutRowOrganizer = itemView.findViewById(R.id.layoutRowOrganizer);
            tvOrganizerLabel = itemView.findViewById(R.id.tvOrganizerLabel);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);

            layoutConflictDetails = itemView.findViewById(R.id.layoutConflictDetails);
            tvConflictDetails = itemView.findViewById(R.id.tvConflictDetails);

            layoutDateRow = itemView.findViewById(R.id.layoutDateRow);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDateLabel = itemView.findViewById(R.id.tvDateLabel);
            tvStt = itemView.findViewById(R.id.tvStt);

            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}
