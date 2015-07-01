package com.jzs.dr.mtplauncher.sjar.widget;

import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MtpShortcutView extends RelativeLayout implements IShortcutView {
	protected static final String TAG = "MtpShortcutView";
    public BubbleTextView mFavorite;
    public TextView mUnread;
    protected ShortcutInfo mInfo;
    protected boolean mIsShowTitle = true;

    public MtpShortcutView(final Context context) {
        this(context, null);
    }

    public MtpShortcutView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MtpShortcutView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();  

        /*
         * If use the default id, can get the view, but if not, may return null,
         * so be careful when create the shortcut icon from different layout,
         * make it the same id is very important, like application and
         * boxed_application.
         */
        mFavorite = (BubbleTextView) findViewWithTag("app_icon_title");
        mUnread = (TextView)findViewWithTag("app_unread"); 
    }

    @Override
    public void setTag(Object tag) {
        super.setTag(tag);
        mFavorite.setTag(tag);
        mUnread.setTag(tag);
        mInfo = (ShortcutInfo)tag;
    }

    /**
     * Set favorite icon and tag, then update current unread number of the shortcut.
     * 
     * @param info
     * @param iconCache
     */
    public void applyFromShortcutInfo(ShortcutInfo info, IIconCache iconCache) {
        mFavorite.applyFromShortcutInfo(info, iconCache);
       	setTitle(info.title);
        setTag(info);
        updateShortcutUnreadNum();
    }

    /**
     * Set the icon image of the favorite.
     * 
     * @param paramDrawable
     */
    public void setIcon(Drawable paramDrawable) {
        mFavorite.setCompoundDrawablesWithIntrinsicBounds(null, paramDrawable, null, null);
    }

    /**
     * Set the content text of the favorite text view.
     * 
     * @param title
     */
    public void setTitle(CharSequence title) {
        if(mFavorite.getViewLabelVisible())
            mFavorite.setText(title);
        mFavorite.setVisibility(mIsShowTitle ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Get favorite text.
     * 
     * @return
     */
    public CharSequence getTitle() {
        return mFavorite.getText();
    }

    /**
     * Get the top compound drawable in textview.
     * 
     * @return
     */
    public Drawable getFavoriteCompoundDrawable() {
        return mFavorite.getCompoundDrawables()[1];
    }

    /**
     * Update unread message of the shortcut, the number of unread information comes from
     * the list. 
     */
    public void updateShortcutUnreadNum() {
        if (Util.DEBUG_UNREAD) {
        	Util.Log.d(TAG, "updateShortcutUnreadNum: mInfo = " + mInfo + ", this = " + this);
        }
//        if (mInfo.unreadNum <= 0) {
//            mUnread.setVisibility(View.GONE);
//        } else {
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
    public void updateShortcutUnreadNum(int unreadNum) {
    	if (Util.DEBUG_UNREAD) {
        	Util.Log.d(TAG, "updateShortcutUnreadNum: unreadNum = " + unreadNum + ", mInfo = "
                    + mInfo + ", this = " + this);
        }
//        if (unreadNum <= 0) {
//            mInfo.unreadNum = 0;
//            mUnread.setVisibility(View.GONE);
//        } else {
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

    /**
     * Get the unread text of shortcut.
     * 
     * @return
     */   
    public CharSequence getUnreadText() { 
        if (mUnread == null || mUnread.getVisibility() != View.VISIBLE) {
            return "0";
        } else {
            return mUnread.getText();
        }        
    }

    /**
     * Get the visibility of the shortcut unread text.
     * 
     * @return
     */
    public int getUnreadVisibility() {
        if (mUnread != null) {
            return mUnread.getVisibility();
        }

        return View.GONE;
    }

    /**
     * Set the margin right of unread text view, used for user folder in hotseat
     * only.
     * 
     * @param marginRight
     */
    public void setShortcutUnreadMarginRight(int marginRight) {
        MarginLayoutParams params = (MarginLayoutParams) mUnread.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, marginRight, params.bottomMargin);
        if (Util.DEBUG_UNREAD) {
        	Util.Log.d(TAG, "Set shortcut margin right (" + marginRight + ") of shortcut " + mInfo);
        }
        mUnread.setLayoutParams(params);
        mUnread.requestLayout();
    }
    
    public void setShowTitle(boolean isShowTitle){
    	mIsShowTitle = isShowTitle;
    }
    
    public void setViewLabelVisible(boolean isShowTitle){
        if(mFavorite != null){
            mFavorite.setViewLabelVisible(isShowTitle);
        }
    }
}
