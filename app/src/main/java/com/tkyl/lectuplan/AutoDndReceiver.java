package com.tkyl.lectuplan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AutoDndReceiver extends BroadcastReceiver {
    public static final String ACTION_DND_START = "com.tkyl.lectuplan.ACTION_DND_START";
    public static final String ACTION_DND_END = "com.tkyl.lectuplan.ACTION_DND_END";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_EVENT_ID = "extra_event_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        String title = intent.getStringExtra(EXTRA_EVENT_TITLE);
        int eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1);

        if (ACTION_DND_START.equals(action)) {
            if (AutoDndHelper.isAutoDndEnabled(context)) {
                AutoDndHelper.enableDnd(context);
                AlarmReceiver.triggerSoundAndNotification(context,
                    "🔇 Đã bật Không Làm Phiền",
                    "Đã bắt đầu ca học: " + title);
                new DatabaseHelper(context).insertNotification("🔇 Đã bật Không Làm Phiền", "Đã bắt đầu ca học: " + title, "DND");
            }
            if (AutoDndHelper.isAutoCheckinEnabled(context)) {
                performGpsCheckIn(context, eventId, title);
            }
        } else if (ACTION_DND_END.equals(action)) {
            if (AutoDndHelper.isAutoDndEnabled(context)) {
                AutoDndHelper.disableDnd(context);
                AlarmReceiver.triggerSoundAndNotification(context,
                    "🔊 Đã khôi phục Chuông",
                    "Đã kết thúc ca học: " + title);
                new DatabaseHelper(context).insertNotification("🔊 Đã khôi phục Chuông", "Đã kết thúc ca học: " + title, "DND");
            }
            // Tự động bắn thông báo nhắc Giảng viên Ghi âm dặn dò sau ca dạy
            AlarmReceiver.triggerVoiceMemoNotification(context, title);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void performGpsCheckIn(Context context, int eventId, String title) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (lm != null) {
                    Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (loc == null) {
                        loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    if (loc != null) {
                        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        AlarmReceiver.triggerSoundAndNotification(context,
                            "📍 Check-in Thành Công",
                            title + "\nTọa độ: " + loc.getLatitude() + ", " + loc.getLongitude());
                        new DatabaseHelper(context).insertNotification("📍 Check-in Thành Công", title + "\nTọa độ: " + loc.getLatitude() + ", " + loc.getLongitude(), "CHECKIN");
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
