package com.matthias.presentationlayer;

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.matthias.businesslogic.FingerDetectorBL;
import com.matthias.domain.NamedMat;
import org.opencv.android.Utils;
import org.opencv.core.*;

import java.util.ArrayList;
import java.util.List;

public class AirPhotoUI extends AppCompatActivity
{
	private final static String LOGTAG = "AirPhotoUI";
	private final static int PICK_PHOTO_FOR_RESULT_MASK = 1000;    // to get index to containers
	private final static int PICK_PHOTO_FOR_RESULT_1 = 1000;    // for photo container 1
	private final static int PICK_PHOTO_FOR_RESULT_2 = 1001;    // for photo container 2


	// pointers to who requested pick photo intent
	ListView listView;
	NamedMatListAdapter adapter;
	private List<NamedMat> images;

	// buttons
	private static Button btn_openPhoto1;
	private static Button btn_openPhoto2;
	private static Button btn_startCompare;
	private static Button btn_toggleColour;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_air_photo_ui);

		// set up stuff

		// always required
		images = new ArrayList<>();

		setupContainers();
		setupButtons();


	}

	static void startActivity(Context context)
	{
		Intent intent = new Intent(context, AirPhotoUI.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	private void setupContainers()
	{
		// find views
		listView = (ListView) findViewById(R.id.histComp_listview);
		adapter = new NamedMatListAdapter(this, images);
		listView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	private void setupButtons()
	{
		// find views
		btn_openPhoto1   = (Button) findViewById(R.id.button_histComp_openPhoto1);
		btn_openPhoto2   = (Button) findViewById(R.id.button_histComp_openPhoto2);
		btn_startCompare = (Button) findViewById(R.id.button_histComp_startCompare);
		btn_toggleColour = (Button) findViewById(R.id.button_histComp_toggleColour);

		// set click listeners
		btn_openPhoto1.setOnClickListener(openPhotoClickListener);
		btn_openPhoto2.setOnClickListener(openPhotoClickListener);
		btn_startCompare.setOnClickListener(comparePhotosClickListener);
	}


	// ============================================================================================================
	// Bitmap is loaded
	// ============================================================================================================

	private final static String name_pic0 = "Background";
	private final static String name_pic1 = "Target";
	private boolean isSet_pic0 = false;
	private boolean isSet_pic1 = false;

	// todo : if set again for pic0 and pic1 - need to replace, instead of add()
	private void processBitmap(Bitmap bitmap, int index)
	{
		String name;

		if (index == 0)
			name = name_pic0;
		else if (index == 1)
			name = name_pic1;
		else
			name = "Unknown: index " + index;

		Mat image = new Mat();
		Utils.bitmapToMat(bitmap, image);

		if (isSet_pic0 && index == 0)
		{
			images.get(0).getImage().release();
			images.set(0, new NamedMat(name, image));
		}
		else if (isSet_pic1 && index == 1)
		{
			images.get(1).getImage().release();
			images.set(1, new NamedMat(name, image));
		}
		else
		{
			images.add(new NamedMat(name, image));

			if (index == 0)
				isSet_pic0 = true;
			else if (index == 1)
				isSet_pic1 = true;
		}

		adapter.notifyDataSetChanged();
	}

	// ============================================================================================================
	// Click Listeners
	// ============================================================================================================

	// click load button to load an image from the gallery
	View.OnClickListener openPhotoClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			int requestCode = -1;

			if (v.equals(btn_openPhoto1))
			{
				requestCode = PICK_PHOTO_FOR_RESULT_1;      // for container 1
			}
			else if (v.equals(btn_openPhoto2))
			{
				requestCode = PICK_PHOTO_FOR_RESULT_2;      // for container 2
			}

			String msg1 = "openPhotoClickListener.onClick(): requesterNumber is " + requestCode + ". ";
			Log.d(LOGTAG, msg1);

			if ( requestCode < 0 )
			{
				String err = "openPhotoClickListener.onClick(): Invoke nothing and return.";
				Log.e(LOGTAG, err);
				return;
			}

			pickImageFromGallery(requestCode);
		}
	};

	View.OnClickListener comparePhotosClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			boolean comparable = images.size() >= 2;

			if (!comparable)
			{
				Toast.makeText(AirPhotoUI.this, "Both images have to be loaded!", Toast.LENGTH_SHORT).show();
				return;
			}

			// reset images - for 2+ time of compares
			NamedMat namedMat1 = images.get(0);
			NamedMat namedMat2 = images.get(1);

			for (int i = 2; i < images.size(); i++)
				images.get(i).getImage().release();

			images.clear();
			images.add(namedMat1);
			images.add(namedMat2);

			// prep required stuff for FingerDetector
			Mat img1 = namedMat1.getImage();
			Mat img2 = namedMat2.getImage();

			// get absolute difference from two images
			FingerDetectorBL fingerDetector = new FingerDetectorBL(img1, img2);
			List<NamedMat> debug_images = fingerDetector.getDebugImages();

			images.addAll(debug_images);
			adapter.notifyDataSetChanged();

//			Mat handSegmentation = fingerDetector.getHandSegmentation();

			// choose the final result - masking, filtering, blurring ... ?
//			Bitmap result = HistogramComparerBL.makeBitmapFromMat(handSegmentation);

			// wrap up
//			saveBitmapToRequested(result, PHOTO_CONTAINER_3);
//			drawToRequestedContainer(PHOTO_CONTAINER_3);
		}
	};

	// ============================================================================================================
	// Gallery Helper
	// ============================================================================================================

	void openImageInFullScreen(Bitmap bitmap)
	{
		Log.d(LOGTAG, "openImageInFullScreen(): is called");
		FullScreenPhotoUI.startActivity(getApplicationContext(), bitmap);
	}

	public void pickImageFromGallery(int requestCode)
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		Log.d(LOGTAG, "onActivityResult(): is called");

		if ((requestCode == PICK_PHOTO_FOR_RESULT_1 || requestCode == PICK_PHOTO_FOR_RESULT_2) && resultCode == Activity.RESULT_OK)
		{
			if (data == null)
			{
				Log.e(LOGTAG, "onActivityResult(): Intent data is null!");

				Toast.makeText(AirPhotoUI.this, "Intent data is null!", Toast.LENGTH_SHORT).show();
				return;
			}

			Log.d(LOGTAG, "onActivityResult(): data is not null");
			int index = requestCode - PICK_PHOTO_FOR_RESULT_MASK;

			try
			{
//				Uri selectedImageUri = data.getData();
//				String[] projection = {MediaStore.MediaColumns.DATA };
//				CursorLoader cursorLoader = new CursorLoader(this, selectedImageUri, projection, null, null,
//				                                             null);
//				Cursor cursor =cursorLoader.loadInBackground();
//				int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
//				cursor.moveToFirst();
//				String selectedImagePath = cursor.getString(column_index);
//
//				Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);

				Uri selectedImageUri;
				selectedImageUri = data.getData();
				String[] projection = {MediaStore.MediaColumns.DATA};
				Cursor cursor = managedQuery(selectedImageUri, projection, null, null, null);
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
				cursor.moveToFirst();

				String filePath = cursor.getString(column_index);

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(filePath, options);
				final int REQUIRED_SIZE = 200;
				int scale = 1;
				while (options.outWidth / scale / 2 >= REQUIRED_SIZE
						&& options.outHeight / scale / 2 >= REQUIRED_SIZE)
					scale *= 2;
				options.inSampleSize = scale;
				options.inJustDecodeBounds = false;
				Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

				processBitmap(bitmap, index);
			}
			catch (Exception e)
			{
				String prompt = "onActivityResult from code [" + resultCode + "]: error from opening the image file!";
				Toast.makeText(AirPhotoUI.this, prompt, Toast.LENGTH_SHORT).show();
				Log.e(LOGTAG, prompt);
				e.printStackTrace();
			}
		}
	}
}
