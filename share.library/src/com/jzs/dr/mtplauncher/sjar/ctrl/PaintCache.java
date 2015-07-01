package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.graphics.Paint;

public class PaintCache extends WeakReferenceThreadLocal<Paint> {
    @Override
    protected Paint initialValue() {
        return null;
    }
}