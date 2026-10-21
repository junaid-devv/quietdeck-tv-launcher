package com.still.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Byte-bounded memory cache; no disk cache, original-size bitmaps or retained Activity. */
final class ArtworkLoader implements AutoCloseable {
    private final PackageManager pm;
    private final LruCache<String, Bitmap> cache;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(24), new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile boolean closed;
    private final int width, height;
    ArtworkLoader(Context context, boolean lowRam) {
        pm = context.getApplicationContext().getPackageManager();
        width = lowRam ? 240 : 320; height = width * 9 / 16;
        cache = new LruCache<String, Bitmap>((lowRam ? 2 : 4) * 1024 * 1024) {
            @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
        };
    }
    void bind(AppEntry app, ImageView view) {
        view.setTag(app.id);
        Bitmap hit = cache.get(app.id);
        if (hit != null) { view.setImageBitmap(hit); return; }
        view.setImageDrawable(null);
        WeakReference<ImageView> target = new WeakReference<>(view);
        if (closed) return;
        worker.execute(() -> {
            ImageView current = target.get();
            if (closed || current == null) return;
            Bitmap bitmap = cache.get(app.id);
            try {
                if (bitmap == null) {
                    Drawable source = app.info.activityInfo.loadBanner(pm);
                    if (source == null) source = app.info.activityInfo.applicationInfo.loadBanner(pm);
                    boolean banner = source != null;
                    if (source == null) source = app.info.loadIcon(pm);
                    if (source == null || closed) return;
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    int sw = Math.max(1, source.getIntrinsicWidth()), sh = Math.max(1, source.getIntrinsicHeight());
                    int bw = banner ? width : height * 3 / 4, bh = banner ? height : height * 3 / 4;
                    int[] fit = AspectFit.inside(sw, sh, bw, bh);
                    int x = (width - bw) / 2, y = (height - bh) / 2;
                    source.setBounds(x + fit[0], y + fit[1], x + fit[2], y + fit[3]);
                    source.draw(new Canvas(bitmap));
                    if (!closed) cache.put(app.id, bitmap);
                }
                final Bitmap ready = bitmap;
                if (!closed) main.post(() -> {
                    ImageView image = target.get();
                    if (!closed && image != null && app.id.equals(image.getTag())) image.setImageBitmap(ready);
                });
            } catch (RuntimeException | OutOfMemoryError malformedArtwork) {
                // A bad app banner must not take down the user's Home screen.
                cache.evictAll();
            }
        });
    }
    void clear() { cache.evictAll(); worker.getQueue().clear(); }
    void trim(boolean severe) { if (severe) clear(); else cache.trimToSize(cache.maxSize() / 2); }
    int bytes() { return cache.size(); }
    @Override public void close() { closed = true; worker.shutdownNow(); main.removeCallbacksAndMessages(null); cache.evictAll(); }
}
