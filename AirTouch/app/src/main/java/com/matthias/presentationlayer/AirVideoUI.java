package com.matthias.presentationlayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.matthias.businesslogic.FingerDetectorBL;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


// todo: if completely same image, fingerDetector crash
// todo: if hand is not shown?

// todo: use 1 background, but with 1 instance?
// todo: (A,B), (B,C), (C,D) ?

// todo: maybe allow to change resolution of video in run time

/**
 * How to use Camera - Followed App OpenCVBackProjection, https://github.com/ruimarques/OpenCV-BackProjection
 */
public class AirVideoUI extends Activity implements CameraBridgeViewBase.CvCameraViewListener2
{
	private final static String LOGTAG = "AirVideoUI";

	private final static String TOGGLE_CAMERA_ON = "Camera View";
//	private final static String TOGGLE_APP_VIEW_ON = "App View";

	// for sample app
	AppViewHelper appViewHelper;
	private boolean isAppView;      // app view / hand view
	private TextView tv_toggleView;

	static void startActivity(Context context)
	{
		Intent intent = new Intent(context, AirVideoUI.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_air_video_ui);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnTouchListener(onTouchCameraSurfaceView);
		mOpenCvCameraView.enableView();
		Toast.makeText(AirVideoUI.this, "Java Camera Enabled", Toast.LENGTH_SHORT).show();

		// init var
		isAppView = false;
		tv_toggleView = (TextView) findViewById(R.id.textView_airVideo_toggleView);
		tv_toggleView.setText(TOGGLE_CAMERA_ON);
		tv_toggleView.setOnClickListener(onClickToggleView);
		background_bgr = null;      // null is used to check if backgronud is ready
		lastProcessedFrame_bgr = new Mat();

		// for clickling sample app
		appViewHelper = new AppViewHelper();

	}

	// ============================================================================================================
	// Android Activity State Change Listeners
	// ============================================================================================================

	@Override
	protected void onPause()
	{
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	// ============================================================================================================
	// OpenCV Camera Related Methods
	// ============================================================================================================

	private CameraBridgeViewBase mOpenCvCameraView;

	private Mat                  mIntermediateMat;
	private Mat 				 mGray;

	private int outputWidth=300;
	private int outputHeight=200;

	private Bitmap mBitmap;

	private boolean bpUpdated = false;

	private Mat mRgba;
	private Mat mHSV;
	private Mat mask;

	// Imgproc.floodFill vars
	private Mat mask2;
	private int lo = 20;
	private int up = 20;

	private Scalar newVal;
	private Scalar loDiff;
	private Scalar upDiff;

	private Range rowRange;
	private Range colRange;


	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(LOGTAG, "OpenCV loaded successfully");
					mOpenCvCameraView.enableView();
					Toast.makeText(AirVideoUI.this, "OpenCV Camera Loaded Successfully", Toast.LENGTH_SHORT).show();
				} break;
				default:
				{
					super.onManagerConnected(status);
				} break;
			}
		}
	};

	/**
	 * This method is invoked when camera preview has started. After this method is invoked
	 * the frames will start to be delivered to client via the onCameraFrame() callback.
	 *
	 * @param width  -  the width of the frames that will be delivered
	 * @param height - the height of the frames that will be delivered
	 */
	@Override
	public void onCameraViewStarted(int width, int height)
	{
		mRgba               = new Mat(height, width, CvType.CV_8UC3);
		mHSV                = new Mat();

		mIntermediateMat    = new Mat();
		mGray               = new Mat(height, width, CvType.CV_8UC1);

		mask2               = Mat.zeros( mRgba.rows() + 2, mRgba.cols() + 2, CvType.CV_8UC1 );
		newVal              = new Scalar(120, 120, 120 );
		loDiff              = new Scalar( lo, lo, lo );
		upDiff              = new Scalar( up, up, up );

		rowRange            = new Range(1, mask2.rows() - 1 );
		colRange            = new Range( 1, mask2.cols() - 1 );

		mBitmap             = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	}

	/**
	 * This method is invoked when camera preview has been stopped for some reason.
	 * No frames will be delivered via onCameraFrame() callback after this method is called.
	 */
	@Override
	public void onCameraViewStopped()
	{
		// Explicitly deallocate Mats
		if (mIntermediateMat != null)
			mIntermediateMat.release();
		mIntermediateMat = null;

		if(mHSV!= null)
			mHSV.release();
		mHSV = null;

		if(mGray!= null)
			mGray.release();
		mGray = null;

		if (mIntermediateMat != null)
			mIntermediateMat.release();
		mIntermediateMat = null;

		if (mBitmap != null) {
			mBitmap.recycle();
		}
	}


	// my variables
	private boolean readyToGetBackgroundImage;
	private boolean readyToStart;
	private boolean processingImage;
	private Mat background_bgr;                 // Hand with Highest Point that was last displayed
	private Mat lastProcessedFrame_bgr;         // Hand with Highest Point that was last displayed
	private Point clickPoint;

	/**
	 * This method is invoked when delivery of the frame needs to be done.
	 * The returned values - is a modified frame which needs to be displayed on the screen.
	 * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
	 *
	 * Note:
	 * FingerDetectorBL uses BGR, following OpenCV.Mat.
	 * Camera uses RGBA. Frames saved here to display are in RGBA, whereas frames to feed to FingerDetector are converted to BGR.
	 *
	 * @param inputFrame
	 */
	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
	{
		Mat mCamera_rgba = inputFrame.rgba();
		Mat mCamera_rgb = new Mat(mCamera_rgba.rows(), mCamera_rgba.cols(), CvType.CV_8UC3);
		Imgproc.cvtColor(mCamera_rgba, mCamera_rgb, Imgproc.COLOR_RGBA2RGB);
//		Mat mCamera_gray = inputFrame.gray();
		Mat mCamera_bgr = new Mat(mCamera_rgba.rows(), mCamera_rgba.cols(), CvType.CV_8UC3);
		Imgproc.cvtColor(mCamera_rgb, mCamera_bgr, Imgproc.COLOR_RGB2BGR);

		if (background_bgr == null)
		{
			if (readyToGetBackgroundImage)
			{
				background_bgr = mCamera_bgr;
			}

			return mCamera_rgb;
		}

		// wait until user press the surface view second time
		if (!readyToStart)
			return mCamera_rgb;

		// if not processing an image, start to update a new frame and capture updated finger point

		FingerDetectorBL fingerDetector = new FingerDetectorBL(background_bgr, mCamera_bgr);
		lastProcessedFrame_bgr = fingerDetector.getFinalViewFrame();      // BGR
		this.clickPoint = fingerDetector.getHighestPoint();

		// can happen if FingerDector couldnt find any convex hull and final view be null
		boolean isNotReady = lastProcessedFrame_bgr == null;

		if (isNotReady)
			return mCamera_rgb;

		// prepare to display
		Mat displayFrame_rgb = new Mat(mCamera_rgba.rows(), mCamera_rgba.cols(), CvType.CV_8UC3);


		if (isAppView)      // display App view
		{
			appViewHelper.processClickPoint(clickPoint);
			displayFrame_rgb = appViewHelper.getDisplay(displayFrame_rgb.size(), clickPoint);
 		}
		else                // display Camera View
		{
			Log.e(LOGTAG, String.format("lastProcessedFrame_bgr == null: %s", lastProcessedFrame_bgr == null));
			Log.e(LOGTAG, String.format("displayFrame_rgb == null: %s", displayFrame_rgb == null));
			Imgproc.cvtColor(lastProcessedFrame_bgr, displayFrame_rgb, Imgproc.COLOR_BGR2RGB);
		}


		return displayFrame_rgb;
	}

	// ============================================================================================================
	// Click Listeners
	// ============================================================================================================

	// process image to find the finger point

	View.OnTouchListener onTouchCameraSurfaceView = new View.OnTouchListener()
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				if (!readyToGetBackgroundImage)
				{
					readyToGetBackgroundImage = true;
					Toast.makeText(AirVideoUI.this, "Background is ready!\nTouch one more time to start app", Toast.LENGTH_SHORT).show();
				}

				// only call if background is ready, and
				// don't care this listener anymore after start processing
				else if (background_bgr != null && !readyToStart)
				{
					readyToStart = true;
					Toast.makeText(AirVideoUI.this, "Start Processing!", Toast.LENGTH_SHORT).show();
					tv_toggleView.setVisibility(View.VISIBLE);
				}
			}

			return true;
		}
	};

	View.OnClickListener onClickToggleView = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
//			if (isAppView)
//				tv_toggleView.setText(TOGGLE_APP_VIEW_ON);
//			else
//				tv_toggleView.setText(TOGGLE_CAMERA_ON);
			isAppView = !isAppView;
		}
	};
}
