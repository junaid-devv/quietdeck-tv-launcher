package com.still.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Original vector scenery: no full-screen bitmap, animation or draw-time allocations. */
final class DeckBackground extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Path far = new Path(), near = new Path();
    private final int scene;
    private Shader sky, shade;
    DeckBackground(int scene) {
        this.scene = scene;
        far.moveTo(0, 610); far.lineTo(290, 490); far.lineTo(490, 580); far.lineTo(840, 375);
        far.lineTo(1170, 520); far.lineTo(1480, 340); far.lineTo(1920, 490);
        far.lineTo(1920, 1080); far.lineTo(0, 1080); far.close();
        near.moveTo(0, 720); near.cubicTo(400, 470, 540, 820, 980, 580);
        near.cubicTo(1400, 430, 1600, 600, 1920, 495);
        near.lineTo(1920, 1080); near.lineTo(0, 1080); near.close();
    }
    @Override protected void onBoundsChange(Rect bounds) {
        int top = scene == 1 ? 0xFF262134 : scene == 2 ? 0xFF080D16 : 0xFF0D2335;
        int bottom = scene == 1 ? 0xFF654555 : scene == 2 ? 0xFF142333 : 0xFF355D6A;
        sky = new LinearGradient(0, 0, 0, 1080, top, bottom, Shader.TileMode.CLAMP);
        shade = new LinearGradient(0, 120, 0, 1080, 0x00080E18, 0xFF080E18, Shader.TileMode.CLAMP);
    }
    @Override public void draw(Canvas canvas) {
        Rect b = getBounds(); canvas.save(); canvas.translate(b.left, b.top);
        canvas.scale(b.width() / 1920f, b.height() / 1080f);
        paint.setShader(sky); canvas.drawRect(0, 0, 1920, 1080, paint);
        paint.setShader(null); paint.setColor(scene == 1 ? 0xFFBDA5A1 : 0xFFB4DBD8);
        canvas.drawCircle(1590, 275, 42, paint);
        paint.setColor(scene == 1 ? 0xFF463A4D : 0xFF244350); canvas.drawPath(far, paint);
        paint.setColor(scene == 1 ? 0xFF272A3A : 0xFF142F3F); canvas.drawPath(near, paint);
        paint.setShader(shade); canvas.drawRect(0, 0, 1920, 1080, paint); canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
