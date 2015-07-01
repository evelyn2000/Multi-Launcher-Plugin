package com.jzs.dr.mtplauncher.sjar.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

public class WorkspaceContainer extends FrameLayout {
    
    private final static int MSG_REFRESH = 10;
    
    public interface IScreenUpdateCallback{
        public void onScreenUpdated(View view);
    }
    
    protected static Handler sHandler;
    private WeakReference<IScreenUpdateCallback> mScreenUpdateCallback;
    private final Object mLock = new Object();
    
    public WorkspaceContainer(Context context) {
        this(context, null);
    }

    public WorkspaceContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkspaceContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        sHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH:
                        updateView();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void updateView(){
        synchronized (mLock) {
            final IScreenUpdateCallback callback = getCallbacks();
            if(callback != null)
                callback.onScreenUpdated(this);
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        // TODO Auto-generated method stub
        super.onDetachedFromWindow();        
        setScreenUpdateCallback(null);
    }

    public void setScreenUpdateCallback(IScreenUpdateCallback callback){
        sHandler.removeMessages(MSG_REFRESH);
        
        synchronized (mLock) {
            if(callback != null){
                mScreenUpdateCallback =  new WeakReference<IScreenUpdateCallback>(callback);
            } else if(mScreenUpdateCallback != null){
                mScreenUpdateCallback.clear();
            }
        }
    }
    
    public IScreenUpdateCallback getCallbacks(){
        if(mScreenUpdateCallback != null)
            return mScreenUpdateCallback.get();
        return null;
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.dispatchDraw(canvas);
        //android.util.Log.d("QsLog", "dispatchDraw()===");
        sHandler.sendMessageDelayed(sHandler.obtainMessage(MSG_REFRESH), 100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        
        //android.util.Log.d("QsLog", "onDraw()===");
//        synchronized (mLock) {
//            final IScreenUpdateCallback callback = getCallbacks();
//            if(callback != null)
//                callback.onScreenUpdated(this);
//        }
    }
}
