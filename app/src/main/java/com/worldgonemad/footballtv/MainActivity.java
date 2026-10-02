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

                                    showMatches();

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

            String prefix =
                    html.substring(
                            Math.max(0, linkMatcher.start() - 15000),
                            linkMatcher.start()
                    );

            Matcher footballMatcher =
                    Pattern.compile(
                            "(?is)<img[^>]+alt\\s*=\\s*[\\\"']Football\\.([^\\\"']+)[\\\"'][^>]*>"
                    ).matcher(prefix);

            String league = "";

            while (footballMatcher.find()) {
                league = cleanText(footballMatcher.group(1));
            }

            if (league.isEmpty()) {
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

        // The /player/links/ endpoint normally needs the event's current
        // temporary "t" value. Do not invent it: first load the event page
        // and let the JavaScript resolver extract the actual source URL.
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

                        // First, inspect resources already requested by the page.
                        "var e=performance.getEntriesByType('resource');" +
                        "for(var i=0;i<e.length;i++){" +
                        "var p=e[i].name||'';" +
                        "if(/\\.m3u8(?:\\?|$)/i.test(p)){location.href=p;return;}" +
                        "}" +

                        // Keep popups in this WebView.
                        "window.open=function(u){if(u){location.href=new URL(u,location.href).href;}return null;};" +

                        // Search the complete HTML too. LiveTV904 can put the
                        // current player URL in JavaScript rather than an href.
                        "var raw=document.documentElement?document.documentElement.innerHTML:'';" +
                        "var direct=raw.match(/https?:[^\\\\\\\"'<>\\\\s]+\\.m3u8(?:\\\\?[^\\\\\\\"'<>\\\\s]*)?/i);" +
                        "if(direct){location.href=direct[0];return;}" +

                        // Prefer the real player/source endpoints exposed by
                        // the event page. Resolve relative URLs against it.
                        "var patterns=[" +
                        "/(?:https?:\\\\/\\\\/[^\\\\\\\"'<>\\\\s]+)?(?:webplayer2?\\\\.php|\\\\/player\\\\/links\\\\/|\\\\/export\\\\/)[^\\\\\\\"'<>\\\\s]*/i," +
                        "/https?:[^\\\\\\\"'<>\\\\s]*(?:apl614|azplay)[^\\\\\\\"'<>\\\\s]*/i" +
                        "];" +
                        "for(var pi=0;pi<patterns.length;pi++){" +
                        "var pm=raw.match(patterns[pi]);" +
                        "if(pm&&pm[0]){" +
                        "var pu=pm[0].replace(/&amp;/g,'&').replace(/\\\\\\\\\\\\//g,'/');" +
                        "try{pu=new URL(pu,location.href).href;}catch(x){}" +
                        "if(/^https?:/i.test(pu)){location.href=pu;return;}" +
                        "}" +
                        "}" +

                        // Inspect normal DOM attributes and inline handlers.
                        "var a=document.querySelectorAll('a,iframe,source,video,button');" +
                        "for(var j=0;j<a.length;j++){" +
                        "var u=a[j].href||a[j].src||'';" +
                        "var d=a[j].getAttribute('data-url')||a[j].getAttribute('data-href')||'';" +
                        "var oc=a[j].getAttribute('onclick')||'';" +
                        "var txt=(a[j].innerText||a[j].textContent||'').trim();" +
                        "var all=(u+' '+d+' '+oc);" +

                        "if(/\\.m3u8(?:\\?|$)/i.test(all)){" +
                        "var mh=all.match(/https?:[^\\\\\\\"'\\\\s]+\\.m3u8(?:\\\\?[^\\\\\\\"'\\\\s]*)?/i);" +
                        "if(mh){location.href=mh[0];return;}" +
                        "}" +

                        // Resolve relative player/source URLs instead of requiring
                        // them to be absolute in the HTML.
                        "if(/webplayer2?\\\\.php|\\\\/player\\\\/links\\\\/|\\\\/export\\\\/|apl614|azplay/i.test(all)){" +
                        "var candidate=u||d||'';" +
                        "if(!candidate){" +
                        "var cm=all.match(/(?:https?:[^\\\\\\\"'\\\\s)]+|\\\\/[^\\\\\\\"'\\\\s)]+)/i);" +
                        "candidate=cm?cm[0]:'';" +
                        "}" +
                        "if(candidate&& !/^javascript:/i.test(candidate)){" +
                        "try{candidate=new URL(candidate,location.href).href;}catch(x){}" +
                        "if(/^https?:/i.test(candidate)){location.href=candidate;return;}" +
                        "}" +
                        "}" +
                        "}" +

                        // Browser Links can expose sources only through a click.
                        // Try source links/buttons one by one, prioritizing the
                        // actual player/source endpoints over generic labels.
                        "if(/\\\\/player\\\\/links\\\\//i.test(location.href)){" +
                        "var links=document.querySelectorAll('a,button');" +
                        "for(var k=0;k<links.length;k++){" +
                        "var lu=links[k].href||links[k].getAttribute('data-url')||links[k].getAttribute('data-href')||'';" +
                        "var lo=links[k].getAttribute('onclick')||'';" +
                        "var lt=(links[k].innerText||links[k].textContent||'').trim();" +
                        "var la=(lu+' '+lo+' '+lt);" +
                        "if(/webplayer2?\\\\.php|\\\\/export\\\\/|apl614|azplay|Aliez|Web/i.test(la)){" +
                        "try{links[k].click();return;}catch(x){}" +
                        "}" +
                        "}" +
                        "}" +

                        "}catch(x){}" +
                        "})()";}