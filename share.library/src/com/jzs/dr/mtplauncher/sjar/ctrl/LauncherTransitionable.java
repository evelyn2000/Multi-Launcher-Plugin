package com.jzs.dr.mtplauncher.sjar.ctrl;

import com.jzs.dr.mtplauncher.sjar.Launcher;

import android.view.View;

public interface LauncherTransitionable {
	View getContent();
    void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStep(Launcher l, float t);
    void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace);
}
