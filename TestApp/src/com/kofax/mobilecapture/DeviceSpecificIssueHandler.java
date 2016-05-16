// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.app.Activity;
import android.content.Intent;

import com.kofax.mobilecapture.utilities.UtilityRoutines;
import com.kofax.mobilecapture.views.Main;

/// This class handles device specific issues.

public class DeviceSpecificIssueHandler {
    //! Used for device specific problem with HTC. when HTC is shut down and restarted while application is in use, instead of main, the last used activity is entry point.
	/**
	 * @return true if application has started with main as the first activity, false otherwise.
	 */		
	public boolean checkEntryPoint (Activity activity)
	{
		if (!Initializer.getInstance().getEntryPointMain())
		{
			new Initializer().initiateManagersAndServices(activity.getApplicationContext());
			UtilityRoutines mUtilRoutines = UtilityRoutines.getInstance();
			mUtilRoutines.showToast(activity, activity.getResources().getString(R.string.toast_unrecoverable_problem));
			
			Intent intent = new Intent(activity, Main.class);  
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(intent);  
			
			activity.finish();
			return true;
		}
		return false;
	}
}