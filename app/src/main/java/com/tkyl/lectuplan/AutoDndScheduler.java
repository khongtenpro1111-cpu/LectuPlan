package com.tkyl.lectuplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AutoDndScheduler {

    public static void syncTodayAlarms(Context context) {
        if (!AutoDndHelper.isAutoDndEnabled(context) && !AutoDndHelper.isAutoCheckinEnabled(context)) {
            // Can optimize to cancel all, but for now we just skip.
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String todayIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        List<EventModel> events = dbHelper.getEventsForDate(todayIso);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long now = System.currentTimeMillis();

        for (EventModel ev : events) {
            // Schedule Start (ID * 2)
            scheduleEventAlarm(context, alarmManager, ev, ev.getStartMin(), AutoDndReceiver.ACTION_DND_START, ev.getId() * 2, now);
            // Schedule End (ID * 2 + 1)
            scheduleEventAlarm(context, alarmManager, ev, ev.getEndMin(), AutoDndReceiver.ACTION_DND_END, ev.getId() * 2 + 1, now);
        }
    }

    private static void scheduleEventAlarm(Context context, AlarmManager alarmManager, EventModel ev, int minuteOfDay, String action, int requestCode, long now) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        cal.set(Calendar.MINUTE, minuteOfDay % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long triggerAt = cal.getTimeInMillis();
        if (triggerAt <= now) return; // Passed

        Intent intent = new Intent(context, AutoDndReceiver.class);
        intent.setAction(action);
        intent.putExtra(AutoDndReceiver.EXTRA_EVENT_TITLE, ev.getTitle());
        intent.putExtra(AutoDndReceiver.EXTRA_EVENT_ID, ev.getId());

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(triggerAt, pi);
                alarmManager.setAlarmClock(clockInfo, pi);
            }
        } catch (SecurityException e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
