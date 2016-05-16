// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

    import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.kofax.kmc.kut.utilities.SdkVersion;
import com.kofax.mobilecapture.R;

    public class AboutActivity extends Activity {

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
        TextView mAppVersionView, mSDKVersionView;

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
                getActionBar().setTitle(getResources().getString(R.string.str_about_kmc));
            }
            getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            setContentView(R.layout.aboutscreen);
            mAppVersionView = (TextView)findViewById(R.id.app_versionview);
            mSDKVersionView = (TextView)findViewById(R.id.app_sdkversionview);
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = pInfo.versionName;
                mAppVersionView.setText(version);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            mSDKVersionView.setText(SdkVersion.getSdkVersion());
        }

        @Override
        public void finish() {
            super.finish();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            // Handle action buttons
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
