package com.still.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** GridView recycles these native, accessible cells; only visible cells request artwork. */
final class AppGridAdapter extends BaseAdapter {
    private final Context context;
    private final ArtworkLoader artwork;
    private final PreferencesStore prefs;
    private final List<AppEntry> entries = new ArrayList<>();
    AppGridAdapter(Context context, ArtworkLoader artwork, PreferencesStore prefs) {
        this.context = context; this.artwork = artwork; this.prefs = prefs;
    }
    void submit(List<AppEntry> next) { entries.clear(); entries.addAll(next); notifyDataSetChanged(); }
    @Override public int getCount() { return entries.size(); }
    @Override public AppEntry getItem(int position) { return entries.get(position); }
    @Override public long getItemId(int position) { return position; }
    private int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static GradientDrawable shape(int color, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(12);
        if (stroke != 0) drawable.setStroke(3, stroke); return drawable;
    }
    private static final class Holder { LinearLayout root; ImageView image; TextView label; }
    @Override public View getView(int position, View convert, ViewGroup parent) {
        Holder holder;
        if (convert == null) {
            holder = new Holder(); holder.root = new LinearLayout(context); holder.root.setOrientation(LinearLayout.VERTICAL);
            holder.root.setPadding(dp(8), dp(7), dp(8), dp(7));
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_selected}, shape(0xFF23434F, 0xFFACF0E8));
            states.addState(new int[]{android.R.attr.state_pressed}, shape(0xFF23434F, 0xFFACF0E8));
            states.addState(new int[]{}, shape(prefs.contrast() ? 0xFF050A10 : 0xDF11212E, 0x30394D5C));
            holder.root.setBackground(states);
            holder.root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(116)));
            holder.root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            holder.image = new ImageView(context); holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            holder.root.addView(holder.image, new LinearLayout.LayoutParams(-1, 0, 1));
            holder.label = new TextView(context); holder.label.setGravity(Gravity.CENTER);
            holder.label.setTextColor(Color.WHITE); holder.label.setSingleLine(true);
            holder.label.setEllipsize(TextUtils.TruncateAt.END);
            holder.label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            holder.label.setMinHeight(dp(27));
            holder.root.addView(holder.label, new LinearLayout.LayoutParams(-1, -2));
            holder.root.setTag(holder); convert = holder.root;
        } else holder = (Holder) convert.getTag();
        AppEntry app = getItem(position);
        holder.label.setText(app.label); holder.label.setTextSize(new int[]{12, 14, 17}[prefs.textSize()]);
        holder.root.setContentDescription(app.label); artwork.bind(app, holder.image);
        return convert;
    }
}
