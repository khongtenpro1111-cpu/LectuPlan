package com.tkyl.lectuplan;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfRenderer;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.widget.NestedScrollView;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EventAdapter adapter;
    private FileHistoryAdapter fileHistoryAdapter;
    private RecyclerView rvEvents;
    private EditText edtSearch;
    private Spinner spnSemester, spnYear, spnChartType;
    private BarChart barChart, barChartTab;
    private TextView tvTotalCount;
    private View btnNotificationBell;
    private TextView tvUnreadNotifBadge;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddEvent, fabScrollToTop;
    private boolean isFilteringToday = false;
    private int currentCalendarMode = 0; // 0: Ngày, 1: Tuần, 2: Tháng, 3: Trống

    // Tab 3: Biểu đồ & Thống kê đa trường
    private Spinner spnChartTimeFilter, spnChartSchoolFilter;
    private TextView tvKpiPeriods, tvKpiPeriodsSub, tvKpiEvents, tvKpiEventsSub;
    private TextView tvKpiSchools, tvKpiSchoolsSub, tvKpiIncidents, tvKpiIncidentsSub;
    private TextView tvKpiIncome, tvKpiIncomeSub;
    private MaterialCardView cardCrossSchoolAlert;
    private TextView tvCrossSchoolAlertTitle, tvCrossSchoolAlertDesc;
    private Button btnViewCrossSchoolDays;
    private PieChart pieChartSchool;
    private HorizontalBarChart barChartNature;
    private BarChart barChartDayOfWeek;
    private PieChart pieChartShift;
    private TextView tvMonthlyPeakNotice;
    private MaterialCardView cardKpiPeriods, cardKpiEvents, cardKpiSchools, cardKpiIncidents, cardKpiIncome;
    private List<EventModel> currentFilteredChartEvents = new ArrayList<>();
    private final Map<String, List<EventModel>> crossSchoolDaysMap = new LinkedHashMap<>();

    // Tab 2 Lịch biểu
    private RecyclerView rvTab2Events;
    private EventAdapter tab2Adapter;
    private int currentTab2Mode = 0; // 0: Ngày, 1: Tuần, 2: Tháng, 3: Trống
    private String selectedTab2DateIso = "";
    private boolean isTab2Dirty = true;
    private int currentActiveTabIndex = 0;
    private int savedInitialTabIndex = 0;
    private final RecyclerView.RecycledViewPool sharedEventViewPool = new RecyclerView.RecycledViewPool();

    // Custom Month Calendar with Dot Badges
    private RecyclerView rvCustomCalendarGrid;
    private CustomCalendarAdapter customCalendarAdapter;
    private TextView tvCalMonthTitle;
    private Calendar currentCalMonthInstance;

    private View viewActivePillIndicator;
    private String currentCategoryFilter = "ALL";
    private LinearLayout layoutNavContainer;

    // 1. Bộ chọn file Excel (.xlsx / .xls)
    private final ActivityResultLauncher<String> excelPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) importExcelFile(uri);
            });

    // 2. Bộ chọn file PDF (.pdf)
    private final ActivityResultLauncher<String> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) processPdfImport(uri);
            });

    // 3. Bộ chọn Ảnh chụp OCR (.png, .jpg)
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) processImageOcrImport(uri);
            });

    // 4. Bộ chọn Đa năng (Tự động nhận diện mọi loại file: Excel, PDF, Word, CSV/TXT, Ảnh/Tài liệu ghi tay)
    private final ActivityResultLauncher<String> universalFilePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) processUniversalFileImport(uri);
            });

    // Bộ chọn tạo file Excel xuất báo cáo (SAF)
    private final ActivityResultLauncher<String> exportExcelLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), uri -> {
                if (uri != null) writeExcelToUri(uri);
            });

    // Bộ chọn lưu/tải bất kỳ file nào về thư mục tùy chọn trên máy (SAF)
    private String pendingSaveFileName = null;
    private final ActivityResultLauncher<String> saveAnyFileLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"), uri -> {
                if (uri != null && pendingSaveFileName != null) {
                    writeHistoryFileToCustomUri(pendingSaveFileName, uri);
                }
            });

    // 🌟 TÍNH NĂNG 5: Nhận diện giọng nói chuyển đổi thành văn bản (Speech-To-Text)
    private String pendingVoiceSubjectTitle = null;
    private final ActivityResultLauncher<Intent> speechToTextLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<String> results = result.getData().getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        String spokenText = results.get(0);
                        handleVoiceMemoResult(spokenText);
                    }
                }
            });

    private void launchSpeechRecognizer(String subjectTitle) {
        this.pendingVoiceSubjectTitle = subjectTitle;
        Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "🎙️ Nói ghi chú dặn dò " + (subjectTitle != null ? "cho " + subjectTitle : "") + "...");
        try {
            speechToTextLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Thiết bị không hỗ trợ nhận diện giọng nói Tiếng Việt!", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVoiceMemoResult(String spokenText) {
        if (spokenText == null || spokenText.trim().isEmpty()) return;
        
        playSuccessSound();
        String subject = pendingVoiceSubjectTitle != null ? pendingVoiceSubjectTitle : "Môn học";
        
        new AlertDialog.Builder(this)
                .setTitle("🎙️ Nhận diện giọng nói thành công")
                .setMessage("Nội dung dặn dò:\n\"" + spokenText + "\"\n\nBạn có muốn lưu thông tin này vào danh sách Lịch trình không?")
                .setPositiveButton("Lưu Lịch Ghi Chú", (dialog, which) -> {
                    SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String todayIso = sdfIso.format(new Date());
                    
                    String noteTitle = "📝 [Dặn dò] " + subject + ": " + spokenText;
                    dbHelper.insertEvent(1, noteTitle, todayIso, "Ghi chú giọng nói Giảng viên", EventModel.CAT_MANUAL_OTHER);
                    refreshAllTabs();
                    Toast.makeText(this, "✅ Đã lưu ghi chú giọng nói thành công!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkVoiceMemoIntent(intent);
    }

    private void checkVoiceMemoIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("ACTION_VOICE_MEMO_POST_CLASS", false)) {
            String subject = intent.getStringExtra("EXTRA_SUBJECT_TITLE");
            new AlertDialog.Builder(this)
                    .setTitle("🎙️ Ghi chú sau buổi học")
                    .setMessage("Bạn vừa hoàn thành ca dạy '" + (subject != null ? subject : "") + "'. Bạn có muốn ghi âm dặn dò / nhắc nhở cho buổi học tới không?")
                    .setPositiveButton("🎙️ Ghi âm ngay", (dialog, which) -> {
                        launchSpeechRecognizer(subject);
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        KeyboardUtils.handleTouchHideKeyboard(this, ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View target = findInteractiveTarget(getWindow().getDecorView(), (int) ev.getRawX(), (int) ev.getRawY());
            if (target != null) {
                SoundHelper.playFeedback(this, target);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private View findInteractiveTarget(View view, int rawX, int rawY) {
        if (view == null || view.getVisibility() != View.VISIBLE) return null;

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();

        if (rawX < left || rawX > right || rawY < top || rawY > bottom) {
            return null;
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View childTarget = findInteractiveTarget(group.getChildAt(i), rawX, rawY);
                if (childTarget != null) return childTarget;
            }
        }

        if (view.isClickable() || view.isLongClickable() || isInteractiveControl(view)) {
            return view;
        }

        return null;
    }

    private boolean isInteractiveControl(View v) {
        return v instanceof android.widget.Button
                || v instanceof android.widget.CompoundButton
                || v instanceof android.widget.Spinner
                || v instanceof com.google.android.material.card.MaterialCardView
                || v instanceof com.google.android.material.chip.Chip;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(FontScaleHelper.wrapContext(newBase));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("saved_active_tab_index", currentActiveTabIndex);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            savedInitialTabIndex = savedInstanceState.getInt("saved_active_tab_index", 0);
        }

        // Xử lý System Bar Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            insetsController.setAppearanceLightStatusBars(!isNightMode);
            insetsController.setAppearanceLightNavigationBars(!isNightMode);
        }

        requestNotificationPermission();

        dbHelper = new DatabaseHelper(this);
        dbHelper.cleanUpGarbageCvEntries();
        dbHelper.healCorruptedSubjectNames();
        dbHelper.syncPhysicalFilesIntoHistory(getHistoryStorageDir());
        dbHelper.syncOrphanFilesIntoHistory();

        if (AutoDeleteHelper.isEnabled(this)) {
            int retentionMonths = AutoDeleteHelper.getRetentionMonths(this);
            int deletedRows = dbHelper.autoDeleteOldData(retentionMonths, getHistoryStorageDir());
            if (deletedRows > 0) {
                AutoDeleteHelper.recordCleanupTime(this);
                Toast.makeText(this, "Đã tự động dọn dẹp " + deletedRows + " lịch & tệp cũ quá " + AutoDeleteHelper.getRetentionText(retentionMonths) + "!", Toast.LENGTH_LONG).show();
            }
        }

        AlarmScheduler.scheduleAlarm(this);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        edtSearch = findViewById(R.id.edtSearch);
        spnSemester = findViewById(R.id.spnSemester);
        spnYear = findViewById(R.id.spnYear);
        spnChartType = findViewById(R.id.spnChartType);
        barChartTab = findViewById(R.id.barChartTab);
        barChart = barChartTab;
        initChartTabViews();
        tvTotalCount = findViewById(R.id.tvTotalCount);
        btnNotificationBell = findViewById(R.id.btnNotificationBell);
        tvUnreadNotifBadge = findViewById(R.id.tvUnreadNotifBadge);
        
        if (btnNotificationBell != null) {
            btnNotificationBell.setOnClickListener(v -> {
                SoundHelper.playFeedback(this, v);
                showNotificationCenterDialog();
            });
        }
        updateUnreadNotificationBadge();

        fabAddEvent = findViewById(R.id.fabAddEvent);
        rvEvents = findViewById(R.id.rvEvents);
        RecyclerView rvFileHistory = findViewById(R.id.rvFileHistory);

        // Chuyển đổi Lịch trình Tab 1
        TextView btnModeDay = findViewById(R.id.btnModeDay);
        TextView btnModeWeek = findViewById(R.id.btnModeWeek);
        TextView btnModeMonth = findViewById(R.id.btnModeMonth);
        TextView btnModeFree = findViewById(R.id.btnModeFree);

        if (btnModeDay != null) btnModeDay.setOnClickListener(v -> applyCalendarMode(0));
        if (btnModeWeek != null) btnModeWeek.setOnClickListener(v -> applyCalendarMode(1));
        if (btnModeMonth != null) btnModeMonth.setOnClickListener(v -> applyCalendarMode(2));
        if (btnModeFree != null) btnModeFree.setOnClickListener(v -> applyCalendarMode(3));

        // Tab 2 Lịch biểu tương tác
        rvTab2Events = findViewById(R.id.rvTab2Events);
        View cardCalendarWidget = findViewById(R.id.cardCalendarWidget);
        TextView btnToggleCalendarTab2 = findViewById(R.id.btnToggleCalendarTab2);
        TextView tvTab2SelectedDateTitle = findViewById(R.id.tvTab2SelectedDateTitle);
        TextView btnQuickPickCalendar = findViewById(R.id.btnQuickPickCalendar);

        // Khởi tạo Custom Calendar Grid với các dấu chấm tròn phân loại công việc
        rvCustomCalendarGrid = findViewById(R.id.rvCustomCalendarGrid);
        tvCalMonthTitle = findViewById(R.id.tvCalMonthTitle);
        View btnPickMonthYear = findViewById(R.id.btnPickMonthYear);
        View btnCalPrevMonth = findViewById(R.id.btnCalPrevMonth);
        View btnCalNextMonth = findViewById(R.id.btnCalNextMonth);

        currentCalMonthInstance = Calendar.getInstance();
        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedTab2DateIso = sdfIso.format(currentCalMonthInstance.getTime());

        if (rvCustomCalendarGrid != null) {
            rvCustomCalendarGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 7));
            rvCustomCalendarGrid.setHasFixedSize(true);
            customCalendarAdapter = new CustomCalendarAdapter(new ArrayList<>(), selectedTab2DateIso, new HashMap<>(), cell -> {
                selectedTab2DateIso = cell.dateIso;
                if (!cell.isCurrentMonth) {
                    currentCalMonthInstance.setTime(cell.date);
                }
                updateCustomCalendarView();
                isTab2Dirty = true;
                refreshTab2CalendarEvents();
            });
            rvCustomCalendarGrid.setAdapter(customCalendarAdapter);
            updateCustomCalendarView();
        }

        // Cho phép người dùng vuốt cử chỉ (Gesture Swipe) sang trái / phải để chuyển tháng mượt mà
        android.view.GestureDetector calendarGestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50;
            private static final int SWIPE_VELOCITY_THRESHOLD = 50;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Vuốt từ trái sang phải: LÙI VỀ THÁNG TRƯỚC
                            navigateCalendarMonth(-1);
                            return true;
                        } else {
                            // Vuốt từ phải sang trái: TIẾN TỚI THÁNG TIẾP THEO
                            navigateCalendarMonth(1);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        if (rvCustomCalendarGrid != null) {
            rvCustomCalendarGrid.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
                private float startX = 0f;
                private float startY = 0f;
                private boolean isSwiping = false;

                @Override
                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                    calendarGestureDetector.onTouchEvent(e);
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = e.getX();
                            startY = e.getY();
                            isSwiping = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float dx = Math.abs(e.getX() - startX);
                            float dy = Math.abs(e.getY() - startY);
                            if (dx > 40 && dx > dy) {
                                isSwiping = true;
                                if (rv.getParent() != null) {
                                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                                }
                                return true;
                            }
                            break;
                    }
                    return isSwiping;
                }

                @Override
                public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                    calendarGestureDetector.onTouchEvent(e);
                }
            });
        }

        View layoutCustomCalendarRoot = findViewById(R.id.layoutCustomCalendarRoot);
        if (layoutCustomCalendarRoot != null) {
            layoutCustomCalendarRoot.setOnTouchListener((v, event) -> calendarGestureDetector.onTouchEvent(event));
        }
        if (cardCalendarWidget != null) {
            cardCalendarWidget.setOnTouchListener((v, event) -> calendarGestureDetector.onTouchEvent(event));
        }

        if (btnCalPrevMonth != null) {
            btnCalPrevMonth.setOnClickListener(v -> navigateCalendarMonth(-1));
        }
        if (btnCalNextMonth != null) {
            btnCalNextMonth.setOnClickListener(v -> navigateCalendarMonth(1));
        }

        // Nhấp vào tiêu đề Tháng / Năm hoặc biểu tượng tam giác để mở hộp thoại chọn Tháng và Năm
        if (btnPickMonthYear != null) {
            btnPickMonthYear.setOnClickListener(v -> showMonthYearPickerDialog());
        }
        if (tvCalMonthTitle != null) {
            tvCalMonthTitle.setOnClickListener(v -> showMonthYearPickerDialog());
        }

        if (btnToggleCalendarTab2 != null && cardCalendarWidget != null) {
            btnToggleCalendarTab2.setOnClickListener(v -> {
                if (cardCalendarWidget.getVisibility() == View.VISIBLE) {
                    cardCalendarWidget.setVisibility(View.GONE);
                    btnToggleCalendarTab2.setText("📅 Mở lịch tháng");
                } else {
                    cardCalendarWidget.setVisibility(View.VISIBLE);
                    btnToggleCalendarTab2.setText("🔽 Thu gọn lịch");
                    updateCustomCalendarView();
                }
            });
        }

        if (btnQuickPickCalendar != null) {
            btnQuickPickCalendar.setOnClickListener(v -> showDatePickerForTab2());
        }
        if (tvTab2SelectedDateTitle != null) {
            tvTab2SelectedDateTitle.setOnClickListener(v -> showDatePickerForTab2());
        }

        sharedEventViewPool.setMaxRecycledViews(0, 25);

        TextView btnTab2ModeDay = findViewById(R.id.btnTab2ModeDay);
        TextView btnTab2ModeWeek = findViewById(R.id.btnTab2ModeWeek);
        TextView btnTab2ModeMonth = findViewById(R.id.btnTab2ModeMonth);
        TextView btnTab2ModeFree = findViewById(R.id.btnTab2ModeFree);

        if (btnTab2ModeDay != null) btnTab2ModeDay.setOnClickListener(v -> applyTab2CalendarMode(0));
        if (btnTab2ModeWeek != null) btnTab2ModeWeek.setOnClickListener(v -> applyTab2CalendarMode(1));
        if (btnTab2ModeMonth != null) btnTab2ModeMonth.setOnClickListener(v -> applyTab2CalendarMode(2));
        if (btnTab2ModeFree != null) btnTab2ModeFree.setOnClickListener(v -> applyTab2CalendarMode(3));

        TextView btnTab2OpenTimeline = findViewById(R.id.btnTab2OpenTimeline);
        TextView btnTab2SmartFinder = findViewById(R.id.btnTab2SmartFinder);
        TextView btnTab2VoiceNote = findViewById(R.id.btnTab2VoiceNote);

        if (btnTab2OpenTimeline != null) btnTab2OpenTimeline.setOnClickListener(v -> showContinuousTimeline(selectedTab2DateIso));
        if (btnTab2SmartFinder != null) btnTab2SmartFinder.setOnClickListener(v -> showSmartSlotFinderDialog());
        if (btnTab2VoiceNote != null) btnTab2VoiceNote.setOnClickListener(v -> launchSpeechRecognizer(null));

        if (rvTab2Events != null) {
            LinearLayoutManager tab2LayoutManager = new LinearLayoutManager(this);
            tab2LayoutManager.setInitialPrefetchItemCount(6);
            rvTab2Events.setLayoutManager(tab2LayoutManager);
            rvTab2Events.setHasFixedSize(true);
            rvTab2Events.setItemViewCacheSize(15);
            rvTab2Events.setRecycledViewPool(sharedEventViewPool);
            rvTab2Events.setItemAnimator(null);
            tab2Adapter = new EventAdapter(new ArrayList<>(), new EventAdapter.OnEventClickListener() {
                @Override public void onDeleteClick(EventModel event) {
                    showCustomConfirmDialog(
                            "🗑️",
                            "XÓA LỊCH CÔNG VIỆC",
                            "Bạn có chắc chắn muốn xóa lịch '" + event.getTitle() + "'?",
                            "🔴 Xóa lịch này",
                            Color.parseColor("#DC2626"),
                            () -> {
                                dbHelper.deleteEvent(event.getId());
                                playSuccessSound();
                                refreshAllTabs();
                                Toast.makeText(MainActivity.this, "Đã xóa lịch thành công!", Toast.LENGTH_SHORT).show();
                            });
                }
                @Override public void onEditClick(EventModel event) { showEditEventDialog(event); }
                @Override public void onSelectionChanged(int selectedCount) {
                    TextView tvCount = findViewById(R.id.tvSelectedCount);
                    if (tvCount != null) {
                        tvCount.setText("Đã chọn: " + selectedCount + " lịch");
                    }
                    View multiBar = findViewById(R.id.layoutMultiSelectBar);
                    if (multiBar != null && tab2Adapter != null) {
                        multiBar.setVisibility(tab2Adapter.isMultiSelectMode() ? View.VISIBLE : View.GONE);
                    }
                }
            });
            rvTab2Events.setAdapter(tab2Adapter);
        }

        TextView btnEnableMultiSelect = findViewById(R.id.btnEnableMultiSelect);
        TextView btnTab2EnableMultiSelect = findViewById(R.id.btnTab2EnableMultiSelect);
        MaterialCardView layoutMultiSelectBar = findViewById(R.id.layoutMultiSelectBar);
        TextView tvSelectedCount = findViewById(R.id.tvSelectedCount);
        Button btnSelectAll = findViewById(R.id.btnSelectAll);
        Button btnBatchDelete = findViewById(R.id.btnBatchDelete);
        Button btnCancelMultiSelect = findViewById(R.id.btnCancelMultiSelect);

        TextView btnFileEnableMultiSelect = findViewById(R.id.btnFileEnableMultiSelect);
        MaterialCardView layoutFileMultiSelectBar = findViewById(R.id.layoutFileMultiSelectBar);
        TextView tvFileSelectedCount = findViewById(R.id.tvFileSelectedCount);
        Button btnFileSelectAll = findViewById(R.id.btnFileSelectAll);
        Button btnFileBatchDelete = findViewById(R.id.btnFileBatchDelete);
        Button btnFileCancelMultiSelect = findViewById(R.id.btnFileCancelMultiSelect);

        TextView btnManageFileEvents = findViewById(R.id.btnManageFileEvents);
        TextView btnClearAllHistory = findViewById(R.id.btnClearAllHistory);
        Button btnTestAlarmTab = findViewById(R.id.btnTestAlarmTab);

        Button btnImportAnyFileTab = findViewById(R.id.btnImportAnyFileTab);
        Button btnImportExcelTab = findViewById(R.id.btnImportExcelTab);
        Button btnImportPdfTab = findViewById(R.id.btnImportPdfTab);
        Button btnImportOcrTab = findViewById(R.id.btnImportOcrTab);

        Button btnExportExcelTab = findViewById(R.id.btnExportExcelTab);
        Button btnExportPdfTab = findViewById(R.id.btnExportPdfTab);
        Button btnCleanDataNow = findViewById(R.id.btnCleanDataNow);

        View cardAppIntro = findViewById(R.id.cardAppIntro);
        Button btnOpenDetailedAppIntro = findViewById(R.id.btnOpenDetailedAppIntro);
        Button btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        
        View.OnClickListener appIntroListener = v -> {
            SoundHelper.playFeedback(MainActivity.this, v);
            showAppIntroductionDialog();
        };
        if (cardAppIntro != null) cardAppIntro.setOnClickListener(appIntroListener);
        if (btnOpenDetailedAppIntro != null) btnOpenDetailedAppIntro.setOnClickListener(appIntroListener);
        if (btnCheckUpdate != null) btnCheckUpdate.setOnClickListener(v -> VersionCheckHelper.checkForUpdatesManual(this));

        TextView tvFooterVersion = findViewById(R.id.tvFooterVersion);
        if (tvFooterVersion != null) {
            tvFooterVersion.setText("LectuPlan v" + VersionCheckHelper.getCurrentVersionName(this) + " • Ứng dụng Quản lý Lịch giảng dạy");
        }

        fabScrollToTop = findViewById(R.id.fabScrollToTop);

        if (fabAddEvent != null) {
            fabAddEvent.setOnClickListener(v -> {
                SoundHelper.playFeedback(MainActivity.this, v);
                showAddEventDialog();
            });
        }

        // Xử lý sự kiện nhấn nút cuộn lên đầu trang (Scroll to Top)
        if (fabScrollToTop != null) {
            fabScrollToTop.setOnClickListener(v -> {
                SoundHelper.playFeedback(MainActivity.this, v);
                if (currentActiveTabIndex == 0) {
                    if (rvEvents != null) rvEvents.smoothScrollToPosition(0);
                } else if (currentActiveTabIndex == 1) {
                    if (rvTab2Events != null) rvTab2Events.smoothScrollToPosition(0);
                } else if (currentActiveTabIndex == 2) {
                    NestedScrollView sv = findViewById(R.id.layoutChartTab);
                    if (sv != null) sv.smoothScrollTo(0, 0);
                } else if (currentActiveTabIndex == 3) {
                    NestedScrollView sv = findViewById(R.id.layoutReportTab);
                    if (sv != null) sv.smoothScrollTo(0, 0);
                } else if (currentActiveTabIndex == 4) {
                    NestedScrollView sv = findViewById(R.id.layoutSettingsTab);
                    if (sv != null) sv.smoothScrollTo(0, 0);
                }
                fabScrollToTop.hide();
            });
        }

        // Lắng nghe sự kiện cuộn danh sách Tab 1 (Lịch trình) để hiện/ẩn nút lên đầu trang
        if (rvEvents != null) {
            rvEvents.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (currentActiveTabIndex != 0) return;
                    RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
                    if (lm instanceof LinearLayoutManager) {
                        int firstPos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                        if (firstPos > 1 || dy > 10) {
                            if (fabScrollToTop != null && fabScrollToTop.getVisibility() != View.VISIBLE) {
                                fabScrollToTop.show();
                            }
                        } else if (firstPos == 0) {
                            if (fabScrollToTop != null && fabScrollToTop.getVisibility() == View.VISIBLE) {
                                fabScrollToTop.hide();
                            }
                        }
                    }
                }
            });
        }

        // Lắng nghe sự kiện cuộn danh sách Tab 2 (Lịch biểu) để hiện/ẩn nút lên đầu trang
        if (rvTab2Events != null) {
            rvTab2Events.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (currentActiveTabIndex != 1) return;
                    RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
                    if (lm instanceof LinearLayoutManager) {
                        int firstPos = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
                        if (firstPos > 1 || dy > 10) {
                            if (fabScrollToTop != null && fabScrollToTop.getVisibility() != View.VISIBLE) {
                                fabScrollToTop.show();
                            }
                        } else if (firstPos == 0) {
                            if (fabScrollToTop != null && fabScrollToTop.getVisibility() == View.VISIBLE) {
                                fabScrollToTop.hide();
                            }
                        }
                    }
                }
            });
        }

        // Lắng nghe sự kiện cuộn cho các tab lướt dọc (Biểu đồ, Báo cáo, Cài đặt)
        NestedScrollView.OnScrollChangeListener nestedScrollListener = (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > 250) {
                if (fabScrollToTop != null && fabScrollToTop.getVisibility() != View.VISIBLE) {
                    fabScrollToTop.show();
                }
            } else if (scrollY < 100) {
                if (fabScrollToTop != null && fabScrollToTop.getVisibility() == View.VISIBLE) {
                    fabScrollToTop.hide();
                }
            }
        };
        NestedScrollView svChart = findViewById(R.id.layoutChartTab);
        if (svChart != null) svChart.setOnScrollChangeListener(nestedScrollListener);
        NestedScrollView svReport = findViewById(R.id.layoutReportTab);
        if (svReport != null) svReport.setOnScrollChangeListener(nestedScrollListener);
        NestedScrollView svSettings = findViewById(R.id.layoutSettingsTab);
        if (svSettings != null) svSettings.setOnScrollChangeListener(nestedScrollListener);

        if (btnTestAlarmTab != null) {
            btnTestAlarmTab.setOnClickListener(v -> {
                if (AlarmReceiver.isAlarmRinging()) {
                    AlarmReceiver.stopAlarmSound();
                    Toast.makeText(this, "Đã tắt chuông báo thức!", Toast.LENGTH_SHORT).show();
                } else {
                    int hour = AlarmScheduler.getAlarmHour(this);
                    int minute = AlarmScheduler.getAlarmMinute(this);
                    String timeStr = AlarmScheduler.getFormattedTime(hour, minute);
                    AlarmReceiver.triggerSoundAndNotification(this,
                            "⏰ Báo lịch Giảng viên (" + timeStr + ")",
                            "Thầy/Cô có lịch sự kiện sắp diễn ra! Hãy kiểm tra ứng dụng LectuPlan.");
                    showStopAlarmDialog();
                }
            });
        }

        if (btnImportAnyFileTab != null) btnImportAnyFileTab.setOnClickListener(v -> universalFilePickerLauncher.launch("*/*"));
        if (btnImportExcelTab != null) btnImportExcelTab.setOnClickListener(v -> excelPickerLauncher.launch("*/*"));
        if (btnImportPdfTab != null) btnImportPdfTab.setOnClickListener(v -> pdfPickerLauncher.launch("application/pdf"));
        if (btnImportOcrTab != null) btnImportOcrTab.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        if (btnExportExcelTab != null) btnExportExcelTab.setOnClickListener(v -> startExportExcelProcess());
        if (btnExportPdfTab != null) btnExportPdfTab.setOnClickListener(v -> exportPdfReportProcess());

        setupCustomBottomNav();
        setupThemeSettings();
        setupFontSizeSettings();
        setupAlarmSettings();
        setupAutoDeleteSettings();
        setupSoundSettings();
        setupFilterChips();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    Color.parseColor("#2563EB"),
                    Color.parseColor("#10B981"),
                    Color.parseColor("#8B5CF6")
            );
            int startOffset = (int) (40 * getResources().getDisplayMetrics().density);
            int endOffset = (int) (110 * getResources().getDisplayMetrics().density);
            swipeRefreshLayout.setProgressViewOffset(false, startOffset, endOffset);

            swipeRefreshLayout.setOnRefreshListener(() -> {
                dbHelper.autoDeleteOldData();
                refreshData();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Đã làm mới dữ liệu!", Toast.LENGTH_SHORT).show();
                }, 800);
            });
        }

        LinearLayoutManager tab1LayoutManager = new LinearLayoutManager(this);
        tab1LayoutManager.setInitialPrefetchItemCount(6);
        rvEvents.setLayoutManager(tab1LayoutManager);
        rvEvents.setHasFixedSize(true);
        rvEvents.setItemViewCacheSize(15);
        rvEvents.setRecycledViewPool(sharedEventViewPool);
        rvEvents.setItemAnimator(null);
        adapter = new EventAdapter(new ArrayList<>(), new EventAdapter.OnEventClickListener() {
            @Override
            public void onDeleteClick(EventModel event) {
                showCustomConfirmDialog(
                        "🗑️",
                        "XÓA LỊCH CÔNG VIỆC",
                        "Bạn có chắc chắn muốn xóa lịch '" + event.getTitle() + "'?",
                        "🔴 Xóa lịch này",
                        Color.parseColor("#DC2626"),
                        () -> {
                            dbHelper.deleteEvent(event.getId());
                            playSuccessSound();
                            refreshAllTabs();
                            Toast.makeText(MainActivity.this, "Đã xóa lịch thành công!", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onEditClick(EventModel event) {
                showEditEventDialog(event);
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                if (tvSelectedCount != null) {
                    tvSelectedCount.setText("Đã chọn: " + selectedCount + " lịch");
                }
                if (layoutMultiSelectBar != null) {
                    layoutMultiSelectBar.setVisibility(adapter.isMultiSelectMode() ? View.VISIBLE : View.GONE);
                }
                if (fabAddEvent != null) {
                    if (adapter.isMultiSelectMode()) fabAddEvent.hide();
                    else fabAddEvent.show();
                }
            }
        });
        rvEvents.setAdapter(adapter);

        if (btnEnableMultiSelect != null) {
            btnEnableMultiSelect.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.setMultiSelectMode(true);
                    if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.VISIBLE);
                    if (fabAddEvent != null) fabAddEvent.hide();
                }
            });
        }

        if (btnTab2EnableMultiSelect != null) {
            btnTab2EnableMultiSelect.setOnClickListener(v -> {
                if (tab2Adapter != null) {
                    tab2Adapter.setMultiSelectMode(true);
                    if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.VISIBLE);
                }
            });
        }

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                EventAdapter targetAdapter = (currentActiveTabIndex == 1) ? tab2Adapter : adapter;
                if (targetAdapter != null) targetAdapter.selectAll();
            });
        }

        if (btnCancelMultiSelect != null) {
            btnCancelMultiSelect.setOnClickListener(v -> {
                if (adapter != null) adapter.setMultiSelectMode(false);
                if (tab2Adapter != null) tab2Adapter.setMultiSelectMode(false);
                if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.GONE);
                if (fabAddEvent != null && currentActiveTabIndex == 0) fabAddEvent.show();
            });
        }

        if (btnBatchDelete != null) {
            btnBatchDelete.setOnClickListener(v -> {
                EventAdapter targetAdapter = (currentActiveTabIndex == 1) ? tab2Adapter : adapter;
                if (targetAdapter == null) return;
                List<Integer> ids = targetAdapter.getSelectedEventIds();
                if (ids.isEmpty()) {
                    Toast.makeText(this, "Vui lòng tích chọn ít nhất 1 lịch để xóa!", Toast.LENGTH_SHORT).show();
                    return;
                }

                showCustomConfirmDialog(
                        "🗑️",
                        "XÓA HÀNG LOẠT LỊCH",
                        "Bạn có chắc chắn muốn XÓA HÀNG LOẠT " + ids.size() + " lịch biểu đã chọn?",
                        "🔴 Xóa " + ids.size() + " lịch",
                        Color.parseColor("#DC2626"),
                        () -> {
                            dbHelper.deleteMultipleEvents(ids);
                            playSuccessSound();
                            if (adapter != null) adapter.setMultiSelectMode(false);
                            if (tab2Adapter != null) tab2Adapter.setMultiSelectMode(false);
                            if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.GONE);
                            if (fabAddEvent != null && currentActiveTabIndex == 0) fabAddEvent.show();
                            refreshData();
                            refreshTab2CalendarEvents();
                            updateChartTab();
                            Toast.makeText(MainActivity.this, "Đã xóa thành công " + ids.size() + " lịch!", Toast.LENGTH_SHORT).show();
                        });
            });
        }

        if (rvFileHistory != null) {
            rvFileHistory.setLayoutManager(new LinearLayoutManager(this));
            fileHistoryAdapter = new FileHistoryAdapter(new ArrayList<>(), new FileHistoryAdapter.OnHistoryClickListener() {
                @Override
                public void onItemClick(FileHistoryModel item) {
                    openExcelFile(item.getFileName());
                }

                @Override
                public void onViewContentClick(FileHistoryModel item) {
                    viewHistoryFileContentInApp(item);
                }

                @Override
                public void onDownloadClick(FileHistoryModel item) {
                    downloadHistoryFileToDevice(item.getFileName());
                }

                @Override
                public void onDeleteClick(FileHistoryModel item) {
                    showDeleteFileAndEventsDialog(item);
                }

                @Override
                public void onSelectionChanged(int selectedCount) {
                    if (tvFileSelectedCount != null) {
                        tvFileSelectedCount.setText("Đã chọn: " + selectedCount + " tệp");
                    }
                    if (layoutFileMultiSelectBar != null) {
                        layoutFileMultiSelectBar.setVisibility(fileHistoryAdapter != null && fileHistoryAdapter.isMultiSelectMode() ? View.VISIBLE : View.GONE);
                    }
                }
            });
            rvFileHistory.setAdapter(fileHistoryAdapter);
            refreshFileHistory();
        }

        if (btnFileEnableMultiSelect != null) {
            btnFileEnableMultiSelect.setOnClickListener(v -> {
                if (fileHistoryAdapter != null) {
                    if (fileHistoryAdapter.getItemCount() == 0) {
                        Toast.makeText(this, "Chưa có tệp nào trong lịch sử để chọn!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fileHistoryAdapter.setMultiSelectMode(true);
                    if (layoutFileMultiSelectBar != null) layoutFileMultiSelectBar.setVisibility(View.VISIBLE);
                    if (tvFileSelectedCount != null) tvFileSelectedCount.setText("Đã chọn: 0 tệp");
                }
            });
        }

        if (btnFileSelectAll != null) {
            btnFileSelectAll.setOnClickListener(v -> {
                if (fileHistoryAdapter != null) fileHistoryAdapter.selectAll();
            });
        }

        if (btnFileCancelMultiSelect != null) {
            btnFileCancelMultiSelect.setOnClickListener(v -> {
                if (fileHistoryAdapter != null) {
                    fileHistoryAdapter.setMultiSelectMode(false);
                    if (layoutFileMultiSelectBar != null) layoutFileMultiSelectBar.setVisibility(View.GONE);
                }
            });
        }

        if (btnFileBatchDelete != null) {
            btnFileBatchDelete.setOnClickListener(v -> handleBatchDeleteFileHistory());
        }

        if (btnManageFileEvents != null) {
            btnManageFileEvents.setOnClickListener(v -> showManageImportedFilesDialog());
        }

        if (btnClearAllHistory != null) {
            btnClearAllHistory.setOnClickListener(v -> showClearAllFilesAndEventsDialog());
        }

        setupSpinners();

        if (tvTotalCount != null) {
            tvTotalCount.setOnClickListener(v -> {
                isFilteringToday = !isFilteringToday;
                if (isFilteringToday) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String todayStr = sdf.format(Calendar.getInstance().getTime());
                    edtSearch.setText(todayStr);
                    Toast.makeText(this, "📌 Đang lọc danh sách lịch biểu cho HÔM NAY!", Toast.LENGTH_SHORT).show();
                } else {
                    edtSearch.setText("");
                    Toast.makeText(this, "🔄 Đã hiển thị toàn bộ lịch biểu!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnClearSearch = findViewById(R.id.btnClearSearch);
        if (btnClearSearch != null && edtSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                edtSearch.setText("");
            });
        }

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                refreshData();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dbHelper.deleteSampleEvents();

        refreshData();
        refreshTab2CalendarEvents();
        checkVoiceMemoIntent(getIntent());

        // 🌟 Tự động kiểm tra và mở Dialog "Cập nhật thành công - Có gì mới?" nếu người dùng vừa lên bản mới v1.1.0
        VersionCheckHelper.checkAndShowWhatsNew(this);
    }

    private void applyTab2CalendarMode(int mode) {
        if (currentTab2Mode == mode && !isTab2Dirty) return;
        currentTab2Mode = mode;
        isTab2Dirty = true;

        TextView btnTab2ModeDay = findViewById(R.id.btnTab2ModeDay);
        TextView btnTab2ModeWeek = findViewById(R.id.btnTab2ModeWeek);
        TextView btnTab2ModeMonth = findViewById(R.id.btnTab2ModeMonth);
        TextView btnTab2ModeFree = findViewById(R.id.btnTab2ModeFree);

        TextView[] modeButtons = new TextView[]{btnTab2ModeDay, btnTab2ModeWeek, btnTab2ModeMonth, btnTab2ModeFree};

        for (int k = 0; k < modeButtons.length; k++) {
            if (modeButtons[k] != null) {
                if (k == mode) {
                    modeButtons[k].setBackgroundResource(R.drawable.bg_chip_selected);
                    modeButtons[k].setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    modeButtons[k].setBackground(null);
                    modeButtons[k].setTextColor(k == 3 ? Color.parseColor("#B45309") : Color.parseColor("#64748B"));
                }
            }
        }

        refreshTab2CalendarEvents();
    }

    private void refreshTab2CalendarEvents() {
        if (tab2Adapter == null || dbHelper == null) return;
        isTab2Dirty = false;

        List<EventModel> displayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        if (selectedTab2DateIso != null && !selectedTab2DateIso.trim().isEmpty()) {
            try {
                Date d = sdf.parse(selectedTab2DateIso.trim());
                if (d != null) cal.setTime(d);
            } catch (Exception ignored) {}
        }

        if (currentTab2Mode == 0) {
            // 1. Chế độ NGÀY: CHỈ hiển thị các công việc diễn ra trong đúng ngày được chọn
            String dateIso = sdf.format(cal.getTime());
            List<EventModel> dayEvents = dbHelper.searchEvents(dateIso, "Tất cả HK", "Tất cả Năm");

            for (EventModel e : dayEvents) {
                if (e.getId() > 0) displayList.add(e);
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-100, 0, "☕ Ngày này chưa có lịch bận", dateIso, "Bạn đang có trọn vẹn cả ngày trống rảnh để nghỉ ngơi hoặc làm việc cá nhân.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }

        } else if (currentTab2Mode == 1) {
            // 2. Chế độ TUẦN: Tìm đúng Thứ 2 đầu tuần của ngày đang chọn (Thứ 2 đến Chủ nhật)
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int diffToMonday = (dayOfWeek == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dayOfWeek);
            cal.add(Calendar.DAY_OF_MONTH, diffToMonday);

            for (int i = 0; i < 7; i++) {
                String dateIso = sdf.format(cal.getTime());
                List<EventModel> dayEvents = dbHelper.searchEvents(dateIso, "Tất cả HK", "Tất cả Năm");
                for (EventModel e : dayEvents) {
                    if (e.getId() > 0) displayList.add(e);
                }
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-200, 0, "☕ Cả tuần này chưa có lịch bận", selectedTab2DateIso, "Tất cả các ngày trong tuần này đều chưa có lịch giảng dạy hay lịch họp.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }

        } else if (currentTab2Mode == 2) {
            // 3. Chế độ THÁNG: CHỈ hiển thị đúng các công việc trong tháng/năm của ngày đang chọn (yyyy-MM)
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String targetMonthIso = monthFormat.format(cal.getTime());

            List<EventModel> allEvents = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
            for (EventModel e : allEvents) {
                if (e.getDate() != null && e.getDate().startsWith(targetMonthIso) && e.getId() > 0) {
                    displayList.add(e);
                }
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-300, 0, "☕ Tháng " + targetMonthIso + " chưa có lịch bận", selectedTab2DateIso, "Chưa có công việc nào được ghi nhận trong tháng này.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }

        } else {
            // 4. Chế độ TRỐNG: Phân tích chi tiết từng khung giờ trống sáng & chiều của từng ngày trong tuần
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int diffToMonday = (dayOfWeek == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dayOfWeek);
            cal.add(Calendar.DAY_OF_MONTH, diffToMonday);

            for (int i = 0; i < 7; i++) {
                String dateIso = sdf.format(cal.getTime());
                List<EventModel> events = dbHelper.searchEvents(dateIso, "Tất cả HK", "Tất cả Năm");
                DayFreeTimeResult res = analyzeDayFreeTime(dateIso, events);

                displayList.add(new EventModel(
                        -400 - i,
                        i + 1,
                        res.statusBadge,
                        dateIso,
                        res.summaryText,
                        "HK1",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.WEEK_OF_YEAR)));

                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        tab2Adapter.updateList(displayList);

        if (customCalendarAdapter != null) {
            customCalendarAdapter.setSelectedDateIso(selectedTab2DateIso);
        }

        TextView tvTab2SelectedDateTitle = findViewById(R.id.tvTab2SelectedDateTitle);
        if (tvTab2SelectedDateTitle != null) {
            String modeName = "Ngày";
            if (currentTab2Mode == 1) modeName = "Tuần";
            else if (currentTab2Mode == 2) modeName = "Tháng";
            else if (currentTab2Mode == 3) modeName = "Khoảng trống";

            String formattedDate = selectedTab2DateIso;
            try {
                Date d = sdf.parse(selectedTab2DateIso);
                if (d != null) {
                    if (currentTab2Mode == 2) {
                        SimpleDateFormat outSdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                        formattedDate = outSdf.format(d);
                    } else {
                        SimpleDateFormat outSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        formattedDate = outSdf.format(d);
                    }
                }
            } catch (Exception ignored) {}

            tvTab2SelectedDateTitle.setText("📌 Xem theo " + modeName + ": " + formattedDate);
        }
    }

    private void updateCustomCalendarView() {
        if (currentCalMonthInstance == null || customCalendarAdapter == null) return;

        SimpleDateFormat monthTitleSdf = new SimpleDateFormat("MMMM 'năm' yyyy", new Locale("vi", "VN"));
        String monthTitle = monthTitleSdf.format(currentCalMonthInstance.getTime());
        if (monthTitle.length() > 0) {
            monthTitle = Character.toUpperCase(monthTitle.charAt(0)) + monthTitle.substring(1);
        }
        if (tvCalMonthTitle != null) {
            tvCalMonthTitle.setText(monthTitle);
        }

        // Tạo 42 ô ngày (6 tuần x 7 ngày) bắt đầu từ Thứ Hai
        Calendar cal = (Calendar) currentCalMonthInstance.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int currentMonth = cal.get(Calendar.MONTH);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday, ...
        int daysBefore = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : (firstDayOfWeek - Calendar.MONDAY);

        cal.add(Calendar.DAY_OF_YEAR, -daysBefore);

        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar todayCal = Calendar.getInstance();
        String todayIso = sdfIso.format(todayCal.getTime());

        List<CustomCalendarAdapter.DayCell> dayCells = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            Date d = cal.getTime();
            String cellDateIso = sdfIso.format(d);
            boolean isCurMonth = (cal.get(Calendar.MONTH) == currentMonth);
            boolean isToday = cellDateIso.equals(todayIso);
            int dayNumber = cal.get(Calendar.DAY_OF_MONTH);

            dayCells.add(new CustomCalendarAdapter.DayCell(d, dayNumber, isCurMonth, isToday, cellDateIso));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Truy vấn phân loại sự kiện theo ngày từ database: 1 = dạy/học, 2 = họp/thi, 3 = viết tay
        Map<String, Set<Integer>> eventTypesMap = new HashMap<>();
        List<EventModel> allEvents = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
        for (EventModel ev : allEvents) {
            if (ev.getDate() != null && !ev.getDate().trim().isEmpty() && ev.getId() > 0) {
                String dIso = ev.getDate().trim();
                Set<Integer> types = eventTypesMap.computeIfAbsent(dIso, k -> new HashSet<>());

                String cat = ev.getScheduleCategory();
                if (EventModel.CAT_TEACHING.equalsIgnoreCase(cat)) {
                    types.add(1); // Xanh: Dạy / Học
                } else if (EventModel.CAT_EVENT.equalsIgnoreCase(cat) || EventModel.CAT_INCIDENT.equalsIgnoreCase(cat)) {
                    types.add(2); // Cam: Sự kiện / Phát sinh
                } else {
                    types.add(3); // Tím: Lịch khác / Nhập tay
                }
            }
        }

        customCalendarAdapter.updateData(dayCells, selectedTab2DateIso, eventTypesMap);
    }

    private void navigateCalendarMonth(int offset) {
        if (currentCalMonthInstance == null) {
            currentCalMonthInstance = Calendar.getInstance();
        }
        currentCalMonthInstance.add(Calendar.MONTH, offset);

        int prevDay = 1;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedTab2DateIso);
            if (d != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                prevDay = c.get(Calendar.DAY_OF_MONTH);
            }
        } catch (Exception ignored) {}

        int maxDays = currentCalMonthInstance.getActualMaximum(Calendar.DAY_OF_MONTH);
        currentCalMonthInstance.set(Calendar.DAY_OF_MONTH, Math.min(prevDay, maxDays));
        selectedTab2DateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalMonthInstance.getTime());

        updateCustomCalendarView();
        isTab2Dirty = true;
        refreshTab2CalendarEvents();
    }

    private void showDatePickerForTab2() {
        Calendar cal = Calendar.getInstance();
        if (selectedTab2DateIso != null && !selectedTab2DateIso.trim().isEmpty()) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedTab2DateIso.trim());
                if (d != null) cal.setTime(d);
            } catch (Exception ignored) {}
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (picker, y, m, d) -> {
            cal.set(Calendar.YEAR, y);
            cal.set(Calendar.MONTH, m);
            cal.set(Calendar.DAY_OF_MONTH, d);
            currentCalMonthInstance.setTime(cal.getTime());
            selectedTab2DateIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            View cardCalendarWidget = findViewById(R.id.cardCalendarWidget);
            if (cardCalendarWidget != null && cardCalendarWidget.getVisibility() != View.VISIBLE) {
                cardCalendarWidget.setVisibility(View.VISIBLE);
                TextView btnToggleCalendarTab2 = findViewById(R.id.btnToggleCalendarTab2);
                if (btnToggleCalendarTab2 != null) btnToggleCalendarTab2.setText("🔽 Thu gọn lịch");
            }
            updateCustomCalendarView();
            isTab2Dirty = true;
            refreshTab2CalendarEvents();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void showMonthYearPickerDialog() {
        if (currentCalMonthInstance == null) {
            currentCalMonthInstance = Calendar.getInstance();
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_month_year_picker, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        android.widget.NumberPicker npMonth = dialogView.findViewById(R.id.npMonthPicker);
        android.widget.NumberPicker npYear = dialogView.findViewById(R.id.npYearPicker);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelMonthYear);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmMonthYear);

        int curMonth = currentCalMonthInstance.get(Calendar.MONTH); // 0 - 11
        int curYear = currentCalMonthInstance.get(Calendar.YEAR);

        String[] monthNames = new String[]{
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };

        if (npMonth != null) {
            npMonth.setDisplayedValues(null);
            npMonth.setMinValue(0);
            npMonth.setMaxValue(11);
            npMonth.setDisplayedValues(monthNames);
            npMonth.setValue(curMonth);
            npMonth.setWrapSelectorWheel(true);
        }

        if (npYear != null) {
            npYear.setMinValue(2020);
            npYear.setMaxValue(2035);
            npYear.setValue(curYear);
            npYear.setWrapSelectorWheel(false);
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (npMonth != null) npMonth.clearFocus();
                if (npYear != null) npYear.clearFocus();
                int selectedM = npMonth != null ? npMonth.getValue() : curMonth;
                int selectedY = npYear != null ? npYear.getValue() : curYear;

                int prevDay = currentCalMonthInstance.get(Calendar.DAY_OF_MONTH);
                currentCalMonthInstance.set(Calendar.YEAR, selectedY);
                currentCalMonthInstance.set(Calendar.MONTH, selectedM);
                int maxDays = currentCalMonthInstance.getActualMaximum(Calendar.DAY_OF_MONTH);
                currentCalMonthInstance.set(Calendar.DAY_OF_MONTH, Math.min(prevDay, maxDays));

                SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedTab2DateIso = sdfIso.format(currentCalMonthInstance.getTime());

                updateCustomCalendarView();
                isTab2Dirty = true;
                refreshTab2CalendarEvents();

                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void applyCalendarMode(int mode) {
        currentCalendarMode = mode;

        TextView btnModeDay = findViewById(R.id.btnModeDay);
        TextView btnModeWeek = findViewById(R.id.btnModeWeek);
        TextView btnModeMonth = findViewById(R.id.btnModeMonth);
        TextView btnModeFree = findViewById(R.id.btnModeFree);

        TextView[] modeButtons = new TextView[]{btnModeDay, btnModeWeek, btnModeMonth, btnModeFree};

        for (int k = 0; k < modeButtons.length; k++) {
            if (modeButtons[k] != null) {
                if (k == mode) {
                    modeButtons[k].setBackgroundResource(R.drawable.bg_chip_selected);
                    modeButtons[k].setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    modeButtons[k].setBackground(null);
                    modeButtons[k].setTextColor(k == 3 ? Color.parseColor("#B45309") : Color.parseColor("#64748B"));
                }
            }
        }

        refreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AlarmReceiver.isAlarmRinging()) {
            showStopAlarmDialog();
        }
    }

    private void showStopAlarmDialog() {
        showCustomConfirmDialog(
                "🔔",
                "BÁO THỨC LỊCH GIẢNG VIÊN",
                "Chuông báo lịch công việc đang phát! Bạn có muốn tắt chuông ngay bây giờ?",
                "🔕 TẮT CHUÔNG",
                Color.parseColor("#DC2626"),
                () -> {
                    AlarmReceiver.stopAlarmSound();
                    Toast.makeText(this, "Đã tắt chuông báo thức!", Toast.LENGTH_SHORT).show();
                });
    }

    // Cấu trúc phân tích khung giờ bận & tính toán khoảng giờ trống chính xác
    private static class TimeSlotInterval {
        int startMin;
        int endMin;
        String label;
        TimeSlotInterval(int startMin, int endMin, String label) {
            this.startMin = startMin;
            this.endMin = endMin;
            this.label = label;
        }
    }

    public static class DayFreeTimeResult {
        public String dateIso;
        public String dayTitle;
        public String statusBadge;
        public String statusColor;
        public String statusBgColor;
        public String morningDetail;
        public String afternoonDetail;
        public String summaryText;
    }

    private String formatMinutesToHourString(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    private List<TimeSlotInterval> extractBusyIntervalsFromEvent(EventModel event) {
        List<TimeSlotInterval> intervals = new ArrayList<>();
        if (event == null || event.getId() <= 0) return intervals;

        String rawTitle = event.getTitle() != null ? event.getTitle().trim() : "";
        String cleanTitle = rawTitle
                .replaceAll("(?i)\\|.*", "")
                .replaceAll("(?i)-\\s*phòng.*", "")
                .replaceAll("(?i)phòng\\s*[:=]?\\s*[A-Za-z0-9._/-]+", "")
                .replaceAll("\\[[^\\]]*\\]", "")
                .replaceAll("\\([^)]*\\)", "")
                .trim();
        if (cleanTitle.isEmpty()) cleanTitle = rawTitle;

        int startMin = event.getStartMin();
        int endMin = event.getEndMin();

        if (startMin > 0 && endMin > startMin) {
            intervals.add(new TimeSlotInterval(startMin, endMin, cleanTitle));
        } else {
            intervals.add(new TimeSlotInterval(7 * 60 + 30, 11 * 60 + 35, cleanTitle));
        }

        return intervals;
    }

    private int getStartMinuteOfPeriod(int period) {
        switch (period) {
            case 1: return 7 * 60; // 07h00
            case 2: return 7 * 60 + 50; // 07h50
            case 3: return 8 * 60 + 40; // 08h40
            case 4: return 9 * 60 + 40; // 09h40
            case 5: return 10 * 60 + 30; // 10h30
            case 6: return 11 * 60 + 20; // 11h20
            case 7: return 13 * 60; // 13h00
            case 8: return 13 * 60 + 50; // 13h50
            case 9: return 14 * 60 + 40; // 14h40
            case 10: return 15 * 60 + 30; // 15h30
            case 11: return 16 * 60 + 20; // 16h20
            case 12: return 17 * 60 + 10; // 17h10
            case 13: return 18 * 60; // 18h00
            case 14: return 18 * 60 + 50; // 18h50
            case 15: return 19 * 60 + 40; // 19h40
            default:
                if (period <= 6) return (6 + period) * 60;
                else return (6 + period) * 60;
        }
    }

    private int getEndMinuteOfPeriod(int period) {
        switch (period) {
            case 1: return 7 * 60 + 45;
            case 2: return 8 * 60 + 35;
            case 3: return 9 * 60 + 30;
            case 4: return 10 * 60 + 25;
            case 5: return 11 * 60 + 15;
            case 6: return 12 * 60 + 05;
            case 7: return 13 * 60 + 45;
            case 8: return 14 * 60 + 35;
            case 9: return 15 * 60 + 25;
            case 10: return 16 * 60 + 15;
            case 11: return 17 * 60 + 05;
            case 12: return 17 * 60 + 55;
            case 13: return 18 * 60 + 45;
            case 14: return 19 * 60 + 35;
            case 15: return 20 * 60 + 25;
            default:
                return (7 + period) * 60;
        }
    }

    private List<String> calculateFreeIntervals(List<TimeSlotInterval> busyIntervals, int sessionStartMin, int sessionEndMin) {
        List<String> freeSlots = new ArrayList<>();
        if (busyIntervals == null || busyIntervals.isEmpty()) {
            freeSlots.add(formatMinutesToHourString(sessionStartMin) + " - " + formatMinutesToHourString(sessionEndMin));
            return freeSlots;
        }

        // Gộp các khoảng bận trùng hoặc giao nhau
        busyIntervals.sort((a, b) -> Integer.compare(a.startMin, b.startMin));
        List<TimeSlotInterval> mergedBusy = new ArrayList<>();
        TimeSlotInterval curr = busyIntervals.get(0);
        for (int i = 1; i < busyIntervals.size(); i++) {
            TimeSlotInterval next = busyIntervals.get(i);
            if (next.startMin <= curr.endMin) {
                curr.endMin = Math.max(curr.endMin, next.endMin);
            } else {
                mergedBusy.add(curr);
                curr = next;
            }
        }
        mergedBusy.add(curr);

        // Quét tìm khoảng trống trong session
        int pointer = sessionStartMin;
        for (TimeSlotInterval b : mergedBusy) {
            int bStart = Math.max(sessionStartMin, b.startMin);
            int bEnd = Math.min(sessionEndMin, b.endMin);

            if (bStart > pointer && (bStart - pointer) >= 30) { // Trống ít nhất 30 phút
                freeSlots.add(formatMinutesToHourString(pointer) + " - " + formatMinutesToHourString(bStart));
            }
            pointer = Math.max(pointer, bEnd);
        }

        if (sessionEndMin > pointer && (sessionEndMin - pointer) >= 30) {
            freeSlots.add(formatMinutesToHourString(pointer) + " - " + formatMinutesToHourString(sessionEndMin));
        }

        return freeSlots;
    }

    // Phân tích chính xác thời gian rảnh và bận trong 1 ngày cụ thể
    private DayFreeTimeResult analyzeDayFreeTime(String dateIso, List<EventModel> dayEvents) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE (dd/MM/yyyy)", new Locale("vi", "VN"));
        String dayTitle = dateIso;
        try {
            Date d = sdf.parse(dateIso);
            if (d != null) dayTitle = dayFormat.format(d);
        } catch (Exception ignored) {}

        List<TimeSlotInterval> morningBusyList = new ArrayList<>();
        List<TimeSlotInterval> afternoonBusyList = new ArrayList<>();
        List<String> morningBusyDesc = new ArrayList<>();
        List<String> afternoonBusyDesc = new ArrayList<>();

        if (dayEvents != null) {
            for (EventModel e : dayEvents) {
                if (e.getId() <= 0) continue;
                List<TimeSlotInterval> intervals = extractBusyIntervalsFromEvent(e);
                for (TimeSlotInterval interval : intervals) {
                    String timeRangeStr = formatMinutesToHourString(interval.startMin) + "-" + formatMinutesToHourString(interval.endMin);
                    String desc = timeRangeStr;
                    if (interval.label != null && !interval.label.isEmpty() && !interval.label.equalsIgnoreCase(timeRangeStr)) {
                        desc += " (" + interval.label + ")";
                    }
                    
                    if (interval.startMin < 12 * 60 + 30) {
                        morningBusyList.add(interval);
                        if (!morningBusyDesc.contains(desc)) morningBusyDesc.add(desc);
                    }
                    if (interval.endMin > 12 * 60) {
                        afternoonBusyList.add(interval);
                        if (!afternoonBusyDesc.contains(desc)) afternoonBusyDesc.add(desc);
                    }
                }
            }
        }

        // Buổi Sáng chuẩn: 07h00 - 11h30
        List<String> morningFreeSlots = calculateFreeIntervals(morningBusyList, 7 * 60, 11 * 60 + 30);
        // Buổi Chiều chuẩn: 13h00 - 17h00
        List<String> afternoonFreeSlots = calculateFreeIntervals(afternoonBusyList, 13 * 60, 17 * 60);

        DayFreeTimeResult res = new DayFreeTimeResult();
        res.dateIso = dateIso;
        res.dayTitle = dayTitle;

        StringBuilder morningSb = new StringBuilder();
        if (morningFreeSlots.isEmpty()) {
            morningSb.append("🔴 Kín lịch cả buổi sáng (07:00 - 11:30)");
        } else if (morningFreeSlots.size() == 1 && (morningFreeSlots.get(0).contains("07h00 - 11h30") || morningFreeSlots.get(0).contains("07:00 - 11:30"))) {
            morningSb.append("🟢 Trống cả buổi sáng (07:00 - 11:30)");
        } else {
            morningSb.append("🟢 Còn trống: ").append(String.join(", ", morningFreeSlots));
            if (!morningBusyDesc.isEmpty()) {
                morningSb.append("\n🔴 Lịch bận:");
                for (String b : morningBusyDesc) {
                    morningSb.append("\n  • ").append(b);
                }
            }
        }
        res.morningDetail = morningSb.toString();

        StringBuilder afternoonSb = new StringBuilder();
        if (afternoonFreeSlots.isEmpty()) {
            afternoonSb.append("🔴 Kín lịch cả buổi chiều (13:00 - 17:00)");
        } else if (afternoonFreeSlots.size() == 1 && (afternoonFreeSlots.get(0).contains("13h00 - 17h00") || afternoonFreeSlots.get(0).contains("13:00 - 17:00"))) {
            afternoonSb.append("🟢 Trống cả buổi chiều (13:00 - 17:00)");
        } else {
            afternoonSb.append("🟢 Còn trống: ").append(String.join(", ", afternoonFreeSlots));
            if (!afternoonBusyDesc.isEmpty()) {
                afternoonSb.append("\n🔴 Lịch bận:");
                for (String b : afternoonBusyDesc) {
                    afternoonSb.append("\n  • ").append(b);
                }
            }
        }
        res.afternoonDetail = afternoonSb.toString();

        boolean isMorningFreeAll = morningFreeSlots.size() == 1 && (morningFreeSlots.get(0).contains("07h00 - 11h30") || morningFreeSlots.get(0).contains("07:00 - 11:30"));
        boolean isAfternoonFreeAll = afternoonFreeSlots.size() == 1 && (afternoonFreeSlots.get(0).contains("13h00 - 17h00") || afternoonFreeSlots.get(0).contains("13:00 - 17:00"));

        if (isMorningFreeAll && isAfternoonFreeAll) {
            res.statusBadge = "🟢 TRỐNG CẢ NGÀY";
            res.statusColor = "#15803D";
            res.statusBgColor = "#DCFCE7";
        } else if (morningFreeSlots.isEmpty() && afternoonFreeSlots.isEmpty()) {
            res.statusBadge = "🔴 KÍN LỊCH CẢ NGÀY";
            res.statusColor = "#B91C1C";
            res.statusBgColor = "#FEE2E2";
        } else if (!morningFreeSlots.isEmpty() && afternoonFreeSlots.isEmpty()) {
            res.statusBadge = "🟢 CÒN TRỐNG SÁNG";
            res.statusColor = "#15803D";
            res.statusBgColor = "#DCFCE7";
        } else if (morningFreeSlots.isEmpty() && !afternoonFreeSlots.isEmpty()) {
            res.statusBadge = "🟢 CÒN TRỐNG CHIỀU";
            res.statusColor = "#15803D";
            res.statusBgColor = "#DCFCE7";
        } else {
            res.statusBadge = "🟡 TRỐNG TỪNG KHUNG GIỜ";
            res.statusColor = "#B45309";
            res.statusBgColor = "#FEF3C7";
        }

        res.summaryText = "☀️ Buổi Sáng:\n   " + res.morningDetail + "\n\n⛅ Buổi Chiều:\n   " + res.afternoonDetail;
        return res;
    }

    private void showContinuousTimeline(String targetDateIso) {
        playSuccessSound();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_continuous_timeline, null);
        TextView tvTimelineDayTitle = view.findViewById(R.id.tvTimelineDayTitle);
        TextView tvTimelineStatsSummary = view.findViewById(R.id.tvTimelineStatsSummary);
        ImageButton btnCloseTimelineDialog = view.findViewById(R.id.btnCloseTimelineDialog);
        Button btnTimelinePrevDay = view.findViewById(R.id.btnTimelinePrevDay);
        Button btnTimelineToday = view.findViewById(R.id.btnTimelineToday);
        Button btnTimelineNextDay = view.findViewById(R.id.btnTimelineNextDay);
        RecyclerView rvTimelineStream = view.findViewById(R.id.rvTimelineStream);
        Button btnOpenSmartFinderFromTimeline = view.findViewById(R.id.btnOpenSmartFinderFromTimeline);
        Button btnAddNewEventFromTimeline = view.findViewById(R.id.btnAddNewEventFromTimeline);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        final String[] currentDateArr = new String[]{targetDateIso != null ? targetDateIso : selectedTab2DateIso};

        Runnable updateTimelineUI = () -> {
            String currentIso = currentDateArr[0];
            List<EventModel> dayEvents = dbHelper.getEventsForDate(currentIso);
            GranularTimelineEngine.DayTimelineSummary summary = GranularTimelineEngine.generateDayTimeline(currentIso, dayEvents);
            
            tvTimelineDayTitle.setText(summary.dayHeaderTitle);
            tvTimelineStatsSummary.setText(summary.statsSummary);
            
            TimelineSlotAdapter adapter = new TimelineSlotAdapter(summary.timelineSlots, new TimelineSlotAdapter.OnSlotClickListener() {
                @Override
                public void onFreeSlotClick(TimelineSlot slot) {
                    dialog.dismiss();
                    showAddEventDialog();
                }

                @Override
                public void onBusyEventClick(EventModel event) {
                    dialog.dismiss();
                    showEditEventDialog(event);
                }
            });
            rvTimelineStream.setLayoutManager(new LinearLayoutManager(this));
            rvTimelineStream.setAdapter(adapter);
        };

        updateTimelineUI.run();

        btnCloseTimelineDialog.setOnClickListener(v -> dialog.dismiss());
        btnTimelineToday.setOnClickListener(v -> {
            currentDateArr[0] = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
            updateTimelineUI.run();
        });
        btnTimelinePrevDay.setOnClickListener(v -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(currentDateArr[0]));
                cal.add(Calendar.DAY_OF_MONTH, -1);
                currentDateArr[0] = sdf.format(cal.getTime());
                updateTimelineUI.run();
            } catch (Exception e) {}
        });
        btnTimelineNextDay.setOnClickListener(v -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(currentDateArr[0]));
                cal.add(Calendar.DAY_OF_MONTH, 1);
                currentDateArr[0] = sdf.format(cal.getTime());
                updateTimelineUI.run();
            } catch (Exception e) {}
        });

        btnOpenSmartFinderFromTimeline.setOnClickListener(v -> {
            dialog.dismiss();
            showFreeTimeAnalysis();
        });

        btnAddNewEventFromTimeline.setOnClickListener(v -> {
            dialog.dismiss();
            showAddEventDialog();
        });

        dialog.show();
    }

    private void showFreeTimeAnalysis() {
        playSuccessSound();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_free_time_analysis, null);
        RecyclerView rvFreeTimeDays = view.findViewById(R.id.rvFreeTimeDays);
        ImageButton btnCloseDialog = view.findViewById(R.id.btnCloseFreeTimeDialog);
        Button btnClose = view.findViewById(R.id.btnCloseFreeTime);
        Button btnOpenTimelineFromFree = view.findViewById(R.id.btnOpenTimelineFromFree);
        Button btnOpenSmartFinderFromFree = view.findViewById(R.id.btnOpenSmartFinderFromFree);

        List<FreeTimeDayModel> dayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            String dateIso = sdf.format(cal.getTime());
            List<EventModel> events = dbHelper.searchEvents(dateIso, "Tất cả HK", "Tất cả Năm");
            DayFreeTimeResult res = analyzeDayFreeTime(dateIso, events);

            dayList.add(new FreeTimeDayModel(res.dateIso, res.dayTitle, res.statusBadge, res.statusColor, res.statusBgColor, res.morningDetail, res.afternoonDetail));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (rvFreeTimeDays != null) {
            rvFreeTimeDays.setLayoutManager(new LinearLayoutManager(this));
            rvFreeTimeDays.setAdapter(new FreeTimeDayAdapter(dayList));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnOpenTimelineFromFree != null) {
            btnOpenTimelineFromFree.setOnClickListener(v -> {
                dialog.dismiss();
                showContinuousTimeline(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime()));
            });
        }
        
        if (btnOpenSmartFinderFromFree != null) {
            btnOpenSmartFinderFromFree.setOnClickListener(v -> {
                dialog.dismiss();
                showSmartSlotFinderDialog();
            });
        }

        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showSmartSlotFinderDialog() {
        playSuccessSound();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_smart_slot_finder, null);
        EditText etRequiredMinutes = view.findViewById(R.id.etRequiredMinutes);
        Button btnDoFindSlots = view.findViewById(R.id.btnDoFindSlots);
        TextView chipFind60 = view.findViewById(R.id.chipFind60);
        TextView chipFind90 = view.findViewById(R.id.chipFind90);
        TextView chipFind120 = view.findViewById(R.id.chipFind120);
        TextView chipFind180 = view.findViewById(R.id.chipFind180);
        TextView tvFinderResultCount = view.findViewById(R.id.tvFinderResultCount);
        RecyclerView rvFoundSlots = view.findViewById(R.id.rvFoundSlots);
        ImageButton btnCloseSlotFinder = view.findViewById(R.id.btnCloseSlotFinder);
        Button btnCloseFinderDialog = view.findViewById(R.id.btnCloseFinderDialog);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Runnable performScan = () -> {
            int reqMinutes = 120;
            try {
                String valStr = etRequiredMinutes.getText().toString().trim();
                if (!valStr.isEmpty()) reqMinutes = Integer.parseInt(valStr);
            } catch (Exception ignored) {}

            List<SmartSlotAdapter.FoundSlot> foundSlots = new ArrayList<>();
            SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfDisplay = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
            Calendar cal = Calendar.getInstance();

            for (int day = 0; day < 7; day++) {
                String dateIso = sdfIso.format(cal.getTime());
                String dayDisplayStr = sdfDisplay.format(cal.getTime());
                List<EventModel> dayEvents = dbHelper.getEventsForDate(dateIso);
                GranularTimelineEngine.DayTimelineSummary summary = GranularTimelineEngine.generateDayTimeline(dateIso, dayEvents);

                if (summary != null && summary.timelineSlots != null) {
                    for (TimelineSlot slot : summary.timelineSlots) {
                        if (slot.isFreeSlot() && slot.getDurationMinutes() >= reqMinutes) {
                            String timeRange = TimeNormalizer.minuteToTimeString(slot.getStartMin()) + " - " + TimeNormalizer.minuteToTimeString(slot.getEndMin());
                            int h = slot.getDurationMinutes() / 60;
                            int m = slot.getDurationMinutes() % 60;
                            String durFormatted = (h > 0 ? h + " giờ " : "") + (m > 0 ? m + " phút" : "");
                            if (durFormatted.isEmpty()) durFormatted = slot.getDurationMinutes() + " phút";

                            SmartSlotAdapter.FoundSlot fs = new SmartSlotAdapter.FoundSlot(
                                    dateIso,
                                    dayDisplayStr,
                                    slot.getStartMin(),
                                    slot.getEndMin(),
                                    timeRange,
                                    slot.getDurationMinutes(),
                                    durFormatted,
                                    slot.getSuggestion() != null ? slot.getSuggestion() : "Khả dụng nhận việc"
                            );
                            
                            // 🌟 AI Scoring Logic (Offline, rule-based)
                            int score = 100; // Base score
                            String recommendation = "";
                            
                            if (dayEvents.isEmpty()) {
                                score += 20; // Khá tốt, ngày hoàn toàn trống
                                recommendation = "Ngày trống hoàn toàn, thoải mái sắp xếp.";
                            } else {
                                // Tính toán dựa trên các môn đã có trong ngày
                                boolean sameCampus = false;
                                String matchedCampus = "";
                                for (EventModel ev : dayEvents) {
                                    if (ev.getSchoolCode() != null && !ev.getSchoolCode().isEmpty() && !ev.getSchoolCode().equals("OTHER")) {
                                        sameCampus = true;
                                        matchedCampus = ev.getSchoolCode();
                                        break;
                                    }
                                }
                                
                                if (sameCampus) {
                                    score += 50; // Ưu tiên cực cao: Dạy bù cùng ngày, cùng trường
                                    recommendation = "Nên chọn! Tiện đường đi dạy ở " + matchedCampus;
                                } else {
                                    score -= 10;
                                    recommendation = "Ngày có lịch dạy, cần chú ý di chuyển.";
                                }
                            }
                            
                            fs.score = score;
                            fs.aiRecommendation = recommendation;

                            foundSlots.add(fs);
                        }
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Sắp xếp các slot dựa trên điểm số AI (giảm dần)
            Collections.sort(foundSlots, (a, b) -> Integer.compare(b.score, a.score));

            if (tvFinderResultCount != null) {
                if (foundSlots.isEmpty()) {
                    tvFinderResultCount.setText("⚠️ Không tìm thấy khe rảnh liên tục nào đủ " + reqMinutes + " phút trong 7 ngày tới.");
                    tvFinderResultCount.setTextColor(Color.parseColor("#DC2626"));
                } else {
                    tvFinderResultCount.setText("✅ Tìm thấy " + foundSlots.size() + " khe rảnh đủ điều kiện (>= " + reqMinutes + " phút) trong 7 ngày tới:");
                    tvFinderResultCount.setTextColor(Color.parseColor("#16A34A"));
                }
            }

            SmartSlotAdapter adapter = new SmartSlotAdapter(foundSlots, new SmartSlotAdapter.OnSlotScheduleListener() {
                @Override
                public void onSchedule(SmartSlotAdapter.FoundSlot slot) {
                    dialog.dismiss();
                    showAddEventDialogWithPrefill(slot.dateIso, TimeNormalizer.minuteToTimeString(slot.startMin), TimeNormalizer.minuteToTimeString(slot.endMin));
                }
                
                @Override
                public void onShare(SmartSlotAdapter.FoundSlot slot) {
                    OfficeHoursShareHelper.shareSlotAsImage(MainActivity.this, slot);
                }
            });
            if (rvFoundSlots != null) {
                rvFoundSlots.setLayoutManager(new LinearLayoutManager(this));
                rvFoundSlots.setAdapter(adapter);
            }
        };

        TextView[] durChips = new TextView[]{chipFind60, chipFind90, chipFind120, chipFind180};
        int[] minutesArr = new int[]{60, 90, 120, 180};

        for (int i = 0; i < durChips.length; i++) {
            final int idx = i;
            if (durChips[idx] != null) {
                durChips[idx].setOnClickListener(v -> {
                    for (TextView c : durChips) {
                        if (c != null) {
                            c.setBackgroundResource(R.drawable.bg_chip_unselected);
                            c.setTextColor(Color.parseColor("#64748B"));
                        }
                    }
                    durChips[idx].setBackgroundResource(R.drawable.bg_chip_selected);
                    durChips[idx].setTextColor(Color.parseColor("#FFFFFF"));
                    etRequiredMinutes.setText(String.valueOf(minutesArr[idx]));
                    performScan.run();
                });
            }
        }

        if (btnDoFindSlots != null) {
            btnDoFindSlots.setOnClickListener(v -> performScan.run());
        }

        if (btnCloseSlotFinder != null) btnCloseSlotFinder.setOnClickListener(v -> dialog.dismiss());
        if (btnCloseFinderDialog != null) btnCloseFinderDialog.setOnClickListener(v -> dialog.dismiss());

        performScan.run();
        dialog.show();
    }

    // Lấy tên file gốc từ Uri (hoặc tạo tên mặc định nếu không lấy được)
    private String resolveFileName(Uri uri, String defaultPrefix, String defaultExt) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null && uri != null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        if (result == null || result.trim().isEmpty()) {
            result = defaultPrefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + defaultExt;
        }
        return result.trim();
    }

    // Thư mục lưu trữ riêng của ứng dụng (không bị chặn bởi Scoped Storage trên Android 11-16 khi mở qua FileProvider)
    private File getHistoryStorageDir() {
        File dir = new File(getExternalFilesDir(null), "HistoryFiles");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void deletePhysicalImportedFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        try {
            File dir = getHistoryStorageDir();
            if (dir.exists()) {
                File target = new File(dir, fileName.trim());
                if (target.exists()) target.delete();
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File f : list) {
                        String name = f.getName();
                        if (name.equals(fileName.trim()) || name.equals("view_" + fileName.trim()) || name.startsWith("view_" + fileName.trim())) {
                            f.delete();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void clearAllPhysicalImportedFiles() {
        try {
            File dir = getHistoryStorageDir();
            if (dir.exists()) {
                File[] list = dir.listFiles();
                if (list != null) {
                    for (File f : list) {
                        f.delete();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    // Lưu bản sao nguyên gốc (binary) của file đã chọn vào bộ nhớ ứng dụng (HistoryFiles) để mở lại 100% thành công qua FileProvider
    private void copyUriToDownloads(Uri sourceUri, String targetFileName) {
        if (sourceUri == null || targetFileName == null) return;
        try {
            File appHistoryFile = new File(getHistoryStorageDir(), targetFileName.trim());
            try (InputStream is = getContentResolver().openInputStream(sourceUri);
                 FileOutputStream fos = new FileOutputStream(appHistoryFile)) {
                if (is == null) return;
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
            }

            // Nếu trong thư mục Download chưa có sẵn file này thì tạo thêm bản sao ở Download
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File downloadFile = new File(downloadsDir, targetFileName.trim());
            if (!downloadFile.exists() && appHistoryFile.exists() && appHistoryFile.length() > 0) {
                try (InputStream fis = new java.io.FileInputStream(appHistoryFile);
                     FileOutputStream fos2 = new FileOutputStream(downloadFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        fos2.write(buffer, 0, len);
                    }
                    fos2.flush();
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // Tự động nhận diện và điều hướng xử lý mọi định dạng file (Excel, PDF, PPTX, Word DOCX, CSV/TXT, Ảnh/Tài liệu ghi tay)
    private void processUniversalFileImport(Uri uri) {
        if (uri == null) return;
        String fileName = resolveFileName(uri, "TaiLieu", ".dat");
        String lower = fileName.toLowerCase(Locale.getDefault());
        String mime = getContentResolver().getType(uri);
        if (mime == null) mime = "";
        String lowerMime = mime.toLowerCase(Locale.getDefault());

        if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lowerMime.contains("spreadsheet") || lowerMime.contains("excel")) {
            importExcelFile(uri);
        } else if (lower.endsWith(".pdf") || lowerMime.contains("pdf")) {
            processPdfImport(uri);
        } else if (lower.endsWith(".docx") || lowerMime.contains("wordprocessingml") || lowerMime.contains("msword")) {
            processDocxImport(uri);
        } else if (lower.endsWith(".txt") || lower.endsWith(".csv") || lowerMime.startsWith("text/")) {
            processTextOrCsvImport(uri);
        } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lowerMime.startsWith("image/")) {
            processImageOcrImport(uri);
        } else {
            // Mặc định thử đọc như ảnh OCR / văn bản
            processImageOcrImport(uri);
        }
    }

    // Đọc và phân tích file Word (.docx)
    private void processDocxImport(Uri uri) {
        try {
            String fileName = resolveFileName(uri, "VanBan_Word", ".docx");
            copyUriToDownloads(uri, fileName);

            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is);
            java.util.zip.ZipEntry entry;
            StringBuilder sb = new StringBuilder();

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    byte[] buffer = new byte[4096];
                    int len;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String docXml = baos.toString(StandardCharsets.UTF_8.name());
                    String structured = docXml
                            .replaceAll("</w:tc>", " | ")
                            .replaceAll("</w:tr>", "\n")
                            .replaceAll("</w:p>", "\n")
                            .replaceAll("<w:br\\s*/?>", "\n")
                            .replaceAll("<[^>]+>", "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"");
                    sb.append(structured).append("\n");
                }
            }
            zis.close();
            is.close();

            String cleanedText = normalizeVietnameseOcrText(sb.toString());
            parseAndInsertTextSchedule(cleanedText, fileName, "📝 File Word (.docx)", "Trích xuất cấu trúc XML (Bảng & Đoạn văn)", false);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file Word (.docx): " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Đọc và phân tích file Text / CSV (.txt, .csv)
    private void processTextOrCsvImport(Uri uri) {
        try {
            String fileName = resolveFileName(uri, "DuLieu_Text", ".txt");
            copyUriToDownloads(uri, fileName);

            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            is.close();

            String content = baos.toString(StandardCharsets.UTF_8.name());
            if (fileName.toLowerCase(Locale.getDefault()).endsWith(".csv")) {
                content = content.replace(",", " | ").replace(";", " | ");
            }
            String cleanedText = normalizeVietnameseOcrText(content);
            String formatLabel = fileName.toLowerCase(Locale.getDefault()).endsWith(".csv") ? "📃 Bảng dữ liệu CSV (.csv)" : "📃 Văn bản thuần (.txt)";
            parseAndInsertTextSchedule(cleanedText, fileName, formatLabel, "Đọc trực tiếp luồng văn bản UTF-8", false);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file Text/CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Phát hiện tài liệu có chữ ghi bằng tay (dựa trên độ biến thiên độ tin cậy nét chữ ML Kit, dòng chấm biểu mẫu điền tay hoặc đặc trưng nét viết tay)
    private boolean detectHandwrittenCharacteristics(Text visionText, String rawText) {
        if (rawText != null) {
            // Biểu mẫu có dòng chấm điền tay "......." hoặc các mẫu chữ viết tay đặc trưng
            if (rawText.contains("....") || rawText.contains("NG..NEN") || rawText.contains("40.104") || rawText.contains("Ký tên") || rawText.contains("Chữ ký")) {
                return true;
            }
        }
        if (visionText == null) return false;
        int totalSymbols = 0;
        int lowConfSymbols = 0;
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Float lineConf = line.getConfidence();
                if (lineConf != null && lineConf > 0f && lineConf < 0.78f) {
                    lowConfSymbols += 5;
                }
                for (Text.Element elem : line.getElements()) {
                    totalSymbols++;
                    Float conf = elem.getConfidence();
                    if (conf != null && conf > 0f && conf < 0.80f) {
                        lowConfSymbols++;
                    }
                }
            }
        }
        return totalSymbols > 5 && (lowConfSymbols * 100 / totalSymbols) >= 18;
    }

    // Hiển thị bảng Báo cáo Phân tích Chi tiết & Xem Trực Tiếp Nội Dung cho mọi loại file
    private void showFileAnalysisReportDialog(
            String fileName,
            String fileFormatLabel,
            String extractionMethod,
            boolean isHandwritten,
            int recordCount,
            List<String> extractedSummaries) {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_file_analysis_report, null);
        TextView tvTitle = view.findViewById(R.id.tvFileAnalysisTitle);
        TextView tvBadge = view.findViewById(R.id.tvFileAnalysisBadge);
        TextView tvMeta = view.findViewById(R.id.tvFileAnalysisMeta);
        TextView tvContent = view.findViewById(R.id.tvFileAnalysisContent);
        LinearLayout layoutVisualPreview = view.findViewById(R.id.layoutFileVisualPreview);
        Button btnDownloadReport = view.findViewById(R.id.btnDownloadFileReport);
        Button btnOpenExternalReport = view.findViewById(R.id.btnOpenExternalReport);
        ImageButton btnCloseTop = view.findViewById(R.id.btnCloseAnalysisTop);
        Button btnCloseBottom = view.findViewById(R.id.btnCloseAnalysisBottom);

        if (tvTitle != null) {
            tvTitle.setText("🔍 XEM NỘI DUNG & PHÂN TÍCH FILE");
        }

        if (tvBadge != null) {
            if (isHandwritten) {
                tvBadge.setText("✍️ PHÁT HIỆN TÀI LIỆU CÓ CHỮ GHI BẰNG TAY (ĐÃ CHUẨN HÓA TIẾNG VIỆT)");
                tvBadge.setBackgroundColor(Color.parseColor("#EDE9FE"));
                tvBadge.setTextColor(Color.parseColor("#6D28D9"));
            } else {
                tvBadge.setText("✅ TÀI LIỆU ĐIỆN TỬ / ĐÁNH MÁY CHUẨN XÁC (" + fileFormatLabel.toUpperCase(Locale.getDefault()) + ")");
                tvBadge.setBackgroundColor(Color.parseColor("#DCFCE7"));
                tvBadge.setTextColor(Color.parseColor("#15803D"));
            }
        }

        if (tvMeta != null) {
            String handwritingNote = isHandwritten
                    ? "✍️ Chữ ghi bằng tay / Điền tay trên biểu mẫu (Đã khử nhiễu dấu chấm & phục hồi dấu Tiếng Việt)"
                    : "⌨️ Chữ in / Điện tử chuẩn Unicode Tiếng Việt";
            String metaInfo = "📂 Tên file gốc: " + fileName + "\n"
                    + "🏷️ Loại định dạng: " + fileFormatLabel + "\n"
                    + "⚙️ Công nghệ đọc: " + extractionMethod + "\n"
                    + "🔎 Phân tích chữ: " + handwritingNote + "\n"
                    + "📊 Số mục bóc tách: " + recordCount + " sự kiện lịch biểu chính xác";
            tvMeta.setText(metaInfo);
        }

        File targetFile = resolveHistoryFile(fileName);
        if (targetFile != null && layoutVisualPreview != null) {
            populateVisualPreviewForFile(targetFile, layoutVisualPreview);
        }

        if (tvContent != null) {
            StringBuilder sb = new StringBuilder();
            if (extractedSummaries == null || extractedSummaries.isEmpty()) {
                sb.append("Không tìm thấy nội dung phù hợp.");
            } else {
                for (int i = 0; i < extractedSummaries.size(); i++) {
                    String healed = ThumoiGiangParser.healVietnameseText(extractedSummaries.get(i));
                    sb.append(i + 1).append(". ").append(healed).append("\n\n");
                }
            }
            tvContent.setText(sb.toString().trim());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnDownloadReport != null) {
            btnDownloadReport.setOnClickListener(v -> downloadHistoryFileToDevice(fileName));
        }
        if (btnOpenExternalReport != null) {
            btnOpenExternalReport.setOnClickListener(v -> openExcelFile(fileName));
        }
        if (btnCloseTop != null) btnCloseTop.setOnClickListener(v -> dialog.dismiss());
        if (btnCloseBottom != null) btnCloseBottom.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ TRUNG TÂM THÔNG BÁO (NOTIFICATION CENTER)
    // ==========================================

    private void updateUnreadNotificationBadge() {
        if (dbHelper == null) return;
        int unread = dbHelper.getUnreadNotificationCount();
        if (tvUnreadNotifBadge != null) {
            if (unread > 0) {
                tvUnreadNotifBadge.setVisibility(View.VISIBLE);
                tvUnreadNotifBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            } else {
                tvUnreadNotifBadge.setVisibility(View.GONE);
            }
        }
    }

    private void showNotificationCenterDialog() {
        if (isFinishing() || isDestroyed()) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_notification_center, null);
        ImageButton btnCloseTop = view.findViewById(R.id.btnCloseNotifCenterTop);
        TextView btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        TextView btnEnableMultiSelect = view.findViewById(R.id.btnEnableNotifMultiSelect);
        MaterialCardView layoutMultiBar = view.findViewById(R.id.layoutNotifMultiSelectBar);
        TextView tvSelectedCount = view.findViewById(R.id.tvNotifSelectedCount);
        Button btnSelectAll = view.findViewById(R.id.btnNotifSelectAll);
        Button btnBatchDelete = view.findViewById(R.id.btnNotifBatchDelete);
        Button btnCancelMultiSelect = view.findViewById(R.id.btnNotifCancelMultiSelect);
        RecyclerView rv = view.findViewById(R.id.rvNotifications);
        TextView tvEmpty = view.findViewById(R.id.tvNotifEmpty);
        Button btnClearAll = view.findViewById(R.id.btnClearAllNotifs);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnCloseTop != null) btnCloseTop.setOnClickListener(v -> dialog.dismiss());

        List<NotificationModel> notifList = dbHelper.getAllNotifications();
        final NotificationAdapter[] adapterHolder = new NotificationAdapter[1];
        
        NotificationAdapter adapter = new NotificationAdapter(notifList, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onItemClick(NotificationModel item) {
                // Đánh dấu đã đọc
                if (!item.isRead()) {
                    dbHelper.markNotificationAsRead(item.getId());
                    item.setRead(true);
                    updateUnreadNotificationBadge();
                }
                
                // Mở dialog xem chi tiết thông báo
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(item.getTitle())
                        .setMessage("⏰ Thời gian: " + item.getTimestamp() + "\n\nNội dung chi tiết:\n" + item.getMessage())
                        .setPositiveButton("Đóng", (d, w) -> {
                            d.dismiss();
                            if (adapterHolder[0] != null) {
                                adapterHolder[0].updateData(dbHelper.getAllNotifications());
                            }
                        })
                        .show();
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                if (tvSelectedCount != null) {
                    tvSelectedCount.setText("Đã chọn: " + selectedCount);
                }
                if (layoutMultiBar != null) {
                    layoutMultiBar.setVisibility(adapterHolder[0] != null && adapterHolder[0].isMultiSelectMode() ? View.VISIBLE : View.GONE);
                }
            }
        });
        adapterHolder[0] = adapter;

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(adapter);
        }

        Runnable refreshUiState = () -> {
            List<NotificationModel> currentList = dbHelper.getAllNotifications();
            adapter.updateData(currentList);
            boolean isEmpty = currentList.isEmpty();
            if (tvEmpty != null) tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (rv != null) rv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            updateUnreadNotificationBadge();
        };

        refreshUiState.run();

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                dbHelper.markAllNotificationsAsRead();
                playSuccessSound();
                refreshUiState.run();
                Toast.makeText(this, "Đã đánh dấu tất cả thông báo là đã đọc!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnEnableMultiSelect != null) {
            btnEnableMultiSelect.setOnClickListener(v -> {
                adapter.setMultiSelectMode(true);
            });
        }

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        }

        if (btnCancelMultiSelect != null) {
            btnCancelMultiSelect.setOnClickListener(v -> adapter.setMultiSelectMode(false));
        }

        if (btnBatchDelete != null) {
            btnBatchDelete.setOnClickListener(v -> {
                Set<Integer> ids = adapter.getSelectedIds();
                if (ids.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn ít nhất 1 thông báo để xóa!", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbHelper.deleteMultipleNotifications(new ArrayList<>(ids));
                playSuccessSound();
                adapter.setMultiSelectMode(false);
                refreshUiState.run();
                Toast.makeText(this, "Đã xóa " + ids.size() + " thông báo cũ!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> {
                showCustomConfirmDialog(
                        "🧹",
                        "XÓA TẤT CẢ THÔNG BÁO",
                        "Bạn có chắc chắn muốn xóa toàn bộ lịch sử thông báo không?",
                        "🔴 Xóa tất cả",
                        Color.parseColor("#DC2626"),
                        () -> {
                            dbHelper.clearAllNotifications();
                            playSuccessSound();
                            refreshUiState.run();
                            Toast.makeText(this, "Đã xóa toàn bộ thông báo!", Toast.LENGTH_SHORT).show();
                        });
            });
        }

        dialog.show();
    }

    private void showAppIntroductionDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_app_introduction, null);
        ImageButton btnCloseTop = view.findViewById(R.id.btnCloseAppIntroTop);
        Button btnCloseBottom = view.findViewById(R.id.btnCloseAppIntroBottom);
        TextView tvTechInfo = view.findViewById(R.id.tvAppIntroTechnicalInfo);

        if (tvTechInfo != null) {
            tvTechInfo.setText("• Phiên bản: v" + VersionCheckHelper.getCurrentVersionName(this) + " (Stable)\n• Tác giả / Phát triển: TKyl\n• Yêu cầu thiết bị: Android 8.0 trở lên (API 26+)\n• Nền tảng: Android (Java / Kotlin, Material Design 3)\n• Môn học: Lập trình Di động (LTDD)\n• Trường: Đại học Nguyễn Tất Thành (NTTU)");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        KeyboardUtils.applyAutoHideKeyboard(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View.OnClickListener dismissListener = v -> {
            SoundHelper.playFeedback(this, v);
            dialog.dismiss();
        };

        if (btnCloseTop != null) btnCloseTop.setOnClickListener(dismissListener);
        if (btnCloseBottom != null) btnCloseBottom.setOnClickListener(dismissListener);

        dialog.show();
    }

    private void processImageOcrImport(Uri uri) {
        try {
            String fileName = resolveFileName(uri, "OCR_Anh", ".png");
            copyUriToDownloads(uri, fileName);

            InputImage image = InputImage.fromFilePath(this, uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String rawOrdered = extractOrderedTextFromVision(visionText);
                        boolean isHandwritten = detectHandwrittenCharacteristics(visionText, rawOrdered);
                        String orderedText = normalizeVietnameseOcrText(rawOrdered);
                        String formatLabel = isHandwritten ? "✍️ Ảnh chụp chữ ghi tay (" + getFileExtensionLabel(fileName) + ")" : "🖼️ Ảnh chụp bảng biểu (" + getFileExtensionLabel(fileName) + ")";
                        String methodLabel = isHandwritten ? "AI OCR Nhận diện chữ viết tay + Phục hồi dấu Tiếng Việt" : "Google ML Kit Vision OCR (Sắp xếp tọa độ hàng/cột)";
                        parseAndInsertTextSchedule(orderedText, fileName, formatLabel, methodLabel, isHandwritten);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi đọc chữ OCR từ ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileExtensionLabel(String fileName) {
        if (fileName == null) return "FILE";
        int idx = fileName.lastIndexOf('.');
        if (idx >= 0 && idx < fileName.length() - 1) {
            return fileName.substring(idx).toLowerCase(Locale.getDefault());
        }
        return "FILE";
    }

    // Sắp xếp các dòng chữ nhận diện được theo tọa độ Y (từ trên xuống dưới) và X (từ trái sang phải)
    private String extractOrderedTextFromVision(Text visionText) {
        if (visionText == null) return "";
        List<Text.Line> allLines = new ArrayList<>();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            allLines.addAll(block.getLines());
        }
        if (allLines.isEmpty()) return visionText.getText();

        Collections.sort(allLines, (l1, l2) -> {
            Rect r1 = l1.getBoundingBox();
            Rect r2 = l2.getBoundingBox();
            if (r1 == null || r2 == null) return 0;
            int tol = Math.max(18, Math.min(r1.height(), r2.height()) / 2);
            if (Math.abs(r1.centerY() - r2.centerY()) <= tol) {
                return Integer.compare(r1.left, r2.left);
            }
            return Integer.compare(r1.centerY(), r2.centerY());
        });

        StringBuilder sb = new StringBuilder();
        int lastCenterY = -9999;
        int lastHeight = 30;
        for (Text.Line line : allLines) {
            Rect r = line.getBoundingBox();
            int tol = r != null ? Math.max(18, Math.min(r.height(), lastHeight) / 2) : 20;
            if (r != null && lastCenterY != -9999 && Math.abs(r.centerY() - lastCenterY) <= tol) {
                sb.append(" | ").append(line.getText().trim());
            } else {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line.getText().trim());
                if (r != null) {
                    lastCenterY = r.centerY();
                    lastHeight = r.height();
                }
            }
        }
        return sb.toString();
    }

    private void processPdfImport(Uri uri) {
        String fileName = resolveFileName(uri, "ThongBao_PDF", ".pdf");
        copyUriToDownloads(uri, fileName);
        Toast.makeText(this, "⏳ Đang đọc & phân tích cấu trúc file PDF...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            StringBuilder extractedBuilder = new StringBuilder();
            boolean isScannedPdf = false;
            boolean isHandwrittenPdf = false;

            // Cách 1 (Ưu tiên số 1): Dùng PDFBox-Android trích xuất trực tiếp lớp văn bản Unicode Tiếng Việt gốc trong PDF
            try {
                PDFBoxResourceLoader.init(getApplicationContext());
                File localCopy = resolveHistoryFile(fileName);
                if (localCopy != null && localCopy.exists() && localCopy.length() > 0) {
                    try (PDDocument document = PDDocument.load(localCopy)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        stripper.setSortByPosition(false);
                        String text = stripper.getText(document);
                        if (text == null || text.trim().isEmpty()) {
                            stripper.setSortByPosition(true);
                            text = stripper.getText(document);
                        }
                        if (text != null && !text.trim().isEmpty()) {
                            extractedBuilder.append(text.trim()).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("LectuPlan", "PDFBox load local file error: " + e.getMessage(), e);
            }

            if (extractedBuilder.toString().trim().isEmpty()) {
                try {
                    PDFBoxResourceLoader.init(getApplicationContext());
                    try (InputStream is = getContentResolver().openInputStream(uri)) {
                        if (is != null) {
                            PDDocument document = PDDocument.load(is);
                            PDFTextStripper stripper = new PDFTextStripper();
                            stripper.setSortByPosition(false);
                            String text = stripper.getText(document);
                            if (text == null || text.trim().isEmpty()) {
                                stripper.setSortByPosition(true);
                                text = stripper.getText(document);
                            }
                            document.close();
                            if (text != null && !text.trim().isEmpty()) {
                                extractedBuilder.append(text.trim()).append("\n");
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("LectuPlan", "PDFBox stream error: " + e.getMessage(), e);
                }
            }

            // Cách 2 (Dành cho PDF bản scan ảnh chụp hoặc biểu mẫu ghi tay không có lớp text): Dùng PdfRenderer dựng ảnh 3x + ML Kit OCR + Hậu xử lý chữ viết tay Tiếng Việt
            if (extractedBuilder.toString().trim().isEmpty()) {
                isScannedPdf = true;
                try {
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        PdfRenderer renderer = new PdfRenderer(pfd);
                        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                        int pageCount = Math.min(renderer.getPageCount(), 15);

                        for (int i = 0; i < pageCount; i++) {
                            PdfRenderer.Page page = renderer.openPage(i);
                            int width = Math.max(1600, page.getWidth() * 3);
                            int height = Math.max(2200, page.getHeight() * 3);
                            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(Color.WHITE);
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            page.close();

                            InputImage image = InputImage.fromBitmap(bitmap, 0);
                            Text visionText = Tasks.await(recognizer.process(image));
                            String pageText = extractOrderedTextFromVision(visionText);
                            if (detectHandwrittenCharacteristics(visionText, pageText)) {
                                isHandwrittenPdf = true;
                            }
                            if (!pageText.trim().isEmpty()) {
                                extractedBuilder.append(pageText).append("\n");
                            }
                            bitmap.recycle();
                        }
                        renderer.close();
                        pfd.close();
                    }
                } catch (Exception ignored) {}
            } else {
                String rawStr = extractedBuilder.toString();
                if (detectHandwrittenCharacteristics(null, rawStr)) {
                    isHandwrittenPdf = true;
                }
            }

            final boolean finalHandwritten = isHandwrittenPdf;
            final String formatLabel = finalHandwritten
                    ? "✍️ Tài liệu PDF Bản scan ghi tay (.pdf)"
                    : (isScannedPdf ? "📄 Tài liệu PDF Bản scan (.pdf)" : "📄 Tài liệu PDF Văn bản số (.pdf)");
            final String methodLabel = isScannedPdf
                    ? "PdfRenderer 3x DPI + AI OCR Chữ viết tay & Phục hồi Tiếng Việt"
                    : "PDFBox-Android (Trích xuất trực tiếp lớp văn bản Unicode gốc)";
            final String finalExtractedText = normalizeVietnameseOcrText(extractedBuilder.toString());
            runOnUiThread(() -> parseAndInsertTextSchedule(finalExtractedText, fileName, formatLabel, methodLabel, finalHandwritten));
        }).start();
    }

    // Hậu xử lý sửa lỗi dấu Tiếng Việt phổ biến khi đọc OCR từ ảnh chụp hoặc bản scan chữ viết tay
    private String normalizeVietnameseOcrText(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw;

        // 1. Sửa lỗi nhầm số/chữ cái đầu giờ (ví dụ "Zh50- 8h40" -> "7h50 - 8h40", "Bh40- 9h30" -> "8h40 - 9h30")
        s = s.replaceAll("(?i)\\b[Zz]h(\\d{2})", "7h$1");
        s = s.replaceAll("(?i)\\b[Bb]h(\\d{2})", "8h$1");
        s = s.replaceAll("(\\d{1,2}h\\d{2})\\s*-\\s*(\\d{1,2}h\\d{2})", "$1 - $2");

        // 2. Sửa các cụm từ Tiếng Việt phổ biến bị OCR nhận diện nhầm
        String[][] replacements = new String[][]{
                {"(?i)\\bBU[ỐỔỖO]I\\s+SANG\\b", "BUỔI SÁNG"},
                {"(?i)\\bBU[ỐỔỖO]I\\s+CHI[ỀỂỄE]U\\b", "BUỔI CHIỀU"},
                {"(?i)\\bBU[ỐỔỖO]I\\s+T[ỐỔỖO]I\\b", "BUỔI TỐI"},
                {"(?i)\\bGi[àảãa]i\\s+lao\\b", "Giải lao"},
                {"(?i)\\bNgay\\s+phat\\b", "Ngày phát"},
                {"(?i)\\bNgay\\s+nha[i]?n\\b", "Ngày nhận"},
                {"(?i)\\bS[ốổ]\\s+h[ồổ]\\s+s[ơo][i]?\\b", "Số hồ sơ"},
                {"(?i)\\bTh[ờơ]i\\s+kho[áa]\\s+bi[ểe]u\\b", "Thời khóa biểu"},
                {"(?i)\\bTh[ưu]\\s+m[ờo]i\\s+gi[ảa]ng\\b", "Thư mời giảng"},
                {"(?i)\\bL[ịi]ch\\s+gi[ảa]ng\\b", "Lịch giảng"},
                {"(?i)\\bPh[òo]ng\\s+h[ọo]c\\b", "Phòng học"},
                {"(?i)\\bThực[\\s|/]+h[àa][\\s|/]+h[\\s|/]+lập[\\s|/]+r[ìi][\\s|/]+h[\\s|/]+Web\\b", "Thực hành lập trình Web"},
                {"(?i)\\bThực[\\s|/]+h[a-zà-ỹA-ZÀ-Ỹ\\s|/]{1,10}[\\s|/]+lập[\\s|/]+[a-zà-ỹA-ZÀ-Ỹ\\s|/]{1,10}[\\s|/]+Web\\b", "Thực hành lập trình Web"},
                {"(?i)\\bThực[\\s|/]+h[àa][\\s|/]+h\\b", "Thực hành"},
                {"(?i)\\blập[\\s|/]+r[ìi][\\s|/]+h\\b", "lập trình"},
                {"(?i)\\blập[\\s|/]+tr[ìi][\\s|/]+h\\b", "lập trình"},
                {"(?i)\\br[ìi][\\s|/]+h[\\s|/]+Web\\b", "trình Web"},
                {"(?i)\\bchương[\\s|/]+r[ìi][\\s|/]+h\\b", "chương trình"},
                {"(?i)\\bquá[\\s|/]+r[ìi][\\s|/]+h\\b", "quá trình"},
                {"(?i)\\bh[àa][\\s|/]+h\\b", "hành"}
        };

        for (String[] pair : replacements) {
            s = s.replaceAll(pair[0], pair[1]);
        }

        s = ThumoiGiangParser.healVietnameseText(s);

        // 3. Dọn dẹp các chuỗi chấm "......." dài trên biểu mẫu in sẵn
        s = s.replaceAll("\\.{2,}", " ");
        return s;
    }



    public static class FileValidationResult {
        public final boolean isValid;
        public final String detectedType; // Lịch dạy/học, Lịch sự kiện, Lịch phát sinh, Lịch nhập thủ công(khác)
        public final String rejectReason;

        public FileValidationResult(boolean isValid, String detectedType, String rejectReason) {
            this.isValid = isValid;
            this.detectedType = detectedType;
            this.rejectReason = rejectReason;
        }

        public static FileValidationResult valid(String detectedType) {
            return new FileValidationResult(true, detectedType, null);
        }

        public static FileValidationResult reject(String reason) {
            return new FileValidationResult(false, null, reason);
        }
    }

    public static FileValidationResult validateScheduleDocument(String fullText, String fileName) {
        if (fullText == null || fullText.trim().isEmpty()) {
            return FileValidationResult.reject("Tệp tin rỗng hoặc không trích xuất được nội dung văn bản.");
        }

        String lowerText = fullText.toLowerCase(Locale.getDefault());
        String lowerFileName = (fileName != null ? fileName : "").toLowerCase(Locale.getDefault());

        // 1. KIỂM DUYỆT TỪ CHỐI CV / HỒ SƠ CÁ NHÂN / SƠ YẾU LÝ LỊCH / BẢN THÔNG TIN
        boolean hasCvFileName = lowerFileName.contains("cv") 
                || lowerFileName.contains("curriculum") 
                || lowerFileName.contains("resume") 
                || lowerFileName.contains("so yeu ly lich") 
                || lowerFileName.contains("sơ yếu lý lịch") 
                || lowerFileName.contains("ly lich") 
                || lowerFileName.contains("lý lịch") 
                || lowerFileName.contains("don xin viec")
                || lowerFileName.contains("đơn xin việc");

        int cvKeywordsCount = 0;
        String[] strongCvKeywords = {
                "curriculum vitae", "sơ yếu lý lịch", "lý lịch khoa học", "bản thông tin cá nhân",
                "hồ sơ cá nhân", "hồ sơ xin việc", "thông tin ứng viên", "người nhận học bổng",
                "kinh nghiệm làm việc", "mục tiêu nghề nghiệp", "kết quả học vấn", "quá trình đào tạo",
                "người tham chiếu", "kỹ năng chuyên môn", "kỹ năng mềm", "tình trạng hôn nhân",
                "họ và tên cha", "họ và tên mẹ", "hộ khẩu thường trú", "địa chỉ thường trú",
                "chứng minh nhân dân", "căn cước công dân", "nguyện vọng bản thân"
        };
        for (String kw : strongCvKeywords) {
            if (lowerText.contains(kw)) {
                cvKeywordsCount++;
            }
        }

        if (hasCvFileName || cvKeywordsCount >= 2 || 
            (cvKeywordsCount >= 1 && (lowerText.contains("dân tộc") || lowerText.contains("tôn giáo") || lowerText.contains("quê quán") || lowerText.contains("nơi sinh")))) {
            return FileValidationResult.reject(
                    "Tệp tin được nhận diện là Hồ sơ cá nhân / Sơ yếu lý lịch / CV (" + (fileName != null ? fileName : "Tệp đã chọn") + ").\n\n" +
                    "Ứng dụng LectuPlan từ chối nhập loại tài liệu này. Ứng dụng chỉ tiếp nhận các tài liệu lịch trình: Thư mời dạy, Thời khóa biểu, Lịch sự kiện trường, hoặc Lịch phát sinh (dạy bù, coi thi, chấm thi).");
        }

        // 2. KIỂM TRA TÍN HIỆU LỊCH BIỂU (SCHEDULE SIGNALS)
        boolean hasTeachingSignal = lowerText.contains("thư mời giảng") 
                || lowerText.contains("thư mời dạy") 
                || lowerText.contains("giấy báo giảng") 
                || lowerText.contains("thời khóa biểu") 
                || lowerText.contains("thoi khoa bieu") 
                || lowerText.contains("tkb") 
                || lowerText.contains("lịch giảng dạy") 
                || lowerText.contains("kế hoạch giảng dạy")
                || lowerText.contains("lịch học")
                || lowerText.contains("lịch trực tuyến")
                || lowerText.contains("tiết bđ") 
                || lowerText.contains("tiết bắt đầu") 
                || lowerText.contains("số tiết")
                || lowerText.contains("mã học phần") 
                || lowerText.contains("mã môn") 
                || lowerText.contains("mã mh")
                || lowerText.contains("tổ th") 
                || lowerText.contains("nhóm lớp")
                || lowerText.matches("(?s).*\\btiết\\s*[:=\\s-]*\\d+.*");

        boolean hasIncidentSignal = lowerText.contains("dạy bù") 
                || lowerText.contains("học bù") 
                || lowerText.contains("đổi ca") 
                || lowerText.contains("tăng tiết") 
                || lowerText.contains("dời lịch") 
                || lowerText.contains("dời phòng") 
                || lowerText.contains("coi thi") 
                || lowerText.contains("chấm thi") 
                || lowerText.contains("khảo thí") 
                || lowerText.contains("hội đồng thi") 
                || lowerText.contains("bảo vệ đồ án") 
                || lowerText.contains("khóa luận") 
                || lowerText.contains("vấn đáp")
                || lowerText.contains("phân công coi thi") 
                || lowerText.contains("lịch thi");

        boolean hasEventSignal = lowerText.contains("lịch công tác") 
                || lowerText.contains("lịch tuần") 
                || lowerText.contains("kế hoạch tuần") 
                || lowerText.contains("thông báo số") 
                || lowerText.contains("thông báo về việc") 
                || lowerText.contains("lịch làm việc") 
                || lowerText.contains("hội nghị") 
                || lowerText.contains("hội thảo") 
                || lowerText.contains("khai giảng") 
                || lowerText.contains("tốt nghiệp") 
                || lowerText.contains("bế giảng") 
                || lowerText.contains("đại hội") 
                || lowerText.contains("kỷ niệm") 
                || lowerText.contains("tập huấn") 
                || lowerText.contains("sinh hoạt chuyên môn") 
                || lowerText.contains("giao ban") 
                || lowerText.contains("họp khoa") 
                || lowerText.contains("họp bộ môn") 
                || (lowerText.contains("chủ trì") && lowerText.contains("địa điểm"));

        boolean hasDate = lowerText.matches("(?s).*\\b(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4})\\b.*")
                || lowerText.matches("(?s).*\\b(\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2})\\b.*")
                || lowerText.matches("(?s).*\\b(thứ\\s+(hai|ba|tư|năm|sáu|bảy)|chủ\\s+nhật)\\b.*")
                || lowerText.matches("(?s).*\\b(thứ\\s+[2-7]|th\\s+[2-7]|cn)\\b.*")
                || lowerText.matches("(?s).*\\b(\\d{1,2}[/.-]\\d{1,2})\\b.*");

        if (!hasTeachingSignal && !hasIncidentSignal && !hasEventSignal && !hasDate) {
            return FileValidationResult.reject(
                    "Tệp tin không chứa thông tin lịch biểu, thư mời dạy hoặc sự kiện hợp lệ.\n\n" +
                    "Ứng dụng LectuPlan chỉ hỗ trợ nhập:\n" +
                    "• Lịch dạy/học: Thư mời dạy, thời khóa biểu, lịch giảng dạy\n" +
                    "• Lịch sự kiện: Thông báo trường gửi, lịch tuần, hội nghị, khai giảng\n" +
                    "• Lịch phát sinh: Dạy bù, học bù, đổi ca, lịch coi/chấm thi\n" +
                    "• Lịch nhập thủ công(khác)");
        }

        if (hasIncidentSignal) {
            return FileValidationResult.valid(EventModel.CAT_INCIDENT);
        } else if (hasTeachingSignal) {
            return FileValidationResult.valid(EventModel.CAT_TEACHING);
        } else if (hasEventSignal) {
            return FileValidationResult.valid(EventModel.CAT_EVENT);
        } else {
            return FileValidationResult.valid(EventModel.CAT_MANUAL_OTHER);
        }
    }

    private void showFileRejectedDialog(String fileName, String reason) {
        if (isFinishing() || isDestroyed()) return;

        runOnUiThread(() -> {
            try {
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_file_rejected, null);
                TextView tvRejectFileName = dialogView.findViewById(R.id.tvRejectFileName);
                TextView tvRejectReason = dialogView.findViewById(R.id.tvRejectReason);
                Button btnRejectUnderstand = dialogView.findViewById(R.id.btnRejectUnderstand);

                if (tvRejectFileName != null) {
                    tvRejectFileName.setText("Tệp: " + (fileName != null ? fileName : "Tệp đã chọn"));
                }
                if (tvRejectReason != null && reason != null && !reason.trim().isEmpty()) {
                    tvRejectReason.setText(reason);
                }

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(true)
                        .create();
                KeyboardUtils.applyAutoHideKeyboard(dialog);

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                if (btnRejectUnderstand != null) {
                    btnRejectUnderstand.setOnClickListener(v -> dialog.dismiss());
                }

                dialog.show();
            } catch (Exception e) {
                Toast.makeText(this, "TỪ CHỐI TÀI LIỆU: " + reason, Toast.LENGTH_LONG).show();
            }
        });
    }

    public enum DocumentType {
        TEACHING_SCHEDULE,
        CV_OR_PROFILE,
        WORK_OR_MEETING,
        GENERAL_NOTE
    }

    public static DocumentType detectDocumentType(String fullText) {
        if (fullText == null || fullText.isEmpty()) return DocumentType.GENERAL_NOTE;
        String lower = fullText.toLowerCase(Locale.getDefault());

        int cvScore = 0;
        if (lower.contains("curriculum vitae") || lower.contains("sơ yếu lý lịch") || lower.contains("lý lịch khoa học")
                || lower.contains("bản thông tin cá nhân") || lower.contains("hồ sơ cá nhân") || lower.contains("hồ sơ xin việc")
                || lower.contains("thông tin ứng viên") || lower.contains("người nhận học bổng")) {
            cvScore += 5;
        }
        if (lower.contains("kinh nghiệm làm việc") || lower.contains("work experience")) cvScore += 2;
        if (lower.contains("mục tiêu nghề nghiệp") || lower.contains("career objective")) cvScore += 2;
        if (lower.contains("quá trình đào tạo") || lower.contains("kết quả học vấn")) cvScore += 2;
        if (lower.contains("dân tộc") && (lower.contains("tôn giáo") || lower.contains("nơi sinh") || lower.contains("quê quán"))) cvScore += 3;
        if (lower.contains("họ và tên") && (lower.contains("năm sinh") || lower.contains("ngày sinh") || lower.contains("giới tính"))) cvScore += 3;
        if (lower.contains("địa chỉ thường trú") || lower.contains("hộ khẩu thường trú")) cvScore += 2;

        int teachingScore = 0;
        if (lower.contains("thư mời giảng") || lower.contains("thoi khoa bieu") || lower.contains("thời khóa biểu")
                || lower.contains("lịch giảng dạy") || lower.contains("kế hoạch giảng dạy")
                || lower.contains("lịch học") || lower.contains("lịch dạy")) {
            teachingScore += 5;
        }
        if (lower.contains("tiết bđ") || lower.contains("tiết bắt đầu") || lower.contains("số tiết")) teachingScore += 3;
        if (lower.contains("tổ th") || lower.contains("nhóm/tổ") || lower.contains("nhóm lớp")) teachingScore += 3;
        if (lower.contains("mã mh") || lower.contains("mã môn") || lower.contains("mã học phần")) teachingScore += 3;
        if (lower.contains("sĩ số") || lower.contains("tín chỉ") || lower.contains("đợt học")) teachingScore += 2;
        if (lower.matches("(?s).*\\btiết\\s*[:=\\s-]*\\d+\\s*[-–đếnto]+\\s*\\d+\\b.*")) teachingScore += 4;
        if (lower.contains("giảng viên:") || lower.contains("giảng viên :") || lower.contains("gv:")) teachingScore += 3;
        if (lower.matches("(?s).*\\bphòng\\s*[:=]?\\s*[a-z0-9_./-]+.*") && (lower.contains("tiết") || lower.contains("lịch"))) teachingScore += 3;

        int workScore = 0;
        if (lower.contains("lịch công tác") || lower.contains("kế hoạch công tác") || lower.contains("lịch làm việc")
                || lower.contains("lịch tuần") || lower.contains("thông báo họp")) {
            workScore += 4;
        }
        if (lower.contains("chủ trì") || lower.contains("thành phần") || lower.contains("nội dung công tác")) workScore += 3;

        if (cvScore >= 4 && cvScore > teachingScore) return DocumentType.CV_OR_PROFILE;
        if (teachingScore >= 3) return DocumentType.TEACHING_SCHEDULE;
        if (workScore >= 3) return DocumentType.WORK_OR_MEETING;
        if (cvScore >= 3) return DocumentType.CV_OR_PROFILE;
        return DocumentType.GENERAL_NOTE;
    }

    private static boolean isStaticBiographicalLine(String line) {
        if (line == null) return true;
        String lower = line.toLowerCase(Locale.getDefault()).trim();
        if (lower.length() < 3) return true;

        String[] bioKeywords = new String[]{
                "họ và tên", "họ tên", "tên thánh", "năm sinh", "ngày sinh", "nơi sinh",
                "giới tính", "dân tộc", "tôn giáo", "quốc tịch", "địa chỉ", "quê quán",
                "thường trú", "tạm trú", "hộ khẩu", "số điện thoại", "điện thoại", "sđt",
                "email", "địa chỉ thư điện tử", "chỗ ở", "cmnd", "cccd", "số căn cước",
                "họ và tên cha", "họ và tên mẹ", "cha:", "mẹ:", "nghề nghiệp", "thu nhập",
                "chi phí", "học bổng", "người nhận học bổng", "xác nhận", "ký tên", "chữ ký",
                "linh mục", "giáo xứ", "giáo phận", "ban điều hành", "người nhận", "bản thông tin",
                "sơ yếu lý lịch", "curriculum vitae", "tình trạng hôn nhân", "người tham chiếu",
                "sở thích", "mục tiêu nghề nghiệp", "kỹ năng", "tin học", "ngoại ngữ",
                "cam kết", "lưu ý", "mẫu hồ sơ", "kết quả học vấn", "có vay tín dụng",
                "hiện nay, có nhận", "có tham gia sinh hoạt", "xin quý cha", "điều hành địa chỉ",
                "là sinh viên của trường", "năm nhập học", "thời gian đào tạo", "chi phí bình quân"
        };

        for (String kw : bioKeywords) {
            if (lower.startsWith(kw) || lower.contains(kw + ":") || lower.contains(kw + " :")) {
                return true;
            }
        }

        if (lower.matches("(?i)^(họ\\s+và\\s+tên|ngày\\s+sinh|năm\\s+sinh|nơi\\s+sinh|giới\\s+tính|dân\\s+tộc|tôn\\s+giáo|địa\\s+chỉ).*")) {
            return true;
        }

        return false;
    }

    private void parseAndInsertTextSchedule(String fullText, String fileName, String fileFormatLabel, String extractionMethod, boolean isHandwritten) {
        if (fullText == null || fullText.trim().isEmpty()) {
            Toast.makeText(this, "Không đọc thấy văn bản lịch biểu trong file!", Toast.LENGTH_SHORT).show();
            return;
        }

        FileValidationResult valResult = validateScheduleDocument(fullText, fileName);
        if (!valResult.isValid) {
            showFileRejectedDialog(fileName, valResult.rejectReason);
            return;
        }

        DocumentType docType = detectDocumentType(fullText);

        String activeFormatLabel = fileFormatLabel;
        String activeExtractionMethod = extractionMethod;
        String defaultSourceOrganizer;

        if (EventModel.CAT_TEACHING.equals(valResult.detectedType) || docType == DocumentType.TEACHING_SCHEDULE) {
            activeFormatLabel = isHandwritten
                    ? "✍️ Lịch dạy viết tay (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")"
                    : "👨‍🏫 Thời khóa biểu / Thư mời giảng (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
            activeExtractionMethod = "Bóc tách môn học, lớp, phòng & số tiết chuẩn hóa";
            defaultSourceOrganizer = isHandwritten
                    ? "✍️ " + EventModel.CAT_TEACHING + " • " + fileName
                    : "👨‍🏫 " + EventModel.CAT_TEACHING + " • " + fileName;
        } else if (EventModel.CAT_INCIDENT.equals(valResult.detectedType)) {
            activeFormatLabel = "⚡ Lịch phát sinh (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
            activeExtractionMethod = "Bóc tách lịch dạy bù, đổi ca, coi/chấm thi & hội đồng";
            defaultSourceOrganizer = "⚡ " + EventModel.CAT_INCIDENT + " • " + fileName;
        } else if (EventModel.CAT_EVENT.equals(valResult.detectedType) || docType == DocumentType.WORK_OR_MEETING) {
            activeFormatLabel = "🏛️ Lịch sự kiện (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
            activeExtractionMethod = "Bóc tách thông báo trường, hội nghị, khai giảng & sự vụ";
            defaultSourceOrganizer = "🏛️ " + EventModel.CAT_EVENT + " • " + fileName;
        } else {
            defaultSourceOrganizer = isHandwritten
                    ? "✍️ Ghi tay • " + fileFormatLabel + " (" + fileName + ")"
                    : fileFormatLabel + " • " + fileName;
        }

        int count = 0;
        int stt = 1;
        SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDateStr = sdfVn.format(new Date());
        List<String> extractedSummaries = new ArrayList<>();

        // 1. Thư mời giảng có cấu trúc thời khóa biểu đại học chuyên sâu (kỳ học nhiều tuần)
        if (docType == DocumentType.TEACHING_SCHEDULE) {
            try {
                ThumoiGiangParser.ParseResult pResult = ThumoiGiangParser.parseInvitationDocument(fullText);
                if (pResult != null && !pResult.generatedEvents.isEmpty()) {
                    android.database.sqlite.SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                    writableDb.beginTransaction();
                    try {
                        for (ThumoiGiangParser.GeneratedClassEvent gEv : pResult.generatedEvents) {
                            String orgWithFile = (gEv.organizer != null ? gEv.organizer : "");
                            if (fileName != null && !fileName.trim().isEmpty() && !orgWithFile.contains(fileName.trim())) {
                                orgWithFile = orgWithFile + " • " + fileName.trim();
                            }
                            String healedTitle = ThumoiGiangParser.healVietnameseText(gEv.title);
                            dbHelper.insertEvent(stt++, healedTitle, gEv.dateIso, orgWithFile);
                            extractedSummaries.add("[" + gEv.dateIso + "] " + healedTitle);
                            count++;
                        }
                        dbHelper.normalizeSttNumbers();
                        writableDb.setTransactionSuccessful();
                    } finally {
                        writableDb.endTransaction();
                    }

                    String historyBadgeType = "LỊCH DẠY (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
                    dbHelper.addFileHistory(fileName, historyBadgeType, count);
                    refreshFileHistory();
                    playSuccessSound();
                    refreshData();
                    refreshTab2CalendarEvents();
                    showFileAnalysisReportDialog(fileName, activeFormatLabel, "Bóc tách Thư mời giảng (Tự động rải lịch từng tuần & kiểm tra ngày lễ)", isHandwritten, count, extractedSummaries);
                    return;
                }
            } catch (Exception ignored) {}
        }

        // 2. Bóc tách thông minh đa năng (UniversalScheduleParser): Dành cho Ảnh chụp lịch học OCR (theo ngày/tuần), thẻ môn học điện tử
        try {
            List<UniversalScheduleParser.ParsedScheduleEvent> parsedEvents = UniversalScheduleParser.parseDocument(fullText, fileName);
            if (parsedEvents != null && !parsedEvents.isEmpty()) {
                android.database.sqlite.SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
                writableDb.beginTransaction();
                try {
                    for (UniversalScheduleParser.ParsedScheduleEvent pEv : parsedEvents) {
                        String healedTitle = ThumoiGiangParser.healVietnameseText(pEv.title);
                        dbHelper.insertEvent(stt++, healedTitle, pEv.dateIso, pEv.organizer);
                        extractedSummaries.add("[" + pEv.dateIso + "] " + healedTitle);
                        count++;
                    }
                    dbHelper.normalizeSttNumbers();
                    writableDb.setTransactionSuccessful();
                } finally {
                    writableDb.endTransaction();
                }

                String historyBadgeType = isHandwritten
                        ? "LỊCH BIỂU (VIẾT TAY • " + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")"
                        : "LỊCH BIỂU (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
                dbHelper.addFileHistory(fileName, historyBadgeType, count);
                refreshFileHistory();
                playSuccessSound();
                refreshData();
                refreshTab2CalendarEvents();
                showFileAnalysisReportDialog(fileName, activeFormatLabel, "Bóc tách Thẻ Lịch học & Thời khóa biểu chuẩn xác 100% (Khử hoàn toàn nhiễu giao diện)", isHandwritten, count, extractedSummaries);
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("LectuPlan", "Lỗi UniversalScheduleParser: " + e.getMessage(), e);
        }

        String[] lines = fullText.split("\\r?\\n");
        android.database.sqlite.SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.beginTransaction();
        try {
            for (String line : lines) {
                String lineTrim = line.replaceAll("\\s+", " ").trim();
                if (lineTrim.isEmpty() || lineTrim.length() < 4) continue;

                // Bỏ qua tuyệt đối các dòng rác status bar, nút bấm giao diện, menu và văn bản hành chính trường học
                if (UniversalScheduleParser.isNoiseLine(lineTrim) || UniversalScheduleParser.isInstitutionalBoilerplate(lineTrim)) {
                    continue;
                }

                // Bỏ qua các nhãn huy hiệu (VD: • Lịch học, Lịch trực tuyến) đứng riêng lẻ
                if (UniversalScheduleParser.extractCategoryBadge(lineTrim) != null) {
                    continue;
                }

                // Bỏ qua các dòng chỉ chứa nhãn thuộc tính gãy dòng
                String lowerOnly = lineTrim.toLowerCase(Locale.getDefault());
                if (lowerOnly.startsWith("tiết:") || lowerOnly.startsWith("phòng:") || lowerOnly.startsWith("giảng viên:") || lowerOnly.startsWith("gv:")) {
                    continue;
                }

                // Nếu là CV/hồ sơ cá nhân: Bỏ qua tuyệt đối các dòng chứa thông tin cá nhân tĩnh
                if (docType == DocumentType.CV_OR_PROFILE && isStaticBiographicalLine(lineTrim)) {
                    continue;
                }

                String foundDate = extractDateString(lineTrim);
                if (!foundDate.isEmpty()) {
                    currentDateStr = foundDate;
                }

                String lower = lineTrim.toLowerCase(Locale.getDefault());
                // Bỏ qua các dòng tiêu đề bảng hoặc tiêu đề báo cáo
                if (lower.equals("stt")
                        || (lower.contains("stt") && (lower.contains("tên sự kiện") || lower.contains("ngày tổ chức") || lower.contains("nội dung") || lower.contains("đơn vị")))
                        || lower.startsWith("báo cáo lịch giảng dạy")
                        || lower.startsWith("báo cáo đầy đủ lịch")
                        || lower.startsWith("ứng dụng lectuplan")
                        || lower.startsWith("xuất từ ứng dụng")
                        || lower.startsWith("tổng cộng:")
                        || lower.startsWith("giảng viên lập báo cáo")
                        || lower.equals("(ký và ghi rõ họ tên)")
                        || lower.matches("^(thứ\\s+[a-zà-ỹ0-9]+\\s*,?\\s*)?\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{4}$")) {
                    continue;
                }

                // Làm sạch chuỗi tên sự kiện (bỏ phần STT đầu dòng và ngày tháng nếu đứng đầu dòng)
                String cleanedLine = lineTrim
                        .replaceAll("^\\|\\s*", "")
                        .replaceAll("\\s*\\|$", "")
                        .replaceFirst("^\\d{1,3}\\s*[.|:-]?\\s+", "")
                        .trim();

                String organizer = defaultSourceOrganizer;
                if (cleanedLine.contains("|")) {
                    String[] parts = cleanedLine.split("\\|");
                    List<String> validParts = new ArrayList<>();
                    for (String p : parts) {
                        String pt = p.trim();
                        if (pt.isEmpty()) continue;
                        if (pt.matches("^\\d{1,3}$")) continue; // cột STT / Số tiết
                        if (pt.toLowerCase(Locale.getDefault()).matches("^tuần\\s*\\d+$")) continue; // cột Tuần
                        if (pt.toLowerCase(Locale.getDefault()).matches("^hk[123].*$")) continue; // cột Học kỳ
                        if (!extractDateString(pt).isEmpty() && pt.length() <= 24) continue; // cột Ngày thuần túy
                        validParts.add(pt);
                    }
                    if (!validParts.isEmpty()) {
                        boolean allTimeRanges = true;
                        for (String vp : validParts) {
                            if (!vp.matches(".*\\d{1,2}h\\d{2}.*")) {
                                allTimeRanges = false;
                                break;
                            }
                        }
                        if (allTimeRanges && validParts.size() > 1) {
                            cleanedLine = String.join(" • ", validParts);
                        } else {
                            cleanedLine = validParts.get(0);
                            if (validParts.size() >= 2) {
                                String lastPart = validParts.get(validParts.size() - 1);
                                if (lastPart.toLowerCase(Locale.getDefault()).matches(".*(khoa|phòng|ban|bộ môn|trường|trung tâm|hội đồng|giảng viên|gv|ts\\.|ths\\.).*")
                                        && lastPart.length() <= 45 && docType != DocumentType.CV_OR_PROFILE) {
                                    organizer = lastPart + " (" + activeFormatLabel + ")";
                                    if (validParts.size() > 2) {
                                        cleanedLine = String.join(" - ", validParts.subList(0, validParts.size() - 1));
                                    }
                                } else {
                                    cleanedLine = String.join(" - ", validParts);
                                }
                            }
                        }
                    }
                } else if (!foundDate.isEmpty()) {
                    String withoutDate = cleanedLine
                            .replace(foundDate, "")
                            .replaceAll("(?i)thứ\\s+(hai|ba|tư|năm|sáu|bảy|chủ nhật)\\s*,?", "")
                            .replaceAll("^[,\\s-|–:]+|[,\\s-|–:]+$", "")
                            .trim();
                    if (withoutDate.length() >= 4) {
                        cleanedLine = withoutDate;
                    }
                }

                if (cleanedLine.length() >= 4) {
                    cleanedLine = normalizeVietnameseOcrText(cleanedLine).replaceAll("\\s+", " ").trim();
                    if (cleanedLine.matches("(?i)^(PH[ẢA]N|LINH MỤC|GIA ĐÌNH:?|Ký tên|Sinh Hoạt\\)?|\\\\?Sinh Hoat\\)?|adithang.*|với gia đình.*|sự hỗ trợ đặc biệt.*)$")) {
                        continue;
                    }

                    // Kiểm tra xem dòng này có đặc trưng sự kiện lịch biểu hay không (có khung giờ/tiết, phòng học, ngày tháng hoặc từ khóa sự vụ)
                    boolean hasPipe = lineTrim.contains("|");
                    boolean hasEventSignals = hasPipe
                            || !foundDate.isEmpty()
                            || UniversalScheduleParser.extractPeriodRange(lineTrim) != null
                            || UniversalScheduleParser.extractTimeRange(lineTrim) != null
                            || UniversalScheduleParser.extractRoom(lineTrim) != null
                            || cleanedLine.toLowerCase(Locale.getDefault()).matches(".*\\b(?:họp|báo cáo|tập huấn|khai mạc|bế mạc|hội đồng|chấm thi|coi thi|thi kết thúc|dạy bù|sinh hoạt|học phần|chuyên đề|môn học)\\b.*");

                    if (!hasEventSignals) {
                        continue;
                    }

                    String title = cleanedLine;
                    dbHelper.insertEvent(stt++, title, currentDateStr, organizer);
                    extractedSummaries.add("[" + currentDateStr + "] " + title);
                    count++;
                }
            }
            dbHelper.normalizeSttNumbers();
            writableDb.setTransactionSuccessful();
        } finally {
            writableDb.endTransaction();
        }

        if (count == 0 && docType == DocumentType.CV_OR_PROFILE) {
            extractedSummaries.add("ℹ️ Tài liệu được nhận diện là Hồ sơ/CV cá nhân. Ứng dụng đã bảo vệ lịch biểu bằng cách không tự động tạo lịch dạy hay gán tiết từ các thông tin lý lịch cá nhân tĩnh.");
        }

        String historyBadgeType = isHandwritten ? "NHẬP FILE (VIẾT TAY • " + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")"
                : "NHẬP FILE (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
        dbHelper.addFileHistory(fileName, historyBadgeType, count);
        refreshFileHistory();
        playSuccessSound();
        refreshData();
        refreshTab2CalendarEvents();

        showFileAnalysisReportDialog(fileName, activeFormatLabel, activeExtractionMethod, isHandwritten, count, extractedSummaries);
    }

    // Tự động tách dòng văn bản dài theo độ rộng cột để in PDF đầy đủ 100% nội dung (không bao giờ cắt "..." )
    private List<String> wrapPdfTextToLines(String text, android.graphics.Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] paragraphs = text.trim().split("\\r?\\n");
        for (String para : paragraphs) {
            String[] words = para.trim().split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            for (String word : words) {
                if (currentLine.length() == 0) {
                    // Nếu 1 từ quá dài vượt maxWidth thì tách theo ký tự
                    if (paint.measureText(word) > maxWidth) {
                        StringBuilder charBuf = new StringBuilder();
                        for (int c = 0; c < word.length(); c++) {
                            char ch = word.charAt(c);
                            if (paint.measureText(charBuf.toString() + ch) > maxWidth && charBuf.length() > 0) {
                                lines.add(charBuf.toString());
                                charBuf.setLength(0);
                            }
                            charBuf.append(ch);
                        }
                        currentLine.append(charBuf);
                    } else {
                        currentLine.append(word);
                    }
                } else {
                    String candidate = currentLine.toString() + " " + word;
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine.append(" ").append(word);
                    } else {
                        lines.add(currentLine.toString());
                        currentLine.setLength(0);
                        if (paint.measureText(word) > maxWidth) {
                            StringBuilder charBuf = new StringBuilder();
                            for (int c = 0; c < word.length(); c++) {
                                char ch = word.charAt(c);
                                if (paint.measureText(charBuf.toString() + ch) > maxWidth && charBuf.length() > 0) {
                                    lines.add(charBuf.toString());
                                    charBuf.setLength(0);
                                }
                                charBuf.append(ch);
                            }
                            currentLine.append(charBuf);
                        } else {
                            currentLine.append(word);
                        }
                    }
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private void drawPdfTableHeader(android.graphics.Canvas canvas, android.graphics.Paint paint, int topY) {
        paint.setColor(Color.parseColor("#1E3A8A"));
        paint.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawRect(30, topY, 565, topY + 26, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("STT", 34, topY + 17, paint);
        canvas.drawText("Ngày & Giờ", 62, topY + 17, paint);
        canvas.drawText("Nội dung công việc & Địa điểm", 134, topY + 17, paint);
        canvas.drawText("Phân loại", 344, topY + 17, paint);
        canvas.drawText("Đơn vị & Tuần học", 429, topY + 17, paint);
    }

    private void exportPdfReportProcess() {
        try {
            List<EventModel> list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, "Không có lịch biểu nào để xuất báo cáo PDF!", Toast.LENGTH_SHORT).show();
                return;
            }

            android.graphics.pdf.PdfDocument pdfDoc = new android.graphics.pdf.PdfDocument();
            int pageNumber = 1;
            android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
            android.graphics.pdf.PdfDocument.Page page = pdfDoc.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();

            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);

            paint.setColor(Color.parseColor("#1E3A8A"));
            paint.setTextSize(15f);
            paint.setFakeBoldText(true);
            canvas.drawText("BÁO CÁO ĐẦY ĐỦ LỊCH GIẢNG DẠY & NỘI DUNG TÀI LIỆU", 30, 42, paint);

            paint.setColor(Color.parseColor("#64748B"));
            paint.setTextSize(9.5f);
            paint.setFakeBoldText(false);
            String printDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("Ứng dụng LectuPlan • Ngày lập báo cáo: " + printDate + " • Tổng số: " + list.size() + " mục (Hiển thị đầy đủ 100%)", 30, 60, paint);

            paint.setColor(Color.parseColor("#CBD5E1"));
            paint.setStrokeWidth(1f);
            canvas.drawLine(30, 70, 565, 70, paint);

            drawPdfTableHeader(canvas, paint, 80);

            int y = 106;
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            // Khổ A4 (595pt x 842pt), lề 30pt..565pt (rộng 535pt)
            // Cột STT: 30..58 (28pt) | Cột Ngày & Giờ: 58..130 (72pt) | Cột Nội dung: 130..340 (210pt) | Cột Phân loại: 340..425 (85pt) | Cột Đơn vị: 425..565 (140pt)
            float dateMaxWidth = 66f;
            float titleMaxWidth = 202f;
            float catMaxWidth = 78f;
            float orgMaxWidth = 132f;
            int lineHeight = 13;

            for (int i = 0; i < list.size(); i++) {
                EventModel e = list.get(i);

                paint.setTextSize(9f);
                paint.setFakeBoldText(false);

                String dateStr = e.getDate() != null ? e.getDate() : "";
                try {
                    Date d = sdfInput.parse(dateStr);
                    if (d != null) dateStr = sdfOutput.format(d);
                } catch (Exception ignored) {}

                String timeStr = (e.getStartTime() != null && !e.getStartTime().isEmpty())
                        ? (e.getStartTime() + " - " + e.getEndTime()) : "";
                String dateText = dateStr + (timeStr.isEmpty() ? "" : "\n" + timeStr);

                String titleText = e.getTitle() != null ? e.getTitle() : "";
                String roomInfo = "";
                if (e.getRoom() != null && !e.getRoom().trim().isEmpty()) {
                    roomInfo += "Phòng " + e.getRoom().trim();
                }
                if (e.getCampus() != null && !e.getCampus().trim().isEmpty()) {
                    if (!roomInfo.isEmpty()) roomInfo += " • ";
                    roomInfo += e.getCampus().trim();
                }
                if (!roomInfo.isEmpty()) {
                    titleText += "\n(" + roomInfo + ")";
                }

                String catText = e.getScheduleCategory();

                String orgText = (e.getOrganizer() != null ? e.getOrganizer() : "")
                        + "\n(Tuần " + e.getWeek() + " • Năm " + e.getYear() + ")";

                List<String> dateLines = wrapPdfTextToLines(dateText, paint, dateMaxWidth);
                List<String> titleLines = wrapPdfTextToLines(titleText, paint, titleMaxWidth);
                List<String> catLines = wrapPdfTextToLines(catText, paint, catMaxWidth);
                List<String> orgLines = wrapPdfTextToLines(orgText, paint, orgMaxWidth);

                int maxLines = Math.max(dateLines.size(), Math.max(titleLines.size(), Math.max(catLines.size(), orgLines.size())));
                int rowHeight = Math.max(26, maxLines * lineHeight + 10);

                // Nếu vượt quá chiều cao trang A4 thì sang trang mới
                if (y + rowHeight > 780) {
                    paint.setColor(Color.parseColor("#64748B"));
                    paint.setTextSize(8.5f);
                    canvas.drawText("Trang " + pageNumber, 280, 822, paint);
                    pdfDoc.finishPage(page);

                    pageNumber++;
                    pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    page = pdfDoc.startPage(pageInfo);
                    canvas = page.getCanvas();

                    paint.setColor(Color.parseColor("#64748B"));
                    paint.setTextSize(9f);
                    paint.setFakeBoldText(false);
                    canvas.drawText("BÁO CÁO LỊCH GIẢNG DẠY & CÔNG VIỆC GIẢNG VIÊN (Tiếp theo - Trang " + pageNumber + ")", 30, 34, paint);

                    drawPdfTableHeader(canvas, paint, 44);
                    y = 70;
                }

                // Tô nền xen kẽ cho hàng
                if (i % 2 == 1) {
                    paint.setColor(Color.parseColor("#F8FAFC"));
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    canvas.drawRect(30, y, 565, y + rowHeight, paint);
                }

                // Kẻ khung bảng mảnh cho hàng
                paint.setColor(Color.parseColor("#E2E8F0"));
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(0.6f);
                canvas.drawRect(30, y, 565, y + rowHeight, paint);
                canvas.drawLine(58, y, 58, y + rowHeight, paint);
                canvas.drawLine(130, y, 130, y + rowHeight, paint);
                canvas.drawLine(340, y, 340, y + rowHeight, paint);
                canvas.drawLine(425, y, 425, y + rowHeight, paint);

                // In nội dung từng cột
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTextSize(9f);
                paint.setFakeBoldText(true);
                canvas.drawText(String.valueOf(i + 1), 34, y + 15, paint);

                paint.setFakeBoldText(false);
                for (int l = 0; l < dateLines.size(); l++) {
                    canvas.drawText(dateLines.get(l), 62, y + 15 + (l * lineHeight), paint);
                }

                paint.setColor(Color.parseColor("#0F172A"));
                for (int l = 0; l < titleLines.size(); l++) {
                    canvas.drawText(titleLines.get(l), 134, y + 15 + (l * lineHeight), paint);
                }

                paint.setColor(Color.parseColor("#2563EB"));
                for (int l = 0; l < catLines.size(); l++) {
                    canvas.drawText(catLines.get(l), 344, y + 15 + (l * lineHeight), paint);
                }

                paint.setColor(Color.parseColor("#334155"));
                for (int l = 0; l < orgLines.size(); l++) {
                    canvas.drawText(orgLines.get(l), 429, y + 15 + (l * lineHeight), paint);
                }

                y += rowHeight;
            }

            // Nếu phần chữ ký cuối báo cáo bị sát mép dưới trang thì sang trang mới
            if (y + 85 > 800) {
                paint.setColor(Color.parseColor("#64748B"));
                paint.setTextSize(8.5f);
                canvas.drawText("Trang " + pageNumber, 280, 822, paint);
                pdfDoc.finishPage(page);

                pageNumber++;
                pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                page = pdfDoc.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTextSize(10f);
            paint.setFakeBoldText(true);
            canvas.drawText("TỔNG CỘNG: ĐÃ XUẤT ĐẦY ĐỦ " + list.size() + "/" + list.size() + " LỊCH & NỘI DUNG CÔNG VIỆC", 30, y + 24, paint);

            canvas.drawText("GIẢNG VIÊN LẬP BÁO CÁO", 400, y + 44, paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(9f);
            canvas.drawText("(Ký và ghi rõ họ tên)", 412, y + 58, paint);

            paint.setColor(Color.parseColor("#64748B"));
            paint.setTextSize(8.5f);
            canvas.drawText("Trang " + pageNumber + "/" + pageNumber, 275, 822, paint);

            pdfDoc.finishPage(page);

            String fileName = "BaoCao_LectuPlan_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".pdf";
            File appPdfFile = new File(getHistoryStorageDir(), fileName);
            try (FileOutputStream fosApp = new FileOutputStream(appPdfFile)) {
                pdfDoc.writeTo(fosApp);
                fosApp.flush();
            } catch (Exception ignored) {}

            try {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File pdfFile = new File(path, fileName);
                FileOutputStream fos = new FileOutputStream(pdfFile);
                pdfDoc.writeTo(fos);
                fos.flush();
                fos.close();
                android.media.MediaScannerConnection.scanFile(this, new String[]{pdfFile.getAbsolutePath()}, new String[]{"application/pdf"}, null);
            } catch (Exception ignored) {}
            pdfDoc.close();

            dbHelper.addFileHistory(fileName, "XUẤT BÁO CÁO PDF (" + pageNumber + " TRANG)", list.size());
            refreshFileHistory();
            playSuccessSound();

            Toast.makeText(this, "✅ Đã xuất đầy đủ " + list.size() + " mục (" + pageNumber + " trang PDF A4) vào thư mục Download!\n" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất file PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCustomConfirmDialog(
            String iconEmoji,
            String title,
            String message,
            String confirmText,
            int confirmColorHex,
            Runnable onConfirmAction) {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_action, null);
        TextView tvIcon = view.findViewById(R.id.tvDialogConfirmIcon);
        TextView tvTitle = view.findViewById(R.id.tvDialogConfirmTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogConfirmMessage);
        Button btnCancel = view.findViewById(R.id.btnDialogConfirmCancel);
        Button btnConfirm = view.findViewById(R.id.btnDialogConfirmAction);

        if (tvIcon != null) tvIcon.setText(iconEmoji);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);
        if (btnConfirm != null) {
            btnConfirm.setText(confirmText);
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(confirmColorHex));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (onConfirmAction != null) {
                    onConfirmAction.run();
                }
            });
        }

        dialog.show();
    }

    private void handleBatchDeleteFileHistory() {
        if (fileHistoryAdapter == null) return;
        List<FileHistoryModel> selectedFiles = fileHistoryAdapter.getSelectedItems();
        if (selectedFiles.isEmpty()) {
            Toast.makeText(this, "Vui lòng tích chọn ít nhất 1 tệp để xóa!", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalEvents = 0;
        for (FileHistoryModel file : selectedFiles) {
            if (file.getFileName() != null) {
                totalEvents += dbHelper.countEventsByFileName(file.getFileName().trim());
            }
        }

        int fileCount = selectedFiles.size();
        final int finalTotalEvents = totalEvents;

        if (finalTotalEvents <= 0) {
            showCustomConfirmDialog(
                    "🗑️",
                    "XÓA " + fileCount + " TỆP ĐÃ CHỌN",
                    "Bạn có chắc muốn xóa " + fileCount + " tệp đã chọn khỏi ứng dụng?",
                    "🔴 Xóa " + fileCount + " tệp",
                    Color.parseColor("#DC2626"),
                    () -> {
                        List<Integer> ids = new ArrayList<>();
                        for (FileHistoryModel file : selectedFiles) {
                            ids.add(file.getId());
                            if (file.getFileName() != null) {
                                deletePhysicalImportedFile(file.getFileName().trim());
                            }
                        }
                        dbHelper.deleteMultipleFileHistories(ids);
                        playSuccessSound();
                        fileHistoryAdapter.setMultiSelectMode(false);
                        View bar = findViewById(R.id.layoutFileMultiSelectBar);
                        if (bar != null) bar.setVisibility(View.GONE);
                        refreshFileHistory();
                        Toast.makeText(MainActivity.this, "Đã xóa thành công " + fileCount + " tệp!", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_file_events, null);
        TextView tvDialogTitle = view.findViewById(R.id.tvDeleteDialogTitle);
        TextView tvDeleteFileName = view.findViewById(R.id.tvDeleteFileName);
        TextView tvDeleteEventCount = view.findViewById(R.id.tvDeleteEventCount);
        Button btnDeleteFileAndEvents = view.findViewById(R.id.btnDeleteFileAndEvents);
        Button btnDeleteHistoryOnly = view.findViewById(R.id.btnDeleteHistoryOnly);
        TextView btnCancel = view.findViewById(R.id.btnCancelDeleteFile);

        if (tvDialogTitle != null) tvDialogTitle.setText("XÓA " + fileCount + " TỆP VÀ LỊCH ĐÃ NHẬP");
        if (tvDeleteFileName != null) tvDeleteFileName.setText("Đã chọn: " + fileCount + " tệp");
        if (tvDeleteEventCount != null) {
            tvDeleteEventCount.setText("⚠️ Phát hiện có tổng cộng " + finalTotalEvents + " buổi / lịch trình trong ứng dụng được nhập từ các tệp này.\n\nBạn có muốn xóa sạch toàn bộ nội dung của các tệp này khỏi ứng dụng không?");
        }
        if (btnDeleteFileAndEvents != null) {
            btnDeleteFileAndEvents.setText("🔴 Xóa " + fileCount + " tệp & sạch toàn bộ " + finalTotalEvents + " lịch");
        }
        if (btnDeleteHistoryOnly != null) {
            btnDeleteHistoryOnly.setText("Chỉ xóa " + fileCount + " tệp khỏi nhật ký (giữ lại lịch)");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnDeleteFileAndEvents != null) {
            btnDeleteFileAndEvents.setOnClickListener(v -> {
                dialog.dismiss();
                int totalDeletedEvents = 0;
                List<Integer> ids = new ArrayList<>();
                for (FileHistoryModel file : selectedFiles) {
                    ids.add(file.getId());
                    if (file.getFileName() != null) {
                        String fn = file.getFileName().trim();
                        totalDeletedEvents += dbHelper.deleteEventsByFileName(fn);
                        deletePhysicalImportedFile(fn);
                    }
                }
                dbHelper.deleteMultipleFileHistories(ids);
                playSuccessSound();
                fileHistoryAdapter.setMultiSelectMode(false);
                View bar = findViewById(R.id.layoutFileMultiSelectBar);
                if (bar != null) bar.setVisibility(View.GONE);
                refreshData();
                refreshTab2CalendarEvents();
                updateChartTab();
                refreshFileHistory();
                Toast.makeText(MainActivity.this, "✅ Đã xóa " + fileCount + " tệp và toàn bộ " + totalDeletedEvents + " lịch trình liên quan!", Toast.LENGTH_LONG).show();
            });
        }

        if (btnDeleteHistoryOnly != null) {
            btnDeleteHistoryOnly.setOnClickListener(v -> {
                dialog.dismiss();
                List<Integer> ids = new ArrayList<>();
                for (FileHistoryModel file : selectedFiles) {
                    ids.add(file.getId());
                    if (file.getFileName() != null) {
                        deletePhysicalImportedFile(file.getFileName().trim());
                    }
                }
                dbHelper.deleteMultipleFileHistories(ids);
                playSuccessSound();
                fileHistoryAdapter.setMultiSelectMode(false);
                View bar = findViewById(R.id.layoutFileMultiSelectBar);
                if (bar != null) bar.setVisibility(View.GONE);
                refreshFileHistory();
                Toast.makeText(MainActivity.this, "Đã xóa " + fileCount + " tệp khỏi nhật ký (giữ lại lịch trình trong ứng dụng).", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showDeleteFileAndEventsDialog(FileHistoryModel item) {
        if (item == null || item.getFileName() == null) return;
        String fileName = item.getFileName().trim();
        int eventCount = dbHelper.countEventsByFileName(fileName);

        if (eventCount <= 0) {
            showCustomConfirmDialog(
                    "🗑️",
                    "XÓA TỆP KHỎI ỨNG DỤNG",
                    "Bạn có chắc muốn xóa tệp '" + fileName + "' khỏi bộ nhớ ứng dụng?",
                    "🔴 Xóa tệp",
                    Color.parseColor("#DC2626"),
                    () -> {
                        dbHelper.deleteFileHistory(item.getId());
                        deletePhysicalImportedFile(fileName);
                        playSuccessSound();
                        refreshFileHistory();
                        Toast.makeText(MainActivity.this, "Đã xóa tệp khỏi ứng dụng!", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_file_events, null);
        TextView tvDialogTitle = view.findViewById(R.id.tvDeleteDialogTitle);
        TextView tvDeleteFileName = view.findViewById(R.id.tvDeleteFileName);
        TextView tvDeleteEventCount = view.findViewById(R.id.tvDeleteEventCount);
        Button btnDeleteFileAndEvents = view.findViewById(R.id.btnDeleteFileAndEvents);
        Button btnDeleteHistoryOnly = view.findViewById(R.id.btnDeleteHistoryOnly);
        TextView btnCancel = view.findViewById(R.id.btnCancelDeleteFile);

        if (tvDialogTitle != null) tvDialogTitle.setText("XÓA TỆP VÀ NỘI DUNG ĐÃ NHẬP");
        if (tvDeleteFileName != null) tvDeleteFileName.setText("Tệp: " + fileName);
        if (tvDeleteEventCount != null) {
            tvDeleteEventCount.setText("⚠️ Phát hiện có " + eventCount + " buổi / lịch trình trong ứng dụng được nhập từ tệp này.\n\nBạn có muốn xóa sạch toàn bộ nội dung của tệp này khỏi ứng dụng không?");
        }
        if (btnDeleteFileAndEvents != null) {
            btnDeleteFileAndEvents.setText("🔴 Xóa tệp & sạch toàn bộ " + eventCount + " lịch đã nhập");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnDeleteFileAndEvents != null) {
            btnDeleteFileAndEvents.setOnClickListener(v -> {
                dialog.dismiss();
                int deleted = dbHelper.deleteEventsByFileName(fileName);
                dbHelper.deleteFileHistory(item.getId());
                deletePhysicalImportedFile(fileName);
                playSuccessSound();
                refreshData();
                refreshTab2CalendarEvents();
                updateChartTab();
                refreshFileHistory();
                Toast.makeText(MainActivity.this, "✅ Đã xóa tệp và sạch toàn bộ " + deleted + " lịch trình liên quan!", Toast.LENGTH_LONG).show();
            });
        }

        if (btnDeleteHistoryOnly != null) {
            btnDeleteHistoryOnly.setOnClickListener(v -> {
                dialog.dismiss();
                dbHelper.deleteFileHistory(item.getId());
                deletePhysicalImportedFile(fileName);
                playSuccessSound();
                refreshFileHistory();
                Toast.makeText(MainActivity.this, "Đã xóa bản ghi nhật ký (giữ lại lịch trình trong ứng dụng).", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showClearAllFilesAndEventsDialog() {
        List<DatabaseHelper.SourceFileInfo> files = dbHelper.getImportedFilesSummary();
        int totalEvents = 0;
        for (DatabaseHelper.SourceFileInfo f : files) {
            totalEvents += f.eventCount;
        }

        if (totalEvents <= 0) {
            showCustomConfirmDialog(
                    "🧹",
                    "XÓA TẤT CẢ LỊCH SỬ FILE",
                    "Bạn có chắc chắn muốn xóa TOÀN BỘ nhật ký lịch sử nhập xuất file?",
                    "🔴 Xóa tất cả",
                    Color.parseColor("#DC2626"),
                    () -> {
                        dbHelper.clearAllFileHistory();
                        clearAllPhysicalImportedFiles();
                        playSuccessSound();
                        refreshFileHistory();
                        Toast.makeText(MainActivity.this, "Đã dọn sạch nhật ký lịch sử file!", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_file_events, null);
        TextView tvDialogTitle = view.findViewById(R.id.tvDeleteDialogTitle);
        TextView tvDeleteFileName = view.findViewById(R.id.tvDeleteFileName);
        TextView tvDeleteEventCount = view.findViewById(R.id.tvDeleteEventCount);
        Button btnDeleteFileAndEvents = view.findViewById(R.id.btnDeleteFileAndEvents);
        Button btnDeleteHistoryOnly = view.findViewById(R.id.btnDeleteHistoryOnly);
        TextView btnCancel = view.findViewById(R.id.btnCancelDeleteFile);

        if (tvDialogTitle != null) tvDialogTitle.setText("DỌN SẠCH TỆP & LỊCH TRÌNH ĐÃ NHẬP");
        if (tvDeleteFileName != null) tvDeleteFileName.setText("Tổng cộng: " + files.size() + " tệp • " + totalEvents + " lịch trình");
        if (tvDeleteEventCount != null) {
            tvDeleteEventCount.setText("⚠️ Có tổng cộng " + totalEvents + " buổi / lịch trình trong ứng dụng được nhập từ " + files.size() + " tệp.\n\nBạn có muốn XÓA SẠCH toàn bộ các lịch trình này hay chỉ xóa danh sách nhật ký?");
        }
        if (btnDeleteFileAndEvents != null) {
            btnDeleteFileAndEvents.setText("🔴 Xóa tất cả tệp & sạch " + totalEvents + " lịch đã nhập");
        }
        if (btnDeleteHistoryOnly != null) {
            btnDeleteHistoryOnly.setText("📄 Chỉ xóa nhật ký lịch sử (Giữ lại lịch)");
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnDeleteFileAndEvents != null) {
            btnDeleteFileAndEvents.setOnClickListener(b -> {
                dialog.dismiss();
                int deleted = dbHelper.deleteAllEventsFromAllImportedFiles();
                clearAllPhysicalImportedFiles();
                playSuccessSound();
                refreshData();
                refreshTab2CalendarEvents();
                updateChartTab();
                refreshFileHistory();
                Toast.makeText(MainActivity.this, "✅ Đã xóa sạch toàn bộ " + deleted + " lịch trình từ tất cả tệp!", Toast.LENGTH_LONG).show();
            });
        }

        if (btnDeleteHistoryOnly != null) {
            btnDeleteHistoryOnly.setOnClickListener(b -> {
                dialog.dismiss();
                dbHelper.clearAllFileHistory();
                clearAllPhysicalImportedFiles();
                playSuccessSound();
                refreshFileHistory();
                Toast.makeText(MainActivity.this, "Đã xóa nhật ký lịch sử file!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(b -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showManageImportedFilesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manage_imported_files, null);
        RecyclerView rvManage = view.findViewById(R.id.rvManageFilesList);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyManageFiles);
        Button btnDeleteAll = view.findViewById(R.id.btnDeleteAllFilesAndEvents);
        ImageButton btnClose = view.findViewById(R.id.btnCloseManageFiles);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        Runnable reloadList = new Runnable() {
            @Override
            public void run() {
                dbHelper.cleanUpGarbageCvEntries();
                dbHelper.syncPhysicalFilesIntoHistory(getHistoryStorageDir());
                dbHelper.syncOrphanFilesIntoHistory();
                List<DatabaseHelper.SourceFileInfo> files = dbHelper.getImportedFilesSummary();
                int totalEvents = 0;
                for (DatabaseHelper.SourceFileInfo f : files) totalEvents += f.eventCount;

                if (files.isEmpty()) {
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("☕ Hiện không có tệp nào được lưu trong ứng dụng.");
                    }
                    if (rvManage != null) rvManage.setVisibility(View.GONE);
                    if (btnDeleteAll != null) {
                        btnDeleteAll.setEnabled(false);
                        btnDeleteAll.setAlpha(0.4f);
                    }
                } else {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    if (rvManage != null) {
                        rvManage.setVisibility(View.VISIBLE);
                        rvManage.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        final Runnable selfRunnable = this;
                        rvManage.setAdapter(new RecyclerView.Adapter<ManageFileViewHolder>() {
                            @Override
                            public ManageFileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_file_entry, parent, false);
                                return new ManageFileViewHolder(itemView);
                            }

                            @Override
                            public void onBindViewHolder(ManageFileViewHolder holder, int position) {
                                DatabaseHelper.SourceFileInfo fileInfo = files.get(position);
                                holder.tvName.setText(fileInfo.fileName);

                                String lower = fileInfo.fileName.toLowerCase(Locale.getDefault());
                                String iconEmoji = "📄";
                                if (lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv")) iconEmoji = "📊";
                                else if (lower.endsWith(".docx") || lower.endsWith(".doc")) iconEmoji = "📝";
                                else if (lower.endsWith(".pptx") || lower.endsWith(".ppt")) iconEmoji = "📑";
                                else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) iconEmoji = "🖼️";
                                holder.tvIcon.setText(iconEmoji);

                                if (fileInfo.eventCount > 0) {
                                    holder.tvCount.setText("⚠️ " + fileInfo.eventCount + " lịch trình trong ứng dụng");
                                    holder.tvCount.setTextColor(Color.parseColor("#DC2626"));
                                } else {
                                    holder.tvCount.setText("✓ 0 lịch trình (chỉ tệp tin)");
                                    holder.tvCount.setTextColor(Color.parseColor("#10B981"));
                                }
                                holder.btnDelete.setEnabled(true);
                                holder.btnDelete.setAlpha(1.0f);

                                holder.btnDelete.setOnClickListener(v -> {
                                    String msg = fileInfo.eventCount > 0
                                            ? "Bạn có chắc muốn xóa tệp '" + fileInfo.fileName + "' cùng toàn bộ " + fileInfo.eventCount + " lịch trình đã nhập?"
                                            : "Bạn có chắc muốn xóa tệp '" + fileInfo.fileName + "' khỏi ứng dụng?";
                                    showCustomConfirmDialog(
                                            "🗑️",
                                            "XÓA TỆP & DỮ LIỆU",
                                            msg,
                                            "🔴 Xóa tệp",
                                            Color.parseColor("#DC2626"),
                                            () -> {
                                                int deleted = dbHelper.deleteEventsByFileName(fileInfo.fileName);
                                                dbHelper.deleteFileHistoryByName(fileInfo.fileName);
                                                deletePhysicalImportedFile(fileInfo.fileName);
                                                playSuccessSound();
                                                refreshData();
                                                refreshTab2CalendarEvents();
                                                updateChartTab();
                                                refreshFileHistory();
                                                Toast.makeText(MainActivity.this, "Đã xóa tệp '" + fileInfo.fileName + "'!", Toast.LENGTH_SHORT).show();
                                                selfRunnable.run();
                                            });
                                });
                            }

                            @Override
                            public int getItemCount() {
                                return files.size();
                            }
                        });
                    }

                    if (btnDeleteAll != null) {
                        btnDeleteAll.setEnabled(true);
                        btnDeleteAll.setAlpha(1.0f);
                        final int finalTotal = totalEvents;
                        btnDeleteAll.setOnClickListener(v -> {
                            String msg = finalTotal > 0
                                    ? "Bạn có chắc chắn muốn xóa TOÀN BỘ " + finalTotal + " lịch trình của tất cả " + files.size() + " tệp đã nhập vào ứng dụng?"
                                    : "Bạn có chắc chắn muốn xóa TOÀN BỘ " + files.size() + " tệp đã nhập vào ứng dụng?";
                            showCustomConfirmDialog(
                                    "🚨",
                                    "XÓA TOÀN BỘ DỮ LIỆU TẤT CẢ TỆP",
                                    msg,
                                    "🔴 XÓA TẤT CẢ SẠCH SẼ",
                                    Color.parseColor("#DC2626"),
                                    () -> {
                                        dialog.dismiss();
                                        int deleted = dbHelper.deleteAllEventsFromAllImportedFiles();
                                        clearAllPhysicalImportedFiles();
                                        playSuccessSound();
                                        refreshData();
                                        refreshTab2CalendarEvents();
                                        updateChartTab();
                                        refreshFileHistory();
                                        Toast.makeText(MainActivity.this, "✅ Đã xóa sạch toàn bộ " + deleted + " lịch trình và tất cả các tệp!", Toast.LENGTH_LONG).show();
                                    });
                        });
                    }
                }
            }
        };

        reloadList.run();
        dialog.show();
    }

    private static class ManageFileViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvCount;
        Button btnDelete;

        ManageFileViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvManageFileIcon);
            tvName = itemView.findViewById(R.id.tvManageFileName);
            tvCount = itemView.findViewById(R.id.tvManageFileCount);
            btnDelete = itemView.findViewById(R.id.btnDeleteThisFileEvents);
        }
    }

    private void showAddEventDialog() {
        showAddEventDialogWithPrefill(null, null, null);
    }

    private void showAddEventDialogWithPrefill(String prefillDateIso, String prefillStart, String prefillEnd) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        TextView tvDialogHeaderTitle = view.findViewById(R.id.tvDialogHeaderTitle);
        EditText edtStt = view.findViewById(R.id.edtStt);
        Spinner spnCategory = view.findViewById(R.id.spnCategory);
        LinearLayout layoutCustomCategory = view.findViewById(R.id.layoutCustomCategory);
        EditText edtCustomCategory = view.findViewById(R.id.edtCustomCategory);
        EditText edtTitle = view.findViewById(R.id.edtTitle);
        EditText edtDate = view.findViewById(R.id.edtDate);
        EditText edtStartTime = view.findViewById(R.id.edtStartTime);
        EditText edtEndTime = view.findViewById(R.id.edtEndTime);
        EditText edtRoom = view.findViewById(R.id.edtRoom);
        EditText edtOrganizer = view.findViewById(R.id.edtOrganizer);
        Button btnSaveDialog = view.findViewById(R.id.btnSaveDialog);
        Button btnCancelDialog = view.findViewById(R.id.btnCancelDialog);

        if (tvDialogHeaderTitle != null) {
            tvDialogHeaderTitle.setText("➕ THÊM LỊCH CÔNG VIỆC MỚI");
        }

        if (prefillDateIso != null && !prefillDateIso.isEmpty()) {
            try {
                SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date d = sdfIso.parse(prefillDateIso);
                if (d != null && edtDate != null) edtDate.setText(sdfVn.format(d));
            } catch (Exception ignored) {
                if (edtDate != null) edtDate.setText(prefillDateIso);
            }
        }
        if (prefillStart != null && edtStartTime != null) edtStartTime.setText(prefillStart);
        if (prefillEnd != null && edtEndTime != null) edtEndTime.setText(prefillEnd);

        String[] categoryDisplayOptions = new String[]{
                "👨‍🏫 Lịch dạy/học",
                "🏛️ Lịch sự kiện",
                "⚡ Lịch phát sinh",
                "📝 Lịch khác (tự đặt tên)"
        };

        if (spnCategory != null) {
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_chart_spinner, categoryDisplayOptions);
            catAdapter.setDropDownViewResource(R.layout.item_chart_spinner_dropdown);
            spnCategory.setAdapter(catAdapter);

            spnCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    if (position == 3) {
                        if (layoutCustomCategory != null) {
                            layoutCustomCategory.setVisibility(View.VISIBLE);
                            if (edtCustomCategory != null) edtCustomCategory.requestFocus();
                        }
                    } else {
                        if (layoutCustomCategory != null) layoutCustomCategory.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        int nextStt = adapter != null ? (adapter.getItemCount() + 1) : 1;
        edtStt.setText(String.valueOf(nextStt));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        edtDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (picker, y, m, d) -> {
                String dateFormatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                edtDate.setText(dateFormatted);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        edtStartTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new android.app.TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                edtStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        edtEndTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new android.app.TimePickerDialog(this, (view12, hourOfDay, minute) -> {
                edtEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        if (btnCancelDialog != null) {
            btnCancelDialog.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSaveDialog != null) {
            btnSaveDialog.setOnClickListener(v -> {
                int stt = nextStt;
                try { stt = Integer.parseInt(edtStt.getText().toString().trim()); } catch (Exception ignored) {}
                String title = edtTitle.getText().toString().trim();
                String date = edtDate.getText().toString().trim();
                String startTime = edtStartTime.getText().toString().trim();
                String endTime = edtEndTime.getText().toString().trim();
                String room = edtRoom.getText().toString().trim();
                String org = edtOrganizer.getText().toString().trim();

                if (title.isEmpty() || date.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập Tên sự kiện và Ngày tổ chức!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                saveEvent(nextStt, edtStt, title, room, startTime, endTime, date, org, spnCategory, edtCustomCategory);
                dialog.dismiss();
            });
        }

        dialog.show();
    }
    
    private void saveEvent(int nextStt, EditText edtStt, String title, String room, String startTime, String endTime, String date, String org, Spinner spnCategory, EditText edtCustomCategory) {
        int stt = nextStt;
        try { stt = Integer.parseInt(edtStt.getText().toString().trim()); } catch (Exception ignored) {}
        
        String roomInput = room != null ? room.trim() : "";
        String cleanTitle = title != null ? title.trim() : "";

        if (roomInput.isEmpty()) {
            roomInput = CampusBufferHelper.extractGenericRoom(cleanTitle);
        }

        cleanTitle = EventModel.cleanDisplayTitle(cleanTitle);
        StringBuilder savedTitle = new StringBuilder(cleanTitle);

        if (!roomInput.isEmpty()) {
            savedTitle.append(" - ").append(roomInput);
        }

        if (startTime != null && !startTime.trim().isEmpty()) {
            String startStr = startTime.trim();
            String endStr = (endTime != null && !endTime.trim().isEmpty()) ? endTime.trim() : "";
            if (!endStr.isEmpty()) {
                savedTitle.append(" [").append(startStr).append(" - ").append(endStr).append("]");
            } else {
                savedTitle.append(" [").append(startStr).append("]");
            }
        }

        int selectedPos = spnCategory != null ? spnCategory.getSelectedItemPosition() : 0;
        String category;
        if (selectedPos == 0) {
            category = EventModel.CAT_TEACHING;
        } else if (selectedPos == 1) {
            category = EventModel.CAT_EVENT;
        } else if (selectedPos == 2) {
            category = EventModel.CAT_INCIDENT;
        } else {
            String customName = (edtCustomCategory != null) ? edtCustomCategory.getText().toString().trim() : "";
            category = customName.isEmpty() ? "Lịch khác" : customName;
        }

        String isoDate = dbHelper.parseToIsoDate(date);
        dbHelper.insertEvent(stt, savedTitle.toString(), date, org.isEmpty() ? "Lịch nhập thủ công" : org, category);

        SimpleDateFormat sdfToday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayIso = sdfToday.format(Calendar.getInstance().getTime());

        boolean isViewingToday = (currentCalendarMode == 0)
                || isFilteringToday
                || (edtSearch != null && todayIso.equals(edtSearch.getText().toString().trim()));

        if (isoDate != null && !isoDate.trim().isEmpty()) {
            selectedTab2DateIso = isoDate;
        }

        if (isViewingToday) {
            currentCalendarMode = 0;
            isFilteringToday = true;
            if (edtSearch != null) {
                edtSearch.setText(todayIso);
            }
            updateChipUi(R.id.chipToday);
        }

        playSuccessSound();
        refreshAllTabs();
        scrollToEvent(stt, savedTitle.toString(), isoDate);
        Toast.makeText(this, "Đã thêm lịch mới thành công!", Toast.LENGTH_SHORT).show();
    }

    private void showEditEventDialog(EventModel event) {
        if (event == null) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        TextView tvDialogHeaderTitle = view.findViewById(R.id.tvDialogHeaderTitle);
        EditText edtStt = view.findViewById(R.id.edtStt);
        Spinner spnCategory = view.findViewById(R.id.spnCategory);
        LinearLayout layoutCustomCategory = view.findViewById(R.id.layoutCustomCategory);
        EditText edtCustomCategory = view.findViewById(R.id.edtCustomCategory);
        EditText edtTitle = view.findViewById(R.id.edtTitle);
        EditText edtDate = view.findViewById(R.id.edtDate);
        EditText edtStartTime = view.findViewById(R.id.edtStartTime);
        EditText edtEndTime = view.findViewById(R.id.edtEndTime);
        EditText edtRoom = view.findViewById(R.id.edtRoom);
        EditText edtOrganizer = view.findViewById(R.id.edtOrganizer);
        Button btnSaveDialog = view.findViewById(R.id.btnSaveDialog);
        Button btnCancelDialog = view.findViewById(R.id.btnCancelDialog);

        if (tvDialogHeaderTitle != null) {
            tvDialogHeaderTitle.setText("✏️ CHỈNH SỬA LỊCH CÔNG VIỆC");
        }

        String[] categoryDisplayOptions = new String[]{
                "👨‍🏫 Lịch dạy/học",
                "🏛️ Lịch sự kiện",
                "⚡ Lịch phát sinh",
                "📝 Lịch khác (tự đặt tên)"
        };

        if (spnCategory != null) {
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_chart_spinner, categoryDisplayOptions);
            catAdapter.setDropDownViewResource(R.layout.item_chart_spinner_dropdown);
            spnCategory.setAdapter(catAdapter);

            String currentCat = event.getCategory();
            if (currentCat == null || currentCat.trim().isEmpty()) {
                currentCat = event.getScheduleCategory();
            }

            if (EventModel.CAT_TEACHING.equalsIgnoreCase(currentCat)) {
                spnCategory.setSelection(0);
                if (layoutCustomCategory != null) layoutCustomCategory.setVisibility(View.GONE);
            } else if (EventModel.CAT_EVENT.equalsIgnoreCase(currentCat)) {
                spnCategory.setSelection(1);
                if (layoutCustomCategory != null) layoutCustomCategory.setVisibility(View.GONE);
            } else if (EventModel.CAT_INCIDENT.equalsIgnoreCase(currentCat)) {
                spnCategory.setSelection(2);
                if (layoutCustomCategory != null) layoutCustomCategory.setVisibility(View.GONE);
            } else {
                spnCategory.setSelection(3);
                if (layoutCustomCategory != null) layoutCustomCategory.setVisibility(View.VISIBLE);
                if (edtCustomCategory != null) {
                    if (!EventModel.CAT_MANUAL_OTHER.equalsIgnoreCase(currentCat)
                            && !EventModel.LEGACY_CAT_MANUAL_OTHER.equalsIgnoreCase(currentCat)
                            && !"Lịch khác".equalsIgnoreCase(currentCat)) {
                        edtCustomCategory.setText(currentCat);
                    } else {
                        edtCustomCategory.setText("");
                    }
                }
            }

            spnCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    if (position == 3) {
                        if (layoutCustomCategory != null) {
                            layoutCustomCategory.setVisibility(View.VISIBLE);
                            if (edtCustomCategory != null) edtCustomCategory.requestFocus();
                        }
                    } else {
                        if (layoutCustomCategory != null) layoutCustomCategory.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        edtStt.setText(String.valueOf(event.getStt()));
        
        // Hiển thị tên sự kiện sạch (không đính kèm chuỗi phòng và thời gian dư thừa)
        edtTitle.setText(EventModel.cleanDisplayTitle(event.getTitle()));

        if (event.getStartTime() != null) edtStartTime.setText(event.getStartTime());
        if (event.getEndTime() != null) edtEndTime.setText(event.getEndTime());
        
        String currentRoom = event.getRoom();
        if (currentRoom == null || currentRoom.trim().isEmpty()) {
            EventModel.EventFieldBreakdown bd = event.getBreakdown();
            currentRoom = bd.room;
        }
        if (currentRoom != null && edtRoom != null) {
            edtRoom.setText(currentRoom.trim());
        }

        String displayDate = event.getDate();
        try {
            SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdfIso.parse(event.getDate());
            if (d != null) {
                SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                displayDate = sdfVn.format(d);
            }
        } catch (Exception ignored) {}
        edtDate.setText(displayDate);

        edtOrganizer.setText(event.getOrganizer());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        edtDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date d = sdfVn.parse(edtDate.getText().toString().trim());
                if (d != null) cal.setTime(d);
            } catch (Exception ignored) {
                try {
                    SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date d = sdfIso.parse(event.getDate());
                    if (d != null) cal.setTime(d);
                } catch (Exception ignored2) {}
            }

            new DatePickerDialog(this, (picker, y, m, d) -> {
                String dateFormatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                edtDate.setText(dateFormatted);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        edtStartTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            String st = edtStartTime.getText().toString().trim();
            if(!st.isEmpty() && st.contains(":")) {
                try {
                    String[] parts = st.split(":");
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                    cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
                } catch (Exception ignored){}
            }
            new android.app.TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                edtStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        edtEndTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            String et = edtEndTime.getText().toString().trim();
            if(!et.isEmpty() && et.contains(":")) {
                try {
                    String[] parts = et.split(":");
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                    cal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
                } catch (Exception ignored){}
            }
            new android.app.TimePickerDialog(this, (view12, hourOfDay, minute) -> {
                edtEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        if (btnCancelDialog != null) {
            btnCancelDialog.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSaveDialog != null) {
            btnSaveDialog.setText("💾 LƯU THAY ĐỔI");
            btnSaveDialog.setOnClickListener(v -> {
                int stt = event.getStt();
                try { stt = Integer.parseInt(edtStt.getText().toString().trim()); } catch (Exception ignored) {}
                String title = edtTitle.getText().toString().trim();
                String date = edtDate.getText().toString().trim();
                String startTime = edtStartTime.getText().toString().trim();
                String endTime = edtEndTime.getText().toString().trim();
                String room = edtRoom.getText().toString().trim();
                String org = edtOrganizer.getText().toString().trim();

                if (title.isEmpty() || date.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập Tên sự kiện và Ngày tổ chức!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                updateEvent(event, edtStt, title, room, startTime, endTime, date, org, spnCategory, edtCustomCategory);
                dialog.dismiss();
            });
        }

        dialog.show();
    }
    
    private void updateEvent(EventModel event, EditText edtStt, String title, String room, String startTime, String endTime, String date, String org, Spinner spnCategory, EditText edtCustomCategory) {
        int stt = event != null ? event.getStt() : 1;
        try { stt = Integer.parseInt(edtStt.getText().toString().trim()); } catch (Exception ignored) {}
        
        String roomInput = room != null ? room.trim() : "";
        String cleanTitle = title != null ? title.trim() : "";

        if (roomInput.isEmpty()) {
            roomInput = CampusBufferHelper.extractGenericRoom(cleanTitle);
        }

        cleanTitle = EventModel.cleanDisplayTitle(cleanTitle);
        StringBuilder savedTitle = new StringBuilder(cleanTitle);

        if (!roomInput.isEmpty()) {
            savedTitle.append(" - ").append(roomInput);
        }

        if (startTime != null && !startTime.trim().isEmpty()) {
            String startStr = startTime.trim();
            String endStr = (endTime != null && !endTime.trim().isEmpty()) ? endTime.trim() : "";
            if (!endStr.isEmpty()) {
                savedTitle.append(" [").append(startStr).append(" - ").append(endStr).append("]");
            } else {
                savedTitle.append(" [").append(startStr).append("]");
            }
        }

        int selectedPos = spnCategory != null ? spnCategory.getSelectedItemPosition() : 0;
        String category;
        if (selectedPos == 0) {
            category = EventModel.CAT_TEACHING;
        } else if (selectedPos == 1) {
            category = EventModel.CAT_EVENT;
        } else if (selectedPos == 2) {
            category = EventModel.CAT_INCIDENT;
        } else {
            String customName = (edtCustomCategory != null) ? edtCustomCategory.getText().toString().trim() : "";
            category = customName.isEmpty() ? "Lịch khác" : customName;
        }

        String isoDate = dbHelper.parseToIsoDate(date);
        String organizerName = org.isEmpty() ? "Nhà trường" : org;

        if (event != null && event.getId() > 0) {
            dbHelper.updateEvent(event.getId(), stt, savedTitle.toString(), date, organizerName, category);
        } else {
            dbHelper.insertEvent(stt, savedTitle.toString(), date, organizerName, category);
        }

        SimpleDateFormat sdfToday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayIso = sdfToday.format(Calendar.getInstance().getTime());

        boolean isViewingToday = (currentCalendarMode == 0)
                || isFilteringToday
                || (edtSearch != null && todayIso.equals(edtSearch.getText().toString().trim()));

        if (isoDate != null && !isoDate.trim().isEmpty()) {
            selectedTab2DateIso = isoDate;
        }

        if (isViewingToday) {
            currentCalendarMode = 0;
            isFilteringToday = true;
            if (edtSearch != null) {
                edtSearch.setText(todayIso);
            }
            updateChipUi(R.id.chipToday);
        }

        playSuccessSound();
        refreshAllTabs();
        scrollToEvent(stt, savedTitle.toString(), isoDate);
        Toast.makeText(this, "Đã cập nhật lịch thành công!", Toast.LENGTH_SHORT).show();
    }

    private void updateChipUi(int selectedChipId) {
        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipTeaching = findViewById(R.id.chipTeaching);
        TextView chipMeeting = findViewById(R.id.chipMeeting);
        TextView chipIncident = findViewById(R.id.chipIncident);
        TextView chipManual = findViewById(R.id.chipManual);
        TextView chipToday = findViewById(R.id.chipToday);
        TextView chipFreeTime = findViewById(R.id.chipFreeTime);

        TextView[] allChips = new TextView[]{chipAll, chipTeaching, chipMeeting, chipIncident, chipManual, chipToday, chipFreeTime};
        for (TextView c : allChips) {
            if (c != null) {
                if (c.getId() == selectedChipId) {
                    c.setBackgroundResource(R.drawable.bg_chip_selected);
                    c.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    c.setBackgroundResource(R.drawable.bg_chip_unselected);
                    c.setTextColor(Color.parseColor("#334155"));
                }
            }
        }
    }

    private void scrollToEvent(int stt, String title, String isoDate) {
        if (rvEvents == null || adapter == null || adapter.getEventList() == null) return;
        List<EventModel> list = adapter.getEventList();
        for (int i = 0; i < list.size(); i++) {
            EventModel em = list.get(i);
            if (em == null) continue;
            boolean matchDate = (isoDate != null && isoDate.equals(em.getDate()));
            boolean matchTitle = (title != null && (title.equals(em.getTitle()) || EventModel.cleanDisplayTitle(title).equals(EventModel.cleanDisplayTitle(em.getTitle()))));
            if (matchDate && matchTitle) {
                final int pos = i;
                rvEvents.post(() -> rvEvents.scrollToPosition(pos));
                return;
            }
        }
        if (!list.isEmpty()) {
            rvEvents.post(() -> rvEvents.scrollToPosition(0));
        }
    }

    private void importExcelFile(Uri fileUri) {
        try {
            String fileName = resolveFileName(fileUri, "Import", ".xlsx");

            InputStream is = getContentResolver().openInputStream(fileUri);
            if (is == null) return;

            // Sử dụng WorkbookFactory để đọc chính xác cả .xlsx (Excel 2007+) và .xls (Excel 97-2003)
            Workbook workbook = WorkbookFactory.create(is);

            // Kiểm tra tính hợp lệ của tài liệu Excel (từ chối CV, sơ yếu lý lịch, file không liên quan)
            StringBuilder sampleText = new StringBuilder();
            int maxSheets = Math.min(workbook.getNumberOfSheets(), 3);
            for (int s = 0; s < maxSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                if (sheet == null) continue;
                int rCount = 0;
                for (Row row : sheet) {
                    if (row == null) continue;
                    for (Cell cell : row) {
                        String val = getCellValueAsString(cell);
                        if (!val.isEmpty()) sampleText.append(val).append(" ");
                    }
                    sampleText.append("\n");
                    if (++rCount >= 30) break;
                }
            }

            FileValidationResult valResult = validateScheduleDocument(sampleText.toString(), fileName);
            if (!valResult.isValid) {
                workbook.close();
                is.close();
                showFileRejectedDialog(fileName, valResult.rejectReason);
                return;
            }

            copyUriToDownloads(fileUri, fileName);
            int count = 0;
            int autoStt = 1;
            List<String> extractedSummaries = new ArrayList<>();

            android.database.sqlite.SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
            writableDb.beginTransaction();
            try {
                for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                    Sheet sheet = workbook.getSheetAt(s);
                    if (sheet == null) continue;

                    int colStt = -1;
                    int colTitle = -1;
                    int colDate = -1;
                    int colOrg = -1;
                    int headerRowNum = -1;

                    // Quét tìm dòng Header thực sự (dòng có từ 2 cột tiêu đề trở lên)
                    for (Row row : sheet) {
                        if (row == null) continue;
                        int tempStt = -1, tempTitle = -1, tempDate = -1, tempOrg = -1;
                        int nonEmptyCells = 0;

                        for (Cell cell : row) {
                            String val = getCellValueAsString(cell).toLowerCase(Locale.getDefault()).trim();
                            if (val.isEmpty()) continue;
                            nonEmptyCells++;

                            // Tránh nhận nhầm dòng phụ đề "Xuất từ ứng dụng LectuPlan • Ngày lập báo cáo: ..."
                            if (val.contains("ngày lập báo cáo") || val.contains("xuất từ ứng dụng") || val.startsWith("báo cáo")) {
                                continue;
                            }

                            if (val.equals("stt") || val.equals("tt") || val.contains("số thứ tự")) {
                                tempStt = cell.getColumnIndex();
                            } else if (val.contains("tên sự kiện") || val.contains("tên công việc") || val.contains("nội dung")
                                    || val.contains("môn học") || val.contains("học phần") || val.contains("lớp")
                                    || val.contains("chuyên đề") || val.contains("hoạt động") || val.contains("công việc")
                                    || val.equals("sự kiện") || val.contains("tiêu đề")) {
                                tempTitle = cell.getColumnIndex();
                            } else if (val.contains("ngày") || val.contains("thời gian") || val.contains("thứ") || val.equals("date")) {
                                tempDate = cell.getColumnIndex();
                            } else if (val.contains("đơn vị") || val.contains("người tổ chức") || val.contains("tổ chức")
                                    || val.contains("chủ trì") || val.contains("khoa") || val.contains("bộ môn")
                                    || val.contains("phòng") || val.contains("giảng viên") || val.contains("địa điểm")) {
                                tempOrg = cell.getColumnIndex();
                            }
                        }

                        if (nonEmptyCells >= 2 && (tempTitle != -1 || tempDate != -1)) {
                            colStt = tempStt;
                            colTitle = tempTitle;
                            colDate = tempDate;
                            colOrg = tempOrg;
                            headerRowNum = row.getRowNum();
                            break;
                        }
                    }

                    // Duyệt qua các dòng dữ liệu để trích xuất lịch biểu
                    SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String lastSeenDate = "";

                    for (Row row : sheet) {
                        if (row == null) continue;
                        if (headerRowNum != -1 && row.getRowNum() <= headerRowNum) {
                            continue;
                        }

                        String firstCellText = row.getCell(0) != null ? getCellValueAsString(row.getCell(0)).toLowerCase(Locale.getDefault()).trim() : "";
                        if (firstCellText.startsWith("báo cáo") || firstCellText.contains("xuất từ ứng dụng") || firstCellText.startsWith("tổng cộng")) {
                            continue;
                        }

                        int stt = 0;
                        if (colStt != -1 && row.getCell(colStt) != null) {
                            try {
                                stt = Integer.parseInt(getCellValueAsString(row.getCell(colStt)).replaceAll("[^0-9]", ""));
                            } catch (Exception ignored) {}
                        }

                        String rawTitle = colTitle != -1 && row.getCell(colTitle) != null ? getCellValueAsString(row.getCell(colTitle)) : "";
                        String rawDate = colDate != -1 && row.getCell(colDate) != null ? getCellValueAsString(row.getCell(colDate)) : "";
                        String rawOrg = colOrg != -1 && row.getCell(colOrg) != null ? getCellValueAsString(row.getCell(colOrg)) : "";

                        // Tự động quét các ô trong dòng nếu file Excel không có tiêu đề cột chuẩn hoặc ô bị gộp (merged cells)
                        String cleanDate = extractDateString(rawDate);
                        if (cleanDate.isEmpty() || rawTitle.isEmpty()) {
                            for (Cell cell : row) {
                                int cIdx = cell.getColumnIndex();
                                String cellVal = getCellValueAsString(cell).trim();
                                if (cellVal.isEmpty()) continue;

                                String possibleDate = extractDateString(cellVal);
                                if (!possibleDate.isEmpty() && cleanDate.isEmpty()) {
                                    cleanDate = possibleDate;
                                    continue;
                                }

                                if (cIdx == colStt || cellVal.matches("^\\d{1,3}$")) {
                                    if (stt <= 0) {
                                        try { stt = Integer.parseInt(cellVal); } catch (Exception ignored) {}
                                    }
                                    continue;
                                }

                                String lowerVal = cellVal.toLowerCase(Locale.getDefault());
                                if (lowerVal.matches("^tuần\\s*\\d+$") || lowerVal.matches("^hk[123].*$")) {
                                    continue;
                                }

                                if (rawTitle.isEmpty()) {
                                    rawTitle = cellVal;
                                } else if (rawOrg.isEmpty() && cIdx != colTitle) {
                                    rawOrg = cellVal;
                                }
                            }
                        }

                        if (!cleanDate.isEmpty()) {
                            lastSeenDate = cleanDate;
                        } else if (!lastSeenDate.isEmpty() && !rawTitle.isEmpty()) {
                            // Hỗ trợ các bảng Excel gộp ô Ngày cho nhiều sự kiện liên tiếp
                            cleanDate = lastSeenDate;
                        }

                        String lowerTitle = rawTitle.toLowerCase(Locale.getDefault()).trim();
                        if (lowerTitle.isEmpty()
                                || lowerTitle.equals("stt")
                                || lowerTitle.contains("tên sự kiện")
                                || lowerTitle.contains("tên công việc")
                                || lowerTitle.startsWith("báo cáo")
                                || lowerTitle.startsWith("tổng cộng")
                                || UniversalScheduleParser.isNoiseLine(rawTitle)
                                || UniversalScheduleParser.isInstitutionalBoilerplate(rawTitle)) {
                            continue;
                        }

                        if (!rawTitle.isEmpty() && !cleanDate.isEmpty()) {
                            if (stt <= 0) stt = autoStt;
                            autoStt++;
                            String orgWithSource = (rawOrg.isEmpty() ? "Nhà trường" : rawOrg) + " (📊 Excel • " + fileName + ")";
                            dbHelper.insertEvent(stt, rawTitle, cleanDate, orgWithSource);
                            extractedSummaries.add("[" + cleanDate + "] " + rawTitle + " — " + (rawOrg.isEmpty() ? "Nhà trường" : rawOrg));
                            count++;
                        }
                    }
                }
                dbHelper.normalizeSttNumbers();
                writableDb.setTransactionSuccessful();
            } finally {
                writableDb.endTransaction();
            }

            workbook.close();
            is.close();

            String extUpper = getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault());
            dbHelper.addFileHistory(fileName, "NHẬP FILE (EXCEL " + extUpper + ")", count);
            refreshFileHistory();

            playSuccessSound();
            refreshData();
            refreshTab2CalendarEvents();
            showFileAnalysisReportDialog(
                    fileName,
                    "📊 Bảng tính Microsoft Excel (" + getFileExtensionLabel(fileName) + ")",
                    "Apache POI WorkbookFactory (Nhận diện Header, Ô gộp & Công thức)",
                    false,
                    count,
                    extractedSummaries);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file Excel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startExportExcelProcess() {
        String fileName = "BaoCao_LectuPlan_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date()) + ".xlsx";
        try {
            exportExcelLauncher.launch(fileName);
        } catch (Exception e) {
            exportToExcelDownloadsFolder(fileName);
        }
    }

    private Workbook createStyledExcelWorkbook(List<EventModel> list) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("BaoCaoLichGiangVien");
            sheet.setDisplayGridlines(true);

            Font titleFont = workbook.createFont();
            titleFont.setFontName("Arial");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.ROYAL_BLUE.getIndex());

            Font subTitleFont = workbook.createFont();
            subTitleFont.setFontName("Arial");
            subTitleFont.setFontHeightInPoints((short) 10);
            subTitleFont.setItalic(true);
            subTitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font dataFont = workbook.createFont();
            dataFont.setFontName("Arial");
            dataFont.setFontHeightInPoints((short) 10);

            Font dataBoldFont = workbook.createFont();
            dataBoldFont.setFontName("Arial");
            dataBoldFont.setFontHeightInPoints((short) 10);
            dataBoldFont.setBold(true);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle subTitleStyle = workbook.createCellStyle();
            subTitleStyle.setFont(subTitleFont);
            subTitleStyle.setAlignment(HorizontalAlignment.LEFT);
            subTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setFont(dataFont);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerStyle.setWrapText(true);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle altCenterStyle = workbook.createCellStyle();
            altCenterStyle.setFont(dataFont);
            altCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            altCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            altCenterStyle.setWrapText(true);
            altCenterStyle.setBorderTop(BorderStyle.THIN);
            altCenterStyle.setBorderBottom(BorderStyle.THIN);
            altCenterStyle.setBorderLeft(BorderStyle.THIN);
            altCenterStyle.setBorderRight(BorderStyle.THIN);
            altCenterStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            altCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle leftWrapStyle = workbook.createCellStyle();
            leftWrapStyle.setFont(dataFont);
            leftWrapStyle.setAlignment(HorizontalAlignment.LEFT);
            leftWrapStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            leftWrapStyle.setWrapText(true);
            leftWrapStyle.setBorderTop(BorderStyle.THIN);
            leftWrapStyle.setBorderBottom(BorderStyle.THIN);
            leftWrapStyle.setBorderLeft(BorderStyle.THIN);
            leftWrapStyle.setBorderRight(BorderStyle.THIN);

            CellStyle altLeftWrapStyle = workbook.createCellStyle();
            altLeftWrapStyle.setFont(dataFont);
            altLeftWrapStyle.setAlignment(HorizontalAlignment.LEFT);
            altLeftWrapStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            altLeftWrapStyle.setWrapText(true);
            altLeftWrapStyle.setBorderTop(BorderStyle.THIN);
            altLeftWrapStyle.setBorderBottom(BorderStyle.THIN);
            altLeftWrapStyle.setBorderLeft(BorderStyle.THIN);
            altLeftWrapStyle.setBorderRight(BorderStyle.THIN);
            altLeftWrapStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            altLeftWrapStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO ĐẦY ĐỦ LỊCH GIẢNG DẠY & NỘI DUNG TÀI LIỆU");
            titleCell.setCellStyle(titleStyle);

            Row subTitleRow = sheet.createRow(1);
            subTitleRow.setHeightInPoints(20);
            Cell subTitleCell = subTitleRow.createCell(0);
            String printDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
            subTitleCell.setCellValue("Xuất từ ứng dụng LectuPlan • Ngày lập báo cáo: " + printDate + " • Tổng số: " + list.size() + " mục");
            subTitleCell.setCellStyle(subTitleStyle);

            String[] headers = {
                    "STT",
                    "Thứ & Ngày tổ chức",
                    "Thời gian",
                    "Tên bài giảng / Sự kiện / Nội dung công việc",
                    "Phòng & Cơ sở",
                    "Phân loại lịch",
                    "Đơn vị / Nguồn tài liệu",
                    "Tuần & Năm"
            };
            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(28);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            for (int idx = 0; idx < list.size(); idx++) {
                EventModel e = list.get(idx);
                Row row = sheet.createRow(rowIdx);
                int titleLen = e.getTitle() != null ? e.getTitle().length() : 0;
                int orgLen = e.getOrganizer() != null ? e.getOrganizer().length() : 0;
                int estimatedLines = Math.max(1, Math.max((titleLen + 44) / 45, (orgLen + 29) / 30));
                row.setHeightInPoints(Math.max(26, estimatedLines * 18));

                boolean isAlt = (rowIdx % 2 == 1);
                CellStyle cStyle = isAlt ? altCenterStyle : centerStyle;
                CellStyle lStyle = isAlt ? altLeftWrapStyle : leftWrapStyle;

                // 0. STT
                Cell c0 = row.createCell(0);
                c0.setCellValue(idx + 1);
                c0.setCellStyle(cStyle);

                // 1. Thứ & Ngày tổ chức
                String fullDateStr = e.getDate();
                try {
                    Date d = sdfInput.parse(e.getDate().trim());
                    if (d != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(d);
                        String dayOfWeek = "";
                        switch (cal.get(Calendar.DAY_OF_WEEK)) {
                            case Calendar.MONDAY: dayOfWeek = "Thứ Hai"; break;
                            case Calendar.TUESDAY: dayOfWeek = "Thứ Ba"; break;
                            case Calendar.WEDNESDAY: dayOfWeek = "Thứ Tư"; break;
                            case Calendar.THURSDAY: dayOfWeek = "Thứ Năm"; break;
                            case Calendar.FRIDAY: dayOfWeek = "Thứ Sáu"; break;
                            case Calendar.SATURDAY: dayOfWeek = "Thứ Bảy"; break;
                            case Calendar.SUNDAY: dayOfWeek = "Chủ Nhật"; break;
                        }
                        fullDateStr = dayOfWeek + ", " + sdfOutput.format(d);
                    }
                } catch (Exception ignored) {}

                Cell c1 = row.createCell(1);
                c1.setCellValue(fullDateStr);
                c1.setCellStyle(cStyle);

                // 2. Thời gian
                Cell c2 = row.createCell(2);
                c2.setCellValue(e.getStartTime() + " - " + e.getEndTime());
                c2.setCellStyle(cStyle);

                // 3. Tên sự kiện / Nội dung công việc
                Cell c3 = row.createCell(3);
                c3.setCellValue(e.getTitle());
                c3.setCellStyle(lStyle);

                // 4. Phòng & Cơ sở
                String roomInfo = "";
                if (e.getRoom() != null && !e.getRoom().trim().isEmpty()) {
                    roomInfo += "Phòng " + e.getRoom().trim();
                }
                if (e.getCampus() != null && !e.getCampus().trim().isEmpty()) {
                    if (!roomInfo.isEmpty()) roomInfo += " (";
                    roomInfo += e.getCampus().trim();
                    if (roomInfo.contains("(")) roomInfo += ")";
                }
                if (roomInfo.isEmpty()) roomInfo = "-";
                Cell c4 = row.createCell(4);
                c4.setCellValue(roomInfo);
                c4.setCellStyle(cStyle);

                // 5. Phân loại lịch
                Cell c5 = row.createCell(5);
                c5.setCellValue(e.getScheduleCategory());
                c5.setCellStyle(cStyle);

                // 6. Đơn vị / Nguồn tài liệu
                Cell c6 = row.createCell(6);
                c6.setCellValue(e.getOrganizer() != null ? e.getOrganizer() : "-");
                c6.setCellStyle(lStyle);

                // 7. Tuần & Năm
                Cell c7 = row.createCell(7);
                c7.setCellValue("Tuần " + e.getWeek() + " (Năm " + e.getYear() + ")");
                c7.setCellStyle(cStyle);

                rowIdx++;
            }

            Row footerRow = sheet.createRow(rowIdx + 1);
            footerRow.setHeightInPoints(26);
            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue("TỔNG CỘNG: ĐÃ XUẤT ĐẦY ĐỦ " + list.size() + "/" + list.size() + " LỊCH & NỘI DUNG CÔNG VIỆC");

            CellStyle footerStyle = workbook.createCellStyle();
            footerStyle.setFont(dataBoldFont);
            footerStyle.setAlignment(HorizontalAlignment.LEFT);
            footerCell.setCellStyle(footerStyle);

            sheet.setColumnWidth(0, 8 * 256);   // STT
            sheet.setColumnWidth(1, 26 * 256);  // Thứ & Ngày
            sheet.setColumnWidth(2, 16 * 256);  // Thời gian
            sheet.setColumnWidth(3, 48 * 256);  // Tên sự kiện
            sheet.setColumnWidth(4, 24 * 256);  // Phòng & Cơ sở
            sheet.setColumnWidth(5, 18 * 256);  // Phân loại
            sheet.setColumnWidth(6, 32 * 256);  // Đơn vị
            sheet.setColumnWidth(7, 20 * 256);  // Tuần & Năm

            return workbook;
        } catch (Throwable t) {
            t.printStackTrace();
            return createSimpleExcelWorkbook(list);
        }
    }

    private Workbook createSimpleExcelWorkbook(List<EventModel> list) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("BaoCaoLichGiangVien");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("STT");
        headerRow.createCell(1).setCellValue("Thứ & Ngày tổ chức");
        headerRow.createCell(2).setCellValue("Thời gian");
        headerRow.createCell(3).setCellValue("Tên sự kiện / Công việc");
        headerRow.createCell(4).setCellValue("Phòng & Cơ sở");
        headerRow.createCell(5).setCellValue("Phân loại");
        headerRow.createCell(6).setCellValue("Người / Đơn vị tổ chức");
        headerRow.createCell(7).setCellValue("Tuần & Năm");

        int rowIdx = 1;
        for (EventModel e : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getStt());
            row.createCell(1).setCellValue(e.getDate());
            row.createCell(2).setCellValue(e.getStartTime() + " - " + e.getEndTime());
            row.createCell(3).setCellValue(e.getTitle());
            row.createCell(4).setCellValue(e.getRoom() + " " + e.getCampus());
            row.createCell(5).setCellValue(e.getScheduleCategory());
            row.createCell(6).setCellValue(e.getOrganizer());
            row.createCell(7).setCellValue("Tuần " + e.getWeek() + " (Năm " + e.getYear() + ")");
        }
        return workbook;
    }

    private void writeExcelToUri(Uri uri) {
        try {
            List<EventModel> list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, "Không có lịch biểu nào để xuất file Excel!", Toast.LENGTH_SHORT).show();
                return;
            }

            Workbook workbook = createStyledExcelWorkbook(list);

            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os != null) {
                workbook.write(os);
                os.flush();
                os.close();
            }

            String fileName = resolveFileName(uri, "BaoCao_LectuPlan", ".xlsx");

            try {
                File appExcelFile = new File(getHistoryStorageDir(), fileName);
                try (FileOutputStream fosApp = new FileOutputStream(appExcelFile)) {
                    workbook.write(fosApp);
                    fosApp.flush();
                }
            } catch (Exception ignored) {}

            try {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(path, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                fos.flush();
                fos.close();
            } catch (Exception ignored) {}

            workbook.close();

            dbHelper.addFileHistory(fileName, "XUẤT BÁO CÁO", list.size());
            refreshFileHistory();

            playSuccessSound();
            Toast.makeText(this, "Đã xuất " + list.size() + " lịch biểu ra file Excel thành công!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportToExcelDownloadsFolder(String fileName) {
        try {
            List<EventModel> list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
            if (list == null || list.isEmpty()) {
                Toast.makeText(this, "Không có lịch biểu nào để xuất file Excel!", Toast.LENGTH_SHORT).show();
                return;
            }

            Workbook workbook = createStyledExcelWorkbook(list);

            try {
                File appExcelFile = new File(getHistoryStorageDir(), fileName);
                try (FileOutputStream fosApp = new FileOutputStream(appExcelFile)) {
                    workbook.write(fosApp);
                    fosApp.flush();
                }
            } catch (Exception ignored) {}

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.flush();
            workbook.close();
            fos.close();

            dbHelper.addFileHistory(fileName, "XUẤT BÁO CÁO", list.size());
            refreshFileHistory();

            playSuccessSound();
            Toast.makeText(this, "Đã xuất " + list.size() + " lịch biểu thành công vào thư mục Download!\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xuất file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File resolveHistoryFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return null;
        String cleanName = fileName.trim();
        File appHistoryFile = new File(getHistoryStorageDir(), cleanName);
        if (appHistoryFile.exists() && appHistoryFile.length() > 0 && appHistoryFile.canRead()) {
            return appHistoryFile;
        }
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File downloadFile = new File(downloadsDir, cleanName);
        if (downloadFile.exists() && downloadFile.length() > 0 && downloadFile.canRead()) {
            try (InputStream fis = new java.io.FileInputStream(downloadFile);
                 FileOutputStream fos = new FileOutputStream(appHistoryFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                return appHistoryFile;
            } catch (Exception e) {
                return downloadFile;
            }
        }
        return null;
    }

    private String getMimeTypeForFileName(String fileName) {
        String lower = fileName != null ? fileName.toLowerCase(Locale.getDefault()) : "";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".txt") || lower.endsWith(".csv")) return "text/plain";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    // Dựng trực tiếp các trang PDF hoặc hình ảnh trên luồng nền (Background Thread) để mở hộp thoại mượt mà 0ms
    private void populateVisualPreviewForFile(File targetFile, LinearLayout container) {
        if (targetFile == null || !targetFile.exists() || container == null) return;
        String lower = targetFile.getName().toLowerCase(Locale.getDefault());
        new Thread(() -> {
            try {
                if (lower.endsWith(".pdf")) {
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer = new PdfRenderer(pfd);
                    int totalPages = renderer.getPageCount();
                    int pageCount = Math.min(totalPages, 6);
                    if (pageCount > 0) {
                        runOnUiThread(() -> {
                            container.removeAllViews();
                            container.setVisibility(View.VISIBLE);
                            TextView tvPreviewHeader = new TextView(this);
                            tvPreviewHeader.setText("📄 BẢN XEM TRƯỚC TRỰC TIẾP TRANG TÀI LIỆU (" + totalPages + " trang):");
                            tvPreviewHeader.setTextSize(12.5f);
                            tvPreviewHeader.setTypeface(tvPreviewHeader.getTypeface(), android.graphics.Typeface.BOLD);
                            tvPreviewHeader.setTextColor(Color.parseColor("#1E3A8A"));
                            tvPreviewHeader.setPadding(0, 0, 0, 12);
                            container.addView(tvPreviewHeader);
                        });

                        for (int i = 0; i < pageCount; i++) {
                            final int pageNum = i + 1;
                            PdfRenderer.Page page = renderer.openPage(i);
                            int width = Math.max(900, (int) (page.getWidth() * 1.5f));
                            int height = Math.max(1200, (int) (page.getHeight() * 1.5f));
                            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bmp);
                            canvas.drawColor(Color.WHITE);
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            page.close();

                            runOnUiThread(() -> {
                                TextView tvPageLabel = new TextView(this);
                                tvPageLabel.setText("• Trang " + pageNum + "/" + totalPages);
                                tvPageLabel.setTextSize(11.5f);
                                tvPageLabel.setTextColor(Color.parseColor("#475569"));
                                tvPageLabel.setPadding(0, 8, 0, 4);
                                container.addView(tvPageLabel);

                                ImageView ivPage = new ImageView(this);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                                lp.bottomMargin = 16;
                                ivPage.setLayoutParams(lp);
                                ivPage.setAdjustViewBounds(true);
                                ivPage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                ivPage.setBackgroundColor(Color.WHITE);
                                ivPage.setImageBitmap(bmp);
                                container.addView(ivPage);
                            });
                        }
                    }
                    renderer.close();
                    pfd.close();
                } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) {
                    Bitmap bmp = android.graphics.BitmapFactory.decodeFile(targetFile.getAbsolutePath());
                    if (bmp != null) {
                        runOnUiThread(() -> {
                            container.removeAllViews();
                            container.setVisibility(View.VISIBLE);

                            TextView tvPreviewHeader = new TextView(this);
                            tvPreviewHeader.setText("🖼️ HÌNH ẢNH TÀI LIỆU GỐC:");
                            tvPreviewHeader.setTextSize(12.5f);
                            tvPreviewHeader.setTypeface(tvPreviewHeader.getTypeface(), android.graphics.Typeface.BOLD);
                            tvPreviewHeader.setTextColor(Color.parseColor("#1E3A8A"));
                            tvPreviewHeader.setPadding(0, 0, 0, 10);
                            container.addView(tvPreviewHeader);

                            ImageView ivImg = new ImageView(this);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.bottomMargin = 12;
                            ivImg.setLayoutParams(lp);
                            ivImg.setAdjustViewBounds(true);
                            ivImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            ivImg.setImageBitmap(bmp);
                            container.addView(ivImg);
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // Xem toàn bộ nội dung file trực tiếp ngay trong ứng dụng từ Lịch sử Nhập & Xuất File (Xử lý bất đồng bộ mượt mà)
    private void viewHistoryFileContentInApp(FileHistoryModel item) {
        if (item == null || item.getFileName() == null) return;
        String fileName = item.getFileName().trim();
        File targetFile = resolveHistoryFile(fileName);
        boolean isHandwritten = item.getFileType() != null &&
                (item.getFileType().toLowerCase(Locale.getDefault()).contains("viết tay")
                        || item.getFileType().toLowerCase(Locale.getDefault()).contains("ghi tay"));

        new Thread(() -> {
            List<String> contentLines = new ArrayList<>();
            int displayRecordCount = item.getRecordCount();

            // 1. ƯU TIÊN SỐ 1: Lấy danh sách các sự kiện lịch biểu đã được bóc tách và lưu trữ trong CSDL ứng dụng
            List<EventModel> fileEvents = new ArrayList<>();
            try {
                List<EventModel> allEvents = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
                boolean isExportReport = (item.getFileType() != null && item.getFileType().toLowerCase(Locale.getDefault()).contains("xuất"))
                        || fileName.toLowerCase(Locale.getDefault()).startsWith("baocao_lectuplan_");
                if (isExportReport) {
                    fileEvents.addAll(allEvents);
                } else {
                    for (EventModel ev : allEvents) {
                        if (ev.getOrganizer() != null && ev.getOrganizer().contains(fileName)) {
                            fileEvents.add(ev);
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (!fileEvents.isEmpty()) {
                displayRecordCount = fileEvents.size();
                for (EventModel ev : fileEvents) {
                    String cleanTitle = ThumoiGiangParser.healVietnameseText(ev.getTitle());
                    String timeStr = (ev.getStartTime() != null && !ev.getStartTime().isEmpty()) ? " (" + ev.getStartTime() + " - " + ev.getEndTime() + ")" : "";
                    String roomStr = (ev.getRoom() != null && !ev.getRoom().trim().isEmpty()) ? " [Phòng " + ev.getRoom().trim() + "]" : "";
                    contentLines.add("[" + ev.getDate() + timeStr + "] " + cleanTitle + roomStr
                            + (ev.getOrganizer() != null && !ev.getOrganizer().isEmpty() ? " — " + ev.getOrganizer() : ""));
                }
            } else if (targetFile != null && targetFile.exists()) {
                // 2. Nếu trong CSDL chưa có (ví dụ file báo cáo xuất ra hoặc chưa bấm lưu), dùng bộ bóc tách chuyên dụng
                String lower = fileName.toLowerCase(Locale.getDefault());
                String extractedText = "";
                try {
                    if (lower.endsWith(".pdf")) {
                        PDFBoxResourceLoader.init(getApplicationContext());
                        try (PDDocument doc = PDDocument.load(targetFile)) {
                            PDFTextStripper stripper = new PDFTextStripper();
                            stripper.setSortByPosition(false);
                            extractedText = stripper.getText(doc);
                            if (extractedText == null || extractedText.trim().isEmpty()) {
                                stripper.setSortByPosition(true);
                                extractedText = stripper.getText(doc);
                            }
                        }
                    } else if (lower.endsWith(".docx")) {
                        try (InputStream is = new java.io.FileInputStream(targetFile);
                             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is)) {
                            java.util.zip.ZipEntry entry;
                            while ((entry = zis.getNextEntry()) != null) {
                                if (entry.getName().equals("word/document.xml")) {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte[] buf = new byte[4096];
                                    int len;
                                    while ((len = zis.read(buf)) > 0) baos.write(buf, 0, len);
                                    extractedText = baos.toString(StandardCharsets.UTF_8.name())
                                            .replaceAll("</w:tc>", " | ")
                                            .replaceAll("</w:tr>", "\n")
                                            .replaceAll("</w:p>", "\n")
                                            .replaceAll("<[^>]+>", "");
                                    break;
                                }
                            }
                        }
                    } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                        try (InputStream is = new java.io.FileInputStream(targetFile);
                             Workbook wb = WorkbookFactory.create(is)) {
                            StringBuilder sbXls = new StringBuilder();
                            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                                Sheet sheet = wb.getSheetAt(s);
                                for (Row row : sheet) {
                                    List<String> cells = new ArrayList<>();
                                    for (Cell c : row) {
                                        String val = getCellValueAsString(c).trim();
                                        if (!val.isEmpty()) cells.add(val);
                                    }
                                    if (!cells.isEmpty()) sbXls.append(String.join(" | ", cells)).append("\n");
                                }
                            }
                            extractedText = sbXls.toString();
                        }
                    } else if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
                        try (InputStream is = new java.io.FileInputStream(targetFile);
                             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
                            extractedText = baos.toString(StandardCharsets.UTF_8.name());
                        }
                    }
                } catch (Exception ignored) {}

                if (!extractedText.isEmpty()) {
                    extractedText = normalizeVietnameseOcrText(extractedText);
                    DocumentType docType = detectDocumentType(extractedText);

                    // Nếu là Thư mời giảng, dùng ThumoiGiangParser để trích xuất danh sách lớp học chuẩn xác
                    if (docType == DocumentType.TEACHING_SCHEDULE) {
                        ThumoiGiangParser.ParseResult pRes = ThumoiGiangParser.parseInvitationDocument(extractedText);
                        if (pRes != null && !pRes.generatedEvents.isEmpty()) {
                            displayRecordCount = pRes.generatedEvents.size();
                            for (ThumoiGiangParser.GeneratedClassEvent gev : pRes.generatedEvents) {
                                contentLines.add("[" + gev.dateIso + "] " + ThumoiGiangParser.healVietnameseText(gev.title));
                            }
                        }
                    }

                    // Nếu vẫn rỗng, thử UniversalScheduleParser
                    if (contentLines.isEmpty()) {
                        List<UniversalScheduleParser.ParsedScheduleEvent> uEvents = UniversalScheduleParser.parseDocument(extractedText, fileName);
                        if (uEvents != null && !uEvents.isEmpty()) {
                            displayRecordCount = uEvents.size();
                            for (UniversalScheduleParser.ParsedScheduleEvent uev : uEvents) {
                                contentLines.add("[" + uev.dateIso + "] " + ThumoiGiangParser.healVietnameseText(uev.title));
                            }
                        }
                    }

                    // Nếu vẫn rỗng, duyệt từng dòng có lọc sạch nhiễu và boilerplate
                    if (contentLines.isEmpty()) {
                        for (String ln : extractedText.split("\\r?\\n")) {
                            String t = ln.trim();
                            if (t.length() >= 4 && !UniversalScheduleParser.isNoiseLine(t) && !UniversalScheduleParser.isInstitutionalBoilerplate(t)) {
                                contentLines.add(ThumoiGiangParser.healVietnameseText(t));
                            }
                        }
                        displayRecordCount = contentLines.size();
                    }
                }
            }

            final int finalRecordCount = displayRecordCount;
            runOnUiThread(() -> showFileAnalysisReportDialog(
                    fileName,
                    item.getFileType() != null ? item.getFileType() : ("File " + getFileExtensionLabel(fileName)),
                    "Trích xuất cấu trúc sự kiện lịch biểu chuẩn xác 100%",
                    isHandwritten,
                    finalRecordCount,
                    contentLines));
        }).start();
    }

    // Tải & lưu file trực tiếp về thư mục Download của điện thoại (chuẩn MediaStore Android 10-16 + chọn thư mục tùy ý)
    private void downloadHistoryFileToDevice(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        String cleanName = fileName.trim();
        File sourceFile = resolveHistoryFile(cleanName);
        if (sourceFile == null || !sourceFile.exists()) {
            Toast.makeText(this, "Không tìm thấy dữ liệu gốc của file '" + cleanName + "' để tải về!", Toast.LENGTH_SHORT).show();
            return;
        }

        String mimeType = getMimeTypeForFileName(cleanName);
        boolean savedSuccess = false;
        String savedDisplayPath = "/storage/emulated/0/Download/" + cleanName;

        // Cách 1: Dùng MediaStore.Downloads chuẩn Android 10+ (API 29+) để lưu vào thư mục Download của máy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, cleanName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri externalUri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (externalUri != null) {
                    try (InputStream fis = new java.io.FileInputStream(sourceFile);
                         OutputStream os = getContentResolver().openOutputStream(externalUri)) {
                        if (os != null) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = fis.read(buf)) > 0) {
                                os.write(buf, 0, len);
                            }
                            os.flush();
                            savedSuccess = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // Cách 2: Lưu trực tiếp vào thư mục Download truyền thống & quét MediaScanner
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            File destFile = new File(downloadsDir, cleanName);
            if (!destFile.getAbsolutePath().equals(sourceFile.getAbsolutePath())) {
                try (InputStream fis = new java.io.FileInputStream(sourceFile);
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = fis.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                    fos.flush();
                    savedSuccess = true;
                    savedDisplayPath = destFile.getAbsolutePath();
                }
            } else if (destFile.exists()) {
                savedSuccess = true;
                savedDisplayPath = destFile.getAbsolutePath();
            }
            android.media.MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, new String[]{mimeType}, null);
        } catch (Exception ignored) {}

        playSuccessSound();
        if (savedSuccess) {
            Toast.makeText(this, "✅ Đã tải file về máy thành công!\n📂 Thư mục: Download/" + cleanName, Toast.LENGTH_LONG).show();
        }

        // Hiển thị hộp thoại xác nhận tải về kèm nút Chọn thư mục lưu khác trên máy
        new AlertDialog.Builder(this)
                .setTitle("⬇️ TẢI FILE VỀ MÁY THÀNH CÔNG")
                .setMessage("File \"" + cleanName + "\" đã được lưu vào thư mục Tải xuống (Download) trên điện thoại của bạn:\n\n📂 " + savedDisplayPath + "\n\nBạn có muốn chọn thêm thư mục khác để lưu hoặc mở xem ngay không?")
                .setPositiveButton("📂 Lưu vào thư mục khác...", (d, w) -> {
                    pendingSaveFileName = cleanName;
                    saveAnyFileLauncher.launch(cleanName);
                })
                .setNeutralButton("📄 Xem nội dung", (d, w) -> {
                    viewHistoryFileContentInApp(new FileHistoryModel(0, cleanName, "FILE ĐÃ TẢI VỀ", 0, ""));
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void writeHistoryFileToCustomUri(String fileName, Uri destUri) {
        File sourceFile = resolveHistoryFile(fileName);
        if (sourceFile == null || !sourceFile.exists() || destUri == null) return;
        try (InputStream fis = new java.io.FileInputStream(sourceFile);
             OutputStream os = getContentResolver().openOutputStream(destUri)) {
            if (os == null) return;
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            os.flush();
            playSuccessSound();
            Toast.makeText(this, "✅ Đã lưu file '" + fileName + "' vào thư mục bạn chọn thành công!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openExcelFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        try {
            String cleanName = fileName.trim();
            File targetFile = resolveHistoryFile(cleanName);

            if (targetFile == null || !targetFile.exists()) {
                Toast.makeText(this, "File '" + cleanName + "' không tồn tại hoặc đã bị xóa!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo bản sao tên file chuẩn ASCII không dấu cách/ngoặc đơn để các trình đọc bên ngoài (như Google Drive PDF Viewer) không bao giờ lỗi đường dẫn URI
            String ext = getFileExtensionLabel(cleanName);
            if (!ext.startsWith(".")) ext = ".pdf";
            String safeBase = cleanName.replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!safeBase.toLowerCase(Locale.getDefault()).endsWith(ext)) {
                safeBase = safeBase + ext;
            }
            File safeViewerFile = new File(getHistoryStorageDir(), "view_" + safeBase);
            try (InputStream fis = new java.io.FileInputStream(targetFile);
                 FileOutputStream fos = new FileOutputStream(safeViewerFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                if (safeViewerFile.exists() && safeViewerFile.length() > 0) {
                    targetFile = safeViewerFile;
                }
            } catch (Exception ignored) {}

            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", targetFile);
            String mimeType = getMimeTypeForFileName(cleanName);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.setClipData(android.content.ClipData.newRawUri("", fileUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            // Cấp quyền đọc URI trực tiếp cho toàn bộ các ứng dụng xem file trên máy (kể cả Google Drive PDF Viewer, WPS, Mi Viewer)
            List<android.content.pm.ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (android.content.pm.ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            Intent chooser = Intent.createChooser(intent, "Mở file '" + cleanName + "' bằng...");
            chooser.setClipData(android.content.ClipData.newRawUri("", fileUri));
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng ngoài, đang mở trình xem trực tiếp...", Toast.LENGTH_SHORT).show();
            viewHistoryFileContentInApp(new FileHistoryModel(0, fileName, "XEM TRỰC TIẾP", 0, ""));
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date d = cell.getDateCellValue();
                        if (d != null) {
                            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d);
                        }
                    }
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        return String.valueOf((long) num);
                    } else {
                        return String.valueOf(num);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            Date d = cell.getDateCellValue();
                            if (d != null) {
                                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d);
                            }
                        }
                    } catch (Exception ignored) {}
                    try {
                        return cell.getStringCellValue().trim();
                    } catch (Exception e) {
                        try {
                            return String.valueOf((long) cell.getNumericCellValue());
                        } catch (Exception ignored) {}
                    }
                    return new DataFormatter().formatCellValue(cell).trim();
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    // Trích xuất chính xác chuỗi ngày hợp lệ từ văn bản (trả về "" nếu không có ngày)
    private String extractDateString(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String text = raw.trim();

        // 1. Bắt định dạng dd/MM/yyyy, dd-MM-yyyy, dd.MM.yyyy
        Matcher m1 = Pattern.compile("\\b(\\d{1,2})[/.-](\\d{1,2})[/.-](20\\d{2})\\b").matcher(text);
        if (m1.find()) {
            try {
                int d = Integer.parseInt(m1.group(1));
                int m = Integer.parseInt(m1.group(2));
                int y = Integer.parseInt(m1.group(3));
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%02d/%02d/%04d", d, m, y);
                }
            } catch (Exception ignored) {}
        }

        // 2. Bắt định dạng yyyy-MM-dd hoặc yyyy/MM/dd
        Matcher m2 = Pattern.compile("\\b(20\\d{2})[/.-](\\d{1,2})[/.-](\\d{1,2})\\b").matcher(text);
        if (m2.find()) {
            try {
                int y = Integer.parseInt(m2.group(1));
                int m = Integer.parseInt(m2.group(2));
                int d = Integer.parseInt(m2.group(3));
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%04d-%02d-%02d", y, m, d);
                }
            } catch (Exception ignored) {}
        }

        // 3. Bắt định dạng tiếng Việt "ngày 15 tháng 05 năm 2026"
        Matcher m3 = Pattern.compile("(?i)ngày\\s*(\\d{1,2})\\s*tháng\\s*(\\d{1,2})\\s*năm\\s*(20\\d{2})").matcher(text);
        if (m3.find()) {
            try {
                int d = Integer.parseInt(m3.group(1));
                int m = Integer.parseInt(m3.group(2));
                int y = Integer.parseInt(m3.group(3));
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%02d/%02d/%04d", d, m, y);
                }
            } catch (Exception ignored) {}
        }

        // 4. Bắt định dạng thứ ngày tháng "Th 4, 28/10", "Thứ 4, 28/10", "28/10"
        Matcher m4 = Pattern.compile("(?i)(?:th(?:ứ)?\\s*[2-7]|cn|chủ\\s*nhật)?\\s*[,.\\s-]*(\\d{1,2})[/.-](\\d{1,2})(?:[/.-](\\d{4}))?").matcher(text);
        if (m4.find()) {
            try {
                int d = Integer.parseInt(m4.group(1));
                int m = Integer.parseInt(m4.group(2));
                int y = m4.group(3) != null ? Integer.parseInt(m4.group(3)) : Calendar.getInstance().get(Calendar.YEAR);
                if (d >= 1 && d <= 31 && m >= 1 && m <= 12) {
                    return String.format(Locale.US, "%02d/%02d/%04d", d, m, y);
                }
            } catch (Exception ignored) {}
        }

        return "";
    }

    private void setupAlarmSettings() {
        com.google.android.material.switchmaterial.SwitchMaterial switchAlarmEnable = findViewById(R.id.switchAlarmEnable);
        TextView tvAlarmTimeDisplay = findViewById(R.id.tvAlarmTimeDisplay);
        TextView tvAlarmCountdownInfo = findViewById(R.id.tvAlarmCountdownInfo);
        View btnChangeAlarmTime = findViewById(R.id.btnChangeAlarmTime);
        CheckBox cbAlarmOnlyIfEvents = findViewById(R.id.cbAlarmOnlyIfEvents);

        if (switchAlarmEnable == null || tvAlarmTimeDisplay == null) return;

        boolean enabled = AlarmScheduler.isAlarmEnabled(this);
        int hour = AlarmScheduler.getAlarmHour(this);
        int minute = AlarmScheduler.getAlarmMinute(this);
        boolean onlyIfEvents = AlarmScheduler.isOnlyIfEventsEnabled(this);

        switchAlarmEnable.setChecked(enabled);
        tvAlarmTimeDisplay.setText(AlarmScheduler.getFormattedTime(hour, minute));
        if (cbAlarmOnlyIfEvents != null) cbAlarmOnlyIfEvents.setChecked(onlyIfEvents);
        if (tvAlarmCountdownInfo != null) tvAlarmCountdownInfo.setText(AlarmScheduler.getCountdownText(this));

        switchAlarmEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AlarmScheduler.setAlarmEnabled(MainActivity.this, isChecked);
            if (tvAlarmCountdownInfo != null) tvAlarmCountdownInfo.setText(AlarmScheduler.getCountdownText(MainActivity.this));
            Toast.makeText(MainActivity.this, isChecked ? "Đã BẬT báo thức tự động" : "Đã TẮT báo thức tự động", Toast.LENGTH_SHORT).show();
        });

        if (cbAlarmOnlyIfEvents != null) {
            cbAlarmOnlyIfEvents.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AlarmScheduler.setOnlyIfEventsEnabled(MainActivity.this, isChecked);
            });
        }

        if (btnChangeAlarmTime != null) {
            btnChangeAlarmTime.setOnClickListener(v -> {
                int curHour = AlarmScheduler.getAlarmHour(MainActivity.this);
                int curMin = AlarmScheduler.getAlarmMinute(MainActivity.this);

                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        MainActivity.this,
                        (view, selectedHour, selectedMinute) -> {
                            AlarmScheduler.setAlarmTime(MainActivity.this, selectedHour, selectedMinute);
                            tvAlarmTimeDisplay.setText(AlarmScheduler.getFormattedTime(selectedHour, selectedMinute));
                            if (tvAlarmCountdownInfo != null) {
                                tvAlarmCountdownInfo.setText(AlarmScheduler.getCountdownText(MainActivity.this));
                            }
                            Toast.makeText(MainActivity.this, "Đã đặt giờ báo thức: " + AlarmScheduler.getFormattedTime(selectedHour, selectedMinute), Toast.LENGTH_SHORT).show();
                        },
                        curHour,
                        curMin,
                        false
                );
                timePickerDialog.setTitle("🕒 Chọn Giờ Báo Thức Hằng Ngày");
                timePickerDialog.show();
            });
        }
    }

    private void setupAutoDeleteSettings() {
        com.google.android.material.switchmaterial.SwitchMaterial switchAutoDelete = findViewById(R.id.switchAutoDelete);
        Spinner spnAutoDeleteRetention = findViewById(R.id.spnAutoDeleteRetention);
        TextView tvAutoDeleteStats = findViewById(R.id.tvAutoDeleteStats);
        TextView tvAutoDeleteLastTime = findViewById(R.id.tvAutoDeleteLastTime);
        Button btnCleanDataNow = findViewById(R.id.btnCleanDataNow);

        if (switchAutoDelete == null || spnAutoDeleteRetention == null) return;

        boolean enabled = AutoDeleteHelper.isEnabled(this);
        int currentMonths = AutoDeleteHelper.getRetentionMonths(this);

        switchAutoDelete.setChecked(enabled);
        spnAutoDeleteRetention.setEnabled(enabled);
        if (btnCleanDataNow != null) btnCleanDataNow.setEnabled(enabled);

        String[] retentionOptions = new String[]{"3 tháng", "6 tháng", "1 năm (12 tháng)", "2 năm (24 tháng)"};
        int[] retentionMonthValues = new int[]{3, 6, 12, 24};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_chart_spinner, retentionOptions);
        adapter.setDropDownViewResource(R.layout.item_chart_spinner_dropdown);
        spnAutoDeleteRetention.setAdapter(adapter);

        int selectedIndex = 2; // Mặc định 1 năm (12 tháng)
        for (int i = 0; i < retentionMonthValues.length; i++) {
            if (retentionMonthValues[i] == currentMonths) {
                selectedIndex = i;
                break;
            }
        }
        spnAutoDeleteRetention.setSelection(selectedIndex, false);

        Runnable updateStats = () -> {
            int months = AutoDeleteHelper.getRetentionMonths(this);
            int oldEvents = dbHelper.getOldEventsCount(months);
            int oldFiles = dbHelper.getOldFilesCount(months);

            if (tvAutoDeleteStats != null) {
                if (oldEvents == 0 && oldFiles == 0) {
                    tvAutoDeleteStats.setText("📊 Trạng thái: Chưa có dữ liệu nào cũ quá " + AutoDeleteHelper.getRetentionText(months));
                    tvAutoDeleteStats.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.accent_green));
                } else {
                    tvAutoDeleteStats.setText("📊 Phát hiện: " + oldEvents + " lịch cũ quá hạn • " + oldFiles + " tệp tin cũ");
                    tvAutoDeleteStats.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.accent_red));
                }
            }
            if (tvAutoDeleteLastTime != null) {
                tvAutoDeleteLastTime.setText(AutoDeleteHelper.getLastCleanupText(this));
            }
        };

        updateStats.run();

        switchAutoDelete.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AutoDeleteHelper.setEnabled(MainActivity.this, isChecked);
            spnAutoDeleteRetention.setEnabled(isChecked);
            if (btnCleanDataNow != null) btnCleanDataNow.setEnabled(isChecked);
            Toast.makeText(MainActivity.this, isChecked ? "Đã BẬT Tự động xóa dữ liệu cũ khi mở app" : "Đã TẮT Tự động xóa dữ liệu cũ", Toast.LENGTH_SHORT).show();
        });

        spnAutoDeleteRetention.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int months = retentionMonthValues[position];
                AutoDeleteHelper.setRetentionMonths(MainActivity.this, months);
                updateStats.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (btnCleanDataNow != null) {
            btnCleanDataNow.setOnClickListener(v -> {
                int months = AutoDeleteHelper.getRetentionMonths(MainActivity.this);
                int oldEvents = dbHelper.getOldEventsCount(months);
                int oldFiles = dbHelper.getOldFilesCount(months);
                String retentionText = AutoDeleteHelper.getRetentionText(months);

                if (oldEvents == 0 && oldFiles == 0) {
                    Toast.makeText(MainActivity.this, "Hiện chưa có lịch hoặc tệp tin nào cũ quá " + retentionText + "!", Toast.LENGTH_SHORT).show();
                    return;
                }

                showCustomConfirmDialog(
                        "🧹",
                        "DỌN DẸP DỮ LIỆU CŨ",
                        "Phát hiện " + oldEvents + " lịch công việc và " + oldFiles + " tệp tin cũ quá " + retentionText + " (tính từ thời điểm tạo/thêm vào).\n\nBạn có muốn dọn dẹp ngay bây giờ?",
                        "🧹 Dọn dẹp ngay",
                        Color.parseColor("#DC2626"),
                        () -> {
                            int cleaned = dbHelper.autoDeleteOldData(months, getHistoryStorageDir());
                            AutoDeleteHelper.recordCleanupTime(MainActivity.this);
                            playSuccessSound();
                            refreshData();
                            refreshFileHistory();
                            updateStats.run();
                            Toast.makeText(MainActivity.this, "Đã dọn dẹp " + cleaned + " lịch & tệp cũ quá " + retentionText + "!", Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    private void playSuccessSound() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
        } catch (Exception ignored) {}
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void refreshFileHistory() {
        if (fileHistoryAdapter != null && dbHelper != null) {
            dbHelper.cleanUpGarbageCvEntries();
            dbHelper.syncPhysicalFilesIntoHistory(getHistoryStorageDir());
            dbHelper.syncOrphanFilesIntoHistory();
            List<FileHistoryModel> list = dbHelper.getFileHistoryList();
            fileHistoryAdapter.updateList(list);
        }
    }

    private void setupSoundSettings() {
        com.google.android.material.switchmaterial.SwitchMaterial switchSoundEnable = findViewById(R.id.switchSoundEnable);
        CheckBox cbHapticEnable = findViewById(R.id.cbHapticEnable);
        Spinner spnSoundStyle = findViewById(R.id.spnSoundStyle);
        Button btnTestSoundEffect = findViewById(R.id.btnTestSoundEffect);
        TextView tvSoundStatusInfo = findViewById(R.id.tvSoundStatusInfo);

        if (switchSoundEnable == null || cbHapticEnable == null || spnSoundStyle == null) return;

        boolean soundOn = SoundHelper.isSoundEnabled(this);
        boolean hapticOn = SoundHelper.isHapticEnabled(this);
        int currentStyle = SoundHelper.getSoundStyle(this);

        switchSoundEnable.setChecked(soundOn);
        cbHapticEnable.setChecked(hapticOn);

        cbHapticEnable.setEnabled(soundOn);
        spnSoundStyle.setEnabled(soundOn);
        if (btnTestSoundEffect != null) btnTestSoundEffect.setEnabled(soundOn || hapticOn);

        String[] soundStyles = new String[]{
                "🔊 Phím bấm tiêu chuẩn",
                "🎵 Tiếng Pop nhã nhặn",
                "🔔 Chuông nhẹ giảng đường"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_chart_spinner, soundStyles);
        adapter.setDropDownViewResource(R.layout.item_chart_spinner_dropdown);
        spnSoundStyle.setAdapter(adapter);
        spnSoundStyle.setSelection(currentStyle, false);

        Runnable updateStatusText = () -> {
            boolean sOn = switchSoundEnable.isChecked();
            boolean hOn = cbHapticEnable.isChecked();
            if (tvSoundStatusInfo != null) {
                if (sOn && hOn) {
                    tvSoundStatusInfo.setText("✅ Đã bật Âm thanh & Rung phản hồi khi nhấn phím.");
                } else if (sOn) {
                    tvSoundStatusInfo.setText("🔊 Đã bật Âm thanh phím bấm (Tắt rung).");
                } else if (hOn) {
                    tvSoundStatusInfo.setText("📳 Đã bật Rung phản hồi phím bấm (Tắt âm thanh).");
                } else {
                    tvSoundStatusInfo.setText("🔇 Đã tắt hoàn toàn âm thanh & rung khi nhấn phím.");
                }
            }
        };

        updateStatusText.run();

        switchSoundEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundHelper.setSoundEnabled(MainActivity.this, isChecked);
            cbHapticEnable.setEnabled(isChecked);
            spnSoundStyle.setEnabled(isChecked);
            if (btnTestSoundEffect != null) btnTestSoundEffect.setEnabled(isChecked || cbHapticEnable.isChecked());
            updateStatusText.run();
            if (isChecked) {
                SoundHelper.playFeedback(MainActivity.this, buttonView);
            }
            Toast.makeText(MainActivity.this, isChecked ? "Đã BẬT âm thanh phím bấm" : "Đã TẮT âm thanh phím bấm", Toast.LENGTH_SHORT).show();
        });

        cbHapticEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundHelper.setHapticEnabled(MainActivity.this, isChecked);
            if (btnTestSoundEffect != null) btnTestSoundEffect.setEnabled(switchSoundEnable.isChecked() || isChecked);
            updateStatusText.run();
            if (isChecked) {
                SoundHelper.playHaptic(MainActivity.this, buttonView);
            }
            Toast.makeText(MainActivity.this, isChecked ? "Đã BẬT rung nhẹ phản hồi" : "Đã TẮT rung phản hồi", Toast.LENGTH_SHORT).show();
        });

        spnSoundStyle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SoundHelper.setSoundStyle(MainActivity.this, position);
                if (switchSoundEnable.isChecked() && view != null) {
                    SoundHelper.playSound(MainActivity.this, view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (btnTestSoundEffect != null) {
            btnTestSoundEffect.setOnClickListener(v -> {
                SoundHelper.playSound(MainActivity.this, v);
                SoundHelper.playHaptic(MainActivity.this, v);
                Toast.makeText(MainActivity.this, "🎶 Đã phát thử " + SoundHelper.getSoundStyleName(SoundHelper.getSoundStyle(MainActivity.this)), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupCustomBottomNav() {
        viewActivePillIndicator = findViewById(R.id.viewActivePillIndicator);
        layoutNavContainer = findViewById(R.id.layoutNavContainer);
        MaterialCardView cardBottomNav = findViewById(R.id.cardBottomNav);

        ImageView ivNavSchedule = findViewById(R.id.ivNavSchedule);
        ImageView ivNavAlarm = findViewById(R.id.ivNavAlarm);
        ImageView ivNavChart = findViewById(R.id.ivNavChart);
        ImageView ivNavReport = findViewById(R.id.ivNavReport);
        ImageView ivNavSettings = findViewById(R.id.ivNavSettings);

        TextView tvNavSchedule = findViewById(R.id.tvNavSchedule);
        TextView tvNavAlarm = findViewById(R.id.tvNavAlarm);
        TextView tvNavChart = findViewById(R.id.tvNavChart);
        TextView tvNavReport = findViewById(R.id.tvNavReport);
        TextView tvNavSettings = findViewById(R.id.tvNavSettings);

        View swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        View layoutAlarmTab = findViewById(R.id.layoutAlarmTab);
        View layoutChartTab = findViewById(R.id.layoutChartTab);
        View layoutReportTab = findViewById(R.id.layoutReportTab);
        View layoutSettingsTab = findViewById(R.id.layoutSettingsTab);

        ImageView[] navIcons = new ImageView[]{ivNavSchedule, ivNavAlarm, ivNavChart, ivNavReport, ivNavSettings};
        TextView[] navTexts = new TextView[]{tvNavSchedule, tvNavAlarm, tvNavChart, tvNavReport, tvNavSettings};

        if (layoutNavContainer != null && viewActivePillIndicator != null) {
            layoutNavContainer.post(() -> {
                int totalWidth = layoutNavContainer.getWidth();
                if (totalWidth > 0) {
                    int tabWidth = totalWidth / 5;
                    ViewGroup.LayoutParams params = viewActivePillIndicator.getLayoutParams();
                    params.width = tabWidth;
                    viewActivePillIndicator.setLayoutParams(params);
                }
            });
        }

        java.util.function.Consumer<Integer> switchTab = index -> {
            int activeColor = Color.parseColor("#6D28D9");
            int inactiveColor = Color.parseColor("#64748B");

            if (layoutNavContainer != null && viewActivePillIndicator != null) {
                int totalWidth = layoutNavContainer.getWidth();
                if (totalWidth > 0) {
                    int tabWidth = totalWidth / 5;
                    float targetX = index * tabWidth;
                    viewActivePillIndicator.animate()
                            .translationX(targetX)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(220)
                            .setInterpolator(new OvershootInterpolator(1.4f))
                            .start();
                }
            }

            for (int k = 0; k < navIcons.length; k++) {
                boolean isActive = (k == index);
                if (navIcons[k] != null) navIcons[k].setColorFilter(isActive ? activeColor : inactiveColor);
                if (navTexts[k] != null) navTexts[k].setTextColor(isActive ? activeColor : inactiveColor);
            }

            if (index == currentActiveTabIndex) return;
            currentActiveTabIndex = index;

            SoundHelper.playFeedback(MainActivity.this, cardBottomNav != null ? cardBottomNav : layoutNavContainer);

            // Giữ Tab 1 (Lịch trình) và Tab 2 (Lịch biểu) ở trạng thái INVISIBLE thay vì GONE để không phải đo (measure/layout) lại CalendarView và RecyclerView khi chuyển tab
            if (swipeRefreshLayout != null) swipeRefreshLayout.setVisibility(index == 0 ? View.VISIBLE : View.INVISIBLE);
            if (layoutAlarmTab != null) layoutAlarmTab.setVisibility(index == 1 ? View.VISIBLE : View.INVISIBLE);
            if (layoutChartTab != null) layoutChartTab.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
            if (layoutReportTab != null) layoutReportTab.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
            if (layoutSettingsTab != null) layoutSettingsTab.setVisibility(index == 4 ? View.VISIBLE : View.GONE);

            if (fabAddEvent != null) {
                if (index == 0 && (adapter == null || !adapter.isMultiSelectMode())) fabAddEvent.show();
                else fabAddEvent.hide();
            }
            if (fabScrollToTop != null) {
                fabScrollToTop.hide();
            }

            if (index != 3) {
                if (fileHistoryAdapter != null && fileHistoryAdapter.isMultiSelectMode()) {
                    fileHistoryAdapter.setMultiSelectMode(false);
                }
                View fileBar = findViewById(R.id.layoutFileMultiSelectBar);
                if (fileBar != null) fileBar.setVisibility(View.GONE);
            }
            if (index != 0 && index != 1) {
                if (adapter != null && adapter.isMultiSelectMode()) {
                    adapter.setMultiSelectMode(false);
                }
                if (tab2Adapter != null && tab2Adapter.isMultiSelectMode()) {
                    tab2Adapter.setMultiSelectMode(false);
                }
                View multiBar = findViewById(R.id.layoutMultiSelectBar);
                if (multiBar != null) multiBar.setVisibility(View.GONE);
            }

            if (index == 0) {
                refreshData();
            } else if (index == 1) {
                if (layoutAlarmTab != null) {
                    layoutAlarmTab.post(this::refreshTab2CalendarEvents);
                } else {
                    refreshTab2CalendarEvents();
                }
            } else if (index == 2) {
                if (layoutChartTab != null) layoutChartTab.post(this::updateChartTab);
                else updateChartTab();
            } else if (index == 3) {
                if (layoutReportTab != null) layoutReportTab.post(this::refreshFileHistory);
                else refreshFileHistory();
            }
        };

        View touchTargetView = cardBottomNav != null ? cardBottomNav : layoutNavContainer;
        if (touchTargetView != null) {
            touchTargetView.setOnTouchListener(new View.OnTouchListener() {
                private float startX = 0f;
                private float lastX = 0f;
                private boolean isDragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    int totalWidth = layoutNavContainer != null ? layoutNavContainer.getWidth() : v.getWidth();
                    if (totalWidth <= 0) return false;
                    int tabWidth = totalWidth / 5;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            lastX = startX;
                            isDragging = false;
                            if (viewActivePillIndicator != null) {
                                viewActivePillIndicator.animate().cancel();
                            }
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float currentX = event.getX();
                            float deltaX = currentX - lastX;
                            lastX = currentX;

                            if (Math.abs(currentX - startX) > 5) {
                                isDragging = true;
                            }
                            if (isDragging && viewActivePillIndicator != null) {
                                float clampedX = Math.max(0, Math.min(totalWidth - tabWidth, currentX - tabWidth / 2f));
                                viewActivePillIndicator.setTranslationX(clampedX);

                                float speed = Math.abs(deltaX);
                                float stretchX = 1.0f + Math.min(0.35f, speed / 25.0f);
                                float compressY = 1.0f - Math.min(0.18f, speed / 50.0f);

                                if (deltaX > 0) {
                                    viewActivePillIndicator.setPivotX(0f);
                                } else if (deltaX < 0) {
                                    viewActivePillIndicator.setPivotX(viewActivePillIndicator.getWidth());
                                                                }

                                viewActivePillIndicator.setScaleX(stretchX);
                                viewActivePillIndicator.setScaleY(compressY);

                                int hoverIndex = Math.min(4, Math.max(0, Math.round(clampedX / tabWidth)));
                                int activeColor = Color.parseColor("#6D28D9");
                                int inactiveColor = Color.parseColor("#64748B");
                                for (int i = 0; i < navIcons.length; i++) {
                                    boolean isActive = (i == hoverIndex);
                                    if (navIcons[i] != null) navIcons[i].setColorFilter(isActive ? activeColor : inactiveColor);
                                    if (navTexts[i] != null) navTexts[i].setTextColor(isActive ? activeColor : inactiveColor);
                                }
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.performClick();
                            if (v.getParent() != null) {
                                v.getParent().requestDisallowInterceptTouchEvent(false);
                            }

                            if (viewActivePillIndicator != null) {
                                viewActivePillIndicator.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(280)
                                        .setInterpolator(new OvershootInterpolator(2.2f))
                                        .start();
                            }

                            int targetIndex;
                            if (!isDragging) {
                                targetIndex = Math.min(4, Math.max(0, (int) (startX / tabWidth)));
                            } else {
                                float finalX = viewActivePillIndicator != null ? viewActivePillIndicator.getTranslationX() : startX;
                                targetIndex = Math.min(4, Math.max(0, Math.round(finalX / tabWidth)));
                            }
                            switchTab.accept(targetIndex);
                            return true;
                    }
                    return false;
                }
            });
        }

        if (savedInitialTabIndex > 0 && savedInitialTabIndex < 5) {
            final int restoredTab = savedInitialTabIndex;
            if (layoutNavContainer != null) {
                layoutNavContainer.post(() -> switchTab.accept(restoredTab));
            } else {
                switchTab.accept(restoredTab);
            }
        }
    }

    private void setupAutoDndSettings() {
        android.widget.CompoundButton swAutoDnd = findViewById(R.id.swAutoDnd);
        android.widget.CompoundButton swAutoCheckin = findViewById(R.id.swAutoCheckin);

        if (swAutoDnd != null) {
            swAutoDnd.setChecked(AutoDndHelper.isAutoDndEnabled(this));
            swAutoDnd.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked && !AutoDndHelper.hasDndPermission(this)) {
                    Toast.makeText(this, "Vui lòng cấp quyền Không Làm Phiền (DND)!", Toast.LENGTH_LONG).show();
                    AutoDndHelper.requestDndPermission(this);
                    btn.setChecked(false); // Reset while waiting for permission
                } else {
                    AutoDndHelper.setAutoDndEnabled(this, isChecked);
                    AutoDndScheduler.syncTodayAlarms(this);
                }
            });
        }

        if (swAutoCheckin != null) {
            swAutoCheckin.setChecked(AutoDndHelper.isAutoCheckinEnabled(this));
            swAutoCheckin.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
                    btn.setChecked(false);
                } else {
                    AutoDndHelper.setAutoCheckinEnabled(this, isChecked);
                    AutoDndScheduler.syncTodayAlarms(this);
                }
            });
        }
    }

    private void setupThemeSettings() {
        setupAutoDndSettings();
        
        RadioGroup rgThemeMode = findViewById(R.id.rgThemeMode);
        RadioButton rbThemeSystem = findViewById(R.id.rbThemeSystem);
        RadioButton rbThemeLight = findViewById(R.id.rbThemeLight);
        RadioButton rbThemeDark = findViewById(R.id.rbThemeDark);

        if (rgThemeMode == null) return;

        int currentMode = ThemeHelper.getThemeMode(this);
        if (currentMode == ThemeHelper.MODE_LIGHT) {
            if (rbThemeLight != null) rbThemeLight.setChecked(true);
        } else if (currentMode == ThemeHelper.MODE_DARK) {
            if (rbThemeDark != null) rbThemeDark.setChecked(true);
        } else {
            if (rbThemeSystem != null) rbThemeSystem.setChecked(true);
        }

        rgThemeMode.setOnCheckedChangeListener((group, checkedId) -> {
            int targetMode;
            if (checkedId == R.id.rbThemeLight) {
                targetMode = ThemeHelper.MODE_LIGHT;
            } else if (checkedId == R.id.rbThemeDark) {
                targetMode = ThemeHelper.MODE_DARK;
            } else {
                targetMode = ThemeHelper.MODE_SYSTEM;
            }

            if (ThemeHelper.getThemeMode(MainActivity.this) != targetMode) {
                ThemeHelper.setThemeMode(MainActivity.this, targetMode);
            }
        });
    }

    private void setupFontSizeSettings() {
        RadioGroup rgFontSizeMode = findViewById(R.id.rgFontSizeMode);
        RadioButton rbFontSmall = findViewById(R.id.rbFontSmall);
        RadioButton rbFontMedium = findViewById(R.id.rbFontMedium);
        RadioButton rbFontLarge = findViewById(R.id.rbFontLarge);

        if (rgFontSizeMode == null) return;

        int currentMode = FontScaleHelper.getFontSizeMode(this);
        if (currentMode == FontScaleHelper.FONT_SMALL) {
            if (rbFontSmall != null) rbFontSmall.setChecked(true);
        } else if (currentMode == FontScaleHelper.FONT_LARGE) {
            if (rbFontLarge != null) rbFontLarge.setChecked(true);
        } else {
            if (rbFontMedium != null) rbFontMedium.setChecked(true);
        }

        rgFontSizeMode.setOnCheckedChangeListener((group, checkedId) -> {
            int targetMode;
            if (checkedId == R.id.rbFontSmall) {
                targetMode = FontScaleHelper.FONT_SMALL;
            } else if (checkedId == R.id.rbFontLarge) {
                targetMode = FontScaleHelper.FONT_LARGE;
            } else {
                targetMode = FontScaleHelper.FONT_MEDIUM;
            }

            if (FontScaleHelper.getFontSizeMode(MainActivity.this) != targetMode) {
                FontScaleHelper.setFontSizeMode(MainActivity.this, targetMode);
                recreate();
            }
        });
    }

    private void setupFilterChips() {
        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipTeaching = findViewById(R.id.chipTeaching);
        TextView chipMeeting = findViewById(R.id.chipMeeting);
        TextView chipIncident = findViewById(R.id.chipIncident);
        TextView chipManual = findViewById(R.id.chipManual);
        TextView chipToday = findViewById(R.id.chipToday);
        TextView chipFreeTime = findViewById(R.id.chipFreeTime);

        TextView[] allChips = new TextView[]{chipAll, chipTeaching, chipMeeting, chipIncident, chipManual, chipToday, chipFreeTime};

        View.OnClickListener chipClickListener = v -> {
            for (TextView c : allChips) {
                if (c != null) {
                    c.setBackgroundResource(R.drawable.bg_chip_unselected);
                    c.setTextColor(Color.parseColor("#334155"));
                }
            }
            v.setBackgroundResource(R.drawable.bg_chip_selected);
            ((TextView) v).setTextColor(Color.parseColor("#FFFFFF"));

            int id = v.getId();
            if (id == R.id.chipAll) {
                currentCategoryFilter = "ALL";
                currentCalendarMode = 2;
                isFilteringToday = false;
                edtSearch.setText("");
                if (spnSemester != null) spnSemester.setSelection(0);
                if (spnYear != null) spnYear.setSelection(0);
                refreshData();
            } else if (id == R.id.chipTeaching) {
                currentCategoryFilter = EventModel.CAT_TEACHING;
                isFilteringToday = false;
                edtSearch.setText("");
                refreshData();
            } else if (id == R.id.chipMeeting) {
                currentCategoryFilter = EventModel.CAT_EVENT;
                isFilteringToday = false;
                edtSearch.setText("");
                refreshData();
            } else if (id == R.id.chipIncident) {
                currentCategoryFilter = EventModel.CAT_INCIDENT;
                isFilteringToday = false;
                edtSearch.setText("");
                refreshData();
            } else if (id == R.id.chipManual) {
                currentCategoryFilter = EventModel.CAT_MANUAL_OTHER;
                isFilteringToday = false;
                edtSearch.setText("");
                refreshData();
            } else if (id == R.id.chipToday) {
                currentCategoryFilter = "ALL";
                currentCalendarMode = 0;
                isFilteringToday = true;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = sdf.format(Calendar.getInstance().getTime());
                edtSearch.setText(todayStr);
            } else if (id == R.id.chipFreeTime) {
                isFilteringToday = false;
                showFreeTimeAnalysis();
            }
        };

        for (TextView c : allChips) {
            if (c != null) c.setOnClickListener(chipClickListener);
        }

        TextView chipTimeline = findViewById(R.id.chipTimeline);
        TextView chipSmartFinder = findViewById(R.id.chipSmartFinder);

        if (chipTimeline != null) {
            chipTimeline.setOnClickListener(v -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = sdf.format(Calendar.getInstance().getTime());
                showContinuousTimeline(todayStr);
            });
        }

        if (chipSmartFinder != null) {
            chipSmartFinder.setOnClickListener(v -> showSmartSlotFinderDialog());
        }
    }

    private void setupSpinners() {
        String[] semesters = {"Tất cả HK", "HK1", "HK2", "HK3"};
        spnSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semesters));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String[] years = {"Tất cả Năm", String.valueOf(currentYear), String.valueOf(currentYear - 1), String.valueOf(currentYear + 1)};
        spnYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));

        String[] chartTypes = {"Biểu đồ theo Học kỳ", "Biểu đồ theo Tuần"};
        spnChartType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, chartTypes));

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { refreshAllTabs(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spnSemester.setOnItemSelectedListener(filterListener);
        spnYear.setOnItemSelectedListener(filterListener);
        spnChartType.setOnItemSelectedListener(filterListener);
    }

    private void refreshAllTabs() {
        isTab2Dirty = true;
        refreshData();
        refreshTab2CalendarEvents();
        updateChartTab();
        refreshFileHistory();
        AutoDndScheduler.syncTodayAlarms(this);
    }

    private void refreshData() {
        if (edtSearch == null || dbHelper == null || adapter == null) return;

        String keyword = edtSearch.getText().toString();
        String sem = spnSemester != null && spnSemester.getSelectedItem() != null ? spnSemester.getSelectedItem().toString() : "Tất cả HK";
        String yr = spnYear != null && spnYear.getSelectedItem() != null ? spnYear.getSelectedItem().toString() : "Tất cả Năm";

        List<EventModel> displayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        if (!keyword.trim().isEmpty()) {
            // Khi người dùng nhập từ khóa tìm kiếm, hiển thị trực tiếp tất cả kết quả khớp từ khóa
            displayList.addAll(dbHelper.searchEvents(keyword, sem, yr));
        } else if (currentCalendarMode == 0) {
            String todayIso = sdf.format(cal.getTime());
            List<EventModel> todayEvents = dbHelper.searchEvents(todayIso, sem, yr);

            for (EventModel e : todayEvents) {
                if (e.getId() > 0) displayList.add(e);
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-100, 0, "☕ Hôm nay không có lịch bận", todayIso, "Bạn đang có trọn vẹn cả ngày trống rảnh để nghỉ ngơi hoặc làm việc cá nhân.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }

        } else if (currentCalendarMode == 1) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

            for (int i = 0; i < 7; i++) {
                String dateIso = sdf.format(cal.getTime());
                List<EventModel> dayEvents = dbHelper.searchEvents(dateIso, sem, yr);
                for (EventModel e : dayEvents) {
                    if (e.getId() > 0) displayList.add(e);
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-200, 0, "☕ Tuần này không có lịch bận", sdf.format(new Date()), "Tất cả các ngày trong tuần này đều chưa có lịch giảng dạy hay lịch họp.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }

        } else if (currentCalendarMode == 2) {
            // Chế độ TẤT CẢ LỊCH BIỂU: Hiển thị toàn bộ lịch theo bộ lọc Học kỳ / Năm
            List<EventModel> allEvents = dbHelper.searchEvents("", sem, yr);
            for (EventModel e : allEvents) {
                if (e.getId() > 0) {
                    displayList.add(e);
                }
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-300, 0, "☕ Chưa có lịch bận", sdf.format(new Date()), "Chưa có công việc nào được ghi nhận trong ứng dụng.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }

        } else {
            // Chế độ TRỐNG: Phân tích chi tiết từng khung giờ trống sáng & chiều của 7 ngày tới
            for (int i = 0; i < 7; i++) {
                String dateIso = sdf.format(cal.getTime());
                List<EventModel> events = dbHelper.searchEvents(dateIso, sem, yr);
                DayFreeTimeResult res = analyzeDayFreeTime(dateIso, events);

                displayList.add(new EventModel(
                        -400 - i,
                        i + 1,
                        res.statusBadge,
                        dateIso,
                        res.summaryText,
                        "HK1",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.WEEK_OF_YEAR)));

                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        if (!"ALL".equals(currentCategoryFilter)) {
            List<EventModel> filtered = new ArrayList<>();
            for (EventModel em : displayList) {
                if (em.getId() < 0) continue;
                String cat = em.getScheduleCategory();
                if (currentCategoryFilter.equals(cat)) {
                    filtered.add(em);
                } else if (EventModel.CAT_MANUAL_OTHER.equals(currentCategoryFilter)
                        || EventModel.LEGACY_CAT_MANUAL_OTHER.equals(currentCategoryFilter)
                        || "Lịch khác".equals(currentCategoryFilter)) {
                    if (!EventModel.CAT_TEACHING.equalsIgnoreCase(cat)
                            && !EventModel.CAT_EVENT.equalsIgnoreCase(cat)
                            && !EventModel.CAT_INCIDENT.equalsIgnoreCase(cat)) {
                        filtered.add(em);
                    }
                }
            }
            displayList = filtered;
            if (displayList.isEmpty()) {
                String filterName = (EventModel.CAT_MANUAL_OTHER.equals(currentCategoryFilter) || EventModel.LEGACY_CAT_MANUAL_OTHER.equals(currentCategoryFilter)) ? "Lịch khác" : currentCategoryFilter;
                displayList.add(new EventModel(-400, 0, "☕ Không có lịch thuộc nhóm này", sdf.format(new Date()), "Chưa có sự kiện nào thuộc phân loại: " + filterName, "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
            }
        }

        adapter.updateList(displayList);
        isTab2Dirty = true;
        if (customCalendarAdapter != null) {
            updateCustomCalendarView();
        }
        if (currentActiveTabIndex == 1) {
            refreshTab2CalendarEvents();
        }
        if (tvTotalCount != null) {
            tvTotalCount.setText(displayList.size() + " Lịch biểu");
        }
        if (barChart != null && barChart.isShown()) {
            updateChart();
        }
    }

    private void updateChart() {
        updateChartTab();
    }

    private void initChartTabViews() {
        spnChartTimeFilter = findViewById(R.id.spnChartTimeFilter);
        spnChartSchoolFilter = findViewById(R.id.spnChartSchoolFilter);
        tvKpiPeriods = findViewById(R.id.tvKpiPeriods);
        tvKpiPeriodsSub = findViewById(R.id.tvKpiPeriodsSub);
        tvKpiEvents = findViewById(R.id.tvKpiEvents);
        tvKpiEventsSub = findViewById(R.id.tvKpiEventsSub);
        tvKpiSchools = findViewById(R.id.tvKpiSchools);
        tvKpiSchoolsSub = findViewById(R.id.tvKpiSchoolsSub);
        tvKpiIncidents = findViewById(R.id.tvKpiIncidents);
        tvKpiIncidentsSub = findViewById(R.id.tvKpiIncidentsSub);
        tvKpiIncome = findViewById(R.id.tvKpiIncome);
        tvKpiIncomeSub = findViewById(R.id.tvKpiIncomeSub);
        cardCrossSchoolAlert = findViewById(R.id.cardCrossSchoolAlert);
        tvCrossSchoolAlertTitle = findViewById(R.id.tvCrossSchoolAlertTitle);
        tvCrossSchoolAlertDesc = findViewById(R.id.tvCrossSchoolAlertDesc);
        btnViewCrossSchoolDays = findViewById(R.id.btnViewCrossSchoolDays);
        pieChartSchool = findViewById(R.id.pieChartSchool);
        barChartNature = findViewById(R.id.barChartNature);
        barChartDayOfWeek = findViewById(R.id.barChartDayOfWeek);
        pieChartShift = findViewById(R.id.pieChartShift);
        tvMonthlyPeakNotice = findViewById(R.id.tvMonthlyPeakNotice);
        cardKpiPeriods = findViewById(R.id.cardKpiPeriods);
        cardKpiEvents = findViewById(R.id.cardKpiEvents);
        cardKpiSchools = findViewById(R.id.cardKpiSchools);
        cardKpiIncidents = findViewById(R.id.cardKpiIncidents);
        cardKpiIncome = findViewById(R.id.cardKpiIncome);

        setupChartSpinners();

        if (cardKpiEvents != null) {
            cardKpiEvents.setOnClickListener(v -> {
                showChartDrillDownDialog("TỔNG SỐ BUỔI CÔNG VIỆC", 
                        "Danh sách toàn bộ " + currentFilteredChartEvents.size() + " buổi", currentFilteredChartEvents);
            });
        }
        if (cardKpiPeriods != null) {
            cardKpiPeriods.setOnClickListener(v -> {
                showChartDrillDownDialog("DANH SÁCH GIỜ / TIẾT GIẢNG", 
                        "Chi tiết các buổi dạy trong phạm vi lọc", currentFilteredChartEvents);
            });
        }
        if (cardKpiIncidents != null) {
            cardKpiIncidents.setOnClickListener(v -> {
                List<EventModel> incidentEvents = new ArrayList<>();
                for (EventModel e : currentFilteredChartEvents) {
                    String nat = e.getScheduleCategory();
                    if (EventModel.CAT_INCIDENT.equals(nat)) {
                        incidentEvents.add(e);
                    }
                }
                showChartDrillDownDialog("DANH SÁCH LỊCH PHÁT SINH", 
                        "Gồm " + incidentEvents.size() + " buổi phát sinh (dạy bù, coi thi, đột xuất...)", incidentEvents);
            });
        }
        if (cardKpiSchools != null) {
            cardKpiSchools.setOnClickListener(v -> {
                showChartDrillDownDialog("DANH SÁCH CÔNG VIỆC THEO TRƯỜNG", 
                        "Toàn bộ lịch các đơn vị đang tham gia", currentFilteredChartEvents);
            });
        }
        
        if (cardKpiIncome != null) {
            cardKpiIncome.setOnClickListener(v -> {
                showEditIncomeRatesDialog();
            });
        }
        
        if (btnViewCrossSchoolDays != null) {
            btnViewCrossSchoolDays.setOnClickListener(v -> showCrossSchoolAlertDetailsDialog());
        }
        if (cardCrossSchoolAlert != null) {
            cardCrossSchoolAlert.setOnClickListener(v -> showCrossSchoolAlertDetailsDialog());
        }
    }

    private void setupChartSpinners() {
        if (spnChartTimeFilter == null || spnChartSchoolFilter == null) return;

        Calendar cal = Calendar.getInstance();
        int curMonth = cal.get(Calendar.MONTH) + 1;
        int curYear = cal.get(Calendar.YEAR);

        List<String> timeOptions = new ArrayList<>();
        timeOptions.add("Tất cả thời gian");
        timeOptions.add("Năm nay (" + curYear + ")");
        timeOptions.add("Tháng này (T" + curMonth + "/" + curYear + ")");
        timeOptions.add("Tháng trước");
        for (int m = 1; m <= 12; m++) {
            timeOptions.add("Tháng " + m + " (" + curYear + ")");
        }

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, R.layout.item_chart_spinner, timeOptions);
        timeAdapter.setDropDownViewResource(R.layout.item_chart_spinner_dropdown);
        spnChartTimeFilter.setAdapter(timeAdapter);

        List<String> schoolOptions = new ArrayList<>();
        schoolOptions.add("Tất cả đơn vị / trường");
        schoolOptions.add("NTTU (ĐH Nguyễn Tất Thành)");
        schoolOptions.add("HUTECH (ĐH Công nghệ TP.HCM)");
        schoolOptions.add("UEF (ĐH Kinh tế - Tài chính)");
        schoolOptions.add("Khác / Ngoài trường");

        ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this, R.layout.item_chart_spinner, schoolOptions);
        schoolAdapter.setDropDownViewResource(R.layout.item_chart_spinner_dropdown);
        spnChartSchoolFilter.setAdapter(schoolAdapter);

        AdapterView.OnItemSelectedListener filterChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateChartTab();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spnChartTimeFilter.setOnItemSelectedListener(filterChangeListener);
        spnChartSchoolFilter.setOnItemSelectedListener(filterChangeListener);
    }

    public static String classifyEventNature(String title, String organizer) {
        String t = (title != null ? title : "").trim();
        String o = (organizer != null ? organizer : "").trim();
        String text = (t + " " + o).toLowerCase(Locale.getDefault());

        // 1. LỊCH PHÁT SINH
        if (text.contains("dạy bù") || text.contains("học bù") || text.contains("bù") || 
            text.contains("tăng tiết") || text.contains("đổi ca") || text.contains("dời lịch") || text.contains("dời phòng") ||
            text.contains("coi thi") || text.contains("chấm thi") || text.contains("khảo thí") || text.contains("hội đồng") || 
            text.contains("bảo vệ") || text.contains("đồ án") || text.contains("khóa luận") || text.contains("vấn đáp") ||
            text.contains("phát sinh") || text.contains("đột xuất") || text.contains("phòng thi") || text.contains("ca thi")) {
            return EventModel.CAT_INCIDENT;
        }

        // 2. LỊCH SỰ KIỆN
        if (text.contains("thông báo") || text.contains("họp") || text.contains("tập huấn") || text.contains("hội thảo") || 
            text.contains("hội nghị") || text.contains("khai giảng") || text.contains("tốt nghiệp") || text.contains("đại hội") || 
            text.contains("kỷ niệm") || text.contains("lễ") || text.contains("sinh hoạt") || text.contains("giao ban") || 
            text.contains("sự kiện") || text.contains("công tác tuần") || text.contains("lịch công tác") ||
            text.contains("khoa") || text.contains("bộ môn") || text.contains("nhà trường")) {
            EventModel dummy = new EventModel(0, 0, t, "", o, "", 0, 0);
            if (!dummy.isTeachingEvent()) {
                return EventModel.CAT_EVENT;
            }
        }

        // 3. LỊCH NHẬP THỦ CÔNG(KHÁC)
        if (text.contains("thủ công") || text.contains("việc riêng") || text.contains("cá nhân") || text.contains("khám bệnh") || 
            text.contains("nghỉ") || text.contains("hẹn") || text.contains("gia đình") || text.contains("soạn") ||
            text.contains("đề thi") || text.contains("bảng điểm") || text.contains("nhắc nhở") || text.contains("nhắc việc") ||
            text.contains("chuẩn bị") || text.contains("ghi chú") || o.contains("thủ công") || o.contains("Cá nhân")) {
            return EventModel.CAT_MANUAL_OTHER;
        }

        // 4. LỊCH DẠY/HỌC
        EventModel dummy = new EventModel(0, 0, t, "", o, "", 0, 0);
        if (dummy.isTeachingEvent()) {
            return EventModel.CAT_TEACHING;
        }

        return EventModel.CAT_MANUAL_OTHER;
    }

    public static int extractPeriodsCount(EventModel em) {
        if (em == null) return 0;
        String title = em.getTitle() != null ? em.getTitle() : "";

        // 1. Quét số tiết rõ ràng (VD: Tiết 2-6 -> 5 tiết, Tiết 7-11 -> 5 tiết)
        // Loại trừ từ 'chi tiết', 'tiết kiệm'
        Pattern p = Pattern.compile("(?i)(?<![a-zA-ZÀ-ỹ0-9_])(?<!chi\\s?)(?<!tình\\s?)\\btiết\\s*[:=\\s-]*(\\d{1,2})\\s*[-–đếnto]+\\s*(\\d{1,2})\\b");
        Matcher m = p.matcher(title);
        if (m.find()) {
            try {
                int p1 = Integer.parseInt(m.group(1));
                int p2 = Integer.parseInt(m.group(2));
                if (p2 >= p1 && (p2 - p1 + 1) <= 15) {
                    return p2 - p1 + 1;
                }
            } catch (Exception ignored) {}
        }

        // 2. NẾU KHÔNG PHẢI LỊCH GIẢNG DẠY (CV, sơ yếu lý lịch, họp, việc riêng): SỐ TIẾT LUÔN LÀ 0!
        if (!em.isTeachingEvent()) {
            return 0;
        }

        // 3. Nếu là lịch giảng dạy nhưng không ghi rõ số tiết, suy luận từ thời lượng (50 phút/tiết)
        int dur = em.getEndMin() - em.getStartMin();
        if (dur > 0) {
            int inferred = Math.round(dur / 50.0f);
            if (inferred > 0 && inferred <= 12) return inferred;
        }
        return 0;
    }

    private void updateChartTab() {
        if (dbHelper == null) return;

        List<EventModel> allEvents = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
        if (allEvents == null) allEvents = new ArrayList<>();

        // 1. Lọc theo Thời gian
        int timeFilterPos = spnChartTimeFilter != null ? spnChartTimeFilter.getSelectedItemPosition() : 0;
        Calendar calNow = Calendar.getInstance();
        int currentYear = calNow.get(Calendar.YEAR);
        int currentMonth = calNow.get(Calendar.MONTH) + 1;
        String currentYearStr = String.valueOf(currentYear);
        String currentMonthIso = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth);

        calNow.add(Calendar.MONTH, -1);
        int prevYear = calNow.get(Calendar.YEAR);
        int prevMonth = calNow.get(Calendar.MONTH) + 1;
        String prevMonthIso = String.format(Locale.getDefault(), "%04d-%02d", prevYear, prevMonth);

        List<EventModel> timeFiltered = new ArrayList<>();
        for (EventModel em : allEvents) {
            String d = em.getDate();
            if (d == null || d.length() < 7) {
                timeFiltered.add(em);
                continue;
            }
            if (timeFilterPos == 0) {
                timeFiltered.add(em);
            } else if (timeFilterPos == 1) {
                if (d.startsWith(currentYearStr + "-")) {
                    timeFiltered.add(em);
                }
            } else if (timeFilterPos == 2) {
                if (d.startsWith(currentMonthIso)) {
                    timeFiltered.add(em);
                }
            } else if (timeFilterPos == 3) {
                if (d.startsWith(prevMonthIso)) {
                    timeFiltered.add(em);
                }
            } else if (timeFilterPos >= 4 && timeFilterPos <= 15) {
                int targetMonth = timeFilterPos - 3;
                String targetMonthStr = String.format(Locale.getDefault(), "-%02d-", targetMonth);
                if (d.contains(targetMonthStr)) {
                    timeFiltered.add(em);
                }
            } else {
                timeFiltered.add(em);
            }
        }

        // 2. Lọc theo Trường / Đơn vị
        int schoolFilterPos = spnChartSchoolFilter != null ? spnChartSchoolFilter.getSelectedItemPosition() : 0;
        List<EventModel> filtered = new ArrayList<>();
        for (EventModel em : timeFiltered) {
            String sc = em.getSchoolCode();
            if (sc == null) sc = "";
            if (schoolFilterPos == 0) {
                filtered.add(em);
            } else if (schoolFilterPos == 1) {
                if ("NTTU".equalsIgnoreCase(sc)) filtered.add(em);
            } else if (schoolFilterPos == 2) {
                if ("HUTECH".equalsIgnoreCase(sc)) filtered.add(em);
            } else if (schoolFilterPos == 3) {
                if ("UEF".equalsIgnoreCase(sc)) filtered.add(em);
            } else if (schoolFilterPos == 4) {
                if (!"NTTU".equalsIgnoreCase(sc) && !"HUTECH".equalsIgnoreCase(sc) && !"UEF".equalsIgnoreCase(sc)) {
                    filtered.add(em);
                }
            } else {
                filtered.add(em);
            }
        }

        currentFilteredChartEvents = filtered;

        // Theme colors (Light vs Dark mode)
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int textColorPrimary = isDarkMode ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        int textColorSecondary = isDarkMode ? Color.parseColor("#94A3B8") : Color.parseColor("#475569");
        int gridLineColor = isDarkMode ? Color.parseColor("#334155") : Color.parseColor("#E2E8F0");

        // 3. Tính toán 4 thẻ KPI + 1 Dự toán Thu nhập
        int totalEvents = filtered.size();
        int totalPeriods = 0;
        float totalHours = 0f;
        int completedEvents = 0;
        int incidentEvents = 0;
        long estimatedIncome = 0;
        int completedPeriods = 0;
        Set<String> activeSchools = new HashSet<>();
        String todayIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (EventModel em : filtered) {
            int pCount = extractPeriodsCount(em);
            totalPeriods += pCount;
            totalHours += (pCount * 45f / 60f);

            boolean isCompleted = false;
            if (em.getDate() != null && em.getDate().compareTo(todayIso) <= 0) {
                completedEvents++;
                isCompleted = true;
                completedPeriods += pCount;
            }

            String scode = em.getSchoolCode();
            if (scode == null || "OTHER".equals(scode)) {
                if (em.getOrganizer() != null && !em.getOrganizer().trim().isEmpty()) {
                    scode = em.getOrganizer().trim().toUpperCase(Locale.getDefault());
                } else {
                    scode = "OTHER";
                }
            }

            if (em.isTeachingEvent() && isCompleted) {
                // Tính thu nhập: (số tiết) * (đơn giá của trường do người dùng cấu hình)
                SchoolProfile.SchoolType sc = SchoolProfile.detectSchool(scode);
                long customRate = getCustomPayRate(this, scode, sc != null && sc != SchoolProfile.SchoolType.DEFAULT ? sc.getDefaultPayRate() : 100000);
                estimatedIncome += (pCount * customRate);
            }

            String nat = em.getScheduleCategory();
            if (EventModel.CAT_INCIDENT.equals(nat)) {
                incidentEvents++;
            }

            if (scode != null && !scode.isEmpty()) {
                activeSchools.add(scode);
            }
        }

        if (tvKpiPeriods != null) {
            tvKpiPeriods.setText(totalPeriods + " Tiết");
        }
        if (tvKpiPeriodsSub != null) {
            tvKpiPeriodsSub.setText(String.format(Locale.getDefault(), "%.1f giờ chuẩn", totalHours));
        }
        if (tvKpiEvents != null) {
            tvKpiEvents.setText(totalEvents + " Buổi");
        }
        if (tvKpiEventsSub != null) {
            int upcoming = Math.max(0, totalEvents - completedEvents);
            tvKpiEventsSub.setText(completedEvents + " đã dạy • " + upcoming + " sắp tới");
        }
        if (tvKpiSchools != null) {
            tvKpiSchools.setText(activeSchools.size() + " Trường");
        }
        if (tvKpiSchoolsSub != null) {
            tvKpiSchoolsSub.setText(activeSchools.isEmpty() ? "Chưa có lịch" : TextUtils.join(", ", activeSchools));
        }
        if (tvKpiIncidents != null) {
            float ratio = totalEvents > 0 ? (incidentEvents * 100f / totalEvents) : 0f;
            tvKpiIncidents.setText(incidentEvents + " Buổi (" + Math.round(ratio) + "%)");
        }
        if (tvKpiIncidentsSub != null) {
            tvKpiIncidentsSub.setText("Dạy bù, coi thi, họp...");
        }
        if (tvKpiIncome != null) {
            java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
            tvKpiIncome.setText(formatter.format(estimatedIncome) + " VNĐ");
        }
        if (tvKpiIncomeSub != null) {
            tvKpiIncomeSub.setText("Đã tính " + completedPeriods + " tiết đã dạy. (Đơn giá tham khảo)");
        }

        // 4. Quét phát hiện ngày chạy đa trường
        detectCrossSchoolDays(filtered);

        // 5. Render biểu đồ tròn theo Trường
        renderSchoolPieChart(filtered, textColorPrimary, textColorSecondary);

        // 6. Render biểu đồ cột ngang phân loại tính chất công việc
        renderNatureBarChart(filtered, textColorPrimary, textColorSecondary, gridLineColor);

        // 7. Render biểu đồ cột 12 tháng trong năm
        renderMonthlyBarChart(allEvents, textColorPrimary, textColorSecondary, gridLineColor);

        // 8. Render biểu đồ mật độ theo thứ trong tuần
        renderDayOfWeekBarChart(filtered, textColorPrimary, textColorSecondary, gridLineColor);

        // 9. Render biểu đồ tròn phân bổ ca dạy
        renderShiftPieChart(filtered, textColorPrimary, textColorSecondary);
    }

    private void detectCrossSchoolDays(List<EventModel> events) {
        crossSchoolDaysMap.clear();
        Map<String, Set<String>> dateToSchools = new HashMap<>();
        Map<String, List<EventModel>> dateToEvents = new HashMap<>();

        for (EventModel em : events) {
            String d = em.getDate();
            if (d == null || d.trim().isEmpty()) continue;
            String sc = em.getSchoolCode();
            if (sc == null || sc.trim().isEmpty()) sc = "OTHER";

            dateToSchools.computeIfAbsent(d, k -> new HashSet<>()).add(sc.toUpperCase(Locale.getDefault()));
            dateToEvents.computeIfAbsent(d, k -> new ArrayList<>()).add(em);
        }

        for (Map.Entry<String, Set<String>> entry : dateToSchools.entrySet()) {
            if (entry.getValue().size() >= 2) {
                crossSchoolDaysMap.put(entry.getKey(), dateToEvents.get(entry.getKey()));
            }
        }

        if (cardCrossSchoolAlert != null) {
            if (!crossSchoolDaysMap.isEmpty()) {
                cardCrossSchoolAlert.setVisibility(View.VISIBLE);
                if (tvCrossSchoolAlertTitle != null) {
                    tvCrossSchoolAlertTitle.setText("⚠️ CẢNH BÁO: " + crossSchoolDaysMap.size() + " NGÀY DI CHUYỂN ĐA TRƯỜNG");
                }
                if (tvCrossSchoolAlertDesc != null) {
                    tvCrossSchoolAlertDesc.setText("Phát hiện " + crossSchoolDaysMap.size() + " ngày có lịch tại từ 2 trường khác nhau trong cùng ngày. Chú ý dự phòng thời gian di chuyển!");
                }
            } else {
                cardCrossSchoolAlert.setVisibility(View.GONE);
            }
        }
    }

    private void showCrossSchoolAlertDetailsDialog() {
        List<EventModel> crossEvents = new ArrayList<>();
        for (List<EventModel> list : crossSchoolDaysMap.values()) {
            crossEvents.addAll(list);
        }
        EventModel.sortChronologically(crossEvents);
        showChartDrillDownDialog("⚠️ CẢNH BÁO DI CHUYỂN ĐA TRƯỜNG", 
                "Gồm " + crossSchoolDaysMap.size() + " ngày có lịch tại từ 2 trường khác nhau", crossEvents);
    }

    private void renderSchoolPieChart(List<EventModel> events, int textColorPrimary, int textColorSecondary) {
        if (pieChartSchool == null) return;
        Map<String, List<EventModel>> schoolMap = new LinkedHashMap<>();
        for (EventModel em : events) {
            String sc = em.getSchoolCode();
            if (sc == null || sc.trim().isEmpty()) sc = "OTHER";
            sc = sc.toUpperCase(Locale.getDefault());
            schoolMap.computeIfAbsent(sc, k -> new ArrayList<>()).add(em);
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (Map.Entry<String, List<EventModel>> entry : schoolMap.entrySet()) {
            String code = entry.getKey();
            int count = entry.getValue().size();
            String label = code;
            int color = Color.parseColor("#8B5CF6");
            if ("NTTU".equalsIgnoreCase(code)) {
                label = "NTTU (" + count + ")";
                color = Color.parseColor("#1D4ED8");
            } else if ("HUTECH".equalsIgnoreCase(code)) {
                label = "HUTECH (" + count + ")";
                color = Color.parseColor("#EA580C");
            } else if ("UEF".equalsIgnoreCase(code)) {
                label = "UEF (" + count + ")";
                color = Color.parseColor("#0284C7");
            } else if ("UEH".equalsIgnoreCase(code)) {
                label = "UEH (" + count + ")";
                color = Color.parseColor("#059669");
            } else {
                label = "Khác (" + count + ")";
                color = Color.parseColor("#64748B");
            }
            entries.add(new PieEntry(count, label));
            colors.add(color);
        }

        if (entries.isEmpty()) {
            pieChartSchool.clear();
            pieChartSchool.setNoDataText("Chưa có dữ liệu trường học");
            pieChartSchool.setNoDataTextColor(textColorSecondary);
            pieChartSchool.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextSize(11.5f);
        dataSet.setValueTextColor(Color.WHITE);
        // Chỉ vẽ % lên lát cắt nếu lát cắt >= 8% để loại bỏ hoàn toàn đè chữ!
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value < 8f) return "";
                return Math.round(value) + "%";
            }
        });

        PieData pieData = new PieData(dataSet);
        pieChartSchool.setData(pieData);
        pieChartSchool.setUsePercentValues(true);
        pieChartSchool.getDescription().setEnabled(false);
        pieChartSchool.setDrawHoleEnabled(true);
        pieChartSchool.setHoleColor(Color.TRANSPARENT);
        pieChartSchool.setHoleRadius(46f);
        pieChartSchool.setTransparentCircleRadius(52f);
        pieChartSchool.setDrawEntryLabels(false); // Tắt nhãn bên trong hình tròn để không đè lên nhau

        // Chữ ở tâm vòng tròn Donut
        pieChartSchool.setCenterText(events.size() + "\nBUỔI");
        pieChartSchool.setCenterTextColor(textColorPrimary);
        pieChartSchool.setCenterTextSize(14f);
        pieChartSchool.setCenterTextTypeface(Typeface.DEFAULT_BOLD);

        // Chú thích (Legend) rõ ràng bên dưới
        Legend legend = pieChartSchool.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextColor(textColorPrimary);
        legend.setTextSize(11.5f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8.5f);
        legend.setXEntrySpace(14f);
        legend.setYEntrySpace(6f);

        pieChartSchool.setExtraOffsets(5, 5, 5, 12);

        pieChartSchool.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;
                    String rawLabel = pe.getLabel();
                    String schoolName = rawLabel != null ? rawLabel.replaceAll("\\s*\\(\\d+\\)", "").trim() : "";
                    List<EventModel> matched = new ArrayList<>();
                    for (EventModel item : events) {
                        String sc = item.getSchoolCode();
                        if (sc != null && (sc.equalsIgnoreCase(schoolName) || 
                            ("Khác".equalsIgnoreCase(schoolName) && !sc.equalsIgnoreCase("NTTU") && !sc.equalsIgnoreCase("HUTECH") && !sc.equalsIgnoreCase("UEF")))) {
                            matched.add(item);
                        }
                    }
                    showChartDrillDownDialog("ĐƠN VỊ: " + schoolName, "Gồm " + matched.size() + " buổi công việc", matched);
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        pieChartSchool.animateY(700, Easing.EaseInOutQuad);
        pieChartSchool.invalidate();
    }

    private void renderNatureBarChart(List<EventModel> events, int textColorPrimary, int textColorSecondary, int gridLineColor) {
        if (barChartNature == null) return;

        Map<String, List<EventModel>> natureMap = new LinkedHashMap<>();
        // 4 Nhóm chính hiển thị từ dưới lên trên trên biểu đồ cột ngang
        natureMap.put(EventModel.CAT_MANUAL_OTHER, new ArrayList<>());
        natureMap.put(EventModel.CAT_INCIDENT, new ArrayList<>());
        natureMap.put(EventModel.CAT_EVENT, new ArrayList<>());
        natureMap.put(EventModel.CAT_TEACHING, new ArrayList<>());

        for (EventModel em : events) {
            String cat = em.getScheduleCategory();
            if (EventModel.CAT_TEACHING.equalsIgnoreCase(cat)) {
                natureMap.get(EventModel.CAT_TEACHING).add(em);
            } else if (EventModel.CAT_EVENT.equalsIgnoreCase(cat)) {
                natureMap.get(EventModel.CAT_EVENT).add(em);
            } else if (EventModel.CAT_INCIDENT.equalsIgnoreCase(cat)) {
                natureMap.get(EventModel.CAT_INCIDENT).add(em);
            } else {
                natureMap.get(EventModel.CAT_MANUAL_OTHER).add(em);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, List<EventModel>> entry : natureMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().size()));
            labels.add(entry.getKey());
            int c;
            if (EventModel.CAT_TEACHING.equals(entry.getKey())) {
                c = Color.parseColor("#2563EB"); // Xanh lam
            } else if (EventModel.CAT_EVENT.equals(entry.getKey())) {
                c = Color.parseColor("#10B981"); // Xanh lá
            } else if (EventModel.CAT_INCIDENT.equals(entry.getKey())) {
                c = Color.parseColor("#F59E0B"); // Vàng cam
            } else {
                c = Color.parseColor("#8B5CF6"); // Tím
            }
            colors.add(c);
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số buổi");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(textColorPrimary);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " buổi";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);
        barChartNature.setData(barData);
        barChartNature.getDescription().setEnabled(false);
        barChartNature.getLegend().setEnabled(false);

        // Chống cắt lề phải chữ "buổi"
        barChartNature.setExtraRightOffset(42f);
        barChartNature.setExtraLeftOffset(8f);

        XAxis xAxis = barChartNature.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColorPrimary);
        xAxis.setTextSize(10f);

        barChartNature.getAxisRight().setEnabled(false);
        barChartNature.getAxisLeft().setDrawGridLines(true);
        barChartNature.getAxisLeft().setGridColor(gridLineColor);
        barChartNature.getAxisLeft().setTextColor(textColorSecondary);
        barChartNature.getAxisLeft().setAxisMinimum(0f);
        barChartNature.getAxisLeft().setSpaceTop(28f); // 28% lề ngang để số liệu không bị ép sát mép
        barChartNature.getAxisLeft().setGranularity(1f);

        barChartNature.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int idx = (int) e.getX();
                if (idx >= 0 && idx < labels.size()) {
                    String catName = labels.get(idx);
                    List<EventModel> list = natureMap.get(catName);
                    showChartDrillDownDialog(catName.toUpperCase(Locale.getDefault()), 
                            "Gồm " + (list != null ? list.size() : 0) + " sự kiện", list);
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        barChartNature.animateY(600, Easing.EaseInOutQuad);
        barChartNature.invalidate();
    }

    private void renderMonthlyBarChart(List<EventModel> allEvents, int textColorPrimary, int textColorSecondary, int gridLineColor) {
        if (barChartTab == null) return;

        Calendar cal = Calendar.getInstance();
        int curYear = cal.get(Calendar.YEAR);
        String yearPrefix = curYear + "-";

        int[] monthlyCounts = new int[12];
        Map<Integer, List<EventModel>> monthlyEvents = new HashMap<>();
        for (int m = 1; m <= 12; m++) monthlyEvents.put(m, new ArrayList<>());

        for (EventModel em : allEvents) {
            String d = em.getDate();
            if (d != null && d.startsWith(yearPrefix) && d.length() >= 7) {
                try {
                    int m = Integer.parseInt(d.substring(5, 7));
                    if (m >= 1 && m <= 12) {
                        monthlyCounts[m - 1]++;
                        monthlyEvents.get(m).add(em);
                    }
                } catch (Exception ignored) {}
            }
        }

        int maxMonth = 1;
        int maxCount = 0;
        for (int i = 0; i < 12; i++) {
            if (monthlyCounts[i] > maxCount) {
                maxCount = monthlyCounts[i];
                maxMonth = i + 1;
            }
        }

        if (tvMonthlyPeakNotice != null) {
            if (maxCount > 0) {
                tvMonthlyPeakNotice.setText("⚡ Tháng " + maxMonth + " là tháng cao điểm nhất trong năm " + curYear + " (" + maxCount + " buổi làm việc)");
            } else {
                tvMonthlyPeakNotice.setText("Biểu đồ theo dõi số buổi/tiết qua 12 tháng trong năm " + curYear);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < 12; i++) {
            entries.add(new BarEntry(i, monthlyCounts[i]));
            labels.add("T" + (i + 1));
            if (i == (maxMonth - 1) && maxCount > 0) {
                colors.add(Color.parseColor("#F59E0B"));
            } else {
                colors.add(Color.parseColor("#2563EB"));
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số buổi");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(textColorPrimary);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.valueOf((int) value) : "";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.50f);
        barChartTab.setData(barData);
        barChartTab.getDescription().setEnabled(false);
        barChartTab.getLegend().setEnabled(false);

        // Chống đè số trên đỉnh cột
        barChartTab.setExtraTopOffset(15f);

        XAxis xAxis = barChartTab.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColorPrimary);

        barChartTab.getAxisRight().setEnabled(false);
        barChartTab.getAxisLeft().setDrawGridLines(true);
        barChartTab.getAxisLeft().setGridColor(gridLineColor);
        barChartTab.getAxisLeft().setTextColor(textColorSecondary);
        barChartTab.getAxisLeft().setAxisMinimum(0f);
        barChartTab.getAxisLeft().setSpaceTop(30f); // 30% khoảng trống trên đỉnh để số không bị đè
        barChartTab.getAxisLeft().setGranularity(1f);

        barChartTab.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int monthIdx = (int) e.getX() + 1;
                List<EventModel> mList = monthlyEvents.get(monthIdx);
                showChartDrillDownDialog("LỊCH TRÌNH THÁNG " + monthIdx + "/" + curYear, 
                        "Tổng cộng " + (mList != null ? mList.size() : 0) + " buổi trong tháng", mList);
            }

            @Override
            public void onNothingSelected() {}
        });

        barChartTab.animateY(700, Easing.EaseInOutQuad);
        barChartTab.invalidate();
    }

    private void renderDayOfWeekBarChart(List<EventModel> events, int textColorPrimary, int textColorSecondary, int gridLineColor) {
        if (barChartDayOfWeek == null) return;

        int[] dayCounts = new int[7];
        Map<Integer, List<EventModel>> dayEvents = new HashMap<>();
        for (int i = 0; i < 7; i++) dayEvents.put(i, new ArrayList<>());

        SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (EventModel em : events) {
            String d = em.getDate();
            if (d != null && d.length() >= 10) {
                try {
                    Date dateObj = sdfIso.parse(d);
                    if (dateObj != null) {
                        cal.setTime(dateObj);
                        int dow = cal.get(Calendar.DAY_OF_WEEK);
                        int mappedIdx = (dow == Calendar.SUNDAY) ? 6 : (dow - 2);
                        if (mappedIdx >= 0 && mappedIdx < 7) {
                            dayCounts[mappedIdx]++;
                            dayEvents.get(mappedIdx).add(em);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        String[] dayLabels = {"Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật"};
        List<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, dayCounts[i]));
            if (i == 5 || i == 6) {
                colors.add(Color.parseColor("#8B5CF6"));
            } else {
                colors.add(Color.parseColor("#059669"));
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số buổi");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10.5f);
        dataSet.setValueTextColor(textColorPrimary);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.valueOf((int) value) : "";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.50f);
        barChartDayOfWeek.setData(barData);
        barChartDayOfWeek.getDescription().setEnabled(false);
        barChartDayOfWeek.getLegend().setEnabled(false);

        // Chống đè số trên đỉnh cột
        barChartDayOfWeek.setExtraTopOffset(15f);

        XAxis xAxis = barChartDayOfWeek.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColorPrimary);
        xAxis.setTextSize(10f);

        barChartDayOfWeek.getAxisRight().setEnabled(false);
        barChartDayOfWeek.getAxisLeft().setDrawGridLines(true);
        barChartDayOfWeek.getAxisLeft().setGridColor(gridLineColor);
        barChartDayOfWeek.getAxisLeft().setTextColor(textColorSecondary);
        barChartDayOfWeek.getAxisLeft().setAxisMinimum(0f);
        barChartDayOfWeek.getAxisLeft().setSpaceTop(30f); // 30% khoảng trống trên đỉnh
        barChartDayOfWeek.getAxisLeft().setGranularity(1f);

        barChartDayOfWeek.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                int dIdx = (int) e.getX();
                if (dIdx >= 0 && dIdx < dayLabels.length) {
                    List<EventModel> dList = dayEvents.get(dIdx);
                    showChartDrillDownDialog("LỊCH " + dayLabels[dIdx].toUpperCase(Locale.getDefault()), 
                            "Tổng cộng " + (dList != null ? dList.size() : 0) + " buổi vào " + dayLabels[dIdx], dList);
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        barChartDayOfWeek.animateY(650, Easing.EaseInOutQuad);
        barChartDayOfWeek.invalidate();
    }

    private void renderShiftPieChart(List<EventModel> events, int textColorPrimary, int textColorSecondary) {
        if (pieChartShift == null) return;

        List<EventModel> morningList = new ArrayList<>();
        List<EventModel> afternoonList = new ArrayList<>();
        List<EventModel> eveningList = new ArrayList<>();

        for (EventModel em : events) {
            int sm = em.getStartMin();
            if (sm < 720) {
                morningList.add(em);
            } else if (sm < 1050) {
                afternoonList.add(em);
            } else {
                eveningList.add(em);
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (!morningList.isEmpty()) {
            entries.add(new PieEntry(morningList.size(), "Ca Sáng (" + morningList.size() + ")"));
            colors.add(Color.parseColor("#F59E0B"));
        }
        if (!afternoonList.isEmpty()) {
            entries.add(new PieEntry(afternoonList.size(), "Ca Chiều (" + afternoonList.size() + ")"));
            colors.add(Color.parseColor("#2563EB"));
        }
        if (!eveningList.isEmpty()) {
            entries.add(new PieEntry(eveningList.size(), "Ca Tối (" + eveningList.size() + ")"));
            colors.add(Color.parseColor("#6366F1"));
        }

        if (entries.isEmpty()) {
            pieChartShift.clear();
            pieChartShift.setNoDataText("Chưa có dữ liệu ca dạy");
            pieChartShift.setNoDataTextColor(textColorSecondary);
            pieChartShift.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextSize(11.5f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value < 8f) return "";
                return Math.round(value) + "%";
            }
        });

        PieData pieData = new PieData(dataSet);
        pieChartShift.setData(pieData);
        pieChartShift.setUsePercentValues(true);
        pieChartShift.getDescription().setEnabled(false);
        pieChartShift.setDrawHoleEnabled(true);
        pieChartShift.setHoleColor(Color.TRANSPARENT);
        pieChartShift.setHoleRadius(46f);
        pieChartShift.setTransparentCircleRadius(52f);
        pieChartShift.setDrawEntryLabels(false); // Tắt nhãn bên trong lát cắt để không đè

        pieChartShift.setCenterText(events.size() + "\nBUỔI");
        pieChartShift.setCenterTextColor(textColorPrimary);
        pieChartShift.setCenterTextSize(14f);
        pieChartShift.setCenterTextTypeface(Typeface.DEFAULT_BOLD);

        Legend legend = pieChartShift.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setWordWrapEnabled(true);
        legend.setTextColor(textColorPrimary);
        legend.setTextSize(11.5f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8.5f);
        legend.setXEntrySpace(14f);
        legend.setYEntrySpace(6f);

        pieChartShift.setExtraOffsets(5, 5, 5, 12);

        pieChartShift.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;
                    String label = pe.getLabel();
                    List<EventModel> selList = morningList;
                    if (label != null && label.contains("Chiều")) selList = afternoonList;
                    else if (label != null && label.contains("Tối")) selList = eveningList;
                    showChartDrillDownDialog("DANH SÁCH " + (label != null ? label.toUpperCase(Locale.getDefault()) : ""), 
                            "Tổng cộng " + selList.size() + " buổi", selList);
                }
            }

            @Override
            public void onNothingSelected() {}
        });

        pieChartShift.animateY(700, Easing.EaseInOutQuad);
        pieChartShift.invalidate();
    }

    private long getCustomPayRate(Context context, String schoolCode, long defaultRate) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("lectuplan_income_prefs", Context.MODE_PRIVATE);
        return prefs.getLong("pay_rate_" + schoolCode, defaultRate);
    }

    private void setCustomPayRate(Context context, String schoolCode, long rate) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("lectuplan_income_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("pay_rate_" + schoolCode, rate).apply();
    }

    private void showEditIncomeRatesDialog() {
        playSuccessSound();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💰 Cài đặt Đơn giá Thù lao");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView info = new TextView(this);
        info.setText("Thêm/Xóa các trường và nhập đơn giá (VNĐ) trên 1 tiết dạy.");
        info.setTextColor(Color.parseColor("#475569"));
        info.setTextSize(13);
        info.setPadding(0, 0, 0, 40);
        layout.addView(info);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(scrollContent);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        scrollView.setLayoutParams(scrollLp);
        layout.addView(scrollView);

        android.content.SharedPreferences prefs = getSharedPreferences("lectuplan_income_prefs", Context.MODE_PRIVATE);
        String savedList = prefs.getString("dialog_school_list", null);
        List<String> schoolList = new ArrayList<>();
        if (savedList == null || savedList.isEmpty()) {
            for (SchoolProfile.SchoolType sc : SchoolProfile.SchoolType.values()) {
                if (sc != SchoolProfile.SchoolType.DEFAULT) schoolList.add(sc.getCode());
            }
        } else {
            schoolList.addAll(java.util.Arrays.asList(savedList.split(",")));
        }

        final Map<String, EditText> inputMap = new HashMap<>();

        Runnable renderRows = new Runnable() {
            @Override
            public void run() {
                scrollContent.removeAllViews();
                inputMap.clear();
                for (int i = 0; i < schoolList.size(); i++) {
                    String code = schoolList.get(i);
                    long currentRate = getCustomPayRate(MainActivity.this, code, 100000);
                    SchoolProfile.SchoolType sc = SchoolProfile.detectSchool(code);
                    if (sc != SchoolProfile.SchoolType.DEFAULT && currentRate == 100000) {
                        currentRate = getCustomPayRate(MainActivity.this, code, sc.getDefaultPayRate());
                    }

                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 0, 0, 30);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    TextView tvSchool = new TextView(MainActivity.this);
                    tvSchool.setText(code);
                    tvSchool.setTextSize(15);
                    tvSchool.setTypeface(null, Typeface.BOLD);
                    tvSchool.setTextColor(Color.parseColor(sc != SchoolProfile.SchoolType.DEFAULT ? sc.getPrimaryColor() : "#1D4ED8"));
                    LinearLayout.LayoutParams lpTv = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    tvSchool.setLayoutParams(lpTv);
                    row.addView(tvSchool);

                    EditText etRate = new EditText(MainActivity.this);
                    etRate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    etRate.setTextSize(15);
                    etRate.setPadding(20, 20, 20, 20);
                    LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(300, ViewGroup.LayoutParams.WRAP_CONTENT);
                    etRate.setLayoutParams(lpEt);
                    
                    etRate.addTextChangedListener(new TextWatcher() {
                        private String current = "";
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override public void afterTextChanged(Editable s) {
                            if (!s.toString().equals(current)) {
                                etRate.removeTextChangedListener(this);
                                String cleanString = s.toString().replaceAll("[^\\d]", "");
                                if (!cleanString.isEmpty()) {
                                    try {
                                        long parsed = Long.parseLong(cleanString);
                                        String formatted = java.text.NumberFormat.getInstance(new Locale("vi", "VN")).format(parsed);
                                        current = formatted;
                                        etRate.setText(formatted);
                                        etRate.setSelection(formatted.length());
                                    } catch (NumberFormatException e) {}
                                } else {
                                    current = "";
                                    etRate.setText("");
                                }
                                etRate.addTextChangedListener(this);
                            }
                        }
                    });
                    etRate.setText(String.valueOf(currentRate));
                    row.addView(etRate);
                    inputMap.put(code, etRate);

                    ImageButton btnDelete = new ImageButton(MainActivity.this);
                    btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
                    btnDelete.setBackgroundColor(Color.TRANSPARENT);
                    btnDelete.setColorFilter(Color.parseColor("#EF4444"));
                    btnDelete.setPadding(20, 10, 10, 10);
                    final String codeToRemove = code;
                    btnDelete.setOnClickListener(v -> {
                        schoolList.remove(codeToRemove);
                        this.run(); 
                    });
                    row.addView(btnDelete);
                    
                    scrollContent.addView(row);
                }
            }
        };
        renderRows.run();

        Button btnAdd = new Button(this);
        btnAdd.setText("+ Thêm trường khác");
        btnAdd.setBackgroundColor(Color.TRANSPARENT);
        btnAdd.setTextColor(Color.parseColor("#2563EB"));
        btnAdd.setOnClickListener(v -> {
            AlertDialog.Builder inputBuilder = new AlertDialog.Builder(this);
            inputBuilder.setTitle("Thêm Trường mới");
            final EditText input = new EditText(this);
            input.setHint("Nhập tên viết tắt (VD: VĂN HIẾN)");
            input.setPadding(50, 40, 50, 40);
            inputBuilder.setView(input);
            inputBuilder.setPositiveButton("Thêm", (d, w) -> {
                String newCode = input.getText().toString().trim().toUpperCase();
                if (!newCode.isEmpty() && !schoolList.contains(newCode)) {
                    for (Map.Entry<String, EditText> entry : inputMap.entrySet()) {
                        long val = 0;
                        try { val = Long.parseLong(entry.getValue().getText().toString().replaceAll("[^\\d]", "")); } catch (Exception e) {}
                        setCustomPayRate(this, entry.getKey(), val);
                    }
                    schoolList.add(newCode);
                    renderRows.run();
                }
            });
            inputBuilder.setNegativeButton("Hủy", null);
            inputBuilder.show();
        });
        layout.addView(btnAdd);

        builder.setView(layout);

        builder.setPositiveButton("Lưu Lại", (dialog, which) -> {
            prefs.edit().putString("dialog_school_list", TextUtils.join(",", schoolList)).apply();
            boolean changed = false;
            for (Map.Entry<String, EditText> entry : inputMap.entrySet()) {
                String code = entry.getKey();
                String valStr = entry.getValue().getText().toString();
                String cleanVal = valStr.replaceAll("[^\\d]", "");
                if (!cleanVal.isEmpty()) {
                    try {
                        long rate = Long.parseLong(cleanVal);
                        setCustomPayRate(this, code, rate);
                        changed = true;
                    } catch (Exception ignored) {}
                }
            }
            if (changed) {
                Toast.makeText(this, "Đã lưu cài đặt đơn giá thành công!", Toast.LENGTH_SHORT).show();
                updateChartTab();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);
        dialog.show();
    }

    private void showChartDrillDownDialog(String title, String subtitle, List<EventModel> events) {
        if (isFinishing() || isDestroyed()) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_chart_drilldown, null);
        TextView tvTitle = view.findViewById(R.id.tvDrillDownTitle);
        TextView tvSub = view.findViewById(R.id.tvDrillDownSub);
        TextView tvEmpty = view.findViewById(R.id.tvDrillDownEmpty);
        RecyclerView rv = view.findViewById(R.id.rvDrillDownEvents);
        ImageButton btnClose = view.findViewById(R.id.btnDrillDownClose);

        tvTitle.setText(title);
        int count = events != null ? events.size() : 0;
        tvSub.setText(subtitle != null ? subtitle : ("Tổng cộng " + count + " buổi"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        KeyboardUtils.applyAutoHideKeyboard(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (events == null || events.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new EventAdapter(new ArrayList<>(events), new EventAdapter.OnEventClickListener() {
                @Override
                public void onDeleteClick(EventModel event) {
                    dialog.dismiss();
                    showCustomConfirmDialog("🗑️", "XÓA LỊCH CÔNG VIỆC",
                            "Bạn có chắc chắn muốn xóa lịch '" + event.getTitle() + "'?",
                            "XÓA NGAY",
                            Color.parseColor("#DC2626"),
                            () -> {
                                dbHelper.deleteEvent(event.getId());
                                refreshData();
                                updateChartTab();
                            });
                }

                @Override
                public void onEditClick(EventModel event) {
                    dialog.dismiss();
                    showEditEventDialog(event);
                }

                @Override
                public void onSelectionChanged(int selectedCount) {}
            }));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


}
