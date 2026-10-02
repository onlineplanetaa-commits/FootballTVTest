package com.worldgonemad.footballtv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

    private final int BG = Color.rgb(10, 12, 16);
    private final int PANEL = Color.rgb(20, 23, 29);
    private final int TEXT = Color.WHITE;
    private final int MUTED = Color.rgb(160, 168, 180);
    private final int ACCENT = Color.rgb(55, 125, 255);

    /*
     * ТЕСТОВЫЙ HLS-ПОТОК.
     *
     * Это только для проверки встроенного плеера.
     * Позже сюда подставляются разрешённые HLS-источники
     * конкретных матчей.
     */
    private static final String TEST_HLS =
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showMatches();
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

    private Button action(String caption) {

        Button b = new Button(this);

        b.setText(caption);
        b.setTextColor(TEXT);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setFocusableInTouchMode(false);
        b.setBackground(bg(ACCENT, 14));

        b.setPadding(18, 2, 18, 2);

        b.setOnFocusChangeListener(
                (v, hasFocus) ->
                        v.setAlpha(hasFocus ? 1f : 0.82f)
        );

        return b;
    }

    private void showMatches() {

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

        TextView status = label(
                "LIVE FOOTBALL",
                14,
                MUTED
        );

        status.setGravity(
                Gravity.CENTER_VERTICAL | Gravity.RIGHT
        );

        header.addView(
                status,
                new LinearLayout.LayoutParams(
                        -2,
                        65
                )
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

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(34, 0, 34, 30);

        LinearLayout list = new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        scroll.addView(list);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        /*
         * Пока это тестовый список.
         * Главное сейчас — проверить, что выбор матча
         * открывает ВСТРОЕННЫЙ плеер, а не LiveTV904.
         */

        addMatch(
                list,
                "LIVE",
                "Premier League",
                "Manchester United",
                "Arsenal"
        );

        addMatch(
                list,
                "LIVE",
                "La Liga",
                "Real Madrid",
                "Barcelona"
        );

        addMatch(
                list,
                "UPCOMING",
                "Champions League",
                "Bayern Munich",
                "Inter Milan"
        );

        addMatch(
                list,
                "UPCOMING",
                "Serie A",
                "Juventus",
                "AC Milan"
        );

        addMatch(
                list,
                "UPCOMING",
                "Bundesliga",
                "Dortmund",
                "Bayer Leverkusen"
        );

        addMatch(
                list,
                "UPCOMING",
                "Ligue 1",
                "PSG",
                "Marseille"
        );

        setContentView(root);
    }

    private void addMatch(
            LinearLayout list,
            String status,
            String league,
            String home,
            String away
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

        LinearLayout info =
                new LinearLayout(this);

        info.setOrientation(
                LinearLayout.VERTICAL
        );

        info.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView leagueText =
                label(
                        league,
                        12,
                        MUTED
                );

        info.addView(
                leagueText,
                new LinearLayout.LayoutParams(
                        -1,
                        25
                )
        );

        TextView teams =
                label(
                        home + "  —  " + away,
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

        TextView statusText =
                label(
                        status,
                        11,
                        status.equals("LIVE")
                                ? Color.rgb(90, 220, 140)
                                : MUTED
                );

        info.addView(
                statusText,
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
                v -> playMatch(
                        home + " — " + away
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

    private void playMatch(String matchName) {

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

        playerView.setUseController(true);

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

        playerView.setPlayer(player);

        MediaItem mediaItem =
                MediaItem.fromUri(TEST_HLS);

        player.setMediaItem(mediaItem);

        player.prepare();

        player.play();
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
    public void onBackPressed() {

        if (player != null) {

            releasePlayer();

            showMatches();

        } else {

            super.onBackPressed();
        }
    }
}
