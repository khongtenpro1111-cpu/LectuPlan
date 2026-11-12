package com.tkyl.lectuplan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

public class OfficeHoursShareHelper {
    public static void shareSlotAsImage(Context context, SmartSlotAdapter.FoundSlot slot) {
        // 1. Tạo Bitmap tỉ lệ vuông (1080x1080) chuẩn thiệp chia sẻ Zalo/Facebook
        int width = 1080;
        int height = 1080;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 2. Vẽ Nền (Background)
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
        titlePaint.setTextSize(48);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LỊCH TIẾP SINH VIÊN (OFFICE HOURS)", width / 2f, 120, titlePaint);

        // 3. Vẽ Card thông tin giờ trống lớn & vừa khít
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
        canvas.drawText("📅 " + slot.dayDisplay, 120, 340, dayPaint);

        // Chữ Khung giờ
        TextPaint timePaint = new TextPaint();
        timePaint.setAntiAlias(true);
        timePaint.setColor(Color.parseColor("#16A34A")); // Xanh lá
        timePaint.setTextSize(60);
        timePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("⏰ " + slot.timeRangeStr, 120, 450, timePaint);

        // Đường kẻ phân cách nhạt
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#E2E8F0"));
        linePaint.setStrokeWidth(3);
        canvas.drawLine(120, 500, width - 120, 500, linePaint);

        // Lời nhắn / Ngữ cảnh
        TextPaint notePaint = new TextPaint();
        notePaint.setAntiAlias(true);
        notePaint.setColor(Color.parseColor("#334155"));
        notePaint.setTextSize(38);
        
        String message = "Giảng viên có mặt tại trường và sẵn sàng hỗ trợ, giải đáp thắc mắc, duyệt đồ án cho sinh viên trong khoảng thời gian này.";
        StaticLayout staticLayout;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(message, 0, message.length(), notePaint, width - 240)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0, 1.4f)
                    .build();
        } else {
            staticLayout = new StaticLayout(message, notePaint, width - 240, Layout.Alignment.ALIGN_NORMAL, 1.4f, 0, false);
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

        // 4. Lưu ra file và Share
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

            // Text đính kèm
            String textMsg = "Chào các bạn, tuần này chúng ta sẽ học bù/gặp mặt vào " + slot.dayDisplay + ", khung giờ từ " + slot.timeRangeStr + ".\nCác bạn sắp xếp thời gian tham gia nhé!";
            shareIntent.putExtra(Intent.EXTRA_TEXT, textMsg);
            shareIntent.setType("image/png");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ lịch Office Hours"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
