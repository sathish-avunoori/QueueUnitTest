// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kofax.mobilecapture.PrefManager;
import com.kofax.mobilecapture.R;
import com.kofax.mobilecapture.utilities.Constants;
import com.kofax.mobilecapture.utilities.Globals;

    
public class ServerTypeList extends Activity{
    private ArrayList<String> mServerTypeList = null;
    private PrefManager mPrefUtils = null;
    private int selection = 0;
    private BaseAdapter mListAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.servertypelist);
        mPrefUtils = PrefManager.getInstance();
        selection = getIntent().getIntExtra(Constants.STR_SERVER_TYPE,0);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        
        mServerTypeList = new ArrayList<String>(Globals.getServerTypeNames(getApplicationContext()));
        
        // get the listview
        mListView = (ListView)findViewById(R.id.serverListview);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListAdapter = new BaseListAdapter(this, mServerTypeList);

        // setting list adapter
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mPrefUtils.putPrefValueString(mPrefUtils.KEY_USR_SERVER_TYPE, Globals.getServerTypeName(position)); //Commit selected server type in preference
                 setResult(Globals.ResultState.RESULT_OK.ordinal());
                 finish();
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
        private ArrayList<String> dlist;

        public BaseListAdapter(Context context, ArrayList<String> list) {
            this._context = context;
            this.dlist = list;
        }

        @SuppressLint("InflateParams")
		@Override
        public View getView(int groupPosition, View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.servertype_listrow,
                        null);
                TextView tView = (TextView)convertView.findViewById(R.id.serverlist_textview);
                tView.setText(dlist.get(groupPosition));
                if(selection == groupPosition){
                   ImageView iView = (ImageView)convertView.findViewById(R.id.serverlist_iView);
                   iView.setVisibility(View.VISIBLE);
                }
            }

            return convertView;
        }

    @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return dlist.size();
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

    
}
