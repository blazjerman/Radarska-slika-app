package com.example.vremenskaslika;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.DrawableImageViewTarget;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private ImageView background;
    private ImageView radarImageView;


    private TextView utcTimeTextView;
    private TextView showStill;


    private Handler mainHandler;

    private final String radarImageUrl = "https://meteo.arso.gov.si/uploads/probase/www/observ/radar/si0-rm-anim.gif";
    private final String backgroundImage = "https://meteo.arso.gov.si/uploads/probase/www/observ/radar/si0-rm.gif";
    private final int gifLength = 5750;//In milliseconds
    private final int updateAfter = 5;


    private final int fastRefresh = 10;//In milliseconds


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        setContentView(R.layout.activity_main);

        //Setup thread  for updates
        Thread setUpdates = new Thread(new Runnable() {
            @Override
            public void run() {setupUpdates();}}
        );


        setUpdates.start();


        super.onCreate(savedInstanceState);

        // Enable immersive fullscreen mode
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

        Configuration config = getResources().getConfiguration();

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getWindow().getDecorView().setSystemUiVisibility(flags);


    }



    @SuppressLint("ClickableViewAccessibility")
    private void setupUpdates(){

        utcTimeTextView = findViewById(R.id.utcTimeTextView);
        showStill = findViewById(R.id.showStill);

        radarImageView = findViewById(R.id.radarImageView);
        background = findViewById(R.id.background);


        mainHandler = new Handler(Looper.getMainLooper());


        showStill.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        showStill.setTextColor(0xFF000000);//Set text to black
                        radarImageView.setVisibility(View.GONE);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        showStill.setTextColor(0xFFFFFFFF);//Set text back to white
                        radarImageView.setVisibility(View.VISIBLE);
                        break;
                }
                return true;
            }
        });

        // Start UTC updating
        updateUTCTime();

        // Start image updating
        refreshImage(fastRefresh);
    }



    @SuppressLint("SetTextI18n")
    private void updateUTCTime() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(new Date());

        // Update the TextView with the new UTC time
        utcTimeTextView.setText("UTC čas:\n" + utcTime);

        // Schedule the update again after 1 second
        utcTimeTextView.postDelayed(this::updateUTCTime, 1000);
    }


    private void refreshImage(long refresh) {
        mainHandler.postDelayed(() -> {

            if (isDestroyed()) return;

            if (!isNetworkOnline()) {
                refreshImage(fastRefresh);
                return;
            }

            loadImageFromUrl(backgroundImage, background);

            //Delayed second image
            mainHandler.postDelayed(() -> {
                if (isDestroyed()) return;
                loadImageFromUrl(radarImageUrl, radarImageView);
            }, fastRefresh * fastRefresh);

            refreshImage((long) gifLength * updateAfter);

        }, refresh);
    }

    private void loadImageFromUrl(String imageUrl, ImageView image) {

        Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable caching
                .skipMemoryCache(true) // Skip memory cache as well
                .into(new DrawableImageViewTarget(image));
    }

    public boolean isNetworkOnline() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null); // Remove callbacks to prevent leaks
    }

    public void openLink(View view) {
        String url = "https://meteo.arso.gov.si/met/sl/weather/observ/radar/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}


