package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.View;

public class HotseatIconKeyEventListener implements View.OnKeyListener {
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        final Configuration configuration = v.getResources().getConfiguration();
        return FocusHelper.handleHotseatButtonKeyEvent(v, keyCode, event, configuration.orientation);
    }
}
