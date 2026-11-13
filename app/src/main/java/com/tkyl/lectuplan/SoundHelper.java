package com.tkyl.lectuplan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.View;

public class SoundHelper {
    private static final String TAG = "SoundHelper";

    public static final String PREF_NAME = "lectuplan_sound_prefs";
    public static final String KEY_SOUND_ENABLED = "key_sound_enabled";
    public static final String KEY_HAPTIC_ENABLED = "key_haptic_enabled";
    public static final String KEY_SOUND_STYLE = "key_sound_style";

    public static final int STYLE_STANDARD_CLICK = 0; // Phím bấm tiêu chuẩn (System Click)
    public static final int STYLE_GENTLE_POP = 1;     // Tiếng Pop nhã nhặn (Soft Ack Tone)
    public static final int STYLE_LECTURE_CHIME = 2;  // Chuông nhẹ giảng đường (Soft Chime)

    private static ToneGenerator toneGenMusic;
    private static long lastFeedbackTime = 0;

    public static boolean isSoundEnabled(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public static boolean isHapticEnabled(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true);
    }

    public static void setHapticEnabled(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply();
    }

    public static int getSoundStyle(Context context) {
        if (context == null) return STYLE_STANDARD_CLICK;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SOUND_STYLE, STYLE_STANDARD_CLICK);
    }

    public static void setSoundStyle(Context context, int style) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_SOUND_STYLE, style).apply();
    }

    /**
     * Tự động phát âm thanh và/hoặc rung phản hồi dựa trên Cài đặt hiện tại.
     * Có tích hợp chống trùng lặp âm (debounce 100ms).
     */
    public static synchronized void playFeedback(Context context, View view) {
        if (context == null) return;

        if (view != null) {
            view.setSoundEffectsEnabled(false);
        }

        if (!isSoundEnabled(context)) return;

        long now = System.currentTimeMillis();
        if (now - lastFeedbackTime < 100) return;
        lastFeedbackTime = now;

        playSound(context, view, true);

        if (isHapticEnabled(context)) {
            playHaptic(context, view, true);
        }
    }

    /**
     * Phát hiệu ứng âm thanh nút bấm dứt khoát trên kênh STREAM_MUSIC / PCM AudioTrack.
     */
    public static void playSound(Context context, View view) {
        playSound(context, view, false);
    }

    public static void playSound(Context context, View view, boolean force) {
        if (context == null) return;
        if (view != null) {
            view.setSoundEffectsEnabled(false);
        }
        if (!force && !isSoundEnabled(context)) return;

        int style = getSoundStyle(context);
        try {
            switch (style) {
                case STYLE_GENTLE_POP:
                    // Tiếng Pop nhã nhặn (Âm PCM 880Hz, 35ms)
                    playPcmBeep(880, 35);
                    break;
                case STYLE_LECTURE_CHIME:
                    // Chuông nhẹ giảng đường (Âm PCM 1760Hz, 60ms)
                    playPcmBeep(1760, 60);
                    break;
                case STYLE_STANDARD_CLICK:
                default:
                    // Phím bấm tiêu chuẩn (Âm PCM 1400Hz, 25ms dứt khoát)
                    playPcmBeep(1400, 25);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + e.getMessage());
        }
    }

    private static final java.util.concurrent.ExecutorService soundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    /**
     * Tổng hợp sóng âm PCM trực tiếp qua AudioTrack để đảm bảo phát âm thanh 100% rõ ràng,
     * ngay cả khi hệ thống điện thoại đang tắt âm bàn phím gốc.
     */
    private static void playPcmBeep(int frequencyHz, int durationMs) {
        soundExecutor.execute(() -> {
            try {
                int sampleRate = 22050;
                int numSamples = sampleRate * durationMs / 1000;
                double[] sample = new double[numSamples];
                byte[] generatedSnd = new byte[2 * numSamples];

                for (int i = 0; i < numSamples; ++i) {
                    double angle = 2.0 * Math.PI * i / (sampleRate / (double) frequencyHz);
                    // Fade out envelope để tránh tiếng lạch cạch ở cuối âm
                    double envelope = 1.0 - ((double) i / numSamples);
                    sample[i] = Math.sin(angle) * envelope;
                }

                int idx = 0;
                for (final double dVal : sample) {
                    final short val = (short) ((dVal * 32767));
                    generatedSnd[idx++] = (byte) (val & 0x00ff);
                    generatedSnd[idx++] = (byte) ((val & 0xff00) >> 8);
                }

                AudioTrack audioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(generatedSnd.length)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                audioTrack.write(generatedSnd, 0, generatedSnd.length);
                audioTrack.play();
                try {
                    Thread.sleep(durationMs + 20);
                } catch (InterruptedException ignored) {}
                audioTrack.release();
            } catch (Exception e) {
                // Fallback nếu AudioTrack bận: Dùng ToneGenerator
                try {
                    ToneGenerator tg = getToneGenerator();
                    if (tg != null) tg.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * Phát hiệu ứng rung phản hồi xúc giác (Haptic Feedback) dứt khoát qua Vibrator.
     */
    public static void playHaptic(Context context, View view) {
        playHaptic(context, view, false);
    }

    @SuppressLint("MissingPermission")
    public static void playHaptic(Context context, View view, boolean force) {
        if (context == null) return;
        if (!force && (!isSoundEnabled(context) || !isHapticEnabled(context))) return;

        try {
            Vibrator v = getVibrator(context);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(35);
                }
            } else if (view != null) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing haptic: " + e.getMessage());
        }
    }

    public static String getSoundStyleName(int style) {
        switch (style) {
            case STYLE_GENTLE_POP:
                return "🎵 Tiếng Pop nhã nhặn";
            case STYLE_LECTURE_CHIME:
                return "🔔 Chuông nhẹ giảng đường";
            case STYLE_STANDARD_CLICK:
            default:
                return "🔊 Phím bấm tiêu chuẩn";
        }
    }

    private static synchronized ToneGenerator getToneGenerator() {
        if (toneGenMusic == null) {
            try {
                toneGenMusic = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            } catch (Exception e) {
                try {
                    toneGenMusic = new ToneGenerator(AudioManager.STREAM_SYSTEM, 100);
                } catch (Exception ignored) {}
            }
        }
        return toneGenMusic;
    }

    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        } else {
            return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }
}
