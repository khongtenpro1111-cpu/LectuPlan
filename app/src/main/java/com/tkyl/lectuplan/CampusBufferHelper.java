package com.tkyl.lectuplan;

import java.util.Locale;

/**
 * Thuật toán "Khoảng cách cơ sở & Đệm di chuyển" (Campus Buffer Engine)
 * Phát hiện vị trí cơ sở, phân biệt rảnh tại chỗ vs thời gian chạy xe ngoài đường.
 */
public class CampusBufferHelper {

    public static final int DEFAULT_TRAVEL_BUFFER_MINUTES = 50; // Đệm di chuyển chuẩn giữa các cơ sở (45-60 phút)

    public static class CampusInfo {
        public String campusId;       // e.g. HUTECH_THUDUC, HUTECH_SAIGON, NTTU_Q4
        public String campusName;     // e.g. Thu Duc Campus (Khu E)
        public String shortName;      // e.g. Khu E Thủ Đức
        public String address;        // e.g. Khu Công nghệ cao - P. Tăng Nhơn Phú
        public String room;           // e.g. E1-04.06/1
        public boolean isOnline;

        public CampusInfo(String campusId, String campusName, String shortName, String address, String room, boolean isOnline) {
            this.campusId = campusId;
            this.campusName = campusName;
            this.shortName = shortName;
            this.address = address;
            this.room = room;
            this.isOnline = isOnline;
        }
    }

    /**
     * Nhận diện thông tin cơ sở và phòng học từ mô tả sự kiện
     */
    public static CampusInfo detectCampus(String text, String defaultSchool) {
        if (text == null) text = "";
        String lower = text.toLowerCase(Locale.getDefault());

        // Kiểm tra họp online
        if (lower.contains("zoom") || lower.contains("meet") || lower.contains("teams") || lower.contains("online") || lower.contains("trực tuyến") || lower.contains("e-learning")) {
            String r = extractGenericRoom(text);
            return new CampusInfo("ONLINE", "Họp / Dạy Online (E-Learning)", "Online", "Không cần di chuyển", r.isEmpty() ? "Online" : r, true);
        }

        // 1. Phân tích cơ sở trường Văn Lang (VLU)
        if (lower.contains("văn lang") || defaultSchool.equalsIgnoreCase("VLU")) {
            if (lower.contains("đặng thùy trâm") || lower.contains("cơ sở 3") || lower.contains("cs3")) {
                return new CampusInfo("VLU_CS3", "Cơ sở 3 Đặng Thùy Trâm (Văn Lang)", "Văn Lang CS3", "69/68 Đặng Thùy Trâm, P.13, Bình Thạnh", extractGenericRoom(text), false);
            }
            if (lower.contains("phan văn trị") || lower.contains("cơ sở 2") || lower.contains("cs2")) {
                return new CampusInfo("VLU_CS2", "Cơ sở 2 Phan Văn Trị (Văn Lang)", "Văn Lang CS2", "233A Phan Văn Trị, P.11, Bình Thạnh", extractGenericRoom(text), false);
            }
            return new CampusInfo("VLU_CS1", "Trụ sở 45 Nguyễn Khắc Nhu (Văn Lang)", "Văn Lang CS1", "45 Nguyễn Khắc Nhu, Cô Giang, Q.1", extractGenericRoom(text), false);
        }

        // 2. Phân tích cơ sở trường NTTU (Nguyễn Tất Thành)
        if (lower.contains("nttu") || defaultSchool.equalsIgnoreCase("NTTU") || lower.contains("nguyễn tất thành")
                || lower.matches("(?s).*\\b(l\\.?\\d{3}|l2\\.?\\d{3})\\b.*")) {
            if (lower.contains("an phú đông") || lower.contains("quận 12") || lower.contains("q12") || lower.contains("q.12")
                    || lower.matches("(?s).*\\b(l\\.?\\d{3}|l2\\.?\\d{3})\\b.*")) {
                return new CampusInfo("NTTU_Q12", "Cơ sở Quận 12 (An Phú Đông)", "NTTU Q12", "331 Quốc lộ 1A, P. An Phú Đông, Q.12", extractGenericRoom(text), false);
            }
            if (lower.contains("nguyễn hữu thọ") || lower.contains("quận 7") || lower.contains("q7") || lower.contains("q.7")) {
                return new CampusInfo("NTTU_Q7", "Cơ sở Quận 7 (Nguyễn Hữu Thọ)", "NTTU Q7", "Nguyễn Hữu Thọ, Tân Phong, Q.7", extractGenericRoom(text), false);
            }
            if (lower.contains("công nghệ cao") || lower.contains("thủ đức")) {
                return new CampusInfo("NTTU_THUDUC", "Cơ sở Khu CNC Thủ Đức", "NTTU Thủ Đức", "Khu CNC, TP. Thủ Đức", extractGenericRoom(text), false);
            }
            return new CampusInfo("NTTU_Q4", "Trụ sở Quận 4 (300A Nguyễn Tất Thành)", "NTTU Quận 4", "300A Nguyễn Tất Thành, P.13, Q.4", extractGenericRoom(text), false);
        }

        // 3. Phân tích cơ sở UEF
        if (lower.contains("uef") || defaultSchool.equalsIgnoreCase("UEF")) {
            if (lower.contains("hai bà trưng") || lower.contains("q3") || lower.contains("quận 3")) {
                return new CampusInfo("UEF_Q3", "Cơ sở 254 Hai Bà Trưng (UEF)", "UEF Q3", "254 Hai Bà Trưng, P. Tân Định, Q.1", extractGenericRoom(text), false);
            }
            return new CampusInfo("UEF_DIBIPHU", "Trụ sở 141 Điện Biên Phủ (UEF)", "UEF Điện Biên Phủ", "141-145 Điện Biên Phủ, P.15, Bình Thạnh", extractGenericRoom(text), false);
        }

        // 4. Phân tích cơ sở ĐH Tôn Đức Thắng (TDTU)
        if (lower.contains("tôn đức thắng") || defaultSchool.equalsIgnoreCase("TDTU")) {
            return new CampusInfo("TDTU_Q7", "Trụ sở Tân Phong Q7 (TDTU)", "TDTU Quận 7", "19 Nguyễn Hữu Thọ, Tân Phong, Q.7", extractGenericRoom(text), false);
        }

        // 5. Phân tích cơ sở ĐH Bách Khoa (HCMUT)
        if (lower.contains("bách khoa") || defaultSchool.equalsIgnoreCase("HCMUT")) {
            if (lower.contains("dĩ an") || lower.contains("thủ đức") || lower.contains("đhqg") || lower.contains("cơ sở 2")) {
                return new CampusInfo("HCMUT_DIAN", "Cơ sở Dĩ An (ĐH Bách Khoa)", "Bách Khoa Dĩ An", "Khu Đô thị ĐHQG, Dĩ An", extractGenericRoom(text), false);
            }
            return new CampusInfo("HCMUT_Q10", "Cơ sở Lý Thường Kiệt (ĐH Bách Khoa)", "Bách Khoa Q10", "268 Lý Thường Kiệt, P.14, Q.10", extractGenericRoom(text), false);
        }

        // 6. Phân tích cơ sở ĐH Sư phạm Kỹ thuật (HCMUTE)
        if (lower.contains("sư phạm kỹ thuật") || defaultSchool.equalsIgnoreCase("HCMUTE")) {
            return new CampusInfo("HCMUTE_THUDUC", "Trụ sở Thủ Đức (HCMUTE)", "SPKT Thủ Đức", "01 Võ Văn Ngân, Linh Chiểu, Thủ Đức", extractGenericRoom(text), false);
        }

        // 7. Phân tích cơ sở trường HUTECH
        if (lower.contains("hutech") || defaultSchool.equalsIgnoreCase("HUTECH")
                || text.matches(".*(?i)[ER]\\d?[-–]\\d{2}.*")
                || lower.contains("ung văn khiêm")
                || (defaultSchool.equalsIgnoreCase("HUTECH") && text.matches(".*(?i)[ABCU][-–]\\d{2}.*"))
                || lower.contains("điện biên phủ")) {

            // Phòng ký hiệu E- hoặc R- : Thu Duc Campus / Hitech Park Campus (Khu CNC)
            if (text.matches(".*(?i)\\b[ER]\\d?[-–.0-9/]+\\b.*") || lower.contains("khu e") || lower.contains("khu r") || lower.contains("thu duc") || lower.contains("thủ đức") || lower.contains("công nghệ cao")) {
                String room = extractRoomPattern(text, "[ER]\\d?[-–.0-9/]+");
                return new CampusInfo("HUTECH_THUDUC", "Thu Duc Campus (Khu Công nghệ cao)", "Khu E Thủ Đức", "Khu CNC, P. Tăng Nhơn Phú, TP. Thủ Đức", room, false);
            }

            // Phòng ký hiệu U- : Ung Van Khiem Campus
            if (text.matches(".*(?i)\\bU[-–.0-9/]+\\b.*") || lower.contains("ung văn khiêm") || lower.contains("khu u")) {
                String room = extractRoomPattern(text, "U[-–.0-9/]+");
                return new CampusInfo("HUTECH_UNGVANKHIEM", "Ung Van Khiem Campus", "Khu U Ung Văn Khiêm", "31/36 Ung Văn Khiêm, P. Thạnh Mỹ Tây, Bình Thạnh", room, false);
            }

            // Phòng ký hiệu A, B, C- : Sai Gon Campus (475A Điện Biên Phủ)
            if (text.matches(".*(?i)\\b[ABC][-–.0-9/]+\\b.*") || lower.contains("điện biên phủ") || lower.contains("sai gon campus") || lower.contains("sài gòn")) {
                String room = extractRoomPattern(text, "[ABC][-–.0-9/]+");
                return new CampusInfo("HUTECH_SAIGON", "Sai Gon Campus (475A Điện Biên Phủ)", "Cơ sở Điện Biên Phủ", "475A Điện Biên Phủ, P. Thạnh Mỹ Tây, Bình Thạnh", room, false);
            }

            return new CampusInfo("HUTECH_GEN", "Cơ sở HUTECH", "HUTECH", "TP. Hồ Chí Minh", extractGenericRoom(text), false);
        }

        return new CampusInfo("OTHER", "Cơ sở bên ngoài", "Tại chỗ", "TP. Hồ Chí Minh", extractGenericRoom(text), false);
    }

    private static String extractRoomPattern(String text, String regex) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\b(" + regex + ")\\b").matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return "";
    }

    public static String extractGenericRoom(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        try {
            // 1. Tìm sau tiền tố Phòng / Room / Địa điểm (hỗ trợ mọi ký tự phòng: gạch ngang, chấm, phẩy, v.v.)
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:phòng|room|địa\\s*điểm)\\s*[:=–-]?\\s*([^|\\[\\]\\n]+)").matcher(text);
            if (m.find()) {
                String r = m.group(1).trim();
                r = r.replaceAll("(?i)\\s*\\|\\s*tiết.*", "")
                     .replaceAll("(?i)\\s*\\[\\d{1,2}:\\d{2}.*", "")
                     .replaceAll("(?i)^[:=–-]\\s*", "")
                     .trim();
                if (!r.isEmpty()) return r;
            }
            // 2. Tìm mã phòng phổ biến như E1-04, L2.503, A-102, U-01
            java.util.regex.Matcher mCode = java.util.regex.Pattern.compile("(?i)\\b([A-Z]\\d?[-–.0-9/]+)\\b").matcher(text);
            if (mCode.find()) {
                return mCode.group(1).trim();
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Kiểm tra xem 2 sự kiện liên tiếp có cùng cơ sở hay không
     */
    public static boolean isSameCampus(CampusInfo c1, CampusInfo c2) {
        if (c1 == null || c2 == null) return true;
        if (c1.isOnline || c2.isOnline) return true; // Một trong hai là online thì không cần đệm di chuyển xe
        if (c1.campusId.equals("OTHER") && c2.campusId.equals("OTHER")) return true;
        return c1.campusId.equals(c2.campusId);
    }
}

