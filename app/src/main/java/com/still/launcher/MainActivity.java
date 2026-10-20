package com.still.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline TV home. Uses only Android framework APIs; no SDKs, ads or network requests. */
public class MainActivity extends Activity {
    private static final int HOME = 0, APPS = 1;
    private final Handler handler = new Handler();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<App> favourites = new ArrayList<>();
    private SharedPreferences prefs;
    private HomeView home;
    private boolean resumed, loading, reloadPending;
    private int page = HOME, zone = 1, nav = 0, selection = 0, shortcut = 0;
    private int scene = 0;
    private long keyStarted;
    private boolean longHandled;
    private final Runnable longPress = new Runnable() {
        @Override public void run() {
            if (resumed && zone == 1 && current() != null) {
                longHandled = true;
                showAppOptions(current());
            }
        }
    };
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (home != null) home.invalidate();
            if (resumed) handler.postDelayed(this, 30000);
        }
    };
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { reloadApps(); }
    };

    private static class App {
        String label, pkg;
        Intent launch;
        Bitmap artwork;
        boolean banner;
        int colour;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences("still", MODE_PRIVATE);
        scene = prefs.getInt("scene", 0);
        home = new HomeView();
        setContentView(home);
        home.setFocusable(true);
        home.setFocusableInTouchMode(true);
        home.requestFocus();
        IntentFilter changes = new IntentFilter();
        changes.addAction(Intent.ACTION_PACKAGE_ADDED);
        changes.addAction(Intent.ACTION_PACKAGE_REMOVED);
        changes.addAction(Intent.ACTION_PACKAGE_CHANGED);
        changes.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, changes, Context.RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, changes);
        immersive();
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        immersive();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
        reloadApps();
    }
    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(clockTick);
        handler.removeCallbacks(longPress);
        super.onPause();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(packageReceiver);
        worker.shutdownNow();
        super.onDestroy();
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            page = HOME; zone = 1; selection = 0; nav = 0;
            home.invalidate();
        }
    }

    private void reloadApps() {
        if (isFinishing() || isDestroyed()) return;
        if (loading) { reloadPending = true; return; }
        loading = true;
        worker.execute(new Runnable() {
            @Override public void run() {
                final ArrayList<App> found = new ArrayList<>();
                try {
                    PackageManager pm = getPackageManager();
                    Set<String> homes = new HashSet<>();
                    for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_HOME), 0)) homes.add(r.activityInfo.packageName);
                    LinkedHashMap<String, ResolveInfo> unique = new LinkedHashMap<>();
                    for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
                        List<ResolveInfo> results = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0);
                        for (ResolveInfo r : results) {
                            String p = r.activityInfo.packageName;
                            if (!unique.containsKey(p) && !homes.contains(p) && !p.equals(getPackageName())
                                    && !p.equals("com.android.settings") && !p.equals("com.android.tv.settings")
                                    && !p.contains("factorymenu") && !p.equals("com.haier.launcher.haiermatrix")
                                    && !p.equals("com.mediatek.tv.service")) {
                                unique.put(p, r);
                            }
                        }
                    }
                    for (ResolveInfo r : unique.values()) {
                        try {
                            App a = new App();
                            a.pkg = r.activityInfo.packageName;
                            a.label = r.loadLabel(pm).toString();
                            // OEM wrappers sometimes label their entry simply "apps".
                            // Retain them in All apps, but identify them clearly.
                            if (a.label.trim().equalsIgnoreCase("apps")) {
                                String suffix = a.pkg.substring(a.pkg.lastIndexOf('.') + 1);
                                a.label = suffix.equals("apps") ? "Haier Apps" : suffix;
                            }
                            a.launch = pm.getLeanbackLaunchIntentForPackage(a.pkg);
                            if (a.launch == null) a.launch = pm.getLaunchIntentForPackage(a.pkg);
                            if (a.launch == null) continue;
                            a.launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            Drawable d = r.activityInfo.loadBanner(pm);
                            if (d == null) d = r.activityInfo.applicationInfo.loadBanner(pm);
                            a.banner = d != null;
                            if (d == null) d = r.loadIcon(pm);
                            if (d != null) {
                                int w = a.banner ? 384 : 144;
                                int h = a.banner ? 216 : 144;
                                a.artwork = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                                Canvas c = new Canvas(a.artwork);
                                d.setBounds(0, 0, w, h);
                                d.draw(c);
                            }
                            a.colour = tileColour(a.pkg);
                            found.add(a);
                        } catch (RuntimeException ignored) { /* Skip a package that disappeared mid-query. */ }
                    }
                    Collections.sort(found, new Comparator<App>() {
                        @Override public int compare(App a, App b) { return a.label.compareToIgnoreCase(b.label); }
                    });
                } catch (RuntimeException ignored) { }
                handler.post(new Runnable() {
                    @Override public void run() {
                        if (isFinishing() || isDestroyed()) return;
                        apps.clear(); apps.addAll(found);
                        loading = false;
                        rebuildFavourites();
                        clamp();
                        home.invalidate();
                        if (reloadPending) { reloadPending = false; reloadApps(); }
                    }
                });
            }
        });
    }

    private int tileColour(String pkg) {
        String s = pkg.toLowerCase(Locale.ROOT);
        if (s.contains("youtube")) return Color.rgb(204, 28, 41);
        if (s.contains("netflix")) return Color.rgb(25, 22, 26);
        if (s.contains("amazon") || s.contains("prime")) return Color.rgb(16, 91, 140);
        if (s.contains("spotify")) return Color.rgb(18, 72, 48);
        if (s.contains("disney")) return Color.rgb(17, 39, 94);
        if (s.contains("vlc")) return Color.rgb(125, 65, 24);
        int[] colours = {0xFF263A52, 0xFF394251, 0xFF344A4B, 0xFF443A50, 0xFF2A4258};
        return colours[(pkg.hashCode() & 0x7fffffff) % colours.length];
    }

    private void rebuildFavourites() {
        if (!prefs.contains("favourites") && !apps.isEmpty()) {
            ArrayList<String> initial = new ArrayList<>();
            for (String pattern : new String[]{"youtube", "netflix", "amazon", "disney", "spotify", "vlc"})
                for (App a : apps) if (a.pkg.toLowerCase(Locale.ROOT).contains(pattern) && !initial.contains(a.pkg)) initial.add(a.pkg);
            for (App a : apps) if (initial.size() < 6 && !initial.contains(a.pkg)
                    && !a.pkg.startsWith("com.haier.apps") && !a.pkg.contains("factorymenu")
                    && !a.pkg.contains("launcher")) initial.add(a.pkg);
            prefs.edit().putString("favourites", TextUtils.join("|", initial)).apply();
        }
        favourites.clear();
        Set<String> hidden = hidden();
        for (String pkg : prefs.getString("favourites", "").split("\\|"))
            for (App a : apps) if (a.pkg.equals(pkg) && !hidden.contains(pkg)) favourites.add(a);
    }
    private Set<String> hidden() { return new HashSet<>(prefs.getStringSet("hidden", Collections.<String>emptySet())); }
    private ArrayList<App> visibleApps() {
        ArrayList<App> visible = new ArrayList<>();
        Set<String> hidden = hidden();
        for (App a : apps) if (!hidden.contains(a.pkg)) visible.add(a);
        return visible;
    }
    private List<App> items() { return page == HOME ? favourites : visibleApps(); }
    private App current() {
        List<App> list = items();
        return selection >= 0 && selection < list.size() ? list.get(selection) : null;
    }
    private void clamp() { selection = Math.max(0, Math.min(selection, items().size() - 1)); }
    private void saveFavourites() {
        ArrayList<String> ordered = new ArrayList<>();
        for (App a : favourites) ordered.add(a.pkg);
        prefs.edit().putString("favourites", TextUtils.join("|", ordered)).apply();
        clamp(); home.invalidate();
    }
    private void launch(App a) {
        if (a == null) return;
        try { startActivity(a.launch); }
        catch (RuntimeException e) { toast("This app is unavailable. Refreshing your apps."); reloadApps(); }
    }
    private void openSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (RuntimeException e) { toast("TV settings are unavailable."); }
    }
    private void openInputs() {
        Intent i = new Intent("android.settings.TV_INPUT_SETTINGS");
        try { startActivity(i); }
        catch (RuntimeException e) { toast("Use the Input / Source button on your remote."); }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void go(int target) {
        page = target; nav = target; zone = 1; selection = 0;
        home.invalidate(); announce();
    }
    private void announce() {
        String label = zone == 0 ? new String[]{"Home", "Apps", "Appearance", "Settings"}[nav]
                : zone == 2 ? new String[]{"All apps", "Inputs", "Settings"}[shortcut]
                : current() == null ? "No apps" : current().label;
        home.setContentDescription("QuietDeck TV. " + label);
        home.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getRepeatCount() == 0) {
                keyStarted = event.getDownTime(); longHandled = false;
                if (zone == 1) handler.postDelayed(longPress, 650);
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_INFO) {
            if (event.getRepeatCount() == 0) {
                if (zone == 1 && current() != null) showAppOptions(current()); else showPreferences();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (page == APPS) go(HOME);
            else { zone = 1; selection = 0; nav = 0; home.invalidate(); }
            return true;
        }
        if (keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            handler.removeCallbacks(longPress);
            navigate(keyCode); home.invalidate(); announce(); return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            handler.removeCallbacks(longPress);
            if (!longHandled && keyStarted != 0) activate();
            keyStarted = 0; return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    private void navigate(int key) {
        int count = items().size();
        if (zone == 0) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) nav = Math.max(0, nav - 1);
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) nav = Math.min(3, nav + 1);
            if (key == KeyEvent.KEYCODE_DPAD_DOWN) zone = 1;
        } else if (zone == 2) {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT) shortcut = Math.max(0, shortcut - 1);
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT) shortcut = Math.min(2, shortcut + 1);
            if (key == KeyEvent.KEYCODE_DPAD_UP) { zone = 1; selection = Math.max(0, Math.min(selection, count - 1)); }
        } else {
            int row = selection / 6;
            if (key == KeyEvent.KEYCODE_DPAD_UP && row == 0) { zone = 0; nav = page; }
            else if (key == KeyEvent.KEYCODE_DPAD_DOWN && (row + 1) * 6 >= count && page == HOME) zone = 2;
            else {
                GridNavigator.Direction direction = key == KeyEvent.KEYCODE_DPAD_LEFT ? GridNavigator.Direction.LEFT
                        : key == KeyEvent.KEYCODE_DPAD_RIGHT ? GridNavigator.Direction.RIGHT
                        : key == KeyEvent.KEYCODE_DPAD_UP ? GridNavigator.Direction.UP : GridNavigator.Direction.DOWN;
                selection = GridNavigator.move(selection, count, 6, direction);
            }
        }
    }
    private void activate() {
        if (zone == 0) {
            if (nav < 2) go(nav);
            else if (nav == 2) showPreferences(); else openSettings();
        } else if (zone == 2) {
            if (shortcut == 0) go(APPS); else if (shortcut == 1) openInputs(); else openSettings();
        } else if (current() != null) launch(current());
        else go(APPS);
    }

    private void showAppOptions(final App a) {
        final int index = favourites.indexOf(a);
        final ArrayList<String> actions = new ArrayList<>();
        actions.add("Open " + a.label);
        actions.add(index >= 0 ? "Remove from favourites" : "Add to favourites");
        if (index > 0) actions.add("Move left in favourites");
        if (index >= 0 && index < favourites.size() - 1) actions.add("Move right in favourites");
        actions.add("Hide from launcher");
        actions.add("App information");
        new AlertDialog.Builder(this).setTitle(a.label)
                .setItems(actions.toArray(new String[0]), (dialog, which) -> {
                    String action = actions.get(which);
                    if (which == 0) launch(a);
                    else if (which == 1) {
                        if (index >= 0) favourites.remove(a); else favourites.add(a);
                        saveFavourites();
                    } else if (action.equals("Move left in favourites")) {
                        Collections.swap(favourites, index, index - 1);
                        if (page == HOME) selection = index - 1;
                        saveFavourites();
                    } else if (action.equals("Move right in favourites")) {
                        Collections.swap(favourites, index, index + 1);
                        if (page == HOME) selection = index + 1;
                        saveFavourites();
                    } else if (action.equals("Hide from launcher")) {
                        Set<String> hidden = hidden(); hidden.add(a.pkg);
                        prefs.edit().putStringSet("hidden", hidden).apply();
                        rebuildFavourites(); clamp(); home.invalidate();
                    } else {
                        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + a.pkg))); }
                        catch (RuntimeException e) { openSettings(); }
                    }
                }).setNegativeButton("Close", null).show();
    }
    private void showPreferences() {
        new AlertDialog.Builder(this).setTitle("Make yourself at home")
                .setItems(new String[]{"Background", "Restore hidden apps", "Choose default home", "Refresh apps", "About QuietDeck TV"}, (d, which) -> {
                    if (which == 0) {
                        new AlertDialog.Builder(this).setTitle("Background")
                                .setSingleChoiceItems(new String[]{"Blue hour", "Warm dusk", "Midnight"}, scene, (pick, s) -> {
                                    scene = s; prefs.edit().putInt("scene", s).apply();
                                    home.invalidateScene(); pick.dismiss();
                                }).setNegativeButton("Close", null).show();
                    } else if (which == 1) {
                        prefs.edit().putStringSet("hidden", Collections.<String>emptySet()).apply();
                        rebuildFavourites(); clamp(); home.invalidate(); toast("Hidden apps restored.");
                    } else if (which == 2) {
                        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
                        catch (RuntimeException e) { toast("Press Home and choose QuietDeck TV if your TV offers a home-app picker."); }
                    } else if (which == 3) { reloadApps(); toast("Refreshing apps."); }
                    else new AlertDialog.Builder(this).setTitle("QuietDeck TV · 1.1.0")
                            .setMessage("A quieter home for your TV.\n\nUse the arrow keys to move and OK to open. Hold OK or press Menu for app options.\n\nYour favourites and background stay on this TV. No ads, accounts, tracking or internet permission.\n\nBuilt for Android TV.")
                            .setPositiveButton("Done", null).show();
                }).setNegativeButton("Close", null).show();
    }

    private class HomeView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
        private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
        private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        private Bitmap backdrop;
        private float scale = 1, dx, dy;
        private float downX, downY;
        private long touchDown;
        HomeView() { super(MainActivity.this); }
        void invalidateScene() {
            // Leave the old bitmap for GC: an in-flight hardware display list may still reference it.
            backdrop = null; invalidate();
        }
        private void colour(int c) { p.setShader(null); p.setColor(c); p.setStyle(Paint.Style.FILL); }
        private void text(Canvas c, String s, float x, float y, float size, int colour, Typeface face) {
            colour(colour); p.setTypeface(face); p.setTextSize(size); p.setTextAlign(Paint.Align.LEFT);
            c.drawText(s, x, y, p);
        }
        private void round(Canvas c, float x, float y, float w, float h, float r, int colour) {
            colour(colour); c.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
        }
        private String fit(String label, float width, float size) {
            p.setTextSize(size); p.setTypeface(regular);
            if (p.measureText(label) <= width) return label;
            int n = p.breakText(label, true, width - p.measureText("…"), null);
            return label.substring(0, Math.max(0, n)) + "…";
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            scale = Math.min(getWidth() / 1920f, getHeight() / 1080f);
            dx = (getWidth() - 1920 * scale) / 2; dy = (getHeight() - 1080 * scale) / 2;
            canvas.drawColor(0xFF09121E);
            canvas.save(); canvas.translate(dx, dy); canvas.scale(scale, scale);
            if (backdrop == null) backdrop = makeBackdrop();
            colour(Color.WHITE); canvas.drawBitmap(backdrop, null, new RectF(0, 0, 1920, 1080), p);
            header(canvas);
            if (page == HOME) drawHome(canvas); else drawApps(canvas);
            canvas.restore();
        }
        private Bitmap makeBackdrop() {
            Bitmap b = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            int top = scene == 1 ? 0xFF2B2339 : scene == 2 ? 0xFF101822 : 0xFF142F48;
            int horizon = scene == 1 ? 0xFF9F746C : scene == 2 ? 0xFF253846 : 0xFF628DAB;
            p.setShader(new LinearGradient(0, 0, 0, 1080, new int[]{top, horizon, 0xFF112334, 0xFF0B1420}, new float[]{0, .35f, .68f, 1}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 1920, 1080, p);
            int glow = scene == 1 ? 0x55F7C89C : scene == 2 ? 0x194F7590 : 0x55C7E1EF;
            p.setShader(new RadialGradient(1430, 282, 490, new int[]{glow, 0x00000000}, null, Shader.TileMode.CLAMP));
            c.drawRect(700, 0, 1920, 800, p);
            colour(scene == 1 ? 0xFFDDC1A2 : scene == 2 ? 0xFF889AA7 : 0xFFBFD5DF);
            c.drawCircle(1430, 251, scene == 2 ? 28 : 39, p);
            mountain(c, new float[]{0,468, 130,433, 252,447, 415,371, 562,405, 723,347, 900,394, 1115,310, 1238,348, 1381,303, 1540,365, 1712,307, 1920,381}, scene == 1 ? 0xFF5D586B : 0xFF42617C);
            mountain(c, new float[]{0,522, 197,488, 354,503, 570,445, 748,480, 981,410, 1100,428, 1287,389, 1456,441, 1675,393, 1920,430}, scene == 1 ? 0xFF393B51 : 0xFF29465F);
            mountain(c, new float[]{0,540, 220,559, 440,527, 689,566, 931,520, 1185,499, 1380,524, 1611,478, 1794,493, 1920,470}, 0xFF192E43);
            p.setShader(new LinearGradient(0, 0, 1120, 0, new int[]{0xB8081726, 0x500A1928, 0x000A1928}, new float[]{0,.65f,1}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 1920, 1080, p);
            p.setShader(new LinearGradient(0, 450, 0, 1080, new int[]{0x000B1420,0xF00B1420,0xFF0B1420}, new float[]{0,.47f,1}, Shader.TileMode.CLAMP));
            c.drawRect(0, 450, 1920, 1080, p);
            colour(Color.WHITE);
            return b;
        }
        private void mountain(Canvas c, float[] points, int colour) {
            Path path = new Path(); path.moveTo(points[0], points[1]);
            for (int i=2; i<points.length; i+=2) path.lineTo(points[i], points[i+1]);
            path.lineTo(1920,1080); path.lineTo(0,1080); path.close();
            colour(colour); c.drawPath(path,p);
        }
        private void header(Canvas c) {
            colour(0xFFDAE7EE); p.setStrokeWidth(2.7f); p.setStyle(Paint.Style.STROKE);
            c.drawCircle(132, 105, 18, p); c.drawLine(121,105,143,105,p);
            text(c, "QuietDeck", 168, 118, 32, 0xFFEEF4F7, medium);
            String[] labels = {"Home", "Apps", "Appearance", "Settings"};
            float[] xs = {640, 792, 930, 1158};
            float[] widths = {130, 114, 204, 160};
            for (int i=0; i<4; i++) {
                boolean focus = zone == 0 && nav == i;
                if (focus) round(c,xs[i]-22,76,widths[i],58,29,0xFFF0F5F8);
                text(c, labels[i], xs[i], 114, 25, focus ? 0xFF152535 : i == page ? 0xFFF0F5F8 : 0xFFACBDC9, i == page ? medium : regular);
                if (!focus && i == page) round(c,xs[i]+18,133,30,3,2,0xFFD4E4EE);
            }
            String time = android.text.format.DateFormat.getTimeFormat(MainActivity.this).format(new java.util.Date());
            p.setTypeface(light); p.setTextSize(32);
            float tw = p.measureText(time);
            text(c,time,1808-tw,113,32,0xFFECF4F8,light);
        }
        private void drawHome(Canvas c) {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            String greeting = hour < 12 ? "Good morning." : hour < 18 ? "Good afternoon." : "Good evening.";
            text(c,new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(now.getTime()),112,244,23,0xFFADC4D5,regular);
            text(c,greeting,108,344,76,0xFFF3F7FA,light);
            text(c,"Your favourites. Nothing in the way.",112,399,27,0xFFB9CDD9,regular);
            text(c,"Favourites",112,529,29,0xFFEEF4F7,medium);
            String instruction = favourites.isEmpty() ? "Add apps from the Apps tab" : "Hold OK to organise";
            p.setTypeface(regular); p.setTextSize(21);
            text(c,instruction,1808-p.measureText(instruction),529,21,0xFF8197A9,regular);
            int first = (selection / 6) * 6;
            for (int j=0; j<6 && first+j<favourites.size(); j++) tile(c,favourites.get(first+j),112+j*286,579,zone==1 && selection==first+j);
            if (favourites.isEmpty()) {
                round(c,112,579,1696,166,22,0x40384E62);
                text(c,loading ? "Finding your apps…" : "A home for your favourites",148,645,29,0xFFE4EEF4,regular);
                text(c,"Open Apps, hold OK on an app, then choose Add to favourites.",148,692,23,0xFFA7BDCC,regular);
            }
            if (favourites.size()>6) {
                String pages = ((first / 6)+1) + " / " + ((favourites.size()+5)/6);
                text(c,pages,1736,805,20,0xFF8197A9,regular);
            }
            shortcut(c,"All apps",112,873,192,zone==2 && shortcut==0,0);
            shortcut(c,"Inputs",324,873,176,zone==2 && shortcut==1,1);
            shortcut(c,"Settings",520,873,200,zone==2 && shortcut==2,2);
            text(c,"A little space to switch off.",112,1004,20,0xFF6E8597,regular);
            text(c,"QUIETDECK  /  TV",1600,1004,18,0xFF5F788B,medium);
        }
        private void drawApps(Canvas c) {
            List<App> visible = visibleApps();
            text(c,"Your apps",108,274,60,0xFFF3F7FA,light);
            text(c,visible.size() + (visible.size()==1 ? " app" : " apps") + "  ·  Hold OK for options",112,323,24,0xFFABC1D1,regular);
            int first = selection < 12 ? 0 : ((selection/6)-1)*6;
            for (int j=0; j<12 && first+j<visible.size(); j++)
                tile(c,visible.get(first+j),112+(j%6)*286,386+(j/6)*264,zone==1 && selection==first+j);
            if (visible.isEmpty()) {
                text(c,loading ? "Finding your apps…" : "No visible apps",112,460,32,0xFFE4EEF4,regular);
                text(c,"Appearance → Restore hidden apps, or install apps from your TV's Play Store.",112,516,24,0xFFA7BDCC,regular);
            }
            text(c,"Back to return home",112,1004,21,0xFF8197A9,regular);
            if (visible.size()>12) text(c,(selection+1)+" / "+visible.size(),1715,1004,21,0xFF8197A9,regular);
        }
        private void tile(Canvas c, App a, float x, float y, boolean focused) {
            final float w=266,h=150;
            c.save();
            if (focused) {
                c.scale(1.035f,1.035f,x+w/2,y+h/2);
                round(c,x-5,y-5,w+10,h+10,22,0xFFF3F7FA);
                round(c,x-1,y-1,w+2,h+2,18,0xFF0A1520);
            }
            round(c,x,y,w,h,17,a.colour);
            Path clip=new Path(); clip.addRoundRect(new RectF(x,y,x+w,y+h),17,17,Path.Direction.CW);
            c.save(); c.clipPath(clip);
            if (a.artwork!=null) {
                colour(Color.WHITE);
                if (a.banner) c.drawBitmap(a.artwork,null,new RectF(x,y,x+w,y+h),p);
                else c.drawBitmap(a.artwork,null,new RectF(x+w/2-49,y+h/2-49,x+w/2+49,y+h/2+49),p);
            } else {
                p.setTypeface(medium); p.setTextSize(42);
                String mark=a.label.length()>0 ? a.label.substring(0,1).toUpperCase(Locale.getDefault()) : "·";
                text(c,mark,x+(w-p.measureText(mark))/2,y+91,42,0xFFEAF1F5,medium);
            }
            c.restore();
            p.setShader(new LinearGradient(x,y,x,y+h,0x12FFFFFF,0x00000000,Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(x,y,x+w,y+h),17,17,p);
            colour(0x15FFFFFF); p.setStrokeWidth(1); p.setStyle(Paint.Style.STROKE);
            c.drawRoundRect(new RectF(x+.5f,y+.5f,x+w-.5f,y+h-.5f),17,17,p);
            c.restore();
            String label=fit(a.label,w,23);
            p.setTypeface(regular); p.setTextSize(23);
            text(c,label,x+(w-p.measureText(label))/2,y+h+43,23,focused?0xFFF4F8FA:0xFF93ABBD,regular);
        }
        private void shortcut(Canvas c,String title,float x,float y,float w,boolean focused,int icon) {
            round(c,x,y,w,62,31,focused?0xFFE8F0F5:0x303C5266);
            int ink=focused?0xFF162A3B:0xFFBCCDD9;
            colour(ink);
            if (icon==0) {
                for(int i=0;i<4;i++) c.drawRoundRect(new RectF(x+25+(i%2)*11,y+23+(i/2)*11,x+32+(i%2)*11,y+30+(i/2)*11),1,1,p);
            } else if (icon==1) {
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
                c.drawRoundRect(new RectF(x+23,y+23,x+45,y+40),2,2,p);
                c.drawLine(x+30,y+44,x+39,y+44,p);
            } else {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
                c.drawCircle(x+34,y+32,9,p);c.drawCircle(x+34,y+32,3,p);
                for(int i=0;i<8;i++) {
                    double angle=i*Math.PI/4;
                    c.drawLine(x+34+(float)Math.cos(angle)*10,y+32+(float)Math.sin(angle)*10,x+34+(float)Math.cos(angle)*13,y+32+(float)Math.sin(angle)*13,p);
                }
            }
            text(c,title,x+59,y+40,23,ink,regular);
        }
        @Override public boolean performClick() { super.performClick(); activate(); return true; }
        @Override public boolean onTouchEvent(MotionEvent e) {
            float x=(e.getX()-dx)/scale, y=(e.getY()-dy)/scale;
            if(e.getAction()==MotionEvent.ACTION_DOWN) {
                downX=x;downY=y;touchDown=System.currentTimeMillis();return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP) {
                float delta=y-downY;
                if(Math.abs(delta)>70 && Math.abs(x-downX)<150) {
                    if (zone!=1) zone=1;
                    navigate(delta<0?KeyEvent.KEYCODE_DPAD_DOWN:KeyEvent.KEYCODE_DPAD_UP);invalidate();return true;
                }
                if(Math.abs(x-downX)>40 || Math.abs(delta)>40)return true;
                if(y>=65 && y<=145) {
                    float[] starts={618,770,908,1136}, ends={748,884,1112,1296};
                    for(int i=0;i<4;i++)if(x>=starts[i]&&x<=ends[i]){zone=0;nav=i;performClick();invalidate();return true;}
                }
                if(page==HOME && y>=873 && y<=935 && x>=112 && x<=720) {
                    if(x<=304)shortcut=0;else if(x>=324&&x<=500)shortcut=1;else if(x>=520)shortcut=2;else return true;
                    zone=2;performClick();invalidate();return true;
                }
                int first=page==HOME?(selection/6)*6:selection<12?0:((selection/6)-1)*6;
                float tileY=page==HOME?579:386;
                int col=(int)((x-112)/286),row=(int)((y-tileY)/264);
                if(x>=112 && col>=0&&col<6 && row>=0&&row<(page==HOME?1:2)
                        && x-(112+col*286)<=266 && y>=tileY && y-(tileY+row*264)<=204) {
                    int index=first+row*6+col;
                    if(index<items().size()) {
                        zone=1;selection=index;
                        if(System.currentTimeMillis()-touchDown>=650)showAppOptions(current());else performClick();
                        invalidate();announce();
                    }
                }
                return true;
            }
            return true;
        }
    }
}
