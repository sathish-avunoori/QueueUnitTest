// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.utilities;

import java.io.File;

/// A class defining all the parameters used internally in application.

public class Constants {

	/* Storage related constants */
	public static final Boolean RELEASE_VERSION = true;  ///Check Application data store in internal/external directory 
	public static boolean BACKGROUND_IMAGE_PROCESSING = true;
	public static boolean APP_STAT_ENABLED = true;
	public static boolean IS_APP_ONLINE = false;
	public static boolean IS_HELPKOFAX_FLOW = false;
	public static boolean IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = false;
	

	/* General constants*/
	public static final int MEMORY_OR_LOWER_END_DEVICES = 128; //App memory equal to or less than 128MB is considered as low end device
	
	public static final String APP_NAME = File.separator + "KofaxMobileCapture" + File.separator;
	public static final String ANONYMOUS_LOGIN_ID = "6416d2e4-Anon-4f";

	public static final int CAM_STABILITY_VALUE = 95;
	public static final int BYTES_TO_MB_VALUE = 1048576;   //Value for conversion from bytes to MB
	public static final String ITEM_DIRECTORY_NAME = "Items" + File.separator;

	public static final String TEMP_DIRECTORY_NAME = "Temp" + File.separator;
	public static final String TEMP_FILENAME = "temp.jpeg";

	public static final String FILENAME_SEPERATOR = "_";
	
	public static final String KMC_STRING_SEPERATOR = "!@�$%^^"; 
	public static final String KMC_STRING_SPLIT_SEPERATOR = "\\!\\@\\�\\$\\%\\^\\^";

	public static final String STR_EXTENSION_JEPG = "jpeg";
	public static final String STR_EXTENSION_JPG = "jpg";
	public static final String STR_EXTENSION_PNG = "png";
	public static final String STR_EXTENSION_TIFF = "tiff";
	public static final String STR_EXTENSION_TIF = "tif";
	
	public static final String STR_EMPTY = "";
	
	/* intent bundle constants */
	public static final String STR_TRUE = "true";
	public static final String STR_FALSE = "false";
	public static final String STR_DELETE = "delete";
	public static final String STR_DONE = "done";
	public static final String STR_VALIDATION = "Field_Validation";
	public static final String STR_IMAGE_COUNT = "image_count";
	public static final String STR_IMAGE_INDEX = "image_index";
	public static final String STR_IMAGE_TYPE= "image_type";
	public static final String STR_OFFLINE_TO_LOGIN = "app_offline_to_login";
	
	public static final String STR_MOBILE_DEVICE_EMAIL= "MobileDeviceEmail";

	public static final String STR_DO_BLUR_ILLUMINATION = "DoBlurAndIlluminationCheck";

	public static final String STR_EDITED_IMG_LOCATION = "edited_img_location";
	public static final String STR_CAPTURE_COUNT = "capture_count";
	public static final String STR_URL = "url";
	public static final String STR_QUICK_PREVIEW = "quick_preview";
	public static final String STR_IMG_SOURCE_TYPE = "image_source_type";
	public static final String STR_CHANGE_ITEM_TYPE = "change_item_type";
	public static final String STR_NEW_ITEM_INDEX = "new_item_index";
	public static final String STR_DOCUMENT_TYPE = "document_type";
	public static final String STR_PREVIEW_SCREEN_SUBMIT_CANCEL = "submit_cancel_from_preview";
	public static final String STR_ERROR_MESSAGE = "error_message";
	
	public static final String STR_IS_NEW_ITEM = "is_new_item";
	public static final String STR_CAPTURE_TYPE = "capture_type";
	public static final String STR_IS_FROM_PENDING = "is_from_pending";

	public static final String STR_ITEM_NAME = "item_name";
	public static final String STR_ITEM_TYPE = "item_type";
	public static final String STR_ITEM_INDEX = "item_index";
	public static final String STR_ITEM_ID = "item_id";
	public static final String STR_PAGE_ID = "page_id";
	
	public static final String STR_EMPTY_FIELD = "@fieldValueEmpty";
	public static final String STR_NO_FIELDS = "@noFields";

	public static final String STR_VISIBLE_FIELD_COUNT = "visible_field_count";

	public static final String STR_FRONT_SIDE = "Front Side";
	public static final String STR_PAGE_DETECTION = "Page Detection";
	public static final String STR_TETRAGON = "Tetragon";
	public static final String STR_BLURRY = "Blurry";
	public static final String STR_OVER_SATURATED = "Oversaturated";
	public static final String STR_UNDER_SATURATED = "Undersaturated";
	
	
	public static final String STR_PROCESS_STATUS = "process_status";
	public static final String STR_IMAGE_METADATA = "image_metadata";

	public static final String CURRENT_PHOTO_PATH = "current_photo_path";
	public static final String LAST_IMAGE_PATH = "last_image_path";
	
	public static final String EXTRACTION_RESULT = "extraction_result";

	/* broadcast-receiver constants */
	public static final String CUSTOM_INTENT_LOGIN_ERROR = "com.kofax.kmc.CUSTOM_INTENT_LOGIN_ERROR";
	public static final String CUSTOM_INTENT_OFFLINE_LOGIN = "com.kofax.kmc.CUSTOM_INTENT_OFFLINE_LOGIN";
	public static final String CUSTOM_INTENT_OFFLINE_LOGIN_ERROR = "com.kofax.kmc.CUSTOM_INTENT_OFFLINE_LOGIN_ERROR";
	public static final String CUSTOM_INTENT_OFFLINE_LOGOUT_TO_LOGIN = "com.kofax.kmc.CUSTOM_INTENT_OFFLINE_LOGOUT_TO_LOGIN";
	public static final String CUSTOM_INTENT_LOGIN_UPDATED = "com.kofax.kmc.CUSTOM_INTENT_LOGIN_UPDATED";
	public static final String CUSTOM_INTENT_IMAGE_PROCESSING_STARTED = "com.kofax.kmc.CUSTOM_INTENT_IMAGE_PROCESSING_STARTED";
	public static final String CUSTOM_INTENT_IMAGE_PROCESSED = "com.kofax.kmc.CUSTOM_INTENT_IMAGE_PROCESSED";
	public static final String CUSTOM_INTENT_IMAGE_CAPTURED = "com.kofax.kmc.CUSTOM_INTENT_IMAGE_CAPTURED";
	public static final String CUSTOM_INTENT_ITEM_SUBMITTED = "com.kofax.kmc.CUSTOM_INTENT_ITEM_SUBMITTED";
	public static final String CUSTOM_INTENT_ITEM_MODIFIED = "com.kofax.kmc.CUSTOM_INTENT_ITEM_MODIFIED";
	public static final String CUSTOM_INTENT_DOCTYPE_DOWNLOADED = "com.kofax.kmc.CUSTOM_INTENT_DOCTYPE_DOWNLOAD";
	/* Document Field constants*/
	public static final String MOBILE_DEVICE_EMAIL = "MobileDeviceEmail";
	
	/* Constants */
	public static final String STRING_PROCESSED_IMAGE_NAME = "_proc.";
	public static final String STRING_PHOTO_IMAGE_NAME = "_photo";
	public static final String PNG_EXTENSION = ".png";
	public static final String TIFF_EXTENSION = ".tiff";
	public static final String STR_HANDLER_THREAD = "HandlerThread";
	public static final String STR_SERVER_TYPE = "Servertype";
    public static final String STR_GIFTCARD_COUNT = "Giftcardcount";
    public static final String STR_GIFTCARD = "IsGiftcard";
    public static final String STR_RETAKE = "IsRetake";
    public static final String STR_ASSIST = "AssistKofax_";   //TODO:: Need to change text based on assist kofax.
	
	
	/*1.2 TO 2.1 Migratoin */
	public static final String CASELIST = "caselist";
	public static final String CASEBASE_DIR = "caseBaseDirectory";
	public static final String CASE_NAME = "caseName";
	public static final String CASE_TYPE = "caseTypeName";
	public static final String CASE_FILE = "case_file";
	
	//network change listener
	public static NetworkChangedListener NETWORK_CHANGE_LISTENER ;
	
	//Low memory show alert values
	public static final long LAUNCH_APP_MEMORY = 100;
	public static final long CAPTURE_LAUNCH_MEMORY = 50;

	public static int COLUMNWIDTH = 0;
}
