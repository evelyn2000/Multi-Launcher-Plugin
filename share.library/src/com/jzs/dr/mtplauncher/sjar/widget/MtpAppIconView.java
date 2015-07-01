package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.dr.mtplauncher.sjar.ctrl.HolographicOutlineHelper;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MtpAppIconView extends RelativeLayout {
	protected static final String TAG = "MtpAppIconView"; 

    public PagedViewIcon mAppIcon;
    protected TextView mUnread;
    protected ViewStub mUnreadStub;
    protected ApplicationInfo mInfo;
    protected int mAlpha = 255;
    protected int mHolographicAlpha;

    public MtpAppIconView(final Context context) {
        this(context, null);
    }

    public MtpAppIconView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MtpAppIconView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();  
        
        /* If use the default id, can get the view, but if not, may return null, so 
         * be careful when create the shortcut icon from different layout, make it the same
         * id is very important, like application and boxed_application.
         */
        mAppIcon = (PagedViewIcon)findViewWithTag("app_customize_application_icon");
        mUnreadStub = (ViewStub)findViewWithTag("app_customize_unread_stub"); 
    }

    /**
     * Update unread message of the shortcut, the number of unread information
     * comes from the list.
     */
    public void updateUnreadNum() {
//        if (mInfo.unreadNum <= 0) {
//            if (mUnread != null) {
//                mUnread.setVisibility(View.GONE);
//            }
//        } else {
//            if (mUnread == null) {
//                mUnread = (TextView) mUnreadStub.inflate();
//            }
//            mUnread.setVisibility(View.VISIBLE);
//            if (mInfo.unreadNum > Launcher.MAX_UNREAD_COUNT) {
//                mUnread.setText(MTKUnreadLoader.getExceedText());
//            } else {
//                mUnread.setText(String.valueOf(mInfo.unreadNum));
//            }
//        }
    }

    /**
     * Update the unread message of the shortcut with the given information.
     * 
     * @param unreadNum the number of the unread message.
     */
    public void updateUnreadNum(final int unreadNum) {
        if (Util.DEBUG_UNREAD) {
        	Util.Log.d(TAG, "updateUnreadNum: unreadNum = " + unreadNum + ", mInfo = " + mInfo);
        }
//        if (unreadNum <= 0) {
//            mInfo.unreadNum = 0;
//            if (mUnread != null) {
//                mUnread.setVisibility(View.GONE);
//            }
//        } else {
//            if (mUnread == null) {
//                mUnread = (TextView) mUnreadStub.inflate();
//            }
//            mInfo.unreadNum = unreadNum;
//            mUnread.setVisibility(View.VISIBLE);
//            if (unreadNum > Launcher.MAX_UNREAD_COUNT) {
//                mUnread.setText(MTKUnreadLoader.getExceedText());
//            } else {
//                mUnread.setText(String.valueOf(unreadNum));
//            }
//        }
        setTag(mInfo);
    }

    @Override
    public void setTag(final Object tag) {
        super.setTag(tag);
        mAppIcon.setTag(tag);
        mInfo = (ApplicationInfo) tag;
    }

    public void applyFromApplicationInfo(final ApplicationInfo info, final boolean scaleUp,
            final PagedViewIcon.PressedCallback cb) {
        //LauncherLog.d(TAG, "applyFromApplicationInfo info = " + info + ", mAppIcon = " + mAppIcon);
        mAppIcon.applyFromApplicationInfo(info, scaleUp, cb);
        setTag(info);
        updateUnreadNum();
    }

    @Override
    public void setAlpha(final float alpha) {
        final float viewAlpha = HolographicOutlineHelper.viewAlphaInterpolator(alpha);
        final float holographicAlpha = HolographicOutlineHelper.highlightAlphaInterpolator(alpha);
        int newViewAlpha = (int) (viewAlpha * 255);
        int newHolographicAlpha = (int) (holographicAlpha * 255);
        if ((mAlpha != newViewAlpha) || (mHolographicAlpha != newHolographicAlpha)) {
            mAlpha = newViewAlpha;
            mHolographicAlpha = newHolographicAlpha;
            super.setAlpha(viewAlpha);
        }
    }

    public void invalidateCheckedImage() {
        mAppIcon.invalidate();
    }

}
