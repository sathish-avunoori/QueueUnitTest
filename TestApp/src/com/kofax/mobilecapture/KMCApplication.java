// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.crittercism.app.Crittercism;
import com.kofax.mobilecapture.dbentities.DaoMaster;
import com.kofax.mobilecapture.dbentities.DaoSession;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

/// An application class consists of method to initialize the database.

public class KMCApplication extends Application {

	 public DaoSession daoSession;
	 public DaoMaster mDaomaster;
	 private final String DATABASE_NAME = "kmcappdb.db";
	 private boolean isOnHomeScreen;

	@Override
	public void onCreate() {
        Crittercism.initialize(getApplicationContext(), "55377cf98172e25e67906a0c");
        setupDatabase();
		super.onCreate();
	}

	 private void setupDatabase() {
		 	String appRootDir = UtilityRoutines.getInstance().getAppRootPath(getApplicationContext());
	        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, appRootDir + DATABASE_NAME, null);
	        SQLiteDatabase db = helper.getWritableDatabase();
	        mDaomaster = new DaoMaster(db);
	        daoSession = mDaomaster.newSession();
	    }
	 
	    public DaoSession getDaoSession() {
	        return daoSession;
	    }
	    
	    public DaoMaster getDaoMaster(){
	    	return mDaomaster;
	    }
	    
	    public void setupgradeFlag(boolean status){
	    	DaoMaster.Is_Upgrade_From_2_2 = status;
	    }

		/**
		 * @return the isOnHomeScreen
		 */
		public boolean isOnHomeScreen() {
			return isOnHomeScreen;
		}

		/**
		 * @param isOnHomeScreen the isOnHomeScreen to set
		 */
		public void setOnHomeScreen(boolean isOnHomeScreen) {
			this.isOnHomeScreen = isOnHomeScreen;
		}
	

}
