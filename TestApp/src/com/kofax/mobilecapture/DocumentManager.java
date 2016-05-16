// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kofax.kmc.ken.engines.data.BasicSettingsProfile;
import com.kofax.kmc.klo.logistics.KfsSessionStateEvent;
import com.kofax.kmc.klo.logistics.SessionState;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.ResultState;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

/// This centralized class to handle all the mechanisms related with queuing and download of Document object, keeping track of opened document, pending documents, etc.  

public class DocumentManager {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = DocumentManager.class.getSimpleName();

	// - Private data.
	private static volatile DocumentManager pSelf = null;

	/* SDK objects */

	/* Application objects */
	private ServerManager mServerMgr;
	private UtilityRoutines mUtilRoutines = null;
	private PrefManager mPrefUtils = null;
	//	private Globals globalObj = null;

	/* Standard variables */
	private Context mContext;
	private DocumentTypeObjectDownloadTask loadDocTypeTask = null;
	private Handler mCallerHandler = null;

	private ArrayList<DocumentType> arrDocumentTypeRefObj = null;
	//private ArrayList<Page> pagelist = null;
	private ArrayList<String> imgUrlList = null;
	private ArrayList<Integer> docTypeRequestQueue = null;
	private ArrayList<String> mAllDocumentList;
	private ArrayList<String> mNonHelpDocumentList;
	private ArrayList<String> mHelpDocumentList;
	private String mOpenedDocName = null; 	// Contains unique name(created using document and timestamp) saved on disk of current item being created/modified.
	private int currentDocTypeIndex;	// Contains the index of the type from the new-tab.
	private int requestedDocTypeIndex = -1;
	private SessionState kfsprevState;
	private boolean isDownloaderIdle = true;

	// - public constructors

	// - private constructors

	// - Private constructor prevents instantiation from other classes
	private DocumentManager(Context context) {
		mContext = context;

		mServerMgr = ServerManager.getInstance();
		mUtilRoutines = UtilityRoutines.getInstance();
		mPrefUtils = PrefManager.getInstance();
	}

	// - public getters and setters
	// - public methods
	//! The factory method.
	public static DocumentManager getInstance(Context context) {
		if (pSelf == null) {
			synchronized (DocumentManager.class) {
				if (pSelf == null) {
					pSelf = new DocumentManager(context);		
				}
			}
		}
		return pSelf;
	}

	//! Get array list of DocumentTypes downlaoded and saved as a reference by application.
	/**
	 * @return ArrayList of DocumentType
	 */
	public ArrayList<DocumentType> getDocTypeReferenceArray() {
		return arrDocumentTypeRefObj;
	}

	//! Get DocymentType from the reference array at a specified index. 
	/**
	 * @return Function returns DocumentType object if available at specified index, null otherwise.
	 */
	public DocumentType getDocTypeFromRefArray(int arrayIndex) {
		DocumentType docTypeObj = null;
		if (arrDocumentTypeRefObj == null) {
			return null;
		}

		if(arrayIndex < arrDocumentTypeRefObj.size()) {
			docTypeObj = arrDocumentTypeRefObj.get(arrayIndex); 
		}
		return docTypeObj;
	}

	//! Get list of document type names by requesting it from server. 
	/**
	 * DocumentManager request server the list of DocumentType names which are present under the currently logged in user.
	 * The same list of documentType names is displayed on home screen of application in the same order.
	 * 
	 * @return List of DocumentType names.
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> downloadDocTypeNamesList() {
		
		clearAllList();
		
		if(mNonHelpDocumentList == null) {			
			mNonHelpDocumentList = new ArrayList<String>();
		}
		if(mHelpDocumentList == null){		
			mHelpDocumentList = new ArrayList<String>();
		}
		
		if(mAllDocumentList == null){		
			mAllDocumentList = new ArrayList<String>();
		}
		
		List<String> cloneDocList = mServerMgr.getDocTypeNamesFromServer();
		if(mPrefUtils.isUsingKofax()){
		for(int i = 0;i< cloneDocList.size();i++){
			if(cloneDocList.get(i).contains(Constants.STR_ASSIST)){
				mHelpDocumentList.add(cloneDocList.get(i));
			}
			else{
				mNonHelpDocumentList.add(cloneDocList.get(i));
			}
		}
		if(!mNonHelpDocumentList.isEmpty()){
			mAllDocumentList = (ArrayList<String>) mNonHelpDocumentList.clone();
		}
		if(!mHelpDocumentList.isEmpty()){
			for(String docName : mHelpDocumentList){
				mAllDocumentList.add(docName);
		}
		}
		}else{
			mAllDocumentList = (ArrayList<String>) cloneDocList;
			mNonHelpDocumentList = mAllDocumentList;
		}		
		return mNonHelpDocumentList;
	}

	//! Get the index of currently selected documentType name. 
	/**
	 * Application maintains an index of DocumentType any active document. 
	 * The index is selected based on comparing DocumentType of active document with list containing all the downloaded documentType names for the currently logged in user.     
	 * 
	 * @return List of DocumentType names.
	 */
	public int getCurrentDocTypeIndex() {
		return currentDocTypeIndex;
	}

	//! Get the index of currently selected documentType name. 
	/**
	 * Application maintains an index of DocumentType any active document. 
	 * The index is selected based on comparing DocumentType name of active(new or pending) document with list containing all the downloaded documentType names for the currently logged in user.     
	 * The index is set everytime when any new document is created or any pending document is opened.
	 *  
	 * @return index of active documentType name from list of valid documentType names downloaded from server.
	 */
	public void setCurrentDocTypeIndex(int currentDocTypeIndex) {
		this.currentDocTypeIndex = currentDocTypeIndex;
	}

	//! Get list of helpkofax document type names. 
		/**
		 * Application maintains a list of helpkofax DocumentType names which are present under the currently logged in user by requesting it to server.
		 * The same list of documentType names is displayed on HelpKofaxActivity of application in the same order.
		 * 
		 * @return List of DocumentType names.
		 */
	
	public ArrayList<String> getHelpKofaxDocTypes(){
		return mHelpDocumentList;
	}
	
	//! Check the open document belongs to HelpKofax category or not.
	
	/***
	 * This method tells whether the current document type belongs to HelpKofax category or not.
	 * @return This method returns true if the document type falls under HelpKofax category else returns false. 
	 */
	
	public boolean isHelpKofaxDocType(){
		//TODO: Need to change based on helpkofax tag
		if(mPrefUtils.isUsingKofax() && getOpenedDoctName() != null && getOpenedDoctName().contains(Constants.STR_ASSIST)){
			return true;
		}
		return false;
	}
	
	//! Set list of Help document type names. 
			/**
			 * Application maintains a list of Help DocumentType names which are present under the currently logged in(online/offline) user by requesting it to server.
			 * The same list of Help documentType names is displayed on help screen of application in the same order.
			 * 
			 */
	
	public void setHelpDocumentNamesList(ArrayList<String> list) {
		mHelpDocumentList = list;
	}
	
	//! Set list of non Help document type names. 
	/**
	 * Application maintains a list of non Help DocumentType names which are present under the currently logged in(online/offline) user by requesting it to server.
	 * The same list of non Help documentType names is displayed on Home screen of application in the same order.
	 * 
	 */
	
	public void setNonHelpDocumentNamesList(ArrayList<String> list) {
		mNonHelpDocumentList = list;
	}

	
	//! Get list of non helpkofax document type names. 
			/**
			 * Application maintains a list of non helpkofax DocumentType names which are present under the currently logged in user by requesting it to server.
			 * The same list of documentType names is displayed on Home screen of application in the same order.
			 * 
			 * @return List of DocumentType names.
			 */
	public ArrayList<String> getNonHelpDocumentNamesList() {
		return mNonHelpDocumentList;
	}
	
	//! Get list of document type names. 
	/**
	 * Application maintains a list of DocumentType names which are present under the currently logged in user by requesting it to server.
	 * 
	 * @return List of DocumentType names.
	 */
	public ArrayList<String> getDocTypeNamesList() {
		return mAllDocumentList;
	}

	//! Set list of document type names. 
		/**
		 * Application maintains a list of DocumentType names which are present under the currently logged in(online/offline) user by requesting it to server.
		 * The same list of documentType names is displayed on home screen of application in the same order.
		 * 
		 */
		public void setDocTypeNamesList(ArrayList<String> list) {
			mAllDocumentList = list;
		}
	

	//! Download DocumentType object from server if not downloaded in the current instance of application. 
	/**
	 * Function to check if the documentType object for the selected type is
	 * already downloaded. If not, enqueue the index of selected DocumentType name
	 * to download documentType object.
	 * @param index
	 * @return ResultState
	 * @throws KmcRuntimeException
	 * @throws KmcException
	 */
	public Globals.ResultState downloadDocTypeObject(int index) throws KmcRuntimeException, KmcException {
		Globals.ResultState err = ResultState.RESULT_OK;
		//Log.i(TAG, "Enter: downloadDocTypeObject");
		// check if documentType object is already downloaded, if not, enqueue
		// doctype-index for download
		if (getDocTypeFromRefArray(index) == null) {
			err = enqueueForDownload(index);
			if(err != ResultState.RESULT_OK) {
				Log.e(TAG, "Error: " + err);
			}
		} else {
			Log.i(TAG,
					"DocumentType object for this type is already downloaded....");
		}
		return err;
	}

	//! Get index of specified DocumentType name from the maintained list of supported DocumentType names for the currently logged in user.  
	/**
	 * @param documentTypeName
	 * @return index > 0 if specified DocumentType name is present in list, -1 otherwise.
	 */
	public int findDocumentTypeIndex(String documentTypeName) {
		int index = -1;
		if (mAllDocumentList != null) {
			int length = mAllDocumentList.size();
			for(int i=0; i<length; i++) {
				if (mAllDocumentList.get(i).equals(documentTypeName)) {
					index = i;
					break;
				}
			}
		}
		return index;
	}

	//! Compare processing parameters(ImagePerfectionProfile and BasicSettingProfile) of of two documentType objects.  
	/**
	 * Function gives priority to ImagePerfectionProfile first. If both ImagePerfectionProfile are matching, it does not compare BasicSettingProfile.
	 * If ImagePerfectionProfile are empty, function compares BasicSettingProfile.
	 * 
	 * @param docType1
	 * @param docType2
	 * @return true required profiles match, false otherwise.
	 */
	public boolean compareProcessingParams(DocumentType docType1, DocumentType docType2) {
		Log.e(TAG, "Enter :: compareProcessingParams");
		boolean isMatching = true;
		if(docType1 == null || docType2 == null) {
			return false;
		}
		Log.e(TAG, "docType1 typename :: " + docType1.getTypeName());
		Log.e(TAG, "docType2 typename :: " + docType2.getTypeName());
		// if both IPP are null, compare basic setting profiles.
		if(docType1.getImagePerfectionProfile() == null && docType2.getImagePerfectionProfile() == null) {
			Log.e(TAG, "both IPP are null, comparing BSP");

			BasicSettingsProfile oldBSP = docType1.getBasicSettingsProfile();
			BasicSettingsProfile newBSP = docType2.getBasicSettingsProfile();

			Log.d(TAG, "oldBSP.getDoDeskew() :: " + oldBSP.getDoDeskew() + " newBSP.getDoDeskew():: " + newBSP.getDoDeskew());
			Log.d(TAG, "oldBSP.getCropType() :: " + oldBSP.getCropType() + " newBSP.getCropType():: " + newBSP.getCropType());
			Log.d(TAG, "oldBSP.getInputDocLongEdge() :: " + oldBSP.getInputDocLongEdge() + " newBSP.getInputDocLongEdge():: " + newBSP.getInputDocLongEdge());
			Log.d(TAG, "oldBSP.getInputDocShortEdge() :: " + oldBSP.getInputDocShortEdge() + " newBSP.getInputDocShortEdge():: " + newBSP.getInputDocShortEdge());
			Log.d(TAG, "oldBSP.getOutputBitDepth() :: " + oldBSP.getOutputBitDepth() + " newBSP.getOutputBitDepth():: " + newBSP.getOutputBitDepth());
			Log.d(TAG, "oldBSP.getOutputDPI() :: " + oldBSP.getOutputDPI() + " newBSP.getOutputDPI():: " + newBSP.getOutputDPI());
			Log.d(TAG, "oldBSP.getRotateType() :: " + oldBSP.getRotateType() + " newBSP.getRotateType():: " + newBSP.getRotateType());

			if(isMatchingDeskew(oldBSP, newBSP) && 
					isMatchingCropType(oldBSP, newBSP) && 
					isMatchingDocLongEdge(oldBSP, newBSP) && 
					isMatchingDocShortEdge(oldBSP, newBSP) && 
					isMatchingBitDepth(oldBSP, newBSP) && 
					isMatchingOutputDPI(oldBSP, newBSP) && 
					isMatchingRotateType(oldBSP, newBSP) && 
					isMatchingCroppingTetragon(oldBSP, newBSP)) {
				isMatching = true;
				Log.e(TAG, "BSP matching!");
			}
			else {
				isMatching = false;
				Log.e(TAG, "BSP NOT matching");
			}
		}
		//match image perfection profile
		else if(docType1.getImagePerfectionProfile() != null && docType2.getImagePerfectionProfile() != null) {
			if(!(docType1.getImagePerfectionProfile().getIpOperations().equals(docType2.getImagePerfectionProfile().getIpOperations()))) {
				Log.d(TAG, "oldBSP.IPP() :: " + docType1.getImagePerfectionProfile().getIpOperations() + "\n newBSP.IPP():: " + docType2.getImagePerfectionProfile().getIpOperations());

				Log.e(TAG, "both IPP are NOT null, and NOT matching");
				isMatching = false;
			}
		}
		else {
			isMatching = false;
		}

		Log.e(TAG, "Exit :: compareProcessingParams");
		return isMatching;
	}

	
	//check Field validation,if it returns true required fields are null
	public boolean validateDocumentFields(Document doc){
		boolean isReq = false;
		if(doc != null){
			for (int i = 0; i < doc.getDocumentType().getFieldTypes().size(); i++) {
				if (doc.getFields().get(i).getFieldType().isRequired()
						&& !doc.getFields().get(i).getFieldType().isHidden()
						&& !doc.getFields()
								.get(i)
								.getFieldType()
								.getDisplayName()
								.trim()
								.equalsIgnoreCase(
										Constants.STR_MOBILE_DEVICE_EMAIL)) {
					if (!((doc.getFields().get(i).getValue() != null && doc
							.getFields().get(i).getValue().length() > 0) || (doc
							.getDocumentType().getFieldTypes().get(i)
							.getDefault() != null && doc.getDocumentType()
							.getFieldTypes().get(i).getDefault().length() > 0))) {
						isReq = true;
					}
				}
			}
		}
		return isReq;
	}
	
	//! Get currently active document name.  
	/**
	 * DocumentManagers maintains and gives the currently active document name. 
	 * 
	 * @return the unique name of currently opened document/item
	 */
	public String getOpenedDoctName() {
		return mOpenedDocName;
	}

	//! Set currently active document name.  
	/**
	 * DocumentManagers maintains currently active document name
	 * 
	 * @param currentSelectedDocName
	 */
	public void setOpenedDocName(String currentSelectedDocName) {
		this.mOpenedDocName = currentSelectedDocName;
	}

	/**
	 * @return the imgUrlList
	 */
	public ArrayList<String> getImgUrlList() {
		if(imgUrlList == null) {
			imgUrlList = new ArrayList<String>();
			setImgUrlList(imgUrlList);
		}
		return imgUrlList;
	}

	/**
	 * @param imgUrlList the imgUrlList to set
	 */
	public void setImgUrlList(ArrayList<String> imgUrlList) {
		this.imgUrlList = imgUrlList;
	}

	//! Set the caller MessageHandler parameter. 
	/**
	 * This is to be used by DocumentManager to send the required result back to the caller.
	 * @param handler
	 */
	public void setCurrentHandler(Handler handler){
		if(handler != null){
			mCallerHandler = handler;
		}
	}

	//! Reset caller MessageHandler parameter. 
	public void removeCurrenthandler(){
		mCallerHandler = null;
	}

	//! Clear and reset all internally maintained document names list, array of downloaded DocumentTypes and request queue of DocumentType download. 
	public void reset() {
		if (mAllDocumentList != null) {
			mAllDocumentList = null;
			mAllDocumentList = new ArrayList<String>();
		}

		if (arrDocumentTypeRefObj != null) {
			arrDocumentTypeRefObj.clear();
			arrDocumentTypeRefObj = null;
		}

		if (docTypeRequestQueue != null) {
			docTypeRequestQueue.clear();
			docTypeRequestQueue = null;
		}
	}

	//! Clear and reset all internally maintained parameters of DocumentManager. 
	public void cleanup() {

		Log.i(TAG, "Enter:: Cleanup");
		if(loadDocTypeTask != null) {
			loadDocTypeTask.cancel(true);
		}

		if (mAllDocumentList != null) {
			mAllDocumentList = new ArrayList<String>();		
		}

		if (arrDocumentTypeRefObj != null) {
			arrDocumentTypeRefObj.clear();
		}

		if (docTypeRequestQueue != null) {
			docTypeRequestQueue.clear();
			docTypeRequestQueue = null;
		}
		mCallerHandler = null;
		mySessionStateHandler = null;

		isDownloaderIdle = true;

		pSelf = null;
		System.gc();
	}

	// - private nested classes (more than 10 lines)

	/*
	 * Async task to get Document type Objects for the specified document name
	 * From server
	 */
	private class DocumentTypeObjectDownloadTask extends
	AsyncTask<String, Void, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			String result = "";
			Log.e(TAG, "Enter: doInBackground");

			Log.e(TAG,
					"Document name is :: " + params[0]);
			try {
				mServerMgr.downloadDocumentType(mySessionStateHandler, params[0]);
			} catch (KmcException e) {
				isDownloaderIdle = true;
				result = e.getCause().getMessage();
				e.printStackTrace();
			}catch (KmcRuntimeException e){
				isDownloaderIdle = true;
				result = e.getCause().getMessage();
				e.printStackTrace();
			}

			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
		}
	}

	// - private methods
	private Handler mySessionStateHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {

			KfsSessionStateEvent arg0 = (KfsSessionStateEvent) msg.obj;

			Log.i(TAG, "arg0.getSessionState() ::: " + arg0.getSessionState());

			switch (arg0.getSessionState()) {

			case SESSION_LOGGED_IN:
				if(kfsprevState == SessionState.SESSION_GETTING_DOCUMENT_FIELDS || kfsprevState == SessionState.SESSION_GETTING_IP_SETTINGS ||
				kfsprevState == SessionState.SESSION_PREPARING_DOCUMENT_TYPE || kfsprevState == SessionState.SESSION_DOCUMENT_TYPE_READY){
					if(arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS){
						if(arg0.getErrorInfo() == ErrorInfo.KMC_LO_DOWNLOAD_DOCUMENT_FIELDS_ERROR) {
							isDownloaderIdle = true;
							if(mCallerHandler != null){
								Message msg1 = new Message();
								msg1.obj = arg0;
								msg1.what = Globals.Messages.MESSAGE_DOWNLOAD_DOCUMENTS_FAILED.ordinal();
								msg1.arg1 = requestedDocTypeIndex;
								mCallerHandler.sendMessage(msg1);
							}
							else {
								Log.e(TAG, "Error:: callerHandler is NULL!!");
							}
							if((docTypeRequestQueue.size() > 0) && (requestedDocTypeIndex == docTypeRequestQueue.get(0))){
								return true;
							}
						}
						removeDocTypeRequestQueueIndex();
						if ((docTypeRequestQueue != null) && (!docTypeRequestQueue.isEmpty()) && (docTypeRequestQueue.size() > 0)) {
							Log.i(TAG,
									"Doc Type Request Queue is not EMPTY yet, requesting DocumentType object for next type "
											+ docTypeRequestQueue.size());
							if(docTypeRequestQueue.size() > 0){
							requestedDocTypeIndex = docTypeRequestQueue.get(0);
							getDocumentTypeObject(requestedDocTypeIndex);
						}
					}
				}
				}
				break;
			case SESSION_GETTING_DOCUMENT_FIELDS:
			case SESSION_GETTING_IP_SETTINGS:
			case SESSION_PREPARING_DOCUMENT_TYPE:
				kfsprevState = arg0.getSessionState();
				if(arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS){
					isDownloaderIdle = true;
				}
				break;
			case SESSION_DOCUMENT_TYPE_READY:
				Log.e(TAG, "Received SESSION_DOCUMENT_TYPE_READY!!!!!!!!!!!!!!!!!!!!!!");
				kfsprevState = arg0.getSessionState();
				if(arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS){
					sendBroadcast(mAllDocumentList.get(requestedDocTypeIndex), arg0.getErrorInfo().getErrMsg());
				}
				else {
					DocumentType doc = mServerMgr.getRecentDocTypeObj();
					if(doc != null) {
						addToDocTypeRefArray(docTypeRequestQueue.get(0), doc);
						sendBroadcast(doc.getTypeName(), null);
					}
					else {
						Log.e(TAG, "DocumentType object is NULL");
					}
				}
				isDownloaderIdle = true;
				break;
			default:
				break;
			}

			if (((arg0.getSessionState() == SessionState.SESSION_REGISTERING || arg0
					.getSessionState() != SessionState.SESSION_LOGGING_IN) && (arg0
							.getErrorInfo() != ErrorInfo.KMC_SUCCESS))
							|| (arg0.getSessionState() != SessionState.SESSION_REGISTERING && arg0
							.getSessionState() != SessionState.SESSION_LOGGING_IN)) {

				if(mCallerHandler != null){
					Message msg1 = new Message();
					msg1.obj = arg0;
					mCallerHandler.sendMessage(msg1);
				}
				else {
					Log.e(TAG, "Error:: callerHandler is NULL!!");
				}
			} else {
				Log.d(TAG, "Did not send message back to the caller");
			}

			if (arg0.getSessionState() == SessionState.SESSION_DOCUMENT_TYPE_READY) {
				if ((docTypeRequestQueue != null) && (!docTypeRequestQueue.isEmpty())) {
					removeDocTypeRequestQueueIndex();
					if ((!docTypeRequestQueue.isEmpty()) && (docTypeRequestQueue.size() > 0)) {
						Log.i(TAG,
								"Doc Type Request Queue is not EMPTY yet, requesting DocumentType object for next type "
										+ docTypeRequestQueue.size());
						Log.i(TAG, "Next type to be requested is ===> "
								+ mAllDocumentList.get(docTypeRequestQueue.get(0)));

						requestedDocTypeIndex = docTypeRequestQueue.get(0);
						getDocumentTypeObject(requestedDocTypeIndex);
					}
				}
			}

			return true;
		}
	});

	private void addToDocTypeRefArray(int index, DocumentType object) {

		Log.i(TAG,
				"Document names list size =============================================> "
						+ mAllDocumentList.size());
		Log.i(TAG,
				"Adding at index ============================= " + index);

		if (arrDocumentTypeRefObj == null) {
			Log.i(TAG,
					"Allocating array *********************************************");
			arrDocumentTypeRefObj = new ArrayList<DocumentType>(
					mAllDocumentList.size());
			for (int i = 0; i < mAllDocumentList.size(); i++) {
				arrDocumentTypeRefObj.add(i, null);
			}
		}
		Log.i(TAG,
				"Document names list size =============================================> "
						+ arrDocumentTypeRefObj.size());
		arrDocumentTypeRefObj.set(index, object);
	}

	private void removeDocTypeRequestQueueIndex(){
		if(docTypeRequestQueue != null && (!docTypeRequestQueue.isEmpty())){
			//update queue once object is downloaded
			docTypeRequestQueue.remove(0);  //remove index of downloaded object to schedule next index 
			requestedDocTypeIndex = -1;
		}
	}

	private void getDocumentTypeObject(int index) {
		Log.i(TAG, "Enter: getDocumentTypeObject");

		loadDocTypeTask = new DocumentTypeObjectDownloadTask();

	 if(mAllDocumentList != null && mAllDocumentList.size() > index) {
			isDownloaderIdle = false;
			loadDocTypeTask.execute(mAllDocumentList.get(index));
		}
	}


	private Globals.ResultState enqueueForDownload(Integer index) throws KmcRuntimeException,
	KmcException {
		Globals.ResultState result = ResultState.RESULT_OK;
		if(!mUtilRoutines.checkInternet(mContext)) {
			result = ResultState.RESULT_FAILED;
		}
		if (docTypeRequestQueue == null) {
			docTypeRequestQueue = new ArrayList<Integer>();
		}

		if (((isAlreadyEnqueued(index)) && (index != requestedDocTypeIndex)) ||
				(!isAlreadyEnqueued(index))){
			// prepend index always at first position in array to set it to be
			// picked immediately after current object is downloaded.
			if (docTypeRequestQueue.size() >= 1) {
				docTypeRequestQueue.add(1, index);
			}
			else {
				docTypeRequestQueue.add(0, index);
			}
		}
		else {
			//Log.e(TAG, "Document object for the type is already being downloaded.");
		}
		if(loadDocTypeTask != null) {
			//Log.i(TAG, "loadDocTypeTask.getStatus() ==> " + loadDocTypeTask.getStatus());
			//Log.i(TAG, "isDownloaderIdle ==> " + isDownloaderIdle);
		}

		// if no download is in progress currently, schedule one.
		if(loadDocTypeTask == null || /*loadDocTypeTask.getStatus() == Status.FINISHED || */ isDownloaderIdle) {
			// if the queue contains only the
			// currently added index,
			// initiate object download from
			// here
			Log.i(TAG, "loadDocTypeTask ::" + loadDocTypeTask);
			if(docTypeRequestQueue.size() > 0 ) {
				requestedDocTypeIndex = docTypeRequestQueue.get(0);
				getDocumentTypeObject(requestedDocTypeIndex);
			}
		
		} else {
			result = ResultState.RESULT_FAILED;
		}
		return result;
	}


	private boolean isMatchingDeskew(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		return (bsp1.getDoDeskew() == bsp2.getDoDeskew());
	}

	private boolean isMatchingCropType(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getCropType() != null && bsp2.getCropType() != null) {
			if(!bsp1.getCropType().equals(bsp2.getCropType())) {
				isMatching = false;
			}
		}
		else if((bsp1.getCropType() == null && bsp2.getCropType() != null) || 
				(bsp1.getCropType() != null && bsp2.getCropType() == null)){
			isMatching = false;
		}
		Log.e(TAG, "CropType matching :: " + isMatching);
		return isMatching;
	}

	private boolean isMatchingDocLongEdge(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getInputDocLongEdge() != null && bsp2.getInputDocLongEdge() != null) {
			if(!bsp1.getInputDocLongEdge().equals(bsp2.getInputDocLongEdge())) {
				isMatching = false;
			}
		}
		else if((bsp1.getInputDocLongEdge() == null && bsp2.getInputDocLongEdge() != null) || 
				(bsp1.getInputDocLongEdge() != null && bsp2.getInputDocLongEdge() == null)){
			isMatching = false;
		}
		Log.e(TAG, "DocLongEdge matching :: " + isMatching);
		return isMatching;
	}

	private boolean isMatchingDocShortEdge(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getInputDocShortEdge() != null && bsp2.getInputDocShortEdge() != null) {
			if(!bsp1.getInputDocShortEdge().equals(bsp2.getInputDocShortEdge())) {
				isMatching = false;
			}
		}
		else if((bsp1.getInputDocShortEdge() == null && bsp2.getInputDocShortEdge() != null) || 
				(bsp1.getInputDocShortEdge() != null && bsp2.getInputDocShortEdge() == null)){
			isMatching = false;
		}
		Log.e(TAG, "DocShortEdge matching :: " + isMatching);
		return isMatching;
	}

	private boolean isMatchingBitDepth(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getOutputBitDepth() != null && bsp2.getOutputBitDepth() != null) {
			if(!bsp1.getOutputBitDepth().equals(bsp2.getOutputBitDepth())) {
				isMatching = false;
			}
		}
		else if((bsp1.getOutputBitDepth() == null && bsp2.getOutputBitDepth() != null) || 
				(bsp1.getOutputBitDepth() != null && bsp2.getOutputBitDepth() == null)){
			isMatching = false;
		}
		Log.e(TAG, "OutputBitDepth matching :: " + isMatching);
		return isMatching;
	}

	private boolean isMatchingOutputDPI(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getOutputDPI() != null && bsp2.getOutputDPI() != null) {
			if(!bsp1.getOutputDPI().equals(bsp2.getOutputDPI())) {
				isMatching = false;
			}
		}
		else if((bsp1.getOutputDPI() == null && bsp2.getOutputDPI() != null) || 
				(bsp1.getOutputDPI() != null && bsp2.getOutputDPI() == null)){
			isMatching = false;
		}
		Log.e(TAG, "OutputDPI matching :: " + isMatching);
		return isMatching;
	}

	private boolean isMatchingRotateType(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getRotateType() != null && bsp2.getRotateType() != null) {
			if(!bsp1.getRotateType().equals(bsp2.getRotateType())) {
				isMatching = false;
			}
		}
		else if((bsp1.getRotateType() == null && bsp2.getRotateType() != null) || 
				(bsp1.getRotateType() != null && bsp2.getRotateType() == null)){
			isMatching = false;
		}
		Log.e(TAG, "getRotateType matching :: " + isMatching);
		return isMatching;
	}

	private boolean isMatchingCroppingTetragon(BasicSettingsProfile bsp1, BasicSettingsProfile bsp2) {
		boolean isMatching = true;
		if(bsp1.getCroppingTetragon() != null && bsp2.getCroppingTetragon() != null) {
			if(!bsp1.getCroppingTetragon().equals(bsp2.getCroppingTetragon())) {
				isMatching = false;
			}
		}
		else if((bsp1.getCroppingTetragon() == null && bsp2.getCroppingTetragon() != null) || 
				(bsp1.getCroppingTetragon() != null && bsp2.getCroppingTetragon() == null)){
			isMatching = false;
		}
		Log.e(TAG, "getRotateType matching :: " + isMatching);
		return isMatching;
	}



	private void sendBroadcast(String docType, String errorMessage){
		Intent i = new Intent(Constants.CUSTOM_INTENT_DOCTYPE_DOWNLOADED).putExtra(Constants.STR_DOCUMENT_TYPE, docType);
		i.putExtra(Constants.STR_ERROR_MESSAGE, errorMessage);
		mContext.sendBroadcast(i);
	}

	private boolean isAlreadyEnqueued(Integer index) {
		//Log.i(TAG, "isAlreadyEnqueued : index => " + index);
		boolean isPresent = false;
		if ((docTypeRequestQueue != null) && (!docTypeRequestQueue.isEmpty())) {
			int len = docTypeRequestQueue.size();
			for (int i=0; i<len; i++) {
				if (i < docTypeRequestQueue.size() && docTypeRequestQueue.get(i).equals(index)) {
					isPresent = true;
					break;
				}
			}
		}
		//Log.i(TAG, "isPresent : " + isPresent);
		return isPresent;
	}
	
	private void clearAllList(){
		if(mNonHelpDocumentList != null) {
			mNonHelpDocumentList.clear();
		}
		if(mHelpDocumentList != null){
			mHelpDocumentList.clear();
		}		
		if(mAllDocumentList != null){
			mAllDocumentList.clear();
		}
	}
}
