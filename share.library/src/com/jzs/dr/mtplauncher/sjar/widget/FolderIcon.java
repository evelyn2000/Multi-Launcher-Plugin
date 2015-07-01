package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.ArrayList;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.CheckLongPressHelper;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget.DragObject;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.common.launcher.model.FolderInfo.FolderListener;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jzs.dr.mtplauncher.sjar.R;

public class FolderIcon extends RelativeLayout implements FolderListener, IShortcutView {
	private final static String TAG = "FolderIcon";
	protected Launcher mLauncher;
	protected Folder mFolder;
	protected FolderInfo mInfo;
	protected static boolean sStaticValuesDirty = true;
    
	protected CheckLongPressHelper mLongPressHelper;
    
	protected ImageView mPreviewBackground;
	protected BubbleTextView mFolderName;
	protected TextView mUnreadTextView;
    
    // The number of icons to display in the
	protected static final int NUM_ITEMS_IN_PREVIEW = 4;
    protected static final int CONSUMPTION_ANIMATION_DURATION = 100;
    protected static final int DROP_IN_ANIMATION_DURATION = 400;
    protected static final int INITIAL_ITEM_ANIMATION_DURATION = 350;
    protected static final int FINAL_ITEM_ANIMATION_DURATION = 200;
    // The degree to which the inner ring grows when accepting drop
    protected static final float INNER_RING_GROWTH_FACTOR = 0.15f;

    // The degree to which the outer ring is scaled in its natural state
    protected static final float OUTER_RING_GROWTH_FACTOR = 0.3f;

    // The amount of vertical spread between items in the stack [0...1]
    protected static final float PERSPECTIVE_SHIFT_FACTOR = 0.24f;

    // The degree to which the item in the back of the stack is scaled [0...1]
    // (0 means it's not scaled at all, 1 means it's scaled to nothing)
    protected static final float PERSPECTIVE_SCALE_FACTOR = 0.35f;
    
    public static Drawable sSharedFolderLeaveBehind = null;
    protected FolderRingAnimator mFolderRingAnimator = null;
    // These variables are all associated with the drawing of the preview; they are stored
    // as member variables for shared usage and to avoid computation on each frame
    protected int mIntrinsicIconSize;
    protected float mBaselineIconScale;
    protected int mBaselineIconSize;
    protected int mAvailableSpaceInPreview;
    protected int mTotalWidth = -1;
    protected int mPreviewOffsetX;
    protected int mPreviewOffsetY;
    protected float mMaxPerspectiveShift;
    protected boolean mAnimating = false;
    
    protected PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    protected PreviewItemDrawingParams mAnimParams = new PreviewItemDrawingParams(0, 0, 0, 0);
    protected ArrayList<ShortcutInfo> mHiddenItems = new ArrayList<ShortcutInfo>();
    
//    protected static int sPreviewSize = -1;
//    protected static int sPreviewPadding = -1;
    
	protected String mFolderNameFormat;
	//private int mMaxNumItemsInPreview;
	private int mMaxCellXItemsInPreview;
	private int mMaxCellYItemsInPreview;
	private boolean mIsSupportPreviewChild;
    private int mPrevPreviewChildCount;
    public FolderIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        mLongPressHelper = new CheckLongPressHelper(this);
        //Launcher launcher = (Launcher)context;
        
        //float density = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.FolderIcon, defStyle, 0);
        mFolderNameFormat = a.getString(R.styleable.FolderIcon_folderNameFormat);//, R.string.folder_name_format);
        // R.string.folder_name_format
        //mMaxNumItemsInPreview = a.getInteger(R.styleable.FolderIcon_numItemsInPreview, NUM_ITEMS_IN_PREVIEW);
        mMaxCellXItemsInPreview = a.getInteger(R.styleable.FolderIcon_cellCountX, 2);
        mMaxCellYItemsInPreview = a.getInteger(R.styleable.FolderIcon_cellCountY, 2);
        mIsSupportPreviewChild = a.getBoolean(R.styleable.FolderIcon_isSupportPreviewChild, false);
        
        mPrevPreviewChildCount = 0;
        //sPreviewSize = a.getDimensionPixelSize(R.styleable.FolderIcon_folderPreviewSize, (int)(density * 60));
        //sPreviewPadding = a.getDimensionPixelSize(R.styleable.FolderIcon_folderPreviewPadding, (int)(density * 4));
        a.recycle();
    }
    
    public FolderIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderIcon(Context context) {
        this(context, null);
    }
    
    protected void setup(Launcher launcher, FolderInfo folderInfo){
    	mFolderName = (BubbleTextView) findViewWithTag("folder_icon_name");
    	mLauncher = launcher;
    	
    	ItemInfo info = new ItemInfo();
        info.title = folderInfo.title;
        mFolderName.setTag(info);
        mFolderName.setText(folderInfo.title);
        
        mPreviewBackground = (ImageView) findViewWithTag("preview_background");
        mUnreadTextView = (TextView) findViewWithTag("folder_unread");
 
        setTag(folderInfo);
        setOnClickListener(launcher);
        mInfo = folderInfo;
        
        
        final IResConfigManager resManager = launcher.getResConfigManager();
        
        
      
        if(TextUtils.isEmpty(mFolderNameFormat)){
        	mFolderNameFormat = resManager.getString(IResConfigManager.STR_FOLDER_NAME_FORMAT);
        }
        	
        if(!TextUtils.isEmpty(mFolderNameFormat)){
	        setContentDescription(String.format(mFolderNameFormat,
	                folderInfo.title));
        }
        
        setViewLabelVisible(launcher.getShortcutLableVisiable());
        
        Folder folder = Folder.fromXml(launcher);
        folder.setup(launcher);//(launcher.getDragController());
        folder.setFolderIcon(this);
        folder.bind(folderInfo);
        mFolder = folder;

        mFolderRingAnimator = new FolderRingAnimator(launcher, this);
        folderInfo.addListener(this);
        
        if(isSupportPreviewChild()){
            updatePreviewDrewableItems();
        } else if(!isContainerPreviewImageView()){            
            
            Bitmap icon = folderInfo.getIcon(launcher.getIconCache());
            if(icon != null){
                mFolderName.setCompoundDrawablesWithIntrinsicBounds(null,
                        new FastBitmapDrawable(icon), null, null);
            } else {
                icon = launcher.getIconCache().createIconBitmap(null, 
                        resManager.getDrawable(IResConfigManager.IMG_FOLDER_DEFAULT_ICON));
                mFolderName.setCompoundDrawablesWithIntrinsicBounds(null,
                        new FastBitmapDrawable(icon), null, null);
            }
        }
    }
    
    protected int getNumItemsInPreview(){
    	return mMaxCellXItemsInPreview * mMaxCellYItemsInPreview;//mMaxNumItemsInPreview;
    }

    public static FolderIcon fromXml(Launcher launcher, ViewGroup group,
            FolderInfo folderInfo){
    	return fromXml(launcher, group, folderInfo, null, 0);
    }
    
    public static FolderIcon fromXml(Launcher launcher, ViewGroup group,
            FolderInfo folderInfo, Context context, int resId) {
//        @SuppressWarnings("all") // suppress dead code warning
//        final boolean error = INITIAL_ITEM_ANIMATION_DURATION >= DROP_IN_ANIMATION_DURATION;
//        if (error) {
//            throw new IllegalStateException("DROP_IN_ANIMATION_DURATION must be greater than " +
//                    "INITIAL_ITEM_ANIMATION_DURATION, as sequencing of adding first two items " +
//                    "is dependent on this");
//        }

        FolderIcon icon = null;
        if(resId > 0 && context != null){
        	icon = (FolderIcon) LayoutInflater.from(context).inflate(resId, group, false);
        }
        if(icon == null)
        	icon = (FolderIcon) launcher.getResConfigManager().inflaterView(IResConfigManager.LAYOUT_FOLDER_ICON, group);//LayoutInflater.from(launcher).inflate(resId, group, false);

        if(icon != null)
        	icon.setup(launcher, folderInfo);

        return icon;
    }
    
    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public boolean isDropEnabled() {
        final ViewGroup cellLayoutChildren = (ViewGroup) getParent();
        final ViewGroup cellLayout = (ViewGroup) cellLayoutChildren.getParent();
        final Workspace workspace = (Workspace) cellLayout.getParent();
        return workspace.isDropEnabled();//!workspace.isSmall();
    }
    
    public Folder getFolder() {
        return mFolder;
    }

    public FolderInfo getFolderInfo() {
        return mInfo;
    }

    protected boolean willAcceptItem(ItemInfo item) {
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                !mFolder.isFull() && item != mInfo && !mInfo.opened);
    }

    public boolean acceptDrop(Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
        return !mFolder.isDestroyed() && willAcceptItem(item);
    }

    public void addItem(ShortcutInfo item) {
        mInfo.add(item);
    }

    public void onDragEnter(Object dragInfo) {
        if (mFolder.isDestroyed() || !willAcceptItem((ItemInfo) dragInfo)) return;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        CellLayout layout = (CellLayout) getParent().getParent();
        mFolderRingAnimator.setCell(lp.cellX, lp.cellY);
        mFolderRingAnimator.setCellLayout(layout);
        mFolderRingAnimator.animateToAcceptState();
        layout.showFolderAccept(mFolderRingAnimator);
    }

    public void onDragOver(Object dragInfo) {
    	
    }
    
    public void onDragExit(Object dragInfo) {
        onDragExit();
    }

    public void onDragExit() {
        if(mFolderRingAnimator != null)
            mFolderRingAnimator.animateToNaturalState();
    }

    public void onDrop(DragObject d) {
//    	if(LauncherLog.DEBUG) {
//    	    LauncherLog.d(TAG, "onDrop: DragObject = " + d);
//    	}

        ShortcutInfo item;
        if (d.dragInfo instanceof ApplicationInfo) {
            // Came from all apps -- make a copy
            item = ((ApplicationInfo) d.dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        mFolder.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, mInfo.contents.size(), d.postAnimationRunnable, d);
    }

    public DropTarget getDropTargetDelegate(DragObject d) {
        return null;
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mFolder == null) return;
        if (mFolder.getItemCount() == 0 && !mAnimating) return;

        if(mPreviewBackground != null){
	        ArrayList<View> items = mFolder.getItemsInReadingOrder(false);
	        Drawable d;
	        MtpShortcutView v;
	
	        // Update our drawing parameters if necessary
	        if (mAnimating) {
	            computePreviewDrawingParams(mAnimParams.drawable);
	        } else {
	            v = (MtpShortcutView) items.get(0);
	            d = v.getFavoriteCompoundDrawable();
	            computePreviewDrawingParams(d);
	        }
	
	        int nItemsInPreview = Math.min(items.size(), getNumItemsInPreview());
	        if (!mAnimating) {
	            for (int i = nItemsInPreview - 1; i >= 0; i--) {
	                v = (MtpShortcutView) items.get(i);
	                if (!mHiddenItems.contains(v.getTag())) {
	                    d = v.getFavoriteCompoundDrawable();
	                    mParams = computePreviewItemDrawingParams(i, mParams);
	                    mParams.drawable = d;
	                    drawPreviewItem(canvas, mParams);
	                }
	            }
	        } else {
	            drawPreviewItem(canvas, mAnimParams);
	        }
        }
        /*
         * M: Because the clip region doesn't work while hardware accelerated is
         * enabled, we need to draw the unread text again to make it display on
         * the very top. 
         */      
        if (mUnreadTextView != null && mUnreadTextView.getVisibility() == View.VISIBLE) {
            drawChild(canvas, mUnreadTextView, getDrawingTime());
        }
    }

    public boolean isSupportPreviewChild(){
    	return (mIsSupportPreviewChild || (mPreviewBackground != null ? true : false));
    }
    
    public boolean isContainerPreviewImageView(){
        return (mPreviewBackground != null ? true : false);
    }

    public void setTextVisible(boolean visible) {
        if(isContainerPreviewImageView()){
            if (visible) {
                mFolderName.setVisibility(VISIBLE);
            } else {
                mFolderName.setVisibility(INVISIBLE);
            }
        }
    }
    
    public void setViewLabelVisible(boolean visible){
        if(mFolderName != null)
            mFolderName.setViewLabelVisible(visible);
    }

    public boolean getTextVisible() {
        return mFolderName.getVisibility() == VISIBLE;
    }

    public void onItemsChanged() {
        updatePreviewDrewableItems();
        invalidate();
        requestLayout();
    }

    public void onAdd(ShortcutInfo item) {
//    	if (LauncherLog.DEBUG) {
//    		LauncherLog.d(TAG, "onAdd item = " + item);
//    	}
        /// M: added for unread feature, when add a item to a folder, we need to update
        /// the unread num of the folder.
        final ComponentName componentName = item.intent.getComponent();
        //updateFolderUnreadNum(componentName, item.unreadNum);
        invalidate();
        requestLayout();
    }

    public void onRemove(ShortcutInfo item) {
//    	if (LauncherLog.DEBUG) {
//    		LauncherLog.d(TAG, "onRemove item = " + item);
//    	}
        /// M: added for Unread feature, when remove a item from a folder, we need to update
        /// the unread num of the folder
        final ComponentName componentName = item.intent.getComponent();
        //updateFolderUnreadNum(componentName, item.unreadNum);
        invalidate();
        requestLayout();
    }

    public void onTitleChanged(CharSequence title) {
        ItemInfo info = (ItemInfo)mFolderName.getTag();
        if(info != null)
            info.title = title;
        
        mFolderName.setText(title.toString());
        if(!TextUtils.isEmpty(mFolderNameFormat)){
	        setContentDescription(String.format(mFolderNameFormat,
	                title));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLongPressHelper.cancelLongPress();
                break;
        }
        return result;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }
    
//    @Override
//    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        // TODO Auto-generated method stub
//        super.onSizeChanged(w, h, oldw, oldh);
//               
//        if(mPreviewBackground == null && mFolderName != null){
//            mFolderName.onSizeChanged(w, h, oldw, oldh);
//        } else {
//            android.util.Log.w("QsLog", "Folder::onSizeChanged(2)=="
//                    +"==w:"+w
//                    +"==h:"+h
//                    +"==oldw:"+oldw
//                    +"==oldh:"+oldh
//                    +"==title:"+(mFolderName != null ? mFolderName.getText() : "null"));
//        }
//    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mUnreadTextView != null && mUnreadTextView.getVisibility() == View.VISIBLE && mUnreadTextView.getLeft() == 0) {
            invalidate();
        }

        super.onLayout(changed, l, t, r, b);
    }
    
    public void setFolderUnreadMarginRight(int marginRight) {
        MarginLayoutParams params = (MarginLayoutParams) mUnreadTextView.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, marginRight, params.bottomMargin);

        mUnreadTextView.setLayoutParams(params);
        mUnreadTextView.requestLayout();
    }

    /**
     * M: Update unread number of the folder, the number is the total unread number
     * of all shortcuts in folder, duplicate shortcut will be only count once.
     */
    public void updateFolderUnreadNum() {
//        final ArrayList<ShortcutInfo> contents = mInfo.contents;
//        final int contentsCount = contents.size();
//        int unreadNumTotal = 0;
//        final ArrayList<ComponentName> components = new ArrayList<ComponentName>();
//        ShortcutInfo shortcutInfo = null;
//        ComponentName componentName = null;
//        int unreadNum = 0;
//        for (int i = 0; i < contentsCount; i++) {
//            shortcutInfo = contents.get(i);
//            componentName = shortcutInfo.intent.getComponent();
//            unreadNum = MTKUnreadLoader.getUnreadNumberOfComponent(componentName);
//            if (unreadNum > 0) {
//                shortcutInfo.unreadNum = unreadNum;
//                int j = 0;
//                for (j = 0; j < components.size(); j++) {
//                    if (componentName != null && componentName.equals(components.get(j))) {
//                        break;
//                    }
//                }
//
//                if (j >= components.size()) {
//                    components.add(componentName);
//                    unreadNumTotal += unreadNum;
//                }
//            }
//        }
//
//        setFolderUnreadNum(unreadNumTotal);
    }

    /**
     * M: Update the unread message of the shortcut with the given information.
     * 
     * @param unreadNum the number of the unread message.
     */
    public void updateFolderUnreadNum(ComponentName component, int unreadNum) {
//        final ArrayList<ShortcutInfo> contents = mInfo.contents;
//        final int contentsCount = contents.size();
//        int unreadNumTotal = 0;
//        ShortcutInfo appInfo = null;
//        ComponentName name = null;
//        final ArrayList<ComponentName> components = new ArrayList<ComponentName>();
//        for (int i = 0; i < contentsCount; i++) {
//            appInfo = contents.get(i);
//            name = appInfo.intent.getComponent();
//            if (name != null && name.equals(component)) {
//                appInfo.unreadNum = unreadNum;
//            }
//            if (appInfo.unreadNum > 0) {
//                int j = 0;
//                for (j = 0; j < components.size(); j++) {
//                    if (name != null && name.equals(components.get(j))) {
//                        break;
//                    }
//                }
//
//                if (j >= components.size()) {
//                    components.add(name);
//                    unreadNumTotal += appInfo.unreadNum;
//                }
//            }
//        }
//
//        setFolderUnreadNum(unreadNumTotal);
    }
    
    public void setFolderUnreadNum(int unreadNum) {
//        if (unreadNum <= 0) {
//            mInfo.unreadNum = 0;
//            mUnreadTextView.setVisibility(View.GONE);
//        } else {
//            mInfo.unreadNum = unreadNum;
//            mUnreadTextView.setVisibility(View.VISIBLE);
//            if (unreadNum > Launcher.MAX_UNREAD_COUNT) {
//            	mUnreadTextView.setText(MTKUnreadLoader.getExceedText());
//            } else {
//            	mUnreadTextView.setText(String.valueOf(unreadNum));
//            }
//        }
    }
    
    public int getUnreadVisibility() {
        if (mUnreadTextView != null) {
            return mUnreadTextView.getVisibility();
        }
        
        return View.GONE;
    }
    
    /**
     * M: Reset the value of the variable of sStaticValuesDirty.
     */
    public static void resetValuesDirty() {
        sStaticValuesDirty = true;
    }
    
    protected void onDrop(final ShortcutInfo item, DragView animateView, Rect finalRect,
            float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable,
            DragObject d) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDrop: item = " + item + ", animateView = "
                    + animateView + ", finalRect = " + finalRect + ", scaleRelativeToDragLayer = "
                    + scaleRelativeToDragLayer + ", index = " + index + ", d = " + d);
        }

        item.cellX = -1;
        item.cellY = -1;
        //android.util.Log.i("QsLog", "==onDrop(0)==="+animateView);
        // Typically, the animateView corresponds to the DragView; however, if this is being done
        // after a configuration activity (ie. for a Shortcut being dragged from AllApps) we
        // will not have a view to animate
        if (animateView != null) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(animateView, from);
            Rect to = finalRect;
            if (to == null) {
                to = new Rect();
                Workspace workspace = mLauncher.getWorkspace();
                // Set cellLayout and this to it's final state to compute final animation locations
                workspace.setFinalTransitionTransform((CellLayout) getParent().getParent());
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
                // Finished computing final animation locations, restore current state
                setScaleX(scaleX);
                setScaleY(scaleY);
                workspace.resetTransitionTransform((CellLayout) getParent().getParent());
            }

            int[] center = new int[2];
            float scale = getLocalCenterForIndex(index, center);
            center[0] = (int) Math.round(scaleRelativeToDragLayer * center[0]);
            center[1] = (int) Math.round(scaleRelativeToDragLayer * center[1]);

            to.offset(center[0] - animateView.getMeasuredWidth() / 2,
                    center[1] - animateView.getMeasuredHeight() / 2);

            float finalAlpha = index < getNumItemsInPreview() ? 0.5f : 0f;

            float finalScale = scale * scaleRelativeToDragLayer;
            dragLayer.animateView(animateView, from, to, finalAlpha,
                    1, 1, finalScale, finalScale, DROP_IN_ANIMATION_DURATION,
                    new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                    postAnimationRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
            addItem(item);
            mHiddenItems.add(item);
            postDelayed(new Runnable() {
                public void run() {
                    mHiddenItems.remove(item);
                    invalidate();
                }
            }, DROP_IN_ANIMATION_DURATION);
        } else {
            addItem(item);
        }
    }
    
    private boolean updatePreviewDrewableItems(){
        //android.util.Log.i("QsLog", "==updatePreviewDrewableItems(0)===");
        if(mLauncher == null || mFolderName == null) return false;
        if(!isSupportPreviewChild() || isContainerPreviewImageView()) return false;
        
        final IResConfigManager resManager = mLauncher.getResConfigManager();
        
        Drawable bg = resManager.getDrawable(IResConfigManager.IMG_FOLDER_PREVIEW_BG_ICON);
        ArrayList<View> items = mFolder.getItemsInReadingOrder(true);
        int nItemsInPreview = Math.min(items.size(), getNumItemsInPreview());
//        android.util.Log.i("QsLog", "==updatePreviewDrewableItems(2)==="+nItemsInPreview
//                +", size:"+items.size());
        if(nItemsInPreview > 0 && mPrevPreviewChildCount != nItemsInPreview){
            mPrevPreviewChildCount = nItemsInPreview;
            int sPreviewPadding = resManager.getDimensionPixelSize(IResConfigManager.DIM_FOLDER_ICON_PREVIEW_PADDING);
            Rect bgpadding = new Rect();
            bg.getPadding(bgpadding);
            
            int validwidth = bg.getIntrinsicWidth() - bgpadding.left - bgpadding.right;
            int validheight = bg.getIntrinsicHeight() - bgpadding.top - bgpadding.top;
            int cellx = (validwidth - (mMaxCellXItemsInPreview + 1) * sPreviewPadding) / mMaxCellXItemsInPreview;
            int celly = (validheight - (mMaxCellYItemsInPreview + 1) * sPreviewPadding) / mMaxCellYItemsInPreview;
            int sPreviewSize = Math.min(cellx, celly);
            
            int xpadding = (validwidth - mMaxCellXItemsInPreview*sPreviewSize) / (mMaxCellXItemsInPreview + 1);
            int ypadding = (validheight - mMaxCellYItemsInPreview*sPreviewSize) / (mMaxCellYItemsInPreview + 1);
            
//            android.util.Log.i("QsLog", "=nItemsInPreview:"+nItemsInPreview
//                    +"=xpadding:"+xpadding
//                    +"=ypadding:"+ypadding
//                    +"=sPreviewSize:"+sPreviewSize
//                    +"=left:"+bgpadding.left
//                    +"=right:"+bgpadding.right
//                    +"=top:"+bgpadding.top
//                    +"=bot:"+bgpadding.bottom);
            
            Bitmap bitmap = Bitmap.createBitmap(Math.max(bg.getIntrinsicWidth(), 1),
                    Math.max(bg.getIntrinsicHeight(), 1),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            bg.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            bg.draw(canvas);
            
            Rect oldbounds = new Rect();
            int i = 0, j = 0;
            while(i < items.size() && j < nItemsInPreview){
                MtpShortcutView v = (MtpShortcutView) items.get(i);
                //if (!mHiddenItems.contains(v.getTag())) {
                    Drawable d = v.getFavoriteCompoundDrawable();
                    if(d != null){
                        cellx = bgpadding.left + xpadding + (j%mMaxCellXItemsInPreview)*(xpadding + sPreviewSize);
                        celly = bgpadding.top + ypadding + (j/mMaxCellYItemsInPreview)*(ypadding + sPreviewSize);
                        d.copyBounds(oldbounds);
                        d.setBounds(cellx, celly, cellx + sPreviewSize, celly + sPreviewSize);
                        d.draw(canvas);
                        d.setBounds(oldbounds);
                        j++;
                    }
                //}
                i++;
            }
            canvas.setBitmap(null);

            mFolderName.setCompoundDrawablesWithIntrinsicBounds(null,
                    new FastBitmapDrawable(bitmap), null, null);
            return true;
        } else if(nItemsInPreview == 0){
            mFolderName.setCompoundDrawablesWithIntrinsicBounds(null,
                    bg, null, null);
        }
        
        return false;
    }
    
	protected class PreviewItemDrawingParams {
		public PreviewItemDrawingParams(float transX, float transY,
				float scale, int overlayAlpha) {
			this.transX = transX;
			this.transY = transY;
			this.scale = scale;
			this.overlayAlpha = overlayAlpha;
		}

		public float transX;
		public float transY;
		public float scale;
		public int overlayAlpha;
		public Drawable drawable;
	}

	protected void computePreviewDrawingParams(int drawableSize, int totalSize) {
        if (mIntrinsicIconSize != drawableSize || mTotalWidth != totalSize) {
            mIntrinsicIconSize = drawableSize;
            mTotalWidth = totalSize;

            final int previewSize = FolderRingAnimator.sPreviewSize;
            final int previewPadding = FolderRingAnimator.sPreviewPadding;

            mAvailableSpaceInPreview = (previewSize - 2 * previewPadding);
            // cos(45) = 0.707  + ~= 0.1) = 0.8f
            int adjustedAvailableSpace = (int) ((mAvailableSpaceInPreview / 2) * (1 + 0.8f));

            int unscaledHeight = (int) (mIntrinsicIconSize * (1 + PERSPECTIVE_SHIFT_FACTOR));
            mBaselineIconScale = (1.0f * adjustedAvailableSpace / unscaledHeight);

            mBaselineIconSize = (int) (mIntrinsicIconSize * mBaselineIconScale);
            mMaxPerspectiveShift = mBaselineIconSize * PERSPECTIVE_SHIFT_FACTOR;

            mPreviewOffsetX = (mTotalWidth - mAvailableSpaceInPreview) / 2;
            mPreviewOffsetY = previewPadding;
        }
    }

	protected void computePreviewDrawingParams(Drawable d) {
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    protected float getLocalCenterForIndex(int index, int[] center) {
        mParams = computePreviewItemDrawingParams(Math.min(getNumItemsInPreview(), index), mParams);

        mParams.transX += mPreviewOffsetX;
        mParams.transY += mPreviewOffsetY;
        float offsetX = mParams.transX + (mParams.scale * mIntrinsicIconSize) / 2;
        float offsetY = mParams.transY + (mParams.scale * mIntrinsicIconSize) / 2;

        center[0] = (int) Math.round(offsetX);
        center[1] = (int) Math.round(offsetY);
        return mParams.scale;
    }

    protected PreviewItemDrawingParams computePreviewItemDrawingParams(int index,
            PreviewItemDrawingParams params) {
        index = getNumItemsInPreview() - index - 1;
        float r = (index * 1.0f) / (getNumItemsInPreview() - 1);
        float scale = (1 - PERSPECTIVE_SCALE_FACTOR * (1 - r));

        float offset = (1 - r) * mMaxPerspectiveShift;
        float scaledSize = scale * mBaselineIconSize;
        float scaleOffsetCorrection = (1 - scale) * mBaselineIconSize;

        // We want to imagine our coordinates from the bottom left, growing up and to the
        // right. This is natural for the x-axis, but for the y-axis, we have to invert things.
        float transY = mAvailableSpaceInPreview - (offset + scaledSize + scaleOffsetCorrection);
        float transX = offset + scaleOffsetCorrection;
        float totalScale = mBaselineIconScale * scale;
        final int overlayAlpha = (int) (80 * (1 - r));

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
        } else {
            params.transX = transX;
            params.transY = transY;
            params.scale = totalScale;
            params.overlayAlpha = overlayAlpha;
        }
        return params;
    }

    protected void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.translate(params.transX + mPreviewOffsetX, params.transY + mPreviewOffsetY);
        canvas.scale(params.scale, params.scale);
        Drawable d = params.drawable;

        if (d != null) {
            d.setBounds(0, 0, mIntrinsicIconSize, mIntrinsicIconSize);
            d.setFilterBitmap(true);
            d.setColorFilter(Color.argb(params.overlayAlpha, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            d.draw(canvas);
            d.clearColorFilter();
            d.setFilterBitmap(false);
        }
        canvas.restore();
    }
	    
	public void performCreateAnimation(final ShortcutInfo destInfo, final View destView,
            final ShortcutInfo srcInfo, final DragView srcView, Rect dstRect,
            float scaleRelativeToDragLayer, Runnable postAnimationRunnable) {
        // These correspond two the drawable and view that the icon was dropped _onto_
        /// M: modidfied for unread feature.
        Drawable animateDrawable = ((MtpShortcutView) destView).getFavoriteCompoundDrawable();
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), destView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, INITIAL_ITEM_ANIMATION_DURATION, false, null);
        addItem(destInfo);

        // This will animate the dragView (srcView) into the new folder
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable, null);
    }

    public void performDestroyAnimation(final View finalView, Runnable onCompleteRunnable) {
        Drawable animateDrawable = ((MtpShortcutView) finalView).getFavoriteCompoundDrawable();
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(), 
                finalView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, FINAL_ITEM_ANIMATION_DURATION, true,
                onCompleteRunnable);
    }
    
    protected void animateFirstItem(final Drawable d, int duration, final boolean reverse,
            final Runnable onCompleteRunnable) {
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);

        final float scale0 = 1.0f;
        final float transX0 = (mAvailableSpaceInPreview - d.getIntrinsicWidth()) / 2;
        final float transY0 = (mAvailableSpaceInPreview - d.getIntrinsicHeight()) / 2;
        mAnimParams.drawable = d;

        ValueAnimator va = LauncherAnimUtils.ofFloat(0f, 1.0f);
        if(mPreviewBackground != null){
	        va.addUpdateListener(new AnimatorUpdateListener(){
	            public void onAnimationUpdate(ValueAnimator animation) {
	                float progress = (Float) animation.getAnimatedValue();
	                if (reverse) {
	                    progress = 1 - progress;
	                    mPreviewBackground.setAlpha(progress);
	                }
	
	                mAnimParams.transX = transX0 + progress * (finalParams.transX - transX0);
	                mAnimParams.transY = transY0 + progress * (finalParams.transY - transY0);
	                mAnimParams.scale = scale0 + progress * (finalParams.scale - scale0);
	                invalidate();
	            }
	        });
        }
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        });
        va.setDuration(duration);
        va.start();
    }
    
    public static class FolderRingAnimator {
        public int mCellX;
        public int mCellY;
        protected CellLayout mCellLayout;
        public float mOuterRingSize;
        public float mInnerRingSize;
        public FolderIcon mFolderIcon = null;
        public Drawable mOuterRingDrawable = null;
        public Drawable mInnerRingDrawable = null;
        public static Drawable sSharedOuterRingDrawable = null;
        public static Drawable sSharedInnerRingDrawable = null;
        
        public static int sPreviewSize = -1;
        public static int sPreviewPadding = -1;

        protected ValueAnimator mAcceptAnimator;
        protected ValueAnimator mNeutralAnimator;

        public FolderRingAnimator(Launcher launcher, FolderIcon folderIcon) {
            mFolderIcon = folderIcon;
            //Resources res = launcher.getResources();
            IResConfigManager resManager = launcher.getResConfigManager();
            mOuterRingDrawable = resManager.getDrawable(IResConfigManager.IMG_FOLDER_RING_ANIMATOR_OUTER);//res.getDrawable(R.drawable.portal_ring_outer_holo);
            mInnerRingDrawable = resManager.getDrawable(IResConfigManager.IMG_FOLDER_RING_ANIMATOR_INNER);//res.getDrawable(R.drawable.portal_ring_inner_holo);

            // We need to reload the static values when configuration changes in case they are
            // different in another configuration
            if (sStaticValuesDirty) {
                sPreviewSize = resManager.getDimensionPixelSize(IResConfigManager.DIM_FOLDER_ICON_PREVIEW_SIZE);//res.getDimensionPixelSize(R.dimen.folder_preview_size);
                sPreviewPadding = resManager.getDimensionPixelSize(IResConfigManager.DIM_FOLDER_ICON_PREVIEW_PADDING);//res.getDimensionPixelSize(R.dimen.folder_preview_padding);
                sSharedOuterRingDrawable = resManager.getDrawable(IResConfigManager.IMG_FOLDER_RING_ANIMATOR_SHARED_OUTER);//res.getDrawable(R.drawable.portal_ring_outer_holo);
                sSharedInnerRingDrawable = resManager.getDrawable(IResConfigManager.IMG_FOLDER_RING_ANIMATOR_SHARED_INNER);//res.getDrawable(R.drawable.portal_ring_inner_holo);
                sSharedFolderLeaveBehind = resManager.getDrawable(IResConfigManager.IMG_FOLDER_RING_ANIMATOR_SHARED_LEAVE);//res.getDrawable(R.drawable.portal_ring_rest);
                sStaticValuesDirty = false;
            }
        }

        public void animateToAcceptState() {
            if (mNeutralAnimator != null) {
                mNeutralAnimator.cancel();
            }
            mAcceptAnimator = LauncherAnimUtils.ofFloat(0f, 1f);
            mAcceptAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = sPreviewSize;
            mAcceptAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    mOuterRingSize = (1 + percent * OUTER_RING_GROWTH_FACTOR) * previewSize;
                    mInnerRingSize = (1 + percent * INNER_RING_GROWTH_FACTOR) * previewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            if(mFolderIcon != null && mFolderIcon.mPreviewBackground != null){
	            mAcceptAnimator.addListener(new AnimatorListenerAdapter() {
	                @Override
	                public void onAnimationStart(Animator animation) {
	                    if (mFolderIcon != null) {
	                        mFolderIcon.mPreviewBackground.setVisibility(INVISIBLE);
	                    }
	                }
	            });
            }
            mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
            if (mAcceptAnimator != null) {
                mAcceptAnimator.cancel();
            }
            mNeutralAnimator = LauncherAnimUtils.ofFloat(0f, 1f);
            mNeutralAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = sPreviewSize;
            mNeutralAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    mOuterRingSize = (1 + (1 - percent) * OUTER_RING_GROWTH_FACTOR) * previewSize;
                    mInnerRingSize = (1 + (1 - percent) * INNER_RING_GROWTH_FACTOR) * previewSize;
                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            if(mFolderIcon != null && mFolderIcon.mPreviewBackground != null){
                mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mFolderIcon != null) {
                            mFolderIcon.mPreviewBackground.setVisibility(VISIBLE);
                        }
                    }
                });
            }
            mNeutralAnimator.start();
            /// M: Call the fun 'hideFolderAccept' outside neutral animation, avoid to 
            /// draw folder outer ring at wrong position due to not remove folder outer ring promptly.
            if (mCellLayout != null) {
                mCellLayout.hideFolderAccept(FolderRingAnimator.this);
            }
        }

        // Location is expressed in window coordinates
        public void getCell(int[] loc) {
            loc[0] = mCellX;
            loc[1] = mCellY;
        }

        // Location is expressed in window coordinates
        public void setCell(int x, int y) {
            mCellX = x;
            mCellY = y;
        }

        public void setCellLayout(CellLayout layout) {
            mCellLayout = layout;
        }

        public float getOuterRingSize() {
            return mOuterRingSize;
        }

        public float getInnerRingSize() {
            return mInnerRingSize;
        }
    }
}
