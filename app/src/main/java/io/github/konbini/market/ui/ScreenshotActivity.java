package io.github.konbini.market.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import io.github.konbini.market.ui.tasks.LoadScreenshotFullscreenAsyncTask;

public class ScreenshotActivity extends Activity {
    public GestureImageView gestureImageView;
    public ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xFF000000);

        gestureImageView = new GestureImageView(this);
        gestureImageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(gestureImageView);

        progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams pbLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        pbLp.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(pbLp);
        root.addView(progressBar);

        setContentView(root);

        String url = getIntent().getStringExtra("screenshot_url");
        if (url == null || url.length() == 0) {
            Toast.makeText(this, "Invalid screenshot URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loadScreenshotFullscreen(url);
    }

    @SuppressWarnings("deprecation")
    private void loadScreenshotFullscreen(final String url) {
        new LoadScreenshotFullscreenAsyncTask(this, url, 2048, 2048).execute();
    }
}
