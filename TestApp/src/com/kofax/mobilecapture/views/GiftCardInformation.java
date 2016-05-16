// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;


import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.GiftCardManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.UtilityRoutines;

public class GiftCardInformation extends Activity {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants
	private final String[] fieldNames = {"Brand", "CardNumber", "PinNumber", "Balance", "ExpirationDate"};
//	private final String[] displayNames = {"Brand", "Card Number", "Pin Number", "Balance", "Expiration Date"};

	private final String BRAND_TEXT = fieldNames[0];
	private final String CARD_NUMBER_TEXT = fieldNames[1];
	private final String PIN_NUMBER_TEXT = fieldNames[2];
	private final String BALANCE_TEXT = fieldNames[3];
	private final String EXPIRY_DATE_TEXT = fieldNames[4];

	private final String TAG = GiftCardInformation.class.getSimpleName();
	private final String processedImageName = "giftcardprocessed.tiff";
	
	// - Private data.
	/* SDK objects */
	/* Application objects */
	private UtilityRoutines mUtilityRoutines = null;
	private GiftCardManager mGiftCardManager = null;
	/* Standard variables */
	private CustomDialog mCustomDialog;
	private View mFieldsParentView = null;
	private EditText brandTextView = null;
	private EditText cardNumberTextView = null;
	private EditText pinNumberTextView = null;
	private ImageView mImagePreview = null;
	private Bitmap mImgBitmap = null;
	private Handler mHandler = null;

	private String mProcessedImageFileLocation;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes

	// - public getters and setters

	// - public methods
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new DeviceSpecificIssueHandler().checkEntryPoint(this);

		setContentView(R.layout.giftcard_information);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

		setupHandler();

		mUtilityRoutines = UtilityRoutines.getInstance();
		mCustomDialog = CustomDialog.getInstance();
		mGiftCardManager = GiftCardManager.getInstance(mHandler);

		mProcessedImageFileLocation = mUtilityRoutines.getAppRootPath(this) + processedImageName;

		mImagePreview = (ImageView)findViewById(R.id.preview);
		mImagePreview.setOnClickListener(previewImageOnClickListener);

		mFieldsParentView = findViewById(R.id.fieldsParent);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		displayFields();

		if(mGiftCardManager.getImage() != null && mGiftCardManager.getImage().getImageBitmap() != null) {
			displayImage(mGiftCardManager.getImage().getImageBitmap());
			//displayImage();
		}
		assignTextViews();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		deleteFileAtLocation(mProcessedImageFileLocation);

		if(mImgBitmap != null){
			mImgBitmap.recycle();
			mImgBitmap = null;
		}
		mGiftCardManager.cleanup();	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.giftcardinfo_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
		case R.id.gc_menu_done:
			finish();
			break;
		case R.id.gc_menu_refresh:
			if(!mUtilityRoutines.checkInternet(this)) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_no_network_connectivity), Toast.LENGTH_LONG).show();
			}
			else {
				//pin number (access code) is not mandatory
				if(cardNumberTextView.length() == 0) {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_cardnumber_required), Toast.LENGTH_LONG).show();
				}
				else {
					mCustomDialog.showProgressDialog(this, getResources().getString(R.string.progress_msg_please_wait),false);
					//Toast.makeText(getApplicationContext(), "Card number :: " + cardNumberTextView.getText().toString() + "   Pin number :: " + pinNumberTextView.getText().toString() , Toast.LENGTH_LONG).show();
					mGiftCardManager.retrieveBalance(brandTextView.getText().toString(), cardNumberTextView.getText().toString(), pinNumberTextView.getText().toString());					
				}
			}
			break;
		default:
			break;
		}
		return true;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private OnClickListener previewImageOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			//recaptrue gift card image
			openCaptureActivity();
			finish();
		}
	};

	private void setupHandler() {
		mHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				Globals.Messages whatMessage = Globals.Messages.values()[msg.what];
				switch (whatMessage) {
				case MESSAGE_GIFTCARD_VALIDATION_COMPLETED:
					mCustomDialog.closeProgressDialog();
					//if extraction was successful, launch next screen
					if(msg.arg1 == Globals.ResultState.RESULT_OK.ordinal()) {
						//updateBalance();
						displayFields();
						assignTextViews();
					}
					else {
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_balance_retrieval_failed), Toast.LENGTH_LONG).show();
					}
					break;
				default:
					break;
				}
				return true;
			}
		}); 
	}

	/*Set bitmap to the Image view.*/
	private void displayImage(Bitmap bmp) {
		if(bmp == null) {
			Log.e(TAG, "Giftcard processed image bitmap is NULL.");
			return;
		}
		mImagePreview.setImageDrawable(null);
		if (bmp != null) {
			float frameWidth = bmp.getWidth();
			float frameHeight = bmp.getHeight();

			float referenceFrameW = frameWidth;
			float referenceFrameH = (int)((3.075f * frameWidth) / (2.125f));
			float scale;
			float sx = (float)frameWidth / referenceFrameW;
			float sy = (float)frameHeight / referenceFrameH;
			scale = Math.min(sx, sy);

			frameWidth = (int)(referenceFrameW * scale);
			frameHeight = (int)(referenceFrameH * scale);

			mImgBitmap = Bitmap.createScaledBitmap(bmp, (int)frameHeight, (int)frameWidth, false);
			mImagePreview.setImageBitmap(mImgBitmap);
			mImagePreview.invalidate();
			bmp = null;
		}
	}

	@SuppressLint("InflateParams")
	private void displayFields() {
		int noOfFields = 0; 

		if(mGiftCardManager.getFieldsLength() > 0) {
			((LinearLayout)mFieldsParentView).removeAllViews();
			noOfFields = mGiftCardManager.getFieldsLength();

			for(int i=0; i<noOfFields; i++) {
				LayoutInflater infalInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View childContainer = infalInflater.inflate(R.layout.giftcardinfocontainer,
						null);

				((TextView) childContainer
						.findViewById(R.id.field_name)).setText(mGiftCardManager.getFieldNames()[i]);
				if(mGiftCardManager.getFieldNames()[i].equalsIgnoreCase(BALANCE_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setText("$"+mGiftCardManager.getFieldValues()[i]);
				}
				else {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setText(mGiftCardManager.getFieldValues()[i]);
				}
				//set input type to text fields according to the fields
				if(mGiftCardManager.getFieldNames()[i].equalsIgnoreCase(BRAND_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setEnabled(false);
				}
				else if(mGiftCardManager.getFieldNames()[i].equalsIgnoreCase(CARD_NUMBER_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setInputType(InputType.TYPE_CLASS_NUMBER);
				}
				else if(mGiftCardManager.getFieldNames()[i].equalsIgnoreCase(PIN_NUMBER_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setInputType(InputType.TYPE_CLASS_NUMBER);
				}
				else if(mGiftCardManager.getFieldNames()[i].equalsIgnoreCase(BALANCE_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setEnabled(false);
				}
				else if(mGiftCardManager.getFieldNames()[i].equalsIgnoreCase(EXPIRY_DATE_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setEnabled(false);
					((EditText) childContainer
							.findViewById(R.id.field_value)).setInputType(InputType.TYPE_CLASS_DATETIME);
				}
				//add view
				((LinearLayout)mFieldsParentView).addView(childContainer);
			}
		}
		else {
			//display only card number and balance fields.
			String[] inputFieldName = {BRAND_TEXT, CARD_NUMBER_TEXT, PIN_NUMBER_TEXT, BALANCE_TEXT, EXPIRY_DATE_TEXT};
			noOfFields = inputFieldName.length;
			for(int i=0; i<noOfFields; i++) {
				LayoutInflater infalInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				View childContainer = infalInflater.inflate(R.layout.giftcardinfocontainer,
						null);
				((TextView) childContainer
						.findViewById(R.id.field_name)).setText(inputFieldName[i]);
				((EditText) childContainer
						.findViewById(R.id.field_value)).setText("");
				//set input-type as numeric for card and pin number
				if(inputFieldName[i].equalsIgnoreCase(CARD_NUMBER_TEXT) ||
						(inputFieldName[i].equalsIgnoreCase(PIN_NUMBER_TEXT))) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setInputType(InputType.TYPE_CLASS_NUMBER);
				}
				else {
					//disable all text-fields except card and pin number
					((EditText) childContainer
							.findViewById(R.id.field_value)).setEnabled(false);
				}
				if(inputFieldName[i].equalsIgnoreCase(BRAND_TEXT)) {
					((EditText) childContainer
							.findViewById(R.id.field_value)).setText(mGiftCardManager.getCardBrand());
				}
				((LinearLayout)mFieldsParentView).addView(childContainer);
			}
		}
	}

	private void assignTextViews() {
		for(int i=0; i<((ViewGroup)mFieldsParentView).getChildCount(); ++i) {
			View nextChild = ((ViewGroup)mFieldsParentView).getChildAt(i);
			if(((TextView)nextChild.findViewById(R.id.field_name)).getText().toString().equalsIgnoreCase(BRAND_TEXT)) {
				brandTextView =  (EditText)nextChild.findViewById(R.id.field_value);
			}
			else if(((TextView)nextChild.findViewById(R.id.field_name)).getText().toString().equalsIgnoreCase(CARD_NUMBER_TEXT)) {
				cardNumberTextView =  (EditText)nextChild.findViewById(R.id.field_value);
				cardNumberTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
			}
			else if(((TextView)nextChild.findViewById(R.id.field_name)).getText().toString().equalsIgnoreCase(PIN_NUMBER_TEXT)) {
				pinNumberTextView =  (EditText)nextChild.findViewById(R.id.field_value);
				pinNumberTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
			}
		}
	}

	private void openCaptureActivity(){
		Intent intent = new Intent(this, Capture.class);
		Bundle bundle = new Bundle();
		bundle.putBoolean(Constants.STR_GIFTCARD, true);
		intent.putExtras(bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	private void deleteFileAtLocation(String path) {
		File fp = new File(path);
		if(fp.exists()) {
			fp.delete();
			fp = null;
		}
	}
}