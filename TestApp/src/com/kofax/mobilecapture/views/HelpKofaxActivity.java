package com.kofax.mobilecapture.views;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.kofax.kmc.kut.utilities.error.KmcException;
import com.kofax.kmc.kut.utilities.error.KmcRuntimeException;
import com.kofax.mobilecapture.DatabaseManager;
import com.kofax.mobilecapture.DocumentManager;
import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.dbentities.ItemEntity;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.AppLoginStatus;
import com.kofax.mobilecapture.utilities.Globals.RequestCode;
import com.kofax.mobilecapture.utilities.NetworkChangedListener;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class HelpKofaxActivity extends Activity implements NetworkChangedListener{
	
	private ListView mListView;
	private MyListAdapter mListAdapter;
	private List<String> mDocList;
	private DatabaseManager mDBManager = null;
	private UtilityRoutines mUtilRoutines = null;
	private DocumentManager mDocMgr = null;
	private PrefManager mPrefUtils = null;
	private CustomDialog mCustomDialog = null;
	
	private Handler mHandler = null;
	
	private int nonHelpKofaxDocCount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.helpkofaxscreen);
		
		setupHandler();
		
		initObjects();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setIcon(
					new ColorDrawable(getResources().getColor(
							android.R.color.transparent)));
		}

		mDocList = mDocMgr.getHelpKofaxDocTypes();
		ArrayList<String> list = mDocMgr.getNonHelpDocumentNamesList();
		
		if(null != list && !list.isEmpty()){
			nonHelpKofaxDocCount = list.size();
		}
		
		
		// get the listview
		mListView = (ListView) findViewById(R.id.doctypeHelpListView);

		mListAdapter = new MyListAdapter(this, mDocList);

		// setting list adapter
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(mPrefUtils.isUsingKofax() && !mPrefUtils.isUsingAnonymousDemo()){
					mDocMgr.setCurrentDocTypeIndex(nonHelpKofaxDocCount-2+position);
				}else{
					mDocMgr.setCurrentDocTypeIndex(nonHelpKofaxDocCount-1+position);
				}
				//create unique file name for the item directory to store on disk
				String uniqueFileName = mUtilRoutines.createUniqueItemName();
				mDocMgr.setOpenedDocName(uniqueFileName);
				try {
					mDocMgr.downloadDocTypeObject(mDocMgr.getCurrentDocTypeIndex());
				} catch (KmcRuntimeException e) {
					e.printStackTrace();
				} catch (KmcException e) {
					e.printStackTrace();
				}
				insertIteminDB(mDocList.get(position));
				openCaptureView();
			}
		});

	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
				finish();		
			return true;
		default:
			break;
		}
		return false;
	}

	@Override
	protected void onStart() {		
		super.onStart();
	}

	@Override
	protected void onResume() {		
		super.onResume();
		Constants.NETWORK_CHANGE_LISTENER = HelpKofaxActivity.this;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(Globals.RequestCode.values().length > requestCode){
			Globals.RequestCode myRequestCode = Globals.getRequestCode(requestCode);
			switch (myRequestCode) {
			case CAPTURE_DOCUMENT:		
				if (resultCode == Globals.ResultState.RESULT_OK.ordinal()) {
					if(data != null &&  data.hasExtra(Constants.STR_OFFLINE_TO_LOGIN) && data.getBooleanExtra(Constants.STR_OFFLINE_TO_LOGIN,false)){						
						mUtilRoutines.offlineLogout(this);															
						return;
					}
					if (data != null && data.hasExtra(Constants.STR_CAPTURE_COUNT) && data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) > 0) {
						openHomeScreen(data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0),resultCode);				
					}
				} else if (resultCode == Globals.ResultState.RESULT_CANCELED.ordinal()) {
					if(data.getIntExtra(Constants.STR_CAPTURE_COUNT, 0) == 0){
						removeItemFromDB();
					}
				}
				break;
			default:
				break;
			}
		}
	}
	
	private void removeItemFromDB(){
		mDBManager.deleteItemWithId(this, mDBManager.getItemEntity().getItemId());
	}
	
	private void openCaptureView() {
		/*List<ProcessingParametersEntity> list = mDBManager.getProcessingParametersFromDetails(this,mDBManager.getItemEntity().getItemTypeName(),mDBManager.getUserInformationEntity().getUserInformationId());
		if(null != list && list.size() > 0){
			mDBManager.setProcessingParametersEntity(list.get(0));
		}*/
		Constants.IS_HELPKOFAX_FLOW = true;
		Intent cameraIntent = new Intent(this, Capture.class);

		cameraIntent.putExtra(Constants.STR_IMAGE_COUNT, 0);
		cameraIntent.putExtra(Constants.STR_IS_NEW_ITEM, true);

		startActivityForResult(cameraIntent,
				RequestCode.CAPTURE_DOCUMENT.ordinal());
	}
	
	
	
	private void setupHandler() {
		mHandler = new Handler(new Handler.Callback() {

			@Override
			public boolean handleMessage(Message msg) {

				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];

				switch (whatMessage) {
				case MESSAGE_DIALOG_ONLINE_CONFIRMATION:
					//Navigate back to settings
					if(msg.arg1 == RESULT_OK){
						mUtilRoutines.offlineLogout(HelpKofaxActivity.this);
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
	}
	
	private void initObjects() {
		mPrefUtils = PrefManager.getInstance();
		mDBManager = DatabaseManager.getInstance();
		mUtilRoutines = UtilityRoutines.getInstance();
		mDocMgr = DocumentManager.getInstance(getApplicationContext());
		mCustomDialog = CustomDialog.getInstance();
	}
	
	private void openHomeScreen(int count,int resultCode){
		Intent returnIntent = new Intent();
		returnIntent.putExtra(Constants.STR_CAPTURE_COUNT, count);
		setResult(resultCode, returnIntent);
		finish();
	}
	
	private void insertIteminDB(String itemTypeName){
		ItemEntity item = new ItemEntity();
		//TODO::Helpkofax need to change
		item.setItemName(mDocMgr.getOpenedDoctName());
		item.setItemTypeName(itemTypeName);
		item.setItemCreatedTimeStamp(new Date());
		item.setServerId(mPrefUtils.getCurrentServerType());
		item.setHostname(mPrefUtils.getCurrentHostname());
		item.setUserId(mUtilRoutines.getUser());
		if(Globals.gAppLoginStatus ==  AppLoginStatus.LOGIN_ONLINE){
			item.setIsOffline(false);
		}else{
			item.setIsOffline(true);
		}
		
		item.setFieldName(null);
		mDBManager.insertOrUpdate(this, item);
		mDBManager.setItemEntity(item);
	}
	
	private class MyListAdapter extends BaseAdapter {
		Context context;
		List<String> dList;

		public MyListAdapter(Context context, List<String> mailList) {
			this.context = context;
			this.dList = mailList;
		}

		@Override
		public int getCount() {
			return dList.size();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ExistingListViewHolder holder;
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.helpkofaxlistrow,
						null);
				holder = new ExistingListViewHolder();

				holder.eName = (TextView) convertView
						.findViewById(R.id.helptextview);
				holder.eName.setTag(position);
				convertView.setTag(holder);
			}else{
				holder = (ExistingListViewHolder)convertView.getTag();
			}
			
			holder.eName.setText(dList.get(position));
			
			return convertView;

		}		

		private class ExistingListViewHolder {
			TextView eName;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

	}
	
	@Override
	public void onNetworkChanged(boolean isConnected) {
		if(Globals.isRequiredOfflineAlert() && isConnected && mUtilRoutines.isAppOnForeground(HelpKofaxActivity.this)){
			if(mCustomDialog != null){
				mCustomDialog.dismissAlertDialog();
				mCustomDialog.show_popup_dialog(HelpKofaxActivity.this,AlertType.CONFIRM_ALERT,getResources().getString(R.string.lbl_confirm) , getResources().getString(R.string.app_msg_online_msg), 
						getResources().getString(R.string.lbl_login), getResources().getString(R.string.lbl_offline),Globals.Messages.MESSAGE_DIALOG_ONLINE_CONFIRMATION, mHandler, false);	
			}
		}	
	}
}
