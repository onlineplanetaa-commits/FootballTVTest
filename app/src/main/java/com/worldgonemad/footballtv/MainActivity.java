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
import android.widget.ScrollView;
import android.widget.TextView;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
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
import java.util.Set;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {

    private ExoPlayer player;
    private PlayerView playerView;
    private WebView resolverWebView;

    // True while the Activity is showing the player/resolver screen.
    // Match refreshes must never replace that screen.
    private boolean playerScreen = false;

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

    private void showLoading() {

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
                        "MAX FOOTBALL ONLINE",
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

        TextView loading =
                label(
                        "Loading football matches...",
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
                        matches.size()
                                + " MATCHES",
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

        scroll.addView(listContainer);

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

            empty.setGravity(Gravity.CENTER);

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

        Button watch =
                action("WATCH");

        watch.setOnClickListener(
                v ->
                        resolveAndPlay(
                                match
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

                                    // Refresh data in the background, but never
                                    // replace the player/resolver Activity view.
                                    if (!playerScreen) {
                                        showMatches();
                                    }

                                } else {

                                    if (matches.isEmpty()) {

                                        showError(
                                                "Could not load football matches."
                                        );

                                    } else if (
                                            statusText != null
                                    ) {

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
                connection.setRequestProperty(
                        "Referer",
                        "https://livetv904.me/"
                );
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

    private ArrayList<Match> parseMatches(
            String html
    ) {

        ArrayList<Match> result =
                new ArrayList<>();

        if (html == null || html.isEmpty()) {
            return result;
        }

        // LiveTV904 changes the exact HTML structure from time to time.
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

        String sport = "";
        String league = "";

        while (sportMatcher.find()) {
            String alt = cleanText(sportMatcher.group(1));
            String lowerAlt = alt.toLowerCase();

            if (lowerAlt.startsWith("football.")
                    || lowerAlt.equals("football")) {
                sport = "football";
                league = alt.length() > 9
                        ? cleanText(alt.substring(9))
                        : "Football";
            } else if (lowerAlt.startsWith("hockey")
                    || lowerAlt.startsWith("basketball")
                    || lowerAlt.startsWith("baseball")
                    || lowerAlt.startsWith("tennis")
                    || lowerAlt.startsWith("volleyball")
                    || lowerAlt.startsWith("handball")
                    || lowerAlt.startsWith("darts")
                    || lowerAlt.startsWith("rugby")
                    || lowerAlt.startsWith("formula")
                    || lowerAlt.startsWith("motorsport")) {
                sport = "other";
                league = "";
            }
        }

        if (!"football".equals(sport) || league.isEmpty()) {
            continue;
        }

        int afterStart = linkMatcher.end();
            int afterEnd =
                    Math.min(
                            html.length(),
                            afterStart + 500
                    );

            String after =
                    html.substring(afterStart, afterEnd);

            // The first HH:mm-looking value can be the score (for example
            // 1:0 or 2:1). The scheduled start time is the next HH:mm value.
            Matcher timeMatcher =
                    Pattern.compile(
                            "\\b(\\d{1,2}:\\d{2})\\b"
                    ).matcher(after);

            String firstTime = "";
            String scheduledTime = "";
            int timeCount = 0;

            while (timeMatcher.find() && timeCount < 2) {
                if (timeCount == 0) {
                    firstTime = timeMatcher.group(1);
                } else {
                    scheduledTime = timeMatcher.group(1);
                }
                timeCount++;
            }

            if (scheduledTime.isEmpty()) {
                scheduledTime = firstTime;
            }

            // LiveTV904 has a dedicated "Top Events LIVE" block. Treat
            // matches in that block as live. Everything else must be either
            // scheduled for now/future or it is removed from our list.
            int topLivePos = prefix.lastIndexOf("Top Events LIVE");
            int todayPos = prefix.lastIndexOf("Today (");
            boolean inTopLive = topLivePos > todayPos;

            String pageTime = extractPageCurrentTime(html);
            String pageDate = extractPageCurrentDate(html);
            String eventDate = extractLastEventDate(prefix);

            if (!inTopLive
                    && !isCurrentOrUpcomingEvent(
                            eventDate,
                            scheduledTime,
                            pageDate,
                            pageTime
                    )) {
                continue;
            }

            boolean live = inTopLive;

            String time =
                    scheduledTime.isEmpty()
                            ? (live ? "LIVE" : "UPCOMING")
                            : scheduledTime;

            String[] teams = splitTeams(anchorText);

            if (teams == null || teams.length != 2) {
                continue;
            }

            String home = cleanTeamName(teams[0]);
            String away = cleanTeamName(teams[1]);

            if (home.isEmpty() || away.isEmpty()) {
                continue;
            }

            String eventUrl = normalizeUrl(href);

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

    private String extractPageCurrentTime(String html) {
        Matcher m =
                Pattern.compile(
                        "(?is)Your current time zone:\\s*(\\d{1,2}:\\d{2})"
                ).matcher(html);
        return m.find() ? m.group(1) : "";
    }

    private String extractPageCurrentDate(String html) {
        Matcher m =
                Pattern.compile(
                        "(?is)Today\\s*\\(\\s*(\\d{1,2})\\s+([A-Za-z]+)"
                ).matcher(html);
        return m.find() ? m.group(1) + " " + m.group(2) : "";
    }

    private String extractLastEventDate(String prefix) {
        Matcher m =
                Pattern.compile(
                        "(?i)\\b(\\d{1,2})\\s+"
                                + "(January|February|March|April|May|June|July|August|"
                                + "September|October|November|December)\\b"
                ).matcher(prefix);
        String result = "";
        while (m.find()) {
            result = m.group(1) + " " + m.group(2);
        }
        return result;
    }

    private boolean isCurrentOrUpcomingEvent(
            String eventDate,
            String scheduledTime,
            String pageDate,
            String pageTime
    ) {
        if (scheduledTime == null || scheduledTime.isEmpty()) {
            return true;
        }

        int currentDay = extractDay(pageDate);
        int currentMonth = extractMonth(pageDate);
        int eventDay = extractDay(eventDate.isEmpty() ? pageDate : eventDate);
        int eventMonth = extractMonth(eventDate.isEmpty() ? pageDate : eventDate);

        if (currentDay <= 0 || currentMonth <= 0
                || eventDay <= 0 || eventMonth <= 0) {
            return true;
        }

        if (eventMonth != currentMonth) {
            return eventMonth > currentMonth;
        }

        if (eventDay != currentDay) {
            return eventDay > currentDay;
        }

        int eventMinutes = parseClockMinutes(scheduledTime);
        int currentMinutes = parseClockMinutes(pageTime);

        if (eventMinutes < 0 || currentMinutes < 0) {
            return true;
        }

        return eventMinutes >= currentMinutes;
    }

    private int extractDay(String value) {
        if (value == null || value.isEmpty()) return -1;
        Matcher m = Pattern.compile("\\b(\\d{1,2})\\b").matcher(value);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private int extractMonth(String value) {
        if (value == null || value.isEmpty()) return -1;

        Matcher m =
                Pattern.compile(
                        "(?i)\\b(January|February|March|April|May|June|July|"
                                + "August|September|October|November|December)\\b"
                ).matcher(value);

        if (!m.find()) return -1;

        String month = m.group(1).toLowerCase();
        String[] months = {
                "january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december"
        };

        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(month)) return i + 1;
        }
        return -1;
    }

    private int parseClockMinutes(String value) {
        if (value == null || value.isEmpty()) return -1;

        Matcher m =
                Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b")
                        .matcher(value);

        if (!m.find()) return -1;

        int hour = Integer.parseInt(m.group(1));
        int minute = Integer.parseInt(m.group(2));

        if (hour > 23 || minute > 59) return -1;

        return hour * 60 + minute;
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
                            teams[0]
                    );

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
                    normalizeUrl(href);

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
            String href
    ) {

        if (href == null) {
            return "";
        }

        href = href.trim();

        if (
                href.startsWith(
                        "https://"
                )
        ) {
            return href;
        }

        if (
                href.startsWith(
                        "http://"
                )
        ) {
            return href;
        }

        if (
                href.startsWith("//")
        ) {
            return "https:" + href;
        }

        if (
                href.startsWith("/")
        ) {
            return "https://livetv904.me" + href;
        }

        return "https://livetv904.me/" + href;
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
            showError("Match page is unavailable.");
            return;
        }

        // Use the JavaScript-capable browser path first. The old blocking
        // HTTP crawler could spend almost a minute on static pages.
        showPlayer(match, "Finding video stream...");

        runOnUiThread(() -> resolveWithWebView(match));
    }

    private String resolvePublicHls(String eventUrl) {
        String html = downloadPage(eventUrl);
        if (html == null || html.isEmpty()) return "";

        String hls = findHls(html);
        if (!hls.isEmpty()) return hls;

        ArrayList<String> candidates = findCandidatePages(html, eventUrl);

        // LiveTV904 also exposes the stream links through the
        // "player/links" page. The link is often generated by JavaScript
        // and therefore is not necessarily present in href/src attributes.
        addPlayerLinksCandidates(candidates, html, eventUrl);

        // Some pages contain the webplayer URL only as a quoted JavaScript
        // string. Scan the whole HTML for those URLs as a fallback.
        addRawUrlCandidates(candidates, html, eventUrl);

        Set<String> visited = new HashSet<>();

        for (String page : candidates) {
            if (!visited.add(page)) continue;

            String pageHtml = downloadPage(page);
            if (pageHtml.isEmpty()) continue;

            hls = findHls(pageHtml);
            if (!hls.isEmpty()) return hls;

            ArrayList<String> nested = findCandidatePages(pageHtml, page);
            addRawUrlCandidates(nested, pageHtml, page);

            for (String child : nested) {
                if (!visited.add(child)) continue;

                String childHtml = downloadPage(child);
                if (childHtml.isEmpty()) continue;

                hls = findHls(childHtml);
                if (!hls.isEmpty()) return hls;

                // One additional level catches:
                // event -> player/links -> webplayer2 -> embed -> HLS
                ArrayList<String> deep = findCandidatePages(childHtml, child);
                addRawUrlCandidates(deep, childHtml, child);

                for (String third : deep) {
                    if (!visited.add(third)) continue;

                    String thirdHtml = downloadPage(third);
                    if (thirdHtml.isEmpty()) continue;

                    hls = findHls(thirdHtml);
                    if (!hls.isEmpty()) return hls;
                }
            }
        }

        return "";
    }

    private void addPlayerLinksCandidates(
            ArrayList<String> candidates,
            String html,
            String eventUrl
    ) {
        String eid = extractFirst(html,
                "(?i)(?:[?&]eid=|\\\\\"eid\\\\\"\\\\s*[:=]\\\\s*\\\\\"?)(\\\\d+)"
        );

        if (eid.isEmpty()) {
            eid = extractFirst(eventUrl,
                    "(?i)eventinfo/([0-9]+)"
            );        }

        String t = extractFirst(html,
                "(?i)(?:[?&]t=|\\\\\"t\\\\\"\\\\s*[:=]\\\\s*\\\\\"?)(\\\\d+)"
        );

        if (!eid.isEmpty()) {
            // The mobile links page used by LiveTV904/livetv.sx.
            // If t is available, keep it because it selects the current source.
            if (!t.isEmpty()) {
                addCandidate(candidates,
                        "https://livetv904.me/player/links/ru/"
                                + eid + "?mob=1&t=" + t);
                addCandidate(candidates,
                        "https://livetv904.me/player/links/en/"
                                + eid + "?mob=1&t=" + t);
            }

            // Also try the page without t. Some events expose the source
            // list directly and do not require the extra parameter.
            addCandidate(candidates,
                    "https://livetv904.me/player/links/ru/"
                            + eid + "?mob=1");
            addCandidate(candidates,
                    "https://livetv904.me/player/links/en/"
                            + eid + "?mob=1");
        }
    }

    private void addRawUrlCandidates(
            ArrayList<String> candidates,
            String html,
            String baseUrl
    ) {
        if (html == null || html.isEmpty()) return;

        // LiveTV904 frequently puts player URLs inside JavaScript/onClick
        // strings rather than normal href/src attributes. Extract both
        // absolute and relative quoted URLs.
        Pattern p = Pattern.compile(
                "(?is)(?:https?:\\/\\/|//|/|\\b)([^\\\"'<>\\s]+)"
        );

        Matcher m = p.matcher(html);

        while (m.find()) {
            String raw = m.group(0);
            if (raw == null || raw.isEmpty()) continue;

            String url = decodeUrl(raw);

            // Ignore ordinary text. Keep only strings that look like one
            // of LiveTV904's player/source endpoints.
            String lower = url.toLowerCase();

            if (!lower.contains("webplayer")
                    && !lower.contains("/player/")
                    && !lower.contains("/export/")
                    && !lower.contains("iframe")
                    && !lower.contains("apl614")
                    && !lower.contains("azplay")) {
                continue;
            }

            String absolute = absoluteUrl(url, baseUrl);

            if (!absolute.isEmpty()) {
                addCandidate(candidates, absolute);
            }
        }

        // Also catch protocol-relative URLs even when the generic expression
        // above starts in the middle of a JavaScript string.
        Pattern quoted = Pattern.compile(
                "(?is)[\\\"']([^\\\"']*(?:webplayer|/player/|/export/|apl614|azplay)[^\\\"']*)[\\\"']"
        );

        Matcher qm = quoted.matcher(html);

        while (qm.find()) {
            String url = decodeUrl(qm.group(1));
            if (url.isEmpty()) continue;

            String absolute = absoluteUrl(url, baseUrl);

            if (!absolute.isEmpty()) {
                addCandidate(candidates, absolute);
            }
        }
    }

    private void addCandidate(
            ArrayList<String> candidates,
            String url
    ) {
        if (url == null || url.isEmpty()) return;

        url = decodeUrl(url);

        if ((url.startsWith("https://") || url.startsWith("http://"))
                && !candidates.contains(url)) {
            candidates.add(url);
        }
    }

    private String extractFirst(
            String text,
            String regex
    ) {
        if (text == null || text.isEmpty()) return "";

        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? decodeUrl(m.group(1)) : "";
    }

    private String findHls(String html) {
        if (html == null || html.isEmpty()) return "";

        // Normal absolute URL.
        Pattern p = Pattern.compile(
                "(?i)(https?://[^\\\"'\\s<>]+\\.m3u8(?:\\?[^\\\"'\\s<>]*)?)"
        );

        Matcher m = p.matcher(html);
        while (m.find()) {
            String url = decodeUrl(m.group(1));
            if (url.startsWith("https://") || url.startsWith("http://")) {
                return url;
            }
        }

        // JavaScript frequently escapes slashes as \/.
        Pattern escaped = Pattern.compile(
                "(?i)(https?:\\\\/\\\\/[^\\\"'\\s<>]+\\.m3u8(?:\\?[^\\\"'\\s<>]*)?)"
        );

        Matcher em = escaped.matcher(html);
        while (em.find()) {
            String url = decodeUrl(em.group(1));
            if (url.startsWith("https://") || url.startsWith("http://")) {
                return url;
            }
        }

        return "";
    }

    private ArrayList<String> findCandidatePages(String html, String baseUrl) {
        ArrayList<String> result = new ArrayList<>();
        if (html == null || html.isEmpty()) return result;

        Pattern p = Pattern.compile(
                "(?is)(?:href|src|data-url|data-href|onclick)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']"
        );

        Matcher m = p.matcher(html);

        while (m.find()) {
            String href = decodeUrl(m.group(1));

            String lower = href.toLowerCase();

            if (!lower.contains("webplayer")
                    && !lower.contains("/player/")
                    && !lower.contains("/export/")
                    && !lower.contains("iframe")
                    && !lower.contains("apl614")
                    && !lower.contains("azplay")) {
                continue;
            }

            String absolute = absoluteUrl(href, baseUrl);

            if (!absolute.isEmpty()
                    && (absolute.startsWith("https://")
                    || absolute.startsWith("http://"))) {
                if (!result.contains(absolute)) result.add(absolute);
            }
        }

        // Some source links are plain quoted JavaScript strings.
        Pattern quoted = Pattern.compile(
                "(?is)[\\\"']([^\\\"']*(?:webplayer|/player/|/export/|apl614|azplay)[^\\\"']*)[\\\"']"
        );

        Matcher qm = quoted.matcher(html);

        while (qm.find()) {
            String href = decodeUrl(qm.group(1));
            String absolute = absoluteUrl(href, baseUrl);

            if (!absolute.isEmpty()
                    && (absolute.startsWith("https://")
                    || absolute.startsWith("http://"))
                    && !result.contains(absolute)) {
                result.add(absolute);
            }
        }

        return result;
    }

    private String decodeUrl(String value) {
        if (value == null) return "";

        String result = value.trim();

        result = result.replace("&amp;", "&");
        result = result.replace("\\/", "/");
        result = result.replace("\\\"", "\"");
        result = result.replace("&#x3D;", "=");
        result = result.replace("&#61;", "=");

        return result;
    }

    private String absoluteUrl(String href, String baseUrl) {
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, href);
            return resolved.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void resolveWithWebView(Match match) {
        if (match == null || match.eventUrl == null || match.eventUrl.isEmpty()) {
            showPlayer(match, "Match page is unavailable.");
            return;
        }

        if (resolverWebView != null) {
            try {
                resolverWebView.stopLoading();
                resolverWebView.destroy();
            } catch (Exception ignored) {}
            resolverWebView = null;
        }

        WebView web = new WebView(this);
        resolverWebView = web;

        // Use a real WebView session because LiveTV904 builds some source
        // links with JavaScript. We only observe public browser requests;
        // we do not bypass login, DRM, or access controls.
        web.setVisibility(View.VISIBLE);
        web.setAlpha(0.01f);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        );

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        final String eventUrl = match.eventUrl;
        final String eid = extractFirst(
                eventUrl,
                "(?i)eventinfo/([0-9]+)"
        );

        final ArrayList<String> pages = new ArrayList<>();
        pages.add(eventUrl);

        if (!eid.isEmpty()) {
            pages.add(
                    "https://livetv904.me/player/links/ru/"
                            + eid + "?mob=1"
            );
            pages.add(
                    "https://livetv904.me/player/links/en/"
                            + eid + "?mob=1"
            );
        }

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    android.os.Message resultMsg
            ) {
                // Many source buttons use target=_blank/window.open().
                // Reuse the same resolver WebView instead of creating a
                // separate window that we could not observe.
                WebView.HitTestResult hit = view.getHitTestResult();
                if (hit != null && hit.getExtra() != null) {
                    String u = hit.getExtra();
                    if (u.startsWith("http://")
                            || u.startsWith("https://")) {
                        view.loadUrl(u);
                        return true;
                    }
                }

                view.evaluateJavascript(
                        "(function(){window.open=function(u){if(u)location.href=u;};return true;})()",
                        null
                );
                return false;
            }
        });

        web.setWebViewClient(new WebViewClient() {
            private int pageIndex = 0;
            private boolean finished = false;
            private final Set<String> loadedPages = new HashSet<>();

            private boolean isHls(String url) {
                return url != null
                        && url.toLowerCase().contains(".m3u8");
            }

            private void found(String url) {
                if (!isHls(url) || finished) return;

                finished = true;

                runOnUiThread(() -> {
                    if (resolverWebView != web) return;

                    try {
                        web.stopLoading();
                        web.destroy();
                    } catch (Exception ignored) {}

                    resolverWebView = null;

                    playHls(url);
                });
            }

            private void inspectPage(WebView view, String currentUrl) {
                if (finished) return;

                // Important: do not only look at href/src. LiveTV904 source
                // buttons can use onclick, data-url, JavaScript navigation,
                // target=_blank, or a generated player link.
                String js =
                        "(function(){" +
                        "try{" +
                        "var e=performance.getEntriesByType('resource');" +
                        "for(var i=0;i<e.length;i++){" +
                        "var p=e[i].name||'';" +
                        "if(p.toLowerCase().indexOf('.m3u8')>=0){location.href=p;return;}" +
                        "}" +
                        "window.open=function(u){if(u){location.href=new URL(u,location.href).href;}return null;};" +
                        "var nodes=document.querySelectorAll('a,iframe,source,video,button');" +
                        "for(var j=0;j<nodes.length;j++){" +
                        "var u=nodes[j].href||nodes[j].src||'';" +
                        "var d=nodes[j].getAttribute('data-url')||nodes[j].getAttribute('data-href')||'';" +
                        "var oc=nodes[j].getAttribute('onclick')||'';" +
                        "var all=(u+' '+d+' '+oc).toLowerCase();" +
                        "if(all.indexOf('.m3u8')>=0){" +
                        "var z=(u||d||oc);var q=z.toLowerCase().indexOf('http');" +
                        "if(q>=0){location.href=z.substring(q);return;}" +
                        "}" +
                        "if(all.indexOf('webplayer')>=0||all.indexOf('/player/')>=0||all.indexOf('/export/')>=0||all.indexOf('apl614')>=0||all.indexOf('azplay')>=0){" +
                        "var candidate=u||d||'';" +
                        "if(candidate&&candidate.toLowerCase().indexOf('javascript:')!==0){" +
                        "try{candidate=new URL(candidate,location.href).href;}catch(x){}" +
                        "if(candidate.indexOf('http://')===0||candidate.indexOf('https://')===0){location.href=candidate;return;}" +
                        "}" +
                        "}" +
                        "}" +
                        "var raw=document.documentElement?document.documentElement.innerHTML:'';" +
                        "var low=raw.toLowerCase();" +
                        "var keys=['webplayer','/player/links/','/export/','apl614','azplay'];" +
                        "for(var kk=0;kk<keys.length;kk++){" +
                        "var pos=low.indexOf(keys[kk]);" +
                        "if(pos>=0){" +
                        "var left=pos;" +
                        "while(left>0&&raw.charAt(left)!=='\\\"'&&raw.charAt(left)!=='\\\''&&raw.charAt(left)!=='<')left--;" +
                        "var right=pos;" +
                        "while(right<raw.length&&raw.charAt(right)!=='\\\"'&&raw.charAt(right)!=='\\\''&&raw.charAt(right)!=='>'&&raw.charAt(right)!==' '&&raw.charAt(right)!=='\\n')right++;" +
                        "var candidate=raw.substring(left+1,right);" +
                        "if(candidate.indexOf('http')===0){location.href=candidate;return;}" +
                        "if(candidate.indexOf('/')===0){location.href=new URL(candidate,location.href).href;return;}" +
                        "}" +
                        "}" +
                        "if(location.href.toLowerCase().indexOf('/player/links/')>=0){" +
                        "for(var k=0;k<nodes.length;k++){" +
                        "var txt=(nodes[k].innerText||nodes[k].textContent||'').trim().toLowerCase();" +
                        "if(txt==='aliez'||txt.indexOf('aliez ')===0||txt==='web'||txt.indexOf('web ')===0){" +
                        "try{nodes[k].click();return;}catch(x){}" +
                        "}" +
                        "}" +
                        "}" +
                        "}catch(x){}" +
                        "})()";

                view.evaluateJavascript(js, null);

                if (!finished) {
                    handler.postDelayed(
                            () -> inspectPage(view, currentUrl),
                            600
                    );
                }
            }

            private void loadNextPage() {
                if (finished) return;

                while (pageIndex < pages.size()) {
                    String next = pages.get(pageIndex++);

                    if (loadedPages.add(next)) {
                        web.loadUrl(next);
                        return;
                    }
                }

                // Give the last page a little time for delayed JS/player
                // initialization before declaring failure.
                handler.postDelayed(() -> {
                    if (!finished && resolverWebView == web) {
                        try {
                            web.stopLoading();
                            web.destroy();
                        } catch (Exception ignored) {}

                        resolverWebView = null;

                        showPlayer(
                                match,
                                "No public video stream was found for this match."
                        );
                    }
                }, 8000);
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                String url = request.getUrl().toString();

                // WebView exposes resource requests here, including XHR/fetch
                // resources used by browser video players.
                if (isHls(url)) {
                    found(url);
                }

                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                String url = request.getUrl().toString();

                if (isHls(url)) {
                    found(url);
                    return true;
                }

                return false;
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                super.onPageFinished(view, url);

                if (finished) return;

                inspectPage(view, url);

                // Allow source-list JavaScript to finish, then move to the
                // next known LiveTV904 page if no player was opened.
                handler.postDelayed(() -> {
                    if (!finished && resolverWebView == web) {
                        loadNextPage();
                    }
                }, 5500);
            }
        });

        if (playerView != null
                && playerView.getParent() instanceof LinearLayout) {
            LinearLayout parent =
                    (LinearLayout) playerView.getParent();

            parent.addView(
                    web,
                    new LinearLayout.LayoutParams(2, 2)
            );
        }

        web.loadUrl(pages.get(0));

        // Safety timeout. The resolver should normally finish much earlier.
        handler.postDelayed(() -> {
            if (resolverWebView == web) {
                try {
                    web.stopLoading();
                    web.destroy();
                } catch (Exception ignored) {}

                resolverWebView = null;

                showPlayer(
                        match,
                        "No public video stream was found for this match."
                );
            }
        }, 50000);
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

        Button back = action("BACK");

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

        root.addView(
                playerView,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

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

        TextView title = label("MAX FOOTBALL ONLINE", 19, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(title, new LinearLayout.LayoutParams(0, 56, 1));

        Button back = action("BACK");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));

        root.addView(top);

        root.addView(
                playerView,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

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

    private void releasePlayer() {
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

        if (player != null || playerView != null || playerScreen) {

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
