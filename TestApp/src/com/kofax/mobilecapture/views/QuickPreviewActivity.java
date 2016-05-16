// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.ImageProcessor.AnalysisCompleteEvent;
import com.kofax.kmc.ken.engines.ImageProcessor.AnalysisCompleteListener;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.ken.engines.data.Image.ImageRep;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;
import com.kofax.kmc.ken.engines.data.QuickAnalysisFeedback;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.kui.uicontrols.ImgReviewEditCntrl;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.kmc.kut.utilities.error.NullPointerException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DatabaseManager.ProcessingStates;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.ImageType;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.Globals.RequestCode;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class QuickPreviewActivity extends Activity implements AnalysisCompleteListener, OnLayoutChangeListener,NetworkChangedListener {

	// - public enums

	// - Private enums
	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = QuickPreviewActivity.class.getSimpleName();

	private Menu menu;
	// - Private data.
	/* SDK objects */
	private Image mInputImageObj = null;
	private QuickAnalysisFeedback qaf = null;
	private ImageProcessor mKMCImageProcessor = null;

	/* Application objects */

	private DatabaseManager mDBManager = null;
	private DiskManager mDiskManager = null;
	private ImageProcessQueueManager mProcessQueueMgr = null;
	private ProcessingStates mImgStatus = ProcessingStates.UNPROCESSED;   //Using for toggle the image 
	private DocumentManager mDocMgrObj = null;
	private UtilityRoutines mUtilRoutines = null;
	/* Standard variables */
	private ImgReviewEditCntrl mPreview;
	private CustomDialog mCustomDialog = null;
	private BroadcastReceiver mReceiver = null;
	private Bitmap mPreviewBitmap = null;
	private Bitmap scaledBmp = null;
	private Timer overlayTimer = null;
	private View feedbackRL = null;
	private RelativeLayout parentLayout = null;
	private RelativeLayout mdetailLayout = null;
	private ViewFlipper mFlipper = null;
	private TextView mImgDetailsTextView = null;
	private List<PageEntity> mPageList = null;
	private Intent mReturnIntent = null;

	private Animation slideLeftIn = null;
	private Animation slideLeftOut = null;
	private Animation slideRightIn = null;
	private Animation slideRightOut = null;

	private ImageType mImageType = null;

	private String mImgUrl = null;
	private long mSelected_PageId = -1;
	private int mImageSourceType = -1;
	private int mImageIndex = -1;
	private int mMaxImageCount = -1;
	private boolean mIsQuickPreview;
	private boolean mIsImgEdited = false;
	private boolean doQuickProcess = false;
	private boolean mIsToggled = false;
	private boolean doProcessRevertedImage = false;
	private boolean isLoading = false;
	private boolean isRequiredOfflineAlert = false;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressLint({ "NewApi", "InflateParams" })
	@Override
	protected void onCreate(Bundle saveBundle) {
		super.onCreate(saveBundle);

		new DeviceSpecificIssueHandler().checkEntryPoint(this);

		setContentView(R.layout.quick_preview_layout);

		feedbackRL = findViewById(R.id.FeedbackRL);
		parentLayout = (RelativeLayout) findViewById(R.id.parentLayout);


		getActionBar().setTitle(getResources().getString(R.string.actionbar_lbl_screen_preview));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			//getActionBar().setHomeButtonEnabled(true);
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		Bundle b = getIntent().getExtras();
		mImgUrl = b.getString(Constants.STR_URL);
		mIsQuickPreview = b.getBoolean(Constants.STR_QUICK_PREVIEW);
		mImageSourceType = b.getInt(Constants.STR_IMG_SOURCE_TYPE);
		mSelected_PageId = b.getLong(Constants.STR_PAGE_ID);

		if(b.containsKey(Constants.STR_OFFLINE_TO_LOGIN) && b.getBoolean(Constants.STR_OFFLINE_TO_LOGIN)){
			isRequiredOfflineAlert = true;
		}

		mDBManager = DatabaseManager.getInstance();
		mDiskManager = DiskManager.getInstance(getApplicationContext());
		mCustomDialog = CustomDialog.getInstance();
		mProcessQueueMgr = ImageProcessQueueManager
				.getInstance(getApplicationContext());
		mDocMgrObj = DocumentManager.getInstance(getApplicationContext());
		mUtilRoutines = UtilityRoutines.getInstance();

		mdetailLayout = (RelativeLayout)findViewById(R.id.imageDetailsLayout);
		mImgDetailsTextView = (TextView)findViewById(R.id.ImageDetailsText);

		if (mIsQuickPreview) {
			mdetailLayout.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					hideDetails();
					return true;
				}
			});
		}

		if (mImgUrl != null) {
			if (mIsQuickPreview) {
				mPreview = (ImgReviewEditCntrl) findViewById(R.id.imgPreview);
				/*
				 * Show quick analysis feedback only if the image is a document and
				 * not a photo, and is just captured. No feedback is shown for
				 * regular preview which is shown on selecting thumbnail from
				 * item-details screen.
				 */
               /* mPreview.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        hideDetails();
                        return true;
                    }
                });*/
				if (mImageSourceType == RequestCode.CAPTURE_PHOTO.ordinal()
						|| mImageSourceType == RequestCode.SELECT_PHOTO.ordinal()) {
					mImageType = ImageType.PHOTO;
				}
				else {
					mImageType = ImageType.DOCUMENT;
				}
			} else {
				if (b.getInt(Constants.STR_IMAGE_TYPE) == ImageType.PHOTO.ordinal()) {
					mImageType = ImageType.PHOTO;
				}
				else {
					mImageType = ImageType.DOCUMENT;
				}

				mImageIndex = b.getInt(Constants.STR_IMAGE_INDEX);

				mFlipper = (ViewFlipper) findViewById(R.id.imgPreviewFlipper);
				mFlipper.addOnLayoutChangeListener(this);
				LayoutInflater inflater = (LayoutInflater) this
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				//load page list for remaining images in item
				mPageList = mDBManager.getAllPagesForItem(this, mDBManager.getItemEntity().getItemId());

				if(mPageList != null) {
					mMaxImageCount = mPageList.size();
					for(int i = 0; i<mMaxImageCount; i++) {
						View child = inflater.inflate(R.layout.quick_preview_image_item,
								null);
						mFlipper.addView(child);
					}
				}

				slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
				slideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
				slideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
				slideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

				/*mFlipper.setOnTouchListener(new CaptureSwipeListener(getApplicationContext()) {
					public void onSwipeTop() {
						//hideDetails();
					}
					public void onSwipeRight() {     
						loadPreviousImage();
					}
					public void onSwipeLeft() { 
						loadNextImage();
					}
					public void onSwipeBottom() {
						//displayDetails();
					}
					public boolean onTouch(View v, MotionEvent event) {
						hideDetails();
						return gestureDetector.onTouchEvent(event);
					}
				});*/

				if(!isDocumentTypeValid()) {
					Toast.makeText(this, getResources().getString(R.string.str_user_msg_no_item_type_exists),
							Toast.LENGTH_LONG).show();
				}
			}

			if(mPageList != null && mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSED.ordinal()){
				mImgStatus = ProcessingStates.PROCESSED;
			}
			loadImage();
		}
		registerBroadcastReceiver();
	}


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		boolean status = mDBManager.isOfflineDocumentSerializedInDB(mDBManager.getProcessingParametersEntity());
		if(mIsQuickPreview && mImageType == ImageType.DOCUMENT ) {
			if((Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || status)){
				boolean isConnectivityAvailable = mUtilRoutines.checkInternet(this);
				//check if reference documentType object is available(to get processing string), if not check if internet connectivity is available.
				if (((mDocMgrObj.getDocTypeReferenceArray() != null) && (mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()) != null)) ||
						isConnectivityAvailable || status) {
					doQuickProcess = true;
					mDocMgrObj.setCurrentHandler(mHandler);
					mCustomDialog.showProgressDialog(QuickPreviewActivity.this, getResources().getString(R.string.progress_msg_please_wait), false);
					mProcessQueueMgr.pauseQueue(mHandler);
				}
				else {
					//show network error message and allow user to accept image
					if(!isConnectivityAvailable) {
						Toast.makeText(this, getResources().getString(R.string.toast_no_network_connectivity) + "\n"+getResources().getString(R.string.toast_error_image_processing_cannot_be_performed), Toast.LENGTH_LONG).show();
					}
				}
			}else if((Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE && !status)){
				Toast.makeText(this, getResources().getString(R.string.toast_no_network_connectivity) + "\n"+getResources().getString(R.string.toast_error_image_processing_cannot_be_performed), Toast.LENGTH_LONG).show();
			}

		}
	}

	@Override
	protected void onResume(){
		super.onResume();
		Constants.NETWORK_CHANGE_LISTENER = QuickPreviewActivity.this;
		if(isRequiredOfflineAlert){
			isRequiredOfflineAlert = false;
			mCustomDialog.dismissAlertDialog();
			mCustomDialog.show_popup_dialog(QuickPreviewActivity.this,AlertType.CONFIRM_ALERT ,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg),
					getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION,mHandler, false);

		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.quick_preview_menu, menu);
		this.menu = menu;
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if(mImgUrl != null) {
			if (mIsQuickPreview) {
				menu.getItem(0).setVisible(true); // show done option
				//menu.getItem(1).setVisible(true); // show accept
				menu.getItem(2).getSubMenu().getItem(0).setVisible(true); // show recapture
			} else {
				menu.getItem(1).setVisible(true); // show delete option
				if(mPageList.get(mImageIndex).getImageType().equals(ImageType.DOCUMENT.name()) &&
						mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSED.ordinal()){
					menu.getItem(2).getSubMenu().getItem(3).setVisible(true);
					menu.getItem(2).getSubMenu().getItem(4).setTitle(getResources().getString(R.string.str_revert));
					if(mImgStatus == ProcessingStates.PROCESSED){
						menu.getItem(2).getSubMenu().getItem(3).setTitle(getResources().getString(R.string.str_image_unprocess));
						menu.getItem(2).getSubMenu().getItem(4).setVisible(true);
					}else{
						menu.getItem(2).getSubMenu().getItem(3).setTitle(getResources().getString(R.string.str_image_process));
						menu.getItem(2).getSubMenu().getItem(4).setVisible(false);
					}
				}else if(mPageList.get(mImageIndex).getImageType().equals(ImageType.DOCUMENT.name()) &&
						(mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.REVERTED.ordinal() ||
								mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSFAILED.ordinal())){
					menu.getItem(2).getSubMenu().getItem(4).setTitle(getResources().getString(R.string.str_process));
					menu.getItem(2).getSubMenu().getItem(4).setVisible(true);
					menu.getItem(2).getSubMenu().getItem(3).setEnabled(false);
				}
				//for lowend devices, if the previewed document is not processed, display 'process' option in menu.
				else if(Constants.BACKGROUND_IMAGE_PROCESSING == false){
					if(mPageList.get(mImageIndex).getImageType().equals(ImageType.DOCUMENT.name()) &&
							mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.UNPROCESSED.ordinal()){
						menu.getItem(2).getSubMenu().getItem(4).setTitle(getResources().getString(R.string.str_process));
						menu.getItem(2).getSubMenu().getItem(4).setVisible(true);
					}
				}
			}
			//hide edit option if image is toggled to unprocessed
			if(mIsToggled) {
				menu.getItem(2).getSubMenu().getItem(1).setVisible(false); // hide edit
			}
			else {
				menu.getItem(2).getSubMenu().getItem(1).setVisible(true); // show edit
			}
			if(!isDocumentTypeValid()) {
				menu.getItem(1).setEnabled(false);	// disable delete option
				menu.getItem(1).setIcon(R.drawable.delete_gray);
				menu.getItem(2).setEnabled(false);	// disable more option
				menu.getItem(2).setIcon(R.drawable.more_gray);
			}
			if(Constants.IS_HELPKOFAX_FLOW){
				menu.getItem(2).getSubMenu().getItem(1).setVisible(false);
			}
		}
		return true;
	}

	private boolean isDocumentTypeValid() {
		String docTypeName = mDBManager.getItemEntity().getItemTypeName();
		boolean isValid = true;
		int index = mDocMgrObj.findDocumentTypeIndex(docTypeName);
		if (index < 0) {
			isValid = false;
		}
		return isValid;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		//      Intent returnIntent = null;

		if (menuItem.getItemId() != R.id.quick_menu_edit_image) {
			mReturnIntent = new Intent();
			mReturnIntent.putExtra(Constants.STR_IMG_SOURCE_TYPE, mImageSourceType);
		}
		switch (menuItem.getItemId()) {

			case R.id.menu_more:
				MenuItem detailitem = menu.findItem(R.id.menu_image_details);
				if(mdetailLayout.getVisibility()== View.VISIBLE){
					detailitem.setVisible(false);
				}
				else{
					detailitem.setVisible(true);
				}
				break;
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.quick_menu_edit_image:
				Intent i = new Intent(this, ImageEditActivity.class);
				i.putExtra(Constants.STR_URL, mImgUrl);
				startActivityForResult(i,
						Globals.RequestCode.PREVIEW_IMAGE.ordinal());
				break;
			case R.id.quick_menu_recapture_image:
				onBackPressed();
				break;
		/*case R.id.quick_menu_accept_image:
			if(mImageType == ImageType.PHOTO) {
				//acceptImage();
				mReturnIntent.putExtra(Constants.STR_IMAGE_TYPE, mImageType.ordinal());
			}
			Log.e(TAG, "mImgUrl =================> " + mImgUrl);
			mReturnIntent.putExtra(Constants.STR_EDITED_IMG_LOCATION, mImgUrl);
			setResult(Globals.ResultState.RESULT_OK.ordinal(), mReturnIntent);
			finish();
			break;*/
			case R.id.quick_menu_delete_image:
				DispalyConfirmationMessage(getResources().getString(R.string.str_user_msg_delete_confirmation_title),
						getResources().getString(R.string.str_user_msg_delete_image_confirmation),
						Messages.MESSAGE_DIALOG_IMAGE_DELETE_CONFIRMATION);
				break;
			case R.id.quick_menu_done:
				mReturnIntent.putExtra(Constants.STR_DONE,true);
				if(mImageType == ImageType.PHOTO) {
					//acceptImage();
					mReturnIntent.putExtra(Constants.STR_IMAGE_TYPE, mImageType.ordinal());
				}
				mReturnIntent.putExtra(Constants.STR_EDITED_IMG_LOCATION, mImgUrl);
				setResult(Globals.ResultState.RESULT_OK.ordinal(), mReturnIntent);
				finish();
				break;
			case R.id.menu_toggle_image:
				if(mImgStatus == ProcessingStates.PROCESSED){
					toggleImage(ProcessingStates.PROCESSED);
				}else{
					toggleImage(ProcessingStates.UNPROCESSED);
				}
				break;
			case R.id.menu_image_revert_image:
				//check if image is processed, if so, the selected menu option is 'revert'. Display a confirmation alert before reverting a processed image back to original.
				if(mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSED.ordinal()) {
					DispalyConfirmationMessage(getResources().getString(R.string.str_user_msg_revert_confirmation_title),
							getResources().getString(R.string.str_user_msg_revert_single_image_confirmation),
							Messages.MESSAGE_DIALOG_IMAGE_REVERT_CONFIRMATION);
				}
				else {
					//if no reference object available and internet connectivity is also not available, image processing cannot be performed(as document object cannot be downloaded). Show error msg to user in such case
					if (((mDocMgrObj.getDocTypeReferenceArray() == null) || (mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()) == null)) &&
							!mUtilRoutines.checkInternet(this)) {
						Toast.makeText(this, getResources().getString(R.string.toast_no_network_connectivity) + "\n"+getResources().getString(R.string.toast_error_image_processing_cannot_be_performed), Toast.LENGTH_LONG).show();
					}
					else {
						//else image is already reverted, the selected menu option is 'process'.
						revertOrProcessCurrentImage();
					}
				}
				break;
			case R.id.menu_image_details:
				displayDetails();
				break;
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "Enter: onactivity result");
		Log.i(TAG, "requestCode :: " + requestCode);
		Log.i(TAG, "resultcode :: " + resultCode);

		if (requestCode == Globals.RequestCode.PREVIEW_IMAGE.ordinal()) {
			if(data != null && data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){
				mUtilRoutines.offlineLogout(QuickPreviewActivity.this);
				return;
			}
			if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
				Log.i(TAG, "Image was edited.. reloading...");
				mIsImgEdited = true;
				loadImage();
			}
		}
	}

	@Override
	public void analysisComplete(AnalysisCompleteEvent event) {
		mCustomDialog.closeProgressDialog();
		if(mPreviewBitmap != null && !mPreviewBitmap.isRecycled()) {
			mPreviewBitmap.recycle();
		}
		mPreviewBitmap = null;
		clearImage(mInputImageObj);
		if(event.getImage() != null && event.getImage().getImageQuickAnalysisFeedBack() != null) {
			qaf = event.getImage().getImageQuickAnalysisFeedBack();
			if (mKMCImageProcessor != null) {
				mKMCImageProcessor.removeAnalysisCompleteEventListener(this);
			}
			if(event.getImage().getImageQuickAnalysisFeedBack().getViewBoundariesImage() != null &&
					!event.getImage().getImageQuickAnalysisFeedBack().getViewBoundariesImage().isRecycled()) {
				event.getImage().getImageQuickAnalysisFeedBack().getViewBoundariesImage().recycle();
			}
			showQuickAnalysisFeedback();
		}
		clearImage(event.getImage());
	}

	@Override
	public void onBackPressed() {
		Intent returnIntent = new Intent();
		returnIntent.putExtra(Constants.STR_IMG_SOURCE_TYPE, mImageSourceType);
		if ((!mIsQuickPreview) && mIsImgEdited) {
			// if its a regular preview, edited image is saved on disk by now. So
			// set RESULT_OK since even though its a back-press, changes in image
			// will not be reverted.
			setResult(Globals.ResultState.RESULT_OK.ordinal(), returnIntent);
		} else {
			if(mIsQuickPreview) {
				mCustomDialog.closeProgressDialog();
				deletePage(mSelected_PageId);
			}
			returnIntent.putExtra(Constants.STR_RETAKE, true);
			setResult(Globals.ResultState.RESULT_CANCELED.ordinal(), returnIntent);
		}
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "on Destroy::");
		super.onDestroy();

		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		if(mDocMgrObj != null){
			mDocMgrObj.removeCurrenthandler();
		}

		if (mKMCImageProcessor != null) {
			resetProcessingProfiles();
			mKMCImageProcessor = null;
		}

		mImgUrl = null;

		if(mPreview != null){
			mPreview.destroyDrawingCache();
			clearImage(mPreview.getImage());
		}

		clearImage(mInputImageObj);

		if (mPreviewBitmap != null && !mPreviewBitmap.isRecycled()) {
			mPreviewBitmap.recycle();
		}
		mPreviewBitmap = null;

		if(scaledBmp != null && !scaledBmp.isRecycled()) {
			scaledBmp.recycle();
		}

		scaledBmp = null;
		if (overlayTimer != null) {
			overlayTimer.cancel();
			overlayTimer.purge();
		}

		if (parentLayout != null) {
			parentLayout.removeAllViews();
			parentLayout.removeCallbacks(Timer_Tick);
			parentLayout = null;
		}
		if(mPageList != null) {
			mPageList.clear();
			mPageList = null;
		}

		if(mFlipper != null) {
			mFlipper.removeAllViews();
			mFlipper = null;
		}
		if(Constants.BACKGROUND_IMAGE_PROCESSING) {
			//mProcessQueueMgr.resumeQueue();
		}
		mDiskManager = null;
		mDBManager = null;
		mDocMgrObj = null;
		Timer_Tick = null;
		mPreview = null;
		mImageIndex = -1;
	}

	// - private nested classes (more than 10 lines)
	private void DispalyConfirmationMessage(String title, String message, Messages messageCode){
		mCustomDialog
				.show_popup_dialog(QuickPreviewActivity.this, AlertType.CONFIRM_ALERT,
						title,
						message,
						null, null,
						messageCode,
						mHandler,
						false);
	}

	// - private methods

	private void revertOrProcessCurrentImage() {
		if (mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSED.ordinal()){
			mPageList.get(mImageIndex).setProcessingStatus((long)ProcessingStates.REVERTED.ordinal());
			mDBManager.getPageForId(getApplicationContext(), mPageList.get(mImageIndex).getPageId()).update();
			mDiskManager.deleteImageFromDisk(mPageList.get(mImageIndex).getProcessedImageFilePath());
			toggleImage(ProcessingStates.PROCESSED);
			mIsImgEdited = true;
		}else if((mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.REVERTED.ordinal()) ||
				(mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSFAILED.ordinal()) ||
				((Constants.BACKGROUND_IMAGE_PROCESSING == false) &&
						(mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.UNPROCESSED.ordinal()))){
			if(scaledBmp != null && !scaledBmp.isRecycled()){
				scaledBmp.isRecycled();
				scaledBmp = null;
			}
			doProcessRevertedImage = true;
			//reset image status to unprocessed for processingQueue to pick this image
			mPageList.get(mImageIndex).setProcessingStatus((long)ProcessingStates.UNPROCESSED.ordinal());
			mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_please_wait), false);
			//pause the image-process-queue first
			mProcessQueueMgr.pauseQueue(mHandler);
		}
	}


	/**
	 * Update the preview image with latest change,Exp:-Display both processed and unprocessed images using menu(Process image/original image)option.
	 * Also Update revert and processed image
	 * @param status :- Image current status
	 */
	private void toggleImage(ProcessingStates status){
		View v;
		KmcZoomImageView iv;

		v = (View)mFlipper.getChildAt(mImageIndex);
		if(status == ProcessingStates.PROCESSED){
			mImgUrl = mPageList.get(mImageIndex).getImageFilePath();
			mImgStatus = ProcessingStates.UNPROCESSED;
			mIsToggled = true;
		}else{
			mImgUrl = mPageList.get(mImageIndex).getProcessedImageFilePath();
			mImgStatus = ProcessingStates.PROCESSED;
			mIsToggled = false;
		}
		iv =  (KmcZoomImageView)v.findViewById(R.id.imgContainer);
		if(mPreviewBitmap != null && !mPreviewBitmap.isRecycled()) {
			mPreviewBitmap.recycle();
		}
		mPreviewBitmap = null;
		if(scaledBmp != null &&
				!scaledBmp.isRecycled()) {
			scaledBmp.recycle();
		}

		scaledBmp = null;
		File temp = new File(mImgUrl);
		if(temp.exists()){
			mPreviewBitmap = mUtilRoutines.getBitmapFromUrl(mImgUrl);
		}

		if(mPreviewBitmap != null) {
			scaledBmp = scaleBitmap(mPreviewBitmap);
			mPreviewBitmap.recycle();
			mPreviewBitmap = null;
			iv.setImageBitmap(scaledBmp);
			iv.setLayoutParams(new RelativeLayout.LayoutParams(scaledBmp.getWidth(),scaledBmp.getHeight()));
		}
		iv.setHandler(mHandler);
		invalidateOptionsMenu();
	}

	private void loadImage() {
		Log.i(TAG, "Enter:: loadImage");
		if (mImgUrl != null) {

			if(scaledBmp != null && !scaledBmp.isRecycled()) {
				scaledBmp.recycle();
			}
			scaledBmp = null;
			File file = new File(mImgUrl);
			if(file.exists()){
				mPreviewBitmap = mUtilRoutines.getBitmapFromUrl(mImgUrl);
			}
			if (mPreviewBitmap != null) {
				if(mIsQuickPreview) {
//					scaledBmp = scaleQuickPreviewBitmap(mPreviewBitmap);
					mPreview.setVisibility(View.VISIBLE);
					try {
						Image mImage = new Image(mPreviewBitmap);
						mPreview.setImage(mImage);
						clearImage(mImage);
					} catch (NullPointerException e) {
						e.printStackTrace();
					} catch (KmcException e) {
						e.printStackTrace();
					}
				}
				else {
					scaledBmp = scaleBitmap(mPreviewBitmap);
					mPreviewBitmap.recycle();
					mPreviewBitmap = null;
					Log.i(TAG, "ImageIndex ==> " + mImageIndex);
					mFlipper.setVisibility(View.VISIBLE);
					View v = (View)mFlipper.getChildAt(mImageIndex);
					Log.i(TAG, "V ==> " + v);
					getActionBar().setTitle(getResources().getString(R.string.actionbar_lbl_screen_preview)+"("+(mImageIndex+1)+"/"+mFlipper.getChildCount()+")");
					KmcZoomImageView iv =  (KmcZoomImageView)v.findViewById(R.id.imgContainer);
					iv.setImageBitmap(scaledBmp);
					iv.setLayoutParams(new RelativeLayout.LayoutParams(scaledBmp.getWidth(),scaledBmp.getHeight()));
					iv.setHandler(mHandler);
					//switch flipper to show child view of selected image index
					mFlipper.setDisplayedChild(mImageIndex);
				}
			}
			else {
				Toast.makeText(this, getResources().getString(R.string.app_msg_image_notfound), Toast.LENGTH_LONG).show();
			}
		}
	}

	private void loadNextImage() {
		if(isLoading) {
			return;
		}
		mIsToggled = false;
		int currentIndex = mFlipper.indexOfChild(mFlipper.getCurrentView());
		if(currentIndex < (mMaxImageCount-1)) {
			isLoading = true;
			mdetailLayout.setVisibility(View.GONE);

			View v = (View)mFlipper.getChildAt(currentIndex);
			KmcZoomImageView iv =  (KmcZoomImageView)v.findViewById(R.id.imgContainer);
			iv.setImageDrawable(null);

			currentIndex++;
			mImageIndex = currentIndex;

			loadSelectedImage(currentIndex);

			mSelected_PageId = mPageList.get(mImageIndex).getPageId();

			mFlipper.setInAnimation(slideRightIn);
			mFlipper.setOutAnimation(slideLeftOut);
			mFlipper.showNext();
		}
	}

	private void loadPreviousImage() {
		if(isLoading) {
			return;
		}
		mIsToggled = false;
		int currentIndex = mFlipper.indexOfChild(mFlipper.getCurrentView());
		if(currentIndex > 0) {
			isLoading = true;
			mdetailLayout.setVisibility(View.GONE);

			View v = (View)mFlipper.getChildAt(currentIndex);
			KmcZoomImageView iv =  (KmcZoomImageView)v.findViewById(R.id.imgContainer);
			iv.setImageDrawable(null);

			currentIndex--;
			mImageIndex = currentIndex;
			loadSelectedImage(currentIndex);

			mSelected_PageId = mPageList.get(mImageIndex).getPageId();

			mFlipper.setInAnimation(slideLeftIn);
			mFlipper.setOutAnimation(slideRightOut);
			mFlipper.showPrevious();
		}
	}

	private void hideDetails(){
		mdetailLayout.setVisibility(View.GONE);
	}

	private void resetProcessingProfiles() {
		mKMCImageProcessor.removeAnalysisCompleteEventListener(this);
		mKMCImageProcessor.setBasicSettingsProfile(null);
		mKMCImageProcessor.setImagePerfectionProfile(null);
	}

	@SuppressLint("StringFormatInvalid")
	private void displayDetails() {

		double filesizeBase64 = 0;
		String text_l = "";
		String str_date = "";
		String img_latitude = null;
		String img_longitude = null;

		File fp = null;
		if(!mIsQuickPreview) {
			if(mPageList.get(mImageIndex).getProcessingStatus() == ProcessingStates.PROCESSED.ordinal() && !mIsToggled){
				fp = new File(mPageList.get(mImageIndex).getProcessedImageFilePath());
			}else{
				fp = new File(mPageList.get(mImageIndex).getImageFilePath());
			}
		}
		else {
			fp = new File(mImgUrl);
		}
		if (fp.exists()) {
			//read geo-location if available.
			try {
				PageEntity current_page = mDBManager.getPageForId(this, mSelected_PageId);
				if(current_page != null && current_page.getImageLatitude() != null) {
					img_latitude = current_page.getImageLatitude();
				}
				if(current_page != null && current_page.getImageLongitude() != null) {
					img_longitude = current_page.getImageLongitude();
				}
				current_page = null;
			} catch (KmcRuntimeException e) {
				e.printStackTrace();
			}
			filesizeBase64 += 4 * Math.round(Math.ceil(fp.length() / 3.0) * 1.03333);

			double mbsize = 1024 * 1024;
			double mbytessize = 0;
			if (filesizeBase64 > mbsize) {
				mbytessize = (filesizeBase64 / (mbsize));
				text_l = String.format(getResources().getString(R.string.str_size)+"             : %.2f "+getResources().getString(R.string.str_MB), mbytessize);
			} else {
				mbytessize = (filesizeBase64 / (1024));
				text_l = String.format(getResources().getString(R.string.str_size)+"             : %.2f "+getResources().getString(R.string.str_KB), mbytessize);
			}
			int height = 0;
			int width = 0;
			//if its a tiff file, bitmap decode will fail. Use Image object API instead.
			if(mUtilRoutines.getMimeType(fp.getAbsolutePath()) == ImageMimeType.MIMETYPE_TIFF) {
				Image tempImg = new Image(fp, ImageMimeType.MIMETYPE_TIFF);
				height = tempImg.getImageFileHeight();
				width = tempImg.getImageFileWidth();
				clearImage(tempImg);
				tempImg = null;
			}
			else {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(fp.getAbsolutePath(), options);

				// get height and width of image
				height = options.outHeight;
				width = options.outWidth;
			}
			String dim = String.format("%dx%d", height, width);

			SimpleDateFormat formater = new SimpleDateFormat("dd MMM, yyyy | hh:mm a");
			Date d = new Date(fp.lastModified());
			str_date = formater.format(d);

			text_l = text_l + "\n"+getResources().getString(R.string.str_dimension)+" : "+dim;
			text_l = text_l + "\n"+getResources().getString(R.string.str_created)+"       : "+str_date;

			if(img_latitude != null && img_longitude != null) {
				text_l = text_l + "\n"+getResources().getString(R.string.str_latitude)+"       : "+img_latitude;
				text_l = text_l + "\n"+getResources().getString(R.string.str_longitude)+"   : "+img_longitude;
			}
			else {
				text_l = text_l + "\n"+getResources().getString(R.string.str_latitude)+"       : "+ getResources().getString(R.string.str_not_available);
				text_l = text_l + "\n"+getResources().getString(R.string.str_longitude)+"   : "+getResources().getString(R.string.str_not_available);
			}

			mImgDetailsTextView.setText(text_l);
			mdetailLayout.setVisibility(View.VISIBLE);
		}
		else {
			Toast.makeText(getApplicationContext(), R.string.toast_error_cannot_retrieve_image_details, Toast.LENGTH_LONG).show();
		}
		fp = null;
	}

	private void loadSelectedImage(int index) {

		View v;
		KmcZoomImageView iv;

		v = (View)mFlipper.getChildAt(index);
		iv =  (KmcZoomImageView)v.findViewById(R.id.imgContainer);
		System.gc();
		if(mPageList.get(index).getProcessedImageFilePath() != null) {
			File fp = new File(mPageList.get(index).getProcessedImageFilePath());
			if(fp.exists()) {
				mImgStatus = ProcessingStates.PROCESSED;
				mImgUrl = mPageList.get(index).getProcessedImageFilePath();
				//if processed image exists, load processed image

			}
			else {
				mImgStatus = ProcessingStates.UNPROCESSED;
				//else load unprocessed image
				mImgUrl = mPageList.get(index).getImageFilePath();
			}
			fp = null;
		}
		else {
			//else load unprocessed image
			mImgUrl = mPageList.get(index).getImageFilePath();
		}
		if(mPreviewBitmap != null && !mPreviewBitmap.isRecycled()) {
			mPreviewBitmap.recycle();
		}
		mPreviewBitmap = null;
		mPreviewBitmap = mUtilRoutines.getBitmapFromUrl(mImgUrl);
		if(mPreviewBitmap != null && !mPreviewBitmap.isRecycled()) {
			scaledBmp = scaleBitmap(mPreviewBitmap);
			mPreviewBitmap.recycle();
			mPreviewBitmap = null;
			if(scaledBmp != null && !scaledBmp.isRecycled()){
				iv.setImageBitmap(scaledBmp);
				iv.setLayoutParams(new RelativeLayout.LayoutParams(scaledBmp.getWidth(),scaledBmp.getHeight()));
			}
			iv.setHandler(mHandler);
			getActionBar().setTitle(getResources().getString(R.string.actionbar_lbl_screen_preview)+"("+(index+1)+"/"+mFlipper.getChildCount()+")");
		}
		invalidateOptionsMenu();
	}

	@SuppressLint("NewApi")
	private Bitmap scaleBitmap(Bitmap bmp) {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int displayWidth = size.x;
		int displayHeight = size.y;

		Bitmap background = Bitmap.createBitmap((int)displayWidth, (int)displayHeight, Config.ARGB_8888);
		try {
			float originalWidth = bmp.getWidth();
			float originalHeight = bmp.getHeight();
			Canvas canvas = new Canvas(background);
			float scale = 1f;
			float xscale = 1f;
			float yscale = 1f;
			xscale = displayWidth/originalWidth;
			yscale = displayHeight/originalHeight;
			scale = (xscale <= yscale) ? xscale : yscale;
			float xTranslation = (displayWidth - originalWidth * scale)/2.0f;
			float yTranslation = (displayHeight - originalHeight * scale)/2.0f;
			Matrix transformation = new Matrix();
			transformation.postTranslate(xTranslation, yTranslation);
			transformation.preScale(scale, scale);
			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(bmp, transformation, paint);
			paint = null;
			transformation = null;
			bmp.recycle();
			bmp = null;
		}catch (Exception e){
			e.printStackTrace();
		}
		return background;

		//  return scaledBmp;
	}

	/* Function to request quick-analysis-feedback for the image. 
	 * The QAF will be requested only once for the lifetime of this activity. 
	 * Once received, the same feedback will be shown on next consecutive requests (if any).*/
	@SuppressWarnings("deprecation")
	private void getQuickAnalysisFeedback() throws KmcException {
		/*if (mPreviewBitmap == null) {
			Log.i(TAG,
					"Error: mPreviewBitmap is NULL.. returning without showing quick-analysis-feedback");
			return;
		}*/
		mPreviewBitmap = mUtilRoutines.getBitmapFromUrl(mImgUrl);
		mInputImageObj = new Image(mPreviewBitmap);
		mKMCImageProcessor = new ImageProcessor();
		mKMCImageProcessor.setProcessedImageRepresentation(ImageRep.IMAGE_REP_BITMAP);
		mKMCImageProcessor.addAnalysisCompleteEventListener(this);
		mKMCImageProcessor.doQuickAnalysis(mInputImageObj, true);
	}

	private void showQuickAnalysisFeedback() {
		// check if the feedback is already visible on screen.
		if(overlayTimer != null) {
			return;
		}
		Log.e(TAG, "isBlurry ==> " + qaf.isBlurry());
		Log.e(TAG, "isOversaturated ==> " + qaf.isOversaturated());
		Log.e(TAG, "isUndersaturated ==> " + qaf.isUndersaturated());

		// if nothing wrond with image, do not show the feedback.
		if(qaf.isBlurry() == false && qaf.isOversaturated() == false && qaf.isUndersaturated() == false) {
			Log.i(TAG, "Image quality is good!");
			return;
		}
		TextView txtFeedback = (TextView) findViewById(R.id.txtFeedback);
		feedbackRL.setVisibility(View.VISIBLE);

		/*		String msg = getResources().getString(R.string.lbl_image)
				+ (qaf.isBlurry() == true ? getResources().getString(R.string.lbl_blurry) : "")
				+ (qaf.isOversaturated() == true ? getResources().getString(R.string.lbl_oversaturated)
						: (qaf.isUndersaturated() == true ? getResources().getString(R.string.lbl_undersaturated)
														: "."));
		 */
		String msg = getResources().getString(R.string.lbl_image) + " ";
		msg += (qaf.isBlurry() == true ? getResources().getString(R.string.lbl_blurry) + " ": "");
		if(qaf.isBlurry()) {
			msg += (qaf.isOversaturated() == true ? getResources().getString(R.string.lbl_and) + " " +getResources().getString(R.string.lbl_oversaturated) : "");
			msg += (qaf.isUndersaturated() == true ? getResources().getString(R.string.lbl_and) + " " + getResources().getString(R.string.lbl_undersaturated) : "");
		}
		else {
			msg += (qaf.isOversaturated() == true ? getResources().getString(R.string.lbl_oversaturated) : "");
			msg += (qaf.isUndersaturated() == true ? getResources().getString(R.string.lbl_undersaturated) : "");
		}
		txtFeedback.setText(msg);

		// start timer to hide the feedback message after 4 seconds
		overlayTimer = new Timer();
		overlayTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				feedbackMsgTimout();
			}
			private void feedbackMsgTimout() {
				runOnUiThread(Timer_Tick);
				overlayTimer = null;
			}
		}, 3000);
	}

	private Runnable Timer_Tick = new Runnable() {
		@Override
		public void run() {
			if(feedbackRL != null){
				feedbackRL.setVisibility(View.INVISIBLE);
			}
		}
	};

	private void clearImage(Image img) {
		if (img != null) {
			img.imageClearBitmap();
			try {
				img.imageClearFileBuffer();
			} catch (KmcException e) {
				e.printStackTrace();
			}
			img = null;
		}
		System.gc();
	}

	/*
	 * This method is responsible for deleting selected page along with processed page from DB and from disk location as well.
	 * */

	private void deletePage(final long pageID){
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = Globals.Messages.MESSAGE_IMAGE_DELETED.ordinal();
				PageEntity page = mDBManager.getPageForId(getApplicationContext(), pageID);
				if(page != null){
					String pageUrl = page.getImageFilePath();
					if(page.getProcessingStatus().intValue() == ProcessingStates.PROCESSED.ordinal()){
						String processed_pageUrl = page.getProcessedImageFilePath();
						if(processed_pageUrl != null && !processed_pageUrl.equals(Constants.STR_EMPTY)){
							mDiskManager.deleteImageFromDisk(processed_pageUrl);
						}
					}
					if(pageUrl != null && !pageUrl.equals(Constants.STR_EMPTY)){
						mDiskManager.deleteImageFromDisk(pageUrl);
					}
					mDBManager.deletePageWithId(getApplicationContext(), pageID);
				}
				else {
					Log.e(TAG, "Page ID not found!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + pageID);
				}
				mHandler.sendMessage(msg);
			}
		});
		t.start();
	}

	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
			switch(whatMessage){
				case MESSAGE_IMAGE_DELETED:
					Intent returnIntent  = new Intent();
					returnIntent.putExtra(Constants.STR_DELETE, true);
					setResult(Globals.ResultState.RESULT_OK.ordinal(), returnIntent);
					finish();
					break;
				case MESSAGE_DIALOG_IMAGE_DELETE_CONFIRMATION:
					if (msg.arg1 == RESULT_OK) {
						deletePage(mSelected_PageId);
					}
					break;
				case MESSAGE_DIALOG_IMAGE_REVERT_CONFIRMATION:
					if (msg.arg1 == RESULT_OK) {
						revertOrProcessCurrentImage();
					}
					break;
				case MESSAGE_PROCESS_QUEUE_PAUSED:
					Log.e(TAG, "Queue Paused!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					if(doQuickProcess || doProcessRevertedImage) {
						doQuickProcess = false;
						doProcessRevertedImage = false;
						if(mIsQuickPreview) {
							//boolean status = acceptImage();
							//if(!status) {
							//	mCustomDialog.closeProgressDialog();
							//	Toast.makeText(getApplicationContext(), "Image update failed, please try again", Toast.LENGTH_LONG).show();
							//}
						}
						if ((Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mDBManager.isOfflineDocumentSerializedInDB(mDBManager.getProcessingParametersEntity()))) {
							mProcessQueueMgr.addItemToQueue(mDBManager
									.getItemEntity());
							if(Constants.BACKGROUND_IMAGE_PROCESSING) {
								mProcessQueueMgr.resumeQueue();
							}
						}
					}
					break;
				case MESSAGE_IMAGE_FLIP:
					try {
						if(msg.arg1 == 1){
							loadPreviousImage();
						}else if(msg.arg1 == 2){
							loadNextImage();
						}else{
							hideDetails();
						}
					}catch (Exception e){
						e.printStackTrace();
					}
					break;
				case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
					//Navigate back to settings
					if(msg.arg1 == RESULT_OK){
						mUtilRoutines.offlineLogout(QuickPreviewActivity.this);
					}else{
						Globals.gAppModeStatus = Globals.AppModeStatus.FORCE_OFFLINEMODE;
					}
					break;
				case MESSAGE_DOWNLOAD_DOCUMENTS_FAILED:
					mCustomDialog.closeProgressDialog();
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_error_field_details_cannot_be_downloaded), Toast.LENGTH_SHORT).show();
					if(mIsQuickPreview){
						doQuickProcess = false;
						mProcessQueueMgr.pauseQueue(mHandler);
					}
					break;
				default:
					break;
			}
			return true;
		}
	});

	private void registerBroadcastReceiver() {
		// Log.i(TAG, "registerBroadcastReceiver");

		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {

					Log.i(TAG,
							"Broadcast received!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					if (intent.getAction() == Constants.CUSTOM_INTENT_IMAGE_PROCESSED) {
						Bundle bundle = intent.getExtras();
						//match page id to check if the displayed image is processed. If not, wait till the correct image is processed.
						if(mPageList != null && (mPageList.get(mImageIndex).getPageId() != bundle.getLong(Constants.STR_PAGE_ID))) {
							return;
						}
						if(mIsQuickPreview) {
							//request for quickanalysisfeedback once processing is completed
							if(intent.hasExtra(Constants.STR_PROCESS_STATUS) && intent.getIntExtra(Constants.STR_PROCESS_STATUS, 0) != 0){
								mCustomDialog.closeProgressDialog();
								Toast.makeText(context, getResources().getString(R.string.toast_processing_failed), Toast.LENGTH_SHORT).show();
								return;
							}
							try {
								if(mDocMgrObj != null && mDocMgrObj.getDocTypeReferenceArray()!= null && mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()) != null){
									DocumentType currentDocumentType = mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex());
									if(currentDocumentType != null && currentDocumentType.getImagePerfectionProfile() != null){
										ImagePerfectionProfile ipp = currentDocumentType.getImagePerfectionProfile();
										if(ipp.getIpOperations() != null && ipp.getIpOperations().contains(Constants.STR_DO_BLUR_ILLUMINATION)){
											getQuickAnalysisFeedback();
										}else{
											mCustomDialog.closeProgressDialog();
										}
									}else{
										mCustomDialog.closeProgressDialog();
									}

								}

							} catch (KmcException e) {
								e.printStackTrace();
								mCustomDialog.closeProgressDialog();
							}
							if(intent.hasExtra(Constants.STR_PAGE_ID)) {
								Long pageId = intent.getLongExtra(Constants.STR_PAGE_ID, -1);
								if(pageId != -1) {
									mImgUrl = mDBManager.getPageForId(QuickPreviewActivity.this, pageId).getProcessedImageFilePath();
								}
							}
							loadImage();
						}
						else {
							mCustomDialog.closeProgressDialog();

							if(intent.hasExtra(Constants.STR_PROCESS_STATUS) && intent.getIntExtra(Constants.STR_PROCESS_STATUS, 0) == 0) {
								mImgUrl = mDBManager.getPageForId(QuickPreviewActivity.this, mSelected_PageId).getProcessedImageFilePath();
								updateProcessState((long)ProcessingStates.PROCESSED.ordinal(), ProcessingStates.UNPROCESSED);
							}else{
								updateProcessState((long)ProcessingStates.PROCESSFAILED.ordinal(), ProcessingStates.PROCESSED);
							}
						}

					}
				}
			};
		}
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_IMAGE_PROCESSED);
		registerReceiver(mReceiver, intentFilter);
	}

	private void updateProcessState(Long status, ProcessingStates state){
		mPageList.get(mImageIndex).setProcessingStatus(status);
		mDBManager.getPageForId(getApplicationContext(), mPageList.get(mImageIndex).getPageId()).update();
		toggleImage(state);
	}


	@Override
	public void onLayoutChange(View v, int left, int top, int right,
							   int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		isLoading = false;
	}


	@Override
	public void onNetworkChanged(boolean isConnected) {
		if(Globals.isRequiredOfflineAlert()  && isConnected && mUtilRoutines.isAppOnForeground(QuickPreviewActivity.this)){
			if(mCustomDialog != null){
				mCustomDialog.dismissAlertDialog();
				mCustomDialog.show_popup_dialog(QuickPreviewActivity.this,AlertType.CONFIRM_ALERT ,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg),
						getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION,mHandler, false);
			}
		}else{
			if(mCustomDialog != null){
				mCustomDialog.dismissAlertDialog();
			}
		}
	}
}

