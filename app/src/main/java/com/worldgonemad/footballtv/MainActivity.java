```java
package com.worldgonemad.footballtv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private final String LIVETV_URL = "https://livetv904.me/";

    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);
    private final int LIVE = Color.rgb(90, 220, 140);

    private LinearLayout list;
    private TextView statusText;
    private final Handler handler = new Handler();

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadMatches();
            handler.postDelayed(this, 5 * 60 * 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMatchesScreen();
        loadMatches();
    }

    private TextView label(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
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
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setBackground(bg(ACCENT, 14));
        b.setPadding(18, 2, 18, 2);

        b.setOnFocusChangeListener((v, hasFocus) ->
                v.setAlpha(hasFocus ? 1f : 0.82f)
        );

        return b;
    }

    private void showMatchesScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(42, 20, 42, 12);

        TextView logo = label(
                "MAX FOOTBALL ONLINE",
                27,
                TEXT
        );

        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        header.addView(
                logo,
                new LinearLayout.LayoutParams(0, 65, 1)
        );

        statusText = label(
                "Loading...",
                14,
                MUTED
        );

        statusText.setGravity(
                Gravity.CENTER_VERTICAL | Gravity.RIGHT
        );

        header.addView(
                statusText,
                new LinearLayout.LayoutParams(-2, 65)
        );

        root.addView(header);

        TextView title = label(
                "LIVE & UPCOMING",
                24,
                TEXT
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setPadding(42, 4, 42, 12);

        root.addView(
                title,
                new LinearLayout.LayoutParams(-1, 55)
        );

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(34, 0, 34, 30);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(list);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        setContentView(root);
    }

    private void loadMatches() {

        statusText.setText("Updating...");

        new Thread(() -> {

            ArrayList<Match> matches =
                    parseLiveTV();

            runOnUiThread(() -> {

                list.removeAllViews();

                if (matches.isEmpty()) {

                    TextView empty = label(
                            "No football events found.",
                            18,
                            MUTED
                    );

                    empty.setPadding(20, 30, 20, 30);

                    list.addView(empty);

                    statusText.setText("No matches");

                    return;
                }

                for (Match match : matches) {
                    addMatch(match);
                }

                statusText.setText(
                        matches.size() + " matches"
                );
            });

        }).start();
    }

    private ArrayList<Match> parseLiveTV() {

        ArrayList<Match> result =
                new ArrayList<>();

        HttpURLConnection connection = null;

        try {

            URL url = new URL(LIVETV_URL);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
            );

            InputStream input =
                    connection.getInputStream();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(input)
                    );

            StringBuilder html =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                html.append(line).append("\n");
            }

            reader.close();

            String page = html.toString();

            /*
             * LiveTV uses links to individual events.
             * We collect links containing /event/
             * and keep football entries.
             */

            Pattern linkPattern = Pattern.compile(
                    "<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>",
                    Pattern.CASE_INSENSITIVE |
                    Pattern.DOTALL
            );

            Matcher matcher =
                    linkPattern.matcher(page);

            while (matcher.find()) {

                String link = matcher.group(1);
                String rawTitle = matcher.group(2);

                if (link == null || rawTitle == null) {
                    continue;
                }

                String title =
                        rawTitle
                                .replaceAll("<[^>]+>", " ")
                                .replace("&ndash;", "–")
                                .replace("&nbsp;", " ")
                                .replace("&amp;", "&")
                                .replaceAll("\\s+", " ")
                                .trim();

                if (title.length() < 5) {
                    continue;
                }

                String lower =
                        (title + " " + link)
                                .toLowerCase();

                /*
                 * Keep football-related events.
                 */
                if (!isFootball(lower)) {
                    continue;
                }

                if (!link.startsWith("http")) {

                    if (link.startsWith("/")) {
                        link = "https://livetv904.me" + link;
                    } else {
                        link = LIVETV_URL + link;
                    }
                }

                boolean exists = false;

                for (Match m : result) {

                    if (m.url.equals(link)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {

                    result.add(
                            new Match(
                                    title,
                                    link
                            )
                    );
                }

                if (result.size() >= 60) {
                    break;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    private boolean isFootball(String text) {

        String[] words = {

                "футбол",
                "football",
                "soccer",

                "premier league",
                "премьер лига",

                "la liga",
                "примера",

                "bundesliga",
                "бундеслига",

                "serie a",
                "серия а",

                "ligue 1",

                "champions league",
                "лига чемпионов",

                "europa league",
                "лига европы",

                "conference league",

                "uefa",

                "fifa",

                "mls",

                "national league",
                "лига наций",

                "world cup",
                "чемпионат мира"
        };

        for (String word : words) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private void addMatch(Match match) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                24, 14, 18, 14
        );

        card.setBackground(
                bg(PANEL, 18)
        );

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        -1,
                        100
                );

        cp.setMargins(
                8, 7, 8, 7
        );

        list.addView(card, cp);

        LinearLayout info =
                new LinearLayout(this);

        info.setOrientation(
                LinearLayout.VERTICAL
        );

        info.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView name =
                label(
                        match.title,
                        18,
                        TEXT
                );

        name.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        info.addView(
                name,
                new LinearLayout.LayoutParams(
                        -1,
                        45
                )
        );

        TextView source =
                label(
                        "LiveTV event",
                        12,
                        MUTED
                );

        info.addView(
                source,
                new LinearLayout.LayoutParams(
                        -1,
                        28
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

        Button open =
                action("OPEN");

        open.setOnClickListener(
                v -> openEvent(match.url)
        );

        card.addView(
                open,
                new LinearLayout.LayoutParams(
                        125,
                        56
                )
        );
    }

    private void openEvent(String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        handler.removeCallbacks(
                refreshRunnable
        );

        handler.postDelayed(
                refreshRunnable,
                5 * 60 * 1000
        );
    }

    @Override
    protected void onPause() {

        super.onPause();

        handler.removeCallbacks(
                refreshRunnable
        );
    }

    private static class Match {

        String title;
        String url;

        Match(
                String title,
                String url
        ) {

            this.title = title;
            this.url = url;
        }
    }
}
```
