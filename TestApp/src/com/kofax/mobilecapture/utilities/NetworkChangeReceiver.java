package com.kofax.mobilecapture.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class NetworkChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		boolean isConnected = UtilityRoutines.getInstance().checkInternet(context);
		if(Constants.NETWORK_CHANGE_LISTENER != null){
			Constants.NETWORK_CHANGE_LISTENER.onNetworkChanged(isConnected);
		}
	}

}
