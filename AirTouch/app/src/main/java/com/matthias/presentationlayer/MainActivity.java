package com.matthias.presentationlayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity
{
	private final static String LOGTAG = "MainActivity";

	// buttons
	private Button btn_openAirPhoto;
	private Button btn_openAirVideo;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupButtons();
	}

	private void setupButtons()
	{
		btn_openAirPhoto = (Button) findViewById(R.id.button_main_openAirPhoto);
		btn_openAirPhoto.setOnClickListener(openAirPhotoListener);
		btn_openAirVideo = (Button) findViewById(R.id.button_main_openAirVideo);
		btn_openAirVideo.setOnClickListener(openAirVideoListener);
	}

	// ============================================================================================================
	// Button Click Listeners
	// ============================================================================================================

	private View.OnClickListener openAirPhotoListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			AirPhotoUI.startActivity(getApplicationContext());
		}
	};

	private View.OnClickListener openAirVideoListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			AirVideoUI.startActivity(getApplicationContext());
		}
	};

	// ============================================================================================================
	// OpenCV Loaders
	// ============================================================================================================

	// http://answers.opencv.org/question/16993/display-image/
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if (status == LoaderCallbackInterface.SUCCESS ) {
				// now we can call opencv code !
				Log.d(LOGTAG, "OpenCV is successfully loaded");
				Toast.makeText(MainActivity.this, "OpenCV is successfully loaded", Toast.LENGTH_SHORT).show();
				//				helloworld();
			} else {
				super.onManagerConnected(status);
				Log.d(LOGTAG, "OpenCV is not loaded");
				Toast.makeText(MainActivity.this, "OpenCV is not loaded", Toast.LENGTH_SHORT).show();
			}
		}
	};

	// http://answers.opencv.org/question/16993/display-image/
	@Override
	public void onResume()
	{
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
		// you may be tempted, to do something here, but it's *async*, and may take some time,
		// so any opencv call here will lead to unresolved native errors.
	}
}
