// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.kut.utilities.error.ErrorInfo;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager.ProcessingStates;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

/// The ImageProcessingQueueManager takes care of queueing up given document and processing all the unprocessed images in it.

public class ImageProcessQueueManager {
	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = ImageProcessQueueManager.class.getSimpleName();

	// - Private data.
	private static volatile ImageProcessQueueManager pSelf = null;

	/* SDK objects */

	/* Application objects */
	PageEntity mPageEntity = null;
	ImageProcessManager mProcessMgr = null;
	DatabaseManager mItemDBMgr = null;
	DiskManager mDiskMgr = null;
	DocumentManager mDocMgr = null;
	DatabaseManager mDBMgr = null;
	UtilityRoutines mUtilRoutines = null;

	/* Standard variables */
	private Context mContext = null;
	private List<ItemEntity> itemQueue = null;
	private Handler mHandler = null;
	private boolean isQueueBusy = false;
	private boolean isQueueStopped = true;
	private boolean isQueuePaused = false; 
	private boolean isQueueHalted = false;

	private Long mLastItemId = -1L;
	private Long mLastPageId = -1L;
	private String mCurrentInputImagePath = null;
	private Runnable mRunnable = null;
	private Handler mThreadHandler = null;
	private Handler mCallerHandler = null;
	private HandlerThread mHandlerThread = null; 

	// - public constructors
	private ImageProcessQueueManager(Context context) {
		mContext = context;
		initSelf();
	}

	// - private constructors
	// - Private constructor prevents instantiation from other classes
	// - public getters and setters

	// - public methods
    //! The factory method.
	public synchronized static ImageProcessQueueManager getInstance(Context context) {
		if(pSelf == null) {
			synchronized (ImageProcessQueueManager.class) {
				if(pSelf == null) {
					pSelf = new ImageProcessQueueManager(context);		
				}
			}
		}
		return pSelf;
	}

    //! Adds an itemEntity into queue.
	/**
	 * The specified itemEntity is added into the first position of the queue. If the queue is already present in queue, and its not in the first position, it is pushed to first position.
	 * This is done so as to give higher priority to the currently added itemEntity for its images to get processed.
	 * Once itemEntity is added, queue is started.
	 * 
	 * @param itemEntity
	 */
	public void addItemToQueue(ItemEntity itemEntity) {
		// add itemId if not exist already in list
		if (itemQueue.size() > 0) {
			if(itemQueue.get(0).getItemId().equals(itemEntity.getItemId())) {
				// if item is already present at first index in queue.
				Log.i(TAG, "item is already present at first index in queue... returning.: isQueueStopped" + isQueueStopped);
			}
			else {
				//start loop from second element
				for (int i = 1; i < itemQueue.size(); i++) {
					if (itemQueue.get(i).getItemId().equals(itemEntity.getItemId())) {
						// if item is present somewhere but first index in list,
						// remove it from that index to add it at first index later.
						itemQueue.remove(i);
					}
				}
				itemQueue.clear();	//--Added recently to prevent processing queue from being used for the unopened items and other foreground tasks can be performed smoothly.
				itemQueue.add(0, itemEntity);
				Log.i(TAG, "Added in itemQueue :: " + itemQueue.size());
			}
		}
		else {
			itemQueue.add(0, itemEntity);
			Log.i(TAG, "Added in itemQueue :: " + itemQueue.size());
		}
		if(isQueueStopped) {
			startQueue();
		}
	}

    //! Cancels ongoing image processing and pauses queue until resumed again using resumeQueue() method. 
	/**
	 * If queue is successfully paused, method sends MESSAGE_PROCESS_QUEUE_PAUSED message back to caller using handler passed as a parameter. 
	 * If not, the MESSAGE_PROCESS_QUEUE_PAUSED message will be sent back once queue receives MESSAGE_IMAGE_PROCESS_CANCELLED message in its own message handler.
	 * 
	 * @param handler
	 * 
	 * @return true if queue is already free, false otherwise.
	 */
	public boolean pauseQueue(Handler handler) {
		mCallerHandler = handler;
		boolean processCancelled = true;
		if (isQueueBusy) {
			processCancelled = mProcessMgr.cancelProcess();
			isQueuePaused = true;
		}

		if(!isQueueBusy || processCancelled) {
			mProcessMgr.reset();
			if(mCallerHandler != null) {
				Message msg = new Message();
				msg.what = Messages.MESSAGE_PROCESS_QUEUE_PAUSED.ordinal();
				mCallerHandler.sendMessage(msg);
				mCallerHandler = null;
			}
		}
		return processCancelled;
	}

    //! Resumes paused queue and calls method to schedule send next unprocessed image of the current ItemEntity for processing.
	public void resumeQueue() {
		isQueuePaused = false;
		isQueueBusy = false;
		try {
			scheduleProcessing();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/// Check queue is paused or not
		public boolean isQueuePaused(){
			return isQueuePaused; 
		}

    //! Searches and removes the specified itemid from queue.
	public void removeItemFromQueue(Long itemId) {
		if (itemQueue == null) {
			return;
		}
		for (int i = 0; i < itemQueue.size(); i++) {
			if (itemQueue.get(i) != null) {
				if (itemId.equals(itemQueue.get(i).getItemId())) {
					itemQueue.remove(i);
				}
			}
		}
	}

	//! Cleanup of ImageProcessingQueueManager object.
	/**
	 * Method also cancels ongoing image processing.
	 * @return
	 */
	public boolean cleanup(){
		isQueueHalted = true;

		if(mThreadHandler != null) {
			mThreadHandler.post(null);
			mThreadHandler.removeCallbacks(mRunnable);
		}
		if(mHandlerThread != null) {
			mHandlerThread.quit();
			mHandlerThread = null;
		}

		boolean result = false;
		pauseQueue(null);
		mDiskMgr.cleanupPartiallyProcessedImages();

		if(itemQueue != null) {
			itemQueue.clear();
		}
		if(mProcessMgr != null) {
			mProcessMgr.cleanup();
		}
		pSelf = null;
		mHandler = null;
		mLastItemId = null;
		mCurrentInputImagePath = null;
		mLastPageId = -1L;
		result = true;
		isQueueHalted = false;
		return result;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void initSelf() {
		mProcessMgr = ImageProcessManager.getInstance(mContext);
		mItemDBMgr = DatabaseManager.getInstance();
		mDocMgr = DocumentManager.getInstance(mContext);
		mUtilRoutines = UtilityRoutines.getInstance();
		mDiskMgr = DiskManager.getInstance(mContext);
		mDBMgr = DatabaseManager.getInstance();

		if (itemQueue == null) {
			itemQueue = new ArrayList<ItemEntity>(0);
		}

		mHandler = new Handler(new Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];

				switch (whatMessage) {
				case MESSAGE_IMAGE_PROCESSED:
				case MESSAGE_PROCESS_FAILED:
					Log.i(TAG, "MESSAGE_IMAGE_PROCESSED received!");
					updateProcessStatusInPageTable(mLastPageId, msg);
					sendBroadcast(mLastItemId, mLastPageId, msg, Constants.CUSTOM_INTENT_IMAGE_PROCESSED);
					mLastPageId = -1L;
					isQueueBusy = false;
					//don't schedule next image for processing if background-image-processing is not enabled(low-end-devices)
					if(!Constants.BACKGROUND_IMAGE_PROCESSING) {
						if((itemQueue != null) && (itemQueue.size() > 0)) {
							itemQueue.remove(0);
						}
					}
					else {
						if(!isQueueHalted) {
							try {
								scheduleProcessing();
							} catch (Exception e) {
								e.printStackTrace();
							}
							startQueue();
						}
					}
					break;
				case MESSAGE_IMAGE_PROCESS_CANCELLED:
					Log.d(TAG, "MESSAGE_IMAGE_PROCESS_CANCELLED received in ProcessQueueManager.");
					Log.d(TAG, "Checking if any processing is in progress");
					//On cancel processing, reset page status from 'PROCESSING' to 'UNPROCESSED'
					mDiskMgr.cleanupPartiallyProcessedImages();

					if(!isQueueHalted) {
						if(mCallerHandler != null) {
							mProcessMgr.reset();
							Message myMsg = new Message();
							myMsg.what = Messages.MESSAGE_PROCESS_QUEUE_PAUSED.ordinal();
							mCallerHandler.sendMessage(myMsg);
							mCallerHandler = null;
						}
					}
					break;
				default:
					break;
				}
				return false;
			}
		});
	}


	private void startQueue() {
		if (isQueueHalted) {
			Log.i(TAG, "Queue is halted permanently.");
			return;
		}
		if(!isQueueStopped) {
			Log.i(TAG, "Queue is already running... returning.");
			return;
		}
		if (isQueueBusy || isQueuePaused) {
			Log.i(TAG, "Queue is busy, need not be started again.");
			return;
		}
		if(mThreadHandler == null) {
			mHandlerThread = new HandlerThread(Constants.STR_HANDLER_THREAD);
			mHandlerThread.start();
			mThreadHandler = new Handler(mHandlerThread.getLooper());
		}
		if(mRunnable == null) {
			mRunnable = new Runnable() {
				@Override
				public void run() {
					if(isQueueEmpty() || isQueueBusy || isQueuePaused || isQueueHalted) {
						//	Log.i(TAG, "Is Queue EMPTY? ::" + isQueueEmpty());
						//	Log.i(TAG, "Is Queue BUSY? ::" + isQueueBusy);
					}
					else { 
						scheduleProcessing();
					}
					//if queue is not requested to be stopped permanently, iterate the thread
					if(!isQueueHalted) {
						mThreadHandler.postDelayed(mRunnable, 500);
					}
				}
			};
		}
		mThreadHandler.post(mRunnable);

		isQueueStopped = false;
	}

	private void stopQueue() {
		if(mThreadHandler != null) {
			mThreadHandler.removeCallbacks(mRunnable);
		}
		isQueueStopped = true;
	}

	private synchronized boolean  scheduleProcessing() {
		//Log.i(TAG, "Enter: scheduleProcessing");

		boolean returnResult = false;
		if(isQueueHalted) {
			return returnResult;
		}

		if (isQueueBusy || isQueuePaused) {
			Log.i(TAG, "Queue is busy, returning without new scheduling");
			return returnResult;
		}
		isQueueBusy = true;
		stopQueue();
		boolean isExeption = false;
		if (!isQueueEmpty()) {
			List<PageEntity> unproPages = null;
			for(int i=0; i < itemQueue.size(); i++) {
				try {
					if(unproPages != null) {
						unproPages.clear();
						unproPages = null;
					}

					ItemEntity itemEntity = itemQueue.get(i);
					Long currentItemId = itemEntity.getItemId();

					unproPages = mItemDBMgr.getPages(mContext, currentItemId, Globals.ImageType.DOCUMENT.name(), ProcessingStates.UNPROCESSED);

					//if all images in item are processed, continue to loop and get next item
					if(unproPages == null || unproPages.size() == 0) {
						// if no more unprocessed images are present in item, remove
						// that item from queue
						Log.i(TAG, "no more unprocessed images are present in item... removing item from queue.");
						removeItemFromQueue(currentItemId);
						continue;
					}
					//check if corresponding document object present in document-types array of docMgr
					int index = mDocMgr.findDocumentTypeIndex(itemEntity.getItemTypeName());
					DocumentType refDocType = null;
					if(index == -1) {
						Log.e(TAG, "Invalid document type.");
					}
					else {
						if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE && mDBMgr.isOfflineDocumentSerializedInDB(mDBMgr.getProcessingParametersEntity())){
							Document doc = (Document)mDiskMgr.byteArrayToDocument(mDBMgr.getProcessingParametersEntity().getSerializeDocument());
							refDocType = doc.getDocumentType();
						}else{
						refDocType = mDocMgr.getDocTypeFromRefArray(index);
							if(refDocType == null && mDBMgr.isOfflineDocumentSerializedInDB(mDBMgr.getProcessingParametersEntity())){
								Document doc = (Document)mDiskMgr.byteArrayToDocument(mDBMgr.getProcessingParametersEntity().getSerializeDocument());
								refDocType = doc.getDocumentType();
							}
						}
						if(refDocType == null) {
							//the reference documentTyps is not downloaded yet.
							//schedule for download and continue with loop to get next item in queue
							//Log.i(TAG, "the reference documentTyps is not downloaded yet.. scheduling for download and waiting.");
							if(mUtilRoutines.checkInternet(mContext)) {
								mDocMgr.downloadDocTypeObject(index);
							}
							else {
								Log.i(TAG, "Network not available.");
								//remove item from the queue
								removeItemFromQueue(currentItemId);
							}
							//continue;
						}
						else {
							//get saved documentObj from DB
							boolean result = false;
							mLastItemId = currentItemId;
							result = false;
							if(unproPages != null && unproPages.size() > 0) {
								Log.d(TAG, "Content of unprocPages is");
								for(int cnt=0; cnt<unproPages.size(); cnt++) {
									Log.d(TAG, "Page ID ==> " + unproPages.get(cnt).getPageId() + " Processing status ==> " + mItemDBMgr.getPageForId(mContext, unproPages.get(cnt).getPageId()).getProcessingStatus());
								}

								Log.i(TAG, "Sending next image for processing for page ID ." + unproPages.get(0).getPageId());

								result = sendForProcess(unproPages.get(0), refDocType);
								if(mLastPageId != -1 && result != false) {
									mItemDBMgr.getPageForId(mContext, mLastPageId).setProcessingStatus((long)ProcessingStates.PROCESSING.ordinal());
									mItemDBMgr.getPageForId(mContext, mLastPageId).update();
									//send broadcast message that the image processing for the respective image has started(this is required on item-details screen to update the progressbar on respecive image thumbnail).
									Message msg = new Message();
									msg.what = Messages.MESSAGE_IMAGE_PROCESSING_STARTED.ordinal();
									sendBroadcast(mLastItemId, mLastPageId, msg, Constants.CUSTOM_INTENT_IMAGE_PROCESSING_STARTED);
								}
								unproPages.clear();
								unproPages = null;
							}

							if (!result) {
								//if sendForProcess fails for some reason, pick next image to send for processing 
								//continue;
							}
							else {
								returnResult = true;
								break;
							}
						}
					}
				} catch (KmcRuntimeException e) {
					Log.e(TAG, "KmcRuntimeException occured!!!!!!!!!!!");
					isExeption = true;
					e.printStackTrace();
					// in case of error, mark processing state of the image as processed-failed.
					mItemDBMgr.getPageForId(mContext, mLastPageId).setProcessingStatus((long)ProcessingStates.PROCESSFAILED.ordinal());
					mItemDBMgr.getPageForId(mContext, mLastPageId).update();
				} catch (KmcException e) {
					Log.e(TAG, "KmcException occured!!!!!!!!!!!");
					isExeption = true;
					e.printStackTrace();
					Log.e(TAG, "Exception : " + e.getErrorInfo().name());;

					if(e.getErrorInfo() == ErrorInfo.KMC_ED_FILE_EXISTS) {
						Log.e(TAG, "Removing already existing file");

						//delete the processed file from disk
						mDiskMgr.deleteImageFromDisk(mItemDBMgr.getPageForId(mContext, mLastPageId).getProcessedImageFilePath());
					}
					// in case of error, mark processing state of the image as processed-failed.
					mItemDBMgr.getPageForId(mContext, mLastPageId).setProcessingStatus((long)ProcessingStates.PROCESSFAILED.ordinal());
					mItemDBMgr.getPageForId(mContext, mLastPageId).update();
				} catch (IOException e) {
					isExeption = true;
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					isExeption = true;
					e.printStackTrace();
				}
				if(isExeption) {
					//isQueueBusy = false;
				}
			}
		}
		else {
			Log.i(TAG, "itemQueue is EMPTY :: returnResult ==> " + returnResult);
			//start a timer here to periodically check if any new item is added to for processing
			if(isQueueStopped) {
				isQueueBusy = false;
				startQueue();
			}
		}

		if(returnResult == false ) {
			isQueueBusy = false;
			if(isQueueStopped) {
				startQueue();
			}
		}

		return returnResult;
	}

	private boolean sendForProcess(PageEntity pageE, DocumentType docTypeObj) throws KmcRuntimeException, KmcException,
	StreamCorruptedException, IOException, ClassNotFoundException {
		boolean result = false;
		if (mProcessMgr == null) {
			mProcessMgr = ImageProcessManager.getInstance(mContext);
		}
		isQueueBusy = true;
		mLastPageId = pageE.getPageId();;
		mCurrentInputImagePath = pageE.getImageFilePath();
		if (mCurrentInputImagePath != null) {
			mProcessMgr.Process(pageE.getImageFilePath(),
					pageE.getProcessedImageFilePath(), docTypeObj,
					mHandler);
			result = true;
		}
		return result;
	}

	private void updateProcessStatusInPageTable(Long pageId, Message msg){
		//check if page record is present in table, if not, delete image from disk as well.
		if (mItemDBMgr.getPageForId(mContext, pageId) == null) {
			mDiskMgr.deleteImageFromDisk(mItemDBMgr.getPageForId(mContext, pageId).getProcessedImageFilePath());
			mDiskMgr.deleteImageFromDisk(mCurrentInputImagePath);
		}
		//if the corresponding item is not present in first position of queue, remove processed image from disk
		else if ((itemQueue == null) || (itemQueue.size() == 0) || (itemQueue.get(0) != mItemDBMgr.getPageForId(mContext, pageId).getItemEntity())) {
			mDiskMgr.deleteImageFromDisk(mItemDBMgr.getPageForId(mContext, pageId).getProcessedImageFilePath());
			mItemDBMgr.getPageForId(mContext, pageId).setProcessingStatus((long)ProcessingStates.UNPROCESSED.ordinal());
			mItemDBMgr.getPageForId(mContext, pageId).update();
		}
		else {
			if(msg.arg1 == ErrorInfo.KMC_SUCCESS.ordinal()) {
				//update successful processed status in table
				mItemDBMgr.getPageForId(mContext, pageId).setProcessingStatus((long)ProcessingStates.PROCESSED.ordinal());
				mItemDBMgr.getPageForId(mContext, pageId).update();

				Log.d(TAG, "Page ID ==> " + pageId + " Processing status after update is ====> " + mItemDBMgr.getPageForId(mContext, pageId).getProcessingStatus());
			}
			else {
				//Log.i(TAG, "Updating status as  " + ProcessingStates.PROCESSFAILED.name() + " for page ID " + mLastPageId);
				mItemDBMgr.getPageForId(mContext, pageId).setProcessingStatus((long)ProcessingStates.PROCESSFAILED.ordinal());
				mItemDBMgr.getPageForId(mContext, pageId).update();
			}
		}
	}

	private boolean isQueueEmpty() {
		return ((itemQueue == null)||(itemQueue.size()) == 0);
	}

	private void sendBroadcast(Long itemId, Long pageId, Message msg, String action) {
		Log.i(TAG, "Image processed for page ====================> " + pageId);
		Intent i = new Intent(action);
		i.putExtra(Constants.STR_ITEM_ID, itemId);
		i.putExtra(Constants.STR_PAGE_ID, pageId);
		i.putExtra(Constants.STR_PROCESS_STATUS, msg.arg1);
		if(msg.obj != null){
			i.putExtra(Constants.STR_IMAGE_METADATA,(String)msg.obj);
		}
		mContext.sendBroadcast(i);
	}
}