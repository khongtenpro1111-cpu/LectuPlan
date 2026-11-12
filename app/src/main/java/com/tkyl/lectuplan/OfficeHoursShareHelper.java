package com.tkyl.lectuplan;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class OfficeHoursShareHelper {

    private static final int MAX_CARD_NOTE_LENGTH = 220;

    /**
     * Mở Dialog cho phép người dùng chỉnh sửa thời gian, tiêu đề, lời nhắn trước khi tạo & chia sẻ
     */
    public static void shareSlotAsImage(Context context, SmartSlotAdapter.FoundSlot slot) {
        showShareEditDialog(context, slot);
    }

    public static void showShareEditDialog(Context context, SmartSlotAdapter.FoundSlot slot) {
        if (context == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_office_hours_share, null);

        EditText edtCardTitle = view.findViewById(R.id.edtCardTitle);
        EditText edtShareDate = view.findViewById(R.id.edtShareDate);
        EditText edtShareStartTime = view.findViewById(R.id.edtShareStartTime);
        EditText edtShareEndTime = view.findViewById(R.id.edtShareEndTime);
        EditText edtCardNote = view.findViewById(R.id.edtCardNote);
        TextView tvCardNoteCharCount = view.findViewById(R.id.tvCardNoteCharCount);
        EditText edtShareTextMessage = view.findViewById(R.id.edtShareTextMessage);
        Button btnCancelShare = view.findViewById(R.id.btnCancelShare);
        Button btnDoShare = view.findViewById(R.id.btnDoShare);

        // Giá trị mặc định
        String defaultTitle = "LỊCH TIẾP SINH VIÊN (OFFICE HOURS)";
        String defaultDayDisplay = (slot != null && slot.dayDisplay != null) ? slot.dayDisplay : "Thứ Bảy, 14/11/2026";

        String defaultStart = "06:45";
        String defaultEnd = "21:45";
        if (slot != null) {
            if (slot.startMin > 0) defaultStart = TimeNormalizer.minuteToTimeString(slot.startMin);
            if (slot.endMin > 0) defaultEnd = TimeNormalizer.minuteToTimeString(slot.endMin);
            if ((slot.startMin <= 0 || slot.endMin <= 0) && slot.timeRangeStr != null && slot.timeRangeStr.contains("-")) {
                String[] parts = slot.timeRangeStr.split("-");
                if (parts.length == 2) {
                    defaultStart = parts[0].trim();
                    defaultEnd = parts[1].trim();
                }
            }
        }

        String defaultNote = "Giảng viên có mặt tại trường và sẵn sàng hỗ trợ, giải đáp thắc mắc, duyệt đồ án cho sinh viên trong khoảng thời gian này.";

        edtCardTitle.setText(defaultTitle);
        edtShareDate.setText(defaultDayDisplay);
        edtShareStartTime.setText(defaultStart);
        edtShareEndTime.setText(defaultEnd);
        edtCardNote.setText(defaultNote);

        if (tvCardNoteCharCount != null) {
            tvCardNoteCharCount.setText(edtCardNote.getText().length() + "/" + MAX_CARD_NOTE_LENGTH);
        }

        // Đếm số ký tự lời nhắn trực tiếp để cảnh báo người dùng không nhập quá dài
        edtCardNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvCardNoteCharCount != null) {
                    tvCardNoteCharCount.setText(s.length() + "/" + MAX_CARD_NOTE_LENGTH);
                    if (s.length() >= MAX_CARD_NOTE_LENGTH) {
                        tvCardNoteCharCount.setTextColor(Color.parseColor("#DC2626")); // Đỏ cảnh báo
                    } else {
                        tvCardNoteCharCount.setTextColor(Color.parseColor("#64748B"));
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Đánh dấu người dùng có tự sửa tin nhắn hay không
        final boolean[] userManuallyEditedText = new boolean[]{false};

        Runnable updateTextMessageRunnable = () -> {
            if (userManuallyEditedText[0]) return;
            String day = edtShareDate.getText().toString().trim();
            String st = edtShareStartTime.getText().toString().trim();
            String et = edtShareEndTime.getText().toString().trim();
            String range = st + " - " + et;
            String msg = "Chào các bạn, tuần này chúng ta sẽ học bù/gặp mặt vào " + day + ", khung giờ từ " + range + ".\nCác bạn sắp xếp thời gian tham gia nhé!";
            edtShareTextMessage.setText(msg);
        };

        // Điền tin nhắn mặc định
        updateTextMessageRunnable.run();

        // Theo dõi xem người dùng có nhập tay vào ô tin nhắn không
        edtShareTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                userManuallyEditedText[0] = true;
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Bấm chọn Ngày tổ chức
        edtShareDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(context, (picker, y, m, d) -> {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(y, m, d);
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
                String formatted = sdf.format(selectedCal.getTime());
                if (!formatted.isEmpty()) {
                    formatted = Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
                }
                edtShareDate.setText(formatted);
                updateTextMessageRunnable.run();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Bấm chọn Giờ bắt đầu
        edtShareStartTime.setOnClickListener(v -> {
            int currentH = 6, currentM = 45;
            String stStr = edtShareStartTime.getText().toString().trim();
            int parsedMin = TimeNormalizer.timeStringToMinute(stStr);
            if (parsedMin >= 0) {
                currentH = parsedMin / 60;
                currentM = parsedMin % 60;
            }
            new TimePickerDialog(context, (view1, hourOfDay, minute) -> {
                edtShareStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                updateTextMessageRunnable.run();
            }, currentH, currentM, true).show();
        });

        // Bấm chọn Giờ kết thúc
        edtShareEndTime.setOnClickListener(v -> {
            int currentH = 21, currentM = 45;
            String etStr = edtShareEndTime.getText().toString().trim();
            int parsedMin = TimeNormalizer.timeStringToMinute(etStr);
            if (parsedMin >= 0) {
                currentH = parsedMin / 60;
                currentM = parsedMin % 60;
            }
            new TimePickerDialog(context, (view12, hourOfDay, minute) -> {
                edtShareEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                updateTextMessageRunnable.run();
            }, currentH, currentM, true).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.94);
            int maxAllowedWidth = (int) (600 * metrics.density);
            if (width > maxAllowedWidth) width = maxAllowedWidth;
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        btnCancelShare.setOnClickListener(v -> dialog.dismiss());

        btnDoShare.setOnClickListener(v -> {
            String title = edtCardTitle.getText().toString().trim();
            if (title.isEmpty()) title = "LỊCH TIẾP SINH VIÊN (OFFICE HOURS)";

            String day = edtShareDate.getText().toString().trim();
            if (day.isEmpty()) day = "Thứ Bảy, 14/11/2026";

            String st = edtShareStartTime.getText().toString().trim();
            String et = edtShareEndTime.getText().toString().trim();
            if (st.isEmpty()) st = "06:45";
            if (et.isEmpty()) et = "21:45";
            String timeRange = st + " - " + et;

            String note = edtCardNote.getText().toString().trim();
            if (note.isEmpty()) {
                note = "Giảng viên có mặt tại trường và sẵn sàng hỗ trợ, giải đáp thắc mắc, duyệt đồ án cho sinh viên trong khoảng thời gian này.";
            }

            String textMsg = edtShareTextMessage.getText().toString().trim();
            if (textMsg.isEmpty()) {
                textMsg = "Chào các bạn, tuần này chúng ta sẽ học bù/gặp mặt vào " + day + ", khung giờ từ " + timeRange + ".\nCác bạn sắp xếp thời gian tham gia nhé!";
            }

            dialog.dismiss();
            generateAndShareBitmapImage(context, title, day, timeRange, note, textMsg);
        });

        dialog.show();
    }

    /**
     * Tạo Bitmap thiệp chia sẻ chuẩn 1080x1080 và gọi Intent chia sẻ ra bên ngoài ứng dụng
     */
    public static void generateAndShareBitmapImage(Context context, String headerTitle, String dayDisplay, String timeRangeStr, String cardNoteInput, String shareText) {
        String cardNote = cardNoteInput != null ? cardNoteInput : "";
        // 1. Giới hạn tối đa độ dài lời nhắn để đảm bảo khung ảnh thiệp không bao giờ bị nổ/tràn chữ
        if (cardNote.length() > MAX_CARD_NOTE_LENGTH) {
            cardNote = cardNote.substring(0, MAX_CARD_NOTE_LENGTH - 3) + "...";
        }

        // 2. Tạo Bitmap tỉ lệ vuông (1080x1080) chuẩn thiệp chia sẻ Zalo/Facebook
        int width = 1080;
        int height = 1080;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 3. Vẽ Nền (Background)
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#F1F5F9")); // Nền Slate 100 cao cấp
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Vẽ Header
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#1E3A8A")); // Xanh Indigo đậm
        canvas.drawRect(0, 0, width, 200, headerPaint);

        TextPaint titlePaint = new TextPaint();
        titlePaint.setAntiAlias(true);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(44f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        StaticLayout titleLayout;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            titleLayout = StaticLayout.Builder.obtain(headerTitle, 0, headerTitle.length(), titlePaint, width - 80)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();
        } else {
            titleLayout = new StaticLayout(headerTitle, titlePaint, width - 80, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
        }

        int titleHeight = titleLayout.getHeight();
        canvas.save();
        canvas.translate(40, Math.max(20, (200 - titleHeight) / 2f));
        titleLayout.draw(canvas);
        canvas.restore();

        // 4. Vẽ Card thông tin giờ trống lớn & vừa khít
        Paint cardPaint = new Paint();
        cardPaint.setColor(Color.WHITE);
        cardPaint.setShadowLayer(16, 0, 8, Color.parseColor("#CBD5E1"));
        RectF cardRect = new RectF(60, 240, width - 60, height - 100);
        canvas.drawRoundRect(cardRect, 48, 48, cardPaint);

        // Chữ Ngày
        TextPaint dayPaint = new TextPaint();
        dayPaint.setAntiAlias(true);
        dayPaint.setColor(Color.parseColor("#0F172A"));
        dayPaint.setTextSize(46);
        dayPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("📅 " + dayDisplay, 120, 340, dayPaint);

        // Chữ Khung giờ
        TextPaint timePaint = new TextPaint();
        timePaint.setAntiAlias(true);
        timePaint.setColor(Color.parseColor("#16A34A")); // Xanh lá
        timePaint.setTextSize(58);
        timePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("⏰ " + timeRangeStr, 120, 450, timePaint);

        // Đường kẻ phân cách nhạt
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        linePaint.setStrokeWidth(3);
        canvas.drawLine(120, 500, width - 120, 500, linePaint);

        // Lời nhắn / Ngữ cảnh (Tự động co cỡ chữ & cắt chữ '...' nếu quá dài để không bị tràn khung)
        TextPaint notePaint = new TextPaint();
        notePaint.setAntiAlias(true);
        notePaint.setColor(Color.parseColor("#334155"));

        float noteSize = cardNote.length() > 100 ? 32f : 36f;
        notePaint.setTextSize(noteSize);

        StaticLayout staticLayout;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(cardNote, 0, cardNote.length(), notePaint, width - 240)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, 1.4f)
                    .setMaxLines(6)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build();
        } else {
            staticLayout = new StaticLayout(cardNote, notePaint, width - 240, Layout.Alignment.ALIGN_NORMAL, 1.4f, 0, false);
        }

        canvas.save();
        canvas.translate(120, 540);
        staticLayout.draw(canvas);
        canvas.restore();

        // Footer watermark ở cuối thẻ
        TextPaint footerPaint = new TextPaint();
        footerPaint.setAntiAlias(true);
        footerPaint.setColor(Color.parseColor("#94A3B8"));
        footerPaint.setTextSize(32);
        footerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        footerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("✨ Tạo tự động bởi Trợ lý LectuPlan", width / 2f, height - 130, footerPaint);

        // 5. Lưu ra file và Share
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs();
            File imagePath = new File(cachePath, "office_hours_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri imageUri = FileProvider.getUriForFile(context, "com.tkyl.lectuplan.provider", imagePath);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.setType("image/png");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ lịch tiếp sinh viên / học bù"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
