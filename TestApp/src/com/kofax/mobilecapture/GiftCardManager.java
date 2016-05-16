// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyStore;

import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kofax.kmc.ken.engines.data.Image;
import com.kofax.mobilecapture.utilities.CustSSLSocketFactory;
import com.kofax.mobilecapture.utilities.Globals;
import com.kofax.mobilecapture.utilities.Globals.ResultState;

/// This class consists of methods to invoke gift-card extraction and validation call and store the result.

public class GiftCardManager {
	// - private constants
	private final String TAG = GiftCardManager.class.getSimpleName();

	// - Private data.
	private static volatile GiftCardManager pSelf = null;
	private static Handler mCallerHandler = null;

	private Image mImageObj = null;
	private Globals.ResultState mResult;

	private String[] fieldNames = null;
	private String[] fieldValues = null;
	private String response = null;
	private String mImageFilePath = null;
	private String cardNumber = null;
	private String pinNumber = null;
	private String brandName = null;
	long extractionStartTime = 0;
	long validationStartTime = 0;
	private int mFieldsLength = 0;
	private boolean isValidated = true;

	private GiftCardManager() {

	}
    //! The factory method.
    /**
        This method returns a singleton object of GiftCardManager.
     */
	public static GiftCardManager getInstance(Handler handler) {
		if (pSelf == null) {
			synchronized (GiftCardManager.class) {
				if (pSelf == null) {
					pSelf = new GiftCardManager();		
				}
			}
		}
		mCallerHandler = handler;
		return pSelf;
	}

    //! Function to initiate the extraction service.
	public void extractGiftCardData(final String imageFilePath){
		resetExtractionParams();
		mImageFilePath = imageFilePath;
		extractionStartTime = System.currentTimeMillis();
		GiftCardExtractionTask task = new GiftCardExtractionTask();
		task.execute(new String[] { "giftcardextraction" });
	}

    //! Function to initiate validtion service.
	public void retrieveBalance(String brandName, String cardNumber, String pinNumber) {
		resetExtractionParams();
		this.brandName = brandName;
		this.cardNumber = cardNumber;
		this.pinNumber = pinNumber;
		validationStartTime = System.currentTimeMillis();

		GiftCardBalanceRetrievalTask task = new GiftCardBalanceRetrievalTask();
		task.execute();
	}

    //! Get array of field names.
	/**
	 * GiftCardManager maintains a array of field names which are received in extraction and validation response.
	 * 
	 * @return array of names of fields
	 */
	public String[] getFieldNames() {
		return fieldNames;
	}

    //! Get array of field values.
	/**
	 * GiftCardManager maintains a array of field values which are received in extraction and validation response.
	 * 
	 * @return array of values of fields
	 */
	public String[] getFieldValues() {
		return fieldValues;
	}

    //! Get length of fields array received in extraction or validation response.
	/** 
	 * @return response array length
	 */
	public int getFieldsLength() {
		return mFieldsLength;
	}

    //! Get the processed image of the gift card.
	/**
	 * @return Image object
	 */
	public Image getImage() {
		return mImageObj;
	}

    //! Set the processed image of the gift card.
	public void setImage(Image imgObj) {
		mImageObj = imgObj;
	}

    //! Removes caller MessageHandler which was passed while creating instance of GiftCardManager.
	public void removeReceiverHandler(Handler handler) {
		if(handler == mCallerHandler) {
			mCallerHandler = null;
		}
		else {
			Log.e(TAG, "Handler Not same!!");
		}
	}

	public void cleanup() {
		resetExtractionParams();
		clearImage(mImageObj);
	}

	// - private nested classes (more than 10 lines)

	private class GiftCardExtractionTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... urls) {
			try {
				//String url = "http://mobilertti-beta.kofax.com:80/mobilesdk/api/GiftCard";
				//String url = "http://172.31.1.32:80/mobilesdk/api/GiftCard";
				String url = "https://mobile.kofax.com:8443/mobilesdk/api/GiftCard";

				final HttpClient httpclient = getNewHttpClient();
				final ResponseHandler<String> responseHandler = new BasicResponseHandler();

				MultipartEntity reqEntity = new MultipartEntity();
				final HttpPost post = new HttpPost(url);

				reqEntity.addPart("sessionKey", new StringBody("12345"));

				StringBody sb1;
				sb1 = new StringBody("false");

				reqEntity.addPart("ProcessImage", sb1);
				String imageHeader = "", imageExtension = "";
				imageHeader = "image/tiff";
				imageExtension = ".tif";

				FileBody bin = new FileBody(new File(mImageFilePath), imageHeader);
				reqEntity.addPart("img0"+imageExtension, bin);

				post.setEntity(reqEntity);

				// execute request
				response = httpclient.execute(post, responseHandler);
				Log.i(TAG, "DataExtractionResponse :: "+response);

				mResult = processHttpExtractionResponse(response);

				long stopTime = System.currentTimeMillis();
				long elapsedTime = stopTime - extractionStartTime;
				Log.d(TAG, "Extraction time for card: " + getCardBrand() + " ==> " + elapsedTime/1000);

				if (reqEntity != null) 
					reqEntity.consumeContent();
				httpclient.getConnectionManager().shutdown();
			}
			catch (UnsupportedEncodingException e1) {
				mResult = Globals.ResultState.RESULT_OK;
				e1.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}		
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			if(mCallerHandler != null) {
				Message msg = new Message();
				msg.what = Globals.Messages.MESSAGE_GIFTCARD_EXTRACTION_COMPLETED.ordinal();
				Log.i(TAG, "mExtractionResult ============================******************>>>"+mResult);
				msg.arg1 = mResult.ordinal();
				mCallerHandler.sendMessage(msg);
			}
		}
	}

	private class GiftCardBalanceRetrievalTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			String url = null;

			if(pinNumber != null && pinNumber.length() > 0) {
				//url = "http://mobilertti-beta.kofax.com:80/mobilesdk/api/Validation/GiftCard?validate=true&class=GiftCard&xCardNumber=" + cardNumber + "&xPinNumber=" + pinNumber + "&xBrand=" +  getCardBrand().replaceAll(" ", "%20") +"&xBalance";	
				//url = "http://172.31.1.32:80/mobilesdk/api/Validation/GiftCard?validate=true&class=GiftCard&xCardNumber=" + cardNumber + "&xPinNumber=" + pinNumber + "&xBrand=" +  getCardBrand().replaceAll(" ", "%20") +"&xBalance";
				url = "https://mobile.kofax.com:8443/mobilesdk/api/Validation/GiftCard?validate=true&class=GiftCard&xCardNumber=" + cardNumber + "&xPinNumber=" + pinNumber + "&xBrand=" +  getCardBrand().replaceAll(" ", "%20") +"&xBalance";
			}
			else {
				//pin number (access code) is not mandatory
				//url = "http://mobilertti-beta.kofax.com:80/mobilesdk/api/Validation/GiftCard?validate=true&class=GiftCard&xCardNumber=" + cardNumber + "&xBrand=" +  getCardBrand().replaceAll(" ", "%20") +"&xBalance";
				//url = "http://172.31.1.32:80/mobilesdk/api/Validation/GiftCard?validate=true&class=GiftCard&xCardNumber=" + cardNumber + "&xBrand=" +  getCardBrand().replaceAll(" ", "%20") +"&xBalance";
				url = "https://mobile.kofax.com:8443/mobilesdk/api/Validation/GiftCard?validate=true&class=GiftCard&xCardNumber=" + cardNumber + "&xBrand=" +  getCardBrand().replaceAll(" ", "%20") +"&xBalance";
			}
			Log.i(TAG, "url :: "+url);

			String response = executeGetRequest(url);
			Log.i(TAG, "Validation Response :: "+response);

			mResult = processHttpValidationResponse(response);
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - validationStartTime;
			Log.d(TAG, "Validation time for card: " + getCardBrand() + " ==> " + elapsedTime/1000);
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			if(mCallerHandler != null) {
				Message msg = new Message();
				msg.what = Globals.Messages.MESSAGE_GIFTCARD_VALIDATION_COMPLETED.ordinal();
				Log.e(TAG, "Sending message with result ===================> " + isValidated);
				msg.arg1 = mResult.ordinal();
				mCallerHandler.sendMessage(msg);
			}
		}
	}

	// - private methods
	
	private String executeGetRequest(String url) {
		HttpClient httpclient = getNewHttpClient();
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response = null;

		try {
			HttpGet request = new HttpGet();

			HttpParams params = httpclient.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 20000);
			HttpConnectionParams.setSoTimeout(params, 20000);
			request.setParams(params);

			request.setURI(new URI(url));
			response = httpclient.execute(request, responseHandler);

		}catch (Exception e){
			response = "Error: " + e.getMessage() + "\nCause: " + e.getCause();
			e.printStackTrace();
		}
		return response;
	}



	private HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new CustSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			Log.e(TAG, "getNewHttpClient exception: " + e.toString());
			return new DefaultHttpClient();
		}
	}

	private Globals.ResultState processHttpExtractionResponse(String extractionResponse) {
		Log.e(TAG, "Enter:: processHttpResponse ::  " + extractionResponse);

		JSONObject mJSONObject = null;
		try {
			JSONArray outerArray = new JSONArray(extractionResponse);
			Log.i(TAG, outerArray.length()+"");
			for(int i = 0; i < outerArray.length(); i++){
				mJSONObject = (JSONObject) outerArray.get(i);
				JSONArray mFields = mJSONObject.getJSONArray("fields");
				JSONObject mJSONObj = null;
				mFieldsLength = mFields.length();
				fieldNames = new String[mFieldsLength];
				fieldValues = new String[mFieldsLength];
				Log.d(TAG, "=========================== EXTRACTION RESPONSE ================================");
				for (int j = 0; j < mFieldsLength; j++) {
					mJSONObj = mFields.getJSONObject(j);	
					if (mJSONObj != null) {
						String fieldName = getValue(mJSONObj, "name");
						String fieldValue = getValue(mJSONObj, "text");
						String fieldValid = getValue(mJSONObj, "valid");
						String fieldErrDesc = getValue(mJSONObj, "errorDescription");
						Log.e("Test", "fieldName ==> " + fieldName + ", FieldValue ==> " + fieldValue + ", FieldValid ==> " + fieldValid + ", FieldErrDesc ==> " + fieldErrDesc + "\n");
						fieldNames[j] = fieldName;
						fieldValues[j] = fieldValue;
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return ResultState.RESULT_FAILED;
		}
		return ResultState.RESULT_OK;
	}

	private Globals.ResultState processHttpValidationResponse(String validationResponse) {
		JSONObject mJSONObject = null;
		try {

			mJSONObject = new JSONObject(validationResponse);
			Log.i("DataValidationResponse", ""+validationResponse);
			JSONArray mFields = mJSONObject.getJSONArray("fields");

			JSONObject mJSONObj = null;
			mFieldsLength = mFields.length();
			fieldNames = new String[mFieldsLength];
			fieldValues = new String[mFieldsLength];
			Log.d(TAG, "=========================== GET-BALANCE RESPONSE ================================");
			for (int i = 0; i < mFieldsLength; i++) {
				mJSONObj = mFields.getJSONObject(i);
				if (mJSONObj != null) {
					String fieldName = getValue(mJSONObj, "name");
					String fieldValue = getValue(mJSONObj, "text");
					String fieldValid = getValue(mJSONObj, "valid");
					String fieldErrDesc = getValue(mJSONObj, "errorDescription");
					Log.e("Test", "fieldName ==> " + fieldName + ", FieldValue ==> " + fieldValue + ", FieldValid ==> " + fieldValid + ", FieldErrDesc ==> " + fieldErrDesc + "\n");
					fieldNames[i] = fieldName;
					fieldValues[i] = fieldValue;
				}
				mJSONObj = null;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return ResultState.RESULT_FAILED;
		}
		return ResultState.RESULT_OK;
	}


	private String getValue(JSONObject obj, String type) {
		String result = "";
		if (obj != null) {
			try {
				result = obj.getString(type);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private void resetExtractionParams() {
		fieldNames = null;
		fieldValues = null;
		mFieldsLength = 0;
		response = null;
		mImageFilePath = null;
		mResult = ResultState.RESULT_FAILED;
	}

	private void clearImage(Image img) { 
		if (img != null) {
			img.imageClearBitmap(); 
			img = null; 
		} 
	}

	/**
	 * @return the mCardBrand
	 */
	public String getCardBrand() {
		return brandName;
	}

	/**
	 * @param mCardBrand the mCardBrand to set
	 */
	public void setCardBrand(String mCardBrand) {
		this.brandName = mCardBrand;
	}

}
