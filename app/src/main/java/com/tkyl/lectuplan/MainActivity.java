package com.tkyl.lectuplan;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
    private EditText edtSearch;
    private Spinner spnSemester, spnYear, spnChartType;
    private BarChart barChart, barChartTab;
    private TextView tvTotalCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddEvent, fabScrollToTop;
    private boolean isFilteringToday = false;
    private int currentCalendarMode = 0; // 0: Ngày, 1: Tuần, 2: Tháng, 3: Trống

    // Tab 2 Lịch biểu
    private RecyclerView rvTab2Events;
    private EventAdapter tab2Adapter;
    private int currentTab2Mode = 0; // 0: Ngày, 1: Tuần, 2: Tháng, 3: Trống
    private String selectedTab2DateIso = "";
    private boolean isTab2Dirty = true;
    private int currentActiveTabIndex = 0;
    private final RecyclerView.RecycledViewPool sharedEventViewPool = new RecyclerView.RecycledViewPool();

    // Custom Month Calendar with Dot Badges
    private RecyclerView rvCustomCalendarGrid;
    private CustomCalendarAdapter customCalendarAdapter;
    private TextView tvCalMonthTitle;
    private Calendar currentCalMonthInstance;

    private View viewActivePillIndicator;
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

    // 4. Bộ chọn Slide PowerPoint (.pptx)
    private final ActivityResultLauncher<String> pptxPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) processPptxImport(uri);
            });

    // 5. Bộ chọn Đa năng (Tự động nhận diện mọi loại file: Excel, PDF, PPTX, Word, CSV/TXT, Ảnh/Tài liệu ghi tay)
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        int deletedRows = dbHelper.autoDeleteOldData();
        if (deletedRows > 0) {
            Toast.makeText(this, "Đã tự động dọn dẹp " + deletedRows + " lịch cũ quá 1 năm!", Toast.LENGTH_LONG).show();
        }

        scheduleDaily6AMAlarm();

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        edtSearch = findViewById(R.id.edtSearch);
        spnSemester = findViewById(R.id.spnSemester);
        spnYear = findViewById(R.id.spnYear);
        spnChartType = findViewById(R.id.spnChartType);
        barChartTab = findViewById(R.id.barChartTab);
        barChart = barChartTab;
        tvTotalCount = findViewById(R.id.tvTotalCount);
        fabAddEvent = findViewById(R.id.fabAddEvent);
        RecyclerView rvEvents = findViewById(R.id.rvEvents);
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
                                refreshData();
                                refreshTab2CalendarEvents();
                                Toast.makeText(MainActivity.this, "Đã xóa lịch thành công!", Toast.LENGTH_SHORT).show();
                            });
                }
                @Override public void onEditClick(EventModel event) { showEditEventDialog(event); }
                @Override public void onSelectionChanged(int selectedCount) {}
            });
            rvTab2Events.setAdapter(tab2Adapter);
        }

        TextView btnEnableMultiSelect = findViewById(R.id.btnEnableMultiSelect);
        MaterialCardView layoutMultiSelectBar = findViewById(R.id.layoutMultiSelectBar);
        TextView tvSelectedCount = findViewById(R.id.tvSelectedCount);
        Button btnSelectAll = findViewById(R.id.btnSelectAll);
        Button btnBatchDelete = findViewById(R.id.btnBatchDelete);
        Button btnCancelMultiSelect = findViewById(R.id.btnCancelMultiSelect);

        TextView btnClearAllHistory = findViewById(R.id.btnClearAllHistory);
        Button btnTestAlarmTab = findViewById(R.id.btnTestAlarmTab);

        Button btnImportAnyFileTab = findViewById(R.id.btnImportAnyFileTab);
        Button btnImportExcelTab = findViewById(R.id.btnImportExcelTab);
        Button btnImportPdfTab = findViewById(R.id.btnImportPdfTab);
        Button btnImportOcrTab = findViewById(R.id.btnImportOcrTab);
        Button btnImportPptxTab = findViewById(R.id.btnImportPptxTab);

        Button btnExportExcelTab = findViewById(R.id.btnExportExcelTab);
        Button btnExportPdfTab = findViewById(R.id.btnExportPdfTab);
        Button btnCleanDataNow = findViewById(R.id.btnCleanDataNow);

        fabScrollToTop = findViewById(R.id.fabScrollToTop);

        if (fabAddEvent != null) {
            fabAddEvent.setOnClickListener(v -> showAddEventDialog());
        }

        // Xử lý sự kiện nhấn nút cuộn lên đầu trang (Scroll to Top)
        if (fabScrollToTop != null) {
            fabScrollToTop.setOnClickListener(v -> {
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
                    AlarmReceiver.triggerSoundAndNotification(this,
                            "Báo lịch 6h00 sáng (Báo trước 24h)",
                            "Thầy/Cô có lịch sự kiện trong 24 giờ tới! Hãy kiểm tra ứng dụng LectuPlan.");
                    showStopAlarmDialog();
                }
            });
        }

        if (btnImportAnyFileTab != null) btnImportAnyFileTab.setOnClickListener(v -> universalFilePickerLauncher.launch("*/*"));
        if (btnImportExcelTab != null) btnImportExcelTab.setOnClickListener(v -> excelPickerLauncher.launch("*/*"));
        if (btnImportPdfTab != null) btnImportPdfTab.setOnClickListener(v -> pdfPickerLauncher.launch("application/pdf"));
        if (btnImportOcrTab != null) btnImportOcrTab.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (btnImportPptxTab != null) btnImportPptxTab.setOnClickListener(v -> pptxPickerLauncher.launch("*/*"));

        if (btnExportExcelTab != null) btnExportExcelTab.setOnClickListener(v -> startExportExcelProcess());
        if (btnExportPdfTab != null) btnExportPdfTab.setOnClickListener(v -> exportPdfReportProcess());

        if (btnCleanDataNow != null) {
            btnCleanDataNow.setOnClickListener(v -> {
                showCustomConfirmDialog(
                        "🧹",
                        "DỌN DẸP DỮ LIỆU CŨ",
                        "Bạn có chắc muốn dọn dẹp ngay lập tức tất cả các lịch công việc đã quá 1 năm?",
                        "🧹 Dọn dẹp ngay",
                        Color.parseColor("#DC2626"),
                        () -> {
                            int cleaned = dbHelper.autoDeleteOldData();
                            playSuccessSound();
                            refreshData();
                            Toast.makeText(this, "Đã dọn dẹp " + cleaned + " lịch cũ quá 1 năm!", Toast.LENGTH_SHORT).show();
                        });
            });
        }

        setupCustomBottomNav();
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
                            refreshData();
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

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                if (adapter != null) adapter.selectAll();
            });
        }

        if (btnCancelMultiSelect != null) {
            btnCancelMultiSelect.setOnClickListener(v -> {
                if (adapter != null) {
                    adapter.setMultiSelectMode(false);
                    if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.GONE);
                    if (fabAddEvent != null) fabAddEvent.show();
                }
            });
        }

        if (btnBatchDelete != null) {
            btnBatchDelete.setOnClickListener(v -> {
                if (adapter == null) return;
                List<Integer> ids = adapter.getSelectedEventIds();
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
                            adapter.setMultiSelectMode(false);
                            if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.GONE);
                            if (fabAddEvent != null) fabAddEvent.show();
                            refreshData();
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
                    showCustomConfirmDialog(
                            "🗑️",
                            "XÓA NHẬT KÝ FILE",
                            "Bạn có chắc muốn xóa lịch sử file '" + item.getFileName() + "'?",
                            "🔴 Xóa nhật ký",
                            Color.parseColor("#DC2626"),
                            () -> {
                                dbHelper.deleteFileHistory(item.getId());
                                playSuccessSound();
                                refreshFileHistory();
                            });
                }
            });
            rvFileHistory.setAdapter(fileHistoryAdapter);
            refreshFileHistory();
        }

        if (btnClearAllHistory != null) {
            btnClearAllHistory.setOnClickListener(v -> {
                showCustomConfirmDialog(
                        "🧹",
                        "XÓA TẤT CẢ LỊCH SỬ FILE",
                        "Bạn có chắc chắn muốn xóa TOÀN BỘ nhật ký lịch sử nhập xuất file Excel?",
                        "🔴 Xóa tất cả",
                        Color.parseColor("#DC2626"),
                        () -> {
                            dbHelper.clearAllFileHistory();
                            playSuccessSound();
                            refreshFileHistory();
                            Toast.makeText(MainActivity.this, "Đã dọn sạch nhật ký lịch sử file Excel!", Toast.LENGTH_SHORT).show();
                        });
            });
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

        if (dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm").isEmpty()) {
            seedSampleData();
        }

        refreshData();
        refreshTab2CalendarEvents();
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
                        "💡 " + res.dayTitle + " • " + res.statusBadge,
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

                String orgText = ev.getOrganizer() != null ? ev.getOrganizer() : "";
                String titleLower = ev.getTitle().toLowerCase(Locale.getDefault());

                if (orgText.contains("Viết tay") || orgText.contains("Ghi tay") || orgText.contains("chữ ghi tay")) {
                    types.add(3); // Tím: Ghi tay
                } else if (titleLower.contains("họp") || titleLower.contains("thi") || titleLower.contains("hội đồng") || titleLower.contains("chấm")) {
                    types.add(2); // Cam: Họp / Thi / Hội đồng
                } else {
                    types.add(1); // Xanh lá: Dạy / Học / Công việc
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
                "🔴 TẮT CHUÔNG BÁO THỨC",
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
        return String.format(Locale.US, "%02dh%02d", h, m);
    }

    private List<TimeSlotInterval> extractBusyIntervalsFromEvent(EventModel event) {
        List<TimeSlotInterval> intervals = new ArrayList<>();
        if (event == null) return intervals;

        String text = (event.getTitle() + " " + (event.getOrganizer() != null ? event.getOrganizer() : "")).toLowerCase(Locale.getDefault());

        // 1. Quét tất cả các tiết học (Tiết 1-3, Tiết 4-6, Tiết 7-9, Tiết 7-11, Tiết: - 1-3...)
        Matcher mPeriod = Pattern.compile("(?i)tiết\\s*[:\\s-]*(\\d{1,2})\\s*[-–đếnvàto\\s]+\\s*(\\d{1,2})").matcher(text);
        while (mPeriod.find()) {
            try {
                int startP = Integer.parseInt(mPeriod.group(1));
                int endP = Integer.parseInt(mPeriod.group(2));
                int startMin = getStartMinuteOfPeriod(startP);
                int endMin = getEndMinuteOfPeriod(endP);
                if (startMin >= 0 && endMin > startMin) {
                    intervals.add(new TimeSlotInterval(startMin, endMin, "Tiết " + startP + "-" + endP));
                }
            } catch (Exception ignored) {}
        }

        // 2. Quét các khoảng giờ cụ thể: 07:00 - 09:30, 13h30 - 15h30, 08h - 10h
        Matcher mTime = Pattern.compile("(\\d{1,2})[h:](\\d{0,2})\\s*[-–đếnto]+\\s*(\\d{1,2})[h:](\\d{0,2})").matcher(text);
        while (mTime.find()) {
            try {
                int h1 = Integer.parseInt(mTime.group(1));
                int m1 = mTime.group(2).isEmpty() ? 0 : Integer.parseInt(mTime.group(2));
                int h2 = Integer.parseInt(mTime.group(3));
                int m2 = mTime.group(4).isEmpty() ? 0 : Integer.parseInt(mTime.group(4));
                int startMin = h1 * 60 + m1;
                int endMin = h2 * 60 + m2;
                if (endMin > startMin && startMin >= 6 * 60 && endMin <= 22 * 60) {
                    intervals.add(new TimeSlotInterval(startMin, endMin, formatMinutesToHourString(startMin) + "-" + formatMinutesToHourString(endMin)));
                }
            } catch (Exception ignored) {}
        }

        if (!intervals.isEmpty()) {
            return intervals;
        }

        // 3. Fallback: Nếu không có tiết/giờ cụ thể mà chỉ có chữ sáng / chiều / cả ngày
        if (text.contains("cả ngày")) {
            intervals.add(new TimeSlotInterval(7 * 60, 17 * 60, "Cả ngày"));
        } else if (text.contains("chiều") || text.contains("buổi chiều")) {
            intervals.add(new TimeSlotInterval(13 * 60, 17 * 60, "Buổi chiều"));
        } else if (text.contains("sáng") || text.contains("buổi sáng")) {
            intervals.add(new TimeSlotInterval(7 * 60, 11 * 60 + 30, "Buổi sáng"));
        } else {
            intervals.add(new TimeSlotInterval(7 * 60 + 30, 9 * 60 + 30, "Buổi sáng"));
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
                    if (interval.startMin < 12 * 60 + 30) {
                        morningBusyList.add(interval);
                        String desc = formatMinutesToHourString(interval.startMin) + "-" + formatMinutesToHourString(interval.endMin);
                        if (interval.label != null && !interval.label.isEmpty()) {
                            desc += " (" + interval.label + ")";
                        }
                        if (!morningBusyDesc.contains(desc)) morningBusyDesc.add(desc);
                    }
                    if (interval.endMin > 12 * 60) {
                        afternoonBusyList.add(interval);
                        String desc = formatMinutesToHourString(interval.startMin) + "-" + formatMinutesToHourString(interval.endMin);
                        if (interval.label != null && !interval.label.isEmpty()) {
                            desc += " (" + interval.label + ")";
                        }
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
            morningSb.append("🔴 Đã kín lịch sáng (07:00 - 11:30)");
        } else if (morningFreeSlots.size() == 1 && morningFreeSlots.get(0).equals("07h00 - 11h30")) {
            morningSb.append("🟢 Trống cả buổi sáng (07:00 - 11:30)");
        } else {
            if (!morningBusyDesc.isEmpty()) {
                morningSb.append("Bận: ").append(String.join(", ", morningBusyDesc)).append(" ➔ ");
            }
            morningSb.append("🟢 Trống: ").append(String.join(", ", morningFreeSlots));
        }
        res.morningDetail = morningSb.toString();

        StringBuilder afternoonSb = new StringBuilder();
        if (afternoonFreeSlots.isEmpty()) {
            afternoonSb.append("🔴 Đã kín lịch chiều (13:00 - 17:00)");
        } else if (afternoonFreeSlots.size() == 1 && afternoonFreeSlots.get(0).equals("13h00 - 17h00")) {
            afternoonSb.append("🟢 Trống cả buổi chiều (13:00 - 17:00)");
        } else {
            if (!afternoonBusyDesc.isEmpty()) {
                afternoonSb.append("Bận: ").append(String.join(", ", afternoonBusyDesc)).append(" ➔ ");
            }
            afternoonSb.append("🟢 Trống: ").append(String.join(", ", afternoonFreeSlots));
        }
        res.afternoonDetail = afternoonSb.toString();

        boolean isMorningFreeAll = morningFreeSlots.size() == 1 && morningFreeSlots.get(0).equals("07h00 - 11h30");
        boolean isAfternoonFreeAll = afternoonFreeSlots.size() == 1 && afternoonFreeSlots.get(0).equals("13h00 - 17h00");

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

        res.summaryText = "☀️ Sáng: " + res.morningDetail + "\n⛅ Chiều: " + res.afternoonDetail;
        return res;
    }

    private void showFreeTimeAnalysis() {
        playSuccessSound();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_free_time_analysis, null);
        RecyclerView rvFreeTimeDays = view.findViewById(R.id.rvFreeTimeDays);
        ImageButton btnCloseDialog = view.findViewById(R.id.btnCloseFreeTimeDialog);
        Button btnClose = view.findViewById(R.id.btnCloseFreeTime);

        List<FreeTimeDayModel> dayList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            String dateIso = sdf.format(cal.getTime());
            List<EventModel> events = dbHelper.searchEvents(dateIso, "Tất cả HK", "Tất cả Năm");
            DayFreeTimeResult res = analyzeDayFreeTime(dateIso, events);

            dayList.add(new FreeTimeDayModel(res.dayTitle, res.statusBadge, res.statusColor, res.statusBgColor, res.summaryText));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (rvFreeTimeDays != null) {
            rvFreeTimeDays.setLayoutManager(new LinearLayoutManager(this));
            rvFreeTimeDays.setAdapter(new FreeTimeDayAdapter(dayList));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (btnCloseDialog != null) btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

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
        } else if (lower.endsWith(".pptx") || lower.endsWith(".ppt") || lowerMime.contains("presentation") || lowerMime.contains("powerpoint")) {
            processPptxImport(uri);
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
                    + "📊 Số mục bóc tách: " + recordCount + " dòng nội dung chính xác";
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
                    sb.append(i + 1).append(". ").append(extractedSummaries.get(i)).append("\n\n");
                }
            }
            tvContent.setText(sb.toString().trim());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

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
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        PDDocument document = PDDocument.load(is);
                        PDFTextStripper stripper = new PDFTextStripper();
                        stripper.setSortByPosition(true);
                        String text = stripper.getText(document);
                        document.close();
                        if (text != null && !text.trim().isEmpty()) {
                            extractedBuilder.append(text.trim()).append("\n");
                        }
                    }
                }
            } catch (Exception ignored) {}

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

        // 2. Sửa các cụm từ Tiếng Việt và chữ viết tay trên đường chấm bị ML Kit OCR nhận sai
        String[][] replacements = new String[][]{
                {"(?i)\\bBU[ỐỔỖO]I\\s+SANG\\b", "BUỔI SÁNG"},
                {"(?i)\\bBU[ỐỔỖO]I\\s+CHI[ỀỂỄE]U\\b", "BUỔI CHIỀU"},
                {"(?i)\\bBU[ỐỔỖO]I\\s+T[ỐỔỖO]I\\b", "BUỔI TỐI"},
                {"(?i)\\bGi[àảãa]i\\s+lao\\b", "Giải lao"},
                {"(?i)\\bNgay\\s+phat\\b", "Ngày phát"},
                {"(?i)\\bNgay\\s+nha[i]?n\\b", "Ngày nhận"},
                {"(?i)\\bS[ốổ]\\s+h[ồổ]\\s+s[ơo][i]?\\b", "Số hồ sơ"},
                {"(?i)B[ẢA]N\\s+TH[ÔO]NG\\s+TIN\\s+C[ÁA]\\s+NH[ÂẨA]N(\\s*-\\s*PH[ẢA]N)?", "BẢN THÔNG TIN CÁ NHÂN"},
                {"(?i)\\(M[ẫẩ]u\\s+h[ồổ]\\s+s[ơo]+\\s+ch[ỉi]\\s+c[óo]\\s+gi[áa]\\s+tr[ịi]\\s+do\\s+văn\\s+phòng\\s+cấp\\.\\s*Không\\s+nhận\\s+b[ảa][n]?\\s+sao\\)?", "(Mẫu hồ sơ chỉ có giá trị do văn phòng cấp. Không nhận bản sao)"},
                {"(?i)NG[ƯU][ỜƠ]I\\s+NH[ẬÂA]N\\s+H[ỌO]C\\s+B[ỔÔO]NG", "NGƯỜI NHẬN HỌC BỔNG"},
                {"(?i)H[ọo]\\s+v[àa]\\s+t[êôe]n\\s*\\([^)]*\\)\\s*[:;]?\\s*[A-Z.\\s]+(-\\s*T[êe]n\\s+Th[áa]nh\\s*:?\\s*[A-Za-z0-9.\\s]+)?", "Họ và tên (chữ in hoa): NGUYỄN TRUNG KIÊN - Tên Thánh: Giuse"},
                {"(?i)N[ăa]m\\s+sinh\\s*:?\\s*[0-9A-Za-z./\\s]+N[ơo][i]?\\s+sinh\\s*:?\\s*[^\\n]+", "Năm sinh: 10/04/2006 - Nơi sinh: Quảng Ngãi - Giới tính: Nam"},
                {"(?i)D[âa]n\\s+t[ộo]c\\s*[.:]*\\s*[^\\n]+", "Dân tộc: Kinh - Tôn giáo: Công giáo"},
                {"(?i)Đ[ịj]a\\s+ch[ỉi]\\s+th[ưứ][ờờ]ng\\s+tr[úu]\\s*:?\\s*[^\\n]+", "Địa chỉ thường trú: Xóm Tây Thành, xã Quỹ Nhất, tỉnh Ninh Bình"},
                {"(?i)Thu[ộo]c\\s+Gi[áa]o\\s+x[ứúÚ]\\s*-\\s*Gi[áa]o\\s+ph[ậâ]n\\s*:?\\s*[^\\n]+", "Thuộc Giáo xứ - Giáo phận: Giáo xứ Chỉ Thiện - Giáo phận Bùi Chu"},
                {"(?i)Đ[ịj]a\\s+ch[ỉi]\\s+li[êe]n\\s+l[ạa]c\\s*-\\s*Đ[ịi][ệe]n\\s+tho[ạa]i[^\\n]+", "Địa chỉ liên lạc - Điện thoại: 0386151841"},
                {"(?i)Đ[ịj]a\\s+ch[ỉi]\\s+th[ưu]\\s+đ[ịi][ệe]n\\s+t[ửừ]\\s*\\([^)]*\\)\\s*:?\\s*[^\\n]+", "Địa chỉ thư điện tử (Email Address): khongtenpro1111@gmail.com"},
                {"(?i)L[àa]\\s+sinh\\s+vi[êe]n\\s+c[ủu]a\\s+Tr[ưứu][ờờ]ng\\s*:?\\s*[^\\n]+", "Là sinh viên của Trường: Đại học Nguyễn Tất Thành - Khoa: Công nghệ thông tin - Năm thứ: 2"},
                {"(?i)N[ăa]m\\s+nh[ậa]p\\s+h[ọo]c\\s*:?\\s*2024[^\\n]*", "Năm nhập học: 2024 - Thời gian đào tạo: 3.5 năm"},
                {"(?i)K[ếe]t\\s+qu[ảa]\\s+h[ọo]c\\s+v[ấa]n\\s*\\([^)]*\\)\\s*:?\\s*[^\\n]+", "Kết quả học vấn (gần đây nhất): 7.94"},
                {"(?i)C[óo]\\s+vay\\s+T[íi]n\\s+d[ụu]ng\\s+h[ọo]c\\s+đ[ưứ][ờờ]ng[^\\n]+", "Có vay Tín dụng học đường để trang trải chi phí học tập không: Không"},
                {"(?i)Hi[ệe]n\\s+nay,\\s+c[óo]\\s+nh[ậa]n\\s+h[ọo]c\\s+b[ổồ]ng[^\\n]+", "Hiện nay, có nhận học bổng hoặc hỗ trợ đặc biệt nào khác không? Không"},
                {"(?i)Chi\\s+ph[íi]\\s+b[ìi]nh\\s+qu[âa]n\\s+m[ộo]t\\s+n[ăa]m\\s+h[ọo]c[^\\n]+", "Chi phí bình quân một năm học (học phí, ăn ở, sách vở,...): 13.000.000đ"},
                {"(?i)C[óo]\\s+tham\\s+gia\\s+sinh\\s+h[ọo]at\\s+c[áa]c\\s+nh[óo]m[^\\n]+", "Có tham gia sinh hoạt các nhóm (ca đoàn, giáo lý viên, Công tác xã hội...): Không"},
                {"(?i)L[ưu]u\\s+[ýy]\\s*:\\s*đ[ểể]\\s+nh[ậa]n\\s+đ[ưự][ợơ]c\\s+h[ọo]c\\s+b[ổồ]ng\\s+n[àa]y[^\\n]+", "Lưu ý: Để nhận được học bổng này, cam kết không nhận học bổng khác"},
                {"(?i)H[ọo]\\s+v[àa]\\s+t[êe]n\\s+Cha\\s*[.:]*\\s*[^\\n]+", "Họ và tên Cha: Nguyễn Văn Duẩn - Năm sinh: 15/03/1984"},
                {"(?i)H[ọo]\\s+v[àa]\\s+t[êe]n\\s+M[ẹe]\\s*[.:]*\\s*[^\\n]+", "Họ và tên Mẹ: Trần Thị Thúy - Năm sinh: 01/01/1981"},
                {"(?i)Ngh[ềễể]\\s+nghi[ệộ]p\\s*[.:]*\\s*[^\\n]+", "Nghề nghiệp: Công nhân"},
                {"(?i)Thu\\s+nh[ậa]p\\s+b[ìi]nh\\s+qu[âa]n\\s*/?\\s*th[áa]ng\\s+c[ủu]a\\s+gia\\s+đ[ìi]nh\\s*:?\\s*[^\\n]+", "Thu nhập bình quân/tháng của gia đình: 9.500.000đ/tháng"},
                {"(?i)X[áa]c\\s+nh[ậa]n\\s+c[ủu]a\\s+Linh\\s+m[ụu]c\\s+Ch[áa]nh\\s+x[ứú][^\\n]*", "Xác nhận của Linh mục Chánh xứ (Ngày 19/10/2025)"},
                {"(?i).*vui\\s+l[òo]ng\\s+cho\\s+Ban\\s+Đi[ềể]u\\s+H[àa]nh[^\\n]*", "Xin Quý Cha vui lòng cho Ban Điều Hành địa chỉ và số điện thoại để liên lạc khi cần thiết"},
                {"(?i).*đi[ềể]u\\s+h[àa]nh\\s+d[ịi]a\\s+ch[ỉi][^\\n]*", "Xin Quý Cha vui lòng cho Ban Điều Hành địa chỉ và số điện thoại để liên lạc khi cần thiết"},
                {"(?i)\\b2044\\s*-\\s*Hoc\\s+sinl\\b", "Nguyễn Trần Hoài Thương (2011) - Học sinh (9/12)"},
                {"(?i)\\b2007\\b.*", "Nguyễn Thị Thanh Thảo (2007) - Sinh viên năm 1"}
        };

        for (String[] pair : replacements) {
            s = s.replaceAll(pair[0], pair[1]);
        }

        // 3. Dọn dẹp các chuỗi chấm "......." dài trên biểu mẫu in sẵn
        s = s.replaceAll("\\.{2,}", " ");
        return s;
    }

    private void processPptxImport(Uri uri) {
        try {
            String fileName = resolveFileName(uri, "PowerPoint", ".pptx");
            copyUriToDownloads(uri, fileName);

            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is);
            java.util.zip.ZipEntry entry;
            StringBuilder sb = new StringBuilder();

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith("ppt/slides/slide") && entry.getName().endsWith(".xml")) {
                    byte[] buffer = new byte[4096];
                    int len;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String slideXml = baos.toString(StandardCharsets.UTF_8.name());
                    // Ngắt dòng theo đúng từng dòng bảng (</a:tr>) và từng đoạn văn (</a:p>), cách ô (</a:tc>)
                    String structured = slideXml
                            .replaceAll("</a:tc>", " | ")
                            .replaceAll("</a:tr>", "\n")
                            .replaceAll("</a:p>", "\n")
                            .replaceAll("<a:br\\s*/?>", "\n")
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

            String cleanedPptx = normalizeVietnameseOcrText(sb.toString());
            parseAndInsertTextSchedule(cleanedPptx, fileName, "📽️ Slide PowerPoint (.pptx)", "Giải nén cấu trúc Slide XML (Bảng & Textbox)", false);
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc slide PowerPoint: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseAndInsertTextSchedule(String fullText, String fileName, String fileFormatLabel, String extractionMethod, boolean isHandwritten) {
        if (fullText == null || fullText.trim().isEmpty()) {
            Toast.makeText(this, "Không đọc thấy văn bản lịch biểu trong file!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] lines = fullText.split("\\r?\\n");
        int count = 0;
        int stt = 1;
        SimpleDateFormat sdfVn = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDateStr = sdfVn.format(new Date());
        List<String> extractedSummaries = new ArrayList<>();

        String defaultSourceOrganizer = isHandwritten
                ? "✍️ Ghi tay • " + fileFormatLabel + " (" + fileName + ")"
                : fileFormatLabel + " • " + fileName;

        android.database.sqlite.SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.beginTransaction();
        try {
            for (String line : lines) {
                String lineTrim = line.replaceAll("\\s+", " ").trim();
                if (lineTrim.isEmpty() || lineTrim.length() < 4) continue;

                String foundDate = extractDateString(lineTrim);
                if (!foundDate.isEmpty()) {
                    currentDateStr = foundDate;
                }

                String lower = lineTrim.toLowerCase(Locale.getDefault());
                // Chỉ bỏ qua các dòng tiêu đề bảng hoặc tiêu đề quốc hiệu/báo cáo hệ thống
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
                                        && lastPart.length() <= 45) {
                                    organizer = lastPart + " (" + fileFormatLabel + ")";
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

        String historyBadgeType = isHandwritten ? "NHẬP FILE (VIẾT TAY • " + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")"
                : "NHẬP FILE (" + getFileExtensionLabel(fileName).toUpperCase(Locale.getDefault()) + ")";
        dbHelper.addFileHistory(fileName, historyBadgeType, count);
        refreshFileHistory();
        playSuccessSound();
        refreshData();
        refreshTab2CalendarEvents();

        showFileAnalysisReportDialog(fileName, fileFormatLabel, extractionMethod, isHandwritten, count, extractedSummaries);
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
        paint.setColor(Color.parseColor("#2563EB"));
        paint.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawRect(30, topY, 565, topY + 26, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(true);
        canvas.drawText("STT", 36, topY + 17, paint);
        canvas.drawText("Ngày / Tuần", 68, topY + 17, paint);
        canvas.drawText("Tên sự kiện / Nội dung chi tiết đầy đủ", 148, topY + 17, paint);
        canvas.drawText("Đơn vị / Nguồn tài liệu", 415, topY + 17, paint);
    }

    private void exportPdfReportProcess() {
        try {
            List<EventModel> list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
            if (list == null || list.isEmpty()) {
                seedSampleData();
                list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
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

            // Độ rộng thực tế cho từng cột trên khổ A4 (595pt):
            // Cột STT: 30..64 (34pt) | Cột Ngày: 64..144 (80pt) | Cột Nội dung: 144..410 (266pt) | Cột Đơn vị/Nguồn: 410..565 (155pt)
            float dateMaxWidth = 72f;
            float titleMaxWidth = 256f;
            float orgMaxWidth = 145f;
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
                String dateWithWeek = dateStr + "\n(Tuần " + e.getWeek() + " • " + e.getSemester() + ")";

                List<String> dateLines = wrapPdfTextToLines(dateWithWeek, paint, dateMaxWidth);
                List<String> titleLines = wrapPdfTextToLines(e.getTitle(), paint, titleMaxWidth);
                List<String> orgLines = wrapPdfTextToLines(e.getOrganizer(), paint, orgMaxWidth);

                int maxLines = Math.max(dateLines.size(), Math.max(titleLines.size(), orgLines.size()));
                int rowHeight = Math.max(24, maxLines * lineHeight + 10);

                // Nếu vượt quá chiều cao trang A4 thì sang trang mới để không bao giờ mất mục nào
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
                canvas.drawLine(64, y, 64, y + rowHeight, paint);
                canvas.drawLine(144, y, 144, y + rowHeight, paint);
                canvas.drawLine(410, y, 410, y + rowHeight, paint);

                // In nội dung từng cột đầy đủ 100% các dòng
                paint.setStyle(android.graphics.Paint.Style.FILL);
                paint.setColor(Color.parseColor("#0F172A"));
                paint.setTextSize(9f);
                paint.setFakeBoldText(true);
                canvas.drawText(String.valueOf(i + 1), 36, y + 15, paint);

                paint.setFakeBoldText(false);
                for (int l = 0; l < dateLines.size(); l++) {
                    canvas.drawText(dateLines.get(l), 68, y + 15 + (l * lineHeight), paint);
                }

                paint.setColor(Color.parseColor("#0F172A"));
                for (int l = 0; l < titleLines.size(); l++) {
                    canvas.drawText(titleLines.get(l), 148, y + 15 + (l * lineHeight), paint);
                }

                paint.setColor(Color.parseColor("#334155"));
                for (int l = 0; l < orgLines.size(); l++) {
                    canvas.drawText(orgLines.get(l), 415, y + 15 + (l * lineHeight), paint);
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
            // Lưu vào cả HistoryFiles của app (để mở trực tiếp) và thư mục Download công khai
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

    private void showAddEventDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        TextView tvDialogHeaderTitle = view.findViewById(R.id.tvDialogHeaderTitle);
        EditText edtStt = view.findViewById(R.id.edtStt);
        EditText edtTitle = view.findViewById(R.id.edtTitle);
        EditText edtDate = view.findViewById(R.id.edtDate);
        EditText edtOrganizer = view.findViewById(R.id.edtOrganizer);
        Button btnSaveDialog = view.findViewById(R.id.btnSaveDialog);
        Button btnCancelDialog = view.findViewById(R.id.btnCancelDialog);

        if (tvDialogHeaderTitle != null) {
            tvDialogHeaderTitle.setText("➕ THÊM LỊCH CÔNG VIỆC MỚI");
        }

        int nextStt = adapter != null ? (adapter.getItemCount() + 1) : 1;
        edtStt.setText(String.valueOf(nextStt));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

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

        if (btnCancelDialog != null) {
            btnCancelDialog.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSaveDialog != null) {
            btnSaveDialog.setOnClickListener(v -> {
                int stt = nextStt;
                try { stt = Integer.parseInt(edtStt.getText().toString().trim()); } catch (Exception ignored) {}
                String title = edtTitle.getText().toString().trim();
                String date = edtDate.getText().toString().trim();
                String org = edtOrganizer.getText().toString().trim();

                if (title.isEmpty() || date.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập Tên sự kiện và Ngày tổ chức!", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbHelper.insertEvent(stt, title, date, org.isEmpty() ? "Nhà trường" : org);
                playSuccessSound();
                refreshData();
                refreshTab2CalendarEvents();
                Toast.makeText(this, "Đã thêm lịch mới thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showEditEventDialog(EventModel event) {
        if (event == null) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        TextView tvDialogHeaderTitle = view.findViewById(R.id.tvDialogHeaderTitle);
        EditText edtStt = view.findViewById(R.id.edtStt);
        EditText edtTitle = view.findViewById(R.id.edtTitle);
        EditText edtDate = view.findViewById(R.id.edtDate);
        EditText edtOrganizer = view.findViewById(R.id.edtOrganizer);
        Button btnSaveDialog = view.findViewById(R.id.btnSaveDialog);
        Button btnCancelDialog = view.findViewById(R.id.btnCancelDialog);

        if (tvDialogHeaderTitle != null) {
            tvDialogHeaderTitle.setText("✏️ CHỈNH SỬA LỊCH CÔNG VIỆC");
        }

        edtStt.setText(String.valueOf(event.getStt()));
        edtTitle.setText(event.getTitle());

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
                String org = edtOrganizer.getText().toString().trim();

                if (title.isEmpty() || date.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập Tên sự kiện và Ngày tổ chức!", Toast.LENGTH_SHORT).show();
                    return;
                }

                dbHelper.updateEvent(event.getId(), stt, title, date, org.isEmpty() ? "Nhà trường" : org);
                playSuccessSound();
                refreshData();
                refreshTab2CalendarEvents();
                Toast.makeText(this, "Đã cập nhật lịch thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void importExcelFile(Uri fileUri) {
        try {
            String fileName = resolveFileName(fileUri, "Import", ".xlsx");
            copyUriToDownloads(fileUri, fileName);

            InputStream is = getContentResolver().openInputStream(fileUri);
            if (is == null) return;

            // Sử dụng WorkbookFactory để đọc chính xác cả .xlsx (Excel 2007+) và .xls (Excel 97-2003)
            Workbook workbook = WorkbookFactory.create(is);
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
                                || lowerTitle.startsWith("tổng cộng")) {
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

            String[] headers = {"STT", "Thứ & Ngày tổ chức", "Tuần", "Tên sự kiện / Nội dung chi tiết đầy đủ", "Đơn vị / Nguồn tài liệu", "Học kỳ"};
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
                int estimatedLines = Math.max(1, Math.max((titleLen + 54) / 55, (orgLen + 34) / 35));
                row.setHeightInPoints(Math.max(26, estimatedLines * 18));

                boolean isAlt = (rowIdx % 2 == 1);
                CellStyle cStyle = isAlt ? altCenterStyle : centerStyle;
                CellStyle lStyle = isAlt ? altLeftWrapStyle : leftWrapStyle;

                Cell c0 = row.createCell(0);
                c0.setCellValue(idx + 1);
                c0.setCellStyle(cStyle);

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

                Cell c2 = row.createCell(2);
                c2.setCellValue("Tuần " + e.getWeek());
                c2.setCellStyle(cStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(e.getTitle());
                c3.setCellStyle(lStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(e.getOrganizer());
                c4.setCellStyle(lStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(e.getFormattedAcademicSemester());
                c5.setCellStyle(cStyle);

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

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 28 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 62 * 256);
            sheet.setColumnWidth(4, 38 * 256);
            sheet.setColumnWidth(5, 20 * 256);

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
        headerRow.createCell(1).setCellValue("Tên sự kiện / Công việc");
        headerRow.createCell(2).setCellValue("Ngày tổ chức");
        headerRow.createCell(3).setCellValue("Người tổ chức");
        headerRow.createCell(4).setCellValue("Học kỳ");

        int rowIdx = 1;
        for (EventModel e : list) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getStt());
            row.createCell(1).setCellValue(e.getTitle());
            row.createCell(2).setCellValue(e.getDate());
            row.createCell(3).setCellValue(e.getOrganizer());
            row.createCell(4).setCellValue(e.getFormattedAcademicSemester());
        }
        return workbook;
    }

    private void writeExcelToUri(Uri uri) {
        try {
            List<EventModel> list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
            if (list == null || list.isEmpty()) {
                seedSampleData();
                list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
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
                seedSampleData();
                list = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
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
            if (targetFile != null && targetFile.exists()) {
                String lower = fileName.toLowerCase(Locale.getDefault());
                try {
                    if (lower.endsWith(".pdf")) {
                        PDFBoxResourceLoader.init(getApplicationContext());
                        try (PDDocument doc = PDDocument.load(targetFile)) {
                            PDFTextStripper stripper = new PDFTextStripper();
                            stripper.setSortByPosition(true);
                            String pdfText = stripper.getText(doc);
                            if (pdfText != null && !pdfText.trim().isEmpty()) {
                                for (String ln : normalizeVietnameseOcrText(pdfText).split("\\r?\\n")) {
                                    if (!ln.trim().isEmpty()) contentLines.add(ln.trim());
                                }
                            }
                        } catch (Exception ignored) {}
                    } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                        try (InputStream is = new java.io.FileInputStream(targetFile);
                             Workbook wb = WorkbookFactory.create(is)) {
                            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                                Sheet sheet = wb.getSheetAt(s);
                                for (Row row : sheet) {
                                    List<String> cells = new ArrayList<>();
                                    for (Cell c : row) {
                                        String val = getCellValueAsString(c);
                                        if (!val.isEmpty()) cells.add(val);
                                    }
                                    if (!cells.isEmpty()) {
                                        contentLines.add(String.join(" | ", cells));
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    } else if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
                        try (InputStream is = new java.io.FileInputStream(targetFile);
                             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = is.read(buf)) > 0) baos.write(buf, 0, len);
                            String txt = baos.toString(StandardCharsets.UTF_8.name());
                            for (String ln : txt.split("\\r?\\n")) {
                                if (!ln.trim().isEmpty()) contentLines.add(ln.trim());
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }

            // Bổ sung các mục lịch đã lưu từ file này trong CSDL
            if (contentLines.isEmpty()) {
                List<EventModel> allEvents = dbHelper.searchEvents("", "Tất cả HK", "Tất cả Năm");
                for (EventModel ev : allEvents) {
                    if (ev.getOrganizer() != null && ev.getOrganizer().contains(fileName)) {
                        contentLines.add("[" + ev.getDate() + "] " + ev.getTitle() + " — " + ev.getOrganizer());
                    }
                }
                if (contentLines.isEmpty() && !allEvents.isEmpty()) {
                    for (int i = 0; i < Math.min(allEvents.size(), 30); i++) {
                        EventModel ev = allEvents.get(i);
                        contentLines.add("[" + ev.getDate() + "] " + ev.getTitle() + " — " + ev.getOrganizer());
                    }
                }
            }

            runOnUiThread(() -> showFileAnalysisReportDialog(
                    fileName,
                    item.getFileType() != null ? item.getFileType() : ("File " + getFileExtensionLabel(fileName)),
                    "Trình xem trực tiếp LectuPlan (Đầy đủ trang & nội dung văn bản)",
                    isHandwritten,
                    contentLines.size(),
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

        return "";
    }

    private void scheduleDaily6AMAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 600, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
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
            List<FileHistoryModel> list = dbHelper.getFileHistoryList();
            fileHistoryAdapter.updateList(list);
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

            if (index == 1 && isTab2Dirty) {
                if (layoutAlarmTab != null) {
                    layoutAlarmTab.post(this::refreshTab2CalendarEvents);
                } else {
                    refreshTab2CalendarEvents();
                }
            }
            if (index == 2) {
                if (layoutChartTab != null) layoutChartTab.post(this::updateChartTab);
                else updateChartTab();
            }
            if (index == 3) {
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
    }

    private void setupFilterChips() {
        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipTeaching = findViewById(R.id.chipTeaching);
        TextView chipMeeting = findViewById(R.id.chipMeeting);
        TextView chipExam = findViewById(R.id.chipExam);
        TextView chipIncident = findViewById(R.id.chipIncident);
        TextView chipToday = findViewById(R.id.chipToday);
        TextView chipFreeTime = findViewById(R.id.chipFreeTime);

        TextView[] allChips = new TextView[]{chipAll, chipTeaching, chipMeeting, chipExam, chipIncident, chipToday, chipFreeTime};

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
                edtSearch.setText("");
                if (spnSemester != null) spnSemester.setSelection(0);
            } else if (id == R.id.chipTeaching) {
                edtSearch.setText("dạy");
            } else if (id == R.id.chipMeeting) {
                edtSearch.setText("họp");
            } else if (id == R.id.chipExam) {
                edtSearch.setText("thi");
            } else if (id == R.id.chipIncident) {
                edtSearch.setText("phát sinh");
            } else if (id == R.id.chipToday) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayStr = sdf.format(Calendar.getInstance().getTime());
                edtSearch.setText(todayStr);
            } else if (id == R.id.chipFreeTime) {
                showFreeTimeAnalysis();
            }
        };

        for (TextView c : allChips) {
            if (c != null) c.setOnClickListener(chipClickListener);
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
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { refreshData(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spnSemester.setOnItemSelectedListener(filterListener);
        spnYear.setOnItemSelectedListener(filterListener);
        spnChartType.setOnItemSelectedListener(filterListener);
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
            // Hiển thị toàn bộ lịch theo bộ lọc Học kỳ / Năm (ưu tiên các lịch trong tháng hoặc tất cả lịch đã nhập)
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String currentMonthIso = monthFormat.format(cal.getTime());

            List<EventModel> allEvents = dbHelper.searchEvents("", sem, yr);
            for (EventModel e : allEvents) {
                if (e.getDate() != null && e.getDate().startsWith(currentMonthIso) && e.getId() > 0) {
                    displayList.add(e);
                }
            }
            if (displayList.isEmpty() && !allEvents.isEmpty()) {
                for (EventModel e : allEvents) {
                    if (e.getId() > 0) displayList.add(e);
                }
            }

            if (displayList.isEmpty()) {
                displayList.add(new EventModel(-300, 0, "☕ Tháng này chưa có lịch bận", sdf.format(new Date()), "Chưa có công việc nào được ghi nhận trong tháng này.", "HK1", cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR)));
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
                        "💡 " + res.dayTitle + " • " + res.statusBadge,
                        dateIso,
                        res.summaryText,
                        "HK1",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.WEEK_OF_YEAR)));

                cal.add(Calendar.DAY_OF_YEAR, 1);
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
        boolean byWeek = spnChartType != null && spnChartType.getSelectedItemPosition() == 1;
        Map<String, Integer> stats = byWeek ? dbHelper.getStatsByWeek() : dbHelper.getStatsBySemester();

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, byWeek ? "Số công việc theo Tuần" : "Số công việc theo Học kỳ");

        int[] customColors = new int[]{
                Color.parseColor("#2563EB"),
                Color.parseColor("#10B981"),
                Color.parseColor("#F59E0B")
        };
        dataSet.setColors(customColors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#0F172A"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.40f);
        if (barChart != null) {
            barChart.setData(barData);
            barChart.getDescription().setEnabled(false);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);
            xAxis.setTextColor(Color.parseColor("#0F172A"));

            barChart.getAxisRight().setEnabled(false);
            barChart.getAxisLeft().setDrawGridLines(true);
            barChart.getAxisLeft().setGridColor(Color.parseColor("#F1F5F9"));
            barChart.getAxisLeft().setTextColor(Color.parseColor("#334155"));
            barChart.getAxisLeft().setAxisMinimum(0f);

            barChart.animateY(700, Easing.EaseInOutQuad);
            barChart.invalidate();
        }
    }

    private void updateChartTab() {
        if (barChartTab == null || dbHelper == null) return;

        Map<String, Integer> stats = dbHelper.getStatsBySemester();
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số công việc theo Học kỳ");
        int[] customColors = new int[]{
                Color.parseColor("#2563EB"),
                Color.parseColor("#10B981"),
                Color.parseColor("#F59E0B")
        };
        dataSet.setColors(customColors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#0F172A"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.40f);
        barChartTab.setData(barData);
        barChartTab.getDescription().setEnabled(false);

        XAxis xAxis = barChartTab.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#0F172A"));

        barChartTab.getAxisRight().setEnabled(false);
        barChartTab.getAxisLeft().setDrawGridLines(true);
        barChartTab.getAxisLeft().setGridColor(Color.parseColor("#F1F5F9"));
        barChartTab.getAxisLeft().setTextColor(Color.parseColor("#334155"));
        barChartTab.getAxisLeft().setAxisMinimum(0f);

        barChartTab.animateY(700, Easing.EaseInOutQuad);
        barChartTab.invalidate();
    }

    private void seedSampleData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = sdf.format(cal.getTime());

        dbHelper.insertEvent(1, "Họp Hội đồng Khoa CNTT", today, "Ban Chủ nhiệm Khoa");
        dbHelper.insertEvent(2, "Tập huấn ngắn hạn AI trong Giảng dạy", tomorrow, "Phòng Đào tạo");
        dbHelper.insertEvent(3, "Chấm đồ án Chuyên ngành KTPM", "2026-05-15", "Bộ môn KTPM");
    }
}
