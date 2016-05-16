// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kofax.kmc.ken.engines.ImageProcessor;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutEvent;
import com.kofax.kmc.ken.engines.ImageProcessor.ImageOutListener;
import com.kofax.kmc.ken.engines.data.BasicSettingsProfile;
import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.ken.engines.data.Image.ImageRep;
import com.kofax.kmc.ken.engines.data.ImagePerfectionProfile;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

import java.io.File;

/// The ImageProcessManager takes care of processing image and posting result to the caller. 

public class ImageProcessManager implements ImageOutListener {
	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants
	// - private constants
	private final String TAG = ImageProcessManager.class.getSimpleName();

	// - Private data.
	private static volatile ImageProcessManager pSelf = null;
	private AppStatsManager mAppStatsManager = null;

	/* SDK objects */
	private ImageProcessor mProcessor = null;
	private Image mImage = null;

	/* Application objects */
	/* Standard variables */
	private Handler mCallerHandler = null;
	private boolean isProcessorBusy = false;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes
	@SuppressWarnings("deprecation")
	private ImageProcessManager(Context context) {
		mProcessor = new ImageProcessor();
		mAppStatsManager = AppStatsManager.getInstance(context);
	}

	// - public getters and setters
    //! The factory method.
	public static ImageProcessManager getInstance(Context context) {
		if (pSelf == null) {
			synchronized (ImageProcessManager.class) {
				if (pSelf == null) {
					pSelf = new ImageProcessManager(context);					
				}
			}
		}
		return pSelf;
	}

	// - public methods

	@SuppressWarnings("deprecation")
    //! Initiates SDK's ImageProcessor object and ImagePerfectionprofile and BasicSettingProfile and call for image processing on the specified image.
	/**
	 * 
	 * @param inImgPath
	 * @param outImagePath
	 * @param docTypeObj
	 * @param handler
	 * @throws KmcException
	 * @throws KmcRuntimeException
	 */
	public void Process(String inImgPath, String outImagePath, DocumentType docTypeObj,
			Handler handler) throws KmcException, KmcRuntimeException {

        if (Constants.APP_STAT_ENABLED && !mAppStatsManager.isRecordingOn()) {
            mAppStatsManager.startAppStatsRecord();
        }

		mCallerHandler = handler;
		if (mProcessor == null) {
			mProcessor = new ImageProcessor();
		}
		ImagePerfectionProfile ipp = docTypeObj
				.getImagePerfectionProfile();
		BasicSettingsProfile bsp = docTypeObj
				.getBasicSettingsProfile();
		Process(inImgPath, outImagePath, ipp, bsp, handler);

	}

	@Override
	public void imageOut(final ImageOutEvent arg0) {
		Log.i(TAG, "Enter imageOut Listener");
		String imageFilePath = null;

		mProcessor.removeImageOutEventListener(this);
		
/*        if (mAppStatsManager.isRecordingOn()) {
            mAppStatsManager.stopAppStatsRecord();
        }
*/
		clearImage(mImage);
		resetProfiles();
		
		Log.i(TAG, "ImageOut received!!!!!!!!!!! with error status :::::::::: + " + arg0.getStatus().name());

		if(arg0.getStatus().ordinal() == ErrorInfo.KMC_EV_CANCEL_OPERATION_SUCCESS.ordinal()) {
			Message msg = new Message();
			msg.what = Messages.MESSAGE_IMAGE_PROCESS_CANCELLED.ordinal();
			msg.arg1 = arg0.getStatus().getErr();
			mCallerHandler.sendMessage(msg);
		}else if(arg0.getStatus().ordinal() == ErrorInfo.KMC_EV_IMAGE_PROCESSING.ordinal()){
			Message msg = new Message();
			msg.what = Messages.MESSAGE_PROCESS_FAILED.ordinal();
			msg.arg1 = arg0.getStatus().getErr();
			mCallerHandler.sendMessage(msg);
		}else if(arg0.getStatus().ordinal() == ErrorInfo.KMC_GN_UNKNOWN_ERROR.ordinal()){
			Message msg = new Message();
			msg.what = Messages.MESSAGE_PROCESS_FAILED.ordinal();
			msg.arg1 = arg0.getStatus().getErr();
			mCallerHandler.sendMessage(msg);
		}else {
			if (arg0.getImage() != null) {
				imageFilePath = arg0.getImage().getImageFilePath();
				Log.i(TAG, "Processed image file path is ===========> "
						+ imageFilePath);
				UtilityRoutines.getInstance().saveScaledBitmapToFilepath(arg0.getImage().getImageBitmap(), imageFilePath);
				clearImage(arg0.getImage());
			}
			if (mCallerHandler != null) {
				new Handler().postDelayed(new Runnable(){
					@Override
					public void run() {
						Message msg = new Message();
						msg.what = Messages.MESSAGE_IMAGE_PROCESSED.ordinal();
						msg.arg1 = arg0.getStatus().getErr();
						if(arg0.getImage() != null){
							msg.obj = arg0.getImage().getImageMetaData();
						}
						Log.e(TAG, "Processing result ===============> " + arg0.getStatus().getErr());
						Log.e(TAG, "Processing error message ===============> " + arg0.getStatus().getErrMsg());
						mCallerHandler.sendMessage(msg);
					}
				},500);

			} else {
				Log.e(TAG, "Caller Handler is NULL, cannot send message");
			}
		}
		isProcessorBusy = false;
	}

	//! Cancels image processing if already in progress.
	/**
	 * Method initiates a cancel on ImageProcessor object and returns false if already some processing is in progress.
	 * @return true of processor is already free, false otherwise.
	 */
	public boolean cancelProcess() {
		if(isProcessorBusy) {
			mProcessor.cancel();
			return false;	//false for caller to wait until cancel-callback is received
		}
		else  {
			return true;	//true for caller not to wait for cancel-callback since processor is already free.
		}
	}

	//! Removes ImageOut event listener from ImageProcessManager.
	public void reset() {
		if(mProcessor != null) {
			mProcessor.removeImageOutEventListener(this);
		}
	}

	//! Cleanup of ImageProcessManager object.
	public void cleanup() {
		pSelf = null;
		if(mProcessor != null) {
			mProcessor.removeImageOutEventListener(this);
			//	mProcessor = null;
		}
		clearImage(mImage);
		mCallerHandler = null;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void Process(String inImagePath, String outImagePath,
			ImagePerfectionProfile ipp, BasicSettingsProfile bsp,
			Handler handler) throws KmcException, KmcRuntimeException {

		Log.e(TAG, "In Process ::  inImagePath ===============> " + inImagePath);
		Log.e(TAG, "In Process ::  outImagePath ===============> " + outImagePath);
		Log.e(TAG, "In Process ::  ipp ===============> " + ipp);

		//Deleting output file if it is already exists
		File file = new File(outImagePath);
		if(file.exists()){
			file.delete();
		}

		mCallerHandler = handler;
		
		//reset processing profiles before assigning new ones.
		resetProfiles();
		
		if (ipp != null) {
			mProcessor.setImagePerfectionProfile(ipp);
		} else {
			mProcessor.setBasicSettingsProfile(bsp);
		}
		mProcessor.setProcessedImageRepresentation(ImageRep.IMAGE_REP_BOTH);
		mProcessor.setProcessedImageFilePath(outImagePath);
		mProcessor.setProcessedImageMimeType(ImageMimeType.MIMETYPE_TIFF);
		if(inImagePath.contains(Constants.PNG_EXTENSION)){
			mImage = new Image(inImagePath, ImageMimeType.MIMETYPE_PNG);
		}
		else if(inImagePath.contains(Constants.TIFF_EXTENSION)) {
			mImage = new Image(inImagePath, ImageMimeType.MIMETYPE_TIFF);
		}
		else{
			mProcessor.setProcessedImageJpegQuality(100);
			mImage = new Image(inImagePath, ImageMimeType.MIMETYPE_JPEG);
		}

		mImage.imageReadFromFile();

		mProcessor.addImageOutEventListener(this);
		mProcessor.processImage(mImage);
		isProcessorBusy = true;
	}
	
	private void resetProfiles() {
		mProcessor.setImagePerfectionProfile(null);
		mProcessor.setBasicSettingsProfile(null);
	}

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
	}
}
