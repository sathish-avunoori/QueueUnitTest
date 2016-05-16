// Copyright (c) 2014 Kofax. Use of this code is with permission pursuant to Kofax license terms.
package com.kofax.mobilecapture.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import com.kofax.kmc.ken.engines.data.BoundingTetragon;

public class DrawView extends View {

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
	private BoundingTetragon bound;
	private Paint paint;
	private long lastUpdate;
	private int top = 0;
	private int left = 0;
	private int previewWidth;
	private int previewHeight;
	private float xScale = 0;
	private float yScale = 0;
	private float alpha;
	
	// - public constructors
	public DrawView(Context context) { 
		this(context, null);
	}

	public DrawView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DrawView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		paint = new Paint();
		paint.setColor(0xFF00FF00);
	}

	// - private constructors
	// - Private constructor prevents instantiation from other classes
	
	// - public getters and setters
	
	// - public methods
	public void setBound(BoundingTetragon bound, int previewWidth, int previewHeight) {
		this.bound = bound;
		this.previewWidth = previewWidth;
		this.previewHeight = previewHeight;
	}

	public void update () {
		lastUpdate = System.currentTimeMillis();
	}

	public float getxScale() {
		return xScale;
	}

	public float getyScale() {
		return yScale;
	}

	public float getPaintAlpha() {
		return alpha;
	}	
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		this.left = left;
		this.top = top;
		super.onLayout(changed, left, top, right, bottom);
		getParent().bringChildToFront(this);
	}	

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		long curTime = System.currentTimeMillis();
		if (curTime - lastUpdate > 500)
			return;

		long curDelta = curTime - lastUpdate;
		alpha = 1;

		if (curDelta > 250) {
			alpha = (500 - curDelta) / 250f;
		}

		if (bound == null)
			return;

		paint.setColor(((int) (alpha * 255) << 24) | Color.YELLOW);
		paint.setStrokeWidth(2);
		paint.setStyle(Style.FILL_AND_STROKE);

		canvas.save();
		canvas.translate(left, top);
		canvas.scale((float) getWidth() / previewWidth, (float) getHeight() / previewHeight);

		this.xScale = ((float) getWidth() / previewWidth);
		this.yScale = ((float) getHeight() / previewHeight);
		
		float p0x = bound.getBottomLeft().x;
		float p0y = bound.getBottomLeft().y;
		float p1x = bound.getTopLeft().x;
		float p1y = bound.getTopLeft().y;
		float p2x = bound.getTopRight().x;
		float p2y = bound.getTopRight().y;
		float p3x = bound.getBottomRight().x;
		float p3y = bound.getBottomRight().y;

		canvas.drawLine(p0x, p0y, p1x, p1y, paint);
		canvas.drawLine(p1x, p1y, p2x, p2y, paint);
		canvas.drawLine(p2x, p2y, p3x, p3y, paint);
		canvas.drawLine(p3x, p3y, p0x, p0y, paint);

		canvas.restore();
	}

	// - private nested classes (more than 10 lines)
	
	// - private methods

}