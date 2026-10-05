package com.worldgonemad.footballtv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String DEFAULT_API = "https://zerohazaarop.store/";
    private static final String UA = "Mozilla/5.0 (Linux; Android 10; Pixel 3 XL) AppleWebKit/537.36 Chrome/122.0.0.0 Mobile Safari/537.36";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Match> matches = new ArrayList<>();

    private ExoPlayer player;
    private PlayerView playerView;
    private LinearLayout list;
    private TextView status;
    private boolean playerScreen;

    private final int BG = Color.rgb(10,12,16);
    private final int PANEL = Color.rgb(20,23,29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160,168,180);
    private final int ACCENT = Color.rgb(55,125,255);
    private final int LIVE = Color.rgb(90,220,140);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showLoading("Loading AK47 football events...");
        loadMatches();
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!playerScreen) loadMatches();
                handler.postDelayed(this, 5 * 60 * 1000L);
            }
        }, 5 * 60 * 1000L);
    }

    private TextView label(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius);
        return d;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextColor(TEXT); b.setTextSize(14); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(ACCENT, 14)); b.setFocusable(true);
        return b;
    }

    private void showLoading(String message) {
        playerScreen = false;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);
        TextView title = label("MAX FOOTBALL ONLINE", 28, TEXT);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1,70));
        TextView msg = label(message,18,MUTED);
        msg.setGravity(Gravity.CENTER);
        root.addView(msg, new LinearLayout.LayoutParams(-1,60));
        setContentView(root);
    }

    private void showMatches() {
        playerScreen = false;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(36,16,36,8);
        TextView title = label("MAX FOOTBALL ONLINE",27,TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,65,1));
        status = label(matches.size()+" MATCHES",14,MUTED);
        status.setGravity(Gravity.CENTER);
        header.addView(status,new LinearLayout.LayoutParams(-2,65));
        root.addView(header);

        TextView sub = label("LIVE & UPCOMING",24,TEXT);
        sub.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        sub.setPadding(36,4,36,12);
        root.addView(sub,new LinearLayout.LayoutParams(-1,55));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(28,0,28,30);
        scroll.addView(list);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        renderMatches();
    }

    private void renderMatches() {
        if (list == null) return;
        list.removeAllViews();
        if (matches.isEmpty()) {
            TextView e = label("No football matches found.",18,MUTED);
            e.setGravity(Gravity.CENTER);
            list.addView(e,new LinearLayout.LayoutParams(-1,100));
            return;
        }
        for (final Match m : matches) {
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(20,10,14,10);
            card.setBackground(bg(PANEL,18));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,112);
            cp.setMargins(6,6,6,6);
            list.addView(card,cp);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            TextView league = label(m.league,12,MUTED);
            info.addView(league,new LinearLayout.LayoutParams(-1,25));
            TextView teams = label(m.home+"  —  "+m.away,18,TEXT);
            teams.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            info.addView(teams,new LinearLayout.LayoutParams(-1,40));
            TextView tm = label(m.live ? "LIVE  "+m.time : m.time,11,m.live?LIVE:MUTED);
            info.addView(tm,new LinearLayout.LayoutParams(-1,25));
            card.addView(info,new LinearLayout.LayoutParams(0,-1,1));

            Button watch = button("WATCH");
            watch.setOnClickListener(v -> resolveAndPlay(m));
            card.addView(watch,new LinearLayout.LayoutParams(130,58));
        }
        if (status != null) status.setText(matches.size()+" MATCHES");
    }

    private void loadMatches() {
        new Thread(() -> {
            String host = DEFAULT_API;
            try {
                String app = download(host + "app.txt");
                String decoded = decodeAk47(app);
                JSONObject cfg = tryObject(decoded);
                if (cfg != null && cfg.optString("api_url","").startsWith("http")) {
                    host = cfg.optString("api_url", DEFAULT_API);
                } else if (app != null && app.contains("api_url")) {
                    cfg = tryObject(app);
                    if (cfg != null && cfg.optString("api_url","").startsWith("http")) {
                        host = cfg.optString("api_url", DEFAULT_API);
                    }
                }
            } catch (Exception ignored) {}

            if (!host.endsWith("/")) host += "/";
            final String api = host;
            String body = download(api + "events.txt");
            String decoded = decodeAk47(body);
            ArrayList<Match> result = parseAk47Events(decoded);

            runOnUiThread(() -> {
                if (!result.isEmpty()) {
                    matches.clear();
                    matches.addAll(result);
                    showMatches();
                } else if (matches.isEmpty()) {
                    showError("Could not load AK47 football events.");
                } else if (status != null) {
                    status.setText("UPDATE FAILED");
                }
            });
        }).start();
    }

    private ArrayList<Match> parseAk47Events(String text) {
        ArrayList<Match> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return out;
        try {
            JSONArray root = new JSONArray(text);
            for (int i=0;i<root.length();i++) {
                JSONObject item = root.optJSONObject(i);
                if (item == null) continue;
                String raw = item.optString("event", "");
                JSONObject e;
                try { e = raw.trim().startsWith("{") ? new JSONObject(raw) : item; }
                catch (Exception ex) { e = item; }

                if (!e.optBoolean("visible", true)) continue;
                String category = e.optString("category","");
                if (!category.equalsIgnoreCase("football")) continue;

                String home = e.optString("teamAName","").trim();
                String away = e.optString("teamBName","").trim();
                String links = e.optString("links","");
                if (home.isEmpty() || away.isEmpty() || links.isEmpty()) continue;

                String date = e.optString("date","");
                String time = e.optString("time","");
                String endDate = e.optString("end_date","");
                String endTime = e.optString("end_time","");
                Match m = new Match();
                m.league = category;
                m.home = home;
                m.away = away;
                m.date = date;
                m.time = time;
                m.links = links;
                m.live = isLive(date,time,endDate,endTime);
                m.when = formatTime(date,time,m.live);
                if (isEnded(date,time,endDate,endTime)) continue;
                out.add(m);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private boolean isLive(String date,String time,String endDate,String endTime) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy|HH:mm:ss",Locale.US);
            Date start = f.parse(date+"|"+time);
            if (start == null) return false;
            Date end = null;
            if (!endDate.isEmpty() && !endTime.isEmpty()) end = f.parse(endDate+"|"+endTime);
            long now = System.currentTimeMillis();
            if (end != null) return now >= start.getTime() && now <= end.getTime();
            return now >= start.getTime() && now <= start.getTime()+3*60*60*1000L;
        } catch (Exception e) { return false; }
    }

    private boolean isEnded(String date,String time,String endDate,String endTime) {
        try {
            if (endDate.isEmpty() || endTime.isEmpty()) return false;
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy|HH:mm:ss",Locale.US);
            Date end = f.parse(endDate+"|"+endTime);
            return end != null && end.getTime() < System.currentTimeMillis();
        } catch (Exception e) { return false; }
    }

    private String formatTime(String date,String time,boolean live) {
        if (live) return "LIVE";
        if (date == null || date.isEmpty()) return time;
        return date+"  "+time;
    }

    private void resolveAndPlay(Match m) {
        if (m == null || m.links.isEmpty()) {
            showError("Stream link is unavailable.");
            return;
        }
        showPlayer(m,"Finding AK47 stream...");
        new Thread(() -> {
            String host = DEFAULT_API;
            String app = download(host+"app.txt");
            try {
                JSONObject cfg = tryObject(decodeAk47(app));
                if (cfg != null && cfg.optString("api_url","").startsWith("http")) host=cfg.optString("api_url",host);
            } catch (Exception ignored) {}
            if (!host.endsWith("/")) host += "/";

            String path = m.links.trim();
            String url = path.startsWith("http://") || path.startsWith("https://") ? path : host + path.replaceFirst("^/+","");
            String body = download(url);
            String decoded = decodeAk47(body);
            String stream = selectAk47Link(decoded);
            runOnUiThread(() -> {
                if (stream.isEmpty()) showPlayer(m,"AK47 returned no playable stream.");
                else playStream(m,stream);
            });
        }).start();
    }

    private String selectAk47Link(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        try {
            JSONArray a = new JSONArray(text);
            for (int i=0;i<a.length();i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null || !o.optBoolean("visible",true)) continue;
                String link = o.optString("link","").trim();
                if (link.isEmpty()) continue;
                return link;
            }
        } catch (Exception ignored) {}
        if (text.startsWith("http://") || text.startsWith("https://")) return text.trim();
        return "";
    }

    private JSONObject tryObject(String text) {
        try { return new JSONObject(text); } catch (Exception e) { return null; }
    }

    // Exact AK47Sports v1.6 decoder from y5.a.b(): custom alphabet -> Base64 -> UTF-8.
    private String decodeAk47(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.isEmpty()) return "";
        if (s.startsWith("[") || s.startsWith("{")) return s;
        final char[] standard = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ".toCharArray();
        final char[] custom = "fFgGjJkKaApPbBmMoOzZeEnNcCdDrRqQtTvVuUxXhHiIwWyYlLsS".toCharArray();
        char[] out = new char[s.length()];
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            int pos=-1;
            for (int j=0;j<custom.length;j++) if (custom[j]==c) {pos=j;break;}
            out[i]=pos>=0?standard[pos]:c;
        }
        try {
            return new String(Base64.decode(new String(out),Base64.DEFAULT),StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String download(String address) {
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)new URL(address).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent",UA);
            c.setRequestProperty("Accept","*/*");
            int code=c.getResponseCode();
            if (code<200 || code>=400) return "";
            BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();
            String line;
            while((line=r.readLine())!=null)b.append(line).append('\n');
            r.close();
            return b.toString();
        } catch(Exception e) { return ""; }
        finally { if(c!=null)c.disconnect(); }
    }

    private void showPlayer(Match m,String message) {
        releasePlayer();
        playerScreen=true;
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(20,8,20,8);
        TextView title=label(m.home+" — "+m.away,19,TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        top.addView(title,new LinearLayout.LayoutParams(0,56,1));
        Button back=button("BACK");
        top.addView(back,new LinearLayout.LayoutParams(120,52));
        root.addView(top);
        TextView msg=label(message,16,MUTED);
        msg.setGravity(Gravity.CENTER);
        root.addView(msg,new LinearLayout.LayoutParams(-1,55));
        playerView=new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        playerView.setKeepScreenOn(true);
        root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
        back.setOnClickListener(v->{releasePlayer();showMatches();});
        setContentView(root);
    }

    private void playStream(Match m,String raw) {
        String[] parts=raw.split("\\|",2);
        String url=parts[0].trim();
        if (url.isEmpty()) { showPlayer(m,"Empty AK47 stream URL."); return; }

        Map<String,String> headers=new HashMap<>();
        if (parts.length>1) {
            String meta=parts[1];
            for (String p:meta.split("&")) {
                int eq=p.indexOf('=');
                if(eq>0) {
                    String k=p.substring(0,eq).trim().toLowerCase(Locale.US);
                    String v=p.substring(eq+1).trim();
                    if(k.equals("user-agent")) headers.put("User-Agent",v);
                    else if(k.equals("referer") || k.equals("referrer")) headers.put("Referer",v);
                    else if(k.equals("origin")) headers.put("Origin",v);
                    else if(k.equals("cookie")) headers.put("Cookie",v);
                }
            }
        }

        releasePlayer();
        playerScreen=true;
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(20,8,20,8);
        TextView title=label(m.home+" — "+m.away,19,TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        top.addView(title,new LinearLayout.LayoutParams(0,56,1));
        Button back=button("BACK");
        top.addView(back,new LinearLayout.LayoutParams(120,52));
        root.addView(top);
        playerView=new PlayerView(this);
        playerView.setBackgroundColor(Color.BLACK);
        root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
        back.setOnClickListener(v->{releasePlayer();showMatches();});
        setContentView(root);

        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent(UA);
        if(!headers.isEmpty()) http.setDefaultRequestProperties(headers);
        DefaultMediaSourceFactory media=new DefaultMediaSourceFactory(http);
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(media).build();
        playerView.setPlayer(player);
        player.setMediaItem(new MediaItem.Builder().setUri(url).build());
        player.prepare();
        player.play();
    }

    private void releasePlayer() {
        if(playerView!=null) try{playerView.setPlayer(null);}catch(Exception ignored){}
        if(player!=null){try{player.stop();}catch(Exception ignored){} try{player.release();}catch(Exception ignored){} player=null;}
        playerView=null;
    }

    private void showError(String msg) {
        playerScreen=false;
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setBackgroundColor(BG);
        TextView title=label("MAX FOOTBALL ONLINE",28,TEXT);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,70));
        TextView e=label(msg,18,MUTED); e.setGravity(Gravity.CENTER);
        root.addView(e,new LinearLayout.LayoutParams(-1,70));
        Button retry=button("RETRY"); retry.setOnClickListener(v->{showLoading("Loading AK47 football events...");loadMatches();});
        root.addView(retry,new LinearLayout.LayoutParams(180,58));
        setContentView(root);
    }

    @Override protected void onStop() {
        super.onStop();
        if(playerScreen){releasePlayer();playerScreen=false;}
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if(playerScreen){releasePlayer();playerScreen=false;showMatches();}
        else super.onBackPressed();
    }

    private static class Match {
        String league,home,away,date,time,links,when;
        boolean live;
    }
}
