package com.matthias.domain;

import android.graphics.Bitmap;
import com.matthias.businesslogic.HistogramComparerBL;
import org.opencv.core.Mat;

/**
 * Created by Matthias on 16-04-24.
 */
public class NamedMat
{
	private String name;
	private Mat image;
	private Bitmap image_bitmap;

	public NamedMat(String name, Mat image)
	{
		this.name  = name;
		this.image = image;
		image_bitmap = HistogramComparerBL.makeBitmapFromMat(image);
	}

	public String getName()
	{ return name; }

	public Mat getImage()
	{ return image; }

	public Bitmap getImageBitmap()
	{ return image_bitmap; }
}
