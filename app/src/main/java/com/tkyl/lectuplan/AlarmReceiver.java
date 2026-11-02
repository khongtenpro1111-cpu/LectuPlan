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
    private static final String CHANNEL_ID = "LECTUPLAN_6AM_ALARM";
    public static Ringtone currentRingtone = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<EventModel> upcoming = db.getUpcomingEventsForAlarm();

        // Chỉ phát âm thanh nếu có sự kiện trong ngày hoặc báo trước 24h (ngày mai)
        if (!upcoming.isEmpty()) {
            StringBuilder content = new StringBuilder();
            for (EventModel e : upcoming) {
                content.append("• ").append(e.getTitle()).append(" (").append(e.getDate()).append(")\n");
            }
            triggerSoundAndNotification(context, "Nhắc lịch Giảng viên (6h00 sáng)",
                    "Thầy/Cô có " + upcoming.size() + " lịch công việc sắp diễn ra:\n" + content);
        }
    }

    public static void stopAlarmSound() {
        try {
            if (currentRingtone != null && currentRingtone.isPlaying()) {
                currentRingtone.stop();
            }
            currentRingtone = null;
        } catch (Exception e) {
            e.printStackTrace();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Báo lịch 6h00 sáng", NotificationManager.IMPORTANCE_HIGH);
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
