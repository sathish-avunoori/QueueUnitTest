// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.kofax.kmc.klo.logistics.KfsSessionStateEvent;
import com.kofax.kmc.klo.logistics.SessionState;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.AppStatsManager;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
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
import com.kofax.mobilecapture.utilities.Globals.AppModeStatus;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.Globals.RequestCode;
import com.kofax.mobilecapture.utilities.Globals.serverType;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class HomeActivity extends FragmentActivity implements NetworkChangedListener{

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes
	public class documentAdapter extends BaseAdapter {
		private Context context;
		private List<String> dlist = null;

		public documentAdapter(Context context, List<String> listinbox) {
			this.context = context;
			this.dlist = listinbox;
		}

		public int getCount() {
			if (dlist != null)
				return dlist.size();
			else
				return 0;
		}

		public Object getItem(int position) {
			if (dlist != null)
				return dlist.get(position);
			else
				return null;
		}

		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		public View getView(int position, View convertView, ViewGroup viewGroup) {

			if (dlist != null) {
				String node = dlist.get(position);
				TextView documentType;
				ImageView categoryIcon = null;

				if (node != null) {
					if (null == convertView) {
						Log.d(TAG, "context :: " + context);

						LayoutInflater inflater = (LayoutInflater) context
								.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						convertView = inflater.inflate(
								R.layout.documenttypelistrow, null);
					}
					documentType = (TextView) convertView
							.findViewById(R.id.doctypetextview);
					categoryIcon = (ImageView) convertView
							.findViewById(R.id.docimgView);

					if ((node != null) && (node.length() > 0)) {
						documentType.setText(node);
						//set type specific icons for below category, for others default icons is used
						if(node.equals(str_business_card)) {
							categoryIcon.setImageDrawable(getResources().getDrawable(R.drawable.business_cards_icon));
						}
						else if(node.equals(str_document)) {
							categoryIcon.setImageDrawable(getResources().getDrawable(R.drawable.document_icon));
						}
						else if(node.equals(str_receipts)) {
							categoryIcon.setImageDrawable(getResources().getDrawable(R.drawable.receipts_icon));
						}
						else if(node.equals(str_gift_card)) {
							categoryIcon.setImageDrawable(getResources().getDrawable(R.drawable.gift_cards_icon));
						}else if(node.equals(str_helpkofax)){
							categoryIcon.setImageDrawable(getResources().getDrawable(R.drawable.help_kofax));
						}
					}
				}
			}
			return convertView;
		}

		private void clear() {
			if (this.dlist != null && dlist.size() > 0) {
				this.dlist = new ArrayList<String>();
				this.dlist = null;
			}
		}
	}

	// - private nested classes (10 lines or less)

	// - public constants
	public static Handler mHomeActivityHandler = null;

	// - private constants
	//These strings need not be localized
	private final String TAG = HomeActivity.class.getSimpleName();

	private final String str_business_card = "Business Cards";
	private final String str_gift_card = "Gift Card Balance";
	private final String str_document = "Document";
	private final String str_receipts = "Receipts";
	private final String str_helpkofax = "Assist Kofax";
	private final String mYogaPadLenovoModel = "Lenovo B8000-H";

	// - Private data.
	/* SDK objects */
	/* Application objects */
	//	private Globals globalObj = null;
	private UtilityRoutines mUtilRoutines = null;
	private DocumentManager mDocMgr;
	private PrefManager mPrefUtils = null;
	private DiskManager mDiskMgr = null;
	private ImageProcessQueueManager mProcessQueueMgr = null;
	private ServerManager mServerMgr = null;
	private DatabaseManager mDBManager = null;
	private CustomUrlUtils mCustomUrlUtils = null;
	private AppStatsManager mAppStatsManager = null;
	/* Standard variables */
	private ActionBarDrawerToggle mDrawerToggle;
	private DrawerLayout mDrawerLayout;
	private ActionBar mActionbar;
	private CustomDialog mCustomDialog;
	private SettingsFragment mSettingsFragment;
	private documentAdapter mdocumentAdapter = null;
	private ListView mListview;
	private List<String> mDoctypelist;
	private TextView mMenucountTextview = null;
	private ImageView mMenucountImageView = null;
	private BroadcastReceiver mReceiver = null;
	private Calendar mDocListRefreshTime = null;

	private Boolean doReLogin = false;
	private Boolean doCloseApp = false;
	private Boolean isRequiredLogin = false;
	private Boolean isLoggedInForRefreshList = false;  // Used for every 24 hours refresh the document list
	private TextView mNoInternetConnectionView = null;
	private final int APPLICATION_PERMISSIONS_REQUEST = 2;
	private boolean isFromServerType = false;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		new DeviceSpecificIssueHandler().checkEntryPoint(this);
		new Initializer().initiateManagersAndServices(getApplicationContext());

		mUtilRoutines = UtilityRoutines.getInstance();
		
		
		// Check if valid license is present before proceeding
		if (!mUtilRoutines.checkKMCLicense(this)) {
			mUtilRoutines
			.showToast(this, getResources().getString(R.string.toast_use_valid_license));
			finish();
		}
		
		initObjects();
		
		mSettingsFragment = (SettingsFragment) getFragmentManager().findFragmentById(R.id.left_drawer);

		mListview = (ListView) findViewById(R.id.doctypeListView);
		mListview.setClickable(true);
		mListview.setOnItemClickListener(onItemClickListener);

		initActionBar();
		initDrawer();
		setupHandler();
		Boolean isAppLaunchedFromCustomUrl =  mPrefUtils.sharedPref.getBoolean(mPrefUtils.KEY_APP_LAUNCHED_FROM_URL, false);

		if(!isAppLaunchedFromCustomUrl){
			CheckDangerousApplicationPermissions();
		}

		if (mUtilRoutines.getAvailableMemorySize() <= Constants.LAUNCH_APP_MEMORY) {

			mCustomDialog.show_popup_dialog(HomeActivity.this,
					AlertType.ERROR_ALERT,
					getResources().getString(R.string.error_lbl),
					getString(R.string.str_launch_time_memory_warning), null,
					null, Messages.MESSAGE_DIALOG_LOW_MEMORY_ERROR,
					mHomeActivityHandler, false);
		}else if(mServerMgr.isLoggedIn()) {
			// get the listview
			Log.d(TAG, "Already logged in, refreshing the list");
			refreshList(mDocMgr.downloadDocTypeNamesList());
			mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
		}
		else {
			boolean status = false;
			status = mUtilRoutines.checkInternet(this);
			if(status){
			Log.d(TAG, "User is not logged in already");
			mdocumentAdapter = new documentAdapter(this, mDoctypelist);
			Log.d(TAG, "DocumentTypeList::onCreateView");
			// setting list adapter
			mListview.setAdapter(mdocumentAdapter);
			Log.i(TAG, "mPrefUtils.isPreferenceEmpty() ==> " + mPrefUtils.isPreferenceEmpty());
			Log.i(TAG, "mCustomUrlUtils.isUsingCustomUrl() ==> " + mCustomUrlUtils.isUsingCustomUrl());
			// if not using custom-url and if preferences are empty, set default preferences
			if ((mPrefUtils.isPreferenceEmpty()) && (!mCustomUrlUtils.isUsingCustomUrl())) {
				mPrefUtils.setDefaultsharedPreference();
			}

			// check if email ID is already registered
			doInitialEmailCheck();
			}else{
				if(checkForOfflineSupport()){
					showOfflineModeSupportAlert();
				}else{
					mUtilRoutines
					.showToast(HomeActivity.this, getResources().getString(R.string.toast_no_network_connectivity)+", "+getResources().getString(R.string.toast_app_work_offline));				
				}
			}						
		}
		registerBroadcastReceiver();
		mNoInternetConnectionView = (TextView) findViewById(R.id.noInternetConnectionTextView);

		Constants.COLUMNWIDTH = getColumnWidth();
	}

	private DrawerLayout.DrawerListener drawerListener = new DrawerListener() {

		@Override
		public void onDrawerStateChanged(int arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDrawerSlide(View arg0, float arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDrawerOpened(View drawerView) {
			// TODO Auto-generated method stub


			mSettingsFragment.updateFields();
			mSettingsFragment.disablePasswordFieldOfflineMode();
			mSettingsFragment.updateManualCaptureTimerEditText();

		}

		@Override
		public void onDrawerClosed(View drawerView) {
			if (drawerView == mDrawerLayout.getChildAt(1)) {
				// Setting drawer
				// close keyboard if already visible
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (drawerView == null) {
					drawerView = new View(getApplicationContext());
				}
					imm.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);

			}			
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
	}

	

	@Override
	protected void onStop() {
		
		super.onStop();
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		final View menu_view = menu.findItem(R.id.existingDocs).getActionView();
		mMenucountTextview = (TextView) menu_view.findViewById(R.id.custom_menu_tview);
		mMenucountImageView = (ImageView)menu_view.findViewById(R.id.custom_menu_iview);
		mMenucountImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				((KMCApplication) getApplicationContext()).setOnHomeScreen(false);
				Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
				Intent i = new Intent(HomeActivity.this, ExistingItemsActivity.class);
				startActivityForResult(i, RequestCode.SHOW_PENDING.ordinal());
			}
		});
		updatePendingCount();	    
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// Handle action buttons
		switch (item.getItemId()) {
		case R.id.info:
			if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
			Intent intent = new Intent(this, InfoScreen.class);
			startActivity(intent);
			overridePendingTransition( R.anim.slide_in_right, R.anim.fadeout);
			((KMCApplication) getApplicationContext()).setOnHomeScreen(false);
			Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
			return true;
		case R.id.existingDocs:
			Constants.IS_HELPKOFAX_FLOW = false;
			Intent i = new Intent(this, ExistingItemsActivity.class);
			startActivity(i);
			Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
			((KMCApplication) getApplicationContext()).setOnHomeScreen(false);
			break;
		default:
			break;
		}
		return true;
	}


	OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View view,
				int position, long index) {
			Log.d(TAG, "position::" + position);
			Constants.IS_HELPKOFAX_FLOW = false;
			Log.e(TAG, "doc name is:: "+mDoctypelist.get(position));
			if (mUtilRoutines.getAvailableMemorySize() <= Constants.CAPTURE_LAUNCH_MEMORY) {
				mCustomDialog.show_popup_dialog(HomeActivity.this,
						AlertType.INFO_ALERT,
						getResources().getString(R.string.error_lbl),
						getString(R.string.str_launch_time_memory_warning), null,
						null, Messages.MESSAGE_DIALOG_LOW_MEMORY_ERROR,
						null, false);
			} else {
				((KMCApplication) getApplicationContext())
						.setOnHomeScreen(false);
				Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
				if (mDoctypelist.get(position).equals(str_gift_card)) {
					openGiftcardListScreen();
				}else if(mDoctypelist.get(position).equals(str_helpkofax)){
					Constants.IS_HELPKOFAX_FLOW = true;
					openHelpKofaxScreen();
				} else {
					mDocMgr.setCurrentDocTypeIndex(position);
					// create unique file name for the item directory to store
					// on disk
					String uniqueFileName = mUtilRoutines
							.createUniqueItemName();
				mDocMgr.setOpenedDocName(uniqueFileName);
				try {
						mDocMgr.downloadDocTypeObject(mDocMgr
								.getCurrentDocTypeIndex());
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
				} catch (KmcException e) {
					e.printStackTrace();
				}
				insertIteminDB(mDocMgr);
				//ArrayList<String> mImgUrlList = new ArrayList<String>();
				//mDocMgr.setImgUrlList(mImgUrlList);
				openCaptureView(HomeActivity.this, HomeActivity.this);
				//openItemDetailScreen(Globals.RequestCode.CAPTURE_DOCUMENT.ordinal());
			}
		}
		}
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(Globals.RequestCode.values().length > requestCode){
			Globals.RequestCode myRequestCode = Globals.getRequestCode(requestCode);
			switch (myRequestCode) {
			case CAPTURE_DOCUMENT:
			case SHOW_HELPSCREEN:
				if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					if(data != null &&  data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
						offlineLogout();					
						return;
					}
					if (data != null && data.hasExtra(Constants.STR_CAPTURE_COUNT) && data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) > 0) {
						showItemDetailsScreen();					
					}
				} else if (resultCode == Globals.ResultState.RESULT_CANCELED.ordinal()) {
					//When use cancels on capture screen and if no images are captured, then delete the added item record from database
					if(data != null && 
							data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) <= 0) {
						removeItemFromDB();
					}
				}
				break;

			case SERVER_TYPE:
				isFromServerType = true;
				if(resultCode == Globals.ResultState.RESULT_OK.ordinal()){
					mSettingsFragment.updateServerTypeName();
				}
				break;
			case SHOW_PENDING:
			case SHOW_ITEM_DETAILS:
				if(data != null && data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
					offlineLogout();					
					return;
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		if(mDrawerLayout.isDrawerOpen(Gravity.RIGHT)){
			mDrawerLayout.closeDrawer(Gravity.RIGHT);
			return;
		}else if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)){
			mDrawerLayout.closeDrawer(Gravity.LEFT);
			return;
		}

		doCloseApp = true;
		try {
			if (mServerMgr.isLoggedIn()) {
				// isClosingApp = true;
				mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_please_wait),true);
				mServerMgr.logout(mSessionStateHandler);
				//update preference for explicit-logout = false
				mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_EXPLICIT_LOGOUT, false);
			}else if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
				Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_OFFLINE;
				clearList();
				mSettingsFragment.updateUI(SessionState.SESSION_LOGGING_OUT);
				finish();
			} else {
				finish();
			}
		} catch (KmcException e) {
			mCustomDialog.closeProgressDialog();
			e.printStackTrace();
			finish();
		} catch (KmcRuntimeException e) {
			mCustomDialog.closeProgressDialog();
			e.printStackTrace();
			finish();
		}
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onResume() {
		super.onResume();
		((KMCApplication) getApplicationContext()).setOnHomeScreen(true);
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = false;
		if(mServerMgr.isLoggedIn() && mDocListRefreshTime != null){
			Calendar cal = Calendar.getInstance();
			cal.getTimeInMillis();
			long diff = cal.getTimeInMillis() - mDocListRefreshTime.getTimeInMillis();
			long hours = diff / (60 * 60 * 1000);
			if(hours >= 24){
				try {
					isLoggedInForRefreshList = true;
					Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_ONLINE;
					mServerMgr.logout(mSessionStateHandler);
				} catch (KmcException e) {
					isLoggedInForRefreshList = false;
					e.printStackTrace();
				}
			}
			Log.d(TAG,"no.of hours"+hours);
		}
		//if(mdocumentAdapter != null && mListview != null) {
		if( mListview != null) {
			if(mServerMgr.isLoggedIn()){
				refreshList(mDocMgr.downloadDocTypeNamesList());
			}else if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
				updateDocumentTypeListFromDatabase();
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
		}
		if(!isFromServerType) {
			if (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE) {
				mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
			} else if (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGOUT_ONLINE || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGOUT_OFFLINE) {
				mSettingsFragment.updateUI(SessionState.SESSION_LOGGING_OUT);
			}
		}else{
			isFromServerType = false;
		}
		updatePendingCount();
		Constants.NETWORK_CHANGE_LISTENER = HomeActivity.this;
		if(!mUtilRoutines.checkInternet(HomeActivity.this) || Globals.gAppModeStatus ==  AppModeStatus.FORCE_OFFLINEMODE){
			mNoInternetConnectionView.setVisibility(View.VISIBLE);
		}else{
			mNoInternetConnectionView.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	protected void onDestroy() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		Constants.NETWORK_CHANGE_LISTENER = null;
		cleanup();
		super.onDestroy();
	}

	// - private nested classes (more than 10 lines)
	/**
	 * Task to import all pending cases from old KMC version
	 */
	private class ImportPendingCases extends AsyncTask<Void, Void, Void>{
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mCustomDialog.showProgressDialog(HomeActivity.this, getResources().getString(R.string.progress_msg_please_wait),false);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if(((KMCApplication) getApplicationContext()).getDaoMaster().isUpgradeFrom_2_2()){
				if(mDBManager.updatePendingProcessFileNames(HomeActivity.this)){
					((KMCApplication) getApplicationContext()).setupgradeFlag(false);
				}
			}else{
			mDiskMgr.retrieve1o2data(getFilesDir().getAbsolutePath());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mCustomDialog.closeProgressDialog();
			refreshList(mDocMgr.downloadDocTypeNamesList());
			updatePendingCount();
		}
	}

	// - private methods
	private void setupHandler() {
		mHomeActivityHandler = new Handler(new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];

				switch (whatMessage) {
				case MESSAGE_DIALOG_EMAIL_INFORMATION:
					if (msg.arg1 == RESULT_OK) {
						// open settings slider for user to set email ID
						mDrawerLayout.openDrawer(Gravity.LEFT);
						mSettingsFragment.focusEmailField();
					}
					break;
					case MESSAGE_DIALOG_PERMISSION:
						if (msg.arg1 == RESULT_OK) {
							if(!mUtilRoutines.isEmailRegistered() && mPrefUtils.isUsingKofax()) {
								mCustomDialog
										.show_popup_dialog(HomeActivity.this,
												AlertType.INFO_ALERT,
												getResources().getString(R.string.lbl_no_email_registered),
												getResources().getString(R.string.toast_error_email_required),
												null, null,
												Messages.MESSAGE_DIALOG_EMAIL_INFORMATION,
												mHomeActivityHandler,
												false);
							}
						}
						break;

				case MESSAGE_DLALOG_INVALID_EMAIL:
					if (msg.arg1 == RESULT_OK) {
						// open settings slider for user to re-enter email ID
						mDrawerLayout.openDrawer(Gravity.LEFT);
						mSettingsFragment.focusEmailField();
					}
					break;
				case MESSAGE_DIALOG_LOW_MEMORY_ERROR:
					if (msg.arg1 == RESULT_OK) {
						finish();
					}
					break;
				case MESSAGE_LOGIN:
					if(mNoInternetConnectionView != null) {
						mNoInternetConnectionView.setVisibility(View.GONE);
					}
					if(mDocMgr != null){
						try {
							mCustomDialog.showProgressDialog(HomeActivity.this, getResources().getString(R.string.progress_msg_logging_in),false);
							isRequiredLogin = true;
							mServerMgr.setServerTimeout(120);	//timout of 2 mins
							mRetry = 1;
							mServerMgr.registerDevice(mSessionStateHandler, false);
						} catch (KmcRuntimeException e) {
							mCustomDialog.closeProgressDialog();
							mCustomDialog
							.show_popup_dialog(HomeActivity.this,
									AlertType.ERROR_ALERT,
									getResources().getString(R.string.lbl_login_failed_error),
									e.getErrorInfo().getErrMsg(),
									null, null,
									Messages.MESSAGE_DIALOG_LOGIN_FAILURE,
									null,
									false);
							e.printStackTrace();
						} catch (KmcException e) {
							mCustomDialog.closeProgressDialog();
							mCustomDialog
							.show_popup_dialog(HomeActivity.this,
									AlertType.ERROR_ALERT,
									getResources().getString(R.string.lbl_login_failed_error),
									e.getErrorInfo().getErrMsg(),
									null, null,
									Messages.MESSAGE_DIALOG_LOGIN_FAILURE,
									null,
									false);
							e.printStackTrace();
						}
					}
					break;
				case MESSAGE_DIALOG_LOGOUT_CONFIRMATION:
					if (msg.arg1 == RESULT_OK) {
						try {
							isRequiredLogin = false;
							mRetry = 0;
							mCustomDialog.showProgressDialog(HomeActivity.this, getResources().getString(R.string.progress_msg_please_wait),true);
							mServerMgr.logout(mSessionStateHandler);
							//update preference for explicit-logout = true
							mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_EXPLICIT_LOGOUT, true);
						}
						catch (KmcRuntimeException e) {
							mCustomDialog.closeProgressDialog();
							e.printStackTrace();
						}
						catch (KmcException e) {
							mCustomDialog.closeProgressDialog();
							e.printStackTrace();
						} 
					}
					break;
				case MESSAGE_DIALOG_LOGIN_FAILURE:
					if (msg.arg1 == RESULT_OK) {
						if(mCustomUrlUtils.isUsingCustomUrl()) {
							finish();
						}
						else {
							mDrawerLayout.openDrawer(Gravity.LEFT);
						}
					}
					break;
				case MESSAGE_PROCESS_QUEUE_HALTED:
					cleanup();
					break;

				case MESSAGE_DEFAULT:
					if(mCustomUrlUtils.isUsingCustomUrl()) {
						finish();
						break;
					}
					else {
						break;
					}

				case MESSAGE_DIALOG_IMPORT_DATA_CONFIRMATION:
					if(msg.arg1 == RESULT_OK){
						new ImportPendingCases().execute();
					}else{
						mCustomDialog.closeProgressDialog();
						refreshList(mDocMgr.downloadDocTypeNamesList());
					}
					break;
				case MESSAGE_DIALOG_OFFLINE_CONFIRMATION:
					if(msg.arg1 == RESULT_OK){
						Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_OFFLINE;
						List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(HomeActivity.this,
								mUtilRoutines.getUser(),
								mPrefUtils.getCurrentHostname(),mPrefUtils.getCurrentServerType());
						
						if(list != null && list.size() > 0){
							mDBManager.setUserInformationEntity(list.get(0));
						}
						updateDocumentTypeListFromDatabase();
						updatePendingCount();
						mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
					}else{
						Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_OFFLINE;
						mDrawerLayout.openDrawer(Gravity.LEFT);
					}
					break;
				case MESSAGE_OFFLINE_LOGIN:
					if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
						updateDocumentTypeListFromDatabase();
						mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
						mDrawerLayout.closeDrawer(Gravity.LEFT);
					}else{
						clearList();
						mSettingsFragment.updateUI(SessionState.SESSION_LOGGING_OUT);
					}
					updatePendingCount();
					break;
				case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
					//Navigate back to settings
					if(msg.arg1 == RESULT_OK){
						offlineLogout();
					}else{
						Globals.gAppModeStatus = Globals.AppModeStatus.FORCE_OFFLINEMODE;
						mNoInternetConnectionView.setVisibility(View.VISIBLE);
					}
					break;
				default:
					break;
				}
				return true;
			}
		});
	}

	int mRetry = 0;

	private Handler mSessionStateHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {

			KfsSessionStateEvent arg0 = (KfsSessionStateEvent) msg.obj;

			Log.i(TAG, "arg0.getSessionState() ::: " + arg0.getSessionState());

			switch (arg0.getSessionState()) {
			case SESSION_UNREGISTERED:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					if(((mPrefUtils.getCurrentServerType() != null) && (mPrefUtils.getCurrentServerType().equals(serverType.KFS.name()))) && (mRetry == 1)) {
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
							mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.ERROR_ALERT,
									getResources().getString(R.string.error_lbl), arg0.getErrorInfo().getErrMsg(),
									null, null,
									Messages.MESSAGE_DEFAULT, mHomeActivityHandler,
									false);
							mCustomDialog.closeProgressDialog();
						}
						mRetry = 0;
					}
					else {
						mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.ERROR_ALERT,
								getResources().getString(R.string.error_lbl), arg0.getErrorInfo().getErrMsg(),
								null, null,
								Messages.MESSAGE_DEFAULT, mHomeActivityHandler,
								false);
						mRetry = 0;
						mCustomDialog.closeProgressDialog();
					}
				}

				break;
			case SESSION_REGISTERING:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					mUtilRoutines.showToast(HomeActivity.this, getResources().getString(R.string.error_msg_while_registering)
							+ arg0.getErrorInfo().name());
					mCustomDialog.closeProgressDialog();
				}
				break;
			case SESSION_REGISTERED:
				try {
					if(arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS){
						if (arg0.getErrorInfo() == ErrorInfo.KMC_LO_USER_LOGIN_ERROR || arg0.getErrorInfo() == ErrorInfo.KMC_LO_REQUEST_TIMEOUT) {
							mCustomDialog.closeProgressDialog();
							mCustomDialog
							.show_popup_dialog(HomeActivity.this,
									AlertType.ERROR_ALERT,
									getResources().getString(R.string.lbl_login_error),
									arg0.getErrorInfo().getErrDesc(),
									null, null,
									Messages.MESSAGE_DIALOG_LOGIN_FAILURE,
									mHomeActivityHandler,
									false);
						}else if(arg0.getErrorInfo() == ErrorInfo.KMC_LO_USER_LOGOUT_ERROR){
							if(isLoggedInForRefreshList){
								isLoggedInForRefreshList = false;
								mCustomDialog.closeProgressDialog();
								mCustomDialog
								.show_popup_dialog(HomeActivity.this,
										AlertType.ERROR_ALERT,
										getResources().getString(R.string.progress_msg_updating_list_label),
										getResources().getString(R.string.progress_msg_updating_list_failed),
										null, null,
										Messages.MESSAGE_DIALOG_LOGIN_FAILURE,
										mHomeActivityHandler,
										false);
							}
						}
					}else {
						
						if (doReLogin) {
							doReLogin = false;
							mCustomDialog.dismissAlertDialog();
							//reset all common required variables on logout to initialize for next user
							resetApplicationState();
							mCustomDialog
							.showProgressDialog(HomeActivity.this, getResources().getString(R.string.progress_msg_logging_in),false);
							isRequiredLogin = true;
							mServerMgr.setServerTimeout(120);	//timout of 2 mins
							mRetry = 1;
							mServerMgr.registerDevice(mSessionStateHandler, false);
						} else if (!doCloseApp) {
							if(isRequiredLogin){
								mCustomDialog.dismissAlertDialog();
								mRetry = 0;
								if (isLoggedInForRefreshList == false) {
									mCustomDialog.showProgressDialog(HomeActivity.this, getResources().getString(R.string.progress_msg_logging_in), false);                                      
								}
								resetApplicationState();
								
								if(!mPrefUtils.isUsingKofax() ){
									if (mPrefUtils.isUsingAnonymous()) {
										mServerMgr.anonymousLogin(mSessionStateHandler);

									} else {
										mServerMgr.login(mSessionStateHandler);
									}
								}
								else{
									if (mPrefUtils.isUsingAnonymousDemo()) {
										mServerMgr.anonymousLogin(mSessionStateHandler);

									} else {
										mServerMgr.login(mSessionStateHandler);
									}
								}

							}
						}
					}
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
					isLoggedInForRefreshList = false;
					mUtilRoutines.showToast(HomeActivity.this, e.getMessage());
					mCustomDialog.closeProgressDialog();
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
					isLoggedInForRefreshList = false;					
					mUtilRoutines.showToast(HomeActivity.this, e.getErrorInfo().getErrMsg());
					mCustomDialog.closeProgressDialog();
				} catch (KmcException e) {
					e.printStackTrace();
					isLoggedInForRefreshList = false;
					mUtilRoutines.showToast(HomeActivity.this, e.getErrorInfo().name());
					mCustomDialog.closeProgressDialog();
				}
				break;
			case SESSION_LOGGING_IN:
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					mUtilRoutines.showToast(HomeActivity.this, getResources().getString(R.string.error_msg_while_logging_in)
							+ arg0.getErrorInfo().name());
					isLoggedInForRefreshList = false;
					mCustomDialog.closeProgressDialog();
				}
				break;
			case SESSION_LOGGED_IN:
				mPrefUtils.putPrefValueString(mPrefUtils.KEY_APP_LAST_lOGGED_SERVER_TYPE, mPrefUtils.getCurrentServerType());
				mPrefUtils.putPrefValueString(mPrefUtils.KEY_APP_LAST_lOGGED_HOSTNAME, mPrefUtils.getCurrentHostname());

				mPrefUtils.putPrefValueString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, mUtilRoutines.getUser());

				isLoggedInForRefreshList = false;
				if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
					Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_ONLINE;
					mCustomDialog.closeProgressDialog();
					mSettingsFragment.updateUI(SessionState.SESSION_LOGGING_OUT);
				} else {					
					Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_ONLINE;
					mRetry = 0;
					isRequiredLogin = false;
					mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
					if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)){
						mDrawerLayout.closeDrawer(Gravity.LEFT);
					}
					mCustomDialog.dismissAlertDialog();
					String lastLoggedUser = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, null);
					mDBManager.updateAllItemsToOnline(HomeActivity.this,							
							mPrefUtils.getCurrentServerType(),mPrefUtils.getCurrentHostname(),lastLoggedUser);
					//create directory(if not present already) with the name of logged in user.
					mDiskMgr.createUserDirectory();
					updatePendingCount();
					File oldCaseDir = new File(HomeActivity.this.getFilesDir().getAbsolutePath()+File.separator+Constants.CASELIST);
					if(null != oldCaseDir && oldCaseDir.exists()){
						if(!(mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_IMPORT_DATA_DONT_SHOW_AGAIN, false))){
							mCustomDialog.closeProgressDialog();
							mCustomDialog.show_popup_dialog(HomeActivity.this, 
									AlertType.CONFIRM_ALERT_WITH_CHECKBOX, 
									getResources().getString(R.string.lbl_confirm), 
									getResources().getString(R.string.str_msg_do_you_want_to_import_existing_data), 
									null, null,
									Messages.MESSAGE_DIALOG_IMPORT_DATA_CONFIRMATION, 
									mHomeActivityHandler,
									false);
						}else{
							mCustomDialog.closeProgressDialog();
							refreshList(mDocMgr.downloadDocTypeNamesList());
						}
					}else{
						if(((KMCApplication) getApplicationContext()).getDaoMaster().isUpgradeFrom_2_2()){
							new ImportPendingCases().execute(); 
						}else{
						mCustomDialog.closeProgressDialog();
						refreshList(mDocMgr.downloadDocTypeNamesList());
					}
					}
					mDocListRefreshTime =  Calendar.getInstance();
					mDocListRefreshTime.getTimeInMillis();					
					mUtilRoutines.updateUserInformationList(HomeActivity.this,mDBManager,mPrefUtils,mDocMgr);
				}
				break;
			case SESSION_DOCUMENT_TYPE_READY:
				Log.i(TAG, "DocumentType object is ready!!!!!!!!!!!!!!!!!!!!!");
				break;
			case SESSION_LOGGING_OUT:
				mRetry = 0;
				Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_ONLINE;
				updatePendingCount();
				mDocListRefreshTime = null;
				mSettingsFragment.updateUI(SessionState.SESSION_LOGGING_OUT);
				clearList();
				mCustomDialog.closeProgressDialog();
				if (doCloseApp) {
					finish();
				}
				if(isLoggedInForRefreshList){
					mCustomDialog
					.showProgressDialog(HomeActivity.this, getResources().getString(R.string.progress_msg_updating_list),false);                                          
					isRequiredLogin = true;
				}
				Log.i(TAG, "Logged out!");
				break;
			default:
				break;
			}
			return true;
		}
	});

	private void offlineLogout(){
		Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_OFFLINE;
		Message msg1 = new Message();
		msg1.what = Globals.Messages.MESSAGE_OFFLINE_LOGIN.ordinal();
		mHomeActivityHandler.sendMessage(msg1);
		mDrawerLayout.openDrawer(Gravity.LEFT);
	}

	private void initObjects() {
		mServerMgr = ServerManager.getInstance();
		mCustomDialog = CustomDialog.getInstance();
		mPrefUtils = PrefManager.getInstance();
		mDocMgr = DocumentManager.getInstance(this);
		mDiskMgr = DiskManager.getInstance(this);
		mProcessQueueMgr = ImageProcessQueueManager.getInstance(this);
		mDBManager = DatabaseManager.getInstance();
		mCustomUrlUtils = CustomUrlUtils.getInstance();
		mAppStatsManager = AppStatsManager.getInstance(this);
	}

	private void initActionBar() {
		mActionbar = getActionBar();
		mActionbar.setDisplayHomeAsUpEnabled(true);
		mActionbar.setHomeButtonEnabled(true);
		mActionbar.setDisplayShowTitleEnabled(true);
		if(android.os.Build.MODEL.contains(mYogaPadLenovoModel)) {
			mActionbar.setTitle("KOFAX");
		}
	}

	private void initDrawer() {
		int wid = getDeviceWidth();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		findViewById(R.id.left_drawer).getLayoutParams().width = wid;
		// set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.END);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
				mDrawerLayout, /* DrawerLayout object */
				R.drawable.menu, /* nav drawer image to replace 'Up' caret */
				R.string.drawer_open, /* "open drawer" description for accessibility */
				R.string.drawer_close /* "close drawer" description for accessibility */
				);

		mDrawerLayout.setDrawerListener(drawerListener);
		mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mSettingsFragment.getView());
		mDrawerLayout.setFocusableInTouchMode(false);
	}

	private void doInitialEmailCheck() {
		if (mPrefUtils.isUsingKofax()) {
			if(!mUtilRoutines.isEmailRegistered()) {
				mCustomDialog
				.show_popup_dialog(this,
						AlertType.INFO_ALERT,
						getResources().getString(R.string.lbl_no_email_registered),
						getResources().getString(R.string.toast_error_email_required),
						null, null,
						Messages.MESSAGE_DIALOG_EMAIL_INFORMATION,
						mHomeActivityHandler,
						false);
			} // check if entered email is valid
			else if (!mUtilRoutines.validateEmail(mPrefUtils
					.getCurrentEmail())) {
				//Check if custom server email is not empty
				if(!(!mPrefUtils.isUsingKofax() && mPrefUtils.getCurrentEmail().length() == 0)){
					mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.ERROR_ALERT,
							getResources().getString(R.string.lbl_invalid_email),
							getResources().getString(R.string.toast_error_invalid_email),
							null, null,
							Messages.MESSAGE_DLALOG_INVALID_EMAIL,
							mHomeActivityHandler,
							false);
				}
			}
			else {
				if (!mUtilRoutines.checkInternet(this)) {
					mUtilRoutines
					.showToast(HomeActivity.this, getResources().getString(R.string.toast_no_network_connectivity)+", "+getResources().getString(R.string.toast_app_work_offline));
				} else {
					//before auto-login, check if user had explicitly logged out(by selecting logout option on setting screen) last time. If yes, do not auto-login.
					if(mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_EXPLICIT_LOGOUT, mPrefUtils.DEF_EXPLICIT_LOGOUT) == false) {
						initNewCaseTypeList();
					}
					else {
						mDrawerLayout.openDrawer(Gravity.LEFT);
					}
				}
			}
		}
		else if (!mPrefUtils.isUsingKofax()){
			if(mUtilRoutines.isEmailRegistered() && !mUtilRoutines.validateEmail(mPrefUtils
					.getCurrentEmail())) {
				mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.ERROR_ALERT,
						getResources().getString(R.string.lbl_invalid_email),
						getResources().getString(R.string.toast_error_invalid_email),
						null, null,
						Messages.MESSAGE_DLALOG_INVALID_EMAIL,
						mHomeActivityHandler,
						false);
			}
			else {
				if (!mUtilRoutines.checkInternet(this)) {
					mUtilRoutines
					.showToast(HomeActivity.this, getResources().getString(R.string.toast_no_network_connectivity)+", "+getResources().getString(R.string.toast_app_work_offline));
				} else {
					//before auto-login, check if user had explicitly logged out(by selecting logout option on setting screen) last time. If yes, do not auto-login.
					if(mCustomUrlUtils.isUsingCustomUrl() || mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_EXPLICIT_LOGOUT, mPrefUtils.DEF_EXPLICIT_LOGOUT) == false) {
						mCustomUrlUtils.setUsingCustomUrl(false);
						initNewCaseTypeList();
					}
					else {
						mDrawerLayout.openDrawer(Gravity.LEFT);
					}
				}
			}
		}
	}

	private void updatePendingCount(){
		List<ItemEntity> itemList = null;
		if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE || Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE){
				itemList = mDBManager.getAllItems(
				this,
				mPrefUtils.getCurrentServerType(),
				mPrefUtils.getCurrentHostname(), mUtilRoutines.getUser());
		}
		if(itemList != null && mMenucountTextview != null){
			int count = 0;
				count = itemList.size();
			//If count is zero then visibility of counter is gone.else display count value.
			if(count == 0){
				mMenucountTextview.setVisibility(View.GONE);
			}if(count > 0){
				mMenucountTextview.setVisibility(View.VISIBLE);
				mMenucountTextview.setText(Constants.STR_EMPTY+count);
			}
		}else{
			if(mMenucountTextview != null)
			mMenucountTextview.setVisibility(View.GONE);
		}
	}

	private void removeItemFromDB(){
		mDBManager.deleteItemWithId(this, mDBManager.getItemEntity().getItemId());
	}

	private void refreshList(ArrayList<String> doctypeList) {
		clearList();

		mDoctypelist = doctypeList;
		if(mPrefUtils.isUsingKofax() && mDoctypelist != null){
			if(!mPrefUtils.isUsingAnonymousDemo() && Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE ){
				mDoctypelist.add(str_gift_card); //add only for demo server
			}
			mDoctypelist.add(str_helpkofax);
		}
		
		if (mDoctypelist != null) {
			mdocumentAdapter = new documentAdapter(this, mDoctypelist);
			Log.d(TAG, "mDoctypelist size::" + mDoctypelist.size());
		} else {
			Log.d(TAG, "mDoctypelist is null");
		}
		// setting list adapter
		Log.d(TAG, "mListview :: " + mListview);
		Log.d(TAG, "mdocumentAdapter :: " + mdocumentAdapter);

		if(mdocumentAdapter != null)
			mListview.setAdapter(mdocumentAdapter);
//		mdocumentAdapter.notifyDataSetChanged();
	}

	private void clearList() {
		if (mdocumentAdapter != null) {
			mdocumentAdapter.clear(); // clear existing data
			mListview.setAdapter(mdocumentAdapter);
//			mdocumentAdapter.notifyDataSetChanged();
		}
	}

	@SuppressLint("NewApi")
	private int getDeviceWidth(){
		Display display = getWindowManager().getDefaultDisplay();
		int width = 0;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			width = display.getWidth();
		} else {
			Point size = new Point();
			display.getSize(size);
			width = size.x;
		}	            
		//		width -= (width/4)*3;	// 75% of screen width

		width -= width/5;	//80% of screen width
		return width;
	}

	private void initNewCaseTypeList() {
		mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_please_wait), false);
		try {
			if (!mServerMgr.isLoggedIn()) {
				mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_logging_in),false);
				isRequiredLogin = true;
				mServerMgr.setServerTimeout(120);	//timout of 2 mins
				mRetry = 1;
				mServerMgr.registerDevice(mSessionStateHandler, false);
			} else {
				doReLogin = true;
				mServerMgr.logout(mSessionStateHandler);
			}
			clearList();

		} catch (KmcException e) {
			Log.e(TAG, "KmcException occured:: " + e.getMessage());
			e.printStackTrace();
			mUtilRoutines.showToast(this, e.getErrorInfo().name());
			mCustomDialog.closeProgressDialog();
		} catch (KmcRuntimeException e) {
			Log.e(TAG, "KmcRuntimeException occured:: " + e.getMessage());
			Log.e(TAG, "e.getCause:: " + e.getCause());
			Log.e(TAG, "e.getErrorInfo():: " + e.getErrorInfo());
			e.printStackTrace();
			if (e.getErrorInfo() == ErrorInfo.KMC_LO_SESSION_STATE_BUSY) {
				mUtilRoutines
				.showToast(this, e.getMessage());
			} else {
				mUtilRoutines.showToast(this, e.getErrorInfo().name());
			}
			mCustomDialog.closeProgressDialog();
		}
	}

	private void openGiftcardListScreen(){
		Intent intent = new Intent(this, GiftCardListActivity.class);
		startActivity(intent);
	}

	private void openHelpKofaxScreen(){
		Intent intent = new Intent(this, HelpKofaxActivity.class);
		startActivityForResult(intent,
				RequestCode.SHOW_HELPSCREEN.ordinal());	
	}

	public void openCaptureView(Context context, Activity activity) {
		List<ProcessingParametersEntity> list = mDBManager.getProcessingParametersFromDetails(this,mDBManager.getItemEntity().getItemTypeName(),mDBManager.getUserInformationEntity().getUserInformationId());
		if(null != list && list.size() > 0){
			mDBManager.setProcessingParametersEntity(list.get(0));
		}
		
		Intent cameraIntent = new Intent(context, Capture.class);

		cameraIntent.putExtra(Constants.STR_IMAGE_COUNT, 0);
		cameraIntent.putExtra(Constants.STR_IS_NEW_ITEM, true);

		activity.startActivityForResult(cameraIntent,
				RequestCode.CAPTURE_DOCUMENT.ordinal());
	}


	private void insertIteminDB(DocumentManager mDocMgrObj){
		ItemEntity item = new ItemEntity();
		item.setItemName(mDocMgrObj.getOpenedDoctName());
		item.setItemTypeName(mDocMgrObj.getDocTypeNamesList().get(mDocMgrObj.getCurrentDocTypeIndex()));
		item.setItemCreatedTimeStamp(new Date());
		item.setServerId(mPrefUtils.getCurrentServerType());
		item.setHostname(mPrefUtils.getCurrentHostname());

		item.setUserId(mUtilRoutines.getUser());
		if(Globals.gAppLoginStatus ==  AppLoginStatus.LOGIN_ONLINE){
			item.setIsOffline(false);
		}else{
			item.setIsOffline(true);
		}
		
		item.setFieldName(null);
		mDBManager.insertOrUpdate(this, item);
		mDBManager.setItemEntity(item);
	}

	private void doOfflineLogin(){		
		if(checkForOfflineSupport()&& mUtilRoutines.isLastLoggedInUser(mPrefUtils)){
			Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_OFFLINE;
			List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(HomeActivity.this,
					mUtilRoutines.getUser(),
					mPrefUtils.getCurrentHostname(),mPrefUtils.getCurrentServerType());
			
			if(list != null && list.size() > 0){

				mDBManager.setUserInformationEntity(list.get(0));
			}
			mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
			mSettingsFragment.updateFields();
			mDrawerLayout.closeDrawer(Gravity.LEFT);
			updateDocumentTypeListFromDatabase();
		}else
		{

				mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.app_msg_invalid_session_msg), null, null, Messages.MESSAGE_DIALOG_OFFLINE_ERROR, mHomeActivityHandler,false);


		}

		
	}

	private void registerBroadcastReceiver() {
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i(TAG,
							"Broadcast received!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					//This broadcast is usually received when there is login-failure because of invalid custom-url on main activity.
					if (intent.getAction() == Constants.CUSTOM_INTENT_LOGIN_ERROR) {
						Log.i(TAG,
								"Broadcast received CUSTOM_INTENT_LOGIN_ERROR");
						mSettingsFragment.updateFields();
						mSettingsFragment.updateUI(SessionState.SESSION_LOGGING_OUT);
						clearList();
						Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_ONLINE;
						updatePendingCount();
					}else if (intent.getAction() == Constants.CUSTOM_INTENT_OFFLINE_LOGIN) {					
						doOfflineLogin();
					}else if (intent.getAction() == Constants.CUSTOM_INTENT_OFFLINE_LOGOUT_TO_LOGIN) {
						offlineLogout();
					}
					else if(intent.getAction() == Constants.CUSTOM_INTENT_LOGIN_UPDATED) {
						List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(HomeActivity.this,
								mUtilRoutines.getUser(),
								mPrefUtils.getCurrentHostname(),mPrefUtils.getCurrentServerType());
						
						if(list != null && list.size() > 0){
							mDBManager.setUserInformationEntity(list.get(0));
						}
						mCustomDialog.dismissAlertDialog();
						mSettingsFragment.updateFields();
						mSettingsFragment.updateUI(SessionState.SESSION_LOGGED_IN);
						mSettingsFragment.disablePasswordFieldOfflineMode();
						mDrawerLayout.closeDrawer(Gravity.LEFT);
					}

					else if (intent.getAction() == Constants.CUSTOM_INTENT_OFFLINE_LOGIN_ERROR) {
						mDrawerLayout.closeDrawer(Gravity.LEFT);
					}
				}
			};
		}
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_LOGIN_ERROR);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_LOGIN_UPDATED);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_OFFLINE_LOGIN);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_OFFLINE_LOGOUT_TO_LOGIN);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_OFFLINE_LOGIN_ERROR);
		registerReceiver(mReceiver, intentFilter);
	}

	private boolean checkForOfflineSupport(){
		boolean isOfflineSupport = false;

		String lastLoggedUserServerType =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_SERVER_TYPE, mPrefUtils.DEF_USR_SERVER_TYPE);
		String lastLoggedUserHostName =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_HOSTNAME, mPrefUtils.DEF_USR_HOSTNAME);
		String lastLoggedUser = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, mPrefUtils.DEF_USR_UNAME);
		
		if(mPrefUtils.isUsingKofax() && mPrefUtils.isUsingAnonymous()){		
			lastLoggedUser = mPrefUtils.DEF_KFX_UNAME;			
		}
		
		List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(this,
				lastLoggedUser,
				lastLoggedUserHostName,lastLoggedUserServerType);
		
		if(list != null && list.size() > 0){
			isOfflineSupport = true; 
		}
		return isOfflineSupport;
	}
	
	private void showOfflineModeSupportAlert(){
		mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.CONFIRM_ALERT,
				getResources().getString(R.string.lbl_offline), getResources().getString(R.string.app_msg_offline_msg),
				null, null,
				Messages.MESSAGE_DIALOG_OFFLINE_CONFIRMATION, mHomeActivityHandler,
				false);
		
	}
	
	@SuppressWarnings("unchecked")
	private void updateDocumentTypeListFromDatabase(){
		List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(this,
				mUtilRoutines.getUser(),
				mPrefUtils.getCurrentHostname(),mPrefUtils.getCurrentServerType());
		if(list != null && list.size() > 0){
			UserInformationEntity userEntity = list.get(0);
			String docTypeList = userEntity.getDocumentTypes();
			if (docTypeList != null) {
				String[] separated = docTypeList
						.split(Constants.KMC_STRING_SPLIT_SEPERATOR);
				
			ArrayList<String> doclist = new ArrayList<String>(Arrays.asList(separated)); //new ArrayList is only needed if you absolutely need an ArrayList
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
					refreshList(nonHelpKofaxList);
				}else{
					allDocList = doclist;
					nonHelpKofaxList = allDocList;
					refreshList(allDocList);
				}
				mDocMgr.setNonHelpDocumentNamesList(nonHelpKofaxList);
				mDocMgr.setHelpDocumentNamesList(helpKofaxList);
				mDocMgr.setDocTypeNamesList(allDocList);
				
			}
		}
	}
	}
	
	
	private void resetApplicationState() {
		if (mDocMgr != null) {
			mDocMgr.reset();
		}

		if (mDiskMgr != null) {
			mDiskMgr.reset();
		}
	}
	
	private void showItemDetailsScreen(){
		 Intent i = new Intent(this, ItemDetailsActivity.class);
         i.putExtra(Constants.STR_IS_NEW_ITEM, true);
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

	private void cleanup() {
		isRequiredLogin = false;
		//stop process-queue if its running
		if((mProcessQueueMgr == null) || (mProcessQueueMgr.cleanup() == true)) {

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
			if(mPrefUtils != null) {
				//cleanup
				mPrefUtils = null;
			}
			if(mDBManager != null) {
				mDBManager.cleanup();
				mDBManager = null;
			}
			if(mCustomUrlUtils != null) {
				mCustomUrlUtils.cleanup();
				mCustomUrlUtils = null;				
			}
			if (mCustomDialog != null) {
				mCustomDialog.finish();
				mCustomDialog = null;
			}
			if(mdocumentAdapter != null) {
				mdocumentAdapter.clear();
				mdocumentAdapter = null;
			}
			if(mListview != null) {
				mListview = null;
			}
			if(mDoctypelist != null) {
				mDoctypelist = null;
			}
			
			if(mAppStatsManager != null) {
				mAppStatsManager.deinitAppStatistics();
				mAppStatsManager = null;
			}
			mHomeActivityHandler = null;
			System.gc();
		}
		mDocListRefreshTime = null;
		//else wait until process-queue manager is stopped.
	}
	
	@Override
	public void onNetworkChanged(boolean isConnected) {
		if(mPrefUtils.isUsingKofax() && !mPrefUtils.isUsingAnonymousDemo() && mDoctypelist != null){
			if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE && mUtilRoutines.checkInternet(this)){
				mDoctypelist.add(str_gift_card); //add only for demo server
			}else{
				mDoctypelist.remove(str_gift_card);
			}
		}
		if(isConnected && Globals.gAppModeStatus !=  AppModeStatus.FORCE_OFFLINEMODE){
			mNoInternetConnectionView.setVisibility(View.GONE);
		}else{
			mNoInternetConnectionView.setVisibility(View.VISIBLE);
		}
		if(Globals.isRequiredOfflineAlert()  && isConnected){
			if(mCustomDialog != null && !mCustomDialog.isProgressDialogShowing() && mUtilRoutines.isAppOnForeground(HomeActivity.this)){
				mCustomDialog.dismissAlertDialog();
				mCustomDialog.show_popup_dialog(HomeActivity.this,AlertType.CONFIRM_ALERT ,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
					getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION,mHomeActivityHandler, false);
			}
		}

		mSettingsFragment.disablePasswordFieldOfflineMode();
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
				break;
			}
			default:
				break;
		}
		return;
	}

	private void CheckDangerousApplicationPermissions(){
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
		}

		// No explanation needed, we can request the permission.
		if(null != permissions) {
			ActivityCompat.requestPermissions(this,
					permissions,
					APPLICATION_PERMISSIONS_REQUEST);
		}

	}

	private void showPermissionDialog(){
		if (mCustomDialog != null) {
			mCustomDialog.dismissAlertDialog();
			mCustomDialog.show_popup_dialog(HomeActivity.this, AlertType.INFO_ALERT, getResources().getString(R.string.permissions),
					getResources().getString(R.string.permission_alert), null, null, Messages.MESSAGE_DIALOG_PERMISSION, mHomeActivityHandler, false);
		}


	}

	private int getColumnWidth() {
		Display display = getWindowManager().getDefaultDisplay();
		int width = 0;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			width = display.getWidth();
		} else {
			Point size = new Point();
			display.getSize(size);
			width = size.x;
		}

		Resources res = HomeActivity.this.getResources();
		int lPad = (int) res.getDimension(R.dimen.left_padding);
		int rPad = (int) res.getDimension(R.dimen.right_padding);
		int hSpace = (int) res.getDimension(R.dimen.horizontal_spacing);
		return (width - lPad - rPad + hSpace) / 3 - hSpace;
	}
}
