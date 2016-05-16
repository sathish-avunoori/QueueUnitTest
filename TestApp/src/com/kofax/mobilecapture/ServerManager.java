// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kofax.kmc.klo.logistics.FrontOfficeServer;
import com.kofax.kmc.klo.logistics.KfsDocTypeResultEvent;
import com.kofax.kmc.klo.logistics.KfsDocTypeResultEventListener;
import com.kofax.kmc.klo.logistics.KfsSessionStateEvent;
import com.kofax.kmc.klo.logistics.KfsSessionStateEventListener;
import com.kofax.kmc.klo.logistics.SessionState;
import com.kofax.kmc.klo.logistics.TotalAgilityServer;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.klo.logistics.data.UserProfile;
import com.kofax.kmc.kut.utilities.CertificateValidatorListener;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.utilities.Globals.serverType;

import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/// The ServerManager takes care of application wide preferences.  

public class ServerManager implements KfsDocTypeResultEventListener,
KfsSessionStateEventListener, CertificateValidatorListener {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants

	// - Private data.
	private final String TAG = ServerManager.class.getSimpleName();
	private static volatile ServerManager pSelf = null;

	/* SDK objects */
	private FrontOfficeServer frontOfficeServer;
	private TotalAgilityServer mTotalAgilityServer;
	private DocumentType mRecentDocTypeObj = null;
	
	/* Application objects */
	private PrefManager mPrefUtils = null;

	/* Standard variables */
	private Handler mCallerHandler = null;


	// - public constructors

	// - Private constructor prevents instantiation from other classes
	private ServerManager() {
		frontOfficeServer = new FrontOfficeServer();
		frontOfficeServer.setCertificateValidatorListener(this);
		frontOfficeServer.addKfsSessionStateEventListener(this);
		frontOfficeServer.addKfsDocTypeResultEventListener(this);
		
		mTotalAgilityServer = new TotalAgilityServer();
		mTotalAgilityServer.setCertificateValidatorListener(this);
		mTotalAgilityServer.addKfsSessionStateEventListener(this);
		mTotalAgilityServer.addKfsDocTypeResultEventListener(this);
		mPrefUtils = PrefManager.getInstance();
	}

	// - public getters and setters
	
    //! The factory method.
	public static ServerManager getInstance() {
		if (pSelf == null) {
			synchronized (ServerManager.class) {
				if (pSelf == null) {		
					pSelf = new ServerManager();		
				}
			}
		}
		return pSelf;
	}

    //! Get frontOfficeServer object.
	/**
	 * @return FrontOfficeServer object
	 */
	public FrontOfficeServer getFrontOfficeServer() {
		return frontOfficeServer;
	}
	
    //! Get SessionState from kfs server.
	/**
	 * 
	 * @return SessionState object
	 */
	public SessionState getSessionState(){
		//Fixed bug#596878 Android:KMC2.4:KTA user is Failed to Logout in KMC app
		SessionState sessionState = null;
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
		if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
			sessionState = frontOfficeServer.getSessionState();
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
        	sessionState = mTotalAgilityServer.getSessionState();
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
        	sessionState = mTotalAgilityServer.getSessionState();
        }
		return sessionState;
		
	}
	
    //! Get recent DocumentType.
	/**
	 * @return the mRecentDocTypeObj
	 */
	public DocumentType getRecentDocTypeObj() {
		return mRecentDocTypeObj;
	}

	// - public methods

	@Override
	public void kfsSessionStateChanged(KfsSessionStateEvent arg0) {
		Log.d(TAG,
				"Session State is:: ****************** "
						+ arg0.getSessionState());
		Log.d(TAG, "Error info is:: ****************** " + arg0.getErrorInfo());

		Log.d(TAG, "getErrCause is:: ****************** "
				+ arg0.getErrorInfo().getErrCause());

		Log.d(TAG, "getErrDesc is:: ****************** "
				+ arg0.getErrorInfo().getErrDesc());

		Log.d(TAG, "getErrMsg is:: ****************** "
				+ arg0.getErrorInfo().getErrMsg());

		if (((arg0.getSessionState() == SessionState.SESSION_REGISTERING || arg0
				.getSessionState() != SessionState.SESSION_LOGGING_IN) && (arg0
						.getErrorInfo() != ErrorInfo.KMC_SUCCESS))
						|| (arg0.getSessionState() != SessionState.SESSION_REGISTERING && arg0
						.getSessionState() != SessionState.SESSION_LOGGING_IN)) {

			Log.d(TAG, "Sending message back to caller :: " + mCallerHandler);

			Message msg = new Message();
			msg.obj = arg0;
			if(mCallerHandler != null){				
				mCallerHandler.sendMessage(msg);
			}
			else {
				Log.e(TAG, "Error:: callerHandler is NULL!!");
			}
		} else {
			Log.d(TAG, "Did not send message back to the caller");
		}
	}

	@Override
	public void kfsDocumentTypeResultReady(KfsDocTypeResultEvent arg0) {
		mRecentDocTypeObj = (DocumentType) arg0.getDocumentType();

		// Add downloaded documentType object into array
		DocumentType doc = (DocumentType) arg0.getDocumentType();

/*		Log.i(TAG, "DisplayName :: " + doc.getDisplayName());
		Log.i(TAG, "Doc Height :: " + doc.getDocHeight());
		Log.i(TAG, "Doc Width :: " + doc.getDocWidth());
		Log.i(TAG, "TypeName :: " + doc.getTypeName());
		Log.i(TAG, "Version :: " + doc.getVersion());
		Log.i(TAG, "IPP :: "
				+ (doc.getImagePerfectionProfile() == null ? null : doc
						.getImagePerfectionProfile().getIpOperations()));
		Log.i(TAG, "BSP :: " + doc.getBasicSettingsProfile());
		Log.i(TAG, "Source Server :: " + doc.getSourceServer());

		Log.i(TAG, "\n===============================================");
*/
		if (doc.getFieldTypes() != null) {
			for(int i=0; i< doc.getFieldTypes().size(); i++) {
				if (doc.getFieldTypes().get(i) != null) {
/*					Log.i(TAG, "Field 0 :: " + doc.getFieldTypes().get(0) == null ? null
						: doc.getFieldTypes().get(0).getDisplayName());
					 
					Log.i(TAG, "DisplayName :: " + doc.getFieldTypes().get(i).getDisplayName());
					Log.i(TAG, "CustomTag :: " + doc.getFieldTypes().get(i).getCustomTag());
					Log.i(TAG, "Default :: " + doc.getFieldTypes().get(i).getDefault());	//**
					Log.i(TAG, "Label :: " + doc.getFieldTypes().get(i).getLabel());
					Log.i(TAG, "Max :: " + doc.getFieldTypes().get(i).getMax());//**
					Log.i(TAG, "Min :: " + doc.getFieldTypes().get(i).getMin());//**
					Log.i(TAG, "Name :: " + doc.getFieldTypes().get(i).getName());
					Log.i(TAG, "Class :: " + doc.getFieldTypes().get(i).getClass());
					Log.i(TAG, "DataType :: " + doc.getFieldTypes().get(i).getDataType());	//**
					Log.i(TAG, "Options :: " + doc.getFieldTypes().get(i).getOptions());	//**
					Log.i(TAG, "isForceMatch :: " + doc.getFieldTypes().get(i).isForceMatch());	//**
					Log.i(TAG, "isHidden :: " + doc.getFieldTypes().get(i).isHidden());	//**
					Log.i(TAG, "isReadOnly :: " + doc.getFieldTypes().get(i).isReadOnly());	//**
					Log.i(TAG, "isRequired :: " + doc.getFieldTypes().get(i).isRequired());	//**
*/
					if (doc.getFieldTypes().get(i).getOptions() != null)
//						Log.i(TAG, "\n-------------------- options------------------\n");
					for (int j=0; j<doc.getFieldTypes().get(i).getOptions().length; j++) {
//						Log.i(TAG, "Options- " + j + " :: " + doc.getFieldTypes().get(i).getOptions()[j]);	//**
					}
//					Log.i(TAG, "\n-------------------- options------------------\n");
				}
			}
//			Log.i(TAG, "===============================================");
		}
	}


	//! Checks if server is in logged-in state
	/**
	 * 
	 * @return true if logged in, false otherwise.
	 */
	public boolean isLoggedIn(){
		boolean return_value = false;
		SessionState state = getSessionState();
		if(state == SessionState.SESSION_LOGGED_IN || state == SessionState.SESSION_DOCUMENT_TYPE_READY || state == SessionState.SESSION_GETTING_DOCUMENT_FIELDS ||
				state == SessionState.SESSION_GETTING_IP_SETTINGS || state == SessionState.SESSION_PREPARING_DOCUMENT_TYPE || state == SessionState.SESSION_SUBMIT_COMPLETED || state == SessionState.SESSION_SUBMITTING){
			return_value = true;
		}
		return return_value;
	}

	//! Checks if server is logging-in.
	/**
	 * 
	 * @return true if logging-in false otherwise
	 */
	public boolean isLoggingIn() {
		//Fixed bug#596878 Android:KMC2.4:KTA user is Failed to Logout in KMC app
		SessionState state = getSessionState();
		
		return (state == SessionState.SESSION_LOGGING_IN
				|| state == SessionState.SESSION_GETTING_IP_SETTINGS
				|| state == SessionState.SESSION_REGISTERING);
	}
	
	//! Checks if server is in some intermediate state
	/**
	 * 
	 * @return true if busy false otherwise
	 */
	public boolean isServerBusy() {
		//Fixed bug#596878 Android:KMC2.4:KTA user is Failed to Logout in KMC app
		SessionState state = getSessionState();
		
		return (state == SessionState.SESSION_LOGGING_IN
				|| state == SessionState.SESSION_GETTING_DOCUMENT_FIELDS
				|| state == SessionState.SESSION_GETTING_IP_SETTINGS
				|| state == SessionState.SESSION_PREPARING_DOCUMENT_TYPE
				|| state == SessionState.SESSION_REGISTERING
				|| state == SessionState.SESSION_SUBMITTING);
	}
	

	//! Initiates download of DocumentType object by calling  getDocumentType() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
	public void downloadDocumentType(Handler pHandler, String documentTypeName) throws KmcException {
		mRecentDocTypeObj = null;
		mCallerHandler = pHandler;
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
                mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
        
        if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
            frontOfficeServer.getDocumentType(documentTypeName);
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
            mTotalAgilityServer.getDocumentType(documentTypeName);
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
            mTotalAgilityServer.getDocumentType(documentTypeName);
        }
		
	}
	
	//! Gets list of supported documentType names for current user from server by calling getDocumentTypeList() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
	/**
	 * 
	 * @return List of supported documentType names for current user.
	 */
	public List<String> getDocTypeNamesFromServer() {
		mRecentDocTypeObj = null;
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
                mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
        
        if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
            return frontOfficeServer.getDocumentTypeList();
        }else{
            return mTotalAgilityServer.getDocumentTypeList();
        }
		
	}
	
	//! Check for documentType list in SDK
	
	/*** Check for documentType list in SDK
	 * 
	 * @return true:sdk have documentType list
	 */
	
	public boolean isDocumentTypeDownload(){
		List<String> docList = null;
		boolean status = false; 
		
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
                mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
        
        if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
        	docList = frontOfficeServer.getDocumentTypeList();
        }else{
        	docList = mTotalAgilityServer.getDocumentTypeList();
        }
        if(docList != null && docList.size() > 0){
        	status = true;
        }
        docList = null;
        return status;
	}
	
	//! Sets server timeout value on FrontOfficeServer or TotalAgilityServer based on current server type used.
	public void setServerTimeout(int timeout){
	    Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
                mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
	    if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
             frontOfficeServer.setServerTimeout(timeout);
        }else{
             mTotalAgilityServer.setServerTimeout(timeout);
        }
	}
	

	//! Initiates login by invoking login() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
	/**
	 * Username, password and domain are picked from preferences based demo or non-demo server being used.
	 * @param pHandler
	 * @throws KmcException
	 * @throws KmcRuntimeException
	 * @throws SocketTimeoutException
	 */
	public void login(Handler pHandler) throws KmcException,
	KmcRuntimeException, SocketTimeoutException {
		mCallerHandler = pHandler;
		UserProfile userProfile_l = new UserProfile();
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
				mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
		if (usekofax) {
			userProfile_l.setDomain("");
			userProfile_l.setUsername(mPrefUtils.sharedPref.getString(
					mPrefUtils.KEY_KFX_UNAME, mPrefUtils.DEF_KFX_UNAME));
			userProfile_l.setPassword(mPrefUtils.sharedPref.getString(
					mPrefUtils.KEY_KFX_PASSWORD, mPrefUtils.DEF_KFX_PASSWORD));
			userProfile_l.setUserEmailAddress(mPrefUtils.sharedPref.getString(
			        mPrefUtils.KEY_KFX_EMAIL, mPrefUtils.DEF_KFX_EMAIL));
			frontOfficeServer.login(userProfile_l);
		} else {
			userProfile_l.setDomain(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_DOMAIN, mPrefUtils.DEF_USR_DOMAIN));
			userProfile_l.setUsername(mPrefUtils.sharedPref.getString(
					mPrefUtils.KEY_USR_UNAME, mPrefUtils.DEF_USR_UNAME));
			userProfile_l.setPassword(mPrefUtils.sharedPref.getString(
					mPrefUtils.KEY_USR_PASSWORD, mPrefUtils.DEF_USR_PASSWORD));
			userProfile_l.setUserEmailAddress(mPrefUtils.sharedPref.getString(
			        mPrefUtils.KEY_USR_EMAIL, mPrefUtils.DEF_USR_EMAIL));
			                
			if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
			    frontOfficeServer.login(userProfile_l);
            }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
                mTotalAgilityServer.login(userProfile_l);
            }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
                mTotalAgilityServer.login(userProfile_l);
            }
		}

		
	}
	
	//! Initiates Anonymous login by invoking getDeviceProfile() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
		/**
		 
		 * @param pHandler
		 * @throws KmcException
		 * @throws KmcRuntimeException
		 * @throws SocketTimeoutException
		 */
		public void anonymousLogin(Handler pHandler) throws KmcException,
		KmcRuntimeException, SocketTimeoutException {
			mCallerHandler = pHandler;
				if(mPrefUtils.isUsingKofax()){
					frontOfficeServer.loginAnonymously();
					return;
				}
				if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
				    frontOfficeServer.loginAnonymously();
	            }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
	                mTotalAgilityServer.loginAnonymously();
	            }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
	                mTotalAgilityServer.loginAnonymously();

			}

			
		}
	//! Initiates logout by invoking logout() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
	public void logout(Handler pHandler) throws KmcException {
		mCallerHandler = pHandler;
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
                mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
		
		if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
		    frontOfficeServer.logout();
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
            mTotalAgilityServer.logout();
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
            mTotalAgilityServer.logout();
        }
	}

	//! Initiates submit by invoking submitDocument() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
	public void submitDocument(Handler pHandler,Document doc) throws KmcRuntimeException, KmcException {
		mCallerHandler = pHandler;Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
                mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
        
        if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
		    frontOfficeServer.submitDocument(doc);
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
            mTotalAgilityServer.submitDocument(doc);
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
            mTotalAgilityServer.submitDocument(doc);
        }
	}

	//! Cancels submit by invoking cancel() method on FrontOfficeServer or TotalAgilityServer based on current server type used.
	public void cancelSubmitDocument() throws KmcException{
	    Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
            mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
    
    if( usekofax || mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
	        frontOfficeServer.cancel();
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
            mTotalAgilityServer.cancel();
        }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
            mTotalAgilityServer.cancel();
        }
		
	}

	//! Initiates registration on FrontOfficeServer or TotalAgilityServer based on current server type used.
	public void registerDevice(Handler pHandler, boolean retry) throws KmcException,
	KmcRuntimeException {
		mCallerHandler = pHandler;
		updateUrl(retry);	
	}

	public void cleanup() {

		Log.i(TAG, "Enter:: Cleanup");
		pSelf = null;
		mCallerHandler = null;
		if (frontOfficeServer != null) {
			frontOfficeServer.removeKfsSessionStateEventListener(this);
			frontOfficeServer.removeKfsDocTypeResultEventListener(this);
			frontOfficeServer = null;
		}
		mTotalAgilityServer = null;

	}
	
	// - private nested classes (more than 10 lines)

	
	// - private methods
	private void updateUrl(boolean retry) throws KmcException,KmcRuntimeException{
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
				mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
		String url_str = "";
		if (usekofax) {
			url_str = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_URL, mPrefUtils.DEF_KFX_URL);
			if(url_str != null && url_str.length() > 0){
				frontOfficeServer.setServerURL(url_str);
			}else{
				url_str = formUrlFromUserCredentials(usekofax, retry);
				frontOfficeServer.setServerURL(url_str);
			}
			 frontOfficeServer.registerDevice();
		} else {
			url_str = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_URL, mPrefUtils.DEF_USR_URL);
			
			if((url_str == null) || (url_str.equals(""))){
				url_str = formUrlFromUserCredentials(usekofax, retry);
			}
			Log.e(TAG, "url_str :: " + url_str);
			Log.e(TAG, "Current session state :: " + frontOfficeServer.getSessionState().toString());
			if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
			    frontOfficeServer.setServerURL(url_str);
			    frontOfficeServer.registerDevice();
            }else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
                mTotalAgilityServer.setServerURL(url_str);
                mTotalAgilityServer.setServerTimeout(120);
                mTotalAgilityServer.registerDevice();
            }
            else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
            	mTotalAgilityServer.setServerTimeout(100);
                mTotalAgilityServer.setServerURL(url_str);
                mTotalAgilityServer.registerDevice();
            }else{           	
            	throw new KmcException(ErrorInfo.KMC_LO_INVALID_SERVER_URL);
            }
		}
	}
	
	private String formUrlFromUserCredentials(Boolean isKofaxServer, boolean retry){
		String url = "";
		if(isKofaxServer){
			url = mPrefUtils.DEF_KFX_URL;
		}else{
			if(mPrefUtils.getPrefValueBoolean(mPrefUtils.KEY_USR_SSL,false)){
				url = "https://"; 
			}else{
				url = "http://";
			}
			if(mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PORT) != "") {
				url += mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_HOSTNAME)+":"+mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_PORT)+"/";
			}
			else {
				url += mPrefUtils.getPrefValueString(mPrefUtils.KEY_USR_HOSTNAME)+"/";
			}
			if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KFS.name())){
				if(!retry) {
					url += "Kofax/KFS/legacy/ws/";
				}
				else {
					url += "KFS/axis2/services/";
				}
			}else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA.name())){
			//	url += "TA/axis2/services/";
				url += "TotalAgility/kofax/kfs/legacy/ws/";
			}else if(mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_SERVER_TYPE, mPrefUtils.DEF_KFX_SERVER_TYPE).equals(serverType.KTA_AZURE.name())){
				url += "kofax/kfs/legacy/ws/";
			}
		}
		return url;
	}
	@Override
	public SSLSocketFactory getSSLSocketFactory(String hostname) {
		
        return  HttpsURLConnection.getDefaultSSLSocketFactory();
	}

}
