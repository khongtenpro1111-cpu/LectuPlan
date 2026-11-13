package com.tkyl.lectuplan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UniversalScheduleParser: Bộ bóc tách thông minh đa năng dành cho ảnh chụp màn hình lịch học (OCR),
 * thời khóa biểu điện tử dạng thẻ card, lịch học theo ngày/tuần, và bảng biểu từ PDF/Word/Slide.
 *
 * Khắc phục triệt để lỗi phân mảnh tách từng dòng thành sự kiện độc lập (tránh trường hợp 1 ngày 4 môn
 * bị đẻ thành 24 sự kiện rác do nhồi nhét cả nút bấm, thanh điều hướng và nhãn Tiết/Phòng/Giảng viên).
 */
public class UniversalScheduleParser {

    public static class ParsedScheduleEvent {
        public String title = "";
        public String subjectName = "";
        public String dateIso = "";
        public String organizer = "";
        public String room = "";
        public String periodStr = "";
        public int startPeriod = 0;
        public int endPeriod = 0;
        public int periodCount = 0;
        public String timeRangeStr = "";
        public String categoryBadge = "";
        public String lecturer = "";
        public String className = "";
        public SchoolProfile.SchoolType school = SchoolProfile.SchoolType.DEFAULT;
    }

    private static class RawCard {
        String dateIso;
        String categoryBadge = "";
        String subjectName = "";
        int startPeriod = 0;
        int endPeriod = 0;
        String timeRangeStr = "";
        String room = "";
        String lecturer = "";
        String className = "";

        RawCard(String dateIso) {
            this.dateIso = dateIso;
        }
    }

    /**
     * Bóc tách toàn diện tài liệu / văn bản OCR thành danh sách sự kiện chuẩn hóa 100%.
     */
    public static List<ParsedScheduleEvent> parseDocument(String fullText, String fileName) {
        List<ParsedScheduleEvent> resultEvents = new ArrayList<>();
        if (fullText == null || fullText.trim().isEmpty()) return resultEvents;

        // 1. Nhận diện trường (NTTU, HUTECH, UEF, etc.)
        SchoolProfile.SchoolType school = SchoolProfile.detectSchool(fullText);

        // 2. Trích xuất năm toàn cục của tài liệu (ưu tiên năm xuất hiện trong header / bộ lọc năm / khoảng ngày)
        int documentYear = detectDocumentYear(fullText);

        // 3. Ngày mặc định ban đầu nếu tài liệu chưa chỉ rõ ngày
        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String activeDateIso = sdfIso.format(new Date());

        // Nếu ngày mặc định có năm khác với documentYear, chuẩn hóa theo documentYear
        if (activeDateIso.length() == 10) {
            activeDateIso = String.format(Locale.US, "%04d-%s", documentYear, activeDateIso.substring(5));
        }

        // 4. Tiền xử lý dòng: Tách dòng, loại bỏ khoảng trắng thừa và gộp các nhãn bị gãy đôi
        List<String> foldedLines = preprocessAndFoldLines(fullText);

        // 5. Quét duyệt tuần tự xây dựng các thẻ lịch trình (Card Assembler)
        RawCard currentCard = null;

        for (String line : foldedLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Bỏ qua tuyệt đối các dòng rác (thanh trạng thái, pin, sóng, nút bấm giao diện, menu) và thông tin hành chính
            if (isNoiseLine(trimmed) || isInstitutionalBoilerplate(trimmed)) {
                continue;
            }

            // Kiểm tra xem dòng này có phải là tiêu đề Ngày trong tuần hay không (VD: "Th 4, 28/10", "Thứ 4, 28/10/2026")
            String foundDate = parseDateHeader(trimmed, documentYear);
            if (foundDate != null && !foundDate.isEmpty()) {
                if (currentCard != null && isValidCard(currentCard)) {
                    ParsedScheduleEvent ev = convertCardToEvent(currentCard, school, fileName);
                    if (ev != null) resultEvents.add(ev);
                    currentCard = null;
                }
                activeDateIso = foundDate;
                continue;
            }

            // Kiểm tra dòng có phân cách ống '|' chứa nhiều thuộc tính trong cùng 1 hàng
            if (trimmed.contains("|") && (containsPeriodIndicator(trimmed) || containsRoomIndicator(trimmed) || containsLecturerIndicator(trimmed))) {
                if (currentCard != null && isValidCard(currentCard)) {
                    ParsedScheduleEvent ev = convertCardToEvent(currentCard, school, fileName);
                    if (ev != null) resultEvents.add(ev);
                    currentCard = null;
                }
                RawCard pipeCard = parsePipeDelimitedCard(trimmed, activeDateIso);
                if (pipeCard != null && isValidCard(pipeCard)) {
                    ParsedScheduleEvent ev = convertCardToEvent(pipeCard, school, fileName);
                    if (ev != null) resultEvents.add(ev);
                }
                continue;
            }

            // Kiểm tra nhãn huy hiệu phân loại (VD: "• Lịch học", "• Lịch trực tuyến", "Lịch thi")
            String badge = extractCategoryBadge(trimmed);
            if (badge != null) {
                if (currentCard != null && isValidCard(currentCard)) {
                    ParsedScheduleEvent ev = convertCardToEvent(currentCard, school, fileName);
                    if (ev != null) resultEvents.add(ev);
                    currentCard = null;
                }
                currentCard = new RawCard(activeDateIso);
                currentCard.categoryBadge = badge;
                continue;
            }

            // Kiểm tra Tiết học (VD: "Tiết: 1 - 3", "Tiết 4-6", "Tiết: 13 - 13", "Ca 1")
            PeriodRange periodRange = extractPeriodRange(trimmed);
            if (periodRange != null) {
                if (currentCard == null) currentCard = new RawCard(activeDateIso);
                currentCard.startPeriod = periodRange.start;
                currentCard.endPeriod = periodRange.end;
                continue;
            }

            // Kiểm tra Khung giờ trực tiếp (VD: "07:00 - 09:30", "13h00 - 17h20")
            String timeRange = extractTimeRange(trimmed);
            if (timeRange != null && !timeRange.isEmpty()) {
                if (currentCard == null) currentCard = new RawCard(activeDateIso);
                currentCard.timeRangeStr = timeRange;
                continue;
            }

            // Kiểm tra Phòng học (VD: "Phòng: L.503", "Phòng: L2.123", "Phòng: E-LEARNING 15")
            String room = extractRoom(trimmed);
            if (room != null && !room.isEmpty()) {
                if (currentCard == null) currentCard = new RawCard(activeDateIso);
                currentCard.room = room;
                continue;
            }

            // Kiểm tra Giảng viên (VD: "Giảng viên: Đặng Như Phú", "GV: Lý Quang Vinh")
            String lecturer = extractLecturer(trimmed);
            if (lecturer != null && !lecturer.isEmpty()) {
                if (currentCard == null) currentCard = new RawCard(activeDateIso);
                currentCard.lecturer = lecturer;
                continue;
            }

            // Kiểm tra Lớp / Nhóm / Mã môn
            String classOrGroup = extractClassOrGroup(trimmed);
            if (classOrGroup != null && !classOrGroup.isEmpty()) {
                if (currentCard == null) currentCard = new RawCard(activeDateIso);
                currentCard.className = classOrGroup;
                continue;
            }

            // Nếu không phải là thuộc tính nào ở trên -> Đây chính là TÊN MÔN HỌC / TÊN SỰ KIỆN
            if (isValidSubjectTitle(trimmed)) {
                if (currentCard != null && !currentCard.subjectName.isEmpty()
                        && (currentCard.startPeriod > 0 || !currentCard.room.isEmpty() || !currentCard.timeRangeStr.isEmpty())) {
                    // Đã có 1 thẻ hoàn chỉnh trước đó, commit thẻ cũ và khởi tạo thẻ mới
                    ParsedScheduleEvent ev = convertCardToEvent(currentCard, school, fileName);
                    if (ev != null) resultEvents.add(ev);
                    currentCard = new RawCard(activeDateIso);
                    currentCard.subjectName = cleanSubjectTitle(trimmed);
                } else {
                    if (currentCard == null) currentCard = new RawCard(activeDateIso);
                    currentCard.subjectName = cleanSubjectTitle(trimmed);
                }
            }
        }

        // Commit thẻ cuối cùng nếu hợp lệ
        if (currentCard != null && isValidCard(currentCard)) {
            ParsedScheduleEvent ev = convertCardToEvent(currentCard, school, fileName);
            if (ev != null) resultEvents.add(ev);
        }

        return resultEvents;
    }

    /**
     * Tiền xử lý dòng và gộp các nhãn bị gãy đôi do OCR ngắt dòng (Ví dụ dòng 1: "Tiết:" dòng 2: "1 - 3")
     */
    private static List<String> preprocessAndFoldLines(String fullText) {
        String[] rawLines = fullText.split("\\r?\\n");
        List<String> cleaned = new ArrayList<>();
        for (String r : rawLines) {
            String t = r.replaceAll("\\s+", " ").trim();
            if (!t.isEmpty()) cleaned.add(t);
        }

        List<String> folded = new ArrayList<>();
        int i = 0;
        while (i < cleaned.size()) {
            String line = cleaned.get(i);
            String lower = line.toLowerCase(Locale.getDefault());

            // Gộp nhãn "Tiết:" hoặc "Tiết" với giá trị số ở dòng kế tiếp
            if ((lower.equals("tiết:") || lower.equals("tiết") || lower.equals("tiet:") || lower.equals("tiet"))
                    && i + 1 < cleaned.size() && cleaned.get(i + 1).matches("^\\d{1,2}\\s*[-–đếnvàto.]+\\s*\\d{1,2}.*")) {
                folded.add(line + " " + cleaned.get(i + 1));
                i += 2;
                continue;
            }

            // Gộp nhãn "Phòng:" hoặc "Phòng" với mã phòng ở dòng kế tiếp
            if ((lower.equals("phòng:") || lower.equals("phòng") || lower.equals("phong:") || lower.equals("phong") || lower.equals("room:"))
                    && i + 1 < cleaned.size()) {
                folded.add(line + " " + cleaned.get(i + 1));
                i += 2;
                continue;
            }

            // Gộp nhãn "Giảng viên:" hoặc "GV:" với họ tên ở dòng kế tiếp
            if ((lower.equals("giảng viên:") || lower.equals("giảng viên") || lower.equals("gv:") || lower.equals("cbgd:"))
                    && i + 1 < cleaned.size()) {
                folded.add(line + " " + cleaned.get(i + 1));
                i += 2;
                continue;
            }

            // Gộp thứ ngày bị tách (Ví dụ "Th 4," dòng sau "28/10")
            if (lower.matches("^(?:th(?:ứ)?\\s*[2-7]|cn|chủ\\s*nhật)\\s*[,.]?$")
                    && i + 1 < cleaned.size() && cleaned.get(i + 1).matches("^\\d{1,2}[/.-]\\d{1,2}(?:[/.-]\\d{4})?.*")) {
                folded.add(line + " " + cleaned.get(i + 1));
                i += 2;
                continue;
            }

            folded.add(line);
            i++;
        }
        return folded;
    }

    /**
     * Bóc tách năm toàn cục từ tài liệu (Tìm năm dạng 20xx)
     */
    public static int detectDocumentYear(String fullText) {
        if (fullText == null) return Calendar.getInstance().get(Calendar.YEAR);
        Pattern pYear = Pattern.compile("(?i)(?:năm|tuần\\s*\\d+.*?|hk\\s*\\d+.*?|học\\s*kỳ.*?|năm\\s*học\\s*|\\b)(20[2-3]\\d)\\b");
        Matcher m = pYear.matcher(fullText);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * Kiểm tra và phân tích dòng tiêu đề ngày tháng (Trả về định dạng chuẩn ISO yyyy-MM-dd)
     */
    public static String parseDateHeader(String line, int defaultYear) {
        if (line == null || line.trim().isEmpty()) return null;
        String text = line.trim();

        // 1. Dạng tiếng Việt phổ biến trong app mobile: "Th 4, 28/10", "Thứ 4, 28/10", "Thứ 4, 28/10/2026"
        Matcher m1 = Pattern.compile("(?i)(?:th(?:ứ)?\\s*([2-7])|cn|chủ\\s*nhật)\\s*[,.\\s-]+\\s*(\\d{1,2})[/.-](\\d{1,2})(?:[/.-](\\d{4}))?").matcher(text);
        if (m1.find()) {
            try {
                int d = Integer.parseInt(m1.group(2));
                int m = Integer.parseInt(m1.group(3));
                int y = m1.group(4) != null ? Integer.parseInt(m1.group(4)) : defaultYear;
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                }
            } catch (Exception ignored) {}
        }

        // 2. Dạng "Thứ 4 ngày 28 tháng 10 năm 2026" hoặc "ngày 28 tháng 10"
        Matcher m2 = Pattern.compile("(?i)(?:th(?:ứ)?\\s*[2-7]|cn|chủ\\s*nhật)?\\s*[,.\\s-]*ngày\\s*(\\d{1,2})\\s*tháng\\s*(\\d{1,2})(?:\\s*năm\\s*(\\d{4}))?").matcher(text);
        if (m2.find()) {
            try {
                int d = Integer.parseInt(m2.group(1));
                int m = Integer.parseInt(m2.group(2));
                int y = m2.group(3) != null ? Integer.parseInt(m2.group(3)) : defaultYear;
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                }
            } catch (Exception ignored) {}
        }

        // 3. Dạng chuẩn dd/MM/yyyy hoặc dd-MM-yyyy
        Matcher m3 = Pattern.compile("\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](20\\d{2})\\b").matcher(text);
        if (m3.find()) {
            try {
                int d = Integer.parseInt(m3.group(1));
                int m = Integer.parseInt(m3.group(2));
                int y = Integer.parseInt(m3.group(3));
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                }
            } catch (Exception ignored) {}
        }

        // 4. Dạng chuẩn yyyy-MM-dd
        Matcher m4 = Pattern.compile("\\b(20\\d{2})[/.-](\\d{1,2})[/.-](\\d{1,2})\\b").matcher(text);
        if (m4.find()) {
            try {
                int y = Integer.parseInt(m4.group(1));
                int m = Integer.parseInt(m4.group(2));
                int d = Integer.parseInt(m4.group(3));
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                }
            } catch (Exception ignored) {}
        }

        // 5. Dòng chỉ chứa ngày/tháng ngắn gọn (VD: "28/10") đứng độc lập
        if (text.length() <= 12 && text.matches("^\\s*(\\d{1,2})[/.-](\\d{1,2})\\s*$")) {
            Matcher m5 = Pattern.compile("^\\s*(\\d{1,2})[/.-](\\d{1,2})\\s*$").matcher(text);
            if (m5.find()) {
                try {
                    int d = Integer.parseInt(m5.group(1));
                    int m = Integer.parseInt(m5.group(2));
                    if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                        return String.format(Locale.US, "%04d-%02d-%02d", defaultYear, m, d);
                    }
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    /**
     * Nhận diện và loại trừ triệt để các dòng rác giao diện, nút bấm, tiêu đề báo cáo, điều hướng
     */
    public static boolean isNoiseLine(String line) {
        if (line == null) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;
        String lower = trimmed.toLowerCase(Locale.getDefault());

        // 1. Giờ đồng hồ trên status bar điện thoại (VD: "01:05", "12:30", "9:41")
        if (lower.matches("^\\d{1,2}[:.]\\d{2}$")) return true;

        // 2. Ký hiệu kết nối mạng, pin status bar (VD: "4G", "5G", "Wi-Fi", "84%", "100%")
        if (lower.matches(".*\\b(4g|5g|lte|wifi|wi-fi|battery)\\b.*") || lower.matches("^\\d{1,3}%$")) return true;

        // 3. Tiêu đề hộp thoại phân tích file của chính ứng dụng
        if (lower.contains("xem nội dung & phân tích") || lower.contains("xem noi dung & phan tich")) return true;

        // 4. Thanh chọn Tuần / Ngày / Tháng trên đầu app (VD: "2026 | Tuần 43 | Ngày | Tuần | Tháng")
        if (lower.contains("tuần") && (lower.contains("ngày") || lower.contains("tháng")) && lower.contains("|")) return true;
        if (lower.matches("^(ngày|tuần|tháng|học kỳ|hk[1-3]|tuần\\s*\\d+)$")) return true;

        // 5. Năm đứng riêng lẻ trên thanh chọn năm (VD: "2026")
        if (lower.matches("^20[2-3]\\d$")) return true;

        // 6. Thanh Bottom Navigation của ứng dụng sinh viên/giảng viên (Trang chủ | Lịch học | Điểm danh | Cá nhân)
        if (lower.matches("^(?:trang chủ|lịch học|điểm danh|cá nhân|thông báo|tài khoản|cài đặt)(?:\\s*\\|\\s*(?:trang chủ|lịch học|điểm danh|cá nhân|thông báo|tài khoản|cài đặt))*$")) return true;

        // 7. Các nút bấm hành động trong hộp thoại hoặc app
        if (lower.contains("tải file về máy") || lower.contains("mở bằng app khác") || lower.contains("đóng cửa sổ")
                || lower.equals("đóng") || lower.equals("hủy") || lower.equals("quay lại")
                || lower.equals("chia sẻ") || lower.equals("lưu file") || lower.equals("tải về") || lower.equals("xem chi tiết")) {
            return true;
        }

        // 8. Tiêu đề và chân trang báo cáo xuất từ ứng dụng
        if (lower.startsWith("báo cáo lịch giảng dạy") || lower.startsWith("báo cáo đầy đủ lịch")
                || lower.startsWith("ứng dụng lectuplan") || lower.startsWith("xuất từ ứng dụng")
                || lower.startsWith("giảng viên lập báo cáo") || lower.equals("(ký và ghi rõ họ tên)")
                || lower.startsWith("tổng cộng:") || lower.matches("^(?:stt|tt)$")) {
            return true;
        }

        // 9. Chỉ gồm các ký tự mũi tên, dấu chấm tròn, ký hiệu nổi
        if (trimmed.matches("^[↑↓←→•*+\\-^#_~\\s]+$")) return true;

        return false;
    }

    /**
     * Nhận diện và loại trừ triệt để văn bản hành chính trường học, quy chế chấm điểm, danh bạ thư ký,
     * bảng chuông báo giờ học chuẩn và các dòng ghi chú chính sách không phải là sự kiện lịch biểu.
     */
    public static boolean isInstitutionalBoilerplate(String line) {
        if (line == null) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;
        String lower = trimmed.toLowerCase(Locale.getDefault());

        // 1. Mã biểu mẫu quy trình đào tạo (VD: BM01/QT02/ĐT, BM-P.ĐT/02/07)
        if (lower.matches("(?i)^(?:bm\\d*[-/][a-z0-9_./-]+|qt\\d*[-/][a-z0-9_./-]+).*")) return true;

        // 2. Quốc hiệu, tiêu ngữ, cơ quan quản lý
        if (lower.contains("cộng hòa xã hội chủ nghĩa việt nam")
                || lower.contains("độc lập - tự do - hạnh phúc")
                || lower.contains("bộ giáo dục và đào tạo")
                || lower.contains("bộ lao động - thương binh")) {
            return true;
        }

        // 3. Tên trường, khoa, viện, phòng ban hành chính (nếu không kèm tiết/giờ học cụ thể)
        if (lower.startsWith("trường đại học") || lower.startsWith("khoa ") || lower.startsWith("phòng đào tạo")
                || lower.startsWith("viện ") || lower.startsWith("trung tâm ") || lower.startsWith("ban giám hiệu")
                || lower.startsWith("bộ môn ")) {
            if (!lower.contains("tiết") && !lower.matches(".*\\d{1,2}h\\d{2}.*") && !lower.contains("phòng")) {
                return true;
            }
        }

        // 4. Lời chào, thư mời giảng, cảm ơn
        if (lower.startsWith("thư mời giảng") || lower.startsWith("kính gửi")
                || lower.contains("kính mời thầy/cô") || lower.contains("kinh moi thay/co")
                || lower.startsWith("xin chân thành cảm ơn") || lower.startsWith("trân trọng cảm ơn")) {
            return true;
        }

        // 5. Chức danh ký tên, ban lãnh đạo, địa điểm ký (Lưu ý: Không chặn "Giảng viên: [Tên]" vì đây là thuộc tính của thẻ môn học)
        if (lower.matches("^(?:trưởng khoa|phó trưởng khoa|hiệu trưởng|phó hiệu trưởng|trưởng phòng)(?:\\s*:.*)?$")
                || lower.matches("^(?:giảng viên|giảng viên lập biểu|giảng viên lập báo cáo|người lập biểu|người lập bảng)$")
                || lower.matches("^(?:pgs\\.?ts\\.?|ts\\.?|ths\\.?)\\s+[a-zà-ỹ\\s]+$")
                || lower.matches("^(?:tp\\.?\\s*hcm|hà nội|đà nẵng|cần thơ)\\s*,?\\s*ngày\\s*\\d{1,2}\\s*tháng\\s*\\d{1,2}\\s*năm\\s*\\d{4}$")) {
            return true;
        }

        // 6. Tiêu đề các bảng biểu hành chính (kể cả dạng phân mảnh theo cột)
        if (lower.matches("^(?:tt|stt)\\s+nhóm(?:\\s+tổ.*)?$")
                || lower.matches("^(?:mã\\s+mh\\s+môn|mã\\s+mh|tên\\s+lớp|ngày\\s+học|phòng\\s+tên\\s+lớp.*)$")
                || lower.matches("^(?:buổi\\s+)?ca\\s+tiết\\s+giờ\\s+học$")
                || lower.matches("^nội dung\\s+trọng số$")
                || lower.matches("^công việc\\s+phụ trách$")) {
            return true;
        }

        // 7. Bảng chuông báo giờ học chuẩn của trường (Sáng / Chiều / Tối, Ca 1..5, Tiết 1..15 kèm giờ)
        if (lower.matches("^(?:sáng|chiều|tối)(?:\\s*\\d+)?$") || lower.matches("^ca\\s*[1-5]$")
                || lower.matches("^tiết$")
                || lower.matches("^\\d{1,2}\\s+\\d{1,2}h\\d{2}\\s*[-–]\\s*\\d{1,2}h\\d{2}$")) {
            return true;
        }

        // 8. Mô tả cơ sở khuôn viên trường (Campus)
        if (lower.startsWith("• địa điểm:") || lower.startsWith("địa điểm:")
                || lower.matches("^\\+\\s*[a-z0-9\\s.-]+campus\\s*:.*")) {
            return true;
        }

        // 9. Khối thông báo ngày nghỉ lễ hoặc quy định đợt học
        if (lower.startsWith("• các ngày nghỉ lễ") || lower.startsWith("các ngày nghỉ lễ")
                || lower.startsWith("+ ngày giải phóng") || lower.startsWith("+ ngày giỗ tổ")
                || lower.matches("^\\+\\s*đợt\\s+[a-z0-9]+:?\\s*bắt đầu học từ tuần.*")
                || lower.matches("^(?:thời gian:|giờ học:)$")) {
            return true;
        }

        // 10. Quy chế chấm điểm, đánh giá, ghi chú học phần
        if (lower.startsWith("• ghi chú") || lower.startsWith("ghi chú:") || lower.matches("^lưu\\s*ý:?$")
                || lower.contains("điểm học phần được qui định") || lower.contains("điểm học phần được quy định")
                || lower.contains("đối với các học phần") || lower.contains("điểm quá trình")
                || lower.contains("điểm thi kết thúc học phần") || lower.contains("các điểm đánh giá này")
                || lower.contains("điểm trung bình cộng") || lower.contains("thang điểm 10")
                || lower.contains("quy định trong đề cương") || lower.contains("phê duyệt và được quy định")
                || lower.contains("sinh viên có quyền khiếu nại") || lower.contains("khiếu nại về điểm")
                || lower.startsWith("- điểm") || lower.startsWith("- giảng viên công bố")
                || lower.contains("tài liệu hay không")
                || lower.matches("^\\d{1,2}%$")
                || lower.matches("^\\d+\\.\\s*gv\\s*nộp\\s*đề\\s*thi.*")) {
            return true;
        }

        // 11. Danh bạ liên hệ, thư ký khoa, hotline
        if (lower.startsWith("văn phòng khoa") || lower.startsWith("điện thoại liên hệ:")
                || lower.startsWith("thư ký phụ trách:") || lower.contains("thời khóa biểu, thù lao")
                || lower.contains("học vụ, điểm") || lower.contains("hợp đồng giảng dạy")
                || lower.contains("công tác sinh viên")
                || (lower.contains("đt:") && lower.contains("email:"))) {
            return true;
        }

        return false;
    }

    /**
     * Bóc tách huy hiệu phân loại lịch (VD: "• Lịch học" -> "Lịch học", "• Lịch trực tuyến" -> "Lịch trực tuyến")
     */
    public static String extractCategoryBadge(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        Matcher m = Pattern.compile("(?i)^[•*\\-·]?\\s*(lịch\\s*(?:học|trực\\s*tuyến|thi|bù|sự\\s*kiện|thực\\s*hành|kiểm\\s*tra|giảng\\s*dạy)|online|trực\\s*tiếp)\\s*$").matcher(trimmed);
        if (m.find()) {
            String b = m.group(1).trim();
            // Viết hoa chữ cái đầu cho đẹp
            return Character.toUpperCase(b.charAt(0)) + b.substring(1);
        }
        return null;
    }

    public static class PeriodRange {
        public int start;
        public int end;
        public PeriodRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Bóc tách Tiết học: hỗ trợ "Tiết: 1 - 3", "Tiết 4-6", "Tiết: 13 - 13", "Ca 1", "Ca 2"
     */
    public static PeriodRange extractPeriodRange(String text) {
        if (text == null) return null;

        // Dạng 1: "Tiết: 1 - 3", "Tiết: 13 - 13", "Tiết 4 - 6"
        Matcher m1 = Pattern.compile("(?i)(?<![a-zA-ZÀ-ỹ0-9_])(?<!chi\\s?)(?<!tình\\s?)\\btiết\\s*[:=\\s-]*(\\d{1,2})\\s*[-–đếnvàto.]+\\s*(\\d{1,2})\\b").matcher(text);
        if (m1.find()) {
            try {
                int p1 = Integer.parseInt(m1.group(1));
                int p2 = Integer.parseInt(m1.group(2));
                if (p2 >= p1 && p1 >= 1 && p2 <= 20) {
                    return new PeriodRange(p1, p2);
                }
            } catch (Exception ignored) {}
        }

        // Dạng 2: "Ca 1", "Ca 2", "Ca 3", "Ca 4", "Ca 5"
        Matcher mCa = Pattern.compile("(?i)\\bca\\s*[:=\\s-]*([1-5])\\b").matcher(text);
        if (mCa.find()) {
            try {
                int ca = Integer.parseInt(mCa.group(1));
                int start = (ca - 1) * 3 + 1;
                return new PeriodRange(start, start + 2);
            } catch (Exception ignored) {}
        }

        // Dạng 3: Tiết đơn "Tiết: 13"
        Matcher mSingle = Pattern.compile("(?i)(?<![a-zA-ZÀ-ỹ0-9_])(?<!chi\\s?)\\btiết\\s*[:=\\s-]*(\\d{1,2})\\b").matcher(text);
        if (mSingle.find()) {
            try {
                int p = Integer.parseInt(mSingle.group(1));
                if (p >= 1 && p <= 20) {
                    return new PeriodRange(p, p);
                }
            } catch (Exception ignored) {}
        }

        // Dạng 4: Hàng bảng số không chứa từ "Tiết": (Thứ) (Tiết BĐ) (Số tiết) (Phòng)
        Matcher mBare = Pattern.compile("(?i)(?:thứ\\s*[2-7]|cn|\\b[2-7]\\b)\\s+(\\d{1,2})\\s+(\\d{1,2})\\b").matcher(text);
        if (mBare.find() && mBare.group(1) != null && mBare.group(2) != null) {
            try {
                int pStart = Integer.parseInt(mBare.group(1));
                int count = Integer.parseInt(mBare.group(2));
                if (pStart >= 1 && pStart <= 16 && count >= 1 && count <= 10) {
                    return new PeriodRange(pStart, pStart + count - 1);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Bóc tách giờ trực tiếp (VD: "07:00 - 09:30", "13h00 - 15h30")
     */
    public static String extractTimeRange(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)\\b(\\d{1,2})[h:g:](\\d{0,2})\\s*[-–đếnto]+\\s*(\\d{1,2})[h:g:](\\d{0,2})\\b").matcher(text);
        if (m.find()) {
            try {
                int h1 = Integer.parseInt(m.group(1));
                int m1 = m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
                int h2 = Integer.parseInt(m.group(3));
                int m2 = m.group(4).isEmpty() ? 0 : Integer.parseInt(m.group(4));
                return String.format(Locale.US, "%02d:%02d - %02d:%02d", h1, m1, h2, m2);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Bóc tách Phòng học (VD: "Phòng: L.503", "Phòng: L2.123", "Phòng: E-LEARNING 15")
     */
    public static String extractRoom(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)(?:phòng|room|địa\\s*điểm|giảng\\s*đường)\\s*[:=\\s-]*\\s*([a-zA-ZÀ-ỹ0-9/._-]+(?:\\s+[a-zA-ZÀ-ỹ0-9/._-]+)*)").matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Bóc tách Giảng viên (VD: "Giảng viên: Đặng Như Phú", "GV: Lý Quang Vinh")
     */
    public static String extractLecturer(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)(?:giảng\\s*viên|gv|cbgd|thầy/cô|thầy|cô|người\\s*hướng\\s*dẫn)\\s*[:=\\s-]*\\s*([a-zA-ZÀ-ỹ\\s.'-]+)").matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Bóc tách Lớp / Nhóm
     */
    public static String extractClassOrGroup(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?i)(?:lớp|nhóm|tổ|mã\\s*hp|mã\\s*môn|sĩ\\s*số)\\s*[:=\\s-]*\\s*([a-zA-Z0-9/._-]+)").matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static boolean containsPeriodIndicator(String text) {
        return text != null && text.toLowerCase(Locale.getDefault()).matches(".*(?<!chi\\s?)\\btiết.*");
    }

    private static boolean containsRoomIndicator(String text) {
        return text != null && text.toLowerCase(Locale.getDefault()).matches(".*\\b(phòng|room).*");
    }

    private static boolean containsLecturerIndicator(String text) {
        return text != null && text.toLowerCase(Locale.getDefault()).matches(".*\\b(giảng viên|gv|cbgd).*");
    }

    /**
     * Xử lý một dòng phân cách bằng '|' chứa nhiều thông tin của 1 thẻ
     */
    private static RawCard parsePipeDelimitedCard(String line, String activeDateIso) {
        String[] parts = line.split("\\|");
        RawCard card = new RawCard(activeDateIso);

        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty() || isNoiseLine(p)) continue;

            String badge = extractCategoryBadge(p);
            if (badge != null) {
                card.categoryBadge = badge;
                continue;
            }

            PeriodRange pr = extractPeriodRange(p);
            if (pr != null) {
                card.startPeriod = pr.start;
                card.endPeriod = pr.end;
                continue;
            }

            String tr = extractTimeRange(p);
            if (tr != null) {
                card.timeRangeStr = tr;
                continue;
            }

            String rm = extractRoom(p);
            if (rm != null) {
                card.room = rm;
                continue;
            }

            String lec = extractLecturer(p);
            if (lec != null) {
                card.lecturer = lec;
                continue;
            }

            String cls = extractClassOrGroup(p);
            if (cls != null) {
                card.className = cls;
                continue;
            }

            if (isValidSubjectTitle(p) && card.subjectName.isEmpty()) {
                card.subjectName = cleanSubjectTitle(p);
            }
        }
        return card;
    }

    private static boolean isValidSubjectTitle(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.length() < 3) return false;
        if (isNoiseLine(t) || isInstitutionalBoilerplate(t)) return false;
        if (extractCategoryBadge(t) != null) return false;
        if (extractPeriodRange(t) != null && t.toLowerCase(Locale.getDefault()).startsWith("tiết")) return false;
        if (extractRoom(t) != null && t.toLowerCase(Locale.getDefault()).startsWith("phòng")) return false;
        if (extractLecturer(t) != null && t.toLowerCase(Locale.getDefault()).startsWith("giảng viên")) return false;

        String lower = t.toLowerCase(Locale.getDefault());
        if (lower.matches("^\\d{1,2}[/.-]\\d{1,2}(?:[/.-]\\d{2,4})?$")) return false;
        if (lower.matches("^(?:th(?:ứ)?\\s*[2-7]|cn|chủ\\s*nhật).*")) return false;

        return true;
    }

    private static String cleanSubjectTitle(String text) {
        if (text == null) return "";
        String s = text.replaceAll("^[•*\\-·]+\\s*", "").trim();
        return ThumoiGiangParser.healVietnameseText(s);
    }

    private static boolean isValidCard(RawCard card) {
        if (card == null) return false;
        if (card.subjectName == null || card.subjectName.trim().length() < 3) return false;
        if (isNoiseLine(card.subjectName) || isInstitutionalBoilerplate(card.subjectName)) return false;
        if (extractCategoryBadge(card.subjectName) != null) return false;

        // Thẻ phải có ít nhất một thông số thời gian, phòng hoặc giảng viên
        return card.startPeriod > 0
                || !card.timeRangeStr.isEmpty()
                || !card.room.isEmpty()
                || !card.lecturer.isEmpty()
                || !card.categoryBadge.isEmpty();
    }

    /**
     * Chuyển đổi RawCard thành ParsedScheduleEvent hoàn chỉnh với đầy đủ tiêu đề, phòng, tiết, giờ và người tổ chức
     */
    private static ParsedScheduleEvent convertCardToEvent(RawCard card, SchoolProfile.SchoolType school, String fileName) {
        if (!isValidCard(card)) return null;

        ParsedScheduleEvent ev = new ParsedScheduleEvent();
        ev.school = school;
        ev.dateIso = card.dateIso;
        ev.subjectName = card.subjectName;
        ev.room = card.room;
        ev.categoryBadge = card.categoryBadge;
        ev.lecturer = card.lecturer;
        ev.className = card.className;
        ev.startPeriod = card.startPeriod;
        ev.endPeriod = card.endPeriod;
        ev.periodCount = (card.endPeriod >= card.startPeriod && card.startPeriod > 0)
                ? (card.endPeriod - card.startPeriod + 1) : 0;

        // 1. Tính toán chuẩn hóa khung giờ học
        if (card.startPeriod > 0) {
            TimeNormalizer.NormalizedRange nr = TimeNormalizer.fromPeriodRange(school, card.startPeriod, ev.periodCount);
            ev.timeRangeStr = nr.startTimeStr + " - " + nr.endTimeStr;
            ev.periodStr = card.startPeriod + "-" + card.endPeriod;
        } else if (!card.timeRangeStr.isEmpty()) {
            ev.timeRangeStr = card.timeRangeStr;
        }

        // 2. Tạo tiêu đề giàu thông tin chuẩn của LectuPlan
        StringBuilder titleSb = new StringBuilder();
        titleSb.append(card.subjectName);
        if (!card.className.isEmpty()) {
            titleSb.append(" (").append(card.className).append(")");
        }
        if (!card.room.isEmpty()) {
            titleSb.append(" - Phòng ").append(card.room);
        }
        if (card.startPeriod > 0) {
            titleSb.append(" | Tiết ").append(card.startPeriod).append("-").append(card.endPeriod);
        }
        if (!ev.timeRangeStr.isEmpty()) {
            titleSb.append(" [").append(ev.timeRangeStr).append("]");
        }
        ev.title = titleSb.toString();

        // 3. Tạo thông tin người tổ chức / Giảng viên
        StringBuilder orgSb = new StringBuilder();
        if (school != SchoolProfile.SchoolType.DEFAULT) {
            orgSb.append("[").append(school.getCode()).append("] ");
        }
        if (!card.lecturer.isEmpty()) {
            orgSb.append(card.lecturer);
            if (!card.categoryBadge.isEmpty()) {
                orgSb.append(" (").append(card.categoryBadge).append(")");
            }
        } else if (!card.categoryBadge.isEmpty()) {
            orgSb.append(card.categoryBadge);
        } else {
            orgSb.append(EventModel.CAT_TEACHING);
        }
        if (fileName != null && !fileName.trim().isEmpty() && !orgSb.toString().contains(fileName.trim())) {
            orgSb.append(" • ").append(fileName.trim());
        }
        ev.organizer = orgSb.toString();

        return ev;
    }
}

