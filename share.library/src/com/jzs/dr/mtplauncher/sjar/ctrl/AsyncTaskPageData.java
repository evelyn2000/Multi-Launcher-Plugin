package com.jzs.dr.mtplauncher.sjar.ctrl;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

public class AsyncTaskPageData {
    public enum Type {
        LoadWidgetPreviewData
    }

    public AsyncTaskPageData(int p, List<Object> l, List<Bitmap> si, AsyncTaskCallback bgR,
            AsyncTaskCallback postR) {
        page = p;
        items = l;
        sourceImages = si;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = maxImageHeight = -1;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }
    public AsyncTaskPageData(int p, List<Object> l, int cw, int ch, AsyncTaskCallback bgR,
            AsyncTaskCallback postR) {
        page = p;
        items = l;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = cw;
        maxImageHeight = ch;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
    }
    public void cleanup(boolean cancelled) {
        // Clean up any references to source/generated bitmaps
        if (sourceImages != null) {
            if (cancelled) {
                for (Bitmap b : sourceImages) {
                    b.recycle();
                }
            }
            sourceImages.clear();
        }
        if (generatedImages != null) {
            if (cancelled) {
                for (Bitmap b : generatedImages) {
                    b.recycle();
                }
            }
            generatedImages.clear();
        }
    }
    public int page;
    public List<Object> items;
    public List<Bitmap> sourceImages;
    public List<Bitmap> generatedImages;
    public int maxImageWidth;
    public int maxImageHeight;
    public AsyncTaskCallback doInBackgroundCallback;
    public AsyncTaskCallback postExecuteCallback;
}