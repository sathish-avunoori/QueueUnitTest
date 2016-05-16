// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.kofax.kmc.klo.logistics.KfsSessionStateEvent;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.AppStatsManager;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.Initializer;
import com.kofax.mobilecapture.KMCApplication;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.ServerManager;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.dbentities.UserInformationEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.CustomUrlUtils;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.AppLoginStatus;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.Globals.RequestCode;
import com.kofax.mobilecapture.utilities.Globals.serverType;
import com.kofax.mobilecapture.utilities.UtilityRoutines;
import com.kofax.processqueue.ImageProcessingQueueManager;
import com.kofax.processqueue.exceptions.QueueException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Splash screen activity
 * 
 */
public class Main extends Activity {
	// - private constants
	private static final String CUSTOM_APP_NAME = "kmc://?";
	private final String TAG = Main.class.getSimpleName();
	private final int SPLASH_SCREEN_TIMEOUT = 2000;
	// - Private data.
	/* SDK objects */
	/* Application objects */
	private UtilityRoutines mUtilRoutines = null;
	private PrefManager mPrefUtils = null;
	private ServerManager mServerMgr = null;
	private DiskManager mDiskMgr = null;
	private CustomUrlUtils mCustomUrlUtils= null;
	private DocumentManager mDocMgr = null;
	private DatabaseManager mDBManager = null;
	private AppStatsManager mAppStatsManager = null;

	/* Standard variables */
	private Handler mMessageHandler = null;
	private Handler threadHandler = null;
	private CustomDialog mCustomDialog = null;

	private Uri mCustomURI = null;
	private boolean kmcAlreadyRunningAsRootTask = false; // KMC already running
	private boolean kmcAlreadyRunningAsRootTaskTopHome = false; // KMC already running; Home screen at Top
	//private boolean settingSameCredentials = true;

	private boolean doReLogin = false;
	private boolean isRequiredLogin = false;
	private boolean isCustomUrlFlagRequired = false;
	private final int APPLICATION_PERMISSIONS_REQUEST = 1;


	// - public methods
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Enter :: oncreate Main");
		mUtilRoutines = UtilityRoutines.getInstance();

		if(mUtilRoutines.checkKMCLicense(this)){
		//the entry point is Main for the app. In some devices (HTC) when device is shutdown, the app start from different activity. 
		Initializer.getInstance().setEntryPointMain();
		initObjects();
		
		checkForLowendDevice();
		mUtilRoutines.checkApplicationDependencies(this);
		setupHandler();

			ImageProcessingQueueManager processQueueManager = ImageProcessingQueueManager.getInstance();
			Log.d("=== main =====","=== Process Queue Manager instantiated ===");
			try {
				processQueueManager.start(null);
			} catch (QueueException e) {
				e.printStackTrace();
			}

			threadHandler = new Handler();
			mPrefUtils.init(getApplicationContext());///IMP call to initialize sharedPreferences with applicationContext. Do not remove this!!!!

			kmcAlreadyRunningAsRootTask = false;
			kmcAlreadyRunningAsRootTaskTopHome = false;

			mAppStatsManager = AppStatsManager.getInstance(this);
			
			mDiskMgr.cleanupPartiallyProcessedImages();

			identifyCurrentTopActivity();

			mCustomURI = getIntent().getData();
			// Check if custom url is null or empty URL with only application name, if so, launch application normally				
			if ((null == mCustomURI)) {
				mCustomUrlUtils.setUsingCustomUrl(false);
				// Verifying the preferences of KMC 1.2 and importing them into KMC 2.1
				if(mPrefUtils.isPreferenceEmpty()) {
					mPrefUtils.setDefaultsharedPreference();
				}

				if(mPrefUtils.isUsingKofax()){
					String oldUname = mPrefUtils.sharedPref.getString(
							mPrefUtils.KEY_KFX_UNAME, mPrefUtils.DEF_KFX_UNAME);
					String oldPwd = mPrefUtils.sharedPref.getString(
							mPrefUtils.KEY_KFX_PASSWORD, mPrefUtils.DEF_KFX_PASSWORD);

					int unameNameCount = oldUname.compareToIgnoreCase(mPrefUtils.DEF_KFX_UNAME);
					int pwdNameCount = oldPwd.compareToIgnoreCase(mPrefUtils.DEF_KFX_PASSWORD);
					if(unameNameCount != 0 || pwdNameCount != 0){
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_UNAME,mPrefUtils.DEF_KFX_UNAME);
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_PASSWORD, mPrefUtils.DEF_KFX_PASSWORD);
					}
				}

				mPrefUtils.updatePreferencesFrom2o1();
				mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_APP_LAUNCHED_FROM_URL, false);
				if(!kmcAlreadyRunningAsRootTaskTopHome) {
					// splash_screen
					setContentView(R.layout.splash_screen);
					// launch home screen after 2 seconds of splash activity
					threadHandler.postDelayed(mRunnable, SPLASH_SCREEN_TIMEOUT);
				} 
				else {	//close main screen and continue with already running application
					mCustomUrlUtils.setClosingApp(false);
					finish();
				}
			}
			else {
				mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_APP_LAUNCHED_FROM_URL, true);
				if(Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES){
					
					mCustomDialog
					.show_popup_dialog(Main.this,
							AlertType.ERROR_ALERT,
							getResources().getString(R.string.error_lbl),
							getResources().getString(R.string.progress_msg_session_already_has_active_case_open),
							null, null,
							Messages.MESSAGE_DIALOG_ERROR,
							mMessageHandler,
							false);
				}else{
					mCustomUrlUtils.resetCustomUrlFlags();
				handleCustomUrl();
			}
			}
		} else {
			Log.e(TAG, "Invalid KMC License");
			mUtilRoutines
			.showToast(this, getResources().getString(R.string.toast_use_valid_license));
			setContentView(R.layout.splash_screen);
		}
	}

	Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			launchAppMainFlow();
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(Globals.RequestCode.values().length > requestCode){
			Globals.RequestCode myRequestCode = Globals.getRequestCode(requestCode);
			switch (myRequestCode) {
			case CAPTURE_DOCUMENT:
				if(!mCustomUrlUtils.isUsingCustomUrl()) {
					return;
				}
				if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					if(data != null &&  data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
						offlineLogoutToLogIn();					
						return;
					}
					if (data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) > 0) {
						mCustomUrlUtils.setUsingCustomUrl(false);
						showItemDetailsScreen();					
					}
				} else if (resultCode == Globals.ResultState.RESULT_CANCELED.ordinal()) {
					mCustomUrlUtils.setClosingApp(false);
					//When use cancels on capture screen and if no images are captured, then delete the added item record from database
					if(data != null && 
							data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) <= 0) {
						mDBManager.deleteItemWithId(getApplicationContext(), mDBManager.getItemEntity().getItemId());
						
						//if home screen is already displayed, do not close application from main.
						if(kmcAlreadyRunningAsRootTaskTopHome) {
							bringAppToForeground();							
						}
						else {
							showHomeScreen();						
						}
					}
					else {
						showHomeScreen();
					}
					finish();
				}
				break;
			case EDIT_FIELDS:
				if(!mCustomUrlUtils.isUsingCustomUrl()) {
					return;
				}
				if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					if(mCustomUrlUtils.isAutoLaunchShowDocumentInfoFirst()) {
						//if application is launched using custom url and if do_field_update_first is false, 
						//then launch capture activity once the fields are saved on edit fields screen.
						openCaptureView();
					}
					else {
						showSubmitScreen();
					}
				}
				else if (resultCode == Globals.ResultState.RESULT_CANCELED.ordinal()) {
					/*
					 * when new item is being created(edit-fields-screen not
					 * launched by selecting option present on this screen).
					 */
					if(mCustomUrlUtils.isAutoLaunchShowDocumentInfoFieldPresent()) {
						if(mCustomUrlUtils.isAutoLaunchShowDocumentInfoFirst()) {
							//if application is launched using custom url and if do_field_update_first is false, 
							//then launch capture activity once user comes back from edit fields screen.
							openCaptureView();
						}
						else {
						 
						//show home screen on canceling from edit-fields screen
						showHomeScreen();
						}
						mCustomUrlUtils.setClosingApp(false);
					}
				}
				break;
			case SUBMIT_DOCUMENT:
				
				showHomeScreen();
				mCustomUrlUtils.setClosingApp(false);
				break;
			case SHOW_ITEM_DETAILS:		
				if(!kmcAlreadyRunningAsRootTaskTopHome) {					
					showHomeScreen();
				}else{
					bringAppToForeground();
				}
				mCustomUrlUtils.setClosingApp(false);
				finish();
				break;
			default:
				break;
			}
		}
	}

	//Before submit document check all required fields are not null,if it is null display Editinfo screen
	/*	private void proceedToSubmit(){
		boolean isRequiredEditinfoScreen = false;
		//		boolean result = mDiskMgrObj.isDocumentSerialized(mDocMgrObj.getOpenedDoctName());
		if (mDBManager.isDocumentSerializedInDB(mDBManager.getItemEntity())) {
			Document DocumentObj = (Document)mDiskMgr.byteArrayToDocument(mDBManager.getItemEntity().getItemSerializedData());
			isRequiredEditinfoScreen = mDocMgr.validateDocumentFields(DocumentObj);
			if(!isRequiredEditinfoScreen){
				showSubmitScreen();
			}
		}
		else{
			isRequiredEditinfoScreen = true;
		}
		if(isRequiredEditinfoScreen){
			Log.e(TAG, "Launching EditFieldsActivity from 3");
			Intent i = new Intent(this,
					EditFieldsActivity.class);
			i.putExtra(Constants.STR_IS_NEW_ITEM, true);
			i.putExtra(Constants.STR_VALIDATION, true);
			startActivityForResult(
					i,
					Globals.RequestCode.EDIT_FIELDS_VALIDATION
					.ordinal());
		}
	}
	 */
	
	private void showItemDetailsScreen(){
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
		 Intent i = new Intent(this, ItemDetailsActivity.class);
			 i.putExtra(Constants.STR_IS_NEW_ITEM, false);	
		    ItemEntity itemEntity =  mDBManager.getItemEntity();

	        Log.i(TAG,
	                "Filename ====>"
	                        + itemEntity.getFieldName());
	        i.putExtra(Constants.STR_ITEM_NAME,
	                itemEntity.getItemName()); // send the complete path of selected item 
	        i.putExtra(Constants.STR_IS_FROM_PENDING, false);
	        i.putExtra(Constants.STR_ITEM_TYPE,
	                itemEntity.getItemTypeName());
	        startActivityForResult(i, RequestCode.SHOW_ITEM_DETAILS.ordinal());	
	} 
	
	private void showSubmitScreen(){
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
		Intent submit_intent = new Intent(this,
				SubmitDocument.class);
		submit_intent.putExtra(Constants.STR_IS_NEW_ITEM, true);
		startActivityForResult(submit_intent,
				Globals.RequestCode.SUBMIT_DOCUMENT.ordinal());
	}

	private void offlineLogoutToLogIn(){	
		if(kmcAlreadyRunningAsRootTaskTopHome) {
			mCustomUrlUtils.setClosingApp(false);	
			bringAppToForeground();
		}else{
			showHomeScreen();
		}
		sendBroadcast(Constants.CUSTOM_INTENT_OFFLINE_LOGOUT_TO_LOGIN);
		finish();
	}
	

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(TAG, "Enter:: onDestroy");
		//Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = false;
		if(threadHandler != null) {
			threadHandler.removeCallbacks(mRunnable);
			threadHandler = null;
			mRunnable = null;
		}
		if (mCustomDialog != null) {
			mCustomDialog.finish();
			mCustomDialog = null;
		}

		mMessageHandler = null;

		if(mCustomUrlUtils != null) {
			if(mCustomUrlUtils.isClosingApp() == true) {
				Log.e(TAG, "Closing the app, so cleaning all the parameters......");
				mCustomUrlUtils.cleanup();
				mCustomUrlUtils = null;

				ImageProcessQueueManager mProcessQueueMgr = ImageProcessQueueManager.getInstance(this);
				if(mProcessQueueMgr.cleanup() == true) {
					if(mServerMgr != null) {
						mServerMgr.cleanup();
						mServerMgr = null;
					}
					if (mDiskMgr != null) {
						mDiskMgr.cleanup();
						mDiskMgr = null;
					}

					if (mUtilRoutines != null) {
						mUtilRoutines.cleanup();
						mUtilRoutines = null;
					}

					if (mDocMgr != null) {
						mDocMgr.cleanup();
						mDocMgr = null;
					}

					if(mDBManager != null) {
						mDBManager.cleanup();
						mDBManager = null;
					}
					
		            if (mAppStatsManager != null) {
		                mAppStatsManager.deinitAppStatistics();
		            }
				}
			}
		}
		Log.e(TAG, "Cleanup complete");
		mPrefUtils = null;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	int mRetry = 0;
	private Handler mSessionStateHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			KfsSessionStateEvent arg0 = (KfsSessionStateEvent) msg.obj;
			Log.i(TAG, "arg0.getSessionState() ::: " + arg0.getSessionState());
			if(mCustomDialog == null){
				mCustomDialog = CustomDialog.getInstance();
			}
			if(mPrefUtils == null){
				mPrefUtils = PrefManager.getInstance();
			}
			if(mUtilRoutines == null){
				mUtilRoutines = UtilityRoutines.getInstance();
			}
			if(mCustomUrlUtils == null){
				mCustomUrlUtils = CustomUrlUtils.getInstance();
			}
			if(mDocMgr ==  null){
				mDocMgr = DocumentManager.getInstance(getApplicationContext());
			}
			if(mDBManager == null){
				mDBManager = DatabaseManager.getInstance();
			}
			switch (arg0.getSessionState()) {
			case SESSION_UNREGISTERED:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					if( mPrefUtils.getCurrentServerType() != null && (mPrefUtils.getCurrentServerType().equals(serverType.KFS.name())) && (mRetry == 1)) {
						String errorMsg = null;
						try {
							mRetry++;
							mServerMgr.registerDevice(mSessionStateHandler, true);
						} catch (KmcRuntimeException e) {
							errorMsg = e.getMessage();
							e.printStackTrace();
						} catch (KmcException e) {
							errorMsg = e.getMessage();
							e.printStackTrace();
						}
						if(errorMsg != null) {
							mRetry = 0;
							mCustomDialog.show_popup_dialog(Main.this, AlertType.ERROR_ALERT,
									getResources().getString(R.string.error_lbl), errorMsg,
									null, null,
									Messages.MESSAGE_DIALOG_ERROR, mMessageHandler,
									false);
							mCustomDialog.closeProgressDialog();
						}
					}
					else {
						mRetry = 0;
						mCustomDialog.show_popup_dialog(Main.this, AlertType.ERROR_ALERT,
								getResources().getString(R.string.error_lbl), arg0.getErrorInfo().getErrMsg(),
								null, null,
								Messages.MESSAGE_DIALOG_ERROR, mMessageHandler,
								false);
						mCustomDialog.closeProgressDialog();
					}
				}
				break;
			case SESSION_REGISTERING:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					mUtilRoutines.showToast(Main.this, getResources().getString(R.string.error_msg_while_registering)
							+ arg0.getErrorInfo().name());
					mCustomDialog.closeProgressDialog();
				}
				break;
			case SESSION_REGISTERED:
				try {
					if (arg0.getErrorInfo() == ErrorInfo.KMC_LO_USER_LOGIN_ERROR) {
						mCustomDialog.closeProgressDialog();
						mCustomDialog
						.show_popup_dialog(Main.this,
								AlertType.ERROR_ALERT,
								getResources().getString(R.string.lbl_login_error),
								arg0.getErrorInfo().getErrDesc(),
								null, null,
								Messages.MESSAGE_DIALOG_ERROR,
								mMessageHandler,
								false);
					} else {
						if (doReLogin) {
							doReLogin = false;
							//reset all common required variables on logout to initialize for new user
							resetApplicationState();
							mCustomDialog
							.showProgressDialog(Main.this, getResources().getString(R.string.progress_msg_logging_in),false);
							isRequiredLogin = true;
							mRetry = 1;
							mServerMgr.registerDevice(mSessionStateHandler, false);
						} else {
							mRetry = 0;
							if(isRequiredLogin){
								mCustomDialog
								.showProgressDialog(Main.this, getResources().getString(R.string.progress_msg_logging_in),false);

								if(mPrefUtils.isUsingAnonymous()){
									mServerMgr.anonymousLogin(mSessionStateHandler);
								}else{
									mServerMgr.login(mSessionStateHandler);
								}
								
							}
						}
					}
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
					mUtilRoutines.showToast(Main.this, e.getMessage());
					mCustomDialog.closeProgressDialog();
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
					mUtilRoutines.showToast(Main.this, e.getErrorInfo().name());
					mCustomDialog.closeProgressDialog();
				} catch (KmcException e) {
					e.printStackTrace();
					mUtilRoutines.showToast(Main.this, e.getErrorInfo().name());
					mCustomDialog.closeProgressDialog();
				}
				break;
			case SESSION_LOGGING_IN:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					mUtilRoutines.showToast(Main.this, getResources().getString(R.string.error_msg_while_logging_in)
							+ arg0.getErrorInfo().name());
					mCustomDialog.closeProgressDialog();
					//if home screen is already disaplyed, do not close application from main.
					if(kmcAlreadyRunningAsRootTaskTopHome) {
						bringAppToForeground();
						mCustomUrlUtils.setClosingApp(false);
					}
					else {
						mCustomUrlUtils.setClosingApp(true);
					}
					finish();
				}
				break;
			case SESSION_LOGGED_IN:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_ONLINE;
					mCustomDialog.closeProgressDialog();
					mCustomDialog
					.show_popup_dialog(Main.this,
							AlertType.ERROR_ALERT,
							getResources().getString(R.string.lbl_login_error),
							arg0.getErrorInfo().getErrDesc(),
							null, null,
							Messages.MESSAGE_DIALOG_ERROR,
							mMessageHandler,
							false);
				} 
				else {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_APP_LAST_lOGGED_SERVER_TYPE, mPrefUtils.getCurrentServerType());
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_APP_LAST_lOGGED_HOSTNAME, mPrefUtils.getCurrentHostname());

					String user = null;
					if(mPrefUtils.isUsingAnonymous()){
						user = Constants.ANONYMOUS_LOGIN_ID;
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_UNAME, Constants.ANONYMOUS_LOGIN_ID);
					}else{
						user = mPrefUtils.getCurrentUser();
					}
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, user);

					Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_ONLINE;
					if(!mPrefUtils.isUsingKofax()){
						String url =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_URL, mPrefUtils.DEF_USR_URL);
						if(null != url && url.length() > 0){
							mUtilRoutines.updateURLDetails(url);
						}
					}
					mRetry = 0;
					isRequiredLogin = false;
					mCustomDialog.closeProgressDialog();
					//get list of document-type names from server
					mDocMgr.downloadDocTypeNamesList();
					mUtilRoutines.updateUserInformationList(Main.this,mDBManager,mPrefUtils,mDocMgr);					
					mDBManager.updateAllItemsToOnline(Main.this,							
							mPrefUtils.getCurrentServerType(),mPrefUtils.getCurrentHostname(),mUtilRoutines.getUser());
					checkCustomUrlValidations(true);					
					break;
				}
			case SESSION_DOCUMENT_TYPE_READY:
				Log.i(TAG, "DocumentType object is ready!!!!!!!!!!!!!!!!!!!!!");
				break;
			case SESSION_LOGGING_OUT:
				mCustomDialog.closeProgressDialog();
				mRetry = 0;
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					mCustomDialog
					.show_popup_dialog(Main.this,
							AlertType.ERROR_ALERT,
							getResources().getString(R.string.lbl_login_error),
							arg0.getErrorInfo().getErrDesc(),
							null, null,
							Messages.MESSAGE_DIALOG_ERROR,
							mMessageHandler,
							false);	
				}
				Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_ONLINE;
				Log.i(TAG, "Logged out!");
				break;
			default:
				break;
			}
			return true;
		}
	});
	
	@SuppressWarnings("unchecked")
	private void updateDocTypeList(){
		List<UserInformationEntity> list = mDBManager.getUserInformationFromDetails(this,
				mUtilRoutines.getUser(),
				mPrefUtils.getCurrentHostname(),
				mPrefUtils.getCurrentServerType());
			if(list != null && list.size()>0 ){
				UserInformationEntity entity = list.get(0);
				String docTypeList = entity.getDocumentTypes();
				String[] separated = docTypeList.split(Constants.KMC_STRING_SPLIT_SEPERATOR);
				//TODO::helpkofax changes need
				ArrayList<String> doclist =  new ArrayList<String>(Arrays.asList(separated));  
				if(doclist != null && doclist.size() > 0){
					ArrayList<String> helpKofaxList = new ArrayList<String>();
					ArrayList<String> nonHelpKofaxList = new ArrayList<String>();
					ArrayList<String> allDocList = new ArrayList<String>();
					if(mPrefUtils.isUsingKofax()){
					for(int i = 0;i< doclist.size();i++){
						if(doclist.get(i).contains(Constants.STR_ASSIST)){
							helpKofaxList.add(doclist.get(i));
						}
						else{
							nonHelpKofaxList.add(doclist.get(i));
						}
					}
					if(!nonHelpKofaxList.isEmpty()){
						allDocList = (ArrayList<String>) nonHelpKofaxList.clone();
					}
					if(!helpKofaxList.isEmpty()){
						for(String docName : helpKofaxList){
							allDocList.add(docName);
						}
					}
					}else{
						allDocList = doclist;	
						nonHelpKofaxList = allDocList;											
					}
					mDocMgr.setNonHelpDocumentNamesList(nonHelpKofaxList);
					mDocMgr.setHelpDocumentNamesList(helpKofaxList);
					mDocMgr.setDocTypeNamesList(allDocList);									
				}							
			}
	}

	private void checkCustomUrlValidations(boolean isLoginRequired){
					if(isLoginRequired){
					sendBroadcast(Constants.CUSTOM_INTENT_LOGIN_UPDATED);
					}
					
					if(mCustomUrlUtils.isUsingCustomUrl()) {
						//if url contains document type, initiate download of document type object to check if document of that type exists under currently logged in user.
						if(mCustomUrlUtils.getAutoLaunchDocumentName() != null) {
							if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){								
								updateDocTypeList();
							}
							
							int docIndex = validateDocumentTypeFromUrl();
							if(docIndex != -1) {
								//create directory(if not present already) with the name of logged in user.
								mDiskMgr.createUserDirectory();

								mDocMgr.setCurrentDocTypeIndex(docIndex);
								String uniqueFileName = mUtilRoutines.createUniqueItemName();
								mDocMgr.setOpenedDocName(uniqueFileName);

								insertIteminDB(mDocMgr);

								if(mCustomUrlUtils.isAutoLaunchShowDocumentInfoFirst()) {
									//launch edit fields screen
									showEditFieldsScreen();
								}
								else {
									openCaptureView();
								}
							}
							else {
								// if the document type in invalid, display error message and exit the application.
								mCustomDialog.show_popup_dialog(Main.this,
										AlertType.ERROR_ALERT,
										getResources().getString(R.string.error_lbl_argument),
										getResources().getString(R.string.progress_msg_url_contains_invalid_document_types),
										null, null,
										Messages.MESSAGE_DIALOG_ERROR,
										mMessageHandler,
										false);
					return;
							}
						}
						else {
							//create directory(if not present already) with the name of logged in user.
							mDiskMgr.createUserDirectory();
							Log.d(TAG, "Launching home screen from here.........................");
							launchAppMainFlow();
						}
					}
					else {
						//create directory(if not present already) with the name of logged in user.
						mDiskMgr.createUserDirectory();
					}
				}

	private void initObjects() {
		mServerMgr = ServerManager.getInstance();
		mDiskMgr = DiskManager.getInstance(this);
		mCustomDialog = CustomDialog.getInstance();
		mDBManager = DatabaseManager.getInstance();
		mPrefUtils = PrefManager.getInstance();
		mCustomUrlUtils = CustomUrlUtils.getInstance();
		mDocMgr = DocumentManager.getInstance(this);
	}

	private void identifyCurrentTopActivity() {
		ActivityManager manager = (ActivityManager) getSystemService( ACTIVITY_SERVICE );
		List<RunningTaskInfo> tasks =  manager.getRunningTasks(Integer.MAX_VALUE);
		for (RunningTaskInfo taskInfo : tasks) {
			Log.i(TAG, "RunningTaskInfo; taskInfo.baseActivity=" + taskInfo.baseActivity.getClassName() + ", taskInfo.topActivity=" +
					taskInfo.topActivity.getClassName() + " -- isTaskRoot() :: " + isTaskRoot());
			if(taskInfo.baseActivity.getClassName().contains("com.kofax.mobilecapture") && !isTaskRoot()){
				kmcAlreadyRunningAsRootTask = true;
				kmcAlreadyRunningAsRootTaskTopHome = ((KMCApplication) getApplicationContext()).isOnHomeScreen();
				break;
			}
		}
	}

	private void checkForLowendDevice() {
		long appMemory = Runtime.getRuntime().maxMemory()/Constants.BYTES_TO_MB_VALUE;

		//If App allocated size is lessThan or equal to 128MB then consider it as a lowerEnd device.
		if(appMemory <= Constants.MEMORY_OR_LOWER_END_DEVICES){
			//disable background image processing if host device is lowend
			Constants.BACKGROUND_IMAGE_PROCESSING = false;
		}

		Log.d(TAG, "Is Lowerend Device:: "+!Constants.BACKGROUND_IMAGE_PROCESSING);
	}

	private void handleCustomUrl() {
		if(mCustomURI.toString().replace("|", "%7C") == CUSTOM_APP_NAME) {
			// splash_screen
			setContentView(R.layout.splash_screen);
			// launch home screen after 2 seconds of splash activity
			threadHandler.postDelayed(mRunnable, SPLASH_SCREEN_TIMEOUT);
		}
		else{
			ArrayList<String> permissionList = new ArrayList<String>();

			String[] permissions = null;
			if(ContextCompat.checkSelfPermission(this,
					Manifest.permission.CAMERA)
					!= PackageManager.PERMISSION_GRANTED){
				permissionList.add(Manifest.permission.CAMERA);
			}
			if(ContextCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED){
				permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
			}
			if(ContextCompat.checkSelfPermission(this,
					Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED){
				permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}

			if(!permissionList.isEmpty()){
				permissions = permissionList.toArray(new String[permissionList.size()]);
				// No explanation needed, we can request the permission.
				if(null != permissions) {
					ActivityCompat.requestPermissions(this,
							permissions,
							APPLICATION_PERMISSIONS_REQUEST);
				}
			}else{				
				handleCustomUrlWithPermissionsCheck();				
			}
		}
	}

	private void handleCustomUrlWithPermissionsCheck(){

			//if customUrl is not empty
			mCustomUrlUtils.setChangeInCredentials(false);
			String errMsg = null;
			String path = mCustomURI.toString().replace("|", "%7C");
			try {
				URI myCustomURL = new URI(path);

				Log.i(TAG, "myCustomURL ::: " + myCustomURL);

				List<NameValuePair> queryStringParams = URLEncodedUtils.parse(myCustomURL, "utf-8");
				myCustomURL = null;
				Log.i(TAG, "queryStringParams ::: " + queryStringParams);

				if(0 == queryStringParams.size()) {
					mCustomDialog
							.show_popup_dialog(Main.this,
									AlertType.ERROR_ALERT,
									getResources().getString(R.string.error_lbl),
									getResources().getString(R.string.error_msg_provided_url_has_errors),
									null, null,
									Messages.MESSAGE_DIALOG_ERROR,
									mMessageHandler,
									false);
				}
				else {
					errMsg = mCustomUrlUtils.parseUrlAndFindErrors(this, queryStringParams);
					if (StringUtils.isEmpty(errMsg)) {
						errMsg = mCustomUrlUtils.parseUrlAndUpdatePreferences(this, queryStringParams);
						if (StringUtils.isEmpty(errMsg)) {
							if(mCustomUrlUtils.isChangeInCredentials() == true) {
/*							if(mPrefUtils.isUsingKofax()) {
								settingSameCredentials = mPrefUtils.getPrefValueString(mPrefUtils.KEY_KFX_URL).equals(mCustomUrlUtils.getKfsServerUrl()) &&
										mPrefUtils.getPrefValueString(mPrefUtils.KEY_KFX_UNAME).equalsIgnoreCase(mCustomUrlUtils.getUsername()) &&
										mPrefUtils.getPrefValueString(mPrefUtils.KEY_KFX_PASSWORD).equals(mCustomUrlUtils.getPassword());
							}
							else {
								settingSameCredentials = mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_URL).equals(mCustomUrlUtils.getKfsServerUrl()) &&
										mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_UNAME).equalsIgnoreCase(mCustomUrlUtils.getUsername()) &&
										mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PASSWORD).equals(mCustomUrlUtils.getPassword());
							}
*/
							}
							errMsg = detectMultipleKMCTasksCollisions();

							mCustomUrlUtils.setUsingCustomUrl(true);
							Log.i(TAG, "isChageInEmail =========================== " + mCustomUrlUtils.isChangeInEmail());
							// Check for Invalid case : case field without the case name
							if((true == mCustomUrlUtils.isAutoLaunchDocumentHasFields()) && (null == mCustomUrlUtils.getAutoLaunchDocumentName()))	{
								mCustomDialog
										.show_popup_dialog(Main.this,
												AlertType.ERROR_ALERT,
												getResources().getString(R.string.error_lbl),
												getResources().getString(R.string.error_msg_provided_url_has_errors),
												null, null,
												Messages.MESSAGE_DIALOG_ERROR,
												mMessageHandler,
												false);
							}
							else {
								// if there are no parameters to change login credentials (server-url/username/password/email), then simply bring application to foreground
								if (!mCustomUrlUtils.isChangeInCredentials() && (mCustomUrlUtils.isChangeInEmail() == false)) {
									//if application is already running in background
									if(kmcAlreadyRunningAsRootTask && (mServerMgr.isLoggedIn() || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE)) {
										if(mCustomUrlUtils.getAutoLaunchDocumentName() != null) {
											if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
												updateDocTypeList();
											}
											int docIndex = validateDocumentTypeFromUrl();
											if(docIndex != -1) {
												mDocMgr.setCurrentDocTypeIndex(docIndex);
												String uniqueFileName = mUtilRoutines.createUniqueItemName();
												mDocMgr.setOpenedDocName(uniqueFileName);

												insertIteminDB(mDocMgr);

												//if documentType is valid, check if capture screen should be launched first or edit-fields screen.
												if(mCustomUrlUtils.isAutoLaunchShowDocumentInfoFirst()) {
													//launch edit fields screen
													showEditFieldsScreen();
												}
												else {
													openCaptureView();
												}
											}
											else {
												// if the document type in invalid, display error message and exit the application.
												mCustomDialog.show_popup_dialog(Main.this,
														AlertType.ERROR_ALERT,
														getResources().getString(R.string.error_lbl_argument),
														getResources().getString(R.string.progress_msg_url_contains_invalid_document_types),
														null, null,
														Messages.MESSAGE_DIALOG_ERROR,
														mMessageHandler,
														false);
												mCustomUrlUtils.reset_login_pref();
												return;
											}
										}
										else {
											bringAppToForeground();
										}
									}else if(kmcAlreadyRunningAsRootTask && !(mServerMgr.isLoggedIn() || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE) && mCustomUrlUtils.getAutoLaunchDocumentName() == null){
										initiate_silent_login();
									}
									else {
										if(mCustomUrlUtils.getAutoLaunchDocumentName() == null) {
											// splash_screen
											setContentView(R.layout.splash_screen);
											// launch home screen after 2 seconds of splash activity
											threadHandler.postDelayed(mRunnable, SPLASH_SCREEN_TIMEOUT);
										}
										else {
										/*mCustomDialog.show_popup_dialog(Main.this,
													AlertType.ERROR_ALERT,
													getResources().getString(R.string.error_lbl_user_not_loggedin),
													getResources().getString(R.string.error_msg_doc_error),
													null, null,
													Messages.MESSAGE_DIALOG_ERROR,
													mMessageHandler,
													false);*/
											// Do silent login, and once login is completed, check if capture screen should be launched first or edit-fields screen based on parameters passed.
											initiate_silent_login();
										}
									}
								}
								else {
									//logout from existing user and silent login with new one
									show_relogin_confirmation ();
								}
							}
						}
					}
				}
			}
			catch (IllegalArgumentException e)	{
				errMsg = getResources().getString(R.string.error_msg_provided_url_has_errors);
				Log.i(TAG, getResources().getString(R.string.error_msg_provided_url_has_errors));
				e.printStackTrace();
			}
			catch (URISyntaxException e) {
				errMsg = getResources().getString(R.string.error_msg_provided_url_has_errors);
				e.printStackTrace();
			}
			catch (Exception e) {
				e.printStackTrace();
				if (StringUtils.isEmpty(errMsg)) {
					errMsg = getResources().getString(R.string.error_msg_error_parsing_custom_url);
					Log.i(TAG, getResources().getString(R.string.error_msg_error_parsing_custom_url) + e.getMessage());
				}
			}
			if (!StringUtils.isEmpty(errMsg)) {
				mCustomDialog
						.show_popup_dialog(Main.this,
								AlertType.ERROR_ALERT,
								getResources().getString(R.string.error_lbl),
								errMsg,
								null, null,
								Messages.MESSAGE_DIALOG_ERROR,
								mMessageHandler,
								false);
				mCustomUrlUtils.reset_login_pref();
			}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case APPLICATION_PERMISSIONS_REQUEST: {
				if(permissions.length > 0){
					for(int i = 0;i<permissions.length;i++){
						if ( (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_DENIED)
								|| (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_DENIED)
								|| (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_DENIED)	)
						{
							showPermissionDialog();
						}
					}
				}
				handleCustomUrlWithPermissionsCheck();
				break;
			}
			default:
				break;

		}
		return;
	}

	private void showPermissionDialog(){
		if (mCustomDialog != null) {
			mCustomDialog.dismissAlertDialog();
			mCustomDialog.show_popup_dialog(Main.this, AlertType.INFO_ALERT, getResources().getString(R.string.permissions), getResources().getString(R.string.permission_alert), null, null, Messages.MESSAGE_DIALOG_PERMISSION, mMessageHandler,false);
		}


	}

	private void setupHandler() {
		mMessageHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
				switch (whatMessage) {
				case MESSAGE_DIALOG_ERROR:
					if(kmcAlreadyRunningAsRootTask /*|| mServerMgr.isLoggedIn()*/) {
						mCustomUrlUtils.setClosingApp(false);
						// if application is already on home screen, and loging error has occured due to custom-url, inform home screen to clear the list of document types.
						if(((KMCApplication) getApplicationContext()).isOnHomeScreen() && !mServerMgr.isLoggedIn()) {
							sendBroadcast(Constants.CUSTOM_INTENT_LOGIN_ERROR);
						}
					}
					else {
						mCustomUrlUtils.setClosingApp(true);
					}
					finish();
					break;
				case MESSAGE_DIALOG_OFFLINE_ERROR:
					mCustomUrlUtils.setClosingApp(false);
					sendBroadcast(Constants.CUSTOM_INTENT_OFFLINE_LOGIN_ERROR);
					finish();
					break;
				case MESSAGE_DIALOG_RELOGIN_CONFIRMATION:
					if (msg.arg1 == RESULT_OK) {
						if ((mCustomUrlUtils.getUsername() == null) && (null != mCustomUrlUtils.getPassword())) {
							//invalid parameters passed, empty username is not valid, if password argument is present  in url
							Log.d(TAG, "invalid paramters passed, username is null, password not null!");
							mCustomDialog
							.show_popup_dialog(Main.this,
									AlertType.ERROR_ALERT,
									getResources().getString(R.string.error_lbl_argument),
									getResources().getString(R.string.toast_error_required_fields_empty),
									null, null,
									Messages.MESSAGE_DIALOG_ERROR,
									mMessageHandler,
									false);
						}
						else {
							if(mCustomUrlUtils.isChangeInEmail() == true) {
								Log.i(TAG, "Email ID is being updated");
								mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_EMAIL, mCustomUrlUtils.getEmailAddress());
							}
							if (mCustomUrlUtils.isChangeInCredentials() == false) {
								//if already logged in and credentials are same
								if(mServerMgr.isLoggedIn() || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE) {
									if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){								
										updateDocTypeList();
									}
									int docIndex = validateDocumentTypeFromUrl();
									if(docIndex != -1) {
										mDocMgr.setCurrentDocTypeIndex(docIndex);
										String uniqueFileName = mUtilRoutines.createUniqueItemName();
										mDocMgr.setOpenedDocName(uniqueFileName);

										insertIteminDB(mDocMgr);

										//if documentType is valid, check if capture screen should be launched first or edit-fields screen.
										if(mCustomUrlUtils.isAutoLaunchShowDocumentInfoFirst()) {
											//launch edit fields screen
											showEditFieldsScreen();											
										}
										else {
											openCaptureView();
										}
									}
									else {
										// if the document type in invalid, display error message and exit the application.
										mCustomDialog.show_popup_dialog(Main.this,
												AlertType.ERROR_ALERT,
												getResources().getString(R.string.error_lbl_argument),
												getResources().getString(R.string.progress_msg_url_contains_invalid_document_types),
												null, null,
												Messages.MESSAGE_DIALOG_ERROR,
												mMessageHandler,
												false);
										return true;
									}
								}
								else {
									initiate_silent_login();
								}
							}
							else {
								// if some other user is already logged in, log out first, then log in with new credentials
								if(mServerMgr.isLoggedIn()&& mUtilRoutines.checkInternet(getApplicationContext())) {
									Log.d(TAG, "Some user is already logged in.");
									try {
										mCustomDialog.showProgressDialog(Main.this, getResources().getString(R.string.progress_msg_logging_out),false);
										doReLogin = true;
										mCustomUrlUtils.update_login_pref_from_custom_url();
										mServerMgr.logout(mSessionStateHandler);
									} catch (KmcException e) {
										e.printStackTrace();
										mCustomDialog.closeProgressDialog();
										mCustomDialog.show_popup_dialog(Main.this,
												AlertType.ERROR_ALERT,
												getResources().getString(R.string.error_lbl_argument),
												getResources().getString(R.string.progress_msg_url_contains_invalid_document_types),
												null, null,
												Messages.MESSAGE_DIALOG_ERROR,
												mMessageHandler,
												false);
									}
								}else if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
									mCustomUrlUtils.update_login_pref_from_custom_url();
									doOfflineLogout();							
								}
								else {
									//If no DocumentType is included, the KMC app will jump into Home Screen, which performs an automatic login, using persisted login credentials.
									if(!kmcAlreadyRunningAsRootTaskTopHome && mCustomUrlUtils.getAutoLaunchDocumentName() == null) {
										// splash_screen
										setContentView(R.layout.splash_screen);
										isCustomUrlFlagRequired = true;
										// launch home screen after 2 seconds of splash activity
										threadHandler.postDelayed(mRunnable, SPLASH_SCREEN_TIMEOUT);
									}
									else {
										//if a DocumentType is included, then a silent login must occur, using persisted login credentials
										initiate_silent_login();
									}
								}
							}
						}
					}
					else {
						// do nothing
						Log.d("AlertDialog", "Not changing settings");
						mCustomUrlUtils.reset_login_pref();
						if(kmcAlreadyRunningAsRootTask || mServerMgr.isLoggedIn() || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE) {
							mCustomUrlUtils.setClosingApp(false);
						}
						else {
							mCustomUrlUtils.setClosingApp(true);
						}
						finish();
					}
					break;
				default:
					break;
				}
				return true;
			}
		});
	}

	private String detectMultipleKMCTasksCollisions() {
		String errMsg = new String();
		Log.d(TAG, "kmcAlreadyRunningAsRootTask :: " + kmcAlreadyRunningAsRootTask);			
		Log.d(TAG, "kmcAlreadyRunningAsRootTaskTopHome :: " + kmcAlreadyRunningAsRootTaskTopHome);

		if (kmcAlreadyRunningAsRootTask && mServerMgr.isLoggingIn()) {
			errMsg = getResources().getString(R.string.progress_msg_session_currently_logging_in);
		}
		else if (kmcAlreadyRunningAsRootTask && !kmcAlreadyRunningAsRootTaskTopHome) {
			if (!mServerMgr.isLoggedIn()) {
				
				// someone started KMC already, but quickly sent it to the background, before it even had a chance to start logging in
				errMsg = getResources().getString(R.string.progress_msg_session_currently_paused);

			} else {
				errMsg = getResources().getString(R.string.progress_msg_session_already_has_active_case_open);
			}
		}
		else if(mServerMgr.isServerBusy()) {
			errMsg = getResources().getString(R.string.progress_msg_session_currently_busy);			
		}
		return errMsg;
	}


	// Function to show dialog alert, asking user whether to accept change in login credentials and relogin
	private void show_relogin_confirmation () {
		// build message string
		String confirmation_str = new String();

		confirmation_str = mCustomUrlUtils.build_message_string(confirmation_str);
		Log.i(TAG, "Displaying confirmation alert! :::::::::" + confirmation_str);

		mCustomDialog.dismissAlertDialog();
		mCustomDialog
		.show_popup_dialog(Main.this,
				AlertType.CONFIRM_ALERT,
				getResources().getString(R.string.app_name),
				confirmation_str,
				null, null,
				Messages.MESSAGE_DIALOG_RELOGIN_CONFIRMATION,
				mMessageHandler,
				false);
	}

	private void launchAppMainFlow() {
		if(mCustomUrlUtils.isChangeInEmail()) {
			mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_EMAIL, mCustomUrlUtils.getEmailAddress());
		}

		if (mCustomUrlUtils.isChangeInCredentials()) {
			mCustomUrlUtils.update_login_pref_from_custom_url();
		}

		if(!kmcAlreadyRunningAsRootTaskTopHome) {
			if(isCustomUrlFlagRequired && mPrefUtils.isPreferenceEmpty()){
				isCustomUrlFlagRequired = false;
				mCustomUrlUtils.setUsingCustomUrl(true);
			}
			Intent intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
		}else{
			bringAppToForeground();
		}
		// make sure we close the splash screen so the user won't come
		// back when back key is pressed
		mCustomUrlUtils.setClosingApp(false);
		finish();
	}

	private void bringAppToForeground() {
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = false;
		mCustomUrlUtils.setUsingCustomUrl(false);
		Intent intent = new Intent(this, HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		startActivity(intent);
	}

	private void showHomeScreen() {
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = false;
		mCustomUrlUtils.setUsingCustomUrl(false);
		Intent intent = new Intent(this, HomeActivity.class);
		startActivity(intent);
	}

	private void openCaptureView() {
		Constants.IS_HELPKOFAX_FLOW = false;
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
		List<ProcessingParametersEntity> list = mDBManager.getProcessingParametersFromDetails(this,mDBManager.getItemEntity().getItemTypeName(),mDBManager.getUserInformationEntity().getUserInformationId());
		if(null != list && list.size() > 0){
			mDBManager.setProcessingParametersEntity(list.get(0));
		}
		Intent cameraIntent = new Intent(this, Capture.class);

		cameraIntent.putExtra(Constants.STR_IMAGE_COUNT, 0);
		cameraIntent.putExtra(Constants.STR_IS_NEW_ITEM, true);

		startActivityForResult(cameraIntent,
				RequestCode.CAPTURE_DOCUMENT.ordinal());
	}

	private void showEditFieldsScreen(){
		Constants.IS_HELPKOFAX_FLOW = false;
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
		Intent i = new Intent(this, EditFieldsActivity.class);
		i.putExtra(Constants.STR_IS_NEW_ITEM, true);
		startActivityForResult(i,
				Globals.RequestCode.EDIT_FIELDS.ordinal());
	}

	/*
	 * Method to insert a new Item into DB
	 */	
	private void insertIteminDB(DocumentManager mDocMgrObj){
		String user = mUtilRoutines.getUser();
		if(mDBManager == null) {
			mDBManager = DatabaseManager.getInstance();
		}

		ItemEntity item = new ItemEntity();
		item.setItemName(mDocMgrObj.getOpenedDoctName());
		item.setItemTypeName(mDocMgrObj.getDocTypeNamesList().get(mDocMgrObj.getCurrentDocTypeIndex()));
		item.setItemCreatedTimeStamp(new Date());

		item.setServerId(mPrefUtils.getCurrentServerType());
		item.setHostname(mPrefUtils.getCurrentHostname());

		item.setUserId(user);
		item.setFieldName(null);
		if(Globals.gAppLoginStatus ==  AppLoginStatus.LOGIN_ONLINE){
		item.setIsOffline(false);
		}else{
			item.setIsOffline(true);
		}
		mDBManager.insertOrUpdate(getApplicationContext(), item);
		mDBManager.setItemEntity(item);
	}

	private void initiate_silent_login () {
		if (mCustomUrlUtils.isChangeInCredentials()) {
			mCustomUrlUtils.update_login_pref_from_custom_url();
		}
		if(!mUtilRoutines.checkInternet(getApplicationContext())){
			doOfflineLogin();
			return;
		}
		
		mCustomDialog
		.showProgressDialog(this, getResources().getString(R.string.progress_msg_logging_in),false);

		
		try {
			doReLogin = true;
			mRetry = 1;
			mServerMgr.registerDevice(mSessionStateHandler, false);
		} catch (KmcRuntimeException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), e.getErrorInfo().getErrMsg(), Toast.LENGTH_LONG).show();
			mCustomDialog.closeProgressDialog();
			finish();
		} catch (KmcException e) {
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), e.getErrorInfo().getErrMsg(), Toast.LENGTH_LONG).show();
			mCustomDialog.closeProgressDialog();
			finish();
		}
	}

	private void doOfflineLogin(){		
		mCustomDialog.dismissAlertDialog();
		if(checkForOfflineSupport()){
			checkCustomUrlValidations(true);
		}else{
				mCustomUrlUtils.reset_login_pref();

				String lastLoggedUser = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, null);
				if(lastLoggedUser.isEmpty()){
					mCustomDialog.show_popup_dialog(Main.this, AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.app_msg_invalid_only_once_session_msg), null, null, Messages.MESSAGE_DIALOG_OFFLINE_ERROR, mMessageHandler, false);
				}else {
					mCustomDialog.show_popup_dialog(Main.this, AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.app_msg_invalid_session_msg), null, null, Messages.MESSAGE_DIALOG_OFFLINE_ERROR, mMessageHandler, false);
				}
		}

		
	}
	
	private void doOfflineLogout(){		
		mCustomDialog.dismissAlertDialog();
		if(mUtilRoutines.checkInternet(Main.this)){
			Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_OFFLINE;
			initiate_silent_login();
		}else if(checkForOfflineSupport()){	
			Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_OFFLINE;
			sendBroadcast(Constants.CUSTOM_INTENT_OFFLINE_LOGIN);
			checkCustomUrlValidations(false);
		}else{
			mCustomUrlUtils.reset_login_pref();
			mCustomDialog.show_popup_dialog(Main.this, AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.app_msg_invalid_session_msg), null, null, Messages.MESSAGE_DIALOG_OFFLINE_ERROR, mMessageHandler, false);
		}
	}
	
	private boolean checkForOfflineSupport(){
		boolean isOfflineSupport = false;

		if(!mUtilRoutines.isLastLoggedInUser(mPrefUtils)){
			return isOfflineSupport;
		}
		Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_OFFLINE;

		String lastLoggedUserServerType =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_SERVER_TYPE, mPrefUtils.DEF_USR_SERVER_TYPE);
		String lastLoggedUserHostName =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_HOSTNAME, mPrefUtils.DEF_USR_HOSTNAME);
		String lastLoggedUser = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, mPrefUtils.DEF_USR_UNAME);

		List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(this,
				lastLoggedUser,
				lastLoggedUserHostName,lastLoggedUserServerType);
		
		if(list != null && list.size() > 0){
			isOfflineSupport = true;

		}
		return isOfflineSupport;
	}

	private void resetApplicationState() {
		if (mDocMgr != null) {
			mDocMgr.reset();
		}

		if (mDiskMgr != null) {
			mDiskMgr.reset();
		}
	}

	private int validateDocumentTypeFromUrl() {
		int foundIndex = -1;
		if(mDocMgr.getDocTypeNamesList() != null) {
			foundIndex = mDocMgr.findDocumentTypeIndex(mCustomUrlUtils.getAutoLaunchDocumentName()); 
		}
		
		return foundIndex;
	}

	private void sendBroadcast(String message) {
		Intent i = new Intent(message);
		sendBroadcast(i);
	}

}
