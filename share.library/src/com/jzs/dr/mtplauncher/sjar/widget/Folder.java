package com.jzs.dr.mtplauncher.sjar.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.jzs.common.launcher.IResConfigManager;
import com.jzs.dr.mtplauncher.sjar.Launcher;
import com.jzs.dr.mtplauncher.sjar.ctrl.Alarm;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragController;
import com.jzs.dr.mtplauncher.sjar.ctrl.DragSource;
import com.jzs.dr.mtplauncher.sjar.ctrl.DropTarget;
import com.jzs.dr.mtplauncher.sjar.ctrl.FolderKeyEventListener;
import com.jzs.dr.mtplauncher.sjar.ctrl.LauncherAnimUtils;
import com.jzs.dr.mtplauncher.sjar.ctrl.OnAlarmListener;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.FolderInfo;
import com.jzs.dr.mtplauncher.sjar.model.IconCache;
import com.jzs.common.launcher.model.ItemInfo;
import com.jzs.dr.mtplauncher.sjar.model.LauncherModel;
import com.jzs.dr.mtplauncher.sjar.model.LauncherSettings;
import com.jzs.common.launcher.model.ShortcutInfo;
import com.jzs.dr.mtplauncher.sjar.utils.Util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.TextView;

import com.jzs.dr.mtplauncher.sjar.R;

public class Folder extends LinearLayout implements DragSource, View.OnClickListener,
		View.OnLongClickListener, DropTarget, FolderInfo.FolderListener, TextView.OnEditorActionListener,
		View.OnFocusChangeListener {
    
    public final static boolean SUPPORT_EMPTY_FOLDER = false;
	
	protected static final String TAG = "MtpLauncher.Folder";

    protected DragController mDragController;
    protected Launcher mLauncher;
    public FolderInfo mInfo;

    public static final int STATE_NONE = -1;
    public static final int STATE_SMALL = 0;
    public static final int STATE_ANIMATING = 1;
    public static final int STATE_OPEN = 2;
    
    
    protected CellLayout mContent;
    //protected final LayoutInflater mInflater;
    //protected final IconCache mIconCache;
    protected int mState = STATE_NONE;
    
    protected FolderIcon mFolderIcon;
    protected int mMaxCountX;
    protected int mMaxCountY;
    protected int mMaxNumItems;
    
    protected Alarm mReorderAlarm = new Alarm();
    protected Alarm mOnExitAlarm = new Alarm();
    
    protected Drawable mIconDrawable;
    protected FolderEditText mFolderName;
    
    private boolean mIsEditingName = false;
    protected InputMethodManager mInputMethodManager;

    protected static int mExpandDuration;
    protected static String sDefaultFolderName;
    protected static String sHintText;
    protected boolean mDestroyed;
    
    protected ObjectAnimator mOpenCloseAnimator;
    protected static String sForderRenameFormat;
    protected static String sForderOpenedFormat;
    protected static String sForderClosedFormat;
    protected int mFolderNameHeight;
    
    protected static final int REORDER_ANIMATION_DURATION = 230;
    protected static final int ON_EXIT_CLOSE_DELAY = 800;
    protected boolean mItemsInvalidated = false;
    protected ShortcutInfo mCurrentDragInfo;
    protected View mCurrentDragView;
    protected boolean mSuppressOnAdd = false;
    protected int[] mTargetCell = new int[2];
    protected int[] mPreviousTargetCell = new int[2];
    protected int[] mEmptyCell = new int[2];
    protected Rect mTempRect = new Rect();
    protected boolean mDragInProgress = false;
    protected boolean mDeleteFolderOnDropCompleted = false;
    protected boolean mSuppressFolderDeletion = false;
    protected boolean mItemAddedBackToSelfViaIcon = false;
    protected boolean mRearrangeOnClose = false;
    protected ArrayList<View> mItemsInReadingOrder = new ArrayList<View>();
    protected float mFolderIconPivotX;
    protected float mFolderIconPivotY;

    public Folder(Context context) {
        this(context, null);
    }
    
    public Folder(Context context, AttributeSet attrs) {
    	this(context, attrs, 0);
    }
    
    public Folder(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAlwaysDrawnWithCacheEnabled(false);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
        		R.styleable.Folder, defStyle, 0);
        if (sDefaultFolderName == null) {
        	sDefaultFolderName = a.getString(R.styleable.Folder_defaultFolderName);
        }
        if (sHintText == null) {
            sHintText = a.getString(R.styleable.Folder_folderHintText);
        }
        
        mMaxCountX = a.getInteger(R.styleable.Folder_cellCountX, -1);
        mMaxCountY = a.getInteger(R.styleable.Folder_cellCountY, -1);
        mMaxNumItems = a.getInteger(R.styleable.Folder_folderMaxNumItems, 16);
//        if (mMaxCountX < 0 || mMaxCountY < 0 || mMaxNumItems < 0) {
//            mMaxCountX = LauncherModel.getCellCountX();
//            mMaxCountY = LauncherModel.getCellCountY();
//            mMaxNumItems = mMaxCountX * mMaxCountY;
//        }
        if (sForderRenameFormat == null) {
        	sForderRenameFormat = a.getString(R.styleable.Folder_folderRenameFormat);
        }
        if (sForderOpenedFormat == null) {
        	sForderOpenedFormat = a.getString(R.styleable.Folder_folderOpenedFormat);
        }
        if (sForderClosedFormat == null) {
        	sForderClosedFormat = a.getString(R.styleable.Folder_folderClosedFormat);
        }
//        mForderRenameFormat = a.getResourceId(R.styleable.Folder_folderRenameFormat, R.string.folder_renamed);
//        mForderOpenedFormat = a.getResourceId(R.styleable.Folder_folderOpenedFormat, R.string.folder_opened);
//        mForderClosedFormat = a.getResourceId(R.styleable.Folder_folderClosedFormat, R.string.folder_closed);
        a.recycle();
        
        mInputMethodManager = (InputMethodManager)
        		context.getSystemService(Context.INPUT_METHOD_SERVICE);
        
        setFocusableInTouchMode(true);
    }
    
    public void setup(Launcher launcher){
    	mLauncher = launcher;
    	mDragController = launcher.getDragController();
    	if(mContent != null)
    		mContent.setLauncher(launcher);
    	
    	if (mMaxCountX < 0 || mMaxCountY < 0 || mMaxNumItems < 0) {
            mMaxCountX = launcher.getWorkspaceCellCountX();
            mMaxCountY = launcher.getWorkspaceCellCountY();
            mMaxNumItems = mMaxCountX * mMaxCountY;
        }
    }
    
    public void setDragController(DragController dragController) {
        mDragController = dragController;
    }

    public void setFolderIcon(FolderIcon icon) {
        mFolderIcon = icon;
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mContent = (CellLayout) super.findViewWithTag("folder_content");
        if(mContent != null){
        	if(mLauncher != null)
        		mContent.setLauncher(mLauncher);
	        mContent.setGridSize(1, 1);
	        mContent.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        }
        mFolderName = (FolderEditText) super.findViewWithTag("folder_name");
        mFolderName.setFolder(this);
        mFolderName.setOnFocusChangeListener(this);

        // We find out how tall the text view wants to be (it is set to wrap_content), so that
        // we can allocate the appropriate amount of space for it.
        int measureSpec = MeasureSpec.UNSPECIFIED;
        mFolderName.measure(measureSpec, measureSpec);
        mFolderNameHeight = mFolderName.getMeasuredHeight();

        // We disable action mode for now since it messes up the view on phones
        mFolderName.setCustomSelectionActionModeCallback(mActionModeCallback);
        mFolderName.setOnEditorActionListener(this);
        mFolderName.setSelectAllOnFocus(true);
        mFolderName.setInputType(mFolderName.getInputType() |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        
        /// M: added for theme feature, get the different folder text color for different themes.
//        final Resources res = getContext().getResources();
//        final int folderTextColor = Launcher.getThemeColor(res, R.color.folder_text_color);
//        mFolderName.setTextColor(folderTextColor);
    }
    
    protected ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    public void onClick(View v) {
        Object tag = v.getTag();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onClick: v = " + v + ", tag = " + tag);
        }

        if (tag instanceof ShortcutInfo) {
            // refactor this code from Folder
            ShortcutInfo item = (ShortcutInfo) tag;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            item.intent.setSourceBounds(new Rect(pos[0], pos[1],
                    pos[0] + v.getWidth(), pos[1] + v.getHeight()));

            mLauncher.startActivitySafely(v, item.intent, item);
        }
    }

    public boolean onLongClick(View v) {
        // Return if global dragging is not enabled
        if (!mLauncher.isDraggingEnabled()) {
            return true;
        }

        Object tag = v.getTag();
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onLongClick: v = " + v + ", tag = " + tag);
        }

        if (tag instanceof ShortcutInfo) {
            ShortcutInfo item = (ShortcutInfo) tag;
            if (!v.isInTouchMode()) {
                return false;
            }

            //mLauncher.dismissFolderCling(null);

            mLauncher.getWorkspace().onDragStartedWithItem(v);
            mLauncher.getWorkspace().beginDragShared(v, this);
            mIconDrawable = ((TextView) v).getCompoundDrawables()[1];

            mCurrentDragInfo = item;
            mEmptyCell[0] = item.cellX;
            mEmptyCell[1] = item.cellY;
            /// M: modified for unread feature, the icon is playced in a RelativeLayout.
            mCurrentDragView = (View)v.getParent();

            mContent.removeView(mCurrentDragView);
            mInfo.remove(mCurrentDragInfo);
            mDragInProgress = true;
            mItemAddedBackToSelfViaIcon = false;
        }
        return true;
    }

    public boolean isEditingName() {
        return mIsEditingName;
    }

    public void startEditingFolderName() {
        mFolderName.setHint("");
        mIsEditingName = true;
    }

    public void dismissEditingName() {
        mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        doneEditingFolderName(true);
    }

    public void doneEditingFolderName(boolean commit) {
        mFolderName.setHint(sHintText);
        // Convert to a string here to ensure that no other state associated with the text field
        // gets saved.
        String newTitle = mFolderName.getText().toString();
        if(!TextUtils.isEmpty(newTitle)){
            mInfo.setTitle(newTitle);
            mLauncher.getModel().updateItemInDatabase(mInfo);
        } else if (!sDefaultFolderName.contentEquals(mInfo.title)){
            mFolderName.setText(mInfo.title);
        }

        if (commit && !TextUtils.isEmpty(sForderRenameFormat)) {
            sendCustomAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    String.format(sForderRenameFormat, newTitle));
        }
        // In order to clear the focus from the text field, we set the focus on ourself. This
        // ensures that every time the field is clicked, focus is gained, giving reliable behavior.
        requestFocus();

        Selection.setSelection((Spannable) mFolderName.getText(), 0, 0);
        mIsEditingName = false;
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            dismissEditingName();
            return true;
        }
        return false;
    }

    public View getEditTextRegion() {
        return mFolderName;
    }

    public Drawable getDragDrawable() {
        return mIconDrawable;
    }

    /**
     * We need to handle touch events to prevent them from falling through to the workspace below.
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }


    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // When the folder gets focus, we don't want to announce the list of items.
        return true;
    }

    /**
     * @return the FolderInfo object associated with this folder
     */
    public FolderInfo getInfo() {
        return mInfo;
    }

    protected class GridComparator implements Comparator<ShortcutInfo> {
        int mNumCols;
        public GridComparator(int numCols) {
            mNumCols = numCols;
        }

        @Override
        public int compare(ShortcutInfo lhs, ShortcutInfo rhs) {
            int lhIndex = lhs.cellY * mNumCols + lhs.cellX;
            int rhIndex = rhs.cellY * mNumCols + rhs.cellX;
            return (lhIndex - rhIndex);
        }
    }

    protected void placeInReadingOrder(ArrayList<ShortcutInfo> items) {
        int maxX = 0;
        int count = items.size();
        for (int i = 0; i < count; i++) {
            ShortcutInfo item = items.get(i);
            if (item.cellX > maxX) {
                maxX = item.cellX;
            }
        }

        GridComparator gridComparator = new GridComparator(maxX + 1);
        Collections.sort(items, gridComparator);
        final int countX = mContent.getCountX();
        for (int i = 0; i < count; i++) {
            int x = i % countX;
            int y = i / countX;
            ShortcutInfo item = items.get(i);
            item.cellX = x;
            item.cellY = y;
        }
    }

    public void bind(FolderInfo info) {
        mInfo = info;
        ArrayList<ShortcutInfo> children = info.contents;
        ArrayList<ShortcutInfo> overflow = new ArrayList<ShortcutInfo>();
        setupContentForNumItems(children.size());
        placeInReadingOrder(children);
        int count = 0;
        for (int i = 0; i < children.size(); i++) {
            ShortcutInfo child = (ShortcutInfo) children.get(i);
            if (!createAndAddShortcut(child)) {
                overflow.add(child);
            } else {
                count++;
            }
        }

        // We rearrange the items in case there are any empty gaps
        setupContentForNumItems(count);

        // If our folder has too many items we prune them from the list. This is an issue 
        // when upgrading from the old Folders implementation which could contain an unlimited
        // number of items.
        for (ShortcutInfo item: overflow) {
            mInfo.remove(item);
            mLauncher.getModel().deleteItemFromDatabase(item);
        }

        mItemsInvalidated = true;
        updateTextViewFocus();
        mInfo.addListener(this);

        if (!sDefaultFolderName.contentEquals(mInfo.title)) {
            mFolderName.setText(mInfo.title);
        } else {
            mFolderName.setText("");
        }
        updateItemLocationsInDatabase();
    }

    /**
     * Creates a new UserFolder, inflated from R.layout.user_folder.
     *
     * @param context The application's context.
     *
     * @return A new UserFolder.
     */
    public static Folder fromXml(Launcher launcher) {
    	final IResConfigManager resManager = launcher.getResConfigManager();
    	
    	mExpandDuration = resManager.getInteger(IResConfigManager.CONFIG_FOLDER_ANIM_DURATION);
    	//R.integer.config_folderAnimDuration);

        if (sDefaultFolderName == null) {
            sDefaultFolderName = resManager.getString(IResConfigManager.STR_FOLDER_DEFAULT_NAME);
            //R.string.folder_name
        }
        if (sHintText == null) {
            sHintText = resManager.getString(IResConfigManager.STR_FOLDER_HINT_TEXT);
            //R.string.folder_hint_text);
        }
        
        if (sForderRenameFormat == null) {
        	sForderRenameFormat = resManager.getString(IResConfigManager.STR_FOLDER_RENAMED_FORMAT);
            //R.string.folder_hint_text);
        }
        
        if (sForderOpenedFormat == null) {
        	sForderOpenedFormat = resManager.getString(IResConfigManager.STR_FOLDER_OPENED_FORMAT);
            //R.string.folder_hint_text);
        }
        
        if (sForderClosedFormat == null) {
        	sForderClosedFormat = resManager.getString(IResConfigManager.STR_FOLDER_CLOSED_FORMAT);
            //R.string.folder_hint_text);
        }
        
        return (Folder) resManager.inflaterView(IResConfigManager.LAYOUT_USER_FOLDER);//LayoutInflater.from(context).inflate(R.layout.user_folder, null);
    }

    /**
     * This method is intended to make the UserFolder to be visually identical in size and position
     * to its associated FolderIcon. This allows for a seamless transition into the expanded state.
     */
    protected void positionAndSizeAsIcon() {
        if (!(getParent() instanceof DragLayer)) {
            return;
        }
        setScaleX(0.8f);
        setScaleY(0.8f);
        setAlpha(0f);
        mState = STATE_SMALL;
    }
    
    public void animateOpen() {
        positionAndSizeAsIcon();

        /// M: Update unread number of content shortcuts.
        //updateContentUnreadNum();

        if (!(getParent() instanceof DragLayer)) {
            return;
        }
        centerAboutIcon();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);
        final ObjectAnimator oa = mOpenCloseAnimator =
            LauncherAnimUtils.ofPropertyValuesHolder(this, alpha, scaleX, scaleY);

        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
            	if(!TextUtils.isEmpty(sForderOpenedFormat)){
	                sendCustomAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
	                        String.format(sForderOpenedFormat,
	                        mContent.getCountX(), mContent.getCountY()));
            	}
                mState = STATE_ANIMATING;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mState = STATE_OPEN;
                setLayerType(LAYER_TYPE_NONE, null);
//                Cling cling = mLauncher.showFirstRunFoldersCling();
//                if (cling != null) {
//                    cling.bringToFront();
//                }
                setFocusOnFirstChild();
            }
        });
        oa.setDuration(mExpandDuration);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        buildLayer();
        post(new Runnable() {
            public void run() {
                // Check if the animator changed in the meantime
                if (oa != mOpenCloseAnimator)
                    return;
                oa.start();
            }
        });
    }

    private void sendCustomAccessibilityEvent(int type, String text) {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(type);
            onInitializeAccessibilityEvent(event);
            event.getText().add(text);
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    private void setFocusOnFirstChild() {
        View firstChild = mContent.getChildAt(0, 0);
        if (firstChild != null) {
            firstChild.requestFocus();
        }
    }

    public void animateClosed() {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "animateClosed: parent = " + getParent());
        }
        if (!(getParent() instanceof DragLayer)) {
            return;
        }
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 0.9f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 0.9f);
        final ObjectAnimator oa = mOpenCloseAnimator =
                LauncherAnimUtils.ofPropertyValuesHolder(this, alpha, scaleX, scaleY);

        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //onCloseComplete();
                setLayerType(LAYER_TYPE_NONE, null);
                mState = STATE_SMALL;
            }
            @Override
            public void onAnimationStart(Animator animation) {
            	if(!TextUtils.isEmpty(sForderClosedFormat)){
	                sendCustomAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
	                		sForderClosedFormat);
            	}
                mState = STATE_ANIMATING;
            }
        });
        oa.setDuration(mExpandDuration);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        buildLayer();
        post(new Runnable() {
            public void run() {
                // Check if the animator changed in the meantime
                if (oa != mOpenCloseAnimator)
                    return;
                oa.start();
            }
        });
        onCloseComplete();
    }

    public void notifyDataSetChanged() {
        // recreate all the children if the data set changes under us. We may want to do this more
        // intelligently (ie just removing the views that should no longer exist)
        mContent.removeAllViewsInLayout();
        /// M: reset unread events number.
        //mInfo.unreadNum = 0;
        bind(mInfo);
    }

    public boolean acceptDrop(DragObject d) {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "acceptDrop: DragObject = " + d);
        }

        final ItemInfo item = (ItemInfo) d.dragInfo;
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                    itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                    !isFull());
    }

    protected boolean findAndSetEmptyCells(ShortcutInfo item) {
        int[] emptyCell = new int[2];
        if (mContent.findCellForSpan(emptyCell, item.spanX, item.spanY)) {
            item.cellX = emptyCell[0];
            item.cellY = emptyCell[1];
            return true;
        } else {
            return false;
        }
    }

    protected boolean createAndAddShortcut(ShortcutInfo item) {
        /// M: modified for Unread feature, the icon is playced in a RelativeLayout,
        /// so we should set click/longClick listener for the icon, not the RealtiveLayout.
        final MtpShortcutView shortcut = (MtpShortcutView)mLauncher.createShortcut(IResConfigManager.LAYOUT_APPLICATION_SHORTCUT_NORESIZE, 
                                            this, item);
            //(MTKShortcut) mInflater.inflate(R.layout.mtk_application, this, false);        
//        shortcut.setIcon(new FastBitmapDrawable(item.getIcon(mIconCache)));
//        shortcut.setTitle(item.title);
//        shortcut.setTag(item);
//        shortcut.setShortcutUnreadMarginRight(mUnreadMarginRight);
//        shortcut.updateShortcutUnreadNum(item.unreadNum);
//        mFolderIcon.updateFolderUnreadNum(item.intent.getComponent(), item.unreadNum);

        shortcut.mFavorite.setOnClickListener(this);
        shortcut.mFavorite.setOnLongClickListener(this);

        // We need to check here to verify that the given item's location isn't already occupied
        // by another item.
        if (mContent.getChildAt(item.cellX, item.cellY) != null || item.cellX < 0 || item.cellY < 0
                || item.cellX >= mContent.getCountX() || item.cellY >= mContent.getCountY()) {
            // This shouldn't happen, log it. 
            Util.Log.e(TAG, "Folder order not properly persisted during bind");
            if (!findAndSetEmptyCells(item)) {
                return false;
            }
        }

        CellLayout.LayoutParams lp =
            new CellLayout.LayoutParams(item.cellX, item.cellY, item.spanX, item.spanY);
        boolean insert = false;
        //shortcut.setOnKeyListener(new FolderKeyEventListener());
        mContent.addViewToCellLayout(shortcut, insert ? 0 : -1, (int)item.id, lp, true);
        return true;
    }

    public void onDragEnter(DragObject d) {
        mPreviousTargetCell[0] = -1;
        mPreviousTargetCell[1] = -1;
        mOnExitAlarm.cancelAlarm();
    }

    public OnAlarmListener mReorderAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            realTimeReorder(mEmptyCell, mTargetCell);
        }
    };

    public boolean readingOrderGreaterThan(int[] v1, int[] v2) {
        if (v1[1] > v2[1] || (v1[1] == v2[1] && v1[0] > v2[0])) {
            return true;
        } else {
            return false;
        }
    }

    private void realTimeReorder(int[] empty, int[] target) {
        boolean wrap;
        int startX;
        int endX;
        int startY;
        int delay = 0;
        float delayAmount = 30;
        if (readingOrderGreaterThan(target, empty)) {
            wrap = empty[0] >= mContent.getCountX() - 1;
            startY = wrap ? empty[1] + 1 : empty[1];
            for (int y = startY; y <= target[1]; y++) {
                startX = y == empty[1] ? empty[0] + 1 : 0;
                endX = y < target[1] ? mContent.getCountX() - 1 : target[0];
                for (int x = startX; x <= endX; x++) {
                    View v = mContent.getChildAt(x,y);
                    if (mContent.animateChildToPosition(v, empty[0], empty[1],
                            REORDER_ANIMATION_DURATION, delay, true, true)) {
                        empty[0] = x;
                        empty[1] = y;
                        delay += delayAmount;
                        delayAmount *= 0.9;
                    }
                }
            }
        } else {
            wrap = empty[0] == 0;
            startY = wrap ? empty[1] - 1 : empty[1];
            for (int y = startY; y >= target[1]; y--) {
                startX = y == empty[1] ? empty[0] - 1 : mContent.getCountX() - 1;
                endX = y > target[1] ? 0 : target[0];
                for (int x = startX; x >= endX; x--) {
                    View v = mContent.getChildAt(x,y);
                    if (mContent.animateChildToPosition(v, empty[0], empty[1],
                            REORDER_ANIMATION_DURATION, delay, true, true)) {
                        empty[0] = x;
                        empty[1] = y;
                        delay += delayAmount;
                        delayAmount *= 0.9;
                    }
                }
            }
        }
    }

    public void onDragOver(DragObject d) {
        float[] r = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView, null);
        mTargetCell = mContent.findNearestArea((int) r[0], (int) r[1], 1, 1, mTargetCell);

        if (mTargetCell[0] != mPreviousTargetCell[0] || mTargetCell[1] != mPreviousTargetCell[1]) {
            mReorderAlarm.cancelAlarm();
            mReorderAlarm.setOnAlarmListener(mReorderAlarmListener);
            mReorderAlarm.setAlarm(150);
            mPreviousTargetCell[0] = mTargetCell[0];
            mPreviousTargetCell[1] = mTargetCell[1];
        }
    }

    // This is used to compute the visual center of the dragView. The idea is that
    // the visual center represents the user's interpretation of where the item is, and hence
    // is the appropriate point to use when determining drop location.
    protected float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }

        // These represent the visual top and left of drag view if a dragRect was provided.
        // If a dragRect was not provided, then they correspond to the actual view left and
        // top, as the dragRect is in that case taken to be the entire dragView.
        // R.dimen.dragViewOffsetY.
        int left = x - xOffset;
        int top = y - yOffset;

        // In order to find the visual center, we shift by half the dragRect
        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;

        return res;
    }

    public OnAlarmListener mOnExitAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            completeDragExit();
        }
    };

    public void completeDragExit() {
    	/// M: Because of not using animation callback, so we judge this in advance.
    	mRearrangeOnClose = true;
        mLauncher.closeFolder();
        mCurrentDragInfo = null;
        mCurrentDragView = null;
        mSuppressOnAdd = false;
        //mRearrangeOnClose = true;
    }

    public void onDragExit(DragObject d) {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDragExit: DragObject = " + d);
        }

        // We only close the folder if this is a true drag exit, ie. not because a drop
        // has occurred above the folder.
        if (!d.dragComplete) {
            mOnExitAlarm.setOnAlarmListener(mOnExitAlarmListener);
            mOnExitAlarm.setAlarm(ON_EXIT_CLOSE_DELAY);
        }
        mReorderAlarm.cancelAlarm();
    }

    public void onDropCompleted(View target, DragObject d, boolean isFlingToDelete,
            boolean success) {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDropCompleted: View = " + target + ", DragObject = " + d
                    + ", isFlingToDelete = " + isFlingToDelete + ", success = " + success);
        }

        if (success) {
            if (mDeleteFolderOnDropCompleted && !mItemAddedBackToSelfViaIcon) {
                replaceFolderWithFinalItem();
            }
        } else {
            // The drag failed, we need to return the item to the folder
            mFolderIcon.onDrop(d);

            // We're going to trigger a "closeFolder" which may occur before this item has
            // been added back to the folder -- this could cause the folder to be deleted
            if (mOnExitAlarm.alarmPending()) {
                mSuppressFolderDeletion = true;
            }
        }

        /// M: Because of not using animation callback, so we judge this in advance
        mDragInProgress = false;
        mCurrentDragInfo = null;
        
        if (target != this) {
            if (mOnExitAlarm.alarmPending()) {
                mOnExitAlarm.cancelAlarm();
                completeDragExit();
            }
        }
        mDeleteFolderOnDropCompleted = false;
        //mDragInProgress = false;
        mItemAddedBackToSelfViaIcon = false;
        //mCurrentDragInfo = null;
        mCurrentDragView = null;
        mSuppressOnAdd = false;

        // Reordering may have occured, and we need to save the new item locations. We do this once
        // at the end to prevent unnecessary database operations.
        updateItemLocationsInDatabase();
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    public void onFlingToDelete(DragObject d, int x, int y, PointF vec) {
        // Do nothing
    }

    @Override
    public void onFlingToDeleteCompleted() {
        // Do nothing
    }

    protected void updateItemLocationsInDatabase() {
        ArrayList<View> list = getItemsInReadingOrder();
        for (int i = 0; i < list.size(); i++) {
            View v = list.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            mLauncher.getModel().moveItemInDatabase(info, mInfo.id, 0,
                        info.cellX, info.cellY);
        }
    }

    public void notifyDrop() {
        if (mDragInProgress) {
            mItemAddedBackToSelfViaIcon = true;
        }
    }

    public boolean isDropEnabled() {
        return true;
    }

    public DropTarget getDropTargetDelegate(DragObject d) {
        return null;
    }

    protected void setupContentDimensions(int count) {
        ArrayList<View> list = getItemsInReadingOrder();

        int countX = mContent.getCountX();
        int countY = mContent.getCountY();
        boolean done = false;

        while (!done) {
            int oldCountX = countX;
            int oldCountY = countY;
            if (countX * countY < count) {
                // Current grid is too small, expand it
                if ((countX <= countY || countY == mMaxCountY) && countX < mMaxCountX) {
                    countX++;
                } else if (countY < mMaxCountY) {
                    countY++;
                }
                if (countY == 0) countY++;
            } else if ((countY - 1) * countX >= count && countY >= countX) {
                countY = Math.max(0, countY - 1);
            } else if ((countX - 1) * countY >= count) {
                countX = Math.max(0, countX - 1);
            }
            done = countX == oldCountX && countY == oldCountY;
        }
        if(SUPPORT_EMPTY_FOLDER)
            mContent.setGridSize(Math.max(1, countX), Math.max(1, countY));
        else
            mContent.setGridSize(countX, countY);
        arrangeChildren(list);
    }

    public boolean isFull() {
        return getItemCount() >= mMaxNumItems;
    }

    protected void centerAboutIcon() {
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();

        int width = getPaddingLeft() + getPaddingRight() + mContent.getDesiredWidth();
        int height = getPaddingTop() + getPaddingBottom() + mContent.getDesiredHeight()
                + mFolderNameHeight;
        DragLayer parent = mLauncher.getDragLayer();

        float scale = parent.getDescendantRectRelativeToSelf(mFolderIcon, mTempRect);

        int centerX = (int) (mTempRect.left + mTempRect.width() * scale / 2);
        int centerY = (int) (mTempRect.top + mTempRect.height() * scale / 2);
        int centeredLeft = centerX - width / 2;
        int centeredTop = centerY - height / 2;

        int currentPage = mLauncher.getWorkspace().getCurrentPage();
        // In case the workspace is scrolling, we need to use the final scroll to compute
        // the folders bounds.
        mLauncher.getWorkspace().setFinalScrollForPageChange(currentPage);
        // We first fetch the currently visible CellLayoutChildren
        CellLayout currentLayout = (CellLayout) mLauncher.getWorkspace().getChildAt(currentPage);
        ShortcutAndWidgetContainer boundingLayout = currentLayout.getShortcutsAndWidgets();
        Rect bounds = new Rect();
        parent.getDescendantRectRelativeToSelf(boundingLayout, bounds);
        /// M: Restore current layout bounds.
        if (mLauncher.getWorkspace().isSmall()) {
            bounds = mLauncher.getCurrentBounds();
        }
        // We reset the workspaces scroll
        mLauncher.getWorkspace().resetFinalScrollForPageChange(currentPage);

        // We need to bound the folder to the currently visible CellLayoutChildren
        int left = Math.min(Math.max(bounds.left, centeredLeft),
                bounds.left + bounds.width() - width);
        int top = Math.min(Math.max(bounds.top, centeredTop),
                bounds.top + bounds.height() - height);
        // If the folder doesn't fit within the bounds, center it about the desired bounds
        if (width >= bounds.width()) {
            left = bounds.left + (bounds.width() - width) / 2;
        }
        if (height >= bounds.height()) {
            top = bounds.top + (bounds.height() - height) / 2;
        }

        int folderPivotX = width / 2 + (centeredLeft - left);
        int folderPivotY = height / 2 + (centeredTop - top);
        setPivotX(folderPivotX);
        setPivotY(folderPivotY);
        mFolderIconPivotX = (int) (mFolderIcon.getMeasuredWidth() *
                (1.0f * folderPivotX / width));
        mFolderIconPivotY = (int) (mFolderIcon.getMeasuredHeight() *
                (1.0f * folderPivotY / height));

        lp.width = width;
        lp.height = height;
        lp.x = left;
        lp.y = top;
    }

    public float getPivotXForIconAnimation() {
        return mFolderIconPivotX;
    }
    public float getPivotYForIconAnimation() {
        return mFolderIconPivotY;
    }

    protected void setupContentForNumItems(int count) {
        setupContentDimensions(count);

        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new DragLayer.LayoutParams(0, 0);
            lp.customPosition = true;
            setLayoutParams(lp);
        }
        centerAboutIcon();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getPaddingLeft() + getPaddingRight() + mContent.getDesiredWidth();
        int height = getPaddingTop() + getPaddingBottom() + mContent.getDesiredHeight()
                + mFolderNameHeight;

        int contentWidthSpec = MeasureSpec.makeMeasureSpec(mContent.getDesiredWidth(),
                MeasureSpec.EXACTLY);
        int contentHeightSpec = MeasureSpec.makeMeasureSpec(mContent.getDesiredHeight(),
                MeasureSpec.EXACTLY);
        mContent.measure(contentWidthSpec, contentHeightSpec);

        mFolderName.measure(contentWidthSpec,
                MeasureSpec.makeMeasureSpec(mFolderNameHeight, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    protected void arrangeChildren(ArrayList<View> list) {
        int[] vacant = new int[2];
        if (list == null) {
            list = getItemsInReadingOrder();
        }
        mContent.removeAllViews();

        for (int i = 0; i < list.size(); i++) {
            View v = list.get(i);
            mContent.getVacantCell(vacant, 1, 1);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
            lp.cellX = vacant[0];
            lp.cellY = vacant[1];
            ItemInfo info = (ItemInfo) v.getTag();
            if (info.cellX != vacant[0] || info.cellY != vacant[1]) {
                info.cellX = vacant[0];
                info.cellY = vacant[1];
                mLauncher.getModel().addOrMoveItemInDatabase(info, mInfo.id, 0,
                        info.cellX, info.cellY);
            }
            boolean insert = false;
            mContent.addViewToCellLayout(v, insert ? 0 : -1, (int)info.id, lp, true);
        }
        mItemsInvalidated = true;
    }

    public int getItemCount() {
        return mContent.getShortcutsAndWidgets().getChildCount();
    }

    public View getItemAt(int index) {
        return mContent.getShortcutsAndWidgets().getChildAt(index);
    }

    protected void onCloseComplete() {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onCloseComplete: parent = " + getParent());
        }

        DragLayer parent = (DragLayer) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        mDragController.removeDropTarget((DropTarget) this);
        clearFocus();
        mFolderIcon.requestFocus();

        if (mRearrangeOnClose) {
            setupContentForNumItems(getItemCount());
            mRearrangeOnClose = false;
        }
        if (!SUPPORT_EMPTY_FOLDER && getItemCount() <= 1) {
            if (!mDragInProgress && !mSuppressFolderDeletion) {
                replaceFolderWithFinalItem();
            } else if (mDragInProgress) {
                mDeleteFolderOnDropCompleted = true;
            }
        }
        mSuppressFolderDeletion = false;
    }

    protected void replaceFolderWithFinalItem() {
        if(SUPPORT_EMPTY_FOLDER)
            return;
        // Add the last remaining child to the workspace in place of the folder
        Runnable onCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                CellLayout cellLayout = mLauncher.getCellLayout(mInfo.container, mInfo.screen);

               View child = null;
                // Move the item from the folder to the workspace, in the position of the folder
                if (getItemCount() == 1) {
                    ShortcutInfo finalItem = mInfo.contents.get(0);
                    child = mLauncher.createShortcut(cellLayout, finalItem);
                    mLauncher.getModel().addOrMoveItemInDatabase(finalItem, mInfo.container,
                            mInfo.screen, mInfo.cellX, mInfo.cellY);
                }
                if (getItemCount() <= 1) {
                    // Remove the folder
                	mLauncher.getModel().deleteItemFromDatabase(mInfo);
                    cellLayout.removeView(mFolderIcon);
                    if (mFolderIcon instanceof DropTarget) {
                        mDragController.removeDropTarget((DropTarget) mFolderIcon);
                    }
                    mLauncher.removeFolder(mInfo);
                }
                // We add the child after removing the folder to prevent both from existing at
                // the same time in the CellLayout.
                if (child != null) {
                    mLauncher.getWorkspace().addInScreen(child, mInfo.container, mInfo.screen,
                            mInfo.cellX, mInfo.cellY, mInfo.spanX, mInfo.spanY);
                }

                /// M: Clear folder information after folder is deleted.
                mContent.removeAllViews();
                mInfo.contents.clear();
                mItemsInReadingOrder.clear();
            }
        };
        View finalChild = getItemAt(0);
        if (finalChild != null) {
            mFolderIcon.performDestroyAnimation(finalChild, onCompleteRunnable);
        }
        mDestroyed = true;
    }

    public boolean isDestroyed() {
        return mDestroyed;
    }

    // This method keeps track of the last item in the folder for the purposes
    // of keyboard focus
    protected void updateTextViewFocus() {
        View lastChild = getItemAt(getItemCount() - 1);
        //getItemAt(getItemCount() - 1);
        if (lastChild != null) {
            mFolderName.setNextFocusDownId(lastChild.getId());
            mFolderName.setNextFocusRightId(lastChild.getId());
            mFolderName.setNextFocusLeftId(lastChild.getId());
            mFolderName.setNextFocusUpId(lastChild.getId());
        }
    }

    public void onDrop(DragObject d) {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onDrop: DragObject = " + d);
        }

        ShortcutInfo item;
        if (d.dragInfo instanceof ApplicationInfo) {
            // Came from all apps -- make a copy
            item = ((ApplicationInfo) d.dragInfo).makeShortcut();
            item.spanX = 1;
            item.spanY = 1;
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        // Dragged from self onto self, currently this is the only path possible, however
        // we keep this as a distinct code path.
        if (item == mCurrentDragInfo) {
            ShortcutInfo si = (ShortcutInfo) mCurrentDragView.getTag();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mCurrentDragView.getLayoutParams();
            si.cellX = lp.cellX = mEmptyCell[0];
            si.cellX = lp.cellY = mEmptyCell[1];
            mContent.addViewToCellLayout(mCurrentDragView, -1, (int)item.id, lp, true);
            if (d.dragView.hasDrawn()) {
                mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, mCurrentDragView);
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                mCurrentDragView.setVisibility(VISIBLE);
            }
            mItemsInvalidated = true;
            setupContentDimensions(getItemCount());
            mSuppressOnAdd = true;
        }
        mInfo.add(item);
    }

    public void onAdd(ShortcutInfo item) {
    	if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onAdd item = " + item);
        }

        mItemsInvalidated = true;
        // If the item was dropped onto this open folder, we have done the work associated
        // with adding the item to the folder, as indicated by mSuppressOnAdd being set
        if (mSuppressOnAdd) return;
        if (!findAndSetEmptyCells(item)) {
            // The current layout is full, can we expand it?
            setupContentForNumItems(getItemCount() + 1);
            findAndSetEmptyCells(item);
        }
        createAndAddShortcut(item);
        mLauncher.getModel().addOrMoveItemInDatabase(
                item, mInfo.id, 0, item.cellX, item.cellY);
    }

    public void onRemove(ShortcutInfo item) {
        if (Util.ENABLE_DEBUG) {
        	Util.Log.d(TAG, "onRemove item = " + item);
        }

        mItemsInvalidated = true;
        // If this item is being dragged from this open folder, we have already handled
        // the work associated with removing the item, so we don't have to do anything here.
        if (item == mCurrentDragInfo) return;
        View v = getViewForInfo(item);
        mContent.removeView(v);
        if (mState == STATE_ANIMATING) {
            mRearrangeOnClose = true;
        } else {
            setupContentForNumItems(getItemCount());
        }
        
        if(!SUPPORT_EMPTY_FOLDER){
            /// M: Uninstall one app, and all items in folder are the same and all are shortcuts of the app uninstalled, 
            /// don't call 'replaceFolderWithFinalItem', remove them directly.
            ShortcutInfo finalItem = null;
            if (getItemCount() == 1) {
                finalItem = (ShortcutInfo)mInfo.contents.get(0);
            }
            
            boolean allItemsSame = false;
            if (finalItem != null) {
                final ComponentName finalComponent = finalItem.intent.getComponent();
                final ComponentName itemComponent = item.intent.getComponent();
                if (finalComponent != null && itemComponent != null) {
                    allItemsSame = finalComponent.getPackageName().equals(itemComponent.getPackageName())
                            && finalComponent.getClassName().equals(itemComponent.getClassName());
                }
            }
    
            if (getItemCount() <= 1 && !allItemsSame) {  
                replaceFolderWithFinalItem();
            }
        }
    }

    protected View getViewForInfo(ShortcutInfo item) {
        for (int j = 0; j < mContent.getCountY(); j++) {
            for (int i = 0; i < mContent.getCountX(); i++) {
                View v = mContent.getChildAt(i, j);
                if (v.getTag() == item) {
                    return v;
                }
            }
        }
        return null;
    }

    public void onItemsChanged() {
        updateTextViewFocus();
    }

    public void onTitleChanged(CharSequence title) {
    }

    public ArrayList<View> getItemsInReadingOrder() {
        return getItemsInReadingOrder(true);
    }

    public ArrayList<View> getItemsInReadingOrder(boolean includeCurrentDragItem) {
        if (mItemsInvalidated) {
            mItemsInReadingOrder.clear();
            for (int j = 0; j < mContent.getCountY(); j++) {
                for (int i = 0; i < mContent.getCountX(); i++) {
                    View v = mContent.getChildAt(i, j);
                    if (v != null) {
                        ShortcutInfo info = (ShortcutInfo) v.getTag();
                        if (info != mCurrentDragInfo || includeCurrentDragItem) {
                            mItemsInReadingOrder.add(v);
                        }
                    }
                }
            }
            mItemsInvalidated = false;
        }
        return mItemsInReadingOrder;
    }

    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    public void onFocusChange(View v, boolean hasFocus) {
        if (v == mFolderName && hasFocus) {
            startEditingFolderName();
        }
    }
}
