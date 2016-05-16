// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.data.DocumentDetectionSettings;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.klo.logistics.data.FieldType;
import com.kofax.kmc.kui.uicontrols.AutoFocusResultEvent;
import com.kofax.kmc.kui.uicontrols.AutoFocusResultListener;
import com.kofax.kmc.kui.uicontrols.CameraInitializationEvent;
import com.kofax.kmc.kui.uicontrols.CameraInitializationFailedEvent;
import com.kofax.kmc.kui.uicontrols.CameraInitializationFailedListener;
import com.kofax.kmc.kui.uicontrols.CameraInitializationListener;
import com.kofax.kmc.kui.uicontrols.ImageCaptureView;
import com.kofax.kmc.kui.uicontrols.ImageCapturedEvent;
import com.kofax.kmc.kui.uicontrols.ImageCapturedListener;
import com.kofax.kmc.kui.uicontrols.LevelnessEvent;
import com.kofax.kmc.kui.uicontrols.LevelnessListener;
import com.kofax.kmc.kui.uicontrols.PageDetectionEvent;
import com.kofax.kmc.kui.uicontrols.PageDetectionListener;
import com.kofax.kmc.kui.uicontrols.StabilityDelayEvent;
import com.kofax.kmc.kui.uicontrols.StabilityDelayListener;
import com.kofax.kmc.kui.uicontrols.captureanimations.CaptureExperience;
import com.kofax.kmc.kui.uicontrols.captureanimations.CaptureMessage;
import com.kofax.kmc.kui.uicontrols.captureanimations.DocumentCaptureExperience;
import com.kofax.kmc.kui.uicontrols.captureanimations.DocumentCaptureExperienceCriteriaHolder;
import com.kofax.kmc.kui.uicontrols.data.Flash;
import com.kofax.kmc.kui.uicontrols.data.ImageCaptureFrame;
import com.kofax.kmc.kui.uicontrols.data.PageDetectMode;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.AppStatsManager;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.CustomUrlUtils;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.RequestCode;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class Capture extends Activity implements OnClickListener,
		ImageCapturedListener, OnTouchListener, NetworkChangedListener,
		StabilityDelayListener, LevelnessListener,
		AutoFocusResultListener, PageDetectionListener,
		CameraInitializationListener, CameraInitializationFailedListener {

	// - public enums

	// - Private enums
	enum ImageMode {
		DOCUMENT_MODE,
		PHOTO_MODE
	};


	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = Capture.class.getSimpleName();
	private final int mOffSetThreshold = 85;
	private final int savePitchThreshold = 7;

	private final int saveRollThreshold = 7;
	private final int offSetThreshold = 85;

	// - Private data.
	/* SDK objects */
	private ImageCaptureView mImgCapView = null;
	private ImageCaptureFrame icf = null;
	private Image capturedImage = null;

	private CaptureExperience mCaptureExperience = null;
	private DocumentCaptureExperience mDocumentCaptureExperience = null;
	private CaptureMessage mUserInstructionMessage = null, mZoomInMessage = null, mZoomOutMessage = null,
			mCenterMessage = null,mHoldSteadyMessage = null, mCapturedMessage = null;


	/* Application objects */
	private DocumentManager mDocMgrObj = null;
	private ImageProcessQueueManager mProcessQueueMgr = null;
	private PrefManager mPrefUtilsObj = null;
	private DiskManager mDiskMgrObj = null;
	private DatabaseManager mItemDBManager = null;
	private UtilityRoutines mUtilityRoutines = null;
	private AppStatsManager mAppStatsManager = null;
	private CustomUrlUtils mCustomUrlUtils = null;
	/* Standard variables */

	private GalleryAdapter adapter = null;
	private View mImgFlashAuto = null;
	private View mImgFlashOn = null;
	private View mImgFlashOff = null;
	private View mFlashOptionsLayout = null;
	private ImageView mPreviewThumbnail, mPreviewOverlay;
	private ImageView mIVFlash = null;
	//private ImageView mReferenceFrame = null;
	private TextView mTVCount;
	private TextView mTVDone;
	private View mImageCountLayout = null;
	private FrameLayout mParentLayout = null;
	private ImageView mPhotoCaptureButton = null;
	private Button mGalleryButton = null;
	private Bitmap mBmpAnimatePreview = null;
	private Bitmap mBmpThumbnail = null;
	private Handler mManualCaptureHandler = null;
	private Runnable mManualCaptureRunnable = null;

	final private int REQUEST_CODE_ASK_PERMISSIONS = 1565;

	/**
	 * To hold a reference to the current animator, so that it can be canceled
	 * mid-way.
	 */
	private Animator mCurrentAnimator;
	private BroadcastReceiver mReceiver = null;
	private CustomDialog mCustomDialog = null;


	private String lastCapturedImage = null;

	private int mSavePitchThreshold = 7;
	private int mSaveRollThreshold = 7;
	private int mTotalImgCount = 0;
	private int mCurrentCaptureCount = 0;


	private int mShortAnimationDuration = 1000;
	private boolean mIsCaptureCalled = false;
	private boolean mIsNewItem = true;

	private ImageMode mImageMode = ImageMode.DOCUMENT_MODE;
	private Gallery mCaptureTypeText = null;
	private boolean mIsGiftCard = false;
	private boolean mIsGalleryFlow = false;
	private boolean isAutoCaptureStopped = false;
	private boolean mIsOfflineAlertRequest = false;
	private boolean mIsDownloadStart = false;
	private boolean misSendForProcess = false;
	private boolean misFirstTimeVisible = false;

	private int selectedMode = RequestCode.SELECT_DOCUMENT.ordinal();
	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);

		new DeviceSpecificIssueHandler().checkEntryPoint(this);

		setContentView(R.layout.capture);

		mPrefUtilsObj = PrefManager.getInstance();
		mDiskMgrObj = DiskManager.getInstance(getApplicationContext());
		mItemDBManager = DatabaseManager.getInstance();
		mDocMgrObj = DocumentManager.getInstance(getApplicationContext());
		mProcessQueueMgr = ImageProcessQueueManager.getInstance(getApplicationContext());
		mCustomDialog = CustomDialog.getInstance();
		mUtilityRoutines = UtilityRoutines.getInstance();
		mAppStatsManager = AppStatsManager.getInstance(this);
		mCustomUrlUtils = CustomUrlUtils.getInstance();

		Bundle bundle = getIntent().getExtras();
		if(bundle != null){
			mIsGiftCard = bundle.getBoolean(Constants.STR_GIFTCARD);
		}

		capturedImage = null;
		Log.e(TAG, "Constants.APP_STAT_ENABLED ==> " + Constants.APP_STAT_ENABLED);
		Log.e(TAG, "mAppStatsManager.isRecordingOn() ==> " + mAppStatsManager.isRecordingOn());
        if (Constants.APP_STAT_ENABLED && !mAppStatsManager.isRecordingOn()) {
        	Log.e(TAG, "Starting appStatRecord");
            mAppStatsManager.startAppStatsRecord();
        }
		
		mTVDone = (TextView) findViewById(R.id.camera_done);
		mPreviewThumbnail = (ImageView) findViewById(R.id.img_thumbnail_view);
		mImageCountLayout = findViewById(R.id.image_count_layout);
		mTVCount = (TextView) findViewById(R.id.countText);
		mPreviewOverlay = (ImageView) findViewById(R.id.doc_preview_imagevieww);

		mPhotoCaptureButton = (ImageView)findViewById(R.id.capture);
		mPhotoCaptureButton.setOnClickListener(this);
		mPhotoCaptureButton.setOnTouchListener(mCaptureButtonOnTouchListener);

		//set the runtime dimension of mPreviewThumnail equal to dimension of center capture button. This is to change the dimension according to device resource resolution at runtime. 
		mPreviewThumbnail.getLayoutParams().height = getResources().getDrawable(R.drawable.capture_button_active).getIntrinsicHeight();
		mPreviewThumbnail.getLayoutParams().width = getResources().getDrawable(R.drawable.capture_button_active).getIntrinsicHeight();
		mPreviewThumbnail.setVisibility(View.VISIBLE);

		// Inflate ImageCaptureView runtime.
		mParentLayout = (FrameLayout) findViewById(R.id.parent_layout);
		mImgCapView = (ImageCaptureView) findViewById(R.id.viewFinder);

		mTVDone.setOnClickListener(this);
		mTVDone.setOnTouchListener(this);

		mGalleryButton = (Button)findViewById(R.id.Gallery_view);
		mCaptureTypeText = (Gallery)findViewById(R.id.cameratype_gallery_view);

		if (!mIsGiftCard) {
			mGalleryButton.setOnClickListener(this);
			mGalleryButton.setVisibility(View.VISIBLE);
			
			adapter = new GalleryAdapter(this);
			if(!Constants.IS_HELPKOFAX_FLOW){
			mCaptureTypeText.setAdapter(adapter);
			misFirstTimeVisible = true;
			mCaptureTypeText.setOnItemSelectedListener(mCaptureTypeTextSelectedListener);

			mParentLayout.setOnTouchListener(new CaptureSwipeListener(getApplicationContext()) {
				public void onSwipeTop() {
				}
				public void onSwipeRight() {             
					mCaptureTypeText.setSelection(0);
					startManualCaptureTimer();
				}
				public void onSwipeLeft() {            
					mCaptureTypeText.setSelection(1);
					stopManualCaptureTimer();
				}
				public void onSwipeBottom() {
				}
				public boolean onTouch(View v, MotionEvent event) {
					return gestureDetector.onTouchEvent(event);
				}
			});
			}

			mTotalImgCount = getIntent().getIntExtra(Constants.STR_IMAGE_COUNT, 0);
			mIsNewItem = getIntent().getBooleanExtra(Constants.STR_IS_NEW_ITEM, true);

			lastCapturedImage = getIntent().getStringExtra(Constants.LAST_IMAGE_PATH);

		}else{
			mGalleryButton.setVisibility(View.GONE);
			startManualCaptureTimer();
			mIsNewItem = true;
			mPhotoCaptureButton.setEnabled(false);
			mPhotoCaptureButton
					.setImageResource(R.drawable.capture_button_disabled);
		}


		initPreview();
		if(!Constants.BACKGROUND_IMAGE_PROCESSING) {
			registerBroadcastReceiver();
		}

		mImgCapView.setVisibility(View.VISIBLE);

		mImgCapView.addCameraInitializationListener(this);
		mImgCapView.addCameraInitializationFailedListener(this);

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mDocMgrObj.getImgUrlList() != null && !mIsGiftCard) {
			updateActivity();
		}
	}

	@Override
	protected void onResume(){
		super.onResume();
		Constants.NETWORK_CHANGE_LISTENER = Capture.this;
		if(Globals.gAppModeStatus == Globals.AppModeStatus.FORCE_OFFLINEMODE){

			mIsOfflineAlertRequest = false;
		}
	}

	@Override
	protected void onPause(){
		super.onPause();
		removeCaptureExperience();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.capture_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		RequestCode myRequestCode = Globals.getRequestCode(requestCode);
		// Getting image from Gallery
		if(requestCode == RequestCode.SELECT_DOCUMENT.ordinal() || requestCode == RequestCode.SELECT_PHOTO.ordinal()){
			mGalleryButton.setEnabled(true);
			if (resultCode == RESULT_OK) {
				if (null != data) {
					Uri imageUri = data.getData();
					String[] filePathColumns = { MediaColumns.DATA,
							MediaColumns.DISPLAY_NAME };

					Cursor cursor = getContentResolver().query(imageUri,
							filePathColumns, null, null, null);					
					cursor.moveToFirst();
					int columnIndex = cursor
							.getColumnIndex(MediaColumns.DATA);
					String galleryFilePath = cursor.getString(columnIndex);
					Log.e(TAG, "Filepath ===> " + galleryFilePath);
					Bitmap bmp = null;
					//Image tempImage = null;
					// this is for images selected from Picasa albums
					// (Note: MediaColumns.DISPLAY_NAME is supported by  Picasa)
					if (capturedImage != null) {
						capturedImage = null;
					}
					columnIndex = cursor
							.getColumnIndex(MediaColumns.DISPLAY_NAME);
					if (columnIndex != -1) {
						try {
							final InputStream is = getContentResolver()
									.openInputStream(imageUri);
							BufferedInputStream bufferedInputStream = new BufferedInputStream(
									is);
							bmp = BitmapFactory
									.decodeStream(bufferedInputStream);
							bufferedInputStream = null;
							if (bmp != null) {
								capturedImage = new Image(bmp);
								bmp = null;
							}
							else if(bmp == null) {
								Toast.makeText(this, getResources().getString(R.string.toast_unable_to_decode_file), Toast.LENGTH_LONG).show();
							}
						} catch (FileNotFoundException e) {
							Toast.makeText(this, ""+e.getCause(), Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
					}
					cursor.close();
					if (capturedImage != null) {
						String imageDiskLocation = null;
						long pageId = -1;
						if (mPrefUtilsObj.sharedPref.getBoolean(
								mPrefUtilsObj.KEY_QUICK_PREVIEW, true)) {
							if (myRequestCode == RequestCode.SELECT_DOCUMENT && !Constants.IS_HELPKOFAX_FLOW) {
								
								pageId = acceptImage(
										Globals.ImageType.DOCUMENT.ordinal(),
										capturedImage, galleryFilePath);  
							}
							else {
								pageId = acceptImage(
										Globals.ImageType.PHOTO.ordinal(),
										capturedImage, galleryFilePath);
							}
							mIsGalleryFlow = true;
							if(pageId != -1) {
								imageDiskLocation = mItemDBManager.getPageForId(Capture.this, pageId).getImageFilePath();
							}
							if(imageDiskLocation != null) {
								if(capturedImage != null) {
									capturedImage = null;
								}
								showQuickPreview(myRequestCode, imageDiskLocation, pageId);
							}
							else {
								Toast.makeText(getApplicationContext(), R.string.toast_save_image_failed, Toast.LENGTH_LONG).show();
							}
						} else { // save image to disk
							if (myRequestCode == RequestCode.SELECT_DOCUMENT && !Constants.IS_HELPKOFAX_FLOW) {
								acceptImage(
										Globals.ImageType.DOCUMENT.ordinal(),
										capturedImage, galleryFilePath);
								if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mItemDBManager.isOfflineDocumentSerializedInDB(mItemDBManager.getProcessingParametersEntity())){
								mProcessQueueMgr.addItemToQueue(mItemDBManager.getItemEntity()); // gives the currently selected itemEntity
								}
							} else {
								acceptImage(
										Globals.ImageType.PHOTO.ordinal(),
										capturedImage, galleryFilePath);
							}
							// open image gallery again to select next
							// image.
							// image gallery will be displayed until user
							// cancels on gallery screen
							/* if background image-processing is enabled or image-type is PHOTO, then launch gallery. 
							 * If image-type is DOCUMENT and background processing is disabled, wait for foreground image processing to complete and 
							 * launch gallery from broadcast receiver callback */
							if(Constants.BACKGROUND_IMAGE_PROCESSING || myRequestCode == RequestCode.SELECT_PHOTO) {
								openImageGallery(myRequestCode.ordinal());
							}
							updateScreenOnImageCapture();
						}
					}else{
						mIsGalleryFlow = false;
						// show imageCaptureView back when gallery is closed.
						mImgCapView.setVisibility(View.VISIBLE);
						mImgCapView.invalidate();
					}
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.str_user_msg_no_image_selected), Toast.LENGTH_LONG).show();
				}
			} else if (resultCode == RESULT_CANCELED) {
				mIsGalleryFlow = false;
				// show imageCaptureView back when gallery is closed.
				mImgCapView.setVisibility(View.VISIBLE);
				mImgCapView.invalidate();
				if (mImageMode == ImageMode.DOCUMENT_MODE) {
					setDocumentMode();
				} else {
					setPhotoMode();
				}
			}
	}

		else if (requestCode == RequestCode.PREVIEW_IMAGE.ordinal()) {
			
			if(data != null && data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
				mUtilityRoutines.offlineLogout(this);					
				return;
			}

			// if image is rejected on preview screen
			if (resultCode == Globals.ResultState.RESULT_CANCELED.ordinal()) {
				//				Log.i(TAG, "Image was rejected at preview screen");
				if(mIsGalleryFlow) {
					mIsGalleryFlow = false;
				}
				if (capturedImage != null) {
					capturedImage = null;
					System.gc();
				}
				if(data != null && !data.getBooleanExtra(Constants.STR_RETAKE,true)){
					finish();
				}
				else {
					mImgCapView.setVisibility(View.VISIBLE);
				}
			}
			// if image is accepted on preview screen
			else if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
				/* if image was edited and accepted, use the image location sent by quickpreview screen 
				 * where the edited image is saved and update the local capturedImage variable to use it
				 * to save it further at the regular location.
				 */
				Bundle b = data.getExtras();
				String imgFilePath = b.getString(Constants.STR_EDITED_IMG_LOCATION);
				
				/*if user has selected 'Done' option on preview screen to end the capture flow and proceed with next step,
				then close capture screen and send appropriate parameters to ItemDetailsScreen to launch further screen." 
				 */
				if(b.containsKey(Constants.STR_DONE)){
					// open image gallery again to select next
					// image.
					// image gallery will be displayed until user
					// cancels on gallery screen
					if(mIsGalleryFlow){
						//						mIsGalleryFlow = false;
						if (mImageMode == ImageMode.DOCUMENT_MODE) {
							openImageGallery(RequestCode.SELECT_DOCUMENT.ordinal());
						} else {
							openImageGallery(RequestCode.SELECT_PHOTO.ordinal());
						}
					}
					//get the edited image file from received location and assign it to capturedImage object
					clearImage(capturedImage);		
					ImageMimeType mime = mUtilityRoutines.getMimeType(imgFilePath);
					if(mime == ImageMimeType.MIMETYPE_JPEG) {
						capturedImage = new Image(imgFilePath, ImageMimeType.MIMETYPE_JPEG);
					}
					else if(mime == ImageMimeType.MIMETYPE_TIFF) {
						capturedImage = new Image(imgFilePath, ImageMimeType.MIMETYPE_TIFF);
					}
					else if(mime == ImageMimeType.MIMETYPE_PNG) {
						capturedImage = new Image(imgFilePath, ImageMimeType.MIMETYPE_PNG);
					}
					updateScreenOnImageCapture();
					createAndSaveDocument();
				}
			}
		}else if (requestCode == RequestCode.EDIT_FIELDS.ordinal()){
			if (resultCode == Globals.ResultState.RESULT_CANCELED.ordinal()) {
				finish();
			}
			else if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
				finish();
			}
		}
	}

	// Events and Callback Handlers

	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.camera_done:
			createAndSaveDocument();
			Intent returnIntent = new Intent();
			returnIntent.putExtra(Constants.STR_CAPTURE_COUNT, mCurrentCaptureCount);
			setResult(Globals.ResultState.RESULT_OK.ordinal(), returnIntent);
			finish();
			break;
		case R.id.capture:
			if (!mIsCaptureCalled) {
				mImgCapView.forceTakePicture();
				mPhotoCaptureButton.setEnabled(false);
				mPhotoCaptureButton.setImageResource(R.drawable.capture_button_disabled);
			}
			break;
		case R.id.Gallery_view:
			mIsGalleryFlow = true;
			mGalleryButton.setEnabled(false);
			if (mImageMode == ImageMode.DOCUMENT_MODE) {
				openImageGallery(RequestCode.SELECT_DOCUMENT.ordinal());
			} else {
				openImageGallery(RequestCode.SELECT_PHOTO.ordinal());
			}
			//hide image capture view when image-gallery is selected.
			mImgCapView.setVisibility(View.GONE);
			break;
		default:
			break;
		}
	}

	@Override
	public void onBackPressed() {
		createAndSaveDocument();
		Intent returnIntent = new Intent();
		returnIntent.putExtra(Constants.STR_CAPTURE_COUNT, mCurrentCaptureCount);
		setResult(Globals.ResultState.RESULT_CANCELED.ordinal(), returnIntent);

		finish();
	}

	// Volume button integration to force take picture
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (null != mImgCapView && (mImgCapView instanceof ImageCaptureView)
					&& (mImgCapView.getVisibility() == View.VISIBLE)) {
				// We should hide volume dialog in only camera view finder.
				return true;
			} else {
				// We should hide volume dialog in only camera view finder.
				return false;
			}
		} else {
			// We should hide volume dialog in only camera view finder.
			return false;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			if (null != mImgCapView && (mImgCapView instanceof ImageCaptureView)
					&& (mImgCapView.getVisibility() == View.VISIBLE)) {

				if (!mIsCaptureCalled ){//&& mPrefUtilsObj.sharedPref.getBoolean(mPrefUtilsObj.KEY_MANUAL_CAPTURE, true)) {
					mImgCapView.forceTakePicture();
				}
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBackPressed();
			return true;
		} else if(keyCode == KeyEvent.KEYCODE_MENU){
			return true;
		}
		return false;
	}

	@Override
	public void onImageCaptured(ImageCapturedEvent arg0) {
		removeCallbacks();
		if (capturedImage != null) {
			capturedImage = null;
		}
		System.gc();

		capturedImage = arg0.getImage();	//TODO: save image at a temp location instead of using this global variable.
		if(mIsGiftCard) {
			String imgLocation = mDiskMgrObj.saveImageToTempLocation(arg0.getImage());
			if (imgLocation != null) {
				Intent i = new Intent(getApplicationContext(), GiftCardPreviewActivity.class);
				i.putExtra(Constants.STR_URL, imgLocation);
				startActivity(i);
				//close capture activity as soon as giftcard preview is shown
				finish();
			}
		}
		else if (mPrefUtilsObj.sharedPref.getBoolean(mPrefUtilsObj.KEY_QUICK_PREVIEW, true)) {
			long pageId = -1;
			removeCaptureExperience();
			if(mImageMode == ImageMode.DOCUMENT_MODE) {

				stopManualCaptureTimer();
				if(Constants.IS_HELPKOFAX_FLOW){
					pageId = acceptImage(Globals.ImageType.PHOTO.ordinal(),arg0.getImage(), null);
				}else{
					pageId = acceptImage(Globals.ImageType.DOCUMENT.ordinal(),arg0.getImage(), null);
				}
			}
			else {
				pageId = acceptImage(Globals.ImageType.PHOTO.ordinal(),arg0.getImage(), null);
			}
			if (pageId != -1) {
				Intent i = null;
				if(mIsGiftCard) {
					i = new Intent(getApplicationContext(), GiftCardPreviewActivity.class);
				}
				else {
					i = new Intent(getApplicationContext(), QuickPreviewActivity.class);
					i.putExtra(Constants.STR_QUICK_PREVIEW, true);	 //flag to indicate this is a quick preview after capture and not the regular preview.
					if(mImageMode == ImageMode.DOCUMENT_MODE && !Constants.IS_HELPKOFAX_FLOW) {
						i.putExtra(Constants.STR_IMG_SOURCE_TYPE, RequestCode.CAPTURE_DOCUMENT.ordinal());	// type to indicate captured image is of type 'document'
					}
					else {
						i.putExtra(Constants.STR_IMG_SOURCE_TYPE, RequestCode.CAPTURE_PHOTO.ordinal());	// type to indicate captured image is of type 'photo'
					}
				}
				i.putExtra(Constants.STR_PAGE_ID, pageId);
				i.putExtra(Constants.STR_URL, mItemDBManager.getPageForId(Capture.this, pageId).getImageFilePath());

				startActivityForResult(i, RequestCode.PREVIEW_IMAGE.ordinal());
			}
			else {
				Toast.makeText(this,getResources().getString(R.string.toast_save_image_failed), Toast.LENGTH_LONG).show();
			}
		} else {
			long pageId = -1;
			String imageDiskLocation = null;
			if(mImageMode == ImageMode.DOCUMENT_MODE) {
				stopManualCaptureTimer();
				startManualCaptureTimer();
				if(Constants.IS_HELPKOFAX_FLOW){
					pageId = acceptImage(Globals.ImageType.PHOTO.ordinal(),arg0.getImage(), null);
				}else{
				pageId = acceptImage(Globals.ImageType.DOCUMENT.ordinal(),arg0.getImage(), null);
				}
				setDocumentMode();
			}
			else {
				pageId = acceptImage(Globals.ImageType.PHOTO.ordinal(),arg0.getImage(), null);
				setPhotoMode();
			}
			if(pageId != -1) {
				imageDiskLocation = mItemDBManager.getPageForId(Capture.this, pageId).getImageFilePath();
				if(imageDiskLocation != null) {
					updateScreenOnImageCapture();
					if(Constants.BACKGROUND_IMAGE_PROCESSING) {
						if(!misSendForProcess && mImageMode == ImageMode.DOCUMENT_MODE && (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mItemDBManager.isOfflineDocumentSerializedInDB(mItemDBManager.getProcessingParametersEntity()))) {
							mIsDownloadStart = true;
							mDocMgrObj.setCurrentHandler(captureHandler);
							mProcessQueueMgr.addItemToQueue(mItemDBManager.getItemEntity());	//mItemDBManager.getItemEntity() gives the currently selected itemEntity
						}
					}
				}
			}
		}
		Log.e(TAG, "Image mode is ===> " + mImageMode);
		if(mImageMode == ImageMode.PHOTO_MODE) {
			mPhotoCaptureButton.setEnabled(true);
			Log.e(TAG, "enabling capture button");
			mPhotoCaptureButton.setImageResource(R.drawable.capture_button_enabled);
		}
	}



	@Override
	public void onLevelness(LevelnessEvent arg0) {
	}

	@Override
	public void onStabilityDelay(StabilityDelayEvent arg0) {
	}

	@Override
	public void pageDetected(PageDetectionEvent event) {

	}

	@Override
	public void onAutoFocus(AutoFocusResultEvent arg0) {

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			((TextView) v).setBackgroundColor(0xff32b4e4);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			((TextView) v).setBackgroundColor(Color.TRANSPARENT);
		}
		return false;
	}

	OnClickListener flashOptionClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int flash = 0;

			if (v == mImgFlashAuto) {
				mIVFlash.setImageDrawable(getResources().getDrawable(
						R.drawable.ic_flash_automatic));
				flash = Flash.AUTO.ordinal();
				mImgCapView.setFlash(Flash.AUTO);
			} else if (v == mImgFlashOff) {
				mIVFlash.setImageDrawable(getResources().getDrawable(
						R.drawable.ic_flash_off));
				flash = Flash.OFF.ordinal();
				mImgCapView.setFlash(Flash.OFF);
			} else if (v == mImgFlashOn) {
				mIVFlash.setImageDrawable(getResources().getDrawable(
						R.drawable.ic_flash_on));
				flash = Flash.ON.ordinal();
				mImgCapView.setFlash(Flash.ON);
			}
			mIVFlash.refreshDrawableState();
			// update preferences
			mPrefUtilsObj.putPrefValueInt(mPrefUtilsObj.KEY_FLASH, flash);
			mIVFlash.setVisibility(View.VISIBLE);
			mFlashOptionsLayout.setVisibility(View.INVISIBLE);
		}
	};

	public Handler captureHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
			switch (whatMessage) {
			case MESSAGE_FADEOUT_INFO_POPUP:
				//mInfoNotiBubbleLayout.startAnimation(mAnimFadeOut);
				break;
			case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
				//Navigate back to settings
				if(msg.arg1 == RESULT_OK){
					mUtilityRoutines.offlineLogout(Capture.this);
				}else{
					Globals.gAppModeStatus = Globals.AppModeStatus.FORCE_OFFLINEMODE;
				}
				break;
			 case MESSAGE_DOWNLOAD_DOCUMENTS_FAILED:
				 mCustomDialog.closeProgressDialog();
				 Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_error_field_details_cannot_be_downloaded), Toast.LENGTH_SHORT).show();
				 if(mIsDownloadStart){
					 misSendForProcess = true;
					 mProcessQueueMgr.pauseQueue(captureHandler);
					 mDocMgrObj.removeCurrenthandler();
				 }
				 break;
			default:
				break;

			}

			return false;
		}
	});

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mCaptureExperience != null) {
			mCaptureExperience.stopCapture();
			removeCaptureExperience();
		}
		//mAnimatedDocumentCaptureExperience.stopCapture();
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}

		if (mCurrentAnimator != null) {
			mCurrentAnimator.cancel();
			mCurrentAnimator = null;
		}

		deinitPreview();

		icf = null;

		if (capturedImage != null) {
			capturedImage = null;
		}

		if(mBmpThumbnail != null) {
			mBmpThumbnail.recycle();
			mBmpThumbnail = null;
		}

		if(mBmpAnimatePreview != null) {
			mBmpAnimatePreview.recycle();
			mBmpAnimatePreview = null;
		}

		if(mPreviewThumbnail != null){
			mPreviewThumbnail.setImageBitmap(null);
		}

		if(mIVFlash != null){
			mIVFlash.setImageBitmap(null);
		}

		if(mPreviewOverlay != null){
			mPreviewOverlay.setImageBitmap(null);
		}

		if(mCaptureTypeText != null) {
			mCaptureTypeText.setOnItemSelectedListener(null);
			mCaptureTypeTextSelectedListener = null;
		}

		if(mPhotoCaptureButton != null) {
			mPhotoCaptureButton.setOnTouchListener(null);
			mCaptureButtonOnTouchListener = null;
		}

		//mCaptureExperienceCriteriaHolder = null;
		mZoomInMessage = null;
		mZoomOutMessage = null;
		mUserInstructionMessage = null;
		mCenterMessage = null;
		mCapturedMessage = null;

		View mainparent = findViewById(R.id.mainparent_layout);
		((RelativeLayout) mainparent).removeAllViews();
		mainparent = null;

		mDiskMgrObj.deleteTempLocation();

		mDiskMgrObj = null;
		mDocMgrObj = null;
		mItemDBManager = null;
		mPrefUtilsObj = null;
		mProcessQueueMgr = null;
		mCustomDialog = null;
		try {
			System.gc();
		} catch (Throwable e) {
			Log.i(TAG, "onDestroy, Exception occured: " + e.getMessage());
			e.printStackTrace();
		}
		
		stopManualCaptureTimer();
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	/**
	 *
	 */
	private synchronized void removeCaptureExperience(){
		if (null != mDocumentCaptureExperience) {
			mDocumentCaptureExperience.removeOnImageCapturedListener(this);
			mDocumentCaptureExperience.destroy();
			mDocumentCaptureExperience = null;
		}
	}

	private void showQuickPreview(RequestCode captureSource, String imgLocation, long pageId) {

		Intent i = new Intent(this, QuickPreviewActivity.class);
		i.putExtra(Constants.STR_URL, imgLocation);
		i.putExtra(Constants.STR_PAGE_ID, pageId);
		/*
		 * quick_preview is true only when image is just captured and not
		 * accepted yet. It is false when image is selected by tapping on
		 * thumbnail on item detail screen
		 */
		i.putExtra(Constants.STR_QUICK_PREVIEW, true);
		if(Constants.IS_HELPKOFAX_FLOW){
			i.putExtra(Constants.STR_IMG_SOURCE_TYPE, RequestCode.CAPTURE_PHOTO.ordinal());
		}else{		
		i.putExtra(Constants.STR_IMG_SOURCE_TYPE, captureSource.ordinal());
		}
		
		if(mIsOfflineAlertRequest){
			i.putExtra(Constants.STR_OFFLINE_TO_LOGIN, true);
		}

		startActivityForResult(i,
				RequestCode.PREVIEW_IMAGE.ordinal());
	}

	private void createAndSaveDocument(){
		if(mTotalImgCount <= 0) {
			return;
		}
		DocumentType doctype = mDocMgrObj.getDocTypeFromRefArray(mDocMgrObj.getCurrentDocTypeIndex());
		if ((mIsNewItem) || (!mItemDBManager.isDocumentSerializedInDB(mItemDBManager.getItemEntity()))) {
			if (doctype != null) {
				Document doc = new Document(doctype);

				if(!mItemDBManager.isDocumentSerializedInDB(mItemDBManager.getItemEntity())) {
					//if (!result) {
					byte[] data = mDiskMgrObj.documentToByteArray(doc);
					mItemDBManager.getItemEntity().setItemSerializedData(data);
					ProcessingParametersEntity ppEntity = mItemDBManager.getProcessingParametersEntity();
					if(null != ppEntity){
						ppEntity.setSerializeDocument(data);
						mItemDBManager.updateProcessingEntity(this, ppEntity);
					}
					//save serialized object in DB
					mItemDBManager.update(getApplicationContext(), mItemDBManager.getItemEntity());
					//mItemDBManager.insertOrUpdate(getApplicationContext(), mItemDBManager.getItemEntity());
				}

				List<FieldType> currentFieldTypesList = doctype.getFieldTypes();
				for (int i = 0; i < currentFieldTypesList.size(); i++) {
					if(!currentFieldTypesList.get(i).isHidden() && doc.getFields().get(i).getValue() != null){
						if(doc.getFields().get(i).getValue().length() == 0){
							mItemDBManager.getItemEntity().setFieldName(Constants.STR_EMPTY_FIELD);
						}else{
							if(mCustomUrlUtils.isUsingCustomUrl() &&
									(mCustomUrlUtils.getDocumentFieldDefaults() != null && mCustomUrlUtils.getDocumentFieldDefaults().size() > 0)){
								copyCustomUrlFieldValuesToDocument(doc);
								byte[] data = mDiskMgrObj.documentToByteArray(doc);
								mItemDBManager.getItemEntity().setItemSerializedData(data);
							}
							mItemDBManager.getItemEntity().setFieldName(doc.getDocumentType().getFieldTypes().get(i).getDisplayName() + " : " + doc.getFields().get(i).getValue());

						}
						break;
					}
				}
				doc = null;

				int visibleFieldCount = 0;

				// check if document object have any displayable fields. If yes, display edit-fields screen. If no, skip edit-fields screen and display submit screen directly.
				for (int i = 0; i < currentFieldTypesList.size(); i++) {
					if (!currentFieldTypesList.get(i).isHidden() && !currentFieldTypesList.get(i).getName().equalsIgnoreCase(Constants.STR_MOBILE_DEVICE_EMAIL)) {
						visibleFieldCount++;
					}
				}
				if (visibleFieldCount <= 0) {
					mItemDBManager.getItemEntity().setFieldName(Constants.STR_NO_FIELDS);
				}
				mItemDBManager.update(getApplicationContext(), mItemDBManager.getItemEntity());
				//mItemDBManager.insertOrUpdate(getApplicationContext(), mItemDBManager.getItemEntity());
			}
		}
	}

	/**
	 * Function to copy all the valid field values which are present in custom url into Document object.
	 */
	private void copyCustomUrlFieldValuesToDocument(Document doc) {
		boolean isInvalidField = true;
		Map<String, String> fieldMap = mCustomUrlUtils.getDocumentFieldDefaults();

		DocumentType doctype = mDocMgrObj.getDocTypeFromRefArray(mDocMgrObj.getCurrentDocTypeIndex());
		if ((mIsNewItem) || (!mItemDBManager.isDocumentSerializedInDB(mItemDBManager.getItemEntity()))) {
			if (doctype != null) {
				List<FieldType> fieldTypes = doc.getDocumentType().getFieldTypes();
				int length = fieldTypes.size();
				for (Map.Entry<String, String> mapEntry : fieldMap.entrySet()) {
					for(int i=0; i<length; i++) {
						if(fieldTypes.get(i).getName().equals(mapEntry.getKey())) {
							doc.getFields().get(i).updateFieldProperties(mapEntry.getValue(), true, "");
						}
					}
				}
				if(isInvalidField == true) {
					mCustomUrlUtils.setInvalidFieldPresent(isInvalidField);
				}
			}
		}

	}

	@TargetApi(23)
	private void checkPermissionAndLaunchGallery(int option){
		selectedMode = option;
		int hasSDCardPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (hasSDCardPermission != PackageManager.PERMISSION_GRANTED) {
			if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				showMessageOKCancel(getString(R.string.gallery_permission_alert),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
//								if (Build.VERSION.SDK_INT >= 23) {
									requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
											REQUEST_CODE_ASK_PERMISSIONS);
//								}
							}
						},new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
//									mIsGalleryFlow = false;
								mImgCapView.setVisibility(View.VISIBLE);
								mImgCapView.invalidate();
								mGalleryButton.setEnabled(true);
								if (mImageMode == ImageMode.DOCUMENT_MODE) {
									setDocumentMode();
								} else {
									setPhotoMode();
								}
							}
						});
				return;
			}
			requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
					REQUEST_CODE_ASK_PERMISSIONS);
			return;
		}else{
			launchGallery(option);
		}
	}

	private void openImageGallery(int option) {

		if (Build.VERSION.SDK_INT >= 23) {
			checkPermissionAndLaunchGallery(option);
		} else {
			launchGallery(option);
		}

	}

	private void launchGallery(int option){
		Intent galleryIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(galleryIntent, option);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode){
			case REQUEST_CODE_ASK_PERMISSIONS:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// Permission Granted
					launchGallery(selectedMode);
				} else {
					// Permission Denied
					Toast.makeText(Capture.this, "Gallery Pemission Denied", Toast.LENGTH_SHORT)
							.show();
					// show imageCaptureView back when gallery is closed.
					mIsGalleryFlow = false;
					mImgCapView.setVisibility(View.VISIBLE);
					mImgCapView.invalidate();
					if (mImageMode == ImageMode.DOCUMENT_MODE) {
						setDocumentMode();
					} else {
						setPhotoMode();
					}

				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}

	}

	private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener,DialogInterface.OnClickListener cancelListener) {
		new AlertDialog.Builder(new ContextThemeWrapper(Capture.this, android.R.style.Theme_Holo_Light_Dialog))
				.setMessage(message)
				.setPositiveButton("OK", okListener)
				.setNegativeButton("Cancel", cancelListener)
				.create()
				.show();
	}

	private OnItemSelectedListener mCaptureTypeTextSelectedListener= new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			if (position == 0) {
				if(!misFirstTimeVisible){					
				setDocumentMode();
				}
				misFirstTimeVisible = false; 
			} else {
				setPhotoMode();
			}
			adapter.refreshView(position);
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {

		}
	};

	private OnTouchListener mCaptureButtonOnTouchListener = new OnTouchListener(){
		public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction())
			{
			case MotionEvent.ACTION_DOWN :
				mPhotoCaptureButton.setImageResource(R.drawable.capture_button_active);
				break;
			case MotionEvent.ACTION_UP :
				mPhotoCaptureButton.setImageResource(R.drawable.capture_button_enabled);
				break;
			}
			return false;
		}
	};
	
	

	private void setPhotoMode(){
		mImageMode = ImageMode.PHOTO_MODE;

		disableAutoCaptureExperience(false);
		
		//update gallery-launch image to indicate photo-selection
		mGalleryButton.setBackgroundResource(R.drawable.photo_gallery);
	}

	private void setDocumentMode(){
		mImageMode = ImageMode.DOCUMENT_MODE;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (icf != null && mImgCapView != null) {
					mImgCapView.setImageCaptureFrame(icf);
				}

				if (mDocumentCaptureExperience != null) {
					mDocumentCaptureExperience.setZoomInMessage(mZoomInMessage);
					mDocumentCaptureExperience.setUserInstructionMessage(mUserInstructionMessage);
					mDocumentCaptureExperience.setHoldSteadyMessage(mHoldSteadyMessage);
					mDocumentCaptureExperience.setCenterMessage(mCenterMessage);
					mDocumentCaptureExperience.setZoomOutMessage(mZoomOutMessage);
					mDocumentCaptureExperience.setCapturedMessage(mCapturedMessage);
					mDocumentCaptureExperience.setGuidanceFrameColor(Color.GREEN);
					mDocumentCaptureExperience.addOnImageCapturedListener(Capture.this);
					mDocumentCaptureExperience.setOuterViewFinderColor(Color.parseColor("#80000000"));
					if (mPrefUtilsObj.sharedPref.getBoolean(mPrefUtilsObj.KEY_QUICK_PREVIEW, true)) {
						mDocumentCaptureExperience.takePicture();
						isAutoCaptureStopped = false;
					} else {
						if (isAutoCaptureStopped) {
							mDocumentCaptureExperience.takePictureContinually();
							isAutoCaptureStopped = false;
						}
					}
				} else {
					setUniformCaptureExperience();
				}

				removeCallbacks();
				setCallbacks();

				setCaptureParameters();
				startManualCaptureTimer();

				mPhotoCaptureButton.setEnabled(false);
				mPhotoCaptureButton.setImageResource(R.drawable.capture_button_disabled);
				// update gallery-launch image to indicate document-selection
				mGalleryButton.setBackgroundResource(R.drawable.document_gallery);

			}
		});
	}
	

	/*** Method to Disable the auto capture experience
	 * 
	 * @param needStaticFrame: To Enable/disable the static frame
	 */
	private void disableAutoCaptureExperience(boolean needStaticFrame) {
		//set capture properties
		mImgCapView.setPageDetectMode(PageDetectMode.OFF);
		mImgCapView.setImageCaptureFrame(null);		

		removeCallbacks();
		setCallbacks();

		hideUniformCaptureExperienceMessages();

		mPhotoCaptureButton.setVisibility(View.VISIBLE);
		mPhotoCaptureButton.setEnabled(true);
		mPhotoCaptureButton.setImageResource(R.drawable.capture_button_enabled);
	}
	
	/**
	 * Method to start the manual capture timer.
	 * */
	private void startManualCaptureTimer(){
		if (mManualCaptureRunnable == null) {
			mManualCaptureRunnable = new Runnable() {
				@Override
				public void run() {
					disableAutoCaptureExperience(true);
					if(mImageMode==ImageMode.DOCUMENT_MODE ) {
						mImgCapView.addOnImageCapturedListener(Capture.this);
					}
				}
			};
		}

		if (mManualCaptureHandler == null) {
			mManualCaptureHandler = new Handler();
		}

		mManualCaptureHandler.postDelayed(mManualCaptureRunnable, mPrefUtilsObj
				.getPrefValueInt(mPrefUtilsObj.KEY_MANUAL_CAPTURE,
						mPrefUtilsObj.DEF_MANUAL_TIME) * 1000);
	}
	
	/**
	 * Method to stop the manual capture timer.
	 * */
	private void stopManualCaptureTimer(){
		if (mManualCaptureHandler != null
				&& mManualCaptureRunnable != null) {
			mManualCaptureHandler.removeCallbacks(mManualCaptureRunnable);
		}
	}

	private void sendBroadcast(){
		Intent i = new Intent(Constants.CUSTOM_INTENT_IMAGE_CAPTURED);
		sendBroadcast(i);
	}

	private void initPreview() {
		if (true == mIsCaptureCalled) {
			return;
		}
		setCaptureParameters();
	}

	private void setUniformCaptureExperience() {
		setDocumentCaptureExperience();
		setCaptureInstructionMessages();

		if (mPrefUtilsObj.sharedPref.getBoolean(
				mPrefUtilsObj.KEY_QUICK_PREVIEW, true)) {
			mCaptureExperience.takePicture();
		} else {
			mCaptureExperience.takePictureContinually();
		}
		if (mPrefUtilsObj.getPrefValueInt(mPrefUtilsObj.KEY_MANUAL_CAPTURE,
				mPrefUtilsObj.DEF_MANUAL_TIME) <= 0) {
			mCaptureExperience.stopCapture();
		}
	}

	private void setDocumentCaptureExperience() {
		DocumentCaptureExperienceCriteriaHolder criteria = initDocumentCaptureExperienceCriteria();
		if (mDocumentCaptureExperience == null) {
			mDocumentCaptureExperience = new DocumentCaptureExperience(mImgCapView);
		}
		mDocumentCaptureExperience.setCaptureCriteria(criteria);
		mDocumentCaptureExperience.addOnImageCapturedListener(this);
		mCaptureExperience = mDocumentCaptureExperience;
	}

	private DocumentCaptureExperienceCriteriaHolder initDocumentCaptureExperienceCriteria() {

		DocumentCaptureExperienceCriteriaHolder criteria = new DocumentCaptureExperienceCriteriaHolder();
		criteria.setDetectionSettings(createDocumentDetectionSettings());
		criteria.setStabilityThresholdEnabled(true);
		criteria.setStabilityThreshold(Constants.CAM_STABILITY_VALUE);
		criteria.setPitchThresholdEnabled(true);
		criteria.setPitchThreshold(mSavePitchThreshold);
		criteria.setRollThresholdEnabled(true);
		criteria.setRollThreshold(mSaveRollThreshold);
		criteria.setFocusEnabled(true);
		return criteria;
	}

	private DocumentDetectionSettings createDocumentDetectionSettings() {

		DocumentDetectionSettings settings = new DocumentDetectionSettings();
		settings.setShortEdgeThreshold(mOffSetThreshold / 100.0);
		settings.setLongEdgeThreshold(mOffSetThreshold / 100.0);
		if (mIsGiftCard) {
			settings.setTargetFrameAspectRatio((double) (2.125f / 3.375));
		}

		if(!mIsGiftCard && mPrefUtilsObj.isUsingKofax()) {
			if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_ID")) {
				settings.setTargetFrameAspectRatio((double) (2.125f / 3.375));
			} else if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_Passport")){
				settings.setTargetFrameAspectRatio((double) (2.125f / 3.375));
			} else if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_Bill")){
				settings.setTargetFrameAspectRatio((double) (2.125f / 3.375));
			}else if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_Check")){
				settings.setTargetFrameAspectRatio((double) (2.125f / 3.375));
			}else{

			}
		}
		return settings;
	}

	private void setCaptureInstructionMessages() {
		if (null != mDocumentCaptureExperience) {
			if (mUserInstructionMessage == null) {
				mUserInstructionMessage = mDocumentCaptureExperience.getUserInstructionMessage();
			}
			setUserInstructionMessage();

			mUserInstructionMessage.setTextSize(24);
			mDocumentCaptureExperience.setUserInstructionMessage(mUserInstructionMessage);

			if (mZoomInMessage == null) {
				mZoomInMessage = mDocumentCaptureExperience.getZoomInMessage();
			}
			mDocumentCaptureExperience.setZoomInMessage(mZoomInMessage);

			if (mHoldSteadyMessage == null) {
				mHoldSteadyMessage = mDocumentCaptureExperience.getHoldSteadyMessage();
			}
			mDocumentCaptureExperience.setHoldSteadyMessage(mHoldSteadyMessage);

			if (mCenterMessage == null) {
				mCenterMessage = mDocumentCaptureExperience.getCenterMessage();
			}
			mDocumentCaptureExperience.setCenterMessage(mCenterMessage);

			if (mCapturedMessage == null) {
				mCapturedMessage = mDocumentCaptureExperience.getCapturedMessage();

			}
			mDocumentCaptureExperience.setCapturedMessage(mCapturedMessage);

			if (mZoomOutMessage == null) {
				mZoomOutMessage = mDocumentCaptureExperience.getZoomOutMessage();
			}
			mDocumentCaptureExperience.setZoomOutMessage(mZoomOutMessage);

			int manualCaptureTimer = mPrefUtilsObj.getPrefValueInt(
					mPrefUtilsObj.KEY_MANUAL_CAPTURE,mPrefUtilsObj.DEF_MANUAL_TIME);

			mUserInstructionMessage.setVisibility(manualCaptureTimer != 0);
			mZoomInMessage.setVisibility(manualCaptureTimer != 0);
			mHoldSteadyMessage.setVisibility(manualCaptureTimer != 0);
			mCenterMessage.setVisibility(manualCaptureTimer != 0);
			mCapturedMessage.setVisibility(manualCaptureTimer != 0);
			mZoomOutMessage.setVisibility(manualCaptureTimer != 0);

		}
	}

	/**
	 * Method to hide animated capture experience messages
	 * */
	private void hideUniformCaptureExperienceMessages(){
		CaptureMessage captureMessage = new CaptureMessage();
		captureMessage.setVisibility(false);

		if(mDocumentCaptureExperience != null){
			mDocumentCaptureExperience.setZoomInMessage(captureMessage);
			mDocumentCaptureExperience.setUserInstructionMessage(captureMessage);
			mDocumentCaptureExperience.setHoldSteadyMessage(captureMessage);
			mDocumentCaptureExperience.setCenterMessage(captureMessage);
			mDocumentCaptureExperience.setZoomOutMessage(captureMessage);
			mDocumentCaptureExperience.setCapturedMessage(captureMessage);
			if(mImageMode==ImageMode.PHOTO_MODE) {
				mDocumentCaptureExperience.setGuidanceFrameColor(Color.TRANSPARENT);
				mDocumentCaptureExperience.setOuterViewFinderColor(Color.TRANSPARENT);
			}
			mCaptureExperience.stopCapture();
			isAutoCaptureStopped = true;
		}
	}

	private void setUserInstructionMessage(){
		
		if(!mIsGiftCard && mPrefUtilsObj.isUsingKofax()){
		if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_ID")) {
			 mUserInstructionMessage.setMessage(getString(R.string.userdlInstruction));
		}else if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_Passport")){
			mUserInstructionMessage.setMessage(getString(R.string.userppInstruction));
		}else if(mItemDBManager.getItemEntity().getItemTypeName().equalsIgnoreCase("AssistKofax_Bill")){
			mUserInstructionMessage.setMessage(getString(R.string.userbpInstruction));
		}else if(mItemDBManager.getItemEntity().getItemTypeName().contains("AssistKofax_Check")){
			mUserInstructionMessage.setMessage(getString(R.string.usercdInstruction));
		}else{
			mUserInstructionMessage.setMessage(mDocumentCaptureExperience.getUserInstructionMessage().getMessage().toString());
		}
		}else if(mIsGiftCard && mPrefUtilsObj.isUsingKofax()){
			mUserInstructionMessage.setMessage(getString(R.string.usergiftcardInstruction));
		}
		else{
			mUserInstructionMessage.setMessage(mDocumentCaptureExperience.getUserInstructionMessage().getMessage().toString());
		}		
		
	}

	private void deinitPreview() {
		if (null != mImgCapView) {
			mImgCapView.removeCameraInitializationFailedListener(this);
			mImgCapView.removeCameraInitializationListener(this);
			mImgCapView.setPageDetectMode(PageDetectMode.OFF);
			mImgCapView.setImageCaptureFrame(null);

			removeCallbacks();

			mImgCapView.removeAllViews();
			// remove the view from the parent and add it runtime to resolve
			// issue in background
			View parent = findViewById(R.id.parent_layout);
			((FrameLayout) parent).removeAllViews();
			mImgCapView = null;
			// System.gc();
		}
	}

	private void setCallbacks() {
		
		if(mImageMode == ImageMode.DOCUMENT_MODE){
			mImgCapView.addStabilityDelayListener(this);
			mImgCapView.addOnAutoFocusResultListener(this);
			mImgCapView.addPageDetectionListener(this);
			mImgCapView.addLevelnessListener(this);
		}else{
			mImgCapView.addOnImageCapturedListener(Capture.this);
		}
	}

	private void removeCallbacks() {
		mImgCapView.removeOnImageCapturedListener(Capture.this);
		mImgCapView.removeLevelnessListener(this);
		mImgCapView.removePageDetectionListener(this);
		mImgCapView.removeStabilityDelayListener(this);
		mImgCapView.removeOnAutoFocusResultListener(this);
	}

	private void setCaptureParameters() {
		// Save the SDK defaults for possible reset after manual photo
		mSavePitchThreshold = mImgCapView.getLevelThresholdPitch();
		mSaveRollThreshold = mImgCapView.getLevelThresholdRoll();

		mImgCapView.setLevelThresholdPitch(mSavePitchThreshold);
		mImgCapView.setLevelThresholdRoll(mSaveRollThreshold);
		mImgCapView.setDeviceDeclinationPitch(0);
		mImgCapView.setDeviceDeclinationRoll(0);
		mImgCapView.setPageDetectMode(PageDetectMode.CONTINUOUS);
	}

	private void init_flash() {
		if(mIsGalleryFlow == true){
			mImgCapView.setFlash(Flash.OFF);
			return;
		}
		if(mIVFlash == null) {
			mIVFlash = (ImageView) findViewById(R.id.imgFlashSelected);
			mFlashOptionsLayout = findViewById(R.id.flashOptionsLayout);

			mIVFlash.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mFlashOptionsLayout.setVisibility(View.VISIBLE);
					mIVFlash.setVisibility(View.INVISIBLE);
				}
			});

			mImgFlashAuto = findViewById(R.id.imgFlashAuto);
			mImgFlashOn = findViewById(R.id.imgFlashOn);
			mImgFlashOff = findViewById(R.id.imgFlashOff);

			mImgFlashAuto.setOnClickListener(flashOptionClickListener);
			mImgFlashOn.setOnClickListener(flashOptionClickListener);
			mImgFlashOff.setOnClickListener(flashOptionClickListener);
		}
		int current_flash = mPrefUtilsObj.getPrefValueInt(mPrefUtilsObj.KEY_FLASH,
				Flash.OFF.ordinal());

		if (current_flash == Flash.OFF.ordinal()) {
			mIVFlash.setImageResource(R.drawable.ic_flash_off);
			mImgCapView.setFlash(Flash.OFF);
		} else if (current_flash == Flash.ON.ordinal()) {
			mIVFlash.setImageResource(R.drawable.ic_flash_on);
			mImgCapView.setFlash(Flash.ON);
		} else if (current_flash == Flash.AUTO.ordinal()) {
			mIVFlash.setImageResource(R.drawable.ic_flash_automatic);
			mImgCapView.setFlash(Flash.AUTO);
		}
	}

	private void updateActivity() {
		mPreviewThumbnail.setVisibility(View.VISIBLE);
		mTVCount.setVisibility(View.VISIBLE);
		mTVCount.setText(Integer.toString(mTotalImgCount));
		// if captured images are more than 1, display 'Done' option on screen.
		if(mTotalImgCount > 0) {
			mTVDone.setVisibility(View.VISIBLE);
			mImageCountLayout.setVisibility(View.VISIBLE);
		}
	}

	private void displayImageThumbnailPreview(String filePath) {
		Bitmap bmp = null;
		if(filePath != null) {
			File fp = new File(filePath);
			if(fp.exists()) {
				fp = null;
				if(mBmpThumbnail != null) {
					mBmpThumbnail.recycle();
					mBmpThumbnail = null;
				}
				bmp = mUtilityRoutines.getScaledBitmapFromFilepath(filePath);
				if(bmp != null) {
					mBmpThumbnail = Bitmap.createScaledBitmap(bmp, mPreviewThumbnail.getWidth(), mPreviewThumbnail.getHeight(), false);
					mPreviewThumbnail.setImageBitmap(mBmpThumbnail);
				}
				else {
					Log.e(TAG, "Bitmap is NULL!!!!!!!!!!!!");
				}
			}
			else {
				Log.e(TAG, "File DOES NOT exists !!!");
			}
		}
	}

	private void loadCapturedImagePreviewToAnimate(String filePath) {
		Bitmap bmp = BitmapFactory.decodeFile(filePath);

		if(mBmpAnimatePreview != null) {
			mBmpAnimatePreview.recycle();
			mBmpAnimatePreview = null;
		}
		mBmpAnimatePreview = Bitmap.createScaledBitmap(bmp, mPreviewOverlay.getWidth(), mPreviewOverlay.getHeight(), false);
		mPreviewOverlay.setImageBitmap(mBmpAnimatePreview);
		bmp.recycle();
		bmp = null;
	}

	private void updateScreenOnImageCapture() {
		mTotalImgCount++;
		mCurrentCaptureCount++;

		//DO NOT animate if preview is ON or images are being selected from image-gallery.
		if (mPrefUtilsObj.sharedPref.getBoolean(mPrefUtilsObj.KEY_QUICK_PREVIEW, true) || 
				mIsGalleryFlow == true) {
			//display captured image thumbnail at the bottombar
			//mPreviewThumbnail.setImageURI(Uri.parse(capturedImage.getImageFilePath()));
			displayImageThumbnailPreview(capturedImage.getImageFilePath());
		} else {
			//Load captured image on full screen mIVPreview to make it animate
			//mPreviewOverlay.setImageURI(Uri.parse(capturedImage.getImageFilePath()));
			loadCapturedImagePreviewToAnimate(capturedImage.getImageFilePath());
			mPreviewOverlay.setVisibility(View.VISIBLE);
			animateImageview();
		}
		if(Constants.BACKGROUND_IMAGE_PROCESSING) {
			sendBroadcast();
		}
		// update capture activity
		updateActivity();
	}

	private long acceptImage(int imageType, Image img, String galleryFilePath) {
		Log.i(TAG, "galleryFilePath =====================> " + galleryFilePath);
		String imageLocation = null;
		long pageId = -1;
		try {
			pageId = mDiskMgrObj.saveImageToDisk(img, null, mDocMgrObj.getOpenedDoctName(), imageType);
			if(pageId != -1) {
				imageLocation = mItemDBManager.getPageForId(Capture.this, pageId).getImageFilePath();
				String[] geoLocation = null;
				//pass galleryFilePath if image is selected from gallery
				if(mIsGalleryFlow) {
					geoLocation = mUtilityRoutines.saveImageGeoLocation(imageLocation, galleryFilePath);
				}
				else { //else send the disk path where captured image is recently stored
					geoLocation = mUtilityRoutines.saveImageGeoLocation(imageLocation, imageLocation);
				}
				if(geoLocation != null) {
					String[] gpsCoordinates = mUtilityRoutines.getGPSCoordinatesFromGeoLocation(geoLocation);
					if(gpsCoordinates != null) {
						Log.i(TAG, "gpsCoordinates[0] returned =====================> " + gpsCoordinates[0]);
						Log.i(TAG, "gpsCoordinates[1] returned =====================> " + gpsCoordinates[1]);
						mItemDBManager.getPageForId(Capture.this, pageId).setImageLatitude(gpsCoordinates[0]);
						mItemDBManager.getPageForId(Capture.this, pageId).setImageLongitude(gpsCoordinates[1]);
						mItemDBManager.insertOrUpdatePage(this, mItemDBManager.getPageForId(Capture.this, pageId));
					}
				}
			}
			if(pageId != -1 && imageLocation != null) {
				mDocMgrObj.getImgUrlList().add(imageLocation);
				mDiskMgrObj.updateItemImgCountInDetailsList(mDocMgrObj.getOpenedDoctName(), mDocMgrObj.getImgUrlList().size());
				mDocMgrObj.getImgUrlList().add(imageLocation);
				mDiskMgrObj.updateItemImgCountInDetailsList(mDocMgrObj.getOpenedDoctName(), mDocMgrObj.getImgUrlList().size());
				//result = ResultState.RESULT_OK;
			}
			else {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_save_image_failed), Toast.LENGTH_LONG).show();
			}
		} catch (KmcRuntimeException e) {
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (KmcException e) {
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		return pageId;
	}


	@SuppressLint({ "InlinedApi", "NewApi" })
	private void animateImageview() {
		if (mCurrentAnimator != null) {
			mCurrentAnimator.cancel();
		}

		// Calculate the starting and ending bounds for the zoomed-in image.
		// This step
		// involves lots of math. Yay, math.
		final Rect startBounds = new Rect();
		final Rect finalBounds = new Rect();
		Point globalOffset = new Point();
		// Set the pivot point for SCALE_X and SCALE_Y transformations to the
		// top-left corner of
		// the zoomed-in view (the default is the center of the view).
		mPreviewOverlay.setPivotX(0f);
		mPreviewOverlay.setPivotY(0f);
		// The start bounds are the global visible rectangle of the thumbnail,
		// and the
		// final bounds are the global visible rectangle of the container view.
		// Also
		// set the container view's offset as the origin for the bounds, since
		// that's
		// the origin for the positioning animation properties (X, Y).
		mPreviewThumbnail.getGlobalVisibleRect(startBounds);
		findViewById(R.id.mainparent_layout).getGlobalVisibleRect(finalBounds,
				globalOffset);
		startBounds.offset(-globalOffset.x, -globalOffset.y);
		finalBounds.offset(-globalOffset.x, -globalOffset.y);

		// Adjust the start bounds to be the same aspect ratio as the final
		// bounds using the
		// "center crop" technique. This prevents undesirable stretching during
		// the animation.
		// Also calculate the start scaling factor (the end scaling factor is
		// always 1.0).
		final float startScale;
		if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds
				.width() / startBounds.height()) {
			// Extend start bounds horizontally
			startScale = (float) startBounds.height() / finalBounds.height();
			float startWidth = startScale * finalBounds.width();
			float deltaWidth = (startWidth - startBounds.width()) / 2;
			startBounds.left -= deltaWidth;
			startBounds.right += deltaWidth;
		} else {
			// Extend start bounds vertically
			startScale = (float) startBounds.width() / finalBounds.width();
			float startHeight = startScale * finalBounds.height();
			float deltaHeight = (startHeight - startBounds.height()) / 2;
			startBounds.top -= deltaHeight;
			startBounds.bottom += deltaHeight;
		}

		// Upon clicking the zoomed-in image, it should zoom back down to the
		// original bounds
		// and show the thumbnail instead of the expanded image.
		float startScaleFinal = startScale;

		// Animate the four positioning/sizing properties in parallel, back to
		// their
		// original values.
		final AnimatorSet set = new AnimatorSet();
		set.play(ObjectAnimator.ofFloat(mPreviewOverlay, View.X, startBounds.left))
		.with(ObjectAnimator.ofFloat(mPreviewOverlay, View.Y,
				startBounds.top + 100))
				.with(ObjectAnimator.ofFloat(mPreviewOverlay, View.SCALE_X,
						startScaleFinal))
						.with(ObjectAnimator.ofFloat(mPreviewOverlay, View.SCALE_Y,
								startScaleFinal));
		/*
		 * set.play(ObjectAnimator.ofFloat(preview_view, View.X,
		 * startBounds.left)) .with(ObjectAnimator.ofFloat(preview_view, View.Y,
		 * startBounds.top + 50)) .with(ObjectAnimator.ofFloat(preview_view,
		 * View.SCALE_X, startScaleFinal));
		 */

		set.setDuration(mShortAnimationDuration);
		set.setInterpolator(new DecelerateInterpolator());
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mPreviewOverlay.setVisibility(View.INVISIBLE);
				displayImageThumbnailPreview(capturedImage.getImageFilePath());
				//mPreviewThumbnail.setImageURI(Uri.parse(capturedImage.getImageFilePath()));

				// Construct and run the parallel animation of the four
				// translation and scale properties
				// (X, Y, SCALE_X, and SCALE_Y).
				Display display = getWindowManager().getDefaultDisplay();
				final Point size = new Point();
				display.getSize(size);
				int height = size.y;
				mPreviewOverlay.setY(height
						- mPreviewOverlay.getDrawable().getIntrinsicHeight());

				AnimatorSet set = new AnimatorSet();
				set.play(
						ObjectAnimator.ofFloat(mPreviewOverlay, View.X,
								startBounds.left, finalBounds.left))
								.with(ObjectAnimator.ofFloat(mPreviewOverlay, View.Y,
										startBounds.top, finalBounds.top))
										.with(ObjectAnimator.ofFloat(mPreviewOverlay,
												View.SCALE_X, startScale, 1f))
												.with(ObjectAnimator.ofFloat(mPreviewOverlay,
														View.SCALE_Y, startScale, 1f));
				set.setDuration(mShortAnimationDuration);
				set.setInterpolator(new DecelerateInterpolator());
				set.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mCurrentAnimator = null;
						// doc_view.setImageURI(Uri.parse(capturedImg.getImageFilePath()));
					}

					@Override
					public void onAnimationCancel(Animator animation) {
						mCurrentAnimator = null;
					}

				});
				set.start();
				mCurrentAnimator = set;
				// initPreview();
				mIsCaptureCalled = false;
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				mCurrentAnimator = null;
			}
		});
		set.start();
		mCurrentAnimator = set;
	}

	private void registerBroadcastReceiver() {

		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i(TAG,
							"Broadcast received!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					if (intent.getAction() == Constants.CUSTOM_INTENT_IMAGE_PROCESSED) {
						mCustomDialog.closeProgressDialog();
						Log.i(TAG,
								"Broadcast received CUSTOM_INTENT_IMAGE_PROCESSED");
						if(!Constants.BACKGROUND_IMAGE_PROCESSING) {
							setCallbacks();
						}
					}
				}
			};
		}
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_IMAGE_PROCESSED);
		registerReceiver(mReceiver, intentFilter);
	}

	private void clearImage(Image img) {
		if (img != null) {
//			img.imageClearBitmap();
			try {
				img.imageClearFileBuffer();
			} catch (KmcException e) {
				e.printStackTrace();
			}
		img = null;
		}
	}

	private class GalleryAdapter extends BaseAdapter {
		Context mContext;
		int selectedPos = -1;

		String[] galleryValues = null;

		public GalleryAdapter(Context context) {
			mContext = context;
			galleryValues = mContext.getResources().getStringArray(R.array.capturetype_arrays);
		}

		@Override
		public int getCount() {
			return galleryValues.length;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflator = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflator.inflate(R.layout.camera_type_layout, null);
			}

			TextView captureItem = (TextView)v.findViewById(R.id.gallery_item_title);
			if(!Constants.IS_HELPKOFAX_FLOW){
			captureItem.setText(galleryValues[position]);
			}
			if(selectedPos == position){
				captureItem.setTextColor(Color.parseColor("#0079C2"));
			}else{
				captureItem.setTextColor(Color.parseColor("#ffffff"));
			}

			return v;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		void refreshView(int pos){
			selectedPos = pos;
			notifyDataSetChanged();
		}

	}

	@Override
	public void onNetworkChanged(boolean isConnected) {
		if(Globals.isRequiredOfflineAlert() && isConnected){
				mIsOfflineAlertRequest = true;
				if(mCustomDialog != null){
					mCustomDialog.dismissAlertDialog();
					mCustomDialog.show_popup_dialog(Capture.this,AlertType.CONFIRM_ALERT,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
							getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION, captureHandler, false);	
				}	
		}else{
			mIsOfflineAlertRequest = false;
			//mCustomDialog.dismissAlertDialog();			
		}
		
	}

	@Override
	public void onCameraInitializationFailed(
			CameraInitializationFailedEvent arg0) {
		Toast.makeText(getApplicationContext(), arg0.getCause().getMessage(),
				Toast.LENGTH_LONG).show();
		onBackPressed();
	}

	@Override
	public void onCameraInitialized(CameraInitializationEvent arg0) {
		if (arg0.getCameraInitStatus() == CameraInitializationEvent.CameraInitStatus.CAMERA_VIEW_CREATED) {
			if (mImgCapView == null) {
				return;
			}
			if(!mIsGiftCard) {
				//display thumbnail of last captured image in thumbnail preview displayed on bottom-left corner
				if (lastCapturedImage != null && (!lastCapturedImage.equals(""))) {
					Log.e(TAG, "mPreviewThumbnail Width => " + mPreviewThumbnail.getWidth() + " and height => " + mPreviewThumbnail.getHeight());
					displayImageThumbnailPreview(lastCapturedImage);
					lastCapturedImage = "";
				}
			}

			if (!mIsGalleryFlow) {
				if (mImageMode == ImageMode.DOCUMENT_MODE) {
					setDocumentMode();
				} else {
					setPhotoMode();
				}
				init_flash();
			}
		}
	}
	
}