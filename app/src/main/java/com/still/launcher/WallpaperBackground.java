package com.still.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Center-crop photos without stretching or allocating another bitmap. */
final class WallpaperBackground extends Drawable {
    private final Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect source = new Rect();
    WallpaperBackground(Bitmap bitmap) { this.bitmap = bitmap; }
    @Override protected void onBoundsChange(Rect bounds) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        float scale = Math.max((float) bounds.width() / bitmap.getWidth(), (float) bounds.height() / bitmap.getHeight());
        int width = Math.max(1, Math.round(bounds.width() / scale));
        int height = Math.max(1, Math.round(bounds.height() / scale));
        int left = (bitmap.getWidth() - width) / 2, top = (bitmap.getHeight() - height) / 2;
        source.set(left, top, left + width, top + height);
    }
    @Override public void draw(Canvas canvas) { canvas.drawBitmap(bitmap, source, getBounds(), paint); }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
