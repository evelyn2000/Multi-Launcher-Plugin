package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherTransitionable;

import android.view.View;

public interface IAppsCustomizePanel extends LauncherTransitionable {

	//public View getContent();
	public void onWindowVisible();
	public boolean isTransitioning();
	public void setContentVisibility(int visibility);
	public int getContentVisibility();
	
	public void clearAllWidgetPages();
}
