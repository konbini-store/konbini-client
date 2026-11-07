package io.github.konbini.market.ui;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

public class BaseActivity extends Activity {

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyEdgeToEdge();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyEdgeToEdge();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyEdgeToEdge();
    }

    private void applyEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= 35) {
            View content = findViewById(android.R.id.content);
            if (content != null) {
                content.setOnApplyWindowInsetsListener((v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return WindowInsets.CONSUMED;
                });
            }
        }
    }
}
