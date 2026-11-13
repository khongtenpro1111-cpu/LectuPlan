package com.tkyl.lectuplan;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class LectuPlanEngineTest {

    @Test
    public void testUniversalTimeNormalizer_HutechPeriods() {
        // HUTECH Tiết 2, kéo dài 5 tiết (Tiết 2 - 6): 07:30 đến 11:35
        TimeNormalizer.NormalizedRange morningRange = TimeNormalizer.fromPeriodRange(SchoolProfile.SchoolType.HUTECH, 2, 5);
        assertEquals(450, morningRange.startMin); // 07:30 = 7*60 + 30 = 450
        assertEquals(695, morningRange.endMin);   // 11:35 = 11*60 + 35 = 695
        assertEquals("07:30", morningRange.startTimeStr);
        assertEquals("11:35", morningRange.endTimeStr);
        assertEquals(245, morningRange.getDurationMinutes());

        // HUTECH Tiết 7, kéo dài 5 tiết (Tiết 7 - 11): 12:30 đến 16:35
        TimeNormalizer.NormalizedRange afternoonRange = TimeNormalizer.fromPeriodRange(SchoolProfile.SchoolType.HUTECH, 7, 5);
        assertEquals(750, afternoonRange.startMin); // 12:30 = 12*60 + 30 = 750
        assertEquals(995, afternoonRange.endMin);   // 16:35 = 16*60 + 35 = 995
        assertEquals("12:30", afternoonRange.startTimeStr);
        assertEquals("16:35", afternoonRange.endTimeStr);
}

    @Test
    public void testUniversalTimeNormalizer_NttuPeriods() {
        // NTTU Ca sáng: Tiết 1-3: 07h00 - 09h30
        TimeNormalizer.NormalizedRange morning1 = TimeNormalizer.fromPeriodRange(SchoolProfile.SchoolType.NTTU, 1, 3);
        assertEquals(420, morning1.startMin); // 07:00
        assertEquals(570, morning1.endMin);   // 09:30
        assertEquals("07:00", morning1.startTimeStr);
        assertEquals("09:30", morning1.endTimeStr);

        // NTTU Ca sáng sau giải lao: Tiết 4-6: 09h40 - 12h10
        TimeNormalizer.NormalizedRange morning2 = TimeNormalizer.fromPeriodRange(SchoolProfile.SchoolType.NTTU, 4, 3);
        assertEquals(580, morning2.startMin); // 09:40
        assertEquals(730, morning2.endMin);   // 12:10
        assertEquals("09:40", morning2.startTimeStr);
        assertEquals("12:10", morning2.endTimeStr);

        // NTTU Ca chiều: Tiết 7-9: 13h00 - 15h30
        TimeNormalizer.NormalizedRange afternoon1 = TimeNormalizer.fromPeriodRange(SchoolProfile.SchoolType.NTTU, 7, 3);
        assertEquals(780, afternoon1.startMin); // 13:00
        assertEquals(930, afternoon1.endMin);   // 15:30

        // NTTU Ca tối: Tiết 15-16: 20h05 - 21h45
        TimeNormalizer.NormalizedRange evening2 = TimeNormalizer.fromPeriodRange(SchoolProfile.SchoolType.NTTU, 15, 2);
        assertEquals(1205, evening2.startMin); // 20:05
        assertEquals(1305, evening2.endMin);   // 21:45
        assertEquals("20:05", evening2.startTimeStr);
        assertEquals("21:45", evening2.endTimeStr);
}

    @Test
    public void testUniversalTimeNormalizer_AdministrativeHours() {
        // Sự kiện phát sinh: Họp Hội đồng Khoa từ 14:00 đến 16:00
        TimeNormalizer.NormalizedRange adminRange = TimeNormalizer.parseFromText("Họp Hội đồng Khoa từ 14:00 đến 16:00", SchoolProfile.SchoolType.DEFAULT);
        assertEquals(14 * 60, adminRange.startMin); // 840
        assertEquals(16 * 60, adminRange.endMin);   // 960
        assertEquals("14:00", adminRange.startTimeStr);
        assertEquals("16:00", adminRange.endTimeStr);

        // Khám sức khỏe từ 08:00 đến 09:30
        TimeNormalizer.NormalizedRange healthRange = TimeNormalizer.parseFromText("Khám sức khỏe từ 08:00 đến 09:30", SchoolProfile.SchoolType.DEFAULT);
        assertEquals(8 * 60, healthRange.startMin); // 480
        assertEquals(9 * 60 + 30, healthRange.endMin); // 570
}

    @Test
    public void testCampusBufferEngine() {
        // Cùng cơ sở: E1-04.06/1 và E1-04.06/2 tại HUTECH Khu E (Thu Duc Campus)
        CampusBufferHelper.CampusInfo c1 = CampusBufferHelper.detectCampus("Phòng E1-04.06/1", "HUTECH");
        CampusBufferHelper.CampusInfo c2 = CampusBufferHelper.detectCampus("Phòng E1-04.06/2", "HUTECH");
        assertEquals("HUTECH_THUDUC", c1.campusId);
        assertEquals("HUTECH_THUDUC", c2.campusId);
        assertTrue(CampusBufferHelper.isSameCampus(c1, c2));

        // Khác cơ sở: A-05.01 (Sai Gon Campus Điện Biên Phủ) sang E1-04.06 (Khu E Thủ Đức)
        CampusBufferHelper.CampusInfo cSaigon = CampusBufferHelper.detectCampus("Phòng A-05.01 Cơ sở Điện Biên Phủ", "HUTECH");
        assertEquals("HUTECH_SAIGON", cSaigon.campusId);
        assertFalse(CampusBufferHelper.isSameCampus(cSaigon, c1));
}

    @Test
    public void testThumoiGiangParserAndDeadlines() {
        String samplePdfText = "KHOA CÔNG NGHỆ THÔNG TIN - HUTECH\n" +
                "THƯ MỜI GIẢNG\n" +
                "Kính gửi Thầy/Cô: Lý Quang Vinh\n" +
                "1 14 04 CMP376 Thực hành lập trình Web 1 27 5 7 5 E1-04.06/2 23DTHB6,23DTHB7 14/05/2026 - 18/06/2026\n" +
                "2 18 02 CMP376 Thực hành lập trình Web 1 24 5 2 5 E1-04.06/1 23DTHC3 14/05/2026 - 18/06/2026\n" +
                "CÁC NGÀY NGHỈ LỄ TRONG HK2B:\n" +
                "+ Ngày Giải phóng Miền Nam và Quốc tế Lao động: 30/04/2026, 01/05/2026, 02/05/2026, 03/05/2026\n" +
                "+ Ngày Giỗ Tổ Hùng Vương: 26/04/2026, 27/04/2026, 28/04/2026, 29/04/2026\n" +
                "2. GV nộp đề thi cuối kỳ về Văn phòng khoa trước khi kết thúc học phần 2 tuần (2 đề thi + 2 đáp án)\n" +
                "Giảng viên công bố cho sinh viên điểm quá trình trên lớp vào buổi học cuối cùng và nộp bảng điểm quá trình về văn phòng khoa vào tuần kết thúc học phần\n";

        ThumoiGiangParser.ParseResult result = ThumoiGiangParser.parseInvitationDocument(samplePdfText);
        assertEquals(SchoolProfile.SchoolType.HUTECH, result.school);
        assertTrue(result.generatedEvents.size() > 0);

        // Kiểm tra tự động tạo 2 hạn chót hành chính đi kèm môn học
        boolean hasExamDeadline = false;
        boolean hasGradeDeadline = false;
        for (ThumoiGiangParser.GeneratedClassEvent ev : result.generatedEvents) {
            if (ev.isDeadline && ev.title.contains("Nộp đề thi")) {
                hasExamDeadline = true;
                // 18/06/2026 lùi lại 14 ngày = 04/06/2026
                assertEquals("2026-06-04", ev.dateIso);
}
            if (ev.isDeadline && ev.title.contains("điểm quá trình")) {
                hasGradeDeadline = true;
                // Buổi cuối cùng = 18/06/2026
                assertEquals("2026-06-18", ev.dateIso);
}
}
        assertTrue("Phải tự động tạo hạn nộp đề thi trước 2 tuần", hasExamDeadline);
        assertTrue("Phải tự động tạo hạn công bố & nộp bảng điểm buổi cuối", hasGradeDeadline);
}

    @Test
    public void inspectRealPdfFile() {
        try {
            java.io.File pdfFile = new java.io.File("D:\\Android_NTTU\\LTDD_DAN\\VLQ0230284.pdf");
            if (pdfFile.exists()) {
                // Test 1: stripper with setSortByPosition(false)
                org.apache.pdfbox.pdmodel.PDDocument doc2 = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile);
                org.apache.pdfbox.text.PDFTextStripper stripper2 = new org.apache.pdfbox.text.PDFTextStripper();
                stripper2.setSortByPosition(false);
                String text2 = stripper2.getText(doc2);
                doc2.close();

                ThumoiGiangParser.ParseResult resFalse = ThumoiGiangParser.parseInvitationDocument(text2);
                System.out.println("=== RESULT FOR setSortByPosition(false) ===");
                System.out.println("Items found: " + resFalse.items.size());
                for (ThumoiGiangParser.ParsedTeachingItem it : resFalse.items) {
                    System.out.println("Item: group=" + it.group + ", subGroup=" + it.subGroup + ", subject=" + it.subjectName + ", dow=" + it.dayOfWeekVn + ", start=" + it.startPeriod + ", count=" + it.periodCount + ", room=" + it.room + ", class=" + it.className + ", date=" + it.startDateStr + " - " + it.endDateStr);
}
                assertEquals(2, resFalse.items.size());
                assertEquals("18", resFalse.items.get(0).group);
                assertEquals(2, resFalse.items.get(0).startPeriod);
                assertEquals(5, resFalse.items.get(0).periodCount);
                assertEquals("14", resFalse.items.get(1).group);
                assertEquals(7, resFalse.items.get(1).startPeriod);
                assertEquals(5, resFalse.items.get(1).periodCount);

                // Test 2: stripper with setSortByPosition(true)
                org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile);
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(doc);
                doc.close();

                ThumoiGiangParser.ParseResult resTrue = ThumoiGiangParser.parseInvitationDocument(text);
                System.out.println("=== RESULT FOR setSortByPosition(true) ===");
                System.out.println("Items found: " + resTrue.items.size());
                for (ThumoiGiangParser.ParsedTeachingItem it : resTrue.items) {
                    System.out.println("Item: group=" + it.group + ", subGroup=" + it.subGroup + ", subject=" + it.subjectName + ", dow=" + it.dayOfWeekVn + ", start=" + it.startPeriod + ", count=" + it.periodCount + ", room=" + it.room + ", class=" + it.className + ", date=" + it.startDateStr + " - " + it.endDateStr);
}
}
} catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
}
}

    @Test
    public void testThumoiGiangParser_UserRealMultiLineOcr() {
        String realOcrPdf = " BM01/QT02/ĐT\n" +
                "THƯ MỜI GIẢNG\n" +
                "Kính gửi Thầy/Cô: Lý Quang Vinh\n" +
                "Khoa Công nghệ Thông tin - HUTECH kính mời Thầy/Cô tham gia giảng dạy những lớp sau:\n" +
                "TT Nhóm Tổ \n" +
                "TH Mã MH Môn Tín \n" +
                "chỉ\n" +
                "Sĩ \n" +
                "số Thứ\n" +
                "Tiết\n" +
                "bắt \n" +
                "đầu\n" +
                "Số \n" +
                "tiết Phòng Tên Lớp Ngày học\n" +
                "1 14 04 CMP376 Thực hành lập trình \n" +
                "Web 1 27 5 7 5 E1-04.06/2 23DTHB6,23\n" +
                "DTHB7\n" +
                "14/05/2026 -\n" +
                "18/06/2026\n" +
                "2 18 02 CMP376 Thực hành lập trình \n" +
                "Web 1 24 5 2 5 E1-04.06/1 23DTHC3 14/05/2026 -\n" +
                "18/06/2026\n" +
                "THỜI GIAN:\n" +
                "+ Đợt B: bắt đầu học từ tuần 34 đến tuần 42 (từ 20/04/2026 đến 21/06/2026) và thi học kỳ từ tuần 43, 44 (từ 22/06/2026 đến 05/07/2026)\n" +
                "GIỜ HỌC:\n" +
                "BUỔI CA TIẾT GIỜ HỌC\n" +
                "Sáng 1: 1 06h45 – 07h30, 2 07h30 – 08h15, 3 08h15 – 09h00\n" +
                "• ĐỊA ĐIỂM:\n" +
                "+ Thu Duc Campus: Các phòng ký hiệu E- : Khu Công nghệ cao - P. Tăng Nhơn Phú - TP. HCM\n" +
                "• CÁC NGÀY NGHỈ LỄ TRONG HK2B:\n" +
                "+ Ngày Giải phóng Miền Nam và Quốc tế Lao động: 30/04/2026, 01/05/2026, 02/05/2026, 03/05/2026 (Thứ 5, Thứ 6, Thứ 7, CN)\n" +
                "+ Ngày Giỗ Tổ Hùng Vương: 26/04/2026, 27/04/2026, 28/04/2026, 29/04/2026 (CN, Thứ 2, Thứ 3, Thứ 4)\n" +
                "Văn phòng khoa Công nghệ Thông tin – E1-02.06\n" +
                "Điện thoại liên hệ: 028 6686 6176\n" +
                "Thư ký phụ trách:\n" +
                "Thời khóa biểu, thù lao Võ Hồng Linh Ân – ĐT: 0348 913 380 – Email: vhl.an@hutech.edu.vn\n" +
                "Học vụ, điểm, đề cương, nhật ký giảng dạy, lịch thi Trương Thanh Phong – ĐT: 0813 880 150 – Email: tt.phong@hutech.edu.vn\n" +
                "Hợp đồng giảng dạy, hồ sơ giảng viên Nguyễn Thị Trang – ĐT: 0934 041 776 – Email: nt.trang88@hutech.edu.vn\n" +
                "Phạm Thị Hồng Thắm – ĐT: 0989 680 971 – Email: pth.tham@hutech.edu.vn\n" +
                "Công tác sinh viên Nguyễn Thanh Đăng – ĐT: 0969 685 815 – Email: nt.dang@hutech.edu.vn\n";

        ThumoiGiangParser.ParseResult result = ThumoiGiangParser.parseInvitationDocument(realOcrPdf);

        // 1. Kiểm tra nhận diện Giảng viên, Khoa, Trường
        assertEquals(SchoolProfile.SchoolType.HUTECH, result.school);
        assertEquals("Lý Quang Vinh", result.lecturerName);
        assertTrue(result.facultyName.contains("Công nghệ Thông tin"));
        assertTrue(result.termInfo.contains("Đợt B"));

        // 2. Kiểm tra bóc tách bảng đa dòng (Multi-line table parsing)
        assertEquals(2, result.items.size());

        ThumoiGiangParser.ParsedTeachingItem item1 = result.items.get(0);
        assertEquals("18", item1.group);
        assertEquals("02", item1.subGroup);
        assertEquals("CMP376", item1.subjectCode);
        assertEquals("Thực hành lập trình Web", item1.subjectName);
        assertEquals(24, item1.studentCount);
        assertEquals(5, item1.dayOfWeekVn);
        assertEquals(2, item1.startPeriod);
        assertEquals(5, item1.periodCount);
        assertEquals("E1-04.06/1", item1.room);

        ThumoiGiangParser.ParsedTeachingItem item2 = result.items.get(1);
        assertEquals("14", item2.group);
        assertEquals("04", item2.subGroup);
        assertEquals("CMP376", item2.subjectCode);
        assertEquals("Thực hành lập trình Web", item2.subjectName);
        assertEquals(27, item2.studentCount);
        assertEquals(5, item2.dayOfWeekVn);
        assertEquals(7, item2.startPeriod);
        assertEquals(5, item2.periodCount);
        assertEquals("E1-04.06/2", item2.room);

        // 3. Kiểm tra rải 12 buổi học + 2 hạn chót đã khử trùng lặp = 14 sự kiện
        assertEquals(14, result.generatedEvents.size());

        // Kiểm tra sự kiện ngày 14/05/2026: Thời gian nào trước (ca sáng 07:30) thì ở trên đầu trước ca chiều (12:30)
        List<ThumoiGiangParser.GeneratedClassEvent> day14Events = new ArrayList<>();
        for (ThumoiGiangParser.GeneratedClassEvent ev : result.generatedEvents) {
            if ("2026-05-14".equals(ev.dateIso) && !ev.isDeadline) {
                day14Events.add(ev);
}
}
        assertEquals(2, day14Events.size());
        assertTrue("Ca sáng (07:30) phải ở trên đầu trước ca chiều", day14Events.get(0).title.contains("Tiết 2-6"));
        assertTrue("Ca chiều (12:30) phải ở bên dưới", day14Events.get(1).title.contains("Tiết 7-11"));

        // 4. Kiểm tra cảnh báo tải trọng Thứ Năm có 10 tiết
        assertFalse(result.workloadWarnings.isEmpty());
        assertTrue(result.workloadWarnings.get(0).contains("10 tiết"));

        // 5. Kiểm tra ma trận phân tích lịch trống hàng tuần (Weekly Free Schedule Matrix)
        assertFalse(result.weeklyFreeSchedule.isEmpty());
        assertEquals(7, result.weeklyFreeSchedule.size());
        assertTrue(result.weeklyFreeSchedule.get(0).contains("Thứ 2: TRỐNG"));
        assertTrue(result.weeklyFreeSchedule.get(3).contains("Thứ 5: BẬN DẠY 10 TIẾT"));
        assertTrue(result.weeklyFreeSchedule.get(3).contains("Nghỉ giữa ca tại chỗ"));

        // 6. Kiểm tra danh bạ Thư ký & Hotline khoa
        assertTrue(result.contacts.size() >= 5);
}

    @Test
    public void testContinuousTimelineStreamGeneration() {
        List<EventModel> dayEvents = new ArrayList<>();
        // Ca sáng Tiết 2-6 (07:30 - 11:35) tại Khu E
        EventModel morningClass = new EventModel(1, 1, "[HUTECH] Thực hành lập trình Web (Lớp 23DTHC3) Phòng E1-04.06/1 Tiết 2-6", "2026-05-14", "HUTECH", "HK2", 2026, 20);
        // Ca chiều Tiết 7-11 (12:30 - 16:35) tại Khu E
        EventModel afternoonClass = new EventModel(2, 2, "[HUTECH] Thực hành lập trình Web (23DTHB6, 23DTHB7) Phòng E1-04.06/2 Tiết 7-11", "2026-05-14", "HUTECH", "HK2", 2026, 20);
        // Sự kiện phát sinh chiều tối: Họp bộ môn NTTU 18:00 - 19:30 Online
        EventModel eveningMeeting = new EventModel(3, 3, "[NTTU] Họp bộ môn KTPM (Online qua Zoom) 18:00 - 19:30", "2026-05-14", "Ban Chủ nhiệm Khoa", "HK2", 2026, 20);

        dayEvents.add(morningClass);
        dayEvents.add(afternoonClass);
        dayEvents.add(eveningMeeting);

        GranularTimelineEngine.DayTimelineSummary summary = GranularTimelineEngine.generateDayTimeline("2026-05-14", dayEvents);

        assertNotNull(summary.timelineSlots);
        assertTrue(summary.timelineSlots.size() >= 5);

        // 1. Trống đầu ngày: 06:45 - 07:30 (45 phút)
        TimelineSlot s0 = summary.timelineSlots.get(0);
        assertEquals(TimelineSlot.SlotType.START_DAY_FREE, s0.getSlotType());
        assertEquals(405, s0.getStartMin()); // 06:45
        assertEquals(450, s0.getEndMin());   // 07:30

        // 2. Ca sáng: 07:30 - 11:35
        TimelineSlot s1 = summary.timelineSlots.get(1);
        assertEquals(TimelineSlot.SlotType.BUSY_EVENT, s1.getSlotType());
        assertEquals(450, s1.getStartMin());
        assertEquals(695, s1.getEndMin());

        // 3. Nghỉ giữa ca tại chỗ (cùng cơ sở Khu E): 11:35 - 12:30 (55 phút)
        TimelineSlot s2 = summary.timelineSlots.get(2);
        assertEquals(TimelineSlot.SlotType.MICRO_FREE, s2.getSlotType());
        assertEquals(695, s2.getStartMin());
        assertEquals(750, s2.getEndMin());
        assertTrue(s2.getTitle().contains("NGHỈ GIỮA CA TẠI CHỖ"));

        // 4. Ca chiều: 12:30 - 16:35
        TimelineSlot s3 = summary.timelineSlots.get(3);
        assertEquals(TimelineSlot.SlotType.BUSY_EVENT, s3.getSlotType());
        assertEquals(750, s3.getStartMin());
        assertEquals(995, s3.getEndMin());

        // 5. Lịch trống khả dụng chiều: 16:35 - 18:00 (1 giờ 25 phút = 85 phút)
        TimelineSlot s4 = summary.timelineSlots.get(4);
        assertEquals(TimelineSlot.SlotType.DEEP_FREE, s4.getSlotType());
        assertEquals(995, s4.getStartMin());
        assertEquals(1080, s4.getEndMin());
        assertTrue(s4.getTitle().contains("LỊCH TRỐNG KHẢ DỤNG"));
}

    @Test
    public void testMultiSchoolRecognitionAndConflictDetection() {
        // 1. Kiểm tra nhận diện đa trường đại học
        assertEquals(SchoolProfile.SchoolType.NTTU, SchoolProfile.detectSchool("Trường Đại học Nguyễn Tất Thành - NTTU"));
        assertEquals(SchoolProfile.SchoolType.HUTECH, SchoolProfile.detectSchool("Trường ĐH Công nghệ TP.HCM - HUTECH"));
        assertEquals(SchoolProfile.SchoolType.UEF, SchoolProfile.detectSchool("Trường ĐH Kinh tế - Tài chính TP.HCM - UEF"));
        assertEquals(SchoolProfile.SchoolType.UEH, SchoolProfile.detectSchool("Đại học Kinh tế TP.HCM - Cơ sở B"));
        assertEquals(SchoolProfile.SchoolType.HCMUT, SchoolProfile.detectSchool("Trường ĐH Bách Khoa ĐHQG"));
        assertEquals(SchoolProfile.SchoolType.VLU, SchoolProfile.detectSchool("Trường Đại học Văn Lang - CS3 Đặng Thùy Trâm"));
        assertEquals(SchoolProfile.SchoolType.TDTU, SchoolProfile.detectSchool("Đại học Tôn Đức Thắng Q7"));
        assertEquals(SchoolProfile.SchoolType.HCMUTE, SchoolProfile.detectSchool("ĐH Sư phạm Kỹ thuật TP.HCM"));
        assertEquals(SchoolProfile.SchoolType.IUH, SchoolProfile.detectSchool("Trường Đại học Công nghiệp TP.HCM - IUH"));
        assertEquals(SchoolProfile.SchoolType.SGU, SchoolProfile.detectSchool("Trường Đại học Sài Gòn - SGU"));
        assertEquals(SchoolProfile.SchoolType.HCMUS, SchoolProfile.detectSchool("Đại học Khoa học Tự nhiên - KHTN"));
        assertEquals(SchoolProfile.SchoolType.USSH, SchoolProfile.detectSchool("ĐH Khoa học Xã hội và Nhân văn - USSH"));
        assertEquals(SchoolProfile.SchoolType.UIT, SchoolProfile.detectSchool("Trường ĐH Công nghệ Thông tin - UIT"));

        // 2. Kiểm tra nhận diện cơ sở trường khác nhau (Campus Buffer)
        CampusBufferHelper.CampusInfo vluCs3 = CampusBufferHelper.detectCampus("Phòng A-101 Đặng Thùy Trâm", "VLU");
        assertEquals("VLU_CS3", vluCs3.campusId);
        CampusBufferHelper.CampusInfo tdtuQ7 = CampusBufferHelper.detectCampus("Phòng C-202 Tôn Đức Thắng Q7", "TDTU");
        assertEquals("TDTU_Q7", tdtuQ7.campusId);
        assertFalse(CampusBufferHelper.isSameCampus(vluCs3, tdtuQ7));

        // 3. Kiểm tra tự động phát hiện Xung đột trùng giờ (Conflict Detection)
        List<EventModel> overlapEvents = new ArrayList<>();
        // Ca dạy chính khóa: 07:30 - 11:35
        overlapEvents.add(new EventModel(1, 1, "[HUTECH] Thực hành Lập trình Web Tiết 2-6", "2026-05-14", "HUTECH", "HK2", 2026, 20));
        // Lịch họp phát sinh đột xuất: 09:00 - 11:00 (Trùng khít vào giữa ca dạy)
        overlapEvents.add(new EventModel(2, 2, "Họp Hội đồng Đánh giá Đồ án Tốt nghiệp từ 09:00 đến 11:00", "2026-05-14", "VP Khoa", "HK2", 2026, 20));

        GranularTimelineEngine.DayTimelineSummary summary = GranularTimelineEngine.generateDayTimeline("2026-05-14", overlapEvents);
        assertNotNull(summary.timelineSlots);

        // Tìm ca họp trùng giờ
        boolean foundConflict = false;
        for (TimelineSlot slot : summary.timelineSlots) {
            if (slot.getBadgeText() != null && slot.getBadgeText().contains("XUNG ĐỘT")) {
                foundConflict = true;
                assertTrue(slot.getSuggestion().contains("CẢNH BÁO XUNG ĐỘT"));
                break;
}
}
        assertTrue("Hệ thống phải tự động phát hiện và gắn cờ Xung đột trùng giờ khi 2 sự kiện đè lên nhau", foundConflict);
}

    @Test
    public void testEventModelChronologicalSorting() {
        List<EventModel> list = new ArrayList<>();
        // Chiều: 12:30 - 16:35
        EventModel afternoon = new EventModel(1, 1, "Thực hành lập trình Web (Nhóm 14) Tiết 7-11 [12:30 - 16:35]", "2026-05-14", "HUTECH", "HK2", 2026, 20);
        // Sáng: 07:30 - 11:35
        EventModel morning = new EventModel(2, 2, "Thực hành lập trình Web (Nhóm 18) Tiết 2-6 [07:30 - 11:35]", "2026-05-14", "HUTECH", "HK2", 2026, 20);
        list.add(afternoon);
        list.add(morning);

        EventModel.sortChronologically(list);

        assertEquals("Ca sáng (07:30) phải ở trên đầu trước", 2, list.get(0).getId());
        assertEquals("Ca chiều (12:30) phải ở bên dưới", 1, list.get(1).getId());
        assertEquals("07:30", list.get(0).getStartTime());
        assertEquals("12:30", list.get(1).getStartTime());
}

    @Test
    public void testSameDayDateHeaderGrouping() {
        List<EventModel> list = new ArrayList<>();
        list.add(new EventModel(1, 1, "Nhóm 18", "2026-05-14", "HUTECH", "HK2", 2026, 20));
        list.add(new EventModel(2, 2, "Nhóm 14", "2026-05-14", "HUTECH", "HK2", 2026, 20));
        list.add(new EventModel(3, 3, "Nhóm 18", "2026-05-21", "HUTECH", "HK2", 2026, 21));

        EventModel.sortChronologically(list);

        // Giả lập logic kiểm tra isFirstOfDay của EventAdapter
        boolean[] isFirstOfDay = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                isFirstOfDay[i] = true;
} else {
                isFirstOfDay[i] = !list.get(i).getDate().trim().equals(list.get(i - 1).getDate().trim());
}
}

        assertTrue("Sự kiện đầu tiên của ngày 14/05/2026 phải hiển thị badge ngày", isFirstOfDay[0]);
        assertFalse("Sự kiện thứ hai cùng ngày 14/05/2026 KHÔNG được lặp lại badge ngày", isFirstOfDay[1]);
        assertTrue("Sự kiện ngày mới 21/05/2026 phải hiển thị badge ngày mới", isFirstOfDay[2]);
}

    @Test
    public void testParseEventDetailsRowByRow() {
        // Kiểm tra đúng định dạng hàng riêng biệt như người dùng yêu cầu:
        // Tiêu đề: Thực hành lập trình Web (23DTHC3)
        // Tiết: 2-6
        // Thời gian: 07:30 - 11:35
        // Nhóm: 18/Tổ 02 - 24 SV
        // Phòng: E1-04.06/1
        // Tổ chức: [HUTECH] Khoa Công nghệ Thông tin • VLQ0230284.pdf
        String rawTitle18 = "Thực hành lập trình Web (23DTHC3 • Nhóm 18/Tổ 02 • 24 SV) - Phòng E1-04.06/1 | Tiết 2-6 [07:30 - 11:35]";
        EventModel event18 = new EventModel(1, 1, rawTitle18, "2026-05-14", "[HUTECH] Khoa Công nghệ Thông tin • VLQ0230284.pdf", "HK2", 2026, 20);

        EventModel.EventFieldBreakdown bd18 = EventModel.parseEventDetails(event18);
        assertEquals("Thực hành lập trình Web (23DTHC3)", bd18.title);
        assertEquals("2-6", bd18.period);
        assertEquals("07:30 - 11:35", bd18.time);
        assertEquals("18/Tổ 02 - 24 SV", bd18.group);
        assertEquals("E1-04.06/1", bd18.room);
        assertEquals("[HUTECH] Khoa Công nghệ Thông tin • VLQ0230284.pdf", bd18.organizer);

        // Kiểm tra Nhóm 14 ca chiều
        String rawTitle14 = "Thực hành lập trình Web (23DTHB6,23DTHB7 • Nhóm 14/Tổ 04 • 27 SV) - Phòng E1-04.06/2 | Tiết 7-11 [12:30 - 16:35]";
        EventModel event14 = new EventModel(2, 2, rawTitle14, "2026-05-14", "[HUTECH] Khoa Công nghệ Thông tin • VLQ0230284.pdf", "HK2", 2026, 20);

        EventModel.EventFieldBreakdown bd14 = EventModel.parseEventDetails(event14);
        assertEquals("Thực hành lập trình Web (23DTHB6,23DTHB7)", bd14.title);
        assertEquals("7-11", bd14.period);
        assertEquals("12:30 - 16:35", bd14.time);
        assertEquals("14/Tổ 04 - 27 SV", bd14.group);
        assertEquals("E1-04.06/2", bd14.room);
}

    @Test
    public void testTeachingVsNonTeachingClassification() {
        // 1. Dòng lịch dạy đại học chuẩn: phải nhận diện là Teaching Event và phân loại CAT_TEACHING
        EventModel teachingEvent = new EventModel(1, 1, "Thực hành Lập trình Web (23DTHC3) - Phòng E1-04.06/1 | Tiết 2-6", "2026-05-14", "HUTECH", "HK2", 2026, 20);
        assertTrue(teachingEvent.isTeachingEvent());
        assertEquals("FIXED_TEACHING", teachingEvent.getPriorityType());
        assertEquals(5, MainActivity.extractPeriodsCount(teachingEvent));
        assertEquals(EventModel.CAT_TEACHING, MainActivity.classifyEventNature(teachingEvent.getTitle(), teachingEvent.getOrganizer()));

        // 2. Dòng trích xuất từ CV / hồ sơ cá nhân: TUYỆT ĐỐI KHÔNG PHẢI TEACHING EVENT
        EventModel cvWorkExp = new EventModel(2, 2, "Kinh nghiệm làm việc: Công ty CP Công nghệ ABC", "2026-05-14", "💼 Hồ sơ cá nhân • CV_NguyenVanA.pdf", "HK2", 2026, 20);
        assertFalse(cvWorkExp.isTeachingEvent());
        assertEquals("PERSONAL_TASK", cvWorkExp.getPriorityType());
        assertEquals(0, MainActivity.extractPeriodsCount(cvWorkExp)); // 0 Tiết!
        assertEquals(EventModel.CAT_MANUAL_OTHER, MainActivity.classifyEventNature(cvWorkExp.getTitle(), cvWorkExp.getOrganizer()));
        assertEquals("OTHER", cvWorkExp.getSchoolCode());

        // 3. Dòng thông tin cá nhân trong CV (dù có nhắc đến trường ĐH Nguyễn Tất Thành)
        EventModel cvEdu = new EventModel(3, 3, "Là sinh viên của Trường: Đại học Nguyễn Tất Thành - Năm thứ: 2", "2026-05-14", "💼 Hồ sơ cá nhân • CV_Profile.pdf", "HK2", 2026, 20);
        assertFalse(cvEdu.isTeachingEvent());
        assertEquals(0, MainActivity.extractPeriodsCount(cvEdu));
        assertEquals(EventModel.CAT_MANUAL_OTHER, MainActivity.classifyEventNature(cvEdu.getTitle(), cvEdu.getOrganizer()));
        assertEquals("OTHER", cvEdu.getSchoolCode()); // Không gán là trường NTTU dạy học
}

    @Test
    public void testFileValidation_RejectCvAndUnrelated() {
        // Test 1: CV theo tên file
        MainActivity.FileValidationResult res1 = MainActivity.validateScheduleDocument("Nội dung bất kỳ", "CV_LeThiB.pdf");
        assertFalse("Phải từ chối file có tên chứa CV", res1.isValid);
        assertTrue(res1.rejectReason.contains("Hồ sơ cá nhân / Sơ yếu lý lịch / CV"));

        // Test 2: CV theo nội dung
        String cvBody = "BẢN THÔNG TIN CÁ NHÂN\n" +
                "Họ và tên: Trần Văn C\n" +
                "Dân tộc: Kinh - Tôn giáo: Không\n" +
                "Quê quán: Bến Tre - Nơi sinh: TP.HCM\n" +
                "Kinh nghiệm làm việc: 3 năm thiết kế đồ họa\n" +
                "Mục tiêu nghề nghiệp: Trở thành chuyên gia mỹ thuật";
        MainActivity.FileValidationResult res2 = MainActivity.validateScheduleDocument(cvBody, "document_scan.docx");
        assertFalse("Phải từ chối file có nội dung CV", res2.isValid);

        // Test 3: File rỗng hoặc không có dữ liệu lịch trình
        MainActivity.FileValidationResult res3 = MainActivity.validateScheduleDocument("Cách nấu món canh chua cá lóc miền Tây thơm ngon", "huong_dan_nau_an.txt");
        assertFalse("Phải từ chối file không liên quan lịch trình", res3.isValid);
}

    @Test
    public void testFileValidation_AcceptTeachingAndEvents() {
        // Test 1: Thư mời giảng đại học hợp lệ
        String tkb = "TRƯỜNG ĐẠI HỌC CÔNG NGHỆ TP.HCM\n" +
                "THƯ MỜI GIẢNG DẠY HỌC KỲ 2\n" +
                "Môn: Lập trình Web - Nhóm lớp: 23DTHC3\n" +
                "Tiết BĐ: 2 - Số tiết: 5 - Phòng: E1-04.06/1\n" +
                "Thời gian: 14/05/2026 đến 18/06/2026";
        MainActivity.FileValidationResult res1 = MainActivity.validateScheduleDocument(tkb, "ThuMoiGiang_HK2.pdf");
        assertTrue("Phải chấp nhận thư mời giảng", res1.isValid);
        assertEquals(EventModel.CAT_TEACHING, res1.detectedType);

        // Test 2: Thông báo sự kiện trường
        String eventNotice = "TRƯỜNG ĐẠI HỌC NGUYỄN TẤT THÀNH\n" +
                "THÔNG BÁO VỀ VIỆC TỔ CHỨC LỄ KHAI GIẢNG NĂM HỌC 2026-2027\n" +
                "Thời gian: 05/09/2026\n" +
                "Chủ trì: Ban Giám hiệu\n" +
                "Địa điểm: Hội trường A, Cơ sở An Phú Đông";
        MainActivity.FileValidationResult res2 = MainActivity.validateScheduleDocument(eventNotice, "ThongBao_KhaiGiang.docx");
        assertTrue("Phải chấp nhận thông báo sự kiện", res2.isValid);
        assertEquals(EventModel.CAT_EVENT, res2.detectedType);

        // Test 3: Lịch phát sinh (Dạy bù / Coi thi)
        String incidentSchedule = "KHOA CÔNG NGHỆ THÔNG TIN\n" +
                "LỊCH DẠY BÙ & PHÂN CÔNG COI THI KẾT THÚC HỌC PHẦN\n" +
                "Ngày thi: 25/06/2026\n" +
                "Phòng thi: B-02.01 - Ca thi: 2";
        MainActivity.FileValidationResult res3 = MainActivity.validateScheduleDocument(incidentSchedule, "LichCoiThi_HK2.xlsx");
        assertTrue("Phải chấp nhận lịch phát sinh", res3.isValid);
        assertEquals(EventModel.CAT_INCIDENT, res3.detectedType);
}

    @Test
    public void testFourUnifiedScheduleCategories() {
        // 1. Lịch dạy/học
        assertEquals(EventModel.CAT_TEACHING, MainActivity.classifyEventNature("Thực hành Lập trình Web (23DTHC3) - Phòng E1-04.06/1 | Tiết 2-6", "Khoa CNTT"));
        assertEquals(EventModel.CAT_TEACHING, MainActivity.classifyEventNature("Lý thuyết Hệ điều hành - Tiết 7-9", "Bộ môn Mạng"));

        // 2. Lịch sự kiện
        assertEquals(EventModel.CAT_EVENT, MainActivity.classifyEventNature("Hội nghị Cán bộ Viên chức Nhà trường", "Ban Giám hiệu"));
        assertEquals(EventModel.CAT_EVENT, MainActivity.classifyEventNature("Lễ Tốt nghiệp Khóa 2022", "Phòng Đào tạo"));
        assertEquals(EventModel.CAT_EVENT, MainActivity.classifyEventNature("Họp giao ban Khoa CNTT tháng 5", "Văn phòng Khoa"));

        // 3. Lịch phát sinh
        assertEquals(EventModel.CAT_INCIDENT, MainActivity.classifyEventNature("Dạy bù môn Lập trình Web lớp 23DTHC3", "Giảng viên"));
        assertEquals(EventModel.CAT_INCIDENT, MainActivity.classifyEventNature("Phân công coi thi học phần Cơ sở dữ liệu", "Phòng Khảo thí"));
        assertEquals(EventModel.CAT_INCIDENT, MainActivity.classifyEventNature("Hội đồng bảo vệ Đồ án tốt nghiệp K21", "Khoa CNTT"));

        // 4. Lịch nhập thủ công(khác)
        assertEquals(EventModel.CAT_MANUAL_OTHER, MainActivity.classifyEventNature("Khám sức khỏe tổng quát định kỳ", "Lịch nhập thủ công"));
        assertEquals(EventModel.CAT_MANUAL_OTHER, MainActivity.classifyEventNature("Soạn ngân hàng đề thi cuối kỳ", "Lịch nhập thủ công"));
        assertEquals(EventModel.CAT_MANUAL_OTHER, MainActivity.classifyEventNature("Chuẩn bị tài liệu bài giảng tuần sau", "Cá nhân"));
}

    @Test
    public void testPeriodExtractionDoesNotMatchChiTiet() {
        // Kiểm tra từ "chi tiết 1 - 2 năm kinh nghiệm" trong CV KHÔNG bị nhận nhầm thành Tiết 1-2
        TimeNormalizer.NormalizedRange range = TimeNormalizer.parseFromText("Thông tin chi tiết: 1 - 2 năm kinh nghiệm lập trình Android", SchoolProfile.SchoolType.DEFAULT);
        assertFalse("Từ 'chi tiết' không được coi là Tiết học", range.isPeriodBased);

        EventModel dummyCv = new EventModel(4, 4, "Chi tiết công việc: 1 - 2 dự án thương mại", "2026-05-14", "Hồ sơ", "HK2", 2026, 20);
        assertEquals(0, MainActivity.extractPeriodsCount(dummyCv));
        EventModel.EventFieldBreakdown bd = EventModel.parseEventDetails(dummyCv);
        assertEquals("", bd.period); // Không bắt 'chi tiết' thành period
}

    @Test
    public void testDocumentTypeDetection() {
        // Nhận diện CV
        String cvText = "CURRICULUM VITAE\n" +
                "Họ và tên: Nguyễn Văn A\n" +
                "Ngày sinh: 10/04/2000\n" +
                "Nơi sinh: TP.HCM\n" +
                "Mục tiêu nghề nghiệp: Phát triển phần mềm\n" +
                "Kinh nghiệm làm việc: 2 năm tại công ty X\n" +
                "Kỹ năng: Java, Android, SQL\n";
        assertEquals(MainActivity.DocumentType.CV_OR_PROFILE, MainActivity.detectDocumentType(cvText));

        // Nhận diện Thư mời giảng / Thời khóa biểu
        String tkbText = "TRƯỜNG ĐẠI HỌC CÔNG NGHỆ TP.HCM\n" +
                "THƯ MỜI GIẢNG DẠY HỌC KỲ 2B\n" +
                "Giảng viên: Lý Quang Vinh\n" +
                "Mã MH: CMP376 - Tín chỉ: 1 - Sĩ số: 27\n" +
                "Thứ 5 - Tiết BĐ: 7 - Số tiết: 5 - Phòng E1-04.06/2\n";
        assertEquals(MainActivity.DocumentType.TEACHING_SCHEDULE, MainActivity.detectDocumentType(tkbText));
}

    @Test
    public void testFileEventRegexMatching() {
        String testOrganizer = "📄 Tài liệu PDF Văn bản số (.pdf) • CV_Js_Trung_Kien_EN (1).pdf";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("([a-zA-Z0-9_ -]+?(?:\\s*\\([0-9]+\\))?\\.(?:pdf|xlsx|xls|docx|pptx|jpg|jpeg|png|csv|txt))", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(testOrganizer);
        assertTrue(m.find());
        assertEquals("CV_Js_Trung_Kien_EN (1).pdf", m.group(1).trim());

        String testOrganizer2 = "✍️ Ghi tay • ✍️ Ảnh chụp chữ ghi tay (.jpg) (1000026181.jpg)";
        java.util.regex.Matcher m2 = p.matcher(testOrganizer2);
        assertTrue(m2.find());
        assertEquals("1000026181.jpg", m2.group(1).trim());
}

    @Test
    public void testFourMainCategoriesClassification() {
        // 1. Lịch dạy/học
        assertEquals(EventModel.CAT_TEACHING, MainActivity.classifyEventNature("Thực hành Lập trình Android (Tiết 1-5)", "Khoa CNTT - HUTECH"));
        // 2. Lịch sự kiện (Họp Khoa, Hội nghị...)
        assertEquals(EventModel.CAT_EVENT, MainActivity.classifyEventNature("Họp Ban Chủ nhiệm Khoa định kỳ", "Khoa CNTT"));
        // 3. Lịch phát sinh (Coi thi, Dạy bù, Hội đồng chấm thi...)
        assertEquals(EventModel.CAT_INCIDENT, MainActivity.classifyEventNature("Coi thi kết thúc học phần môn Mạng máy tính", "Phòng Khảo thí"));
        assertEquals(EventModel.CAT_INCIDENT, MainActivity.classifyEventNature("Dạy bù Thực hành Web", "Bộ môn"));
        // 4. Lịch nhập thủ công(khác)
        assertEquals(EventModel.CAT_MANUAL_OTHER, MainActivity.classifyEventNature("Đi khám sức khỏe định kỳ", "Lịch nhập thủ công"));
}

    @Test
    public void testUniversalScheduleParser_Screenshot4SubjectsExact() {
        // Mô phỏng chính xác 100% dữ liệu văn bản OCR từ ảnh chụp màn hình lịch học ngày thứ 4 28/10
        String ocrScreenshotText = "01:05\n" +
                "2026 | Tuần 43 | Ngày | Tuần | Tháng\n" +
                "Th 4, 28/10\n" +
                "• Lịch học\n" +
                "Học sâu và ứng dụng\n" +
                "Tiết: 1 - 3\n" +
                "Phòng: L.503\n" +
                "Giảng viên: Đặng Như Phú\n" +
                "• Lịch học\n" +
                "Lập trình trên thiết bị di động\n" +
                "Tiết: 4 - 6\n" +
                "Phòng: L2.123\n" +
                "Giảng viên: Lý Quang Vinh\n" +
                "• Lịch học\n" +
                "Tiếng Anh chuyên ngành\n" +
                "Tiết: 7 - 11\n" +
                "Phòng: L.604\n" +
                "Giảng viên: Cao Văn Kiên\n" +
                "• Lịch trực tuyến\n" +
                "Sinh hoạt cố vấn học tập\n" +
                "Tiết: 13 - 13\n" +
                "Phòng: E-LEARNING 15\n" +
                "Trang chủ | Lịch học | Điểm danh | Cá nhân\n" +
                "Tải file về máy\n" +
                "Mở bằng App khác\n" +
                "Đóng cửa sổ\n";

        List<UniversalScheduleParser.ParsedScheduleEvent> events =
                UniversalScheduleParser.parseDocument(ocrScreenshotText, "LichHoc_Th4_2810.png");

        // KHẲNG ĐỊNH: Chỉ bóc tách đúng 4 môn học, không bao giờ phân mảnh thành 24 dòng rác
        assertEquals(4, events.size());

        // Môn 1: Học sâu và ứng dụng
        UniversalScheduleParser.ParsedScheduleEvent ev1 = events.get(0);
        assertEquals("Học sâu và ứng dụng", ev1.subjectName);
        assertEquals("2026-10-28", ev1.dateIso);
        assertEquals(1, ev1.startPeriod);
        assertEquals(3, ev1.endPeriod);
        assertEquals(3, ev1.periodCount);
        assertEquals("L.503", ev1.room);
        assertEquals("Đặng Như Phú", ev1.lecturer);
        assertEquals("07:00 - 09:30", ev1.timeRangeStr);
        assertEquals(SchoolProfile.SchoolType.NTTU, ev1.school);
        assertTrue(ev1.title.contains("Học sâu và ứng dụng"));
        assertTrue(ev1.title.contains("Phòng L.503"));
        assertTrue(ev1.title.contains("Tiết 1-3"));
        assertTrue(ev1.title.contains("[07:00 - 09:30]"));
        assertTrue(ev1.organizer.contains("Đặng Như Phú"));

        // Môn 2: Lập trình trên thiết bị di động
        UniversalScheduleParser.ParsedScheduleEvent ev2 = events.get(1);
        assertEquals("Lập trình trên thiết bị di động", ev2.subjectName);
        assertEquals("2026-10-28", ev2.dateIso);
        assertEquals(4, ev2.startPeriod);
        assertEquals(6, ev2.endPeriod);
        assertEquals("L2.123", ev2.room);
        assertEquals("Lý Quang Vinh", ev2.lecturer);
        assertEquals("09:40 - 12:10", ev2.timeRangeStr);
        assertTrue(ev2.title.contains("Phòng L2.123"));
        assertTrue(ev2.title.contains("Tiết 4-6"));
        assertTrue(ev2.title.contains("[09:40 - 12:10]"));

        // Môn 3: Tiếng Anh chuyên ngành
        UniversalScheduleParser.ParsedScheduleEvent ev3 = events.get(2);
        assertEquals("Tiếng Anh chuyên ngành", ev3.subjectName);
        assertEquals("2026-10-28", ev3.dateIso);
        assertEquals(7, ev3.startPeriod);
        assertEquals(11, ev3.endPeriod);
        assertEquals(5, ev3.periodCount);
        assertEquals("L.604", ev3.room);
        assertEquals("Cao Văn Kiên", ev3.lecturer);
        assertEquals("13:00 - 17:20", ev3.timeRangeStr);

        // Môn 4: Sinh hoạt cố vấn học tập
        UniversalScheduleParser.ParsedScheduleEvent ev4 = events.get(3);
        assertEquals("Sinh hoạt cố vấn học tập", ev4.subjectName);
        assertEquals("2026-10-28", ev4.dateIso);
        assertEquals(13, ev4.startPeriod);
        assertEquals(13, ev4.endPeriod);
        assertEquals(1, ev4.periodCount);
        assertEquals("E-LEARNING 15", ev4.room);
        assertEquals("18:15 - 19:05", ev4.timeRangeStr);
        assertTrue(ev4.organizer.contains("Lịch trực tuyến"));
}

    @Test
    public void testUniversalScheduleParser_FoldedLabelsAndPipeDelimited() {
        String testText = "2026\n" +
                "Thứ Tư, 28/10/2026\n" +
                "Học sâu và ứng dụng\n" +
                "Tiết:\n" +
                "1 - 3\n" +
                "Phòng:\n" +
                "L.503\n" +
                "Giảng viên:\n" +
                "Đặng Như Phú\n" +
                "Lập trình trên thiết bị di động | Tiết: 4 - 6 | Phòng: L2.123 | Giảng viên: Lý Quang Vinh\n";

        List<UniversalScheduleParser.ParsedScheduleEvent> events =
                UniversalScheduleParser.parseDocument(testText, "sample.txt");
        assertEquals(2, events.size());
        assertEquals("Học sâu và ứng dụng", events.get(0).subjectName);
        assertEquals("L.503", events.get(0).room);
        assertEquals(1, events.get(0).startPeriod);
        assertEquals(3, events.get(0).endPeriod);

        assertEquals("Lập trình trên thiết bị di động", events.get(1).subjectName);
        assertEquals("L2.123", events.get(1).room);
        assertEquals(4, events.get(1).startPeriod);
        assertEquals(6, events.get(1).endPeriod);
}

    @Test
    public void testInstitutionalBoilerplateRejection() {
        // Các dòng tiêu đề văn bản và biểu mẫu đào tạo
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("BM01/QT02/ĐT"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("BM-P.ĐT/02/07"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("THƯ MỜI GIẢNG"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Kính gửi Thầy/Cô: Lý Quang Vinh"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Khoa Công nghệ Thông tin - HUTECH kính mời Thầy/Cô tham gia giảng dạy những lớp sau:"));

        // Tiêu đề cột bảng
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("TT Nhóm Tổ TH Mã MH Môn Tín chỉ Sĩ số Thứ Tiết bắt đầu Số tiết Phòng Tên Lớp Ngày học"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("BUỔI CA TIẾT GIỜ HỌC"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Nội dung Trọng số"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Công việc Phụ trách"));

        // Bảng quy chuẩn chuông báo giờ của trường (Không phải buổi học cụ thể)
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Sáng"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Chiều"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Tối"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Ca 1"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("1  06h45 – 07h30"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("2  07h30 – 08h15"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("7  12h30 – 13h15"));

        // Địa điểm cơ sở và thông báo ngày nghỉ lễ
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("• ĐỊA ĐIỂM:"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("+ Sai Gon Campus: Các phòng ký hiệu A, B, C- : Tòa nhà A, B, C Cơ sở 475A - Điện Biên Phủ - P. Thạnh Mỹ Tây - TP.HCM"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("• CÁC NGÀY NGHỈ LỄ TRONG HK2B:"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("+ Ngày Giải phóng Miền Nam và Quốc tế Lao động: 30/04/2026, 01/05/2026, 02/05/2026, 03/05/2026"));

        // Quy chế ghi chú điểm số và chữ ký
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("• GHI CHÚ"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("1. Điểm học phần được qui định như sau:"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Điểm quá trình: Các hình thức đánh giá quá trình và trọng số"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("50%"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Điểm thi kết thúc học phần 50%"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Sinh viên có quyền khiếu nại về điểm quá trình trực tiếp với giảng viên"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("PGS.TS. Võ Đình Bảy"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("TRƯỞNG KHOA"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("TP. HCM, ngày 14 tháng 04 năm 2026"));

        // Danh bạ thư ký
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Văn phòng khoa Công nghệ Thông tin – E1-02.06"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Điện thoại liên hệ: 028 6686 6176"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Thư ký phụ trách:"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Thời khóa biểu, thù lao  Võ Hồng Linh Ân – ĐT: 0348 913 380 – Email: vhl.an@hutech.edu.vn"));
        assertTrue(UniversalScheduleParser.isInstitutionalBoilerplate("Công tác sinh viên Nguyễn Thanh Đăng – ĐT: 0969 685 815 – Email: nt.dang@hutech.edu.vn"));

        // Dòng môn học thật sự: KHÔNG được coi là boilerplate!
        assertFalse(UniversalScheduleParser.isInstitutionalBoilerplate("1 14 04 CMP376 Thực hành lập trình Web 1 27 5 7 5 E1-04.06/2 23DTHB6,23DTHB7 14/05/2026 - 18/06/2026"));
        assertFalse(UniversalScheduleParser.isInstitutionalBoilerplate("Học sâu và ứng dụng"));
}

    @Test
    public void testDocxAndPptxTableParsingWithPipes() {
        String docxText = "KHOA CÔNG NGHỆ THÔNG TIN - HUTECH\n" +
                "THƯ MỜI GIẢNG\n" +
                "Kính gửi Thầy/Cô: Lý Quang Vinh\n" +
                "TT | Nhóm | Tổ TH | Mã MH | Môn | Tín chỉ | Sĩ số | Thứ | Tiết bắt đầu | Số tiết | Phòng | Tên Lớp | Ngày học\n" +
                "1 | 14 | 04 | CMP376 | Thực hành lập trình Web | 1 | 27 | 5 | 7 | 5 | E1-04.06/2 | 23DTHB6,23DTHB7 | 14/05/2026 - 18/06/2026\n" +
                "2 | 18 | 02 | CMP376 | Thực hành lập trình Web | 1 | 24 | 5 | 2 | 5 | E1-04.06/1 | 23DTHC3 | 14/05/2026 - 18/06/2026\n";

        ThumoiGiangParser.ParseResult res = ThumoiGiangParser.parseInvitationDocument(docxText);
        assertEquals(2, res.items.size());

        // Do ThumoiGiangParser tự động sắp xếp theo thứ tự ca sáng học trước ca chiều học sau:
        // Item 0: Ca sáng Tiết 2 - 6, phòng E1-04.06/1 (Nhóm 18)
        assertEquals("CMP376", res.items.get(0).subjectCode);
        assertEquals("Thực hành lập trình Web", res.items.get(0).subjectName);
        assertEquals(5, res.items.get(0).periodCount);
        assertEquals(2, res.items.get(0).startPeriod);
        assertEquals("E1-04.06/1", res.items.get(0).room);

        // Item 1: Ca chiều Tiết 7 - 11, phòng E1-04.06/2 (Nhóm 14)
        assertEquals("CMP376", res.items.get(1).subjectCode);
        assertEquals("Thực hành lập trình Web", res.items.get(1).subjectName);
        assertEquals(5, res.items.get(1).periodCount);
        assertEquals(7, res.items.get(1).startPeriod);
        assertEquals("E1-04.06/2", res.items.get(1).room);

        // Đảm bảo sinh đúng các buổi học hàng tuần và deadline
        assertTrue(res.generatedEvents.size() > 0);
}

    @Test
    public void testVlqPdfExtraction_ExactClassesAndNoBoilerplate() {
        java.io.File pdfFile = new java.io.File("D:\\Android_NTTU\\LTDD_DAN\\VLQ0230284.pdf");
        if (!pdfFile.exists()) return;

        try {
            org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(pdfFile);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            stripper.setSortByPosition(false);
            String fullText = stripper.getText(doc);
            doc.close();

            // 1. Phân tích văn bản gốc của file PDF Thư Mời Giảng
            ThumoiGiangParser.ParseResult result = ThumoiGiangParser.parseInvitationDocument(fullText);

            // KHẲNG ĐỊNH: Bóc tách chính xác 2 lớp học phần (Nhóm 18 ca sáng và Nhóm 14 ca chiều)
            assertEquals(2, result.items.size());
            assertEquals("18", result.items.get(0).group);
            assertEquals("02", result.items.get(0).subGroup);
            assertEquals("CMP376", result.items.get(0).subjectCode);
            assertEquals("Thực hành lập trình Web", result.items.get(0).subjectName);
            assertEquals("E1-04.06/1", result.items.get(0).room);
            assertEquals(2, result.items.get(0).startPeriod);
            assertEquals(5, result.items.get(0).periodCount);

            assertEquals("14", result.items.get(1).group);
            assertEquals("04", result.items.get(1).subGroup);
            assertEquals("CMP376", result.items.get(1).subjectCode);
            assertEquals("Thực hành lập trình Web", result.items.get(1).subjectName);
            assertEquals("E1-04.06/2", result.items.get(1).room);
            assertEquals(7, result.items.get(1).startPeriod);
            assertEquals(5, result.items.get(1).periodCount);

            // Tổng số buổi học được rải tuần (6 tuần từ 14/05/2026 đến 18/06/2026 cho 2 lớp = 12 buổi) + 2 hạn chót hành chính = 14 sự kiện
            assertEquals(14, result.generatedEvents.size());

            // 2. Kiểm tra bộ lọc boilerplate trên toàn bộ 3 trang văn bản:
            // Toàn bộ các dòng thông tin hành chính, quy chế điểm số, ghi chú, chữ ký và chuông báo đều bị chặn triệt để
            String[] lines = fullText.split("\\r?\\n");
            int nonBoilerplateLines = 0;
            for (String line : lines) {
                String t = line.trim();
                if (t.length() >= 4 && !UniversalScheduleParser.isNoiseLine(t) && !UniversalScheduleParser.isInstitutionalBoilerplate(t)) {
                    nonBoilerplateLines++;
}
}
            // Chỉ có các mảnh của 2 dòng bảng môn học mới vượt qua bộ lọc
            assertTrue("Không có dòng quy chế, biểu mẫu hay chuông báo giờ học nào lọt qua bộ lọc", nonBoilerplateLines <= 14);

} catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
}
}

    @Test
    public void testHealVietnameseSubjectTitleAndOcrErrors() {
        // 1. Kiểm tra khắc phục lỗi mất nét/khoảng trắng OCR đơn lẻ
        assertEquals("Thực hành lập trình Web", ThumoiGiangParser.healVietnameseText("Thực hà h lập rì h Web"));
        assertEquals("Thực hành lập trình Web", ThumoiGiangParser.healVietnameseText("Thực hà h | lập rì h | Web"));
        assertEquals("Thực hành lập trình Web", ThumoiGiangParser.healVietnameseText("Thực hà h \n lập rì h \n Web"));
        assertEquals("Thực hành", ThumoiGiangParser.healVietnameseText("Thực hà h"));
        assertEquals("lập trình Web", ThumoiGiangParser.healVietnameseText("lập rì h Web"));

        // 2. Kiểm tra trên tiêu đề sự kiện đầy đủ: Giữ nguyên các ký tự đặc biệt, phòng, tiết, giờ nhưng sửa đúng tên môn
        String fullRawTitle = "Thực hà h lập rì h Web (23DTHB6,23DTHB7 • Nhóm 14/Tổ 04 • 27 SV) - Phòng E1-04.06/2 | Tiết 7-11 [12:30 - 16:35]";
        String healed = ThumoiGiangParser.healVietnameseText(fullRawTitle);
        assertEquals("Thực hành lập trình Web (23DTHB6,23DTHB7 • Nhóm 14/Tổ 04 • 27 SV) - Phòng E1-04.06/2 | Tiết 7-11 [12:30 - 16:35]", healed);

        // 3. Kiểm tra trích xuất Thư mời giảng khi văn bản đầu vào bị lỗi OCR "Thực hà h lập rì h Web"
        String corruptedPdfOcr = "KHOA CÔNG NGHỆ THÔNG TIN - HUTECH\n" +
                "THƯ MỜI GIẢNG\n" +
                "Kính gửi Thầy/Cô: Lý Quang Vinh\n" +
                "1 14 04 CMP376 Thực hà h lập rì h Web 1 27 5 7 5 E1-04.06/2 23DTHB6,23DTHB7 14/05/2026 - 18/06/2026\n" +
                "2 18 02 CMP376 Thực hà h lập rì h Web 1 24 5 2 5 E1-04.06/1 23DTHC3 14/05/2026 - 18/06/2026\n";

        ThumoiGiangParser.ParseResult pResult = ThumoiGiangParser.parseInvitationDocument(corruptedPdfOcr);
        assertEquals(2, pResult.items.size());
        assertEquals("Thực hành lập trình Web", pResult.items.get(0).subjectName);
        assertEquals("Thực hành lập trình Web", pResult.items.get(1).subjectName);

        for (ThumoiGiangParser.GeneratedClassEvent gEv : pResult.generatedEvents) {
            assertTrue("Tiêu đề buổi học phải chứa 'Thực hành lập trình Web'", gEv.title.contains("Thực hành lập trình Web"));
            assertFalse("Tiêu đề không được chứa 'hà h'", gEv.title.contains("hà h"));
            assertFalse("Tiêu đề không được chứa 'rì h'", gEv.title.contains("rì h"));
        }
    }

    @Test
    public void testEventModelClassificationAndCustomCategory() {
        // 1. Phân loại Lịch dạy/học
        EventModel emTeaching = new EventModel(1, 1, "Thực hành Lập trình Di động", "2026-05-14", "Khoa CNTT", "HK2", 2026, 20);
        emTeaching.setCategory(EventModel.CAT_TEACHING);
        assertEquals(EventModel.CAT_TEACHING, emTeaching.getScheduleCategory());
        assertTrue(emTeaching.isTeachingEvent());

        // 2. Phân loại Lịch sự kiện
        EventModel emEvent = new EventModel(2, 2, "Hội thảo chuyển đổi số trong giáo dục đại học", "2026-05-15", "Ban Giám Hiệu", "HK2", 2026, 20);
        emEvent.setCategory(EventModel.CAT_EVENT);
        assertEquals(EventModel.CAT_EVENT, emEvent.getScheduleCategory());
        assertFalse(emEvent.isTeachingEvent());

        // 3. Phân loại Lịch phát sinh
        EventModel emIncident = new EventModel(3, 3, "Dạy bù môn Lập trình Web ca tối", "2026-05-16", "Bộ môn Mạng", "HK2", 2026, 20);
        emIncident.setCategory(EventModel.CAT_INCIDENT);
        assertEquals(EventModel.CAT_INCIDENT, emIncident.getScheduleCategory());
        assertFalse(emIncident.isTeachingEvent());

        // 4. Phân loại Lịch khác (tự đặt tên)
        EventModel emCustom = new EventModel(4, 4, "Nghiên cứu khoa học cấp trường: Đề tài AI", "2026-05-17", "Phòng QLKH", "HK2", 2026, 20);
        emCustom.setCategory("Nghiên cứu NCKH");
        assertEquals("Nghiên cứu NCKH", emCustom.getScheduleCategory());
        assertFalse(emCustom.isTeachingEvent());

        // 5. Fallback khi chưa có category được gán
        EventModel emFallback = new EventModel(5, 5, "Họp chi bộ tháng 5", "2026-05-18", "Chi bộ Khoa", "HK2", 2026, 20);
        assertEquals(EventModel.CAT_EVENT, emFallback.getScheduleCategory());
    }

    @Test
    public void testLongContentPreservationWithoutTruncation() {
        String longTitle = "Hội nghị viên chức, người lao động năm học 2026 - 2027 | Báo cáo tổng kết công tác giảng dạy | Phương hướng nhiệm vụ năm mới (Hội trường lớn • Bắt buộc tham gia)";
        String longOrganizer = "Ban Giám Hiệu phối hợp cùng Công đoàn Cơ sở Trường Đại học và Đoàn Thanh niên";

        EventModel longEvent = new EventModel(10, 1, longTitle, "2026-05-20", longOrganizer, "HK2", 2026, 20);
        longEvent.setCategory("Hội nghị - Lễ kỷ niệm");

        EventModel.EventFieldBreakdown bd = EventModel.parseEventDetails(longEvent);

        // Kiểm tra toàn bộ nội dung dài và các ký tự đặc biệt (| • -) không bị cắt xén
        assertEquals("Nội dung tiêu đề dài không được bị cắt chữ", longTitle, bd.title);
        assertEquals("Đơn vị tổ chức dài không được bị cắt chữ", longOrganizer, bd.organizer);
    }

    @Test
    public void testManualEventRoomParsingAndTitleCleaning() {
        // 1. Kiểm tra sự kiện nhập tay có phòng có dấu gạch ngang (VD: E1-04) và phân loại Lịch phát sinh
        String rawTitle1 = "Test1 - Phòng E1-04 [07:30 - 11:35]";
        EventModel event1 = new EventModel(20, 20, rawTitle1, "2026-11-12", "ĐH Nguyễn Tất Thành", "HK1", 2026, 46);
        event1.setCategory(EventModel.CAT_INCIDENT);

        EventModel.EventFieldBreakdown bd1 = EventModel.parseEventDetails(event1);
        assertEquals("Test1", bd1.title);
        assertEquals("E1-04", bd1.room);
        assertEquals("07:30 - 11:35", bd1.time);

        // 2. Kiểm tra sự kiện có phòng dạng chuỗi phức tạp (VD: L2.503, Hội trường A)
        String rawTitle2 = "Họp chuyên môn - Phòng L2.503, Hội trường A [08:00 - 10:00]";
        EventModel event2 = new EventModel(21, 21, rawTitle2, "2026-11-12", "Khoa CNTT", "HK1", 2026, 46);
        event2.setCategory(EventModel.CAT_EVENT);

        EventModel.EventFieldBreakdown bd2 = EventModel.parseEventDetails(event2);
        assertEquals("Họp chuyên môn", bd2.title);
        assertEquals("L2.503, Hội trường A", bd2.room);

        // 3. Kiểm tra cleanDisplayTitle không làm cụt tên hoặc để sót ký tự rác
        assertEquals("Test1", EventModel.cleanDisplayTitle("Test1 - Phòng E1-04 [07:30 - 11:35]"));
        assertEquals("Test1", EventModel.cleanDisplayTitle("Test1 - Phòng L2.503"));
    }

    @Test
    public void testConflictDetection_NoFalsePositives_AndExactOverlaps() {
        // 1. Kiểm tra 2 sự kiện cùng ngày nhưng KHÔNG có giờ cụ thể -> KHÔNG ĐƯỢC báo trùng lịch
        EventModel noTime1 = new EventModel(1, 1, "Thực hành lập trình Web (23DTHC3)", "2026-05-14", "Khoa CNTT", "HK2", 2026, 20);
        EventModel noTime2 = new EventModel(2, 2, "Thực hành lập trình Web (23DTHB6)", "2026-05-14", "Khoa CNTT", "HK2", 2026, 20);

        // Đảm bảo cả 2 sự kiện không có giờ thì startMin <= 0
        assertTrue("Sự kiện không có giờ cụ thể không được có startMin > 0", noTime1.getStartMin() <= 0);
        assertTrue("Sự kiện không có giờ cụ thể không được có startMin > 0", noTime2.getStartMin() <= 0);

        // 2. Kiểm tra ca sáng (07:30 - 11:35) và ca chiều (12:30 - 16:35) cùng ngày -> KHÔNG trùng lịch (nghỉ trưa 55p)
        EventModel morning = new EventModel(3, 3, "Thực hành lập trình Web (23DTHC3) - Phòng E1-04.06/1 | Tiết 2-6 [07:30 - 11:35]", "2026-05-14", "[HUTECH] Khoa CNTT", "HK2", 2026, 20);
        EventModel afternoon = new EventModel(4, 4, "Thực hành lập trình Web (23DTHB6) - Phòng E1-04.06/2 | Tiết 7-11 [12:30 - 16:35]", "2026-05-14", "[HUTECH] Khoa CNTT", "HK2", 2026, 20);

        assertEquals("07:30", morning.getStartTime());
        assertEquals("11:35", morning.getEndTime());
        assertEquals("12:30", afternoon.getStartTime());
        assertEquals("16:35", afternoon.getEndTime());

        boolean morningAfternoonOverlap = morning.getStartMin() < afternoon.getEndMin() && morning.getEndMin() > afternoon.getStartMin();
        assertFalse("Ca sáng và ca chiều không được đánh dấu đè giờ nhau", morningAfternoonOverlap);

        // 3. Kiểm tra 2 sự kiện thực sự đè giờ nhau (07:30 - 11:35 và 09:00 - 11:00) -> PHẢI trùng lịch
        EventModel overlap1 = new EventModel(5, 5, "Thực hành lập trình Web | Tiết 2-6 [07:30 - 11:35]", "2026-05-14", "Khoa CNTT", "HK2", 2026, 20);
        EventModel overlap2 = new EventModel(6, 6, "Họp Hội đồng Đánh giá [09:00 - 11:00]", "2026-05-14", "Khoa CNTT", "HK2", 2026, 20);

        assertEquals(450, overlap1.getStartMin()); // 07:30
        assertEquals(695, overlap1.getEndMin());   // 11:35
        assertEquals(540, overlap2.getStartMin()); // 09:00
        assertEquals(660, overlap2.getEndMin());   // 11:00

        boolean isRealOverlap = overlap1.getStartMin() < overlap2.getEndMin() && overlap1.getEndMin() > overlap2.getStartMin();
        assertTrue("Hai sự kiện đè giờ nhau thực sự phải được gắn cờ trùng lịch", isRealOverlap);
    }
}


