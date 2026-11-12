package com.tkyl.lectuplan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventModel {
    // 4 Phân loại lịch chính theo tiêu chuẩn ứng dụng
    public static final String CAT_TEACHING = "Lịch dạy/học";
    public static final String CAT_EVENT = "Lịch sự kiện";
    public static final String CAT_INCIDENT = "Lịch phát sinh";
    public static final String CAT_MANUAL_OTHER = "Lịch khác";
    public static final String LEGACY_CAT_MANUAL_OTHER = "Lịch nhập thủ công(khác)";

    private int id;
    private int stt;
    private String title;       // Tên sự kiện / công việc
    private String date;        // Ngày tổ chức (Định dạng chuẩn: yyyy-MM-dd)
    private String organizer;   // Tên người tổ chức / phân công
    private String semester;    // Học kỳ (HK1, HK2, HK3)
    private int year;           // Năm
    private int week;           // Tuần thứ mấy trong năm
    private String category;    // Phân loại cụ thể (Lịch dạy/học, Lịch sự kiện, Lịch phát sinh, hoặc tên tùy chỉnh người dùng tự đặt)

    // Các trường mở rộng chuẩn hóa thời gian & đa trường
    private String startTime;   // HH:mm (VD: "07:30")
    private String endTime;     // HH:mm (VD: "11:35")
    private int startMin;       // Phút từ 00:00 (VD: 450)
    private int endMin;         // Phút từ 00:00 (VD: 695)
    private String schoolCode;  // HUTECH, NTTU, UEF, OTHER
    private String campus;      // Thu Duc Campus (Khu E), Sai Gon Campus...
    private String room;        // E1-04.06/1
    private String priorityType;// FIXED_TEACHING, ACADEMIC_INCIDENT, PERSONAL_TASK

    public EventModel(int id, int stt, String title, String date, String organizer, String semester, int year, int week) {
        this.id = id;
        this.stt = stt;
        this.title = title;
        this.date = date;
        this.organizer = organizer;
        this.semester = semester;
        this.year = year;
        this.week = week;
        autoInferTimeAndSchool();
    }

    public EventModel(int id, int stt, String title, String date, String organizer, String semester, int year, int week,
                      String startTime, String endTime, String schoolCode, String campus, String room, String priorityType) {
        this.id = id;
        this.stt = stt;
        this.title = title;
        this.date = date;
        this.organizer = organizer;
        this.semester = semester;
        this.year = year;
        this.week = week;
        this.startTime = startTime;
        this.endTime = endTime;
        this.schoolCode = schoolCode;
        this.campus = campus;
        this.room = room;
        this.priorityType = priorityType;
        if (startTime != null && !startTime.isEmpty()) {
            this.startMin = TimeNormalizer.timeStringToMinute(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            this.endMin = TimeNormalizer.timeStringToMinute(endTime);
        }
        if (this.startMin <= 0 || this.endMin <= 0) {
            autoInferTimeAndSchool();
        }
    }

    private void autoInferTimeAndSchool() {
        SchoolProfile.SchoolType sc = SchoolProfile.detectSchool((title != null ? title : "") + " " + (organizer != null ? organizer : ""));
        this.schoolCode = sc.getCode();
        TimeNormalizer.NormalizedRange range = TimeNormalizer.parseFromText((title != null ? title : "") + " " + (organizer != null ? organizer : ""), sc);
        this.startTime = range.startTimeStr;
        this.endTime = range.endTimeStr;
        this.startMin = range.startMin;
        this.endMin = range.endMin;

        CampusBufferHelper.CampusInfo cInfo = CampusBufferHelper.detectCampus((title != null ? title : "") + " " + (organizer != null ? organizer : ""), schoolCode);
        this.campus = cInfo.campusName;
        this.room = cInfo.room;
        if (this.room == null || this.room.isEmpty()) {
            this.room = CampusBufferHelper.extractGenericRoom(title != null ? title : "");
        }
    }

    public int getId() { return id; }
    public int getStt() { return stt; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getOrganizer() { return organizer; }
    public String getSemester() { return semester; }
    public int getYear() { return year; }
    public int getWeek() { return week; }

    public String getStartTime() { return startTime != null ? startTime : "07:30"; }
    public String getEndTime() { return endTime != null ? endTime : "11:35"; }
    public int getStartMin() { return startMin > 0 ? startMin : 450; }
    public int getEndMin() { return endMin > 0 ? endMin : 695; }
    public String getSchoolCode() {
        if (!isTeachingEvent()) return "OTHER";
        return (schoolCode != null && !schoolCode.trim().isEmpty()) ? schoolCode : "OTHER";
    }
    public String getCampus() { return campus != null ? campus : ""; }
    public String getRoom() { return room != null ? room : ""; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getScheduleCategory() {
        if (category != null && !category.trim().isEmpty()) {
            return category.trim();
        }
        return MainActivity.classifyEventNature(this.title, this.organizer);
    }

    public String getPriorityType() {
        if (priorityType != null && !priorityType.isEmpty()) return priorityType;
        if (isTeachingEvent()) return "FIXED_TEACHING";
        String t = (title != null ? title : "").toLowerCase(java.util.Locale.getDefault());
        if (t.contains("họp") || t.contains("hội đồng") || t.contains("coi thi") || t.contains("đột xuất")) {
            return "ACADEMIC_INCIDENT";
        }
        return "PERSONAL_TASK";
    }

    /**
     * Xác định xem một sự kiện có phải là LỊCH GIẢNG DẠY ĐẠI HỌC thực sự hay không.
     * Ngăn chặn việc gán nhầm các dòng trong CV, sơ yếu lý lịch, họp, nhiệm vụ cá nhân thành lịch dạy cố định.
     */
    public boolean isTeachingEvent() {
        if (category != null && !category.trim().isEmpty()) {
            if (CAT_TEACHING.equalsIgnoreCase(category.trim())) {
                return true;
            }
            return false;
        }

        String t = (title != null ? title : "").toLowerCase(java.util.Locale.getDefault());
        String o = (organizer != null ? organizer : "").toLowerCase(java.util.Locale.getDefault());
        String combined = t + " " + o;

        // 1. Nếu là CV, hồ sơ cá nhân, bản thông tin, gia cảnh -> TUYỆT ĐỐI KHÔNG PHẢI LỊCH DẠY
        if (combined.contains("curriculum vitae") || combined.contains("sơ yếu lý lịch")
                || combined.contains("thông tin cá nhân") || combined.contains("hồ sơ cá nhân")
                || combined.contains("hồ sơ •") || combined.contains("hồ sơ")
                || combined.contains("học bổng") || combined.contains("dân tộc")
                || combined.contains("nơi sinh") || combined.contains("thường trú")
                || combined.contains("họ và tên cha") || combined.contains("họ và tên mẹ")
                || combined.contains("kinh nghiệm làm việc") || combined.contains("quá trình đào tạo")
                || combined.contains("mục tiêu nghề nghiệp") || combined.contains("kỹ năng")
                || combined.contains("bản thông tin")) {
            return false;
        }

        // 2. Nếu là nhắc việc, soạn đề, họp, coi thi, nhắc nhở...
        if (combined.contains("soạn") || combined.contains("đề thi") || combined.contains("họp")
                || combined.contains("hội đồng") || combined.contains("coi thi") || combined.contains("chấm bài")
                || combined.contains("nhắc việc") || combined.contains("nhắc nhở") || combined.contains("cá nhân")
                || combined.contains("công tác") || combined.contains("giao ban") || combined.contains("tập huấn")) {
            return false;
        }

        // 3. Dấu hiệu tích cực khẳng định là lịch giảng dạy đại học:
        // - Có tiết học rõ ràng (loại trừ từ 'chi tiết', 'tiết kiệm', 'tình tiết')
        if (t.matches(".*(?<!chi\\s?)(?<!tình\\s?)\\btiết\\s*[:=\\s-]*\\d+.*")) {
            return true;
        }

        // - Có phòng học + (lớp học hoặc mã môn học hoặc nhóm)
        boolean hasRoom = t.matches(".*\\b(phòng|room)\\s*[:=]?\\s*[a-zA-Z0-9._/-]+.*");
        boolean hasClassOrGroup = t.matches(".*\\b(lớp|nhóm|tổ)\\s*[:=]?\\s*[a-zA-Z0-9._/-]+.*");
        boolean hasSubjectCode = t.matches(".*\\b[A-Z]{2,5}\\d{2,5}\\b.*");
        if (hasRoom && (hasClassOrGroup || hasSubjectCode)) {
            return true;
        }

        // - Có các từ khóa giảng dạy chuyên ngành
        if (t.contains("giảng dạy") || t.contains("dạy lớp") || t.contains("thực hành")
                || t.contains("lý thuyết") || t.contains("thư mời giảng") || t.contains("thời khóa biểu")
                || t.contains("tkb") || t.contains("môn học") || t.contains("học phần")) {
            return true;
        }

        return false;
    }

    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setStartMin(int startMin) { this.startMin = startMin; }
    public void setEndMin(int endMin) { this.endMin = endMin; }
    public void setSchoolCode(String schoolCode) { this.schoolCode = schoolCode; }
    public void setCampus(String campus) { this.campus = campus; }
    public void setRoom(String room) { this.room = room; }
    public void setPriorityType(String priorityType) { this.priorityType = priorityType; }

    // Định dạng học kỳ chuẩn đại học: ví dụ HK1 2026-2027
    public String getFormattedAcademicSemester() {
        int nextYear = year + 1;
        return semester + " " + year + "-" + nextYear;
    }

    public static class EventFieldBreakdown {
        public String title = "";
        public String period = "";
        public String time = "";
        public String group = "";
        public String room = "";
        public String organizer = "";
    }

    public EventFieldBreakdown getBreakdown() {
        return parseEventDetails(this);
    }

    /**
     * Làm sạch tiêu đề sự kiện, loại bỏ các chuỗi tự động đính kèm dư thừa như (Phòng: ...) hoặc (Thời gian: ...)
     */
    public static String cleanDisplayTitle(String rawTitle) {
        if (rawTitle == null) return "";
        String clean = rawTitle
                .replaceAll("(?i)\\s*\\(phòng\\s*:[^)]*\\)", "")
                .replaceAll("(?i)\\s*\\(thời gian\\s*:[^)]*\\)", "")
                .replaceAll("(?i)\\s*\\[\\d{1,2}:\\d{2}.*?\\]", "")
                .replaceAll("(?i)\\s*\\|\\s*tiết\\b.*", "")
                .replaceAll("(?i)\\s*-\\s*(?:phòng|room|địa\\s*điểm)\\b.*", "")
                .trim();
        return clean.isEmpty() ? rawTitle : clean;
    }

    public static EventFieldBreakdown parseEventDetails(EventModel event) {
        EventFieldBreakdown bd = new EventFieldBreakdown();
        if (event == null) return bd;

        String rawTitle = event.getTitle() != null ? event.getTitle().trim() : "";
        bd.organizer = event.getOrganizer() != null ? event.getOrganizer().trim() : "";
        bd.time = (event.getStartTime() != null && event.getEndTime() != null)
                ? (event.getStartTime() + " - " + event.getEndTime()) : "";

        if (event.getId() < 0) {
            bd.title = cleanDisplayTitle(rawTitle);
            return bd;
        }

        // 1. Trích xuất Tiết (VD: 2-6, 7-11) - Loại trừ 'chi tiết', 'tiết kiệm'
        Pattern pPeriod = Pattern.compile("(?i)(?<![a-zA-ZÀ-ỹ0-9_])(?<!chi\\s?)(?<!tình\\s?)\\btiết\\s*[:=\\s-]*(\\d{1,2}\\s*[-–tođến]\\s*\\d{1,2}|\\d{1,2})\\b");
        Matcher mPeriod = pPeriod.matcher(rawTitle);
        if (mPeriod.find()) {
            bd.period = mPeriod.group(1).replaceAll("\\s+", "");
        } else if (!event.isTeachingEvent()) {
            bd.period = "";
        }

        // 2. Trích xuất Phòng / Địa điểm - Hỗ trợ cho mọi loại lịch và mọi nội dung nhập tay (bất kỳ từ khóa hoặc cụm từ nào)
        String room = event.getRoom() != null ? event.getRoom().trim() : "";
        if (room.isEmpty()) {
            int bracketIdx = rawTitle.indexOf(" [");
            if (bracketIdx < 0) bracketIdx = rawTitle.indexOf("[");
            String titlePartForRoom = bracketIdx > 0 ? rawTitle.substring(0, bracketIdx) : rawTitle;

            java.util.regex.Matcher mDash = java.util.regex.Pattern.compile("\\s+-\\s+(?!\\d{4})").matcher(titlePartForRoom);
            int lastValidDash = -1;
            while (mDash.find()) {
                lastValidDash = mDash.start();
            }
            if (lastValidDash != -1) {
                String suffix = titlePartForRoom.substring(lastValidDash + 3).trim();
                int pipeIdx = suffix.indexOf(" |");
                if (pipeIdx > 0) {
                    suffix = suffix.substring(0, pipeIdx).trim();
                }
                if (!suffix.isEmpty() && !suffix.toLowerCase(java.util.Locale.getDefault()).contains("tiết") && !suffix.matches("\\d{4}")) {
                    room = suffix;
                }
            }

            if (room.isEmpty()) {
                room = CampusBufferHelper.extractGenericRoom(rawTitle);
            }
        }
        bd.room = room;
        if (event.getRoom().isEmpty() && !room.isEmpty()) {
            event.setRoom(room);
        }

        // 3. Trích xuất Nhóm & Tổ & Sĩ số SV (VD: 18/Tổ 02 - 24 SV)
        Pattern pGroup = Pattern.compile("(?i)nhóm\\s*([^•\\)\\|\\-]+)");
        Matcher mGroup = pGroup.matcher(rawTitle);
        String groupPart = "";
        if (mGroup.find()) {
            groupPart = mGroup.group(1).trim();
        }

        Pattern pSv = Pattern.compile("(?i)(\\d+\\s*SV)");
        Matcher mSv = pSv.matcher(rawTitle);
        String svPart = "";
        if (mSv.find()) {
            svPart = mSv.group(1).trim();
        }

        if (!groupPart.isEmpty() && !svPart.isEmpty()) {
            bd.group = groupPart + " - " + svPart;
        } else if (!groupPart.isEmpty()) {
            bd.group = groupPart;
        } else if (!svPart.isEmpty()) {
            bd.group = svPart;
        }

        // 4. Trích xuất Tên môn học sạch + Tên Lớp (đảm bảo KHÔNG cắt cụt chữ của người dùng)
        String cleanTitle = rawTitle;
        if (bd.room != null && !bd.room.isEmpty()) {
            int dashIdx = cleanTitle.toLowerCase(java.util.Locale.getDefault()).indexOf(" - " + bd.room.toLowerCase(java.util.Locale.getDefault()));
            if (dashIdx > 0) {
                cleanTitle = cleanTitle.substring(0, dashIdx).trim();
            } else {
                int lastDash = cleanTitle.lastIndexOf(" - ");
                if (lastDash > 0) {
                    cleanTitle = cleanTitle.substring(0, lastDash).trim();
                } else {
                    int pipeIdx = cleanTitle.toLowerCase(java.util.Locale.getDefault()).indexOf(" | tiết");
                    if (pipeIdx > 0) {
                        cleanTitle = cleanTitle.substring(0, pipeIdx).trim();
                    }
                }
            }
        }

        // Nếu trong ngoặc có chứa cụm "• Nhóm..." đã được trích xuất riêng vào bd.group
        if (!bd.group.isEmpty()) {
            int openParen = cleanTitle.indexOf("(");
            int closeParen = cleanTitle.indexOf(")");
            if (openParen > 0 && closeParen > openParen) {
                String subjectBase = cleanTitle.substring(0, openParen).trim();
                String parenContent = cleanTitle.substring(openParen + 1, closeParen).trim();
                int bulletGroup = parenContent.toLowerCase(java.util.Locale.getDefault()).indexOf("• nhóm");
                if (bulletGroup > 0) {
                    String classCode = parenContent.substring(0, bulletGroup).trim();
                    cleanTitle = subjectBase + " (" + classCode + ")";
                }
            }
        }

        String resultTitle = cleanTitle.isEmpty() ? rawTitle : cleanTitle;
        bd.title = cleanDisplayTitle(resultTitle);
        return bd;
    }

    /**
     * Sắp xếp danh sách sự kiện theo thứ tự thời gian:
     * - Ngày nào trước thì ở trên đầu trước (date ASC)
     * - Trong cùng ngày: Thời gian nào bắt đầu trước thì ở trên đầu trước (startMin ASC)
     * - Cùng giờ bắt đầu: Sự kiện nào kết thúc trước thì ở trước (endMin ASC)
     */
    public static void sortChronologically(java.util.List<EventModel> list) {
        if (list == null || list.size() <= 1) return;
        boolean allSpecial = true;
        for (EventModel em : list) {
            if (em != null && em.getId() > 0) {
                allSpecial = false;
                break;
            }
        }
        if (allSpecial) return;

        java.util.Collections.sort(list, (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;

            if (a.getId() < 0 && b.getId() > 0) return 1;
            if (a.getId() > 0 && b.getId() < 0) return -1;

            // 1. So sánh ngày tổ chức (yyyy-MM-dd)
            String dateA = a.getDate() != null ? a.getDate() : "";
            String dateB = b.getDate() != null ? b.getDate() : "";
            int dateCmp = dateA.compareTo(dateB);
            if (dateCmp != 0) return dateCmp;

            // 2. Cùng ngày: Thời gian nào trước thì ở trên đầu trước (startMin càng nhỏ càng sớm)
            int timeCmp = Integer.compare(a.getStartMin(), b.getStartMin());
            if (timeCmp != 0) return timeCmp;

            // 3. Cùng giờ bắt đầu: Sự kiện kết thúc sớm hơn lên trước
            int endCmp = Integer.compare(a.getEndMin(), b.getEndMin());
            if (endCmp != 0) return endCmp;

            // 4. Nếu bằng nhau: so sánh theo STT hoặc ID
            int sttCmp = Integer.compare(a.getStt(), b.getStt());
            if (sttCmp != 0) return sttCmp;

            return Integer.compare(a.getId(), b.getId());
        });
    }
}
