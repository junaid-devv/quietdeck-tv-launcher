package com.quietdeck.fixture;

/** Original, non-functional artwork fixture installed only on test emulators. */
public final class FixtureActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.widget.TextView text = new android.widget.TextView(this);
        text.setText("QuietDeck test fixture — not a streaming app");
        text.setTextSize(24); text.setGravity(android.view.Gravity.CENTER);
        setContentView(text);
    }
}
