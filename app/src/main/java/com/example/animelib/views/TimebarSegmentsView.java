package com.example.animelib.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.animelib.models.EpisodeResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Вью для отображения сегментов (оппенинги, эндинги и т.д.) прямо на таймбаре плеера
 */
public class TimebarSegmentsView extends View {

    private final List<EpisodeResponse.TimecodeData> timecodes = new ArrayList<>();
    private long durationMs = 0;

    private Paint segmentPaint;
    private Paint gapPaint;
    private RectF rectF;

    public TimebarSegmentsView(Context context) {
        super(context);
        init();
    }

    public TimebarSegmentsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimebarSegmentsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gapPaint.setColor(Color.parseColor("#0F0F0F"));
        rectF = new RectF();
    }

    public void setTimecodes(List<EpisodeResponse.TimecodeData> timecodes, long durationMs) {
        this.timecodes.clear();
        if (timecodes != null) {
            this.timecodes.addAll(timecodes);
        }
        this.durationMs = durationMs;
        invalidate();
    }

    public void setDuration(long durationMs) {
        if (this.durationMs != durationMs) {
            this.durationMs = durationMs;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (durationMs <= 0 || timecodes.isEmpty()) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float barHeight = 4.5f * density; // Слегка утолщенная полоска под таймбаром
        float centerY = height / 2f;
        float top = centerY - barHeight / 2f;
        float bottom = centerY + barHeight / 2f;
        float cornerRadius = 2f * density;
        float gapWidth = 2.5f * density;

        for (EpisodeResponse.TimecodeData tc : timecodes) {
            long fromMs = tc.getFrom() * 1000L;
            long toMs = tc.getTo() * 1000L;

            if (fromMs >= durationMs || toMs <= fromMs) continue;

            float startX = (float) fromMs / durationMs * width;
            float endX = (float) Math.min(toMs, durationMs) / durationMs * width;

            if (endX <= startX) continue;

            int color = getSegmentColor(tc.getType());
            segmentPaint.setColor(color);

            rectF.set(startX, top, endX, bottom);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, segmentPaint);

            // Отрисовка разделителей (гапов) на границах сегмента
            if (startX > 0) {
                canvas.drawRect(startX - gapWidth / 2f, top - 2f * density, startX + gapWidth / 2f, bottom + 2f * density, gapPaint);
            }
            if (endX < width) {
                canvas.drawRect(endX - gapWidth / 2f, top - 2f * density, endX + gapWidth / 2f, bottom + 2f * density, gapPaint);
            }
        }
    }

    private int getSegmentColor(String type) {
        if (type == null) return Color.parseColor("#A855F7");
        switch (type.toLowerCase()) {
            case "opening":
                return Color.parseColor("#9333EA"); // Фиолетовый акцент
            case "ending":
                return Color.parseColor("#EA580C"); // Оранжевый
            case "splashscreen":
                return Color.parseColor("#0D9488"); // Бирюзовый
            case "compilation":
                return Color.parseColor("#DB2777"); // Розовый
            default:
                return Color.parseColor("#7C3AED"); // Индиго
        }
    }
}
