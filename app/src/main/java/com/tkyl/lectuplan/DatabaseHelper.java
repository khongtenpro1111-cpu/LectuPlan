package com.tkyl.lectuplan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "LectuPlan.db";
    private static final int DB_VERSION = 4;
    public static final String TABLE_EVENTS = "events";
    public static final String TABLE_NOTIFICATIONS = "notifications";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_EVENTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "stt INTEGER, " +
                "title TEXT, " +
                "event_date TEXT, " + // Lưu định dạng yyyy-MM-dd để dễ so sánh và xóa tự động
                "organizer TEXT, " +
                "semester TEXT, " +
                "year INTEGER, " +
                "week INTEGER, " +
                "category TEXT)";
        db.execSQL(createTable);

        String createHistoryTable = "CREATE TABLE IF NOT EXISTS file_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT, " +
                "file_type TEXT, " +
                "record_count INTEGER, " +
                "timestamp TEXT)";
        db.execSQL(createHistoryTable);

        String createNotificationsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "timestamp TEXT, " +
                "is_read INTEGER DEFAULT 0, " +
                "type TEXT)";
        db.execSQL(createNotificationsTable);

        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE IF NOT EXISTS file_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT, " +
                "file_type TEXT, " +
                "record_count INTEGER, " +
                "timestamp TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "message TEXT, " +
                "timestamp TEXT, " +
                "is_read INTEGER DEFAULT 0, " +
                "type TEXT)");
        checkAndAddCategoryColumn(db);
        createIndexes(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            checkAndAddCategoryColumn(db);
            createIndexes(db);
            cleanUpGarbageCvEntries(db);
        }
    }

    private void checkAndAddCategoryColumn(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_EVENTS + ")", null);
            boolean hasCategory = false;
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1 && "category".equalsIgnoreCase(cursor.getString(nameIndex))) {
                        hasCategory = true;
                        break;
                    }
                }
                cursor.close();
            }
            if (!hasCategory) {
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN category TEXT");
            }
        } catch (Exception ignored) {}
    }

    public void cleanUpGarbageCvEntries(SQLiteDatabase db) {
        try {
            String deleteSql = "DELETE FROM " + TABLE_EVENTS + " WHERE " +
                    "title LIKE '%THÔNG TIN CÁ NH%' OR " +
                    "title LIKE '%HỌC BỔNG%' OR " +
                    "title LIKE '%HỌC BÔNG%' OR " +
                    "title LIKE '%Mẫu hồ sơ%' OR " +
                    "title LIKE '%Số hồ sơ%' OR " +
                    "title LIKE '%Ngày phát%' OR " +
                    "title LIKE '%Ngày nhận%' OR " +
                    "title LIKE '%Họ và tên (chữ in hoa)%' OR " +
                    "title LIKE '%Tên Thánh%' OR " +
                    "title LIKE '%NGUYỄN TRUNG KIÊN%' OR " +
                    "title LIKE '%Dân tộc:%' OR " +
                    "title LIKE '%Tôn giáo:%' OR " +
                    "title LIKE '%Địa chỉ thường trú:%' OR " +
                    "title LIKE '%Họ và tên Cha:%' OR " +
                    "title LIKE '%Họ và tên Mẹ:%' OR " +
                    "title LIKE '%Chi phí bình quân%' OR " +
                    "title LIKE '%Xác nhận của Linh mục%' OR " +
                    "title LIKE '%Thuộc Giáo xứ%' OR " +
                    "title LIKE '%Có vay Tín dụng%' OR " +
                    "title LIKE '%Hiện nay, có nhận học bổng%' OR " +
                    "title LIKE '%Nơi sinh:%' OR " +
                    "title LIKE '%Giới tính:%' OR " +
                    "title LIKE '%Năm nhập học:%' OR " +
                    "title LIKE '%Là sinh viên của Trường:%' OR " +
                    "title LIKE '%Kết quả học vấn%' OR " +
                    "title LIKE '%Có tham gia sinh hoạt%' OR " +
                    "title LIKE '%Xin Quý Cha%' OR " +
                    "title LIKE '%Lưu ý: Để nhận được học bổng%' OR " +
                    "title LIKE '%PROFESSIONAL OBJECTIVE%' OR " +
                    "title LIKE '%CAREER OBJECTIVE%' OR " +
                    "title LIKE '%WORK EXPERIENCE%' OR " +
                    "title LIKE '%Sơ yếu lý lịch%' OR " +
                    "title LIKE '%Curriculum Vitae%' OR " +
                    "organizer LIKE '%CV_%' OR " +
                    "organizer LIKE '%.cv.%' OR " +
                    "organizer LIKE '%curriculum vitae%' OR " +
                    "organizer LIKE '%HBTMT%' OR " +
                    "organizer LIKE '%so_yeu_ly_lich%' OR " +
                    "title LIKE '%Thu nhập bình quân%'";
            db.execSQL(deleteSql);
            healCorruptedSubjectNames(db);
        } catch (Exception ignored) {}
    }

    public void healCorruptedSubjectNames(SQLiteDatabase db) {
        if (db == null) return;
        try {
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET title = REPLACE(title, 'Thực hà h lập rì h Web', 'Thực hành lập trình Web') WHERE title LIKE '%Thực hà h lập rì h Web%'");
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET title = REPLACE(title, 'Thực hà h', 'Thực hành') WHERE title LIKE '%Thực hà h%'");
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET title = REPLACE(title, 'lập rì h', 'lập trình') WHERE title LIKE '%lập rì h%'");
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET title = REPLACE(title, 'rì h Web', 'trình Web') WHERE title LIKE '%rì h Web%'");
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET title = REPLACE(title, 'lập trì h', 'lập trình') WHERE title LIKE '%lập trì h%'");
        } catch (Exception ignored) {}
    }

    public void healCorruptedSubjectNames() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            healCorruptedSubjectNames(db);
            sanitizeEventTitles(db);
        } catch (Exception ignored) {}
    }

    public void sanitizeEventTitles(SQLiteDatabase db) {
        if (db == null) return;
        try {
            Cursor cursor = db.rawQuery("SELECT id, title FROM " + TABLE_EVENTS + " WHERE title LIKE '%(Phòng:%' OR title LIKE '%(Thời gian:%' OR title LIKE '%- Phòng%' OR title LIKE '%| Tiết%'", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    String title = cursor.getString(1);
                    String cleanTitle = EventModel.cleanDisplayTitle(title);
                    if (!cleanTitle.equals(title)) {
                        ContentValues cv = new ContentValues();
                        cv.put("title", cleanTitle);
                        db.update(TABLE_EVENTS, cv, "id = ?", new String[]{String.valueOf(id)});
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {}
    }

    public void cleanUpGarbageCvEntries() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            cleanUpGarbageCvEntries(db);
            sanitizeEventTitles(db);
            normalizeSttNumbers();
        } catch (Exception ignored) {}
    }

    private void createIndexes(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_date ON " + TABLE_EVENTS + "(event_date)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_sem_year ON " + TABLE_EVENTS + "(semester, year)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_stt ON " + TABLE_EVENTS + "(stt)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_category ON " + TABLE_EVENTS + "(category)");
        } catch (Exception ignored) {}
    }

    private EventModel cursorToEventModel(Cursor cursor) {
        int id = cursor.getInt(0);
        int stt = cursor.getInt(1);
        String title = cursor.getString(2);
        String eventDate = cursor.getString(3);
        String organizer = cursor.getString(4);
        String semester = cursor.getString(5);
        int year = cursor.getInt(6);
        int week = cursor.getInt(7);

        String category = null;
        int catIndex = cursor.getColumnIndex("category");
        if (catIndex != -1 && !cursor.isNull(catIndex)) {
            category = cursor.getString(catIndex);
        }

        EventModel event = new EventModel(id, stt, title, eventDate, organizer, semester, year, week);
        if (category != null && !category.trim().isEmpty()) {
            event.setCategory(category.trim());
        }
        return event;
    }

    // Ghi nhận lịch sử Nhập / Xuất file Excel, PDF, PPTX, Ảnh OCR
    public void addFileHistory(String fileName, String fileType, int count) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        String cleanName = fileName.trim();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("file_type", fileType);
        cv.put("record_count", count);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        cv.put("timestamp", sdf.format(new Date()));

        if (hasFileHistory(cleanName)) {
            db.update("file_history", cv, "file_name = ?", new String[]{cleanName});
        } else {
            cv.put("file_name", cleanName);
            db.insert("file_history", null, cv);
        }
    }

    // Lấy danh sách lịch sử Nhập / Xuất file
    public List<FileHistoryModel> getFileHistoryList() {
        List<FileHistoryModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM file_history ORDER BY id DESC LIMIT 250", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(new FileHistoryModel(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getInt(3),
                            cursor.getString(4)
                    ));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {}
        return list;
    }

    // Xóa một dòng lịch sử file theo id
    public void deleteFileHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("file_history", "id = ?", new String[]{String.valueOf(id)});
    }

    // Xóa nhiều dòng lịch sử file theo danh sách id
    public void deleteMultipleFileHistories(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Integer id : ids) {
                if (id != null) {
                    db.delete("file_history", "id = ?", new String[]{String.valueOf(id)});
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Xóa một dòng lịch sử file theo tên file
    public void deleteFileHistoryByName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("file_history", "file_name = ?", new String[]{fileName.trim()});
    }

    // Xóa toàn bộ lịch sử file
    public void clearAllFileHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("file_history", null, null);
    }

    public static class SourceFileInfo {
        public final String fileName;
        public final int eventCount;
        public final String fileType;

        public SourceFileInfo(String fileName, int eventCount, String fileType) {
            this.fileName = fileName;
            this.eventCount = eventCount;
            this.fileType = fileType;
        }
    }

    // Đếm số lượng sự kiện/lịch trình trong CSDL thuộc về một tệp tin cụ thể
    public int countEventsByFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String cleanName = fileName.trim();
        String baseName = cleanName;
        int dot = cleanName.lastIndexOf('.');
        if (dot > 0) {
            baseName = cleanName.substring(0, dot).trim();
        }
        int count = 0;
        try {
            if (baseName.length() >= 5 && !baseName.equals(cleanName)) {
                try (Cursor cursor = db.rawQuery(
                        "SELECT COUNT(*) FROM " + TABLE_EVENTS + " WHERE organizer LIKE ? OR title LIKE ? OR organizer LIKE ? OR title LIKE ?",
                        new String[]{"%" + cleanName + "%", "%" + cleanName + "%", "%" + baseName + "%", "%" + baseName + "%"})) {
                    if (cursor != null && cursor.moveToFirst()) {
                        count = cursor.getInt(0);
                    }
                }
            } else {
                try (Cursor cursor = db.rawQuery(
                        "SELECT COUNT(*) FROM " + TABLE_EVENTS + " WHERE organizer LIKE ? OR title LIKE ?",
                        new String[]{"%" + cleanName + "%", "%" + cleanName + "%"})) {
                    if (cursor != null && cursor.moveToFirst()) {
                        count = cursor.getInt(0);
                    }
                }
            }
        } catch (Exception ignored) {}
        return count;
    }

    // Xóa toàn bộ sự kiện đã nhập từ một tệp tin cụ thể ra khỏi ứng dụng
    public int deleteEventsByFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return 0;
        SQLiteDatabase db = this.getWritableDatabase();
        String cleanName = fileName.trim();
        String baseName = cleanName;
        int dot = cleanName.lastIndexOf('.');
        if (dot > 0) {
            baseName = cleanName.substring(0, dot).trim();
        }
        int deleted = 0;
        try {
            if (baseName.length() >= 5 && !baseName.equals(cleanName)) {
                deleted = db.delete(TABLE_EVENTS,
                        "organizer LIKE ? OR title LIKE ? OR organizer LIKE ? OR title LIKE ?",
                        new String[]{"%" + cleanName + "%", "%" + cleanName + "%", "%" + baseName + "%", "%" + baseName + "%"});
            } else {
                deleted = db.delete(TABLE_EVENTS,
                        "organizer LIKE ? OR title LIKE ?",
                        new String[]{"%" + cleanName + "%", "%" + cleanName + "%"});
            }
            String lower = cleanName.toLowerCase(Locale.getDefault());
            if (lower.contains("cv_") || lower.contains("so_yeu_ly_lich") || lower.contains("hbtmt") || lower.contains("thong tin ca nhan")) {
                cleanUpGarbageCvEntries(db);
            }
            normalizeSttNumbers();
        } catch (Exception ignored) {}
        return deleted;
    }

    // Kiểm tra xem một file đã có trong bảng file_history chưa
    public boolean hasFileHistory(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        boolean exists = false;
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM file_history WHERE file_name = ?", new String[]{fileName.trim()})) {
            if (cursor != null && cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }
        } catch (Exception ignored) {}
        return exists;
    }

    // Lấy danh sách thống kê tất cả các tệp hiện có lịch trình trong ứng dụng
    public List<SourceFileInfo> getImportedFilesSummary() {
        List<SourceFileInfo> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Integer> fileMap = new LinkedHashMap<>();

        // 1. Quét từ bảng file_history
        try {
            Cursor c = db.rawQuery("SELECT file_name FROM file_history ORDER BY id DESC", null);
            if (c.moveToFirst()) {
                do {
                    String fName = c.getString(0);
                    if (fName != null && !fName.trim().isEmpty()) {
                        fileMap.put(fName.trim(), 0);
                    }
                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception ignored) {}

        // 2. Quét từ bảng events qua cột organizer và title
        Pattern p = Pattern.compile("([a-zA-Z0-9_ -]+?(?:\\s*\\([0-9]+\\))?\\.(?:pdf|xlsx|xls|docx|pptx|jpg|jpeg|png|csv|txt))", Pattern.CASE_INSENSITIVE);
        try {
            Cursor c = db.rawQuery("SELECT organizer, title FROM " + TABLE_EVENTS, null);
            if (c.moveToFirst()) {
                do {
                    String org = c.getString(0);
                    String tit = c.getString(1);
                    String combined = (org != null ? org : "") + " " + (tit != null ? tit : "");
                    Matcher m = p.matcher(combined);
                    Set<String> matchedInRow = new HashSet<>();
                    while (m.find()) {
                        String matchedFile = m.group(1).trim();
                        if (matchedFile.startsWith(".") || matchedFile.length() < 5) continue;
                        matchedInRow.add(matchedFile);
                    }
                    for (String mf : matchedInRow) {
                        int cur = fileMap.containsKey(mf) ? fileMap.get(mf) : 0;
                        fileMap.put(mf, cur + 1);
                    }
                } while (c.moveToNext());
            }
            c.close();
        } catch (Exception ignored) {}

        for (String fName : fileMap.keySet()) {
            int actualCount = countEventsByFileName(fName);
            String ext = "";
            int dotIdx = fName.lastIndexOf('.');
            if (dotIdx >= 0) ext = fName.substring(dotIdx).toUpperCase(Locale.getDefault());
            result.add(new SourceFileInfo(fName, actualCount, ext));
        }
        return result;
    }

    // Đồng bộ tất cả các tệp vật lý lưu trong thư mục HistoryFiles vào danh sách lịch sử file
    public void syncPhysicalFilesIntoHistory(File historyDir) {
        if (historyDir == null || !historyDir.exists() || !historyDir.isDirectory()) return;
        File[] files = historyDir.listFiles();
        if (files == null || files.length == 0) return;

        // Sắp xếp file theo thời gian chỉnh sửa giảm dần (mới nhất lên đầu)
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        SQLiteDatabase db = this.getWritableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (File f : files) {
            if (f == null || !f.isFile()) continue;
            String name = f.getName().trim();
            // Bỏ qua các file tạm hoặc file ẩn
            if (name.startsWith(".") || name.startsWith("view_") || name.startsWith("temp_")
                    || name.endsWith(".tmp") || name.equalsIgnoreCase(".nomedia")) {
                continue;
            }

            int count = countEventsByFileName(name);
            String lower = name.toLowerCase(Locale.getDefault());

            // Nếu file đã có trong bảng file_history, cập nhật lại số lượng sự kiện chính xác
            if (hasFileHistory(name)) {
                try {
                    ContentValues cvUpdate = new ContentValues();
                    cvUpdate.put("record_count", count);
                    db.update("file_history", cvUpdate, "file_name = ?", new String[]{name});
                } catch (Exception ignored) {}
                continue;
            }

            // Xác định badge phù hợp
            String badge;
            if (name.startsWith("BaoCao_") || name.startsWith("BC_") || lower.contains("baocao")) {
                if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                    badge = "XUẤT BÁO CÁO (EXCEL)";
                } else if (lower.endsWith(".pdf")) {
                    badge = "XUẤT BÁO CÁO (PDF)";
                } else {
                    badge = "XUẤT BÁO CÁO";
                }
            } else if (name.toUpperCase(Locale.getDefault()).contains("HUTECH") || name.toUpperCase(Locale.getDefault()).contains("VLQ")) {
                badge = "THƯ MỜI GIẢNG (HUTECH)";
            } else if (lower.endsWith(".pdf")) {
                badge = "TÀI LIỆU PDF (.pdf)";
            } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                badge = "FILE EXCEL (.xlsx)";
            } else if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) {
                badge = "BÀI GIẢNG PPT (.pptx)";
            } else if (lower.endsWith(".docx") || lower.endsWith(".doc")) {
                badge = "VĂN BẢN WORD (.docx)";
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
                badge = "ẢNH OCR";
            } else {
                badge = "TỆP ĐÃ NHẬP";
            }

            String formattedTime = sdf.format(new Date(f.lastModified()));
            try {
                ContentValues cv = new ContentValues();
                cv.put("file_name", name);
                cv.put("file_type", badge);
                cv.put("record_count", count);
                cv.put("timestamp", formattedTime);
                db.insert("file_history", null, cv);
            } catch (Exception ignored) {}
        }
    }

    // Tự động đồng bộ các file có dữ liệu lịch trình nhưng chưa có trong lịch sử tệp
    public void syncOrphanFilesIntoHistory() {
        List<SourceFileInfo> list = getImportedFilesSummary();
        for (SourceFileInfo s : list) {
            if (s.eventCount > 0 && !hasFileHistory(s.fileName)) {
                String badge = "NHẬP TỪ FILE (" + s.fileType + ")";
                addFileHistory(s.fileName, badge, s.eventCount);
            }
        }
    }

    // Xóa triệt để các lịch mẫu mặc định của ứng dụng
    public void deleteSampleEvents() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_EVENTS,
                    "title = ? OR title = ? OR title = ?",
                    new String[]{
                        "Họp Hội đồng Khoa CNTT",
                        "Tập huấn ngắn hạn AI trong Giảng dạy",
                        "Chấm đồ án Chuyên ngành KTPM"
                    });
            normalizeSttNumbers();
        } catch (Exception ignored) {}
    }

    // Xóa toàn bộ sự kiện đã nhập từ TẤT CẢ các tệp tin trong ứng dụng
    public int deleteAllEventsFromAllImportedFiles() {
        List<SourceFileInfo> files = getImportedFilesSummary();
        int totalDeleted = 0;
        for (SourceFileInfo fi : files) {
            if (fi.eventCount > 0) {
                totalDeleted += deleteEventsByFileName(fi.fileName);
            }
        }
        // Xóa triệt để các sự kiện có nguồn gốc từ file trong bảng events
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int fileEvents = db.delete(TABLE_EVENTS,
                    "organizer LIKE '%.pdf%' OR organizer LIKE '%.xlsx%' OR organizer LIKE '%.xls%' OR " +
                    "organizer LIKE '%.pptx%' OR organizer LIKE '%.docx%' OR organizer LIKE '%.jpg%' OR " +
                    "organizer LIKE '%.png%' OR organizer LIKE '%Ghi tay%' OR organizer LIKE '%Ảnh chụp%' OR " +
                    "organizer LIKE '%Tài liệu PDF%' OR organizer LIKE '%Bảng tính Excel%'", null);
            totalDeleted += fileEvents;
            cleanUpGarbageCvEntries(db);
            normalizeSttNumbers();
        } catch (Exception ignored) {}
        clearAllFileHistory();
        return totalDeleted;
    }

    // Chuyển đổi ngày linh hoạt từ dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy hoặc yyyy-MM-dd thành yyyy-MM-dd (CSDL lưu)
    public String parseToIsoDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        String trimmed = dateStr.trim().replace('-', '/').replace('.', '/');

        // 1. Thử tách trực tiếp dd/MM/yyyy hoặc yyyy/MM/dd
        try {
            String[] parts = trimmed.split("/");
            if (parts.length == 3) {
                if (parts[0].length() == 4) {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int d = Integer.parseInt(parts[2]);
                    if (y >= 1900 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= 31) {
                        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                    }
                } else if (parts[2].length() == 4) {
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    if (y >= 1900 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= 31) {
                        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                    }
                }
            } else if (parts.length == 2) {
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int y = Calendar.getInstance().get(Calendar.YEAR);
                if (m >= 1 && m <= 12 && d >= 1 && d <= 31) {
                    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                }
            }
        } catch (Exception ignored) {}

        // 2. Fallback chuẩn hóa dd/MM/yyyy
        try {
            SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdfVn.setLenient(false);
            Date d = sdfVn.parse(trimmed);
            if (d != null) {
                SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                return sdfIso.format(d);
            }
        } catch (Exception ignored) {}

        // 3. Thử chuẩn hóa yyyy-MM-dd
        try {
            SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdfIso.setLenient(false);
            Date d = sdfIso.parse(dateStr.trim());
            if (d != null) {
                return sdfIso.format(d);
            }
        } catch (Exception ignored) {}

        return dateStr.trim();
    }

    // Tự động dịch chuyển và chuẩn hóa STT linh hoạt (Tối ưu tốc độ cao, không gây lag UI)
    public void insertEvent(int stt, String title, String dateStr, String organizer, String category) {
        if (title == null || title.trim().isEmpty() || dateStr == null || dateStr.trim().isEmpty()) {
            return;
        }

        String isoDateStr = parseToIsoDate(dateStr);
        SQLiteDatabase db = this.getWritableDatabase();
        boolean inTx = db.inTransaction();

        // Kiểm tra xem lịch này đã tồn tại chưa (tránh bị lặp lại)
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_EVENTS + " WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) AND event_date = ? LIMIT 1",
                new String[]{title.trim(), isoDateStr});
        if (cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            updateEventInternal(db, existingId, stt, title, isoDateStr, organizer, category, !inTx);
            return;
        }
        cursor.close();

        if (!inTx && stt > 0) {
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET stt = stt + 1 WHERE stt >= ?", new Object[]{stt});
        }

        ContentValues values = buildEventContentValues(stt, title, isoDateStr, organizer, category);
        db.insert(TABLE_EVENTS, null, values);

        // Chỉ đánh số lại STT khi không nằm trong vòng lặp import hàng loạt
        if (!inTx) {
            normalizeSttNumbers();
        }
    }

    public void insertEvent(int stt, String title, String dateStr, String organizer) {
        insertEvent(stt, title, dateStr, organizer, null);
    }

    private ContentValues buildEventContentValues(int stt, String title, String isoDateStr, String organizer, String category) {
        ContentValues values = new ContentValues();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int week = 1;
        String semester = "HK1";

        try {
            if (isoDateStr.length() == 10 && isoDateStr.charAt(4) == '-' && isoDateStr.charAt(7) == '-') {
                year = Integer.parseInt(isoDateStr.substring(0, 4));
                int month = Integer.parseInt(isoDateStr.substring(5, 7));
                int day = Integer.parseInt(isoDateStr.substring(8, 10));
                Calendar cal = Calendar.getInstance();
                cal.set(year, month - 1, day);
                week = cal.get(Calendar.WEEK_OF_YEAR);
                if (month >= 9 || month == 1) {
                    semester = "HK1";
                } else if (month >= 2 && month <= 6) {
                    semester = "HK2";
                } else {
                    semester = "HK3";
                }
            }
        } catch (Exception ignored) {}

        values.put("stt", stt > 0 ? stt : 999);
        values.put("title", ThumoiGiangParser.healVietnameseText(title.trim()));
        values.put("event_date", isoDateStr);
        values.put("organizer", organizer);
        values.put("semester", semester);
        values.put("year", year);
        values.put("week", week);
        if (category != null && !category.trim().isEmpty()) {
            values.put("category", category.trim());
        }
        return values;
    }

    private ContentValues buildEventContentValues(int stt, String title, String isoDateStr, String organizer) {
        return buildEventContentValues(stt, title, isoDateStr, organizer, null);
    }

    public void updateEvent(int id, int stt, String title, String dateStr, String organizer, String category) {
        if (title == null || title.trim().isEmpty() || dateStr == null || dateStr.trim().isEmpty()) {
            return;
        }
        String isoDateStr = parseToIsoDate(dateStr);
        SQLiteDatabase db = this.getWritableDatabase();
        updateEventInternal(db, id, stt, title, isoDateStr, organizer, category, !db.inTransaction());
    }

    public void updateEvent(int id, int stt, String title, String dateStr, String organizer) {
        updateEvent(id, stt, title, dateStr, organizer, null);
    }

    private void updateEventInternal(SQLiteDatabase db, int id, int stt, String title, String isoDateStr, String organizer, String category, boolean shouldNormalize) {
        if (shouldNormalize && stt > 0) {
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET stt = stt + 1 WHERE stt >= ? AND id != ?", new Object[]{stt, id});
        }
        ContentValues values = buildEventContentValues(stt, title, isoDateStr, organizer, category);
        db.update(TABLE_EVENTS, values, "id = ?", new String[]{String.valueOf(id)});
        if (shouldNormalize) {
            normalizeSttNumbers();
        }
    }

    private void updateEventInternal(SQLiteDatabase db, int id, int stt, String title, String isoDateStr, String organizer, boolean shouldNormalize) {
        updateEventInternal(db, id, stt, title, isoDateStr, organizer, null, shouldNormalize);
    }

    // Chuẩn hóa lại thứ tự STT liên tục 1, 2, 3, 4, 5... theo đúng thứ tự ngày & giờ diễn ra (thời gian trước thì lên đầu)
    public void normalizeSttNumbers() {
        SQLiteDatabase db = this.getWritableDatabase();
        List<EventModel> allEvents = searchEvents("", "Tất cả HK", "Tất cả Năm");
        if (allEvents.isEmpty()) return;
        EventModel.sortChronologically(allEvents);

        boolean startedTx = false;
        if (!db.inTransaction()) {
            db.beginTransaction();
            startedTx = true;
        }
        try (SQLiteStatement stmt = db.compileStatement("UPDATE " + TABLE_EVENTS + " SET stt = ? WHERE id = ?")) {
            for (int i = 0; i < allEvents.size(); i++) {
                stmt.bindLong(1, i + 1);
                stmt.bindLong(2, allEvents.get(i).getId());
                stmt.executeUpdateDelete();
            }
            if (startedTx) {
                db.setTransactionSuccessful();
            }
        } catch (Exception ignored) {
        } finally {
            if (startedTx) {
                db.endTransaction();
            }
        }
    }

    // Tự động lọc bỏ tất cả lịch trùng lặp hoặc dữ liệu rác cũ (event_date không phải yyyy-MM-dd) trong CSDL
    public void removeDuplicateEvents() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_EVENTS + " WHERE event_date IS NULL OR LENGTH(TRIM(event_date)) != 10 OR event_date NOT GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'");
            String sql = "DELETE FROM " + TABLE_EVENTS + " WHERE id NOT IN (" +
                    "SELECT MIN(id) FROM " + TABLE_EVENTS + " GROUP BY LOWER(TRIM(title)), event_date)";
            db.execSQL(sql);
            normalizeSttNumbers();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Tự động xóa dữ liệu cũ (sự kiện + lịch sử tệp tin) đã tạo/xảy ra quá thời hạn quy định (tháng)
    public int autoDeleteOldData(int retentionMonths, File historyStorageDir) {
        if (retentionMonths <= 0) return 0;

        removeDuplicateEvents();
        SQLiteDatabase db = this.getWritableDatabase();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -retentionMonths); // Lùi lại số tháng lưu trữ tính từ hôm nay

        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String cutoffIsoDate = sdfIso.format(cal.getTime());

        // 1. Xóa các sự kiện công việc cũ hơn thời hạn
        int deletedEventsCount = db.delete(TABLE_EVENTS, "event_date < ?", new String[]{cutoffIsoDate});
        if (deletedEventsCount > 0) {
            normalizeSttNumbers();
        }

        // 2. Xóa các tệp lịch sử file_history cũ hơn thời hạn (tính từ thời điểm nhập/tạo vào app)
        int deletedFilesCount = 0;
        try {
            SimpleDateFormat sdfHistory = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Cursor cursor = db.rawQuery("SELECT id, file_name, timestamp FROM file_history", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String fileName = cursor.getString(cursor.getColumnIndexOrThrow("file_name"));
                    String timeStr = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));

                    if (timeStr != null && timeStr.length() >= 10) {
                        try {
                            String datePart = timeStr.substring(0, 10);
                            Date fileDate = sdfHistory.parse(datePart);
                            if (fileDate != null && fileDate.before(cal.getTime())) {
                                db.delete("file_history", "id = ?", new String[]{String.valueOf(id)});
                                deletedFilesCount++;

                                // Xóa file vật lý lưu trong bộ nhớ máy nếu có
                                if (historyStorageDir != null && historyStorageDir.exists()) {
                                    File targetFile = new File(historyStorageDir, fileName);
                                    if (targetFile.exists()) {
                                        targetFile.delete();
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception ignored) {}

        return deletedEventsCount + deletedFilesCount;
    }

    public int autoDeleteOldData() {
        return autoDeleteOldData(12, null);
    }

    // Đếm số lượng lịch công việc cũ hơn thời hạn
    public int getOldEventsCount(int retentionMonths) {
        if (retentionMonths <= 0) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -retentionMonths);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String cutoffIsoDate = sdf.format(cal.getTime());

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EVENTS + " WHERE event_date < ?", new String[]{cutoffIsoDate});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // Đếm số lượng tệp lịch sử cũ hơn thời hạn
    public int getOldFilesCount(int retentionMonths) {
        if (retentionMonths <= 0) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -retentionMonths);
        SimpleDateFormat sdfHistory = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        int count = 0;
        try {
            Cursor cursor = db.rawQuery("SELECT timestamp FROM file_history", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String timeStr = cursor.getString(0);
                        if (timeStr != null && timeStr.length() >= 10) {
                            try {
                                Date d = sdfHistory.parse(timeStr.substring(0, 10));
                                if (d != null && d.before(cal.getTime())) {
                                    count++;
                                }
                            } catch (Exception ignored) {}
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Exception ignored) {}
        return count;
    }

    // Tra cứu và lọc siêu tốc (Chỉ đọc SELECT trên Index, không ghi đè CSDL gây lag UI)
    public List<EventModel> searchEvents(String keyword, String semesterFilter, String yearFilter) {
        List<EventModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_EVENTS + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // Nếu keyword đúng chuẩn ngày yyyy-MM-dd thì lọc trực tiếp bằng dấu '=' trên Index để đạt tốc độ < 1ms
            if (kw.length() == 10 && kw.charAt(4) == '-' && kw.charAt(7) == '-') {
                query.append(" AND event_date = ?");
                args.add(kw);
            } else {
                String isoKw = parseToIsoDate(kw);
                query.append(" AND (title LIKE ? OR organizer LIKE ? OR event_date LIKE ? OR event_date LIKE ?)");
                args.add("%" + kw + "%");
                args.add("%" + kw + "%");
                args.add("%" + kw + "%");
                args.add("%" + isoKw + "%");
            }
        }
        if (semesterFilter != null && !semesterFilter.equals("Tất cả HK")) {
            query.append(" AND semester = ?");
            args.add(semesterFilter);
        }
        if (yearFilter != null && !yearFilter.equals("Tất cả Năm")) {
            query.append(" AND year = ?");
            args.add(yearFilter);
        }

        query.append(" ORDER BY stt ASC, event_date ASC");

        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToEventModel(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        EventModel.sortChronologically(list);
        return list;
    }

    // Thống kê số lượng công việc theo Học kỳ để vẽ biểu đồ (Chỉ đọc SELECT)
    public Map<String, Integer> getStatsBySemester() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("HK1", 0);
        map.put("HK2", 0);
        map.put("HK3", 0);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT semester, COUNT(*) FROM " + TABLE_EVENTS + " GROUP BY semester", null);
        if (cursor.moveToFirst()) {
            do {
                map.put(cursor.getString(0), cursor.getInt(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return map;
    }

    // Thống kê số lượng công việc theo Tuần để vẽ biểu đồ (Chỉ đọc SELECT)
    public Map<String, Integer> getStatsByWeek() {
        Map<String, Integer> map = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT week, COUNT(*) FROM " + TABLE_EVENTS + " GROUP BY week ORDER BY week ASC LIMIT 10", null);
        if (cursor.moveToFirst()) {
            do {
                map.put("Tuần " + cursor.getInt(0), cursor.getInt(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return map;
    }

    // Lấy các sự kiện diễn ra trong Hôm nay và Ngày mai (Báo trước 24h lúc 6h00 sáng)
    public List<EventModel> getUpcomingEventsForAlarm() {
        List<EventModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = sdf.format(cal.getTime());

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE event_date = ? OR event_date = ?", new String[]{today, tomorrow});
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToEventModel(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        EventModel.sortChronologically(list);
        return list;
    }

    // Lấy toàn bộ sự kiện của một ngày cụ thể (định dạng yyyy-MM-dd)
    public List<EventModel> getEventsForDate(String isoDate) {
        List<EventModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE event_date = ? ORDER BY stt ASC", new String[]{isoDate});
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToEventModel(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        EventModel.sortChronologically(list);
        return list;
    }

    // Lấy toàn bộ sự kiện trong khoảng ngày (định dạng yyyy-MM-dd)
    public List<EventModel> getEventsBetweenDates(String startDateIso, String endDateIso) {
        List<EventModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE event_date >= ? AND event_date <= ? ORDER BY event_date ASC, stt ASC",
                new String[]{startDateIso, endDateIso});
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToEventModel(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        EventModel.sortChronologically(list);
        return list;
    }

    public void deleteEvent(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, "id = ?", new String[]{String.valueOf(id)});
        normalizeSttNumbers();
    }

    // Xóa hàng loạt danh sách các ID sự kiện được chọn
    public void deleteMultipleEvents(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int id : ids) {
                db.delete(TABLE_EVENTS, "id = ?", new String[]{String.valueOf(id)});
            }
            normalizeSttNumbers();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public EventModel getEventById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE id = ?", new String[]{String.valueOf(id)});
        EventModel event = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                event = cursorToEventModel(cursor);
            }
            cursor.close();
        }
        return event;
    }

    // Chỉnh sửa hàng loạt các lịch được chọn
    public void batchUpdateEvents(List<Integer> ids, String newTitle, String newPeriod, String newTime, String newRoom, String newOrganizer) {
        if (ids == null || ids.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int id : ids) {
                EventModel event = getEventById(id);
                if (event == null) continue;

                EventModel.EventFieldBreakdown bd = EventModel.parseEventDetails(event);
                String title = (newTitle != null && !newTitle.trim().isEmpty()) ? newTitle.trim() : bd.title;
                String period = (newPeriod != null && !newPeriod.trim().isEmpty()) ? newPeriod.trim() : bd.period;
                String time = (newTime != null && !newTime.trim().isEmpty()) ? newTime.trim() : bd.time;
                String room = (newRoom != null && !newRoom.trim().isEmpty()) ? newRoom.trim() : bd.room;
                String org = (newOrganizer != null && !newOrganizer.trim().isEmpty()) ? newOrganizer.trim() : event.getOrganizer();

                StringBuilder sb = new StringBuilder();
                sb.append(title);
                if (bd.group != null && !bd.group.isEmpty() && !title.contains(bd.group)) {
                    if (title.endsWith(")")) {
                        int lastParen = title.lastIndexOf(')');
                        sb.setLength(0);
                        sb.append(title.substring(0, lastParen)).append(" • Nhóm ").append(bd.group).append(")");
                    } else {
                        sb.append(" (• Nhóm ").append(bd.group).append(")");
                    }
                }
                if (room != null && !room.isEmpty()) {
                    sb.append(" - ").append(room);
                }
                if (period != null && !period.isEmpty()) {
                    sb.append(" | Tiết ").append(period);
                }
                if (time != null && !time.isEmpty()) {
                    sb.append(" [").append(time).append("]");
                }

                ContentValues cv = new ContentValues();
                cv.put("title", sb.toString());
                cv.put("organizer", org);
                db.update(TABLE_EVENTS, cv, "id = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        normalizeSttNumbers();
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ TRUNG TÂM THÔNG BÁO (NOTIFICATION CENTER)
    // ==========================================

    public void insertNotification(String title, String message, String type) {
        if (title == null || title.trim().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("message", message != null ? message.trim() : "");
        values.put("timestamp", new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        values.put("is_read", 0);
        values.put("type", type != null ? type : "SYSTEM");
        db.insert(TABLE_NOTIFICATIONS, null, values);
    }

    public List<NotificationModel> getAllNotifications() {
        List<NotificationModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTIFICATIONS + " ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1;
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                list.add(new NotificationModel(id, title, message, timestamp, isRead, type));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (list.isEmpty()) {
            insertNotification("🎉 Chào mừng đến với LectuPlan v1.1.2", "Hệ thống trợ lý lịch biểu đã được cập nhật phiên bản v1.1.2 hỗ trợ báo SV và tùy chỉnh thiệp linh hoạt!", "SYSTEM");
            insertNotification("🤖 Trợ lý Tự động DND & GPS", "Tính năng tự động tắt chuông và check-in GPS khi đứng lớp đã sẵn sàng.", "DND");
            return getAllNotifications();
        }
        return list;
    }

    public int getUnreadNotificationCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS + " WHERE is_read = 0", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void markNotificationAsRead(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update(TABLE_NOTIFICATIONS, cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public void markAllNotificationsAsRead() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update(TABLE_NOTIFICATIONS, cv, null, null);
    }

    public void deleteNotification(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTIFICATIONS, "id = ?", new String[]{String.valueOf(id)});
    }

    public void deleteMultipleNotifications(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (int id : ids) {
                db.delete(TABLE_NOTIFICATIONS, "id = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearAllNotifications() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTIFICATIONS, null, null);
    }
}
