package com.matthias.presentationlayer;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import org.opencv.core.*;

/**
 * Created by Matthias on 16-04-25.
 *
 * Helps to display App View, and provides function for click listeners
 */
public class AppViewHelper
{
	private final static String LOGTAG = "AppViewHelper";

	private static Point button_position    = new Point(100, 100);

	private static Point rect1_leftTop      = new Point(200, 200);
	private static Point rect1_rightBottom  = new Point(450, 450);

	private static Point rect2_leftTop      = new Point(700, 700);
	private static Point rect2_rightBottom  = new Point(950, 950);


	private static Point rect3_leftTop      = new Point(1100, 500);
	private static Point rect3_rightBottom  = new Point(1250, 650);


	private static Point rect4_leftTop      = new Point(500, 100);
	private static Point rect4_rightBottom  = new Point(950, 250);


	private static Point rect5_leftTop      = new Point(50, 400);
	private static Point rect5_rightBottom  = new Point(450, 900);

	private Point[][] rects;

	private int index_rect1;
	private int index_rect2;

	public AppViewHelper()
	{
		index_rect1 = 0;
		index_rect2 = 1;

		rects = new Point[][] {{rect1_leftTop, rect1_rightBottom},
		                       {rect2_leftTop, rect2_rightBottom},
		                       {rect3_leftTop, rect3_rightBottom},
		                       {rect4_leftTop, rect4_rightBottom},
		                       {rect5_leftTop, rect5_rightBottom} };

		previousClickPoint = null;
	}

	Mat getDisplay(Size screenSize, Point clickPoint)
	{
		Mat frame = Mat.zeros(screenSize, CvType.CV_8UC3);      // RGB ... as it's what camera expects

		// RGB
		Scalar white   = new Scalar(255, 255, 255);
		Scalar red     = new Scalar(255, 0, 0);
		Scalar green   = new Scalar(0, 255, 0);
		Scalar yellow  = new Scalar(255, 255, 0);

		Core.putText(frame, "App View", button_position, Core.FONT_ITALIC, 3, white);

		Core.rectangle(frame, rects[index_rect1][0], rects[index_rect1][1], green, Core.FILLED);
		Core.rectangle(frame, rects[index_rect2][0], rects[index_rect2][1], yellow, Core.FILLED);

		int radius = 20;
		Core.circle(frame, clickPoint, radius, red, Core.FILLED);

		return frame;
	}

	private Point previousClickPoint;
	private int hitNumber_rect1;
	private int hitNumber_rect2;
	private int hitNumber_required = 2;

	void processClickPoint(Point clickPoint)
	{
		if (previousClickPoint == null)
		{
			previousClickPoint = clickPoint;
			return;
		}

		Rect rect1 = new Rect(rects[index_rect1][0], rects[index_rect1][1]);
		Rect rect2 = new Rect(rects[index_rect2][0], rects[index_rect2][1]);

		if (!clickPoint.inside(rect1))
			hitNumber_rect1 = 0;
		else
		{
			hitNumber_rect1++;

			if (hitNumber_rect1 >= hitNumber_required)
			{
				hitNumber_rect1 = 0;
				index_rect1 = getNextRectIndex();
			}
		}

		if (!clickPoint.inside(rect2))
			hitNumber_rect2 = 0;
		else
		{
			hitNumber_rect2++;

			if (hitNumber_rect2 >= hitNumber_required)
			{
				hitNumber_rect2 = 0;
				index_rect2 = getNextRectIndex();
			}
		}

		Log.d(LOGTAG, String.format("%s:", "processClickPoint"));
		Log.d(LOGTAG, String.format("%s: %s", "clickPoint.inside(rect1)", clickPoint.inside(rect1)));
		Log.d(LOGTAG, String.format("%s: %s", "clickPoint.inside(rect2)", clickPoint.inside(rect2)));
		Log.d(LOGTAG, String.format("%s: %d", "hitNumber_rect1", hitNumber_rect1));
		Log.d(LOGTAG, String.format("%s: %d", "hitNumber_rect2", hitNumber_rect2));
	}



	/**
	 * @return a random number between [0, 4] that's not current one, not another one.
	 */
	int getNextRectIndex()
	{
		int random = (int) (Math.random() * 5);

		if (random == index_rect1 || random == index_rect2)
			random = getNextRectIndex();

		return random;
	}
}
