// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import com.kofax.mobilecapture.R;

public class LicenseAgreementActivity extends Activity {

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
    /* Standard variables */
    TextView mLView1, mLView2, mLView3,mLView4,mLView5,mLView6,mLView7,mLView8,mLView9,
             mLView10,mLView11,mLView12,mLView13,mLView14,mLView15,mLView16,mLView17,mLView18;

    // - public constructors

    // - private constructors
    // - Private constructor prevents instantiation from other classes

    // - public getters and setters

    // - public methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(getResources().getString(R.string.str_license_agreement));
        }
        getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        setContentView(R.layout.license_agreement_screen);

        mLView1 = (TextView)findViewById(R.id.app_licenseview1);
        mLView2 = (TextView)findViewById(R.id.app_licenseview2);
        mLView3 = (TextView)findViewById(R.id.app_licenseview3);
        mLView4 = (TextView)findViewById(R.id.app_licenseview4);
        mLView5 = (TextView)findViewById(R.id.app_licenseview5);
        mLView6 = (TextView)findViewById(R.id.app_licenseview6);
        mLView7 = (TextView)findViewById(R.id.app_licenseview7);
        mLView8 = (TextView)findViewById(R.id.app_licenseview8);
        mLView9 = (TextView)findViewById(R.id.app_licenseview9);
        mLView10 = (TextView)findViewById(R.id.app_licenseview10);
        mLView11 = (TextView)findViewById(R.id.app_licenseview11);
        mLView12 = (TextView)findViewById(R.id.app_licenseview12);
        mLView13 = (TextView)findViewById(R.id.app_licenseview13);
        mLView14 = (TextView)findViewById(R.id.app_licenseview14);
        mLView15 = (TextView)findViewById(R.id.app_licenseview15);
        mLView16 = (TextView)findViewById(R.id.app_licenseview16);
        mLView17 = (TextView)findViewById(R.id.app_licenseview17);
        mLView18 = (TextView)findViewById(R.id.app_licenseview18);

        
        String sourceString = "<b>" + getResources().getString(R.string.str_app_license_title1) + "</b> " + getResources().getString(R.string.str_app_license_text1); 
        mLView1.setText(Html.fromHtml(sourceString)); 
         sourceString = "<b>" + getResources().getString(R.string.str_app_license_title2) + "</b> " + getResources().getString(R.string.str_app_license_text2); 
        mLView2.setText(Html.fromHtml(sourceString));       

        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title3) + "</b> " + getResources().getString(R.string.str_app_license_text3)+ "<b>" + getResources().getString(R.string.str_app_license_Kofax_Support_Commitment)+ "</b>"+"" + getResources().getString(R.string.str_app_license_text3_1) ;
         sourceString = sourceString.replace("\n", "<br>");
        mLView3.setText(Html.fromHtml(sourceString));
         sourceString = "<b>" + getResources().getString(R.string.str_app_license_title4) + "</b> " + getResources().getString(R.string.str_app_license_text4); 
         sourceString = sourceString.replace("\n", "<br>");
        mLView4.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title5) + "</b> " + getResources().getString(R.string.str_app_license_text5); 
        mLView5.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title6) + "</b> " + getResources().getString(R.string.str_app_license_text6); 
        mLView6.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title7) + "</b> " + getResources().getString(R.string.str_app_license_text7);
        mLView7.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title8) + "</b> " + getResources().getString(R.string.str_app_license_text8);
        mLView8.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title9) + "</b> " + getResources().getString(R.string.str_app_license_text9)+"<br>"+"<br>"+ getResources().getString(R.string.str_app_license_text9_1)+"<br>"+"<br>" + getResources().getString(R.string.str_app_license_text9_2);
       
        mLView9.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title10) + "</b> " + getResources().getString(R.string.str_app_license_text10)+"<br>"+"<br>"+getResources().getString(R.string.str_app_license_text10_1);
        mLView10.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title11) + "</b> " + getResources().getString(R.string.str_app_license_text11);
        mLView11.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title12) + "</b> " + getResources().getString(R.string.str_app_license_text12);
        mLView12.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title13) + "</b> " + getResources().getString(R.string.str_app_license_text13);
        mLView13.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title14) + "</b> " + getResources().getString(R.string.str_app_license_text14);
        mLView14.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title15) + "</b> " + getResources().getString(R.string.str_app_license_text15);
        mLView15.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title16) + "</b> " + getResources().getString(R.string.str_app_license_text16);
        mLView16.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title17) + "</b> " + getResources().getString(R.string.str_app_license_text17);
        mLView17.setText(Html.fromHtml(sourceString));
        sourceString = "<b>" + getResources().getString(R.string.str_app_license_title18) + "</b> " + getResources().getString(R.string.str_app_license_text18);
        mLView18.setText(Html.fromHtml(sourceString));
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return true;
    }

    // - private nested classes (more than 10 lines)

    // - private methods

}
