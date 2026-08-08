package com.wasabi.daybits;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.TypedValue;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "daybits_store";
    private static final String KEY_BITS = "bits";
    private static final String KEY_FOCUS_TEXT = "focus_text";
    private static final String KEY_FOCUS_DONE = "focus_done";
    private static final String KEY_FOCUS_DATE = "focus_date";

    private final List<DayBit> bits = new ArrayList<>();
    private ScrollView pageScroll;
    private LinearLayout timeline;
    private LinearLayout heatmap;
    private TextView summary;
    private TextView focusStatus;
    private TextView focusToggle;
    private EditText focusInput;
    private LinearLayout composerCard;
    private EditText input;
    private String selectedMood = "平静";
    private String focusText = "";
    private String focusDate = "";
    private boolean focusDone = false;

    private final String[] moods = {"开心", "平静", "疲惫", "焦虑", "灵感"};
    private final int[] moodColors = {
            Color.rgb(227, 107, 79),
            Color.rgb(77, 140, 122),
            Color.rgb(91, 111, 141),
            Color.rgb(164, 92, 80),
            Color.rgb(242, 184, 75)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadBits();
        loadFocus();
        setContentView(buildContent());
        render();
    }

    private View buildContent() {
        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setClipToPadding(false);
        pageScroll.setBackgroundColor(Color.rgb(248, 247, 242));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), statusBarHeight() + dp(18), dp(18), dp(28));
        pageScroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView brand = label("DayBits", 15, Color.rgb(64, 72, 76), true);
        brand.setLetterSpacing(0.08f);
        root.addView(brand);

        TextView title = label("今天发生了什么", 30, Color.rgb(26, 34, 36), true);
        title.setPadding(0, dp(6), 0, dp(4));
        root.addView(title);

        summary = label("", 15, Color.rgb(85, 94, 96), false);
        summary.setLineSpacing(dp(2), 1.0f);
        root.addView(summary);

        root.addView(buildFocusCard(), margins(-1, -2, 0, dp(16), 0, dp(18)));

        root.addView(buildComposer(), margins(-1, -2, 0, dp(18), 0, dp(18)));

        TextView weekTitle = label("近七天情绪", 18, Color.rgb(26, 34, 36), true);
        root.addView(weekTitle, margins(-1, -2, 0, 0, 0, dp(10)));

        heatmap = new LinearLayout(this);
        heatmap.setOrientation(LinearLayout.HORIZONTAL);
        heatmap.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(heatmap, margins(-1, dp(78), 0, 0, 0, dp(20)));

        TextView timelineTitle = label("时间线", 18, Color.rgb(26, 34, 36), true);
        root.addView(timelineTitle, margins(-1, -2, 0, 0, 0, dp(10)));

        timeline = new LinearLayout(this);
        timeline.setOrientation(LinearLayout.VERTICAL);
        root.addView(timeline);

        return pageScroll;
    }

    private View buildComposer() {
        LinearLayout card = new LinearLayout(this);
        composerCard = card;
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(round(Color.WHITE, dp(16), Color.rgb(229, 225, 216), 1));

        input = new EditText(this);
        input.setHint("丢进一个生活碎片...");
        input.setTextColor(Color.rgb(27, 35, 36));
        input.setHintTextColor(Color.rgb(132, 139, 139));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setMinLines(2);
        input.setMaxLines(4);
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                pageScroll.postDelayed(() -> pageScroll.smoothScrollTo(0, Math.max(0, composerCard.getTop() - dp(12))), 180);
            }
        });
        card.addView(input, new LinearLayout.LayoutParams(-1, -2));

        HorizontalScrollView moodScroller = new HorizontalScrollView(this);
        moodScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout moodRow = new LinearLayout(this);
        moodRow.setOrientation(LinearLayout.HORIZONTAL);
        moodScroller.addView(moodRow);
        for (String mood : moods) {
            TextView chip = chip(mood, mood.equals(selectedMood));
            chip.setOnClickListener(v -> {
                selectedMood = ((TextView) v).getText().toString();
                renderMoodRow(moodRow);
            });
            moodRow.addView(chip, margins(-2, dp(38), 0, 0, dp(8), 0));
        }
        card.addView(moodScroller, margins(-1, -2, 0, dp(14), 0, dp(14)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        TextView hint = label("自动识别标签，本地保存", 13, Color.rgb(111, 119, 120), false);
        actions.addView(hint, margins(-1, -2, 0, 0, 0, dp(8)));

        TextView add = button("记录");
        add.setOnClickListener(v -> addBit());
        actions.addView(add, new LinearLayout.LayoutParams(-1, dp(46)));
        card.addView(actions);

        return card;
    }

    private View buildFocusCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(16));
        card.setBackground(gradient(Color.rgb(248, 229, 190), Color.rgb(225, 238, 220), dp(16)));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("今日小目标", 18, Color.rgb(35, 52, 59), true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        focusStatus = label("", 13, Color.rgb(65, 89, 84), true);
        focusStatus.setGravity(Gravity.CENTER);
        focusStatus.setBackground(round(Color.argb(70, 255, 255, 255), dp(14), Color.TRANSPARENT, 0));
        header.addView(focusStatus, margins(dp(86), dp(30), dp(10), 0, 0, 0));
        card.addView(header);

        focusInput = new EditText(this);
        focusInput.setSingleLine(true);
        focusInput.setTextColor(Color.rgb(27, 35, 36));
        focusInput.setHintTextColor(Color.rgb(107, 119, 118));
        focusInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        focusInput.setHint("写一个今天想完成的小动作");
        focusInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        focusInput.setBackground(round(Color.argb(92, 255, 255, 255), dp(12), Color.argb(90, 255, 255, 255), 1));
        focusInput.setPadding(dp(12), 0, dp(12), 0);
        card.addView(focusInput, margins(-1, dp(46), 0, dp(12), 0, dp(12)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView save = secondaryButton("保存");
        save.setOnClickListener(v -> saveFocusFromInput());
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(42), 1));
        focusToggle = button("完成");
        focusToggle.setOnClickListener(v -> toggleFocusDone());
        actions.addView(focusToggle, weightedMargins(0, dp(42), 1, dp(10), 0, 0, 0));
        card.addView(actions);

        return card;
    }

    private void renderMoodRow(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            TextView child = (TextView) row.getChildAt(i);
            boolean chosen = child.getText().toString().equals(selectedMood);
            child.setTextColor(chosen ? Color.WHITE : Color.rgb(51, 61, 63));
            child.setTypeface(Typeface.DEFAULT, chosen ? Typeface.BOLD : Typeface.NORMAL);
            styleChip(child, chosen);
        }
    }

    private void addBit() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            input.requestFocus();
            return;
        }
        bits.add(0, new DayBit(System.currentTimeMillis(), text, selectedMood, inferTag(text)));
        input.setText("");
        hideKeyboard();
        saveBits();
        render();
    }

    private void render() {
        renderSummary();
        renderFocus();
        renderHeatmap();
        renderTimeline();
    }

    private void renderSummary() {
        long now = System.currentTimeMillis();
        DayMoodStats stats = moodStatsForDay(now);
        String topMood = stats.total == 0 ? "暂无" : dominantMood(stats.moodCounts, stats.latestMoodTimes);
        summary.setText("今天有 " + stats.total + " 个生活碎片，状态偏「" + topMood + "」。先抓住一个小目标，再把重要瞬间留给晚上回看。");
    }

    private void renderFocus() {
        if (focusInput == null) {
            return;
        }
        focusInput.setText(focusText);
        boolean hasFocusText = !focusText.trim().isEmpty();
        focusStatus.setText(focusDone ? "已完成" : (hasFocusText ? "进行中" : "待设定"));
        focusStatus.setTextColor(focusDone ? Color.rgb(39, 99, 70) : Color.rgb(65, 89, 84));
        focusToggle.setText(focusDone ? "重开" : "完成");
        focusToggle.setAlpha(hasFocusText ? 1.0f : 0.55f);
        focusToggle.setBackgroundResource(focusDone ? R.drawable.bg_button_goal_done : R.drawable.bg_button_goal);
    }

    private void renderHeatmap() {
        heatmap.removeAllViews();
        for (int i = 6; i >= 0; i--) {
            long time = dayOffsetFromToday(-i);
            DayMoodStats stats = moodStatsForDay(time);
            boolean empty = stats.total == 0;
            String topMood = empty ? "无" : dominantMood(stats.moodCounts, stats.latestMoodTimes);
            int textColor = empty ? Color.rgb(82, 89, 88) : Color.WHITE;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setBackground(round(empty ? Color.rgb(232, 230, 224) : moodColor(topMood), dp(14), Color.TRANSPARENT, 0));
            TextView label = label(dayName(time), 12, textColor, true);
            TextView mood = label(topMood, 12, textColor, true);
            TextView count = label(String.valueOf(stats.total), 18, textColor, true);
            cell.addView(label);
            cell.addView(mood);
            cell.addView(count);
            heatmap.addView(cell, weightedMargins(0, -1, 1, 0, 0, i == 0 ? 0 : dp(8), 0));
        }
    }

    private void renderTimeline() {
        timeline.removeAllViews();
        for (DayBit bit : bits) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(15), dp(14), dp(15), dp(14));
            card.setBackground(round(Color.WHITE, dp(14), Color.rgb(229, 225, 216), 1));

            LinearLayout meta = new LinearLayout(this);
            meta.setGravity(Gravity.CENTER_VERTICAL);
            TextView time = label(formatTime(bit.time), 13, Color.rgb(99, 107, 109), true);
            meta.addView(time, new LinearLayout.LayoutParams(0, -2, 1));
            TextView mood = label(bit.mood, 13, Color.WHITE, true);
            mood.setGravity(Gravity.CENTER);
            mood.setBackground(round(moodColor(bit.mood), dp(18), Color.TRANSPARENT, 0));
            meta.addView(mood, margins(dp(64), dp(32), 0, 0, dp(8), 0));
            TextView delete = label("X", 14, Color.rgb(117, 80, 74), true);
            delete.setGravity(Gravity.CENTER);
            delete.setContentDescription("删除这条记录");
            delete.setBackgroundResource(R.drawable.bg_button_delete);
            ButtonMotion.install(delete);
            delete.setOnClickListener(v -> confirmDelete(bit));
            meta.addView(delete, new LinearLayout.LayoutParams(dp(40), dp(32)));
            card.addView(meta);

            TextView body = label(bit.text, 17, Color.rgb(29, 38, 40), false);
            body.setLineSpacing(dp(3), 1.0f);
            card.addView(body, margins(-1, -2, 0, dp(10), 0, dp(12)));

            TextView tag = label("#" + bit.tag, 13, Color.rgb(64, 72, 76), true);
            tag.setGravity(Gravity.CENTER);
            tag.setBackground(round(Color.rgb(242, 240, 234), dp(16), Color.TRANSPARENT, 0));
            card.addView(tag, new LinearLayout.LayoutParams(dp(84), dp(30)));

            timeline.addView(card, margins(-1, -2, 0, 0, 0, dp(12)));
        }
        if (bits.isEmpty()) {
            timeline.addView(emptyTimelineView());
        }
    }

    private View emptyTimelineView() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(22), dp(18), dp(22));
        card.setBackground(round(Color.WHITE, dp(14), Color.rgb(229, 225, 216), 1));

        TextView title = label("还没有日记", 18, Color.rgb(35, 52, 59), true);
        TextView body = label("写下第一条碎片，今天的情绪和时间线会马上生成。", 14, Color.rgb(98, 107, 109), false);
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, dp(6), 0, dp(14));
        TextView action = button("开始记录");
        action.setOnClickListener(v -> {
            input.requestFocus();
            pageScroll.smoothScrollTo(0, Math.max(0, composerCard.getTop() - dp(12)));
        });

        card.addView(title);
        card.addView(body);
        card.addView(action, new LinearLayout.LayoutParams(-1, dp(44)));
        return card;
    }

    private void confirmDelete(DayBit bit) {
        new AlertDialog.Builder(this)
                .setTitle("删除这条日记？")
                .setMessage(bit.text)
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> deleteBit(bit))
                .show();
    }

    private void deleteBit(DayBit bit) {
        bits.remove(bit);
        saveBitsNow();
        render();
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private TextView chip(String text, boolean selected) {
        TextView chip = label(text, 14, selected ? Color.WHITE : Color.rgb(51, 61, 63), selected);
        chip.setGravity(Gravity.CENTER);
        styleChip(chip, selected);
        ButtonMotion.install(chip);
        return chip;
    }

    private TextView button(String text) {
        TextView button = label(text, 15, Color.WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_button_primary);
        ButtonMotion.install(button);
        return button;
    }

    private TextView secondaryButton(String text) {
        TextView button = label(text, 15, Color.rgb(35, 52, 59), true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_button_secondary);
        ButtonMotion.install(button);
        return button;
    }

    private void styleChip(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackground(moodButtonBackground(chip.getText().toString(), dp(24)));
        } else {
            chip.setBackgroundResource(R.drawable.bg_button_chip);
        }
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private LinearLayout.LayoutParams weightedMargins(int width, int height, float weight, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private GradientDrawable gradient(int startColor, int endColor, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private RippleDrawable moodButtonBackground(String mood, int radius) {
        int base = moodColor(mood);
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{tint(base, 1.08f), tint(base, 0.88f)}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), tint(base, 1.18f));
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(52, 255, 255, 255)), drawable, null);
    }

    private int tint(int color, float amount) {
        return Color.rgb(
                clamp(Math.round(Color.red(color) * amount)),
                clamp(Math.round(Color.green(color) * amount)),
                clamp(Math.round(Color.blue(color) * amount))
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id <= 0) {
            return 0;
        }
        return getResources().getDimensionPixelSize(id);
    }

    private int moodColor(String mood) {
        int index = moodIndex(mood);
        return index >= 0 ? moodColors[index] : Color.rgb(205, 209, 201);
    }

    private int moodIndex(String mood) {
        for (int i = 0; i < moods.length; i++) {
            if (moods[i].equals(mood)) {
                return i;
            }
        }
        return -1;
    }

    private String dominantMood(int[] moodCounts, long[] latestMoodTimes) {
        int best = 0;
        for (int i = 1; i < moodCounts.length; i++) {
            boolean hasMoreRecords = moodCounts[i] > moodCounts[best];
            boolean tiedButNewer = moodCounts[i] == moodCounts[best] && latestMoodTimes[i] > latestMoodTimes[best];
            if (hasMoreRecords || tiedButNewer) {
                best = i;
            }
        }
        return moods[best];
    }

    private int colorForDay(long time) {
        DayMoodStats stats = moodStatsForDay(time);
        if (stats.total == 0) {
            return Color.rgb(205, 209, 201);
        }
        return moodColor(dominantMood(stats.moodCounts, stats.latestMoodTimes));
    }

    private int countForDay(long time) {
        return moodStatsForDay(time).total;
    }

    private DayMoodStats moodStatsForDay(long time) {
        DayMoodStats stats = new DayMoodStats(moods.length);
        for (DayBit bit : bits) {
            if (sameDay(time, bit.time)) {
                int index = moodIndex(bit.mood);
                if (index < 0) {
                    continue;
                }
                stats.total++;
                stats.moodCounts[index]++;
                stats.latestMoodTimes[index] = Math.max(stats.latestMoodTimes[index], bit.time);
            }
        }
        return stats;
    }

    private boolean sameDay(long a, long b) {
        return startOfLocalDay(a) == startOfLocalDay(b);
    }

    private long dayOffsetFromToday(int offset) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, offset);
        return calendar.getTimeInMillis();
    }

    private long startOfLocalDay(long time) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String dayName(long time) {
        return new SimpleDateFormat("E", Locale.CHINA).format(new Date(time));
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(time));
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    private String inferTag(String text) {
        if (containsAny(text, "跑", "健身", "散步", "运动")) return "运动";
        if (containsAny(text, "会", "需求", "代码", "工作", "项目")) return "工作";
        if (containsAny(text, "朋友", "聊天", "聚", "家人")) return "关系";
        if (containsAny(text, "吃", "咖啡", "饭", "甜")) return "饮食";
        if (containsAny(text, "想法", "灵感", "突然", "可以做")) return "灵感";
        return "生活";
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private void seedDemoBits() {
        long now = System.currentTimeMillis();
        bits.add(new DayBit(now - 20 * 60 * 1000, "把 DayBits 的第一个原型跑起来了，先记录一句今天的小胜利。", "灵感", "灵感"));
        bits.add(new DayBit(now - 3 * 60 * 60 * 1000, "下午喝了一杯咖啡，顺手整理了几个待办。", "平静", "饮食"));
        bits.add(new DayBit(now - 26 * 60 * 60 * 1000, "散步十五分钟，脑子清爽很多。", "开心", "运动"));
    }

    private void loadBits() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String stored = prefs.getString(KEY_BITS, "");
        if (stored == null || stored.isEmpty()) {
            return;
        }
        String[] rows = stored.split("\\n");
        for (String row : rows) {
            String[] parts = row.split("\\|", -1);
            if (parts.length != 4) {
                continue;
            }
            try {
                long time = Long.parseLong(parts[0]);
                String text = decode(parts[1]);
                String mood = decode(parts[2]);
                String tag = decode(parts[3]);
                if (!text.isEmpty() && moodIndex(mood) >= 0 && !tag.isEmpty()) {
                    bits.add(new DayBit(time, text, mood, tag));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void loadFocus() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String today = todayKey();
        focusDate = prefs.getString(KEY_FOCUS_DATE, today);
        if (!today.equals(focusDate)) {
            focusDate = today;
            focusText = "";
            focusDone = false;
            saveFocusNow();
            return;
        }
        focusText = prefs.getString(KEY_FOCUS_TEXT, "");
        focusDone = prefs.getBoolean(KEY_FOCUS_DONE, false);
    }

    private void saveFocusFromInput() {
        focusText = focusInput.getText().toString().trim();
        if (focusText.isEmpty()) {
            focusDone = false;
            focusInput.requestFocus();
        }
        focusDate = todayKey();
        saveFocusNow();
        hideKeyboard();
        renderFocus();
    }

    private void toggleFocusDone() {
        String currentText = focusInput.getText().toString().trim();
        if (currentText.isEmpty() && focusText.isEmpty()) {
            focusInput.requestFocus();
            return;
        }
        focusText = currentText.isEmpty() ? focusText : currentText;
        focusDone = !focusDone;
        focusDate = todayKey();
        saveFocusNow();
        hideKeyboard();
        renderFocus();
    }

    private boolean saveFocusNow() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_FOCUS_DATE, focusDate)
                .putString(KEY_FOCUS_TEXT, focusText)
                .putBoolean(KEY_FOCUS_DONE, focusDone)
                .commit();
    }

    private void saveBits() {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(bits.size(), 120);
        for (int i = 0; i < limit; i++) {
            DayBit bit = bits.get(i);
            builder.append(bit.time)
                    .append('|').append(encode(bit.text))
                    .append('|').append(encode(bit.mood))
                    .append('|').append(encode(bit.tag))
                    .append('\n');
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_BITS, builder.toString()).apply();
    }

    private boolean saveBitsNow() {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(bits.size(), 120);
        for (int i = 0; i < limit; i++) {
            DayBit bit = bits.get(i);
            builder.append(bit.time)
                    .append('|').append(encode(bit.text))
                    .append('|').append(encode(bit.mood))
                    .append('|').append(encode(bit.tag))
                    .append('\n');
        }
        return getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_BITS, builder.toString()).commit();
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static class DayBit {
        final long time;
        final String text;
        final String mood;
        final String tag;

        DayBit(long time, String text, String mood, String tag) {
            this.time = time;
            this.text = text;
            this.mood = mood;
            this.tag = tag;
        }
    }

    private static class DayMoodStats {
        int total;
        final int[] moodCounts;
        final long[] latestMoodTimes;

        DayMoodStats(int moodCount) {
            moodCounts = new int[moodCount];
            latestMoodTimes = new long[moodCount];
        }
    }
}
