package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.view.KeyEvent;
import android.view.View;

public class AppsCustomizeTabKeyEventListener implements View.OnKeyListener {
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeTabKeyEvent(v, keyCode, event);
    }
}
