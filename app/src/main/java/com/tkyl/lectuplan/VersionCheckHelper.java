package com.tkyl.lectuplan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class VersionCheckHelper {

    private static final String PREF_VERSION = "lectuplan_version_prefs";
    private static final String KEY_LAST_VERSION_CODE = "key_last_version_code";

    // 🌐 ĐƯỜNG DẪN CẤU HÌNH KIỂM TRA BẢN CẬP NHẬT TRỰC TUYẾN
    public static final String DEFAULT_UPDATE_CONFIG_URL = "https://raw.githubusercontent.com/khongtenpro1111-cpu/LectuPlan/main/version.json";

    public static int getCurrentVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 1;
        }
    }

    public static String getCurrentVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.1.1";
        }
    }

    /**
     * Tự động kiểm tra và hiển thị Hộp thoại "Có gì mới trong phiên bản này?"
     * ngay khi người dụng vừa cập nhật app lên phiên bản mới.
     */
    public static void checkAndShowWhatsNew(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREF_VERSION, Context.MODE_PRIVATE);
        int lastVersionCode = prefs.getInt(KEY_LAST_VERSION_CODE, 0);
        int currentVersionCode = getCurrentVersionCode(activity);

        if (lastVersionCode < currentVersionCode) {
            // Hiển thị Dialog "Có gì mới"
            showWhatsNewDialog(activity, currentVersionCode);
            // Lưu lại versionCode mới để không hiển thị lại ở những lần mở sau
            prefs.edit().putInt(KEY_LAST_VERSION_CODE, currentVersionCode).apply();
        } else {
            // Kiểm tra bản cập nhật trực tuyến ngầm từ Server/GitHub
            checkRemoteUpdateAsync(activity, false);
        }
    }

    public static void showWhatsNewDialog(Activity activity, int versionCode) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_whats_new, null);
        TextView tvVersionName = view.findViewById(R.id.tvWhatsNewVersion);
        Button btnGotIt = view.findViewById(R.id.btnWhatsNewClose);

        if (tvVersionName != null) {
            tvVersionName.setText("Phiên bản v" + getCurrentVersionName(activity) + " (Build " + versionCode + ")");
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnGotIt != null) {
            btnGotIt.setOnClickListener(v -> {
                SoundHelper.playFeedback(activity, v);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    /**
     * Kiểm tra bản cập nhật trực tuyến từ Máy chủ / GitHub Raw (Server Version Checker)
     */
    public static void checkForUpdatesManual(Activity activity) {
        SoundHelper.playFeedback(activity, null);
        Toast.makeText(activity, "🔍 Đang kết nối máy chủ kiểm tra bản cập nhật...", Toast.LENGTH_SHORT).show();
        checkRemoteUpdateAsync(activity, true);
    }

    private static void checkRemoteUpdateAsync(Activity activity, boolean isManualCheck) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(DEFAULT_UPDATE_CONFIG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject json = new JSONObject(response.toString());
                    int remoteVersionCode = json.optInt("latest_version_code", 3);
                    String remoteVersionName = json.optString("latest_version_name", "1.1.1");
                    String apkUrl = json.optString("apk_url", "");
                    String releaseNotes = json.optString("release_notes", "Bổ sung các cải tiến và sửa lỗi hiệu năng.");
                    boolean forceUpdate = json.optBoolean("force_update", false);

                    int currentCode = getCurrentVersionCode(activity);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;

                        if (remoteVersionCode > currentCode) {
                            showRemoteUpdateAvailableDialog(activity, remoteVersionName, releaseNotes, apkUrl, forceUpdate);
                        } else if (isManualCheck) {
                            showAlreadyLatestDialog(activity);
                        }
                    });
                    return;
                }
            } catch (Exception e) {
                // Xử lý khi lỗi mạng hoặc chưa cấu hình URL thực tế
            }

            // Fallback khi không kết nối được server hoặc chạy thủ công
            new Handler(Looper.getMainLooper()).post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (isManualCheck) {
                    showAlreadyLatestDialog(activity);
                }
            });
        });
    }

    private static void showRemoteUpdateAvailableDialog(Activity activity, String newVersionName, String notes, String apkUrl, boolean forceUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("🚀 ĐÃ CÓ BẢN CẬP NHẬT MỚI (v" + newVersionName + ")")
                .setMessage("Phiên bản LectuPlan v" + newVersionName + " đã có sẵn với các cải tiến:\n\n" + notes + "\n\nBạn có muốn tải bản cập nhật về ngay không?")
                .setPositiveButton("TẢI VỀ CẬP NHẬT", (dialog, which) -> {
                    if (apkUrl != null && !apkUrl.isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                            activity.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(activity, "Không thể mở liên kết tải về!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        if (!forceUpdate) {
            builder.setNegativeButton("Để sau", null);
        } else {
            builder.setCancelable(false);
        }

        builder.show();
    }

    private static void showAlreadyLatestDialog(Activity activity) {
        String currentVer = getCurrentVersionName(activity);
        new AlertDialog.Builder(activity)
                .setTitle("🎉 ỨNG DỤNG ĐÃ Ở PHIÊN BẢN MỚI NHẤT")
                .setMessage("Bạn đang sử dụng phiên bản LectuPlan v" + currentVer + " mới nhất.\n\nToàn bộ bộ 5 tính năng cao cấp (Gợi ý Khung giờ Trống, Auto DND, Check-in GPS, Dự toán Thu nhập, Thiệp Office Hours, Ghi âm giọng nói) đã sẵn sàng phục vụ bạn!")
                .setPositiveButton("Xem các tính năng mới", (dialog, which) -> {
                    showWhatsNewDialog(activity, getCurrentVersionCode(activity));
                })
                .setNegativeButton("Đóng", null)
                .show();
    }
}

