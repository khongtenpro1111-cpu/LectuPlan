package com.tkyl.lectuplan;

public class FreeTimeDayModel {
    private String dateIso;          // yyyy-MM-dd
    private String dayTitle;         // e.g. "Thứ Hai (26/10/2026)"
    private String statusBadge;      // e.g. "🟢 TRỐNG CẢ NGÀY"
    private String statusColor;      // e.g. "#15803D"
    private String statusBgColor;    // e.g. "#DCFCE7"
    private String morningDetail;    // Detail text for Morning
    private String afternoonDetail;  // Detail text for Afternoon
    private String detailText;       // Summary fallback

    public FreeTimeDayModel(String dayTitle, String statusBadge, String statusColor, String statusBgColor, String morningDetail, String afternoonDetail) {
        this.dayTitle = dayTitle;
        this.statusBadge = statusBadge;
        this.statusColor = statusColor;
        this.statusBgColor = statusBgColor;
        this.morningDetail = morningDetail;
        this.afternoonDetail = afternoonDetail;
        this.detailText = (morningDetail != null ? morningDetail : "") + "\n" + (afternoonDetail != null ? afternoonDetail : "");
    }

    public FreeTimeDayModel(String dateIso, String dayTitle, String statusBadge, String statusColor, String statusBgColor, String morningDetail, String afternoonDetail) {
        this.dateIso = dateIso;
        this.dayTitle = dayTitle;
        this.statusBadge = statusBadge;
        this.statusColor = statusColor;
        this.statusBgColor = statusBgColor;
        this.morningDetail = morningDetail;
        this.afternoonDetail = afternoonDetail;
        this.detailText = (morningDetail != null ? morningDetail : "") + "\n" + (afternoonDetail != null ? afternoonDetail : "");
    }

    public String getDateIso() { return dateIso; }
    public String getDayTitle() { return dayTitle; }
    public String getStatusBadge() { return statusBadge; }
    public String getStatusColor() { return statusColor; }
    public String getStatusBgColor() { return statusBgColor; }
    public String getMorningDetail() { return morningDetail; }
    public String getAfternoonDetail() { return afternoonDetail; }
    public String getDetailText() { return detailText; }
}
