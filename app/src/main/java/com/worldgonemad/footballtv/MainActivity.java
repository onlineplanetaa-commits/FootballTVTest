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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private ExoPlayer player;
    private PlayerView playerView;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);
    private final int LIVE = Color.rgb(90, 220, 140);

    private static final String LIVE_TV_URL =
            "https://livetv904.me/enx/allupcoming/";

    /*
     * Тестовый поток.
     *
     * Он нужен только пока мы проверяем встроенный ExoPlayer.
     * Позже здесь будут разрешённые HLS-источники матчей.
     */
    private static final String TEST_HLS =
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";

    private final ArrayList<Match> matches =
            new ArrayList<>();

    private LinearLayout listContainer;
    private TextView statusText;

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

        showLoading();

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
        b.setFocusableInTouchMode(false);

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
                                hasFocus
                                        ? 1f
                                        : 0.82f
                        )
        );

        return b;
    }

    private void showLoading() {

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
                        "MAX FOOTBALL ONLINE",
                        28,
                        TEXT
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );

        TextView loading =
                label(
                        "Loading football matches...",
                        18,
                        MUTED
                );

        loading.setGravity(
                Gravity.CENTER
        );

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

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(BG);

        /*
         * HEADER
         */

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
                        "MAX FOOTBALL ONLINE",
                        27,
                        TEXT
                );

        logo.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

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
                        "UPDATING...",
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

        /*
         * TITLE
         */

        TextView title =
                label(
                        "LIVE & UPCOMING",
                        24,
                        TEXT
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setPadding(
                42,
                4,
                42,
                12
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        55
                )
        );

        /*
         * MATCH LIST
         */

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        scroll.setPadding(
                34,
                0,
                34,
                30
        );

        listContainer =
                new LinearLayout(this);

        listContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        scroll.addView(
                listContainer
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);

        renderMatches();
    }

    private void renderMatches() {

        if (listContainer == null) {
            return;
        }

        listContainer.removeAllViews();

        if (matches.isEmpty()) {

            TextView empty =
                    label(
                            "No football matches found.",
                            18,
                            MUTED
                    );

            empty.setGravity(
                    Gravity.CENTER
            );

            listContainer.addView(
                    empty,
                    new LinearLayout.LayoutParams(
                            -1,
                            100
                    )
            );

            return;
        }

        for (Match match : matches) {

            addMatch(
                    listContainer,
                    match
            );
        }

        if (statusText != null) {

            statusText.setText(
                    matches.size()
                            + " MATCHES"
            );
        }
    }

    private void addMatch(
            LinearLayout list,
            Match match
    ) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
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

        /*
         * INFORMATION
         */

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
                        match.time,
                        11,
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

        /*
         * WATCH
         */

        Button watch =
                action("WATCH");

        watch.setOnClickListener(
                v ->
                        playMatch(
                                match.home
                                        + " — "
                                        + match.away
                        )
        );

        card.addView(
                watch,
                new LinearLayout.LayoutParams(
                        130,
                        58
                )
        );
    }

    /*
     * =========================================================
     * LOAD MATCHES
     * =========================================================
     */

    private void loadMatches() {

        new Thread(
                () -> {

                    String html =
                            downloadPage(
                                    LIVE_TV_URL
                            );

                    ArrayList<Match> result =
                            parseMatches(html);

                    runOnUiThread(
                            () -> {

                                if (!result.isEmpty()) {

                                    matches.clear();

                                    matches.addAll(
                                            result
                                    );

                                    showMatches();

                                } else {

                                    if (matches.isEmpty()) {

                                        showError(
                                                "Could not load football matches."
                                        );

                                    } else {

                                        statusText.setText(
                                                "UPDATE FAILED"
                                        );
                                    }
                                }
                            }
                    );

                }
        ).start();
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
                    15000
            );

            connection.setReadTimeout(
                    20000
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Android TV) AppleWebKit/537.36"
            );

            connection.setRequestProperty(
                    "Accept",
                    "text/html,application/xhtml+xml"
            );

            int code =
                    connection.getResponseCode();

            if (code < 200 || code >= 400) {

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
     * PARSE FOOTBALL MATCHES
     * =========================================================
     */

    private ArrayList<Match> parseMatches(
            String html
    ) {

        ArrayList<Match> result =
                new ArrayList<>();

        if (
                html == null ||
                html.isEmpty()
        ) {

            return result;
        }

        /*
         * Удаляем HTML-теги,
         * чтобы анализировать текст страницы.
         */

        String text =
                html.replaceAll(
                        "(?is)<script.*?</script>",
                        " "
                );

        text =
                text.replaceAll(
                        "(?is)<style.*?</style>",
                        " "
                );

        /*
         * Ищем футбольные блоки.
         *
         * LiveTV904 использует записи,
         * содержащие слово Football.
         */

        Pattern footballPattern =
                Pattern.compile(
                        "(?is)Football\\.?\\s*(.*?)"
                                + "(?=Football\\.?|Ice Hockey|"
                                + "Tennis|Basketball|Volleyball|"
                                + "Handball|Darts|Aussie Rules|"
                                + "Show All)"
                );

        Matcher matcher =
                footballPattern.matcher(
                        text
                );

        while (
                matcher.find()
        ) {

            String block =
                    cleanText(
                            matcher.group(1)
                    );

            if (
                    block.length() < 5
            ) {
                continue;
            }

            Match match =
                    parseFootballBlock(
                            block
                    );

            if (
                    match != null
            ) {

                boolean duplicate =
                        false;

                for (
                        Match existing
                        : result
                ) {

                    if (
                            existing.home.equals(
                                    match.home
                            )
                            &&
                            existing.away.equals(
                                    match.away
                            )
                    ) {

                        duplicate =
                                true;

                        break;
                    }
                }

                if (!duplicate) {

                    result.add(match);
                }
            }
        }

        /*
         * Если структура страницы изменилась,
         * попробуем второй более простой вариант.
         */

        if (result.isEmpty()) {

            result =
                    parseSimpleFootballText(
                            html
                    );
        }

        return result;
    }

    private Match parseFootballBlock(
            String block
    ) {

        block =
                cleanText(block);

        /*
         * Ищем:
         *
         * League
         * Team A – Team B
         * 19:00
         */

        Pattern teamsPattern =
                Pattern.compile(
                        "(.+?)\\s+[–—-]\\s+(.+?)"
                                + "(?=\\s+\\d{1,2}:\\d{2}|$)"
                );

        Matcher teamsMatcher =
                teamsPattern.matcher(
                        block
                );

        if (!teamsMatcher.find()) {

            return null;
        }

        String home =
                cleanText(
                        teamsMatcher.group(1)
                );

        String away =
                cleanText(
                        teamsMatcher.group(2)
                );

        if (
                home.length() < 2 ||
                away.length() < 2
        ) {

            return null;
        }

        String time =
                "";

        Pattern timePattern =
                Pattern.compile(
                        "\\b\\d{1,2}:\\d{2}\\b"
                );

        Matcher timeMatcher =
                timePattern.matcher(
                        block
                );

        if (timeMatcher.find()) {

            time =
                    timeMatcher.group();
        }

        /*
         * Определяем лигу.
         */

        String league =
                extractLeague(
                        block
                );

        boolean live =
                block.contains("0:0")
                        || block.contains("1:0")
                        || block.contains("0:1")
                        || block.contains("1:1")
                        || block.contains("2:0")
                        || block.contains("0:2")
                        || block.contains("2:1")
                        || block.contains("1:2");

        if (time.isEmpty()) {

            time =
                    live
                            ? "LIVE"
                            : "UPCOMING";
        }

        return new Match(
                league,
                home,
                away,
                time,
                live
        );
    }

    private String extractLeague(
            String block
    ) {

        /*
         * Берём текст до названий команд.
         * Это не всегда идеально,
         * поэтому дополнительно ограничиваем длину.
         */

        String league =
                "";

        String[] parts =
                block.split(
                        "\\s+[–—-]\\s+"
                );

        if (
                parts.length > 0
        ) {

            String first =
                    parts[0].trim();

            String[] words =
                    first.split(
                            "\\s+"
                    );

            if (
                    words.length > 2
            ) {

                league =
                        first.substring(
                                0,
                                Math.min(
                                        first.length(),
                                        70
                                )
                        );
            }
        }

        if (
                league.isEmpty()
        ) {

            league =
                    "Football";
        }

        return league;
    }

    private ArrayList<Match>
    parseSimpleFootballText(
            String html
    ) {

        ArrayList<Match> result =
                new ArrayList<>();

        String text =
                cleanText(
                        html
                );

        Pattern pattern =
                Pattern.compile(
                        "Football\\.?\\s+(.{3,100}?)"
                                + "\\s+[–—-]\\s+"
                                + "(.{2,80}?)"
                                + "(?=\\s+\\d{1,2}:\\d{2})"
                );

        Matcher matcher =
                pattern.matcher(text);

        while (
                matcher.find()
        ) {

            String home =
                    cleanText(
                            matcher.group(1)
                    );

            String away =
                    cleanText(
                            matcher.group(2)
                    );

            if (
                    home.length() > 1 &&
                    away.length() > 1
            ) {

                result.add(
                        new Match(
                                "Football",
                                home,
                                away,
                                "UPCOMING",
                                false
                        )
                );
            }
        }

        return result;
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
                value.replaceAll(
                        "\\s+",
                        " "
                );

        return value.trim();
    }

    /*
     * =========================================================
     * PLAYER
     * =========================================================
     */

    private void playMatch(
            String matchName
    ) {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.BLACK
        );

        LinearLayout top =
                new LinearLayout(this);

        top.setOrientation(
                LinearLayout.HORIZONTAL
        );

        top.setGravity(
                Gravity.CENTER_VERTICAL
        );

        top.setPadding(
                28,
                8,
                20,
                8
        );

        TextView title =
                label(
                        matchName,
                        20,
                        TEXT
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        top.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        56,
                        1
                )
        );

        Button back =
                action("BACK");

        top.addView(
                back,
                new LinearLayout.LayoutParams(
                        120,
                        52
                )
        );

        root.addView(top);

        playerView =
                new PlayerView(this);

        playerView.setUseController(
                true
        );

        root.addView(
                playerView,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        back.setOnClickListener(
                v -> {

                    releasePlayer();

                    showMatches();
                }
        );

        setContentView(root);

        player =
                new ExoPlayer.Builder(this)
                        .build();

        playerView.setPlayer(
                player
        );

        MediaItem mediaItem =
                MediaItem.fromUri(
                        TEST_HLS
                );

        player.setMediaItem(
                mediaItem
        );

        player.prepare();

        player.play();
    }

    private void showError(
            String message
    ) {

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
                        "MAX FOOTBALL ONLINE",
                        28,
                        TEXT
                );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

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

        error.setGravity(
                Gravity.CENTER
        );

        root.addView(
                error,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );

        Button retry =
                action("RETRY");

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

    private void releasePlayer() {

        if (player != null) {

            player.release();

            player = null;
        }
    }

    @Override
    protected void onStop() {

        super.onStop();

        if (isFinishing()) {

            releasePlayer();
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

        if (player != null) {

            releasePlayer();

            showMatches();

        } else {

            super.onBackPressed();
        }
    }

    /*
     * =========================================================
     * MATCH DATA CLASS
     * =========================================================
     */

    private static class Match {

        String league;
        String home;
        String away;
        String time;
        boolean live;

        Match(
                String league,
                String home,
                String away,
                String time,
                boolean live
        ) {

            this.league = league;
            this.home = home;
            this.away = away;
            this.time = time;
            this.live = live;
        }
    }
}
