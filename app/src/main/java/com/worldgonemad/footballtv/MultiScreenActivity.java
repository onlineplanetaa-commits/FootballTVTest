package com.worldgonemad.footballtv;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MultiScreenActivity extends Activity {
    private static final int BG = Color.rgb(8, 10, 14);
    private static final int PANEL = Color.rgb(22, 26, 34);
    private static final int ACCENT = Color.rgb(55, 125, 255);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(170, 178, 190);

    private final ArrayList<String> titles = new ArrayList<>();
    private final ArrayList<String> urls = new ArrayList<>();
    private final WebView[] webViews = new WebView[4];
    private final TextView[] slotTitles = new TextView[4];
    private final FrameLayout[] slotFrames = new FrameLayout[4];
    private final View[] slotCells = new View[4];
    private int focusedSlot = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> incomingTitles = getIntent().getStringArrayListExtra("match_titles");
        ArrayList<String> incomingUrls = getIntent().getStringArrayListExtra("match_urls");
        if (incomingTitles != null) titles.addAll(incomingTitles);
        if (incomingUrls != null) urls.addAll(incomingUrls);
        buildScreen();
    }

    private GradientDrawable rounded(int color, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(10);
        d.setStroke(2, strokeColor);
        return d;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setBackground(rounded(ACCENT, ACCENT));
        return b;
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(14, 4, 14, 4);

        TextView heading = new TextView(this);
        heading.setText("МУЛЬТИЭКРАН  •  4 МАТЧА");
        heading.setTextColor(TEXT);
        heading.setTextSize(20);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(heading, new LinearLayout.LayoutParams(0, 48, 1));

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(130, 46));
        root.addView(header, new LinearLayout.LayoutParams(-1, 54));

        TextView hint = new TextView(this);
        hint.setText("Выбери окно и матч. Внутри окна можно открыть доступную трансляцию.");
        hint.setTextColor(MUTED);
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setPadding(16, 0, 16, 2);
        root.addView(hint, new LinearLayout.LayoutParams(-1, 28));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row1, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(row2, new LinearLayout.LayoutParams(-1, 0, 1));

        for (int i = 0; i < 4; i++) {
            final int slot = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            slotCells[i] = cell;
            cell.setBackground(rounded(PANEL, i == focusedSlot ? ACCENT : Color.rgb(65, 72, 84)));
            cell.setFocusable(true);
            cell.setFocusableInTouchMode(true);
            cell.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    focusedSlot = slot;
                    updateSlotBorders();
                }
            });

            LinearLayout controls = new LinearLayout(this);
            controls.setGravity(Gravity.CENTER_VERTICAL);
            controls.setPadding(5, 2, 5, 2);
            TextView name = new TextView(this);
            name.setText("Окно " + (i + 1) + " — выбери матч");
            name.setTextColor(TEXT);
            name.setTextSize(12);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            slotTitles[i] = name;
            controls.addView(name, new LinearLayout.LayoutParams(0, 42, 1));

            Button choose = makeButton("ВЫБРАТЬ");
            choose.setOnClickListener(v -> chooseMatch(slot));
            controls.addView(choose, new LinearLayout.LayoutParams(112, 40));
            cell.addView(controls, new LinearLayout.LayoutParams(-1, 44));

            FrameLayout frame = new FrameLayout(this);
            frame.setBackgroundColor(Color.BLACK);
            WebView web = new WebView(this);
            web.setBackgroundColor(Color.BLACK);
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setSupportMultipleWindows(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
            web.setWebChromeClient(new WebChromeClient());
            web.setWebViewClient(new WebViewClient());
            web.setFocusable(true);
            web.setFocusableInTouchMode(true);
            frame.addView(web, new FrameLayout.LayoutParams(-1, -1));
            webViews[i] = web;
            slotFrames[i] = frame;
            cell.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));

            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -1, 1);
            cellParams.setMargins(4, 3, 4, 3);
            (i < 2 ? row1 : row2).addView(cell, cellParams);
        }

        setContentView(root);
        updateSlotBorders();
        if (titles.isEmpty() || urls.isEmpty()) {
            Toast.makeText(this, "Список матчей не передан. Вернись в список матчей и открой мультиэкран снова.", Toast.LENGTH_LONG).show();
        } else {
            chooseMatch(0);
        }
        setImmersive(false);
    }

    private void updateSlotBorders() {
        for (int i = 0; i < slotCells.length; i++) {
            if (slotCells[i] != null) {
                slotCells[i].setBackground(rounded(PANEL,
                        i == focusedSlot ? Color.rgb(255, 205, 35) : Color.rgb(65, 72, 84)));
            }
        }
    }

    private void chooseMatch(int slot) {
        if (titles.isEmpty() || urls.isEmpty()) return;
        final String[] choices = new String[Math.min(titles.size(), urls.size())];
        for (int i = 0; i < choices.length; i++) choices[i] = titles.get(i);
        new AlertDialog.Builder(this)
                .setTitle("Выбери матч для окна " + (slot + 1))
                .setItems(choices, (dialog, which) -> loadMatch(slot, which))
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    private void loadMatch(int slot, int index) {
        if (slot < 0 || slot >= 4 || index < 0 || index >= urls.size()) return;
        String url = urls.get(index);
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "У матча нет ссылки на страницу трансляции.", Toast.LENGTH_SHORT).show();
            return;
        }
        slotTitles[slot].setText(titles.get(index));
        focusedSlot = slot;
        updateSlotBorders();

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36");
        headers.put("Referer", "https://livetv.sx/");
        webViews[slot].loadUrl(url, headers);
        webViews[slot].requestFocus();
    }

    private void setImmersive(boolean enabled) {
        View decor = getWindow().getDecorView();
        if (enabled) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        for (WebView web : webViews) {
            if (web != null) {
                try { web.stopLoading(); web.destroy(); } catch (Exception ignored) {}
            }
        }
        super.onDestroy();
    }
}
