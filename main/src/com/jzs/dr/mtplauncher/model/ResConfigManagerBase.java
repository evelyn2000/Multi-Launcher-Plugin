package com.jzs.dr.mtplauncher.model;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

//import com.jzs.dr.mtplauncher.sjar.IResConfigManager;
import com.jzs.dr.mtplauncher.sjar.model.ResConfigManager;
import com.jzs.common.launcher.ISharedPrefSettingsManager;

import com.jzs.dr.mtplauncher.R;

public final class ResConfigManagerBase extends ResConfigManager{

	private static Map<Integer, Integer> sGlobalResInfoMap = new HashMap<Integer, Integer>();
    static {
    	
    	///////////////////// layout  /////////////////////
    	sGlobalResInfoMap.put(LAYOUT_USER_FOLDER, R.layout.user_folder);
    	sGlobalResInfoMap.put(LAYOUT_FOLDER_ICON, R.layout.folder_icon);
    	sGlobalResInfoMap.put(LAYOUT_INSTALL_WIDGET_ADAPTER, R.layout.external_widget_drop_list_item);
    	sGlobalResInfoMap.put(LAYOUT_APP_WIDGET_ERROR, R.layout.appwidget_error);
    	sGlobalResInfoMap.put(LAYOUT_ADD_LIST_ITEM, R.layout.add_list_item);
    	sGlobalResInfoMap.put(LAYOUT_APPLICATION_SHORTCUT, R.layout.application_with_unread);
    	sGlobalResInfoMap.put(LAYOUT_APPS_CUSTOMIZE_PAGE_SCREEN, R.layout.apps_customize_page_screen);
    	sGlobalResInfoMap.put(LAYOUT_APPS_CUSTOMIZE_APP_WITH_UNREAD, R.layout.apps_customize_application_with_unread);
    	sGlobalResInfoMap.put(LAYOUT_APPS_CUSTOMIZE_WIDGET, R.layout.apps_customize_widget);
    	sGlobalResInfoMap.put(LAYOUT_ADD_WORKSPACE_PREVIEW_ITEM, R.layout.workspace_preview_item_image_add);
    	sGlobalResInfoMap.put(LAYOUT_APPLICATION_SHORTCUT_NORESIZE, R.layout.application_with_unread_noautoresize);
    	
    	///////////////////// drawable /////////////////////////////
    	sGlobalResInfoMap.put(IMG_FOLDER_RING_ANIMATOR_OUTER, R.drawable.portal_ring_outer_holo);
    	sGlobalResInfoMap.put(IMG_FOLDER_RING_ANIMATOR_SHARED_OUTER, R.drawable.portal_ring_outer_holo);
    	sGlobalResInfoMap.put(IMG_FOLDER_RING_ANIMATOR_INNER, R.drawable.portal_ring_inner_holo);
    	sGlobalResInfoMap.put(IMG_FOLDER_RING_ANIMATOR_SHARED_INNER, R.drawable.portal_ring_outer_holo);
    	sGlobalResInfoMap.put(IMG_FOLDER_RING_ANIMATOR_SHARED_LEAVE, R.drawable.portal_ring_rest);
    	
    	sGlobalResInfoMap.put(IMG_APP_WIDGET_RESIZE_BG, R.drawable.widget_resize_frame_holo);
    	sGlobalResInfoMap.put(IMG_APP_WIDGET_RESIZE_HANDLE_LEFT, R.drawable.widget_resize_handle_left);
    	sGlobalResInfoMap.put(IMG_APP_WIDGET_RESIZE_HANDLE_RIGHT, R.drawable.widget_resize_handle_right);
    	sGlobalResInfoMap.put(IMG_APP_WIDGET_RESIZE_HANDLE_TOP, R.drawable.widget_resize_handle_top);
    	sGlobalResInfoMap.put(IMG_APP_WIDGET_RESIZE_HANDLE_BOTTOM, R.drawable.widget_resize_handle_bottom);
    	sGlobalResInfoMap.put(IMG_IC_LAUNCHER_WALLPAPER, R.drawable.ic_launcher_wallpaper);
    	sGlobalResInfoMap.put(IMG_WIDGET_PREVIEW_TILE, R.drawable.widget_preview_tile);
    	sGlobalResInfoMap.put(IMG_FOLDER_DEFAULT_ICON, R.drawable.ic_folder_default);
    	sGlobalResInfoMap.put(IMG_WORKSPACE_GRID_SIZE_DEF, R.drawable.workspace_custom_settings_grid_size_4x4);
    	sGlobalResInfoMap.put(IMG_FOLDER_PREVIEW_BG_ICON, R.drawable.ic_folder_preview_bg);
    	
    	
    	///////////////////// config  /////////////////////
    	sGlobalResInfoMap.put(CONFIG_DROP_ANIM_MIN_DURATION, R.integer.config_dropAnimMinDuration);
    	sGlobalResInfoMap.put(CONFIG_DROP_ANIM_MAX_DURATION, R.integer.config_dropAnimMaxDuration);
    	sGlobalResInfoMap.put(CONFIG_DROP_ANIM_MAX_DIST, R.integer.config_dropAnimMaxDist);
    	sGlobalResInfoMap.put(CONFIG_FLING_TO_DELETE_MIN_VELOCITY, R.integer.config_flingToDeleteMinVelocity);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_SCREEN_COUNT, R.integer.config_defaultWorkspaceCount);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_DEFAULT_SCREEN, R.integer.config_defaultWorkspaceScreen);
    	sGlobalResInfoMap.put(CONFIG_HOTSET_ALLAPP_INDEX, R.integer.hotseat_all_apps_index);
    	sGlobalResInfoMap.put(CONFIG_HOTSET_CELL_COUNT, R.integer.hotseat_cell_count);
    	sGlobalResInfoMap.put(CONFIG_ALLOW_SCREEN_ROTATION, R.bool.allow_rotation);
    	sGlobalResInfoMap.put(CONFIG_DRAGOUTLINE_FADETIME, R.integer.config_dragOutlineFadeTime);
    	sGlobalResInfoMap.put(CONFIG_DRAGOUTLINE_MAXALPHA, R.integer.config_dragOutlineMaxAlpha);
    	sGlobalResInfoMap.put(CONFIG_FOLDER_ANIM_DURATION, R.integer.config_folderAnimDuration);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_CELL_COUNTX, R.integer.cell_count_x);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_CELL_COUNTY, R.integer.cell_count_y);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_LOADSHRINKPERCENT, R.integer.config_workspaceSpringLoadShrinkPercentage);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_FADEADJACENTSCREENS, R.bool.config_workspaceFadeAdjacentScreens);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_MAX_SCREENCOUNT, R.integer.config_defaultWorkspaceMaxCount);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_MIN_SCREENCOUNT, R.integer.config_defaultWorkspaceMinCount);
    	sGlobalResInfoMap.put(CONFIG_APPS_CUSTOMIZE_DRAGSLOPETHRESHOLD, R.integer.config_appsCustomizeDragSlopeThreshold);
    	sGlobalResInfoMap.put(CONFIG_ENABLE_STATIC_WALLPAPER, R.bool.config_enableStaticWallpaper);
    	sGlobalResInfoMap.put(CONFIG_WORKSPACE_SUPPORT_CYCLYSLIDING, R.bool.config_workspaceSupportCycleSliding);
    	sGlobalResInfoMap.put(CONFIG_APPS_SUPPORT_CYCLYSLIDING, R.bool.config_appsSupportCycleSliding);
    	sGlobalResInfoMap.put(CONFIG_APPS_WIDGET_SLIDE_TOGETHER, R.bool.config_slideAppAndWidgetTogether);
    	sGlobalResInfoMap.put(CONFIG_SHOW_BAR_SCREENINDICATOR, R.bool.config_showScreenIndicatorBar);
    	sGlobalResInfoMap.put(CONFIG_SHOW_BAR_HOTSEAT, R.bool.config_showHotseatBar);
    	sGlobalResInfoMap.put(CONFIG_SHOW_WORKSPACE_LABEL, R.bool.config_showWorkspaceLabel);
    	sGlobalResInfoMap.put(CONFIG_IS_FULLSCREEN, R.bool.config_isFullScreen);
    	sGlobalResInfoMap.put(CONFIG_SUPPORT_GESTURE, R.bool.config_supportGesture);
    	sGlobalResInfoMap.put(CONFIG_MAX_WORKSPACE_CELL_COUNTX, R.integer.config_workspaceMaxCellX);
        sGlobalResInfoMap.put(CONFIG_MAX_WORKSPACE_CELL_COUNTY, R.integer.config_workspaceMaxCellY);
        sGlobalResInfoMap.put(CONFIG_DEFAULT_ANIMATE_EFFECT_TYPE, R.string.config_defaultAnimateEffect);
        sGlobalResInfoMap.put(CONFIG_DEFAULT_APP_ICON_SORT_TYPE, R.integer.config_defaultIconSort_Index);
    	
    	///////////////////// string  /////////////////////
    	sGlobalResInfoMap.put(STR_FOLDER_NAME_FORMAT, R.string.folder_name_format);
    	sGlobalResInfoMap.put(STR_FOLDER_TAP_TO_CLOSE, R.string.folder_tap_to_close);
    	sGlobalResInfoMap.put(STR_FOLDER_TAP_TO_RENAME, R.string.folder_tap_to_rename);
    	sGlobalResInfoMap.put(STR_INSTALL_WIDGET_PICK_FORMAT, R.string.external_drop_widget_pick_format);
    	sGlobalResInfoMap.put(STR_UNINSTALL_SYSTEM_APP, R.string.uninstall_system_app_text);
    	sGlobalResInfoMap.put(STR_DELETE_TARGET_UNINSTALL_LABEL, R.string.delete_target_uninstall_label);
    	sGlobalResInfoMap.put(STR_DELETE_TARGET_LABEL, R.string.delete_target_label);
    	sGlobalResInfoMap.put(STR_GROUP_WALLPAPERS, R.string.group_wallpapers);
    	sGlobalResInfoMap.put(STR_ACTIVITY_NOT_FOUND, R.string.activity_not_found);
    	sGlobalResInfoMap.put(STR_CHOOSE_WALLPAPER, R.string.chooser_wallpaper);
    	sGlobalResInfoMap.put(STR_FOLDER_DEFAULT_NAME, R.string.folder_name);
    	sGlobalResInfoMap.put(STR_FOLDER_RENAMED_FORMAT, R.string.folder_renamed);
    	sGlobalResInfoMap.put(STR_FOLDER_OPENED_FORMAT, R.string.folder_opened);
    	sGlobalResInfoMap.put(STR_FOLDER_CLOSED_FORMAT, R.string.folder_closed);
    	sGlobalResInfoMap.put(STR_FOLDER_HINT_TEXT, R.string.folder_hint_text);
    	sGlobalResInfoMap.put(STR_DEFAULT_SCROLL_FORMAT, R.string.default_scroll_format);
    	sGlobalResInfoMap.put(STR_HOTSEAT_OUT_OF_SPACE, R.string.hotseat_out_of_space);
    	sGlobalResInfoMap.put(STR_OUT_OF_SPACE, R.string.out_of_space);
    	sGlobalResInfoMap.put(STR_GROUP_APPLICATIONS, R.string.group_applications);
    	sGlobalResInfoMap.put(STR_TITLE_SELECT_APPLICATION, R.string.title_select_application);
    	sGlobalResInfoMap.put(STR_LONG_PRESS_WIDGET_TO_ADD, R.string.long_press_widget_to_add);
    	sGlobalResInfoMap.put(STR_APPS_CUSTOMIZE_APPS_SCROLL_FORMAT, R.string.apps_customize_apps_scroll_format);
    	sGlobalResInfoMap.put(STR_APPS_CUSTOMIZE_WIDGET_SCROLL_FORMAT, R.string.apps_customize_widgets_scroll_format);
    	sGlobalResInfoMap.put(STR_MENU_CUSTOM_SETTINGS, R.string.menu_system_custom_settings);
    	sGlobalResInfoMap.put(STR_MENU_CUSTOM_ICON_SETTINGS, R.string.menu_change_app_icon);
    	sGlobalResInfoMap.put(STR_ALL_APPS_BTN_LABEL, R.string.all_apps_button_label);
    	sGlobalResInfoMap.put(STR_CUSTOM_SETTINGS_LABEL, R.string.workspace_custom_settings_grid);
    	sGlobalResInfoMap.put(STR_MENU_CREATE_FOLDER, R.string.menu_create_new_folder);
    	sGlobalResInfoMap.put(STR_MENU_SORT_APPS_ICON, R.string.menu_change_sort_type);
    	sGlobalResInfoMap.put(STR_MENU_ANIMATE_EFFECT, R.string.menu_animate_effect_type);
    	
    	///////////////////// Dimension  /////////////////////
    	sGlobalResInfoMap.put(DIM_FOLDER_ICON_PREVIEW_SIZE, R.dimen.folder_preview_size);
    	sGlobalResInfoMap.put(DIM_FOLDER_ICON_PREVIEW_PADDING, R.dimen.folder_preview_padding);
    	
    	sGlobalResInfoMap.put(DIM_SCROLL_ZONE, R.dimen.scroll_zone);
    	sGlobalResInfoMap.put(DIM_DRAG_VIEW_OFFSETX, R.dimen.dragViewOffsetX);
    	sGlobalResInfoMap.put(DIM_DRAG_VIEW_OFFSETY, R.dimen.dragViewOffsetY);
    	sGlobalResInfoMap.put(DIM_DRAG_VIEW_SCALE, R.dimen.dragViewScale);
    	
    	sGlobalResInfoMap.put(DIM_APPS_CELL_WIDTH, R.dimen.apps_customize_cell_width);
    	sGlobalResInfoMap.put(DIM_APPS_CELL_HEIGHT, R.dimen.apps_customize_cell_height);
//    	sGlobalResInfoMap.put(DIM_APPS_CELL_WIDTH_GAP, R.dimen.apps_customize_cell_width);
//    	sGlobalResInfoMap.put(DIM_APPS_CELL_HEIGHT_GAP, R.dimen.dragViewScale);
    	sGlobalResInfoMap.put(DIM_APPS_CELL_MAX_GAP, R.dimen.apps_customize_max_gap);
    	sGlobalResInfoMap.put(DIM_APP_ICON_SIZE, R.dimen.app_icon_size);
    	
    	sGlobalResInfoMap.put(DIM_WORKSPACE_LEFT_PADDING_LAND, R.dimen.workspace_left_padding_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_RIGHT_PADDING_LAND, R.dimen.workspace_right_padding_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_TOP_PADDING_LAND, R.dimen.workspace_top_padding_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_BOTTOM_PADDING_LAND, R.dimen.workspace_bottom_padding_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_LEFT_PADDING_PORT, R.dimen.workspace_left_padding_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_RIGHT_PADDING_PORT, R.dimen.workspace_right_padding_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_TOP_PADDING_PORT, R.dimen.workspace_top_padding_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_BOTTOM_PADDING_PORT, R.dimen.workspace_bottom_padding_port);
    	
    	sGlobalResInfoMap.put(DIM_WORKSPACE_CELL_WIDTH_LAND, R.dimen.workspace_cell_width_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_CELL_HEIGHT_LAND, R.dimen.workspace_cell_height_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_WIDTH_GAP_LAND, R.dimen.workspace_width_gap_land);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_HEIGHT_GAP_LAND, R.dimen.workspace_height_gap_land);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_LEFT_PADDING_LAND, R.dimen.cell_layout_left_padding_land);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_RIGHT_PADDING_LAND, R.dimen.cell_layout_right_padding_land);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_TOP_PADDING_LAND, R.dimen.cell_layout_top_padding_land);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_BOTTOM_PADDING_LAND, R.dimen.cell_layout_bottom_padding_land);
    	
    	sGlobalResInfoMap.put(DIM_WORKSPACE_CELL_WIDTH_PORT, R.dimen.workspace_cell_width_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_CELL_HEIGHT_PORT, R.dimen.workspace_cell_height_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_WIDTH_GAP_PORT, R.dimen.workspace_width_gap_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_HEIGHT_GAP_PORT, R.dimen.workspace_height_gap_port);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_LEFT_PADDING_PORT, R.dimen.cell_layout_left_padding_port);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_RIGHT_PADDING_PORT, R.dimen.cell_layout_right_padding_port);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_TOP_PADDING_PORT, R.dimen.cell_layout_top_padding_port);
    	sGlobalResInfoMap.put(DIM_CELLLAYOUT_BOTTOM_PADDING_PORT, R.dimen.cell_layout_bottom_padding_port);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_MAX_GAP, R.dimen.workspace_max_gap);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_CELL_WIDTH, R.dimen.workspace_cell_width);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_CELL_HEIGHT, R.dimen.workspace_cell_height);
    	sGlobalResInfoMap.put(DIM_HOTSEAT_UNREAD_MARGIN_RIGHT, R.dimen.hotseat_unread_margin_right);
    	sGlobalResInfoMap.put(DIM_WORKSPACE_UNREAD_MARGIN_RIGHT, R.dimen.workspace_unread_margin_right);
    	sGlobalResInfoMap.put(DIM_APP_ICON_PADDING_TOP, R.dimen.app_icon_padding_top);
    	
    	sGlobalResInfoMap.put(DIM_SHORTCUT_PREVIEW_PADDING_TOP, R.dimen.shortcut_preview_padding_top);
    	sGlobalResInfoMap.put(DIM_SHORTCUT_PREVIEW_PADDING_LEFT, R.dimen.shortcut_preview_padding_left);
    	sGlobalResInfoMap.put(DIM_SHORTCUT_PREVIEW_PADDING_RIGHT, R.dimen.shortcut_preview_padding_right);
    	sGlobalResInfoMap.put(DIM_BUTTON_BAR_HOTSEAT_HEIGHT, R.dimen.button_bar_hotseat_height);
    	sGlobalResInfoMap.put(DIM_BUTTON_BAR_SCREENINDICATOR_HEIGHT, R.dimen.button_bar_screenindicator_height);
    	//sGlobalResInfoMap.put(DIM_APP_ICON_PADDING_TOP, R.dimen.shortcut_preview_padding_top);
    	
    	
    	
    	///////////////////// colors  /////////////////////
    	sGlobalResInfoMap.put(CLR_INFO_TARGET_HOVER_HINT, R.color.info_target_hover_tint);
    	
    	///////////////////// Others  /////////////////////
    	sGlobalResInfoMap.put(OTHER_WORKSPACE_DEFAULT_XML, R.xml.default_workspace);
    	sGlobalResInfoMap.put(OTHER_UNREAD_SHORTCUTS_XML, R.xml.unread_support_shortcuts);
    	
    	sGlobalResInfoMap.put(ARRAY_WORKSPACE_GRID_SIZE_PREVIEWS, R.array.config_workspaceGridSize_previewimages);
    	sGlobalResInfoMap.put(ARRAY_WORKSPACE_GRID_SIZE_ENTRIES, R.array.config_workspaceGridSize_entries);
    	sGlobalResInfoMap.put(ARRAY_WORKSPACE_GRID_SIZE_VALUES, R.array.config_workspaceGridSize_values);
    	sGlobalResInfoMap.put(ARRAY_APP_ICONS_SORT_ENTRIES, R.array.config_apps_icons_sort_entries);
    	
    	sGlobalResInfoMap.put(ARRAY_ANIMATE_EFFECT_ENTRIES, R.array.config_animate_effects_names);
    	sGlobalResInfoMap.put(ARRAY_ANIMATE_EFFECT_VALUES, R.array.config_animate_effects_values);
    	sGlobalResInfoMap.put(IGNORE_APP_LIST_XML, R.xml.ignore_app_list);
    	
    }
    
	private static ResConfigManager sResConfigManager;
	public static ResConfigManager getInstance(Context context, ISharedPrefSettingsManager pref){
    	if (sResConfigManager == null) {
    		sResConfigManager = new ResConfigManagerBase(context, pref);
        }
        return sResConfigManager;
    }
	
	public ResConfigManagerBase(Context context, ISharedPrefSettingsManager pref){
		super(context, pref, null);
	}
	
	//global	
	@Override
	public int getResourceId(int type){
		Integer res = sGlobalResInfoMap.get(type);
		if(res != null){
			return res;
		}
		return super.getResourceId(type);
	}

}
