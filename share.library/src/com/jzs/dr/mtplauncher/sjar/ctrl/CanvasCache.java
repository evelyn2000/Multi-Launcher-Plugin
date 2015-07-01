package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.graphics.Canvas;

public class CanvasCache extends WeakReferenceThreadLocal<Canvas> {
    @Override
    protected Canvas initialValue() {
        return new Canvas();
    }
}
