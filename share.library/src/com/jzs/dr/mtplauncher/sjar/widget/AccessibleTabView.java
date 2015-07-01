package com.jzs.dr.mtplauncher.sjar.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

import com.jzs.dr.mtplauncher.sjar.ctrl.FocusHelper;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

public class AccessibleTabView extends TextView {
    private static final String TAG = "AccessibleTabView";
    
    public AccessibleTabView(Context context) {
        super(context);
    }

    public AccessibleTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibleTabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Util.DEBUG_KEY) {
        	Util.Log.d(TAG, "onKeyDown: keyCode = " + keyCode + ", event = " + event);
        }
        
        return FocusHelper.handleTabKeyEvent(this, keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (Util.DEBUG_KEY) {
        	Util.Log.d(TAG, "onKeyUp: keyCode = " + keyCode + ", event = "  + event);
        }
        
        return FocusHelper.handleTabKeyEvent(this, keyCode, event)
                || super.onKeyUp(keyCode, event);
    }


}
