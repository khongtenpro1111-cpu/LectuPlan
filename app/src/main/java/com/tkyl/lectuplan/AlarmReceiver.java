package com.tkyl.lectuplan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "LECTUPLAN_ALARM_CHANNEL";
    public static Ringtone currentRingtone = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Tự động lên lịch lại báo thức cho ngày tiếp theo hoặc sau khi reboot máy
        AlarmScheduler.scheduleAlarm(context);
        
        // Tự động đồng bộ DND alarms hàng ngày
        AutoDndScheduler.syncTodayAlarms(context);

        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            // Đã cập nhật lại lịch báo thức sau khi khởi động máy, không phát âm thanh lúc này
            return;
        }

        // Kiểm tra cài đặt báo thức người dùng
        if (!AlarmScheduler.isAlarmEnabled(context)) {
            return;
        }

        DatabaseHelper db = new DatabaseHelper(context);
        List<EventModel> upcoming = db.getUpcomingEventsForAlarm();

        boolean onlyIfEvents = AlarmScheduler.isOnlyIfEventsEnabled(context);
        if (onlyIfEvents && upcoming.isEmpty()) {
            // Báo thức thông minh: Hôm nay/ngày mai rảnh rỗi, không báo chuông
            return;
        }

        int hour = AlarmScheduler.getAlarmHour(context);
        int minute = AlarmScheduler.getAlarmMinute(context);
        String timeStr = AlarmScheduler.getFormattedTime(hour, minute);

        String title = "⏰ Nhắc lịch Giảng viên (" + timeStr + ")";
        String message;

        if (!upcoming.isEmpty()) {
            StringBuilder content = new StringBuilder();
            for (EventModel e : upcoming) {
                content.append("• ").append(e.getTitle()).append(" (").append(e.getDate()).append(")\n");
            }
            message = "Thầy/Cô có " + upcoming.size() + " lịch công việc sắp diễn ra:\n" + content;
        } else {
            message = "Thầy/Cô không có lịch bận trong ngày hôm nay. Chúc Thầy/Cô một ngày làm việc hiệu quả!";
        }

        triggerSoundAndNotification(context, title, message);
    }

    public static void triggerVoiceMemoNotification(Context context, String subjectTitle) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "LECTUPLAN_VOICE_MEMO", "Ghi chú Giọng nói LectuPlan", NotificationManager.IMPORTANCE_HIGH);
                manager.createNotificationChannel(channel);
            }

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.putExtra("ACTION_VOICE_MEMO_POST_CLASS", true);
            openIntent.putExtra("EXTRA_SUBJECT_TITLE", subjectTitle);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, (int) System.currentTimeMillis(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "LECTUPLAN_VOICE_MEMO")
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentTitle("🎙️ Dặn dò sau ca dạy " + (subjectTitle != null ? subjectTitle : ""))
                    .setContentText("Chạm vào đây để ghi âm dặn dò/ghi chú cho ca học tiếp theo!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    public static void stopAlarmSound() {
        try {
            if (currentRingtone != null && currentRingtone.isPlaying()) {
                currentRingtone.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            currentRingtone = null;
        }
    }

    public static boolean isAlarmRinging() {
        try {
            return currentRingtone != null && currentRingtone.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public static void triggerSoundAndNotification(Context context, String title, String message) {
        stopAlarmSound(); // Tắt âm thanh cũ nếu đang phát

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        // Phát âm thanh trực tiếp để đảm bảo giảng viên nghe thấy rõ
        try {
            currentRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), alarmSound);
            if (currentRingtone != null) {
                currentRingtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, "Báo thức LectuPlan", NotificationManager.IMPORTANCE_HIGH);
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();
                channel.setSound(alarmSound, attributes);
                manager.createNotificationChannel(channel);
            }

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.putExtra("SHOW_STOP_ALARM_DIALOG", true);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setSound(alarmSound)
                    .setContentIntent(pendingIntent);

            manager.notify(600, builder.build());
        }
    }
}
