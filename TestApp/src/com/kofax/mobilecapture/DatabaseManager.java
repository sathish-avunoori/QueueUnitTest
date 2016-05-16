// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.ken.engines.data.Image.ImageMimeType;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.ItemEntityDao;
import com.kofax.mobilecapture.dbentities.ItemEntityDao.Properties;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.dbentities.PageEntityDao;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntityDao;
import com.kofax.mobilecapture.dbentities.UserInformationEntity;
import com.kofax.mobilecapture.dbentities.UserInformationEntityDao;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;

import de.greenrobot.dao.query.CountQuery;
import de.greenrobot.dao.query.DeleteQuery;
import de.greenrobot.dao.query.Query;

/// This class takes care of storage and retrieval of all the images and their respective document details.

public class DatabaseManager {

	/// An enum which identifies the processing status of the specified imaege(page).
	public enum ProcessingStates {
		UNPROCESSED,
		PROCESSING,
		PROCESSFAILED, 
		PROCESSED,
		REVERTED
	};

	private static volatile DatabaseManager pSelf = null;

	private ItemEntity itemEntity;
	private UserInformationEntity userInformationEntity = null;
	private ProcessingParametersEntity processingParametersEntity = null;

	private DatabaseManager() {

	}
    //! The factory method.
    /**
        This method returns a singleton object of DatabaseManager.
     */
	public static DatabaseManager getInstance() {
		if (pSelf == null) {
			synchronized (DatabaseManager.class) {
				if (pSelf == null) {
					pSelf = new DatabaseManager();		
				}
			}
		}
		return pSelf;
	}

    //! Get the ItemEntity instance.
	/**
	 * @return the ItemEntity
	 */
	public ItemEntity getItemEntity() {
		return itemEntity;
	}

	/// Set the ItemEntity instance.
	public void setItemEntity(ItemEntity item_entity) {
		itemEntity = item_entity;
	}
	
	
	 //! Get the processingParametersEntity.
	/**
	 * @return the processingParametersEntity
	 */
	public ProcessingParametersEntity getProcessingParametersEntity() {
		return processingParametersEntity;
	}
	
	/// Set the processingParametersEntity.
	public void setProcessingParametersEntity(ProcessingParametersEntity ppEntity) {
		processingParametersEntity = ppEntity;
	}
	
	 //! Get the userInformationEntity.
		/**
		 * @return the userInformationEntity
		 */
		public UserInformationEntity getUserInformationEntity() {
			return userInformationEntity;
		}
		
		/// Set the userInformationEntity.
		public void setUserInformationEntity(UserInformationEntity user_entity) {
			userInformationEntity = user_entity;
		}
	
	
	
    /// Updates ItemEntity.
	public void update(Context context, ItemEntity item) {
		getItemDao(context).update(item);
	}

	 /// Updates ProcessingParametersEntity.
	public void updateProcessingEntity(Context context, ProcessingParametersEntity ppItem) {
		getProcessingParametersDao(context).update(ppItem);
	}

    /// Updates ItemEntity.
	public long insertOrUpdate(Context context, ItemEntity item) {
		return getItemDao(context).insertOrReplace(item);
	}

    /// Deletes all items from database.
	public void clearItems(Context context) {
		getItemDao(context).deleteAll();
	}

    /// Deletes specified item from database.
	public void deleteItemWithId(Context context, long id) {
		/* delete pages under the item before deleting item entry */
		DeleteQuery<PageEntity> query = getPageDao(context)
				.queryBuilder()
				.where(com.kofax.mobilecapture.dbentities.PageEntityDao.Properties.ItemId.eq(id)).buildDelete();
		query.executeDeleteWithoutDetachingEntities();
		/* delete item entry now */
		getItemDao(context).delete(getItemForId(context, id));
	}

    //! Get all items from database.
	/**
	 * @return The list of all item entities in database
	 */	
	public List<ItemEntity> getAllItems(Context context) {
		Query<ItemEntity> query = getItemDao(context).queryBuilder().orderDesc(Properties.ItemCreatedTimeStamp).build();
		return query.list();
	}

    //! Get all items from database filtered by server-type, host and user.
	/**
	 * @return The list of all item entities selected based on specified criteria.
	 */	
	public List<ItemEntity> getAllItems(Context context, String serverId, String hostName,
			String userId) {
		Query<ItemEntity> query = getItemDao(context)
				.queryBuilder()
				.where(Properties.ServerId.eq(serverId),
						Properties.Hostname.eq(hostName),
						Properties.UserId.like(userId)).orderDesc(Properties.ItemCreatedTimeStamp).build();
		return query.list();
	}

    //! Get item instance for the specified item-id.
	/**
	 * @return Item entity.
	 */	
	public ItemEntity getItemForId(Context context, long id) {
		return getItemDao(context).load(id);
	}

    //! Constructs a complete item url starting from application root directory to form its location on disk.
	/**
	 * Function constructs url combining application root directory, hostname, currently logged in user id, item directory name, and item name.
	 * 
	 * @return Complete url of disk location of specified item.
	 */	
	public String constructItemURL(Context context, long id, String appRootDir) {
		String url = null;
		ItemEntity item = getItemForId(context, id);

		url = appRootDir + item.getServerId() + File.separator + item.getHostname() + File.separator
				+ item.getUserId() + File.separator + Constants.ITEM_DIRECTORY_NAME
				+ item.getItemName();

		return url;
	}

    //! Update page entity in database.
	/**
	 * Used for saving any changes made in specified PageEntity into database. 
	 * 
	 * @return Page id.
	 */
	public long insertOrUpdatePage(Context context, PageEntity page) {
		return getPageDao(context).insertOrReplace(page);
	}

    //! Deletes specified page from database.
	public void deletePageWithId(Context context, long id) {
		getPageDao(context).delete(getPageForId(context, id));
	}

	//! Get list of all the pages from page table.
	/**
	 * @return List of page entity.
	 */
	public List<PageEntity> getAllPages(Context context) {
		return getPageDao(context).loadAll();
	}

    //! Get list of all the pages for the specified item.
	/**
	 * @return List of page entity.
	 */
	public List<PageEntity> getAllPagesForItem(Context context, Long itemId) {

		List<PageEntity> list = getPageDao(context)
				.queryBuilder()
				.where(com.kofax.mobilecapture.dbentities.PageEntityDao.Properties.ItemId
						.eq(itemId))
						.orderAsc(
								com.kofax.mobilecapture.dbentities.PageEntityDao.Properties.SequenceNumber)
								.list();
		return list;
	}

	
    //! Get page instance for the specified page-id.
	/**
	 * @return Page entity.
	 */	
	public PageEntity getPageForId(Context context, long id) {
		return getPageDao(context).load(id);
	}

	private PageEntityDao getPageDao(Context c) {
		return ((KMCApplication) c.getApplicationContext()).getDaoSession()
				.getPageEntityDao();
	}

    //! Get the count of Pages for an Item.
	/**
	 * @return  Page count
	 */
	public int getAllPagesCountForItem(Context context, long itemID) {
		int result = 0;
		CountQuery<PageEntity> query = getPageDao(context).queryBuilder()
				.where(Properties.ItemId.eq(itemID)).buildCount();

		result = (int) query.count();

		return result;

	}
	
    //! Create and insert a new page entity in database .
	/**
	 * Method creates a new page under the currently selected item id (identified by getItemEntity()), populates all parameters of page and inserts in into database.
	 * @return new page ID.
	 */
	public long insertPageinDB(Context mContext, int imageType,
			String unprocessedImgUrl, String processedImgUrl) {
		long pageId = -1;

		if (itemEntity != null) {

			PageEntity page = new PageEntity();

			page.setItemEntity(itemEntity);
			page.setItemId(itemEntity.getItemId());
			page.setDate(new Date());
			Integer intObj = Integer.valueOf(getAllPagesCountForItem(mContext,
					itemEntity.getItemId()) + 1);
			page.setSequenceNumber(intObj);

			if (imageType == Globals.ImageType.DOCUMENT.ordinal()) {
				page.setImageType(Globals.ImageType.DOCUMENT.name());
			} else {
				page.setImageType(Globals.ImageType.PHOTO.name());
			}
			page.setImageFilePath(unprocessedImgUrl);
			page.setProcessedImageFilePath(processedImgUrl); // Storing image
			// path for
			// image which
			// will be
			// processed in
			// future. It
			// will be null
			// in case of
			// PHOTO
			page.setProcessingStatus((long)ProcessingStates.UNPROCESSED
					.ordinal());

			//result = insertOrUpdatePage(mContext, page);
			pageId = insertOrUpdatePage(mContext, page);

			Log.i("DBManager", "Count ============================================> " +
					getPages(mContext, getItemEntity().getItemId(), Globals.ImageType.DOCUMENT.name(), ProcessingStates.UNPROCESSED).size());
		}

		return pageId;
	}
	
    //! Get the list of pages for the selected item based on filters.
	/**
	 * @return List of page entity.
	 */
	public List<PageEntity> getPages(Context context, Long itemId,
			String imageTypeFilter, ProcessingStates processingStatusFilter) {
		List<PageEntity> pageList = null;
		List<PageEntity> tempPageList = getAllPagesForItem(context, itemId);
		if (tempPageList != null && tempPageList.size() > 0) {
			pageList = new ArrayList<PageEntity>();
			for (int i = 0; i < tempPageList.size(); i++) {
				PageEntity page = tempPageList.get(i);
				if (imageTypeFilter != null) {
					if ((page != null && page.getProcessingStatus() != null)
							&& (page.getItemId() == itemId)
							&& (page.getImageType().equals(imageTypeFilter))
							&& (page.getProcessingStatus().intValue() == (processingStatusFilter
									.ordinal()))) {
						pageList.add(page);
					}
				} else {
					if ((page != null && page.getProcessingStatus() != null)
							&& (page.getItemId() == itemId)
							&& (page.getProcessingStatus().intValue() == (processingStatusFilter
									.ordinal()))) {
						pageList.add(page);
					}
				}
			}
			tempPageList.clear();
		}
		tempPageList = null;
		return pageList;
	}

	
    //! Update sequence number of the pages after reorder is done.
	public void updatePageSequence(Context context,
			ArrayList<PageEntity> pagesList) {
		if (pagesList != null && pagesList.size() > 0) {
			for (int i = 0; i < pagesList.size(); i++) {
				PageEntity page = pagesList.get(i);
				page.setSequenceNumber(i + 1);
				insertOrUpdatePage(context, page);
			}

		}
	}
	
    //! Set specified processing status of all pages under specified item.
	public void setAllPagesProcessingStatus(Context context, Long itemId,
			ProcessingStates filter) {
		List<PageEntity> tempPageList = getAllPagesForItem(context, itemId);
		if (tempPageList != null && tempPageList.size() > 0) {
			for (int i = 0; i < tempPageList.size(); i++) {
				PageEntity page = tempPageList.get(i);
				if (page != null && page.getProcessingStatus() != null) {
					page.setProcessingStatus((long)filter.ordinal());
				}
			}
		}
	}
	
    //! Change specified processing status from one type to another of all pages under specified item.
	public void setAllPagesSelectiveProcessingStatus(Context context, Long itemId,
			ProcessingStates oldFilter, ProcessingStates newFilter) {
		List<PageEntity> tempPageList = getAllPagesForItem(context, itemId);
		if (tempPageList != null && tempPageList.size() > 0) {
			for (int i = 0; i < tempPageList.size(); i++) {
				PageEntity page = tempPageList.get(i);
				if (page != null && page.getProcessingStatus() != null && page.getProcessingStatus().equals((long)oldFilter.ordinal())) {
					page.setProcessingStatus((long)newFilter.ordinal());
				}
			}
		}
	}
	
	
	 //! Create and insert a new userInformation entity in database.
		/**
		 * Method creates a new userInformation, populates all parameters of page and inserts in into database.
		 * @return new User ID.
		 */
		public long insertUserInformationinDB(Context mContext, UserInformationEntity userEntity) {
			long userId = -1;													
			userId = insertOrUpdateUserInformation(mContext,userEntity);
			return userId;
		}
	
		//! Update UserInformation entity in database.
		/**
		 * Used for saving any changes made in specified UserEntity into database. 
		 * 
		 * @return User id.
		 */
		public long insertOrUpdateUserInformation(Context context, UserInformationEntity userEntity) {
			return getUserDao(context).insertOrReplace(userEntity);
		}

		private UserInformationEntityDao getUserDao(Context c) {
			return ((KMCApplication) c.getApplicationContext()).getDaoSession()
					.getUserInformationEntityDao();
		}
		
		//! Get the list of UserInformation Entity.
				/**
				 * @return List of userInformation entity.
				 */
				public List<UserInformationEntity> getAllUserInformationList(Context context) {			
					List<UserInformationEntity> userList = getUserDao(context).loadAll();			
					return userList;
				}
		
		
		//! Get the list of UserInformation for the selected entity.
		/**
		 * @return List of userInformation entity.
		 */
		public List<UserInformationEntity> getUserInformationList(Context context,  UserInformationEntity userEntity) {			
			List<UserInformationEntity> userList = getUserInformationFromDetails(context, userEntity.getUserName(),userEntity.getHostName(),userEntity.getServerType());			
			return userList;
		}
		
		//! Get userInfo for the specified Details.
		/**
		 * @return List of userInformation entity.
		 */
		public List<UserInformationEntity> getUserInformationFromDetails(Context context, String userName,String hostName,String serverType) {

			List<UserInformationEntity> list = getUserDao(context)
					.queryBuilder()
					.where(com.kofax.mobilecapture.dbentities.UserInformationEntityDao.Properties.UserName.like(userName),
							com.kofax.mobilecapture.dbentities.UserInformationEntityDao.Properties.HostName.eq(hostName),
							com.kofax.mobilecapture.dbentities.UserInformationEntityDao.Properties.ServerType.eq(serverType)).list();
			return list;
		}
		
		 /// Deletes all items from database.
		public void clearUserInfo(Context context) {
			getUserDao(context).deleteAll();
		}

	    /// Deletes specified userInformationEntity from database.
		public void deleteuserWithEntity(Context context, UserInformationEntity userEntity) {			
			getUserDao(context).delete(userEntity);
		}
		
		
		 //! Create and insert a new  entity in database.
		/**
		 * Method creates a new ProcessingParameters and inserts into database.
		 * @return new ProcessingParameters ID.
		 */
		public long insertProcessingParametersinDB(Context mContext,String documentTypeName) {
			long ProcessingParametersId = -1;
		if (userInformationEntity != null) {
			ProcessingParametersEntity ppEntity = new ProcessingParametersEntity();
			ppEntity.setDocumentTypeName(documentTypeName);
			ppEntity.setUserInformationEntity(userInformationEntity);
			ppEntity.setUserInformationId(userInformationEntity
					.getUserInformationId());
			ProcessingParametersId = insertOrUpdateProcessingParameters(
					mContext, ppEntity);
		}

			return ProcessingParametersId;
		}		
		
		//! Update ProcessingParameters entity in database.
				/**
				 * Used for saving any changes made in specified ProcessingParameters into database. 
				 * 
				 * @return ProcessingParameters id.
				 */
				public long insertOrUpdateProcessingParameters(Context context, ProcessingParametersEntity ppEntity) {
					return getProcessingParametersDao(context).insertOrReplace(ppEntity);
				}

				private ProcessingParametersEntityDao getProcessingParametersDao(Context c) {
					return ((KMCApplication) c.getApplicationContext()).getDaoSession()
							.getProcessingParametersEntityDao();
				}
				
				//! Get the list of ProcessingParameters Entity.
						/**
						 * @return List of ProcessingParameters entity.
						 */
						public List<ProcessingParametersEntity> getAllProcessingParametersList(Context context) {			
							List<ProcessingParametersEntity> ProcessingParametersList = getProcessingParametersDao(context).loadAll();			
							return ProcessingParametersList;
						}
				
				
				//! Get the list of ProcessingParameters for the selected entity.
				/**
				 * @return List of ProcessingParameters entity.
				 */
				public List<ProcessingParametersEntity> getProcessingParametersList(Context context,  ProcessingParametersEntity ppEntity) {			
					List<ProcessingParametersEntity> userList = getProcessingParametersFromDetails(context, ppEntity.getDocumentTypeName(),ppEntity.getUserInformationId());			
					return userList;
				}
				
				//! Get ProcessingParameters for the specified Details.
				/**
				 * @return List of ProcessingParameters entity.
				 */
				public List<ProcessingParametersEntity> getProcessingParametersFromDetails(Context context, String DocTypeName,long id) {

					List<ProcessingParametersEntity> list = getProcessingParametersDao(context)
							.queryBuilder()
							.where(com.kofax.mobilecapture.dbentities.ProcessingParametersEntityDao.Properties.UserInformationId.eq(id),
									com.kofax.mobilecapture.dbentities.ProcessingParametersEntityDao.Properties.DocumentTypeName.eq(DocTypeName)).list();
					return list;
				}
				
				 /// Deletes all items from database.
				public void clearProcessingParametersInfo(Context context) {
					getProcessingParametersDao(context).deleteAll();
				}

			    /// Deletes specified ProcessingParametersEntity from database.
				public void deleteuserWithEntity(Context context, ProcessingParametersEntity ppEntity) {			
					getProcessingParametersDao(context).delete(ppEntity);
				}
		
		/// Update all items to online
		public void updateAllItemsToOnline(Context context, String serverId, String hostName,
				String userId){
			List<ItemEntity> list = getAllItems(context, serverId, hostName, userId);
			if(null != list && list.size() > 0){
				for(int i = 0 ;i < list.size();i++){
					list.get(i).setIsOffline(false);
					getItemDao(context).update(list.get(i));
				}
			}
			
		}
	
	
    //! Check if document object is saved in database for specified item.
	public boolean isDocumentSerializedInDB(ItemEntity itemEntity) {
		return (itemEntity != null
				&& itemEntity.getItemSerializedData() != null
				&& itemEntity.getItemSerializedData().length > 0);
	}
	
	  //! Check if document object is saved in database for specified item for offline support.
		public boolean isOfflineDocumentSerializedInDB(ProcessingParametersEntity ppEntity) {
			return (ppEntity != null
					&& ppEntity.getSerializeDocument() != null
					&& ppEntity.getSerializeDocument().length > 0);
		}
	
	///Update all processed files from jpeg/png to tiff format 
	public boolean updatePendingProcessFileNames(Context context){
		List<ItemEntity> list = getAllItems(context);
		if(null != list && list.size() > 0){
			for(int i = 0 ;i < list.size();i++){
				List<PageEntity> pageList = list.get(i).getPages();
				for(int j = 0;j < pageList.size();j++){
					String path = pageList.get(j).getProcessedImageFilePath();
					if(null != path){
						File processFile = new File(path);
						if(processFile.exists()){
							Image img = null;
							String replaceString = Constants.STR_EXTENSION_TIFF;
							if(path.endsWith(Constants.STR_EXTENSION_JEPG))
							{
							    img = new Image(path, ImageMimeType.MIMETYPE_JPEG);
							    replaceString = Constants.STR_EXTENSION_JEPG;
							}else if(path.endsWith(Constants.STR_EXTENSION_JPG)){
								img = new Image(path, ImageMimeType.MIMETYPE_JPEG);
								replaceString = Constants.STR_EXTENSION_JPG;
							}else if(path.endsWith(Constants.STR_EXTENSION_PNG)){
								img = new Image(path, ImageMimeType.MIMETYPE_PNG);
								replaceString = Constants.STR_EXTENSION_PNG;
							}
							path = path.replaceAll(replaceString,Constants.STR_EXTENSION_TIFF);
	
							if(null != img && null != path){
								Image newImg = null;
								try {					
									img.imageReadFromFile();
									processFile.delete();
									File newFile = new File(path);
									newImg = new Image(img.getImageBitmap());									
									newImg.setImageFilePath(newFile.getAbsolutePath());
									newImg.imageWriteToFile();								
									pageList.get(j).setProcessedImageFilePath(path);
									getPageDao(context).update(pageList.get(j));							
								} catch (KmcRuntimeException e) {
									e.printStackTrace();
								} catch (KmcException e) {
									e.printStackTrace();
								}
								img.imageClearBitmap();
								img = null;
								if(null != newImg){
									newImg.imageClearBitmap();
									newImg = null;
								}
								
								
							}	
						}else{
							String replaceString = Constants.STR_EXTENSION_TIFF;
							if(path.endsWith(Constants.STR_EXTENSION_JEPG))
							{							   
							    replaceString = Constants.STR_EXTENSION_JEPG;
							}else if(path.endsWith(Constants.STR_EXTENSION_JPG)){								
								replaceString = Constants.STR_EXTENSION_JPG;
							}else if(path.endsWith(Constants.STR_EXTENSION_PNG)){							
								replaceString = Constants.STR_EXTENSION_PNG;
							}
							path = path.replaceAll(replaceString,Constants.STR_EXTENSION_TIFF);
							pageList.get(j).setProcessedImageFilePath(path);
							getPageDao(context).update(pageList.get(j));
						}
					}
				}
			}
		}
		return true;
	}
	
	public void cleanup() {
		pSelf = null;
	}

	// private methods
	private ItemEntityDao getItemDao(Context c) {
		return ((KMCApplication) c.getApplicationContext()).getDaoSession()
				.getItemEntityDao();
	}
}
