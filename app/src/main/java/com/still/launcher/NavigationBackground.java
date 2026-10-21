package com.still.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Quiet selected underline; the remote focus remains a separate visible pill. */
final class NavigationBackground extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private boolean focused, selected;
    NavigationBackground(float density) { this.density = density; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] states) {
        focused = false; selected = false;
        for (int state : states) {
            focused |= state == android.R.attr.state_focused || state == android.R.attr.state_pressed;
            selected |= state == android.R.attr.state_selected;
        }
        invalidateSelf(); return true;
    }
    @Override public void draw(Canvas canvas) {
        float w = getBounds().width(), h = getBounds().height();
        if (focused) {
            paint.setColor(0xFFEAF2F7);
            canvas.drawRoundRect(0, 3 * density, w, h - 3 * density, 18 * density, 18 * density, paint);
        } else if (selected) {
            paint.setColor(0xFFD4E4EE);
            canvas.drawRoundRect(w / 2 - 7 * density, h - 3 * density, w / 2 + 7 * density, h - 1.5f * density,
                    density, density, paint);
        }
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
