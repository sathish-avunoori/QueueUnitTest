// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.content.Context;

import com.kofax.mobilecapture.utilities.CustomUrlUtils;
import com.kofax.mobilecapture.utilities.UtilityRoutines;
import com.kofax.mobilecapture.views.CustomDialog;

/// A class to keep check of application launch and when app get closed unexpectedly, recover recover it gracefully.

public class Initializer {
	private static volatile Initializer initializer; 

	public static boolean INITIATED_FROM_MAIN = false; // this is set to true in MainActivity. Required for device specific behavior
	
	public static synchronized Initializer getInstance(){
		if (initializer == null) {
			synchronized (Initializer.class) {
				if (initializer == null) {
					initializer = new Initializer();
				}
			}
		}
		return initializer;
	}

	// - public methods
	//! Sets application entry point as it is intiated from Main.
	public void setEntryPointMain() {
		INITIATED_FROM_MAIN = true;
	}
		
	//! Get entry point of application to check where application was initiated from.
	public boolean getEntryPointMain() {
		return INITIATED_FROM_MAIN;
	}
		
	//! Function to initialize all required object if application was launch from an unexpected entry point instead of main due to abruptly closing application (like device reboot).
	/**
	 * When application gets closed unexpectedly, HTC devices has issue of launching it from the activity where it got closed earlier.
	 * In such cases, this function helps initiating all the required object, so that application can start gracefully.  
	 * @param context
	 */
	public void initiateManagersAndServices(Context context) {
		if(INITIATED_FROM_MAIN) {
			return;
		}
		UtilityRoutines mUtilRoutines = UtilityRoutines.getInstance();
		if (mUtilRoutines.checkKMCLicense(context)) 
		{
			DiskManager.getInstance(context);
			CustomDialog.getInstance();
	
			PrefManager mPrefUtils;
			mPrefUtils = PrefManager.getInstance();
			mPrefUtils.init(context);
	
			UtilityRoutines.getInstance().checkApplicationDependencies(context);//call to initialize sharedPreferences
			CustomUrlUtils.getInstance();
			DocumentManager.getInstance(context);
	
			DocumentManager.getInstance(context);
			UtilityRoutines.getInstance();
			DiskManager.getInstance(context);
			ImageProcessQueueManager.getInstance(context);
			DatabaseManager.getInstance();
			CustomUrlUtils.getInstance();
		}
		mUtilRoutines = null;
	}

}
