// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager.ProcessingStates;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.sdk.kmc.model.PendingCaseSummary;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

/// Object to handle all disk related methods in application.

public class DiskManager {
	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = DiskManager.class.getSimpleName();

	// - Private data.
	/* SDK objects */
	/* Application objects */
	private PrefManager mPrefUtils = null;
	private UtilityRoutines mUtilRoutinesObj = null;
	private DatabaseManager mDBManager = null;
	/* Standard variables */
	private static volatile DiskManager pSelf = null;
	private ArrayList<DirDetails> mDirDetailsArray;	//TODO: check if hashtable can be used instead of arraylist
	private Context mContext = null;
	private String mAppRootDir = null;
	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes
	private DiskManager(Context context) {
		mPrefUtils = PrefManager.getInstance();
		mUtilRoutinesObj = UtilityRoutines.getInstance();
		mDBManager = DatabaseManager.getInstance();
		mContext = context;

		mAppRootDir = mUtilRoutinesObj.getAppRootPath(mContext);
	}

	// - public getters and setters
	//! The factory method.
	/**
        This method returns a singleton object of DiskManager.
	 */

	public static DiskManager getInstance(Context context) {
		if (pSelf == null) {
			synchronized (DiskManager.class) {
				if (pSelf == null) {
					pSelf = new DiskManager(context);		
				}
			}
		}
		return pSelf;
	}

	// - public methods

	//! Creates root and user directory(if does not exists already) upon successful login.
	public void createUserDirectory() {
		//create root directory if not present already
		//File folder = new File(mAppRootDir);

		// check if directoy for the logged in user is available, create one if not available.
		// user directory will be created at the path ExternalStorageDirectory/KofaxMobileCapture/<user_name>
		File userFolder = new File(mAppRootDir + mPrefUtils.getCurrentServerType() + File.separator + mPrefUtils.getCurrentHostname() + File.separator + mUtilRoutinesObj.getUser());

		Log.i(TAG, "mPrefUtils.getCurrentServerType() => " + mPrefUtils.getCurrentServerType());
		Log.i(TAG, "mPrefUtils.getCurrentUser() => " + mUtilRoutinesObj.getUser());
		Log.i(TAG, "userFolder path  => " + userFolder.getAbsolutePath());

		if (!userFolder.exists()) {
			boolean result = userFolder.mkdirs();
			Log.i(TAG, "userFolder.mkdirs result => " + result);
		}else {
			Log.i(TAG, "User folder already exists.!");
		}
	}

	//! Saves specified image object on to disk at a location.
	/**
	 * Function save image at a path constructed using using server type, hostname, user and item name and save image at that location on disk. 
	 * Once image is saved, also invokes method to create a new page in database for the saved image.
	 * 
	 * If there is not mime type provided for the image, it will be saved as a jpeg image on disk.
	 * 
	 * @return newly created pageID if image was saved successfully, null otherwise. 
	 * @throws KmcRuntimeException
	 * @throws KmcException
	 */
	public synchronized  long saveImageToDisk(Image imgObj, String mimeType, String itemName,
			int imgSelectionType) throws KmcRuntimeException, KmcException {
		File dest;
		String processedImgPath = null;
		long pageId = -1;

		String unprocessedImgUrl = getStorageLocationForImage(itemName, imgSelectionType);
		if(mimeType == null) {
			mimeType = Constants.STR_EXTENSION_JEPG;
		}

		if(imgSelectionType == Globals.ImageType.DOCUMENT.ordinal()) {
			processedImgPath = unprocessedImgUrl + "_proc." + "tiff";
		}
		unprocessedImgUrl += "." + mimeType;

		dest = new File(unprocessedImgUrl);
		Image pImg = imgObj;

		pImg.setImageFilePath(dest.getAbsolutePath().toString());

		CompressFormat format = CompressFormat.JPEG;
		ImageMimeType type = ImageMimeType.MIMETYPE_JPEG;
		if (mimeType != null && mimeType != "") {
			type = mUtilRoutinesObj
					.getMimeTypeFromExtn(mimeType);
			pImg.setImageMimeType(type);
		}else{
			pImg.setImageMimeType(ImageMimeType.MIMETYPE_JPEG);
		}

		if(type == ImageMimeType.MIMETYPE_PNG){
			format = CompressFormat.PNG;
		}

		boolean res = saveImageToLocation(dest.getAbsolutePath().toString(), format, imgObj.getImageBitmap());

		if(res) {
			pageId = mDBManager.insertPageinDB(mContext, imgSelectionType, unprocessedImgUrl, processedImgPath);
		}
		else {
			Log.e(TAG, "Error occured during writing image to file");
			unprocessedImgUrl = null;
		}

		return pageId;
	}

	//! Construct a complete image url to store image on disk.
	/**
	 * Function to create a image name using unique timestamp and if the image type os 'photo' then appending timestamp string it with _photo. 
	 * The image name is then appended to a url created using server type, hostname, user and item name, to form a complete url indicating image's location on disk. 
	 * 
	 * The image name is created without imaeg extension.
	 * 
	 * @return newly formed image name with its complete url indicating its location on disk. 
	 */
	public String getStorageLocationForImage(String itemName, int imgSelectionType) {
		String urlWOExtn = getParentItemPathForCurrentUser() + itemName;

		File fp = new File(urlWOExtn);
		if (!fp.exists()) {
			fp.mkdirs();
			addToItemsDetailsList(urlWOExtn);
		}

		Calendar calender = Calendar.getInstance();
		long offset = calender.get(Calendar.ZONE_OFFSET)
				+ calender.get(Calendar.DST_OFFSET);
		long time = (calender.getTimeInMillis() + offset)
				% (24 * 60 * 60 * 1000);

		// if image is of type 'photo', save it with string '_photo'
		// appended to the name.
		// This is to make sure photos not getting picked for image
		// processing later.
		if (imgSelectionType == Globals.ImageType.PHOTO.ordinal()) {
			urlWOExtn += File.separator + time
					+ Constants.STRING_PHOTO_IMAGE_NAME;
		} else {
			urlWOExtn += File.separator + time;
		}
		return urlWOExtn;
	}

	//! Create and save image file using specified bitmap at the specified location on disk.
	/**
	 * @param bmp
	 * @param url
	 * @throws IOException
	 * @throws KmcException 
	 * @throws KmcRuntimeException 
	 */
	public synchronized void saveImageAtLocation(Bitmap bmp, String url) throws IOException, KmcRuntimeException, KmcException{
		File fp = new File(url);
		if (fp.exists()) {
			fp.delete();
		}
		if (bmp != null) {
			String extn = mUtilRoutinesObj.getMimeTypeString(url);
			Bitmap.CompressFormat format = null;
			if (extn != null) {
				if (extn.equalsIgnoreCase(Constants.STR_EXTENSION_JEPG) || extn.equalsIgnoreCase(Constants.STR_EXTENSION_JPG)) {
					format = Bitmap.CompressFormat.JPEG;
				} else if (extn.equalsIgnoreCase(Constants.STR_EXTENSION_PNG)) {
					format = Bitmap.CompressFormat.PNG;
				}
			}
			else {
				format = Bitmap.CompressFormat.JPEG;
			}

			if(extn.equalsIgnoreCase(Constants.STR_EXTENSION_TIFF) || extn.equalsIgnoreCase(Constants.STR_EXTENSION_TIF)){
				UtilityRoutines.getInstance().saveScaledBitmapToFilepath(bmp, url);
				writeBitmapToTiff(bmp, url);
			}
			else {			
				fp = new File(url);
				FileOutputStream fOut = new FileOutputStream(fp);

				bmp.compress(format, 100, fOut);
				fOut.flush();
				fOut.close();
			}
		}
	}
	
	private ErrorInfo writeBitmapToTiff(Bitmap bmp, String path) throws KmcRuntimeException, KmcException {
		Image pImg = new Image(bmp);
		pImg.setImageFilePath(path);
		pImg.setImageMimeType(ImageMimeType.MIMETYPE_TIFF);
		ErrorInfo err = pImg.imageWriteToFile();
		clearImage(pImg);
		return err;
	}

    //! Saves bitmap from specified image object at a temporary location on disk.
	/**
	 * A directory with name Temp is created at the application root path and bitmap is saved as temp.jpeg.
	 * 
	 * @return Image url of the temporary location of saved image.
	 */
	public String saveImageToTempLocation(Image imgObj) {
		String tempDirPath =  mAppRootDir + Constants.TEMP_DIRECTORY_NAME;
		String url  = tempDirPath + Constants.TEMP_FILENAME;

		File fp = new File(tempDirPath);
		if (!fp.exists()) {
			fp.mkdir();
		}
		else {
			clearTempLocation();
		}

		Bitmap bmp = imgObj.getImageBitmap();
		try {
			fp = new File(url);
			FileOutputStream fOut = new FileOutputStream(fp);
			bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		fp = null;
		return url;
	}

	//! Constructs a url of temporary location based on predefined location on disk.
	/**
	 * @return Url of predefined temporary location. 
	 */
	public String getTempDiskImageLocation() {
		String url  = mAppRootDir + Constants.TEMP_DIRECTORY_NAME + Constants.TEMP_FILENAME;
		return url;
	}

	//! Deletes temp directory.
	public void deleteTempLocation() {
		String tempDirPath =  mAppRootDir + Constants.TEMP_DIRECTORY_NAME;

		File fp = new File(tempDirPath);
		if (fp.exists()) {
			clearTempLocation();
			fp.delete();
			fp = null;
		}
	}

	//! Update the image count for specified item.
	/**
	 * Item count can be changed when images are added/deleted from an item.
	 */
	public void updateItemImgCountInDetailsList(String itemName, int count) {
		if(mDirDetailsArray == null){
			return;
		}
		else {
			int i = 0;
			int arrSize = mDirDetailsArray.size();
			for (;i<arrSize; i++) {
				//match current dir name
				if ((mDirDetailsArray.get(i).getFilename() != null) && (mDirDetailsArray.get(i).getFilename().equals(itemName))) {
					// if name found, replace it with new name
					mDirDetailsArray.get(i).setCount(count);
					break;
				}
			}
		}
	}

	//! Delete all the images and the item directory from disk for the specified item.
	public void deleteItemFromDisk(String item_name, int index){
		String fileUrl = getParentItemPathForCurrentUser() + item_name;

		File dir = new File(fileUrl);
		if (dir.isDirectory()) {
			//delete all images saved inside item-folder before deleting item
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				new File(dir, children[i]).delete();
			}
			//delete item
			new File(fileUrl).delete();

			//update the arrays as well
			if(mDirDetailsArray == null){
				getExistingItemsDetailsList();
			}
			if((mDirDetailsArray != null) && (index < mDirDetailsArray.size())) {
				mDirDetailsArray.remove(index);
			}
		}
	}

	//! Delete all the images and the item directory from disk for the specified item.
	public void deleteItemFromDisk(String item_name){
		int index = -1;
		int i = 0;
		String fileUrl = getParentItemPathForCurrentUser() + item_name;

		File dir = new File(fileUrl);
		if (dir.isDirectory()) {
			//delete all images saved inside item-folder before deleting item
			String[] children = dir.list();
			for (i = 0; i < children.length; i++) {
				new File(dir, children[i]).delete();
			}
			//delete item
			new File(fileUrl).delete();
			if(mDirDetailsArray == null){
				getExistingItemsDetailsList();
			}
			if(null != mDirDetailsArray && mDirDetailsArray.size()>0){
				for (i = 0; i < mDirDetailsArray.size(); i++) {
					if(mDirDetailsArray.get(i).getFilename().equals(item_name)) {
						//update the arrays as well
						mDirDetailsArray.remove(i);
						index = i;
						break;
					}
				}
			}
		}

		if(index == -1){
			ItemEntity item = mDBManager.getItemEntity();
			if(item.getItemName().equals(item_name)){
				index = item.getItemId().intValue();
			}
		}
	}

	//! Deletes image present at specified location.
	public void deleteImageFromDisk(String filepath){
		if(filepath != null) {
			File file = new File(filepath);
			if(file.exists()){
				file.delete();
			}
			file = null;
		}

		//To delete thumbnail images
		if(filepath != null){
			if(filepath.endsWith(".tif")){
				filepath = filepath.replace(".tif",".jpg");
			}else if(filepath.endsWith(".tiff")){
				filepath = filepath.replace(".tiff",".jpg");
			}

			File file = new File(filepath);
			if(file.exists()){
				file.delete();
			}
			file = null;
		}
	}

	//! Converts object(Document) to byte array.
	/**
	 * 
	 * @return converted byte array.
	 */
	public byte[] documentToByteArray(Object object){
		byte[] bytesArray = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(object);
			bytesArray = bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return bytesArray;
	}

	//! Converts byte array to object.
	/**
	 *@return Converted object.
	 */
	public Object byteArrayToDocument(byte[] arr){
		Object o = null;
		ByteArrayInputStream bis = new ByteArrayInputStream(arr);
		ObjectInput in = null;
		try {
			in = new ObjectInputStream(bis);
			o = in.readObject(); 
		} catch (StreamCorruptedException e) {
			o = null;
			e.printStackTrace();
		} catch (IOException e) {
			o = null;
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			o = null;
			e.printStackTrace();
		} finally {
			try {
				bis.close();
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return o; 
	}
	
	/**
	 * Method is to import all pending cases from KMC 1.2 to items of KMC 2.1
	 */
	public void retrieve1o2data(String rootDir) {
		File oldCaseDir = new File(rootDir);
		if(null != oldCaseDir){
			File[] filesList = oldCaseDir.listFiles();
			if(null != filesList){
				if(filesList.length >0){
					for(int i = 0; i< filesList.length; i++){
						if(Constants.CASELIST.equals(filesList[i].getName())){
							try {
								ObjectInputStream ois = null;
								ois = new ObjectInputStream(new FileInputStream(filesList[i].getAbsoluteFile()));
								Object obj = null;

								try {

									while ((obj = ois.readObject()) != null) {
										if (obj instanceof PendingCaseSummary) {
											PendingCaseSummary  openCase = (PendingCaseSummary) obj;
											JSONObject jObject = openCase.toJSON();
											try {

												File caseImagesDir = new File(oldCaseDir.getAbsolutePath()+File.separator+jObject.getString(Constants.CASEBASE_DIR));
												File itemDirectory = new File(getParentItemPathForCurrentUser() + jObject.getString(Constants.CASEBASE_DIR));
												if(null != caseImagesDir && caseImagesDir.exists()){
													if(null != itemDirectory && !itemDirectory.exists()){
														FileUtils.forceMkdir(itemDirectory);

														ItemEntity item = null;
														item = new ItemEntity();
														item.setItemName(jObject.getString(Constants.CASE_NAME));
														item.setItemTypeName(jObject.getString(Constants.CASE_TYPE));
														item.setItemCreatedTimeStamp(new Date());
													
														item.setServerId(mPrefUtils.getCurrentServerType());
														item.setHostname(mPrefUtils.getCurrentHostname());
														item.setUserId(mUtilRoutinesObj.getUser());

														long itemIDnum = mDBManager.insertOrUpdate(mContext, item);
														if(itemIDnum != -1){
															if(null != caseImagesDir && caseImagesDir.exists()){
																File[] imageList = caseImagesDir.listFiles();
																if(null != imageList && imageList.length>0){
																	for(int k = 0; k < imageList.length; k++){
																		if(!(Constants.CASE_FILE.equalsIgnoreCase(imageList[k].getName())) && imageList[k].getName().contains("-raw.png")){
																			String[] s = (imageList[k].getName()).split("-");
																			File proceImageFile = new File(caseImagesDir.getAbsolutePath()+File.separator+s[0]+"-proc.png");

																			PageEntity page = new PageEntity();
																			page.setItemEntity(item);
																			page.setItemId(item.getItemId());
																			page.setDate(new Date());
																			Integer intObj = Integer.valueOf(mDBManager.getAllPagesCountForItem(mContext,item.getItemId()) + 1);
																			page.setSequenceNumber(intObj);

																			String outputPath = new File(itemDirectory.getAbsolutePath()+"/"+s[0]+".jpg").getAbsolutePath();
																			if(null != proceImageFile && proceImageFile.exists()){
																				outputPath = new File(itemDirectory.getAbsolutePath()+"/"+s[0]+".jpg").getAbsolutePath();
																				page.setImageType(Globals.ImageType.DOCUMENT.name());	
																				page.setImageFilePath(outputPath);
																				page.setProcessedImageFilePath(itemDirectory.getAbsolutePath()+"/"+s[0]+"_proc.tiff");
																			}else{
																				outputPath = new File(itemDirectory.getAbsolutePath()+"/"+s[0]+"_photo.jpg").getAbsolutePath();
																				page.setImageType(Globals.ImageType.PHOTO.name());
																				page.setImageFilePath(outputPath);
																				page.setProcessedImageFilePath("");
																			}

																			try {
																				FileOutputStream fileOutStr = new FileOutputStream(outputPath);
																				BufferedOutputStream bufOutStr = new BufferedOutputStream(fileOutStr);
																				File imgFile = new File(caseImagesDir.getAbsolutePath()+"/"+s[0]+"-raw.png");
																				Bitmap jpegBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

																				jpegBitmap.compress(CompressFormat.JPEG, 100, bufOutStr);
																				bufOutStr.flush();
																				bufOutStr.close();
																			} catch (FileNotFoundException exception) {
																				Log.e("debug_log", exception.toString());
																			} catch (IOException exception) {
																				Log.e("debug_log", exception.toString());
																			}
																			page.setProcessingStatus((long)ProcessingStates.UNPROCESSED.ordinal());
																			mDBManager.insertOrUpdatePage(mContext, page);
																		}
																	}
																}
															}
														}
													}
													FileUtils.deleteDirectory(caseImagesDir);
												}
											} catch (JSONException e) {
												e.printStackTrace();
											}
										}
									}
									ois.close();
								} 
								catch (ClassNotFoundException e) {
									e.printStackTrace();
								}
							} catch (EOFException e) {
								//e.printStackTrace();
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							try {
								FileUtils.forceDelete(filesList[i]);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	

	//! Removes all the contents of DirDetails array.
	public void reset() {
		if (mDirDetailsArray != null) {
			for (int i = 0; i < mDirDetailsArray.size(); i++) {
				mDirDetailsArray.remove(i);
				mDirDetailsArray.clear();
			}
			mDirDetailsArray = null;
		}
	}

	public void cleanupPartiallyProcessedImages() {
		//get list of all items from DB
		if(mDBManager != null){
		List<ItemEntity> itemEntityList = mDBManager.getAllItems(mContext);
		if(itemEntityList != null && itemEntityList.size() > 0) {
			for(int i=0; i<itemEntityList.size(); i++) {
				Long itemId = itemEntityList.get(i).getItemId();
				List<PageEntity> pages = mDBManager.getPages(mContext, itemId, Globals.ImageType.DOCUMENT.name(), ProcessingStates.PROCESSING);
				if(pages!= null) {
					for(int j=0; j<pages.size(); j++) {
						pages.get(j).setProcessingStatus((long)ProcessingStates.UNPROCESSED.ordinal());
						pages.get(j).update();
						deleteImageFromDisk(pages.get(j).getProcessedImageFilePath());
					}
				}
				else {
					Log.d(TAG, "Currently no image is being processed");
				}
			}
		}
		}
	}
	
	/**
	 * Clean up all allocated parameters of the DiskManager object.
	 */
	public void cleanup() {
		clearExistingItemDetailsList();
		mPrefUtils = null;
		mUtilRoutinesObj = null;
		mDirDetailsArray = null;
		mDBManager = null;
		mContext = null;
		pSelf = null;
	}

	// - private nested classes (more than 10 lines)
	private class DirDetails {
		private String filename;
		private String path;
		private Date time;
		private int imgCount;

		/**
		 * @return the time
		 */
		private Date getTime() {
			return time;
		}

		/**
		 * @param time
		 *            the time to set
		 */
		private void setTime(Date time) {
			this.time = time;
		}

		/**
		 * @return the imgCount
		 */
		private int getCount() {
			return imgCount;
		}

		/**
		 * @param imgCount
		 *            the imgCount to set
		 */
		private void setCount(int imgCount) {
			this.imgCount = imgCount;
		}

		/**
		 * @return the url
		 */
		@SuppressWarnings("unused")
		private String getItemLocation() {
			return path;
		}

		/**
		 * @param url
		 *            the url to set
		 */
		private void setItemLocation(String url) {
			this.path = url;
		}

		/**
		 * @return the filename
		 */
		private String getFilename() {
			return filename;
		}

		/**
		 * @param filename the filename to set
		 */
		private void setFilename(String filename) {
			this.filename = filename;
		}
	}

	// - private methods
	private FilenameFilter unprocessedImagesFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			if (filename.contains(Constants.STRING_PROCESSED_IMAGE_NAME)) {
				return false;
			} else {
				return true;
			}
		}
	};


	private ArrayList<DirDetails> getExistingItemsDetailsList() {
		if (mDirDetailsArray == null) {
			mDirDetailsArray = createExistingItemsDetailsList();
		}
		return mDirDetailsArray;
	}

	/**
	 * Function to add a new item and its details to itemDetails list.
	 * @param dirUrl
	 */
	private void addToItemsDetailsList(String dirUrl) {
		if (mDirDetailsArray == null) {
			mDirDetailsArray = createExistingItemsDetailsList();
		}
		else {
			// get array of (item)files contained in the parent directory
			File fp = new File(dirUrl);

			if (fp != null) {

				int lastIndex = mDirDetailsArray.size();
				mDirDetailsArray.add(lastIndex, new DirDetails());

				mDirDetailsArray.get(lastIndex).setFilename(fp.getName());
				mDirDetailsArray.get(lastIndex).setItemLocation(dirUrl);
				// get count of unprocessed images in each item
				mDirDetailsArray.get(lastIndex).setCount(fp
						.listFiles(unprocessedImagesFilter).length); // get
				// unprocessed image count 
				mDirDetailsArray.get(lastIndex).setTime(new Date(fp
						.lastModified()));
			}
		}
	}

	private ArrayList<DirDetails> createExistingItemsDetailsList() {
		clearExistingItemDetailsList();

		// get logged in user name to retrieve complete url of the existing item
		String itemUrl = getParentItemPathForCurrentUser();
		Log.i(TAG, "itemUrl ==> " + itemUrl);

		// Open parent directory containing all item
		File itemDir = new File(itemUrl);

		// get array of (item)files contained in the parent directory
		File[] filesList = itemDir.listFiles();

		//mDirDetailsArray = null;
		if (filesList != null && filesList.length > 0) {
			mDirDetailsArray = new ArrayList<DiskManager.DirDetails>(filesList.length);
			for (int i = 0; i <filesList.length; i++) {
				mDirDetailsArray.add(i, new DirDetails());
				mDirDetailsArray.get(i).setFilename(filesList[i].getName());
				mDirDetailsArray.get(i).setItemLocation(itemUrl
						+ filesList[i].getName());
				// get count of unprocessed images in each item
				mDirDetailsArray.get(i).setCount(filesList[i]
						.listFiles(unprocessedImagesFilter).length); // get
				// unprocessed image count in each item directory
				mDirDetailsArray.get(i).setTime(new Date(filesList[i]
						.lastModified()));

				Log.i(TAG, "ImageCount ==> " + mDirDetailsArray.get(i).getCount());
				Log.i(TAG, "Time ==> " + mDirDetailsArray.get(i).getTime());
			}
		}
		itemDir = null;

		return mDirDetailsArray;
	}

	/**
	 * Function to construct an url string for root item directory where existing items of the current user are stored.
	 * @return
	 */
	public String getParentItemPathForCurrentUser() {
		String parentItemPath = null;

		parentItemPath = mAppRootDir 
				+ mPrefUtils.getCurrentServerType() + File.separator
				+ mPrefUtils.getCurrentHostname() + File.separator
				+ mUtilRoutinesObj.getUser() + File.separator
				+ Constants.ITEM_DIRECTORY_NAME;

		return parentItemPath;
	}

	/*	private String constructFileUrl(String itemName) {
		String fileUrl = null;

		fileUrl = getParentItemPathForCurrentUser() + itemName;

		return fileUrl;
	}
	 */
	private void clearExistingItemDetailsList() {
		if (mDirDetailsArray != null) {
			for (int i = 0; i < mDirDetailsArray.size(); i++) {
				mDirDetailsArray.remove(i);
			}
			mDirDetailsArray = null;
			System.gc();
		}
	}

	private void clearTempLocation() {
		String tempDirPath =  mAppRootDir + Constants.TEMP_DIRECTORY_NAME;
		File fp = new File(tempDirPath + Constants.TEMP_FILENAME);
		if (fp.exists()) {
			fp.delete();
			fp = null;
		}
	}

	private void clearImage(Image img) {
		if (img != null) {
			ErrorInfo errinfo = img.imageClearBitmap();
			Log.d(TAG, "clear bitmap errinfo::" + errinfo.name());
			try {
				img.imageClearFileBuffer();
			} catch (KmcException e) {
				e.printStackTrace();
			}
			img = null;
		}
	}

	private boolean saveImageToLocation(String filePath, CompressFormat format, Bitmap bitmap){
		FileOutputStream out = null;
		boolean result = false;
		try {
			out = new FileOutputStream(filePath);
			bitmap.compress(format, 100, out); // bmp is your Bitmap instance
			// PNG is a lossless format, the compression factor (100) is ignored
			result = true;
		} catch (Exception e) {
			result = false;
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

		return result;
	}
}
