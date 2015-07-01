package com.jzs.dr.mtplauncher.sjar.ctrl;

import android.graphics.Bitmap;

public class BitmapCache extends WeakReferenceThreadLocal<Bitmap> {
    @Override
    protected Bitmap initialValue() {
        return null;
    }
}
