// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.utilities;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.os.Handler;

import com.kofax.mobilecapture.R;

/// A class defining all the enums and their utility methods used in application.

public class Globals {

	static Globals pSelf = null;
	
	public enum AppLoginStatus {		
		LOGIN_ONLINE,
		LOGOUT_ONLINE,
		LOGIN_OFFLINE,
		LOGOUT_OFFLINE
	}
	
	public enum AppModeStatus {		
		ONLINE_MODE,
		OFFLINE_MODE,
		FORCE_OFFLINEMODE
	}
	
	public static AppModeStatus gAppModeStatus = AppModeStatus.OFFLINE_MODE;
	public static AppLoginStatus gAppLoginStatus = AppLoginStatus.LOGOUT_ONLINE;
	/// The Messages enum is used in application in different message handlers.
	public enum Messages {
		MESSAGE_DEFAULT,
		MESSAGE_DIALOG_EMAIL_INFORMATION,
		MESSAGE_DIALOG_SUBMIT_CONFIRMATION,
		MESSAGE_DLALOG_INVALID_EMAIL,
		MESSAGE_DIALOG_ERROR,
		MESSAGE_DIALOG_OFFLINE_ERROR,
		MESSAGE_DIALOG_CANCEL,
		MESSAGE_DIALOG_LOGOUT_CONFIRMATION,
		MESSAGE_DIALOG_RELOGIN_CONFIRMATION,
		MESSAGE_DIALOG_LOGIN_FAILURE,
		MESSAGE_DIALOG_IMAGE_REVERT_CONFIRMATION,
		MESSAGE_DIALOG_IMAGE_DELETE_CONFIRMATION,
		MESSAGE_DIALOG_REQUIRED_FIELDS,
		MESSAGE_DIALOG_GIFTCARD_RETAKE_CONFIRMATION,
		MESSAGE_DIALOG_CHANGE_ITEM_TYPE_CONFIRMATION,
		MESSAGE_DIALOG_DELETE_CONFIRMATION,
		MESSAGE_DIALOG_IMPORT_DATA_CONFIRMATION,
		MESSAGE_DIALOG_OFFLINE_CONFIRMATION,
		MESSAGE_DIALOG_LOW_MEMORY_ERROR,
		MESSAGE_DIALOG_ONLINE_CONFIRMATION,
		MESSAGE_LOGIN,
		MESSAGE_OFFLINE_LOGIN,
		MESSAGE_CHANGE_ITEM_TYPE,
		MESSAGE_IMAGE_PROCESSING_STARTED,
		MESSAGE_IMAGE_PROCESSED,
		MESSAGE_IMAGE_PROCESS_CANCELLED,
		MESSAGE_PROCESS_QUEUE_PAUSED,
		MESSAGE_PROCESS_QUEUE_HALTED,
		MESSAGE_PROCESS_FAILED,
		MESSAGE_IMAGE_DELETED,
		MESSAGE_IMAGE_FLIP,
		MESSAGE_GIFTCARD_EXTRACTION_COMPLETED,
		MESSAGE_GIFTCARD_VALIDATION_COMPLETED,
		MESSAGE_FADEOUT_INFO_POPUP,
		MESSAGE_DOWNLOAD_DOCUMENTS_FAILED,
		MESSAGE_DIALOG_PERMISSION
	}

	/// Application wide result codes.
	public enum ResultState {
		RESULT_OK,
		RESULT_CANCELED,
		RESULT_FAILED
	}

	/// The AlertType enum is used by application to display alert messages of given types.
	public enum AlertType {
		UNKNOWN,
		INFO_ALERT, 
		CONFIRM_ALERT,
		CONFIRM_ALERT_WITH_CHECKBOX,// added alert type to show confirmation dialog with check box
		ERROR_ALERT,
		LIST_ALERT,
		PROGRESS_ALERT
	}

	/// The ImageType enum is used by application to specify what type of image is capture/selected from the camera/gallery.
	public enum ImageType {
		PHOTO,
		DOCUMENT
	}

	/// The RequestCode enum is used by application to launch activities and get the result back on onActivityResult() when the launched activity exits.  
	public enum RequestCode {
		NONE,
		CAPTURE_DOCUMENT,
		CAPTURE_PHOTO,
		SELECT_DOCUMENT,
		SELECT_PHOTO,
		PREVIEW_IMAGE,
		EDIT_FIELDS,
		EDIT_FIELDS_ON_ITEM_TYPE_CHANGE,
		SHOW_ITEM_DETAILS,
		SUBMIT_DOCUMENT,
		EDIT_FIELDS_VALIDATION,
	    SERVER_TYPE,
	    SHOW_PENDING,
	    SHOW_HELPSCREEN
	}

	/// The serverType enum is used by application to know which type of server the user is registered with.
	public enum serverType {
		KFS,
		KTA,
		KTA_AZURE
	}
	
	
    public static String AppStatsExportServerUrl = null;

	public Globals() {

	}

	public Globals(Context mContext) {
		//this.mContext = mContext;
	}

	
    //! The factory method.
	public static  Globals getInstance() {
		if (pSelf == null) {
			pSelf = new Globals();
		}

		return pSelf;
	}

	public Handler gItemActivityHandler = null;

	public static RequestCode getRequestCode(int status) {
		return RequestCode.values()[status];
	    //here return the appropriate enum constant
	}
	
	/// Get the server type string based on typeindex from serverType enum.
	
	/** Convert enum value to string
	 * 
	 * @param typeindex 
	 * @return Server type String from enum
	 */
	public static String getServerTypeName(int typeindex) {
		return (serverType.values()[typeindex].name());
	}
	
	/// Get the server type List.
	/**
	 * 
	 * @param context
	 * @return List of servertype strings from array.
	 */
	public static  List<String> getServerTypeNames(Context context){
        String[] str = null;
        str = context.getResources().getStringArray(R.array.servertype_arrays);
        List<String> serverList = Arrays.asList(str); 
        return serverList;
    }
	
	/// Get the server type index based on serverType enum.
	
	/**
	 * 
	 * @param type
	 * @return Index value from selected serverType enum
	 */
	public static int getServerTypeValue(String type) {
		int indexInEnum = -1;
		serverType[] typeArray = serverType.values();
		for(int i=0; i<typeArray.length; i++){
			if(typeArray[i].name().equals(type)) {
				indexInEnum = i;
				break;
			}
		}
		return indexInEnum;
	}
	
	// Get the status to display the offline alert.
	/***
	 * 
	 * @return boolean
	 */
	
	public static boolean isRequiredOfflineAlert(){
		return gAppModeStatus !=  AppModeStatus.FORCE_OFFLINEMODE && gAppLoginStatus != AppLoginStatus.LOGIN_ONLINE;
	}
}
