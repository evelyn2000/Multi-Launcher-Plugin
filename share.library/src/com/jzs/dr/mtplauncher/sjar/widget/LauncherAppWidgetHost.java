package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.model.ResConfigManager;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/**
 * Specific {@link AppWidgetHost} that creates our {@link LauncherAppWidgetHostView}
 * which correctly captures all long-press events. This ensures that users can
 * always pick up and move widgets.
 */
public class LauncherAppWidgetHost extends AppWidgetHost {
	private Launcher mLauncher;

    public LauncherAppWidgetHost(Launcher launcher, int hostId) {
        super(launcher.getActivity(), hostId);
        mLauncher = launcher;
    }

    @Override
    protected AppWidgetHostView onCreateView(Context context, int appWidgetId,
            AppWidgetProviderInfo appWidget) {    	
        return new LauncherAppWidgetHostView(context, mLauncher);
    }

    @Override
    public void stopListening() {
        super.stopListening();
        clearViews();
    }

    protected void onProvidersChanged() {
        // Once we get the message that widget packages are updated, we need to rebind items
        // in AppsCustomize accordingly.
        mLauncher.bindPackagesUpdated();
    }
}
