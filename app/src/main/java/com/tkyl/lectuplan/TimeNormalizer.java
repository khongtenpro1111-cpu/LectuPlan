package com.tkyl.lectuplan;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal Time Normalizer (Bộ quy đổi Chuẩn hóa Thời gian)
 * Chuyển đổi mọi loại thời gian (Tiết học của các trường, Khung giờ hành chính, Chuỗi nhập tay)
 * về "Số phút tính từ 00:00" (Minutes from Midnight).
 */
public class TimeNormalizer {

    public static final int DAY_START_MINUTE = 6 * 60 + 45; // 06:45 (405m)
    public static final int DAY_END_MINUTE = 21 * 60 + 45;   // 21:45 (1305m - Khớp trọn vẹn 16 tiết NTTU và 15 tiết HUTECH)

    public static class NormalizedRange {
        public int startMin;
        public int endMin;
        public String startTimeStr;
        public String endTimeStr;
        public String label;
        public boolean isPeriodBased;

        public NormalizedRange(int startMin, int endMin, String label, boolean isPeriodBased) {
            this.startMin = startMin;
            this.endMin = endMin;
            this.startTimeStr = minuteToTimeString(startMin);
            this.endTimeStr = minuteToTimeString(endMin);
            this.label = label;
            this.isPeriodBased = isPeriodBased;
        }

        public int getDurationMinutes() {
            return Math.max(0, endMin - startMin);
        }

        public String getDurationFormatted() {
            int duration = getDurationMinutes();
            int h = duration / 60;
            int m = duration % 60;
            if (h > 0 && m > 0) return h + " giờ " + m + " phút";
            if (h > 0) return h + " giờ";
            return m + " phút";
        }
    }

    /**
     * Quy đổi số phút tính từ 00:00 sang chuỗi HH:mm (VD: 450 -> "07:30")
     */
    public static String minuteToTimeString(int minutes) {
        if (minutes < 0) minutes = 0;
        int h = (minutes / 60) % 24;
        int m = minutes % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    /**
     * Quy đổi chuỗi giờ HH:mm, H:mm, Hh hoặc HhMM sang số phút tính từ 00:00
     */
    public static int timeStringToMinute(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return -1;
        String clean = timeStr.trim().toLowerCase(Locale.getDefault())
                .replace("h", ":").replace(".", ":");
        String[] parts = clean.split(":");
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = parts.length > 1 && !parts[1].trim().isEmpty() ? Integer.parseInt(parts[1].trim()) : 0;
            return h * 60 + m;
        } catch (Exception ignored) {
            return -1;
        }
    }

    /**
     * Chuẩn hóa từ Tiết bắt đầu + Số tiết theo trường học
     * Ví dụ HUTECH: Tiết 2, Số tiết 5 -> Tiết 2 đến Tiết 6 -> 07:30 đến 11:35 (450m - 695m)
     */
    public static NormalizedRange fromPeriodRange(SchoolProfile.SchoolType school, int startPeriod, int periodCount) {
        if (periodCount <= 0) periodCount = 1;
        int endPeriod = startPeriod + periodCount - 1;
        int startMin = SchoolProfile.getPeriodStartMinute(school, startPeriod);
        int endMin = SchoolProfile.getPeriodEndMinute(school, endPeriod);
        String label = "Tiết " + startPeriod + " - " + endPeriod;
        return new NormalizedRange(startMin, endMin, label, true);
    }

    /**
     * Tự động trích xuất và chuẩn hóa khung giờ từ nội dung bất kỳ (Tiết học hoặc Giờ hành chính)
     */
    public static NormalizedRange parseFromText(String text, SchoolProfile.SchoolType defaultSchool) {
        if (text == null || text.trim().isEmpty()) {
            return new NormalizedRange(7 * 60 + 30, 9 * 60 + 30, "Mặc định", false);
        }

        // 1. Quét khung giờ hành chính trước (VD: 14:00 - 16:00, 08:00 đến 09:30, 13h30 - 15h30, 8g30 - 10g00)
        Pattern timePattern = Pattern.compile("(\\d{1,2})[h:g](\\d{0,2})\\s*[-–đếnto]+\\s*(\\d{1,2})[h:g](\\d{0,2})", Pattern.CASE_INSENSITIVE);
        Matcher mTime = timePattern.matcher(text);
        if (mTime.find()) {
            try {
                int h1 = Integer.parseInt(mTime.group(1));
                int m1 = mTime.group(2).isEmpty() ? 0 : Integer.parseInt(mTime.group(2));
                int h2 = Integer.parseInt(mTime.group(3));
                int m2 = mTime.group(4).isEmpty() ? 0 : Integer.parseInt(mTime.group(4));
                int startMin = h1 * 60 + m1;
                int endMin = h2 * 60 + m2;
                if (endMin > startMin) {
                    return new NormalizedRange(startMin, endMin, minuteToTimeString(startMin) + " - " + minuteToTimeString(endMin), false);
                }
            } catch (Exception ignored) {}
        }

        // 2. Quét tiết học (VD: Tiết 2-6, Tiết 7 - 11, Tiết: 2 - 6, Tiết 2 đến 6, Tiết 1..3)
        // Chống bắt nhầm các từ như "chi tiết", "tiết kiệm", "tình tiết"
        Pattern periodPattern = Pattern.compile("(?i)(?<![a-zA-ZÀ-ỹ0-9_])(?<!chi\\s?)(?<!tình\\s?)\\btiết\\s*[:\\s-]*(\\d{1,2})\\s*[-–đếnvàto.\\s]+\\s*(\\d{1,2})\\b");
        Matcher mPeriod = periodPattern.matcher(text);
        if (mPeriod.find()) {
            try {
                int pStart = Integer.parseInt(mPeriod.group(1));
                int pEnd = Integer.parseInt(mPeriod.group(2));
                if (pEnd >= pStart) {
                    int startMin = SchoolProfile.getPeriodStartMinute(defaultSchool, pStart);
                    int endMin = SchoolProfile.getPeriodEndMinute(defaultSchool, pEnd);
                    return new NormalizedRange(startMin, endMin, "Tiết " + pStart + " - " + pEnd, true);
                }
            } catch (Exception ignored) {}
        }

        // 3. Quét dạng Ca học (VD: Ca 1, Ca 2, Ca 3, Ca 4, Ca 5)
        Pattern caPattern = Pattern.compile("(?i)\\bca\\s*([1-5])\\b");
        Matcher mCa = caPattern.matcher(text);
        if (mCa.find()) {
            try {
                int caNum = Integer.parseInt(mCa.group(1));
                int pStart = (caNum - 1) * 3 + 1;
                return fromPeriodRange(defaultSchool, pStart, 3);
            } catch (Exception ignored) {}
        }

        // 3. Quét Tiết bắt đầu + Số tiết (VD: Tiết BĐ: 2, Số tiết: 5)
        Pattern pStartCount = Pattern.compile("(?i)(?<![a-zA-ZÀ-ỹ0-9_])(?<!chi\\s?)\\btiết\\s*(?:bắt\\s*đầu|bđ)?\\s*[:=]?\\s*(\\d{1,2})[^0-9]+(?:số\\s*tiết\\s*[:=]?\\s*(\\d{1,2}))\\b");
        Matcher mStartCount = pStartCount.matcher(text);
        if (mStartCount.find()) {
            try {
                int pStart = Integer.parseInt(mStartCount.group(1));
                int count = Integer.parseInt(mStartCount.group(2));
                return fromPeriodRange(defaultSchool, pStart, count);
            } catch (Exception ignored) {}
        }

        // 4. Fallback theo từ khóa buổi: Sáng / Chiều / Tối / Cả ngày
        String lower = text.toLowerCase(Locale.getDefault());
        if (lower.contains("cả ngày")) {
            return new NormalizedRange(7 * 60, 17 * 60, "Cả ngày", false);
        } else if (lower.contains("chiều") || lower.contains("buổi chiều")) {
            return new NormalizedRange(12 * 60 + 30, 16 * 60 + 35, "Buổi chiều", false);
        } else if (lower.contains("tối") || lower.contains("buổi tối")) {
            return new NormalizedRange(18 * 60, 20 * 60 + 15, "Buổi tối", false);
        } else {
            return new NormalizedRange(7 * 60 + 30, 11 * 60 + 35, "Buổi sáng", false);
        }
    }
}

