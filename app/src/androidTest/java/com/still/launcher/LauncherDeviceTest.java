package com.still.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LauncherDeviceTest {
    private Context context;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PreferencesStore prefs = new PreferencesStore(context);
        prefs.raw.edit().clear().putBoolean("onboarded", true).commit();
        prefs.favourites(Arrays.asList("com.quietdeck.fixture.cinema", "com.quietdeck.fixture.music", "com.quietdeck.fixture.games"));
    }
    @Test public void legacyPreferencesAndImportRoundTripPreserveOrder() throws Exception {
        PreferencesStore store = new PreferencesStore(context, "test-config"); store.raw.edit().clear().commit();
        store.raw.edit().putString("favourites", "tv.alpha.app|tv.beta.app").putStringSet("hidden", Collections.singleton("tv.hidden.app")).commit();
        store.setGroup("Family", Arrays.asList("tv.beta.app", "tv.alpha.app"));
        store.setting("density", 2); String backup = store.exportConfig();
        store.raw.edit().clear().commit(); store.importConfig(backup);
        assertEquals(Arrays.asList("tv.alpha.app", "tv.beta.app"), store.favourites());
        assertTrue(store.hidden().contains("tv.hidden.app"));
        assertEquals(Arrays.asList("tv.beta.app", "tv.alpha.app"), store.groups().get("Family"));
        assertEquals(6, store.columns()); assertFalse(backup.contains("wallpaper")); assertFalse(backup.contains("recent"));
    }
    @Test public void invalidBackupCannotPartiallyReplaceExistingSettings() throws Exception {
        PreferencesStore store = new PreferencesStore(context, "test-invalid"); store.raw.edit().clear().commit();
        store.favourites(Collections.singletonList("tv.keep.app")); String before = store.exportConfig();
        try { store.importConfig(before.replace("\"density\": 1", "\"density\": 99")); fail("Invalid layout accepted"); }
        catch (IllegalArgumentException expected) { assertEquals(before, store.exportConfig()); }
    }
    @Test public void recentAppsAreBoundedDeduplicatedAndClearable() {
        PreferencesStore store = new PreferencesStore(context, "test-recent"); store.raw.edit().clear().commit();
        for (int i = 0; i < 30; i++) store.opened("tv.app" + i);
        assertEquals(12, store.recent().size()); store.opened("tv.app25");
        assertEquals("tv.app25", store.recent().get(0)); assertEquals(12, store.recent().size());
        store.raw.edit().remove("recent").commit(); assertTrue(store.recent().isEmpty());
    }
    @Test public void nativeControlsRestoreSearchAcrossActivityRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.nav_apps).isFocusable());
                assertTrue(activity.findViewById(R.id.app_options).isFocusable());
                activity.findViewById(R.id.nav_apps).performClick();
                ((EditText) activity.findViewById(R.id.app_search)).setText("NoMatchingApp123");
                assertEquals(0, ((GridView) activity.findViewById(R.id.app_grid)).getCount());
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals("NoMatchingApp123", ((EditText) activity.findViewById(R.id.app_search)).getText().toString()));
        }
    }
    @Test public void backLeavesOrdinaryLaunchAndRemoteCanNavigate() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.nav_apps).performClick(); activity.onBackPressed();
            });
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
            scenario.onActivity(activity -> assertNotNull(activity.getCurrentFocus()));
            scenario.onActivity(MainActivity::onBackPressed);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertNotEquals(androidx.lifecycle.Lifecycle.State.RESUMED, scenario.getState());
        }
    }
    @Test public void captureRealEmulatorScreensAndTrimMemory() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.nav_apps).performClick());
            awaitArtwork(scenario); screenshot("apps.png");
            scenario.onActivity(activity -> {
                activity.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW);
                activity.findViewById(R.id.nav_home).performClick();
            });
            awaitArtwork(scenario); screenshot("home.png");
        }
    }
    private void awaitArtwork(ActivityScenario<MainActivity> scenario) throws Exception {
        AtomicBoolean ready = new AtomicBoolean();
        for (int attempt = 0; attempt < 100 && !ready.get(); attempt++) {
            scenario.onActivity(activity -> {
                GridView grid = activity.findViewById(R.id.app_grid);
                if (grid.getCount() < 3 || grid.getChildCount() == 0) return;
                android.view.ViewGroup tile = (android.view.ViewGroup) grid.getChildAt(0);
                ready.set(((android.widget.ImageView) tile.getChildAt(0)).getDrawable() != null);
            });
            if (!ready.get()) Thread.sleep(50);
        }
        assertTrue("Installed fixture artwork did not load", ready.get());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private void screenshot(String name) throws Exception {
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File folder = new File(context.getExternalFilesDir(null), "screenshots"); assertTrue(folder.exists() || folder.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(folder, name))) { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out)); }
        image.recycle();
    }
}
