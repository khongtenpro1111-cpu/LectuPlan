package com.tkyl.lectuplan;

/**
 * Model cho từng mắt xích trong "Dòng chảy thời gian liên tục" (Continuous Timeline Stream)
 */
public class TimelineSlot {

    public enum SlotType {
        BUSY_EVENT,       // Ca dạy hoặc sự kiện bận
        DEEP_FREE,        // Rảnh hoàn toàn (>= 120 phút)
        MICRO_FREE,       // Rảnh ngắn / Nghỉ giữa ca tại chỗ (30 - 90 phút)
        TRAVEL_WARNING,   // Thời gian đệm di chuyển xe giữa 2 cơ sở khác nhau
        START_DAY_FREE,   // Trống đầu ngày (trước ca đầu)
        END_DAY_FREE      // Trống cuối ngày (sau ca cuối)
    }

    public enum PriorityType {
        FIXED_TEACHING("Cực cao (Cố định)", "#EA580C", "#FFEDD5"), // Cam HUTECH hoặc Xanh NTTU
        ACADEMIC_INCIDENT("Cao (Đột xuất)", "#9333EA", "#F3E8FF"),   // Tím / Đỏ
        PERSONAL_TASK("Linh hoạt", "#D97706", "#FEF3C7");            // Vàng / Xanh ngọc

        private final String label;
        private final String color;
        private final String bgColor;

        PriorityType(String label, String color, String bgColor) {
            this.label = label;
            this.color = color;
            this.bgColor = bgColor;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
        public String getBgColor() { return bgColor; }
    }

    private SlotType slotType;
    private PriorityType priorityType;
    private int startMin;
    private int endMin;
    private String timeDisplay;       // e.g. [07:30 - 11:35]
    private String title;             // e.g. [HUTECH] Thực hành lập trình Web (Lớp 23DTHC3)
    private String subtitle;          // e.g. 📍 Phòng E1-04.06/1 (Thu Duc Campus) | Tiết 2 - 6
    private String suggestion;        // e.g. Gợi ý: Ăn trưa, nghỉ ngơi (Không nhận lịch họp dài)
    private String badgeText;         // e.g. 🟢 Rảnh hoàn toàn, 🟧 Cố định, 🟠 Đệm di chuyển
    private String colorHex;          // Màu chủ đạo
    private String bgHex;             // Màu nền card
    private EventModel sourceEvent;   // Sự kiện gốc (nếu là BUSY_EVENT)

    public TimelineSlot(SlotType slotType, int startMin, int endMin, String title, String subtitle, String suggestion, String badgeText, String colorHex, String bgHex) {
        this.slotType = slotType;
        this.startMin = startMin;
        this.endMin = endMin;
        this.timeDisplay = "[" + TimeNormalizer.minuteToTimeString(startMin) + " - " + TimeNormalizer.minuteToTimeString(endMin) + "]";
        this.title = title;
        this.subtitle = subtitle;
        this.suggestion = suggestion;
        this.badgeText = badgeText;
        this.colorHex = colorHex;
        this.bgHex = bgHex;
    }

    public SlotType getSlotType() { return slotType; }
    public boolean isFreeSlot() {
        return slotType == SlotType.DEEP_FREE || slotType == SlotType.START_DAY_FREE || slotType == SlotType.END_DAY_FREE || slotType == SlotType.MICRO_FREE;
    }
    public PriorityType getPriorityType() { return priorityType; }
    public void setPriorityType(PriorityType priorityType) { this.priorityType = priorityType; }

    public int getStartMin() { return startMin; }
    public int getEndMin() { return endMin; }
    public int getDurationMinutes() { return Math.max(0, endMin - startMin); }
    public String getTimeDisplay() { return timeDisplay; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getSuggestion() { return suggestion; }
    public String getBadgeText() { return badgeText; }
    public String getColorHex() { return colorHex; }
    public String getBgHex() { return bgHex; }
    public EventModel getSourceEvent() { return sourceEvent; }
    public void setSourceEvent(EventModel sourceEvent) { this.sourceEvent = sourceEvent; }

    public void setBadgeText(String badgeText) { this.badgeText = badgeText; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public void setBgHex(String bgHex) { this.bgHex = bgHex; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getDurationFormatted() {
        int duration = getDurationMinutes();
        int h = duration / 60;
        int m = duration % 60;
        if (h > 0 && m > 0) return h + " giờ " + m + " phút";
        if (h > 0) return h + " giờ";
        return m + " phút";
    }
}

