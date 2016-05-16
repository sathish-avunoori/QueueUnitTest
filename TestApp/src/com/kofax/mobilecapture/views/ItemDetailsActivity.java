// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.LruCache;
import android.view.Display;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.klo.logistics.data.DocumentType;
import com.kofax.kmc.klo.logistics.data.FieldType;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DatabaseManager.ProcessingStates;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.ImageProcessQueueManager;
import com.kofax.mobilecapture.Initializer;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.ImageType;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.Globals.RequestCode;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

@SuppressLint("NewApi")
public class ItemDetailsActivity extends Activity implements OnTouchListener,
OnItemLongClickListener, OnItemClickListener,NetworkChangedListener {

	// - public enums

	// - Private enums
	private enum mRefreshType {
		NEW_LIST, OLD_LIST
	}

	/* Grid view Selection type */
	private enum mSelectionMode {
		SINGLE_MODE, MULTIPLE_MODE
	}

	private mSelectionMode mGridviewMode = mSelectionMode.SINGLE_MODE;
	private ArrayList<MultipleSelection> selectedlist;
	private LruCache<String, Bitmap> mMemoryCache;

	// - public interfaces

	// - public nested classes
	public class MultipleSelection{
		boolean isselected;
		int position;
	}

	public class CustomGridAdapter extends BaseAdapter {

		private ArrayList<PageEntity> itemImgesUrlList;
		private Context mContext;

		public CustomGridAdapter(Context context,
				ArrayList<PageEntity> itemImgesUrl) {
			this.mContext = context;
			this.itemImgesUrlList = itemImgesUrl;
			selectedlist = new ArrayList<ItemDetailsActivity.MultipleSelection>();
		}

		public class GridItemsHolder {
			View gridItemContainer;
			ImageView image;
			ImageView processSucessSign;
			ImageView processFailedSign;
			View selectionBorderOverlay;
			ProgressBar processingIndicatorBar;
			String filePath;
			int position;
			boolean isSelected;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			GridItemsHolder holder;
			if(selectedlist != null){
				MultipleSelection addsel= new MultipleSelection();
				try {
					selectedlist.get(position);
				} catch ( IndexOutOfBoundsException e ) {
					selectedlist.add( position, addsel);
				}
			}

			if (convertView == null) {

				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				holder = new GridItemsHolder();

				convertView = inflater.inflate(R.layout.itemdetailgridview,
						parent, false);
				holder.gridItemContainer = convertView.findViewById(R.id.gridItemContainer);
				holder.selectionBorderOverlay = convertView.findViewById(R.id.selectionBorderOverlay);
				holder.position = position;
				holder.image = (ImageView) convertView
						.findViewById(R.id.itemdetail_Imageview);
				holder.processSucessSign = (ImageView) convertView
						.findViewById(R.id.ivSucessSign);
				holder.processFailedSign = (ImageView) convertView
						.findViewById(R.id.ivFailedSign);
				holder.processingIndicatorBar = (ProgressBar) convertView
						.findViewById(R.id.itemdetail_PBar);
				RelativeLayout.LayoutParams lp = null;
				lp = new RelativeLayout.LayoutParams(mColumnWidth+10, mColumnWidth+10);
				holder.gridItemContainer.setLayoutParams(lp);

				convertView.setTag(holder);
				holder.image.setTag(convertView);
			} else {
				holder = (GridItemsHolder) convertView.getTag();
				holder.position = position;
			}

			if (mGridviewMode != mSelectionMode.SINGLE_MODE) {
				holder.image.setClickable(true);
				holder.image.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						ImageView iv = (ImageView) v;
						View lview =  (View) iv
								.getTag();

						GridItemsHolder lholder = (GridItemsHolder)lview.getTag();
						Log.d(TAG, "position in imageview ::"+lholder.position);

						if (selectedlist.get(lholder.position).isselected) {
							selectedlist.get(lholder.position).isselected = false;
							lholder.selectionBorderOverlay.setVisibility(View.INVISIBLE);
							mSelectedImgCount--;
						} else {
							if (mPageList != null && mPageList.size() >= 0) {
								// should not open the image whose processing is currently going on.
								if (mPageList.get(lholder.position).getProcessingStatus() == ProcessingStates.PROCESSING.ordinal()) {
									return;
								}
							}
							selectedlist.get(lholder.position).isselected = true;
							lholder.selectionBorderOverlay.setVisibility(View.VISIBLE);
							mSelectedImgCount++;
						}
						updateTitlebarcount(mGridviewMode);
						updateMenuOptions(lholder.position, selectedlist.get(lholder.position).isselected);
						lview.setTag(lholder);
						iv.setTag(lview);
						// notifyDataSetChanged();
					}
				});
			}else {
				holder.image.setClickable(false);
			}

			String filePath = null;
			Log.d(TAG, "position is:: "+position);
			if (itemImgesUrlList.get(position).getProcessingStatus() == ProcessingStates.UNPROCESSED
					.ordinal()) {
				filePath = itemImgesUrlList.get(position)
						.getImageFilePath();
				holder.processSucessSign.setVisibility(View.GONE);
				holder.processingIndicatorBar.setVisibility(View.GONE);
				holder.processFailedSign.setVisibility(View.GONE);
			} else if (itemImgesUrlList.get(position).getProcessingStatus() == ProcessingStates.PROCESSING
					.ordinal()) {
				filePath = itemImgesUrlList.get(position)
						.getImageFilePath();
				holder.processSucessSign.setVisibility(View.GONE);
				holder.processingIndicatorBar.setVisibility(View.VISIBLE);
				holder.processFailedSign.setVisibility(View.GONE);
				// holder.processingIndicatorBar.setVisibility(View.VISIBLE);
			} else if (itemImgesUrlList.get(position).getProcessingStatus() == ProcessingStates.PROCESSED
					.ordinal()) {
				filePath = itemImgesUrlList.get(position)
						.getProcessedImageFilePath();
				holder.processSucessSign.setVisibility(View.VISIBLE);
				holder.processingIndicatorBar.setVisibility(View.GONE);
				holder.processFailedSign.setVisibility(View.GONE);

			} else if (itemImgesUrlList.get(position).getProcessingStatus() == ProcessingStates.PROCESSFAILED
					.ordinal()) {
				filePath = itemImgesUrlList.get(position)
						.getImageFilePath();
				holder.processSucessSign.setVisibility(View.GONE);
				holder.processingIndicatorBar.setVisibility(View.GONE);
				holder.processFailedSign.setVisibility(View.VISIBLE);
			}else if (itemImgesUrlList.get(position).getProcessingStatus() == ProcessingStates.REVERTED
					.ordinal()) {
				filePath = itemImgesUrlList.get(position)
						.getImageFilePath();
				holder.processSucessSign.setVisibility(View.GONE);
				holder.processingIndicatorBar.setVisibility(View.GONE);
				holder.processFailedSign.setVisibility(View.GONE);
			}

			holder.filePath = filePath;
			File file = new File(filePath);
			if(file.exists()){
				loadBitmap(filePath, holder.image, holder.processingIndicatorBar);
			}else{
				holder.image.setImageDrawable(null);
			}

			file = null;

			if (mGridviewMode == mSelectionMode.SINGLE_MODE) {
				holder.selectionBorderOverlay.setVisibility(View.INVISIBLE);
			}
			if (mGridviewMode != mSelectionMode.SINGLE_MODE) {
				View lview = (View) holder.image.getTag();
				GridItemsHolder lholder = (GridItemsHolder) lview.getTag();
				if(selectedlist.get(position).isselected){
					lholder.selectionBorderOverlay.setVisibility(View.VISIBLE);
				}else{
					lholder.selectionBorderOverlay.setVisibility(View.INVISIBLE);
				}
				convertView.setTag(lholder);
				lview.setTag(lholder);
				holder.image.setTag(lview);
			}
			return convertView;
		}

		@Override
		public int getCount() {
			int length = 0;
			if (itemImgesUrlList != null) {
				length = itemImgesUrlList.size();
			}
			return length;
		}

		@Override
		public Object getItem(int position) {
			return itemImgesUrlList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public ArrayList<PageEntity> getItems() {
			return itemImgesUrlList;
		}

		public void changeModelList(ArrayList<PageEntity> models) {
			this.itemImgesUrlList = models;
			notifyDataSetChanged();
		}

		public void loadBitmap(final String filePath, final ImageView imageView, final ProgressBar progress) {

			Bitmap bm = getBitmapFromMemCache(filePath);
			if(bm != null){
				imageView.setImageBitmap(bm);
			}else {
				imageView.setImageDrawable(null);
				progress.setVisibility(View.VISIBLE);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView, progress);
				task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,filePath);
			}
		}

		public synchronized void addBitmapToMemoryCache(String key, Bitmap bitmap) {
			if (key != null && getBitmapFromMemCache(key) == null) {
				if(bitmap != null)
					mMemoryCache.put(key, bitmap);
			}
		}

		public synchronized Bitmap getBitmapFromMemCache(String key) {
			return mMemoryCache.get(key);
		}

		class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
			public String filePath = null;
			private final WeakReference<ImageView> imageViewReference;
			private ProgressBar progress;
			public BitmapWorkerTask(ImageView imageView, ProgressBar progress) {
				// Use a WeakReference to ensure the ImageView can be garbage
				// collected
				this.progress = progress;
				imageViewReference = new WeakReference<ImageView>(imageView);
			}

			// Decode image in background.
			@Override
			protected Bitmap doInBackground(String... params) {
				filePath = String.valueOf(params[0]);
				Bitmap bitmap = ShrinkBitmap(filePath, mColumnWidth, mColumnWidth);
				addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
				return bitmap;
			}

			// Once complete, see if ImageView is still around and set bitmap.
			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (imageViewReference != null && bitmap != null) {
					final ImageView imageView = imageViewReference.get();
					if (imageView != null) {
						progress.setVisibility(View.GONE);
						imageView.setImageBitmap(bitmap);
					}

				}
			}
		}
	}

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = ItemDetailsActivity.class.getSimpleName();

	// - Private data.
	/* SDK objects */

	/* Application objects */
	private DocumentManager mDocMgrObj = null;
	private ItemEntity itemEntity = null;
	private DiskManager mDiskMgrObj = null;
	private PrefManager mPrefUtils = null;
	private ImageProcessQueueManager mProcessQueueMgr = null;
	private DatabaseManager mDBManager = null;
	private UtilityRoutines mUtilityRoutines = null;
	/* Standard variables */
	private CustomGridAdapter mCustomAdapter;
	private BroadcastReceiver mReceiver = null;
	private GridView gridView;
	private Handler mHandler = null;
	private Button msubmittokofax;
	private CustomDialog mCustomDialog = null;
	private LazyLoadGridImagesTask mLazyGridLoadTask = null;
	private ProgressBar mProgressBar = null;
	
	private Vibrator v;
	private ArrayList<PageEntity> mPageList = null;
	private List<String> mTempList = null;
	private HashMap<Long, Integer> positionHash = null;
	private int mStartPos = -1, mEndPos = -1;
	private int mColumnWidth = 0;
	private String mGallerySelPicExtn = null;
	private String mCurrentPhotoPath = null;
	private int mSelectedImgCount = 0;
	private int mProcessedImgSelectionCount = 0;
	private boolean mIsEditOptionDisable = false;  
	private boolean isItemTypeValid = true;

	private int launchNext = -1;
	private Bundle mBundle = null;

	private GridView parent = null;
	private int x;
	private int y;
	private int position;
	private int requestedItemTypeIndex = -1;
	private boolean refresh_list_flag = false;
	private boolean mIsImageOpened = false;
	private boolean activityVisible = false;
	private boolean mIsChangeItemTypeRequested = false;
	private boolean mShowRevert = false;
	private boolean mIsNewFlow = false;
	private boolean mIsDownloadStart = false;
	private boolean mIsFromPending = false;
	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.itemdetail_grid_layout);

		new DeviceSpecificIssueHandler().checkEntryPoint(this);
		new Initializer().initiateManagersAndServices(getApplicationContext());

		String itemType = null;
		long itemID = 0;

		Bundle bundle = getIntent().getExtras();

		initObjects();

		// Get memory class of this device, exceeding this amount will throw an
		// OutOfMemory exception.
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = maxMemory / 8;

		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

			protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in bytes rather than number
				// of items.
				return bitmap.getByteCount() / 1024;
			}

		};

		itemEntity = mDBManager.getItemEntity();
		if (itemEntity != null) {
			itemID = itemEntity.getItemId();
		}

		if (bundle != null) {
			mDocMgrObj.setOpenedDocName(bundle.getString(Constants.STR_ITEM_NAME));
			if (bundle.getString(Constants.STR_ITEM_TYPE) != null) {
				itemType = bundle.getString(Constants.STR_ITEM_TYPE);
			}
			if(bundle.containsKey(Constants.STR_IS_NEW_ITEM)){
				mIsNewFlow = bundle.getBoolean(Constants.STR_IS_NEW_ITEM);
			}
			if(bundle.containsKey(Constants.STR_IS_FROM_PENDING)){
				mIsFromPending = bundle.getBoolean(Constants.STR_IS_FROM_PENDING);
			}
			
			bundle = null;
		}

		if(itemEntity!=null && itemEntity.getFieldName() != null && itemEntity.getFieldName().equals(Constants.STR_NO_FIELDS)){
			mIsEditOptionDisable = true;
		}

		mPageList = (ArrayList<PageEntity>) mDBManager.getAllPagesForItem(getApplicationContext(), itemID);

		updateTitlebarcount(mGridviewMode);
		initGridView();
		initSubmitButton(itemType);

		mColumnWidth = getColumnWidth(getApplicationContext(), gridView);
		mProgressBar = (ProgressBar) findViewById(R.id.progressLoader);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}

		setupHandler();
		mDocMgrObj.setCurrentHandler(mHandler);
		registerBroadcastReceiver();

		lazyLoadGridOnLaunch();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		//		refreshList(mRefreshType.NEW_LIST);
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if(mHandler == null){
			setupHandler();
		}
		if(mHandler != null && mDocMgrObj != null) {
			mDocMgrObj.setCurrentHandler(mHandler);
		}
		if (refresh_list_flag == true) {
			refreshList(mRefreshType.OLD_LIST);
			refresh_list_flag = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(mPageList != null && mPageList.size()>0){
			mDBManager.updatePageSequence(getApplicationContext(),mPageList);
		}
		activityVisible = false;
		if(mMemoryCache != null){
			mMemoryCache.evictAll();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		activityVisible = true;
		Constants.NETWORK_CHANGE_LISTENER = ItemDetailsActivity.this;
		Constants.IS_NEED_TO_CLEAR_BACKGROUND_ACTIVITIES = true;
	}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(mCurrentPhotoPath == null){
			mCurrentPhotoPath = savedInstanceState.getString(Constants.CURRENT_PHOTO_PATH);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mCurrentPhotoPath != null){
			outState.putString(Constants.CURRENT_PHOTO_PATH, mCurrentPhotoPath);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, final int position,
			long arg3) {
		if (mPageList != null && position >= 0) {
			// should not open the image whose processing is currently going on.

			ProgressBar progress = (ProgressBar) view.findViewById(R.id.itemdetail_PBar);

			if (mPageList.get(position).getProcessingStatus() == ProcessingStates.PROCESSING.ordinal()) {
				return;
			}

			if(progress != null && progress.getVisibility() == View.VISIBLE){
				return;
			}

			mIsImageOpened = true;

			Intent i = new Intent(ItemDetailsActivity.this, QuickPreviewActivity.class);

			// If image is processed, then show preview of processed image
			if (mPageList.get(position).getProcessingStatus() == ProcessingStates.PROCESSED
					.ordinal()) {
				i.putExtra(Constants.STR_URL, mPageList.get(position).getProcessedImageFilePath());
			} else {
				i.putExtra(Constants.STR_URL, mPageList.get(position).getImageFilePath());
			}
					/*
					 * quickPreview is false when image is alread selected for prevew by
			 		 * tapping on thumbnail on item detail screen.
			 		 */
			i.putExtra(Constants.STR_QUICK_PREVIEW, false);
			i.putExtra(Constants.STR_IMAGE_INDEX, position);

			i.putExtra(Constants.STR_PAGE_ID, mPageList.get(position).getPageId());
					/*
					 * passing 'imgType' instead of 'sourceType', since after
					 * closing preview screen with normal preview, we dont relaunch
					 * the capture/gallery screen based on sourceType value.
					 */
			i.putExtra(Constants.STR_IMAGE_TYPE, ImageType.PHOTO.ordinal());
			mProgressBar.setVisibility(View.INVISIBLE);
			startActivityForResult(i, RequestCode.PREVIEW_IMAGE.ordinal());

		} else {
			Log.e(TAG, "mImageUrls is null, the items seems to be empty.");
			mIsImageOpened = false;
		}
	}


	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		if (position > AdapterView.INVALID_POSITION) {
			int count = parent.getChildCount();
			v.vibrate(75);
			for (int i = 0; i < count; i++) {
				View curr = parent.getChildAt(i);
				final int pos = i;
				curr.setOnDragListener(new View.OnDragListener() {
					@Override
					public boolean onDrag(View v, DragEvent event) {
						boolean result = true;
						int action = event.getAction();
						switch (action) {
						case DragEvent.ACTION_DRAG_STARTED:
							break;
						case DragEvent.ACTION_DRAG_LOCATION:
							if (shouldScrollUp(v, event)) {
								gridView.smoothScrollByOffset(-5);
							} else if (shouldScrollDown(v, event)) {
								gridView.smoothScrollByOffset(5);
							}
							break;
						case DragEvent.ACTION_DRAG_ENTERED:
							if (-1 == mStartPos) {
								mStartPos = pos;
								v.setBackgroundColor(Color.BLACK);
							} else {

								v.setBackgroundColor(Color.GREEN);
							}
							break;
						case DragEvent.ACTION_DRAG_EXITED:

							v.setBackgroundColor(Color.TRANSPARENT);
							break;
						case DragEvent.ACTION_DROP:
							v.setBackgroundColor(Color.TRANSPARENT);
							if (event.getLocalState() == v) {
								result = false;
							} else {
								mEndPos = pos;
								View droped = (View) event.getLocalState();
								GridView parent = (GridView) droped.getParent();
								if(parent != null) {
									CustomGridAdapter adapter = (CustomGridAdapter) parent
											.getAdapter();
									ArrayList<PageEntity> items = adapter
											.getItems();
									reorderGrids(items, mStartPos, mEndPos);
									refreshList(mRefreshType.OLD_LIST);
									mStartPos = -1;
									mEndPos = -1;
								}
								else {
									Log.e(TAG, "Parent is null");
								}
							}
							break;
						case DragEvent.ACTION_DRAG_ENDED:
							break;
						default:

							result = false;
							break;
						}
						return result;
					}
				});
			}
			int relativePosition = position - parent.getFirstVisiblePosition();
			View target = (View) parent.getChildAt(relativePosition);

			CustomGridAdapter.GridItemsHolder holder = (CustomGridAdapter.GridItemsHolder) target
					.getTag();
			String text = "current position is :: "
					+ holder.position;

			ClipData data = ClipData.newPlainText("DragData", text);
			target.startDrag(data, new View.DragShadowBuilder(target), target,
					0);
		}
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			parent = (GridView) v;

			x = (int) event.getX();
			y = (int) event.getY();

			position = parent.pointToPosition(x, y);
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.item_detail_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mGridviewMode == mSelectionMode.MULTIPLE_MODE) {
			for (int i = 0; i < menu.size(); i++) {
				if (i <= 2) {
					menu.getItem(i).setVisible(false);
				} else {
					menu.getItem(i).setVisible(true);
				}
				menu.findItem(R.id.menu_revert_doc).setVisible(mShowRevert);
			}
		} else {
			//check if item-type is valid before update menu options.
			if(isItemTypeValid) {
				for (int i = 0; i < menu.size(); i++) {
					if (i <= 2) {
						menu.getItem(i).setVisible(true);
					} else {
						menu.getItem(i).setVisible(false);
					}
				}
				if(mIsEditOptionDisable){
					menu.getItem(0).setIcon(R.drawable.edit_case_info_gray);
					menu.getItem(0).setEnabled(false);
				}
				else {
					menu.getItem(0).setIcon(R.drawable.edit_case_info);
					menu.getItem(0).setEnabled(true);
				}
				
				menu.getItem(1).setEnabled(true);	//enable 'add-images'
				menu.getItem(1).setIcon(R.drawable.add_document);
				menu.getItem(2).getSubMenu().getItem(1).setEnabled(true);	//enable 'select'
			}
			else {
				//disable 'edit-document-fields', 'add-images' and 'select' menu options. Only keep change-item-type enabled
				menu.getItem(0).setEnabled(false);	//disable 'edit-fields'
				menu.getItem(0).setIcon(R.drawable.edit_case_info_gray);
				menu.getItem(1).setEnabled(false);	//disable 'add-images'
				menu.getItem(1).setIcon(R.drawable.add_document_gray);
				menu.getItem(2).getSubMenu().getItem(1).setEnabled(false);	//disable 'select'
			}
			if(Constants.IS_HELPKOFAX_FLOW){
				menu.getItem(0).setVisible(false);
				menu.getItem(2).getSubMenu().getItem(0).setVisible(false);
			}
		}

		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String msg = null;
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.menu_change_type:
			if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE && Constants.IS_APP_ONLINE){
				if(Constants.BACKGROUND_IMAGE_PROCESSING) {
					msg = getResources().getString(R.string.str_user_msg_change_item_type_confirmation);
				}
				else {
					msg = getResources().getString(R.string.str_user_msg_change_item_type_lowend_confirmation);
				}
				mCustomDialog
				.show_popup_dialog(ItemDetailsActivity.this, AlertType.CONFIRM_ALERT,
						getResources().getString(R.string.str_user_msg_change_item_type_confirmation_title),
						msg,
						null, null,
						Messages.MESSAGE_DIALOG_CHANGE_ITEM_TYPE_CONFIRMATION,
						mHandler,
						false);
			}else{
				mCustomDialog
				.show_popup_dialog(ItemDetailsActivity.this, AlertType.ERROR_ALERT,
						getResources().getString(R.string.error_lbl),
						getResources().getString(R.string.app_msg_offline_operaton_failed),
						null, null,
						Messages.MESSAGE_DEFAULT,
						mHandler,
						false);
			}
			break;
		case R.id.menu_more:
			break;
		case R.id.menu_add_doc:
			if (mUtilityRoutines.getAvailableMemorySize() <= Constants.CAPTURE_LAUNCH_MEMORY) {
				mCustomDialog.show_popup_dialog(ItemDetailsActivity.this,
						AlertType.INFO_ALERT,
						getResources().getString(R.string.error_lbl),
						getString(R.string.str_launch_time_memory_warning),
						null, null, Messages.MESSAGE_DIALOG_LOW_MEMORY_ERROR,
						null, false);
			} else {
				openCaptureView();
			}
			break;
		case R.id.menu_edit_doc:
			Log.e(TAG, "Launching EditFieldsActivity from 7");
			if (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE && Constants.IS_APP_ONLINE){
				Intent i = new Intent(this, EditFieldsActivity.class);
				i.putExtra(Constants.STR_IS_NEW_ITEM, false);
				startActivity(i);
			}else{
				mCustomDialog
				.show_popup_dialog(ItemDetailsActivity.this, AlertType.ERROR_ALERT,
						getResources().getString(R.string.error_lbl),
						getResources().getString(R.string.app_msg_offline_operaton_failed),
						null, null,
						Messages.MESSAGE_DEFAULT,
						mHandler,
						false);
			}
			break;
		case R.id.menu_select:
			// 'select' option will work only if there are images available on
			// screen
			if ((mPageList != null) && (mPageList.size() > 0)) {
				if (mGridviewMode == mSelectionMode.SINGLE_MODE) {
					setMultipleSectionMode();
				}
			} else {
				Toast.makeText(this, R.string.str_user_msg_no_image_to_select,
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.menu_revert_doc:
			int selectionCount = getSelectionCount(gridView);
			if(selectionCount > 0) {
				if(selectionCount == 1) {
					msg =  getResources().getString(R.string.str_user_msg_revert_single_selected_image_confirmation);
				}
				else {
					msg =  getResources().getString(R.string.str_user_msg_revert_selected_images_confirmation);
				}
				mCustomDialog
				.show_popup_dialog(ItemDetailsActivity.this, AlertType.CONFIRM_ALERT,
						getResources().getString(R.string.str_user_msg_revert_confirmation_title),
						msg,
						null, null,
						Messages.MESSAGE_DIALOG_IMAGE_REVERT_CONFIRMATION,
						mHandler,
						false);
			}
			else {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.str_user_msg_no_image_selected), Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.menu_delete_doc:
			int selectionCount1 = getSelectionCount(gridView);
			if(selectionCount1 > 0) {
				if(selectionCount1 == 1) {
					msg =  getResources().getString(R.string.str_user_msg_delete_single_selected_image_confirmation);
				}
				else {
					msg =  getResources().getString(R.string.str_user_msg_delete_selected_images_confirmation);
				}
				mCustomDialog
				.show_popup_dialog(ItemDetailsActivity.this, AlertType.CONFIRM_ALERT,
						getResources().getString(R.string.str_user_msg_delete_confirmation_title),
						msg,
						null, null,
						Messages.MESSAGE_DIALOG_IMAGE_DELETE_CONFIRMATION,
						mHandler,
						false);
			}
			else {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.str_user_msg_no_image_selected), Toast.LENGTH_LONG).show();
			}

			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.d(TAG, "requestCode:: " + requestCode);
		refresh_list_flag = false;

		Globals.RequestCode myRequestCode = Globals.getRequestCode(requestCode);
		try {
			// Getting image from Gallery
			switch (myRequestCode) {
			case SELECT_DOCUMENT:
			case SELECT_PHOTO:
				if (resultCode == RESULT_OK) {
					if (null != data) {
						Uri imageUri = data.getData();
						String[] filePathColumns = { MediaColumns.DATA,
								MediaColumns.DISPLAY_NAME };

						Cursor cursor = getContentResolver().query(imageUri,
								filePathColumns, null, null, null);
						cursor.moveToFirst();
						int columnIndex = cursor
								.getColumnIndex(MediaColumns.DATA);

						Bitmap bmp = null;
						Image tempImage = null;
						// this is for images selected from Picasa albums
						// (Note: MediaColumns.DISPLAY_NAME is supported by
						// Picasa) .
						columnIndex = cursor
								.getColumnIndex(MediaColumns.DISPLAY_NAME);
						if (columnIndex != -1) {
							try {
								final InputStream is = getContentResolver()
										.openInputStream(imageUri);
								BufferedInputStream bufferedInputStream = new BufferedInputStream(
										is);
								bmp = BitmapFactory
										.decodeStream(bufferedInputStream);
								bufferedInputStream = null;
								if (bmp != null) {
									tempImage = new Image(bmp);
									bmp = null;
								}
								else if(bmp == null) {
									Toast.makeText(this, getResources().getString(R.string.toast_unable_to_decode_file), Toast.LENGTH_LONG).show();
								}
							} catch (FileNotFoundException e) {
								Toast.makeText(this, "" + e.getCause(), Toast.LENGTH_LONG).show();
								e.printStackTrace();
							}
						}
						cursor.close();
						if (tempImage != null) {
							String imageDiskLocation = null;
							if (mPrefUtils.sharedPref.getBoolean(
									mPrefUtils.KEY_QUICK_PREVIEW, true)) {
								//String imgLocation = mDiskMgrObj
								//		.saveImageToTempLocation(tempImage);
								if (myRequestCode == RequestCode.SELECT_DOCUMENT) {
									imageDiskLocation = acceptImage(
											Globals.ImageType.DOCUMENT.ordinal(),
											tempImage, mGallerySelPicExtn);
								} else {
									imageDiskLocation = acceptImage(
											Globals.ImageType.PHOTO.ordinal(),
											tempImage, mGallerySelPicExtn);
								}
								showQuickPreview(myRequestCode, imageDiskLocation);
							} else { // save image to disk
								if (myRequestCode == RequestCode.SELECT_DOCUMENT) {
									imageDiskLocation = acceptImage(
											Globals.ImageType.DOCUMENT.ordinal(),
											tempImage, mGallerySelPicExtn);
								} else {
									imageDiskLocation = acceptImage(
											Globals.ImageType.PHOTO.ordinal(),
											tempImage, mGallerySelPicExtn);
								}
								// open image gallery again to select next
								// image.
								// image gallery will be displayed until user
								// cancels on gallery screen
								/* if background image-processing is enabled or image-type is PHOTO, then launch gallery. 
								 * If image-type is DOCUMENT and background processing is disabled, wait for foreground image processing to complete and 
								 * launch gallery from broadcast receiver callback */
								if(Constants.BACKGROUND_IMAGE_PROCESSING || myRequestCode == RequestCode.SELECT_PHOTO) {
									openImageGallery(myRequestCode.ordinal());
								}
							}
							tempImage = null;
						}
						mGallerySelPicExtn = null;
					} else {
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.str_user_msg_no_image_selected), Toast.LENGTH_LONG).show();
					}
				} else if (resultCode == RESULT_CANCELED) {
					refresh_list_flag = true; // screen will be updated in onStart routine.
					//refreshList(mRefreshType.OLD_LIST);
				}
				break;
			case CAPTURE_DOCUMENT:
				if(data != null &&  data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
					mUtilityRoutines.offlineLogout(this);				
					return;
				}
				if (data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) > 0) {
					refresh_list_flag = true; // screen will be updated in onStart routine.
				}
				break;
			case PREVIEW_IMAGE:
				launchNext = -1;
				mBundle = null;
				if(data != null && data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
					mUtilityRoutines.offlineLogout(this);				
					return;
				}
				if (!mIsImageOpened) {
					if (data!=null){
						mBundle = data.getExtras();
					}

					if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
						refreshList(mRefreshType.OLD_LIST);
						mDocMgrObj.downloadDocTypeObject(mDocMgrObj.getCurrentDocTypeIndex());
					}

					mGallerySelPicExtn = null;
					// clear temp location
					mDiskMgrObj.deleteTempLocation();
					//if user has not selected'done' on preview screen, launch the gallery again.
					if(mBundle!=null && mBundle.getBoolean(Constants.STR_DONE) == false) {
						// if images are being captured for an item, launch
						// appropriate capture/gallery screen again
						launchNext = (int) data.getExtras().getInt(
								Constants.STR_IMG_SOURCE_TYPE);
						if (launchNext == RequestCode.SELECT_DOCUMENT
								.ordinal()
								|| launchNext == RequestCode.SELECT_PHOTO.ordinal()) {
							openImageGallery(launchNext);
						}
					}
					else {
						refreshList(mRefreshType.NEW_LIST);
						mCustomAdapter.notifyDataSetChanged();
					}
				} else {
					// if image was edited by selecting an image from existing
					// items, then simply refresh the screen
					if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
						Bundle b = data.getExtras();
						if(b.getBoolean(Constants.STR_DELETE,false)){
							refreshList(mRefreshType.NEW_LIST);
							mCustomAdapter.notifyDataSetChanged();
						}else{
							refreshList(mRefreshType.OLD_LIST);
							mCustomAdapter.notifyDataSetChanged();
						}
					}
					mIsImageOpened = false;
				}
				break;
			case EDIT_FIELDS:
				break;
			case EDIT_FIELDS_ON_ITEM_TYPE_CHANGE:
				//if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
				mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_updating_view),true);
				/* check if new document type contains any fields and update change_type menu icon accordingly */
				int field_count = data.getIntExtra(Constants.STR_VISIBLE_FIELD_COUNT, 0);
				if(field_count > 0) {
					mIsEditOptionDisable = false;
				}
				else {
					mIsEditOptionDisable = true;
				}
				mCustomDialog.closeProgressDialog();
				invalidateOptionsMenu();
				//}
				break;
			case SUBMIT_DOCUMENT:
				// if submit was successful, then close this screen.
				if(resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					Log.e(TAG, "Launching home screen");
					if(mIsFromPending){
					showHomeScreen();
					}
					finish();
				}
				break;
			case EDIT_FIELDS_VALIDATION:
				if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					ItemEntity itemEntity = mDBManager.getItemEntity();
					if (mDBManager.isDocumentSerializedInDB(itemEntity)) {
						Document DocumentObj = (Document)mDiskMgrObj.byteArrayToDocument(itemEntity.getItemSerializedData());
						if (!mDocMgrObj.validateDocumentFields(DocumentObj)) {
							showSubmitScreen();
						}
					}
				}
				break;
			default:
				break;
			}

		} catch (KmcRuntimeException e) {
			e.printStackTrace();
		} catch (KmcException e) {
			e.printStackTrace();
		}
	}



	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}

		if(mDocMgrObj != null){
			mDocMgrObj.removeCurrenthandler();
		}
		
		if(mLazyGridLoadTask != null) {
			mLazyGridLoadTask.cancel(true);
			mLazyGridLoadTask = null;
		}

		if(mMemoryCache != null){
			mMemoryCache.evictAll();
		}

		if (gridView != null) {
			gridView.removeAllViewsInLayout();
			gridView = null;
		}

		if (positionHash != null) {
			positionHash.clear();
			positionHash = null;
		}

		if(mTempList != null) {
			mTempList.clear();
			mTempList = null;
		}

		if(mCustomDialog != null) {
			mCustomDialog.closeProgressDialog();
		}
		mDocMgrObj = null;
		mDiskMgrObj = null;
		mDBManager = null;
		mProcessQueueMgr = null;
		mCustomDialog = null;
		mUtilityRoutines = null;
		System.gc();
	}

	@Override
	public void onBackPressed() {
		if (mGridviewMode == mSelectionMode.MULTIPLE_MODE) {
			mSelectedImgCount = 0;
			setSingleSelectionMode();
			return;
		}
		finish();
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void initObjects() {
		mDocMgrObj = DocumentManager.getInstance(getApplicationContext());
		mDiskMgrObj = DiskManager.getInstance(getApplicationContext());
		mPrefUtils = PrefManager.getInstance();
		mProcessQueueMgr = ImageProcessQueueManager.getInstance(getApplicationContext());
		mDBManager = DatabaseManager.getInstance();
		mCustomDialog = CustomDialog.getInstance();
		mUtilityRoutines = UtilityRoutines.getInstance();
	}

	private void initGridView() {
		//		mCustomAdapter = new CustomGridAdapter(getApplicationContext(), mPageList);
		gridView = (GridView) findViewById(R.id.gridView1);
		//		gridView.setAdapter(mCustomAdapter);
		gridView.setOnTouchListener(this);
		gridView.setOnItemLongClickListener(this);
		gridView.setOnItemClickListener(this);
	}
	
	private void lazyLoadGridOnLaunch() {
		final Handler tempHandler = new Handler();
		mProgressBar.setVisibility(View.VISIBLE);
		tempHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mLazyGridLoadTask = new LazyLoadGridImagesTask();
				mLazyGridLoadTask.execute();
			}
		}, 100);
	}

	private void initSubmitButton(String itemType) {
		Boolean usekofax = mPrefUtils.sharedPref.getBoolean(
				mPrefUtils.KEY_USE_KOFAX_SERVER, mPrefUtils.DEF_USE_KOFAX);
		msubmittokofax = (Button) findViewById(R.id.doc_submit_button);
		if(usekofax){
			String nickName =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_KFX_NICKNAME,mPrefUtils.DEF_KFX_NICKNAME);
			if(nickName != null && nickName.length() > 0){
				msubmittokofax.setText(getResources().getString(R.string.str_submit) + " " + getResources().getString(R.string.str_to) + " " + nickName);
			}else{
				msubmittokofax.setText(getResources().getString(R.string.str_submit) + " " + getResources().getString(R.string.str_to) + " " + mPrefUtils.DEF_KFX_NICKNAME);
			}
		}else{
			String nickName = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_USR_NICKNAME,mPrefUtils.DEF_USR_NICKNAME);
			if(nickName != null && nickName.length() > 0){
				msubmittokofax.setText(getResources().getString(R.string.str_submit) + " " + getResources().getString(R.string.str_to) + " " + nickName);
			}else{
				msubmittokofax.setText(getResources().getString(R.string.str_submit_server));
			}
		}

		// check if the selected existing item-type is valid (check if present in
		// itemtypes displayed on new tab)
		if(itemType != null) {
			int index = mDocMgrObj.findDocumentTypeIndex(itemType);
			if (index != -1) {
				mDocMgrObj.setCurrentDocTypeIndex(index);
			} else {
				isItemTypeValid = false;
				Toast.makeText(this, getResources().getString(R.string.str_user_msg_no_item_type_exists),
						Toast.LENGTH_LONG).show();
				//disable all the options on screen except delete and change-item-type.
				msubmittokofax.setEnabled(false);
				msubmittokofax.setTextColor(Color.GRAY);
			}
		}

		msubmittokofax.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				proceedToSubmit();
			}
		});
	}

	private void updateTitlebarcount(mSelectionMode mode){
		int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
		TextView titleText = (TextView) findViewById(titleId);

		if(mode == mSelectionMode.MULTIPLE_MODE){
			getActionBar().setIcon(R.drawable.done_gray);
			getActionBar().setTitle(mSelectedImgCount+" "+getResources().getString(R.string.lbl_selected));
			//setting action bar title text color to gray when multi selection mode ON	
			titleText.setTextColor(Color.GRAY);

		}else{
			//re-setting action bar title text color to white when multi selection mode OFF	
			titleText.setTextColor(Color.WHITE);
			if(mPageList != null){
				getActionBar().setTitle(getResources().getString(R.string.lbl_images)+"("+mPageList.size()+")");
			}else{
				getActionBar().setTitle(getResources().getString(R.string.lbl_images)+"(0)");
			}
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		}
	}

	private void updateMenuOptions(int position, boolean isSelected) {
		if(position >= 0) {
			if((mPageList != null) && (position < mPageList.size())) {
				if((mPageList.get(position).getImageType().equalsIgnoreCase(Globals.ImageType.DOCUMENT.name())) &&
						(mPageList.get(position).getProcessingStatus().intValue() == ProcessingStates.PROCESSED.ordinal())) {
					if(isSelected) {
						mProcessedImgSelectionCount++;
					}
					else {
						mProcessedImgSelectionCount--;
					}
				}
				if(mProcessedImgSelectionCount > 0){
					//set flag to show revert option
					mShowRevert = true;
				}
				else {
					//set flag to hide revert option
					mShowRevert = false;
				}
				invalidateOptionsMenu();
			}
		}
	}

	private boolean resetOnProcessingParamMismatch() {
		boolean isResetting = false;
		if(!isReferenceDocObjAvailable()) {
			if(mUtilityRoutines.checkInternet(this)) {
				try {
					mDocMgrObj.downloadDocTypeObject(mDocMgrObj.getCurrentDocTypeIndex());
					if(!Constants.BACKGROUND_IMAGE_PROCESSING) {
						mDocMgrObj.setCurrentHandler(mHandler);
					}
					mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_downloading_doc_details), false);
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
				} catch (KmcException e) {
					e.printStackTrace();
				}
			}
			return isResetting;
		}
		Document docObj = getSerializedDocObjectFromDatabase();
		if(docObj == null || docObj.getDocumentType() == null) {
			createAndSaveDocument();
		}
		else {
			//compare processing parameters(IPP and BSP) of existing serialized document object with the newly downloaded reference document object.
			DocumentType refDocTypeObj = mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex());
			boolean isMatching = mDocMgrObj.compareProcessingParams(docObj.getDocumentType(), refDocTypeObj);
			//Toast.makeText(this, "Processing parameters matching? :: " + isMatching, Toast.LENGTH_LONG).show();
			if(!isMatching) {
				isResetting = true;
				//Remove corresponding processed files from disk.
				List<PageEntity> pages = mDBManager.getPages(this, mDBManager.getItemEntity().getItemId(), Globals.ImageType.DOCUMENT.name(), ProcessingStates.PROCESSED);
				if(pages != null) {
					for(int j = 0; j<pages.size(); j++) {
						mDiskMgrObj.deleteImageFromDisk(pages.get(j).getProcessedImageFilePath());
					}
				}
				Log.e(TAG, "Resetting processing status!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				//update DB to reset processing status of all processed and processed-failed pages (except reverted images)of the corresponding item.
				mDBManager.setAllPagesSelectiveProcessingStatus(this, mDBManager.getItemEntity().getItemId(), ProcessingStates.PROCESSED, ProcessingStates.UNPROCESSED);
				mDBManager.setAllPagesSelectiveProcessingStatus(this, mDBManager.getItemEntity().getItemId(), ProcessingStates.PROCESSFAILED, ProcessingStates.UNPROCESSED);
				mDBManager.setAllPagesSelectiveProcessingStatus(this, mDBManager.getItemEntity().getItemId(), ProcessingStates.PROCESSING, ProcessingStates.UNPROCESSED);
	
				//update processing profiles of saved object with latest reference object profiles. 
				docObj.getDocumentType().setBasicSettingsProfile(refDocTypeObj.getBasicSettingsProfile());
				docObj.getDocumentType().setImagePerfectionProfile(refDocTypeObj.getImagePerfectionProfile());
	
				mDBManager.getItemEntity().setItemSerializedData(mDiskMgrObj.documentToByteArray(docObj));
				ProcessingParametersEntity ppEntity = mDBManager.getProcessingParametersEntity();
				if(null != ppEntity){
					ppEntity.setSerializeDocument(mDiskMgrObj.documentToByteArray(docObj));
					mDBManager.updateProcessingEntity(this, ppEntity);
				}
				mDBManager.update(getApplicationContext(), mDBManager.getItemEntity());
				docObj = null;
				
				if(!Constants.IS_HELPKOFAX_FLOW){
					if(isItemTypeValid == true) {
						if (Constants.BACKGROUND_IMAGE_PROCESSING) {
							Toast.makeText(this, getResources().getString(R.string.str_processing_all_images), Toast.LENGTH_LONG).show();
						}
						else {
							Toast.makeText(this, getResources().getString(R.string.str_process_all_images), Toast.LENGTH_LONG).show();
						}
					}

				refreshList(mRefreshType.OLD_LIST);	
				mProcessQueueMgr.addItemToQueue(mDBManager.getItemEntity());
			}
		}
		}
		return isResetting;
	}
	
	private boolean isReferenceDocObjAvailable() {
		return ((mDocMgrObj != null) && (mDocMgrObj.getDocTypeReferenceArray() != null) && (mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()) != null));
	}
	
	private Document getSerializedDocObjectFromDatabase() {
		Document docObj = null;
		if((mDBManager.getItemEntity().getItemSerializedData() != null) && (mDBManager.getItemEntity().getItemSerializedData().length > 0)) {
			docObj = (Document)mDiskMgrObj.byteArrayToDocument(mDBManager.getItemEntity().getItemSerializedData());
		}
		return docObj;
	}

	private void proceedToChangeItemType(int newItemIndex) {		
		mProcessQueueMgr.removeItemFromQueue(mDBManager.getItemEntity()
				.getItemId());

		if(mCustomDialog != null) {
			mCustomDialog.closeProgressDialog();
		}
		else {
			mCustomDialog = CustomDialog.getInstance();
		}
		//check if the respective reference object is already available. if yes, update the item-type.
		// Else, initiate documentType object download and change item-type then. 
		if(mDocMgrObj.getDocTypeFromRefArray(newItemIndex) != null) {
			isItemTypeValid = true;
			mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_updating_view),false);

			mDocMgrObj.setCurrentDocTypeIndex(newItemIndex);	//update currentDocType index with new type
			mDBManager.getItemEntity().setItemTypeName(mDocMgrObj.getDocTypeNamesList().get(newItemIndex)); //update item-type name in DB before beginning processing image with new type

			// on changing item-type, reset processing status of all
			// images to unprocessed.
			resetItemProcessingState(mDBManager.getItemEntity()
					.getItemId());
			if(Constants.BACKGROUND_IMAGE_PROCESSING && Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE) {
				mProcessQueueMgr.addItemToQueue(mDBManager.getItemEntity());
			}
			refreshList(mRefreshType.NEW_LIST);
			mCustomDialog.closeProgressDialog();
			invalidateOptionsMenu();

			Intent i = new Intent(getApplicationContext(),
					EditFieldsActivity.class);
			i.putExtra(Constants.STR_IS_NEW_ITEM, false);
			i.putExtra(Constants.STR_CHANGE_ITEM_TYPE, true);
			//pass index of new type
			i.putExtra(Constants.STR_NEW_ITEM_INDEX, newItemIndex); 
			startActivityForResult(
					i,
					Globals.RequestCode.EDIT_FIELDS_ON_ITEM_TYPE_CHANGE
					.ordinal());
			mCustomDialog.closeProgressDialog();
		}
		else {
			if(mUtilityRoutines.checkInternet(this)) {
				boolean isError = true;
				requestedItemTypeIndex = -1;
				mIsChangeItemTypeRequested = false;
				mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_downloading_doc_details),false);
				try {
					mDocMgrObj.downloadDocTypeObject(newItemIndex);
					requestedItemTypeIndex = newItemIndex;
					mIsChangeItemTypeRequested = true;
					isError = false;
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
					mCustomDialog.closeProgressDialog();
				} catch (KmcException e) {
					e.printStackTrace();
					mCustomDialog.closeProgressDialog();
				}
				if(isError) {
					mCustomDialog
					.show_popup_dialog(ItemDetailsActivity.this, AlertType.ERROR_ALERT,
							getResources().getString(R.string.error_lbl_download_error),
							getResources().getString(R.string.error_msg_error_document_download_for_new_type) + "\n" + getResources().getString(R.string.app_msg_try_again_later),
							null, null,
							Messages.MESSAGE_DIALOG_IMAGE_REVERT_CONFIRMATION,
							mHandler,
							false);
					mCustomDialog.closeProgressDialog();
				}
			}
		}
	}

	//Before submit document check all required fields are not null,if it is null display Editinfo screen
	private void proceedToSubmit(){
		if (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE && Constants.IS_APP_ONLINE) {
			boolean isRequiredEditinfoScreen = false;
			//		boolean result = mDiskMgrObj.isDocumentSerialized(mDocMgrObj.getOpenedDoctName());
			DocumentType doctype = mDocMgrObj.getDocTypeFromRefArray(mDocMgrObj.getCurrentDocTypeIndex());
			int visibleFieldCount = 0;
			if(mPrefUtils.isUsingKofax() && Constants.IS_HELPKOFAX_FLOW){
				int itemTypeIndex = mDocMgrObj.findDocumentTypeIndex(itemEntity.getItemTypeName());
				mDocMgrObj.setCurrentDocTypeIndex(itemTypeIndex);				
				showSubmitScreen();
			}else{
			if(doctype != null){  
				List<FieldType> currentFieldTypesList = doctype.getFieldTypes();
				// check if document object have any displayable fields. If yes, display edit-fields screen. If no, skip edit-fields screen and display submit screen directly.
				for (int i = 0; i < currentFieldTypesList.size(); i++) {
					if (!currentFieldTypesList.get(i).isHidden() && !currentFieldTypesList.get(i).getName().equalsIgnoreCase(Constants.STR_MOBILE_DEVICE_EMAIL)) {
						visibleFieldCount++;
					}
				}
			}
			if(mIsNewFlow){
				if(visibleFieldCount > 0){
					Intent i = new Intent(getApplicationContext(),
							EditFieldsActivity.class);
					i.putExtra(Constants.STR_IS_NEW_ITEM, true);
					i.putExtra(Constants.STR_VALIDATION, false);
					startActivityForResult(
							i,
							Globals.RequestCode.EDIT_FIELDS_VALIDATION
							.ordinal());
				}else{
					showSubmitScreen();
				}
			}else{
				if (mDBManager.isDocumentSerializedInDB(mDBManager.getItemEntity())) {
					Document DocumentObj = (Document)mDiskMgrObj.byteArrayToDocument(mDBManager.getItemEntity().getItemSerializedData());

					List<FieldType> currentFieldTypesList = doctype.getFieldTypes();
					List<FieldType> storedFieldTypes = DocumentObj.getDocumentType().getFieldTypes();

					isRequiredEditinfoScreen = mDocMgrObj.validateDocumentFields(DocumentObj);
					if(currentFieldTypesList.size() != storedFieldTypes.size()){
						isRequiredEditinfoScreen = true;
					}
					if(!isRequiredEditinfoScreen){
						showSubmitScreen();
					}
				}
				else{
					isRequiredEditinfoScreen = true;
				}
				if(isRequiredEditinfoScreen){
					Log.e(TAG, "Launching EditFieldsActivity from 3");
					Intent i = new Intent(getApplicationContext(),
							EditFieldsActivity.class);
					i.putExtra(Constants.STR_IS_NEW_ITEM, false);
					i.putExtra(Constants.STR_VALIDATION, true);
					startActivityForResult(
							i,
							Globals.RequestCode.EDIT_FIELDS_VALIDATION
							.ordinal());
				}
			}
		}
		}else{
			mCustomDialog
			.show_popup_dialog(ItemDetailsActivity.this, AlertType.ERROR_ALERT,
					getResources().getString(R.string.error_lbl),
					getResources().getString(R.string.app_msg_offline_operaton_failed),
					null, null,
					Messages.MESSAGE_DEFAULT,
					mHandler,
					false);
		}
	}

	// - private methods

	private boolean shouldScrollUp(final View targetView, final DragEvent event) {
		return targetView.getTop() <= 0 && event.getY() + targetView.getTop() < 200;
	}

	private boolean shouldScrollDown(View targetView, DragEvent event) {
		final int screenHeight = gridView.getMeasuredHeight();
		final int bottom = targetView.getBottom();
		if (bottom >= screenHeight) {
			final int itemHeight = targetView.getMeasuredHeight();
			final int visibleArea = itemHeight - (bottom - screenHeight);
			return visibleArea - event.getY() < 200;
		}
		return false;
	}

	private void createAndSaveDocument() {
		Log.e(TAG, "Enter:: createAndSaveDocument");
		if(mDocMgrObj.getDocTypeReferenceArray() == null) {
			return;
		}
		if(!mDBManager.isDocumentSerializedInDB(mDBManager.getItemEntity()) && mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()) != null) {
			Document mDocumentObj = new Document(mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()));
			//serialize document object onto disk
			byte[] barray = mDiskMgrObj.documentToByteArray(mDocumentObj);

			if(barray != null){
				ItemEntity item = mDBManager.getItemEntity();
				if(item != null){
					//save serialized object in DB
					item.setItemSerializedData(barray);
					ProcessingParametersEntity ppEntity = mDBManager.getProcessingParametersEntity();
					if(null != ppEntity){
						ppEntity.setSerializeDocument(barray);
						mDBManager.updateProcessingEntity(this, ppEntity);
					}
					mDBManager.update(getApplicationContext(), item);
					Log.e(TAG, "seralized object in DB!!!!!");
				}
				else {
					Log.e(TAG, "Item entity is null");
				}
			}
			else {
				Log.e(TAG, "barray is null!");
			}
			mDocumentObj = null;
		}
		else {
			Log.e(TAG, "Document is already serialized on DB!");
		}
		Log.e(TAG, "Exit:: createAndSaveDocument");
	}

	private String acceptImage(int imageType, Image pImg, String mimeType)
			throws KmcRuntimeException, KmcException {
		String imageLocation = null;

		long pageId = mDiskMgrObj.saveImageToDisk(pImg, mimeType,
				mDocMgrObj.getOpenedDoctName(), imageType);
		if(pageId != -1) {
			imageLocation = mDBManager.getPageForId(ItemDetailsActivity.this, pageId).getImageFilePath();
		}
		if(pageId != -1 && imageLocation != null) {
			refreshList(mRefreshType.OLD_LIST);
			mDiskMgrObj.updateItemImgCountInDetailsList(
					mDocMgrObj.getOpenedDoctName(), mPageList.size());
			mDocMgrObj.downloadDocTypeObject(mDocMgrObj.getCurrentDocTypeIndex());

			// add image to processing queue if image is of type DOCUMENT
			// (PHOTOS are not processed).
			if (imageType == Globals.ImageType.DOCUMENT.ordinal()) {
				if(!Constants.BACKGROUND_IMAGE_PROCESSING) {
					mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_please_wait),true);
				}
				if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mDBManager.isOfflineDocumentSerializedInDB(mDBManager.getProcessingParametersEntity())){
					mProcessQueueMgr.addItemToQueue(mDBManager.getItemEntity()); // gives the currently selected itemEntity
				}
			}
		} else {
			Toast.makeText(this, getResources().getString(R.string.toast_save_image_failed), Toast.LENGTH_LONG).show();
		}
		return imageLocation;
	}

	private void setupHandler() {
		mHandler = new Handler(new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {

				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];

				switch (whatMessage) {
				case MESSAGE_CHANGE_ITEM_TYPE:
					// if selected the current document-type again, then do
					// nothing.
					if (msg.arg1 != mDocMgrObj.getCurrentDocTypeIndex()) {
						proceedToChangeItemType(msg.arg1);
					}
					break;
				case MESSAGE_DIALOG_IMAGE_REVERT_CONFIRMATION:
					if (msg.arg1 == RESULT_OK) {
						revertSelectedItems();
						//not starting processing immediately after reverting images.
					}
					break;

				case MESSAGE_DIALOG_IMAGE_DELETE_CONFIRMATION:
					if (msg.arg1 == RESULT_OK) {
						deleteSelectedItems();
					}
					break;

				case MESSAGE_DIALOG_CHANGE_ITEM_TYPE_CONFIRMATION:
					if(msg.arg1 == RESULT_OK) {
						/* Display list of document-type names to select from */
						if(mTempList != null) {
							mTempList.clear();
							mTempList = null;
						}
						// if using kofax demo server, exclude 'Gift card' category while displaying the list of item-types to update
						if(mPrefUtils.isUsingKofax()) {
							mTempList = new ArrayList<String>(mDocMgrObj.getNonHelpDocumentNamesList().size() - 2);
							for(int i=0; i<mDocMgrObj.getNonHelpDocumentNamesList().size() - 2; i++) { 
								mTempList.add(i, mDocMgrObj.getNonHelpDocumentNamesList().get(i));
							}
							mCustomDialog.showSelectionAlert(ItemDetailsActivity.this, mTempList,
									mDocMgrObj.getCurrentDocTypeIndex(), mHandler);
						}
						else {

							List<String> doclist =   mDocMgrObj.getNonHelpDocumentNamesList();
							mCustomDialog.showSelectionAlert(ItemDetailsActivity.this, doclist,	
									mDocMgrObj.getCurrentDocTypeIndex(), mHandler);
						}
					}
					break;
				case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
					//Navigate back to settings
					if(msg.arg1 == RESULT_OK){
						mUtilityRoutines.offlineLogout(ItemDetailsActivity.this);
					}else{
						Globals.gAppModeStatus = Globals.AppModeStatus.FORCE_OFFLINEMODE;
					}
					break;
				 case MESSAGE_DOWNLOAD_DOCUMENTS_FAILED:
					 mCustomDialog.closeProgressDialog();
					 Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_error_field_details_cannot_be_downloaded), Toast.LENGTH_SHORT).show();
					 if(mIsDownloadStart){					 
						 mProcessQueueMgr.pauseQueue(mHandler);
					 }
					 break;
				default:
					break;
				}
				return true;
			}
		});
	}

	private void showHomeScreen() {
		Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void showSubmitScreen(){
		Intent submit_intent = new Intent(getApplicationContext(),
				SubmitDocument.class);
		submit_intent.putExtra(Constants.STR_IS_NEW_ITEM, mIsNewFlow);
		startActivityForResult(submit_intent,
				Globals.RequestCode.SUBMIT_DOCUMENT.ordinal());
	}

	private synchronized Bitmap ShrinkBitmap(String filePath, int width, int height) {
		Log.i(TAG, "file to shrint ========> " + filePath);

		Bitmap bitmap = null;
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
		bmpFactoryOptions.inJustDecodeBounds = true;

		if(mUtilityRoutines == null){
			mUtilityRoutines = UtilityRoutines.getInstance();
		}
		return mUtilityRoutines.getScaledBitmapFromFilepath(filePath);
	}

	private void refreshList(mRefreshType type) {
		Log.e(TAG, "Enter :: refreshList :: " + mDocMgrObj.getOpenedDoctName());

		long itemID = 0;

		if (itemEntity != null) {
			itemID = itemEntity.getItemId();
		}

		if (type == mRefreshType.NEW_LIST) {
			ArrayList<PageEntity> newPageList = (ArrayList<PageEntity>) mDBManager
					.getAllPagesForItem(getApplicationContext(), itemID);

			if (newPageList != null && newPageList.size() > 0) {
				mPageList = new ArrayList<PageEntity>(newPageList.size());

				for (int j = 0; j < newPageList.size(); j++) {
					mPageList.add(newPageList.get(j));
				}
			}else{
				mPageList.clear();
			}
		} else {
			ArrayList<PageEntity> updatedList = (ArrayList<PageEntity>) mDBManager
					.getAllPagesForItem(getApplicationContext(), itemID);

			if (mPageList != null && updatedList != null
					&& updatedList.size() > mPageList.size()) {
				int difference = updatedList.size() - mPageList.size();
				for (int j = 0; j < difference; j++) {
					mPageList.add(updatedList.get(mPageList.size()));
				}

			}
		}

		if (mPageList != null) {

			mCustomAdapter = new CustomGridAdapter(getApplicationContext(),
						mPageList);
			gridView.setAdapter(mCustomAdapter);

			if (mPageList.size() > 0) {
				enableScreenOptions();
			} else {
				disableScreenOptions();
			}
			updatePositionHashMap(mPageList);
		} else {
			disableScreenOptions();
		}
		updateTitlebarcount(mGridviewMode);
	}

	/**
	 * Function to reset the processing status of all pages(except reverted ones) for the given itemId
	 * to UNPROCESSED. This is generally called when item-type is changed and
	 * all images under the item should be reprocessed as per processing
	 * parameters of new item type.
	 * 
	 * @param itemId
	 */
	private void resetItemProcessingState(Long itemId) {
		List<PageEntity> pages = mDBManager.getAllPagesForItem(
				getApplicationContext(), itemId);
		if (pages != null && pages.size() > 0) {
			// first delete all processed images from disk
			for (int i = 0; i < pages.size(); i++) {

				Log.i(TAG, "Processed image file path ================> " + pages.get(i)
						.getProcessedImageFilePath());
				mDiskMgrObj.deleteImageFromDisk(pages.get(i)
						.getProcessedImageFilePath());
				if(mMemoryCache != null && pages.get(i).getProcessedImageFilePath() != null) {
					mMemoryCache.remove(pages.get(i)
							.getProcessedImageFilePath());
				}

			}
			// update DB after all processed images are deleted.
			if(Constants.BACKGROUND_IMAGE_PROCESSING){
				mDBManager.setAllPagesSelectiveProcessingStatus(this, itemId, ProcessingStates.PROCESSED, ProcessingStates.UNPROCESSED);
				mDBManager.setAllPagesSelectiveProcessingStatus(this, itemId, ProcessingStates.PROCESSFAILED, ProcessingStates.UNPROCESSED);
				mDBManager.setAllPagesSelectiveProcessingStatus(this, itemId, ProcessingStates.PROCESSING, ProcessingStates.UNPROCESSED);
			}else {
				mDBManager.setAllPagesSelectiveProcessingStatus(this, itemId, ProcessingStates.PROCESSED, ProcessingStates.REVERTED);
				mDBManager.setAllPagesSelectiveProcessingStatus(this, itemId, ProcessingStates.PROCESSFAILED, ProcessingStates.REVERTED);
			}
		}
	}

	@SuppressLint("UseSparseArrays")
	private void updatePositionHashMap(ArrayList<PageEntity> pageList) {
		if (positionHash != null) {
			positionHash.clear();
			positionHash = null;
		}

		if (pageList != null && pageList.size() > 0) {
			positionHash = new HashMap<Long, Integer>(pageList.size());
			for (int i = 0; i < pageList.size(); i++) {
				positionHash.put(pageList.get(i).getPageId(), i);
			}
		}
	}

	private void openImageGallery(int option) {
		Intent galleryIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(galleryIntent, option);
	}

	private void openCaptureView() {
		Intent cameraIntent = new Intent(getApplicationContext(), Capture.class);

		int imgCount = 0;
		if (mCustomAdapter != null) {
			imgCount = mCustomAdapter.getCount();
		}
		cameraIntent.putExtra(Constants.STR_IMAGE_COUNT, imgCount);
		cameraIntent.putExtra(Constants.STR_IS_NEW_ITEM, false);

		if(mPageList != null && mPageList.size() > 0) {
			//send path of last image in item. This is to display thumbnail on capture screen
			cameraIntent.putExtra(Constants.LAST_IMAGE_PATH, mPageList.get(mPageList.size() - 1).getImageFilePath());
		}
		startActivityForResult(cameraIntent,
				RequestCode.CAPTURE_DOCUMENT.ordinal());
	}

	/**
	 * @param captureSource
	 */
	private void showQuickPreview(RequestCode captureSource, String imgLocation) {

		Intent i = new Intent(this, QuickPreviewActivity.class);
		i.putExtra(Constants.STR_URL, imgLocation);
		/*
		 * quick_preview is true only when image is just captured and not
		 * accepted yet. It is false when image is selected by tapping on
		 * thumbnail on item detail screen
		 */
		i.putExtra(Constants.STR_QUICK_PREVIEW, true);
		i.putExtra(Constants.STR_IMG_SOURCE_TYPE, captureSource.ordinal());

		startActivityForResult(i,
				Globals.RequestCode.PREVIEW_IMAGE.ordinal());
	}

	private void reorderGrids(ArrayList<PageEntity> items, int source_pos,
			int dest_pos) {
		// Check for empty list
		if ((null == items) || ((null != items) && (0 == items.size()))) {
			return;
		}

		if (source_pos != dest_pos) {
			// Copy the source_pos to a temp variable;
			PageEntity temp_Obj = items.get(source_pos);

			if (source_pos > dest_pos) {
				for (int i = source_pos; i > dest_pos; i--) {
					items.set(i, items.get(i - 1));
				}
			} else { /* source_pos < dest_pos */
				for (int i = source_pos; i < dest_pos; i++) {
					items.set(i, items.get(i + 1));
				}
			}
			items.set(dest_pos, temp_Obj);
		}
		updatePositionHashMap(items);
		return;
	}

	private void registerBroadcastReceiver() {
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.i(TAG,
							"Broadcast received!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					if (intent.getAction() == Constants.CUSTOM_INTENT_IMAGE_CAPTURED) {
						Log.i(TAG,
								"Broadcast received CUSTOM_INTENT_IMAGE_CAPTURED");
						if (!((mDocMgrObj.getDocTypeReferenceArray() != null) && (mDocMgrObj.getDocTypeReferenceArray().get(mDocMgrObj.getCurrentDocTypeIndex()) != null))){ 
							try {
								mDocMgrObj.downloadDocTypeObject(mDocMgrObj
										.getCurrentDocTypeIndex());
							} catch (KmcRuntimeException e) {
								e.printStackTrace();
							} catch (KmcException e) {
								e.printStackTrace();
							}
						}
					} else if (intent.getAction() == Constants.CUSTOM_INTENT_IMAGE_PROCESSING_STARTED ||
							intent.getAction() == Constants.CUSTOM_INTENT_IMAGE_PROCESSED) {
						Log.i(TAG,
								"Broadcast received CUSTOM_INTENT_IMAGE_PROCESSED");
						Long itemId = intent.getLongExtra(Constants.STR_ITEM_ID, -1);
						/*
						 * check if item id matches with the currently opened
						 * item, if so, update the processing success/failure
						 * icon accordingly
						 */
						Log.i(TAG, "Current Item ID ==> " + itemEntity.getItemId());
						Log.i(TAG, "Processed ItemID ==> " + itemId);
						if (itemId.equals(itemEntity.getItemId())) {
							Log.i(TAG,
									"Item id matches with the currently opened item.");

							Long pageId = intent.getLongExtra(Constants.STR_PAGE_ID, -1);

							if (positionHash != null && positionHash.containsKey(pageId)) {
								Log.i(TAG,
										"PositionHashMap Contains page just processed! :)");
								int position = positionHash.get(pageId);
								Log.i(TAG,
										"Index of page in PositionHashMap is => "
												+ position);
								// update image in gridview for at the found
								// position
								mPageList
								.get(position)
								.setProcessingStatus(
										mDBManager
										.getPageForId(
												getApplicationContext(),
												pageId)
												.getProcessingStatus());
								if(mGridviewMode == mSelectionMode.MULTIPLE_MODE){
									mSelectedImgCount = 0;								 
								}

								refreshList(mRefreshType.OLD_LIST);

							}
						}
						else {
							Log.e(TAG,
									"Item id DOES NOT match with currently opened item.!!!!!!!!!!!!!!!!");
						}
						
						if(intent.getAction() == Constants.CUSTOM_INTENT_IMAGE_PROCESSED && intent.hasExtra(Constants.STR_PROCESS_STATUS) && intent.getIntExtra(Constants.STR_PROCESS_STATUS, 0) != 0){
							Toast.makeText(context, getResources().getString(R.string.toast_processing_failed), Toast.LENGTH_SHORT).show();
						}
					}
					else if (intent.getAction() == Constants.CUSTOM_INTENT_DOCTYPE_DOWNLOADED) {
						String docType = intent.getStringExtra(Constants.STR_DOCUMENT_TYPE);
						Log.i(TAG, "Broadcast CUSTOM_INTENT_DOCTYPE_DOWNLOADED");

						if(mIsChangeItemTypeRequested) {
							if(requestedItemTypeIndex < 0) {
								mIsChangeItemTypeRequested = false;
							}
							else {
								//match the documentType requested before proceeding with changing item-type
								String mRequestedItemTypeName = mDocMgrObj.getDocTypeNamesList().get(requestedItemTypeIndex);

								if(mRequestedItemTypeName == null) {
									mIsChangeItemTypeRequested = false;
								}
								else if(mRequestedItemTypeName.equals(docType)) {
									mIsChangeItemTypeRequested = false;
									proceedToChangeItemType(requestedItemTypeIndex);
								}
							}
						}
						else {
							Log.i(TAG, "Downloaded doc type ====> " + docType);
							Log.i(TAG, "Opened document name ====> " + mDocMgrObj.getDocTypeNamesList().get(mDocMgrObj.getCurrentDocTypeIndex()));
							if(docType.equalsIgnoreCase(mDocMgrObj.getDocTypeNamesList().get(mDocMgrObj.getCurrentDocTypeIndex()))) {
								mCustomDialog.closeProgressDialog();
								resetOnProcessingParamMismatch();
							}
						}
					}
				}
			};
		}
		IntentFilter intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_IMAGE_CAPTURED);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_IMAGE_PROCESSING_STARTED);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_IMAGE_PROCESSED);
		registerReceiver(mReceiver, intentFilter);
		intentFilter = new IntentFilter(
				Constants.CUSTOM_INTENT_DOCTYPE_DOWNLOADED);
		registerReceiver(mReceiver, intentFilter);
	}

	private void setSingleSelectionMode() {
		//change actionbar background color back to theme color
		ColorDrawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.appbgblue));
		getActionBar().setBackgroundDrawable(colorDrawable);

		mGridviewMode = mSelectionMode.SINGLE_MODE;
		invalidateOptionsMenu();

		mCustomAdapter = new CustomGridAdapter(getApplicationContext(),
				mPageList);
		gridView.setAdapter(mCustomAdapter);

		gridView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
		mSelectedImgCount = 0;
		updateTitlebarcount(mGridviewMode);
		showSubmit();
	}

	private void setMultipleSectionMode() {
		mProcessedImgSelectionCount = 0;
		//change actionbar background color to white
		getActionBar().setBackgroundDrawable(
				new ColorDrawable(Color.WHITE));

		mGridviewMode = mSelectionMode.MULTIPLE_MODE;
		invalidateOptionsMenu();

		mCustomAdapter = new CustomGridAdapter(getApplicationContext(),
				mPageList);
		gridView.setAdapter(mCustomAdapter);

		gridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
		mSelectedImgCount = 0;
		updateTitlebarcount(mGridviewMode);
		hideSubmit();
	}

	private int getColumnWidth(Context context, GridView gridView) {
		Display display = getWindowManager().getDefaultDisplay();
		int width = 0;
		if (android.os.Build.VERSION.SDK_INT >= 13) {
			width = display.getWidth();
		} else {
			Point size = new Point();
			display.getSize(size);
			width = size.x;
		}

		Resources res = context.getResources();
		int lPad = (int) res.getDimension(R.dimen.left_padding);
		int rPad = (int) res.getDimension(R.dimen.right_padding);
		int hSpace = (int) res.getDimension(R.dimen.horizontal_spacing);
		return (width - lPad - rPad + hSpace) / 3 - hSpace;
	}

	private void revertSelectedItems() {
		Log.d(TAG, "child count: " + gridView.getChildCount());
		for (int i = 0; i < selectedlist.size(); i++) {
			if (selectedlist.get(i).isselected) {
				PageEntity pageEntity = mPageList.get(i);
				if (pageEntity.getProcessingStatus() == ProcessingStates.PROCESSED
						.ordinal() || 
						pageEntity.getProcessingStatus() == ProcessingStates.PROCESSFAILED
						.ordinal()) {
					// Delete processed image from disk
					mDiskMgrObj.deleteImageFromDisk(pageEntity
							.getProcessedImageFilePath());
					// updated image status in DB.
					pageEntity
					.setProcessingStatus((long)ProcessingStates.REVERTED
							.ordinal());
				}
				//				}
			}
		}
		mSelectedImgCount = 0;
		updateTitlebarcount(mGridviewMode);
		refreshList(mRefreshType.OLD_LIST);
	}

	private void deleteSelectedItems() {
		Log.d(TAG, "child count: " + gridView.getChildCount());
		int count = 0;
		//selectedlist
		for (int i = 0; i < selectedlist.size(); i++) {
			if (selectedlist.get(i).isselected) {
				
				if (mPageList != null && mPageList.size() >= 0) {
					// should not open the image whose processing is currently going on.
					if (mPageList.get(i - count).getProcessingStatus() == ProcessingStates.PROCESSING.ordinal()) {
						continue;
				}
				}

				PageEntity pageEntity = mPageList.get(i - count);

				mDiskMgrObj.deleteImageFromDisk(pageEntity
						.getImageFilePath());
				//delete corresponding processed image (if exists) as well.
				mDiskMgrObj.deleteImageFromDisk(pageEntity
						.getProcessedImageFilePath());

				mDBManager.deletePageWithId(getApplicationContext(),pageEntity.getPageId());
				mPageList.remove(i - count);

				count++;
				//				}
			}
		}
		mDiskMgrObj.updateItemImgCountInDetailsList(
				mDocMgrObj.getOpenedDoctName(), mPageList.size());
		refreshList(mRefreshType.OLD_LIST);

		// if all the images are deleted, exist from selection mode
		if (mPageList.size() <= 0) {
			setSingleSelectionMode();
		}
		mSelectedImgCount = 0;
		updateTitlebarcount(mGridviewMode);
	}

	private int getSelectionCount(GridView gridView) {
		int count = 0;
		for (int i = 0; i < gridView.getChildCount(); i++) {
			View borderView = gridView.getChildAt(i).findViewById(
					R.id.selectionBorderOverlay);
			if (borderView != null) {
				if (borderView.getVisibility() == View.VISIBLE) {
					count++;
				}
			}
		}

		return count;
	}


	/**
	 * Function to disable 'Submit' and 'Select' options when there are no
	 * images present in item.
	 */
	private void enableScreenOptions() {
		if (isItemTypeValid == true) {
			findViewById(R.id.doc_submit_button).setEnabled(true);
			((Button) findViewById(R.id.doc_submit_button)).setTextColor(Color.WHITE);
		}
	}

	/**
	 * Function to disable 'Submit' and 'Select' options when there are no
	 * images present in item.
	 */
	private void disableScreenOptions() {
		findViewById(R.id.doc_submit_button).setEnabled(false);
		((Button)findViewById(R.id.doc_submit_button)).setTextColor(Color.GRAY);
	}

	private void showSubmit() {
		findViewById(R.id.doc_submit_button).setVisibility(View.VISIBLE);
	}

	private void hideSubmit() {
		findViewById(R.id.doc_submit_button).setVisibility(View.GONE); 
	}

	@Override
	public void onNetworkChanged(boolean isConnected) {
		if(Globals.isRequiredOfflineAlert() && isConnected && mUtilityRoutines.isAppOnForeground(ItemDetailsActivity.this)){
			//mIsOfflineAlertRequest = true;
			if(mCustomDialog != null){
				mCustomDialog.dismissAlertDialog();
				mCustomDialog.show_popup_dialog(ItemDetailsActivity.this,AlertType.CONFIRM_ALERT,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
						getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION, mHandler, false);	
			}
		}	
	}

	private class LazyLoadGridImagesTask extends AsyncTask<String, Void, String> {

		boolean mResetOnProcessingParamMismatch;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mResetOnProcessingParamMismatch = resetOnProcessingParamMismatch();

		}
		
		@Override
		protected String doInBackground(String... params) {
			ItemDetailsActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					if(!Constants.IS_HELPKOFAX_FLOW){
					if(!mResetOnProcessingParamMismatch) {
						refreshList(mRefreshType.NEW_LIST);
						if(isItemTypeValid && Constants.BACKGROUND_IMAGE_PROCESSING && (Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE || mDBManager.isOfflineDocumentSerializedInDB(mDBManager.getProcessingParametersEntity()))) {
							mDocMgrObj.setCurrentHandler(mHandler);
							mIsDownloadStart = true;
							if(mProcessQueueMgr.isQueuePaused()){
								mProcessQueueMgr.resumeQueue();
							}
							mProcessQueueMgr.addItemToQueue(mDBManager.getItemEntity());
						}
					}
				}else{
					refreshList(mRefreshType.NEW_LIST);
				}
				}
			});
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mProgressBar.setVisibility(View.INVISIBLE);
//			resetOnProcessingParamMismatch();
		}
	}
}
