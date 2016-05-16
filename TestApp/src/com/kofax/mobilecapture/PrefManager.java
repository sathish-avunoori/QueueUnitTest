// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kofax.kmc.kui.uicontrols.data.Flash;
import com.kofax.mobilecapture.utilities.Globals;

/// The PrefManager takes care of application wide preferences.  

public class PrefManager {

	private static volatile PrefManager pSelf = null;

	String TAG = PrefManager.class.getSimpleName();

	/* Preferences Keys Declarations */
	public final String KEY_SUPERUSER = "superuser";

	public final  String KEY_USE_KOFAX_SERVER = "use kofax server";
	
	public final  String KEY_USE_ANONYMOUS = "use anonymous login";
	public final  String KEY_USE_ANONYMOUS_DEMO = "use anonymous demo login";

	public final String KEY_KFX_HOSTNAME = "usr_hostname";
	public final String KEY_KFX_SERVER_TYPE = "usr_servertype";
	public final String KEY_KFX_NICKNAME = "usr_nickname";
	public final String KEY_KFX_UNAME = "usr_username";
	public final String KEY_KFX_PASSWORD = "usr_password";
	public final String KEY_KFX_EMAIL = "usr_email";
	public final String KEY_KFX_URL = "usr_url";

	public final String KEY_USR_HOSTNAME = "kfx_hostname";
	public final String KEY_USR_SERVER_TYPE = "kfx_servertype";
	public final String KEY_USR_NICKNAME = "kfx_nickname";
	public final String KEY_USR_SSL = "kfx_ssl";
	public final String KEY_USR_PORT = "kfx_port";
	public final String KEY_USR_DOMAIN = "kfx_domain";
	public final String KEY_USR_UNAME = "kfx_username";
	public final String KEY_USR_PASSWORD = "kfx_password";
	public final String KEY_USR_EMAIL = "kfx_email";
	public final String KEY_USR_URL = "kfx_url";

	public final String KEY_SENSITIVITY = "sensitivity";
	public final String KEY_QUICK_PREVIEW = "quick preview";
	public final String KEY_MANUAL_CAPTURE = "manual_capture_time";
	public final String KEY_INDICATOR_COLOR = "indicator color";
	public final String KEY_APP_LAUNCHED_FROM_URL = "app launched from url";

	public final String KEY_APP_LAST_lOGGED_USER = "last loggedin user";
	public final String KEY_APP_LAST_lOGGED_SERVER_TYPE = "last loggedin servertype";
	public final String KEY_APP_LAST_lOGGED_HOSTNAME = "last loggedin hostname";

	public final String KEY_FLASH = "flash";
	public final String KEY_IMPORT_DATA_DONT_SHOW_AGAIN = "import_data_dont_show_again";

	public final String KEY_EXPLICIT_LOGOUT = "explicit_logout";

	/* Preferences Defaults Values Declarations */
	public final Boolean DEF_USE_KOFAX = true;
	public final Boolean DEF_USE_ANONYMOUS = false;

	public final String DEF_SUPERUSER = "Kofax";
	public final boolean DEF_EXPLICIT_LOGOUT = true;

	public final String DEF_KFX_SERVER_TYPE = "KFS";
	public final String DEF_KFX_NICKNAME = "Kofax";
	public final String DEF_KFX_UNAME = "KMC241";     //TODO:Change value to "kofaxuser" before submit to store
	public final String DEF_KFX_PASSWORD = "K00fax!!"; //TODO:Change value to "capture" before submit to store
	public final String DEF_KFX_EMAIL = "user@kofax.com";
	public final String DEF_KFX_URL = "https://mobile.kofax.com:8443/KFS/axis2/services/";

	public final int DEF_SENSITIVITY = 95;
	public final int DEF_FLASH = Flash.AUTO.ordinal();
	public final boolean DEF_PREVIEW = true;
	public final int DEF_MANUAL_TIME = 10;
	public final boolean DEF_CUTOMURL = false;

	public final String DEF_USR_SERVER_TYPE = "KFS";
	public final String DEF_USR_HOSTNAME = "";
	public final String DEF_USR_NICKNAME = "";
	public final boolean DEF_USR_SSL = false;

	public final String DEF_USR_DOMAIN = "";
	public final String DEF_USR_UNAME = "";
	public final String DEF_USR_PASSWORD = "";
	public final String DEF_USR_EMAIL = "";
	public final String DEF_USR_URL = "";

	public final String DEF_SSL_PORT = "443";
	public final String DEF_NON_SSL_PORT = "80";

	public SharedPreferences sharedPref = null;

	private SharedPreferences.Editor editor = null;
	private PrefManager() {
	}

	// - public methods
    //! The factory method.
	public static PrefManager getInstance() {
		if (pSelf == null) {
			synchronized (PrefManager.class) {
				if (pSelf == null) {
					pSelf = new PrefManager();		
				}
			}
		}
		return pSelf;
	}
	
	//!Know the application stop in background.if shared preference is null then application force stop by OS.
	public boolean isAppBackgroundForceStop(){
		if(null == sharedPref){
			return true;
		}
		return false;
	}

    //! Initialize SharedPreferences and SharedPreferences.Editor
	public void init(Context context) {
		sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		editor = sharedPref.edit();
	}

	/* Application Preference Name */

	//! Generic function to get boolean value for specified key.
	public boolean getPrefValueBoolean(String mKey, boolean defValue) {
		return sharedPref.getBoolean(mKey, defValue);
	}
	
	//! Generic function to int boolean value for specified key.
	public int getPrefValueInt(String mKey, int def) {
		return sharedPref.getInt(mKey, def);
	}

	//! Generic function to string boolean value for specified key.
	public String getPrefValueString(String mKey) {
		return sharedPref.getString(mKey, "");
	}

	//! Generic function to set boolean value for specified key.
	public void putPrefValueBoolean(String mKey, boolean mValue) {
		editor.putBoolean(mKey, mValue);
		editor.commit();
	}
	
	//! Generic function to set int value for specified key.
	public void putPrefValueInt(String mKey, int mValue) {
		editor.putInt(mKey, mValue);
		editor.commit();
	}
	
	//! Generic function to set string value for specified key.
	public void putPrefValueString(String mKey, String mValue) {
		editor.putString(mKey, mValue);
		editor.commit();
	}

	//! Get url set in setting screen.
	/**
	 * 
	 * @return server url
	 */
	public String getCurrentUrl() {
		String serverUrl = null;
		if (isUsingKofax()) {
			serverUrl = getPrefValueString(KEY_KFX_URL);
		} else {
			serverUrl = getPrefValueString(KEY_USR_URL);
		}
		return serverUrl;
	}

	//! Get hostname used for currently logged in user.
	/**
	 * @return If using Kofax demo server, demo server hostname is returned. Else hostname of non-demo server is returned.
	 */
	public String getCurrentHostname() {
		String hostname = null;
		if (isUsingKofax()) {
			hostname = getPrefValueString(KEY_KFX_HOSTNAME);
		} else {
			hostname = getPrefValueString(KEY_USR_HOSTNAME);
		}
		return hostname;
	}

	//! Get username of currently logged in user.
	/**
	 * @return If using Kofax demo server, demo server username is returned. Else username of non-demo server is returned.
	 */
	public String getCurrentUser() {
		String user = null;
		if (isUsingKofax()) {
			user = getPrefValueString(KEY_KFX_UNAME);
		} else {
			user = getPrefValueString(KEY_USR_UNAME);
		}
		return user;
	}

	//! Get password of currently logged in user.
	/**
	 * @return If using Kofax demo server, demo server password is returned. Else password of non-demo server is returned.
	 */
	public String getCurrentPassword() {
		String pwd = null;
		if (isUsingKofax()) {
			pwd = getPrefValueString(KEY_KFX_PASSWORD);
		} else {
			pwd = getPrefValueString(KEY_USR_PASSWORD);
		}
		return pwd;
	}

	//! Get email of currently logged in user.
	/**
	 * @return If using Kofax demo server, demo server email is returned. Else email of non-demo server is returned.
	 */
	public String getCurrentEmail() {
		String email = null;
		if (isUsingKofax()) {
			email = getPrefValueString(KEY_KFX_EMAIL);
		} else {
			email = getPrefValueString(KEY_USR_EMAIL);
		}
		return email;
	}

	//! Get server-type of currently logged in user.
	/**
	 * @return If using Kofax demo server, demo server-type is returned. Else selected server-type of non-demo server is returned.
	 */
	public String getCurrentServerType() {
		String serverType = null;
		if (isUsingKofax()) {
			serverType = getPrefValueString(KEY_KFX_SERVER_TYPE);
		} else {
			serverType = getPrefValueString(KEY_USR_SERVER_TYPE);
		}
		return serverType;
	}

	//! Checks if application is logged in to Kofax demo server.
	/**
	 * @return true if using Kofax demo server, false otherwise.
	 */
	public boolean isUsingKofax() {
		Boolean usekofax = getPrefValueBoolean(KEY_USE_KOFAX_SERVER,
				DEF_USE_KOFAX);
		return usekofax;
	}

	//! Set default preference values.
	public void setDefaultsharedPreference() {
		putPrefValueString(KEY_SUPERUSER, DEF_SUPERUSER); //KEY_SUPERUSER is user to mark that preferences are no longer empty.
		
		putPrefValueBoolean(KEY_EXPLICIT_LOGOUT, DEF_EXPLICIT_LOGOUT);

		putPrefValueString(KEY_KFX_HOSTNAME, DEF_USR_HOSTNAME);

		putPrefValueString(KEY_USR_DOMAIN, DEF_USR_DOMAIN);

		putPrefValueString(KEY_USR_PORT, DEF_NON_SSL_PORT);

		putPrefValueBoolean(KEY_USE_KOFAX_SERVER, DEF_USE_KOFAX);
		
		putPrefValueBoolean(KEY_USE_ANONYMOUS, DEF_USE_ANONYMOUS);
		

		putPrefValueString(KEY_KFX_EMAIL, "");

		putPrefValueString(KEY_KFX_UNAME, DEF_KFX_UNAME);
		
		putPrefValueString(KEY_KFX_PASSWORD, DEF_KFX_PASSWORD);

		putPrefValueString(KEY_KFX_URL, DEF_KFX_URL);

		putPrefValueInt(KEY_SENSITIVITY, DEF_SENSITIVITY);

		putPrefValueBoolean(KEY_QUICK_PREVIEW, DEF_PREVIEW);
		
		putPrefValueInt(KEY_MANUAL_CAPTURE, DEF_MANUAL_TIME);
		
		putPrefValueInt(KEY_FLASH, DEF_FLASH);

		putPrefValueString(KEY_KFX_NICKNAME, DEF_KFX_NICKNAME);

		putPrefValueString(KEY_KFX_SERVER_TYPE, DEF_KFX_SERVER_TYPE);
		putPrefValueString(KEY_USR_SERVER_TYPE, DEF_USR_SERVER_TYPE);

		putPrefValueBoolean(KEY_APP_LAUNCHED_FROM_URL, DEF_CUTOMURL);

		putPrefValueString(KEY_APP_LAST_lOGGED_SERVER_TYPE, DEF_USR_SERVER_TYPE);
		putPrefValueString(KEY_APP_LAST_lOGGED_HOSTNAME, DEF_USR_HOSTNAME);
		putPrefValueString(KEY_APP_LAST_lOGGED_USER, DEF_USR_UNAME);
	}

	public void reset_user_preferences() {
		putPrefValueString(KEY_USR_DOMAIN, DEF_USR_DOMAIN);
		putPrefValueString(KEY_USR_HOSTNAME, "");
		putPrefValueString(KEY_USR_PORT, "");
		putPrefValueBoolean(KEY_USR_SSL, false);
		putPrefValueString(KEY_USR_SERVER_TYPE, Globals.serverType.KFS.name());
	}
	
	//! Checks if application preferences are empty.
	/**
	 * @return true if no preferences are set yet, false otherwise.
	 */
	public boolean isPreferenceEmpty() {
		boolean empty = false;
		/* for the very first launch of application, preferences will be empty. 
		 * Once preferences are set, application sets "KEY_SUPERUSER" to indicate the preferences are no longer empty.
		 */
		if ((getPrefValueString(KEY_SUPERUSER) == null)
				|| (getPrefValueString(KEY_SUPERUSER).equals(""))) {
			empty = true;
		}
		return empty;
	}
	
	//! Copy all preferences from KMC-1.2 application to current preferences.
	/**
	 * 
	 * This method gets executed for only first time when application is upgraded. 
	 * When application is launched for the first time after upgrade, it checks if there are any preferences from earlier version is available by checking key "kmc.demo.mode.on" is true.
	 * If so, it overwrites all the current preference with the old ones and sets "kmc.demo.mode.on" to false in preferences to prevent it from copying from next launch onwards.
	 */
	public void updatePreferencesFrom2o1(){
		//If the app is updated from 1.2 to 2.1 
		if(sharedPref.contains("kmc.demo.mode.on")){
			//set flag to indicate that preferences are not empty anymore.
			putPrefValueString(KEY_SUPERUSER, DEF_SUPERUSER);

			// If kofax server is OFF in 1.2 
			if(getPrefValueBoolean("kmc.demo.mode.on", false) == false){
				putPrefValueString(KEY_USR_URL, "");
				
				if(sharedPref.contains("kmc.email") && null != getPrefValueString("kmc.email") && !getPrefValueString("kmc.email").equals("")){	
					putPrefValueString(KEY_USR_EMAIL, getPrefValueString("kmc.email"));
					editor.remove("kmc.email");
					editor.commit();
				}
				
				if(sharedPref.contains("kmc.kfs.hostname") && null != getPrefValueString("kmc.kfs.hostname") && !getPrefValueString("kmc.kfs.hostname").equals("")){
					putPrefValueString(KEY_USR_HOSTNAME, getPrefValueString("kmc.kfs.hostname"));
					editor.remove("kmc.kfs.hostname");
					editor.commit();
				}else{
					//putPrefValueString(KEY_USR_HOSTNAME, DEF_USR_HOSTNAME);
				}
				
				if(sharedPref.contains("kmc.domain") && null != getPrefValueString("kmc.domain") && !getPrefValueString("kmc.domain").equals("")){
					putPrefValueString(KEY_USR_DOMAIN, getPrefValueString("kmc.domain"));
					editor.remove("kmc.domain");
					editor.commit();
				}else{
					//putPrefValueString(KEY_USR_DOMAIN, DEF_USR_DOMAIN);
				}
				
				if(sharedPref.contains("kmc.kfs.use.ssl")){
					putPrefValueBoolean(KEY_USR_SSL, getPrefValueBoolean("kmc.kfs.use.ssl",false));
					editor.remove("kmc.kfs.use.ssl");
					editor.commit();
				}else{
					//putPrefValueString(KEY_KFX_PASSWORD, DEF_KFX_PASSWORD);
				}
				
				if(sharedPref.contains("kmc.kfs.port") && null != getPrefValueString("kmc.kfs.port") && !getPrefValueString("kmc.kfs.port").equals("")){
					putPrefValueString(KEY_USR_PORT, getPrefValueString("kmc.kfs.port"));
					editor.remove("kmc.kfs.port");
					editor.commit();
				}else{
					if(getPrefValueBoolean(KEY_USR_SSL, false) == true) {
						putPrefValueString(KEY_USR_PORT, DEF_SSL_PORT);
					}
					else {
						putPrefValueString(KEY_USR_PORT, DEF_NON_SSL_PORT);
					}
				}
				
				if(sharedPref.contains("kmc.server.type") && null != getPrefValueString("kmc.server.type") && !getPrefValueString("kmc.server.type").equals("")){
					//setting the existing server type in KMC 2.1
					if(getPrefValueString("kmc.server.type").equals("Kofax TotalAgility")){
						putPrefValueString(KEY_USR_SERVER_TYPE, Globals.serverType.KTA.name());
					}else{
						putPrefValueString(KEY_USR_SERVER_TYPE, Globals.serverType.KFS.name());	
					}
					
					editor.remove("kmc.server.type");
					editor.commit();
				}
				
				if(sharedPref.contains("kmc.user.name") && null != getPrefValueString("kmc.user.name") && !getPrefValueString("kmc.user.name").equals("")){
					putPrefValueString(KEY_USR_UNAME, getPrefValueString("kmc.user.name"));
					editor.remove("kmc.user.name");
					editor.commit();
				}else{
					//putPrefValueString(KEY_KFX_UNAME, DEF_KFX_UNAME);
				}

				if(sharedPref.contains("kmc.password") && null != getPrefValueString("kmc.password") && !getPrefValueString("kmc.password").equals("")){
					putPrefValueString(KEY_USR_PASSWORD, getPrefValueString("kmc.password"));
					editor.remove("kmc.password");
					editor.commit();
				}else{
					//putPrefValueString(KEY_KFX_PASSWORD, DEF_KFX_PASSWORD);
				}
				
				if(sharedPref.contains("kmc.kfs.full.url") && null != getPrefValueString("kmc.kfs.full.url") && !getPrefValueString("kmc.kfs.full.url").equals("")){
					putPrefValueString(KEY_USR_URL, getPrefValueString("kmc.kfs.full.url"));
					editor.remove("kmc.kfs.full.url");
					editor.commit();
				}else{
					//putPrefValueString(KEY_KFX_URL, DEF_KFX_URL);	
				}			
				putPrefValueBoolean(KEY_USE_KOFAX_SERVER, false);
			}else{
				if(sharedPref.contains("kmc.email") && null != getPrefValueString("kmc.email") && !getPrefValueString("kmc.email").equals("")){	
					putPrefValueString(KEY_KFX_EMAIL, getPrefValueString("kmc.email"));
					editor.remove("kmc.email");
					editor.commit();
				}
				putPrefValueBoolean(KEY_USE_KOFAX_SERVER, true);
			}
			
			if(sharedPref.contains("kmc.capture.review.on")){
				putPrefValueBoolean(KEY_QUICK_PREVIEW, getPrefValueBoolean("kmc.capture.review.on", DEF_PREVIEW));
				editor.remove("kmc.capture.review.on");
				editor.commit();
			}else{
				//putPrefValueBoolean(KEY_QUICK_PREVIEW, DEF_PREVIEW);	
			}
			if(sharedPref.contains("kmc.camera.stabilization.sensitivity") && (getPrefValueInt("kmc.camera.stabilization.sensitivity",0) > DEF_SENSITIVITY)){
				editor.remove("kmc.camera.stabilization.sensitivity");
				editor.commit();
			}else{
				//putPrefValueInt(KEY_SENSITIVITY, DEF_SENSITIVITY);
			}

			putPrefValueString(KEY_KFX_NICKNAME, DEF_KFX_NICKNAME);			
			
			editor.remove("kmc.demo.mode.on");
			editor.commit();
		}
	}

	public boolean isUsingAnonymous() {
		Boolean useAnonymous = getPrefValueBoolean(KEY_USE_ANONYMOUS,
				DEF_USE_ANONYMOUS);
		return useAnonymous;
	}

	public boolean isUsingAnonymousDemo() {
		Boolean useAnonymous = getPrefValueBoolean(KEY_USE_ANONYMOUS_DEMO,
				DEF_USE_ANONYMOUS);
		return useAnonymous;
	}
}
