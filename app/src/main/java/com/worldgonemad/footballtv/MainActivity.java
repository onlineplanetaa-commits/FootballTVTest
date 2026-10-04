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
import android.webkit.ValueCallback;
import org.json.JSONArray;

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

    private ArrayList<String> fallbackSourceUrls = new ArrayList<>();
    private final Set<String> failedSourceUrls = new HashSet<>();
    private Match fallbackMatch;
    private String activeSourceUrl = "";
    private boolean browserPlaybackDetected = false;

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

        if (lowerLastAlt.startsWith("football.")
                || lowerLastAlt.equals("football")) {
            sport = "football";
            league = lastAlt.length() > 9
                    ? cleanText(lastAlt.substring(9))
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

        // Resolve the public stream in the background first. LiveTV904 often
        // exposes the final HLS URL in the event/player HTML, and this path
        // is much faster and more reliable than waiting for WebView timers.
        showPlayer(match, "Finding video stream...");

        // Keep the known-good startup path. Stream resolving must not block
        // or change Activity initialization on Android TV.
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

        // Use the real LiveTV904 browser player instead of trying to extract
        // an HLS URL. LiveTV904's Browser Links are intended to be opened in
        // a modern browser/WebView and may use JavaScript/fragment handlers.
        if (resolverWebView != null) {
            try {
                resolverWebView.stopLoading();
                resolverWebView.destroy();
            } catch (Exception ignored) {}
            resolverWebView = null;
        }

        final WebView web = new WebView(this);
        resolverWebView = web;

        web.setVisibility(View.VISIBLE);
        web.setAlpha(1f);
        web.setBackgroundColor(Color.BLACK);
        browserPlaybackDetected = false;

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setAllowContentAccess(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        );

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        if (playerView != null && playerView.getParent() instanceof LinearLayout) {
            LinearLayout parent = (LinearLayout) playerView.getParent();
            int index = parent.indexOfChild(playerView);
            parent.removeView(playerView);
            playerView = null;
            parent.addView(
                    web,
                    index,
                    new LinearLayout.LayoutParams(-1, 0, 1)
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
                // Keep popup/player navigation inside this WebView.
                return false;
            }
        });

        web.setWebViewClient(new WebViewClient() {

            private final ArrayList<String> sourceScripts =
                    new ArrayList<>();

            private int sourceIndex = 0;
            private int navigationToken = 0;
            private boolean sourceListCollected = false;
            private boolean playingDetected = false;

            private void status(String text) {
                runOnUiThread(() -> {
                    if (resolverWebView == web) {
                        // The status TextView is the first TextView below the
                        // top bar. Find it without changing the existing layout.
                        if (web.getParent() instanceof LinearLayout) {
                            LinearLayout p = (LinearLayout) web.getParent();
                            for (int i = 0; i < p.getChildCount(); i++) {
                                View child = p.getChildAt(i);
                                if (child instanceof TextView
                                        && child != web
                                        && !(child instanceof Button)) {
                                    ((TextView) child).setText(text);
                                    break;
                                }
                            }
                        }
                    }
                });
            }

            private void inspectForPlayback() {
                if (resolverWebView != web || playingDetected) return;

                String js =
                        "(function(){" +
                        "try{" +
                        "var v=document.querySelector('video');" +
                        "if(v){" +
                        "if(!v.paused && v.readyState>=2)return 'PLAYING';" +
                        "if(v.readyState>=2 && (v.currentTime||0)>0)return 'PLAYING';" +
                        "}" +
                        "var e=document.querySelector('iframe');" +
                        "if(e && e.src)return 'IFRAME';" +
                        "return '';" +
                        "}catch(x){return '';}" +
                        "})()";

                web.evaluateJavascript(js, value -> {
                    if (resolverWebView != web || value == null) return;

                    if (value.contains("PLAYING")) {
                        playingDetected = true;
                        browserPlaybackDetected = true;
                        status("Playing");
                        return;
                    }

                    if (value.contains("IFRAME")) {
                        // The embedded player may need more time to create its
                        // video element. Do not immediately abandon this source.
                        handler.postDelayed(this::inspectForPlayback, 2000);
                    }
                });
            }

            private void tryNextBrowserSource() {
                if (resolverWebView != web) return;

                if (sourceIndex >= sourceScripts.size()) {
                    // Re-open the event page and collect again. This handles
                    // pages whose source list is generated dynamically.
                    if (!sourceListCollected) {
                        sourceListCollected = true;
                        status("No working stream found.");
                    } else {
                        status("No working browser stream found.");
                    }
                    return;
                }

                int current = sourceIndex++;
                navigationToken++;
                playingDetected = false;

                status("Trying Web " + sourceIndex + "...");

                String script = sourceScripts.get(current);

                // Each source is clicked exactly as a user would click it on
                // LiveTV904. This is important because many links use a
                // JavaScript handler or a #webplayer_* fragment instead of
                // exposing a direct HLS URL.
                web.evaluateJavascript(script, value -> {
                    if (resolverWebView != web) return;

                    handler.postDelayed(() -> {
                        if (resolverWebView != web || playingDetected) return;

                        inspectForPlayback();

                        final int token = navigationToken;
                        handler.postDelayed(() -> {
                            if (resolverWebView != web
                                    || playingDetected
                                    || token != navigationToken) {
                                return;
                            }

                            tryNextBrowserSource();
                        }, 9000);

                    }, 1200);
                });
            }

            private void collectAndStartSources() {
                if (resolverWebView != web) return;

                String js =
                        "(function(){" +
                        "var out=[];" +
                        "var n=document.querySelectorAll('a,button,[role=button]');" +
                        "for(var i=0;i<n.length;i++){" +
                        "var x=n[i];" +
                        "var t=((x.innerText||x.textContent||'')+' '+(x.title||'')).trim();" +
                        "var h=x.getAttribute('href')||'';" +
                        "var oc=x.getAttribute('onclick')||'';" +
                        "var z=(t+' '+h+' '+oc).toLowerCase();" +
                        "if(z.indexOf('aliez')>=0||z.indexOf('webplayer')>=0||" +
                        "t.toLowerCase()==='web'||t.toLowerCase().indexOf('web ')===0||" +
                        "t.toLowerCase().indexOf('aliez ')===0){" +
                        "out.push(i);" +
                        "}" +
                        "}" +
                        "return JSON.stringify(out);" +
                        "})()";

                web.evaluateJavascript(js, value -> {
                    if (resolverWebView != web || value == null) return;

                    try {
                        JSONArray arr = new JSONArray(value);
                        sourceScripts.clear();

                        for (int i = 0; i < arr.length(); i++) {
                            int index = arr.optInt(i, -1);
                            if (index < 0) continue;

                            String script =
                                    "(function(){" +
                                    "var n=document.querySelectorAll('a,button,[role=button]');" +
                                    "var x=n[" + index + "];" +
                                    "if(!x)return 'NO';" +
                                    "try{x.scrollIntoView({block:'center'});}catch(e){}" +
                                    "try{x.click();}catch(e){" +
                                    " " +
                                    "var h=x.getAttribute('href');" +
                                    "if(h)location.href=new URL(h,location.href).href;" +
                                    "}" +
                                    "return 'OK';" +
                                    "})()";

                            sourceScripts.add(script);
                        }
                    } catch (Exception ignored) {}

                    if (sourceScripts.isEmpty()) {
                        status("No browser links found.");
                        return;
                    }

                    sourceListCollected = true;
                    fallbackMatch = match;
                    fallbackSourceUrls.clear();
                    failedSourceUrls.clear();
                    sourceIndex = 0;

                    tryNextBrowserSource();
                });
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {
                // Keep all HTTP(S) navigation inside the embedded browser.
                String u = request.getUrl().toString();
                return !(u.startsWith("http://") || u.startsWith("https://"));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (resolverWebView != web) return;

                // If we are on the original event page, collect its actual
                // Browser Links and click them one by one.
                if (url.contains("/eventinfo/")) {
                    handler.postDelayed(
                            this::collectAndStartSources,
                            1200
                    );
                } else {
                    inspectForPlayback();
                }
            }
        });

        statusForBrowserPlayer(web, "Loading LiveTV904 browser player...");
        web.loadUrl(match.eventUrl);

        // Never leave the user stuck forever on a dead source/page.
        handler.postDelayed(() -> {
            if (resolverWebView == web && !browserPlaybackDetected) {
                try {
                    web.stopLoading();
                    web.destroy();
                } catch (Exception ignored) {}
                resolverWebView = null;
                showPlayer(match, "No working browser stream found.");
            }
        }, 60000);
    }

    private void statusForBrowserPlayer(WebView web, String text) {
        if (web == null || !(web.getParent() instanceof LinearLayout)) return;

        LinearLayout parent = (LinearLayout) web.getParent();

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView
                    && child != web
                    && !(child instanceof Button)) {
                ((TextView) child).setText(text);
                return;
            }
        }
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

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                switchToNextSourceAfterFailure();
            }
        });

        MediaItem item = new MediaItem.Builder()
                .setUri(Uri.parse(hls))
                .build();

        player.setMediaItem(item);
        player.prepare();
        player.play();
    }

    private void switchToNextSourceAfterFailure() {
        if (!playerScreen || fallbackMatch == null) return;

        if (activeSourceUrl != null && !activeSourceUrl.isEmpty()) {
            failedSourceUrls.add(activeSourceUrl);
        }

        releasePlayer();

        handler.postDelayed(() -> {
            if (playerScreen || fallbackMatch != null) {
                resolveWithWebView(fallbackMatch);
            }
        }, 300);
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