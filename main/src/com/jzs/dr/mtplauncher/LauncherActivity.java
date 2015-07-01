package com.jzs.dr.mtplauncher;

import com.jzs.common.launcher.LauncherHelper;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

public class LauncherActivity extends Activity /*implements 
		View.OnClickListener, View.OnLongClickListener, View.OnTouchListener */{

    //private ILauncherPluginEntry mLauncherPluginEntry;
    private LauncherHelper mLauncher;
    private final BroadcastReceiver mCloseSystemDialogsReceiver
    				= new CloseSystemDialogsIntentReceiver();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LauncherApplicationMain app = ((LauncherApplicationMain)getApplication());
		mLauncher = app.setLauncherActivity(this);

		mLauncher.onCreate(savedInstanceState);
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mCloseSystemDialogsReceiver, filter);
	}
	
	@Override
    public Resources getResources() {
	    if(mLauncher != null)
	        return mLauncher.getResources();
	    return super.getResources();
	}

	@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);     
        //android.util.Log.i("QsLog", "onNewIntent()====");
        mLauncher.onNewIntent(intent);
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        //android.util.Log.i("QsLog", "onResume()====");
        mLauncher.onResume();
	}
	
	@Override
    protected void onPause() {
		//android.util.Log.i("QsLog", "onPause()====");
		mLauncher.onPause();
		super.onPause();
	}
	@Override
	public void onStart() {
		//android.util.Log.i("QsLog", "onStart()====");
		super.onStart();
		mLauncher.onStart();
	}
	@Override
	public void onStop() {
		//android.util.Log.i("QsLog", "onStop()====");
		super.onStop();
		mLauncher.onStop();
	}
	
	@Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLauncher.onAttachedToWindow();
	}
	
	@Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLauncher.onDetachedFromWindow();
	}
	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		mLauncher.onUserLeaveHint();
    }
//	public void onWindowVisibilityChanged(int visibility) {
//		
//	}
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		mLauncher.onCreateContextMenu(menu, v, menuInfo);
    }
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if(mLauncher.onContextItemSelected(item))
			return true;
		return super.onContextItemSelected(item);
	}
	@Override
	public void onContextMenuClosed(Menu menu){
		mLauncher.onContextMenuClosed(menu);
		super.onContextMenuClosed(menu);
	}
	@Override
	protected Dialog onCreateDialog(int id) {
        return mLauncher.onCreateDialog(id);
    }
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
	    mLauncher.onPrepareDialog(id, dialog);
	    super.onPrepareDialog(id, dialog);
    }
	@Override
	public void onConfigurationChanged(Configuration newConfig){
	    super.onConfigurationChanged(newConfig);
		mLauncher.onConfigurationChanged(newConfig);
	}
	@Override
	public void onAttachFragment(Fragment fragment) {
		mLauncher.onAttachFragment(fragment);
    }
	
	@Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mLauncher.onRestoreInstanceState(state);
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        //outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
        super.onSaveInstanceState(outState);
        mLauncher.onSaveInstanceState(outState);
	}
	
	@Override
    public void onDestroy() {
		unregisterReceiver(mCloseSystemDialogsReceiver);
		
        super.onDestroy();
        mLauncher.onDestroy();
	}
	
	@Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		mLauncher.onActivityResult(requestCode, resultCode, data);
	}
    		
	@Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
		
		if(mLauncher.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch))
			return;
		super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
	}
	
//	@Override
//    public boolean onSearchRequested() {
//        startSearch(null, false, null, true);
//        // Use a custom animation for launching search
//        return true;
//    }
	
//	@Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//		if(mLauncher.dispatchKeyEvent(event))
//			return true;
//		return super.dispatchKeyEvent(event);
//	}
	
	@Override
    public void onBackPressed() {
		mLauncher.onBackPressed();
//		if(mLauncher.onBackPressed())
//			return;
//		super.onBackPressed();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return mLauncher.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mLauncher.onPrepareOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if(mLauncher.onOptionsItemSelected(item))
			return true;
		
        return super.onOptionsItemSelected(item);
    }
	
	public void onClickSearchButton(View v) {
		
	}
	
	public void onClickVoiceButton(View v) {
		
	}
	
	public void onClickAllAppsButton(View v) {
		mLauncher.onClickAllAppsButton(v);
	}
	
	public void onTouchDownAllAppsButton(View v) {
		mLauncher.onTouchDownAllAppsButton(v);
    }
	
	public void onClickAppMarketButton(View v) {
		
	}
	
	@Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
	}
	
	@Override
    public void onWindowFocusChanged(boolean hasFocus) {
		mLauncher.onWindowFocusChanged(hasFocus);
	}
	
	@Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        return mLauncher.dispatchPopulateAccessibilityEvent(event, result);
	}
	@Override
    public Object onRetainNonConfigurationInstance(){
		Object result = mLauncher.onRetainNonConfigurationInstance(); 
		if(result != null)
			return result;
			
		return Boolean.TRUE;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean handled = super.onKeyDown(keyCode, event);
		return mLauncher.onKeyDown(keyCode, event, handled);
	}
	
	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
		boolean handled = super.onKeyUp(keyCode, event);
		return mLauncher.onKeyUp(keyCode, event, handled);
	}
	
	public void closeSystemDialogs() {
        getWindow().closeAllPanels();
        mLauncher.closeSystemDialogs();
    }
	
	/**
     * Receives notifications when system dialogs are to be closed.
     */
    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            if (LauncherLog.DEBUG) {
//                LauncherLog.d(TAG, "Close system dialogs: intent = " + intent);
//            }
            closeSystemDialogs();
        }
    }
	
}
