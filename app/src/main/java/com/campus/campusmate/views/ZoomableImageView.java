package com.campus.campusmate.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * An {@link AppCompatImageView} that supports pinch-to-zoom, panning while zoomed in, and
 * double-tap to toggle between fit and 3x zoom. Used for the full-screen announcement/event
 * image viewer, where the source image may be much larger than the screen.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;
    private static final float DOUBLE_TAP_SCALE = 3f;

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private float baseScale = 1f;
    private float currentScale = 1f;
    private float minScale = MIN_SCALE;
    private float maxScale = MAX_SCALE;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean isDragging;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetToFit();
    }

    @Override
    public void setImageDrawable(Drawable dr) {
        super.setImageDrawable(dr);
        resetToFit();
    }

    /** Resets the image to fully visible and centered ("fit center"), the zoomed-out baseline. */
    private void resetToFit() {
        Drawable d = getDrawable();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (d == null || viewWidth == 0 || viewHeight == 0) return;

        int drawableWidth = d.getIntrinsicWidth();
        int drawableHeight = d.getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) return;

        baseScale = Math.min((float) viewWidth / drawableWidth, (float) viewHeight / drawableHeight);
        float dx = (viewWidth - drawableWidth * baseScale) / 2f;
        float dy = (viewHeight - drawableHeight * baseScale) / 2f;

        matrix.reset();
        matrix.postScale(baseScale, baseScale);
        matrix.postTranslate(dx, dy);
        currentScale = 1f;
        minScale = MIN_SCALE;
        maxScale = MAX_SCALE;
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                activePointerId = event.getPointerId(0);
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (scaleDetector.isInProgress()) break;
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex == -1) break;
                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;
                if (currentScale > MIN_SCALE) {
                    isDragging = true;
                    translateWithinBounds(dx, dy);
                }
                lastTouchX = x;
                lastTouchY = y;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newIndex);
                    lastTouchY = event.getY(newIndex);
                    activePointerId = event.getPointerId(newIndex);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                isDragging = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void translateWithinBounds(float dx, float dy) {
        RectF bounds = getScaledDrawableBounds();
        if (bounds == null) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (bounds.width() > viewWidth) {
            if (bounds.left + dx > 0) dx = -bounds.left;
            if (bounds.right + dx < viewWidth) dx = viewWidth - bounds.right;
        } else {
            dx = 0;
        }
        if (bounds.height() > viewHeight) {
            if (bounds.top + dy > 0) dy = -bounds.top;
            if (bounds.bottom + dy < viewHeight) dy = viewHeight - bounds.bottom;
        } else {
            dy = 0;
        }

        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    private RectF getScaledDrawableBounds() {
        Drawable d = getDrawable();
        if (d == null) return null;
        RectF rect = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(rect);
        return rect;
    }

    private void scaleAndCenterAt(float scaleFactor, float focusX, float focusY) {
        matrix.getValues(matrixValues);
        float scale = matrixValues[Matrix.MSCALE_X];
        float targetScale = scale * scaleFactor;
        float clampedTotal = clamp(targetScale / baseScale, minScale, maxScale);
        float appliedFactor = (clampedTotal * baseScale) / scale;

        matrix.postScale(appliedFactor, appliedFactor, focusX, focusY);
        currentScale = clampedTotal;
        constrainAfterScale();
        setImageMatrix(matrix);
    }

    private void constrainAfterScale() {
        RectF bounds = getScaledDrawableBounds();
        if (bounds == null) return;
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        float dx = 0;
        float dy = 0;
        if (bounds.width() <= viewWidth) {
            dx = viewWidth / 2f - bounds.centerX();
        } else if (bounds.left > 0) {
            dx = -bounds.left;
        } else if (bounds.right < viewWidth) {
            dx = viewWidth - bounds.right;
        }
        if (bounds.height() <= viewHeight) {
            dy = viewHeight / 2f - bounds.centerY();
        } else if (bounds.top > 0) {
            dy = -bounds.top;
        } else if (bounds.bottom < viewHeight) {
            dy = viewHeight - bounds.bottom;
        }
        matrix.postTranslate(dx, dy);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleAndCenterAt(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > MIN_SCALE + 0.05f) {
                resetToFit();
            } else {
                scaleAndCenterAt(DOUBLE_TAP_SCALE, e.getX(), e.getY());
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return performClick();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
