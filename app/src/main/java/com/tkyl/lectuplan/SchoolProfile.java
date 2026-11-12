package com.tkyl.lectuplan;

import java.util.Locale;

/**
 * Universal Time Normalizer: School Profiles (Hồ sơ quy đổi theo từng trường)
 * Quy đổi từ "Tiết học" của từng trường thành "Số phút tính từ 00:00" (Minutes from Midnight)
 */
public class SchoolProfile {

    public enum SchoolType {
        HUTECH("HUTECH", "Trường Đại học Công nghệ TP.HCM", "#EA580C", 115000), // Cam HUTECH
        NTTU("NTTU", "Trường Đại học Nguyễn Tất Thành", "#1D4ED8", 120000),      // Xanh đậm NTTU
        UEF("UEF", "Trường Đại học Kinh tế - Tài chính TP.HCM", "#0284C7", 130000), // Xanh ngọc UEF
        UEH("UEH", "Đại học Kinh tế TP.HCM", "#059669", 150000),                 // Xanh lá UEH
        HCMUT("HCMUT", "Trường Đại học Bách Khoa - ĐHQG-HCM", "#0284C7", 160000), // Xanh dương Bách Khoa
        VLU("VLU", "Trường Đại học Văn Lang", "#DC2626", 140000),                // Đỏ Văn Lang
        TDTU("TDTU", "Trường Đại học Tôn Đức Thắng", "#2563EB", 125000),         // Xanh Tôn Đức Thắng
        HCMUTE("HCMUTE", "Trường Đại học Sư phạm Kỹ thuật TP.HCM", "#7C3AED", 135000), // Tím SPKT
        DEFAULT("OTHER", "Cơ sở / Trường khác", "#475569", 100000);

        private final String code;
        private final String displayName;
        private final String primaryColor;
        private final long defaultPayRate; // Đơn giá thù lao trên 1 tiết dạy

        SchoolType(String code, String displayName, String primaryColor, long defaultPayRate) {
            this.code = code;
            this.displayName = displayName;
            this.primaryColor = primaryColor;
            this.defaultPayRate = defaultPayRate;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
        public String getPrimaryColor() { return primaryColor; }
        public long getDefaultPayRate() { return defaultPayRate; }
    }

    /**
     * Nhận diện trường dựa trên text trong văn bản, tiêu đề hoặc đơn vị
     */
    public static SchoolType detectSchool(String text) {
        if (text == null) return SchoolType.DEFAULT;
        String lower = text.toLowerCase(Locale.getDefault());
        if (lower.contains("hutech") || lower.contains("công nghệ tp.hcm") || lower.contains("đại học công nghệ tphcm")) {
            return SchoolType.HUTECH;
        }
        if (lower.contains("nttu") || lower.contains("nguyễn tất thành") || lower.contains("nguyen tat thanh")
                || lower.matches("(?s).*\\b(l\\.?\\d{3}|l2\\.?\\d{3}|e-learning\\s*\\d*)\\b.*")
                || (lower.contains("trang chủ") && lower.contains("lịch học") && lower.contains("điểm danh"))) {
            return SchoolType.NTTU;
        }
        if (lower.contains("uef") || lower.contains("kinh tế tài chính") || lower.contains("kinh te tai chinh")) {
            return SchoolType.UEF;
        }
        if (lower.contains("ueh") || lower.contains("kinh tế tp") || lower.contains("đại học kinh tế tp")) {
            return SchoolType.UEH;
        }
        if (lower.contains("bách khoa") || lower.contains("bach khoa") || lower.contains("hcmut")) {
            return SchoolType.HCMUT;
        }
        if (lower.contains("văn lang") || lower.contains("van lang") || lower.contains("vlu")) {
            return SchoolType.VLU;
        }
        if (lower.contains("tôn đức thắng") || lower.contains("ton duc thang") || lower.contains("tdtu")) {
            return SchoolType.TDTU;
        }
        if (lower.contains("sư phạm kỹ thuật") || lower.contains("spkt") || lower.contains("hcmute")) {
            return SchoolType.HCMUTE;
        }
        return SchoolType.DEFAULT;
    }

    /**
     * Lấy phút bắt đầu của một tiết học (tính từ 00:00) theo từng trường
     */
    public static int getPeriodStartMinute(SchoolType school, int period) {
        if (school == SchoolType.HUTECH) {
            switch (period) {
                // Ca sáng
                case 1: return 6 * 60 + 45;  // 06:45 (405m)
                case 2: return 7 * 60 + 30;  // 07:30 (450m)
                case 3: return 8 * 60 + 15;  // 08:15 (495m)
                case 4: return 9 * 60 + 20;  // 09:20 (560m)
                case 5: return 10 * 60 + 5;  // 10:05 (605m)
                case 6: return 10 * 60 + 50; // 10:50 (650m)
                // Ca chiều
                case 7: return 12 * 60 + 30; // 12:30 (750m)
                case 8: return 13 * 60 + 15; // 13:15 (795m)
                case 9: return 14 * 60;      // 14:00 (840m)
                case 10: return 15 * 60 + 5; // 15:05 (905m)
                case 11: return 15 * 60 + 50;// 15:50 (950m)
                case 12: return 16 * 60 + 35;// 16:35 (995m)
                // Ca tối
                case 13: return 18 * 60;     // 18:00 (1080m)
                case 14: return 18 * 60 + 45;// 18:45 (1125m)
                case 15: return 19 * 60 + 30;// 19:30 (1170m)
                default:
                    if (period < 1) return 6 * 60 + 45;
                    return (6 + period) * 60;
            }
        } else if (school == SchoolType.NTTU) {
            switch (period) {
                // Ca sáng: mỗi tiết 50 phút
                case 1: return 7 * 60;       // 07h00 (420m)
                case 2: return 7 * 60 + 50;  // 07h50 (470m)
                case 3: return 8 * 60 + 40;  // 08h40 (520m)
                // Giải lao 10 phút (09h30 - 09h40)
                case 4: return 9 * 60 + 40;  // 09h40 (580m)
                case 5: return 10 * 60 + 30; // 10h30 (630m)
                case 6: return 11 * 60 + 20; // 11h20 (680m)
                // Ca chiều: mỗi tiết 50 phút
                case 7: return 13 * 60;      // 13h00 (780m)
                case 8: return 13 * 60 + 50; // 13h50 (830m)
                case 9: return 14 * 60 + 40; // 14h40 (880m)
                // Giải lao 10 phút (15h30 - 15h40)
                case 10: return 15 * 60 + 40;// 15h40 (940m)
                case 11: return 16 * 60 + 30;// 16h30 (990m)
                case 12: return 17 * 60 + 20;// 17h20 (1040m)
                // Ca tối: mỗi tiết 50 phút
                case 13: return 18 * 60 + 15;// 18h15 (1095m)
                case 14: return 19 * 60 + 5; // 19h05 (1145m)
                // Giải lao 10 phút (19h55 - 20h05)
                case 15: return 20 * 60 + 5; // 20h05 (1205m)
                case 16: return 20 * 60 + 55;// 20h55 (1255m)
                default:
                    return (6 + period) * 60;
            }
        } else {
            // Mặc định chuẩn chung
            switch (period) {
                case 1: return 7 * 60;
                case 2: return 7 * 60 + 50;
                case 3: return 8 * 60 + 40;
                case 4: return 9 * 60 + 40;
                case 5: return 10 * 60 + 30;
                case 6: return 11 * 60 + 20;
                case 7: return 13 * 60;
                case 8: return 13 * 60 + 50;
                case 9: return 14 * 60 + 40;
                case 10: return 15 * 60 + 30;
                case 11: return 16 * 60 + 20;
                case 12: return 17 * 60 + 10;
                case 13: return 18 * 60;
                case 14: return 18 * 60 + 50;
                case 15: return 19 * 60 + 40;
                default: return (6 + period) * 60;
            }
        }
    }

    /**
     * Lấy phút kết thúc của một tiết học (tính từ 00:00) theo từng trường
     */
    public static int getPeriodEndMinute(SchoolType school, int period) {
        if (school == SchoolType.HUTECH) {
            switch (period) {
                // Ca sáng
                case 1: return 7 * 60 + 30;  // 07:30 (450m)
                case 2: return 8 * 60 + 15;  // 08:15 (495m)
                case 3: return 9 * 60;       // 09:00 (540m)
                case 4: return 10 * 60 + 5;  // 10:05 (605m)
                case 5: return 10 * 60 + 50; // 10:50 (650m)
                case 6: return 11 * 60 + 35; // 11:35 (695m)
                // Ca chiều
                case 7: return 13 * 60 + 15; // 13:15 (795m)
                case 8: return 14 * 60;      // 14:00 (840m)
                case 9: return 14 * 60 + 45; // 14:45 (885m)
                case 10: return 15 * 60 + 50;// 15:50 (950m)
                case 11: return 16 * 60 + 35;// 16:35 (995m)
                case 12: return 17 * 60 + 20;// 17:20 (1040m)
                // Ca tối
                case 13: return 18 * 60 + 45;// 18:45 (1125m)
                case 14: return 19 * 60 + 30;// 19:30 (1170m)
                case 15: return 20 * 60 + 15;// 20:15 (1215m)
                default:
                    return getPeriodStartMinute(school, period) + 45;
            }
        } else if (school == SchoolType.NTTU) {
            switch (period) {
                // Ca sáng
                case 1: return 7 * 60 + 50;  // 07h50 (470m)
                case 2: return 8 * 60 + 40;  // 08h40 (520m)
                case 3: return 9 * 60 + 30;  // 09h30 (570m)
                case 4: return 10 * 60 + 30; // 10h30 (630m)
                case 5: return 11 * 60 + 20; // 11h20 (680m)
                case 6: return 12 * 60 + 10; // 12h10 (730m)
                // Ca chiều
                case 7: return 13 * 60 + 50; // 13h50 (830m)
                case 8: return 14 * 60 + 40; // 14h40 (880m)
                case 9: return 15 * 60 + 30; // 15h30 (930m)
                case 10: return 16 * 60 + 30;// 16h30 (990m)
                case 11: return 17 * 60 + 20;// 17h20 (1040m)
                case 12: return 18 * 60 + 10;// 18h10 (1090m)
                // Ca tối
                case 13: return 19 * 60 + 5; // 19h05 (1145m)
                case 14: return 19 * 60 + 55;// 19h55 (1195m)
                case 15: return 20 * 60 + 55;// 20h55 (1255m)
                case 16: return 21 * 60 + 45;// 21h45 (1305m)
                default:
                    return getPeriodStartMinute(school, period) + 50;
            }
        } else {
            switch (period) {
                case 1: return 7 * 60 + 45;
                case 2: return 8 * 60 + 35;
                case 3: return 9 * 60 + 30;
                case 4: return 10 * 60 + 25;
                case 5: return 11 * 60 + 15;
                case 6: return 12 * 60 + 05;
                case 7: return 13 * 60 + 45;
                case 8: return 14 * 60 + 35;
                case 9: return 15 * 60 + 25;
                case 10: return 16 * 60 + 15;
                case 11: return 17 * 60 + 05;
                case 12: return 17 * 60 + 55;
                case 13: return 18 * 60 + 45;
                case 14: return 19 * 60 + 35;
                case 15: return 20 * 60 + 25;
                default:
                    return getPeriodStartMinute(school, period) + 45;
            }
        }
    }
}

