/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jzs.dr.mtplauncher.sjar.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.CheckLongPressHelper;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

/**
 * {@inheritDoc}
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView {
	private final static String TAG = "LauncherAppWidgetHostView";

    private CheckLongPressHelper mLongPressHelper;
    //private LayoutInflater mInflater;
    private Context mContext;
    private int mPreviousOrientation;
    private IResConfigManager mResConfigManager;

    public LauncherAppWidgetHostView(Context context, Launcher launcher) {
        super(context);
        mContext = context;
        //mLauncher = launcher;
        mLongPressHelper = new CheckLongPressHelper(this);
        //mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //mErrorViewLayout = errorView;
        mResConfigManager = launcher.getResConfigManager();//.inflaterView(ResConfigManager.LAYOUT_APP_WIDGET_ERROR)
    }

    @Override
    protected View getErrorView() {
        //return mInflater.inflate(R.layout.appwidget_error, this, false);
    	return mResConfigManager.inflaterView(IResConfigManager.LAYOUT_APP_WIDGET_ERROR, this, false);
    }

    @Override
    public void updateAppWidget(RemoteViews remoteViews) {
        // Store the orientation in which the widget was inflated
        mPreviousOrientation = mContext.getResources().getConfiguration().orientation;
        super.updateAppWidget(remoteViews);
    }

    public boolean orientationChangedSincedInflation() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (mPreviousOrientation != orientation) {
           return true;
       }
       return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
    	if (Util.DEBUG_MOTION) {
    		Util.Log.d(TAG, "onTouchEvent: ev = " + ev);
        }
    	/// M: ViewGroup.dispatchTouchEvent do not deliver the MotionEvent.ACTION_UP event 
    	/// to onInterceptTouchEvent(), the appWidget has been in the long press state, 
    	/// so we handle MotionEvent.ACTION_UP event on onTouchEvent
    	if (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP) {
            mLongPressHelper.cancelLongPress();
    	}
    	return super.onTouchEvent(ev);
    }
    
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	if (Util.DEBUG_MOTION) {
    		Util.Log.d(TAG, "onInterceptTouchEvent: ev = " + ev);
        }        
    	
        // Consume any touch events for ourselves after longpress is triggered
        if (mLongPressHelper.hasPerformedLongPress()) {
            mLongPressHelper.cancelLongPress();
            return true;
        }

        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mLongPressHelper.postCheckForLongPress();
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressHelper.cancelLongPress();
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }
}
