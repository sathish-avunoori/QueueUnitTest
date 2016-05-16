// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.kofax.mobilecapture.DeviceSpecificIssueHandler;
import com.kofax.mobilecapture.GiftCardManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;

public class GiftCardListActivity extends Activity{

    private ArrayList<String> mGiftcardList = null;
    private BaseAdapter mListAdapter;
    private ListView mListView;
    private GiftCardManager mGiftCardManager = null;
    
    private final String[] brand = {"Target", "Nordstrom", "Starbucks", "Costco", "Walgreens"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		new DeviceSpecificIssueHandler().checkEntryPoint(this);

        setContentView(R.layout.giftcard_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        
        mGiftCardManager = GiftCardManager.getInstance(null);

        mGiftcardList = new ArrayList<String>();
        for (int i=0; i<brand.length; i++) {
        	mGiftcardList.add(brand[i]);
        }
        // get the listview
        mListView = (ListView)findViewById(R.id.giftcardListview);
        mListAdapter = new BaseListAdapter(this, mGiftcardList);

        // setting list adapter
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	mGiftCardManager.setCardBrand(brand[position]);
                openCaptureActivity(position);
            }
        });
        setResult(Globals.ResultState.RESULT_CANCELED.ordinal());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
       
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
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
    
    
    private class BaseListAdapter extends BaseAdapter {

        private Context _context;
        private ArrayList<String> gclist;

        public BaseListAdapter(Context context, ArrayList<String> list) {
            this._context = context;
            this.gclist = list;
        }

        @SuppressLint("InflateParams")
		@Override
        public View getView(int groupPosition, View convertView,
                ViewGroup parent) {
           
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.giftcardlistrow,
                        null);
                TextView tView = (TextView)convertView.findViewById(R.id.giftcard_textview);
                tView.setText(gclist.get(groupPosition));                
            }

            return convertView;
        }

    @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return gclist.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }
    }
    
    //private Methods
    
    private void openCaptureActivity(int position){
        Intent intent = new Intent(this, Capture.class);
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.STR_GIFTCARD_COUNT, position);
        bundle.putBoolean(Constants.STR_GIFTCARD, true);
        intent.putExtras(bundle);
        startActivity(intent);
    }

}
