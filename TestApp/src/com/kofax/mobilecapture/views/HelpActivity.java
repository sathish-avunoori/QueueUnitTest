// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import com.kofax.mobilecapture.R;

public class HelpActivity extends Activity{

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
			getActionBar().setTitle(getResources().getString(R.string.str_help_info));
		}
		getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		setContentView(R.layout.help_screen);
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
