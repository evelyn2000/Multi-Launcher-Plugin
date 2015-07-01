package com.jzs.dr.mtplauncher;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jzs.common.launcher.IIconCache;
import com.jzs.common.launcher.LauncherHelper;
import com.jzs.common.launcher.model.ApplicationInfo;
import com.jzs.common.launcher.model.IPackageUpdatedCallbacks;
import com.jzs.common.launcher.model.IconCacheEntry;
import com.jzs.common.manager.IAppsManager;
import com.jzs.common.manager.IIconUtilities;
import com.jzs.dr.mtplauncher.R;
import com.jzs.dr.mtplauncher.defaults.widget.QsCropView;
import com.jzs.dr.mtplauncher.defaults.widget.QsCropView.OnTouchMoveListener;
import com.jzs.dr.mtplauncher.defaults.widget.QsCropView.OnZoomChangedListener;
import com.jzs.dr.mtplauncher.sjar.utils.Util;
import com.jzs.dr.mtplauncher.sjar.widget.FastBitmapDrawable;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.BitmapDrawable;
import com.qs.utils.ConfigOption;


public class CustomCropImageActivity extends Activity implements View.OnClickListener, IPackageUpdatedCallbacks{

	/*edit mode views*/
	TextView mSelectBtn;
	TextView mTakePhotoBtn;
	View mSelectPicLable;
	View mIndicatorArrow;
	ImageView mIconRegion;
	QsCropView mCropView;
	TextView mSaveImgBtn;
	TextView mZoomOutImgBtn;
	TextView mZoomInImgBtn;	
	TextView mRenameTitleBtn;
	EditText mTitleView;
	ViewGroup mIconBgContainer;
	ScrollView mImgScrollView;
	
	/*list mode views*/
	GridView mGridList;
	View mGridListEmpty;
	View mPageEdit;
	View mPageList;
	TextView mListTitle;
	TextView mListRestore;
	View mRestoreBtn;
	ImageView mSelectAllBtn;

	
	Context mContext;
	BroadcastReceiver mPackageReceiver;
	//Bitmap mMaskBitmap;
	IAppsManager mIAppsManager;
	
	private static final int RESULT_LOAD_IMAGE = 1001;
	private static final int RESULT_TAKE_PICTURE = 1002;
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int MEDIA_TYPE_VIDEO = 2;
	
	private static final int INDEX_PAGE_LIST = 0;
	private static final int INDEX_PAGE_EDIT = 1;
	private static final int INDEX_PAGE_MAX = 2;	
	private static final int MODE_CUSTOM = 1;
	private static final int MODE_RESTORE = 2;
	
	private final int BG_ICON_ID_START = 0x00010000;	
	private final int MSG_DELAY_LAYOUT = 0x00001000;	
	private final int MSG_SHOW_PROGRESS_DLG = 0x00001001;	
	private final int MSG_HIDE_PROGRESS_DLG = 0x00001002;		
	private final int MSG_SHOW_RESTORE_DLG = 0x00001003;	
	private final int MSG_UPDATE_PROGRESS_DLG = 0x00001004;	
	private final int MSG_NOTIFY_GOTO_LIST_MODE = 0x00001005;	
	private final int MSG_NOTIFY_SAVE_COMPLETED = 0x00001006;		
	private static int PREVIEW_IMG_MARGIN;
	protected static final int TIME_SAVE_DELAY_MS = 500;
	protected static final int TIME_RESTORE_DELAY_MS = 50;
	protected static final int TIME_RESTORE_DELAY_MAX_MS = 4000;
	
	public static final String CUSTOM_APP_ICON_CLASS_NAME = "com.jzs.dr.mtplauncher.CustomCropImageActivity";
	public static final String MTK_APP_ICON_TODO_CLASS_NAME = "com.mediatek.todos.TodosActivity";
	protected static final boolean DEBUG = false;
	protected static final String TAG = "QsLog"; 
	private String mPicturePath;
	private String mAppIconActivityName;
	private int mCurrentIndex = -1;
	private int mAppIconsize;
	private int mSelectedItem = 0;
	private float mDensity;
	private int mSelectedIconBg = 0;
	private int mListMode = 0;
	private int mSelectNum = 0;
	private int mSaveResult = -2;
	private boolean isImgModify;
	private boolean isTitleModify;
	private boolean mIsAllSelect;
	private boolean isImgMove;
	private boolean canSelectedDefaultBg;
	
	private ComponentInfo mSelectedInfo;
	private List<CustomIconInfo> mInfoList = new ArrayList<CustomIconInfo>();
	private List<CustomIconInfo> mRestoreList = new ArrayList<CustomIconInfo>();
	private ArrayList<ImageView> mCustomIconImageViewList = new ArrayList<ImageView>();	
	private ArrayList<View> mCustomIconContainerList = new ArrayList<View>();	
	private ArrayList<Drawable> mIconBgList = new ArrayList<Drawable>();	
	private ProgressDialog mWaitingDlg;
	private ProgressDialog mSavingDlg;	
	private AlertDialog mRestoreDlg;
	private AlertDialog mRenameDlg;
	private EditText mRenameInput;
	
	private LauncherHelper mLauncher;
	private IIconCache mIconCache;
	private IIconUtilities mIconUtils;
	private OnItemClickListener mItemClick;
    private Object synObject = new Object();
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
				case MSG_DELAY_LAYOUT:
//					if(mIconBgContainer.getWidth() < mPageEdit.getWidth()){
//						View firstView = mIconBgContainer.getChildAt(0);//mCustomIconImageViewList.get(0);
//						LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)firstView.getLayoutParams();
//						params.leftMargin = (int)(1.0f * (mPageEdit.getWidth() - mIconBgContainer.getWidth() + (mAppIconsize >> 1))/2);
//						firstView.setLayoutParams(params);
//						mIconBgContainer.requestLayout();
//						if(mIconBgContainer.getChildCount() > 0){
//							View lastView = mIconBgContainer.getChildAt(mIconBgContainer.getChildCount() - 1);//mCustomIconImageViewList.get(0);
//							LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams)lastView.getLayoutParams();
//							params2.rightMargin = 0;
//							lastView.setLayoutParams(params2);
//							mIconBgContainer.requestLayout();
//						}
//					}
					break;
				case MSG_SHOW_PROGRESS_DLG:
					mWaitingDlg.show();
					break;
				case MSG_HIDE_PROGRESS_DLG:
					mWaitingDlg.hide();
					mAdapter.notifyDataSetChanged();
					break;
				case MSG_SHOW_RESTORE_DLG:
					//mSelectNum = getSelectedRestoreCount();
					if(mSelectNum > 0)
						showRestoreDialog();
					break;
				case MSG_UPDATE_PROGRESS_DLG:
					mWaitingDlg.setProgress(msg.arg1);
					break;
				case MSG_NOTIFY_SAVE_COMPLETED:
					mAdapter.notifyDataSetChanged();
					updateRestoreList();//and goto list mode
				case MSG_NOTIFY_GOTO_LIST_MODE:
					synchronized(synObject){
						mCropView.recycleResizeBitmap();
					}
					if(mSaveResult != -2){
						String text = getString(R.string.crop_view_save_image) + 
						(mSaveResult > 0 ? " OK!" : " FAIL!");
						Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
						if(canSelectedDefaultBg){
							mAdapter.notifyDataSetChanged();
						}
					}
					mSavingDlg.hide();
					setPageIndex(INDEX_PAGE_LIST);
					mSaveResult = -2;
					break;
				default:
					break;
			}
			super.handleMessage(msg);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Window win = getWindow();
	    WindowManager.LayoutParams lp = win.getAttributes();
	    lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
	    win.setAttributes(lp);    
		setContentView(R.layout.custom_cropimage_activity);
		
		LauncherApplicationMain app = ((LauncherApplicationMain)getApplication());
		mLauncher = app.getLauncherHelper();
		mIconCache = mLauncher.getIconCache();
		mIconUtils = mLauncher.getIconUtilities();
		mDensity = getResources().getDisplayMetrics().density;
		mIAppsManager = (IAppsManager)getSystemService(IAppsManager.MANAGER_SERVICE);
		initView();
		initData();
		createDialog();
		setOnClickListener();
		startLoad();
		registBroadCastReceiver();
	}
	
	protected void initView(){
		mContext = this;
		mPageList = findViewById(R.id.page_list);
		mPageEdit = findViewById(R.id.page_edit);
		mSelectBtn = (TextView) mPageEdit.findViewById(R.id.btn_select_img);
		mTakePhotoBtn = (TextView) mPageEdit.findViewById(R.id.btn_take_photo);
		mCropView = (QsCropView) mPageEdit.findViewById(R.id.cropView);
		mSaveImgBtn = (TextView) mPageEdit.findViewById(R.id.btn_save_img);
		mZoomInImgBtn = (TextView) mPageEdit.findViewById(R.id.btn_zoom_in_img);
		mZoomOutImgBtn = (TextView) mPageEdit.findViewById(R.id.btn_zoom_out_img);
		mImgScrollView = (ScrollView)mPageEdit.findViewById(R.id.imgScrollView);
		//mRenameTitleBtn = (TextView) mPageEdit.findViewById(R.id.btn_rename_title);
		mGridList = (GridView) mPageList.findViewById(R.id.grid_list);
        mGridList.setAdapter(mAdapter);
		mListTitle = (TextView) mPageList.findViewById(R.id.list_title);
		mListRestore = (TextView) mPageList.findViewById(R.id.list_restore);
        mIconBgContainer = (ViewGroup)mPageEdit.findViewById(R.id.previewContainer);
        mRestoreBtn = mPageList.findViewById(R.id.restore_btn);
        mSelectAllBtn = (ImageView)mPageList.findViewById(R.id.select_all_btn);
        		
        mTitleView = (EditText)mPageEdit.findViewById(R.id.titleView);
		mHandler.sendEmptyMessageDelayed(MSG_DELAY_LAYOUT, 500);
//		Drawable dr = getResources().getDrawable(R.drawable.ic_app_icon_region_mask);
//		if(dr != null)
//			mMaskBitmap = Util.drawable2Bitmap(dr);
//		else
//			mMaskBitmap = null;
		mCropView.setIconUtils(mIconUtils);
		//mCropView.setMaskBitmap(mMaskBitmap);
		mSelectPicLable = findViewById(R.id.select_picture_label);
		mIndicatorArrow = findViewById(R.id.indicator_arrow);
		mGridListEmpty = findViewById(R.id.grid_list_empty_label);
		mIconRegion = (ImageView)findViewById(R.id.icon_region);
	}
	
	protected void initData(){
        mAppIconActivityName = "com.google.android.googlequicksearchbox.SearchActivity";
        //getPackageName() + ".defaults.LauncherDefault";//for test
        mAppIconsize = mIconUtils.getIconSize();//getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        mCropView.setCropImageSize(mAppIconsize, mAppIconsize);
        //TypedArray iconDrawables = getResources().obtainTypedArray(R.array.config_appIconFrameBackground);
        mIconBgList.clear();
        mCustomIconImageViewList.clear();
        mCustomIconContainerList.clear();
        
        mIconUtils.getIconFrameBackgroundList(mIconBgList);
        final LayoutInflater inflater = LayoutInflater.from(this);//.inflate(resId, group, false);
        
        for(int i=0; i<mIconBgList.size(); i++){
            Drawable bgDrawable = mIconBgList.get(i);
            //mIconBgList.add(bgDrawable);
            
            View container = inflater.inflate(R.layout.custom_icon_preview_item, mIconBgContainer, false);
            ImageView imgView = (ImageView)container.findViewWithTag("image");
            imgView.setBackground(bgDrawable);
            imgView.setId(BG_ICON_ID_START + i);
            mCustomIconImageViewList.add(imgView);
            mCustomIconContainerList.add(container);
            mIconBgContainer.addView(container);
        }
        mCropView.setPreviewList(mCustomIconImageViewList);
        mCropView.setOnTouchMoveListener(new OnTouchMoveListener() {
			
			@Override
			public void isOnTouchMove(boolean isMoving) {
				if(isMoving){
					if(mImgScrollView != null)
						mImgScrollView.requestDisallowInterceptTouchEvent(true);
				}
			}
		});
        mCropView.setOnZoomChangedListener(new OnZoomChangedListener() {
			@Override
			public void onZoomChanged(int zoomMode) {
				if(zoomMode == OnZoomChangedListener.MODE_ZOOM_NONE){
					mZoomInImgBtn.setVisibility(View.INVISIBLE);
					mZoomOutImgBtn.setVisibility(View.INVISIBLE);
				}else{
					mZoomInImgBtn.setVisibility(View.VISIBLE);
					mZoomOutImgBtn.setVisibility(View.VISIBLE);
					if((zoomMode & OnZoomChangedListener.MODE_ZOOM_IN)
							== OnZoomChangedListener.MODE_ZOOM_IN){
						updateZoomBtnEnabled(mZoomInImgBtn, true);
						updateZoomBtnEnabled(mZoomOutImgBtn, false);
					}else if((zoomMode & OnZoomChangedListener.MODE_ZOOM_OUT)
							== OnZoomChangedListener.MODE_ZOOM_OUT){
						updateZoomBtnEnabled(mZoomInImgBtn, false);
						updateZoomBtnEnabled(mZoomOutImgBtn, true);
					}
				}
			}
		});

		setListMode(MODE_CUSTOM);
	}
	
	private void updateZoomBtnEnabled(TextView view, boolean enable){
		view.setEnabled(enable);
		view.setTextColor(enable ? 0xFF000000 : 0xFF707070);
	}

	private void createDialog(){
		mWaitingDlg = new ProgressDialog(mContext);
		mWaitingDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mWaitingDlg.setMessage(getString(R.string.crop_view_restore_wait));
		mWaitingDlg.setCanceledOnTouchOutside(false);
		mWaitingDlg.setCancelable(false);
		mRestoreDlg = new AlertDialog.Builder(mContext)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mWaitingDlg.setMax(mSelectNum);
				startRestore();
				dialog.dismiss();
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setTitle(R.string.crop_view_icon_restore)
		.create();
		mRestoreDlg.setCanceledOnTouchOutside(false);
		
		ViewGroup dialogRoot = (ViewGroup)getLayoutInflater()
				.inflate( R.layout.rename_folder, null);
		mRenameDlg = new AlertDialog.Builder(mContext)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveRenamedTitle();				
				dialog.dismiss();
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.setTitle(R.string.crop_view_rename_title)
		.create();
		
		mRenameDlg.setView(dialogRoot);
		dialogRoot.findViewById(R.id.label).setVisibility(View.GONE);
		mRenameInput = (EditText)dialogRoot.findViewById(R.id.folder_name);
		
		mSavingDlg = new ProgressDialog(mContext);
		mSavingDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mSavingDlg.setMessage(getString(R.string.crop_view_saving_wait));
		mSavingDlg.setCanceledOnTouchOutside(false);
		mSavingDlg.setTitle(R.string.crop_view_app_icon);
		mSavingDlg.setCancelable(false);
	}
	
	private void showRestoreDialog(){
		String message = String.format(getString(R.string.crop_view_restore_dlg_message), mSelectNum);
		mRestoreDlg.setMessage(message);
		mRestoreDlg.show();
	}
	
	private void showRenameDialog(){
		mRenameInput.setText(mTitleView.getText());
		mRenameInput.selectAll();
		mRenameDlg.show();
	}
	
	@Override
	protected void onResume() {
		//onClick(mCustomIconImageViewList.get(mSelectedIconBg));
		super.onResume();
		
		mLauncher.getModel().setPackageUpdateCallback(this);
	}

	@Override
    protected void onPause() {
        // TODO Auto-generated method stub
	    mLauncher.getModel().setPackageUpdateCallback(null);
        super.onPause();        
    }

	protected void setOnClickListener(){
		mSelectBtn.setOnClickListener(this);
		mTakePhotoBtn.setOnClickListener(this);
		mSaveImgBtn.setOnClickListener(this);
		mZoomInImgBtn.setOnClickListener(this);
		mZoomOutImgBtn.setOnClickListener(this);
		mItemClick = new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if(mListMode == MODE_CUSTOM){
					mAppIconActivityName = mInfoList.get(arg2).getActivityName();
					//mCropView.setDrawable(mInfoList.get(arg2).icon);
					mIconRegion.setImageBitmap(mInfoList.get(arg2).icon);
					mCropView.setBitmap(null);
					mCropView.setTitle(mInfoList.get(arg2).title);
					mTitleView.setText(mInfoList.get(arg2).title);
					if(DEBUG){
						android.util.Log.i(TAG,"activity:" + mAppIconActivityName);
					}
					setPageIndex(INDEX_PAGE_EDIT);
					mSelectedItem = arg2;
					mSelectedIconBg = 0;
					isImgModify = false;
					isTitleModify = false;
					mIconRegion.setVisibility(View.VISIBLE);
					mSelectedInfo = mInfoList.get(mSelectedItem).getActivityInfo();
				
					canSelectedDefaultBg = checkIsDownLoadAppAndModify(mSelectedInfo);
					if(canSelectedDefaultBg){
						updatePreviewIconEnable(true);
						resetBgSelected(true);
					}else{
						updatePreviewIconEnable(false);
					}
					updateSaveImgBtn();
				}else if(mListMode == MODE_RESTORE){
					ViewHolder holder = (ViewHolder)arg1.getTag();
					if(holder != null){
						boolean checked = holder.select.isChecked();
						holder.select.setChecked(!checked);
						mSelectNum = holder.select.isChecked() ? mSelectNum + 1 : mSelectNum - 1;
						mRestoreList.get(arg2).setRestore(holder.select.isChecked());
					}
				}
				
			}
		};
		mGridList.setOnItemClickListener(mItemClick);

		for(int i=0; i < mCustomIconImageViewList.size(); i++){
			mCustomIconImageViewList.get(i).setOnClickListener(this);
		}
		mListTitle.setOnClickListener(this);
		mListRestore.setOnClickListener(this);
		mRestoreBtn.setOnClickListener(this);
		//mTitleView.setOnClickListener(this);
		//mRenameTitleBtn.setOnClickListener(this);
		mSelectAllBtn.setOnClickListener(this);
		
		TextWatcher mTextWatcher = new TextWatcher() {

			private int textWidth = 0;
			private int inputWidth = 0;
			
	        @Override
	        public void beforeTextChanged(CharSequence s, int arg1, int arg2,
	                int arg3) {
	        	if(inputWidth == 0){
	        		inputWidth = mTitleView.getWidth()
	        				- mTitleView.getPaddingLeft()
	        				- mTitleView.getPaddingRight();
	        	}
	        }
	       
	        @Override
	        public void onTextChanged(CharSequence s, int arg1, int arg2,
	                int arg3) {
	        	textWidth = (int)mTitleView.getPaint().measureText(s + "");
	        	if(textWidth <= inputWidth){
	        		isTitleModify = true;
	        	}
	        }
	       
	        @Override
	        public void afterTextChanged(Editable s) {
	        	updateSaveImgBtn();
	        	if((textWidth > inputWidth) &&(s.length() > 0)){
	        		s.delete(s.length()-1, s.length());
	        	}
	        }
	    };
		mTitleView.addTextChangedListener(mTextWatcher);
		
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
	    super.onActivityResult(requestCode, resultCode, data);  
	      
	    if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {  
	        Uri selectedImage = data.getData();  
	        setImageFromUriData(selectedImage);
	        isImgModify = true;
	    } else if(requestCode == RESULT_TAKE_PICTURE ){
	    	if (resultCode == RESULT_OK) {
	    		if(mPicturePath != null)
	    			setImageFromFileData(mPicturePath);
	    		isImgModify = true;
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the image capture
	        	//isImgModify = false;
	        } else {
	            // Image capture failed, advise user
	        	//isImgModify = false;
	        }
	    }
	    updateSaveImgBtn();
	    if(resultCode != RESULT_CANCELED){
	    	resetBgSelected(false);
	    	updatePreviewIconEnable(isImgModify);
	    	updateIndicatorVisibility(false);
	    }
	}

	private void setImageFromUriData(Uri data){
        String[] filePathColumn = {MediaStore.Images.Media.DATA };  
  
        Cursor cursor = getContentResolver().query(data,  
                filePathColumn, null, null, null);  
        cursor.moveToFirst();  
  
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);  
        String picturePath = cursor.getString(columnIndex);  
        cursor.close();  
        setImageFromFileData(picturePath);
	}
	
	private void setImageFromFileData(String filePath){
		File file = new File(filePath);
		if(!file.exists()){
			return;
		}
		Bitmap bitmap = BitmapFactory.decodeFile(filePath);
		/*Bitmap resizeBitmap = null;
		int scaleSize = (int)(mAppIconsize);
		if(DEBUG){
			android.util.Log.w(TAG,"1.w:" + bitmap.getWidth() + " h:" + bitmap.getHeight());
		}
		if(bitmap.getWidth() < scaleSize || bitmap.getHeight() < scaleSize){
			float scale = scaleSize / Math.min(bitmap.getHeight(), bitmap.getWidth());
			if(DEBUG){
				android.util.Log.w(TAG,"scale:" + scale);
			}
	        Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);
			resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0,
					bitmap.getWidth(),bitmap.getHeight(), matrix, true);
			bitmap.recycle();
		}else{
			resizeBitmap = bitmap;
		}
		//mCropView.setCropImageSize(resizeBitmap.getWidth(), resizeBitmap.getHeight());
		if(DEBUG){
			android.util.Log.w(TAG,"2.w:" + resizeBitmap.getWidth() + " h:" + resizeBitmap.getHeight());
		}
		//mCropView.setDrawable(new BitmapDrawable(resizeBitmap));  */
		mCropView.setBitmap(bitmap); //mCropView.setBitmap(resizeBitmap);
	}
	
	private Uri getOutputMediaFile(int type){

        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);//new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QsFactoryTest");
        if (! mediaStorageDir.exists() && ! mediaStorageDir.mkdirs()){
        	if(DEBUG){
        		Log.d(TAG, "failed to create directory:"+mediaStorageDir.getPath());
        	}
            mediaStorageDir = super.getDir("QsMtpLauncher", MODE_WORLD_WRITEABLE|MODE_WORLD_READABLE);
            if (! mediaStorageDir.exists() && ! mediaStorageDir.mkdirs()){
            	if(DEBUG){
            		Log.d(TAG, "failed to create directory:"+mediaStorageDir.getPath());
            	}
            	return null;
            }
        }
        
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "temple_crop_img.jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
            "temple_crop_video.mp4");
        } else {
            return null;
        }
        
        if(mediaFile.exists())
        	mediaFile.delete();
        mPicturePath = mediaFile.getPath();
        Uri result = Uri.fromFile(mediaFile);
        return result;
    }
	
	private void deleteTempleFile(){
		if(mPicturePath != null){
			File file = new File(mPicturePath);
			if(file.exists()){
				file.delete();
			}
		}
	}
	
	
	@Override
	protected void onDestroy() {
		deleteTempleFile();
		if(mLoadAppTask != null)
			mLoadAppTask.cancel(true);
		if(mRestoreIconTask != null)
			mRestoreIconTask.cancel(true);
		if(mSaveIconAndTitleTask != null){
			mSaveIconAndTitleTask.cancel(true);
		}
		if(mPackageReceiver != null){
			unregisterReceiver(mPackageReceiver);
			mPackageReceiver = null;
		}
		if(mWaitingDlg != null){
			mWaitingDlg.dismiss();
		}
		if(mSavingDlg != null){
			mSavingDlg.dismiss();
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if(v == mSelectBtn){
			Intent i = new Intent(Intent.ACTION_PICK, 
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI); 
			startActivityForResult(i, RESULT_LOAD_IMAGE); 
			return;
		}else if(v == mTakePhotoBtn){
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			Uri fileUri = getOutputMediaFile(MEDIA_TYPE_IMAGE); // create a file to save the image
			if(fileUri != null){
				intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
				startActivityForResult(intent, RESULT_TAKE_PICTURE);
			}
			return;
		}else if(v == mSaveImgBtn){
			startSaveIconAndTitle();
			return;
		}else if(v == mListTitle){
			setListMode(MODE_CUSTOM);
			return;
		}else if(v == mListRestore){
			setListMode(MODE_RESTORE);
			return;
		}else if(v == mRestoreBtn){
			mHandler.sendEmptyMessage(MSG_SHOW_RESTORE_DLG);
			return;
		}else if(v == mTitleView || v == mRenameTitleBtn){
			showRenameDialog();
			return;
		}else if(v == mSelectAllBtn){
			if(mIsAllSelect){
				cancelSelectAll();
			}else{
				selectAll();
			}
		}else if(v == mZoomInImgBtn){
			if(mCropView != null && mCropView.canZoomInOut(true)){
				mCropView.zoomIn();
			}
		}else if(v == mZoomOutImgBtn){
			if(mCropView != null && mCropView.canZoomInOut(false)){
				mCropView.zoomOut();
			}
		}
		
		int viewId = v.getId();
		if(viewId >= BG_ICON_ID_START  && viewId < BG_ICON_ID_START + mCustomIconImageViewList.size()){
			
			if(mSelectedIconBg != -1)
				mCustomIconContainerList.get(mSelectedIconBg).setSelected(false);
			
			if(canSelectedDefaultBg && !isImgModify){
				if(!mSaveImgBtn.isEnabled()){
					isImgModify = true;
					updateSaveImgBtn();
					isImgModify = false;
				}
			}
			mSelectedIconBg = viewId - BG_ICON_ID_START ;
			mCustomIconContainerList.get(mSelectedIconBg).setSelected(true);
			
		}
	}
	
	private void registBroadCastReceiver(){
		
		if(mPackageReceiver == null){
			mPackageReceiver = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent intent) {
					
					if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
						Uri data = intent.getData();
		                String packageName = data.getEncodedSchemeSpecificPart();
						
						for(CustomIconInfo info : mInfoList){
							if(info.getPackageName().equals(packageName)){
								boolean shouldReturn = false;
								if(info.getActivityName().equals(mAppIconActivityName)){
									shouldReturn = true;
								}
								mInfoList.remove(info);
								mAdapter.notifyDataSetChanged();
								if(shouldReturn){
									setPageIndex(INDEX_PAGE_LIST);
									mAppIconActivityName = null;
									mSelectedItem = 0;
								}
								return;
							}
						}
					}else if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)){
						startLoad();
					}
				}
			};
		}
		IntentFilter filter = new IntentFilter();
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addDataScheme("package");
		mContext.registerReceiver(mPackageReceiver, filter);

	}
	
	
	private View getCurrentPageView(){
		if(mCurrentIndex == INDEX_PAGE_LIST){
			return mPageList;
		}else if(mCurrentIndex == INDEX_PAGE_EDIT){
			return mPageEdit;
		}
		return null;
	}
	
	private void setPageIndexAnimation(final View visibleView, final View inVisibleView){
		if(visibleView == inVisibleView){
			inVisibleView.setVisibility(View.VISIBLE);
			return;
		}
		AnimatorSet animeSet = new AnimatorSet();
		ValueAnimator anime = ObjectAnimator.ofFloat(visibleView, "alpha", 1.0f);
		ValueAnimator anime2 = ObjectAnimator.ofFloat(inVisibleView, "alpha", 0.0f);
		animeSet.setDuration(400);
		if(inVisibleView != null)
			animeSet.playTogether(anime, anime2);
		else{
			animeSet.play(anime);
		}
		animeSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				visibleView.setVisibility(View.VISIBLE);
			}
			@Override
			public void onAnimationRepeat(Animator animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				if(inVisibleView != null)
					inVisibleView.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				visibleView.setVisibility(View.VISIBLE);
				if(inVisibleView != null)
					inVisibleView.setVisibility(View.INVISIBLE);
			}
		});
		animeSet.start();
	}
	
	public void setPageIndex(int pageIndex){
		if(pageIndex >= INDEX_PAGE_MAX)
			return;
		View inVisibleView = getCurrentPageView();
		mCurrentIndex = pageIndex;
		View visibleView = getCurrentPageView();
		setPageIndexAnimation(visibleView, inVisibleView);
	}
	
	 protected boolean isSystemApp(int appFlags){
	     if (((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) 
	                || ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)) {
	        return true;
	     } 
	     return false;
	 }
	
	public class CustomIconInfo {
		
		private String title;

		private Bitmap icon;
		private boolean isRestore;
		private boolean isDownLoad;

		private ComponentInfo activityInfo;
		
		public CustomIconInfo(ApplicationInfo info){
			//PackageManager pm = getPackageManager();
		    try {
                activityInfo = getPackageManager().getActivityInfo(info.componentName, 0);
            } catch (PackageManager.NameNotFoundException ex) {
                activityInfo = null;
            }
		    
			title = info.title.toString();
			icon = info.iconBitmap;//mIconCache.getIcon(info.activityInfo.name, info.activityInfo.packageName); 

			isRestore = false;
			//android.util.Log.i("QsLog", "name = " + title + " flag = " + info.flags);
			isDownLoad = isSystemApp(info.flags);

		}
		
		public CustomIconInfo(ResolveInfo info){

		    activityInfo = info.activityInfo;

			IconCacheEntry cacheitem = mIconCache.getTitleAndIcon(getComponentName(), activityInfo);
			//PackageManager pm = getPackageManager();
			title = cacheitem.title;//"" + info.loadLabel(pm);
			icon = cacheitem.icon;//mIconCache.getIcon(info.activityInfo.name, info.activityInfo.packageName); 
			isRestore = false;

		}
		
		public String getTitle() {
			return title;
		}
		public String getPackageName() {
			return activityInfo.packageName;
		}
		public String getActivityName() {
			return activityInfo.name;
		}
		public Bitmap getIcon() {
			return icon;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setIcon(Bitmap icon) {
			this.icon = icon;
		}
		
		public void setRestore(boolean value){
			isRestore = value;
		}
		
		public boolean isRestore(){
			return isRestore;
		}
		
		public ComponentName getComponentName(){
			return new ComponentName(getPackageName(), getActivityName());
		}
		
		public ComponentInfo getActivityInfo(){
		    return activityInfo;
		}
		
		public boolean isDownLoad(){
			return isDownLoad;
		}
		
		public void setIsDownLoad(boolean value){
			isDownLoad = value;
		}
		
	}
	
	static class ViewHolder {
		TextView title;
		Bitmap icon;//ImageView icon;
		CheckBox select;
	}

	BaseAdapter mAdapter = new BaseAdapter(){

		@Override
		public int getCount() {
			if(mListMode == MODE_CUSTOM)
				return mInfoList == null ? 0 : mInfoList.size();
			else if(mListMode == MODE_RESTORE)
				return mRestoreList == null ? 0 : mRestoreList.size();
			else
				return 0;
		}

		@Override
		public Object getItem(int arg0) {
			if(mListMode == MODE_CUSTOM)
				return mInfoList == null ? null :mInfoList.get(arg0);
			else if(mListMode == MODE_RESTORE)
				return mRestoreList == null ? null : mRestoreList.get(arg0);
			else
				return null;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int arg0, View view, ViewGroup arg2) {
			if(mInfoList == null){
				return null;
			}
			ViewHolder holder;
			if(view == null){
				holder = new ViewHolder();
				view = createListItemView(holder);
				view.setTag(holder);
			}else{
				holder = (ViewHolder) view.getTag();
			}
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)holder.title.getLayoutParams();
			if(mListMode == MODE_CUSTOM){
				holder.icon = (mInfoList.get(arg0).getIcon());
				holder.title.setText(mInfoList.get(arg0).getTitle());
				if(arg0 < mGridList.getNumColumns()){
					params.topMargin =(int)(getResources().getDisplayMetrics().density * 15);
				}else{
					params.topMargin =(int)(getResources().getDisplayMetrics().density * 8);
				}
				holder.select.setVisibility(View.GONE);
			}else if(mListMode == MODE_RESTORE){
				holder.icon = (mRestoreList.get(arg0).getIcon());
				holder.title.setText(mRestoreList.get(arg0).getTitle());
				params.topMargin =(int)(getResources().getDisplayMetrics().density * 0);
				holder.select.setVisibility(View.VISIBLE);
				boolean checked = mRestoreList.get(arg0).isRestore();
				holder.select.setChecked(checked);
			}
			holder.title.setCompoundDrawablesWithIntrinsicBounds(null,
	                new FastBitmapDrawable(holder.icon),
	                null, null);
			holder.title.setLayoutParams(params);
			return view;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return mInfoList == null || mInfoList.size() == 0 ? true : false;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int arg0) {
			return true;
		}
		
	};
	
	private ViewGroup createListItemView(ViewHolder holder){

		LayoutInflater inflater = getLayoutInflater();
		ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.custom_icon_grid_item, null);
		CheckBox cb = (CheckBox)layout.findViewWithTag("select");
		holder.select = cb;
		TextView tv = (TextView)layout.findViewWithTag("title");
		holder.title = tv;
		return layout;
	}
	
	protected void startLoad(){
	    if(mLoadAppTask != null)
	        mLoadAppTask.cancel(true);
	    
		mLoadAppTask = new AsyncTask<Void, Integer, Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				
				updateAppInfoList();
				
				updateRestoreList();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				//for test
//				Bitmap bm = mInfoList.get(0).icon;//((BitmapDrawable)mInfoList.get(0).icon).getBitmap();
//				android.util.Log.e("QsLog","iconSize:" + bm.getWidth());
				mAdapter.notifyDataSetChanged();
				setPageIndex(INDEX_PAGE_LIST);
				super.onPostExecute(result);
				mLoadAppTask = null;
			}
		};
		
		mLoadAppTask.execute();
	}
	
	protected void startRestore(){
	    if(mRestoreIconTask != null)
	        mRestoreIconTask.cancel(true);
	    
		mRestoreIconTask = new AsyncTask<Void, Integer, Void>(){

			@Override
			protected Void doInBackground(Void... arg0) {
				
				
				PackageManager pm = getPackageManager();
//				Intent intent = new Intent(Intent.ACTION_MAIN);
//				intent.addCategory(Intent.CATEGORY_LAUNCHER);
//				List<ResolveInfo> list = pm.queryIntentActivities(intent,0);
				List<ComponentInfo> list = new ArrayList<ComponentInfo>();
				for(CustomIconInfo customInfo : mRestoreList){
				    if(customInfo.isRestore())
				        list.add(customInfo.getActivityInfo());
				}
				
				//android.util.Log.i("QsLog", "startRestore(0)==size:"+list.size());
				
				if(list.size() > 0)
				    mIAppsManager.deleteIconAndTitles(list);
				
				int restoredNum = 0;
				for(int i=mRestoreList.size()-1; i>=0; i--){
				    CustomIconInfo customInfo = mRestoreList.get(i);
				    if(!customInfo.isRestore()){
                        continue;
                    }
				    
				    //mIAppsManager.deleteIconAndTitle(customInfo.getActivityInfo());
				    
				    mRestoreList.remove(i);
				    customInfo.setRestore(false);
				    
				    Bitmap iconBmp = null;
                    String title = null;
                    
                    mIconCache.remove(customInfo.getComponentName());
                    IconCacheEntry iconEntry = mIconCache.getTitleAndIcon(customInfo.getComponentName(), customInfo.getActivityInfo());
                    if(iconEntry != null){
                        title = iconEntry.title;
                        iconBmp = iconEntry.icon;
                    }
                    
                    customInfo.setIcon(iconBmp);
                    customInfo.setTitle(title);
                    restoredNum ++;

                    
                    publishProgress(restoredNum);
				    
				}
				return null;
			}
			@Override
			protected void onPreExecute() {
	             //showWaitingDialog();
			    mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS_DLG);
	        }

			@Override
	        protected void onProgressUpdate(Integer... values) {
	            Message msg = mHandler.obtainMessage();
                msg.what = MSG_UPDATE_PROGRESS_DLG;
                msg.arg1 = values[0];
                mHandler.sendMessage(msg);
	        }
			
			@Override
			protected void onPostExecute(Void result) {
				updateRestoreEmptyView();
				int delayTime = Math.min(TIME_RESTORE_DELAY_MS * mSelectNum, TIME_RESTORE_DELAY_MAX_MS);
				mHandler.sendEmptyMessageDelayed(MSG_HIDE_PROGRESS_DLG, delayTime);
				super.onPostExecute(result);
				mRestoreIconTask = null;
				mSelectNum = 0;
				mIsAllSelect = false;
			}
		};
		mRestoreIconTask.execute();
	}
	
	private void startSaveIconAndTitle(){
		
		 if(mSaveIconAndTitleTask != null)
			 mSaveIconAndTitleTask.cancel(true);
		
		 mSaveIconAndTitleTask = new AsyncTask<Void, Integer, Integer>(){

			@Override
			protected void onPreExecute() {
				 mSaveImgBtn.setEnabled(false);
				 mSavingDlg.show();
		    }
			 
			@Override
			protected Integer doInBackground(Void... arg0) {
				int result = 0;
				result = saveNewIconAndTitle();
				return result;
			}

			@Override
			protected void onPostExecute(Integer result) {
				mSaveResult = result;
				mHandler.removeMessages(MSG_NOTIFY_GOTO_LIST_MODE);
				mHandler.sendEmptyMessageDelayed(MSG_NOTIFY_GOTO_LIST_MODE, TIME_SAVE_DELAY_MS);
				super.onPostExecute(result);
				mSaveIconAndTitleTask = null;
			}
		};
			
		mSaveIconAndTitleTask.execute();
	}
	
	AsyncTask<Void, Integer, Void> mLoadAppTask = null;
	AsyncTask<Void, Integer, Void> mRestoreIconTask = null;
	AsyncTask<Void, Integer, Integer> mSaveIconAndTitleTask = null;

	@Override
	public void onBackPressed() {
		if(mCurrentIndex == INDEX_PAGE_EDIT)
			setPageIndex(INDEX_PAGE_LIST);
		else	
			super.onBackPressed();
	}
	
	public void setListMode(int mode){
		if(mode < MODE_CUSTOM || mode > MODE_RESTORE)
			return;
		if(mListMode == mode)
			return;
		mListMode = mode;
		if(mListMode == MODE_CUSTOM){
			mListTitle.setSelected(true);
			mListRestore.setSelected(false);
		}else if(mListMode == MODE_RESTORE){
			mListTitle.setSelected(false);
			mListRestore.setSelected(true);
		}
		setListModeAnimation(mGridList);
	}
	
	private int getListMode(){
		return mListMode;
	}
	
	private void setListModeAnimation(final View view){
		AnimatorSet animeSet = new AnimatorSet();
		ValueAnimator anime = ObjectAnimator.ofFloat(view, "alpha", 0.0f);
		ValueAnimator anime2 = ObjectAnimator.ofFloat(view, "alpha", 1.0f);
		anime.setDuration(200);
		anime2.setDuration(200);
		animeSet.playSequentially(anime, anime2);
		anime.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				
			}
			@Override
			public void onAnimationRepeat(Animator animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				listModeAnimateEnd();
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				listModeAnimateEnd();
			}
		});
		animeSet.start();
	}
	
	private void listModeAnimateEnd(){
		final int mode = getListMode();
		
		mAdapter.notifyDataSetChanged();
		
		if(mode == MODE_CUSTOM){
			mRestoreBtn.setVisibility(View.GONE);
			mSelectAllBtn.setVisibility(View.GONE);
		}else if(mode == MODE_RESTORE){
			mRestoreBtn.setVisibility(View.VISIBLE);
			mSelectAllBtn.setVisibility(View.VISIBLE);
		}
		mGridList.setSelection(0);
		mGridList.invalidate();
		updateRestoreEmptyView();
	}
	
	protected int saveNewIconAndTitle(){
	    hideIME();
	    int ret = 0;
	    String newTitle = "";
	    //android.util.Log.i("QsLog", "saveNewIconAndTitle(0)==");
	    if(isTitleModify){
	        newTitle = mTitleView.getText().toString();
	        
	        if(!TextUtils.isEmpty(newTitle) && !newTitle.equals(mInfoList.get(mSelectedItem).title)){
	            ret++;
	        } else {
	            newTitle = "";
		        isTitleModify = false;
	        }
	    }
	    //android.util.Log.i("QsLog", "saveNewIconAndTitle(1)==");
	    Bitmap icon = null;
	    Bitmap orgbitmap = null; 
	    if(isImgModify){
	        synchronized(synObject){ 
		        orgbitmap = mCropView.getResizeCropedBitmap();
	        
		        if(orgbitmap != null && !orgbitmap.isRecycled()){
		           // android.util.Log.i("QsLog", "saveNewIconAndTitle(2)==size:"+orgbitmap.getWidth()
		           //         +"x"+orgbitmap.getHeight());
		        	Drawable background = mIconUtils.getIconFrameBackground(mSelectedIconBg);
		            icon = mIconUtils.createIconBitmapWithMask(orgbitmap, background);
		            if(icon != null){
		             //   android.util.Log.i("QsLog", "saveNewIconAndTitle(3)==size:"+icon.getWidth()
		             //           +"x"+icon.getHeight());
		                ret++;
		            }
		        }
	        }
	    }
	   // android.util.Log.i("QsLog", "saveNewIconAndTitle(4)==");
	    if(ret > 0 ){
	        if(mIAppsManager != null && mIAppsManager.setIconAndTitle(mInfoList.get(mSelectedItem).getActivityInfo(), 
	                    icon, orgbitmap, newTitle)){
	            // show waiting dialog
	        }
	       // android.util.Log.i("QsLog", "saveNewIconAndTitle(5)==");
	        if(icon != null)
	            icon.recycle();
        }
	    
	    if(!isImgModify && canSelectedDefaultBg && mSelectedIconBg != -1){
	    	Bitmap appIcon = null;
	    	PackageManager pm = getPackageManager();
	    	ComponentName className = new ComponentName(mSelectedInfo.packageName,
	    			mSelectedInfo.name);
	    	try{
	    		ActivityInfo activityInfo = pm.getActivityInfo(className, 0);
		       	Drawable iconDrawable = getFullResIcon(activityInfo);
		       	if(iconDrawable != null){
		       		appIcon = ((BitmapDrawable)iconDrawable).getBitmap();
		       	}
	    	}catch (NameNotFoundException e) {
	    		
			}
	    	
	    	//android.util.Log.i("QsLog", "appIcon = " + appIcon + "/ bg = " + mSelectedIconBg);
	    	
	    	Drawable background = mIconUtils.getIconFrameBackground(mSelectedIconBg);
            icon = mIconUtils.createIconBitmapWithMask(appIcon, background);
            if(icon != null){
                ret++;
			}
	    	if(mIAppsManager != null){
	    		mIAppsManager.setDefaultIcon(mInfoList.get(mSelectedItem).getActivityInfo(), icon);
		    	mInfoList.get(mSelectedItem).setIcon(
		    			((BitmapDrawable)mIAppsManager.getIconDrawable(mSelectedInfo)).getBitmap());
		    	
		    	ArrayList<String> list = new ArrayList<String>();
		    	list.add(mInfoList.get(mSelectedItem).getPackageName());
		    	mIAppsManager.sendNotifyIconOrTitleChanged(list);
	    	}

	        if(icon != null)
	            icon.recycle();
		}
	    
	    synchronized(synObject){ 
	        if(orgbitmap != null)
	            orgbitmap.recycle();
	    }
	   // android.util.Log.i("QsLog", "saveNewIconAndTitle(4)==");
	    return ret;
	}
	
	public Drawable getFullResIcon(ActivityInfo info) {
		PackageManager pm = getPackageManager();
        Resources resources;
        try {
            resources = pm.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return null;
    }

	 public Drawable getFullResIcon(Resources resources, int iconId) {
	        Drawable d;
	        try {
	            d = resources.getDrawableForDensity(iconId, DisplayMetrics.DENSITY_XXHIGH);
	        } catch (Resources.NotFoundException e) {
	            d = null;
	        }
	        return (d != null) ? d : null;
	 }

	
	protected int saveRenamedTitle(){
		hideIME();
		
		if(!isTitleModify)
			return 0;
		//String newTitle = mRenameInput.getText().toString();
		String newTitle = mTitleView.getText().toString();
		if(newTitle.length() != 0){
			mInfoList.get(mSelectedItem).title = newTitle;
			mIAppsManager.setTitle(mInfoList.get(mSelectedItem).getActivityInfo(), newTitle);
		}
		//mTitleView.setText(newTitle);
		isTitleModify = false;
		return 1;
	}

	
	private void updateSaveImgBtn(){
		boolean isEnabled = (isImgModify || isTitleModify) && (mTitleView.getText().length() > 0);
		mSaveImgBtn.setEnabled(isEnabled);
		mSaveImgBtn.setTextColor(isEnabled ? 0xFF000000 : 0xFF707070);
	}
	
	private void updatePreviewIconEnable(boolean enable){
		int index = 0;
		for(ImageView image : mCustomIconImageViewList){
			image.setEnabled(enable);
			if(!enable){
				image.setImageDrawable(mIconBgList.get(index));
				index ++;
			}
		}
		for(View container : mCustomIconContainerList){
			container.setSelected(false);
			container.setAlpha(enable ? 1.0f : 0.3f);
		}
		if(enable){
			mCustomIconContainerList.get(0).setSelected(true);
	    	mSelectedIconBg = 0;
		}
	}
	
	private void updateIndicatorVisibility(boolean visible){
		mSelectPicLable.setVisibility(visible ? View.VISIBLE : View.GONE);
		mIndicatorArrow.setVisibility(visible ? View.VISIBLE : View.GONE);
		mIconRegion.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	private void updateAppInfoList(){
		mInfoList.clear();
		List<ApplicationInfo> list = mLauncher.getModel().getAllAppsList();
		if(list != null && list.size() > 0){
			for(ApplicationInfo info : list){
				if(CUSTOM_APP_ICON_CLASS_NAME.equals(info.getClassName())){
					continue;
				}
				if(ConfigOption.QS_PRJ_NAME.startsWith("A802C_AOC")
					&& MTK_APP_ICON_TODO_CLASS_NAME.equals(info.getClassName())){
					continue;
				}
				mInfoList.add(new CustomIconInfo(info));
			}
		} else {
			PackageManager pm = getPackageManager();
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> listinfo = pm.queryIntentActivities(intent,0);
			for(ResolveInfo info : listinfo){
				if(CUSTOM_APP_ICON_CLASS_NAME.equals(info.activityInfo.name)){
					continue;
				}
				CustomIconInfo item = new CustomIconInfo(info);
				mInfoList.add(item);
			}
		}
	}
	
	private void updateRestoreList(){
		mRestoreList.clear();
		for(CustomIconInfo info : mInfoList){
			if(mIAppsManager.checkIconOrTitleModified(info.getActivityInfo())) {
				mRestoreList.add(info);
				if(DEBUG){
					android.util.Log.i(TAG, "info:" + info.getActivityName());
				}
			}
		}
	}
	
	private void updateRestoreEmptyView(){
		if(mRestoreList.size() > 0 || mListMode != MODE_RESTORE){
			mGridListEmpty.setVisibility(View.GONE);
		}else if(mListMode == MODE_RESTORE){
			mGridListEmpty.setVisibility(View.VISIBLE);
		}
	}
	
	private Bitmap resizeAppBitmap(Bitmap input){

		if(input.getWidth() > mAppIconsize || input.getHeight() > mAppIconsize){
			float scale = 1.0f * mAppIconsize / Math.min(input.getHeight(), input.getWidth());
			//android.util.Log.i("QsLog", "scale:" + scale + " w:" + input.getWidth() + " h:" + input.getHeight());
	        Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);
			return Bitmap.createBitmap(input, 0, 0,
					input.getWidth(),input.getHeight(), matrix, true);
		}
		return input;
	}
	
	private void hideIME(){
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); 
		imm.hideSoftInputFromWindow(mTitleView.getWindowToken(), 0);
	}
	
	private int getSelectedRestoreCount(){
		int restoreNum = 0;
		for(CustomIconInfo info : mRestoreList){
			if(info.isRestore()){
				restoreNum ++;
			}
		}
		return restoreNum;
	}
	
	public void selectAll(){
		if(mListMode != MODE_RESTORE)
			return;
		selectGroup(true);
		mSelectAllBtn.setImageResource(R.drawable.ic_unselect_all);
		mSelectNum = mRestoreList.size();
	}
	
	public void cancelSelectAll(){
		if(mListMode != MODE_RESTORE)
			return;
		mSelectNum = 0;
		selectGroup(false);
		mSelectAllBtn.setImageResource(R.drawable.ic_select_all);
	}
	
	private void selectGroup(boolean isAll){
		if(mListMode != MODE_RESTORE)
			return ;
		for(CustomIconInfo info : mRestoreList){
			info.setRestore(isAll);
		}
		mAdapter.notifyDataSetChanged();
		mIsAllSelect = isAll;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME){
			onBackPressed();
		}
	    return super.onKeyDown(keyCode, event);
	}
	
	private void setNewTitleAndIcon(ApplicationInfo appInfo, CustomIconInfo customInfo){
		customInfo.setTitle(appInfo.title + "");
		customInfo.setIcon(appInfo.iconBitmap);
	}
	
	public void bindAppsAdded(List<ApplicationInfo> apps){
	    
	}
	
    public void bindAppsUpdated(List<ApplicationInfo> apps){
        android.util.Log.i("QsLog", "bindAppsUpdated()=="+apps);
        if(mListMode == MODE_RESTORE)
        	return;
        if(apps != null && apps.size() > 0){
        	for(ApplicationInfo appInfo : apps){

	        	String clsName = appInfo.getClassName();
	           	CustomIconInfo info = mInfoList.get(mSelectedItem);
	        	
	        	if(info.getActivityName().equals(clsName)){
	        		setNewTitleAndIcon(appInfo, info);
	        	}else{
		        	for(CustomIconInfo infos : mInfoList){
		        		if(infos.getActivityName().equals(clsName)){
		        			setNewTitleAndIcon(appInfo, infos);
		        		}
		        	}
	        	}
        	}
        	mHandler.removeMessages(MSG_NOTIFY_GOTO_LIST_MODE);
        	mHandler.sendEmptyMessage(MSG_NOTIFY_SAVE_COMPLETED);
        }
    }
    
    public void bindAppsRemoved(List<String> packageNames, boolean permanent){
        
    }
    
    public void bindPackagesUpdated(){
        
    }
    
    public void resetBgSelected(boolean withImage){
    	for(int i = 0; i < mCustomIconContainerList.size(); i++){
    		mCustomIconContainerList.get(i).setSelected(false);
 			if(withImage)
 				mCustomIconImageViewList.get(i).setImageDrawable(mIconBgList.get(i));
		}
    	mSelectedIconBg = -1;
    }
    
    public boolean checkIsDownLoadAppAndModify(ComponentInfo name){
    	if(mInfoList.get(mSelectedItem).isDownLoad())
    		return !mIAppsManager.checkIconModified(name)
    			&& (mIconBgList.size() > 1);
    	else
    		return false;
    }
}
