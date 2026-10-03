package io.github.konbini.market.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class GestureImageView extends ImageView {
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private float minScale = 1f;
    private float saveScale = 1f;
    private int origWidth, origHeight;
    private int viewWidth, viewHeight;
    
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    public GestureImageView(Context context) {
        super(context);
        init(context);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context ctx) {
        super.setScaleType(ScaleType.MATRIX);
        mScaleDetector = new ScaleGestureDetector(ctx, new ScaleListener());
        mGestureDetector = new GestureDetector(ctx, new GestureListener());
        
        setOnTouchListener((v, event) -> {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    savedMatrix.set(matrix);
                    mode = ZOOM;
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;
            }
            v.performClick();
            return true;
        });
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (bm != null) {
            origWidth = bm.getWidth();
            origHeight = bm.getHeight();
            fitScreen();
        }
    }

    private void fitScreen() {
        if (viewWidth <= 0 || viewHeight <= 0 || origWidth <= 0 || origHeight <= 0) return;

        float scaleX = (float) viewWidth / origWidth;
        float scaleY = (float) viewHeight / origHeight;
        float scale = Math.min(scaleX, scaleY);

        matrix.setScale(scale, scale);
        
        float redundantX = (viewWidth - (scale * origWidth)) / 2;
        float redundantY = (viewHeight - (scale * origHeight)) / 2;
        matrix.postTranslate(redundantX, redundantY);

        saveScale = scale;
        minScale = scale;
        setImageMatrix(matrix);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (getDrawable() != null && getDrawable() instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) getDrawable()).getBitmap();
            if (bmp != null) {
                origWidth = bmp.getWidth();
                origHeight = bmp.getHeight();
                fitScreen();
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        @SuppressWarnings("NullableProblems")
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= scaleFactor;
            float maxScale = 5f;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                scaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                scaleFactor = minScale / origScale;
            }

            float width = origWidth * saveScale;
            float height = origHeight * saveScale;

            if (width < viewWidth || height < viewHeight) {
                matrix.postScale(scaleFactor, scaleFactor, (float) viewWidth / 2, (float) viewHeight / 2);
            } else {
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            }

            fixTranslation();
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        @SuppressWarnings("NullableProblems")
        public boolean onDown(MotionEvent ignored) {
            mode = DRAG;
            savedMatrix.set(matrix);
            return true;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mode == ZOOM) return false;
            
            float deltaX = -distanceX;
            float deltaY = -distanceY;

            matrix.postTranslate(deltaX, deltaY);
            fixTranslation();
            setImageMatrix(matrix);
            return true;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public boolean onDoubleTap(MotionEvent e) {
            if (saveScale == minScale) {
                saveScale = minScale * 2f;
                matrix.postScale(2f, 2f, e.getX(), e.getY());
            } else {
                fitScreen();
            }
            fixTranslation();
            setImageMatrix(matrix);
            return true;
        }
    }

    private void fixTranslation() {
        float[] values = new float[9];
        matrix.getValues(values);
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        float fixTransX = getFixTranslation(transX, viewWidth, origWidth * saveScale);
        float fixTransY = getFixTranslation(transY, viewHeight, origHeight * saveScale);

        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTranslation(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans) return -trans + minTrans;
        if (trans > maxTrans) return -trans + maxTrans;
        return 0;
    }
}
