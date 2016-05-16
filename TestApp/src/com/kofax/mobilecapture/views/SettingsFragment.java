// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.kmc.klo.logistics.SessionState;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.ServerManager;
import com.kofax.mobilecapture.dbentities.UserInformationEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

import java.util.List;


/**
 * This class is responsible for all application settings like Login details,
 * Quick preview status,sensitivity etc...
 */

@SuppressLint("NewApi")
public class SettingsFragment extends Fragment{

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = SettingsFragment.class.getSimpleName();
	// - Private data.
	/* SDK objects */
	/* Application objects */
	private PrefManager mPrefUtils = null;
	private UtilityRoutines mUtilRoutines;
	private CustomDialog    mCustomDialog = null;
	private ServerManager mServerMgr = null;
	private DatabaseManager mDBManager = null;

	/* Standard variables */
	private EditText serverhostnameeditText;
	private EditText porteditText;
	private EditText domaineditText;
	private EditText usernameeditText;
	private EditText passwordeditText;
	private EditText emaileditText;
	private EditText urleditText;
	private Switch mUsekofaxSwitch;
	private Switch sslSwitch;
	private Switch mAnonymousSwitch;
	private Switch mAnonymousSwitchDemo;
	private LinearLayout mHidelayout,mHidLayout;
	private RelativeLayout mAdvlayout;
	private ImageView mAdvOptionArrowImg;
	private View mLayoutview; 
	private TextView mlogoutTextview;
	private TextView server_type_TextView;
	private TextView usernameText,domainText,passwordText;

	private ImageView mlogoutImageview;
	private boolean mAdvanced_flag = false;
	private List<String> ServerTypeList = null;
	private EditText manualCaptureTimerEditText ;

	private View mLogoutlayout;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressLint("NewApi")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@SuppressLint({ "NewApi", "InflateParams", "ResourceAsColor" })
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mUtilRoutines = UtilityRoutines.getInstance();
		
		// Check if valid license is present before proceeding
				if (!mUtilRoutines.checkKMCLicense(getActivity().getApplicationContext())) {
					mUtilRoutines
					.showToast(getActivity().getApplicationContext(), getResources().getString(R.string.toast_use_valid_license));
					getActivity().finish();
				}
		

		mPrefUtils = PrefManager.getInstance();
		if(mPrefUtils.isAppBackgroundForceStop()){
			mPrefUtils.init(getActivity().getApplicationContext());
		}
		
		mCustomDialog = CustomDialog.getInstance();
		mServerMgr = ServerManager.getInstance();
		mDBManager = DatabaseManager.getInstance();

		mLayoutview = inflater.inflate(R.layout.settings, null);
		mLayoutview.setDrawingCacheEnabled(false);

		ServerTypeList = Globals.getServerTypeNames(getActivity().getApplicationContext());
		server_type_TextView = (TextView) mLayoutview.findViewById(R.id.servertype_text);
		
		manualCaptureTimerEditText = (EditText) mLayoutview.findViewById(R.id.manual_capture_timer_edittext);

		usernameText = (TextView)mLayoutview.findViewById(R.id.usernameText);
		domainText = (TextView)mLayoutview.findViewById(R.id.domainText);
		passwordText = (TextView)mLayoutview.findViewById(R.id.passwordText);
		addEdittextListeners();
		resetlayout(mPrefUtils.isUsingKofax());

		mAdvOptionArrowImg = (ImageView)mLayoutview.findViewById(R.id.arrow);
		mUsekofaxSwitch = (Switch) mLayoutview.findViewById(R.id.usekofaxSwitch);
		mUsekofaxSwitch.setChecked(mPrefUtils.isUsingKofax());
		mlogoutTextview = (TextView) mLayoutview.findViewById(R.id.logout_TextView);
		
		Log.d(TAG, "mUsekofaxSwitch onCheckedChanged::"+mPrefUtils.isUsingKofax());
		// Attach a listener to check for changes in state.
		mUsekofaxSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_KOFAX_SERVER, true);
					//mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_ANONYMOUS, false);
					server_type_TextView.setText(ServerTypeList.get(0));
					mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
					if(mPrefUtils.isUsingAnonymousDemo()){
						mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));
					}
					else{
						mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
					}

				} else {
					mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_KOFAX_SERVER, false);
					String serverType = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE);
					Log.d(TAG, "selected server type :: " + serverType);
					if(mPrefUtils.isUsingAnonymous()){
						mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));
					}
					else{
						mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
					}
					if(serverType != null) {
						int selectionIndex = Globals.getServerTypeValue(serverType);
						server_type_TextView.setText(ServerTypeList.get(selectionIndex));
					}
				}
				resetlayout(isChecked);
				addEdittextListeners();
			}
		});
		
		mAnonymousSwitch = (Switch) mLayoutview.findViewById(R.id.anonymous_switch);
		mAnonymousSwitch.setChecked(mPrefUtils.isUsingAnonymous());
		
		
		mAnonymousSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (!mPrefUtils.isUsingKofax()) {
					if (isChecked) {
						mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_ANONYMOUS, true);
						LinearLayout myLayout = (LinearLayout) mLayoutview.findViewById(R.id.domainlayout);
						disable(myLayout);
						mHidelayout = (LinearLayout) mLayoutview.findViewById(R.id.usernamelayout);
						disable(mHidelayout);
						mHidelayout = (LinearLayout) mLayoutview.findViewById(R.id.passwordlayout);
						disable(mHidelayout);

						usernameeditText.setText("");
						passwordeditText.setText("");

						usernameText.setAlpha(0.5f);
						domainText.setAlpha(0.5f);
						passwordText.setAlpha(0.5f);

						mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));


					} else {
						mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_ANONYMOUS, false);
						LinearLayout myLayout = (LinearLayout) mLayoutview.findViewById(R.id.domainlayout);
						enable(myLayout);
						mHidelayout = (LinearLayout) mLayoutview.findViewById(R.id.usernamelayout);
						enable(mHidelayout);
						if (mUtilRoutines.checkInternet(getActivity())) {
							mHidelayout = (LinearLayout) mLayoutview.findViewById(R.id.passwordlayout);
							enable(mHidelayout);
							passwordText.setAlpha(1f);
						}

						String username, password;
						username = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_UNAME, Constants.STR_EMPTY);
						password = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_PASSWORD, Constants.STR_EMPTY);

						usernameeditText.setText(username);
						passwordeditText.setText(password);
						usernameText.setAlpha(1f);
						domainText.setAlpha(1f);


						mlogoutTextview.setText(getResources().getString(R.string.lbl_login));

					}
				}
				
			}
		});


		mAnonymousSwitchDemo = (Switch) mLayoutview.findViewById(R.id.anonymous_switch_demo);
		mAnonymousSwitchDemo.setChecked(mPrefUtils.isUsingAnonymousDemo());
		mAnonymousSwitchDemo.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				if (b) {
					mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_ANONYMOUS_DEMO, true);

/*
					usernameeditText.setText("");
					passwordeditText.setText("");*/

					usernameText.setAlpha(0.5f);
					domainText.setAlpha(0.5f);
					passwordText.setAlpha(0.5f);

					mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));



				} else {
					mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_ANONYMOUS_DEMO, false);


					String username,password;
				
					passwordText.setAlpha(1f);
					usernameText.setAlpha(1f);
					domainText.setAlpha(1f);


					mlogoutTextview.setText(getResources().getString(R.string.lbl_login));


				}
				addEdittextListeners();
			}

		});
		
		
		
		
		//setting filter to edittext of range 0-100
		InputFilter filter = new InputFilter() {
			int min = 0,max = 100;
		   	@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
		   		try {
		            int input = Integer.parseInt(dest.toString() + source.toString());
		            if(max > min ? input >= min && input <= max : input >= max && input <= min){
		                return null;
		            }
		        } catch (NumberFormatException nfe) { }     
		        return "";
			}
		};
		manualCaptureTimerEditText.setFilters(new InputFilter[]{filter});
		
		updateManualCaptureTimerEditText();		
		
		
		addSwitchListeners();

		updateServerTypeText();

		server_type_TextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openServerListScreen();
			}
		});

		mAdvlayout = (RelativeLayout)mLayoutview.findViewById(R.id.advancedoptionslayout);

		mAdvlayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mAdvanced_flag == false){
					mAdvOptionArrowImg.setBackgroundResource(R.drawable.arrow_down);
					mAdvanced_flag = true;
					resetadvancelayout(true);					
				}else{
					mAdvOptionArrowImg.setBackgroundResource(R.drawable.arrow_right);
					mAdvanced_flag = false;
					resetadvancelayout(false);
				}
			}
		});

		mAdvOptionArrowImg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mAdvanced_flag == false){
					mAdvOptionArrowImg.setBackgroundResource(R.drawable.arrow_down);
					mAdvanced_flag = true;
					resetadvancelayout(true);
				}else{
					mAdvOptionArrowImg.setBackgroundResource(R.drawable.arrow_right);
					mAdvanced_flag = false;
					resetadvancelayout(false);
				}
			}
		});

		mLogoutlayout = mLayoutview.findViewById(R.id.logoutlayout);
		mLogoutlayout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mPrefUtils.isUsingKofax()) {
					if (!mUtilRoutines.isEmailRegistered()) {
						mCustomDialog
								.show_popup_dialog(getActivity(),
										AlertType.INFO_ALERT,
										getResources().getString(R.string.lbl_no_email_registered),
										getResources().getString(R.string.toast_error_email_required),
										null, null,
										Messages.MESSAGE_DIALOG_EMAIL_INFORMATION,
										null,
										false);
						return;
					} // check if entered email is valid
					else if (!mUtilRoutines.validateEmail(mPrefUtils
							.getCurrentEmail())) {
						//Check if custom server email is not empty
						if (!(!mPrefUtils.isUsingKofax() && mPrefUtils.getCurrentEmail().length() == 0)) {
							mCustomDialog.show_popup_dialog(getActivity(), AlertType.ERROR_ALERT,
									getResources().getString(R.string.lbl_invalid_email),
									getResources().getString(R.string.toast_error_invalid_email),
									null, null,
									Messages.MESSAGE_DLALOG_INVALID_EMAIL,
									null,
									false);
							return;
						}
					}
				}
				boolean online_status = mUtilRoutines.checkInternet(getActivity().getApplicationContext());
				if(online_status){ 
				if(mServerMgr.isLoggedIn()){ 
					if(mServerMgr.isServerBusy()) {
						CustomDialog mCustomDialog =  CustomDialog.getInstance();
						mCustomDialog.show_popup_dialog(getActivity(), AlertType.INFO_ALERT, 
								getResources().getString(R.string.lbl_logout), 
								getResources().getString(R.string.toast_session_state_busy), 
								null, null,
								null,
								null,
								false);
					}
					else{
						CustomDialog mCustomDialog =  CustomDialog.getInstance();
						mCustomDialog.show_popup_dialog(getActivity(), AlertType.CONFIRM_ALERT, 
								getResources().getString(R.string.lbl_logout), 
								getResources().getString(R.string.error_msg_do_you_want_to_logout), 
								null, null,
								Messages.MESSAGE_DIALOG_LOGOUT_CONFIRMATION,
								HomeActivity.mHomeActivityHandler,
								false);
					}
				}else{
					Globals.gAppModeStatus =  Globals.AppModeStatus.ONLINE_MODE;
					if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
						Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_OFFLINE;
						Message msg = new Message();
						msg.what = Messages.MESSAGE_OFFLINE_LOGIN.ordinal();
						HomeActivity.mHomeActivityHandler.sendMessage(msg);
					}else{
					// incase of kofax user, check if email is registered
					if(mPrefUtils.isUsingKofax()){
						if(!mUtilRoutines.isEmailRegistered()) {
							mCustomDialog
							.show_popup_dialog(getActivity(),
									AlertType.INFO_ALERT,
									getResources().getString(R.string.lbl_no_email_registered),
									getResources().getString(R.string.toast_error_email_required),
									null, null,
									Messages.MESSAGE_DIALOG_EMAIL_INFORMATION,
									HomeActivity.mHomeActivityHandler,
									false);
							return;
						}
						else if(!mUtilRoutines.validateEmail(mPrefUtils
								.getCurrentEmail())) {
							mCustomDialog.show_popup_dialog(getActivity(), AlertType.ERROR_ALERT,
									getResources().getString(R.string.lbl_invalid_email),
									getResources().getString(R.string.toast_error_invalid_email),
									null, null,
									Messages.MESSAGE_DLALOG_INVALID_EMAIL,
									HomeActivity.mHomeActivityHandler,
									false);
							return;
						}
					}
					
					else {
						if((((EditText)mLayoutview.findViewById(R.id.urleditText)).getText().length() == 0 &&  (((EditText)mLayoutview.findViewById(R.id.serverhostnameeditText)).getText().length() == 0)) && mPrefUtils.isUsingAnonymous()){
							Toast.makeText(getActivity(), getResources().getString(R.string.toast_error_required_fields_empty), Toast.LENGTH_LONG).show();
							return;
						}else if(((EditText)mLayoutview.findViewById(R.id.urleditText)).getText().length() > 0) {
							if (!mUtilRoutines.checkForValidUrl(((EditText) mLayoutview.findViewById(R.id.urleditText)).getText().toString())) {
								Toast.makeText(
										getActivity().getApplicationContext(),
										getResources().getString(
												R.string.server_validation_message),
										Toast.LENGTH_SHORT).show();
								return;
							}
						}
						//if URL is empty, check if all mandatory fields(hostname, port, uname and password) are entered
						else if(!mPrefUtils.isUsingAnonymous() && (((EditText)mLayoutview.findViewById(R.id.urleditText)).getText().length() == 0) && (
								(((EditText)mLayoutview.findViewById(R.id.serverhostnameeditText)).getText().length() == 0) ||
								(((EditText)mLayoutview.findViewById(R.id.usernameeditText)).getText().length() == 0) ||
								(((EditText)mLayoutview.findViewById(R.id.passwordeditText)).getText().length() == 0))) {
							Toast.makeText(getActivity(), getResources().getString(R.string.toast_error_required_fields_empty), Toast.LENGTH_LONG).show();
							return;
						}
						// if email is registered, validate email
						if(mUtilRoutines.isEmailRegistered()) {
							if(!mUtilRoutines.validateEmail(mPrefUtils
									.getCurrentEmail())) {
								mCustomDialog.show_popup_dialog(getActivity(), AlertType.ERROR_ALERT,
										getResources().getString(R.string.lbl_invalid_email),
										getResources().getString(R.string.toast_error_invalid_email),
										null, null,
										Messages.MESSAGE_DLALOG_INVALID_EMAIL,
										HomeActivity.mHomeActivityHandler,
										false);
								return;
							}
						}
					}
					//initiate login.
					Message msg = new Message();
					msg.what = Messages.MESSAGE_LOGIN.ordinal();
					HomeActivity.mHomeActivityHandler.sendMessage(msg);
				}
			}
				}else if(!online_status && Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE){
					Message msg = new Message();
					msg.what = Messages.MESSAGE_DIALOG_LOGOUT_CONFIRMATION.ordinal();
					msg.arg1 = HomeActivity.RESULT_OK;
					HomeActivity.mHomeActivityHandler.sendMessage(msg);
					Globals.gAppModeStatus = Globals.AppModeStatus.OFFLINE_MODE;
				}
				else {
					Globals.gAppModeStatus = Globals.AppModeStatus.OFFLINE_MODE;
					LinearLayout ll = (LinearLayout)v;
					TextView tView =  (TextView)ll.getChildAt(1);
					if(tView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.lbl_logout)) || tView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.lbl_disconnect))){
						Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGOUT_OFFLINE;						
					}else if(tView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.lbl_login)) || tView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.lbl_connect))){
						mUtilRoutines.updateURLDetails(mPrefUtils.getCurrentUrl());
						List<UserInformationEntity> list =  mDBManager.getUserInformationFromDetails(getActivity().getApplicationContext(),
								mUtilRoutines.getUser(),
								mPrefUtils.getCurrentHostname(),mPrefUtils.getCurrentServerType());
						if(list != null && list.size() > 0 && mUtilRoutines.isLastLoggedInUser(mPrefUtils)){
							mDBManager.setUserInformationEntity(list.get(0));
							Globals.gAppLoginStatus = Globals.AppLoginStatus.LOGIN_OFFLINE;
							disablePasswordFieldOfflineMode();
						}else{
							  if(!mPrefUtils.isUsingAnonymousDemo() && !mPrefUtils.isUsingAnonymous() && ((EditText)mLayoutview.findViewById(R.id.usernameeditText)).getText().length() == 0){
								  mCustomDialog.show_popup_dialog(getActivity(), AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.error_lbl_user_name_empty), null, null, null, null, false);
							  }else {
								  String lastLoggedUser = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_USER, null);
								  if(lastLoggedUser == null || lastLoggedUser.isEmpty()){
									  mCustomDialog.show_popup_dialog(getActivity(), AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.app_msg_invalid_only_once_session_msg), null, null, null, null, false);
								  }else {
									  //  popup user not registered error dialog
									  mCustomDialog.show_popup_dialog(getActivity(), AlertType.INFO_ALERT, getResources().getString(R.string.lbl_login_failed_error), getResources().getString(R.string.app_msg_invalid_session_msg), null, null, null, null, false);
								  }
							  }
							return;
						}
						
					}
					Message msg = new Message();
					msg.what = Messages.MESSAGE_OFFLINE_LOGIN.ordinal();
					HomeActivity.mHomeActivityHandler.sendMessage(msg);
				}
			}
		});


		mlogoutImageview = (ImageView)mLayoutview.findViewById(R.id.logout_imageView);
		if(mServerMgr.isLoggedIn()){
			mlogoutTextview.setText(getResources().getString(R.string.lbl_logout));   
			mlogoutImageview.setImageResource(R.drawable.logout);
			if(mPrefUtils.isUsingAnonymous()){
				mlogoutTextview.setText(getResources().getString(R.string.lbl_disconnect));
			}
			else{
				mlogoutTextview.setText(getResources().getString(R.string.lbl_logout));
			}
		}else{
			mlogoutTextview.setText(getResources().getString(R.string.lbl_login));     
			mlogoutImageview.setImageResource(R.drawable.login);
			if(mPrefUtils.isUsingAnonymous()){
				mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));
			}else{
				mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
			}
			
		}
		return mLayoutview;
	}

	@SuppressLint("NewApi")
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		if(mPrefUtils.isUsingAnonymous() && !mPrefUtils.isUsingKofax()){
			LinearLayout myLayout1 = (LinearLayout) mLayoutview.findViewById(R.id.domainlayout);
			disable(myLayout1);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.usernamelayout);
			disable(mHidelayout);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.passwordlayout);
			disable(mHidelayout);

			usernameeditText.setText("");
			passwordeditText.setText("");

			usernameText.setAlpha(0.5f);
			domainText.setAlpha(0.5f);
			passwordText.setAlpha(0.5f);
		
		}
		// email field is mandatory if using kofax server
		if ((!mServerMgr.isLoggedIn()) && (mPrefUtils.isUsingKofax() == true) && (mPrefUtils.getPrefValueString(mPrefUtils.KEY_KFX_EMAIL).isEmpty())) {
			Log.e(TAG, "Email Empty!!!!!!");

			EditText emaileditText = (EditText)mLayoutview.findViewById(R.id.emaileditText);
			emaileditText.requestFocus();
			emaileditText.setSelection(0);
			emaileditText.setCursorVisible(true);
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void onDestroy() {
		super.onDestroy();
		mPrefUtils = null;
		mUtilRoutines = null; 
		mCustomDialog = null;
		mServerMgr = null;
	}

	/**
	 * A Function used to update the server type
	 */

	public void updateServerTypeName(){
		String serverType = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE);
		Log.d(TAG, "selected server type :: " + serverType);         
		if(serverType != null) {
			if(serverType.equals(Globals.serverType.KFS.name())){
				server_type_TextView.setText(ServerTypeList.get(0));
			}else if(serverType.equals(Globals.serverType.KTA.name())){
				server_type_TextView.setText(ServerTypeList.get(1));
			}else if(serverType.equals(Globals.serverType.KTA_AZURE.name())){
				server_type_TextView.setText(ServerTypeList.get(2));
			}

		}
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void openServerListScreen(){
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
		Intent intent = new Intent(getActivity().getApplicationContext(), ServerTypeList.class);
		String serverType = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE);
		if(serverType != null) {
			if(serverType.equals(Globals.serverType.KFS.name())){
				intent.putExtra(Constants.STR_SERVER_TYPE,0);
			}else if(serverType.equals(Globals.serverType.KTA.name())){
				intent.putExtra(Constants.STR_SERVER_TYPE,1);
			}else if(serverType.equals(Globals.serverType.KTA_AZURE.name())){
				intent.putExtra(Constants.STR_SERVER_TYPE,2);
			}
		}		
		getActivity().startActivityForResult(intent,Globals.RequestCode.SERVER_TYPE.ordinal());
	}
	
	private void addEdittextListeners(){
		serverhostnameeditText = (EditText)mLayoutview.findViewById(R.id.serverhostnameeditText);

		String serverhostname = null;

		serverhostname = mPrefUtils.getCurrentHostname();

		serverhostnameeditText.setText(serverhostname);
		serverhostnameeditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = serverhostnameeditText.getText().toString();
				if(mPrefUtils.isUsingKofax()){
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_HOSTNAME,str);
				}else{
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_HOSTNAME,str);
				}               
			}
		}); 

		final EditText nicknameeditText = (EditText)mLayoutview.findViewById(R.id.nicknameeditText);
		String nickname = null;

		if (mPrefUtils.isUsingKofax()) {
			nickname = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_NICKNAME, mPrefUtils.DEF_KFX_NICKNAME);
		}
		else {
			nickname = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_NICKNAME, Constants.STR_EMPTY);
		}
		nicknameeditText.setText(nickname);
		Log.e(TAG, "nickname ==> " + nickname);

		nicknameeditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = nicknameeditText.getText().toString();
				if (mPrefUtils.isUsingKofax()) {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_NICKNAME,str);
				}
				else {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_NICKNAME,str);
				}
			}
		}); 

		porteditText = (EditText)mLayoutview.findViewById(R.id.porteditText);
		porteditText.setText(mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PORT));
		porteditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = porteditText.getText().toString();
				if(str != null) {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_PORT,str);
				}
			}
		}); 

		domaineditText = (EditText)mLayoutview.findViewById(R.id.domaineditText);
		String domain = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_DOMAIN,mPrefUtils.DEF_USR_DOMAIN);
		domaineditText.setText(domain);
		if(!mPrefUtils.isUsingAnonymous()){
			domainText.setAlpha(1f);
		}

		domaineditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = domaineditText.getText().toString();
				mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_DOMAIN,str);
			}
		}); 


		usernameeditText = (EditText)mLayoutview.findViewById(R.id.usernameeditText);
		String username;
		if (mPrefUtils.isUsingKofax()) {
			username = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_UNAME, mPrefUtils.DEF_KFX_UNAME);
			usernameText.setAlpha(1f);
			if(mPrefUtils.isUsingAnonymousDemo()){
				username = "";
				usernameText.setAlpha(0.5f);
			}
		}
		else {
			username = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_UNAME,Constants.STR_EMPTY);
			usernameText.setAlpha(1f);
			if(mPrefUtils.isUsingAnonymous()){
				username = "";
				usernameText.setAlpha(0.5f);
			}
		}
		usernameeditText.setText(username);
		Log.e(TAG, "username ==> " + username);

		usernameeditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = usernameeditText.getText().toString();
				if (mPrefUtils.isUsingKofax() ) {
					if(!mPrefUtils.isUsingAnonymousDemo()) {
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_UNAME, str);
					}

				}
				else {
					if(!mPrefUtils.isUsingAnonymous()) {
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_UNAME, str);
					}
				}
			}
		}); 

		passwordeditText = (EditText)mLayoutview.findViewById(R.id.passwordeditText);
		String password = null;
		if (mPrefUtils.isUsingKofax()) {
			password = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_PASSWORD, mPrefUtils.DEF_KFX_PASSWORD);
			passwordText.setAlpha(1f);
			if(mPrefUtils.isUsingAnonymousDemo()){
				password = "";
				passwordText.setAlpha(0.5f);
			}
		}
		else {
			password = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_PASSWORD, Constants.STR_EMPTY);
			passwordText.setAlpha(1f);
			if(mPrefUtils.isUsingAnonymous()){
				password = "";
				passwordText.setAlpha(0.5f);
			}
		}
		passwordeditText.setText(password);
		Log.e(TAG, "password ==> " + password);

		passwordeditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = passwordeditText.getText().toString();
				if (mPrefUtils.isUsingKofax()) {
					if(!mPrefUtils.isUsingAnonymousDemo()) {
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_PASSWORD, str);
					}
				}
				else {
					if(!mPrefUtils.isUsingAnonymous()) {
						mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_PASSWORD, str);
					}
				}
			}
		}); 

		emaileditText = (EditText)mLayoutview.findViewById(R.id.emaileditText);
		String email = null;
		if (mPrefUtils.isUsingKofax()) {
			email = mPrefUtils.getPrefValueString(mPrefUtils.KEY_KFX_EMAIL);
		}
		else {
			email = mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_EMAIL);
		}
		emaileditText.setText(email);
		Log.e(TAG, "email ==> " + email);

		emaileditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}

			@Override
			public void afterTextChanged(Editable arg0) {

				String str = emaileditText.getText().toString();
				if (mPrefUtils.isUsingKofax()) {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_EMAIL,str);
				}
				else {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_EMAIL,str);
				}
			}
		});


		urleditText = (EditText)mLayoutview.findViewById(R.id.urleditText);
		String url;
		if (mPrefUtils.isUsingKofax()) {
			url = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_URL, mPrefUtils.DEF_KFX_URL);
		}
		else {
			url = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_URL, Constants.STR_EMPTY);
		}

		urleditText.setText(url);
		Log.e(TAG, "url ==> " + url);

		urleditText.addTextChangedListener(new TextWatcher(){
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				String str = urleditText.getText().toString();
				if (mPrefUtils.isUsingKofax()) {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_KFX_URL,str);
				}
				else {
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_URL,str);
				}
			}
		}); 
		
		manualCaptureTimerEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
			}
			
			@Override
			public void afterTextChanged(Editable arg0) {
				String timerString = manualCaptureTimerEditText.getText().toString().trim();
				int time = 0;
				try{
					time = Integer.parseInt(timerString);
				}catch(NumberFormatException e){
					time = -1;
				}
				if(time != -1){
					mPrefUtils.putPrefValueInt(mPrefUtils.KEY_MANUAL_CAPTURE,time);
//					manualCaptureTimerEditText.setText(time+"");
				}else{
					mPrefUtils.putPrefValueInt(mPrefUtils.KEY_MANUAL_CAPTURE,0);
//					manualCaptureTimerEditText.setText(mPrefUtils.DEF_MANUAL);
				}
			}
		});

	}

	@SuppressLint("NewApi")
	private void addSwitchListeners(){

		Boolean ssl =  mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_USR_SSL, false);

		sslSwitch = (Switch) mLayoutview.findViewById(R.id.sslswitch);
		sslSwitch.setChecked(ssl);
		// attach a listener to check for changes in state
		sslSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Log.d(TAG, "onCheckedChanged::"+isChecked);
				if (isChecked) {
					mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USR_SSL, true);
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_PORT,mPrefUtils.DEF_SSL_PORT);
					porteditText.setText(String.valueOf(mPrefUtils.DEF_SSL_PORT));
				} else {
					mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USR_SSL, false);
					mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_PORT,mPrefUtils.DEF_NON_SSL_PORT);
					porteditText.setText(String.valueOf(mPrefUtils.DEF_NON_SSL_PORT));
				}

			}
		});

		//for low-end devices (BACKGROUND_IMAGE_PROCESSING is false), do not show quick-preview option as the preview should always be ON for low-end devices.
		if(Constants.BACKGROUND_IMAGE_PROCESSING == false) {
			mLayoutview.findViewById(R.id.previewlayout).setVisibility(View.GONE);
			mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_QUICK_PREVIEW, true);
		}
		else {
			Boolean previewSwitchvalue =  mPrefUtils.sharedPref.getBoolean(mPrefUtils.KEY_QUICK_PREVIEW, true);
			Switch previewSwitch = (Switch) mLayoutview.findViewById(R.id.quickpreviewSwitch);

			previewSwitch.setChecked(previewSwitchvalue);

			// attach a listener to check for changes in state
			previewSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {

					if (isChecked) {
						mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_QUICK_PREVIEW, true);

					} else {
						mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_QUICK_PREVIEW, false);
					}

				}
			});
		}
	}

	private void resetadvancelayout(Boolean status){
		if(status){
			//for low-end devices (BACKGROUND_IMAGE_PROCESSING is false), do not show quick-preview option as the preview should always be ON for low-end devices.
			if(Constants.BACKGROUND_IMAGE_PROCESSING) {
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.previewlayout);
				mHidelayout.setVisibility(View.VISIBLE);
			}
			
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.manuallayout);
            mHidelayout.setVisibility(View.VISIBLE);
			
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.urllayout);
			mHidelayout.setVisibility(View.VISIBLE);

		}else{
			//for low-end devices (BACKGROUND_IMAGE_PROCESSING is false), do not show quick-preview option as the preview should always be ON for low-end devices.
			if(Constants.BACKGROUND_IMAGE_PROCESSING) {
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.previewlayout);
				mHidelayout.setVisibility(View.GONE);
			}
			
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.manuallayout);
            mHidelayout.setVisibility(View.GONE);
			
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.urllayout);
			mHidelayout.setVisibility(View.GONE);
		}
	}

	@SuppressLint("ResourceAsColor")
	private void resetlayout(Boolean isUsingKofax){
		if(isUsingKofax){
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.serverhostnamelayout);
			mHidelayout.setVisibility(View.GONE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.ssllayout);
			mHidelayout.setVisibility(View.GONE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.anonymous_layout);
			mHidelayout.setVisibility(View.GONE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.anonymous_layout_demo);
			mHidelayout.setVisibility(View.VISIBLE);

			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.portlayout);
			mHidelayout.setVisibility(View.GONE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.domainlayout);
			mHidelayout.setVisibility(View.GONE);

			/* When 'Use-kofax-server' is selected, disable all other options except email and nickname. */
			LinearLayout myLayout = (LinearLayout) mLayoutview.findViewById(R.id.layout_container);
			disable(myLayout);

			usernameText.setAlpha(1f);
			passwordText.setAlpha(1f);


			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.anonymous_layout_demo);
			enable(mHidelayout);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.nicknamelayout);
			enable(mHidelayout);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.urllayout);
			urleditText.setFocusableInTouchMode(false);
			urleditText.setFocusable(false);
			disable(mHidelayout);

			/* hide arrow image displayed beside server-type */
			server_type_TextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,0);
		}else{
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.serverhostnamelayout);
			mHidelayout.setVisibility(View.VISIBLE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.ssllayout);
			mHidelayout.setVisibility(View.VISIBLE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.anonymous_layout);
			mHidelayout.setVisibility(View.VISIBLE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.anonymous_layout_demo);
			mHidelayout.setVisibility(View.GONE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.portlayout);
			mHidelayout.setVisibility(View.VISIBLE);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.domainlayout);
			mHidelayout.setVisibility(View.VISIBLE);

			/* enable all fields which were disabled when use-kofax option was selected */
			LinearLayout myLayout = (LinearLayout) mLayoutview.findViewById(R.id.layout_container);
			enable(myLayout);
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.urllayout);
			urleditText.setFocusableInTouchMode(true);
			urleditText.setFocusable(true);
			enable(mHidelayout);
           if(!mUtilRoutines.checkInternet(getActivity())) {
			   mHidelayout = (LinearLayout) mLayoutview.findViewById(R.id.passwordlayout);
			   disable(mHidelayout);
			   passwordText.setAlpha(0.5f);
		   }
			/* show arrow image displayed beside server-type */
			server_type_TextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.arrow_right,0);
			
			if(mPrefUtils.isUsingAnonymous()){
				LinearLayout myLayout1 = (LinearLayout) mLayoutview.findViewById(R.id.domainlayout);
				disable(myLayout1);
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.usernamelayout);
				disable(mHidelayout);
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.passwordlayout);
				disable(mHidelayout);

				usernameeditText.setText("");
				passwordeditText.setText("");

				usernameText.setAlpha(0.5f);
				domainText.setAlpha(0.5f);
				passwordText.setAlpha(0.5f);
				
			}
			
			
		}
	}

	public void updateUI(SessionState sessionstate){
		mUsekofaxSwitch.setChecked(mPrefUtils.isUsingKofax());
		mAnonymousSwitch.setChecked(mPrefUtils.isUsingAnonymous());
		if(sessionstate == SessionState.SESSION_LOGGING_OUT){
			mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
			mlogoutImageview.setImageResource(R.drawable.login);
//			usernameeditText.setText("");
//			passwordeditText.setText("");
		
			if(mPrefUtils.isUsingKofax()){
				if(mPrefUtils.isUsingAnonymousDemo()){
					mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));
				}
				else{
					mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
				}
			}
			else{
				if(mPrefUtils.isUsingAnonymous()){
					mlogoutTextview.setText(getResources().getString(R.string.lbl_connect));
				}
				else{
					mlogoutTextview.setText(getResources().getString(R.string.lbl_login));
				}

			}
			enableFieldsAfterLogout();
		}else if(sessionstate == SessionState.SESSION_LOGGED_IN){
			mlogoutTextview.setText(getResources().getString(R.string.lbl_logout));
			mlogoutImageview.setImageResource(R.drawable.logout);
		
			if(mPrefUtils.isUsingKofax()){
				if(mPrefUtils.isUsingAnonymousDemo()){
					mlogoutTextview.setText(getResources().getString(R.string.lbl_disconnect));
				}
				else{
					mlogoutTextview.setText(getResources().getString(R.string.lbl_logout));
				}
			}
			else{
				if(mPrefUtils.isUsingAnonymous()){
					mlogoutTextview.setText(getResources().getString(R.string.lbl_disconnect));
				}
				else{
					mlogoutTextview.setText(getResources().getString(R.string.lbl_logout));
				}

			}
			disableFieldsAfterLogin();
		}
	}
	
	public void updateFields() {
		serverhostnameeditText = (EditText)mLayoutview.findViewById(R.id.serverhostnameeditText);
		serverhostnameeditText.setText(mPrefUtils.getCurrentHostname());
		
		porteditText = (EditText)mLayoutview.findViewById(R.id.porteditText);
		porteditText.setText(mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PORT));

		String domain = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_DOMAIN, mPrefUtils.DEF_USR_DOMAIN);
		domaineditText = (EditText)mLayoutview.findViewById(R.id.domaineditText);
		domaineditText.setText(domain);
		
		String username = null;
		String password = null;
		String email = null;
		String url = null;
		
		if(mPrefUtils.isUsingKofax()){
			username = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_UNAME,mPrefUtils.DEF_KFX_UNAME);
			password = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_PASSWORD,mPrefUtils.DEF_KFX_PASSWORD);
			email = mPrefUtils.getPrefValueString(mPrefUtils.KEY_KFX_EMAIL);
			url = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_URL,mPrefUtils.DEF_KFX_URL);
			if(mPrefUtils.isUsingAnonymousDemo()){
				username = "";
				password = "";
			}
		}else{
			username = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_UNAME,Constants.STR_EMPTY);
			password = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_PASSWORD, Constants.STR_EMPTY);
			email = mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_EMAIL);
			url = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_URL, Constants.STR_EMPTY);

			if(mPrefUtils.isUsingAnonymous()){
				username = "";
				password = "";
			}
		}
		usernameeditText = (EditText)mLayoutview.findViewById(R.id.usernameeditText);
		usernameeditText.setText(username);


		passwordeditText = (EditText)mLayoutview.findViewById(R.id.passwordeditText);
		passwordeditText.setText(password);
		
		emaileditText = (EditText)mLayoutview.findViewById(R.id.emaileditText);
		emaileditText.setText(email);
		
		 
		urleditText = (EditText)mLayoutview.findViewById(R.id.urleditText);
		urleditText.setText(url);
			
		sslSwitch.setChecked((Boolean) mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_USR_SSL, false));
		mAnonymousSwitch.setChecked((Boolean) mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_USE_ANONYMOUS, false));
		updateServerTypeText();
		
	}

	/**
	 * Method to update the edittext with manual capture time
	 *
	 */
	public void updateManualCaptureTimerEditText(){
		if (manualCaptureTimerEditText != null) {
			int time = mPrefUtils.getPrefValueInt(
					mPrefUtils.KEY_MANUAL_CAPTURE, mPrefUtils.DEF_MANUAL_TIME);
			manualCaptureTimerEditText.setText(time + "");
		}
	}
	
	public void focusEmailField(){
		EditText emailEditText = (EditText)mLayoutview.findViewById(R.id.emaileditText);

		setKeyboardFocus(emailEditText);

	/*	if(emailEditText != null){
			emailEditText.requestFocus();
			//emailEditText.setCursorVisible(true);



			if(emailEditText.getText().length() > 0){
				emailEditText.setSelection(emailEditText.getText().length());
			}
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(emailEditText, InputMethodManager.SHOW_IMPLICIT);
		}*/
	}


	public static void setKeyboardFocus(final EditText primaryTextField) {
		(new Handler()).postDelayed(new Runnable() {
			public void run() {
				primaryTextField.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
				primaryTextField.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
			}
		}, 420);
	}

	private void disableFieldsAfterLogin(){
		LinearLayout myLayout = (LinearLayout) mLayoutview.findViewById(R.id.usekofaxlayout);
		disable(myLayout);
		 myLayout = (LinearLayout) mLayoutview.findViewById(R.id.anonymous_layout_demo);
		disable(myLayout);
		myLayout = (LinearLayout) mLayoutview.findViewById(R.id.emaillayout);
		disable(myLayout);
		myLayout = (LinearLayout) mLayoutview.findViewById(R.id.urllayout);
		urleditText.setFocusableInTouchMode(false);
		urleditText.setFocusable(false);
		disable(myLayout);
		if(!mPrefUtils.isUsingKofax()){
			myLayout = (LinearLayout) mLayoutview.findViewById(R.id.layout_container);
			disable(myLayout);
            if(!mPrefUtils.isUsingAnonymous()) {
				usernameText.setAlpha(1f);
				passwordText.setAlpha(1f);
			}
			myLayout = (LinearLayout) mLayoutview.findViewById(R.id.serverhostnamelayout);
			disable(myLayout);
		}else{
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.nicknamelayout);
			disable(mHidelayout);
		}	
	}

	private void enableFieldsAfterLogout(){
		LinearLayout myLayout = (LinearLayout) mLayoutview.findViewById(R.id.usekofaxlayout);
		enable(myLayout);
		myLayout = (LinearLayout) mLayoutview.findViewById(R.id.anonymous_layout_demo);
		enable(myLayout);
		myLayout = (LinearLayout) mLayoutview.findViewById(R.id.emaillayout);
		enable(myLayout);
		if(!mPrefUtils.isUsingKofax()){
			myLayout = (LinearLayout) mLayoutview.findViewById(R.id.layout_container);
			enable(myLayout);
			myLayout = (LinearLayout) mLayoutview.findViewById(R.id.serverhostnamelayout);
			enable(myLayout);
			myLayout = (LinearLayout) mLayoutview.findViewById(R.id.urllayout);
			urleditText.setFocusableInTouchMode(true);
			urleditText.setFocusable(true);
			enable(myLayout);
			if(!mUtilRoutines.checkInternet(getActivity())){
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.passwordlayout);
				disable(mHidelayout);
				passwordText.setAlpha(0.5f);
			}
			if(mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_EXPLICIT_LOGOUT,true) ){
				if(!mPrefUtils.isUsingKofax()) {
					usernameeditText.setText("");
					passwordeditText.setText("");
				}
			}


			if(mPrefUtils.isUsingAnonymous()){
				LinearLayout myLayout1 = (LinearLayout) mLayoutview.findViewById(R.id.domainlayout);
				disable(myLayout1);
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.usernamelayout);
				disable(mHidelayout);
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.passwordlayout);
				disable(mHidelayout);


				usernameeditText.setText("");
				passwordeditText.setText("");


				usernameText.setAlpha(0.5f);
				domainText.setAlpha(0.5f);
				passwordText.setAlpha(0.5f);
			
			}
		}else{
			mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.nicknamelayout);
			enable(mHidelayout);
		}
	}

	private void disable(ViewGroup layout) {
		layout.setEnabled(false);
		for (int i = 0; i < layout.getChildCount(); i++) {
			View child = layout.getChildAt(i);
			if (child instanceof LinearLayout || child instanceof RelativeLayout) {
				disable((ViewGroup) child);
			}
			else {
				child.setEnabled(false);
			}
		}
	}

	private void enable(ViewGroup layout) {
		layout.setEnabled(false);
		for (int i = 0; i < layout.getChildCount(); i++) {
			View child = layout.getChildAt(i);
			if (child instanceof LinearLayout || child instanceof RelativeLayout) {
				enable((ViewGroup) child);
			} else {
				child.setEnabled(true);
			}
		}
	}
	
	private void updateServerTypeText(){
		if(!mPrefUtils.isUsingKofax()){
			//update serverType
			updateServerTypeName();
		}else{
			server_type_TextView.setText(ServerTypeList.get(0));
		}
	}

	public void disablePasswordFieldOfflineMode(){
		if(!mUtilRoutines.checkInternet(getActivity()) &&( Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE ||
				Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE) ){
			mLogoutlayout.setClickable(false);
			mLogoutlayout.setAlpha(0.5f);
		}else{
			mLogoutlayout.setClickable(true);
			mLogoutlayout.setAlpha(1f);
		}
		if(!mPrefUtils.isUsingKofax() && !(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE ||
				Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE)){
			if(mUtilRoutines.checkInternet(getActivity()) && !mPrefUtils.isUsingAnonymous()){
				
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.passwordlayout);
				enable(mHidelayout);
				passwordText.setAlpha(1f);
			}else{
				
				mHidelayout = (LinearLayout)mLayoutview.findViewById(R.id.passwordlayout);
				disable(mHidelayout);
				passwordText.setAlpha(0.5f);
			}
		}
	}
}
