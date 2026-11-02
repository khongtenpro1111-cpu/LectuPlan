package com.tkyl.lectuplan;

public class FreeTimeDayModel {
    private String dayTitle;      // e.g. "Thứ Hai (26/10/2026)"
    private String statusBadge;   // e.g. "🟢 TRỐNG CẢ NGÀY" or "🟢 TRỐNG CHIỀU"
    private String statusColor;   // e.g. "#15803D"
    private String statusBgColor; // e.g. "#DCFCE7"
    private String detailText;    // e.g. "💡 Sáng (07:00 - 11:30): Bận Họp Khoa • Chiều (13:00 - 17:00): TRỐNG"

    public FreeTimeDayModel(String dayTitle, String statusBadge, String statusColor, String statusBgColor, String detailText) {
        this.dayTitle = dayTitle;
        this.statusBadge = statusBadge;
        this.statusColor = statusColor;
        this.statusBgColor = statusBgColor;
        this.detailText = detailText;
    }

    public String getDayTitle() { return dayTitle; }
    public String getStatusBadge() { return statusBadge; }
    public String getStatusColor() { return statusColor; }
    public String getStatusBgColor() { return statusBgColor; }
    public String getDetailText() { return detailText; }
}
