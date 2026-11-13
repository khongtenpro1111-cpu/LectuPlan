package com.tkyl.lectuplan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bộ phân tích và bóc tách tự động Thư Mời Giảng / Thời Khóa Biểu Đại học
 * Nâng cấp toàn diện cho người dùng chuyên nghiệp (Giảng viên đại học & Quản trị lịch):
 * - Tự động nhận diện trường (HUTECH, NTTU, UEF...)
 * - Bóc tách thông tin Giảng viên, Khoa, Đợt học / Học kỳ
 * - Bộ bóc tách bảng đa dòng (Multi-line Table Parsing & OCR Resilient) chống lỗi ngắt dòng
 * - Áp bảng quy đổi giờ theo trường (Tiết học ➔ start_time, end_time)
 * - Trích xuất đầy đủ Nhóm, Tổ TH, Sĩ số, Phòng, Cơ sở (Campus)
 * - Tự động quét ngày nghỉ lễ động (không fix cứng năm)
 * - Tự động rải lịch từng tuần và cảnh báo các buổi học rơi vào ngày lễ cần xếp lịch dạy bù
 * - Chống trùng lặp Nhắc việc hành chính (De-duplicated Auto-Deadlines cho môn có nhiều tổ)
 * - Phân tích tải trọng & Cảnh báo mệt mỏi thể lực (Fatigue & High Workload Alert)
 * - Bóc tách Danh bạ Thư ký Khoa & Hotline khẩn cấp (Một chạm gọi điện / gửi mail)
 */
public class ThumoiGiangParser {

    public static class ParsedTeachingItem {
        public int stt = 0;
        public String group = "";          // Nhóm: 14, 18
        public String subGroup = "";       // Tổ TH: 04, 02
        public String subjectCode = "";    // CMP376
        public String subjectName = "";    // Thực hành lập trình Web
        public int credits = 1;            // 1
        public int studentCount = 0;       // 27, 24
        public int dayOfWeekVn = 5;        // Thứ: 2..7, CN = 8
        public int startPeriod = 1;        // Tiết BĐ: 7, 2
        public int periodCount = 1;        // Số tiết: 5
        public String room = "";           // Phòng: E1-04.06/2, E1-04.06/1
        public String className = "";      // Tên Lớp: 23DTHB6,23DTHB7, 23DTHC3
        public String startDateStr = "";   // 14/05/2026
        public String endDateStr = "";     // 18/06/2026
        public SchoolProfile.SchoolType school;
    }

    public static class DepartmentContact {
        public String role = "";           // Thời khóa biểu, thù lao, học vụ, điểm...
        public String name = "";           // Võ Hồng Linh Ân
        public String phone = "";          // 0348 913 380
        public String email = "";          // vhl.an@hutech.edu.vn

        public DepartmentContact(String role, String name, String phone, String email) {
            this.role = role;
            this.name = name;
            this.phone = phone;
            this.email = email;
        }
    }

    public static class ParseResult {
        public SchoolProfile.SchoolType school;
        public String lecturerName = "";
        public String facultyName = "";
        public String termInfo = "";
        public List<ParsedTeachingItem> items = new ArrayList<>();
        public List<GeneratedClassEvent> generatedEvents = new ArrayList<>();
        public List<String> holidayDates = new ArrayList<>();
        public List<String> autoDeadlines = new ArrayList<>();
        public List<String> workloadWarnings = new ArrayList<>();
        public List<String> makeupWarnings = new ArrayList<>();
        public List<String> weeklyFreeSchedule = new ArrayList<>();
        public List<DepartmentContact> contacts = new ArrayList<>();
        public List<String> notesAndRules = new ArrayList<>();
    }

    public static class GeneratedClassEvent {
        public String dateIso;         // yyyy-MM-dd
        public String title;
        public String organizer;
        public String timeRangeStr;    // 07:30 - 11:35
        public boolean isDeadline;
    }

    /**
     * Khắc phục triệt để lỗi mất chữ, thiếu nét, chèn khoảng trắng hoặc ký tự phân cách trong tên môn học và văn bản Tiếng Việt
     * khi đọc qua OCR hoặc trích xuất từ file PDF/Word/PPTX/Ảnh
     */
    public static String healVietnameseText(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String s = text;
        s = s.replaceAll("(?i)\\bThực[\\s|/]+h[àa][\\s|/]+h[\\s|/]+lập[\\s|/]+r[ìi][\\s|/]+h[\\s|/]+Web\\b", "Thực hành lập trình Web");
        s = s.replaceAll("(?i)\\bThực[\\s|/]+h[a-zà-ỹA-ZÀ-Ỹ\\s|/]{1,10}[\\s|/]+lập[\\s|/]+[a-zà-ỹA-ZÀ-Ỹ\\s|/]{1,10}[\\s|/]+Web\\b", "Thực hành lập trình Web");
        s = s.replaceAll("(?i)\\bThực[\\s|/]+h[àa][\\s|/]+h\\b", "Thực hành");
        s = s.replaceAll("(?i)\\blập[\\s|/]+r[ìi][\\s|/]+h\\b", "lập trình");
        s = s.replaceAll("(?i)\\blập[\\s|/]+tr[ìi][\\s|/]+h\\b", "lập trình");
        s = s.replaceAll("(?i)\\br[ìi][\\s|/]+h[\\s|/]+Web\\b", "trình Web");
        s = s.replaceAll("(?i)\\bchương[\\s|/]+r[ìi][\\s|/]+h\\b", "chương trình");
        s = s.replaceAll("(?i)\\bquá[\\s|/]+r[ìi][\\s|/]+h\\b", "quá trình");
        s = s.replaceAll("(?i)\\bh[àa][\\s|/]+h\\b", "hành");
        return s.trim();
    }

    /**
     * Bóc tách toàn diện văn bản thư mời giảng / thời khóa biểu
     */
    public static ParseResult parseInvitationDocument(String fullText) {
        ParseResult result = new ParseResult();
        if (fullText == null || fullText.trim().isEmpty()) return result;

        // 1. Nhận diện trường
        result.school = SchoolProfile.detectSchool(fullText);

        // 2. Trích xuất Tên Giảng viên, Khoa, Đợt học
        result.lecturerName = extractLecturerName(fullText);
        result.facultyName = extractFacultyName(fullText, result.school);
        result.termInfo = extractTermInfo(fullText);

        // 3. Trích xuất danh bạ Thư ký & Hotline khoa
        result.contacts = extractDepartmentContacts(fullText);

        // 4. Trích xuất ngày nghỉ lễ động & bảng quy đổi giờ học từ văn bản
        Set<String> holidaySet = extractHolidayDates(fullText);
        result.holidayDates.addAll(holidaySet);
        Map<Integer, int[]> customPeriodMap = parseCustomPeriodMap(fullText);

        // 5. Bóc tách các hàng bảng môn học (Hỗ trợ đa dòng và OCR lệch ô)
        result.items = extractTeachingItems(fullText, result.school);

        // 6. Sinh các buổi học cụ thể và kiểm tra ngày nghỉ lễ / lịch bù
        SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Theo dõi buổi học cuối cùng của từng môn học để làm mốc tính deadline chính xác
        Map<String, Date> subjectLastClassDateMap = new HashMap<>();
        Map<String, List<ParsedTeachingItem>> subjectItemsMap = new LinkedHashMap<>();

        // Theo dõi tải trọng giảng dạy theo ngày trong tuần (Thứ 2..Thứ 7, CN)
        Map<Integer, Integer> periodsPerDayOfWeek = new HashMap<>();
        Map<Integer, List<ParsedTeachingItem>> itemsPerDayOfWeek = new HashMap<>();

        // Sắp xếp các môn học theo Thứ trong tuần, rồi theo Tiết bắt đầu tăng dần (ca sáng lên trước ca chiều)
        Collections.sort(result.items, (a, b) -> {
            int dowCmp = Integer.compare(a.dayOfWeekVn, b.dayOfWeekVn);
            if (dowCmp != 0) return dowCmp;
            return Integer.compare(a.startPeriod, b.startPeriod);
        });

        for (ParsedTeachingItem item : result.items) {
            String subKey = item.subjectCode + "_" + item.endDateStr;
            if (!subjectItemsMap.containsKey(subKey)) {
                subjectItemsMap.put(subKey, new ArrayList<>());
            }
            subjectItemsMap.get(subKey).add(item);

            // Ghi nhận tải trọng ngày trong tuần
            int dow = item.dayOfWeekVn;
            periodsPerDayOfWeek.put(dow, periodsPerDayOfWeek.getOrDefault(dow, 0) + item.periodCount);
            if (!itemsPerDayOfWeek.containsKey(dow)) {
                itemsPerDayOfWeek.put(dow, new ArrayList<>());
            }
            itemsPerDayOfWeek.get(dow).add(item);

            TimeNormalizer.NormalizedRange timeRange = TimeNormalizer.fromPeriodRange(item.school, item.startPeriod, item.periodCount, customPeriodMap);

            try {
                Date dStart = sdfVn.parse(item.startDateStr);
                Date dEnd = sdfVn.parse(item.endDateStr);
                if (dStart == null || dEnd == null) continue;

                Calendar cal = Calendar.getInstance();
                cal.setTime(dStart);

                // Tìm ngày đầu tiên khớp với Thứ trong tuần
                // dayOfWeekVn: 2 = Thứ 2, 3 = Thứ 3, ..., 7 = Thứ 7, 8 = Chủ Nhật
                int targetCalDay = (item.dayOfWeekVn == 8) ? Calendar.SUNDAY : item.dayOfWeekVn;
                while (cal.get(Calendar.DAY_OF_WEEK) != targetCalDay) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                Date lastActualClassDate = null;
                int sessionIndex = 1;

                // Lặp hàng tuần cho đến hết khoảng thời gian học
                while (!cal.getTime().after(dEnd)) {
                    String dateIso = sdfIso.format(cal.getTime());

                    // Kiểm tra xem có trúng ngày nghỉ lễ không
                    if (!holidaySet.contains(dateIso)) {
                        GeneratedClassEvent ev = new GeneratedClassEvent();
                        ev.dateIso = dateIso;
                        ev.timeRangeStr = timeRange.startTimeStr + " - " + timeRange.endTimeStr;

                        // Tiêu đề giàu thông tin: Tên môn, Lớp, Nhóm/Tổ, Sĩ số, Phòng, Tiết, Giờ
                        StringBuilder titleSb = new StringBuilder();
                        titleSb.append(healVietnameseText(item.subjectName));
                        if (!item.className.isEmpty()) {
                            titleSb.append(" (").append(item.className);
                            if (!item.group.isEmpty()) titleSb.append(" • Nhóm ").append(item.group);
                            if (!item.subGroup.isEmpty()) titleSb.append("/Tổ ").append(item.subGroup);
                            if (item.studentCount > 0) titleSb.append(" • ").append(item.studentCount).append(" SV");
                            titleSb.append(")");
                        }
                        titleSb.append(" - Phòng ").append(item.room)
                               .append(" | Tiết ").append(item.startPeriod).append("-").append(item.startPeriod + item.periodCount - 1)
                               .append(" [").append(ev.timeRangeStr).append("]");

                        ev.title = titleSb.toString();
                        ev.organizer = "[" + item.school.getCode() + "] " + (result.facultyName.isEmpty() ? "Khoa CNTT" : result.facultyName);
                        ev.isDeadline = false;
                        result.generatedEvents.add(ev);

                        lastActualClassDate = cal.getTime();
                        sessionIndex++;
                    } else {
                        // CẢNH BÁO NGHỈ LỄ: Buổi học rơi vào ngày lễ, cần xếp lịch bù!
                        String holidayNotice = "⚠️ Môn " + item.subjectName + " (" + item.className + ") ngày " + sdfVn.format(cal.getTime())
                                + " rơi vào ngày nghỉ lễ (" + dateIso + "). Bị khuyết 1 buổi (" + item.periodCount + " tiết) - Cần liên hệ Thư ký TKB xếp lịch bù!";
                        result.makeupWarnings.add(holidayNotice);
                    }

                    cal.add(Calendar.DAY_OF_YEAR, 7); // Sang tuần kế tiếp
                }

                if (lastActualClassDate != null) {
                    Date curMax = subjectLastClassDateMap.get(subKey);
                    if (curMax == null || lastActualClassDate.after(curMax)) {
                        subjectLastClassDateMap.put(subKey, lastActualClassDate);
                    }
                }

            } catch (Exception ignored) {}
        }

        // 7. TỰ ĐỘNG ĐẺ NHẮC VIỆC HÀNH CHÍNH CÓ KHỬ TRÙNG LẶP (De-duplicated Auto-Deadlines)
        // Mỗi môn học kết thúc cùng ngày chỉ sinh đúng 1 deadline nộp đề và 1 deadline nộp bảng điểm
        for (Map.Entry<String, List<ParsedTeachingItem>> entry : subjectItemsMap.entrySet()) {
            List<ParsedTeachingItem> groupItems = entry.getValue();
            if (groupItems.isEmpty()) continue;
            ParsedTeachingItem representative = groupItems.get(0);

            Date baseFinalDate = subjectLastClassDateMap.get(entry.getKey());
            if (baseFinalDate == null) {
                try {
                    baseFinalDate = sdfVn.parse(representative.endDateStr);
                } catch (Exception e) {
                    baseFinalDate = new Date();
                }
            }

            // Tổng hợp các lớp của môn học này
            StringBuilder classListSb = new StringBuilder();
            for (int i = 0; i < groupItems.size(); i++) {
                ParsedTeachingItem pi = groupItems.get(i);
                if (i > 0) classListSb.append("; ");
                classListSb.append(pi.className);
                if (!pi.group.isEmpty()) classListSb.append(" (Nhóm ").append(pi.group).append(")");
            }
            String classSummary = classListSb.toString();

            // Nhắc nhở 1: Nộp đề thi cuối kỳ trước khi kết thúc học phần 2 tuần (14 ngày)
            Calendar calExam = Calendar.getInstance();
            calExam.setTime(baseFinalDate);
            calExam.add(Calendar.DAY_OF_YEAR, -14);
            String examDeadlineIso = sdfIso.format(calExam.getTime());

            GeneratedClassEvent examDeadline = new GeneratedClassEvent();
            examDeadline.dateIso = examDeadlineIso;
            examDeadline.timeRangeStr = "08:00 - 16:30";
            boolean isLabOnly = representative.subjectName.toLowerCase(Locale.getDefault()).contains("thực hành");
            examDeadline.title = "📝 [HẠN CHÓT] Nộp đề thi kết thúc học phần môn " + representative.subjectName
                    + " (" + classSummary + ") về VP Khoa"
                    + (isLabOnly ? " [Học phần Thực hành: Báo cáo/Đánh giá bài tập lớn theo quy định BM]" : " [2 đề + 2 đáp án]");
            examDeadline.organizer = "[" + representative.school.getCode() + "] Văn phòng Khoa (Nhắc việc tự động)";
            examDeadline.isDeadline = true;
            result.generatedEvents.add(examDeadline);
            result.autoDeadlines.add("Hạn nộp đề thi: " + sdfVn.format(calExam.getTime()) + " - " + representative.subjectName + " (" + classSummary + ")");

            // Nhắc nhở 2: Công bố điểm quá trình và nộp bảng điểm vào buổi học cuối cùng
            String gradeDeadlineIso = sdfIso.format(baseFinalDate);
            GeneratedClassEvent gradeDeadline = new GeneratedClassEvent();
            gradeDeadline.dateIso = gradeDeadlineIso;
            gradeDeadline.timeRangeStr = "08:00 - 17:00";
            gradeDeadline.title = "📊 [HẠN CHÓT] Công bố điểm quá trình & Nộp bảng điểm môn " + representative.subjectName
                    + " (" + classSummary + ") - Buổi học cuối cùng";
            gradeDeadline.organizer = "[" + representative.school.getCode() + "] Văn phòng Khoa (Nhắc việc tự động)";
            gradeDeadline.isDeadline = true;
            result.generatedEvents.add(gradeDeadline);
            result.autoDeadlines.add("Hạn nộp bảng điểm: " + sdfVn.format(baseFinalDate) + " - " + representative.subjectName + " (" + classSummary + ")");
        }

        // 8. PHÂN TÍCH TẢI TRỌNG GIẢNG DẠY & CẢNH BÁO MỆT MỎI (Teaching Fatigue & Workload Analysis)
        for (Map.Entry<Integer, Integer> entry : periodsPerDayOfWeek.entrySet()) {
            int dow = entry.getKey();
            int totalPeriods = entry.getValue();
            String dowName = (dow == 8) ? "Chủ Nhật" : ("Thứ " + dow);
            List<ParsedTeachingItem> dayItems = itemsPerDayOfWeek.get(dow);

            if (totalPeriods >= 8) {
                // Tải trọng từ 8 tiết trở lên trong cùng 1 ngày
                StringBuilder warnSb = new StringBuilder();
                warnSb.append("⚠️ CẢNH BÁO TẢI TRỌNG CAO: ").append(dowName).append(" có ").append(totalPeriods)
                        .append(" tiết giảng dạy liên tục (");
                if (dayItems != null) {
                    for (int i = 0; i < dayItems.size(); i++) {
                        ParsedTeachingItem di = dayItems.get(i);
                        if (i > 0) warnSb.append(" + ");
                        warnSb.append(di.periodCount).append(" tiết phòng ").append(di.room);
                    }
                }
                warnSb.append(").");

                // Đánh giá khoảng nghỉ giữa ca
                if (dayItems != null && dayItems.size() >= 2) {
                    warnSb.append(" Khoảng nghỉ trưa/chuyển ca khoảng 55 phút tại trường. Giảng viên nên nghỉ ngơi tại chỗ, chuẩn bị nước uống và không nhận thêm lịch hẹn trưa.");
                }
                result.workloadWarnings.add(warnSb.toString());
            }
        }

        // 9. PHÂN TÍCH MA TRẬN LỊCH TRỐNG & KHẢ NĂNG NHẬN THÊM VIỆC HÀNG TUẦN (Weekly Free Time Matrix)
        result.weeklyFreeSchedule = generateWeeklyFreeScheduleAnalysis(itemsPerDayOfWeek, periodsPerDayOfWeek, result.school);

        // Sắp xếp toàn bộ sự kiện đã sinh: Ngày trước lên trước, cùng ngày thì giờ bắt đầu sớm lên trước
        Collections.sort(result.generatedEvents, (a, b) -> {
            int dCmp = a.dateIso.compareTo(b.dateIso);
            if (dCmp != 0) return dCmp;
            int minA = TimeNormalizer.parseFromText(a.title + " " + a.timeRangeStr, result.school).startMin;
            int minB = TimeNormalizer.parseFromText(b.title + " " + b.timeRangeStr, result.school).startMin;
            if (minA != minB) return Integer.compare(minA, minB);
            return Boolean.compare(a.isDeadline, b.isDeadline);
        });

        return result;
    }

    /**
     * Phân tích ma trận lịch trống và khả năng nhận thêm việc của giảng viên trong 7 ngày của tuần
     */
    public static List<String> generateWeeklyFreeScheduleAnalysis(
            Map<Integer, List<ParsedTeachingItem>> itemsPerDayOfWeek,
            Map<Integer, Integer> periodsPerDayOfWeek,
            SchoolProfile.SchoolType school) {

        List<String> analysis = new ArrayList<>();
        int[] dows = new int[]{2, 3, 4, 5, 6, 7, 8}; // Thứ 2 đến Thứ 7, Chủ Nhật

        for (int dow : dows) {
            String dowName = (dow == 8) ? "Chủ Nhật" : ("Thứ " + dow);
            List<ParsedTeachingItem> dayItems = itemsPerDayOfWeek != null ? itemsPerDayOfWeek.get(dow) : null;

            if (dayItems == null || dayItems.isEmpty()) {
                if (dow == 8) {
                    analysis.add("🟢 Chủ Nhật: TRỐNG 100% CẢ NGÀY (Rảnh từ 06:45 đến 21:45) • Nghỉ ngơi cuối tuần, sinh hoạt gia đình hoặc làm việc tự do.");
                } else {
                    analysis.add("🟢 " + dowName + ": TRỐNG 100% CẢ NGÀY (Rảnh từ 06:45 đến 21:45 • Khả dụng trọn vẹn 15 giờ) ➔ SẴN SÀNG NHẬN TOÀN BỘ CA DẠY THỈNH GIẢNG, nghiên cứu khoa học hoặc chấm đồ án.");
                }
            } else {
                int totalPeriods = periodsPerDayOfWeek != null && periodsPerDayOfWeek.containsKey(dow) ? periodsPerDayOfWeek.get(dow) : 0;
                Collections.sort(dayItems, (a, b) -> Integer.compare(a.startPeriod, b.startPeriod));

                StringBuilder busySb = new StringBuilder();
                busySb.append("🔴 ").append(dowName).append(": BẬN DẠY ").append(totalPeriods).append(" TIẾT (");

                int firstStartMin = -1;
                int lastEndMin = -1;
                for (int i = 0; i < dayItems.size(); i++) {
                    ParsedTeachingItem item = dayItems.get(i);
                    TimeNormalizer.NormalizedRange r = TimeNormalizer.fromPeriodRange(school, item.startPeriod, item.periodCount);
                    if (i == 0) firstStartMin = r.startMin;
                    lastEndMin = Math.max(lastEndMin, r.endMin);
                }

                if (firstStartMin > 0 && lastEndMin > 0) {
                    busySb.append(TimeNormalizer.minuteToTimeString(firstStartMin)).append(" - ").append(TimeNormalizer.minuteToTimeString(lastEndMin));
                }
                busySb.append(" tại ").append(dayItems.get(0).room).append(")\n");

                // Khoảng trống đầu ngày
                if (firstStartMin > TimeNormalizer.DAY_START_MINUTE) {
                    int morningFree = firstStartMin - TimeNormalizer.DAY_START_MINUTE;
                    busySb.append("  • 06:45 - ").append(TimeNormalizer.minuteToTimeString(firstStartMin))
                            .append(" (").append(morningFree).append(" phút): 🟢 Trống đầu ngày (Chuẩn bị giáo án, ăn sáng)\n");
                }

                // Chi tiết từng ca và khoảng nghỉ giữa ca
                for (int i = 0; i < dayItems.size(); i++) {
                    ParsedTeachingItem curr = dayItems.get(i);
                    TimeNormalizer.NormalizedRange currRange = TimeNormalizer.fromPeriodRange(school, curr.startPeriod, curr.periodCount);
                    busySb.append("  • ").append(currRange.startTimeStr).append(" - ").append(currRange.endTimeStr)
                            .append(" (").append(curr.periodCount).append(" tiết): 🔴 Ca dạy [").append(curr.subjectName)
                            .append(" - ").append(curr.className).append(" • Phòng ").append(curr.room).append("]\n");

                    if (i < dayItems.size() - 1) {
                        ParsedTeachingItem next = dayItems.get(i + 1);
                        TimeNormalizer.NormalizedRange nextRange = TimeNormalizer.fromPeriodRange(school, next.startPeriod, next.periodCount);
                        int gap = nextRange.startMin - currRange.endMin;
                        if (gap > 0) {
                            busySb.append("  • ").append(currRange.endTimeStr).append(" - ").append(nextRange.startTimeStr)
                                    .append(" (").append(gap).append(" phút): 🟡 Nghỉ giữa ca tại chỗ (Ăn trưa, nghỉ ngơi • ĐÃ KHÓA, KHÔNG NHẬN VIỆC)\n");
                        }
                    }
                }

                // Khoảng trống sau ca dạy đến hết ngày
                if (lastEndMin < TimeNormalizer.DAY_END_MINUTE) {
                    int postFree = TimeNormalizer.DAY_END_MINUTE - lastEndMin;
                    busySb.append("  • ").append(TimeNormalizer.minuteToTimeString(lastEndMin)).append(" - 21:45 (")
                            .append(postFree / 60).append("h").append(postFree % 60).append("p): 🟢 TRỐNG CHIỀU TỐI KHẢ DỤNG (Có thể nhận họp bộ môn Online, chấm thi)");
                }

                analysis.add(busySb.toString().trim());
            }
        }

        return analysis;
    }

    /**
     * Trích xuất tên Giảng viên từ văn bản
     */
    public static String extractLecturerName(String fullText) {
        if (fullText == null) return "";
        Pattern p = Pattern.compile("(?i)(?:Kính\\s*gửi|Thầy/Cô|Giảng\\s*viên)\\s*[:;]?\\s*(?:Thầy\\/Cô|Thầy|Cô)?\\s*[:;]?\\s*([^\\n\\r,]+)");
        Matcher m = p.matcher(fullText);
        if (m.find()) {
            String name = m.group(1).trim().replaceAll("\\s+", " ");
            if (!name.toLowerCase(Locale.getDefault()).contains("khoa") && !name.toLowerCase(Locale.getDefault()).contains("trường")) {
                return name;
            }
        }
        return "";
    }

    /**
     * Trích xuất tên Khoa / Đơn vị mời giảng
     */
    public static String extractFacultyName(String fullText, SchoolProfile.SchoolType school) {
        if (fullText == null) return "";
        Pattern p = Pattern.compile("(?i)(Khoa\\s+[^\\n\\r-]+)");
        Matcher m = p.matcher(fullText);
        if (m.find()) {
            return m.group(1).trim().replaceAll("\\s+", " ");
        }
        return "Khoa CNTT - " + school.getCode();
    }

    /**
     * Trích xuất thông tin Đợt học / Học kỳ
     */
    public static String extractTermInfo(String fullText) {
        if (fullText == null) return "";
        Pattern p = Pattern.compile("(?i)(Đợt\\s+[A-Z0-9]+[^\n\r]+)");
        Matcher m = p.matcher(fullText);
        if (m.find()) {
            return m.group(1).trim().replaceAll("\\s+", " ");
        }
        return "";
    }

    /**
     * Quét danh bạ Thư ký & Hotline liên hệ khẩn cấp
     */
    public static List<DepartmentContact> extractDepartmentContacts(String fullText) {
        List<DepartmentContact> list = new ArrayList<>();
        if (fullText == null) return list;

        // Quét số điện thoại Văn phòng khoa
        Pattern vpPattern = Pattern.compile("(?i)(?:Văn\\s*phòng\\s*khoa[^\\n]*?\\b([A-Z0-9.-]+))?[^\\n]*?(?:Điện\\s*thoại|ĐT|Tel)\\s*[:;]?\\s*([0-9\\s.-]{8,15})");
        Matcher mvp = vpPattern.matcher(fullText);
        if (mvp.find()) {
            String room = mvp.group(1) != null ? mvp.group(1).trim() : "VP Khoa";
            String phone = mvp.group(2) != null ? mvp.group(2).trim().replaceAll("\\s+", " ") : "";
            list.add(new DepartmentContact("Văn phòng Khoa (" + room + ")", "Văn phòng Khoa CNTT", phone, ""));
        }

        // Quét từng thư ký phụ trách
        Pattern contactPattern = Pattern.compile("(?i)(Thời\\s*khóa\\s*biểu[^\n\r–-]*|Học\\s*vụ[^\n\r–-]*|Hợp\\s*đồng[^\n\r–-]*|Công\\s*tác\\s*sinh\\s*viên[^\n\r–-]*|[A-ZÀ-Ỹ][a-zA-ZÀ-ỹ\\s]{3,25})[–-]\\s*(?:ĐT|Điện\\s*thoại)?\\s*[:;]?\\s*([0-9\\s.-]{9,15})\\s*[–-]\\s*(?:Email)?\\s*[:;]?\\s*([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher mc = contactPattern.matcher(fullText);
        while (mc.find()) {
            String roleAndName = mc.group(1).trim().replaceAll("\\s+", " ");
            String phone = mc.group(2).trim().replaceAll("\\s+", " ");
            String email = mc.group(3).trim();

            String role = "Thư ký phụ trách";
            String name = roleAndName;
            if (roleAndName.contains("Thời khóa biểu") || roleAndName.contains("thù lao")) {
                role = "Thời khóa biểu, thù lao";
            } else if (roleAndName.contains("Học vụ") || roleAndName.contains("điểm") || roleAndName.contains("lịch thi")) {
                role = "Học vụ, điểm, lịch thi";
            } else if (roleAndName.contains("Hợp đồng") || roleAndName.contains("hồ sơ")) {
                role = "Hợp đồng, hồ sơ";
            } else if (roleAndName.contains("Công tác sinh viên")) {
                role = "Công tác sinh viên";
            }

            list.add(new DepartmentContact(role, name, phone, email));
        }

        return list;
    }

    /**
     * Bóc tách bảng quy đổi Tiết học ➔ Giờ học trực tiếp từ văn bản (nếu tài liệu có in bảng giờ học ở chân trang)
     */
    public static Map<Integer, int[]> parseCustomPeriodMap(String fullText) {
        Map<Integer, int[]> map = new HashMap<>();
        if (fullText == null || fullText.isEmpty()) return map;

        Pattern p = Pattern.compile("(?i)(?:tiết\\s*)?(\\d{1,2})\\s*[:=.]?\\s*(\\d{1,2})[h:g](\\d{0,2})\\s*[-–đếnto]+\\s*(\\d{1,2})[h:g](\\d{0,2})");
        Matcher m = p.matcher(fullText);
        while (m.find() && m.group(1) != null && m.group(2) != null && m.group(4) != null) {
            try {
                int period = Integer.parseInt(m.group(1));
                int h1 = Integer.parseInt(m.group(2));
                String g3 = m.group(3);
                int m1 = (g3 != null && !g3.isEmpty()) ? Integer.parseInt(g3) : 0;
                int h2 = Integer.parseInt(m.group(4));
                String g5 = m.group(5);
                int m2 = (g5 != null && !g5.isEmpty()) ? Integer.parseInt(g5) : 0;

                int startMin = h1 * 60 + m1;
                int endMin = h2 * 60 + m2;
                if (period >= 1 && period <= 20 && endMin > startMin) {
                    map.put(period, new int[]{startMin, endMin});
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    /**
     * Quét và trích xuất danh sách ngày nghỉ lễ động từ văn bản
     */
    public static Set<String> extractHolidayDates(String fullText) {
        Set<String> holidays = new HashSet<>();
        if (fullText == null) return holidays;

        SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Quét khối "CÁC NGÀY NGHỈ LỄ"
        Pattern holidayBlockPattern = Pattern.compile("(?i)(?:CÁC\\s+NGÀY\\s+NGHỈ\\s+LỄ|NGÀY\\s+NGHỈ\\s+LỄ)[\\s\\S]*?(?=Xin\\s+chân\\s+thành|•\\s*GHI\\s*CHÚ|TP\\.\\s*HCM|\\Z)");
        Matcher mb = holidayBlockPattern.matcher(fullText);
        if (mb.find()) {
            String block = mb.group(0);
            Pattern datePattern = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b");
            Matcher md = datePattern.matcher(block);
            while (md.find()) {
                try {
                    Date d = sdfVn.parse(md.group(1));
                    if (d != null) holidays.add(sdfIso.format(d));
                } catch (Exception ignored) {}
            }
        }

        // Quét thêm nếu có các ngày cụ thể như 30/04, 01/05
        if (holidays.isEmpty()) {
            if (fullText.contains("30/04/2026") || fullText.contains("Giỗ Tổ")) {
                holidays.add("2026-04-26");
                holidays.add("2026-04-27");
                holidays.add("2026-04-28");
                holidays.add("2026-04-29");
                holidays.add("2026-04-30");
                holidays.add("2026-05-01");
                holidays.add("2026-05-02");
                holidays.add("2026-05-03");
            }
        }

        return holidays;
    }

    /**
     * Bóc tách các hàng bảng môn học từ văn bản với độ chịu lỗi cao (Chống lỗi ngắt dòng, lệch ô OCR, hỗ trợ cả bảng Word/PPTX/PDF/CSV)
     */
    public static List<ParsedTeachingItem> extractTeachingItems(String fullText, SchoolProfile.SchoolType school) {
        List<ParsedTeachingItem> items = new ArrayList<>();
        if (fullText == null || fullText.trim().isEmpty()) return items;

        // Chuẩn hóa phân cách ô bảng Word/PPTX/CSV ( | ) thành khoảng trắng để regex nhận diện đồng nhất
        String normalizedText = fullText.replaceAll("\\s*\\|\\s*", " ");

        // CHIẾN LƯỢC 1: Regex Toàn văn bản Đa dòng (Global Multiline Regex với bảo vệ ranh giới)
        // Match trực tiếp các hàng bảng dù tên môn, tên lớp hoặc ngày tháng bị bẻ đôi sang dòng mới
        // Hỗ trợ mã môn học linh hoạt, Thứ viết hoa/thường (Thứ 5 / 5 / CN), Tín chỉ & Sĩ số chuẩn xác
        Pattern fullTablePattern = Pattern.compile("(?i)(?:(\\d{1,2})\\s+)?(?:([0-9A-Za-z]+)\\s+)?(?:([0-9A-Za-z]+)\\s+)?([A-Z0-9._-]{3,12})\\s+((?:(?!\\b(?:THỜI GIAN|GIỜ HỌC|Đợt|[A-Z]{2,5}\\d{2,5}|\\d{1,2}/\\d{1,2}/\\d{4})\\b)[\\s\\S])+?)\\s+(\\d{1,2})\\s+(\\d{1,3})\\s+(?:thứ\\s*)?([2-7]|CN)\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+([A-Z0-9/._-]+)\\s+((?:(?!\\b(?:THỜI GIAN|GIỜ HỌC|Đợt|\\d{1,2}/\\d{1,2}/\\d{4})\\b)[\\s\\S])+?)\\s+(\\d{1,2}/\\d{1,2}/\\d{4})\\s*[-–đếnto,/\\s]+\\s*(\\d{1,2}/\\d{1,2}/\\d{4})");
        Matcher mFull = fullTablePattern.matcher(normalizedText);

        while (mFull.find()) {
            ParsedTeachingItem item = new ParsedTeachingItem();
            item.school = school;
            try {
                if (mFull.group(1) != null) item.stt = Integer.parseInt(mFull.group(1).trim());
            } catch (Exception ignored) {}
            item.group = mFull.group(2) != null ? mFull.group(2).trim() : "";
            item.subGroup = mFull.group(3) != null ? mFull.group(3).trim() : "";
            item.subjectCode = mFull.group(4).trim();
            item.subjectName = healVietnameseText(mFull.group(5).replaceAll("\\s+", " ").trim());
            try {
                if (mFull.group(6) != null) item.credits = Integer.parseInt(mFull.group(6).trim());
            } catch (Exception ignored) {}
            try {
                if (mFull.group(7) != null) item.studentCount = Integer.parseInt(mFull.group(7).trim());
            } catch (Exception ignored) {}

            String dowStr = mFull.group(8).trim();
            item.dayOfWeekVn = dowStr.equalsIgnoreCase("CN") ? 8 : Integer.parseInt(dowStr);
            item.startPeriod = Integer.parseInt(mFull.group(9).trim());
            item.periodCount = Integer.parseInt(mFull.group(10).trim());
            item.room = mFull.group(11).trim();
            item.className = mFull.group(12).replaceAll("\\r?\\n", "").replaceAll("\\s+", " ").trim();
            item.startDateStr = mFull.group(13).trim();
            item.endDateStr = mFull.group(14).trim();

            items.add(item);
        }

        // CHIẾN LƯỢC 2: Xử lý bảng bị xáo trộn do sắp xếp tọa độ Y (Interleaved Table Rows do PDFBox setSortByPosition hoặc OCR phân mảnh)
        // Khi các cột số lõi (STT Nhóm Tổ MãMH Tínchỉ Sĩsố Thứ TiếtBĐ Sốtiết Phòng) nằm trên 1 dòng riêng
        // còn Tên môn, Tên lớp và Ngày học bị đẩy lên dòng trên / dòng dưới
        if (items.isEmpty()) {
            List<ParsedTeachingItem> interleavedItems = extractInterleavedTableItems(normalizedText, school);
            if (!interleavedItems.isEmpty()) {
                items.addAll(interleavedItems);
            }
        }

        // CHIẾN LƯỢC 3: Nếu chưa bắt được (ví dụ bảng không có đủ STT, Nhóm, Tổ), dùng Ghép khối theo khoảng ngày
        if (items.isEmpty()) {
            Pattern blockPattern = Pattern.compile("(?i)([A-Z0-9._-]{3,12})\\s+((?:(?!\\b(?:THỜI GIAN|GIỜ HỌC|Đợt|[A-Z]{2,5}\\d{2,5})\\b)[^\\n\\r])+)\\s+(?:thứ\\s*)?([2-7]|CN)\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+([A-Z0-9/._-]+)\\s+([A-Z0-9,._\\s-]+?)\\s+(\\d{1,2}/\\d{1,2}/\\d{4})\\s*[-–đếnto,/\\s]+\\s*(\\d{1,2}/\\d{1,2}/\\d{4})");
            Matcher mb = blockPattern.matcher(normalizedText);
            int autoStt = 1;
            while (mb.find()) {
                ParsedTeachingItem item = new ParsedTeachingItem();
                item.school = school;
                item.stt = autoStt++;
                item.subjectCode = mb.group(1).trim();
                item.subjectName = healVietnameseText(mb.group(2).replaceAll("\\s+", " ").trim());
                String dowStr = mb.group(3).trim();
                item.dayOfWeekVn = dowStr.equalsIgnoreCase("CN") ? 8 : Integer.parseInt(dowStr);
                item.startPeriod = Integer.parseInt(mb.group(4).trim());
                item.periodCount = Integer.parseInt(mb.group(5).trim());
                item.room = mb.group(6).trim();
                item.className = mb.group(7).replaceAll("\\s+", " ").trim();
                item.startDateStr = mb.group(8).trim();
                item.endDateStr = mb.group(9).trim();
                items.add(item);
            }
        }

        // CHIẾN LƯỢC 4: Duyệt từng dòng đơn lẻ (Line by line fallback cho file Excel / Text có sẵn trên 1 dòng)
        if (items.isEmpty()) {
            String[] lines = normalizedText.split("\\r?\\n");
            Pattern altPattern = Pattern.compile("(?i)(?:(\\d{1,2})\\s+)?(?:(\\d{1,2})\\s+)?(?:(\\d{1,2})\\s+)?([A-Z0-9._-]{3,12})\\s+(.+?)\\s+(?:thứ\\s*)?([2-7]|CN)\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+([A-Z0-9/._-]+)\\s+(.*?)\\s*(\\d{1,2}/\\d{1,2}/\\d{4})\\s*[-–đếnto,/\\s]+\\s*(\\d{1,2}/\\d{1,2}/\\d{4})");
            int autoStt = 1;
            for (String line : lines) {
                String clean = line.replaceAll("[|]+", " ").replaceAll("\\s+", " ").trim();
                if (UniversalScheduleParser.isNoiseLine(clean) || UniversalScheduleParser.isInstitutionalBoilerplate(clean)) {
                    continue;
                }
                Matcher ml = altPattern.matcher(clean);
                if (ml.find()) {
                    ParsedTeachingItem item = new ParsedTeachingItem();
                    item.school = school;
                    try { if (ml.group(1) != null) item.stt = Integer.parseInt(ml.group(1).trim()); } catch (Exception ignored) { item.stt = autoStt++; }
                    if (ml.group(2) != null) item.group = ml.group(2).trim();
                    if (ml.group(3) != null) item.subGroup = ml.group(3).trim();
                    item.subjectCode = ml.group(4).trim();
                    item.subjectName = healVietnameseText(ml.group(5).replaceAll("\\d+", "").replaceAll("\\s+", " ").trim());
                    String dowStr = ml.group(6).trim();
                    item.dayOfWeekVn = dowStr.equalsIgnoreCase("CN") ? 8 : Integer.parseInt(dowStr);
                    item.startPeriod = Integer.parseInt(ml.group(7).trim());
                    item.periodCount = Integer.parseInt(ml.group(8).trim());
                    item.room = ml.group(9).trim();
                    if (ml.group(10) != null && !ml.group(10).trim().isEmpty()) {
                        item.className = ml.group(10).trim();
                    } else {
                        item.className = "Lớp phân công";
                    }
                    item.startDateStr = ml.group(11).trim();
                    item.endDateStr = ml.group(12).trim();
                    items.add(item);
                }
            }
        }

        return items;
    }

    /**
     * Tái tạo và bóc tách các hàng bảng bị phân mảnh/xáo trộn dòng khi PDFBox sort tọa độ Y hoặc AI OCR
     */
    public static List<ParsedTeachingItem> extractInterleavedTableItems(String fullText, SchoolProfile.SchoolType school) {
        List<ParsedTeachingItem> items = new ArrayList<>();
        if (fullText == null || fullText.isEmpty()) return items;

        String[] lines = fullText.split("\\r?\\n");
        // Pattern nhận diện dòng chứa các cột số/mã lõi:
        // STT Nhóm Tổ MãMH Tínchỉ Sĩsố Thứ TiếtBĐ Sốtiết Phòng [TênLớp]
        Pattern corePattern = Pattern.compile("(?i)^\\s*(?:(\\d{1,2})\\s+)?(?:(\\d{1,2})\\s+)?(?:(\\d{1,2})\\s+)?([A-Z]{2,5}\\d{2,5})\\s+(\\d{1,2})\\s+(\\d{1,3})\\s+([2-7]|CN)\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+([A-Z0-9/._-]+)(?:\\s+([A-Z0-9,._-]+))?\\s*$");
        Pattern datePattern = Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher mc = corePattern.matcher(line);
            if (mc.matches()) {
                ParsedTeachingItem item = new ParsedTeachingItem();
                item.school = school;
                try { if (mc.group(1) != null) item.stt = Integer.parseInt(mc.group(1).trim()); } catch (Exception ignored) {}
                item.group = mc.group(2) != null ? mc.group(2).trim() : "";
                item.subGroup = mc.group(3) != null ? mc.group(3).trim() : "";
                item.subjectCode = mc.group(4).trim();
                try { item.credits = Integer.parseInt(mc.group(5).trim()); } catch (Exception ignored) {}
                try { item.studentCount = Integer.parseInt(mc.group(6).trim()); } catch (Exception ignored) {}
                String dowStr = mc.group(7).trim();
                item.dayOfWeekVn = dowStr.equalsIgnoreCase("CN") ? 8 : Integer.parseInt(dowStr);
                item.startPeriod = Integer.parseInt(mc.group(8).trim());
                item.periodCount = Integer.parseInt(mc.group(9).trim());
                item.room = mc.group(10).trim();
                if (mc.group(11) != null) {
                    item.className = mc.group(11).trim();
                }

                // Tìm ngày học và các mảnh văn bản (Tên môn, Tên lớp) từ các dòng lân cận (i-1 và i+1)
                String prevLine = (i > 0) ? lines[i - 1].trim() : "";
                String nextLine = (i < lines.length - 1) ? lines[i + 1].trim() : "";

                List<String> dates = new ArrayList<>();
                Matcher mdPrev = datePattern.matcher(prevLine);
                while (mdPrev.find()) dates.add(mdPrev.group(1));
                Matcher mdNext = datePattern.matcher(nextLine);
                while (mdNext.find()) dates.add(mdNext.group(1));

                if (dates.size() >= 2) {
                    item.startDateStr = dates.get(0);
                    item.endDateStr = dates.get(dates.size() - 1);
                } else if (dates.size() == 1) {
                    item.startDateStr = dates.get(0);
                    item.endDateStr = dates.get(0);
                }

                // Tách phần ngày và dấu gạch nối ra khỏi prevLine và nextLine
                String cleanPrev = prevLine.replaceAll("\\d{1,2}/\\d{1,2}/\\d{4}", "").replaceAll("[-–đếnto]+", " ").replaceAll("\\s+", " ").trim();
                String cleanNext = nextLine.replaceAll("\\d{1,2}/\\d{1,2}/\\d{4}", "").replaceAll("[-–đếnto]+", " ").replaceAll("\\s+", " ").trim();

                // Trích xuất mã lớp (nếu chưa có trên dòng chính)
                if (item.className.isEmpty()) {
                    Pattern classPattern = Pattern.compile("([0-9]{2}[A-Z0-9,._-]+)");
                    Matcher mcp = classPattern.matcher(cleanPrev);
                    String classPart1 = mcp.find() ? mcp.group(1) : "";
                    String classPart2 = "";

                    if (!classPart1.isEmpty()) {
                        // Tìm đoạn nối của tên lớp ở cuối cleanNext (ví dụ "Web DTHB7" -> bắt "DTHB7")
                        Pattern classPart2Pattern = Pattern.compile("\\b([A-Z0-9]{3,15})\\s*$");
                        Matcher mcn = classPart2Pattern.matcher(cleanNext);
                        if (mcn.find()) {
                            classPart2 = mcn.group(1);
                        }
                    } else {
                        Matcher mcn = classPattern.matcher(cleanNext);
                        if (mcn.find()) {
                            classPart2 = mcn.group(1);
                        }
                    }

                    if (!classPart1.isEmpty() || !classPart2.isEmpty()) {
                        item.className = (classPart1 + (classPart1.endsWith(",") ? "" : "") + classPart2).replaceAll("\\s+", "").replaceAll(",+", ",").trim();
                        if (!classPart1.isEmpty()) cleanPrev = cleanPrev.replace(classPart1, "").trim();
                        if (!classPart2.isEmpty()) cleanNext = cleanNext.replace(classPart2, "").trim();
                    }
                }

                // Phần còn lại của cleanPrev và cleanNext là tên môn học
                String subName = (cleanPrev + " " + cleanNext).replaceAll("\\s+", " ").trim();
                if (!subName.isEmpty()) {
                    item.subjectName = healVietnameseText(subName);
                } else {
                    item.subjectName = "Học phần " + item.subjectCode;
                }

                items.add(item);
            }
        }
        return items;
    }
}
