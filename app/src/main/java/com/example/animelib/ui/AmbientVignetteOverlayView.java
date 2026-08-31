package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Overlay View для фоновой подсветки (Ambilight).
 * Создаёт мягкое угасание подсветки у внешних краев экрана и под видеоплеером в портретном режиме.
 * Не рисует никаких чёрных плашек, рамок и полос.
 */
public class AmbientVignetteOverlayView extends View {

    private final Paint topEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint bottomEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint leftEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint rightEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final Paint portraitBottomFadePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint portraitFullErasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF videoBounds = new RectF();
    private boolean hasVideoBounds = false;

    private int lastWidth = 0;
    private int lastHeight = 0;

    private static final int[] ALPHA_FALLOFF_TO_EDGE = new int[]{
            0x00000000,
            0x1A000000,
            0x4D000000,
            0x80000000,
            0xB3000000,
            0xD8000000
    };

    private static final int[] ALPHA_FALLOFF_PORTRAIT_BOTTOM = new int[]{
            0x00000000,
            0x33000000,
            0x80000000,
            0xCC000000,
            0xFF000000
    };

    private static final float[] FALLOFF_STOPS = new float[]{
            0.0f, 0.20f, 0.45f, 0.70f, 0.88f, 1.0f
    };

    private static final float[] PORTRAIT_STOPS = new float[]{
            0.0f, 0.25f, 0.55f, 0.80f, 1.0f
    };

    public AmbientVignetteOverlayView(Context context) {
        this(context, null);
    }

    public AmbientVignetteOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmbientVignetteOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        PorterDuffXfermode dstOutMode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);

        topEdgePaint.setXfermode(dstOutMode);
        bottomEdgePaint.setXfermode(dstOutMode);
        leftEdgePaint.setXfermode(dstOutMode);
        rightEdgePaint.setXfermode(dstOutMode);

        portraitBottomFadePaint.setXfermode(dstOutMode);

        portraitFullErasePaint.setColor(0xFF000000);
        portraitFullErasePaint.setXfermode(dstOutMode);
        portraitFullErasePaint.setStyle(Paint.Style.FILL);

        setLayerType(LAYER_TYPE_NONE, null);
    }

    public void setVideoBounds(float left, float top, float right, float bottom) {
        if (!hasVideoBounds || videoBounds.left != left || videoBounds.top != top ||
                videoBounds.right != right || videoBounds.bottom != bottom) {
            videoBounds.set(left, top, right, bottom);
            hasVideoBounds = true;
            rebuildShaders();
            invalidate();
        }
    }

    public void clearCustomVideoBounds() {
        if (hasVideoBounds) {
            hasVideoBounds = false;
            rebuildShaders();
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;
            rebuildShaders();
        }
    }

    private void rebuildShaders() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (hasVideoBounds) {
            float vBottom = videoBounds.bottom;
            float fadeDistance = dpToPx(100);

            portraitBottomFadePaint.setShader(new LinearGradient(
                    0, vBottom, 0, vBottom + fadeDistance,
                    ALPHA_FALLOFF_PORTRAIT_BOTTOM, PORTRAIT_STOPS, Shader.TileMode.CLAMP
            ));
        } else {
            float edgeDepth = dpToPx(20);

            topEdgePaint.setShader(new LinearGradient(
                    0, edgeDepth, 0, 0,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            bottomEdgePaint.setShader(new LinearGradient(
                    0, h - edgeDepth, 0, h,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            leftEdgePaint.setShader(new LinearGradient(
                    edgeDepth, 0, 0, 0,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));

            rightEdgePaint.setShader(new LinearGradient(
                    w - edgeDepth, 0, w, 0,
                    ALPHA_FALLOFF_TO_EDGE, FALLOFF_STOPS, Shader.TileMode.CLAMP
            ));
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int saveCount = canvas.saveLayer(0, 0, w, h, null);

        if (hasVideoBounds) {
            float vBottom = videoBounds.bottom;
            float fadeDistance = dpToPx(100);

            if (vBottom < h) {
                canvas.drawRect(0, vBottom, w, Math.min(h, vBottom + fadeDistance), portraitBottomFadePaint);
                if (vBottom + fadeDistance < h) {
                    canvas.drawRect(0, vBottom + fadeDistance, w, h, portraitFullErasePaint);
                }
            }
        } else {
            float edgeDepth = dpToPx(20);

            canvas.drawRect(0, 0, w, edgeDepth, topEdgePaint);
            canvas.drawRect(0, h - edgeDepth, w, h, bottomEdgePaint);
            canvas.drawRect(0, 0, edgeDepth, h, leftEdgePaint);
            canvas.drawRect(w - edgeDepth, 0, w, h, rightEdgePaint);
        }

        canvas.restoreToCount(saveCount);
    }
}
