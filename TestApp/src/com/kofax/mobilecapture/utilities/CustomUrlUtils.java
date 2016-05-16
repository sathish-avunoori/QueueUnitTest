// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;

import android.content.Context;
import android.util.Log;

import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;

/// A class to take care of all the utility functions used in handling custom-url feature.

public class CustomUrlUtils {
	// - private constants
	private final String TAG = CustomUrlUtils.class.getSimpleName();

	// - Private data.
	private static volatile CustomUrlUtils pSelf = null;
	private Map<String, String> documentFieldDefaults = new HashMap<String, String>();
	private String kfs_server_url;
	private String serverType;
	private String hostname;
	private String port;
	private String username;
	private String password;
	private String email_address;
	private String autoLaunchDocumentName;

	
	private String old_kfs_server_url;
	private String old_serverType;
	private String old_hostname;
	private String old_port;
	private String old_username;
	private String old_password;
	private String old_email_address;
	private boolean old_ssl;
	private boolean old_use_kofax_server;
	private boolean isAnonymousUser;

	private boolean ssl;
	private boolean autoLaunchShowDocumentInfoFieldPresent;
	private boolean autoLaunchShowDocumentInfoFirst;
	private boolean autoLaunchDocumentHasFields;
	private boolean isUsingCustomUrl;
	private boolean invalidFieldPresent;
	private boolean isChangeInHostname;
	private boolean isChangeInUsername;
	private boolean isChangeInPwd;
	private boolean isChageInEmail;
	private boolean isClosingApp = false;
	private boolean isChangeInCredentials;

	// - Private constructor prevents instantiation from other classes
	private CustomUrlUtils() {
		Log.e(TAG, "Constructor CustomUrlUtils ");

		documentFieldDefaults = new HashMap<String, String>();

		kfs_server_url = null;
		username = null;
		password = null;
		email_address = null;
		
		autoLaunchDocumentName = null;
		autoLaunchShowDocumentInfoFieldPresent = false;
		autoLaunchShowDocumentInfoFirst = false;
		autoLaunchDocumentHasFields = false;

		isUsingCustomUrl = false;
		invalidFieldPresent = false;
		
		isChangeInHostname = false;
		isChangeInUsername = false;
		isChangeInPwd = false;
		isChageInEmail = false;
		isChangeInCredentials = false;
	}

	// - public getters and setters
	public static CustomUrlUtils getInstance() {
		if (pSelf == null) {
			synchronized (CustomUrlUtils.class) {
				if (pSelf == null) {
					pSelf = new CustomUrlUtils();		
				}
			}
		}
		return pSelf;
	}

	/**
	 * @return the kfs_server_url
	 */
	public String getKfsServerUrl() {
		return kfs_server_url;
	}

	/**
	 * @param kfs_server_url the kfs_server_url to set
	 */
	public void setKfsServerUrl(String kfs_server_url) {
		this.kfs_server_url = kfs_server_url;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the email_address
	 */
	public String getEmailAddress() {
		return email_address;
	}

	/**
	 * @param email_address the email_address to set
	 */
	public void setEmailAddress(String email_address) {
		this.email_address = email_address;
	}

	/**
	 * @return the autoLaunchDocumentName
	 */
	public String getAutoLaunchDocumentName() {
		return autoLaunchDocumentName;
	}

	/**
	 * @param autoLaunchDocumentName the autoLaunchDocumentName to set
	 */
	public void setAutoLaunchDocumentName(String autoLaunchDocumentName) {
		this.autoLaunchDocumentName = autoLaunchDocumentName;
	}

	/**
	 * @return the autoLaunchShowDocumentInfoFirst
	 */
	public boolean isAutoLaunchShowDocumentInfoFirst() {
		return autoLaunchShowDocumentInfoFirst;
	}

	/**
	 * @param autoLaunchShowDocumentInfoFirst the autoLaunchShowDocumentInfoFirst to set
	 */
	public void setAutoLaunchShowDocumentInfoFirst(
			boolean autoLaunchShowDocumentInfoFirst) {
		this.autoLaunchShowDocumentInfoFirst = autoLaunchShowDocumentInfoFirst;
	}

	/**
	 * @return the autoLaunchDocumentHasFields
	 */
	public boolean isAutoLaunchDocumentHasFields() {
		return autoLaunchDocumentHasFields;
	}

	/**
	 * @param autoLaunchDocumentHasFields the autoLaunchDocumentHasFields to set
	 */
	public void setAutoLaunchDocumentHasFields(boolean autoLaunchDocumentHasFields) {
		this.autoLaunchDocumentHasFields = autoLaunchDocumentHasFields;
	}

	/**
	 * @return the isUsingCustomUrl
	 */
	public boolean isUsingCustomUrl() {
		return this.isUsingCustomUrl;
	}

	/**
	 * @param isUsingCustomUrl the isUsingCustomUrl to set
	 */
	public void setUsingCustomUrl(boolean isUsingCustomUrl) {
		Log.e(TAG, "setUsingCustomUrl :: Setting " + isUsingCustomUrl);
		this.isUsingCustomUrl = isUsingCustomUrl;
	}

	/**
	 * @return the documentFieldDefaults
	 */
	public Map<String, String> getDocumentFieldDefaults() {
		return documentFieldDefaults;
	}

	/**
	 * @param documentFieldDefaults the documentFieldDefaults to set
	 */
	public void setDocumentFieldDefaults(Map<String, String> documentFieldDefaults) {
		this.documentFieldDefaults = documentFieldDefaults;
	}
	
	public String[] parseFieldData(String fieldData) {
		String data = (String) fieldData;

		if (data.indexOf("|") == -1) {
			Log.e(TAG, "Bad case field -> " + data);
			return null;
		}

		String fieldName = data.substring(0, data.indexOf("|"));
		String fieldVal = data.substring(fieldName.length() + 1);

		Log.d(TAG, "Field data :: name -> " + fieldName + ", fieldVal -> " + fieldVal);

		String[] parsedFieldData = new String[2];
		parsedFieldData[0] = fieldName;
		parsedFieldData[1] = fieldVal;

		return parsedFieldData;
	}
	
	public String parseUrlAndFindErrors(Context context, List<NameValuePair> queryStringParams) {
		String errMsg = null;
		boolean isDemoServer = false;
		for (NameValuePair param : queryStringParams) {
			Log.i(TAG, param.getName() + " ::: " + param.getName());
			Log.i(TAG, param.getValue() + " ::: " + param.getValue());
			//updating the preference values.
			//	mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_KOFAX_SERVER, false);
			if(param.getName() != null && param.getValue() == null) {
				throw new RuntimeException("Error in " + param.getName());
			}

			
			if(param.getName().equals("kfs_url") || param.getName().equals("user_name") || param.getName().equals("password") || 
					param.getName().equals("email_address") || param.getName().equals("do_field_update_first")) {
				if(param.getName().equals("kfs_url")){
					PrefManager prefUtils = PrefManager.getInstance();
					if(param.getValue() != null && param.getValue().contains(prefUtils.DEF_KFX_URL)){
						isDemoServer = true;
					}
				}
				
			}else if (param.getName().equals("document_name") || param.getName().equals("case_name")) {
				
				if(isDemoServer == true && param.getValue() != null && param.getValue().contains(Constants.STR_ASSIST)){
					errMsg = context.getResources().getString(R.string.progress_msg_url_contains_assistkofx_document_type);
				}
			}			
			else if (param.getName().equals("document_field") || param.getName().equals("case_field")) {
				String[] parsedFieldData = parseFieldData(param.getValue());
				if (null == parsedFieldData) {
					errMsg = context.getResources().getString(R.string.error_msg_query_string_has_errors_and_is_not_usable);
					//throw new RuntimeException(getResources().getString(R.string.error_msg_parsing_failed) + param.getValue());
				}				
			}else if(param.getName().equals("isAnonymous")){
				if(param.getValue() == null){
					errMsg = context.getResources().getString(R.string.progress_msg_url_contains_invalid_parameters);
				}
			} else{
				//else if url contains any other parameter than the standard keywords mentioned above, display 'invalid parameters' error. 
				errMsg = context.getResources().getString(R.string.progress_msg_url_contains_invalid_parameters);
				//throw new RuntimeException(getResources().getString(R.string.error_msg_parsing_failed) + param.getValue());
			}
		} 
		return errMsg;
	}

	
	public String parseUrlAndUpdatePreferences(Context context, List<NameValuePair> queryStringParams) {
		String errMsg = null;
		PrefManager prefUtils = PrefManager.getInstance();
		UtilityRoutines mUtilRoutines = UtilityRoutines.getInstance();
		old_use_kofax_server = prefUtils.getPrefValueBoolean(prefUtils.KEY_USE_KOFAX_SERVER, false);
		for (NameValuePair param : queryStringParams) {
			Log.i(TAG, param.getName() + " ::: " + param.getName());
			Log.i(TAG, param.getValue() + " ::: " + param.getValue());
			//updating the preference values.
			//	mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_KOFAX_SERVER, false);
			if(param.getName() != null && param.getValue() == null) {
				throw new RuntimeException("Error in " + param.getName());
			}
			isAnonymousUser = prefUtils.getPrefValueBoolean(prefUtils.KEY_USE_ANONYMOUS, false);
			prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_KOFAX_SERVER, false);
			if(param.getName().equals("kfs_url")) {
				if(prefUtils.isUsingKofax()) {
					isChangeInCredentials = true;
				}
				//mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_USE_KOFAX_SERVER, false);
				setKfsServerUrl(param.getValue());
				old_kfs_server_url = prefUtils.getPrefValueString(prefUtils.KEY_USR_URL);
				//check if earlier url and new url are same
				if(!param.getValue().equals(prefUtils.getCurrentUrl())) {
					setChangeInHostname(true);
					isChangeInCredentials = true;

					Log.e(TAG, "url ==> " + param.getValue());
				}
				else {
					setChangeInHostname(false);
					
				}
				updateUrlParams(param,prefUtils);
			}
			else if(param.getName().equals("user_name")) {
				//check if earlier username and new username are same
				if(!param.getValue().equalsIgnoreCase(mUtilRoutines.getUser())) {
					setChangeInUsername(true);
					isChangeInCredentials = true;
				}
				else {
					setChangeInUsername(false);
				}
				setUsername(param.getValue());
				old_username = prefUtils.getPrefValueString(prefUtils.KEY_USR_UNAME);
				prefUtils.putPrefValueString(prefUtils.KEY_USR_UNAME, param.getValue());
			}
			else if (param.getName().equals("password")) {
				prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_ANONYMOUS, false);
				//check if earlier password and new password are same
				if(!param.getValue().equals(prefUtils.getCurrentPassword())) {
					setChangeInPassword(true);
					isChangeInCredentials = true;
				}
				else {
					setChangeInPassword(false);
				}
				setPassword(param.getValue());
				old_password = prefUtils.getPrefValueString(prefUtils.KEY_USR_PASSWORD);
				prefUtils.putPrefValueString(prefUtils.KEY_USR_PASSWORD, param.getValue());
			}
			else if (param.getName().equals("email_address")) {
				String currentEmail = null;
				if(!prefUtils.isPreferenceEmpty()) {
					if(prefUtils.isUsingKofax()) {
						currentEmail = prefUtils.getPrefValueString(prefUtils.KEY_KFX_EMAIL);
					}
					else {
						currentEmail = prefUtils.getPrefValueString(prefUtils.KEY_USR_EMAIL);

					}
				}
				if ((currentEmail == null) || currentEmail.equals(param.getValue()) == false) {
					setChangeInEmail(true);
					setEmailAddress(param.getValue());
					old_email_address = prefUtils.getPrefValueString(prefUtils.KEY_USR_EMAIL);
					prefUtils.putPrefValueString(prefUtils.KEY_USR_EMAIL, param.getValue());
				}
				else {
					setChangeInEmail(false);
				}
			}
			else if (param.getName().equals("document_name") || param.getName().equals("case_name")) {
				setAutoLaunchDocumentName(param.getValue());
				}
			else if (param.getName().equals("do_field_update_first")) {
				setAutoLaunchShowDocumentInfoFieldPresent(true);
				setAutoLaunchShowDocumentInfoFirst(param.getValue().equals("true"));
			}
			else if (param.getName().equals("document_field") || param.getName().equals("case_field")) {
				setAutoLaunchDocumentHasFields(true);

				String[] parsedFieldData = parseFieldData(param.getValue());
				if (null == parsedFieldData) {
					errMsg = context.getResources().getString(R.string.error_msg_query_string_has_errors_and_is_not_usable);
					//throw new RuntimeException(getResources().getString(R.string.error_msg_parsing_failed) + param.getValue());
				}
				else {
					getDocumentFieldDefaults().put(parsedFieldData[0], parsedFieldData[1]);
				}
			}else if(param.getName().equals("isAnonymous")){
				//setUsername(Constants.ANONYMOUS_LOGIN_ID);
				old_username = prefUtils.getPrefValueString(prefUtils.KEY_USR_UNAME);
				//prefUtils.putPrefValueString(prefUtils.KEY_USR_UNAME, Constants.ANONYMOUS_LOGIN_ID);
				if(param.getValue() != null && param.getValue().equals("true")){
					//isAnonymousUser = true;
					prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_ANONYMOUS, true);
					setChangeInUsername(false);
					setChangeInPassword(false);
					isChangeInCredentials = true;
				}else{
					//isAnonymousUser = false;
					prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_ANONYMOUS, false);
					errMsg = context.getResources().getString(R.string.progress_msg_url_contains_invalid_parameters);
				}
			}
			else {
				//else if url contains any other parameter than the standard keywords mentioned above, display 'invalid parameters' error. 
				errMsg = context.getResources().getString(R.string.progress_msg_url_contains_invalid_parameters);
				//throw new RuntimeException(getResources().getString(R.string.error_msg_parsing_failed) + param.getValue());
			}
		} // for
		/*
		Log.i(TAG, "*************** NEW ARGUMENTS *********************************");
		Log.i(TAG, "NEW server url" + getKfsServerUrl());
		Log.i(TAG, "NEW username" + getUsername());
		Log.i(TAG, "NEW password" + getPassword());
		Log.i(TAG, "NEW Email address" + getEmailAddress());
		Log.i(TAG, "NEW Hostname" + mPrefUtils.getCurrentHostname());
		Log.i(TAG, "NEW port number" + mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PORT));
		Log.i(TAG, "NEW SSL" + mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_USR_SSL, false));
		Log.i(TAG, "************************************************");

		Log.i(TAG, "*************** OLD ARGUMENTS *********************************");
		Log.i(TAG, "isUsingKofax :: " + mPrefUtils.isUsingKofax());
		Log.i(TAG, "OLD server url" + mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_URL));
		Log.i(TAG, "OLD username" + mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_UNAME));
		Log.i(TAG, "OLD password" + mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PASSWORD));
		Log.i(TAG, "OLD Email address" + getEmailAddress());
		Log.i(TAG, "************************************************");
		 */
		prefUtils = null;
		return errMsg;
	}

	public void reset_login_pref(){
		PrefManager prefUtils = PrefManager.getInstance();

		prefUtils.putPrefValueString(prefUtils.KEY_USR_URL,old_kfs_server_url);
		prefUtils.putPrefValueString(prefUtils.KEY_USR_UNAME,old_username);
		prefUtils.putPrefValueString(prefUtils.KEY_USR_PASSWORD, old_password);
		prefUtils.putPrefValueBoolean(prefUtils.KEY_USR_SSL, old_ssl);
		prefUtils.putPrefValueString(prefUtils.KEY_USR_HOSTNAME, old_hostname);
		prefUtils.putPrefValueString(prefUtils.KEY_USR_PORT, old_port);
		prefUtils.putPrefValueString(prefUtils.KEY_USR_SERVER_TYPE, old_serverType);
		prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_KOFAX_SERVER, old_use_kofax_server);
		prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_ANONYMOUS,isAnonymousUser);
		if(old_email_address != null && old_email_address.length() > 0){
			prefUtils.putPrefValueString(prefUtils.KEY_USR_EMAIL, old_email_address);
		}
	}

	
	public void update_login_pref_from_custom_url () {
		PrefManager prefUtils = PrefManager.getInstance();
		
		prefUtils.reset_user_preferences();

		prefUtils.putPrefValueString(prefUtils.KEY_USR_URL, getKfsServerUrl());	//TODO: will this always be for custom user or for kofax demo user
		prefUtils.putPrefValueString(prefUtils.KEY_USR_UNAME, getUsername());
		prefUtils.putPrefValueString(prefUtils.KEY_USR_PASSWORD, getPassword());

		prefUtils.putPrefValueBoolean(prefUtils.KEY_USR_SSL, isSslOn());
		prefUtils.putPrefValueString(prefUtils.KEY_USR_HOSTNAME, getHostname());
		prefUtils.putPrefValueString(prefUtils.KEY_USR_PORT, getPort());
		prefUtils.putPrefValueString(prefUtils.KEY_USR_SERVER_TYPE, getServerType());

		if((getKfsServerUrl() != null) && (!(getKfsServerUrl().equals("")))) {
			prefUtils.putPrefValueBoolean(prefUtils.KEY_USE_KOFAX_SERVER, false);
		}
	}
	
	public String build_message_string (String confirmation_str) {

		confirmation_str += "Your";

		boolean items = false;

		if(isChangeInHostname) {
			confirmation_str += " Hostname";	//TODO: Globalization!
			items = true;
		}

		if (isChangeInUsername) {
			if(true == items )	{
				confirmation_str += ",";
			}
			confirmation_str += " Username";
			items = true;
		}

		if (isChangeInPwd) {
			if(true == items )	{
				confirmation_str += ",";			
			}
			confirmation_str += " Password";
			items = true;
		}

		if (isChageInEmail) {
			if(true == items )	{
				confirmation_str += ",";			
			}
			confirmation_str += " E-mail";
		}
		if(items)
			confirmation_str += " will be updated. \n\n Accept this change?";
		else {
			confirmation_str = "You will be logged in again with same credentials.\n\n Want to login?";
		}
		return (confirmation_str);
	}

	
	public void cleanup() {
		pSelf = null;

		if(documentFieldDefaults != null) {
			documentFieldDefaults.clear();
			documentFieldDefaults = null;
		}

		kfs_server_url = null;
		username = null;
		password = null;
		email_address = null;
		
		autoLaunchDocumentName = null;
		autoLaunchShowDocumentInfoFirst = false;
		autoLaunchDocumentHasFields = false;

		isUsingCustomUrl = false;
		isChangeInHostname = false;
		isChangeInUsername = false;
		isChangeInPwd = false;
		isChageInEmail = false;
	}
	
	public void resetCustomUrlFlags(){
		autoLaunchDocumentName = null;
		autoLaunchShowDocumentInfoFieldPresent = false;
		autoLaunchShowDocumentInfoFirst = false;
		autoLaunchDocumentHasFields = false;
	}

	public boolean isInvalidFieldPresent() {
		return invalidFieldPresent;
	}

	public void setInvalidFieldPresent(boolean invalidFields) {
		this.invalidFieldPresent = invalidFields;
	}

	/**
	 * @return the isChangeInUser
	 */
	public boolean isChangeInUsername() {
		return isChangeInUsername;
	}

	/**
	 * @param isChangeInUser the isChangeInUser to set
	 */
	public void setChangeInUsername(boolean isChangeInUser) {
		this.isChangeInUsername = isChangeInUser;
	}

	/**
	 * @return the isChangeInPwd
	 */
	public boolean isChangeInPassword() {
		return isChangeInPwd;
	}

	/**
	 * @param isChangeInPwd the isChangeInPwd to set
	 */
	public void setChangeInPassword(boolean isChangeInPwd) {
		this.isChangeInPwd = isChangeInPwd;
	}

	/**
	 * @return the isChageInEmail
	 */
	public boolean isChangeInEmail() {
		return isChageInEmail;
	}

	/**
	 * @param isChageInEmail the isChageInEmail to set
	 */
	public void setChangeInEmail(boolean isChageInEmail) {
		this.isChageInEmail = isChageInEmail;
	}

	/**
	 * @return the isChangeInHostname
	 */
	public boolean isChangeInHostname() {
		return isChangeInHostname;
	}

	/**
	 * @param isChangeInHostname the isChangeInHostname to set
	 */
	public void setChangeInHostname(boolean isChangeInHostname) {
		this.isChangeInHostname = isChangeInHostname;
	}

	/**
	 * @return the isClosingApp
	 */
	public boolean isClosingApp() {
		return isClosingApp;
	}

	/**
	 * @param isClosingApp the isClosingApp to set
	 */
	public void setClosingApp(boolean isClosingApp) {
		this.isClosingApp = isClosingApp;
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @param hostname the hostname to set
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * @return the ssl
	 */
	public boolean isSslOn() {
		return ssl;
	}

	/**
	 * @param ssl the ssl to set
	 */
	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	/**
	 * @return the serverType
	 */
	public String getServerType() {
		return serverType;
	}

	/**
	 * @param serverType the serverType to set
	 */
	public void setServerType(String serverType) {
		this.serverType = serverType;
	}

	/**
	 * @return the autoLaunchShowDocumentInfoFieldPresent
	 */
	public boolean isAutoLaunchShowDocumentInfoFieldPresent() {
		return autoLaunchShowDocumentInfoFieldPresent;
	}

	/**
	 * @param autoLaunchShowDocumentInfoFieldPresent the autoLaunchShowDocumentInfoFieldPresent to set
	 */
	public void setAutoLaunchShowDocumentInfoFieldPresent(
			boolean autoLaunchShowDocumentInfoFieldPresent) {
		this.autoLaunchShowDocumentInfoFieldPresent = autoLaunchShowDocumentInfoFieldPresent;
	}

	/**
	 * @return the isChangeInCredentials
	 */
	public boolean isChangeInCredentials() {
		return isChangeInCredentials;
	}

	/**
	 * @param isChangeInCredentials the isChangeInCredentials to set
	 */
	public void setChangeInCredentials(boolean isChangeInCredentials) {
		this.isChangeInCredentials = isChangeInCredentials;
	}

	
	private void updateUrlParams(NameValuePair param,PrefManager prefUtils){
		String[] urlFrag = param.getValue().split("://");

		old_ssl = prefUtils.getPrefValueBoolean(prefUtils.KEY_USR_SSL,false);
		old_port = prefUtils.getPrefValueString(prefUtils.KEY_USR_PORT);
		old_hostname = prefUtils.getPrefValueString(prefUtils.KEY_USR_HOSTNAME);
		old_serverType = prefUtils.getPrefValueString(prefUtils.KEY_USR_SERVER_TYPE);
		
		//extract SSL
		if(urlFrag[0] != null && urlFrag[0].contains("HTTPS") || urlFrag[0].contains("https")) {
			setSsl(true);
			prefUtils.putPrefValueBoolean(prefUtils.KEY_USR_SSL, true);
		}
		else {
			setSsl(false);
			prefUtils.putPrefValueBoolean(prefUtils.KEY_USR_SSL, false);
		}
		//extract hostname
		if(urlFrag[1] != null) {
			String[] secondLevelFrag = urlFrag[1].split(":");
			if(secondLevelFrag.length >= 2) {
				String hostname = secondLevelFrag[0];
				setHostname(hostname);
				prefUtils.putPrefValueString(prefUtils.KEY_USR_HOSTNAME, hostname);
			}
			else {
				String[] hostNameFrag = secondLevelFrag[0].split("/");
				if(hostNameFrag[0] != null) {
					String hostname = hostNameFrag[0];
					setHostname(hostname);
					prefUtils.putPrefValueString(prefUtils.KEY_USR_HOSTNAME, hostname);
				}
			}
			//extract port number
			if((secondLevelFrag.length >= 2) && secondLevelFrag[1] != null) {
				String[] thirdLevelFrag = secondLevelFrag[1].split("/");
				if(thirdLevelFrag[0] != null) {
					String portnumber = thirdLevelFrag[0];
					setPort(portnumber);
					prefUtils.putPrefValueString(prefUtils.KEY_USR_PORT, portnumber);
				}
			}
			else {
				prefUtils.putPrefValueString(prefUtils.KEY_USR_PORT, "");
			}
		}
		//extract the server type (KFS/TA) from url and update the preferences accordingly. This is required to know the server type while logging in.
		if(param.getValue().contains("/KFS/")) {
			setServerType(Globals.serverType.KFS.name());
			prefUtils.putPrefValueString(prefUtils.KEY_USR_SERVER_TYPE, Globals.serverType.KFS.name());
		}
		else if(param.getValue().contains("/TA/") || param.getValue().contains("/TotalAgility/")) {
			setServerType(Globals.serverType.KTA.name());
			prefUtils.putPrefValueString(prefUtils.KEY_USR_SERVER_TYPE, Globals.serverType.KTA.name());
		}
	}
	
	
}
