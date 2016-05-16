// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.kmc.klo.logistics.KfsSessionStateEvent;
import com.kofax.kmc.klo.logistics.SessionState;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.FieldType;
import com.kofax.kmc.klo.logistics.data.FieldType.DataType;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.Initializer;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.CustomUrlUtils;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.Globals.ResultState;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class EditFieldsActivity extends Activity {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants

	// - Private data.
	/* SDK objects */
	Document mDocumentObj = null;
	private SessionState prevState;

	/* Application objects */
	private UtilityRoutines mUtilRoutines = null;
	private DatabaseManager mItemDBManager = null;
	private PrefManager mPrefUtils = null;
	private DocumentManager mDocMgr = null;
	private DiskManager mDiskMgr = null;
	private CustomUrlUtils mCustomUrlUtils = null;

	/* Standard variables */
	private final String TAG = EditFieldsActivity.class.getSimpleName();
	private BroadcastReceiver mReceiver = null;
	private LinearLayout container;
	private DatePickerDialog mDatePickerDialog = null;
	private CustomDialog mCustomDialog = null;

	private View mProgressBar = null;
	private View mSelectedDateTextField = null;
	private Menu mMenu = null;

	private int[] mViewsIdArr = null;
	private int itemTypeIndex = -1;
	private int minMaxCount = 0;
	private boolean isChangingItemType = false;
	private boolean isrequiredFieldLeftEmpty = false;
	private boolean isFiledValidation = false;

	private List<EditText> mEditTextList = null;
	private EditText mCurrentFocusEdittext = null;
	private String mPrev_text = null;
	private int mFirstTimeDisplay = 0;
	private int mVisibleFieldCount = 0;
	private boolean mIsNewItem = true;

	Runnable myRunnable = null;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		new DeviceSpecificIssueHandler().checkEntryPoint(this);

		setContentView(R.layout.editfields);
		getActionBar().setTitle(getResources().getString(R.string.actionbar_lbl_screen_document_info));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		container = (LinearLayout) findViewById(R.id.container);
		mProgressBar = findViewById(R.id.pBarLoader);

		new Initializer().initiateManagersAndServices(getApplicationContext());
		mDocMgr = DocumentManager.getInstance(getApplicationContext());
		mDiskMgr = DiskManager.getInstance(getApplicationContext());
		mPrefUtils = PrefManager.getInstance();
		mItemDBManager = DatabaseManager.getInstance();
		mCustomDialog = CustomDialog.getInstance();
		mUtilRoutines = UtilityRoutines.getInstance();
		mCustomUrlUtils = CustomUrlUtils.getInstance();
		mEditTextList = new ArrayList<EditText>();
		if(getIntent().hasExtra(Constants.STR_IS_NEW_ITEM)){
			mIsNewItem =getIntent().getBooleanExtra(Constants.STR_IS_NEW_ITEM, true);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		if (!Initializer.getInstance().getEntryPointMain())
		{
			Log.e("RAJESH", "onPostCreate of EditFieldsActivity");
			Toast.makeText(this, "problem", Toast.LENGTH_LONG).show();
			return;
		}

		super.onPostCreate(savedInstanceState);

		// check if document object for this case is serialized in past
		ItemEntity itemEntity = mItemDBManager.getItemEntity();
		if (mItemDBManager.isDocumentSerializedInDB(itemEntity)) {
			mDocumentObj = (Document)mDiskMgr.byteArrayToDocument(itemEntity.getItemSerializedData());
		}

		Bundle b = getIntent().getExtras();
		if (b != null) {
			isChangingItemType = b.getBoolean(Constants.STR_CHANGE_ITEM_TYPE);
			isFiledValidation  = b.getBoolean(Constants.STR_VALIDATION);

		}
		if (isChangingItemType) {
			itemTypeIndex = b.getInt(Constants.STR_NEW_ITEM_INDEX);
		}
		else {
			itemTypeIndex = mDocMgr.getCurrentDocTypeIndex();
		}

		Log.i(TAG, "itemTypeIndex =====> " + itemTypeIndex);
		Log.i(TAG, "isChangingItemType =====> " + isChangingItemType);

		//check if documentType object is available, if not wait till download completes.
		if ((mDocMgr.getDocTypeReferenceArray() != null) && (mDocMgr.getDocTypeReferenceArray().get(itemTypeIndex) != null)) {
			//mProgressBar.setVisibility(View.VISIBLE);
			if (isChangingItemType) {
				mCustomDialog.showProgressDialog(EditFieldsActivity.this, getResources().getString(R.string.progress_msg_downloading_doc_details),true);				
			}
			else {
				mCustomDialog.showProgressDialog(EditFieldsActivity.this, getResources().getString(R.string.progress_msg_validating_fields),true);
			}
			startScreenLoadHandler();
		}
		else {
			//check internet connectivity before initiating document object download.
			if(!mUtilRoutines.checkInternet(this)) {
				Toast.makeText(this, getResources().getString(R.string.toast_no_network_connectivity) + "\n"+getResources().getString(R.string.toast_error_field_details_cannot_be_downloaded), Toast.LENGTH_LONG).show();
				invalidateOptionsMenu();
			}
			else {
				// wait until object is downloaded from KFS and signal is received in broadcast-receiver
				mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_downloading_doc_details),false);
				try {
					mDocMgr.setCurrentHandler(EditSessionStateHandler);
					ResultState resultState = downloadDocTypeObject(itemTypeIndex);
					if(resultState != ResultState.RESULT_OK) {
						mCustomDialog.closeProgressDialog();
					}
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_lbl) + e.getCause(), Toast.LENGTH_LONG).show();
					mCustomDialog.closeProgressDialog();
				} catch (KmcException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_lbl) + e.getCause(), Toast.LENGTH_LONG).show();
					mCustomDialog.closeProgressDialog();
				}
			}
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.edit_info_menu, menu);
		mMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent returnIntent = new Intent();
		switch (item.getItemId()) {
		case R.id.menu_save_fields:
			//mCustomDialog.showProgressDialog(this, "Saving changes. Please wait...",true);
			boolean result = SaveDocument();
			if(result){
				//set result for previous (Item-details) screen.
				returnIntent.putExtra(Constants.STR_VISIBLE_FIELD_COUNT, mVisibleFieldCount);
				setResult(Globals.ResultState.RESULT_OK.ordinal(), returnIntent);
				finish();
			}
			break;
		case android.R.id.home:
			onBackPressed();
		default:
			setResult(Globals.ResultState.RESULT_CANCELED.ordinal(), returnIntent);
			finish();
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(container.getChildCount() > 0) {
			//enable save
			mMenu.findItem(R.id.menu_save_fields).setEnabled(true);
			mMenu.findItem(R.id.menu_save_fields).setIcon(R.drawable.done);
		}
		else {
			//disable save
			mMenu.findItem(R.id.menu_save_fields).setEnabled(false);
			mMenu.findItem(R.id.menu_save_fields).setIcon(R.drawable.done_gray);
		}
		super.onPrepareOptionsMenu(menu);
		return true;
	}
	@Override
	protected void onPause() {
		super.onPause();
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerBroadcastReceiver();

	}

	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		//In case of new item, copy all the default values(if any) as object's actual values and serialize an the new object to db.
		if(mIsNewItem || isChangingItemType || (mDocumentObj != null && (!mItemDBManager.isDocumentSerializedInDB(mItemDBManager.getItemEntity()))) 
				|| ((mItemDBManager.getItemEntity().getFieldName() == null)))  {
			cloneOriginalDocument();
		}
		Intent returnIntent = new Intent();
		returnIntent.putExtra(Constants.STR_VISIBLE_FIELD_COUNT, mVisibleFieldCount);
		setResult(Globals.ResultState.RESULT_CANCELED.ordinal(), returnIntent);
		finish();
	}

	// - private nested classes (more than 10 lines)
	// - private methods

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == Globals.RequestCode.SUBMIT_DOCUMENT.ordinal()) {         
			// if image is accepted on preview screen
			if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
				setResult(Globals.ResultState.RESULT_OK.ordinal());
				finish();
			}
		}
	}

	private Handler EditSessionStateHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			KfsSessionStateEvent arg0 = (KfsSessionStateEvent) msg.obj;
			Log.i(TAG, "arg0.getSessionState() ::: " + arg0.getSessionState());
			switch (arg0.getSessionState()) {
			case SESSION_LOGGED_IN:
				if(prevState == SessionState.SESSION_GETTING_DOCUMENT_FIELDS || prevState == SessionState.SESSION_GETTING_IP_SETTINGS ||
				prevState == SessionState.SESSION_PREPARING_DOCUMENT_TYPE || prevState == SessionState.SESSION_DOCUMENT_TYPE_READY){
					if(arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS){
						if(mCustomDialog != null){
							mCustomDialog.closeProgressDialog();
						}
						// Toast.makeText(getApplicationContext(),arg0.getErrorInfo().getErrDesc(),Toast.LENGTH_SHORT).show();
					}
				}
				break;
			case SESSION_GETTING_DOCUMENT_FIELDS:
			case SESSION_GETTING_IP_SETTINGS:
			case SESSION_PREPARING_DOCUMENT_TYPE:
			case SESSION_DOCUMENT_TYPE_READY:
				prevState = arg0.getSessionState();
				if(arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS){
					Toast.makeText(getApplicationContext(),arg0.getErrorInfo().getErrDesc(),Toast.LENGTH_SHORT).show(); 
				}
				break;              
			default:
				break;
			}
			return true;
		}
	});

	private void startScreenLoadHandler() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mFirstTimeDisplay == 0) {
					boolean isObjectAlreadyCreated = true;
					if (mDocumentObj == null || mCustomUrlUtils.isUsingCustomUrl()) {
						isObjectAlreadyCreated = false;
						// if the document is being newly created(not serialized
						// ever before), create fresh document object and
						// display empty (with default values) fields.
						mDocumentObj = new Document(mDocMgr.getDocTypeReferenceArray().get(itemTypeIndex));
					}					

					Log.i(TAG, "getDocumentTypeObjArray ==> " + mDocMgr.getDocTypeReferenceArray());
					Log.i(TAG, "itemTypeIndex ==> " + itemTypeIndex);
					Log.i(TAG, "Document obj ==> " + mDocMgr.getDocTypeReferenceArray().get(itemTypeIndex));

					//if this screen is launched by using custom-url
					if(mCustomUrlUtils.isUsingCustomUrl() && 
							(mCustomUrlUtils.getDocumentFieldDefaults() != null && mCustomUrlUtils.getDocumentFieldDefaults().size() > 0)) {
						/*construct a document object based on the type passed in custom url and copy all the fields present in url. 
                    	This will later be used to compare with downloaded document object, and all the valid fields values will be displayed on screen 
						 */
						copyCustomUrlFieldValuesToDocument();
					}
					DisplayFields(isObjectAlreadyCreated);

					mFirstTimeDisplay++;
				}
				mProgressBar.setVisibility(View.GONE);
				mCustomDialog.closeProgressDialog();
			}
		}, 300);
	}

	private void showErrorDialog(){
		if(mCustomDialog == null) {
			mCustomDialog = CustomDialog.getInstance();
		}
		if(mCustomDialog != null){
			mCustomDialog.show_popup_dialog(this, AlertType.ERROR_ALERT, 
					getResources().getString(R.string.actionbar_lbl_screen_document_info),
					getResources().getString(R.string.toast_error_required_fields_empty) , 
					null, null,
					Messages.MESSAGE_DIALOG_REQUIRED_FIELDS,
					null,
					false);
		}
	}

	private boolean SaveDocument() {
		// if object is not yet saved on disk
		/*if (mDocumentObj != null) {
			//free mDocumentObj
			mDocMgr.cleanDocument(mDocumentObj);
		}*/

		mDocumentObj = constructDocument(mDocumentObj);

		if(isrequiredFieldLeftEmpty){
			showErrorDialog();
			isrequiredFieldLeftEmpty = false;
			return false;
		}

		if (mDocumentObj != null) {
			Log.i(TAG, "Document Serialization Successful!");
			if(isChangingItemType) {
				//update the required docMgr parameters.
				mDocMgr.setCurrentDocTypeIndex(itemTypeIndex);
				//rename current item dir on disk with newly updated item-type name

				Log.i(TAG, "Old file name is  ==> " + mDocMgr.getOpenedDoctName());
				Log.i(TAG, "New file name is  ==> " + mDocMgr.getDocTypeNamesList().get(itemTypeIndex));
			}
			Log.i(TAG, "mDocMgr.getOpenedDoctName() ==================> " + mDocMgr.getOpenedDoctName());

			byte[] barray = mDiskMgr.documentToByteArray(mDocumentObj);

			ItemEntity item = mItemDBManager.getItemEntity();
			if(item != null){
				if(barray != null){
					item.setItemSerializedData(barray);
					ProcessingParametersEntity ppEntity = mItemDBManager.getProcessingParametersEntity();
					if(null != ppEntity){
						ppEntity.setSerializeDocument(barray);
						mItemDBManager.updateProcessingEntity(this, ppEntity);
					}
				}
				item.setItemTypeName(mDocMgr.getDocTypeNamesList().get(itemTypeIndex));
				item.setItemName(mDocMgr.getOpenedDoctName());
				if(mVisibleFieldCount == 0){
					item.setFieldName(Constants.STR_NO_FIELDS);
				}else if(mViewsIdArr != null){
					for(int i =0;i < mViewsIdArr.length;i++){
						View parent = findViewById(mViewsIdArr[i]);
						if(parent.getVisibility() != View.GONE && mDocumentObj.getFields().get(i).getValue() != null){
							if(mDocumentObj.getFields().get(i).getValue().length() == 0){
								item.setFieldName(Constants.STR_EMPTY_FIELD);
							}else{
								item.setFieldName(mDocumentObj.getDocumentType().getFieldTypes().get(i).getDisplayName() + " : " +  mDocumentObj.getFields().get(i).getValue());
							}
							break;
						}
					}
				}
				mItemDBManager.update(getApplicationContext(), item);
				//mItemDBManager.insertOrUpdate(getApplicationContext(), item);
			}
		}
		else {
			Log.e(TAG, "mDocumentObj is NULL");
		}
		return true;
	}

	private void cloneOriginalDocument() {
		if (mDocumentObj == null) {
			Log.e(TAG, "Document object is NULL!!");
			return;
		}
		if(mDocumentObj.getDocumentType() == null || mDocumentObj.getDocumentType().getFieldTypes() == null || mDocumentObj.getDocumentType().getFieldTypes().size() <= 0) {
			Log.e(TAG, "Document dones not seem to contain fields.");
			return;
		}
		List<FieldType> fieldTypeList =  mDocumentObj.getDocumentType().getFieldTypes();
		int fieldLength = fieldTypeList.size();

		//copy default data(if any) to value field in object.
		for(int i=0; i<fieldLength; i++) {
			if((fieldTypeList.get(i).getDefault() != null) || (!fieldTypeList.get(i).getDefault().equals(Constants.STR_EMPTY))) {
				Log.e(TAG, "setting :: " + fieldTypeList.get(i).getDefault());
				mDocumentObj.getFields().get(i).updateFieldProperties(fieldTypeList.get(i).getDefault(), true, "");
			}
		}
		//serialize document object back to database
		byte[] barray = mDiskMgr.documentToByteArray(mDocumentObj);
		ItemEntity item = mItemDBManager.getItemEntity();
		if(item != null){
			if(barray != null){
				item.setItemSerializedData(barray);
				ProcessingParametersEntity ppEntity = mItemDBManager.getProcessingParametersEntity();
				if(null != ppEntity){
					ppEntity.setSerializeDocument(barray);
					mItemDBManager.updateProcessingEntity(this, ppEntity);
				}
			}
			item.setItemTypeName(mDocMgr.getDocTypeNamesList().get(itemTypeIndex));
			item.setItemName(mDocMgr.getOpenedDoctName());
			if(mVisibleFieldCount == 0){
				item.setFieldName(Constants.STR_NO_FIELDS);
			}else {
				for(int i =0;i < fieldLength; i++){
					if(!fieldTypeList.get(i).isHidden() && mDocumentObj.getFields().get(i).getValue() != null){
						if(mDocumentObj.getFields().get(i).getValue().length() == 0){
							item.setFieldName(Constants.STR_EMPTY_FIELD);
						}else{
							item.setFieldName(fieldTypeList.get(i).getDisplayName() + " : " +  mDocumentObj.getFields().get(i).getValue());
						}
						break;
					}
				}
			}
			mItemDBManager.update(getApplicationContext(), item);
//			mItemDBManager.insertOrUpdate(getApplicationContext(), item);
		}
	}

	@SuppressLint("NewApi")
	private Document constructDocument(Document newDocObj) {

		View parent = null;

		if (mViewsIdArr == null) {
			return newDocObj;
		}
		for (int i = 0; i < mViewsIdArr.length; i++) {

			parent = findViewById(mViewsIdArr[i]);
			if (parent != null) {
				View dataChild = ((ViewGroup)parent).getChildAt(1);
				boolean required = false;
				if(newDocObj.getFields().get(i).getFieldType().isRequired()){
					required = true;
				}
				//if datatype is BOOL, get value from switch
				if (newDocObj.getDocumentType().getFieldTypes().get(i).getDataType() == DataType.BOOL) {
					if (((Switch)dataChild).isChecked()) {
						newDocObj.getFields().get(i).updateFieldProperties(Constants.STR_TRUE, true, "");	
					}
					else {
						newDocObj.getFields().get(i).updateFieldProperties(Constants.STR_FALSE, true, "");	
					}
				}
				else {
					//if field has forcematch set, get value from spinner
					if (newDocObj.getDocumentType().getFieldTypes().get(i).getOptions().length > 0) {
						newDocObj.getFields().get(i).updateFieldProperties(((Spinner)dataChild).getSelectedItem().toString(), true, "");
					}
					else {
						// for everything else, get from text fields
						String text = ((TextView)dataChild).getText().toString();
						TextView textView = (TextView)((ViewGroup)parent).getChildAt(0);
						if(required && !newDocObj.getFields().get(i).getFieldType().isHidden()){
							if(text != null && text.length() > 0){
								newDocObj.getFields().get(i).updateFieldProperties(text, true, "");
								textView.setTextColor(Color.GRAY);
							}else if(!newDocObj.getFields().get(i).getFieldType().getDisplayName().trim().equalsIgnoreCase(Constants.STR_MOBILE_DEVICE_EMAIL)){
								isrequiredFieldLeftEmpty = true;					            
								textView.setTextColor(Color.RED);
							}
						}else{
							newDocObj.getFields().get(i).updateFieldProperties(text, true, "");
						}
					}
				}
			}
		}

		return newDocObj;
	}

	/**
	 * Function to copy all the valid field values which are present in custom url into Document object. 
	 */
	private void copyCustomUrlFieldValuesToDocument() {
		boolean isInvalidField = true;
		Map<String, String> fieldMap = mCustomUrlUtils.getDocumentFieldDefaults();

		List<FieldType> fieldTypes = mDocumentObj.getDocumentType().getFieldTypes();
		int length = fieldTypes.size();
		for (Entry<String, String> mapEntry : fieldMap.entrySet()) {
			for(int i=0; i<length; i++) {
				if(fieldTypes.get(i).getName().equals(mapEntry.getKey())) {
					mDocumentObj.getFields().get(i).updateFieldProperties(mapEntry.getValue(), true, "");
					isInvalidField = false;
				}
			}
		}
		if(isInvalidField == true) {
			mCustomUrlUtils.setInvalidFieldPresent(isInvalidField);
		}
	}

	/**
	 * Function to match each field of oldDoc object with the newDoc object fields. If fields are matching, 
	 * function checks different properties of both the fields before copying the old value into field of newDoc object.
	 *   
	 * @param oldDoc
	 * @param newDoc
	 * @return newDoc object with old values added, if any. 
	 */
	private Document matchAndMergeFieldsToNewDoc(Document oldDoc, Document newDoc) {
		List<FieldType> newFieldTypesList =  newDoc.getDocumentType().getFieldTypes();
		List<FieldType> currentFieldTypesList = oldDoc.getDocumentType().getFieldTypes();
		String[] msgArr = new String[newFieldTypesList.size()];

		for (int i = 0; i < newFieldTypesList.size(); i++) {
			Log.i(TAG, "New Field => " + i);

			FieldType newFT = newFieldTypesList.get(i);
			for (int j=0; j<currentFieldTypesList.size(); j++) {
				FieldType currentFT = currentFieldTypesList.get(j);
				//compare name to get the matching field-type(if any)
				if((newFT.getName() != null) && (newFT.getName().equals(currentFT.getName()))) {
					Log.i(TAG, "Field name matched ==>" + newFT.getName());

					//if name matches, compare data-types
					if (newFT.getDataType() == currentFT.getDataType()) {
						Log.i(TAG, "Data type matched ==>" + newFT.getDataType());
						//if data-types matches, check if current field property is hidden or readonly, if any one is true, use current default value instead of old one
						if ((!newFT.isReadOnly()) && (!newFT.isHidden())) {
							Log.i(TAG, "New field NOT Hidden or readOnly");
							//if forced match is true in new object, check if old value is present in new options array 
							if (newFT.isForceMatch()) {
								Log.i(TAG, "isForceMatch");
								String oldValue = oldDoc.getFields().get(j).getValue();
								Log.i(TAG, "oldValue ==> " + oldValue);
								if (oldValue != null) {
									//if old selected option is present in new array, set it selected.
									for (int k=0; k<newFT.getOptions().length; k++) {
										if(oldValue.equals(newFT.getOptions()[k])) {
											newDoc.getFields().get(i).updateFieldProperties(oldValue, true, "");
											break;
										}
									}
								}
								else {
									Log.i(TAG, "No option was selected earlier");
								}
							}
							//if field datatype is int, compare existing value with current min-max range. 
							if(newFT.getDataType() == DataType.INT) {
								if (currentFT.getMin() != null && currentFT.getMax() != null) {
									try {
										int value = Integer.parseInt(oldDoc.getFields().get(i).getValue());
										int min = Integer.parseInt(currentFT.getMin());
										int max = Integer.parseInt(currentFT.getMax());
										if ((value > min) && (value < max)) {
											//update the value when its in range
											newDoc.getFields().get(i).updateFieldProperties(oldDoc.getFields().get(j).getValue(), true, "");
										}
										else {
											Log.i(TAG, "Current value is OutOfRange as per new range");
											msgArr[i] = getResources().getString(R.string.app_msg_value_outofrange);
										}
									}
									catch(NumberFormatException e) {
										Log.e(TAG, e.getMessage());
										e.printStackTrace();
									}
								}
							}else {
								//update the value when its in range
								newDoc.getFields().get(i).updateFieldProperties(oldDoc.getFields().get(j).getValue(), true, "");
							}
						}
					}
					else {
						Log.i(TAG, "Datatype not matching for field " + newFT.getName());
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.str_datatype_not_matching) + newFT.getName(), Toast.LENGTH_LONG).show();
						msgArr[i] = getResources().getString(R.string.app_msg_datatype_changed);
					}
					break;
				}
			}
		}
		Log.i(TAG, "Exit:: matchAndMergeFieldsToNewDoc");
		return newDoc;
	}


	@SuppressLint({ "NewApi", "InflateParams" })
	private void DisplayFields(boolean isAlreadySaved) {
		Log.i(TAG, "Field size ===> " + mDocumentObj.getFields().size());

		mVisibleFieldCount = 0;

		if (isAlreadySaved == true && (!mCustomUrlUtils.isUsingCustomUrl())) {
			if ((mDocMgr.getDocTypeReferenceArray() != null) && (mDocMgr.getDocTypeReferenceArray().get(itemTypeIndex) != null)) {
				Document newDoc = new Document(mDocMgr.getDocTypeReferenceArray().get(itemTypeIndex));
				// Display this toast only when document types are refreshed. Not when document type is changed.
				if(newDoc.getFields() != null) {
					if (!isChangingItemType && mDocumentObj.getFields().size() < newDoc.getFields().size()) {
						Toast.makeText(this, Constants.STR_EMPTY + (mDocumentObj.getFields().size() - newDoc.getFields().size()) + getResources().getString(R.string.toast_fields_added) , Toast.LENGTH_LONG).show(); 
					}
					else if (!isChangingItemType && mDocumentObj.getFields().size() > newDoc.getFields().size()) {
						Toast.makeText(this, Constants.STR_EMPTY + (mDocumentObj.getFields().size() - newDoc.getFields().size()) + getResources().getString(R.string.toast_fields_removed), Toast.LENGTH_LONG).show();
					}

					Log.i(TAG, "newDoc Size before ===> " + newDoc.getDocumentType().getFieldTypes().size());
					Log.i(TAG, "mDocumentObj Size before ===> " + mDocumentObj.getDocumentType().getFieldTypes().size());

					// function to match each field of old object with the current reference object, retruns the new document object
					mDocumentObj = matchAndMergeFieldsToNewDoc(mDocumentObj, newDoc);

					Log.i(TAG, "newDoc Size after ===> " + newDoc.getDocumentType().getFieldTypes().size());
					Log.i(TAG, "mDocumentObj Size after ===> " + mDocumentObj.getDocumentType().getFieldTypes().size());
				}
				else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_no_fields), Toast.LENGTH_LONG).show();
				}
			}
		}
		else {
			Log.e(TAG, "Already not saved !!!!!!!!!!!!!!");
		}

		mViewsIdArr = null;
		mViewsIdArr = new int[mDocumentObj.getFields().size()];

		List<FieldType> currentFieldTypesList = mDocumentObj.getDocumentType().getFieldTypes();
		Log.i(TAG, "currentFieldTypesList size ===> " + currentFieldTypesList.size());
		Log.i(TAG, "mDocumentObj.getDocumentType() ===> " + mDocumentObj.getDocumentType());

		int size = currentFieldTypesList.size();

		View addView = null;

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				LayoutInflater layoutInflater = (LayoutInflater) getBaseContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				//populate the options if there are any
				if (currentFieldTypesList.get(i).getOptions().length > 0) {
					addView = layoutInflater.inflate(
							R.layout.editfields_spinner, null);

					TextView fieldLabel = (TextView) addView
							.findViewById(R.id.docinfo_textview);
					if(mDocumentObj.getFields().get(i).getFieldType().isRequired() && (!mDocumentObj.getFields().get(i).getFieldType().getName().contains("*"))) {
						Log.d(TAG, "FieldName => " + currentFieldTypesList.get(i).getDisplayName() + " - SET as required");
						fieldLabel.setText("*" + currentFieldTypesList.get(i).getDisplayName());	//display field-label, prepend * to label if its a required field
					}
					else {
						Log.d(TAG, "FieldName => " + currentFieldTypesList.get(i).getDisplayName() + " - NOT set as required");
						fieldLabel.setText(currentFieldTypesList.get(i).getDisplayName());	//display field-label
					}
					fieldLabel.setTextColor(Color.BLACK);
					Spinner spinner = (Spinner) addView.findViewById(R.id.spinner1);
					ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, currentFieldTypesList.get(i).getOptions()); //selected item will look like a spinner set from XML
					spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(spinnerArrayAdapter);
					for(int j= 0;j < spinnerArrayAdapter.getCount();j++){
						if(mDocumentObj.getFields().get(i).getValue().contentEquals(spinnerArrayAdapter.getItem(j))){
							spinner.setSelection(j);
							break;
						}
					}

				}
				else {
					DataType dType = currentFieldTypesList.get(i).getDataType();

					if (dType ==DataType.BOOL) {
						addView = layoutInflater.inflate(
								R.layout.editfields_switch, null);
						if(addView != null) {
							Switch swtch = (Switch) addView
									.findViewById(R.id.docinfo_switch);
							if (currentFieldTypesList.get(i).getDefault() != null) {	//TODO: check what value comes from server
							}
							if (mDocumentObj.getFields().get(i).getValue() != null) {//TODO: check what value comes from server
								swtch.setChecked(true);
							}
							else {
								swtch.setChecked(false);
							}
						}
					}
					else {
						addView = layoutInflater.inflate(
								R.layout.editfields_textview, null);

						TextView fieldLabel = (TextView) addView
								.findViewById(R.id.docinfo_textview);
						if(mDocumentObj.getFields().get(i).getFieldType().isRequired()&& (!mDocumentObj.getFields().get(i).getFieldType().getName().contains("*"))) {
							Log.d(TAG, "FieldName => " + currentFieldTypesList.get(i).getDisplayName() + " - SET as required");
							fieldLabel.setText("*" + currentFieldTypesList.get(i).getDisplayName());	//display field-label, prepend * to label if its a required field
						}
						else {
							Log.d(TAG, "FieldName => " + currentFieldTypesList.get(i).getDisplayName() + " - NOT set as required");
							fieldLabel.setText(currentFieldTypesList.get(i).getDisplayName());	//display field-label
						}
						fieldLabel.setTextColor(Color.BLACK);
						EditText fieldData = (EditText) addView
								.findViewById(R.id.docinfo_edittext);
						fieldData.setTextColor(Color.GRAY);
						fieldData.setTag(i);
						if (dType == DataType.EMAIL || dType == DataType.URL || dType == DataType.STRING) {
							if (mDocumentObj.getFields().get(i).getFieldType().getMax().length() > 0) {
								int length = Integer.parseInt(mDocumentObj.getFields().get(i).getFieldType().getMax());
								InputFilter[] InArray = new InputFilter[1];
								InArray[0] = new InputFilter.LengthFilter(length);
								fieldData.setFilters(InArray);
							}
						}

						if (dType ==DataType.EMAIL) {
							fieldData.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
						}
						else if (dType ==DataType.INT) {
							fieldData.setInputType(InputType.TYPE_CLASS_NUMBER);
						}
						else if (dType ==DataType.FLOAT) {
							fieldData.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL |InputType.TYPE_NUMBER_FLAG_SIGNED);
						}
						else if (dType ==DataType.STRING) {
							fieldData.setInputType(InputType.TYPE_CLASS_TEXT);
						}
						else if (dType ==DataType.URL) {
							fieldData.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
						}
						else if (dType == DataType.DATE) {
							fieldData.setOnClickListener(onClickListener_DateField);

							fieldData.setFocusable(false);
						}

						if(dType ==DataType.INT || dType ==DataType.FLOAT){
							if (mDocumentObj.getFields().get(i).getFieldType().getMax().length() > 0 && mDocumentObj.getFields().get(i).getFieldType().getMin().length() > 0) {
								try{

									mEditTextList.add(fieldData);
									mEditTextList.get(minMaxCount).addTextChangedListener(new TextWatcher(){

										public void beforeTextChanged(CharSequence s, int start, int count, int after){
											mPrev_text = s.toString();
										}
										public void onTextChanged(CharSequence s, int start, int before, int count){
											if (mCurrentFocusEdittext != null) {
												if (s.length() > 0) {    
													DataType type =  mDocumentObj.getFields().get((Integer)mCurrentFocusEdittext.getTag()).getFieldType().getDataType();
													switch (type) {
													case INT:
														int maxValue = 0;
														int minValue = 0;
														long value = 0;
														try {
														 maxValue = Integer.parseInt(mDocumentObj.getFields().get((Integer)mCurrentFocusEdittext.getTag()).getFieldType().getMax());
														 minValue = Integer.parseInt(mDocumentObj.getFields().get((Integer)mCurrentFocusEdittext.getTag()).getFieldType().getMin());																												
															value = Long.parseLong(s.toString());
														}catch (NumberFormatException e) {
															Log.e(TAG, "NumberFormatException occured, reseting field value to minValue.");
														}
														if (!((value > minValue) && (value < maxValue))) {
															mCustomDialog.show_popup_dialog(EditFieldsActivity.this, 
																	AlertType.INFO_ALERT, 
																	getResources().getString(R.string.actionbar_lbl_screen_document_info),
																	String.format(getResources().getString(R.string.progress_msg_please_enter_values_between)+" %d "+getResources().getString(R.string.msg_to)+"  %d "+getResources().getString(R.string.msg_range), minValue, maxValue),
																	null, null,
																	Messages.MESSAGE_DIALOG_REQUIRED_FIELDS, 
																	null,
																	false);
															mCurrentFocusEdittext.setText(mPrev_text);
														}else{
															// update the value when
															// its in range
															mPrev_text = s.toString();
														}
														break;
													case FLOAT:
														Double dValue = 0.0;
														String Number = "0.";
														CharSequence data = s;
														if(!data.toString().contains("-")){
															//parse number here using WarrenFaiths method and place the int or float or double 
															if(data.length() > 0 ){
																if(data.toString().startsWith(".")){
																	String str = "0";
																	str.concat(data.toString());
																	data = str;
																}
																try {
																	dValue = Double.valueOf(data.toString());
																}
																catch(NumberFormatException e) {
																	Log.e(TAG, "NumberFormatException occured, reseting field value to zero.");
																	dValue = 0.0;
																}
															}else{
																break;
															}
														}else{
															Boolean negative = false;

															if(data.toString().startsWith("-")){
																Number = data.toString().replace("-", Constants.STR_EMPTY);
																negative = true;
															}

															//parse number here using WarrenFaiths method and place the float or double 
															if(Number.length() > 0){
																try {
																	dValue = Double.valueOf(Number);

																	if(negative){
																		dValue = dValue * -1;
																	}
																}catch(NumberFormatException e) {
																	Log.e(TAG, "NumberFormatException occured, reseting field value to zero.");
																	dValue = 0.0;
																}

															}else{
																break;
															}
														}

														Double DmaxValue = Double.parseDouble(mDocumentObj.getFields().get((Integer)mCurrentFocusEdittext.getTag()).getFieldType().getMax());
														Double DminValue = Double.parseDouble(mDocumentObj.getFields().get((Integer)mCurrentFocusEdittext.getTag()).getFieldType().getMin());
														if (!((dValue > DminValue) && (dValue < DmaxValue))) {

															mCustomDialog.show_popup_dialog(EditFieldsActivity.this, 
																	AlertType.INFO_ALERT, getResources().getString(R.string.actionbar_lbl_screen_document_info),
																	String.format(getResources().getString(R.string.progress_msg_please_enter_values_between)+" %e "+getResources().getString(R.string.msg_to)+"  %e "+getResources().getString(R.string.msg_range), DminValue, DmaxValue),
																	null, null,
																	Messages.MESSAGE_DIALOG_REQUIRED_FIELDS, 
																	null,
																	false);
															mCurrentFocusEdittext.setText(mPrev_text);
														}else{
															// update the value when
															// its in range
															mPrev_text = s.toString();
														}
														break;
													default:
														break;
													}

												}
											}
										}
										@Override
										public void afterTextChanged(Editable arg0) {       
										}
									}); 

									mEditTextList.get(minMaxCount).setOnFocusChangeListener(new OnFocusChangeListener() {

										@Override
										public void onFocusChange(View v, boolean hasFocus) {
											if(hasFocus){
												mCurrentFocusEdittext = (EditText)v;
											}
										}
									});
									minMaxCount++;

								}catch(NumberFormatException e){
									e.printStackTrace();
								}
							}
						}

						// if earlier object exists, display its data
						if (mDocumentObj.getFields().get(i).getValue() != null && mDocumentObj.getFields().get(i).getValue().length() > 0) {
							fieldData.setText(mDocumentObj.getFields().get(i).getValue());	
						}else if(currentFieldTypesList.get(i).getDefault() != null && currentFieldTypesList.get(i).getDefault().length() > 0){
							//else display default value from fieldType
							fieldData.setText(currentFieldTypesList.get(i).getDefault());
						}else{

							if(mDocumentObj.getFields().get(i).getFieldType().getDisplayName().equalsIgnoreCase(Constants.MOBILE_DEVICE_EMAIL)){
								Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
										mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
								if(usekofax){
									fieldData.setText(mPrefUtils.sharedPref.getString(
											mPrefUtils.KEY_KFX_EMAIL, mPrefUtils.DEF_KFX_EMAIL));
								}else{
									fieldData.setText(mPrefUtils.sharedPref.getString(
											mPrefUtils.KEY_USR_EMAIL, mPrefUtils.DEF_USR_EMAIL));
								}
							}
						}

						if(isFiledValidation && fieldData.getText().length() <= 0 && mDocumentObj.getFields().get(i).getFieldType().isRequired()){
							fieldLabel.setTextColor(Color.RED);
						}else{
							fieldLabel.setTextColor(Color.BLACK);
						}
					}
				}
				if (addView != null) {
					addView.setId(i);	//set the id to refer later
					mViewsIdArr[i] = addView.getId();	//keep reference of all the view-IDs is serial order

					//check if field is hidden
					if (currentFieldTypesList.get(i).isReadOnly()) {
						addView.setEnabled(false);
					}
					//don't display hidden fields and MobileDeviceEmail field (if any).
					if (currentFieldTypesList.get(i).isHidden() || currentFieldTypesList.get(i).getName().equalsIgnoreCase(Constants.STR_MOBILE_DEVICE_EMAIL)) {
						addView.setVisibility(View.GONE);
					}else{
						mVisibleFieldCount++;
					}
					container.addView(addView);				
				}
				Log.d(TAG, "Text:: " + currentFieldTypesList.get(i).getDefault());
			}
		}
		// check if any field is displayed
		if(mVisibleFieldCount > 0) {
			findViewById(R.id.textNoFields).setVisibility(View.GONE);
		}
		else {
			findViewById(R.id.textNoFields).setVisibility(View.VISIBLE);
			Toast.makeText(this, getResources().getString(R.string.toast_no_fields), Toast.LENGTH_LONG).show();
		}
		invalidateOptionsMenu();
	}

	OnClickListener  onClickListener_DateField = new OnClickListener() {
		@Override
		public void onClick(View v) {

			Calendar dtTxt = null;
			String initialDate;
			String initialMonth;
			String initialYear;

			String preExistingDate = ((EditText)v).getText().toString();

			mSelectedDateTextField = v;

			if(preExistingDate != null && !preExistingDate.equals(Constants.STR_EMPTY)){
				//if user has set some invalid value for the date, reset date to current system date
				if(!isDateValid(preExistingDate)) {
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					String date = df.format(Calendar.getInstance().getTime());
					StringTokenizer st = new StringTokenizer(date,"-");
					initialYear	 = st.nextToken();
					initialMonth = st.nextToken();
					initialDate = st.nextToken();
				}
				else {
					StringTokenizer st = new StringTokenizer(preExistingDate,"-");
					initialYear	 = st.nextToken();
					initialMonth = st.nextToken();
					initialDate = st.nextToken();
				}

				if(mDatePickerDialog == null)
					mDatePickerDialog = new DatePickerDialog(v.getContext(),
							null,Integer.parseInt(initialYear),
							Integer.parseInt(initialMonth)-1,
							Integer.parseInt(initialDate));
				mDatePickerDialog.updateDate(Integer.parseInt(initialYear),
						Integer.parseInt(initialMonth)-1,
						Integer.parseInt(initialDate));

			} else {
				dtTxt = Calendar.getInstance();
				if(mDatePickerDialog == null)
					mDatePickerDialog = new DatePickerDialog(v.getContext(),null,dtTxt.get(Calendar.YEAR),dtTxt.get(Calendar.MONTH),
							dtTxt.get(Calendar.DAY_OF_MONTH));
				mDatePickerDialog.updateDate(dtTxt.get(Calendar.YEAR),dtTxt.get(Calendar.MONTH),
						dtTxt.get(Calendar.DAY_OF_MONTH));
			}
			mDatePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Done",new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mDatePickerDialog.getDatePicker().getDayOfMonth();
					((TextView)mSelectedDateTextField).setText(mDatePickerDialog.getDatePicker().getYear()+"-"+(mDatePickerDialog.getDatePicker().getMonth()+1) +"-"+mDatePickerDialog.getDatePicker().getDayOfMonth());
				}
			});

			mDatePickerDialog.show();
		}
	}; 

	private boolean isDateValid(String date) { 
		String DATE_FORMAT = "yyyy-mm-dd";
		try {
			DateFormat df = new SimpleDateFormat(DATE_FORMAT);
			df.setLenient(false);
			df.parse(date);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	/**
	 * Function to check if the documentType object for the selected type is already downloaded. 
	 * If not, enqueue the index of selected documenttype for documentManager to download documentType object for the specified documentType.  
	 * @throws KmcException 
	 * @throws KmcRuntimeException 
	 */
	private ResultState downloadDocTypeObject(int index) throws KmcRuntimeException, KmcException {
		Log.i(TAG, "Enter: downloadDocTypeObject");
		// check if documentType object is already downloaded, if not, enqueue doctype-index for download
		if (mDocMgr.getDocTypeFromRefArray(index) == null) {
			ResultState result = mDocMgr.downloadDocTypeObject(index);
			return result;
		} else {
			startScreenLoadHandler();
		}
		return ResultState.RESULT_OK;
	}
	
	private void registerBroadcastReceiver() {
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_DOCTYPE_DOWNLOADED);
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					//extract our message from intent
					String docType = intent.getStringExtra(Constants.STR_DOCUMENT_TYPE);
					Log.i(TAG, "Received broadcast! Downloaded doc type is===> " + docType); 

					//compare if the downloaded documenttype name is same as the one requested from this screen.
					if(docType != null && docType.equalsIgnoreCase(mDocMgr.getDocTypeReferenceArray().get(itemTypeIndex).getTypeName())) {
						mCustomDialog.closeProgressDialog();
						//check if error while downloading document type
						if(intent.getStringExtra(Constants.STR_ERROR_MESSAGE) != null) {
							Log.e(TAG, "Received error while downloading document type!!!!! ");
							Toast.makeText(getApplicationContext(), intent.getStringExtra(Constants.STR_ERROR_MESSAGE), Toast.LENGTH_LONG).show();
							//disable save option on menu in case of error.
						}
						else {
							startScreenLoadHandler();
						}
					}
				}			
			};
		}
		//registering our receiver
		registerReceiver(mReceiver, intentFilter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mViewsIdArr = null;
		if (mCustomDialog!=null)
		{
			mCustomDialog.closeProgressDialog();
		}
		mReceiver = null;
		if (container != null) {
			container.removeAllViews();
			container = null;
		}
		if(mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		mCustomUrlUtils = null;
		mItemDBManager = null;
		mPrefUtils = null;
		EditSessionStateHandler = null;
		mCustomDialog = null;
		mDocMgr = null;
		isFiledValidation = false;
		isrequiredFieldLeftEmpty = false;
	}
}
