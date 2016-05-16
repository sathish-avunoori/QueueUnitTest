
package com.kofax.mobilecapture;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.content.Context;
import android.util.Log;

import com.kofax.kmc.kut.utilities.appstats.AppStatistics;
import com.kofax.kmc.kut.utilities.appstats.AppStatistics.AppStatsExportFormat;
import com.kofax.kmc.kut.utilities.appstats.AppStatistics.ThresholdType;
import com.kofax.kmc.kut.utilities.appstats.AppStatsExportEvent;
import com.kofax.kmc.kut.utilities.appstats.AppStatsExportListener;
import com.kofax.kmc.kut.utilities.appstats.AppStatsThresholdReachedEvent;
import com.kofax.kmc.kut.utilities.appstats.AppStatsWriteFileListener;
import com.kofax.kmc.kut.utilities.appstats.AppStatsWritetoFileEvent;
import com.kofax.kmc.kut.utilities.appstats.AppstatsThresholdReachedListener;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.UtilityRoutines;
import com.kofax.mobilecapture.utilities.WebServiceHelper;

public class AppStatsManager implements AppstatsThresholdReachedListener, AppStatsExportListener, AppStatsWriteFileListener {

	// private static boolean writeComplete = false;
	private final String TAG = AppStatsManager.class.getSimpleName();
	private AppStatistics mStats = null;
	private static AppStatsManager pSelf = null;

	UtilityRoutines mUtilRoutines = null;

	private boolean isFileThresholdReached = false;
	private boolean canStartRecord = true;

	private final String databasePath = "KMCAppStatDB";

	private String exportServerUrl = "";// "http://172.31.70.102:80/mobilesdk/api/appStats";
	private String exportFilePath = null;

	private int fileSizeThreshold = 10720;//21440;//4194304;//21440;// 4194304; // 4194304;// 3720;//2097152 - 2MB
	// 10720;//61440;4194304
	private int ramSizeThreshold = 10720; //10720; 2097152;// 2097152;//61440;// 2097152;// 1097152-1MB

	private File fpAppStat = null;

	private Context mContext = null;

	public AppStatsManager(Context context) {
		mUtilRoutines = UtilityRoutines.getInstance();
		mContext = context;
		
		initAppStatistics();
	}

	public static AppStatsManager getInstance(Context context) {
		if(pSelf == null) {
			pSelf = new AppStatsManager(context);
		}
		return pSelf;
	}

	public boolean isSDKInit()
	{
		if(mStats!=null)
			return true;
		else
			return false;
	}
	public boolean isRecordingOn() {
		if(mStats != null) {
			return mStats.isRecording();
		}
		else {
			return false;
		}
	}

	private void initAppStatistics() {
		Log.e(TAG, "Enter:: initAppStatistics :: " + mStats);
		if (mStats != null) {
			return;
		}
		try {
			fpAppStat = new File(checkDirectory(), "KMCAppStats.json");

			if (!fpAppStat.exists()) {
				fpAppStat.createNewFile();
			}
			exportFilePath = fpAppStat.getAbsolutePath();
		} catch (IOException ex) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Exception at initAppStatistics() and Message :" + ex.getMessage());
		}

		createInstanceForAppStats();

	}

	public void deinitAppStatistics() {
		if (mStats != null) {
			try {
				if (mStats.isRecording()) {
					mStats.stopRecord();
				}

				mStats.removeAppStatsExportListener(this);
				mStats.removeAppStatsThresholdListener(this);
				mStats.removeAppStatsWriteFileListener(this);
			} catch (KmcRuntimeException ex) {
				Log.e(TAG, "Exception at deinitAppStatistics() and Message :" + ex.getMessage());
			}
		}

		// sDKAppStatistics = null;
	}
	
	private void createInstanceForAppStats() {
		//DatabaseHelper dbHelper = new DatabaseHelper(mContext);
		Log.e(TAG, "Enter:: createInstanceForAppStats");
		mStats = AppStatistics.getInstance();
		Log.e(TAG, "Initialized sDKAppStatistics ==> " + mStats);
		mStats.setDeviceId(UUID.randomUUID().toString());
		mStats.setFileSizeThreshold(fileSizeThreshold);
		mStats.setRamSizeThreshold(ramSizeThreshold);
		mStats.addAppStatsExportListener(this);
		mStats.addAppThresholdListener(this);
		mStats.addAppStatsWriteFileListener(this);
		try{
			mStats.initAppStats(databasePath);
		}
		catch(RuntimeException ex){
			Log.e(TAG, "RuntimeException at createInstanceForAppStats() and Message :" + ex.getMessage());
		}
		catch(Exception ex) {
			Log.e(TAG, "Exception at createInstanceForAppStats() and Message :" + ex.getMessage());
		}
	}

	public void startAppStatsRecord() {
		Log.e(TAG, "Enter:: startAppStatsRecord");

		startRecording();
	}


	private void startRecording() {
		Log.e(TAG, "Enter:: startRecording");
		Log.e(TAG, "sDKAppStatistics =============> " + mStats);

		try {
			if (mStats != null && !mStats.isRecording() && canStartRecord) {
				mStats.startRecord();
			}
		} catch (KmcRuntimeException ex) {
			Log.e(TAG, "Exception at startAppStatsRecord() and Message :" + ex.getMessage());
		}
	}

	public void stopAppStatsRecord() {
		try {
			if (mStats != null && mStats.isRecording()) {
				mStats.stopRecord();
			}
		} catch (KmcRuntimeException ex) {
			Log.e(TAG, "Exception at stopAppStatsRecord() and Message :" + ex.getMessage());
		}
	}

	public void purgeAppStats() {
		Log.e(TAG, "Enter:: purgeAppStats");
		// / Removes data from memory as well as database, that is all
		// data
		// from
		// / repository.
		stopAppStatsRecord();
		mStats.purge();
	}

	private File checkDirectory() {
		File dirFile;
		//dirFile = new File(Environment.getExternalStorageDirectory(), "KMCStats");
		dirFile = new File(mUtilRoutines.getAppRootPath(mContext), "KMCStats");
		dirFile.mkdir();
		return dirFile;
	}

	// boolean checkSDcard() {
	// boolean mExternalStorageWriteable = false;
	// String state = Environment.getExternalStorageState();
	// if (Environment.MEDIA_MOUNTED.equals(state)) {
	// mExternalStorageWriteable = true;
	// } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	// mExternalStorageWriteable = false;
	// } else {
	// mExternalStorageWriteable = false;
	// }
	// return mExternalStorageWriteable;
	// }

	private void exportFileToServer(String exportFilePath, String exportServer) {
		Log.e(TAG, "Enter:: exportFileToServer");
		if (exportServer == null || exportServer.equals("")) {
			Log.e(TAG, "Export Server url is null or empty");
		}
		// else if(mcontext != null & !CommonUtility.checkInternet(mcontext)){
		// Log.e(TAG,
		// "Can not export to server as Internet connection is not available");
		// }
		else {

			WebServiceHelper myAsyncTask = new WebServiceHelper(exportFilePath, exportServerUrl, 80, 443);
			myAsyncTask.execute("");
		}
	}

	@Override
	public void thresholdReached(AppStatsThresholdReachedEvent arg0) {
		Log.e(TAG, "Enter:: thresholdReached event!!!!!!!!!!!");
		ThresholdType threshold = arg0.getThresholdType();

		switch (threshold) {
		case THRESH_TYPE_RAM:
			Log.e(TAG, "Enter:: thresholdReached event - THRESH_TYPE_RAM!!!!!!!!!!!");
			if (mStats.isRecording()) {
				Log.e(TAG, "RAM Threshold reached writing to file.");
				try {
					canStartRecord = false; 
					stopAppStatsRecord();
					mStats.writeToFile();
				} catch (KmcRuntimeException ex) {
					Log.e(TAG, "Exception at thresholdReached() for RAM Thresh type and Message :" + ex.getMessage());
				}
			}

			break;
		case THRESH_TYPE_FILE:
			Log.e(TAG, "Enter:: thresholdReached event - THRESH_TYPE_FILE!!!!!!!!!!!");
			isFileThresholdReached = true;
			break;

		default:
			break;
		}
	}

	@Override
	public void writeFileStatusEvent(AppStatsWritetoFileEvent event) {
		Log.e(TAG, "Enter:: writeFileStatusEvent !!!!!!!!!!!!!!!");
		// ErrorInfo errorInfo = event.getErrorInfo();
		if (event.getWritetoFileStatus() == WriteFileStatus.WRITE_STATUS_COMPLETE) {
			Log.i(TAG, " Writing App Stats file Completed");
			if (isFileThresholdReached) {                               
				Log.i(TAG, "FILE Threshold reached exporting to file :" + exportFilePath);
				try {
					mStats.export(exportFilePath, AppStatsExportFormat.EXP_FORMAT_JSON);
				} catch (KmcRuntimeException ex) {
					Log.e(TAG, "Exception at thresholdReached() for FILE Thresh type and Message :" + ex.getMessage());
				}
			} 
			else {
				canStartRecord = true; 
				startRecording();
			}

		}
		// else {
		// Log.e("Writing App Stats not done", "Percent complete: " +
		// event.getPercentComplete());
		// }

	}

	@Override
	public void exportStatusEvent(AppStatsExportEvent event) {
		Log.e(TAG, "Enter:: exportStatusEvent !!!!!!!!!!!!!!!");
		if (event.getExportStatus() == ExportStatus.COMPLETE) {
			isFileThresholdReached = false;
			Log.i(TAG, " Appstats Export Event Completed");
			try {
				exportServerUrl = Globals.AppStatsExportServerUrl;
				exportFileToServer(exportFilePath, exportServerUrl);
				// startAppStatsRecord() will be called in web service helper after exporting to server.
				// canStartRecord = true;
				//startAppStatsRecord();
			} catch (KmcRuntimeException ex) {
				Log.e(TAG, "Exception at exportStatusEvent() and Message :" + ex.getMessage());
			}

		}
	}

	public boolean isCanStartRecord() {
		return canStartRecord;
	}

	public void setCanStartRecord(boolean canStartRecord) {
		this.canStartRecord = canStartRecord;
	}
}
