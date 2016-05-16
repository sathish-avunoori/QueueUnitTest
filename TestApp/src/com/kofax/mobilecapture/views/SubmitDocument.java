//Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.klo.logistics.KfsSessionStateEvent;
import com.kofax.kmc.klo.logistics.SessionState;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.klo.logistics.data.Field;
import com.kofax.kmc.klo.logistics.data.FieldType;
import com.kofax.kmc.klo.logistics.data.FieldType.DataType;
import com.kofax.kmc.klo.logistics.data.Page;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DatabaseManager.ProcessingStates;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.ServerManager;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.Globals.ResultState;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

/**
 * 
 * This class is responsible for Submit the document and display the document
 * details
 * 
 */

public class SubmitDocument extends Activity {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = SubmitDocument.class.getSimpleName();

	// - Private data.
	/* SDK objects */

	private SessionState prevState;
	private Document mDocumentObj = null;

	/* Application objects */
	private DocumentManager mDocMgr = null;
	private DiskManager mDiskMgr = null;
	private UtilityRoutines mUtilRoutines = null;
	private CustomDialog mCustomDialog = null;
	private DatabaseManager mItemDBManager = null;
	private ServerManager mServerMgr = null;
	ItemEntity mItemEntity = null;
	/* Standard variables */
	private Handler mSubmittingActivityHandler = null;
	private TableLayout mTableLayout = null;
	private ProgressBar progressBar = null;
	private LayoutInflater mLayoutInflater = null;
	private BroadcastReceiver mReceiver = null;
	private int mLayoutCount = 0;
	private boolean cancel_flag = false;
	private boolean isSubmitNeeded = false;
	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.submit_document);
		getActionBar().setTitle(
				getResources().getString(
						R.string.actionbar_lbl_screen_submit_document));
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		// Progress bar to show the submitting progress percentage
		progressBar = (ProgressBar) findViewById(R.id.progressBar2);
		progressBar.setProgressDrawable(getResources().getDrawable(
				R.drawable.progress_horizontal_holo_no_background_light));
		progressBar.setProgress(0);
		progressBar.setVisibility(View.GONE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setIcon(
					new ColorDrawable(getResources().getColor(
							android.R.color.transparent)));
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		mLayoutCount = 0;
		mDocMgr = DocumentManager.getInstance(getApplicationContext());
		mDiskMgr = DiskManager.getInstance(getApplicationContext());
		mUtilRoutines = UtilityRoutines.getInstance();
		mCustomDialog = CustomDialog.getInstance();
		mItemDBManager = DatabaseManager.getInstance();
		mItemEntity = mItemDBManager.getItemEntity();
		mServerMgr = ServerManager.getInstance();

		setupHandler();

		mTableLayout = (TableLayout) findViewById(R.id.submit_table_layout);
		mLayoutInflater = (LayoutInflater) getBaseContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (!mItemDBManager.isDocumentSerializedInDB(mItemEntity)) {
			if(Constants.IS_HELPKOFAX_FLOW){
				try {
					if((mDocMgr.getDocTypeReferenceArray() != null) && (mDocMgr.getDocTypeReferenceArray().get(mDocMgr.getCurrentDocTypeIndex()) != null)) {
						DocumentType refDocTypeObj = mDocMgr.getDocTypeReferenceArray().get(mDocMgr.getCurrentDocTypeIndex());
						mDocumentObj = new Document(refDocTypeObj);
						mItemEntity.setItemSerializedData(mDiskMgr.documentToByteArray(mDocumentObj));
						isSubmitNeeded = true;
						DisplayDocumentInfoOnScreen();
					}else{
					ResultState resultState = downloadDocTypeObject(mDocMgr.getCurrentDocTypeIndex());
					if(resultState == ResultState.RESULT_OK) {
						mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_downloading_doc_details),false);											
					}
					}

				} catch (KmcRuntimeException e) {
					e.printStackTrace();
					Toast.makeText(this, getResources().getString(R.string.error_msg_error_document_download), Toast.LENGTH_LONG).show();
				} catch (KmcException e) {
					e.printStackTrace();
					Toast.makeText(this, getResources().getString(R.string.error_msg_error_document_download), Toast.LENGTH_LONG).show();
				}
			}else{
			mCustomDialog
			.show_popup_dialog(
					SubmitDocument.this,
					AlertType.INFO_ALERT,
					getResources().getString(
							R.string.lbl_submit_not_initiated),
							getResources().getString(
									R.string.error_msg_error_no_document_details_available),
									null, null,
									Messages.MESSAGE_DIALOG_ERROR,
									mSubmittingActivityHandler, false);
		}
		}
		else {
			
			Document serializedDocument = (Document)mDiskMgr.byteArrayToDocument(mItemEntity.getItemSerializedData());
			//Creating new document from the serialized document to make sure that all private properties(field client property) should be initialized properly.
			mDocumentObj = UtilityRoutines.cloneDocumentWithFieldValues(serializedDocument);
			DisplayDocumentInfoOnScreen();
			validateAndSubmit();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.submit_menu,menu);
		return true;
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action buttons
		switch (item.getItemId()) {
		case android.R.id.home:
			/*if (mServerMgr.getkfsSessionState() == SessionState.SESSION_SUBMITTING) {
				cancelsubmitDocument();
			}
			else {
				onBackPressed();
			}*/
			break;
            case R.id.cancel_submut_menu:
                if (mServerMgr.getSessionState() == SessionState.SESSION_SUBMITTING) {
                    cancelsubmitDocument();
                    item.setEnabled(false);
			}
            else {
            	finish();
            }
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {

		//super.onBackPressed();
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	/**
	 * Function to check if the documentType object for the selected type is already downloaded. 
	 * If not, enqueue the index of selected documenttype for documentManager to download documentType object for the specified documentType.  
	 * @throws KmcException 
	 * @throws KmcRuntimeException 
	 */
	private ResultState downloadDocTypeObject(int index) throws KmcRuntimeException, KmcException {
		Log.i(TAG, "Enter: downloadDocTypeObject");
		// check if documentType object is already downloaded, if not, enqueue doctype-index for download
		ResultState result = mDocMgr.downloadDocTypeObject(index);
		registerBroadcastReceiver();
		return result;
	}

	/**
	 * Gets whether all images are processed or not.
	 * 
	 * @return A boolean value indicating whether all images are processed or
	 *         not. false - All are processed. true - All are not processed.
	 */
	private boolean checkIfAllIamgesAreProcessed() {
		boolean status = true;
		List<PageEntity> pageList = mItemDBManager.getAllPagesForItem(
				getApplicationContext(), mItemDBManager.getItemEntity()
				.getItemId());
		if (pageList != null) {
			for (int i = 0; i < pageList.size(); i++) {
				if (pageList.get(i).getImageType().equals(Globals.ImageType.DOCUMENT.name())
						&& ((pageList.get(i).getProcessingStatus().intValue() == ProcessingStates.PROCESSFAILED
						.ordinal()) || (pageList.get(i).getProcessingStatus().intValue() == ProcessingStates.PROCESSING
						.ordinal())|| (pageList.get(i).getProcessingStatus().intValue() == ProcessingStates.REVERTED
						.ordinal()) || (pageList.get(i).getProcessingStatus().intValue() == ProcessingStates.UNPROCESSED
						.ordinal()))) {
					status = false;
					break;
				}
			}
		}
		return status;
	}

	private Handler submitSessionStateHandler = new Handler(
			new Handler.Callback() {

				@Override
				public boolean handleMessage(Message msg) {

					KfsSessionStateEvent arg0 = (KfsSessionStateEvent) msg.obj;

					Log.i(TAG,
							"arg0.getSessionState() ::: "
									+ arg0.getSessionState());

					switch (arg0.getSessionState()) {
					case SESSION_LOGGED_IN:
						// If submit document fails or cancel,KFS change session
						// state to LOGGED_IN and return the error code.
						if (prevState == SessionState.SESSION_SUBMITTING) {
							Log.d(TAG, "current state : SESSION_LOGGED_IN");
							cancel_flag = false;

							if (progressBar != null) {
								progressBar.setVisibility(View.GONE);
								progressBar.setProgress(0);
							}

							if(null != mCustomDialog){
								mCustomDialog.closeProgressDialog();
							}

							Toast.makeText(
									getApplicationContext(),
									getResources().getString(
											R.string.error_msg_submit_failed)
											+ " " +arg0.getErrorInfo().getErrMsg(),
											Toast.LENGTH_LONG).show();
							//if cancel-submit was requested
							if(arg0.getErrorInfo() == ErrorInfo.KMC_LO_OPERATION_CANCELLED) {
								CloseActivity(Globals.ResultState.RESULT_CANCELED.ordinal());
							}
							else if(arg0.getErrorInfo() == ErrorInfo.KMC_LO_SUBMIT_DOCUMENT_IMAGES_ERROR) {
								CloseActivity(Globals.ResultState.RESULT_CANCELED.ordinal());
							}
							else {
								getActionBar()
								.setTitle(
										getResources()
										.getString(
												R.string.actionbar_lbl_screen_submit_document));
								CloseActivity(Globals.ResultState.RESULT_CANCELED.ordinal());
							}
						} else if (prevState == SessionState.SESSION_GETTING_DOCUMENT_FIELDS
								|| prevState == SessionState.SESSION_GETTING_IP_SETTINGS
								|| prevState == SessionState.SESSION_PREPARING_DOCUMENT_TYPE
								|| prevState == SessionState.SESSION_DOCUMENT_TYPE_READY) {
							if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
								if(null != mCustomDialog){
									mCustomDialog.closeProgressDialog();
								}
							}
						}
						break;
					case SESSION_GETTING_DOCUMENT_FIELDS:
					case SESSION_GETTING_IP_SETTINGS:
					case SESSION_PREPARING_DOCUMENT_TYPE:
					case SESSION_DOCUMENT_TYPE_READY:
						prevState = arg0.getSessionState();
						if (arg0.getErrorInfo() != ErrorInfo.KMC_SUCCESS) {
							if(null != mCustomDialog){
								mCustomDialog.closeProgressDialog();
							}
							Toast.makeText(getApplicationContext(),
									arg0.getErrorInfo().getErrDesc(),
									Toast.LENGTH_SHORT).show();
						}
						break;
					case SESSION_SUBMITTING:
						prevState = SessionState.SESSION_SUBMITTING;
						if (progressBar != null) {
							progressBar.setVisibility(View.VISIBLE);
							progressBar.setProgress(5);
						}
						updateProgressdialog(arg0.getProgressPct()); // Update
						// the
						// progress.
						break;
					case SESSION_SUBMIT_COMPLETED:
						updateProgressdialog(arg0.getProgressPct());
						if (progressBar != null) {
							progressBar.setVisibility(View.GONE);
						}
						if(null != mCustomDialog){
							mCustomDialog.closeProgressDialog();
						}
						
						if (arg0.getErrorInfo() == ErrorInfo.KMC_SUCCESS) {
							// Delete submitted item from disk and update
							// documentMgr accordingly
							mDiskMgr.deleteItemFromDisk(mDocMgr.getOpenedDoctName());
							if (mItemEntity != null) {
								//delete item from disk
								ArrayList<PageEntity> pageList = (ArrayList<PageEntity>) mItemDBManager
										.getAllPagesForItem(
												getApplicationContext(),
												mItemEntity.getItemId());
								if (pageList != null && pageList.size() > 0) {
									//delete all pages in item from DB
									for (int i = 0; i < pageList.size(); i++) {
										mItemDBManager.deletePageWithId(
												getApplicationContext(),
												pageList.get(i).getPageId()); 
									}
								}
								//delete submitted item from DB
								mItemDBManager.deleteItemWithId(
										getApplicationContext(),
										mItemEntity.getItemId()); 
							}
							// Check user clicks cancel button.
							if (cancel_flag) {
								cancel_flag = false;
								Toast.makeText(
										getApplicationContext(),
										getResources()
										.getString(
												R.string.toast_item_submitted_successful)
												+ " "
												+ getResources()
												.getString(
														R.string.toast_unable_to_cancel_operation),
														Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(
										getApplicationContext(),
										getResources()
										.getString(
												R.string.toast_item_submitted_successful),
												Toast.LENGTH_SHORT).show();
							}
							sendBroadcast(Globals.ResultState.RESULT_OK.ordinal());
							CloseActivity(Globals.ResultState.RESULT_OK.ordinal());
						} else {
							//when submit is failed/cancelled, if is a new item, switch to homescreen.
							Toast.makeText(
									getApplicationContext(),
									getResources().getString(
											R.string.error_msg_submit_failed)
											+ " " + arg0.getErrorInfo().getErrMsg(),
											Toast.LENGTH_LONG).show();
							CloseActivity(Globals.ResultState.RESULT_FAILED.ordinal());
						}
						break;
					default:
						CloseActivity(Globals.ResultState.RESULT_CANCELED.ordinal());
						break;
					}
					return true;
				}
			});

	private void setupHandler() {

		mSubmittingActivityHandler = new Handler(new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
				switch (whatMessage) {
				case MESSAGE_DIALOG_SUBMIT_CONFIRMATION:
					if (msg.arg1 == RESULT_CANCELED) {
						cancelsubmitDocument();
					}
					break;
				case MESSAGE_DIALOG_ERROR:
					CloseActivity(Globals.ResultState.RESULT_FAILED.ordinal());
					break;
				default:
					break;
				}
				return true;
			}
		});
	}


	private boolean validateAndSubmit() {
		boolean result = false;

		//check Internet connectivity before initiating document object download.
		if(!mUtilRoutines.checkInternet(this)) {
		//	Toast.makeText(this, getResources().getString(R.string.toast_no_network_connectivity) + "\n"+getResources().getString(R.string.toast_error_cannot_initiate_submit), Toast.LENGTH_LONG).show();
			mCustomDialog
			.show_popup_dialog(
					SubmitDocument.this,
					AlertType.INFO_ALERT,
					getResources().getString(
							R.string.lbl_submit_not_initiated),
							getResources().getString(
									R.string.toast_no_network_connectivity),
									null, null,
									Messages.MESSAGE_DIALOG_ERROR,
									mSubmittingActivityHandler, false);
		}
		else {
			// check if reference object is available
			if((mDocMgr.getDocTypeReferenceArray() != null) && (mDocMgr.getDocTypeReferenceArray().get(mDocMgr.getCurrentDocTypeIndex()) != null)) {
				/*Log.i(TAG,
						" Serialized data is not empty "
								+ mItemEntity.getItemSerializedData());
				 */
				DocumentType refDocTypeObj = mDocMgr.getDocTypeReferenceArray().get(mDocMgr.getCurrentDocTypeIndex());
				// compare processing parameters of document being submitted, with the reference object 
				boolean isMatching = mDocMgr.compareProcessingParams(mDocumentObj.getDocumentType(), refDocTypeObj);
				if(!isMatching) {
					//Remove corresponding processed files from disk.
					List<PageEntity> pages = mItemDBManager.getPages(this, mItemEntity.getItemId(), Globals.ImageType.DOCUMENT.name(), ProcessingStates.PROCESSED);
					if(pages != null) {
						for(int j = 0; j<pages.size(); j++) {
							mDiskMgr.deleteImageFromDisk(pages.get(j).getProcessedImageFilePath());
						}
					}
					Log.e(TAG, "Resetting processing status!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					//update DB to reset processing status of all processed and processed-failed pages (except reverted images)of the corresponding item.
					mItemDBManager.setAllPagesSelectiveProcessingStatus(this, mItemDBManager.getItemEntity().getItemId(), ProcessingStates.PROCESSED, ProcessingStates.UNPROCESSED);
					mItemDBManager.setAllPagesSelectiveProcessingStatus(this, mItemDBManager.getItemEntity().getItemId(), ProcessingStates.PROCESSFAILED, ProcessingStates.UNPROCESSED);
					mItemDBManager.setAllPagesSelectiveProcessingStatus(this, mItemDBManager.getItemEntity().getItemId(), ProcessingStates.PROCESSING, ProcessingStates.UNPROCESSED);
					
					//update processing profiles of saved object with latest reference object profiles. 
					mDocumentObj.getDocumentType().setBasicSettingsProfile(refDocTypeObj.getBasicSettingsProfile());
					mDocumentObj.getDocumentType().setImagePerfectionProfile(refDocTypeObj.getImagePerfectionProfile());
					mItemDBManager.getItemEntity().setItemSerializedData(mDiskMgr.documentToByteArray(mDocumentObj));
					ProcessingParametersEntity ppEntity = mItemDBManager.getProcessingParametersEntity();
					if(null != ppEntity){
						ppEntity.setSerializeDocument(mDiskMgr.documentToByteArray(mDocumentObj));
						mItemDBManager.updateProcessingEntity(this, ppEntity);
					}

					//if background-image-processing is enabled, schedule item for background image processing
					if(Constants.BACKGROUND_IMAGE_PROCESSING && (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mItemDBManager.isOfflineDocumentSerializedInDB(mItemDBManager.getProcessingParametersEntity()))) {
						ImageProcessQueueManager processQueueMgr = ImageProcessQueueManager.getInstance(getApplicationContext());
						processQueueMgr.addItemToQueue(mItemDBManager.getItemEntity());
						processQueueMgr = null;
						mCustomDialog
						.show_popup_dialog(
								SubmitDocument.this,
								AlertType.INFO_ALERT,
								getResources().getString(
										R.string.lbl_submit_not_initiated),
										getResources().getString(
												R.string.str_processing_all_images),
												null, null,
												Messages.MESSAGE_DIALOG_ERROR,
												mSubmittingActivityHandler, false);
					}
					else {
						mCustomDialog
						.show_popup_dialog(
								SubmitDocument.this,
								AlertType.INFO_ALERT,
								getResources().getString(
										R.string.lbl_submit_not_initiated),
										getResources().getString(
												R.string.str_process_all_images),
												null, null,
												Messages.MESSAGE_DIALOG_ERROR,
												mSubmittingActivityHandler, false);
					}
				}
				else {
					// check if all the images are processed before initiating the submit. If even one image is not processed, display notification to user and close the screen.
					boolean allProcessed = checkIfAllIamgesAreProcessed();
					if (!allProcessed) {
						mCustomDialog
						.show_popup_dialog(
								SubmitDocument.this,
								AlertType.INFO_ALERT,
								getResources().getString(
										R.string.lbl_submit_not_initiated),
										getResources().getString(
												R.string.app_msg_docs_not_processed),
												null, null,
												Messages.MESSAGE_DIALOG_ERROR,
												mSubmittingActivityHandler, false);
					} else {
						result = true;
					}
				}
			}
			else {
				//initiate download of reference object
				try {
					downloadDocTypeObject(mDocMgr.getCurrentDocTypeIndex());
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
					Toast.makeText(this, getResources().getString(R.string.error_msg_error_document_download), Toast.LENGTH_LONG).show();
				} catch (KmcException e) {
					e.printStackTrace();
					Toast.makeText(this, getResources().getString(R.string.error_msg_error_document_download), Toast.LENGTH_LONG).show();
				}
			}
		}
		if(result) {
			submitDocumentToServer();
		}
		return result;
	}

	/**
	 * Cancel the current submit process
	 */
	private void cancelsubmitDocument() {
		ErrorInfo errorInfo = null;
		try {
			/*getActionBar().setTitle(
					getResources().getString(R.string.progress_msg_caneling));*/
			cancel_flag = true;
			mServerMgr.cancelSubmitDocument();
			if (progressBar != null) {
				progressBar.setProgress(0);
				progressBar.setVisibility(View.GONE);
			}

			if (mCustomDialog != null) {
				mCustomDialog.showProgressDialog(this, getResources()
						.getString(R.string.progress_msg_caneling), false);
			}
		} catch (KmcException e) {
			errorInfo = e.getErrorInfo();
			e.printStackTrace();
		} catch (KmcRuntimeException e) {
			errorInfo = e.getErrorInfo();
			e.printStackTrace();
		}
		if(errorInfo != null) {
			if (progressBar != null) {
				progressBar.setVisibility(View.GONE);
			}
			Toast.makeText(getApplicationContext(), errorInfo.name(),
					Toast.LENGTH_SHORT).show();

			CloseActivity(Globals.ResultState.RESULT_FAILED.ordinal());
		}
	}

	/**
	 * Creating pages and Add images to pages,after prepare document submit it
	 * to server.
	 */
	private void submitDocumentToServer() {
		// Check network before submitting.
		if (mUtilRoutines.checkInternet(getApplicationContext()) == false) {
/*			Toast.makeText(
					getApplicationContext(),
					getResources().getString(
							R.string.toast_no_network_connectivity),
							Toast.LENGTH_LONG).show();
*/
			mCustomDialog
			.show_popup_dialog(
					SubmitDocument.this,
					AlertType.INFO_ALERT,
					getResources().getString(
							R.string.toast_no_network_connectivity),
							getResources().getString(
									R.string.toast_no_network_connectivity),
									null, null,
									Messages.MESSAGE_DIALOG_ERROR,
									mSubmittingActivityHandler, false);
			return;
		}
		try {
			if (mDocumentObj != null) {
				if (mItemEntity != null) {
					ArrayList<PageEntity> pageList = (ArrayList<PageEntity>) mItemDBManager
							.getAllPagesForItem(getApplicationContext(),
									mItemEntity.getItemId());
					if (pageList != null && pageList.size() > 0) {
						for (int i = 0; i < pageList.size(); i++) {
							Image img = null;
							Page page = new Page();
							String ext = Constants.STR_EMPTY;
							if (pageList.get(i).getImageType()
									.equals(Globals.ImageType.DOCUMENT.name())
									&& pageList.get(i).getProcessingStatus()
									.intValue() == ProcessingStates.PROCESSED
									.ordinal()) {
								if (new File(
										(pageList.get(i)
												.getProcessedImageFilePath()))
								.exists()) {
									ext = mUtilRoutines
											.getMimeTypeString(pageList
													.get(i)
													.getProcessedImageFilePath());
									img = new Image(pageList.get(i)
											.getProcessedImageFilePath(),
											mUtilRoutines
											.getMimeTypeFromExtn(ext));
									page.addImage(img);
								}
							} else {
								if (new File(
										(pageList.get(i).getImageFilePath()))
								.exists()) {
									ext = mUtilRoutines
											.getMimeTypeString(pageList.get(i)
													.getImageFilePath());
									img = new Image(pageList.get(i)
											.getImageFilePath(),
											mUtilRoutines
											.getMimeTypeFromExtn(ext));
									page.addImage(img);
								}
							}
							mDocumentObj.addPage(page);
						}
						getActionBar().setTitle(
								getResources().getString(
										R.string.progress_msg_submitting));
						mServerMgr.setServerTimeout(60);
						mServerMgr.submitDocument(submitSessionStateHandler,
								mDocumentObj);
					}
					pageList = null;

				}
			} else {
				Log.d(TAG, "Document is null");
			}
		} catch (KmcException e) {
			mCustomDialog.closeProgressDialog();
			Toast.makeText(getApplicationContext(),
					e.getErrorInfo().getErrDesc(), Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (KmcRuntimeException e) {
			mCustomDialog.closeProgressDialog();
			Toast.makeText(getApplicationContext(),
					e.getErrorInfo().getErrDesc(), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * This function display the field data using dynamically creating the
	 * textviews.
	 * 
	 * @param label
	 *            : Field name
	 * @param text
	 *            : Filed data
	 */
	@SuppressLint("InflateParams")
	private void writeToTextField(String label, String text,boolean isFiled) {
		View addView = null;
		TextView fielddata = null;
		TextView fieldLabel = null;

		if(isFiled){
			addView = mLayoutInflater
					.inflate(R.layout.submit_textview, null);
			fieldLabel = (TextView) addView
					.findViewById(R.id.subinfo_textview);
			fielddata = (TextView) addView
					.findViewById(R.id.subinfo_edittext);
		}else{
			addView = mLayoutInflater
					.inflate(R.layout.submit_editlayout, null);
			fieldLabel = (TextView) addView
					.findViewById(R.id.subinfo_textview);
			fielddata = (TextView) addView
					.findViewById(R.id.subinfo_edittext);
		}

		fieldLabel.setEnabled(false);
		fieldLabel.setText(label);
		fieldLabel.setTextColor(Color.BLACK);
		fielddata.setEnabled(false);
		fielddata.setText(text);
		fielddata.setTextColor(Color.GRAY);
		mTableLayout.addView(addView, mLayoutCount);
		mLayoutCount++;
	}

	/**
	 * This function gets the fields data from document.
	 */
	private void DisplayDocumentInfoOnScreen() {
		String text_l = Constants.STR_EMPTY;
		long filesizeBase64 = 0;


		if (mDocumentObj != null) {
			text_l = mDocumentObj.getDocumentType().getDisplayName();
			writeToTextField(
					getResources().getString(R.string.lbl_document_name),
					text_l,true);			

			List<FieldType> currentFieldTypesList = mDocumentObj
					.getDocumentType().getFieldTypes();
			Log.i(TAG, "currentFieldTypesList size ===> "
					+ currentFieldTypesList.size());
			Log.i(TAG,
					"mDocumentObj.getDocumentType() ===> "
							+ mDocumentObj.getDocumentType());

			int size = currentFieldTypesList.size();

			if (size > 0) {
				for (int i = 0; i < size; i++) {
					text_l = Constants.STR_EMPTY;
					if (currentFieldTypesList.get(i).isHidden()) {
						continue;
					}
					if (currentFieldTypesList.get(i).getOptions().length > 0) {
						text_l = mDocumentObj.getFields().get(i).getValue();
						writeToTextField(currentFieldTypesList.get(i).getDisplayName()
								, text_l,true);

					} else {
						DataType dType = currentFieldTypesList.get(i)
								.getDataType();

						if (dType == DataType.BOOL) {
							if (mDocumentObj.getFields().get(i).getValue() != null) {
								writeToTextField(currentFieldTypesList.get(i)
										.getDisplayName() , Constants.STR_TRUE,true);

							} else {
								writeToTextField(currentFieldTypesList.get(i)
										.getDisplayName() , Constants.STR_FALSE,true);

							}
						} else {
							//populate email field with the email set in preferences 
							if((currentFieldTypesList.get(i).getName() != null) && 
									currentFieldTypesList.get(i).getDisplayName().equals(Constants.MOBILE_DEVICE_EMAIL) && ((mDocumentObj.getFields().get(i).getValue() == null) || (mDocumentObj.getFields().get(i).getValue().length() <= 0))) {
								mDocumentObj.getFields().get(i).updateFieldProperties(PrefManager.getInstance().getCurrentEmail(), true, "");
							}
							// if earlier object exists, display its data
							if (mDocumentObj.getFields().get(i).getValue() != null) {
								writeToTextField(currentFieldTypesList.get(i)
										.getDisplayName() , mDocumentObj
										.getFields().get(i).getValue(),true);

							} else {
								// else display default value from fieldType
								writeToTextField(currentFieldTypesList.get(i)
										.getDisplayName(),
										currentFieldTypesList.get(i)
										.getDefault(),true);

							}
						}
					}
				}
			}
		}
		List<PageEntity> pageList = mItemDBManager.getAllPagesForItem(
				getApplicationContext(), mItemEntity.getItemId());

		if (pageList != null) {


			File fileToSubmit = null;
			writeToTextField(getResources()
					.getString(R.string.lbl_total_images),
					Integer.toString(pageList.size()),false);
			for (int i = 0; i < pageList.size(); i++) {
				if (pageList.get(i).getProcessingStatus() == ProcessingStates.PROCESSED
						.ordinal()) {
					fileToSubmit = new File(pageList.get(i)
							.getProcessedImageFilePath());
				} else {
					fileToSubmit = new File(pageList.get(i).getImageFilePath());
				}
				if (fileToSubmit.exists()) {
					filesizeBase64 += 4 * Math.round(Math.ceil(fileToSubmit
							.length() / 3.0) * 1.03333);
				}
			}
			long mbsize = 1024 * 1024;
			float mbytessize = 0;
			if (filesizeBase64 > mbsize) {
				mbytessize = (filesizeBase64 / (mbsize));
				text_l = String.format(
						"%.2f " + getResources().getString(R.string.str_MB),
						mbytessize);
				writeToTextField(getResources().getString(R.string.str_size)
						, text_l,false);
			} else {
				mbytessize = (filesizeBase64 / (1024));
				text_l = String.format(
						"%.2f " + getResources().getString(R.string.str_KB),
						mbytessize);
				writeToTextField(getResources().getString(R.string.str_size)
						, text_l,false);
			}
		} else {
			writeToTextField(getResources().getString(R.string.lbl_total_pages)
					, "0",false);
			writeToTextField(
					getResources().getString(R.string.str_size) , "0 "
							+ getResources().getString(R.string.str_KB),false);
		}

		SimpleDateFormat formater = new SimpleDateFormat(
				"dd MMM, yyyy EEE | hh:mm a");
		String str_date = formater.format(mItemEntity.getItemCreatedTimeStamp());
		writeToTextField(getResources().getString(R.string.str_date) ,
				str_date,false);

		View view = new View(SubmitDocument.this);
		view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 2));
		view.setBackgroundDrawable(getResources().getDrawable(R.drawable.divider));
		mTableLayout.addView(view,mLayoutCount);	 
		
		if(isSubmitNeeded){
			isSubmitNeeded = false;
			submitDocumentToServer();
		}
	}

	private void updateProgressdialog(int progress) {
		if (!cancel_flag) {
			if (progressBar != null) {
				progressBar.setProgress(progress);
			}

		}
	}

	private void CloseActivity(int resultCode) {
		Intent returnIntent = new Intent();
		setResult(resultCode, returnIntent);
		finish();
	}

	private void sendBroadcast(int resultCode) {
		Intent i = new Intent(Constants.CUSTOM_INTENT_ITEM_SUBMITTED);
		sendBroadcast(i);
	}

	private void registerBroadcastReceiver() {
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_DOCTYPE_DOWNLOADED);
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					mCustomDialog.closeProgressDialog();
					//extract our message from intent
					String docType = intent.getStringExtra(Constants.STR_DOCUMENT_TYPE);
					Log.i(TAG, "Received broadcast! Downloaded doc type is===> " + docType);
					if(mDocumentObj == null && Constants.IS_HELPKOFAX_FLOW){
						if((mDocMgr.getDocTypeReferenceArray() != null) && (mDocMgr.getDocTypeReferenceArray().get(mDocMgr.getCurrentDocTypeIndex()) != null)) {						
							DocumentType refDocTypeObj = mDocMgr.getDocTypeReferenceArray().get(mDocMgr.getCurrentDocTypeIndex());
							mDocumentObj = new Document(refDocTypeObj);
							if(intent.getStringExtra(Constants.STR_ERROR_MESSAGE) == null || intent.getStringExtra(Constants.STR_ERROR_MESSAGE).equals("")) {
								mItemEntity.setItemSerializedData(mDiskMgr.documentToByteArray(mDocumentObj));
								isSubmitNeeded = true;
								DisplayDocumentInfoOnScreen();								
							}else {
								Toast.makeText(getApplicationContext(), "Error: " + intent.getStringExtra(Constants.STR_ERROR_MESSAGE), Toast.LENGTH_LONG).show();
								Log.e(TAG, "Received error while downloading document type!!!!! ");
							}
						}
					}else{
					Log.i(TAG, "Received broadcast! Expected doc type is===> " + mDocumentObj.getDocumentType().getTypeName());

					//compare if the downloaded documenttype name is same as the one requested from this screen.
					if(docType != null && docType.equalsIgnoreCase(mDocumentObj.getDocumentType().getTypeName())) {
						mCustomDialog.closeProgressDialog();
						//check if error while downloading document type
						if(intent.getStringExtra(Constants.STR_ERROR_MESSAGE) == null || intent.getStringExtra(Constants.STR_ERROR_MESSAGE).equals("")) {
							validateAndSubmit();
						}
						else {
							Toast.makeText(getApplicationContext(), "Error: " + intent.getStringExtra(Constants.STR_ERROR_MESSAGE), Toast.LENGTH_LONG).show();
							Log.e(TAG, "Received error while downloading document type!!!!! ");
						}
					}
					else {
						Log.e(TAG, "Document type does not match.");

					}
					}
				}			
			};
		}
		registerReceiver(mReceiver, intentFilter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		mDocMgr = null;
		mDiskMgr = null;
		mUtilRoutines = null;
		mCustomDialog = null;
		mItemDBManager = null;
		mServerMgr = null;
		mItemEntity = null;
		submitSessionStateHandler = null;
		mSubmittingActivityHandler = null;
	}
}
