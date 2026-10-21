package com.still.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Metadata only: app discovery never decodes icons or runs on the UI thread. */
final class AppRepository implements AutoCloseable {
    interface Callback { void loaded(List<AppEntry> entries, boolean failed); }
    private final Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    private boolean loading, again;
    AppRepository(Context context) { this.context = context.getApplicationContext(); }
    void load(Callback callback) {
        if (closed) return;
        if (loading) { again = true; return; }
        loading = true;
        worker.execute(() -> {
            List<AppEntry> result = new ArrayList<>();
            boolean failed = false;
            try {
                PackageManager pm = context.getPackageManager();
                Set<String> homes = new HashSet<>();
                for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME), 0)) homes.add(r.activityInfo.packageName);
                LinkedHashMap<String, ResolveInfo> unique = new LinkedHashMap<>();
                for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                    for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                        String id = r.activityInfo.packageName;
                        if (!homes.contains(id) && !id.equals(context.getPackageName())
                                && !id.equals("com.android.settings") && !id.equals("com.android.tv.settings")
                                && !id.contains("factorymenu") && !id.equals("com.haier.launcher.haiermatrix")
                                && !id.equals("com.mediatek.tv.service") && !unique.containsKey(id)) unique.put(id, r);
                    }
                }
                for (ResolveInfo r : unique.values()) {
                    try {
                        CharSequence name = r.loadLabel(pm);
                        String label = name == null ? r.activityInfo.packageName : name.toString();
                        if (label.trim().isEmpty()) label = r.activityInfo.packageName;
                        result.add(new AppEntry(r, label));
                    } catch (RuntimeException disappearingPackage) { /* The next package broadcast retries it. */ }
                }
                Collator collator = Collator.getInstance();
                java.util.Collections.sort(result, (a, b) -> collator.compare(a.label, b.label));
            } catch (RuntimeException unavailablePackageManager) { failed = true; }
            final boolean error = failed;
            if (!closed) main.post(() -> {
                if (closed) return;
                loading = false; callback.loaded(result, error);
                if (again) { again = false; load(callback); }
            });
        });
    }
    @Override public void close() { closed = true; worker.shutdownNow(); main.removeCallbacksAndMessages(null); }
}
