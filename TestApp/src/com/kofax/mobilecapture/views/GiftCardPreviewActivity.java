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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutEvent;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutListener;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.ken.engines.data.Image.ImageRep;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;
import com.kofax.kmc.kui.uicontrols.ImgReviewEditCntrl;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.NullPointerException;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.GiftCardManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

import java.io.File;

public class GiftCardPreviewActivity extends Activity implements ImageOutListener{

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = GiftCardPreviewActivity.class.getSimpleName();

	// - Private data.
	/* SDK objects */
	private ImgReviewEditCntrl mPreview;
	private ImageProcessor iProcessorObj = null;
	private Image mInputImageObj = null;
	private Image mOutputImageObj = null;
	private ImagePerfectionProfile ipp = null;

	/* Application objects */
	private ImageProcessQueueManager mProcessQueueMgr = null;
	private UtilityRoutines mUtilityRoutines = null;
	GiftCardManager mGiftCardManager = null;

	// private DocumentManager pDocMgr = null;

	/* Standard variables */
	private CustomDialog mCustomDialog = null;
	private BroadcastReceiver mReceiver = null;
	private Bitmap mPreviewBitmap = null;
	private Bitmap scaledBmp = null;
	private Handler mHandler = null;
	//private Bitmap mImgBitmap = null;
	private RelativeLayout parentLayout = null;
	private ViewFlipper mFlipper = null;
	private Intent mReturnIntent = null;
	private Image mProcessedImage = null;
	private String mImgUrl = null;

	private final String processingString = "_DeviceType_2_DoSkewCorrectionPage__DoCropCorrection__Do90DegreeRotation_4_DoBinarization__DoScaleImageToDPI_500_DocDimSmall_2.125_DocDimLarge_3.375_LoadSetting_<Property Name=\"CSkewDetect.prorate_error_sum_thr_bkg_brightness.Bool\" Value=\"1\" Comment=\"Default  0\"></Property>_LoadSetting_<Property Name=\"CSkwCor.Do_Fast_Rotation.Bool\" Value=\"0\" Comment=\"Default   1\"></Property>)";
	private final String processedImageName = "giftcardprocessed.tiff";
	private String processedImageFileLocation;
	private boolean isRekateSelected = false;

	//	private int mImageIndex = -1;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle saveBundle) {
		super.onCreate(saveBundle);
		
		new DeviceSpecificIssueHandler().checkEntryPoint(this);

		setContentView(R.layout.quick_preview_layout);
		parentLayout = (RelativeLayout) findViewById(R.id.parentLayout);

		getActionBar().setTitle(getResources().getString(R.string.actionbar_lbl_screen_preview));

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		Bundle b = getIntent().getExtras();
		mImgUrl = b.getString(Constants.STR_URL);

		mCustomDialog = CustomDialog.getInstance();
		mProcessQueueMgr = ImageProcessQueueManager
				.getInstance(getApplicationContext());
		mUtilityRoutines = UtilityRoutines.getInstance();
		iProcessorObj = new ImageProcessor();

		processedImageFileLocation = mUtilityRoutines.getAppRootPath(this) + processedImageName;

		if (mImgUrl != null) {
			mPreview = (ImgReviewEditCntrl) findViewById(R.id.imgPreview);
			loadImage(mImgUrl, null);
		}
		if(!Constants.BACKGROUND_IMAGE_PROCESSING) {
			registerBroadcastReceiver();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		setupHandler();

		mGiftCardManager = GiftCardManager.getInstance(mHandler);

		//cleanup disk location for the processed image if was not cleaned already due to some reason
		deleteFileAtLocation(processedImageFileLocation);

		Log.i(TAG, "Pausing background process queue...");
		mCustomDialog.showProgressDialogWithButton(this, getResources().getString(R.string.progress_msg_please_wait), Globals.Messages.MESSAGE_DIALOG_CANCEL, mHandler);
		//to begin image-processing for the gift card image, first pause background image-processing queue.
		mProcessQueueMgr.pauseQueue(mHandler);
		super.onPostCreate(savedInstanceState);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		default:
			break;
		}
		return false;
	}

	@Override
	public void imageOut(ImageOutEvent arg0) {
		Log.i(TAG, "Enter:: imageOut");
//		Log.i(TAG, "Error:: " + arg0.getStatus().getErr());
		if (iProcessorObj != null) {
			iProcessorObj.removeImageOutEventListener(this);
		}
		else {
			return;
		}
		if(arg0.getStatus() != ErrorInfo.KMC_SUCCESS) {
			Log.e(TAG, "Image processing failed");
			mCustomDialog.closeProgressDialog();
		}
		else {
			if((arg0.getImage() != null) && (arg0.getImage().getImageFilePath() != null)) {
				clearImage(mProcessedImage);
				mProcessedImage = arg0.getImage();
				loadImage(null, mProcessedImage.getImageBitmap());
				mGiftCardManager.setImage(mProcessedImage);
				mGiftCardManager.extractGiftCardData(mProcessedImage.getImageFilePath());
			}
		}
	}


	@Override
	public void onBackPressed() {
		Intent returnIntent = new Intent();

		returnIntent.putExtra(Constants.STR_RETAKE, false);
		setResult(Globals.ResultState.RESULT_CANCELED.ordinal(), returnIntent);

		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mGiftCardManager.removeReceiverHandler(mHandler);
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}

		if (iProcessorObj != null) {
			resetImageProcessorObject();
			iProcessorObj = null;
		}

		if(isRekateSelected) {
			mGiftCardManager.cleanup();			
		}

		mImgUrl = null;

		if(mPreview != null && mPreview.getImage() != null){
			mPreview.clearImage();
		}

		clearImage(mInputImageObj);
		clearImage(mOutputImageObj);

		if (mPreviewBitmap != null) {
			mPreviewBitmap.recycle();
			mPreviewBitmap = null;
		}

		if(scaledBmp != null) {
			scaledBmp.recycle();
			scaledBmp = null;
		}

		if (parentLayout != null) {
			parentLayout.removeAllViews();
			parentLayout = null;
		}
		if(mFlipper != null) {
			mFlipper.removeAllViews();
			mFlipper = null;
		}

		if(mCustomDialog != null) {
			mCustomDialog.closeProgressDialog();
		}

		if(Constants.BACKGROUND_IMAGE_PROCESSING) {
			mProcessQueueMgr.resumeQueue();
		}
		mPreview = null;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void loadImage(String filePath, Bitmap mPreviewBitmap) {
		if (mPreviewBitmap == null && filePath != null) {
			mPreviewBitmap = BitmapFactory.decodeFile(filePath);
		}
		if (mPreviewBitmap != null) {
			scaledBmp = scaleBitmap(mPreviewBitmap);

			mPreview.setVisibility(View.VISIBLE);
			try {
				mPreview.setImage(new Image(scaledBmp));
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (KmcException e) {
				e.printStackTrace();
			}
		}
		else {
			Toast.makeText(this, getResources().getString(R.string.app_msg_image_notfound), Toast.LENGTH_LONG).show();
		}
	}

	@SuppressLint("NewApi")
	private Bitmap scaleBitmap(Bitmap bmp) {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int displayWidth = size.x;
		int displayHeight = size.y;

		Bitmap background = Bitmap.createBitmap((int)displayWidth, (int)displayHeight, Config.ARGB_8888);
		float originalWidth = bmp.getWidth(), originalHeight = bmp.getHeight();
		Canvas canvas = new Canvas(background);
		float scale = displayWidth/originalWidth;
		float xTranslation = 0.0f, yTranslation = (displayHeight - originalHeight * scale)/2.0f;
		Matrix transformation = new Matrix();
		transformation.postTranslate(xTranslation, yTranslation);
		transformation.preScale(scale, scale);
		Paint paint = new Paint();
		paint.setFilterBitmap(true);
		canvas.drawBitmap(bmp, transformation, paint);
		paint = null;
		transformation = null;
		return background;
	}

	private void processGiftCardImage() throws KmcException{
		resetProfile();
		ipp = new ImagePerfectionProfile();
		ipp.setIpOperations(processingString);

		iProcessorObj.setProcessedImageFilePath(mUtilityRoutines.getAppRootPath(this) + processedImageName);
		iProcessorObj.setProcessedImageMimeType(ImageMimeType.MIMETYPE_TIFF);
		iProcessorObj.setProcessedImageRepresentation(ImageRep.IMAGE_REP_BOTH);
		iProcessorObj.setImagePerfectionProfile(ipp);
		iProcessorObj.addImageOutEventListener(this);

		Image inputImage = new Image(mImgUrl, ImageMimeType.MIMETYPE_JPEG);
		iProcessorObj.processImage(inputImage);
	}

	private void resetProfile(){
		if(iProcessorObj != null) {
			iProcessorObj.setImagePerfectionProfile(null);
		}
	}

	private void resetImageProcessorObject() {
		iProcessorObj.removeImageOutEventListener(this);
		iProcessorObj.setBasicSettingsProfile(null);
		iProcessorObj.setImagePerfectionProfile(null);
		iProcessorObj.setProcessedImageFilePath(null);
		iProcessorObj.setProcessedImageMimeType(null);
		iProcessorObj.setProcessedImageRepresentation(null);
	}


	private void clearImage(Image img) { 
		if (img != null) {
			img.imageClearBitmap(); 
			img = null; 
		} 
	}

	private void deleteFileAtLocation(String path) {
		File fp = new File(path);
		if(fp.exists()) {
			fp.delete();
			fp = null;
		}
	}

	private void setupHandler() {
		mHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
				try {
					switch (whatMessage) {
					case MESSAGE_PROCESS_QUEUE_PAUSED:
						Log.i(TAG, "processing gift card image ...");
						processGiftCardImage();
						break;
					case MESSAGE_GIFTCARD_EXTRACTION_COMPLETED:
						mCustomDialog.closeProgressDialog();
						//if extraction was successful, launch next screen
						if(msg.arg1 == Globals.ResultState.RESULT_OK.ordinal()) {
							Intent i = new Intent(GiftCardPreviewActivity.this, GiftCardInformation.class);
							startActivity(i);
							finish();
						}
						else {
							//else display option to user to either recapture image or enter the card details manually 
							mCustomDialog.show_popup_dialog(GiftCardPreviewActivity.this, AlertType.CONFIRM_ALERT, 
									getResources().getString(R.string.error_lbl),
									getResources().getString(R.string.error_msg_giftcard_balance), 
									getResources().getString(R.string.str_retake), getResources().getString(R.string.str_enter_manually),
									Messages.MESSAGE_DIALOG_GIFTCARD_RETAKE_CONFIRMATION,
									mHandler,
									false);
						}
						break;
					case MESSAGE_DIALOG_GIFTCARD_RETAKE_CONFIRMATION:
						if (msg.arg1 == RESULT_OK) {	//retake giftcard image
							openCaptureActivity();
							isRekateSelected = true;
						}
						else if(msg.arg1 == RESULT_CANCELED) {
							//take backup of processed image object to display it's bitmap on giftcard-information screen. (this is because the saved processed tiff file cannot be displayed on screen)
							Intent i = new Intent(GiftCardPreviewActivity.this, GiftCardInformation.class);
							i.putExtra(Constants.EXTRACTION_RESULT, msg.arg1);
							startActivity(i);
						}
						finish();
						break;
					case MESSAGE_DIALOG_CANCEL:
						mCustomDialog.closeProgressDialog();
						openCaptureActivity();
						finish();
						break;
					default:
						break;
					}
				} catch (KmcException e) {
					e.printStackTrace();
					Toast.makeText(GiftCardPreviewActivity.this, getResources().getString(R.string.toast_processing_failed), Toast.LENGTH_LONG).show();
					mCustomDialog.closeProgressDialog();
				}
				return true;
			}
		}); 
	}

	private void openCaptureActivity(){
		Intent intent = new Intent(this, Capture.class);
		Bundle bundle = new Bundle();
		bundle.putBoolean(Constants.STR_GIFTCARD, true);
		intent.putExtras(bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
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
						if(intent.hasExtra(Constants.STR_PROCESS_STATUS) && intent.getLongExtra(Constants.STR_PROCESS_STATUS, 0) != 0){							
							Toast.makeText(context, getResources().getString(R.string.toast_processing_failed), Toast.LENGTH_SHORT).show();
						}
						GiftCardPreviewActivity.this.setResult(Globals.ResultState.RESULT_OK.ordinal(), mReturnIntent);
						finish();
					}
				}
			};
		}
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_IMAGE_PROCESSED);
		registerReceiver(mReceiver, intentFilter);
	}
}
