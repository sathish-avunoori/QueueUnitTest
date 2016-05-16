// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.kmc.klo.logistics.data.Document;
import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.DiskManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.dbentities.PageEntity;
import com.kofax.mobilecapture.dbentities.ProcessingParametersEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.AppModeStatus;
import com.kofax.mobilecapture.utilities.Globals.Messages;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class ExistingItemsActivity extends Activity implements OnItemClickListener,OnItemLongClickListener,NetworkChangedListener{

	// - public enums
	public enum ListModes {
		DISPLAY_MODE, EDIT_MODE
	}

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String TAG = ExistingItemsActivity.class.getSimpleName();

	// - Private data.
	/* SDK objects */
	/* Application objects */

	private DiskManager mDiskMgrObj = null;
	private DocumentManager mDocMgrObj = null;
	private PrefManager mPrefUtils = null;
	private UtilityRoutines mUtilRoutinesObj = null;
	private CustomDialog mCustomDialog = null;
	/* Standard variables */
	private BroadcastReceiver mReceiver = null;
	private ArrayList<ListComposer> mDocList;
	private MyListAdapter mListAdapter;
	private ListView mListView;
	private View mTopview = null; 
	public ListModes ExistingListMode = ListModes.DISPLAY_MODE;
	String mAppRootDir = null;

	private int mSelectedItemIndex = -1;
	private int mSelectedRowIndex = -1;
	private boolean refreshExistingList = false;
	private boolean mIsEditOptionDisable = false;
	private TextView mNoInternetConnectionView = null;
	DatabaseManager mItemDBManager = null;

	int mSelect_count = 0;  // get multiple selection count

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new DeviceSpecificIssueHandler().checkEntryPoint(this);

		setContentView(R.layout.existingitemlist);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setIcon(
					new ColorDrawable(getResources().getColor(android.R.color.transparent))); 
		}

		/* Display screen data only when a valid user is logged in */
		
			mDiskMgrObj = DiskManager.getInstance(getApplicationContext());
			mDocMgrObj = DocumentManager.getInstance(getApplicationContext());
			mPrefUtils = PrefManager.getInstance();
			mUtilRoutinesObj = UtilityRoutines.getInstance();
			mCustomDialog = CustomDialog.getInstance();

			mAppRootDir = mUtilRoutinesObj.getAppRootPath(getApplicationContext());

			mItemDBManager = DatabaseManager.getInstance();
			mDocList = new ArrayList<ListComposer>();

			mTopview = (View)findViewById(R.id.editmodeitem_view);
			List<ItemEntity> itemList = getPendingList();			

			if (itemList != null && itemList.size() > 0) {
				hideEmptyScreenMessage();
				for (int i = 0; i < itemList.size(); i++) {
					Date d = itemList.get(i).getItemCreatedTimeStamp();
					SimpleDateFormat formater = new SimpleDateFormat(
							"dd MMM, yyyy | hh:mm a");
					String str_date = formater.format(d);

					int pagescount = mItemDBManager.getAllPagesCountForItem(
							this, itemList.get(i).getItemId());
					if(itemList.get(i).getFieldName() == null){
						itemList.get(i).setFieldName(Constants.STR_EMPTY_FIELD);
					}
					mDocList.add(new ListComposer(itemList.get(i).getItemId(),
							itemList.get(i).getItemTypeName(), itemList.get(i)
							.getItemName(), mItemDBManager
							.constructItemURL(getApplicationContext(), itemList
									.get(i).getItemId(), mAppRootDir), str_date,
									pagescount,itemList.get(i).getFieldName()));

				}
				itemList = null;
			} else {
				showEmptyScreenMessage();
			}
			getActionBar().setTitle(getResources().getString(R.string.title_section2)+"("+mDocList.size()+")");
			// get the listview
			mListView = (ListView)findViewById(R.id.itemList);

			mListAdapter = new MyListAdapter(this, mDocList);

			// setting list adapter
			mListView.setAdapter(mListAdapter);
			mListView.setOnItemClickListener(this);
	
		mNoInternetConnectionView = (TextView) findViewById(R.id.noInternetConnectionTextView);
		registerBroadcastReceiver();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (refreshExistingList == true) {
			refreshExistingList = false;
			changeListMode(true);
		}

	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		Constants.IS_HELPKOFAX_FLOW = false;
		if (mListAdapter != null) {
			if(mSelectedRowIndex != -1) {
				refreshItem(mSelectedRowIndex);
			}
			else {
				mListAdapter.notifyDataSetChanged();
			}
		}
		Constants.NETWORK_CHANGE_LISTENER = ExistingItemsActivity.this;
		if(!mUtilRoutinesObj.checkInternet(ExistingItemsActivity.this) || Globals.gAppModeStatus ==  AppModeStatus.FORCE_OFFLINEMODE){
			mNoInternetConnectionView.setVisibility(View.VISIBLE);
		}else{
			mNoInternetConnectionView.setVisibility(View.GONE);
		}
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (Globals.RequestCode.values().length > requestCode) {
			Globals.RequestCode myRequestCode = Globals
					.getRequestCode(requestCode);
			switch (myRequestCode) {
			case EDIT_FIELDS_VALIDATION:
				if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					ItemEntity itemEntity = mItemDBManager.getItemEntity();
					if (mItemDBManager.isDocumentSerializedInDB(itemEntity)) {
						Document DocumentObj = (Document)mDiskMgrObj.byteArrayToDocument(itemEntity.getItemSerializedData());
						boolean isRequiredEditinfoScreen = mDocMgrObj.validateDocumentFields(DocumentObj);
						if (!isRequiredEditinfoScreen) {
							submitDocument();
						}
					}
				}
				break;
			case EDIT_FIELDS: 
				if (mSelectedRowIndex != -1) {
					refreshItem(mSelectedRowIndex);
				}
			case CAPTURE_DOCUMENT:
			case SHOW_ITEM_DETAILS:
				if(data != null && data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
					mUtilRoutinesObj.offlineLogout(ExistingItemsActivity.this);					
					return;
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//open item only if list is not in selection mode.
		if(ExistingListMode == ListModes.DISPLAY_MODE) {
			mSelectedRowIndex = position;
			openItemDetailScreenFromItem(position);
			try {
				//initiate download of documenttype object only if document-type is still valid
				if(isDocumentTypeValid()) {
					mDocMgrObj.downloadDocTypeObject(mDocMgrObj.findDocumentTypeIndex(mItemDBManager.getItemEntity().getItemTypeName()));
				}
			} catch (KmcRuntimeException e) {
				e.printStackTrace();
			} catch (KmcException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		mDocList.get(position).selected = true;
		changeListMode(true);
		return false;
	}


	@SuppressLint("NewApi")
	public void animateListView() {
		Interpolator accelerator = new AccelerateInterpolator();
		ObjectAnimator visToInvis = ObjectAnimator.ofFloat(mListView,
				"rotationY", 0f, 360f);
		visToInvis.setDuration(500);
		visToInvis.setInterpolator(accelerator);
		visToInvis.start();
	}

	public void changeListMode(boolean edit_status) {
		if (edit_status) {
			ExistingListMode = ListModes.DISPLAY_MODE;
			mSelect_count = 0;
			updateTitlebar(ExistingListMode);
			List<ItemEntity> itemList = getPendingList();			
						mDocList.clear();
					if (itemList != null && itemList.size() > 0) {
						hideEmptyScreenMessage();
						for (int i = 0; i < itemList.size(); i++) {

							Date d = itemList.get(i).getItemCreatedTimeStamp();
							SimpleDateFormat formater = new SimpleDateFormat(
									"dd MMM, yyyy | hh:mm a");
							String str_date = formater.format(d);

							int pagescount = mItemDBManager.getAllPagesCountForItem(
									this, itemList.get(i).getItemId());
							if(itemList.get(i).getFieldName() == null){
								itemList.get(i).setFieldName(Constants.STR_NO_FIELDS);
							}
							mDocList.add(new ListComposer(itemList.get(i).getItemId(),
									itemList.get(i).getItemTypeName(), itemList.get(i)
									.getItemName(), mItemDBManager
									.constructItemURL(getApplicationContext(), itemList
											.get(i).getItemId(), mAppRootDir), str_date,
											pagescount,itemList.get(i).getFieldName()));

						}
						itemList = null;
					} else {
						showEmptyScreenMessage();
					}

					for (int i = 0; i < mDocList.size(); i++) {
						mDocList.get(i).selected = false;
					}
					getActionBar().setTitle(getResources().getString(R.string.title_section2)+"("+mDocList.size()+")");
					mListAdapter = new MyListAdapter(getApplicationContext(), mDocList);

					// setting list adapter
					mListView.setAdapter(mListAdapter);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pending_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(ExistingListMode == ListModes.EDIT_MODE){
			for(int i = 0;i < menu.size();i++){
				switch (i) {
				case 0:
					menu.getItem(i).setIcon(R.drawable.delete_gray);
					break;
				case 1:
					menu.getItem(i).setIcon(R.drawable.add_document_gray);
					break;
				case 2:
					menu.getItem(i).setIcon(R.drawable.more_gray);
					break;
				default:
					break;
				}
			}	           
			if(mSelect_count == 0){
				for (int i = 0; i < menu.size(); i++) {
					if(i <= 2){
						menu.getItem(i).setVisible(false);              
					}
				}
			}else if(mSelect_count == 1){
				for (int i = 0; i < menu.size(); i++) {
					if (i <= 2) {
						menu.getItem(i).setVisible(true);
					}
				}
				if(mIsEditOptionDisable){
					mIsEditOptionDisable = false;
					menu.getItem(2).getSubMenu().getItem(0).setEnabled(false);
				}else{
					menu.getItem(2).getSubMenu().getItem(0).setEnabled(true);
				}
				
				if(Constants.IS_HELPKOFAX_FLOW){
					menu.getItem(2).getSubMenu().getItem(0).setVisible(false);
				}
			} else {
				for (int i = 0; i < menu.size(); i++) {
					if(i == 0){
						menu.getItem(i).setVisible(true);	            
					}else if(i <= 2){
						menu.getItem(i).setVisible(false);
					}
				}
			}
		}else{
			for (int i = 0; i < menu.size(); i++) {
				switch (i) {
				case 0:
					menu.getItem(i).setIcon(R.drawable.delete);
					break;
				case 1:
					menu.getItem(i).setIcon(R.drawable.add_document);
					break;
				case 2:
					menu.getItem(i).setIcon(R.drawable.more);
					break;
				default:
					break;
				}
			}
			for (int i = 0; i < menu.size(); i++) {
				if(i <= 2){
					menu.getItem(i).setVisible(false);              
				}
			}
		}
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if(ExistingListMode == ListModes.EDIT_MODE){	               
				changeListMode(true);
			}
			else {
				finish();
			}
			return true;	   
		case R.id.menu_pending_more:
			break;
		case R.id.menu_pending_add_doc:
			if(!isDocumentTypeValid()) {
				Toast.makeText(this, getResources().getString(R.string.str_user_msg_no_item_type_exists) + getResources().getString(R.string.str_user_msg_change_document_type_before_using_option),
						Toast.LENGTH_LONG).show();
			}
			else {
				if (mUtilRoutinesObj.getAvailableMemorySize() <= Constants.CAPTURE_LAUNCH_MEMORY) {
					mCustomDialog
							.show_popup_dialog(
									ExistingItemsActivity.this,
									AlertType.INFO_ALERT,
									getResources()
											.getString(R.string.error_lbl),
									getString(R.string.str_launch_time_memory_warning),
									null, null,
									Messages.MESSAGE_DIALOG_LOW_MEMORY_ERROR,
									null, false);
				} else {
					OpenitemdetailActivity();
					changeListMode(true);
				}
			}
			break;
		case R.id.menu_pending_edit_doc:
			if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE && Constants.IS_APP_ONLINE){
			if(!isDocumentTypeValid()) {
				Toast.makeText(this, getResources().getString(R.string.str_user_msg_no_item_type_exists) + getResources().getString(R.string.str_user_msg_change_document_type_before_using_option),
						Toast.LENGTH_LONG).show();
			}
			else {
				OpenEditFieldActivity();
				changeListMode(true);
			}
			}else{
				mCustomDialog
				.show_popup_dialog(ExistingItemsActivity.this, AlertType.ERROR_ALERT,
						getResources().getString(R.string.error_lbl),
						getResources().getString(R.string.app_msg_offline_operaton_failed),
						null, null,
						Messages.MESSAGE_DEFAULT,
						existingHandler,
						false);
			}
			break;	      	      
		case R.id.menu_pending_delete_doc:
			DispalyConfirmationMessage();
			break;
		case R.id.menu_pending_submit_doc:
			if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE && Constants.IS_APP_ONLINE){
			if(!isDocumentTypeValid()) {
				Toast.makeText(this, getResources().getString(R.string.str_user_msg_no_item_type_exists) + getResources().getString(R.string.str_user_msg_change_document_type_before_using_option),
						Toast.LENGTH_LONG).show();
			}
			else {
				openSubmitDocument();
			}
			}else{
				mCustomDialog
				.show_popup_dialog(ExistingItemsActivity.this, AlertType.ERROR_ALERT,
						getResources().getString(R.string.error_lbl),
						getResources().getString(R.string.app_msg_offline_operaton_failed),
						null, null,
						Messages.MESSAGE_DEFAULT,
						existingHandler,
						false);
			}
			break;
		default:
			break;
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		if(ExistingListMode == ListModes.EDIT_MODE){
			changeListMode(true);
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Enter:: onDestroy - ExistingItemList");
		super.onDestroy();

		unRegisterBroadcastReceiver();

		if (mDocList != null) {
			for (int i = 0; i < mDocList.size(); i++) {
				ListComposer obj = mDocList.remove(i);
				obj.clean();
				obj = null;
			}
			mDocList.clear();
			mDocList = null;
		}
		if(mListView != null) {
			mListView.setOnItemClickListener(null);
			mListView.setOnItemLongClickListener(null);
			mListView = null;
		}
		mDiskMgrObj = null;
		mDocMgrObj = null;
		mItemDBManager = null;
		mPrefUtils = null;
		mUtilRoutinesObj = null;
	}

	// - private nested classes (more than 10 lines)
	private class ListComposer {
		private String itemType;
		private String itemFileName;
		private String time;
		private long id;
		private String fieldName;
		private int count;

		private boolean selected = false;

		private ListComposer(long id, String itemtype, String filename,
				String path, String time, int count,String fieldName) {
			this.itemType = itemtype;
			this.itemFileName = filename;
			this.count = count;
			this.time = time;
			this.id = id;
			this.fieldName = fieldName;
		}

		public void setIsChecked(boolean isselected){
			this.selected = isselected;
		}

		private void clean() {
			this.itemType = null;
			this.itemFileName = null;
			this.time = null;
			this.id = 0;
		}
	}
	
	private class MyListAdapter extends BaseAdapter {
		Context context;
		ArrayList<ListComposer> dList;
		Animation animation1;
		Animation animation2;
		ImageView ivFlip;
		int checkedCount = 0;

		public MyListAdapter(Context context, ArrayList<ListComposer> mailList) {
			this.context = context;
			this.dList = mailList;

			animation1 = AnimationUtils.loadAnimation(context, R.anim.to_imageview);
			animation2 = AnimationUtils.loadAnimation(context, R.anim.from_imageview);
		}

		@Override
		public int getCount() {
			return dList.size();
		}

		@Override
		public ListComposer getItem(int position) {
			return dList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ExistingListViewHolder holder;
			ListComposer Composer = dList.get(position);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.existinglistrow,
						null);
				holder = new ExistingListViewHolder();

				holder.eName = (TextView) convertView
						.findViewById(R.id.doctypenametextview);
				holder.eName.setTag(position);

				holder.eCount = (TextView) convertView
						.findViewById(R.id.imagecounttextview);
				holder.eTime = (TextView) convertView
						.findViewById(R.id.timetextview);
				holder.eFieldName = (TextView)convertView.findViewById(R.id.fieldtextview);
				if(Composer.fieldName != null && Composer.fieldName.length() > 0 && !Composer.fieldName.equals(Constants.STR_EMPTY_FIELD) &&
						!Composer.fieldName.equals(Constants.STR_NO_FIELDS)){
					holder.eFieldName.setVisibility(View.VISIBLE);
					holder.eTime.setTextSize(9.0f);
				}	               
				holder.defIcon = (ImageView) convertView
						.findViewById(R.id.defDocIcon);
				convertView.setTag(holder);
			}

			holder = (ExistingListViewHolder) convertView.getTag();
			holder.eName.setText(Composer.itemType);
			holder.eCount.setText(Constants.STR_EMPTY + Composer.count);
			holder.eTime.setText(Composer.time);
			holder.eFieldName.setText(Composer.fieldName);
			holder.defIcon.setTag("" + position);

			holder.defIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ivFlip = (ImageView) v;
					ivFlip.clearAnimation();
					ivFlip.setAnimation(animation1);
					ivFlip.startAnimation(animation1);
					setAnimListners(dList.get(Integer.parseInt(v.getTag().toString())), Integer.parseInt(v.getTag().toString()));
				}
			});

			if (dList.get(position).selected) {
				holder.defIcon.setImageResource(R.drawable.checkbox_icon);
				convertView.setBackgroundColor(context.getResources().getColor(R.color.bg_lightgray));

			} else {
				if(Composer.itemType.equals("Business Cards")) {
					holder.defIcon.setImageDrawable(getResources().getDrawable(R.drawable.business_cards_icon));
				}
				else if(Composer.itemType.equals("Document")) {
					holder.defIcon.setImageDrawable(getResources().getDrawable(R.drawable.document_icon));
				}
				else if(Composer.itemType.equals("Receipts")) {
					holder.defIcon.setImageDrawable(getResources().getDrawable(R.drawable.receipts_icon));
				}else{
				holder.defIcon.setImageResource(R.drawable.document_icon);
				}
				convertView.setBackgroundColor(context.getResources().getColor(android.R.color.white));	  
			}

			return convertView;

		}

		private void setAnimListners(final ListComposer item, final int position) {
			AnimationListener animListner;
			animListner = new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					if (animation == animation1) {
						if (item.selected) {
							ivFlip.setImageResource(R.drawable.default_document_icon);
						} else {
							ivFlip.setImageResource(R.drawable.checkbox_icon);
						}
						ivFlip.clearAnimation();
						ivFlip.setAnimation(animation2);
						ivFlip.startAnimation(animation2);
					} else {
						item.setIsChecked(!item.selected);
						setCount();
						setActionMode();
					}
				}

				// Set selected count
				private void setCount() {
					if (item.selected) {
						checkedCount++;
						mSelectedRowIndex = position;
					} else {
						if (checkedCount != 0) {
							checkedCount--;
						}
					}

				}

				// Show/Hide action mode
				private void setActionMode() {
					mSelect_count = checkedCount;
					if (checkedCount > 0) {
						ExistingListMode = ListModes.EDIT_MODE;
						if(mSelect_count == 1){
							for(int i = 0;i < dList.size();i++){
								if(mPrefUtils.isUsingKofax() && dList.get(i).itemType.contains(Constants.STR_ASSIST)){
									Constants.IS_HELPKOFAX_FLOW = true;
								}else{
									Constants.IS_HELPKOFAX_FLOW = false;
								}
								if(dList.get(i) != null && dList.get(i).selected && (dList.get(i).fieldName == null || dList.get(i).fieldName.equals(Constants.STR_NO_FIELDS))){
									mIsEditOptionDisable = true;
									break;
								}
							}	                           
						}
						updateTitlebar(ExistingListMode);  
					} else{
						Constants.IS_HELPKOFAX_FLOW = false;
						ExistingListMode = ListModes.DISPLAY_MODE;
						getActionBar().setTitle(getResources().getString(R.string.title_section2)+"("+mDocList.size()+")");
						mSelect_count = 0;
						updateTitlebar(ExistingListMode);
					}	                     
					notifyDataSetChanged();
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {

				}

				@Override
				public void onAnimationEnd(Animation arg0) {

				}
			};

			animation1.setAnimationListener(animListner);
			animation2.setAnimationListener(animListner);

		}

		private class ExistingListViewHolder {
			ImageView defIcon;
			TextView eName;
			TextView eCount;
			TextView eTime;
			TextView eFieldName;
		}

	}

	// - private methods
	
	private void updateEntitys(ItemEntity itemEntity){
		mItemDBManager.setItemEntity(itemEntity);
		List<ProcessingParametersEntity> list = mItemDBManager.getProcessingParametersFromDetails(this,mItemDBManager.getItemEntity().getItemTypeName(),mItemDBManager.getUserInformationEntity().getUserInformationId());
		if(null != list && list.size() > 0){
			mItemDBManager.setProcessingParametersEntity(list.get(0));
		}
	}
	
	
	private void openSubmitDocument(){
		ItemEntity itemEntity = getSelectedItemEntity();			
		updateEntitys(itemEntity);	
		List<PageEntity> pages = mItemDBManager
				.getAllPagesForItem(getApplicationContext(),
						itemEntity.getItemId());
		if (pages == null || pages.size() == 0) {
			Toast.makeText(ExistingItemsActivity.this,getResources().getString(R.string.toast_empty_document),
					Toast.LENGTH_LONG).show();
		} else {
			refreshExistingList = true;
			if(mPrefUtils.isUsingKofax() && Constants.IS_HELPKOFAX_FLOW){
				int itemTypeIndex = mDocMgrObj.findDocumentTypeIndex(itemEntity.getItemTypeName());
				mDocMgrObj.setCurrentDocTypeIndex(itemTypeIndex);
				submitDocument();
			}else{
			checkFieldValidation(itemEntity);
			}

		}
	}

	private void OpenitemdetailActivity() {
		ItemEntity itemEntity = getSelectedItemEntity();
		updateEntitys(itemEntity);
		//openItemDetailScreen(Globals.RequestCode.CAPTURE_DOCUMENT.ordinal());
		openCaptureView();
	}

	private void OpenEditFieldActivity() {
		ItemEntity itemEntity = getSelectedItemEntity();
		if(itemEntity != null) {
			updateEntitys(itemEntity);
			int itemTypeIndex = mDocMgrObj.findDocumentTypeIndex(itemEntity.getItemTypeName());
			mDocMgrObj.setCurrentDocTypeIndex(itemTypeIndex);

			Intent intent = new Intent(getApplicationContext(), EditFieldsActivity.class);
			intent.putExtra(Constants.STR_IS_NEW_ITEM, false);

			startActivityForResult(intent, Globals.RequestCode.EDIT_FIELDS.ordinal());
		}
	}
	//Open ItemDetailScreen when user click on item.
	private void openItemDetailScreenFromItem(int position){
		refreshExistingList = true;
		mSelectedItemIndex = position;
		long itemid = mDocList.get(position).id;
		if(mPrefUtils.isUsingKofax() && mDocList.get(position).itemType.contains(Constants.STR_ASSIST)){
			Constants.IS_HELPKOFAX_FLOW = true;
		}
		ItemEntity itemEntity = mItemDBManager
				.getItemForId(getApplicationContext(), itemid);
		updateEntitys(itemEntity);
		Intent i = new Intent(getApplicationContext(),
				ItemDetailsActivity.class);

		Log.i(TAG,
				"Filename ====>"
						+ mDocList.get(position).itemFileName);
		i.putExtra(Constants.STR_ITEM_NAME,
				mDocList.get(position).itemFileName); // send the complete path of selected item 
		i.putExtra(Constants.STR_IS_FROM_PENDING, true);
		i.putExtra(Constants.STR_ITEM_TYPE,
				itemEntity.getItemTypeName());
		startActivityForResult(i, Globals.RequestCode.SHOW_ITEM_DETAILS.ordinal());	
	}

	private Handler existingHandler = new Handler(new Handler.Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
			switch (whatMessage) {
			case MESSAGE_DIALOG_DELETE_CONFIRMATION:
				if (msg.arg1 == RESULT_OK) {
					int size = mDocList.size();
					for (int i = 0; i < size;) {
						if (mDocList.get(i).selected) {
							mDiskMgrObj.deleteItemFromDisk(
									mDocList.get(i).itemFileName, i);
							mItemDBManager.deleteItemWithId(
									getApplicationContext(), mDocList.get(i).id);
							mDocList.remove(i);
							size = mDocList.size();
						} else {
							i++;
						}
					}
					changeListMode(true);
				}
				break;
			case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
				//Navigate back to settings
				if(msg.arg1 == RESULT_OK){
					mUtilRoutinesObj.offlineLogout(ExistingItemsActivity.this);
				}else{
					Globals.gAppModeStatus = Globals.AppModeStatus.FORCE_OFFLINEMODE;
				}
				break;
			default:
				break;
			}

			return true;
		}
	});

	private void updateTitlebar(ListModes mode){
		int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
		TextView titleText = (TextView) findViewById(titleId);

		if(mode == ListModes.EDIT_MODE){
			//change actionbar background color to white
			getActionBar().setBackgroundDrawable(
					new ColorDrawable(Color.WHITE));
			getActionBar().setIcon(R.drawable.done_gray);
			getActionBar().setTitle(mSelect_count+" "+getResources().getString(R.string.lbl_selected));
			//setting action bar title text color to gray when multi selection mode ON  
			titleText.setTextColor(Color.GRAY);
			mTopview.setVisibility(View.VISIBLE);
		}else{
			mTopview.setVisibility(View.GONE);
			//change actionbar background color back to theme color
			ColorDrawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.appbgblue));
			getActionBar().setBackgroundDrawable(colorDrawable);
			//re-setting action bar title text color to white when multi selection mode OFF 
			titleText.setTextColor(Color.WHITE);        
			getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

		}
		invalidateOptionsMenu();
	}

	private void DispalyConfirmationMessage(){
		mCustomDialog
		.show_popup_dialog(ExistingItemsActivity.this, AlertType.CONFIRM_ALERT,
				getResources().getString(R.string.str_user_msg_delete_confirmation_title),
				getResources().getString(R.string.str_user_msg_generic_delete_confirmation),
				null, null,
				Messages.MESSAGE_DIALOG_DELETE_CONFIRMATION,
				existingHandler,
				false); 
	}

	//Before submit document check all required fields are not null,if it is null display Editinfo screen
	private void checkFieldValidation(ItemEntity item){
		boolean isRequiredEditinfoScreen = false;
		ItemEntity itemEntity = mItemDBManager.getItemEntity();
		if (mItemDBManager.isDocumentSerializedInDB(itemEntity)) {
			//boolean result = mDiskMgrObj.isDocumentSerialized(mDocMgrObj.getOpenedDoctName());
			Document documentObj = (Document)mDiskMgrObj.byteArrayToDocument(itemEntity.getItemSerializedData());
			isRequiredEditinfoScreen = mDocMgrObj.validateDocumentFields(documentObj);
			if(!isRequiredEditinfoScreen){
				int itemTypeIndex = mDocMgrObj.findDocumentTypeIndex(itemEntity.getItemTypeName());
				mDocMgrObj.setCurrentDocTypeIndex(itemTypeIndex);
				submitDocument();
			}
		}
		else{
			isRequiredEditinfoScreen = true;
		}
		if(isRequiredEditinfoScreen){
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

	private ItemEntity getSelectedItemEntity() {
		ItemEntity itemEntity = null;
		for (int i = 0; i < mDocList.size(); i++) {
			if (mDocList.get(i).selected) {
				mDocMgrObj.setOpenedDocName(mDocList.get(i).itemFileName);
				long itemid = mDocList.get(i).id;
				itemEntity = mItemDBManager
						.getItemForId(getApplicationContext(), itemid);
				break;
			}
		}
		return itemEntity;
	}

	//check if any required field is empty
/*	private boolean isRequiredFieldEmpty(Document doc){
		boolean isReqEmpty = false;
		for(int i = 0; i < doc.getDocumentType().getFieldTypes().size();i++){
			if(doc.getFields().get(i).getFieldType().isRequired() && !doc.getFields().get(i).getFieldType().isHidden()){
				if((doc.getFields().get(i).getValue() == null) || (doc.getFields().get(i).getValue().length() <= 0)){
					isReqEmpty = true;
				}
			}
		}
		return isReqEmpty;
	}
*/
	private void submitDocument(){
		Intent submit_intent = new Intent(getApplicationContext(), SubmitDocument.class);
		submit_intent.putExtra(Constants.STR_IS_NEW_ITEM, false);
		startActivity(submit_intent);
	}

	private void openCaptureView() {
		refreshExistingList = true;
//		Bundle extras = new Bundle();
		int size = mDocList.size();
		Intent intent = new Intent(getApplicationContext(), Capture.class);
		for (int i = 0; i < size; i++) {
			if (mDocList.get(i).selected) {
				intent.putExtra(Constants.STR_ITEM_NAME, mDocList.get(i).itemFileName);
				intent.putExtra(Constants.STR_IMAGE_COUNT, mDocList.get(i).count);
				//check if selected document contains any images before adding last_image_path to intent
				if(mItemDBManager.getAllPagesCountForItem(getApplicationContext(), mDocList.get(i).id) > 0)	 {
						intent.putExtra(Constants.LAST_IMAGE_PATH, mItemDBManager.getAllPagesForItem(getApplicationContext(), mDocList.get(i).id).get(mItemDBManager.getAllPagesCountForItem(getApplicationContext(), mDocList.get(i).id) - 1).getImageFilePath());
				}
				break;
			}
		}
		

		intent.putExtra(Constants.STR_IS_NEW_ITEM, false);
//		intent.putExtras(extras);

		startActivityForResult(intent,
				Globals.RequestCode.CAPTURE_DOCUMENT.ordinal());

	}

	private boolean isDocumentTypeValid() {
		String docTypeName = getSelectedDocumentType();
		boolean isValid = true;
		int index = mDocMgrObj.findDocumentTypeIndex(docTypeName);
		if (index < 0) {
			isValid = false;
		}
		return isValid;
	}

	private String getSelectedDocumentType() {
		return mDocList.get(mSelectedRowIndex).itemType;
	}


	private void showEmptyScreenMessage() {
		findViewById(R.id.noDocsMsgLayout).setVisibility(
				View.VISIBLE);
	}

	private void hideEmptyScreenMessage() {
		findViewById(R.id.noDocsMsgLayout).setVisibility(View.GONE);
	}

	private void registerBroadcastReceiver() {
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction() == Constants.CUSTOM_INTENT_ITEM_MODIFIED) {
						Log.i(TAG,
								"MESSAGE_ITEM_UPDATED received!!!!!!!!!!!!!!!!!! :: "
										+ mSelectedItemIndex);
						/*
						 * This is true when images are added/removed, item-type
						 * changed. Update item in mDocList at the selected
						 * index to reflect the item changes on screen.
						 */
						// get the index (which is in sync with index of
						// mDocList parameter of this class) to update that item
						// locally.
						if (mSelectedItemIndex != -1) {
							refreshItem(mSelectedItemIndex);
						}
					}
					else if(intent.getAction() == Constants.CUSTOM_INTENT_ITEM_SUBMITTED) {
						finish();
					}
				};
			};
			IntentFilter intentFilter = new IntentFilter(
					Constants.CUSTOM_INTENT_ITEM_MODIFIED);
			registerReceiver(mReceiver,	intentFilter);
			intentFilter = new IntentFilter(
					Constants.CUSTOM_INTENT_ITEM_SUBMITTED);
			registerReceiver(mReceiver,	intentFilter);
		}
	}

	private void refreshItem(int index) {
		if (mDocList != null && mDocList.size() > 0) {
			int size = mDocList.size();
			/*
			 * OnStart() method will be called before
			 * BroadcastReceiver() method.In onStart()
			 * method refresh the list. so,if any successful
			 * submission,item will be deleted. mDocList
			 * Index will be decreases before called
			 * BroadcastReceiver() method.
			 */
			// check this condition for index out of bound
			// exception.
			if (index < size) {
				ItemEntity itemEntity = mItemDBManager
						.getItemForId(
								getApplicationContext(),
								mDocList.get(index).id);

				if (itemEntity != null) {
					mDocList.remove(index);

					Date d = itemEntity
							.getItemCreatedTimeStamp();
					SimpleDateFormat formater = new SimpleDateFormat(
							"dd MMM, yyyy | hh:mm a");
					String str_date = formater.format(d);

					Log.i(TAG, "DB Item formated date : "
							+ str_date);

					int pagescount = mItemDBManager
							.getAllPagesCountForItem(
									getApplicationContext(),
									itemEntity.getItemId());
					mDocList.add(
							index,
							new ListComposer(itemEntity.getItemId(),
											itemEntity.getItemTypeName(),
											itemEntity.getItemName(),
											mItemDBManager.constructItemURL(
													getApplicationContext(),
													itemEntity.getItemId(),
													mAppRootDir),
											str_date,
											pagescount,
											itemEntity.getFieldName()));
					mListAdapter = new MyListAdapter(getApplicationContext(), mDocList);
					mListView.setAdapter(mListAdapter);
					mListAdapter.notifyDataSetChanged();
				}
			}
		}
	}

	private void unRegisterBroadcastReceiver() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}
	
	private List<ItemEntity> getPendingList(){
		List<ItemEntity> itemList = null;
		String user = mUtilRoutinesObj.getUser();

		if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_ONLINE){
			itemList = mItemDBManager.getAllItems(
				getApplicationContext(),
				mPrefUtils.getCurrentServerType(),
				mPrefUtils.getCurrentHostname(),
				user);
		}else if(Globals.gAppLoginStatus == Globals.AppLoginStatus.LOGIN_OFFLINE){
			if(mPrefUtils.isUsingAnonymous()){
				user = Constants.ANONYMOUS_LOGIN_ID;
				itemList = mItemDBManager.getAllItems(getApplicationContext(),mPrefUtils.getCurrentServerType(),
						mPrefUtils.getCurrentHostname(),
						user);
			}else{
				String lastLoggedUserServerType =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_SERVER_TYPE, mPrefUtils.DEF_USR_SERVER_TYPE);
				String lastLoggedUserHostName =  mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_HOSTNAME, mPrefUtils.DEF_USR_HOSTNAME);
				String lastLoggedUser = mPrefUtils.sharedPref.getString(mPrefUtils.KEY_APP_LAST_lOGGED_USER,  mPrefUtils.DEF_USR_UNAME);
				user = lastLoggedUser;

				itemList = mItemDBManager.getAllItems(getApplicationContext(), lastLoggedUserServerType,
						lastLoggedUserHostName,
						user);
			}

		}
		return itemList;
	}

	@Override
	public void onNetworkChanged(boolean isConnected) {

		if(isConnected && Globals.gAppModeStatus !=  AppModeStatus.FORCE_OFFLINEMODE){
			mNoInternetConnectionView.setVisibility(View.GONE);
		}else{
			mNoInternetConnectionView.setVisibility(View.VISIBLE);
		}
		if(Globals.isRequiredOfflineAlert() && isConnected && mUtilRoutinesObj.isAppOnForeground(ExistingItemsActivity.this)){
			if(mCustomDialog != null){
				mCustomDialog.dismissAlertDialog();
				mCustomDialog.show_popup_dialog(ExistingItemsActivity.this,AlertType.CONFIRM_ALERT,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
				getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION, existingHandler, false);	
			}
		}
	}
}
