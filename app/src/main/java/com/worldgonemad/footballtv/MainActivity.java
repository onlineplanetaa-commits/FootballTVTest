package com.worldgonemad.footballtv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;

import androidx.media3.datasource.DefaultHttpDataSource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {

    private ExoPlayer player;
    private PlayerView playerView;
    private WebView resolverWebView;
    private WebView matchLoaderWebView;
    private boolean youtubePlayerScreen = false;

    // True while the Activity is showing the player/resolver screen.
    // Match refreshes must never replace that screen.
    private boolean playerScreen = false;
    private boolean flashscoreScreen = false;
    private WebView flashscoreWebView;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);
    private final int LIVE = Color.rgb(90, 220, 140);

    private static final String PRIMARY_LIVE_TV_URL =
            "https://livetv.sx/allupcomingsports/1/";

    private static final String НАЗАДUP_LIVE_TV_URL =
            "https://livetv904.me/allupcomingsports/1/";

    // LiveTV schedule widget embedded on myfootball.sbs/football.
    private static final String MYFOOTBALL_SCHEDULE_URL =
            "https://livetv.sx/export/webmasters.php?id=1157892&lang=ru&s=0";

    private final ArrayList<Match> matches =
            new ArrayList<>();
    private final ArrayList<Match> todayMatches = new ArrayList<>();

    private static final int SECTION_TODAY = 0;
    private static final int SECTION_ONLINE = 1;
    private static final int SECTION_FAVORITES = 2;
    private int selectedSection = SECTION_ONLINE;
    private final Set<String> favoriteIds = new HashSet<>();
    private SharedPreferences preferences;

    private LinearLayout listContainer;
    private TextView statusText;
    private TextView sectionTitle;
    private TextView kyivClockText;

    private final Runnable refreshRunnable =
            new Runnable() {
                @Override
                public void run() {
                    loadMatches();

                    handler.postDelayed(
                            this,
                            5 * 60 * 1000
                    );
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("max_football_preferences", MODE_PRIVATE);
        favoriteIds.addAll(preferences.getStringSet("favorite_match_ids", new HashSet<>()));

        // Show cached LiveTV matches immediately. On a fresh install, keep the loading screen
        // visible until the first network load finishes instead of showing an empty list.
        ArrayList<Match> cachedLiveAtStart = readLiveMatchesCache();
        ArrayList<Match> cachedDailyAtStart = readTodayMatchesCache();
        matches.addAll(cachedLiveAtStart);
        todayMatches.addAll(cachedDailyAtStart);
        if (cachedLiveAtStart.isEmpty() && cachedDailyAtStart.isEmpty()) {
            showLoading();
        } else {
            showMatches();
        }

        loadMatches();

        handler.postDelayed(
                refreshRunnable,
                5 * 60 * 1000
        );
    }

    private TextView label(
            String text,
            float size,
            int color
    ) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);

        return t;
    }

    private GradientDrawable bg(
            int color,
            float radius
    ) {
        GradientDrawable d =
                new GradientDrawable();

        d.setColor(color);
        d.setCornerRadius(radius);

        return d;
    }

    private Button action(String caption) {

        Button b = new Button(this);

        b.setText(caption);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setAllCaps(false);

        b.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);

        b.setBackground(
                bg(ACCENT, 14)
        );

        b.setPadding(
                18,
                2,
                18,
                2
        );

        b.setOnFocusChangeListener(
                (v, hasFocus) ->
                        v.setAlpha(
                                hasFocus ? 1f : 0.82f
                        )
        );

        return b;
    }

    private TextView createKyivClock() {
        TextView clock = label("", 14, LIVE);
        clock.setGravity(Gravity.CENTER);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(16);
        clock.setPadding(12, 6, 12, 6);
        clock.setBackground(bg(Color.argb(150, 0, 0, 0), 10));
        clock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        updateKyivClock(clock);
        return clock;
    }

    private void updateKyivClock(TextView clock) {
        if (clock == null) return;
        SimpleDateFormat format = new SimpleDateFormat("HH.mm", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
        clock.setText(format.format(new Date()));
    }

    private void startKyivClock(TextView clock) {
        kyivClockText = clock;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (kyivClockText != clock || clock.getParent() == null) return;
                updateKyivClock(clock);
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void addKyivClock(LinearLayout root) {
        TextView clock = createKyivClock();
        root.addView(clock, new LinearLayout.LayoutParams(-1, 38));
        startKyivClock(clock);
    }

    private void showLoading() {

        setImmersivePlayback(false);
        playerScreen = false;

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);

        TextView title =
                label(
                        "МАКС Футбол Онлайн",
                        28,
                        TEXT
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        ImageView splashLogo = new ImageView(this);
        splashLogo.setImageResource(com.worldgonemad.footballtv.R.drawable.app_icon);
        splashLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        root.addView(splashLogo, new LinearLayout.LayoutParams(-1, 120));

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );

        TextView loading =
                label(
                        "Загрузка футбольных матчей...",
                        18,
                        MUTED
                );

        loading.setGravity(Gravity.CENTER);

        root.addView(
                loading,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );

        setContentView(root);
    }

    private void showMatches() {

        setImmersivePlayback(false);
        playerScreen = false;

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(BG);

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                42,
                20,
                42,
                12
        );

        TextView logo =
                label(
                        "МАКС Футбол Онлайн",
                        27,
                        TEXT
                );

        logo.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        ImageView headerLogo = new ImageView(this);
        headerLogo.setImageResource(com.worldgonemad.footballtv.R.drawable.app_icon);
        headerLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        header.addView(headerLogo, new LinearLayout.LayoutParams(62, 62));

        header.addView(
                logo,
                new LinearLayout.LayoutParams(
                        0,
                        65,
                        1
                )
        );

        statusText =
                label(
                        matches.size()
                                + " МАТЧЕЙ",
                        14,
                        MUTED
                );

        statusText.setGravity(
                Gravity.CENTER_VERTICAL |
                        Gravity.RIGHT
        );

        header.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        -2,
                        65
                )
        );

        root.addView(header);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setPadding(34, 0, 34, 10);
        tabs.addView(sectionButton("⚽ Все матчи сегодня", SECTION_TODAY),
                new LinearLayout.LayoutParams(0, 54, 1));
        tabs.addView(sectionButton("🔴 Онлайн матчи", SECTION_ONLINE),
                new LinearLayout.LayoutParams(0, 54, 1));
        tabs.addView(sectionButton("★ Избранные матчи", SECTION_FAVORITES),
                new LinearLayout.LayoutParams(0, 54, 1));
        root.addView(tabs);

        sectionTitle = label(sectionTitleText(), 22, TEXT);
        sectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sectionTitle.setPadding(42, 2, 42, 10);
        root.addView(sectionTitle, new LinearLayout.LayoutParams(-1, 46));

        flashscoreScreen = false;
        flashscoreWebView = null;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(34, 0, 34, 30);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        renderMatches();
    }

    private Button sectionButton(String caption, int section) {
        Button button = action(caption);
        button.setTextSize(13);
        button.setPadding(6, 0, 6, 0);
        updateSectionButtonColor(button, section, false);
        button.setOnFocusChangeListener((v, hasFocus) ->
                updateSectionButtonColor(button, section, hasFocus));
        button.setOnClickListener(v -> {
            selectedSection = section;
            showMatches();
        });
        return button;
    }

    private void updateSectionButtonColor(Button button, int section, boolean hasFocus) {
        // Selected stays blue; an unselected button turns yellow when the remote focuses it.
        int color = selectedSection == section ? ACCENT
                : (hasFocus ? Color.rgb(255, 205, 35) : Color.BLACK);
        button.setBackground(bg(color, 12));
        button.setTextColor((hasFocus && selectedSection != section)
                ? Color.BLACK : Color.WHITE);
    }

    private String sectionTitleText() {
        if (selectedSection == SECTION_TODAY) return "ВСЕ МАТЧИ СЕГОДНЯ";
        if (selectedSection == SECTION_FAVORITES) return "ИЗБРАННЫЕ МАТЧИ";
        return "ОНЛАЙН МАТЧИ";
    }

    private ArrayList<Match> getVisibleMatches() {
        ArrayList<Match> visible = new ArrayList<>();
        ArrayList<Match> source = selectedSection == SECTION_TODAY ? todayMatches : matches;
        for (Match match : source) {
            if (selectedSection == SECTION_ONLINE && !match.live) continue;
            if (selectedSection == SECTION_FAVORITES && !favoriteIds.contains(match.eventUrl)) continue;
            visible.add(match);
        }
        java.util.Collections.sort(visible, (a, b) -> {
            int rankA = leaguePriority(a.league);
            int rankB = leaguePriority(b.league);
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            return a.league.compareToIgnoreCase(b.league);
        });
        return visible;
    }

    // Popular championships first, Ukraine next, then other leagues by approximate popularity.
    private int leaguePriority(String league) {
        String x = league == null ? "" : league.toLowerCase(Locale.ROOT);
        if (x.contains("champions league") || x.contains("лига чемпионов") || x.contains("ліга чемпіонів")) return 0;
        if ((x.contains("england") || x.contains("англи") || x.contains("англі")) && (x.contains("premier") || x.contains("премьер") || x.contains("прем'єр"))) return 1;
        if (x.contains("spain") || x.contains("испани") || x.contains("іспан") || x.contains("la liga")) return 2;
        if (x.contains("italy") || x.contains("итал") || x.contains("італ") || x.contains("serie a")) return 3;
        if (x.contains("germany") || x.contains("герман") || x.contains("німеч") || x.contains("bundesliga")) return 4;
        if (x.contains("france") || x.contains("франц") || x.contains("ligue 1")) return 5;
        if (x.contains("europa league") || x.contains("лига европы") || x.contains("ліга європи")) return 6;
        if (x.contains("conference league") || x.contains("лига конференций") || x.contains("ліга конференцій")) return 7;
        if (x.contains("ukraine") || x.contains("украин") || x.contains("україн") || x.contains("упл")) return 8;
        if (x.contains("portugal") || x.contains("португал")) return 9;
        if (x.contains("netherlands") || x.contains("голланд") || x.contains("нидерланд") || x.contains("нідерланд")) return 10;
        if (x.contains("turkey") || x.contains("турц")) return 11;
        if (x.contains("saudi") || x.contains("сауд")) return 12;
        if (x.contains("usa") || x.contains("mls") || x.contains("сша")) return 13;
        if (x.contains("brazil") || x.contains("бразил")) return 14;
        if (x.contains("argentina") || x.contains("аргентин")) return 15;
        if (x.contains("scotland") || x.contains("шотланд")) return 16;
        if (x.contains("belgium") || x.contains("бельг")) return 17;
        if (x.contains("greece") || x.contains("грец")) return 18;
        if (x.contains("poland") || x.contains("польш") || x.contains("польщ")) return 19;
        return 20;
    }

    private void updateFavoriteButtonColor(Button button, boolean isFavorite, boolean hasFocus) {
        // Unselected star: dark; focused unselected star: yellow; saved favorite: blue.
        int color = isFavorite ? ACCENT
                : (hasFocus ? Color.rgb(255, 205, 35) : Color.rgb(48, 51, 58));
        button.setBackground(bg(color, 12));
        button.setTextColor((hasFocus && !isFavorite) ? Color.BLACK : Color.WHITE);
    }

    private void renderMatches() {

        if (listContainer == null) {
            return;
        }

        listContainer.removeAllViews();

        ArrayList<Match> visible = getVisibleMatches();
        if (visible.isEmpty()) {
            String message;
            if (selectedSection == SECTION_FAVORITES) {
                message = "Избранных матчей пока нет. Нажми ☆ рядом с матчем, чтобы сохранить его здесь.";
            } else if (selectedSection == SECTION_ONLINE) {
                message = "Сейчас не найдено матчей в прямом эфире.";
            } else {
                message = "Матчи на сегодня не найдены.";
            }
            TextView empty = label(message, 18, MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(36, 12, 36, 12);
            listContainer.addView(empty, new LinearLayout.LayoutParams(-1, 110));
        } else {
            for (Match match : visible) {
                addMatch(listContainer, match);
            }
        }

        if (sectionTitle != null) sectionTitle.setText(sectionTitleText());
        if (statusText != null) {
            statusText.setText(visible.size() + " МАТЧЕЙ");
        }
    }

    private void addMatch(
            LinearLayout list,
            Match match
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                24,
                14,
                18,
                14
        );

        card.setBackground(
                bg(PANEL, 18)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        -1,
                        110
                );

        cardParams.setMargins(
                8,
                7,
                8,
                7
        );

        list.addView(
                card,
                cardParams
        );

        LinearLayout info =
                new LinearLayout(this);

        info.setOrientation(
                LinearLayout.VERTICAL
        );

        info.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView league =
                label(
                        match.league,
                        12,
                        MUTED
                );

        info.addView(
                league,
                new LinearLayout.LayoutParams(
                        -1,
                        25
                )
        );

        TextView teams =
                label(
                        match.home
                                + "  —  "
                                + match.away,
                        18,
                        TEXT
                );

        teams.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        info.addView(
                teams,
                new LinearLayout.LayoutParams(
                        -1,
                        38
                )
        );

        TextView time =
                label(
                        match.time,                        11,
                        match.live
                                ? LIVE
                                : MUTED
                );

        info.addView(
                time,
                new LinearLayout.LayoutParams(
                        -1,
                        25
                )
        );

        card.addView(
                info,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1
                )
        );

        Button favorite = action(favoriteIds.contains(match.eventUrl) ? "★" : "☆");
        favorite.setTextSize(24);
        favorite.setContentDescription(favoriteIds.contains(match.eventUrl)
                ? "Убрать из избранного" : "Добавить в избранное");
        updateFavoriteButtonColor(favorite, favoriteIds.contains(match.eventUrl), false);
        favorite.setOnFocusChangeListener((v, hasFocus) ->
                updateFavoriteButtonColor(favorite, favoriteIds.contains(match.eventUrl), hasFocus));
        favorite.setOnClickListener(v -> {
            if (favoriteIds.contains(match.eventUrl)) {
                favoriteIds.remove(match.eventUrl);
            } else {
                favoriteIds.add(match.eventUrl);
            }
            preferences.edit().putStringSet("favorite_match_ids", new HashSet<>(favoriteIds)).apply();
            boolean isFavorite = favoriteIds.contains(match.eventUrl);
            favorite.setText(isFavorite ? "★" : "☆");
            favorite.setContentDescription(isFavorite
                    ? "Убрать из избранного" : "Добавить в избранное");
            updateFavoriteButtonColor(favorite, isFavorite, favorite.hasFocus());
            if (selectedSection == SECTION_FAVORITES) renderMatches();
        });
        LinearLayout.LayoutParams favoriteParams = new LinearLayout.LayoutParams(58, 58);
        favoriteParams.setMargins(0, 0, 10, 0);
        card.addView(favorite, favoriteParams);

        // The Today tab is a schedule/favorites list: keep only the star there.
        // Playback controls remain available in the Online and other sections.
        if (selectedSection != SECTION_TODAY) {
            Button watch = action("СМОТРЕТЬ");
            watch.setOnFocusChangeListener((v, hasFocus) -> {
                watch.setBackground(bg(hasFocus ? Color.rgb(255, 205, 35) : ACCENT, 14));
                watch.setTextColor(hasFocus ? Color.BLACK : Color.WHITE);
                watch.setAlpha(1f);
            });
            watch.setOnClickListener(v -> {
                Match streamMatch = findLiveTvMatch(match);
                if (streamMatch != null) resolveAndPlay(streamMatch);
                else showPlayer(match, "Трансляция этого матча не найдена в LiveTV.sx / LiveTV904.");
            });
            card.addView(watch, new LinearLayout.LayoutParams(130, 58));
        }
    }

    /*
     * =========================================================
     * LOAD МАТЧЕЙ
     * =========================================================
     */

    private void loadMatches() {
        new Thread(() -> {
            // Both lists come from the two LiveTV upcoming-football pages supplied by the user.
            String liveTvHtml = downloadPage(PRIMARY_LIVE_TV_URL);
            String liveTv904Html = downloadPage(НАЗАДUP_LIVE_TV_URL);

            // Restore the Online tab's previous LiveTV parsing behaviour.
            ArrayList<Match> liveResults = parseMatches(liveTvHtml, true);
            mergeMatches(liveResults, parseMatches(liveTv904Html, true));
            if (liveResults.size() > 80) {
                liveResults = new ArrayList<>(liveResults.subList(0, 80));
            }

            // The daily schedule uses the two fixture pages plus the LiveTV
            // schedule widget embedded on myfootball.sbs/football. Keep this
            // additional source isolated from the Online tab.
            ArrayList<Match> dailyResults = parseMatches(liveTvHtml, false);
            mergeMatches(dailyResults, parseMatches(liveTv904Html, false));
            String myFootballWidgetHtml = downloadPage(MYFOOTBALL_SCHEDULE_URL);
            mergeMatches(dailyResults, parseMatches(myFootballWidgetHtml, false));

            // "Все матчи сегодня" must include every currently live match
            // as well as fixtures from the upcoming schedule.
            // The Online tab remains based on liveResults only.
            mergeMatches(dailyResults, liveResults);

            if (dailyResults.size() > 200) {
                dailyResults = new ArrayList<>(dailyResults.subList(0, 200));
            }

            if (!liveResults.isEmpty()) {
                saveLiveMatchesCache(liveResults);
            } else {
                liveResults = readLiveMatchesCache();
                // Never put the daily schedule into the Online tab.
            }

            if (!dailyResults.isEmpty()) {
                saveTodayMatches(dailyResults);
            } else {
                // Keep only a previously saved daily schedule when sources fail.
                // Never copy the live list into the daily tab: that made both tabs
                // show the same ~50 events even when the schedule could not load.
                dailyResults = readTodayMatchesCache();
            }

            ArrayList<Match> loadedLive = new ArrayList<>(liveResults);
            ArrayList<Match> loadedDaily = new ArrayList<>(dailyResults);
            runOnUiThread(() -> {
                matches.clear();
                matches.addAll(loadedLive);
                todayMatches.clear();
                todayMatches.addAll(loadedDaily);
                if (!playerScreen) {
                    if (listContainer == null) showMatches();
                    else renderMatches();
                }
            });
        }).start();
    }

    private String downloadFlashscoreFeed() {
        String address = "https://local-ruua.flashscore.ninja/46/x/feed/f_1_0_3_ru-kz_1";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(9000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Language", "ru,uk;q=0.9,en;q=0.8");
            connection.setRequestProperty("Referer", "https://www.flashscore.ua/");
            connection.setRequestProperty("Origin", "https://www.flashscore.ua");
            connection.setRequestProperty("x-fsign", "SW9D1eZo");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return "";
            InputStream input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line).append("\n");
            reader.close();
            return body.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private ArrayList<Match> parseFlashscoreFeed(String feed) {
        ArrayList<Match> result = new ArrayList<>();
        if (feed == null || feed.trim().isEmpty()) return result;

        String currentLeague = "Футбол";
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        dayFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kyiv"));
        String todayKey = dayFormat.format(new Date());
        java.util.Calendar tomorrowCalendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("Europe/Kyiv"));
        tomorrowCalendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
        String tomorrowKey = dayFormat.format(tomorrowCalendar.getTime());
        String[] records = feed.split("~");
        for (String record : records) {
            if (record == null || record.trim().isEmpty()) continue;
            HashMap<String, String> fields = new HashMap<>();
            String[] pairs = record.split("¬");
            for (String pair : pairs) {
                int sep = pair.indexOf('÷');
                if (sep > 0 && sep + 1 <= pair.length()) {
                    fields.put(pair.substring(0, sep).trim(), pair.substring(sep + 1).trim());
                }
            }
            String league = fields.get("ZA");
            if (league != null && !league.trim().isEmpty()) currentLeague = cleanText(league);
            String id = fields.get("AA");
            String home = fields.get("AE");
            String away = fields.get("AF");
            if (id == null || home == null || away == null
                    || home.trim().isEmpty() || away.trim().isEmpty()) continue;

            String homeScore = fields.get("AG");
            String awayScore = fields.get("AH");
            String stage = fields.get("AB");
            String statusCode = stage == null ? "" : stage.trim();
            // Flashscore feed: AB=1 scheduled, AB=2 live, AB=3 finished.
            boolean finished = "3".equals(statusCode);
            boolean live = "2".equals(statusCode);

            String time = "";
            String timestamp = fields.get("AD");
            if (timestamp == null || !timestamp.matches("\\d{9,13}")) continue;
            try {
                long value = Long.parseLong(timestamp);
                if (timestamp.length() <= 10) value *= 1000L;
                Date kickoff = new Date(value);
                // Keep only today's and tomorrow's fixtures in Kyiv time.
                String kickoffDay = dayFormat.format(kickoff);
                if (!todayKey.equals(kickoffDay) && !tomorrowKey.equals(kickoffDay)) continue;
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                timeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kyiv"));
                time = timeFormat.format(kickoff);
                if (tomorrowKey.equals(kickoffDay)) time = "ЗАВТРА · " + time;
            } catch (Exception ignored) {
                continue;
            }
            if (result.size() >= 100) break;
            if (live) {
                String score = validScore(homeScore, awayScore);
                String minute = fields.get("BA");
                String liveMinute = minute != null && minute.matches("\\d{1,3}(?:\\+\\d+)?")
                        ? minute + "'" : "";
                time = "🔴 LIVE" + (liveMinute.isEmpty() ? "" : " · " + liveMinute)
                        + (score.isEmpty() ? "" : " · " + score);
            } else if (finished) {
                String score = validScore(homeScore, awayScore);
                time = "ЗАВЕРШЁН" + (score.isEmpty() ? "" : " · " + score);
            } else if (time.isEmpty()) {
                time = "Сегодня";
            }

            Match match = new Match(currentLeague, cleanText(home), cleanText(away), time, live,
                    "https://www.flashscore.ua/match/" + id + "/");
            result.add(match);
        }
        return result;
    }

    private String validScore(String home, String away) {
        if (home == null || away == null || !home.matches("\\d+") || !away.matches("\\d+")) return "";
        return home + ":" + away;
    }

    private void saveTodayMatches(ArrayList<Match> list) {
        try {
            JSONArray array = new JSONArray();
            for (Match match : list) {
                JSONObject item = new JSONObject();
                item.put("league", match.league);
                item.put("home", match.home);
                item.put("away", match.away);
                item.put("time", match.time);
                item.put("live", match.live);
                item.put("eventUrl", match.eventUrl);
                array.put(item);
            }
            preferences.edit().putString("flashscore_today_cache", array.toString()).apply();
        } catch (Exception ignored) {}
    }

    private ArrayList<Match> readTodayMatchesCache() {
        ArrayList<Match> result = new ArrayList<>();
        if (preferences == null) return result;
        String saved = preferences.getString("flashscore_today_cache", "");
        if (saved == null || saved.isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new Match(item.optString("league", "Футбол"),
                        item.optString("home", ""), item.optString("away", ""),
                        item.optString("time", "Сегодня"), item.optBoolean("live", false),
                        item.optString("eventUrl", "")));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private void saveLiveMatchesCache(ArrayList<Match> list) {
        try {
            JSONArray array = new JSONArray();
            for (Match match : list) {
                JSONObject item = new JSONObject();
                item.put("league", match.league);
                item.put("home", match.home);
                item.put("away", match.away);
                item.put("time", match.time);
                item.put("live", match.live);
                item.put("eventUrl", match.eventUrl);
                array.put(item);
            }
            preferences.edit().putString("livetv_matches_cache", array.toString()).apply();
        } catch (Exception ignored) {}
    }

    private ArrayList<Match> readLiveMatchesCache() {
        ArrayList<Match> result = new ArrayList<>();
        if (preferences == null) return result;
        String saved = preferences.getString("livetv_matches_cache", "");
        if (saved == null || saved.isEmpty()) return result;
        try {
            JSONArray array = new JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                result.add(new Match(item.optString("league", "Футбол"),
                        item.optString("home", ""), item.optString("away", ""),
                        item.optString("time", "Сегодня"), item.optBoolean("live", false),
                        item.optString("eventUrl", "")));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private Match findLiveTvMatch(Match requested) {
        if (requested == null) return null;
        String home = normalizeTeamForMatch(requested.home);
        String away = normalizeTeamForMatch(requested.away);
        for (Match candidate : matches) {
            if (normalizeTeamForMatch(candidate.home).equals(home)
                    && normalizeTeamForMatch(candidate.away).equals(away)) return candidate;
            if (normalizeTeamForMatch(candidate.home).equals(away)
                    && normalizeTeamForMatch(candidate.away).equals(home)) return candidate;
        }
        return null;
    }

    private String normalizeTeamForMatch(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .replace("fc", "")
                .replace("фк", "")
                .replace("афк", "");
    }

    private void mergeMatches(ArrayList<Match> target, ArrayList<Match> incoming) {
        for (Match candidate : incoming) {
            boolean duplicate = false;
            for (Match existing : target) {
                boolean sameUrl = existing.eventUrl != null
                        && existing.eventUrl.equalsIgnoreCase(candidate.eventUrl);
                boolean sameTeams = existing.home != null && existing.away != null
                        && candidate.home != null && candidate.away != null
                        && existing.home.trim().equalsIgnoreCase(candidate.home.trim())
                        && existing.away.trim().equalsIgnoreCase(candidate.away.trim());
                if (sameUrl || sameTeams) {
                    duplicate = true;
                    // Keep the live status if either source identifies the match as live.
                    if (candidate.live && !existing.live) {
                        existing.live = true;
                        existing.time = candidate.time;
                    }
                    break;
                }
            }
            if (!duplicate) target.add(candidate);
        }
    }

    private void loadMatchesViaWebView(String primaryUrl, String backupUrl) {
        if (isFinishing() || isDestroyed()) return;

        if (matchLoaderWebView != null) {
            try { matchLoaderWebView.destroy(); } catch (Exception ignored) {}
        }

        WebView web = new WebView(this);
        matchLoaderWebView = web;

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
        );

        web.setVisibility(View.GONE);
        web.setWebViewClient(new WebViewClient() {
            private boolean done = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (done) return;

                view.evaluateJavascript(
                        "(function(){return document.documentElement.outerHTML;})()",
                        value -> {
                            if (done) return;

                            try {
                                String html = value;
                                if (html != null && html.length() >= 2
                                        && html.startsWith("\"")
                                        && html.endsWith("\"")) {
                                    html = html.substring(1, html.length() - 1)
                                            .replace("\\\"", "\"")
                                            .replace("\\\\", "\\")
                                            .replace("\\n", "\n")
                                            .replace("\\r", "\r");
                                }

                                ArrayList<Match> result = parseMatches(html);

                                if (!result.isEmpty()) {
                                    done = true;
                                    if (matchLoaderWebView == web) {
                                        matchLoaderWebView = null;
                                    }
                                    web.stopLoading();
                                    web.destroy();

                                    matches.clear();
                                    matches.addAll(result);

                                    if (!playerScreen) showMatches();
                                } else {
                                    // Give dynamically inserted event rows one
                                    // extra chance before reporting failure.
                                    handler.postDelayed(() -> {
                                        if (done || matchLoaderWebView != web) return;
                                        view.evaluateJavascript(
                                                "(function(){return document.documentElement.outerHTML;})()",
                                                value2 -> {
                                                    if (done) return;
                                                    String html2 = value2;
                                                    try {
                                                        if (html2 != null && html2.length() >= 2
                                                                && html2.startsWith("\"")
                                                                && html2.endsWith("\"")) {
                                                            html2 = html2.substring(1, html2.length() - 1)
                                                                    .replace("\\\"", "\"")
                                                                    .replace("\\\\", "\\")
                                                                    .replace("\\n", "\n")
                                                                    .replace("\\r", "\r");
                                                        }

                                                        ArrayList<Match> result2 = parseMatches(html2);
                                                        if (!result2.isEmpty()) {
                                                            done = true;
                                                            matchLoaderWebView = null;
                                                            view.stopLoading();
                                                            view.destroy();
                                                            matches.clear();
                                                            matches.addAll(result2);
                                                            if (!playerScreen) showMatches();
                                                        } else if (!playerScreen) {
                                                            done = true;
                                                            matchLoaderWebView = null;
                                                            view.destroy();
                                                            if (primaryUrl.equals(view.getUrl())) {
                                                                try { view.stopLoading(); view.loadUrl(backupUrl); } catch (Exception ignored2) {}
                                                            } else {
                                                                if (primaryUrl.equals(url)) {
                        try { view.loadUrl(backupUrl); } catch (Exception ignored2) {}
                    } else {
                        showError("Не удалось загрузить футбольные матчи.");
                    }
                                                            }
                                                        }
                                                    } catch (Exception ignored) {
                                                        if (!playerScreen) {
                                                            done = true;
                                                            matchLoaderWebView = null;
                                                            view.destroy();
                                                            showError("Не удалось загрузить футбольные матчи.");
                                                        }
                                                    }
                                                }
                                        );
                                    }, 1800);
                                }
                            } catch (Exception ignored) {
                                if (!playerScreen) {
                                    done = true;
                                    matchLoaderWebView = null;
                                    web.destroy();
                                    showError("Не удалось загрузить футбольные матчи.");
                                }
                            }
                        }
                );
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame() && !done && !playerScreen) {
                    done = true;
                    matchLoaderWebView = null;
                    view.destroy();
                    showError("Не удалось загрузить футбольные матчи.");
                }
            }
        });

        web.loadUrl(primaryUrl);
    }


    private String downloadPage(
            String address
    ) {

        HttpURLConnection connection =
                null;

        try {

            URL url =
                    new URL(address);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    8000
            );

            connection.setReadTimeout(
                    10000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
            );

            connection.setRequestProperty(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            );

            connection.setRequestProperty(
                    "Accept-Language",
                    "en-US,en;q=0.9,ru;q=0.8"
            );

            if (address.startsWith("https://livetv904.me/")) {
                connection.setRequestProperty("Referer", "https://livetv904.me/");
            } else if (address.startsWith("https://livetv.sx/")) {
                connection.setRequestProperty("Referer", "https://livetv.sx/");
            }

            int code =
                    connection.getResponseCode();

            if (
                    code < 200 ||
                    code >= 400
            ) {
                return "";
            }

            InputStream input =
                    connection.getInputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    "UTF-8"
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                result.append(line)
                        .append("\n");
            }

            reader.close();

            return result.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return "";

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /*
     * =========================================================
     * PARSER
     * =========================================================
     */

    private ArrayList<Match> parseMatches(String html) {
        return parseMatches(html, false);
    }

    private ArrayList<Match> parseMatches(
            String html,
            boolean usePreviousOnlineDetection
    ) {

        ArrayList<Match> result =
                new ArrayList<>();

        if (html == null || html.isEmpty()) {
            return result;
        }

        // LiveTV changes the exact HTML structure from time to time.
        // Do not depend on a fixed distance between the sport image and
        // the event link. Instead scan all eventinfo links and determine
        // the sport from the nearest preceding Football image.
        Pattern linkPattern =
                Pattern.compile(
                        "(?is)<a\\s+[^>]*href\\s*=\\s*[\\\"']"
                                + "([^\\\"']*?/eventinfo/[^\\\"']+)"
                                + "[\\\"'][^>]*>"
                                + "(.*?)"
                                + "</a>"
                );

        Matcher linkMatcher =
                linkPattern.matcher(html);

        while (linkMatcher.find()) {
            if (result.size() >= 200) break;

            String href = linkMatcher.group(1);
            String anchorText = cleanText(linkMatcher.group(2));

            if (href == null || href.isEmpty() || anchorText.isEmpty()) {
                continue;
            }

            if (!anchorText.contains("–")
                    && !anchorText.contains("—")
                    && !anchorText.contains(" - ")) {
                continue;
            }

            // Use the nearest preceding sport marker. The previous code
        // searched 15,000 characters for any Football image, so a Hockey,
        // Baseball or Tennis event could inherit a distant Football league.
        String prefix =
                html.substring(
                        Math.max(0, linkMatcher.start() - 20000),
                        linkMatcher.start()
                );

        Matcher sportMatcher =
                Pattern.compile(
                        "(?is)<img[^>]+alt\\s*=\\s*[\\\"']([^\\\"']+)[\\\"'][^>]*>"
                ).matcher(prefix);

        // Only the LAST marker belongs to the current event.
        // Looking at every marker in the 20,000-character prefix made a
        // football match disappear when a later hockey/volleyball marker
        // happened to be closer to the link.
        String lastAlt = "";

        while (sportMatcher.find()) {
            lastAlt = cleanText(sportMatcher.group(1));
        }

        String league = "Футбол";
        String lowerLastAlt = lastAlt.toLowerCase(Locale.ROOT);

        // All current callers supply football-only LiveTV schedule pages
        // or the football schedule widget. Do not use the nearest image
        // marker as a gate: that marker can belong to a previous row, and
        // this incorrectly drops scheduled fixtures while live rows survive.
        if (lowerLastAlt.startsWith("football.")
                || lowerLastAlt.equals("football")) {
            league = lastAlt.length() > 9
                    ? cleanText(lastAlt.substring(9))
                    : "Футбол";
        } else if (lowerLastAlt.startsWith("футбол.")
                || lowerLastAlt.equals("футбол")) {
            league = lastAlt.length() > 7
                    ? cleanText(lastAlt.substring(7))
                    : "Футбол";
        }
        if (league == null || league.trim().isEmpty()) {
            league = "Футбол";
        }

        int afterStart = linkMatcher.end();
            // Limit detection to this event row. A 1,500-character window
            // includes later fixtures, whose scores were falsely marking
            // upcoming matches as live.
            int nextEventLink = html.indexOf("/eventinfo/", afterStart);
            int afterEnd = nextEventLink > afterStart
                    ? Math.min(nextEventLink, afterStart + 2000)
                    : Math.min(html.length(), afterStart + 500);
            String after = html.substring(afterStart, afterEnd);
            String eventText = (cleanText(anchorText + " " + after)).toLowerCase(Locale.ROOT);

            Matcher timeMatcher = Pattern.compile("\\b(\\d{1,2}:\\d{2})\\b").matcher(after);
            String time = "";
            if (timeMatcher.find()) {
                time = timeMatcher.group(1);
            }

            // For the Online tab, inspect the whole HTML row because LiveTV's
            // red LIVE badge may appear before the teams link, not after it.
            // Daily schedule parsing remains independent of this live-only test.
            int rowStart = html.lastIndexOf("<tr", linkMatcher.start());
            int rowEnd = html.indexOf("</tr>", linkMatcher.end());
            String rowMarkup = (rowStart >= 0 && rowEnd > linkMatcher.end())
                    ? html.substring(rowStart, Math.min(html.length(), rowEnd + 5))
                    : html.substring(Math.max(0, linkMatcher.start() - 1200),
                            Math.min(html.length(), afterEnd));
            // LiveTV's red LIVE badge is an image (usually a GIF), not reliable
            // visible text. Require an actual GIF image in this event row and
            // identify it by its URL or image attributes. Do not treat a team's
            // name, score, CSS text, or unrelated plain "LIVE" text as proof.
            boolean liveLabel = false;
            Matcher imageMatcher = Pattern.compile("(?is)<img\\b[^>]*>").matcher(rowMarkup);
            while (imageMatcher.find()) {
                String imageTag = imageMatcher.group();
                Matcher srcMatcher = Pattern.compile(
                        "(?i)\\bsrc\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]")
                        .matcher(imageTag);
                if (!srcMatcher.find()) {
                    continue;
                }
                String imageSrc = srcMatcher.group(1).toLowerCase(Locale.ROOT);
                boolean isGif = imageSrc.matches("(?s).*\\.gif(?:[?#].*)?$");
                if (!isGif) {
                    continue;
                }
                boolean liveNamedImage = imageSrc.matches("(?s).*\\blive\\b.*")
                        || Pattern.compile(
                                "(?i)(?:\\balt|\\btitle|\\baria-label|\\bclass)\\s*=\\s*['\\\"][^'\\\"]*\\blive\\b[^'\\\"]*['\\\"]")
                                .matcher(imageTag).find();
                if (liveNamedImage) {
                    liveLabel = true;
                    break;
                }
            }
            // The Online tab is controlled solely by the GIF LIVE badge.
            // Do not infer live status from scores, times, or finished-status text.
            boolean live = usePreviousOnlineDetection && liveLabel;

            if (time.isEmpty()) {
                time = live ? "LIVE" : "UPCOMING";
            }

            String[] teams = splitTeams(anchorText);

            if (teams == null || teams.length != 2) {
                continue;
            }

            String home = cleanTeamName(teams[0]);
            String away = cleanTeamName(teams[1]);

            if (home.isEmpty() || away.isEmpty()) {
                continue;
            }

            String eventUrl = normalizeUrl(href, html);

            boolean duplicate = false;

            for (Match existing : result) {
                if (existing.eventUrl.equals(eventUrl)
                        || (existing.home.equals(home)
                        && existing.away.equals(away)
                        && existing.time.equals(time))) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                result.add(
                        new Match(
                                league,
                                home,
                                away,
                                time,
                                live,
                                eventUrl
                        )
                );
            }
        }

        return result;
    }

    private Match parseMatchFromArea(
            String area,
            String league
    ) {

        Pattern linkPattern =
                Pattern.compile(
                        "(?is)"                                + "<a\\s+[^>]*href\\s*=\\s*[\"']"
                                + "([^\"']+)"
                                + "[\"'][^>]*>"
                                + "(.*?)"
                                + "</a>"
                );

        Matcher linkMatcher =
                linkPattern.matcher(area);

        while (
                linkMatcher.find()
        ) {

            String href =
                    linkMatcher.group(1);

            String anchorText =
                    cleanText(
                            linkMatcher.group(2)
                    );

            if (
                    href == null ||
                    href.isEmpty()
            ) {
                continue;
            }

            if (
                    anchorText.isEmpty()
            ) {
                continue;
            }

            if (
                    !anchorText.contains("–")
                    &&
                    !anchorText.contains("—")
                    &&
                    !anchorText.contains(" - ")
            ) {
                continue;
            }

            String lower =
                    anchorText.toLowerCase();

            if (
                    lower.contains("basketball")
                    ||
                    lower.contains("hockey")
                    ||
                    lower.contains("tennis")
                    ||
                    lower.contains("volleyball")
                    ||
                    lower.contains("handball")
                    ||
                    lower.contains("darts")
            ) {
                continue;
            }

            int timeStart =
                    linkMatcher.end();

            int timeEnd =
                    Math.min(
                            area.length(),
                            timeStart + 1200
                    );

            String after =
                    area.substring(
                            timeStart,
                            timeEnd
                    );

            Pattern timePattern =
                    Pattern.compile(
                            "\\b(\\d{1,2}:\\d{2})\\b"
                    );

            Matcher timeMatcher =
                    timePattern.matcher(after);

            String time = "";

            if (timeMatcher.find()) {
                time =
                        timeMatcher.group(1);
            }

            boolean live =
                    after.matches(
                            "(?is).*?\\b\\d+\\s*:\\s*\\d+\\b.*"
                    );

            if (time.isEmpty()) {

                time =
                        live
                                ? "LIVE"
                                : "UPCOMING";
            }

            String[] teams =
                    splitTeams(
                            anchorText
                    );

            if (
                    teams == null ||
                    teams.length != 2
            ) {
                continue;
            }

            String home =
                    cleanTeamName(
                            teams[0]                    );

            String away =
                    cleanTeamName(
                            teams[1]
                    );

            if (
                    home.isEmpty() ||
                    away.isEmpty()
            ) {
                continue;
            }

            String eventUrl =
                    normalizeUrl(href, area);

            return new Match(
                    league,
                    home,
                    away,
                    time,
                    live,
                    eventUrl
            );
        }

        return null;
    }

    private String[] splitTeams(
            String text
    ) {

        String separator = null;

        if (text.contains("–")) {
            separator = "–";
        } else if (text.contains("—")) {
            separator = "—";
        } else if (text.contains(" - ")) {
            separator = " - ";
        }

        if (separator == null) {
            return null;
        }

        String[] parts =
                text.split(
                        Pattern.quote(separator),
                        2
                );

        if (parts.length != 2) {
            return null;
        }

        return parts;
    }

    private String cleanTeamName(
            String value
    ) {

        value =
                cleanText(value);

        value =
                value.replaceFirst(
                        "\\s+[A-Z]{3}$",
                        ""
                );

        return value.trim();
    }

    private String normalizeUrl(
            String href,
            String pageHtml
    ) {
        if (href == null) return "";

        href = href.trim();
        if (href.startsWith("https://") || href.startsWith("http://")) return href;
        if (href.startsWith("//")) return "https:" + href;

        String host = (pageHtml != null
                && pageHtml.toLowerCase(Locale.US).contains("livetv904.me"))
                ? "https://livetv904.me"
                : "https://livetv.sx";

        if (href.startsWith("/")) return host + href;
        return host + "/" + href;
    }

    private String cleanText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        value =
                value.replaceAll(
                        "(?is)<[^>]+>",
                        " "
                );

        value =
                value.replace(
                        "&nbsp;",
                        " "
                );

        value =
                value.replace(
                        "&amp;",
                        "&"
                );

        value =
                value.replace(
                        "&ndash;",
                        "–"
                );

        value =
                value.replace(
                        "&mdash;",
                        "—"
                );

        value =
                value.replace(
                        "&#8211;",
                        "–"
                );

        value =
                value.replace(
                        "&#8212;",
                        "—"
                );

        value =
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    /*
     * =========================================================
     * MATCH PAGE INSIDE APP
     * =========================================================
     */

    private void resolveAndPlay(Match match) {
        if (match == null || match.eventUrl == null || match.eventUrl.isEmpty()) {
            showError("Ссылка на трансляцию недоступна.");
            return;
        }

        showPlayer(match, "Поиск трансляции...");
        startLiveTvResolver(match);
    }

    private String alternateEventUrl(String eventUrl) {
        if (eventUrl == null || eventUrl.isEmpty()) return "";
        if (eventUrl.contains("livetv.sx")) {
            return eventUrl.replace("https://livetv.sx", "https://livetv904.me");
        }
        if (eventUrl.contains("livetv904.me")) {
            return eventUrl.replace("https://livetv904.me", "https://livetv.sx");
        }
        return "";
    }

    private String webPlayerFromFragment(String eventUrl, String fragment) {
        if (eventUrl == null || fragment == null) return "";
        String f = fragment.trim();
        if (!f.toLowerCase(Locale.US).startsWith("#webplayer_")) return "";

        String[] p = f.substring(1).split("\\|", -1);
        if (p.length < 7) return "";

        String host = eventUrl.contains("livetv904.me")
                ? "https://livetv904.me"
                : "https://livetv.sx";

        String selector = p[0];
        String provider = selector.startsWith("webplayer_")
                ? selector.substring("webplayer_".length())
                : "";

        String c = p[1];
        String eid = p[2];
        String lid = p[3];
        String ci = p[4];
        String si = p[5];
        String lang = p[6];

        if (provider.isEmpty() || c.isEmpty() || eid.isEmpty()
                || lid.isEmpty() || ci.isEmpty() || si.isEmpty()) return "";

        return host + "/webplayer2.php?t=" + provider
                + "&c=" + c + "&lang=" + lang + "&eid=" + eid
                + "&lid=" + lid + "&ci=" + ci + "&si=" + si;
    }

    private void collectWebPlayerUrls(String html, String eventUrl, ArrayList<String> found) {
        if (html == null || html.isEmpty()) return;

        // Exact Browser Link targets, for example:
        // /webplayer.php?t=ifr&c=3083550&lang=ru&eid=453410644&lid=3083550&ci=442&si=1
        Pattern wp = Pattern.compile(
                "(?i)(?:https?:\\/\\/[^\\\"'<>\\s]+|/)?webplayer(?:2)?\\.php\\?[^\\\"'<>\\s]+"
        );
        Matcher wm = wp.matcher(html);
        while (wm.find()) {
            String u = wm.group(0)
                    .replace("\\\\/", "/")
                    .replace("&amp;", "&")
                    .replace("\"", "")
                    .trim();

            if (u.startsWith("/")) {
                String host = eventUrl.contains("livetv904.me")
                        ? "https://livetv904.me"
                        : "https://livetv.sx";
                u = host + u;
            } else if (!u.startsWith("http://") && !u.startsWith("https://")) {
                u = eventUrl.substring(0, eventUrl.indexOf("/", 8)) + "/" + u;
            }

            if (!found.contains(u)) found.add(u);
        }

        // Browser selectors are also useful because they contain all
        // parameters needed to construct the exact webplayer.php URL.
        Pattern frag = Pattern.compile(
                "(?i)#webplayer_[A-Za-z0-9_-]+(?:\\|[A-Za-z0-9._:%+~-]+)*"
        );
        Matcher fm = frag.matcher(html);
        while (fm.find()) {
            String u = webPlayerFromFragment(eventUrl, fm.group(0));
            if (!u.isEmpty() && !found.contains(u)) found.add(u);
        }
    }

    private boolean isLikelyLiveMediaUrl(String url) {
        if (url == null || url.isEmpty()) return false;

        String lower = url.toLowerCase(Locale.US);

        boolean media =
                lower.contains(".m3u8")
                || lower.contains(".mp4")
                || lower.contains("/hls/")
                || lower.contains("manifest");

        if (!media) return false;

        String[] blocked = {
                "doubleclick",
                "googlesyndication",
                "googleadservices",
                "adservice",
                "advert",
                "ads.",
                "/ads/",
                "vast",
                "prebid",
                "analytics",
                "tracking",
                "tracker",
                "pixel"
        };

        for (String token : blocked) {
            if (lower.contains(token)) return false;
        }

        if (lower.contains("/webplayer")
                || lower.contains("/eventinfo/")
                || lower.contains("livetv.sx/")
                || lower.contains("livetv904.me/")) {
            return false;
        }

        return true;
    }

    private void startLiveTvResolver(Match match) {
        releasePlayerOnly();
        playerScreen = true;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(20, 8, 20, 8);

        TextView title = label(match.home + " — " + match.away, 19, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, 56, 1));

        TextView playerClock = createKyivClock();
        top.addView(playerClock, new LinearLayout.LayoutParams(100, 52));
        startKyivClock(playerClock);

        Button back = action("НАЗАД");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));
        root.addView(top);

        TextView status = label("Загрузка ссылок трансляций...", 16, MUTED);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, 55));

        WebView web = new WebView(this);
        resolverWebView = web;
        web.setBackgroundColor(Color.BLACK);
        web.setVisibility(View.GONE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMixedContentMode(
                android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        );
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient());

        final ArrayList<String> sourceCandidates = new ArrayList<>();
        final int[] current = {0};
        final boolean[] resolved = {false};
        final boolean[] fallbackUsed = {false};
        final boolean[] playbackAttemptActive = {false};
        final String[] activePlayerUrl = {""};

        Runnable finishNoStream = () -> {
            if (resolved[0]) return;
            String backup = alternateEventUrl(match.eventUrl);
            if (!fallbackUsed[0] && !backup.isEmpty() && !match.eventUrl.contains("livetv904.me")) {
                fallbackUsed[0] = true;
                status.setText("Проверка резервного источника...");
                match.eventUrl = backup;
                handler.postDelayed(() -> startLiveTvResolver(match), 100);
                return;
            }
            resolved[0] = true;
            status.setText("Рабочая трансляция не найдена.");
        };

        final Runnable[] tryNext = new Runnable[1];
        tryNext[0] = new Runnable() {
            @Override
            public void run() {
                if (resolved[0] || resolverWebView != web) return;
                if (current[0] >= sourceCandidates.size()) {
                    finishNoStream.run();
                    return;
                }

                String candidate = sourceCandidates.get(current[0]++);
                if (candidate == null || candidate.trim().isEmpty()) {
                    run();
                    return;
                }

                String playerUrl = candidate.trim();
                if (playerUrl.toLowerCase(Locale.US).startsWith("#webplayer_")) {
                    playerUrl = webPlayerFromFragment(match.eventUrl, playerUrl);
                }
                if (playerUrl.isEmpty()) {
                    run();
                    return;
                }

                activePlayerUrl[0] = playerUrl;

                // YouTube Browser Links are not ordinary HLS/MP4 sources.
                // The YouTube player uses its own HTML5/MSE pipeline, so
                // Media3 extraction cannot reliably see the actual media URL.
                // For a YouTube selector, open the video directly in the
                // Android TV WebView instead of declaring the stream dead.
                if (playerUrl.toLowerCase(Locale.US).contains("t=youtube")) {
                    Matcher youtubeId = Pattern.compile(
                            "(?i)[?&]c=([^&]+)"
                    ).matcher(playerUrl);
                    if (youtubeId.find()) {
                        String videoId = youtubeId.group(1);
                        String youtubeEmbed =
                                "https://www.youtube-nocookie.com/embed/"
                                + videoId
                                + "?autoplay=1&playsinline=1&rel=0";
                        status.setText("Открытие ссылки трансляции " + current[0] + " из "
                                + sourceCandidates.size() + "...");
                        resolved[0] = true;
                        showLiveTvWebPlayer(match, youtubeEmbed);
                        return;
                    }
                }

                status.setText("Открытие ссылки трансляции " + current[0] + " из "
                        + sourceCandidates.size() + "...");

                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", match.eventUrl);
                headers.put(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
                );
                web.stopLoading();
                web.loadUrl(playerUrl, headers);

                handler.postDelayed(() -> {
                    if (resolved[0] || resolverWebView != web) return;

                    web.evaluateJavascript(
                            "(function(){"
                            + "var a=[];"
                            + "document.querySelectorAll('video,video source,audio source').forEach(function(e){if(e.src)a.push(e.src);});"
                            + "document.querySelectorAll('iframe,frame').forEach(function(e){if(e.src)a.push('FRAME:'+e.src);});"
                            + "try{performance.getEntriesByType('resource').forEach(function(e){if(e.name)a.push(e.name);});}catch(x){}"
                            + "return JSON.stringify(a);"
                            + "})()",
                            value -> {
                                if (resolved[0] || resolverWebView != web || playbackAttemptActive[0]) return;

                                String decoded = value == null ? "" : value;
                                if (decoded.length() >= 2
                                        && decoded.startsWith("\"")
                                        && decoded.endsWith("\"")) {
                                    decoded = decoded.substring(1, decoded.length() - 1)
                                            .replace("\\\"", "\"")
                                            .replace("\\\\", "\\");
                                }

                                ArrayList<String> media = new ArrayList<>();
                                ArrayList<String> frames = new ArrayList<>();

                                Matcher mm = Pattern.compile(
                                        "(?i)https?://[^\\\"'<>\\s]+(?:\\.m3u8(?:\\?[^\\\"'<>\\s]*)?|\\.mp4(?:\\?[^\\\"'<>\\s]*)?)"
                                ).matcher(decoded);
                                while (mm.find()) {
                                    String mediaUrl = mm.group(0);
                                    if (!media.contains(mediaUrl)) media.add(mediaUrl);
                                }

                                Matcher fm = Pattern.compile(
                                        "(?i)FRAME:https?://[^\\\"'<>\\s]+"
                                ).matcher(decoded);
                                while (fm.find()) {
                                    String frameUrl = fm.group(0).substring("FRAME:".length()).trim();
                                    if (!frames.contains(frameUrl)) frames.add(frameUrl);
                                }

                                if (!media.isEmpty() && !playbackAttemptActive[0]) {
                                    playbackAttemptActive[0] = true;
                                    playStreamCandidates(match, media, 0, playbackAttemptActive);
                                } else if (!frames.isEmpty()) {
                                    sourceCandidates.addAll(frames);
                                    tryNext[0].run();
                                } else {
                                    tryNext[0].run();
                                }
                            }
                    );
                }, 10000);
            }
        };

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null && !url.isEmpty()) {
                    String top = activePlayerUrl[0];
                    if (top.isEmpty() || url.equals(top) || !url.contains("livetv904.me/webplayer2.php")) {
                        activePlayerUrl[0] = url;
                    }
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                if (resolved[0] || url == null) return;

                if (isLikelyLiveMediaUrl(url) && !playbackAttemptActive[0]) {
                    playbackAttemptActive[0] = true;
                    ArrayList<String> media = new ArrayList<>();
                    String cookie = CookieManager.getInstance().getCookie(url);

                    String candidate = url
                            + "\\tREFERER=" + activePlayerUrl[0]
                            + "\\tUA=Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36";
                    if (cookie != null && !cookie.isEmpty()) {
                        candidate += "\\tCOOKIE=" + cookie;
                    }

                    String origin = "";
                    try {
                        Uri u = Uri.parse(activePlayerUrl[0]);
                        if (u.getScheme() != null && u.getHost() != null) {
                            origin = u.getScheme() + "://" + u.getHost();
                        }
                    } catch (Exception ignored) {}

                    if (!origin.isEmpty()) {
                        candidate += "\\tORIGIN=" + origin;
                    }

                    media.add(candidate);
                    playStreamCandidates(match, media, 0, playbackAttemptActive);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                if (request != null && request.getUrl() != null) {
                    String mediaUrl = request.getUrl().toString();

                    if (isLikelyLiveMediaUrl(mediaUrl) && !playbackAttemptActive[0]) {
                        Map<String, String> requestHeaders = request.getRequestHeaders();
                        String referer = "";
                        String userAgent = "";
                        String cookie = "";
                        String origin = "";

                        if (requestHeaders != null) {
                            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                                if (entry.getKey() == null || entry.getValue() == null) continue;
                                String key = entry.getKey();
                                String value = entry.getValue();

                                if ("Referer".equalsIgnoreCase(key)) referer = value;
                                else if ("User-Agent".equalsIgnoreCase(key)) userAgent = value;
                                else if ("Cookie".equalsIgnoreCase(key)) cookie = value;
                                else if ("Origin".equalsIgnoreCase(key)) origin = value;
                            }
                        }

                        if (referer.isEmpty()) referer = activePlayerUrl[0];
                        if (userAgent.isEmpty()) {
                            userAgent = "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36";
                        }
                        if (cookie.isEmpty()) {
                            String c = CookieManager.getInstance().getCookie(mediaUrl);
                            if (c != null) cookie = c;
                        }
                        if (origin.isEmpty()) {
                            try {
                                Uri u = Uri.parse(referer);
                                if (u.getScheme() != null && u.getHost() != null) {
                                    origin = u.getScheme() + "://" + u.getHost();
                                }
                            } catch (Exception ignored) {}
                        }

                        final String finalReferer = referer;
                        final String finalUserAgent = userAgent;
                        final String finalCookie = cookie;
                        final String finalOrigin = origin;

                        runOnUiThread(() -> {
                            if (resolved[0] || resolverWebView != web) return;

                            playbackAttemptActive[0] = true;
                            ArrayList<String> media = new ArrayList<>();
                            String candidate = mediaUrl
                                    + "\\tREFERER=" + finalReferer
                                    + "\\tUA=" + finalUserAgent;

                            if (finalCookie != null && !finalCookie.isEmpty()) {
                                candidate += "\\tCOOKIE=" + finalCookie;
                            }
                            if (finalOrigin != null && !finalOrigin.isEmpty()) {
                                candidate += "\\tORIGIN=" + finalOrigin;
                            }

                            media.add(candidate);
                            status.setText("Трансляция найдена. Запуск плеера...");
                            playStreamCandidates(match, media, 0, playbackAttemptActive);
                        });
                    }
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request != null && request.getUrl() != null
                        ? request.getUrl().toString() : "";
                if (u.startsWith("acestream://") || u.startsWith("acestream:")) return true;
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null || url.isEmpty()) return true;
                if (url.startsWith("acestream://") || url.startsWith("acestream:")) return true;
                return false;
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    android.webkit.WebResourceError error
            ) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()
                        && !resolved[0] && !sourceCandidates.isEmpty()) {
                    tryNext[0].run();
                }
            }
        });

        root.addView(web, new LinearLayout.LayoutParams(1, 1));

        back.setOnClickListener(v -> {
            releasePlayer();
            showMatches();
        });
        setContentView(root);

        // Fetch the event HTML directly. We do not wait for WebView
        // onPageFinished before discovering Browser Links.
        new Thread(() -> {
            String html = downloadPage(match.eventUrl);
            ArrayList<String> found = new ArrayList<>();
            collectWebPlayerUrls(html, match.eventUrl, found);

            runOnUiThread(() -> {
                if (resolved[0] || resolverWebView != web) return;

                if (!found.isEmpty()) {
                    startCandidates(found, status, sourceCandidates, current, resolved, tryNext);
                } else {
                    status.setText("Ссылки трансляций не найдены. Проверка резервного источника...");
                    String backup = alternateEventUrl(match.eventUrl);

                    if (!fallbackUsed[0] && !backup.isEmpty()) {
                        fallbackUsed[0] = true;
                        match.eventUrl = backup;

                        new Thread(() -> {
                            String backupHtml = downloadPage(backup);
                            ArrayList<String> backupFound = new ArrayList<>();
                            collectWebPlayerUrls(backupHtml, backup, backupFound);

                            runOnUiThread(() -> {
                                if (resolved[0] || resolverWebView != web) return;
                                if (!backupFound.isEmpty()) {
                                    startCandidates(backupFound, status, sourceCandidates, current, resolved, tryNext);
                                } else {
                                    resolved[0] = true;
                                    status.setText("Ссылки на трансляции не найдены.");
                                }
                            });
                        }).start();
                    } else {
                        resolved[0] = true;
                        status.setText("Ссылки на трансляции не найдены.");
                    }
                }
            });
        }).start();

        // Never leave the user on the loading message for minutes.
        handler.postDelayed(() -> {
            if (!resolved[0] && sourceCandidates.isEmpty()) {
                status.setText("Поиск трансляций занимает слишком много времени...");
            }
        }, 8000);
    }

    private void startCandidates(
            ArrayList<String> found,
            TextView status,
            ArrayList<String> sourceCandidates,
            int[] current,
            boolean[] resolved,
            Runnable[] tryNext
    ) {
        if (resolved[0]) return;

        // Put Ukrainian/Russian Browser Links first, then all other links.
        // If there are no Ukrainian/Russian links, the normal links are used immediately.
        found.sort((a, b) -> Integer.compare(
                browserLinkLanguagePriority(a),
                browserLinkLanguagePriority(b)
        ));

        if (found.size() > 12) {
            found.subList(12, found.size()).clear();
        }

        if (found.isEmpty()) {
            resolved[0] = true;
            status.setText("Ссылки на трансляции не найдены.");
            return;
        }

        sourceCandidates.clear();
        sourceCandidates.addAll(found);
        current[0] = 0;
        status.setText("Найдено " + sourceCandidates.size() + " ссылок. Поиск трансляции...");
        tryNext[0].run();
    }

    private int browserLinkLanguagePriority(String url) {
        if (url == null) return 2;

        String lower = url.toLowerCase(Locale.US);
        String lang = "";

        int q = lower.indexOf("lang=");
        if (q >= 0) {
            int start = q + 5;
            int end = lower.indexOf('&', start);
            if (end < 0) end = lower.length();
            lang = lower.substring(start, end);
        } else if (lower.startsWith("#webplayer_")) {
            String[] parts = lower.substring(1).split("\\|", -1);
            if (parts.length >= 7) lang = parts[6];
        }

        if ("ua".equals(lang) || "uk".equals(lang) || "ukr".equals(lang)) return 0;
        if ("ru".equals(lang) || "rus".equals(lang)) return 1;
        return 2;
    }

    private int indexOfIgnoreCase(String text, String needle) {
        if (text == null || needle == null) return -1;
        return text.toLowerCase(Locale.US).indexOf(
                needle.toLowerCase(Locale.US)
        );
    }

    private void collectWebplayerFragments(String html, ArrayList<String> found) {
        if (html == null) return;

        Pattern p = Pattern.compile(
                "(?i)#webplayer_[A-Za-z0-9_-]+(?:\\|[A-Za-z0-9._:%+~-]+)*"
        );
        Matcher m = p.matcher(html);

        while (m.find()) {
            String u = m.group(0)
                    .replace("&amp;", "&")
                    .trim();
            if (!found.contains(u)) {
                found.add(u);
            }
        }
    }

    private void collectHrefUrls(String html, ArrayList<String> found) {
        if (html == null) return;

        Pattern p = Pattern.compile(
                "(?i)(?:href|data-href|data-url|data-link|data-player)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']"
        );
        Matcher m = p.matcher(html);

        while (m.find()) {
            addBroadcastUrl(m.group(1), found);
        }
    }

    private void collectRawStreamUrls(String html, ArrayList<String> found) {
        if (html == null) return;

        // Do not collect every HTTP URL from the page: most are ordinary
        // LiveTV navigation, HTML pages, images, scripts, etc.
        Pattern p = Pattern.compile(
                "(?i)(?:https?://|acestream://|acestream:)[^\\\"'<>\\s]+"
        );
        Matcher m = p.matcher(html);

        while (m.find()) {
            addBroadcastUrl(m.group(0), found);
        }

        // LiveTV Browser Links can also be encoded as fragments such as:
        // #webplayer_alieztv|232729|452611141|3023558|2088|1|ua
        Pattern fragment = Pattern.compile(
                "(?i)#webplayer_[^\\\"'<>\\s]+"
        );
        Matcher fm = fragment.matcher(html);
        while (fm.find()) {
            String fragmentUrl = fm.group(0).replace("&amp;", "&");
            if (!found.contains(fragmentUrl)) {
                found.add(fragmentUrl);
            }
        }
    }

    private void addBroadcastUrl(String raw, ArrayList<String> found) {
        if (raw == null) return;

        String u = raw.trim()
                .replace("\\\"", "\"")
                .replace("\\", "")
                .replace("&amp;", "&");

        if (u.isEmpty()) return;

        if (u.startsWith("//")) {
            u = "https:" + u;
        } else if (u.startsWith("/")) {
            u = "https://livetv904.me" + u;
        }

        // Skip normal LiveTV navigation and the event page itself.
        if (u.startsWith("https://livetv904.me/")) {
            // Only the explicit #webplayer_* fragment is a broadcast selector.
            // Never treat ordinary LiveTV event/broadcast/player pages as a stream.
            if (!u.contains("#webplayer_")) return;
        }

        // Keep real provider/browser links and direct media URLs.
        if (u.startsWith("http://")
                || u.startsWith("https://")
                || u.startsWith("acestream://")
                || u.startsWith("acestream:")) {
            if (!found.contains(u)) {
                found.add(u);
            }
        }
    }

    private void playStreamCandidates(
            Match match,
            ArrayList<String> streams,
            int index,
            boolean[] playbackAttemptActive
    ) {
        if (streams == null || index >= streams.size()) {
            if (playbackAttemptActive != null) playbackAttemptActive[0] = false;
            showPlayer(match, "Все найденные трансляции недоступны.");
            return;
        }

        String candidate = streams.get(index);
        if (candidate == null || candidate.trim().isEmpty()) {
            playStreamCandidates(match, streams, index + 1, playbackAttemptActive);
            return;
        }
        if (candidate.startsWith("WEBVIEW\\t")) {
            String playerUrl = candidate.substring("WEBVIEW\\t".length()).trim();
            showLiveTvWebPlayer(match, playerUrl);
            return;
        }


        String[] parts = candidate.split("\\t", -1);
        String url = parts.length > 0 ? parts[0].trim() : "";
        if (url.isEmpty()) {
            playStreamCandidates(match, streams, index + 1, playbackAttemptActive);
            return;
        }

        Map<String, String> headers = new HashMap<>();
        for (int p = 1; p < parts.length; p++) {
            int eq = parts[p].indexOf('=');
            if (eq <= 0) continue;
            String key = parts[p].substring(0, eq);
            String value = parts[p].substring(eq + 1);
            if ("UA".equals(key) && !value.isEmpty()) {
                headers.put("User-Agent", value);
            } else if ("COOKIE".equals(key) && !value.isEmpty()) {
                headers.put("Cookie", value);
            } else if ("REFERER".equals(key) && !value.isEmpty()) {
                headers.put("Referer", value);
            } else if ("ORIGIN".equals(key) && !value.isEmpty()) {
                headers.put("Origin", value);
            }
        }

        try {
            Uri mediaUri = Uri.parse(url);
            String currentCookie = CookieManager.getInstance().getCookie(url);
            if (currentCookie != null && !currentCookie.isEmpty()
                    && !headers.containsKey("Cookie")) {
                headers.put("Cookie", currentCookie);
            }
        } catch (Exception ignored) {}

        releasePlayerOnly();
        playerScreen = true;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        playerView = new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setKeepScreenOn(true);
        playerView.setUseController(true);
        playerView.setFocusable(true);

        root.addView(
                playerView,
                new LinearLayout.LayoutParams(-1, -1)
        );

        setContentView(root);
        setImmersivePlayback(true);
        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(
                                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
                        )
                        .setDefaultRequestProperties(headers)
                        .setConnectTimeoutMs(20000)
                        .setReadTimeoutMs(30000)
                        .setAllowCrossProtocolRedirects(true);

        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(
                        new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http)
                )
                .build();

        playerView.setPlayer(player);

        final int nextIndex = index + 1;
        player.addListener(
                new androidx.media3.common.Player.Listener() {
                    @Override
                    public void onPlayerError(
                            androidx.media3.common.PlaybackException error
                    ) {
                        if (player != null) {
                            releasePlayerOnly();
                        }
                        playStreamCandidates(match, streams, nextIndex, playbackAttemptActive);
                    }
                }
        );

        player.setMediaItem(
                new MediaItem.Builder()
                        .setUri(Uri.parse(url))
                        .build()
        );

        player.prepare();
        player.play();
    }

    private void showLiveTvWebPlayer(Match match, String playerUrl) {
        releasePlayer();
        playerScreen = true;
        youtubePlayerScreen = playerUrl != null
                && playerUrl.toLowerCase(Locale.US).contains("youtube-nocookie.com/embed/");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        WebView web = new WebView(this);
        resolverWebView = web;
        web.setBackgroundColor(Color.BLACK);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.setClickable(true);
        web.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) v.setAlpha(1f);
        });

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());

        FrameLayout playerFrame = new FrameLayout(this);
        playerFrame.setBackgroundColor(Color.BLACK);
        playerFrame.addView(web, new FrameLayout.LayoutParams(-1, -1));

        // Keep Kyiv time above the LiveTV904 iframe video while it is playing.
        TextView clock = createKyivClock();
        FrameLayout.LayoutParams clockParams =
                new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        clockParams.setMargins(0, 18, 24, 0);
        clock.setElevation(20f);
        playerFrame.addView(clock, clockParams);
        startKyivClock(clock);

        root.addView(playerFrame, new LinearLayout.LayoutParams(-1, -1));

        setContentView(root);
        setImmersivePlayback(true);

        if (youtubePlayerScreen) {
            web.requestFocus();
        }

        // The ltvplayer is embedded by LiveTV and may check the
        // originating event page. Send the event URL as Referer on the
        // initial player request instead of loading the player as an
        // unrelated top-level page.
        Map<String, String> webHeaders = new HashMap<>();
        webHeaders.put("Referer", match.eventUrl);
        webHeaders.put(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
        );

        web.loadUrl(playerUrl, webHeaders);
    }

    private void showPlayer(Match match, String message) {
        releasePlayer();
        playerScreen = true;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(20, 8, 20, 8);

        TextView title = label(
                match.home + " — " + match.away,
                19,
                TEXT
        );

        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(
                title,
                new LinearLayout.LayoutParams(0, 56, 1)
        );

        Button back = action("НАЗАД");

        top.addView(
                back,
                new LinearLayout.LayoutParams(120, 52)
        );

        root.addView(top);

        TextView status = label(
                message,
                16,
                MUTED
        );

        status.setGravity(Gravity.CENTER);

        root.addView(
                status,
                new LinearLayout.LayoutParams(-1, 55)
        );

        playerView = new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setKeepScreenOn(true);

        FrameLayout videoFrame = new FrameLayout(this);
        videoFrame.setBackgroundColor(Color.BLACK);
        videoFrame.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        TextView clock = createKyivClock();
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        clockParams.setMargins(0, 14, 18, 0);
        videoFrame.addView(clock, clockParams);
        startKyivClock(clock);

        root.addView(videoFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        back.setOnClickListener(v -> {
            releasePlayer();
            showMatches();
        });

        setContentView(root);
    }

    private void playHls(String hls) {
        if (hls == null || hls.isEmpty()) return;

        releasePlayer();
        playerScreen = true;

        playerView = new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(20, 8, 20, 8);

        TextView title = label("МАКС Футбол Онлайн", 19, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(title, new LinearLayout.LayoutParams(0, 56, 1));

        Button back = action("НАЗАД");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));

        root.addView(top);

        FrameLayout videoFrame = new FrameLayout(this);
        videoFrame.setBackgroundColor(Color.BLACK);
        videoFrame.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        TextView clock = createKyivClock();
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        clockParams.setMargins(0, 14, 18, 0);
        videoFrame.addView(clock, clockParams);
        startKyivClock(clock);

        root.addView(videoFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        back.setOnClickListener(v -> {
            releasePlayer();
            showMatches();
        });

        setContentView(root);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem item = new MediaItem.Builder()
                .setUri(Uri.parse(hls))
                .build();

        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (youtubePlayerScreen && resolverWebView != null) {
            try {
                if (resolverWebView.dispatchKeyEvent(event)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return super.dispatchKeyEvent(event);
    }

    private void setImmersivePlayback(boolean enabled) {
        View decor = getWindow().getDecorView();
        if (enabled) {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void releasePlayerOnly() {
        if (playerView != null) {
            try {
                playerView.setPlayer(null);
            } catch (Exception ignored) {}
        }

        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {}

            try {
                player.release();
            } catch (Exception ignored) {}

            player = null;
        }

        playerView = null;
    }

    private void releasePlayer() {
        youtubePlayerScreen = false;
        if (matchLoaderWebView != null) {
            try {
                matchLoaderWebView.stopLoading();
                matchLoaderWebView.destroy();
            } catch (Exception ignored) {}
            matchLoaderWebView = null;
        }

        if (resolverWebView != null) {
            try {
                resolverWebView.stopLoading();
                resolverWebView.destroy();
            } catch (Exception ignored) {}
            resolverWebView = null;
        }

        if (playerView != null) {
            // Detach first, then release the player. This prevents a stale
            // video surface from remaining attached while audio continues.
            try {
                playerView.setPlayer(null);
            } catch (Exception ignored) {}
        }

        if (player != null) {
            try {
                player.stop();
            } catch (Exception ignored) {}

            try {
                player.release();
            } catch (Exception ignored) {}

            player = null;
        }

        playerView = null;
    }

    private void showError(
            String message
    ) {

        setImmersivePlayback(false);
        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER
        );

        root.setBackgroundColor(BG);

        TextView title =
                label(
                        "МАКС Футбол Онлайн",
                        28,
                        TEXT
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );

        TextView error =
                label(
                        message,
                        18,
                        MUTED
                );

        error.setGravity(Gravity.CENTER);

        root.addView(
                error,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );

        Button retry =
                action("ПОВТОРИТЬ");

        retry.setOnClickListener(
                v -> loadMatches()
        );

        root.addView(
                retry,
                new LinearLayout.LayoutParams(
                        180,
                        58
                )
        );

        setContentView(root);
    }

    @Override
    protected void onStop() {

        super.onStop();

        // Android recommends releasing an Activity-owned ExoPlayer from
        // onStop(). Do it only when the player screen is actually active.
        if (playerScreen) {
            releasePlayer();
            playerScreen = false;
        }
    }

    @Override
    protected void onStart() {

        super.onStart();

        // If the Activity was stopped while playing, return to the match list
        // instead of leaving a PlayerView without a live ExoPlayer.
        if (!playerScreen && player == null && !matches.isEmpty()) {
            showMatches();
        }
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );

        releasePlayer();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (flashscoreScreen && flashscoreWebView != null) {
            try { flashscoreWebView.stopLoading(); flashscoreWebView.destroy(); } catch (Exception ignored) {}
            flashscoreWebView = null;
            flashscoreScreen = false;
            selectedSection = SECTION_ONLINE;
            showMatches();
        } else if (selectedSection != SECTION_ONLINE && !playerScreen) {
            selectedSection = SECTION_ONLINE;
            showMatches();
        } else if (player != null || playerView != null || playerScreen) {
            releasePlayer();
            playerScreen = false;
            showMatches();
        } else {
            super.onBackPressed();
        }
    }

    private static class Match {

        String league;
        String home;
        String away;
        String time;
        boolean live;
        String eventUrl;

        Match(
                String league,
                String home,
                String away,
                String time,
                boolean live,
                String eventUrl
        ) {

            this.league = league;
            this.home = home;
            this.away = away;
            this.time = time;
            this.live = live;
            this.eventUrl = eventUrl;
        }
    }
}