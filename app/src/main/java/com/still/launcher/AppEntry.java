package com.still.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;

final class AppEntry {
    final String id, label;
    final ResolveInfo info;
    AppEntry(ResolveInfo info, String label) {
        this.info = info; this.id = info.activityInfo.packageName; this.label = label;
    }
    Intent launchIntent() {
        return new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(id, info.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
}
