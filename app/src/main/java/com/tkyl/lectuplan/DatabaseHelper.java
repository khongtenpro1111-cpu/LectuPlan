package com.tkyl.lectuplan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "LectuPlan.db";
    private static final int DB_VERSION = 3;
    public static final String TABLE_EVENTS = "events";

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
                "week INTEGER)";
        db.execSQL(createTable);

        String createHistoryTable = "CREATE TABLE IF NOT EXISTS file_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "file_name TEXT, " +
                "file_type TEXT, " +
                "record_count INTEGER, " +
                "timestamp TEXT)";
        db.execSQL(createHistoryTable);

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
        createIndexes(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            createIndexes(db);
        }
    }

    private void createIndexes(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_date ON " + TABLE_EVENTS + "(event_date)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_sem_year ON " + TABLE_EVENTS + "(semester, year)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_stt ON " + TABLE_EVENTS + "(stt)");
        } catch (Exception ignored) {}
    }

    // Ghi nhận lịch sử Nhập / Xuất file Excel, PDF, PPTX, Ảnh OCR
    public void addFileHistory(String fileName, String fileType, int count) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("file_name", fileName);
        cv.put("file_type", fileType);
        cv.put("record_count", count);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        cv.put("timestamp", sdf.format(new Date()));

        db.insert("file_history", null, cv);
    }

    // Lấy danh sách lịch sử Nhập / Xuất file
    public List<FileHistoryModel> getFileHistoryList() {
        List<FileHistoryModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM file_history ORDER BY id DESC LIMIT 30", null);
            if (cursor.moveToFirst()) {
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
            cursor.close();
        } catch (Exception ignored) {}
        return list;
    }

    // Xóa một dòng lịch sử file
    public void deleteFileHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("file_history", "id = ?", new String[]{String.valueOf(id)});
    }

    // Xóa toàn bộ lịch sử file
    public void clearAllFileHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("file_history", null, null);
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
    public void insertEvent(int stt, String title, String dateStr, String organizer) {
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
            updateEventInternal(db, existingId, stt, title, isoDateStr, organizer, !inTx);
            return;
        }
        cursor.close();

        if (!inTx && stt > 0) {
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET stt = stt + 1 WHERE stt >= ?", new Object[]{stt});
        }

        ContentValues values = buildEventContentValues(stt, title, isoDateStr, organizer);
        db.insert(TABLE_EVENTS, null, values);

        // Chỉ đánh số lại STT khi không nằm trong vòng lặp import hàng loạt
        if (!inTx) {
            normalizeSttNumbers();
        }
    }

    private ContentValues buildEventContentValues(int stt, String title, String isoDateStr, String organizer) {
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
        values.put("title", title.trim());
        values.put("event_date", isoDateStr);
        values.put("organizer", organizer);
        values.put("semester", semester);
        values.put("year", year);
        values.put("week", week);
        return values;
    }

    public void updateEvent(int id, int stt, String title, String dateStr, String organizer) {
        if (title == null || title.trim().isEmpty() || dateStr == null || dateStr.trim().isEmpty()) {
            return;
        }
        String isoDateStr = parseToIsoDate(dateStr);
        SQLiteDatabase db = this.getWritableDatabase();
        updateEventInternal(db, id, stt, title, isoDateStr, organizer, !db.inTransaction());
    }

    private void updateEventInternal(SQLiteDatabase db, int id, int stt, String title, String isoDateStr, String organizer, boolean shouldNormalize) {
        if (shouldNormalize && stt > 0) {
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET stt = stt + 1 WHERE stt >= ? AND id != ?", new Object[]{stt, id});
        }
        ContentValues values = buildEventContentValues(stt, title, isoDateStr, organizer);
        db.update(TABLE_EVENTS, values, "id = ?", new String[]{String.valueOf(id)});
        if (shouldNormalize) {
            normalizeSttNumbers();
        }
    }

    // Chuẩn hóa lại thứ tự STT liên tục 1, 2, 3, 4, 5... bằng SQLiteStatement tốc độ cao
    public void normalizeSttNumbers() {
        SQLiteDatabase db = this.getWritableDatabase();
        List<Integer> ids = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_EVENTS + " ORDER BY stt ASC, event_date ASC, id ASC", null);
        if (cursor.moveToFirst()) {
            do {
                ids.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (ids.isEmpty()) return;

        boolean startedTx = false;
        if (!db.inTransaction()) {
            db.beginTransaction();
            startedTx = true;
        }
        try (SQLiteStatement stmt = db.compileStatement("UPDATE " + TABLE_EVENTS + " SET stt = ? WHERE id = ?")) {
            for (int i = 0; i < ids.size(); i++) {
                stmt.bindLong(1, i + 1);
                stmt.bindLong(2, ids.get(i));
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

    // YÊU CẦU CỦA THẦY: Cứ 1 năm data sẽ tự delete auto không cần cài đặt
    public int autoDeleteOldData() {
        removeDuplicateEvents();
        SQLiteDatabase db = this.getWritableDatabase();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1); // Lùi lại đúng 1 năm tính từ hôm nay
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String oneYearAgoDate = sdf.format(cal.getTime());

        int deletedCount = db.delete(TABLE_EVENTS, "event_date < ?", new String[]{oneYearAgoDate});
        if (deletedCount > 0) {
            normalizeSttNumbers();
        }
        return deletedCount;
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
                list.add(new EventModel(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6),
                        cursor.getInt(7)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
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
                list.add(new EventModel(
                        cursor.getInt(0), cursor.getInt(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getInt(6), cursor.getInt(7)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
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
}
