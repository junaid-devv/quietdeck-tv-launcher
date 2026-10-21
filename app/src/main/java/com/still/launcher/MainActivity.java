package com.still.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native TV home: rendering, discovery, artwork and persistence have separate owners. */
public class MainActivity extends Activity {
    private static final int EXPORT = 301, IMPORT = 302, WALLPAPER = 303;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService files = Executors.newSingleThreadExecutor();
    private final List<AppEntry> apps = new ArrayList<>();
    private PreferencesStore prefs;
    private AppRepository repository;
    private ArtworkLoader artwork;
    private FrameLayout root;
    private GridView grid;
    private AppGridAdapter adapter;
    private EditText search;
    private TextView title, subtitle, count, clock, empty;
    private Button favouritesButton, recentButton;
    private boolean lowRam, resumed, dirty = true, loaded, destroyed;
    private String page = "home", group = "", selectedId = "", pendingSelection = "", savedQuery = "";
    private int wallpaperGeneration;
    private Bitmap wallpaper;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (!resumed || destroyed) return;
            clock.setText(android.text.format.DateFormat.getTimeFormat(MainActivity.this).format(new java.util.Date()));
            main.postDelayed(this, 60000 - System.currentTimeMillis() % 60000);
        }
    };
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            dirty = true; artwork.clear(); if (resumed) refreshApps();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        lowRam = ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).isLowRamDevice();
        prefs = new PreferencesStore(this);
        repository = new AppRepository(this); artwork = new ArtworkLoader(this, lowRam);
        if (state != null) {
            page = state.getString("page", "home"); group = state.getString("group", "");
            pendingSelection = state.getString("selection", ""); savedQuery = state.getString("query", "");
        }
        buildScreen();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED); filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED); filter.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, filter);
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String s(int id, Object... args) { return getString(id, args); }
    private void toast(int id) { Toast.makeText(this, id, Toast.LENGTH_LONG).show(); }
    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL); return row;
    }
    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); return view;
    }
    private Button button(int label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(13);
        button.setFocusableInTouchMode(true);
        button.setTextColor(Color.WHITE); button.setMinHeight(0); button.setMinimumHeight(0); button.setMinWidth(0);
        button.setPadding(dp(14), dp(6), dp(14), dp(6));
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, AppGridAdapter.shape(0xFF315B64, 0xFFB6EFE8));
        background.addState(new int[]{android.R.attr.state_pressed}, AppGridAdapter.shape(0xFF315B64, 0xFFB6EFE8));
        background.addState(new int[]{}, AppGridAdapter.shape(0x661B2D3B, 0)); button.setBackground(background);
        button.setStateListAnimator(null); button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(40)); lp.setMarginEnd(dp(8));
        button.setLayoutParams(lp); return button;
    }
    private void buildScreen() {
        root = new FrameLayout(this); root.setBackground(new DeckBackground(prefs.scene()));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(36), dp(22), dp(36), dp(20)); root.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout header = row();
        ImageView logo = new ImageView(this); logo.setImageResource(R.drawable.ic_still);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(logo, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView brand = text(s(R.string.brand), 21, 0xFFF3F7FA); brand.setPadding(dp(9), 0, dp(25), 0);
        header.addView(brand);
        Button homeButton = button(R.string.home, () -> showPage("home", "")); homeButton.setId(R.id.nav_home); header.addView(homeButton);
        Button appsButton = button(R.string.apps, () -> showPage("apps", "")); appsButton.setId(R.id.nav_apps); header.addView(appsButton);
        header.addView(button(R.string.collections, this::showCollections));
        Button settingsButton = button(R.string.settings, this::showSettings); settingsButton.setId(R.id.nav_settings); header.addView(settingsButton);
        clock = text("", 18, 0xFFDDECEB); clock.setGravity(Gravity.END);
        header.addView(clock, new LinearLayout.LayoutParams(0, -2, 1));
        layout.addView(header, new LinearLayout.LayoutParams(-1, dp(46)));
        title = text("", 35, 0xFFF5F7FA); title.setPadding(0, dp(17), 0, 0);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        layout.addView(title);
        subtitle = text(s(R.string.home_subtitle), 14, 0xFFC4D5DE); subtitle.setPadding(0, dp(3), 0, dp(14)); layout.addView(subtitle);
        LinearLayout toolbar = row();
        favouritesButton = button(R.string.favourites, () -> showPage("home", "")); toolbar.addView(favouritesButton);
        recentButton = button(R.string.recent, () -> showPage("recent", "")); toolbar.addView(recentButton);
        search = new EditText(this); search.setId(R.id.app_search); search.setSingleLine(true);
        search.setTextSize(14); search.setTextColor(Color.WHITE); search.setHintTextColor(0xFFBACBD6);
        search.setHint(R.string.search_hint); search.setContentDescription(s(R.string.search_description));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        toolbar.addView(search, new LinearLayout.LayoutParams(0, dp(44), 1));
        layout.addView(toolbar);
        count = text(s(R.string.loading), 12, 0xFFB9CCD7); count.setPadding(0, dp(9), 0, dp(9)); layout.addView(count);
        FrameLayout content = new FrameLayout(this); layout.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        grid = new GridView(this); grid.setId(R.id.app_grid); grid.setNumColumns(prefs.columns());
        grid.setHorizontalSpacing(dp(12)); grid.setVerticalSpacing(dp(12)); grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setClipToPadding(false); grid.setPadding(dp(3), dp(3), dp(3), dp(3));
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT)); grid.setDrawSelectorOnTop(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        adapter = new AppGridAdapter(this, artwork, prefs); grid.setAdapter(adapter);
        content.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        empty = text(s(R.string.empty) + "\n\n" + s(R.string.empty_hint), 17, 0xFFD9E6EA);
        empty.setPadding(dp(4), dp(20), dp(4), dp(4)); content.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        grid.setEmptyView(empty);
        grid.setOnItemClickListener((parent, view, position, id) -> launch(adapter.getItem(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> { showOptions(adapter.getItem(position)); return true; });
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < adapter.getCount()) selectedId = adapter.getItem(position).id;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        LinearLayout footer = row(); footer.setPadding(0, dp(12), 0, 0);
        Button options = button(R.string.options, this::selectedOptions); options.setId(R.id.app_options); footer.addView(options);
        footer.addView(button(R.string.inputs, this::openInputs));
        footer.addView(button(R.string.choose_favourites, this::chooseFavourites));
        TextView caption = text(s(R.string.footer), 11, 0xFFADC4CF); caption.setGravity(Gravity.END);
        footer.addView(caption, new LinearLayout.LayoutParams(0, -2, 1)); layout.addView(footer);
        setContentView(root);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable value) { }
        });
        if (!savedQuery.isEmpty()) search.setText(savedQuery);
        render(); loadWallpaper();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        main.removeCallbacks(clockTick); main.post(clockTick);
        if (wallpaper == null && !prefs.raw.getString("wallpaper", "").isEmpty()) loadWallpaper();
        if (dirty || !loaded) refreshApps();
    }
    @Override protected void onPause() {
        resumed = false; main.removeCallbacks(clockTick); super.onPause();
    }
    @Override protected void onDestroy() {
        destroyed = true; wallpaperGeneration++;
        unregisterReceiver(packageReceiver); repository.close(); artwork.close(); files.shutdownNow();
        main.removeCallbacksAndMessages(null); wallpaper = null; super.onDestroy();
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (artwork != null) artwork.trim(level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && root != null && !resumed) {
            wallpaperGeneration++; root.setBackground(new DeckBackground(prefs.scene())); wallpaper = null;
        }
    }
    @Override public void onLowMemory() { super.onLowMemory(); if (artwork != null) artwork.clear(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putString("page", page); state.putString("group", group);
        state.putString("selection", selectedId); state.putString("query", search.getText().toString());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); showPage("home", "");
    }
    private void refreshApps() {
        dirty = false;
        repository.load((next, failed) -> {
            if (failed) { dirty = true; toast(R.string.load_failed); }
            else { apps.clear(); apps.addAll(next); loaded = true; }
            render();
            if (!failed && !prefs.raw.getBoolean("onboarded", false) && !apps.isEmpty()) {
                // Existing installs retain their original favourite order.
                if (prefs.raw.contains("favourites")) prefs.raw.edit().putBoolean("onboarded", true).apply();
                else {
                    prefs.raw.edit().putBoolean("onboarded", true).apply();
                    new AlertDialog.Builder(this).setTitle(R.string.welcome).setMessage(R.string.welcome_text)
                            .setPositiveButton(R.string.start_choosing, (d, w) -> chooseFavourites())
                            .setNegativeButton(R.string.later, null).show();
                }
            }
        });
    }
    private void showPage(String page, String collection) {
        this.page = page; group = collection; selectedId = ""; pendingSelection = ""; search.setText("");
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        render(); grid.setSelection(0); focusAppsOrHome();
    }
    private void focusAppsOrHome() {
        if (adapter.getCount() == 0 || !grid.requestFocus()) findViewById(R.id.nav_home).requestFocus();
    }
    private void render() {
        if (adapter == null) return;
        boolean home = page.equals("home"), recent = page.equals("recent");
        title.setText(home || recent ? s(R.string.home_title) : page.equals("group") ? group : s(R.string.apps_title));
        subtitle.setText(home || recent ? s(R.string.home_subtitle) : page.equals("group") ? s(R.string.collections) : s(R.string.apps));
        favouritesButton.setVisibility(home || recent ? View.VISIBLE : View.GONE);
        recentButton.setVisibility(home || recent ? View.VISIBLE : View.GONE);
        Set<String> hidden = prefs.hidden(); Map<String, AppEntry> available = new LinkedHashMap<>();
        for (AppEntry app : apps) if (!hidden.contains(app.id)) available.put(app.id, app);
        List<AppEntry> visible = new ArrayList<>();
        List<String> order = home ? prefs.favourites() : recent ? prefs.recent()
                : page.equals("group") ? prefs.groups().get(group) : null;
        if (order == null && !page.equals("group")) visible.addAll(available.values());
        else if (order != null) for (String id : order) if (available.containsKey(id)) visible.add(available.get(id));
        String query = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        if (!query.isEmpty()) {
            List<AppEntry> filtered = new ArrayList<>();
            for (AppEntry app : available.values()) if (app.label.toLowerCase(Locale.getDefault()).contains(query)) filtered.add(app);
            visible = filtered;
        }
        String retain = !pendingSelection.isEmpty() ? pendingSelection : selectedId;
        adapter.submit(visible); count.setText(!loaded ? s(R.string.loading)
                : getResources().getQuantityString(R.plurals.app_count, visible.size(), visible.size()));
        empty.setText(!loaded ? s(R.string.loading) : s(R.string.empty) + "\n\n" + s(R.string.empty_hint));
        selectedId = "";
        for (int i = 0; i < visible.size(); i++) if (visible.get(i).id.equals(retain)) { grid.setSelection(i); selectedId = retain; break; }
        if (selectedId.isEmpty() && !visible.isEmpty()) selectedId = visible.get(0).id;
        if (loaded) pendingSelection = "";
    }
    private void launch(AppEntry app) {
        try { startActivity(app.launchIntent()); prefs.opened(app.id); }
        catch (RuntimeException failed) { toast(R.string.launch_failed); dirty = true; refreshApps(); }
    }
    private void selectedOptions() {
        for (AppEntry app : apps) if (app.id.equals(selectedId)) { showOptions(app); return; }
        if (!apps.isEmpty()) showPage("apps", "");
    }
    private void showOptions(AppEntry app) {
        List<String> favourites = prefs.favourites(); int index = favourites.indexOf(app.id);
        List<Integer> actions = new ArrayList<>(); List<String> labels = new ArrayList<>();
        actions.add(0); labels.add(s(R.string.open_app, app.label));
        actions.add(1); labels.add(s(index >= 0 ? R.string.remove_favourite : R.string.add_favourite));
        if (index > 0) { actions.add(2); labels.add(s(R.string.move_left)); }
        if (index >= 0 && index < favourites.size() - 1) { actions.add(3); labels.add(s(R.string.move_right)); }
        actions.add(4); labels.add(s(R.string.add_collection));
        if (page.equals("group")) { actions.add(5); labels.add(s(R.string.remove_collection)); }
        actions.add(6); labels.add(s(R.string.hide_app)); actions.add(7); labels.add(s(R.string.app_information));
        new AlertDialog.Builder(this).setTitle(app.label).setItems(labels.toArray(new String[0]), (dialog, which) -> {
            switch (actions.get(which)) {
                case 0: launch(app); return;
                case 1: if (index >= 0) favourites.remove(app.id); else favourites.add(app.id); prefs.favourites(favourites); break;
                case 2: Collections.swap(favourites, index, index - 1); prefs.favourites(favourites); break;
                case 3: Collections.swap(favourites, index, index + 1); prefs.favourites(favourites); break;
                case 4: addCollection(app); return;
                case 5:
                    List<String> members = prefs.groups().get(group);
                    if (members != null) { members.remove(app.id); prefs.setGroup(group, members); } break;
                case 6: prefs.hide(app.id); break;
                default: safeStart(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.id)), R.string.settings_unavailable); return;
            }
            render();
        }).setNegativeButton(R.string.close, null).show();
    }
    private void chooseFavourites() {
        if (apps.isEmpty()) { showPage("apps", ""); return; }
        List<String> chosen = prefs.favourites(); List<AppEntry> snapshot = new ArrayList<>(apps);
        String[] labels = new String[snapshot.size()]; boolean[] checked = new boolean[snapshot.size()];
        for (int i = 0; i < snapshot.size(); i++) { labels[i] = snapshot.get(i).label; checked[i] = chosen.contains(snapshot.get(i).id); }
        new AlertDialog.Builder(this).setTitle(R.string.choose_favourites)
                .setMultiChoiceItems(labels, checked, (d, index, enabled) -> {
                    String id = snapshot.get(index).id;
                    if (enabled && !chosen.contains(id)) chosen.add(id); else if (!enabled) chosen.remove(id);
                }).setPositiveButton(R.string.save, (d, w) -> { prefs.favourites(chosen); render(); })
                .setNegativeButton(R.string.cancel, null).show();
    }
    private void showCollections() {
        List<String> names = new ArrayList<>(prefs.groups().keySet());
        if (names.isEmpty()) {
            new AlertDialog.Builder(this).setTitle(R.string.collections).setMessage(R.string.no_collections)
                    .setPositiveButton(R.string.apps, (d, w) -> showPage("apps", "")).setNegativeButton(R.string.close, null).show(); return;
        }
        new AlertDialog.Builder(this).setTitle(R.string.collections).setItems(names.toArray(new String[0]),
                (d, w) -> showPage("group", names.get(w))).setNegativeButton(R.string.close, null).show();
    }
    private void addCollection(AppEntry app) {
        List<String> names = new ArrayList<>(prefs.groups().keySet()); names.add(s(R.string.new_collection));
        new AlertDialog.Builder(this).setTitle(R.string.add_collection).setItems(names.toArray(new String[0]), (d, w) -> {
            if (w < names.size() - 1) putInGroup(names.get(w), app);
            else {
                EditText input = new EditText(this); input.setHint(R.string.collection_name); input.setSingleLine(true);
                input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32)});
                new AlertDialog.Builder(this).setTitle(R.string.new_collection).setView(input)
                        .setPositiveButton(R.string.add, (dialog, button) -> putInGroup(input.getText().toString(), app))
                        .setNegativeButton(R.string.cancel, null).show();
            }
        }).setNegativeButton(R.string.cancel, null).show();
    }
    private void putInGroup(String name, AppEntry app) {
        try {
            name = ConfigRules.groupName(name);
            List<String> ids = prefs.groups().get(name); if (ids == null) ids = new ArrayList<>();
            if (!ids.contains(app.id)) ids.add(app.id); prefs.setGroup(name, ids);
            Toast.makeText(this, s(R.string.collection_added, name), Toast.LENGTH_SHORT).show(); render();
        } catch (IllegalArgumentException invalid) { toast(R.string.invalid_collection); }
    }
    private void showSettings() {
        int[] ids = {R.string.choose_favourites, R.string.background, R.string.grid_size, R.string.text_size,
                R.string.high_contrast, R.string.local_wallpaper, R.string.remove_wallpaper, R.string.restore_hidden,
                R.string.default_home, R.string.settings, R.string.refresh, R.string.export_config, R.string.import_config,
                R.string.clear_recent, R.string.privacy, R.string.about};
        String[] labels = new String[ids.length]; for (int i = 0; i < ids.length; i++) labels[i] = s(ids[i]);
        new AlertDialog.Builder(this).setTitle(R.string.settings).setItems(labels, (d, which) -> {
            switch (which) {
                case 0: chooseFavourites(); break;
                case 1: chooseSetting(R.string.background, "scene", new int[]{R.string.blue_hour, R.string.warm_dusk, R.string.midnight}, prefs.scene()); break;
                case 2: chooseSetting(R.string.grid_size, "density", new int[]{R.string.large_tiles, R.string.balanced_tiles, R.string.compact_tiles}, prefs.density()); break;
                case 3: chooseSetting(R.string.text_size, "textSize", new int[]{R.string.small_text, R.string.normal_text, R.string.large_text}, prefs.textSize()); break;
                case 4:
                    new AlertDialog.Builder(this).setTitle(R.string.high_contrast)
                            .setSingleChoiceItems(new String[]{s(R.string.off), s(R.string.on)}, prefs.contrast() ? 1 : 0, (dialog, pick) -> {
                                prefs.raw.edit().putBoolean("contrast", pick == 1).apply(); dialog.dismiss(); rebuildScreen();
                            }).setNegativeButton(R.string.cancel, null).show(); break;
                case 5: pickFile(WALLPAPER); break;
                case 6: replaceWallpaper(null); break;
                case 7: prefs.restoreHidden(); render(); toast(R.string.hidden_restored); break;
                case 8: chooseDefaultHome(); break;
                case 9: safeStart(new Intent(Settings.ACTION_SETTINGS), R.string.settings_unavailable); break;
                case 10: artwork.clear(); refreshApps(); break;
                case 11: pickFile(EXPORT); break;
                case 12: pickFile(IMPORT); break;
                case 13: prefs.raw.edit().remove("recent").apply(); render(); toast(R.string.recent_cleared); break;
                case 14: showText(R.string.privacy, s(R.string.policy_text)); break;
                default: showText(R.string.about, s(R.string.about_text, s(R.string.version_name), artwork.bytes() / 1024, s(lowRam ? R.string.on : R.string.off)));
            }
        }).setNegativeButton(R.string.close, null).show();
    }
    private void chooseSetting(int title, String key, int[] values, int current) {
        String[] labels = new String[values.length]; for (int i = 0; i < values.length; i++) labels[i] = s(values[i]);
        new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels, current, (d, w) -> {
            prefs.setting(key, w); d.dismiss(); rebuildScreen();
        }).setNegativeButton(R.string.cancel, null).show();
    }
    private void rebuildScreen() {
        pendingSelection = selectedId; savedQuery = search.getText().toString(); buildScreen();
        main.removeCallbacks(clockTick); main.post(clockTick); focusAppsOrHome();
    }
    private void showText(int title, String value) {
        ScrollView scroll = new ScrollView(this); TextView copy = text(value, 16, Color.WHITE);
        copy.setPadding(dp(24), dp(12), dp(24), dp(18)); scroll.addView(copy);
        new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton(R.string.done, null).show();
    }
    private boolean safeStart(Intent intent, int error) {
        try { startActivity(intent); return true; } catch (RuntimeException unavailable) { toast(error); return false; }
    }
    private void openInputs() { safeStart(new Intent("android.settings.TV_INPUT_SETTINGS"), R.string.input_hint); }
    private void chooseDefaultHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                try { startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 304); return; }
                catch (RuntimeException restrictedRole) { /* Offer the manufacturer's settings next. */ }
            }
        }
        safeStart(new Intent(Settings.ACTION_HOME_SETTINGS), R.string.default_hint);
    }
    private boolean isDefaultHome() {
        ResolveInfo home = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        return home != null && home.activityInfo != null && getPackageName().equals(home.activityInfo.packageName);
    }
    @Override public void onBackPressed() {
        if (!search.getText().toString().isEmpty()) { search.setText(""); focusAppsOrHome(); }
        else if (!page.equals("home")) showPage("home", "");
        else if (isDefaultHome()) { grid.setSelection(0); focusAppsOrHome(); }
        else super.onBackPressed();
    }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_INFO) {
            if (event.getRepeatCount() == 0) selectedOptions(); return true;
        }
        return super.onKeyDown(code, event);
    }
    private void pickFile(int request) {
        Intent intent = new Intent(request == EXPORT ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType(request == WALLPAPER ? "image/*" : "application/json");
        if (request == EXPORT) intent.putExtra(Intent.EXTRA_TITLE, "QuietDeck-settings.json");
        try { startActivityForResult(intent, request); } catch (RuntimeException missingPicker) { toast(R.string.picker_unavailable); }
    }
    private void replaceWallpaper(Uri next) {
        String previous = prefs.raw.getString("wallpaper", "");
        if (next == null) prefs.raw.edit().remove("wallpaper").apply();
        else prefs.raw.edit().putString("wallpaper", next.toString()).apply();
        if (!previous.isEmpty() && (next == null || !previous.equals(next.toString()))) {
            try { getContentResolver().releasePersistableUriPermission(Uri.parse(previous), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (RuntimeException revoked) { /* The provider may already have revoked access. */ }
        }
        loadWallpaper();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == WALLPAPER) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                replaceWallpaper(uri);
            } catch (RuntimeException denied) { toast(R.string.wallpaper_failed); }
        } else if (request == EXPORT) {
            final String json;
            try { json = prefs.exportConfig(); } catch (Exception invalid) { toast(R.string.file_error); return; }
            files.execute(() -> {
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new IllegalArgumentException(); output.write(json.getBytes(StandardCharsets.UTF_8));
                    postToast(R.string.export_done);
                } catch (Exception failed) { postToast(R.string.file_error); }
            });
        } else if (request == IMPORT) files.execute(() -> {
            try (InputStream input = getContentResolver().openInputStream(uri); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (input == null) throw new IllegalArgumentException();
                byte[] buffer = new byte[4096]; int n;
                while ((n = input.read(buffer)) != -1) {
                    if (output.size() + n > ConfigRules.MAX_BYTES) throw new IllegalArgumentException(); output.write(buffer, 0, n);
                }
                String text = new String(output.toByteArray(), StandardCharsets.UTF_8);
                if (!destroyed) main.post(() -> {
                    if (destroyed) return;
                    new AlertDialog.Builder(this).setTitle(R.string.import_config).setMessage(R.string.import_confirm)
                            .setPositiveButton(R.string.save, (dialog, which) -> {
                                try { prefs.importConfig(text); rebuildScreen(); toast(R.string.import_done); }
                                catch (Exception invalid) { toast(R.string.file_error); }
                            }).setNegativeButton(R.string.cancel, null).show();
                });
            } catch (Exception invalid) { postToast(R.string.file_error); }
        });
    }
    private void postToast(int id) { if (!destroyed) main.post(() -> { if (!destroyed) toast(id); }); }
    private void loadWallpaper() {
        int generation = ++wallpaperGeneration;
        root.setBackground(prefs.contrast() ? new ColorDrawable(0xFF05090F) : new DeckBackground(prefs.scene()));
        wallpaper = null;
        String value = prefs.raw.getString("wallpaper", "");
        if (value.isEmpty() || prefs.contrast()) return;
        files.execute(() -> {
            try {
                Uri uri = Uri.parse(value); BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input, null, options); }
                int max = lowRam ? 960 : 1280;
                if (options.outWidth < 1 || options.outHeight < 1) throw new IllegalArgumentException();
                options.inSampleSize = 1;
                while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > max) options.inSampleSize *= 2;
                options.inJustDecodeBounds = false; options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap image;
                try (InputStream input = getContentResolver().openInputStream(uri)) { image = BitmapFactory.decodeStream(input, null, options); }
                if (image == null) throw new IllegalArgumentException();
                if (!destroyed) main.post(() -> {
                    if (destroyed || generation != wallpaperGeneration) return;
                    wallpaper = image;
                    Drawable background = new WallpaperBackground(image);
                    root.setBackground(new LayerDrawable(new Drawable[]{background, new ColorDrawable(0xB008101A)}));
                });
            } catch (Exception | OutOfMemoryError unavailable) {
                if (!destroyed && generation == wallpaperGeneration) postToast(R.string.wallpaper_failed);
            }
        });
    }
}
