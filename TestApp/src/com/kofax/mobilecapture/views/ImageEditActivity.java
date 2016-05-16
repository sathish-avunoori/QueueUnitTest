// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutEvent;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutListener;
import com.kofax.kmc.ken.engines.data.BasicSettingsProfile;
import com.kofax.kmc.ken.engines.data.BasicSettingsProfile.CropType;
import com.kofax.kmc.ken.engines.data.BoundingTetragon;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageRep;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;
import com.kofax.kmc.kui.uicontrols.ImgReviewEditCntrl;
import com.kofax.kmc.kui.uicontrols.ImgReviewEditCntrl.Line_Style_Solid;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

import java.io.IOException;

public class ImageEditActivity extends Activity implements ImageOutListener,NetworkChangedListener {

	// - public enums

	// - Private enums
	private enum requestType {
		NONE,
		CROP,
		ROTATE
	}
	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = ImageEditActivity.class.getSimpleName();

	// - Private data.
	/* SDK objects */
	private ImgReviewEditCntrl mImgReviewEditCntrl;
	private Image mImgObj = null;
	private ImageProcessor iProcessor = null;
	private BasicSettingsProfile bsp = null;
	private ImagePerfectionProfile ipp = null;

	/* Application objects */
	private DiskManager mDiskMgr = null;
	private DatabaseManager mDatabaseManager = null;
	private ImageProcessQueueManager mProcQueueMgr = null;

	/* Standard variables */
	private Menu mMenu = null;
	private ProgressBar mProgressbar = null; 
	private Handler mHandler = null;
	private String mImgUrl = null;
	private boolean mIsEdited = false;
	private boolean mIsOfflineAlertRequest = false;
	private requestType nextRequest = requestType.NONE;
	
	private CustomDialog mCustomDialog = null;
	private UtilityRoutines mUtilRoutines = null;
	
	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle saveBundle) {
		super.onCreate(saveBundle);
		setContentView(R.layout.image_edit_view);

		mCustomDialog = CustomDialog.getInstance();
		mUtilRoutines = UtilityRoutines.getInstance();
		mDatabaseManager = DatabaseManager.getInstance();

		mImgReviewEditCntrl = (ImgReviewEditCntrl) findViewById(R.id.preview_edit_image);
		mImgReviewEditCntrl.setCropLineStyle(Line_Style_Solid.LINE_STYLE_DOTTED);

		mProgressbar = (ProgressBar)findViewById(R.id.preview_edit_progressbar);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		mProcQueueMgr = ImageProcessQueueManager.getInstance(getApplicationContext());

		Bundle b = getIntent().getExtras();
		mImgUrl = b.getString(Constants.STR_URL);

		loadOriginalImage();
		setupHandler();
	}

	@Override
	protected void onResume(){
		super.onResume();
		Constants.NETWORK_CHANGE_LISTENER = ImageEditActivity.this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image_edit_menu, menu);
		this.mMenu = menu;
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent returnIntent = new Intent();
			if (isMenuVisible()) {
				if (mIsEdited) {
					if (mDiskMgr == null) {
						mDiskMgr = DiskManager.getInstance(getApplicationContext());
					}
					//save edited image at the original location on disk
					try {
						mDiskMgr.saveImageAtLocation(mImgObj.getImageBitmap(), mImgUrl);
						setResult(Globals.ResultState.RESULT_OK.ordinal(),returnIntent);
					} catch (KmcRuntimeException e) {
						e.printStackTrace();
					} catch (KmcException e) {
						e.printStackTrace();
					} catch (IOException e) {
						Toast.makeText(this, getResources().getString(R.string.toast_save_image_failed) + e.getMessage(), Toast.LENGTH_LONG).show();
						e.printStackTrace();
						setResult(Globals.ResultState.RESULT_CANCELED.ordinal(),returnIntent);
					}
				}
				else {
					setResult(Globals.ResultState.RESULT_CANCELED.ordinal(),returnIntent);
				}
				finish();
			}
			else {
				resetScreen();
			}
			return true;
		case R.id.menu_crop_image:
			if (isMenuVisible()) {
				hideMenu();
				showCropRectangleOnImage();
			}
			else  {
				//Crop image based on given co-ordinates
				showProgressbar();
				nextRequest = requestType.CROP;
				//pause the image-process-queue first
				mProcQueueMgr.pauseQueue(mHandler);
				showMenu();
			}
			break;
		case R.id.menu_rotate_image:
			showProgressbar();
			nextRequest = requestType.ROTATE;
			//pause the image-process-queue first
			mProcQueueMgr.pauseQueue(mHandler);
			break;
		case android.R.id.title:
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void imageOut(ImageOutEvent arg0) {
		Log.i(TAG, "Enter:: imageOut");
		Log.i(TAG, "Error:: " + arg0.getStatus().getErr());

		if(arg0.getStatus() != ErrorInfo.KMC_SUCCESS) {
			Toast.makeText(this, nextRequest.name() + getResources().getString(R.string.toast_processing_failed), Toast.LENGTH_LONG).show();
		}
		else {
			if (iProcessor != null) {
				iProcessor.removeImageOutEventListener(this);
				try {
					if((arg0.getImage() != null) && (arg0.getImage().getImageBitmap() != null)) {
						if (mImgObj != null) {
							clearImage(mImgObj);
						}
						mImgObj = arg0.getImage();
						clearImage(mImgReviewEditCntrl.getImage());
						mImgReviewEditCntrl.setImage(mImgObj);
						mIsEdited = true;
					}
				} catch (KmcException e) {
					e.printStackTrace();
				}
			}
		}
		resetScreen();
		Log.i(TAG, "Exit:: imageOut");
	}

	@Override
	public void onBackPressed() {
		if (!isMenuVisible()) {
			resetScreen();
		}
		else {
			super.onBackPressed();
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();

		resetProfile();

		if (iProcessor != null) {
			iProcessor.removeImageOutEventListener(this);
			iProcessor = null;
		}
		if(Constants.BACKGROUND_IMAGE_PROCESSING &&  (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mDatabaseManager.isDocumentSerializedInDB(mDatabaseManager.getItemEntity()))) {
			mProcQueueMgr.resumeQueue();
		}
		ipp = null;
		bsp = null;
		hideProgressbar();
		clearImage(mImgObj);
		if (mImgReviewEditCntrl != null) {
			mImgReviewEditCntrl.destroyDrawingCache();
			clearImage(mImgReviewEditCntrl.getImage());
		}
		mDatabaseManager = null;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void setupHandler() {
		mHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
				try {

					switch (whatMessage) {
					case MESSAGE_PROCESS_QUEUE_PAUSED:
						if(nextRequest == requestType.CROP) {
							cropImage(mImgReviewEditCntrl.getCropTetragon());
						}
						else if(nextRequest == requestType.ROTATE) {
							rotateImage();
						}
						break;
					 case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
							//Navigate back to settings
							if(msg.arg1 == RESULT_OK){
								mUtilRoutines.offlineLogout(ImageEditActivity.this);
							}else{
								Globals.gAppModeStatus = Globals.AppModeStatus.FORCE_OFFLINEMODE;
							}
							break;
					default:
						break;
					}
				} catch (KmcException e) {
					e.printStackTrace();
				}
				return true;
			}
		}); 
	}

	
	private void createImage(String url) {
		Bitmap bmp = mUtilRoutines.getBitmapFromUrl(url);
		if (bmp != null) {
			mImgObj = new Image(bmp);
			bmp = null;
		}
		else {
			Log.i(TAG, "Error: bitmap is null");
			Toast.makeText(this, getResources().getString(R.string.toast_error_bitmap_null), Toast.LENGTH_LONG).show();
		}
	}

	@SuppressWarnings("deprecation")
	private void rotateImage() throws KmcException {
		resetProfile();

		if (iProcessor == null) {
			iProcessor = new ImageProcessor();
		}

		if (ipp == null) {
			ipp = new ImagePerfectionProfile();
		}
		ipp.setIpOperations("_DeviceType_0_Do90DegreeRotation_1");
		iProcessor.setImagePerfectionProfile(ipp);        

		iProcessor.setBasicSettingsProfile(null);
		iProcessor.setProcessedImageFilePath(null);
		iProcessor.setProcessedImageRepresentation(ImageRep.IMAGE_REP_BITMAP);
		iProcessor.addImageOutEventListener(this);
		iProcessor.processImage(mImgObj);
	}

	private void showCropRectangleOnImage() {
		if(mImgReviewEditCntrl.getImage() != null && mImgReviewEditCntrl.getImage().getImageBitmap() != null) {
			int width = mImgReviewEditCntrl.getImage().getImageBitmapWidth();
			int height = mImgReviewEditCntrl.getImage().getImageBitmapHeight();
			BoundingTetragon bt = new BoundingTetragon(10,10, width-10, 10,  10, height-10, width-10, height-10);
			mImgReviewEditCntrl.setCropTetragon(bt);
			mImgReviewEditCntrl.showCropRectangle(true);
			mImgReviewEditCntrl.invalidate();
		}
	}
	
	//TODO: Move this function to processManager later once the file is created.
	@SuppressWarnings("deprecation")
	private void cropImage(BoundingTetragon bt) throws KmcException{
		try{
		resetProfile();

		if(iProcessor == null) {
			iProcessor = new ImageProcessor();
		}

		if (bsp == null) {
			bsp = new BasicSettingsProfile();
		}

		bsp.setCroppingTetragon(bt);
		bsp.setCropType(CropType.CROP_TETRAGON);

		iProcessor.setBasicSettingsProfile(bsp);
		iProcessor.setImagePerfectionProfile(null);
		iProcessor.setProcessedImageFilePath(null);
		iProcessor.setProcessedImageRepresentation(ImageRep.IMAGE_REP_BITMAP);
		iProcessor.addImageOutEventListener(this);
		iProcessor.processImage(mImgObj);
		}catch(IllegalArgumentException e){
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
			mImgReviewEditCntrl.showCropRectangle(false);
			hideProgressbar();
		}catch (Exception e) {
			mImgReviewEditCntrl.showCropRectangle(false);
			hideProgressbar();
		}
		
	}

	/**
	 * Clears the edited image object and reloads originally displayed image. 
	 */
	private void loadOriginalImage() {

		clearImage(mImgObj);

		createImage(mImgUrl);

		// clear image-edit-review-control if already loaded(this is applicable when revert option is selected. on screen launch, this will be empty).
		clearImage(mImgReviewEditCntrl.getImage());

		try {
			mImgReviewEditCntrl.setImage(mImgObj);
		} catch (KmcException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reset the BSP before using the object again.
	 */
	private void resetProfile(){
		if(iProcessor != null) {
			iProcessor.setImagePerfectionProfile(null);
			iProcessor.setBasicSettingsProfile(null);
		}
	}

	private void resetScreen() {
		mMenu.findItem(R.id.menu_rotate_image).setVisible(true);
		mImgReviewEditCntrl.showCropRectangle(false);
		mImgReviewEditCntrl.invalidate();
		hideProgressbar();

		if(mIsEdited) {
			setTitle(getResources().getString(R.string.str_save));
		}
		if(mIsOfflineAlertRequest){
			mIsOfflineAlertRequest = false;
			mCustomDialog.dismissAlertDialog();
			mCustomDialog.show_popup_dialog(ImageEditActivity.this,AlertType.CONFIRM_ALERT ,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
				getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION,mHandler, false);
		
		}
	}

	private void showProgressbar() {
		mProgressbar.setVisibility(View.VISIBLE);
	}

	private void hideProgressbar() {
		mProgressbar.setVisibility(View.GONE);
	}

	private void hideMenu() {
		mMenu.findItem(R.id.menu_rotate_image).setVisible(false);
		setTitle(Constants.STR_EMPTY);
	}

	private void showMenu() {
		mMenu.findItem(R.id.menu_rotate_image).setVisible(true);
		setTitle(getResources().getString(R.string.str_save));
	}

	private boolean isMenuVisible() {
		return mMenu.findItem(R.id.menu_rotate_image).isVisible();
	}

	private void clearImage(Image imgObj) {
		if (imgObj != null) {
			imgObj.imageClearBitmap();
			imgObj = null;
		}
	}

	@Override
	public void onNetworkChanged(boolean isConnected) {
		if(Globals.isRequiredOfflineAlert()  && isConnected && mUtilRoutines.isAppOnForeground(ImageEditActivity.this)){
			if(mProgressbar.isShown()){
				mIsOfflineAlertRequest = true;
			}else{
				mIsOfflineAlertRequest = false;
				if(mCustomDialog != null){
					mCustomDialog.dismissAlertDialog();
					mCustomDialog.show_popup_dialog(ImageEditActivity.this,AlertType.CONFIRM_ALERT ,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
							getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION,mHandler, false);
				}
			}
		}
		
	}
}
