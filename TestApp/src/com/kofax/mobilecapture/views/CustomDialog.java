// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.AlertType;
import com.kofax.mobilecapture.utilities.Globals.Messages;

public class CustomDialog {

	// - public enums

	// - Private enums

	// - public interfaces

	// - public nested classes

	// - private nested classes (10 lines or less)

	// - public constants

	// - private constants

	// - Private data.
	/* SDK objects */
	/* Application objects */
	private static CustomDialog pSelf = null;
	private PrefManager mPrefUtils = null;
	/* Standard variables */
	//private static Context mContext;
	private AlertDialog alertDialog = null;
	private AlertDialog.Builder dialogBuilder;
	private Handler mCallerHandler = null;
	private boolean cancelCheck = false;
	private ProgressDialog mProgressDialog = null;

	// - public constructors

	// - private constructors
	// - Private constructor prevents instantiation from other classes
	private CustomDialog() {
		mPrefUtils = PrefManager.getInstance();
	}

	// - public getters and setters
	public static CustomDialog getInstance() {

		if (pSelf == null) {
			pSelf = new CustomDialog();
		}

		return pSelf;
	}

	// - public methods
	public void show_popup_dialog(Context context, AlertType alertType, String title,
			String message, String positiveButtonText, String negativeButtonText, Messages msgCode, Handler handler, boolean isCancellable) {

		mCallerHandler = handler;

		switch (alertType) {
		case CONFIRM_ALERT:
			showConfirmationAlert(context, title, positiveButtonText, negativeButtonText, message, msgCode);
			break;
		case CONFIRM_ALERT_WITH_CHECKBOX:
			showConfirmationAlertWithCheckBox(context, title, message, msgCode);
			break;
		case INFO_ALERT:
			showInformationAlert(context, title, message, msgCode);
			break;
		case ERROR_ALERT:
			showErrorAlert(context, title, message, msgCode);
			break;
		case PROGRESS_ALERT:
			showProgressDialog(context, message, isCancellable);
			break;
		default:
			break;
		}
	}

	public void showSelectionAlert(Context context, List<String> names,  int selectionIndex, Handler handler) {
		dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
		mCallerHandler = handler;
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				context, android.R.layout.select_dialog_singlechoice);

		for (int i=0; i<names.size(); i++) {
			arrayAdapter.add(names.get(i));
		}

		dialogBuilder.setAdapter(arrayAdapter,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int position) {
				//String type = arrayAdapter.getItem(position);
				Message msg = new Message();
				msg.what = Globals.Messages.MESSAGE_CHANGE_ITEM_TYPE
						.ordinal();
				msg.arg1 = position;
				mCallerHandler.sendMessage(msg);
				alertDialog.dismiss();
			}
		});
		alertDialog = dialogBuilder.create();
		alertDialog.show();

		alertDialog.getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		if(selectionIndex >= 0) {
			alertDialog.getListView().setItemChecked(selectionIndex, true);
		}
	}

	public void finish() {
		pSelf = null;
	}


	public void setMessageToProgressDialog(String msg){
		if(mProgressDialog != null){
			mProgressDialog.setMessage(msg);
		}
	}
	
	
	///Dismiss the current alert dialog
	
	/***Dismiss the current alert dialog
	 * 
	 */
	public void dismissAlertDialog(){
		if(alertDialog != null && alertDialog.isShowing()){
			alertDialog.dismiss();
		}
		dialogBuilder = null;
		alertDialog = null;
	}

	// - private nested classes (more than 10 lines)

	// - private methods

	private void showInformationAlert(Context context, String title, String message, final Messages msgCode) {
		dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
		dialogBuilder.setTitle(title);
		dialogBuilder.setMessage(message);
		dialogBuilder.setCancelable(false);

		dialogBuilder.setPositiveButton(context.getResources().getString(R.string.btn_label_ok),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				if(mCallerHandler != null){
					Message mMsg = new Message();
					if(msgCode != null) {
						mMsg.what = msgCode.ordinal();
					}
					mMsg.arg1 = -1;	//TODO: (-1 is for RESULT_OK) temp assignment until the proper result constant is found
					mCallerHandler.sendMessage(mMsg);
				}
				dialog.dismiss();
				dialogBuilder = null;
				alertDialog = null;
			}
		});

		// Remember, create doesn't show the dialog
		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}

	public void showProgressDialogWithButton(Context context, String message, final Messages msgCode, Handler handler) {
		final Handler callerHandler = handler;
		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(context, android.R.style.Theme_Holo_Light_Dialog);
		}
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(R.string.btn_label_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mProgressDialog.getButton(which).setEnabled(false);
				if(callerHandler != null) {
					cancelCheck = true;
					Message mMsg = new Message();
					mMsg.what = msgCode.ordinal();
					mMsg.arg1 = 1;
					callerHandler.sendMessage(mMsg);
				}
			}
		});

		// Set the OnDismissListener (if you need it)       
		mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				// dialog was just dismissed..
				if(cancelCheck && mProgressDialog != null){
					cancelCheck = false;
					mProgressDialog.show();
				}
			}
		});
		if(!((Activity)context).isFinishing()) {
			mProgressDialog.show();
			mProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
			mProgressDialog.getWindow().setLayout(getDeviceWidth(context), LayoutParams.WRAP_CONTENT);
		}
	}


	public void showProgressDialog(Context context, String message,Boolean isCancelable) {
		if (mProgressDialog == null) {
			//progress_dialog = new ProgressDialog(context, style.CustomDialogTheme);
			mProgressDialog = new ProgressDialog(context, android.R.style.Theme_Holo_Light_Dialog);
		}		
		
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(isCancelable);
		if(!((Activity)context).isFinishing()) {
			mProgressDialog.show();
			mProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
		}
//		mProgressDialog.getWindow().setLayout(getDeviceWidth(context), LayoutParams.WRAP_CONTENT);
	}
	
	private int getDeviceWidth(Context context){
	    Activity activity = (Activity) context;
	    Display display =  activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;             
        //      width = (width/4)*3;    // 75% of screen width
        if(width <= 1080){
            width = (width/4)*3;   //50% of screen width
        }else{
            width = (width/2);   //50% of screen width
        }
        return width;
    }

	public void closeProgressDialog() {
		if (null != mProgressDialog) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}
	
	/// Get the ProgressDialog status
	public boolean isProgressDialogShowing(){
		boolean status = false;
		if (null != mProgressDialog) {
			status = mProgressDialog.isShowing();
		}
		return status;
		
	}

	private void showConfirmationAlert(Context context, String title, String positiveButtonText, String negativeButtonText, String message,
			final Messages msgCode) {
		dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));

		dialogBuilder.setTitle(title);
		dialogBuilder.setMessage(message);
		dialogBuilder.setCancelable(false);

		if(positiveButtonText == null) {
			positiveButtonText = context.getResources().getString(R.string.btn_label_ok);
		}
		if(negativeButtonText == null) {
			negativeButtonText = context.getResources().getString(R.string.btn_label_cancel);
		}
		if(msgCode != Messages.MESSAGE_DIALOG_SUBMIT_CONFIRMATION){
			dialogBuilder.setPositiveButton(positiveButtonText,
					new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					Message mMsg = new Message();
					mMsg.what = msgCode.ordinal();
					//mMsg.arg1 = Globals.resultState.RESULT_OK.ordinal();
					mMsg.arg1 = -1;	//TODO: temp assignment until the proper result constant is found
					mCallerHandler.sendMessage(mMsg);
					dialog.dismiss();
					dialogBuilder = null;
					alertDialog = null;
				}
			});
		}

		dialogBuilder.setNegativeButton(negativeButtonText,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message mMsg = new Message();
				mMsg.what = msgCode.ordinal();
				if(msgCode != Messages.MESSAGE_DIALOG_SUBMIT_CONFIRMATION){
					mMsg.arg1 = 0;	//TODO: (0 is for RESULT_CANCEL)temp assignment until the proper result constant is found
					dialog.dismiss();
					dialogBuilder = null;
					alertDialog = null;
				}else{
					mMsg.arg1 = 1;
					cancelCheck = true;
					((AlertDialog)dialog).getButton(which).setEnabled(false);
				}
				mCallerHandler.sendMessage(mMsg);

			}
		});

		alertDialog = dialogBuilder.create();

		// Set the OnDismissListener (if you need it)       
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				// dialog was just dismissed..
				if(cancelCheck && alertDialog != null){
					cancelCheck = false;
					alertDialog.show();
				}
			}
		});

		alertDialog.show();
	}

	/**
	 * @author Sathish.Avunoori
	 * @param context
	 * @param title
	 * @param message
	 * @param msgCode
	 * Method to show the Confirmation dialog with check box for show this message again or not
	 */
	private void showConfirmationAlertWithCheckBox(Context context, String title, String message,final Messages msgCode) {
		dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));		

		dialogBuilder.setTitle(title);
		dialogBuilder.setMessage(message);
		dialogBuilder.setCancelable(false);

		View checkBoxView = View.inflate(context, R.layout.custom_checkbox, null);
		final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
		checkBox.setText(context.getResources().getString(R.string.str_confirm_dont_show_this_message_again));
		dialogBuilder.setView(checkBoxView);

		dialogBuilder.setPositiveButton(context.getResources().getString(R.string.btn_label_ok),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				Message mMsg = new Message();
				mMsg.what = msgCode.ordinal();

				mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_IMPORT_DATA_DONT_SHOW_AGAIN, checkBox.isChecked());
				mMsg.arg1 = -1;	
				mCallerHandler.sendMessage(mMsg);
				dialog.dismiss();
				dialogBuilder = null;
				alertDialog = null;
			}
		});

		dialogBuilder.setNegativeButton(context.getResources().getString(R.string.btn_label_cancel),
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Message mMsg = new Message();
				mMsg.what = msgCode.ordinal();
				mMsg.arg1 = 0;
				mPrefUtils.putPrefValueBoolean(mPrefUtils.KEY_IMPORT_DATA_DONT_SHOW_AGAIN, checkBox.isChecked());
				mCallerHandler.sendMessage(mMsg);
				dialog.dismiss();
				dialogBuilder = null;
				alertDialog = null;

			}
		});

		alertDialog = dialogBuilder.create();

		// Set the OnDismissListener (if you need it)       
		alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {

			}
		});

		alertDialog.show();

	}

	private void showErrorAlert(Context context, String title, String message,
			final Messages msgCode) {

		dialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog));
		dialogBuilder.setTitle(title);
		dialogBuilder.setMessage(message);
		dialogBuilder.setCancelable(false);

		dialogBuilder.setPositiveButton(context.getResources().getString(R.string.btn_label_ok),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				if (mCallerHandler != null) {
					Message mMsg = new Message();
					mMsg.what = msgCode.ordinal();
					//mMsg.arg1 = Globals.resultState.RESULT_OK.ordinal();
					mMsg.arg1 = -1;	//TODO: (-1 is for RESULT_OK) temp assignment until the proper result constant is found
					mCallerHandler.sendMessage(mMsg);
				}
				dialog.dismiss();
				dialogBuilder = null;
				alertDialog = null;
			}
		});

		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}
}