package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.graphics.Rect;

public class RectCache extends WeakReferenceThreadLocal<Rect> {
    @Override
    protected Rect initialValue() {
        return new Rect();
    }
}
