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

import androidx.media3.datasource.DefaultHttpDataSource;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
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

    private static final String PRIMARY_LIVE_TV_URL =
            "https://livetv.sx/allupcomingsports/1/";

    private static final String BACKUP_LIVE_TV_URL =
            "https://livetv904.me/allupcomingsports/1/";

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
        new Thread(() -> {
            String html = downloadPage(PRIMARY_LIVE_TV_URL);
            ArrayList<Match> result = parseMatches(html);

            if (!result.isEmpty()) {
                runOnUiThread(() -> {
                    matches.clear();
                    matches.addAll(result);
                    if (!playerScreen) showMatches();
                });
                return;
            }

            // Primary LiveTV.sx failed. Try the backup LiveTV source.
            String backupHtml = downloadPage(BACKUP_LIVE_TV_URL);
            ArrayList<Match> backup = parseMatches(backupHtml);

            if (!backup.isEmpty()) {
                runOnUiThread(() -> {
                    matches.clear();
                    matches.addAll(backup);
                    if (!playerScreen) showMatches();
                });
                return;
            }

            // If both reject HttpURLConnection, try the primary in WebView,
            // then the backup in WebView.
            runOnUiThread(() -> loadMatchesViaWebView(PRIMARY_LIVE_TV_URL, BACKUP_LIVE_TV_URL));
        }).start();
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
                        showError("Could not load football matches.");
                    }
                                                            }
                                                        }
                                                    } catch (Exception ignored) {
                                                        if (!playerScreen) {
                                                            done = true;
                                                            matchLoaderWebView = null;
                                                            view.destroy();
                                                            showError("Could not load football matches.");
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
                                    showError("Could not load football matches.");
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
                    showError("Could not load football matches.");
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

    private ArrayList<Match> parseMatches(
            String html
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

        String sport = "";
        String league = "";

        String lowerLastAlt = lastAlt.toLowerCase();

        // LiveTV may return the schedule in Russian even when the
        // app requests the same English-capable domain. Accept both forms.
        if (lowerLastAlt.startsWith("football.")
                || lowerLastAlt.equals("football")) {
            sport = "football";
            league = lastAlt.length() > 9
                    ? cleanText(lastAlt.substring(9))
                    : "Football";
        } else if (lowerLastAlt.startsWith("футбол.")
                || lowerLastAlt.equals("футбол")) {
            sport = "football";
            league = lastAlt.length() > 7
                    ? cleanText(lastAlt.substring(7))
                    : "Football";
        }

        if (!"football".equals(sport) || league.isEmpty()) {
            continue;
        }

        int afterStart = linkMatcher.end();
            int afterEnd =
                    Math.min(
                            html.length(),
                            afterStart + 1500
                    );

            String after =
                    html.substring(afterStart, afterEnd);

            Matcher timeMatcher =
                    Pattern.compile(
                            "\\b(\\d{1,2}:\\d{2})\\b"
                    ).matcher(after);

            String time = "";

            if (timeMatcher.find()) {
                time = timeMatcher.group(1);
            }

            boolean live =
                    Pattern.compile(
                            "(?is).*?\\b\\d+\\s*:\\s*\\d+\\b.*"
                    ).matcher(anchorText).matches()
                    || Pattern.compile(
                            "(?is).*?\\b\\d+\\s*:\\s*\\d+\\b.*"
                    ).matcher(after).matches();

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
            showError("Stream link is unavailable.");
            return;
        }

        showPlayer(match, "Finding LiveTV stream...");
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

        String c = p[1];
        String eid = p[2];
        String lid = p[3];
        String ci = p[4];
        String si = p[5];
        String lang = p[6];

        if (c.isEmpty() || eid.isEmpty() || lid.isEmpty() || ci.isEmpty() || si.isEmpty()) {
            return "";
        }

        return host + "/webplayer.php?t=ifr&c=" + c
                + "&lang=" + lang
                + "&eid=" + eid
                + "&lid=" + lid
                + "&ci=" + ci
                + "&si=" + si;
    }

    private void collectWebPlayerUrls(String html, String eventUrl, ArrayList<String> found) {
        if (html == null || html.isEmpty()) return;

        // Exact Browser Link targets, for example:
        // /webplayer.php?t=ifr&c=3083550&lang=ru&eid=453410644&lid=3083550&ci=442&si=1
        Pattern wp = Pattern.compile(
                "(?i)(?:https?:\\/\\/[^\\\"'<>\\s]+|/)?webplayer\\.php\\?[^\\\"'<>\\s]+"
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

    private void startLiveTvResolver(Match match) {
        releasePlayer();
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

        Button back = action("BACK");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));
        root.addView(top);

        TextView status = label("Loading LiveTV browser links...", 16, MUTED);
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

        Runnable finishNoStream = () -> {
            if (resolved[0]) return;

            String backup = alternateEventUrl(match.eventUrl);
            if (!fallbackUsed[0] && !backup.isEmpty() && !match.eventUrl.contains("livetv904.me")) {
                fallbackUsed[0] = true;
                status.setText("Trying backup LiveTV source...");
                handler.postDelayed(() -> {
                    if (resolved[0]) return;
                    match.eventUrl = backup;
                    startLiveTvResolver(match);
                }, 250);
                return;
            }

            resolved[0] = true;
            status.setText("No playable LiveTV stream was found.");
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

                status.setText("Trying browser stream " + current[0] + " of " + sourceCandidates.size() + "...");

                web.stopLoading();

                String playerUrl = candidate;
                if (candidate.toLowerCase(Locale.US).startsWith("#webplayer_")) {
                    playerUrl = webPlayerFromFragment(match.eventUrl, candidate);
                }

                if (playerUrl.isEmpty()) {
                    run();
                    return;
                }

                Map<String, String> headers = new HashMap<>();
                headers.put("Referer", match.eventUrl);
                headers.put(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
                );

                web.loadUrl(playerUrl, headers);

                handler.postDelayed(() -> {
                    if (resolved[0] || resolverWebView != web) return;

                    web.evaluateJavascript(
                            "(function(){"
                            + "var a=[];"
                            + "document.querySelectorAll('video,video source,audio source').forEach(function(e){if(e.src)a.push(e.src);});"
                            + "try{performance.getEntriesByType('resource').forEach(function(e){if(e.name)a.push(e.name);});}catch(x){}"
                            + "return JSON.stringify(a);"
                            + "})()",
                            value -> {
                                if (resolved[0] || resolverWebView != web) return;

                                String decoded = value == null ? "" : value;
                                if (decoded.length() >= 2
                                        && decoded.startsWith("\"")
                                        && decoded.endsWith("\"")) {
                                    decoded = decoded.substring(1, decoded.length() - 1)
                                            .replace("\\\"", "\"")
                                            .replace("\\\\", "\\");
                                }

                                ArrayList<String> media = new ArrayList<>();
                                Matcher mm = Pattern.compile(
                                        "(?i)https?://[^\\\"'<>\\s]+(?:\\.m3u8(?:\\?[^\\\"'<>\\s]*)?|\\.mp4(?:\\?[^\\\"'<>\\s]*)?)"
                                ).matcher(decoded);
                                while (mm.find()) {
                                    String mediaUrl = mm.group(0);
                                    if (!media.contains(mediaUrl)) media.add(mediaUrl);
                                }

                                if (!media.isEmpty()) {
                                    resolved[0] = true;
                                    playStreamCandidates(match, media, 0);
                                } else {
                                    tryNext[0].run();
                                }
                            }
                    );
                }, 6000);
            }
        };

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (resolved[0] || resolverWebView != web) return;

                // Directly read the event page and extract the exact
                // webplayer.php Browser Links. No click simulation and no
                // arbitrary site URLs.
                if (sourceCandidates.isEmpty() && url.equals(match.eventUrl)) {
                    view.evaluateJavascript(
                            "(function(){return document.documentElement ? document.documentElement.outerHTML : '';})()",
                            value -> {
                                if (resolved[0] || resolverWebView != web) return;

                                String decoded = value == null ? "" : value;
                                if (decoded.length() >= 2
                                        && decoded.startsWith("\"")
                                        && decoded.endsWith("\"")) {
                                    decoded = decoded.substring(1, decoded.length() - 1)
                                            .replace("\\\"", "\"")
                                            .replace("\\\\", "\\")
                                            .replace("\\n", "\n")
                                            .replace("\\r", "\r");
                                }

                                ArrayList<String> found = new ArrayList<>();
                                collectWebPlayerUrls(decoded, match.eventUrl, found);

                                if (!found.isEmpty()) {
                                    startCandidates(found, status, sourceCandidates, current, resolved, tryNext);
                                } else {
                                    // Some LiveTV pages insert the player links
                                    // after the initial DOM is ready.
                                    handler.postDelayed(() -> {
                                        if (resolved[0] || resolverWebView != web) return;
                                        view.evaluateJavascript(
                                                "(function(){return document.documentElement ? document.documentElement.outerHTML : '';})()",
                                                value2 -> {
                                                    String d2 = value2 == null ? "" : value2;
                                                    if (d2.length() >= 2
                                                            && d2.startsWith("\"")
                                                            && d2.endsWith("\"")) {
                                                        d2 = d2.substring(1, d2.length() - 1)
                                                                .replace("\\\"", "\"")
                                                                .replace("\\\\", "\\")
                                                                .replace("\\n", "\n")
                                                                .replace("\\r", "\r");
                                                    }

                                                    ArrayList<String> found2 = new ArrayList<>();
                                                    collectWebPlayerUrls(d2, match.eventUrl, found2);
                                                    startCandidates(found2, status, sourceCandidates, current, resolved, tryNext);
                                                }
                                        );
                                    }, 1800);
                                }
                            }
                    );
                }
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);

                if (resolved[0] || sourceCandidates.isEmpty() || url == null) return;

                String lower = url.toLowerCase(Locale.US);
                if (lower.contains(".m3u8")
                        || lower.contains(".mp4")
                        || lower.contains("/hls/")
                        || lower.contains("manifest")) {
                    resolved[0] = true;
                    ArrayList<String> media = new ArrayList<>();
                    media.add(url);
                    playStreamCandidates(match, media, 0);
                }
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

        Map<String, String> eventHeaders = new HashMap<>();
        eventHeaders.put("Referer", match.eventUrl.contains("livetv904.me")
                ? "https://livetv904.me/" : "https://livetv.sx/");
        eventHeaders.put(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
        );

        web.loadUrl(match.eventUrl, eventHeaders);
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

        if (found.size() > 12) {
            found.subList(12, found.size()).clear();
        }

        if (found.isEmpty()) {
            resolved[0] = true;
            status.setText("No LiveTV Browser Links were found.");
            return;
        }

        sourceCandidates.clear();
        sourceCandidates.addAll(found);
        current[0] = 0;
        status.setText("Found " + sourceCandidates.size() + " Browser Links. Resolving...");
        tryNext[0].run();
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
            int index
    ) {
        if (streams == null || index >= streams.size()) {
            showPlayer(match, "All LiveTV streams failed.");
            return;
        }

        String candidate = streams.get(index);
        if (candidate.startsWith("WEBVIEW\\t")) {
            String playerUrl = candidate.substring("WEBVIEW\\t".length()).trim();
            showLiveTvWebPlayer(match, playerUrl);
            return;
        }

        if (candidate == null || candidate.trim().isEmpty()) {
            playStreamCandidates(match, streams, index + 1);
            return;
        }

        String[] parts = candidate.split("\\t", -1);
        String url = parts.length > 0 ? parts[0].trim() : "";
        if (url.isEmpty()) {
            playStreamCandidates(match, streams, index + 1);
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
        top.addView(title, new LinearLayout.LayoutParams(0, 56, 1));

        Button back = action("BACK");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));
        root.addView(top);

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
        DefaultHttpDataSource.Factory http =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(
                                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
                        )
                        .setDefaultRequestProperties(headers);

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
                            releasePlayer();
                        }
                        playStreamCandidates(match, streams, nextIndex);
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

        Button back = action("BACK");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));
        root.addView(top);

        WebView web = new WebView(this);
        resolverWebView = web;
        web.setBackgroundColor(Color.BLACK);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setAllowFileAccess(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());

        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        back.setOnClickListener(v -> {
            releasePlayer();
            showMatches();
        });

        setContentView(root);

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

        web.loadUrl(playerUrl);
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