package com.matthias.presentationlayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import com.matthias.businesslogic.HistogramComparerBL;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

// Zoom in / out: http://stackoverflow.com/questions/6650398/android-imageview-zoom-in-and-zoom-out/6650484#6650484
public class FullScreenPhotoUI extends AppCompatActivity implements View.OnTouchListener
{
	private final static String LOGTAG = "FullScreenPhotoUI";

	private static String TAG_BITMAP = "DATA_BITMAP";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_full_screen_photo);

		Log.d(LOGTAG, "onCreate(): is called");

//		String filepath = getIntent().getStringExtra(TAG_BITMAP);
//		Log.d(LOGTAG, "onCreate(): imageFilepath: " + filepath);

		positionHistory = new ArrayList<>();

		long nativeAddr = getIntent().getLongExtra(TAG_BITMAP, -1);

		Log.d(LOGTAG, "onCreate(): nativeAddr: " + nativeAddr);

		if (nativeAddr != -1)
			displayBitmap(nativeAddr);
		else
			Toast.makeText(FullScreenPhotoUI.this, "bitmap is not successfully loaded!", Toast.LENGTH_SHORT).show();
	}

	// convert Bitmap to Mat: http://stackoverflow.com/questions/17390289/convert-bitmap-to-mat-after-capture-image-using-android-camera
	// passing native addr: http://stackoverflow.com/questions/29060376/how-do-i-send-opencv-mat-as-a-putextra-to-android-intent

	static void startActivity(Context context, Bitmap bitmap)
	{
		if (bitmap == null)
		{
			Toast.makeText(context, "bitmap is not loaded!", Toast.LENGTH_SHORT).show();
			return;
		}

		Log.d(LOGTAG, "static startActivity(): is called");
		Intent intent = new Intent(context, FullScreenPhotoUI.class);

		Mat mat = new Mat();

		Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		Utils.bitmapToMat(bmp32, mat);

		long addr = mat.getNativeObjAddr();

		intent.putExtra(TAG_BITMAP, addr);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}


	private void displayBitmap(long nativeAddr)
	{
		Mat tempImg = new Mat(nativeAddr);      // to avoid cases parent is killed
		Mat matImg = tempImg.clone();

		final Bitmap bitmap = HistogramComparerBL.makeBitmapFromMat(matImg);

		final ImageButton preview = (ImageButton) findViewById(R.id.imageButton_fullScreenPhoto_preview);

		assert (preview != null);

		preview.setImageBitmap(bitmap);
		preview.setOnTouchListener(this);
		preview.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				preview.setImageBitmap(bitmap);
				Toast.makeText(FullScreenPhotoUI.this, "long clicked!", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		preview.setLongClickable(true);
	}


	
	// ============================================================================================================
	// Reset Size if long click!
	// ============================================================================================================

	private List<float[]> positionHistory;

	// ============================================================================================================
	// Zoom in / out: http://stackoverflow.com/questions/6650398/android-imageview-zoom-in-and-zoom-out/6650484#6650484
	// ============================================================================================================

	private static final float MIN_ZOOM = 1f, MAX_ZOOM = 1f;

	// These matrices will be used to scale points of the image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();

	// The 3 states (events) which the user is trying to perform
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// these PointF objects are used to record the point(s) the user is touching
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		ImageView view = (ImageView) v;

		view.setScaleType(ImageView.ScaleType.MATRIX);
		float scale;

		dumpEvent(event);
		// Handle touch events here...

		switch (event.getAction() & MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_DOWN:   // first finger down only
				savedMatrix.set(matrix);
				start.set(event.getX(), event.getY());
				Log.d(LOGTAG, "mode=DRAG"); // write to LogCat
				mode = DRAG;
				break;

			case MotionEvent.ACTION_UP: // first finger lifted

			case MotionEvent.ACTION_POINTER_UP: // second finger lifted

				mode = NONE;
				Log.d(LOGTAG, "mode=NONE");

				// handle reset scale here
				positionHistory.add(new float[] {event.getX(), event.getY()});

				if (positionHistory.size() >= 2)
				{
					float[] previous = positionHistory.get(positionHistory.size() - 2);
					float[] now = positionHistory.get(positionHistory.size() - 1);

					// if close double click
					if (Math.abs(previous[0] - now[0]) <= 30 && Math.abs(previous[1] - now[1]) <= 30)
					{
						// reset!
						positionHistory.clear();

						view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

						return true;
					}
				}

				break;

			case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

				oldDist = spacing(event);
				Log.d(LOGTAG, "oldDist=" + oldDist);
				if (oldDist > 5f) {
					savedMatrix.set(matrix);
					midPoint(mid, event);
					mode = ZOOM;
					Log.d(LOGTAG, "mode=ZOOM");
				}
				break;

			case MotionEvent.ACTION_MOVE:

				if (mode == DRAG)
				{
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
				}
				else if (mode == ZOOM)
				{
					// pinch zooming
					float newDist = spacing(event);
					Log.d(LOGTAG, "newDist=" + newDist);
					if (newDist > 5f)
					{
						matrix.set(savedMatrix);
						scale = newDist / oldDist; // setting the scaling of the
						// matrix...if scale > 1 means
						// zoom in...if scale < 1 means
						// zoom out
						matrix.postScale(scale, scale, mid.x, mid.y);
					}
				}
				break;
		}

		view.setImageMatrix(matrix); // display the transformation on screen

		return true; // indicate event was handled
	}

    /*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

	private float spacing(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

	private void midPoint(PointF point, MotionEvent event)
	{
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/** Show an event in the LogCat view, for debugging */
	private void dumpEvent(MotionEvent event)
	{
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE","POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);

		if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP)
		{
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}

		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++)
		{
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}

		sb.append("]");
		Log.d("Touch Events ---------", sb.toString());
	}

	//  String filepath version - gray images are not to be saved in file system

	//	static void startActivity(Context context, String imageFilepath)
	//	{
	//		Log.d(LOGTAG, "static startActivity(): imageFilepath: " + imageFilepath);
	//		Intent intent = new Intent(context, FullScreenPhotoUI.class);
	//		intent.putExtra(TAG_BITMAP, imageFilepath);
	//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	//		context.startActivity(intent);
	//	}

	//	private void displayBitmap(String filepath)
	//	{
	//		Bitmap bitmap = BitmapFactory.decodeFile(filepath);
	//		ImageButton preview = (ImageButton) findViewById(R.id.imageButton_fullScreenPhoto_preview);
	//
	//		assert (bitmap != null);
	//		assert (preview != null);
	//
	//		preview.setImageBitmap(bitmap);
	//	}
	//
}
