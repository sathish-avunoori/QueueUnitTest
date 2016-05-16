/** Copyright (c) 2012-2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
 * 
 */

package com.kofax.mobilecapture.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.kofax.mobilecapture.AppStatsManager;



/**
 * This Task handles the connection to the server to get extracted data, and is
 * also responsible for sending the server response to the handler.
 */

public class WebServiceHelper extends AsyncTask<String, Void, String> {

    Handler mHandler = null;
    
    String mFilePath = null;

    String serverUrl = "";
    
    int httpPort =80;
    int httpsPort = 443;
    
    private String TAG = WebServiceHelper.class.getSimpleName();

    private AppStatsManager mAppStatsManager = null;

    public WebServiceHelper(String filePath, String exportServerUrl, int httpPort, int httpsPort) {
    	Log.e(TAG, "Enter:: WebServiceHelper -- constructor");
        serverUrl = exportServerUrl;
        mFilePath = filePath;
        
        mAppStatsManager = AppStatsManager.getInstance(null);
        
        if(httpPort>0){
            this.httpPort = httpPort;
        }
        
        if(httpsPort>0){
            this.httpsPort = httpsPort;
        }
        
    }

    @Override
    protected String doInBackground(String... arg0) {
    	Log.e(TAG, "Enter:: doInBackground");
        mAppStatsManager.setCanStartRecord(false);
        mAppStatsManager.stopAppStatsRecord();
        HttpClient httpclient = getNewHttpClient();
        ResponseHandler<String> responseHandler = new BasicResponseHandler();

       InputStreamEntity requestEntity;
        try {
            File file = new File(mFilePath);
            if(!file.exists()){
                Log.e(TAG, "App stats File is not available");
                return null;
            }
            
            requestEntity = new InputStreamEntity(new FileInputStream(mFilePath), -1);
            requestEntity.setChunked(true); // Send in multiple parts if needed
            
            // http://172.31.70.102/mobilesdk/api/appStats
            HttpPut request = new HttpPut(serverUrl);     
            HttpParams params = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 60000);
            HttpConnectionParams.setSoTimeout(params, 60000);
            request.setParams(params);

            request.addHeader(HTTP.CONTENT_TYPE, " application/json");
            request.setEntity(requestEntity);

            String response = httpclient.execute(request, responseHandler);
            processHttpResponse(response);

        } catch (ClientProtocolException e) {
            Log.e(TAG, "Client Protocol Exception while connecting server for exporting appstats file ");
            purgeAppStatsandStartRecord();
           // Toast.
           // e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IO Exception while connecting server for exporting appstats file, error message");
            purgeAppStatsandStartRecord();
           // e.printStackTrace();
        } catch (IllegalArgumentException e) {

            Log.e(TAG, "IIllegal Argument Exception while connecting server for exporting appstats file, error message");
            purgeAppStatsandStartRecord();
        }catch (Exception e) {
            Log.e(TAG, "Exception while connecting server for exporting appstats file, error message");
            purgeAppStatsandStartRecord();
        }

        requestEntity = null;

        return null;
    }

    /* Get Fields from JSON Object. */
    private int processHttpResponse(String extractionResponse) {
        Log.i("App Stats Export to Server Responce", extractionResponse);
        purgeAppStatsandStartRecord();
        
        return 0;
    }

    /*
     * Create new Http Client and return it. If it fails to create it, then it
     * will returns the default Http Client.
     */
    HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new CustSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), httpPort));
            registry.register(new Scheme("https", sf, httpsPort));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    
    private void purgeAppStatsandStartRecord(){
    	Log.e(TAG, "Enter:: purgeAppStatsandStartRecord");
        mAppStatsManager.purgeAppStats();
        mAppStatsManager.setCanStartRecord(true);
        if (Constants.APP_STAT_ENABLED && !mAppStatsManager.isRecordingOn()) {
        	mAppStatsManager.startAppStatsRecord();
        }
    }
}
