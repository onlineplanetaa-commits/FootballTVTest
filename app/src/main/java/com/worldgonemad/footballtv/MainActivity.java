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
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Match> matches = new ArrayList<>();

    private LinearLayout listContainer;
    private TextView statusText;
    private WebView playerWebView;
    private boolean playerScreen = false;

    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);
    private final int LIVE = Color.rgb(90, 220, 140);

    private static final String API =
            "https://api.sportsrc.org/?data=matches&category=football";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        showLoading();
        loadMatches();
    }

    private TextView label(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private Button action(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bg(ACCENT, 14));
        b.setPadding(18, 2, 18, 2);
        return b;
    }

    private void showLoading() {
        playerScreen = false;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);

        TextView title = label("MAX FOOTBALL ONLINE", 28, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 70));

        TextView loading = label("Loading football matches...", 18, MUTED);
        loading.setGravity(Gravity.CENTER);
        root.addView(loading, new LinearLayout.LayoutParams(-1, 60));

        setContentView(root);
    }

    private void showMatches() {
        playerScreen = false;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(42, 20, 42, 12);

        TextView logo = label("MAX FOOTBALL ONLINE", 27, TEXT);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(logo, new LinearLayout.LayoutParams(0, 65, 1));

        statusText = label(matches.size() + " MATCHES", 14, MUTED);
        statusText.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(statusText, new LinearLayout.LayoutParams(-2, 65));
        root.addView(header);

        TextView title = label("LIVE & UPCOMING", 24, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(42, 4, 42, 12);
        root.addView(title, new LinearLayout.LayoutParams(-1, 55));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setPadding(34, 0, 34, 30);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        renderMatches();
    }

    private void renderMatches() {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        if (matches.isEmpty()) {
            TextView empty = label("No football matches found.", 18, MUTED);
            empty.setGravity(Gravity.CENTER);
            listContainer.addView(empty, new LinearLayout.LayoutParams(-1, 100));
            return;
        }

        for (Match m : matches) addMatch(listContainer, m);
        if (statusText != null) statusText.setText(matches.size() + " MATCHES");
    }

    private void addMatch(LinearLayout list, Match match) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(24, 14, 18, 14);
        card.setBackground(bg(PANEL, 18));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 110);
        cp.setMargins(8, 7, 8, 7);
        list.addView(card, cp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);

        TextView league = label(match.league, 12, MUTED);
        info.addView(league, new LinearLayout.LayoutParams(-1, 25));

        TextView teams = label(match.home + "  —  " + match.away, 18, TEXT);
        teams.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        info.addView(teams, new LinearLayout.LayoutParams(-1, 38));

        TextView time = label(match.time, 11, match.live ? LIVE : MUTED);
        info.addView(time, new LinearLayout.LayoutParams(-1, 25));

        card.addView(info, new LinearLayout.LayoutParams(0, -1, 1));

        Button watch = action("WATCH");
        watch.setOnClickListener(v -> openMatch(match));
        card.addView(watch, new LinearLayout.LayoutParams(130, 58));
    }

    private void loadMatches() {
        new Thread(() -> {
            String json = httpGet(API);
            ArrayList<Match> result = parseMatches(json);

            // SportSRC V1 returns a broad schedule. The home screen must show
            // ONLY matches that are currently live, never upcoming fixtures.
            ArrayList<Match> filtered = filterLiveMatches(result);

            runOnUiThread(() -> {
                matches.clear();
                matches.addAll(filtered);
                if (!filtered.isEmpty()) {
                    showMatches();
                } else {
                    showError("No live football matches found.");
                }
            });
        }).start();
    }

    private ArrayList<Match> filterLiveMatches(ArrayList<Match> source) {
        ArrayList<Match> out = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Match m : source) {
            if (m.live) {
                out.add(m);
                continue;
            }

            // If V1 has no live status, infer live state from kickoff time:
            // football normally runs about 90 minutes plus stoppage time.
            long start = parseMatchTime(m.rawTime);
            if (start > 0 && start <= now && now <= start + 125L * 60L * 1000L) {
                m.live = true;
                out.add(m);
            }
        }
        return out;
    }

    private long parseMatchTime(String value) {
        if (value == null || value.trim().isEmpty()) return -1L;
        String s = value.trim();

        try {
            long n = Long.parseLong(s);
            if (n < 100000000000L) n *= 1000L;
            return n;
        } catch (Exception ignored) {}

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm"
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                if (p.endsWith("'Z'")) f.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = f.parse(s);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private String httpGet(String address) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV)");
            c.setRequestProperty("Accept", "application/json,text/plain,*/*");

            int code = c.getResponseCode();
            if (code < 200 || code >= 400) return "";

            BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) out.append(line);
            r.close();
            return out.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private ArrayList<Match> parseMatches(String json) {
        ArrayList<Match> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;

        try {
            Object root = new JSONArray(json);
            parseArray((JSONArray) root, out);
            return out;
        } catch (Exception ignored) {}

        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = firstArray(root, "matches", "events", "data", "results");
            if (arr != null) parseArray(arr, out);
        } catch (Exception ignored) {}

        return out;
    }

    private void parseArray(JSONArray arr, ArrayList<Match> out) {
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;

                String id = getString(o, "id", "match_id", "event_id");
                String home = getString(o, "home", "home_team", "homeTeam", "team_home");
                String away = getString(o, "away", "away_team", "awayTeam", "team_away");
                String title = getString(o, "title", "name", "event", "match");
                String league = getString(o, "league", "competition", "tournament", "category");
                String timeValue = getString(o, "date", "datetime", "time", "timestamp", "start", "start_time", "startTime");

                // SportSRC V1 puts team names inside teams.home.name / teams.away.name.
                JSONObject teams = o.optJSONObject("teams");
                if (teams != null) {
                    JSONObject homeObj = teams.optJSONObject("home");
                    JSONObject awayObj = teams.optJSONObject("away");
                    if ((home == null || home.isEmpty()) && homeObj != null) {
                        home = getString(homeObj, "name", "title");
                    }
                    if ((away == null || away.isEmpty()) && awayObj != null) {
                        away = getString(awayObj, "name", "title");
                    }
                }

                // V1 uses an epoch-millisecond numeric date field.
                // Keep rawTime so live detection uses the exact kickoff timestamp.
                if ((home == null || home.isEmpty()) || (away == null || away.isEmpty())) {
                    if (title != null) {
                        String[] pair = splitTeams(title);
                        if (home == null || home.isEmpty()) home = pair[0];
                        if (away == null || away.isEmpty()) away = pair[1];
                    }
                }

                if (id == null || id.isEmpty() || home == null || away == null
                        || home.isEmpty() || away.isEmpty()) continue;

                if (league == null || league.isEmpty()) league = "Football";

                String time = formatTime(timeValue);
                boolean live = isLive(o, timeValue);

                out.add(new Match(league, home, away, time, live, id, timeValue));
            } catch (Exception ignored) {}
        }
    }

    private JSONArray firstArray(JSONObject o, String... keys) {
        for (String k : keys) {
            JSONArray a = o.optJSONArray(k);
            if (a != null) return a;
        }
        return null;
    }

    private String getString(JSONObject o, String... keys) {
        for (String k : keys) {
            Object v = o.opt(k);
            if (v != null && v != JSONObject.NULL) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty()) return s;
            }
        }
        return "";
    }

    private String[] splitTeams(String title) {
        String s = title.replaceAll("\\s+", " ").trim();
        String[] parts = s.split("\\s+(?:vs\\.?|v\\.?|—|–)\\s+", 2);
        if (parts.length == 2) return parts;
        parts = s.split("\\s+-\\s+", 2);
        if (parts.length == 2) return parts;
        return new String[] {"", ""};
    }

    private String formatTime(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            long n = Long.parseLong(value);
            if (n < 100000000000L) n *= 1000L;
            Date d = new Date(n);
            SimpleDateFormat f = new SimpleDateFormat("dd MMM  HH:mm", Locale.US);
            f.setTimeZone(TimeZone.getDefault());
            return f.format(d);
        } catch (Exception ignored) {
            return value;
        }
    }

    private boolean isLive(JSONObject o, String time) {
        String status = getString(o, "status", "state", "match_status", "matchStatus");
        String s = status == null ? "" : status.toLowerCase(Locale.US);

        if (s.contains("live") || s.contains("inprogress")
                || s.contains("in progress") || s.equals("playing")) {
            return true;
        }

        // SportSRC V1 normally has no explicit live flag.
        // Determine live state from the real kickoff timestamp instead.
        long start = parseMatchTime(time);
        if (start <= 0) return false;

        long now = System.currentTimeMillis();
        long elapsed = now - start;

        // 125 minutes covers 90 minutes, half-time and normal stoppage time.
        return elapsed >= 0 && elapsed <= 125L * 60L * 1000L;
    }

    private void openMatch(Match match) {
        showPlayerLoading(match);

        new Thread(() -> {
            String url = "https://api.sportsrc.org/?data=detail&category=football&id="
                    + encode(match.id);
            String json = httpGet(url);
            String embed = findEmbed(json);

            runOnUiThread(() -> {
                if (embed.isEmpty()) {
                    showPlayerError(match, "No working stream was returned for this match.");
                } else {
                    showEmbedPlayer(match, embed);
                }
            });
        }).start();
    }

    private String findEmbed(String json) {
        if (json == null || json.trim().isEmpty()) return "";

        try {
            Object root;
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                root = new JSONArray(trimmed);
            } else {
                root = new JSONObject(trimmed);
            }

            // SportSRC detail responses can wrap the match inside data/result
            // and streams can be an array of objects containing url/src/embed.
            String found = findStreamUrl(root);
            if (!found.isEmpty()) return found;

            String html = findHtml(root);
            if (!html.isEmpty()) {
                Matcher m = Pattern.compile(
                        "(?i)<iframe[^>]+src\\s*=\\s*[\\\"']([^\\\"']+)")
                        .matcher(html);
                if (m.find()) return m.group(1);
            }
        } catch (Exception ignored) {}

        // Last-resort extraction, but only accept URLs that look like a player/stream.
        Matcher m = Pattern.compile(
                "(?i)https?://[^\\\"'\\s<>]+")
                .matcher(json);
        while (m.find()) {
            String u = m.group();
            if (looksLikeStreamUrl(u)) return u;
        }
        return "";
    }

    private String findStreamUrl(Object value) {
        if (value instanceof JSONObject) {
            JSONObject o = (JSONObject) value;

            // First pass: fields whose names explicitly identify a stream/embed.
            String[] preferred = {
                    "stream", "streams", "stream_url", "streamUrl",
                    "embed", "embed_url", "embedUrl", "iframe",
                    "iframe_url", "iframeUrl", "player", "player_url",
                    "playerUrl", "source", "source_url", "sourceUrl",
                    "src", "url", "link", "href"
            };

            for (String key : preferred) {
                Object v = o.opt(key);
                String r = extractStreamValue(v);
                if (!r.isEmpty()) return r;
            }

            // Then recurse through the rest of the actual JSON structure.
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String key = it.next();
                Object v = o.opt(key);
                if (v instanceof JSONObject || v instanceof JSONArray) {
                    String r = findStreamUrl(v);
                    if (!r.isEmpty()) return r;
                } else if (v instanceof String) {
                    String s = ((String) v).trim();
                    if (looksLikeStreamUrl(s)) return s;
                }
            }
        } else if (value instanceof JSONArray) {
            JSONArray a = (JSONArray) value;

            // Prefer objects that are explicitly stream/source records.
            for (int i = 0; i < a.length(); i++) {
                Object item = a.opt(i);
                if (item instanceof JSONObject) {
                    String r = findStreamUrl((JSONObject) item);
                    if (!r.isEmpty()) return r;
                }
            }

            for (int i = 0; i < a.length(); i++) {
                Object item = a.opt(i);
                if (item instanceof String) {
                    String s = ((String) item).trim();
                    if (looksLikeStreamUrl(s)) return s;
                }
            }
        }
        return "";
    }

    private String extractStreamValue(Object v) {
        if (v == null || v == JSONObject.NULL) return "";

        if (v instanceof JSONObject || v instanceof JSONArray) {
            return findStreamUrl(v);
        }

        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.contains("<iframe") || s.contains("<video")) {
                Matcher m = Pattern.compile(
                        "(?i)<iframe[^>]+src\\s*=\\s*[\\\"']([^\\\"']+)")
                        .matcher(s);
                if (m.find()) return m.group(1);
            }
            if (looksLikeStreamUrl(s)) return s;
        }
        return "";
    }

    private boolean looksLikeStreamUrl(String s) {
        if (s == null) return false;
        String u = s.trim().toLowerCase(Locale.US);
        if (!(u.startsWith("http://") || u.startsWith("https://"))) return false;

        // Avoid selecting logos, flags, posters and other ordinary assets.
        if (u.matches(".*\\.(png|jpe?g|gif|webp|svg)(\\?.*)?$")) return false;

        return u.contains("embed")
                || u.contains("player")
                || u.contains("stream")
                || u.contains("iframe")
                || u.contains(".m3u8")
                || u.contains(".mp4")
                || u.contains("/watch")
                || u.contains("/live")
                || u.contains("video");
    }

    private String findHtml(Object value) {
        if (value instanceof JSONObject) {
            JSONObject o = (JSONObject) value;
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                Object v = o.opt(it.next());
                if (v instanceof String) {
                    String s = (String) v;
                    if (s.contains("<iframe") || s.contains("<video")) return s;
                } else if (v instanceof JSONObject || v instanceof JSONArray) {
                    String r = findHtml(v);
                    if (!r.isEmpty()) return r;
                }
            }
        } else if (value instanceof JSONArray) {
            JSONArray a = (JSONArray) value;
            for (int i = 0; i < a.length(); i++) {
                Object v = a.opt(i);
                if (v instanceof String) {
                    String s = (String) v;
                    if (s.contains("<iframe") || s.contains("<video")) return s;
                } else if (v instanceof JSONObject || v instanceof JSONArray) {
                    String r = findHtml(v);
                    if (!r.isEmpty()) return r;
                }
            }
        }
        return "";
    }

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private void showPlayerLoading(Match match) {
        releaseWebView();
        playerScreen = true;

        LinearLayout root = playerRoot(match);
        TextView status = label("Finding video stream...", 17, MUTED);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showPlayerError(Match match, String message) {
        releaseWebView();
        playerScreen = true;

        LinearLayout root = playerRoot(match);
        TextView status = label(message, 17, MUTED);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, 0, 1));

        Button back = action("BACK");
        root.addView(back, new LinearLayout.LayoutParams(-1, 58));
        back.setOnClickListener(v -> {
            releaseWebView();
            showMatches();
        });
        setContentView(root);
    }

    private LinearLayout playerRoot(Match match) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(20, 8, 20, 8);

        TextView title = label(match.home + " — " + match.away, 19, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, 56, 1));

        Button back = action("BACK");
        top.addView(back, new LinearLayout.LayoutParams(120, 52));
        root.addView(top);

        back.setOnClickListener(v -> {
            releaseWebView();
            showMatches();
        });

        return root;
    }

    private void showEmbedPlayer(Match match, String embed) {
        releaseWebView();
        playerScreen = true;

        LinearLayout root = playerRoot(match);

        WebView web = new WebView(this);
        playerWebView = web;
        web.setBackgroundColor(Color.BLACK);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        web.getSettings().setSupportMultipleWindows(false);
        web.getSettings().setLoadsImagesAutomatically(true);
        web.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebChromeClient(new WebChromeClient());

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String u = req.getUrl().toString();
                if (u.startsWith("http://") || u.startsWith("https://")) {
                    view.loadUrl(u);
                }
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String u) {
                if (u != null && (u.startsWith("http://") || u.startsWith("https://"))) {
                    view.loadUrl(u);
                }
                return true;
            }
        });

        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        String html =
                "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body{margin:0;background:#000;width:100%;height:100%;overflow:hidden}"
                + "iframe{border:0;width:100%;height:100%;}</style></head><body>"
                + "<iframe src='" + htmlEscape(embed)
                + "' allow='autoplay; fullscreen; encrypted-media' allowfullscreen></iframe>"
                + "</body></html>";

        web.loadDataWithBaseURL(
                "https://sportsrc.org/",
                html,
                "text/html",
                "UTF-8",
                null);
    }

    private String htmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void releaseWebView() {
        if (playerWebView != null) {
            try {
                playerWebView.stopLoading();
                playerWebView.destroy();
            } catch (Exception ignored) {}
            playerWebView = null;
        }
    }

    private void showError(String message) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);

        TextView title = label("MAX FOOTBALL ONLINE", 28, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 70));

        TextView error = label(message, 18, MUTED);
        error.setGravity(Gravity.CENTER);
        root.addView(error, new LinearLayout.LayoutParams(-1, 70));

        Button retry = action("RETRY");
        retry.setOnClickListener(v -> loadMatches());
        root.addView(retry, new LinearLayout.LayoutParams(180, 58));

        setContentView(root);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playerScreen) {
            releaseWebView();
            playerScreen = false;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseWebView();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (playerScreen) {
            releaseWebView();
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
        String id;
        String rawTime;

        Match(String league, String home, String away, String time, boolean live, String id, String rawTime) {
            this.league = league;
            this.home = home;
            this.away = away;
            this.time = time;
            this.live = live;
            this.id = id;
            this.rawTime = rawTime;
        }
    }
}
