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

package com.jzs.dr.mtplauncher.sjar.model;

import com.jzs.common.launcher.model.AppWidgetInfo;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.widget.AppWidgetResizeFrame;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.ContentValues;

/**
 * Represents a widget (either instantiated or about to be) in the Launcher.
 */
public class LauncherAppWidgetInfo extends AppWidgetInfo {

    

    private boolean mHasNotifiedInitialWidgetSizeChanged;

    

    public LauncherAppWidgetInfo(int appWidgetId, ComponentName providerName) {
    	super(appWidgetId, providerName);
        
    }

    /**
     * When we bind the widget, we should notify the widget that the size has changed if we have not
     * done so already (only really for default workspace widgets).
     */
    public void onBindAppWidget(Launcher launcher) {
        if (!mHasNotifiedInitialWidgetSizeChanged) {
            notifyWidgetSizeChanged(launcher);
        }
    }

    /**
     * Trigger an update callback to the widget to notify it that its size has changed.
     */
    public void notifyWidgetSizeChanged(Launcher launcher) {
        AppWidgetResizeFrame.updateWidgetSizeRanges(hostView, launcher, spanX, spanY);
        mHasNotifiedInitialWidgetSizeChanged = true;
    }

    public boolean updateWidgetSize(Launcher launcher, int maxSpanX, int maxSpanY, boolean notifyChanged){
        if(minWidth > 0 && minHeight > 0 && (spanX > maxSpanX || spanY > maxSpanY)){
            int[] spanXY = Launcher.getSpanForWidget(launcher, providerName, minWidth, minHeight);
//            android.util.Log.i("QsLog", "==maxSpanX:"+maxSpanX+"==maxSpanY:"+maxSpanY
//                    +"==spanX:"+spanX
//                    +"==spanY:"+spanY
//                    +"==newspanX:"+spanXY[0]
//                    +"==newspanY:"+spanXY[1]
//                    +"==minWidth:"+minWidth
//                    +"==minHeight:"+minHeight);
            
            if(spanX != spanXY[0] || spanY != spanXY[1]){
                spanX = spanXY[0];
                spanY = spanXY[1];
                if(notifyChanged)
                    notifyWidgetSizeChanged(launcher);
                return true;
            }
        }
        
        return false;
    }
    
} 
