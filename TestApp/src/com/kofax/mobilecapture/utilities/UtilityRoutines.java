// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.utilities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.Field;
import com.kofax.kmc.kut.utilities.AppContextProvider;
import com.kofax.kmc.kut.utilities.Licensing;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.UserInformationEntity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// This class takes care of all the generic utility functions required throughout the application.

public class UtilityRoutines {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = UtilityRoutines.class.getSimpleName();
    private final String EVRS_LICENSE = "Ib-UgkOB2P[Nvzi$2DALGj,BcV&#,v94bp8bPl!!!UKD[4[7r5Wl$4dfd0&nqcvF?4dU08@f^!E#XY;h&!(R{60kNmD=fT,^IL!k";
	private String urlRegex = "\\b(https?|ftp|file|ldap)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]";

	// - Private data.
	/* SDK objects */
	/* Application objects */
	private PrefManager pUtils = null;
	/* Standard variables */
	private static volatile UtilityRoutines pSelf = null;
	private boolean isEmailIDRegistered = false;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes
	private UtilityRoutines() {
	}
	// - public getters and setters

	// - public methods
    //! The factory method.
	public static UtilityRoutines getInstance() {
		if (pSelf == null) {
			synchronized (UtilityRoutines.class) {
				if (pSelf == null) {
					pSelf = new UtilityRoutines();		
				}
			}
		}
		return pSelf;
	}

	/// Function to check if user has entered an email ID in setting screen.
	public boolean isEmailRegistered() {
		isEmailIDRegistered = false;

		if(pUtils == null) {
			pUtils = PrefManager.getInstance();
		}
		if (!pUtils.getCurrentEmail().equals("")) {
			isEmailIDRegistered = true;
		}
		return isEmailIDRegistered;
	}
	
	
	//! Check validity of license
			/**
			 * 
			 * @param context
			 * @return returns true if license is valid, false otherwise.
			 */
		public boolean checkKMCLicense(Context context) {

			String LicenseStr = EVRS_LICENSE;

			AppContextProvider.setContext(context);

			if (!LicenseStr.equals(null)) {

				ErrorInfo licenseInfo = Licensing.setMobileSDKLicense(LicenseStr);
				if (licenseInfo == ErrorInfo.KMC_EV_LICENSE_EXPIRED
						|| licenseInfo == ErrorInfo.KMC_EV_LICENSING) {
					Log.i(TAG, "InValid License Key !!! Number of days left "
							+ Licensing.getDaysRemaining());
					Toast.makeText(context,	context.getResources().getString(R.string.toast_use_valid_license),
							Toast.LENGTH_LONG).show();
				} else {
					Log.i(TAG, "Remaining Days: " + Licensing.getDaysRemaining());			
					return true;
				}
			}
			return false;
		}
	
	
	///Check the Application dependencies like email, internet, camera and SD card availability.
    /** This method is used to check the internet,camera,sdcard are available in device and given email is valid or not
     * 
     * @param context
     */
	
	public void checkApplicationDependencies(Context context) {
		if (checkInternet(context)) {
			Log.i(TAG, "Internet Connection - Yes");
		} else {
			Log.i(TAG, "Internet Connection - No");
		}

		if (checkCamera(context)) {
			Log.i(TAG, "Camera Avalible - Yes");
		} else {
			Log.i(TAG, "Camera Avalible - No");
		}

		if (checkSDcard()) {
			Log.i(TAG, "External Storage - Yes");
		} else {
			Log.i(TAG, "External Storage - No");
		}
	}

	///Get the current internet(wifi/data) status on device
	/**
	 * Check the current internet status
	 * @param context
	 * @return Returns the internet status
	 */
	
	public synchronized boolean checkInternet(Context context) {

		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI")) {
				if (ni.isConnected()) {
					haveConnectedWifi = true;
					//Log.i(TAG, "WIFI CONNECTION AVAILABLE");
				} else {
					//Log.i(TAG, "WIFI CONNECTION NOT AVAILABLE");
				}
			}
			if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
				if (ni.isConnected()) {
					haveConnectedMobile = true;
					//Log.i(TAG, "MOBILE INTERNET CONNECTION AVAILABLE");
				} else {
					//Log.i(TAG, "MOBILE INTERNET CONNECTION NOT AVAILABLE");
				}
			}
		}
		Constants.IS_APP_ONLINE = haveConnectedWifi || haveConnectedMobile;
		if(Globals.gAppModeStatus !=  Globals.AppModeStatus.FORCE_OFFLINEMODE){
			if(Constants.IS_APP_ONLINE){
				Globals.gAppModeStatus =  Globals.AppModeStatus.ONLINE_MODE;
			}else{
				Globals.gAppModeStatus =  Globals.AppModeStatus.OFFLINE_MODE;
			}
		}
		return Constants.IS_APP_ONLINE;
	}
	
	///Function to check if camera is available on device or busy with some other application
	/**
	 * 
	 * @return Returns the camera status
	 */
	public boolean checkCamera(Context context) {

		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
	}

	/// Function to check if sdcard storage available on device
	
	/**
	 * 
	 * @return Returns the sdcard storage status
	 */
	public boolean checkSDcard() {

		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageWriteable = false;
		}

		return mExternalStorageWriteable;
	}

	public void showToast(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/// Function to check if specified email string is valid.
	
	/**
	 * Validate the given String is valid email or not 
	 * @param emailTxt
	 * @return Returns the valid email status
	 */
	public Boolean validateEmail(String emailTxt) {
		Boolean valid = false;

		if ((emailTxt.length() > 0)
				&& emailTxt.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.]+")) {
			valid = true;
		} else {
			valid = false;
		}
		return valid;
	}

/*	public Boolean validateEmail(String emailTxt) {
		Boolean valid = false;

		if ((emailTxt.length() > 0)
				&& emailTxt.matches("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")) {
			valid = true;
		} else {
			valid = false;
		}
		return valid;
	}
*/
	
	///Get the ImageMimeType from specified image url
	
	/**
	 * This method used to get the SDK mimeType using url
	 * @param url
	 * @return Returns the mimeType
	 */
	public ImageMimeType getMimeType(String url) {
		ImageMimeType type = null;
		String strType = getMimeTypeString(url);
		type = getMimeTypeFromExtn(strType);

		return type;
	}

	///Get the mimetype from the specified image url
    
    /**
     * This method used to get the mimeType string(.jpg/.jpeg/.png etc) using url
     * @param url
     * @return Returns the mimetype extension
     */

	public String getMimeTypeString(String url) {		
		Log.i(TAG, "Enter:: getMimeTypeString");
		Log.i(TAG, "URL :: " + url);
		String strType = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url)
				.toLowerCase(Locale.getDefault());
		if (extension != null) {
			MimeTypeMap mime = MimeTypeMap.getSingleton();
			strType = mime.getMimeTypeFromExtension(extension);
		}

		if (strType != null) {
			strType = strType.substring(6); // every mimetype starts with
			// "image/" prefix, remove this
			// prefix and send only the mime
			// type.
		}

		Log.i(TAG, "MIME type string is =============> " + strType);
		return strType;
	}

	///Get the ImageMimeType from specified mimetype extension string.
    
    /**
     * This method used to get the SDK mimeType using mimetype extension
     * @param extn
     * @return Returns the SDK mimeType
     */
	
	public ImageMimeType getMimeTypeFromExtn(String extn) {
		ImageMimeType type = null;

		if (extn != null) {
			if (extn.equalsIgnoreCase(Constants.STR_EXTENSION_JEPG) || extn.equalsIgnoreCase(Constants.STR_EXTENSION_JPG)) {
				type = ImageMimeType.MIMETYPE_JPEG;
			} else if (extn.equalsIgnoreCase(Constants.STR_EXTENSION_PNG)) {
				type = ImageMimeType.MIMETYPE_PNG;
			} else if (extn.equalsIgnoreCase(Constants.STR_EXTENSION_TIFF)) {
				type = ImageMimeType.MIMETYPE_TIFF;
			} else {
				type = ImageMimeType.MIMETYPE_UNKNOWN;
			}
		} else {
			type = ImageMimeType.MIMETYPE_UNKNOWN;
		}
		return type;
	}
	
	///Get the formatted date string from specified date Object
	
	/**
	 * This method is used to get the formatted date string using date Object
	 * @param date
	 * @return Returns the Date format string
	 */

	public String getFormattedDate(Date date) {
		String strDateTime = null;

		strDateTime += date.getDay() + " " + date.getMonth() + " " + date.getYear() + " | " + date.getHours() + " " + date.getMinutes() ;

		Log.i(TAG, "day :: " + date.getDay());
		Log.i(TAG, "month :: " + date.getMonth());
		Log.i(TAG, "year :: " + date.getYear());
		Log.i(TAG, "hours :: " + date.getHours());
		Log.i(TAG, "minutes :: " + date.getMinutes());

		return strDateTime;
	}

	
	 ///Function to check if the specified name string is of photo by parsing the name string and checking if it contains '_photo' substring.
	/**
	 * 
	 * @param name
	 * @return Returns true if given string is photo
	 */
	
	
	public boolean isPhoto(String name) {
		boolean result = false;
		/* If name contains '_photo' appended, then its a photo else its document */
		result = name.contains("_photo.");
		name = null;
		return result;
	}

	///Get the application folder path
    /**
     * This method is used to get the application folder path.If app is a released version,
     * it forms root-dir by concatenating 'KofaxMobileCapture' to the storage location. The storage location is decided based on if the application is a released version. 
     * If its a release-version, the storage location is application internal memory location else its external storage directory location.
     * 
     * @param context
     * @return Returns the Application Directory path
     */
    
	public String getAppRootPath(Context context){
		String rootDir = null;
		if(Constants.RELEASE_VERSION) {
			rootDir = context.getFilesDir().toString();
		}
		else {
			rootDir = Environment.getExternalStorageDirectory().toString();
		}

		if(rootDir != null) {
			rootDir += Constants.APP_NAME;
		}
		return rootDir;
	}
	
	 ///Get the unique name for item using timeStamp
    /**
     * This method is used to get the unique name for items using the timeStamp
     * 
     * @return Returns the item name
     */

	public String createUniqueItemName() {
		Calendar calender = Calendar.getInstance();
		long offset = calender.get(Calendar.ZONE_OFFSET)
				+ calender.get(Calendar.DST_OFFSET);
		long time = (calender.getTimeInMillis() + offset) /*% (24 * 60 * 60 * 1000)*/;

		String timeStr = Long.toString(time);
		return (timeStr);
	}

	 ///Get the device model name
    /**
     * This method is used to get the device model name
     * 
     * @return Returns the model name
     */
	public String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		} else {
			return capitalize(manufacturer) + " " + model;
		}
	}

	 ///Save the Geo locations from sourceFileUrl to savedFileUrl and returns the geo location array
    /**
     * This method is used to save the geo location in savedFileUrl
     * 
     * @return Returns the geo location String array
     */
	public String[] saveImageGeoLocation(String savedFileUrl, String sourceFileUrl) {
		Log.e(TAG, "enter:: saveImageGeoLocation");
		String[] geoLocation = null;
		if(sourceFileUrl != null && savedFileUrl != null) {
			try {
				ExifInterface exif = new ExifInterface(sourceFileUrl);

				String lat = ExifInterface.TAG_GPS_LATITUDE;
				String lon = ExifInterface.TAG_GPS_LONGITUDE;
				String lat_ref = ExifInterface.TAG_GPS_LATITUDE_REF;
				String lon_ref = ExifInterface.TAG_GPS_LONGITUDE_REF;
				//get geo-location from the source gallery image
				String lat_data = exif.getAttribute(lat);
				String lon_data = exif.getAttribute(lon);
				String lat_ref_data = exif.getAttribute(lat_ref);
				String lon_ref_data = exif.getAttribute(lon_ref);
				if(lat_data != null && lon_data != null) {
					geoLocation = new String[4];
					//put the geo-location data into saved image object
					ExifInterface destinationImgInterface = new ExifInterface(savedFileUrl);
					destinationImgInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, lat_data);
					destinationImgInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lon_data);
					destinationImgInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, lat_ref_data);
					destinationImgInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lon_ref_data);
					destinationImgInterface.saveAttributes();
					geoLocation[0] = lat_data;
					geoLocation[1] = lon_data;
					geoLocation[2] = lat_ref_data;
					geoLocation[3] = lon_ref_data;
					Log.i(TAG, "lat_data  =====================> " + geoLocation[0]);
					Log.i(TAG, "lon_data =====================> " + geoLocation[1]);
					Log.i(TAG, "lat_ref  =====================> " + geoLocation[2]);
					Log.i(TAG, "lon_ref =====================> " + geoLocation[3]);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Log.e(TAG, "Exit:: saveImageGeoLocation");
		return geoLocation;
	}

	 ///Get the GPS coordinates from the geo locations array
    /**
     * This method is used to get the GPS coordinates from the geoLocation array
     * @param geoLocation
     * @return Returns the GPS coordinates
     */
	public String[] getGPSCoordinatesFromGeoLocation(String[] geoLocation) {
		Log.e(TAG, "enter:: getGPSCoordinatesFromGeoLocation");
		String[] gpsCoordinates = null;
		Float latitude, longitude;

		if(geoLocation != null) {
			String attrLATITUDE = geoLocation[0];
			String attrLONGITUDE = geoLocation[1];
			String attrLATITUDE_REF = geoLocation[2];
			String attrLONGITUDE_REF = geoLocation[3];

			Log.i(TAG, "attrLATITUDE  =====================> " + attrLATITUDE);
			Log.i(TAG, "attrLONGITUDE =====================> " + attrLONGITUDE);
			Log.i(TAG, "attrLATITUDE_REF  =====================> " + attrLATITUDE_REF);
			Log.i(TAG, "attrLONGITUDE_REF =====================> " + attrLONGITUDE_REF);
			
			if((attrLATITUDE !=null)
					&& (attrLATITUDE_REF !=null)
					&& (attrLONGITUDE != null)
					&& (attrLONGITUDE_REF !=null)) {
				if(attrLATITUDE_REF.equals("N")){
					latitude = convertToDegree(attrLATITUDE);
				}
				else{
					latitude = 0 - convertToDegree(attrLATITUDE);
				}

				if(attrLONGITUDE_REF.equals("E")){
					longitude = convertToDegree(attrLONGITUDE);
				}
				else{
					longitude = 0 - convertToDegree(attrLONGITUDE);
				}
				Log.i(TAG, "latitude  =====================> " + latitude);
				Log.i(TAG, "longitude =====================> " + longitude);
				gpsCoordinates = new String[2];
				gpsCoordinates[0] = latitude.toString();
				gpsCoordinates[1] = longitude.toString();
				Log.i(TAG, "gpsCoordinates[0]  =====================> " + gpsCoordinates[0]);
				Log.i(TAG, "gpsCoordinates[1] =====================> " + gpsCoordinates[1]);
			}
		}
		Log.e(TAG, "Exit:: getGPSCoordinatesFromGeoLocation");
		return gpsCoordinates;
	}

	///Send intent for offLine logout
	/***
	 * 
	 * @param activity   Current activity to send result and finish it. 
	 */
	public synchronized void offlineLogout(Activity activity){
		Intent returnIntent = new Intent();
		returnIntent.putExtra(Constants.STR_OFFLINE_TO_LOGIN, true);
		activity.setResult(Globals.ResultState.RESULT_OK.ordinal(), returnIntent);
		activity.finish();
	}

	public boolean checkForValidUrl(String url){
		Pattern urlPattern = Pattern.compile(urlRegex);
		Matcher matcher = urlPattern.matcher(url);
		if (!matcher.matches()) {
			return false;
		}
		return  true;
	}

	public void updateURLDetails(String url) {
		if(pUtils == null) {
			pUtils = PrefManager.getInstance();
		}
		if (url != null && !url.equalsIgnoreCase("") && !pUtils.isUsingKofax()) {
			String[] urlFrag = url.split("://");
			if (urlFrag[0] != null && urlFrag[0].contains("HTTPS") || urlFrag[0].contains("https")) {
				pUtils.putPrefValueBoolean(pUtils.KEY_USR_SSL, true);
			} else {
				pUtils.putPrefValueBoolean(pUtils.KEY_USR_SSL, false);
			}
			if (urlFrag.length > 1 && urlFrag[1] != null) {
				String[] secondLevelFrag = urlFrag[1].split(":");
				if (secondLevelFrag.length >= 2) {
					String hostname = secondLevelFrag[0];
					pUtils.putPrefValueString(pUtils.KEY_USR_HOSTNAME, hostname);
				} else {
					String[] hostNameFrag = secondLevelFrag[0].split("/");
					if (hostNameFrag[0] != null) {
						String hostname = hostNameFrag[0];
						pUtils.putPrefValueString(pUtils.KEY_USR_HOSTNAME, hostname);
					}
				}
				//extract port number
				if ((secondLevelFrag.length >= 2) && secondLevelFrag[1] != null) {
					String[] thirdLevelFrag = secondLevelFrag[1].split("/");
					if (thirdLevelFrag[0] != null) {
						String portnumber = thirdLevelFrag[0];
						pUtils.putPrefValueString(pUtils.KEY_USR_PORT, portnumber);
					}
				} else {
					pUtils.putPrefValueString(pUtils.KEY_USR_PORT, "");
				}
				//extract the server type (KFS/TA) from url and update the preferences accordingly. This is required to know the server type while logging in.
				if (url.contains("/KFS/")) {
					pUtils.putPrefValueString(pUtils.KEY_USR_SERVER_TYPE, Globals.serverType.KFS.name());
				} else if (url.contains("/TA/") || url.contains("/TotalAgility/")) {
					pUtils.putPrefValueString(pUtils.KEY_USR_SERVER_TYPE, Globals.serverType.KTA.name());
				}
			}
		}
	}

	public synchronized Bitmap getBitmapFromUrl(String filePath) {
		Bitmap bmp = null;
		ImageMimeType mime = getMimeType(filePath);
		if(mime == ImageMimeType.MIMETYPE_TIFF) { 
			Image tempImg = new Image(filePath, mime); 
			try {
				tempImg.imageReadFromFile();
				bmp = tempImg.getImageBitmap();
			} catch (KmcRuntimeException e) {
				e.printStackTrace();
			} catch (KmcException e) {
				e.printStackTrace();
			}
			tempImg = null;
		} else {
			InputStream is = null;
			BufferedInputStream bufferedInputStream = null;
			try {
				is = new FileInputStream(filePath);
				bufferedInputStream = new BufferedInputStream(is);
				bmp = BitmapFactory.decodeStream(bufferedInputStream);	
					if (is != null) {
						is.close();
					}
					if(bufferedInputStream!=null){
						bufferedInputStream.close();
					}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
			}
		}
		return bmp;
	}
	
	public void cleanup() {
		pUtils = null;
		pSelf = null;
	}

	// - private nested classes (more than 10 lines)

	// - private methods
	private String capitalize(String s) {
		if (s == null || s.length() == 0) {
			return "";
		}
		char first = s.charAt(0);
		if (Character.isUpperCase(first)) {
			return s;
		} else {
			return Character.toUpperCase(first) + s.substring(1);
		}
	}
	
	
	@SuppressLint("UseValueOf")
	private Float convertToDegree(String stringDMS) {
		Float result = null;

		String[] DMS = stringDMS.split(",", 3);

		String[] stringD = DMS[0].split("/", 2);
		Double D0 = Double.valueOf(stringD[0]);
		Double D1 = Double.valueOf(stringD[1]);
		Double FloatD = D0/D1;

		String[] stringM = DMS[1].split("/", 2);
		Double M0 = Double.valueOf(stringM[0]);
		Double M1 = Double.valueOf(stringM[1]);
		Double FloatM = M0/M1;

		String[] stringS = DMS[2].split("/", 2);
		Double S0 = Double.valueOf(stringS[0]);
		Double S1 = Double.valueOf(stringS[1]);
		Double FloatS = S0/S1;

		result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

		return result;
	};
	
	public long getAvailableMemorySize(){
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
		long megAvailable = bytesAvailable / (1024 * 1024);
		return megAvailable;
	}
	
	///Update the userInformation list
	public void updateUserInformationList(Context context, DatabaseManager dataBase,PrefManager prefUtils,DocumentManager docMgr) {
		UserInformationEntity userEntity = null;
		List<String> newList = null;

		List<UserInformationEntity> list = dataBase.getUserInformationFromDetails(context,
						getUser(),
						prefUtils.getCurrentHostname(),
						prefUtils.getCurrentServerType());
		 ArrayList<String> stringList =  docMgr.getDocTypeNamesList();
		 
		/*// assume oldList exists and has data in it.
		if (stringList != null) {
			newList = cloneList(stringList);
			if (prefUtils.isUsingKofax() && newList.size() > 0) {
				newList.remove(newList.size() - 1);
			}
		}*/

		if(list != null && list.size() > 0){
			 userEntity = list.get(0);			 
		}else{
			userEntity = new UserInformationEntity(); 
			userEntity.setUserName(getUser());
			userEntity.setHostName(prefUtils.getCurrentHostname());
			userEntity.setServerType(prefUtils.getCurrentServerType());						
		}
		if(stringList != null){
			userEntity.setDocumentTypes(formListToString(stringList));	
		}
		long ret =   dataBase.insertOrUpdateUserInformation(context, userEntity);
		if(ret > -1){
			dataBase.setUserInformationEntity(userEntity);
			insertOrUpdateProcessingTable(userEntity,newList,dataBase,context);
		}
		
		newList = null;
	}
	
	 ///Get the status of the application which running in foreGround or backGround.
	 public boolean isAppOnForeground(Context context) {
		    ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		    List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		    if (appProcesses != null) {
		    final String packageName = context.getPackageName();
		    for (RunningAppProcessInfo appProcess : appProcesses) {
		      if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
		        return true;
		      }
		    }
		    }
		    return false;
		  }
	
	
	@SuppressWarnings({ "hiding", "unchecked" })
	public <String extends Object> List<String> cloneList(List<String> list) {
	    return ((List<String>) ((ArrayList<String>) list).clone());
	}

	public boolean isLastLoggedInUser(PrefManager prefUtils){

		String lastLoggedUserServerType =  prefUtils.sharedPref.getString(prefUtils.KEY_APP_LAST_lOGGED_SERVER_TYPE, prefUtils.DEF_USR_SERVER_TYPE);
		String lastLoggedUserHostName =  prefUtils.sharedPref.getString(prefUtils.KEY_APP_LAST_lOGGED_HOSTNAME, prefUtils.DEF_USR_HOSTNAME);
		String lastLoggedUser = prefUtils.sharedPref.getString(prefUtils.KEY_APP_LAST_lOGGED_USER, prefUtils.DEF_USR_UNAME);

		String currentUser = getUser();

//		if(prefUtils.isUsingAnonymous() || prefUtils.isUsingAnonymousDemo())
//			currentUser = Constants.ANONYMOUS_LOGIN_ID;
//		else
//			currentUser = prefUtils.getCurrentUser().toString();

		if( (lastLoggedUserServerType.equals(prefUtils.getCurrentServerType().toString() ) )&&
				( lastLoggedUserHostName.equals(prefUtils.getCurrentHostname().toString() ) )&&
				( lastLoggedUser.equals(currentUser) ) ) {
			return true;
		}
		else {
			return false;
		}
	}

	public static Document cloneDocumentWithFieldValues(Document src){

		Document dest = new Document(src.getDocumentType(), src.getDocumentId());
		if(src != null && dest != null){
			List<Field> srcFields = src.getFields();
			List<Field> destFields = dest.getFields();
			if(srcFields != null && srcFields.size() > 0 && destFields != null && destFields.size() > 0 ){
				for(int srcCount = 0; srcCount < srcFields.size(); srcCount++){
					Field srcField = srcFields.get(srcCount);
					for(int destCount = 0; destCount < destFields.size(); destCount++){
						Field desField = destFields.get(destCount);
						if(srcField.getFieldType().getDisplayName().equalsIgnoreCase(desField.getFieldType().getDisplayName())){
							if(srcField.getValue() != null && !srcField.getValue().equalsIgnoreCase("")) {
								desField.updateFieldProperties(srcField.getValue(),desField.isValid(),desField.getErrorDescription());
								break;
							}
						}
					}
				}
			}
		}
		return dest;
	}

	private void insertOrUpdateProcessingTable(UserInformationEntity userEntity,List<String> list,DatabaseManager dataBase,Context context){
		if(null != list && list.size() > 0){
			for(int i = 0;i<list.size();i++){
				dataBase.insertProcessingParametersinDB(context,list.get(i));
			}
		}
	}
	
	private String formListToString(ArrayList<String> list){
		String docListString = "";
		for(int i = 0; i < list.size() ;i++){
			docListString = docListString + list.get(i) + Constants.KMC_STRING_SEPERATOR;
		}
		return docListString;
	}

	public String getUser() {
		PrefManager mPrefUtils = PrefManager.getInstance();
		String user;
		if(mPrefUtils.isUsingKofax()){
			if(mPrefUtils.isUsingAnonymousDemo()) {
				user = Constants.ANONYMOUS_LOGIN_ID;
			}else{
				user = mPrefUtils.getCurrentUser();
			}
		}else {
			if(mPrefUtils.isUsingAnonymous()){
				user = Constants.ANONYMOUS_LOGIN_ID;
			}else{
				user = mPrefUtils.getCurrentUser();
			}
		}
		return user;
	}

	public Bitmap getScaledBitmapFromFilepath(String filePath){
		try {

			Bitmap bmp = null;
			String modifiedFilePath = filePath;
			if(filePath.endsWith(".tif")){
				modifiedFilePath = filePath.replace(".tif",".jpg");
			}else if(filePath.endsWith(".tiff")){
				modifiedFilePath = filePath.replace(".tiff",".jpg");
			}

			File f = new File(modifiedFilePath);
			if(f.exists()){
				// Decode image size
				BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new FileInputStream(f), null, o);

				// The new size we want to scale to
				final int REQUIRED_SIZE = 70;

				// Find the correct scale value. It should be the power of 2.
				int scale = 1;
				while (o.outWidth / scale / 2 >= Constants.COLUMNWIDTH &&
						o.outHeight / scale / 2 >= Constants.COLUMNWIDTH) {
					scale *= 2;
				}

				// Decode with inSampleSize
				BitmapFactory.Options o2 = new BitmapFactory.Options();
				o2.inSampleSize = scale;
				bmp = BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
			} else{
				bmp = getBitmapFromUrl(filePath);
				bmp = Bitmap.createScaledBitmap(bmp,Constants.COLUMNWIDTH,Constants.COLUMNWIDTH,false);
			}
			return bmp;
		}catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}

	}

	public void saveScaledBitmapToFilepath(Bitmap bitmap, String filepath){
		if(filepath.endsWith(".tif")){
			filepath = filepath.replace(".tif",".jpg");
		}else if(filepath.endsWith(".tiff")){
			filepath = filepath.replace(".tiff",".jpg");
		}
		File file = new File(filepath);
		if(file.exists()){
			file.delete();
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(filepath);
			bitmap = Bitmap.createScaledBitmap(bitmap,Constants.COLUMNWIDTH,Constants.COLUMNWIDTH,false);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out); // bmp is your Bitmap instance
			// PNG is a lossless format, the compression factor (100) is ignored
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
