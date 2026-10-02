package com.worldgonemad.footballtv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private final String TEST_URL = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int PANEL2 = Color.rgb(28, 32, 40);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);

    @Override public void onCreate(Bundle b) { super.onCreate(b); showMatches(); }

    private TextView label(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextColor(color); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private Button action(String caption) {
        Button b = new Button(this); b.setText(caption); b.setTextColor(TEXT); b.setTextSize(15); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setGravity(Gravity.CENTER); b.setFocusable(true); b.setFocusableInTouchMode(false);
        b.setBackground(bg(ACCENT, 14));
        b.setPadding(24, 4, 24, 4);
        b.setOnFocusChangeListener((v, has) -> v.setAlpha(has ? 1f : .82f));
        return b;
    }

    private void showMatches() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(42, 24, 42, 18);
        TextView logo = label("FOOTBALL TV", 28, TEXT); logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD); header.addView(logo, new LinearLayout.LayoutParams(0, 70, 1));
        TextView date = label("TODAY  •  2 OCTOBER", 15, MUTED); date.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT); header.addView(date, new LinearLayout.LayoutParams(-2, 70));
        root.addView(header);

        TextView title = label("LIVE & UPCOMING", 25, TEXT); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setPadding(42, 6, 42, 16); root.addView(title, new LinearLayout.LayoutParams(-1, 58));

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setPadding(34, 0, 34, 30);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);

        addMatch(list, "18:00", "Premier League", "Manchester United", "Arsenal", "LIVE", true);
        addMatch(list, "18:30", "La Liga", "Real Madrid", "Barcelona", "UPCOMING", false);
        addMatch(list, "19:00", "Champions League", "Bayern Munich", "Inter Milan", "UPCOMING", false);
        addMatch(list, "20:00", "Serie A", "Juventus", "AC Milan", "UPCOMING", false);
        addMatch(list, "21:00", "Bundesliga", "Dortmund", "Bayer Leverkusen", "UPCOMING", false);
        addMatch(list, "22:00", "Ligue 1", "PSG", "Marseille", "UPCOMING", false);

        scroll.addView(list); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void addMatch(LinearLayout list, String time, String league, String home, String away, String status, boolean playable) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(24, 14, 18, 14); card.setBackground(bg(PANEL, 18));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 112); cp.setMargins(8, 7, 8, 7); list.addView(card, cp);

        LinearLayout timeBox = new LinearLayout(this); timeBox.setOrientation(LinearLayout.VERTICAL); timeBox.setGravity(Gravity.CENTER); TextView tm = label(time, 21, TEXT); tm.setTypeface(Typeface.DEFAULT, Typeface.BOLD); TextView st = label(status, 11, playable ? Color.rgb(90, 220, 140) : MUTED); st.setGravity(Gravity.CENTER); timeBox.addView(tm, new LinearLayout.LayoutParams(-1, 42)); timeBox.addView(st, new LinearLayout.LayoutParams(-1, 30)); card.addView(timeBox, new LinearLayout.LayoutParams(130, -1));

        LinearLayout teams = new LinearLayout(this); teams.setOrientation(LinearLayout.VERTICAL); teams.setGravity(Gravity.CENTER_VERTICAL); TextView lg = label(league, 12, MUTED); TextView h = label(home, 19, TEXT); h.setTypeface(Typeface.DEFAULT, Typeface.BOLD); TextView a = label(away, 19, TEXT); a.setTypeface(Typeface.DEFAULT, Typeface.BOLD); teams.addView(lg, new LinearLayout.LayoutParams(-1, 28)); teams.addView(h, new LinearLayout.LayoutParams(-1, 30)); teams.addView(a, new LinearLayout.LayoutParams(-1, 30)); card.addView(teams, new LinearLayout.LayoutParams(0, -1, 1));

        Button watch = action(playable ? "WATCH" : "SOON"); watch.setEnabled(playable); watch.setAlpha(playable ? 1f : .45f); watch.setOnClickListener(v -> playTest(home + " — " + away)); card.addView(watch, new LinearLayout.LayoutParams(130, 58));
        if (playable) watch.requestFocus();
    }

    private void playTest(String matchName) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(28, 8, 20, 8); TextView title = label(matchName, 20, TEXT); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); top.addView(title, new LinearLayout.LayoutParams(0, 56, 1)); Button back = action("BACK"); top.addView(back, new LinearLayout.LayoutParams(120, 52)); root.addView(top);
        playerView = new PlayerView(this); playerView.setUseController(true); root.addView(playerView, new LinearLayout.LayoutParams(-1, 0, 1));
        back.setOnClickListener(v -> { releasePlayer(); showMatches(); });
        setContentView(root);
        player = new ExoPlayer.Builder(this).build(); playerView.setPlayer(player); player.setMediaItem(MediaItem.fromUri(TEST_URL)); player.prepare(); player.play();
    }

    private void releasePlayer() { if (player != null) { player.release(); player = null; } }
    @Override protected void onStop() { super.onStop(); if (isFinishing()) releasePlayer(); }
    @Override public void onBackPressed() { if (player != null) { releasePlayer(); showMatches(); } else super.onBackPressed(); }
}
