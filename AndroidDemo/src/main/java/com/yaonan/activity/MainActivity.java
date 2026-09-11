package com.yaonan.activity;

import static com.yaonan.util.global.Global.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tencent.mmkv.MMKV;
import com.yaonan.R;
import com.yaonan.databinding.ActivityMainBinding;
import com.yaonan.service.TimerService;
import com.yaonan.util.NotificationHelper;
import com.yaonan.util.WindowHelper;
import com.yaonan.util.codec.Codec;
import com.yaonan.util.jna.UI;
import com.yaonan.util.json.Array;
import com.yaonan.util.lang.StringUtil;
import com.yaonan.util.lang.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 应用主界面 Activity。
 *
 * <p>职责：展示并管理悬浮弹窗、无障碍服务、定时任务三者的授权与启停入口，
 * 以及"批量单删"定时任务的动态行配置（时间 + 脚本类型），并负责主题（默认/手账风）的切换。</p>
 */
public class MainActivity extends AppCompatActivity {

    /** 视图绑定对象，用于访问布局中的控件 */
    private ActivityMainBinding binding;
    /** 是否处于手账风主题 */
    private boolean isJournalTheme = false;
    /** 最近一次点击开发者标签的时间戳，用于判断是否在 500ms 内连续点击（双击切换主题） */
    private long lastDevTagClickTime = 0;
    /** MMKV 中保存主题状态的键名 */
    private static final String KEY_JOURNAL_THEME = "journal_theme";

    /**
     * 创建界面：绑定布局、初始化主题、注册各按钮点击事件，并恢复已保存的定时任务动态行。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.check(this);

        MMKV kv = UI.getMMKV();

        // 从持久化存储中读取主题状态并应用
        isJournalTheme = kv.decodeBool(KEY_JOURNAL_THEME, false);
        applyTheme();

        // 开发者标签：500ms 内连续点击两次即切换主题（双击在默认/手账风之间切换）
        binding.devTagContainer.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastDevTagClickTime < 500) {
                isJournalTheme = !isJournalTheme;
                kv.encode(KEY_JOURNAL_THEME, isJournalTheme);
                applyTheme();
                UI.alert(isJournalTheme ? "已切换为手账风 ♡" : "已切换为默认风格", this);
            }
            lastDevTagClickTime = now;
        });

        // 弹窗授权
        binding.btnShowScreenshot.setOnClickListener(v -> {
            if (WindowHelper.checkOverlay(this)) {
                WindowHelper.showScreenshotView();
            }
        });
        binding.btnHideScreenshot.setOnClickListener(v -> {
            if (WindowHelper.checkOverlay(this)) {
                WindowHelper.hideScreenshotView();
            }
        });

        // 无障碍授权
        binding.btnStartA.setOnClickListener(v -> {
            AccessibilityManager am =
                    (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am.isEnabled()) {
                UI.alert("无障碍服务已打开");
            } else {
                UI.alert("无障碍服务未打开");
            }

            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // 定时任务
        binding.btnStartTimer.setOnClickListener(v -> {
            TimerService.start();
            UI.alert("1、悬浮弹窗、无障碍服务、定时任务，要同时开启！\n2、可在通知中，查看定时任务是否运行！", this);
        });
        binding.btnStopTimer.setOnClickListener(v -> {
            TimerService.stop();
            UI.alert("定时任务运行脚本时，只能通过此按钮停止！", this);
        });

        // 批量单删：+/- 按钮
        binding.btnBatchPlus.setOnClickListener(v -> {
            LinearLayout rootLayout = findViewById(R.id.timer_rows);
            addRow(rootLayout.getChildCount(), null, null);
        });
        binding.btnBatchMinus.setOnClickListener(v -> {
            LinearLayout rootLayout = findViewById(R.id.timer_rows);
            if (rootLayout.getChildCount() > 1) {
                delRow(rootLayout.getChildCount() - 1);
            }
        });

        // 定时任务，初始化动态行
        String tasksJsonStr = kv.getString("tasks", "[]");
        Array tasksArray = new Array(tasksJsonStr);
        if (tasksArray.size() == 0) {
            addRow(0, null, null);
        } else {
            for (int i = 0; i < tasksArray.size(); i++) {
                Array taskArray = tasksArray.getArray(i);
                String time = taskArray.getString(0);
                int type = taskArray.getInteger(1);
                // 存储的脚本类型值还原为下拉框下标（类型 - 2），与 addRow 的 defaultType 语义保持一致
                addRow(i, time, type - 2);
            }
        }
    }

    /**
     * 保存动态行
     * 空值的行不保存！
     */
    private void saveTasks() {
        LinearLayout rootLayout = findViewById(R.id.timer_rows);

        List<List<Object>> tasksArray = new ArrayList<>();
        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            RelativeLayout lineLayout = (RelativeLayout) rootLayout.getChildAt(i);
            EditText timeText = (EditText) lineLayout.getChildAt(0);
            Spinner timeSpinner = (Spinner) lineLayout.getChildAt(1);
            String time = Objects.requireNonNullElse(timeText.getText(), "") + "";
            // 脚本类型存储值 = 下拉框下标 + 2（下拉框下标从 0 开始，而"批量单删"对应类型 2）
            int type = timeSpinner.getSelectedItemPosition() + 2;
            if (StringUtil.isNotEmpty(time)) {
                tasksArray.add(List.of(time, type));
            }
        }

        String tasksJsonStr = Codec.json_encode(tasksArray);
        Log.d(TAG, "保存定时任务: " + tasksJsonStr);
        MMKV kv = UI.getMMKV();
        kv.putString("tasks", tasksJsonStr);
    }

    /**
     * 添加行
     * @param index
     * @param defaultTime null不设置默认值
     * @param defaultType null不设置默认值
     */
    @SuppressLint("ClickableViewAccessibility")
    private void addRow(int index, String defaultTime, Integer defaultType) {
        LinearLayout rootLayout = findViewById(R.id.timer_rows);

        RelativeLayout lineLayout = new RelativeLayout(MainActivity.this);
        LinearLayout.LayoutParams lineLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rootLayout.addView(lineLayout, index, lineLayoutParams);

        // 创建【时间控件】
        EditText timeText = new EditText(MainActivity.this);
        if (defaultTime != null) {
            timeText.setText(defaultTime);
        }
        timeText.setId(View.generateViewId());
        timeText.setEms(3);
        timeText.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }
            String PATTERN = "HH:mm";
            EditText editText = new EditText(MainActivity.this);
            String oldTime = Objects.requireNonNullElse(timeText.getText(), "") + "";
            if (StringUtil.isEmpty(oldTime)) {
                editText.setText(TimeUtil.now(PATTERN));
            } else {
                editText.setText(oldTime);
            }

            new AlertDialog.Builder(MainActivity.this)
                    .setView(editText)
                    .setTitle("请输入时间")
                    .setNeutralButton("清空", (dialog, which) -> {
                        timeText.setText("");
                        UI.alert("保存：\"\"");
                        saveTasks();
                    })
                    .setNegativeButton("取消", (dialog, which) -> {
                    })
                    .setPositiveButton("确定", (dialog, which) -> {
                        String newTime = Objects.requireNonNullElse(editText.getText(), "") + "";
                        if (StringUtil.isEmpty(newTime)) {
                            timeText.setText("");
                            UI.alert("保存：\"\"");
                            saveTasks();
                        } else {
                            try {
                                String formatTime = TimeUtil.format(TimeUtil.parse(newTime, PATTERN), PATTERN);
                                timeText.setText(formatTime);
                                UI.alert("保存：" + formatTime);
                                saveTasks();
                            } catch (Exception e) {
                                UI.alert("时间格式错误！");
                            }
                        }
                    })
                    .show();
            return true;
        });
        RelativeLayout.LayoutParams timeTextParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lineLayout.addView(timeText, timeTextParams);

        // 创建【脚本类型】
        Spinner timeSpinner = new Spinner(MainActivity.this);
        timeSpinner.setAdapter(new ArrayAdapter<>(
                timeSpinner.getContext(), android.R.layout.simple_spinner_dropdown_item,
                List.of("批量单删")));
        timeSpinner.setSelection(0);
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // 用于跳过初始化时 setSelection 触发的第一次回调，避免重复保存
            boolean isSetSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSetSelection) {
                    isSetSelection = false;
                    return;
                }
                saveTasks();
                UI.alert("保存：批量单删");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        RelativeLayout.LayoutParams timeSpinnerParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        timeSpinnerParams.addRule(RelativeLayout.END_OF, timeText.getId());
        timeSpinnerParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        lineLayout.addView(timeSpinner, timeSpinnerParams);
    }

    /**
     * 删除行
     * @param index
     */
    private void delRow(int index) {
        LinearLayout rootLayout = findViewById(R.id.timer_rows);
        rootLayout.removeViewAt(index);
        saveTasks();
    }

    // ===================== 主题切换 =====================

    /**
     * 根据当前主题标记分发到对应的主题应用方法。
     */
    private void applyTheme() {
        if (isJournalTheme) {
            applyJournalTheme();
        } else {
            applyDefaultTheme();
        }
    }

    /**
     * 应用"手账风"主题：替换各卡片、图标、按钮的背景与文字颜色，并叠加阴影、旋转等装饰效果。
     */
    private void applyJournalTheme() {
        float density = getResources().getDisplayMetrics().density;
        int colorCardTitle = Color.parseColor("#5D4E6D");
        int colorDisabled = Color.parseColor("#B0A5C0");
        int colorDevText = Color.parseColor("#7B6B2D");

        binding.scrollRoot.setBackgroundResource(R.drawable.bg_ha_page);

        binding.tvTitle.setTextColor(Color.parseColor("#FF8FA3"));
        binding.tvTitle.setText("企微单删 ♡");
        binding.tvTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        binding.tvTitle.setShadowLayer(3, 2, 2, Color.parseColor("#FFD6E0"));

        binding.tvSubtitle.setTextColor(Color.parseColor("#A89BBE"));
        binding.tvSubtitle.setText("~ 科技改变生活 ~");

        applyCardJournal(binding.cardPopup, R.drawable.bg_ha_card_1, -0.8f, density);
        binding.iconPopupBg.setBackgroundResource(R.drawable.bg_ha_icon_popup);
        binding.iconPopupImg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#5D9C84")));
        binding.tvPopupTitle.setTextColor(colorCardTitle);

        applyCardJournal(binding.cardAccess, R.drawable.bg_ha_card_2, 0.6f, density);
        binding.iconAccessBg.setBackgroundResource(R.drawable.bg_ha_icon_access);
        binding.iconAccessImg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#6B7BA8")));
        binding.tvAccessTitle.setTextColor(colorCardTitle);

        applyCardJournal(binding.cardTimer, R.drawable.bg_ha_card_3, -0.5f, density);
        binding.iconTimerBg.setBackgroundResource(R.drawable.bg_ha_icon_timer);
        binding.iconTimerImg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#C4845A")));
        binding.tvTimerTitle.setTextColor(colorCardTitle);

        applyCardJournal(binding.cardDelete, R.drawable.bg_ha_card_4, 0.7f, density);
        binding.iconDeleteBg.setBackgroundResource(R.drawable.bg_ha_icon_delete);
        binding.iconDeleteImg.setImageTintList(ColorStateList.valueOf(Color.parseColor("#C46460")));
        binding.tvDeleteTitle.setTextColor(colorCardTitle);

        binding.cardFooter.setBackgroundResource(R.drawable.bg_ha_footer);
        binding.cardFooter.setRotation(-0.3f);
        binding.cardFooter.setElevation(0);
        binding.btnAbout.setBackgroundResource(R.drawable.bg_ha_btn_disabled);
        binding.btnAbout.setTextColor(colorDisabled);

        binding.devTagContainer.setBackgroundResource(R.drawable.bg_ha_dev_tag);
        binding.devTagText.setTextColor(colorDevText);
        binding.devTagImg.setImageTintList(ColorStateList.valueOf(colorDevText));

        binding.btnShowScreenshot.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnShowScreenshot.setTextColor(Color.WHITE);
        binding.btnHideScreenshot.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnHideScreenshot.setTextColor(Color.WHITE);
        binding.btnStartA.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnStartA.setTextColor(Color.WHITE);
        binding.btnStartTimer.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnStartTimer.setTextColor(Color.WHITE);
        binding.btnStopTimer.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnStopTimer.setTextColor(Color.WHITE);
        binding.btnBatchMinus.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnBatchMinus.setTextColor(Color.WHITE);
        binding.btnBatchPlus.setBackgroundResource(R.drawable.bg_ha_btn_primary);
        binding.btnBatchPlus.setTextColor(Color.WHITE);
    }

    /**
     * 将指定卡片应用为手账风样式（自定义背景、旋转角度、按密度缩放阴影）。
     */
    private void applyCardJournal(View card, int bgRes, float rotation, float density) {
        card.setBackgroundResource(bgRes);
        card.setRotation(rotation);
        card.setElevation(density * 2);
    }

    /**
     * 应用默认主题：恢复各卡片、图标、按钮的默认背景与文字颜色。
     */
    private void applyDefaultTheme() {
        float density = getResources().getDisplayMetrics().density;
        int colorTextPrimary = ContextCompat.getColor(this, R.color.text_primary);
        int colorTextSecondary = ContextCompat.getColor(this, R.color.text_secondary);
        int colorBtnDisabledText = ContextCompat.getColor(this, R.color.btn_disabled_text);
        int colorDevTagText = ContextCompat.getColor(this, R.color.dev_tag_text);

        binding.scrollRoot.setBackgroundResource(R.color.page_bg);

        binding.tvTitle.setTextColor(colorTextPrimary);
        binding.tvTitle.setText("企微单删");
        binding.tvTitle.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        binding.tvTitle.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        binding.tvSubtitle.setTextColor(colorTextSecondary);
        binding.tvSubtitle.setText("科技改变生活");

        applyCardDefault(binding.cardPopup, density);
        binding.iconPopupBg.setBackgroundResource(R.drawable.bg_icon_popup);
        binding.iconPopupImg.setImageTintList(null);
        binding.tvPopupTitle.setTextColor(colorTextPrimary);

        applyCardDefault(binding.cardAccess, density);
        binding.iconAccessBg.setBackgroundResource(R.drawable.bg_icon_access);
        binding.iconAccessImg.setImageTintList(null);
        binding.tvAccessTitle.setTextColor(colorTextPrimary);

        applyCardDefault(binding.cardTimer, density);
        binding.iconTimerBg.setBackgroundResource(R.drawable.bg_icon_timer);
        binding.iconTimerImg.setImageTintList(null);
        binding.tvTimerTitle.setTextColor(colorTextPrimary);

        applyCardDefault(binding.cardDelete, density);
        binding.iconDeleteBg.setBackgroundResource(R.drawable.bg_icon_delete);
        binding.iconDeleteImg.setImageTintList(null);
        binding.tvDeleteTitle.setTextColor(colorTextPrimary);

        binding.cardFooter.setBackgroundResource(R.drawable.bg_card);
        binding.cardFooter.setRotation(0);
        binding.cardFooter.setElevation(density * 1);
        binding.btnAbout.setBackgroundResource(R.drawable.bg_btn_disabled);
        binding.btnAbout.setTextColor(colorBtnDisabledText);

        binding.devTagContainer.setBackgroundResource(R.drawable.bg_dev_tag);
        binding.devTagText.setTextColor(colorDevTagText);
        binding.devTagImg.setImageTintList(null);

        binding.btnShowScreenshot.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnShowScreenshot.setTextColor(Color.WHITE);
        binding.btnHideScreenshot.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnHideScreenshot.setTextColor(Color.WHITE);
        binding.btnStartA.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnStartA.setTextColor(Color.WHITE);
        binding.btnStartTimer.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnStartTimer.setTextColor(Color.WHITE);
        binding.btnStopTimer.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnStopTimer.setTextColor(Color.WHITE);
        binding.btnBatchMinus.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnBatchMinus.setTextColor(Color.WHITE);
        binding.btnBatchPlus.setBackgroundResource(R.drawable.bg_btn_primary);
        binding.btnBatchPlus.setTextColor(Color.WHITE);
    }

    /**
     * 将指定卡片恢复为默认样式（默认背景、无旋转、按密度缩放阴影）。
     */
    private void applyCardDefault(View card, float density) {
        card.setBackgroundResource(R.drawable.bg_card);
        card.setRotation(0);
        card.setElevation(density * 1);
    }
}
