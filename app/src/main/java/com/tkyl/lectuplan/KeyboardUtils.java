package com.tkyl.lectuplan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility class giúp tự động ẩn bàn phím ảo khi người dùng chạm vào bất kỳ vị trí nào
 * bên ngoài EditText đang có focus (áp dụng cho toàn bộ Activity và Dialog/AlertDialog).
 */
public class KeyboardUtils {

    /**
     * Xử lý Touch Event trên Activity để ẩn bàn phím khi chạm ra ngoài EditText.
     * Call this in override dispatchTouchEvent(MotionEvent ev) of Activity.
     */
    public static void handleTouchHideKeyboard(Activity activity, MotionEvent ev) {
        if (activity == null || ev == null) return;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = activity.getCurrentFocus();
            if (v instanceof EditText) {
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                float x = ev.getRawX();
                float y = ev.getRawY();
                if (x < location[0] || x > location[0] + v.getWidth() ||
                    y < location[1] || y > location[1] + v.getHeight()) {
                    hideKeyboardAndClearFocus(v);
                }
            }
        }
    }

    /**
     * Áp dụng tự động ẩn bàn phím khi chạm ra ngoài EditText cho một Dialog/AlertDialog.
     */
    public static void applyAutoHideKeyboard(Dialog dialog) {
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window == null) return;

        Window.Callback originalCallback = window.getCallback();
        if (originalCallback == null || originalCallback instanceof AutoHideKeyboardWindowCallback) {
            return;
        }

        window.setCallback(new AutoHideKeyboardWindowCallback(originalCallback, dialog));
    }

    /**
     * Ẩn bàn phím ảo và xóa focus khỏi View hiện tại.
     */
    public static void hideKeyboardAndClearFocus(View v) {
        if (v == null) return;
        v.clearFocus();
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private static class AutoHideKeyboardWindowCallback implements Window.Callback {
        private final Window.Callback target;
        private final Dialog dialog;

        public AutoHideKeyboardWindowCallback(@NonNull Window.Callback target, Dialog dialog) {
            this.target = target;
            this.dialog = dialog;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return target.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return target.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (event != null && event.getAction() == MotionEvent.ACTION_DOWN) {
                View v = dialog.getCurrentFocus();
                if (v instanceof EditText) {
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    float x = event.getRawX();
                    float y = event.getRawY();
                    if (x < location[0] || x > location[0] + v.getWidth() ||
                        y < location[1] || y > location[1] + v.getHeight()) {
                        hideKeyboardAndClearFocus(v);
                    }
                }
            }
            return target.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            return target.dispatchTrackballEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return target.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return target.dispatchPopulateAccessibilityEvent(event);
        }

        @Nullable
        @Override
        public View onCreatePanelView(int featureId) {
            return target.onCreatePanelView(featureId);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
            return target.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu) {
            return target.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, @NonNull Menu menu) {
            return target.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
            return target.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            target.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            target.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            target.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            target.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            target.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, @NonNull Menu menu) {
            target.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return target.onSearchRequested();
        }

        @Override
        public boolean onSearchRequested(SearchEvent searchEvent) {
            return target.onSearchRequested(searchEvent);
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            return target.onWindowStartingActionMode(callback);
        }

        @Nullable
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
            return target.onWindowStartingActionMode(callback, type);
        }

        @Override
        public void onActionModeStarted(ActionMode mode) {
            target.onActionModeStarted(mode);
        }

        @Override
        public void onActionModeFinished(ActionMode mode) {
            target.onActionModeFinished(mode);
        }
    }
}
