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
import android.view.ViewGroup;
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
import org.json.JSONTokener;
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
    private WebView resolverWebView;
    private boolean playerScreen = false;

    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);
    private final int LIVE = Color.rgb(90, 220, 140);

    private static final String LIVE_TV_URL =
            "https://livetv904.me/enx/allupcoming/";
    private static final String SPORTSRC_API =
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

        TextView title = label("LIVE NOW", 24, TEXT);
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
            // SportSRC is the authoritative source for the match list.
            // Only football events with a live_ ID AND a start timestamp
            // that has already passed are accepted. This prevents future
            // events that happen to carry a live_ ID from appearing as LIVE.
            String json = httpGet(SPORTSRC_API);
            ArrayList<Match> liveMatches = parseSportsrcLiveMatches(json);

            if (!liveMatches.isEmpty()) {
                publishLiveMatches(liveMatches);
                return;
            }

            // SportSRC V1 currently returns the full football schedule but no
            // live_ IDs. Use the live-event source to discover active games,
            // then attach the corresponding SportSRC match ID for detail/stream.
            loadMatchesWithBrowser();
        }).start();
    }

    private void attachSportSrcIds(ArrayList<Match> liveMatches) {
        String sportsrcJson = httpGet(SPORTSRC_API);
        ArrayList<Match> sportsrcMatches = parseSportsrcMatches(sportsrcJson);

        for (Match liveMatch : liveMatches) {
            for (Match s : sportsrcMatches) {
                if ((teamsMatch(liveMatch.home, s.home) && teamsMatch(liveMatch.away, s.away))
                        || (teamsMatch(liveMatch.home, s.away) && teamsMatch(liveMatch.away, s.home))) {
                    liveMatch.sportSrcId = s.id;
                    break;
                }
            }
        }
    }

    private void publishLiveMatches(ArrayList<Match> liveMatches) {
        matches.clear();
        matches.addAll(liveMatches);
        if (!liveMatches.isEmpty()) {
            showMatches();
        } else {
            showError("No live football matches found.");
        }
    }

    private void loadMatchesWithBrowser() {
        final WebView browser = new WebView(this);
        resolverWebView = browser;

        browser.setVisibility(View.INVISIBLE);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
        );
        browser.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(
                        "(function(){"
                        + "var out=[];"
                        + "document.querySelectorAll('a[href*=\\\"/eventinfo/\\\"]').forEach(function(a){"
                        + "var e=a,ctx='',alt='';"
                        + "for(var i=0;i<7&&e;i++,e=e.parentElement){"
                        + "var t=(e.innerText||'').replace(/\\s+/g,' ').trim();"
                        + "var aa=Array.from(e.querySelectorAll('img')).map(function(x){return x.alt||'';}).join(' ');"
                        + "if(/\\d{1,2}\\s*:\\s*\\d{1,2}/.test(t)){ctx=t;alt=aa;break;}"
                        + "}"
                        + "if(ctx) out.push(a.href+'\\t'+(a.innerText||'').replace(/\\s+/g,' ').trim()+'\\t'+ctx+'\\t'+alt);"
                        + "});"
                        + "return out.join('\\n');"
                        + "})()",
                        value -> {
                            ArrayList<Match> result = parseBrowserMatches(value);
                            if (!result.isEmpty()) {
                                attachSportSrcIds(result);
                                publishLiveMatches(result);
                            } else {
                                showError("No live football matches found.");
                            }
                            if (resolverWebView == browser) {
                                resolverWebView = null;
                            }
                            ((ViewGroup) browser.getParent()).removeView(browser);
                            browser.destroy();
                        }
                );
            }
        });

        addContentView(
                browser,
                new ViewGroup.LayoutParams(1, 1)
        );
        browser.loadUrl(LIVE_TV_URL);
    }

    private ArrayList<Match> parseBrowserMatches(String value) {
        ArrayList<Match> result = new ArrayList<>();
        if (value == null || value.length() < 2) return result;

        try {
            Object parsed = new JSONTokener(value).nextValue();
            if (!(parsed instanceof String)) return result;

            String data = (String) parsed;
            String[] rows = data.split("\\n");

            for (String row : rows) {
                String[] p = row.split("\\t", -1);
                if (p.length < 4) continue;

                String href = p[0].trim();
                String anchor = cleanText(p[1]);
                String context = cleanText(p[2]);
                String alt = cleanText(p[3]);

                String sportText = (context + " " + alt).toLowerCase();
                if (!sportText.contains("football")) continue;

                Matcher scoreMatcher = Pattern.compile(
                        "(?<!\\d)\\d{1,2}\\s*:\\s*\\d{1,2}(?!\\d)"
                ).matcher(context);
                if (!scoreMatcher.find()) continue;

                String[] teams = splitTeams(anchor);
                if (teams.length != 2) continue;

                String home = cleanTeamName(teams[0]);
                String away = cleanTeamName(teams[1]);
                if (home.isEmpty() || away.isEmpty()) continue;

                String eventUrl = href.startsWith("http")
                        ? href
                        : "https://livetv904.me" + href;

                boolean duplicate = false;
                for (Match existing : result) {
                    if (existing.eventUrl.equals(eventUrl)) {
                        duplicate = true;
                        break;
                    }
                }

                if (!duplicate) {
                    result.add(new Match(
                            "Football",
                            home,
                            away,
                            scoreMatcher.group() + "  LIVE",
                            true,
                            "",
                            "",
                            eventUrl
                    ));
                }
            }
        } catch (Exception ignored) {
        }

        return result;
    }

private String downloadPage(String address) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; Android TV) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36");
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            c.setRequestProperty("Accept-Language", "en-US,en;q=0.9,ru;q=0.8");
            c.setRequestProperty("Referer", "https://livetv904.me/");
            int code = c.getResponseCode();
            if (code < 200 || code >= 400) return "";
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) out.append(line).append("\n");
            r.close();
            return out.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private ArrayList<Match> parseMatches(String html) {
        ArrayList<Match> result = new ArrayList<>();
        if (html == null || html.isEmpty()) return result;

        // LiveTV904 has a dedicated "Top Events LIVE" block. Live football
        // rows contain a football marker, a score, and an /eventinfo/ link.
        // Do not use kickoff times here: the homepage must contain LIVE only.
        Pattern linkPattern = Pattern.compile(
                "(?is)href\\s*=\\s*[\\\"']([^\\\"']*/eventinfo/[^\\\"']+)[\\\"']");
        Matcher linkMatcher = linkPattern.matcher(html);

        while (linkMatcher.find()) {
            String href = linkMatcher.group(1);
            if (href == null || href.isEmpty()) continue;

            int from = Math.max(0, linkMatcher.start() - 1800);
            int to = Math.min(html.length(), linkMatcher.end() + 1800);
            String context = html.substring(from, to);
            String textContext = cleanText(context);
            String lowerContext = textContext.toLowerCase(Locale.US);

            // Football identification comes from the same event row/block.
            if (!lowerContext.contains("football")) continue;

            // A numeric score is the reliable LIVE marker on LiveTV904.
            Matcher scoreMatcher = Pattern.compile(
                    "(?<!\\d)\\d{1,2}\\s*:\\s*\\d{1,2}(?!\\d)")
                    .matcher(textContext);
            if (!scoreMatcher.find()) continue;

            String score = scoreMatcher.group();

            // Extract the actual event anchor text from this context.
            String anchorText = "";
            Pattern exactLink = Pattern.compile(
                    "(?is)<a\\s+[^>]*href\\s*=\\s*[\\\"']"
                    + Pattern.quote(href)
                    + "[\\\"'][^>]*>(.*?)</a>");
            Matcher am = exactLink.matcher(context);
            if (am.find()) anchorText = cleanText(am.group(1));

            if (anchorText.isEmpty()) {
                // Fallback: locate the event URL and take the nearest visible
                // text around it. This handles small HTML layout changes.
                String hrefToken = Pattern.quote(href);
                Matcher tm = Pattern.compile(
                        "(?is)" + hrefToken + "[^>]*>(.*?)</a>")
                        .matcher(context);
                if (tm.find()) anchorText = cleanText(tm.group(1));
            }

            String[] teams = splitTeams(anchorText);
            if (teams.length != 2 || teams[0].isEmpty() || teams[1].isEmpty()) {
                continue;
            }

            String home = cleanTeamName(teams[0]);
            String away = cleanTeamName(teams[1]);
            if (home.isEmpty() || away.isEmpty()) continue;

            String league = "Football";
            Matcher sportMatcher = Pattern.compile(
                    "(?is)<img[^>]+alt\\s*=\\s*[\\\"']([^\\\"']*Football[^\\\"']*)[\\\"'][^>]*>")
                    .matcher(context);
            if (sportMatcher.find()) {
                String alt = cleanText(sportMatcher.group(1));
                String rest = alt.replaceFirst("(?i)^Football\\.?\\s*", "").trim();
                if (!rest.isEmpty()) league = rest;
            }

            String eventUrl = href.startsWith("http") ? href : "https://livetv904.me" + href;

            boolean duplicate = false;
            for (Match existing : result) {
                if (existing.eventUrl.equals(eventUrl)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                result.add(new Match(league, home, away, score + "  LIVE", true, "", "", eventUrl));
            }
        }
        return result;
    }

    private ArrayList<Match> parseSportsrcLiveMatches(String json) {
        ArrayList<Match> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;

        try {
            Object root = new JSONTokener(json.trim()).nextValue();
            JSONArray arr = null;
            if (root instanceof JSONArray) {
                arr = (JSONArray) root;
            } else if (root instanceof JSONObject) {
                JSONObject ro = (JSONObject) root;
                arr = firstArray(ro, "data", "matches", "events", "results");
                if (arr == null) {
                    JSONObject data = ro.optJSONObject("data");
                    if (data != null) arr = firstArray(data, "matches", "events", "results");
                }
            }
            if (arr == null) return out;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;

                String id = getString(o, "id", "match_id", "event_id");
                String category = getString(o, "category", "sport", "type");
                String status = getString(o, "status", "state", "match_status", "matchStatus");
                String home = getString(o, "home", "home_team", "homeTeam", "team_home");
                String away = getString(o, "away", "away_team", "awayTeam", "team_away");
                String title = getString(o, "title", "name", "event", "match");
                String league = getString(o, "league", "competition", "tournament");

                JSONObject teams = o.optJSONObject("teams");
                if (teams != null) {
                    JSONObject ho = teams.optJSONObject("home");
                    JSONObject ao = teams.optJSONObject("away");
                    if (home.isEmpty() && ho != null) home = getString(ho, "name", "title");
                    if (away.isEmpty() && ao != null) away = getString(ao, "name", "title");
                }

                if ((home.isEmpty() || away.isEmpty()) && !title.isEmpty()) {
                    String[] pair = splitTeams(title);
                    if (home.isEmpty()) home = pair[0];
                    if (away.isEmpty()) away = pair[1];
                }

                boolean football = "football".equalsIgnoreCase(category)
                        || "soccer".equalsIgnoreCase(category)
                        || "football".equalsIgnoreCase(getString(o, "sport_name"));
                boolean live = id.toLowerCase(Locale.US).startsWith("live_")
                        || isLive(o, status);

                // Some V1 responses omit category/status/date. A live_ event ID
                // is the strongest LIVE marker, so do not require a timestamp.
                if (!football && !title.toLowerCase(Locale.US).contains("football")) continue;
                if (!live) continue;
                if (home.isEmpty() || away.isEmpty()) continue;

                if (league.isEmpty()) league = "Football";
                String score = getString(o, "score", "result", "current_score");
                if (score.isEmpty()) score = "LIVE";

                out.add(new Match(
                        league,
                        cleanTeamName(home),
                        cleanTeamName(away),
                        cleanText(score) + "  LIVE",
                        true,
                        id,
                        "",
                        ""
                ));
            }
        } catch (Exception ignored) {
        }

        return out;
    }

    private ArrayList<Match> parseSportsrcMatches(String json) {
        ArrayList<Match> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            Object root = new JSONTokener(json.trim()).nextValue();
            JSONArray arr = null;
            if (root instanceof JSONArray) {
                arr = (JSONArray) root;
            } else if (root instanceof JSONObject) {
                arr = firstArray((JSONObject) root, "data", "matches", "events", "results");
            }
            if (arr != null) parseSportsrcArray(arr, out);
        } catch (Exception ignored) {}
        return out;
    }
    private void parseSportsrcArray(JSONArray arr, ArrayList<Match> out) {
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String id = getString(o, "id", "match_id", "event_id");
                String home = getString(o, "home", "home_team", "homeTeam", "team_home");
                String away = getString(o, "away", "away_team", "awayTeam", "team_away");
                String title = getString(o, "title", "name", "event", "match");
                String league = getString(o, "league", "competition", "tournament", "category");

                JSONObject teams = o.optJSONObject("teams");
                if (teams != null) {
                    JSONObject ho = teams.optJSONObject("home");
                    JSONObject ao = teams.optJSONObject("away");
                    if (home.isEmpty() && ho != null) home = getString(ho, "name", "title");
                    if (away.isEmpty() && ao != null) away = getString(ao, "name", "title");
                }
                if ((home.isEmpty() || away.isEmpty()) && !title.isEmpty()) {
                    String[] pair = splitTeams(title);
                    if (home.isEmpty()) home = pair[0];
                    if (away.isEmpty()) away = pair[1];
                }
                if (id.isEmpty() || home.isEmpty() || away.isEmpty()) continue;
                if (league.isEmpty()) league = "Football";
                out.add(new Match(league, home, away, "", false, id, "", ""));
            } catch (Exception ignored) {}
        }
    }

    private boolean sameTeam(String a, String b) {
        return normalizeTeam(a).equals(normalizeTeam(b));
    }

    private boolean teamsMatch(String a, String b) {
        String x = normalizeTeam(a);
        String y = normalizeTeam(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        if (x.length() >= 5 && y.length() >= 5 && (x.contains(y) || y.contains(x))) return true;

        String[] xa = x.split("(?=.{1})");
        String[] ya = y.split("(?=.{1})");
        int common = 0;
        for (String token : new String[]{x, y}) {
            if (token.length() >= 5 && (x.contains(token) || y.contains(token))) common++;
        }
        return common > 0;
    }

    private String normalizeTeam(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "")
                .replace("footballclub", "")
                .replace("fc", "");
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

    private JSONArray firstArray(JSONObject o, String... keys) {
        for (String k : keys) {
            JSONArray a = o.optJSONArray(k);
            if (a != null) return a;
        }
        return null;
    }

    private long getLong(JSONObject o, String... keys) {
        for (String k : keys) {
            Object v = o.opt(k);
            if (v == null || v == JSONObject.NULL) continue;
            if (v instanceof Number) return ((Number) v).longValue();
            try {
                return Long.parseLong(String.valueOf(v).trim());
            } catch (Exception ignored) {}
        }
        return 0L;
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

    private String cleanText(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replace("&amp;", "&").replaceAll("\\s+", " ").trim();
    }

    private String cleanTeamName(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    private String[] splitTeams(String title) {
        String s = cleanText(title);
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

        return s.contains("live") || s.contains("inprogress")
                || s.contains("in progress") || s.equals("playing");
    }

    private void openMatch(Match match) {
        showPlayerLoading(match);
        new Thread(() -> {
            String sportSrcId = match.sportSrcId;
            if (sportSrcId == null || sportSrcId.isEmpty()) {
                ArrayList<Match> sportsrc = parseSportsrcMatches(httpGet(SPORTSRC_API));
                for (Match s : sportsrc) {
                    if (teamsMatch(match.home, s.home) && teamsMatch(match.away, s.away)
                            || teamsMatch(match.home, s.away) && teamsMatch(match.away, s.home)) {
                        sportSrcId = s.id;
                        break;
                    }
                }
            }
            if (sportSrcId == null || sportSrcId.isEmpty()) {
                runOnUiThread(() -> showPlayerError(match, "No SportSRC stream match was found."));
                return;
            }
            String url = "https://api.sportsrc.org/?data=detail&category=football&id=" + encode(sportSrcId);
            String json = httpGet(url);
            String embed = findEmbed(json);
            runOnUiThread(() -> {
                if (embed.isEmpty()) showPlayerError(match, "No working stream was returned for this match.");
                else showEmbedPlayer(match, embed);
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
        String eventUrl;
        String sportSrcId;

        Match(String league, String home, String away, String time, boolean live, String id, String rawTime, String eventUrl) {
            this.league = league;
            this.home = home;
            this.away = away;
            this.time = time;
            this.live = live;
            this.id = id;
            this.rawTime = rawTime;
            this.eventUrl = eventUrl;
            this.sportSrcId = "";
        }
    }
}
