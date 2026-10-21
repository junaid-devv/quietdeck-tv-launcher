package com.still.launcher;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original mountain scene, without a full-screen bitmap or draw-time allocations. */
final class DeckBackground extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Path far, middle, near;
    private final int scene;
    private final Shader sky, glow, leftShade, bottomShade;
    DeckBackground(int scene) {
        this.scene = scene;
        int top = scene == 1 ? 0xFF2B2339 : scene == 2 ? 0xFF101822 : 0xFF142F48;
        int horizon = scene == 1 ? 0xFF9F746C : scene == 2 ? 0xFF253846 : 0xFF628DAB;
        sky = new LinearGradient(0,0,0,1080,new int[]{top,horizon,0xFF112334,0xFF0B1420},
                new float[]{0,.35f,.68f,1},Shader.TileMode.CLAMP);
        glow = new RadialGradient(1430,282,490,scene == 1 ? 0x55F7C89C : scene == 2 ? 0x194F7590 : 0x55C7E1EF,
                0x00000000,Shader.TileMode.CLAMP);
        far = mountain(new float[]{0,468,130,433,252,447,415,371,562,405,723,347,900,394,1115,310,1238,348,1381,303,1540,365,1712,307,1920,381});
        middle = mountain(new float[]{0,522,197,488,354,503,570,445,748,480,981,410,1100,428,1287,389,1456,441,1675,393,1920,430});
        near = mountain(new float[]{0,540,220,559,440,527,689,566,931,520,1185,499,1380,524,1611,478,1794,493,1920,470});
        leftShade = new LinearGradient(0,0,1120,0,new int[]{0xB8081726,0x500A1928,0x000A1928},
                new float[]{0,.65f,1},Shader.TileMode.CLAMP);
        bottomShade = new LinearGradient(0,450,0,1080,new int[]{0x000B1420,0xF00B1420,0xFF0B1420},
                new float[]{0,.47f,1},Shader.TileMode.CLAMP);
    }
    private static Path mountain(float[] points) {
        Path path=new Path(); path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);
        path.lineTo(1920,1080); path.lineTo(0,1080); path.close(); return path;
    }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds(); canvas.save(); canvas.translate(b.left,b.top);
        canvas.scale(b.width()/1920f,b.height()/1080f);
        paint.setShader(sky); canvas.drawRect(0,0,1920,1080,paint);
        paint.setShader(glow); canvas.drawRect(700,0,1920,800,paint);
        paint.setShader(null); paint.setColor(scene==1 ? 0xFFDDC1A2 : scene==2 ? 0xFF889AA7 : 0xFFBFD5DF);
        canvas.drawCircle(1430,251,scene==2 ? 28 : 39,paint);
        paint.setColor(scene==1 ? 0xFF5D586B : 0xFF42617C); canvas.drawPath(far,paint);
        paint.setColor(scene==1 ? 0xFF393B51 : 0xFF29465F); canvas.drawPath(middle,paint);
        paint.setColor(0xFF192E43); canvas.drawPath(near,paint);
        paint.setShader(leftShade); canvas.drawRect(0,0,1920,1080,paint);
        paint.setShader(bottomShade); canvas.drawRect(0,450,1920,1080,paint); canvas.restore();
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
