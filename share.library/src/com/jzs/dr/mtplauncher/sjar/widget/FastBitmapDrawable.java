/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

public class FastBitmapDrawable extends Drawable {
    private Bitmap mBitmap;
    private int mAlpha;
    private int mWidth;
    private int mHeight;
    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private int mTargetDensity;

    public FastBitmapDrawable(Resources res, Bitmap bitmap) {
        mAlpha = 255;
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = (bitmap != null ? bitmap.getDensity() : DisplayMetrics.DENSITY_DEFAULT);
        }
        setBitmap(bitmap);
    }
    
    public FastBitmapDrawable(Bitmap bitmap) {
    	this(null, bitmap);
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect r = getBounds();
        if(mBitmap != null)
        	canvas.drawBitmap(mBitmap, null, r, mPaint);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mPaint.setAlpha(alpha);
    }

    public void setFilterBitmap(boolean filterBitmap) {
        mPaint.setFilterBitmap(filterBitmap);
    }

    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mHeight;
    }

    public void setBitmap(Bitmap b) {
        if(mBitmap != b){
            mBitmap = b;
            if (b != null) {
                computeBitmapSize();
            } else {
                mWidth = mHeight = 0;
            }
        }
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }
    
    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }
    
    public void setTargetDensity(int density) {
        if (mTargetDensity != density) {
            mTargetDensity = density;
            if (mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }
    
    public int getTargetDensity(){
        return mTargetDensity;
    }
    
    private void computeBitmapSize() {
        if (mBitmap != null) {
            mWidth = mBitmap.getScaledWidth(mTargetDensity);
            mHeight = mBitmap.getScaledHeight(mTargetDensity);
        } else {
            mWidth = mHeight = 0;
        }
    }
}
