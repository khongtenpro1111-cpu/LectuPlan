package com.tkyl.lectuplan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Thuật toán "Lát cắt Thời gian & Đệm di chuyển" (Time-Slicing & Travel Buffer Engine)
 * Phân tích và tạo ra "Dòng chảy thời gian liên tục" (Continuous Timeline Stream)
 */
public class GranularTimelineEngine {

    public static class DayTimelineSummary {
        public String dateIso;
        public String dayHeaderTitle;        // 📅 THỨ NĂM, 14/05/2026
        public String statsSummary;          // Tổng: 2 ca dạy - 1 sự kiện | Trống: 4 giờ 40 phút
        public int totalBusyCount;
        public int totalFreeMinutes;
        public List<TimelineSlot> timelineSlots;
    }

    public static DayTimelineSummary generateDayTimeline(String dateIso, List<EventModel> dayEvents) {
        DayTimelineSummary summary = new DayTimelineSummary();
        summary.dateIso = dateIso;
        summary.timelineSlots = new ArrayList<>();

        // 1. Tạo tiêu đề ngày tiếng Việt chuẩn: THỨ NĂM, 14/05/2026
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        try {
            Date d = sdf.parse(dateIso);
            if (d != null) {
                summary.dayHeaderTitle = "📅 " + dayFmt.format(d).toUpperCase(Locale.getDefault());
            } else {
                summary.dayHeaderTitle = "📅 " + dateIso;
            }
        } catch (Exception ignored) {
            summary.dayHeaderTitle = "📅 " + dateIso;
        }

        // 2. Chuyển đổi toàn bộ EventModel sang TimelineSlot loại BUSY_EVENT
        List<TimelineSlot> busySlots = new ArrayList<>();
        List<CampusBufferHelper.CampusInfo> campusInfos = new ArrayList<>();

        int teachingCount = 0;
        int incidentCount = 0;

        if (dayEvents != null) {
            for (EventModel ev : dayEvents) {
                if (ev == null || ev.getId() <= 0) continue;

                SchoolProfile.SchoolType school = SchoolProfile.detectSchool(ev.getTitle() + " " + ev.getOrganizer());
                CampusBufferHelper.CampusInfo campus = CampusBufferHelper.detectCampus(ev.getTitle() + " " + ev.getOrganizer(), school.getCode());
                TimeNormalizer.NormalizedRange range = TimeNormalizer.parseFromText(ev.getTitle() + " " + ev.getOrganizer(), school);

                // Phân loại Nguồn lịch & Độ ưu tiên (Multi-Source & Priority Matrix)
                TimelineSlot.PriorityType priorityType = classifyPriority(ev);
                String schoolPrefix = "[" + school.getCode() + "] ";

                String title = schoolPrefix + cleanTitleForStream(ev.getTitle());
                String subtitle = buildSubtitle(campus, range, ev);
                String badgeText = priorityType.getLabel();
                String colorHex = determineSlotColor(school, priorityType);
                String bgHex = priorityType.getBgColor();

                TimelineSlot busySlot = new TimelineSlot(
                        TimelineSlot.SlotType.BUSY_EVENT,
                        range.startMin,
                        range.endMin,
                        title,
                        subtitle,
                        "Tổ chức: " + (ev.getOrganizer() != null ? ev.getOrganizer() : school.getDisplayName()),
                        badgeText,
                        colorHex,
                        bgHex
                );
                busySlot.setPriorityType(priorityType);
                busySlot.setSourceEvent(ev);

                busySlots.add(busySlot);
                campusInfos.add(campus);

                if (priorityType == TimelineSlot.PriorityType.FIXED_TEACHING) {
                    teachingCount++;
                } else {
                    incidentCount++;
                }
            }
        }

        // Sắp xếp các ca bận theo giờ bắt đầu
        Collections.sort(busySlots, (a, b) -> Integer.compare(a.getStartMin(), b.getStartMin()));

        // Trường hợp cả ngày không có lịch nào bận:
        if (busySlots.isEmpty()) {
            int dayDuration = TimeNormalizer.DAY_END_MINUTE - TimeNormalizer.DAY_START_MINUTE;
            TimelineSlot freeAllDay = new TimelineSlot(
                    TimelineSlot.SlotType.DEEP_FREE,
                    TimeNormalizer.DAY_START_MINUTE,
                    TimeNormalizer.DAY_END_MINUTE,
                    "🟢 TRỐNG TOÀN BỘ NGÀY (Cả ngày rảnh rỗi)",
                    "Khung giờ hoạt động: 06:45 - 20:15 (" + formatMinutesToHourString(dayDuration) + ")",
                    "Gợi ý: Sẵn sàng nhận toàn bộ ca dạy thỉnh giảng, chấm khóa luận hoặc làm việc chuyên sâu.",
                    "🟢 Rảnh cả ngày",
                    "#16A34A",
                    "#F0FDF4"
            );
            summary.timelineSlots.add(freeAllDay);
            summary.totalBusyCount = 0;
            summary.totalFreeMinutes = dayDuration;
            summary.statsSummary = "Hôm nay trống hoàn toàn cả ngày (" + formatMinutesToHourString(dayDuration) + ")";
            return summary;
        }

        // 3. Thực hiện thuật toán Lát cắt Thời gian & Đệm di chuyển (Granular Time-Slicing & Travel Buffer)
        int pointer = TimeNormalizer.DAY_START_MINUTE; // 06:45
        int totalFreeMin = 0;

        for (int i = 0; i < busySlots.size(); i++) {
            TimelineSlot currBusy = busySlots.get(i);
            CampusBufferHelper.CampusInfo currCampus = campusInfos.get(i);

            // Kiểm tra xung đột trùng giờ với ca trước (Conflict / Overlap Detection)
            if (i > 0 && currBusy.getStartMin() < pointer) {
                TimelineSlot prevBusy = busySlots.get(i - 1);
                currBusy.setBadgeText("🚨 XUNG ĐỘT TRÙNG GIỜ");
                currBusy.setColorHex("#DC2626");
                currBusy.setBgHex("#FEF2F2");
                currBusy.setSuggestion("⚠️ CẢNH BÁO XUNG ĐỘT: Bị trùng giờ với sự kiện trước [" + prevBusy.getTitle()
                        + " (" + TimeNormalizer.minuteToTimeString(prevBusy.getStartMin()) + " - " + TimeNormalizer.minuteToTimeString(prevBusy.getEndMin()) + ")]! Giảng viên cần xin đổi ca hoặc dời lịch.");
            }

            // A. Khoảng trống phía trước ca này
            if (currBusy.getStartMin() > pointer) {
                int gap = currBusy.getStartMin() - pointer;

                // Nếu là khoảng trống đầu ngày (trước ca đầu tiên)
                if (i == 0) {
                    TimelineSlot startSlot = new TimelineSlot(
                            TimelineSlot.SlotType.START_DAY_FREE,
                            pointer,
                            currBusy.getStartMin(),
                            "🟢 TRỐNG ĐẦU NGÀY (" + formatDuration(gap) + ")",
                            "[" + TimeNormalizer.minuteToTimeString(pointer) + " - " + TimeNormalizer.minuteToTimeString(currBusy.getStartMin()) + "]",
                            "Gợi ý: Ăn sáng, chuẩn bị tài liệu giảng dạy, di chuyển sớm",
                            "🟢 Rảnh đầu ngày",
                            "#16A34A",
                            "#F0FDF4"
                    );
                    summary.timelineSlots.add(startSlot);
                    totalFreeMin += gap;
                } else {
                    // Khoảng giữa 2 ca: Kiểm tra cơ sở giảng dạy (Campus Buffer)
                    CampusBufferHelper.CampusInfo prevCampus = campusInfos.get(i - 1);
                    boolean isSame = CampusBufferHelper.isSameCampus(prevCampus, currCampus);

                    if (!isSame && gap >= 40) {
                        // Khác cơ sở / khác trường ➔ Cần đệm di chuyển 45-60 phút
                        int travelMin = Math.min(gap, CampusBufferHelper.DEFAULT_TRAVEL_BUFFER_MINUTES);
                        int freeBeforeTravel = gap - travelMin;

                        if (freeBeforeTravel >= 30) {
                            TimelineSlot preFree = new TimelineSlot(
                                    TimelineSlot.SlotType.MICRO_FREE,
                                    pointer,
                                    currBusy.getStartMin() - travelMin,
                                    "🟡 NGHỈ NGẮN TRƯỚC GIỜ DI CHUYỂN (" + formatDuration(freeBeforeTravel) + ")",
                                    "[" + TimeNormalizer.minuteToTimeString(pointer) + " - " + TimeNormalizer.minuteToTimeString(currBusy.getStartMin() - travelMin) + "]",
                                    "Gợi ý: Nghỉ ngơi tại chỗ, chuẩn bị phương tiện",
                                    "🟡 Nghỉ ngắn",
                                    "#CA8A04",
                                    "#FEFCE8"
                            );
                            summary.timelineSlots.add(preFree);
                            totalFreeMin += freeBeforeTravel;
                        }

                        // Đệm di chuyển khóa lại, tuyệt đối không tính là giờ rảnh
                        TimelineSlot travelSlot = new TimelineSlot(
                                TimelineSlot.SlotType.TRAVEL_WARNING,
                                currBusy.getStartMin() - travelMin,
                                currBusy.getStartMin(),
                                "🟠 ĐỆM DI CHUYỂN QUA CƠ SỞ KHÁC (" + travelMin + " phút: " + prevCampus.shortName + " ➔ " + currCampus.shortName + ")",
                                "📍 Lộ trình: " + prevCampus.shortName + " sang " + currCampus.shortName,
                                "⚠️ Đang chạy xe di chuyển ngoài đường - Tuyệt đối không thể nhận việc!",
                                "🟠 Đệm di chuyển",
                                "#EA580C",
                                "#FFF7ED"
                        );
                        summary.timelineSlots.add(travelSlot);
                    } else {
                        // Cùng cơ sở: Không cần đệm di chuyển, toàn bộ là giờ rảnh / nghỉ giữa ca
                        if (gap >= 60) {
                            // Trống từ 60 phút trở lên (VD: 16:35 - 18:00 là 1h25p hoặc >= 120 phút): Lịch trống khả dụng để chèn lịch phát sinh
                            boolean isDeep = gap >= 120;
                            TimelineSlot freeSlot = new TimelineSlot(
                                    TimelineSlot.SlotType.DEEP_FREE,
                                    pointer,
                                    currBusy.getStartMin(),
                                    isDeep ? "🟢 RẢNH HOÀN TOÀN (" + formatDuration(gap) + ")"
                                           : "🟢 LỊCH TRỐNG KHẢ DỤNG (" + formatDuration(gap) + ")",
                                    "[" + TimeNormalizer.minuteToTimeString(pointer) + " - " + TimeNormalizer.minuteToTimeString(currBusy.getStartMin()) + "]",
                                    "[+ Nhấn vào đây để chèn lịch phát sinh / Soạn đề thi]",
                                    isDeep ? "🟢 Rảnh hoàn toàn" : "🟢 Trống khả dụng",
                                    "#16A34A",
                                    "#F0FDF4"
                            );
                            summary.timelineSlots.add(freeSlot);
                            totalFreeMin += gap;
                        } else {
                            // Dưới 60 phút (VD: 11:35 - 12:30 là 55 phút): Nghỉ chuyển ca / Ăn trưa tại chỗ
                            TimelineSlot microFree = new TimelineSlot(
                                    TimelineSlot.SlotType.MICRO_FREE,
                                    pointer,
                                    currBusy.getStartMin(),
                                    "🟡 NGHỈ GIỮA CA TẠI CHỖ (" + gap + " phút - Cùng cơ sở " + currCampus.shortName + ")",
                                    "[" + TimeNormalizer.minuteToTimeString(pointer) + " - " + TimeNormalizer.minuteToTimeString(currBusy.getStartMin()) + "]",
                                    "Gợi ý: Ăn trưa, nghỉ ngơi, họp online nhanh (Không nhận lịch họp dài)",
                                    "🟡 Nghỉ chuyển ca",
                                    "#CA8A04",
                                    "#FEFCE8"
                            );
                            summary.timelineSlots.add(microFree);
                            totalFreeMin += gap;
                        }
                    }
                }
            }

            // B. Thêm ca bận vào dòng chảy
            summary.timelineSlots.add(currBusy);
            pointer = Math.max(pointer, currBusy.getEndMin());
        }

        // C. Khoảng trống cuối ngày (từ sau ca cuối cùng đến 20:15)
        if (pointer < TimeNormalizer.DAY_END_MINUTE) {
            int endGap = TimeNormalizer.DAY_END_MINUTE - pointer;
            if (endGap >= 25) {
                TimelineSlot endSlot = new TimelineSlot(
                        TimelineSlot.SlotType.END_DAY_FREE,
                        pointer,
                        TimeNormalizer.DAY_END_MINUTE,
                        "🟢 TRỐNG CUỐI NGÀY (" + formatDuration(endGap) + ")",
                        "[" + TimeNormalizer.minuteToTimeString(pointer) + " - " + TimeNormalizer.minuteToTimeString(TimeNormalizer.DAY_END_MINUTE) + "]",
                        "Gợi ý: Thể thao, sinh hoạt gia đình hoặc làm việc tự do",
                        "🟢 Rảnh cuối ngày",
                        "#16A34A",
                        "#F0FDF4"
                );
                summary.timelineSlots.add(endSlot);
                totalFreeMin += endGap;
            }
        }

        summary.totalBusyCount = busySlots.size();
        summary.totalFreeMinutes = totalFreeMin;

        // Tổng hợp thống kê ngắn gọn đầu ngày
        String busyStatsStr = (teachingCount > 0 ? teachingCount + " ca dạy" : "")
                + (teachingCount > 0 && incidentCount > 0 ? " - " : "")
                + (incidentCount > 0 ? incidentCount + " sự kiện" : "");
        if (busyStatsStr.isEmpty()) busyStatsStr = busySlots.size() + " công việc";

        summary.statsSummary = "(Tổng: " + busyStatsStr + " | Trống: " + formatDuration(totalFreeMin) + ")";
        return summary;
    }

    private static TimelineSlot.PriorityType classifyPriority(EventModel ev) {
        if (ev == null) return TimelineSlot.PriorityType.PERSONAL_TASK;
        String text = (ev.getTitle() + " " + (ev.getOrganizer() != null ? ev.getOrganizer() : "")).toLowerCase(Locale.getDefault());

        // 1. Sự kiện học thuật phát sinh (Đột xuất): Họp hội đồng, coi thi, chấm đồ án, tập huấn...
        if (text.contains("họp") || text.contains("hội đồng") || text.contains("coi thi") || text.contains("chấm đồ án")
                || text.contains("khóa luận") || text.contains("tập huấn") || text.contains("hội nghị") || text.contains("đột xuất")) {
            return TimelineSlot.PriorityType.ACADEMIC_INCIDENT;
        }

        // 2. Công việc cá nhân / Chuẩn bị: Soạn đề, chấm bài, hạn chót hành chính...
        if (text.contains("soạn") || text.contains("đề thi") || text.contains("chấm bài") || text.contains("cá nhân")
                || text.contains("nộp điểm") || text.contains("bảng điểm") || text.contains("nhắc nhở")) {
            return TimelineSlot.PriorityType.PERSONAL_TASK;
        }

        // 3. Nếu không phải sự kiện giảng dạy (ví dụ CV, công tác cá nhân, hồ sơ)
        if (!ev.isTeachingEvent()) {
            return TimelineSlot.PriorityType.PERSONAL_TASK;
        }

        // 4. Lịch giảng dạy cố định
        return TimelineSlot.PriorityType.FIXED_TEACHING;
    }

    private static String determineSlotColor(SchoolProfile.SchoolType school, TimelineSlot.PriorityType priority) {
        if (priority == TimelineSlot.PriorityType.FIXED_TEACHING) {
            return school.getPrimaryColor(); // Cam cho HUTECH, Xanh cho NTTU
        }
        return priority.getColor();
    }

    private static String cleanTitleForStream(String rawTitle) {
        if (rawTitle == null) return "";
        String clean = EventModel.cleanDisplayTitle(rawTitle);
        return clean
                .replaceAll("(?i)\\[[a-z0-9_-]+\\]", "")
                .replaceAll("(?i)\\b(thứ\\s+\\w+|tiết\\s+[:0-9\\s-–]+|ngày\\s+[:0-9/.-]+)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String buildSubtitle(CampusBufferHelper.CampusInfo campus, TimeNormalizer.NormalizedRange range, EventModel ev) {
        StringBuilder sb = new StringBuilder();
        if (campus.room != null && !campus.room.isEmpty()) {
            sb.append("📍 Phòng ").append(campus.room);
            if (!campus.campusName.isEmpty()) {
                sb.append(" (").append(campus.shortName).append(")");
            }
        } else if (!campus.campusName.isEmpty()) {
            sb.append("📍 ").append(campus.campusName);
        }

        if (range.isPeriodBased) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(range.label);
        }

        return sb.toString();
    }

    private static String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h > 0 && m > 0) return h + " giờ " + m + " phút";
        if (h > 0) return h + " giờ";
        return m + " phút";
    }

    private static String formatMinutesToHourString(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format(Locale.US, "%02dh%02d", h, m);
    }
}

