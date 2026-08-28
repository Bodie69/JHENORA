package com.campus.campusmate.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal direct-labeled bar chart for small categorical datasets (2-4 bars).
 * Every bar carries its own value + category label, so identity never depends on color alone.
 */
public class BarChartView extends View {

    public static class Entry {
        final String label;
        final int value;
        final int color;

        public Entry(String label, int value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private List<Entry> entries = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float cornerRadius;
    private float barGap;
    private float labelGap;
    private float topPadding;

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cornerRadius = dp(4);
        barGap = dp(20);
        labelGap = dp(6);
        topPadding = dp(28);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(12));
        labelPaint.setColor(Color.parseColor("#64748B"));

        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(sp(14));
        valuePaint.setFakeBoldText(true);
        valuePaint.setColor(Color.parseColor("#1E293B"));

        baselinePaint.setColor(Color.parseColor("#E2E8F0"));
        baselinePaint.setStrokeWidth(dp(1));
    }

    public void setEntries(List<Entry> newEntries) {
        this.entries = newEntries != null ? newEntries : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(160);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (entries.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();

        float labelDescent = labelPaint.getFontMetrics().descent;
        float labelHeight = labelDescent - labelPaint.getFontMetrics().ascent;
        float baselineY = height - labelHeight - labelGap;
        float chartHeight = baselineY - topPadding;
        float labelBaselineY = height - labelDescent;

        int maxValue = 1;
        for (Entry e : entries) maxValue = Math.max(maxValue, e.value);

        int n = entries.size();
        float barWidth = (width - barGap * (n + 1)) / n;

        canvas.drawLine(0, baselineY, width, baselineY, baselinePaint);

        float x = barGap;
        for (Entry e : entries) {
            float barHeight = Math.max((e.value / (float) maxValue) * chartHeight, dp(2));
            float top = baselineY - barHeight;

            barPaint.setColor(e.color);
            RectF rect = new RectF(x, top, x + barWidth, baselineY);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint);
            // Square off the bottom so the bar anchors flush to the baseline.
            canvas.drawRect(x, Math.max(top, baselineY - cornerRadius), x + barWidth, baselineY, barPaint);

            float centerX = x + barWidth / 2f;
            canvas.drawText(String.valueOf(e.value), centerX, top - dp(8), valuePaint);
            canvas.drawText(e.label, centerX, labelBaselineY, labelPaint);

            x += barWidth + barGap;
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
