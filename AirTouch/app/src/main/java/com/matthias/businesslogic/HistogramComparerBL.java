package com.matthias.businesslogic;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * Created by Matthias on 16-04-17.
 */
public class HistogramComparerBL
{
	private final static String LOGTAG = "HistogramComparerBL";

//	public static Mat makeHistogram(String filepath)
//	{
//		Log.d(LOGTAG, "makeHistogram(): is called with filepath: " + filepath);
//
//		Mat src;
//
//		// Load image
//		src = Highgui.imread(filepath);
//
//		if (src.empty())
//		{
//			Log.e(LOGTAG, "makeHistogram(): src.empty(), return");
//			return null;
//		}
//
//		// Convert to gray scale
//		Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
//
//		return src;
//	}

	public static Bitmap makeBitmapFromMat(Mat img)
	{
		Bitmap bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
//		Mat argb = new Mat();
//		Imgproc.cvtColor(img, argb, Imgproc.COLOR_BGR2RGBA);
		Utils.matToBitmap(img, bitmap);
		return bitmap;
	}

	public static Mat makeGrayScaleEqualisedMat(Bitmap bitmap)
	{
//		Log.d(LOGTAG, "makeHistogram(): is called with filepath: " + originalFilepath);

		Mat src = new Mat();
		Mat dst = new Mat();

		// Load image
//		src = Highgui.imread(originalFilepath);
		Utils.bitmapToMat(bitmap, src);

		if (src.empty())
		{
			Log.e(LOGTAG, "makeHistogram(): src.empty(), return");
			return null;
		}

		// Convert to gray scale
		Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);

		// Apply Histogram Equalization to increase contrast
		Imgproc.equalizeHist(src, dst);

		// return result
		return dst;
	}

	// not filepath, because "compared result" would not have a filepath
	public static Mat makeHistogram(Bitmap bitmap, int maxRange)
	{
		Mat img = new Mat();
		Utils.bitmapToMat(bitmap, img);

		Mat hist = new Mat();

		List<Mat> imgs = new ArrayList<>();
		imgs.add(img);
		MatOfInt channels = new MatOfInt(0);                // number of dimensions
		MatOfInt histSize = new MatOfInt(maxRange);         // number of bins, bin size = ranges / number of bins
		MatOfFloat ranges = new MatOfFloat(0, maxRange);    // values between this range are measured

		Imgproc.calcHist(Arrays.asList(img), channels, new Mat(), hist, histSize, ranges);

//		for (int i = 0; i < 32; i++)
//		{
//			double[] values = hist.get(i, 0);
//
//			String row = "";
//
//			for (int j = 0; j < values.length; j++)
//				row += values[j] + j == values.length - 1 ? "" : ", ";
//
//			Log.d(LOGTAG, "makeHistogram: [" + i + "]: " + row);
//		}

		Log.d(LOGTAG, "makeHistogram: displaying Mat 'img' ... ");
		displayMatSize(img);
//		displayMat(img);

		Log.d(LOGTAG, "makeHistogram: displaying Mat 'hist' ... ");
		displayMatSize(hist);
		displayMat(hist);

		return hist;
	}

	public static void displayMatSize(Mat mat)
	{
		int rows = mat.rows();
		int cols = mat.cols();
		int length = mat.get(0, 0).length;
		Log.d(LOGTAG, "displayMat: (" + rows + ", " + cols + ", " + length + ")");
	}

	public static void displayMat(Mat mat)
	{
		for (int r = 0; r < mat.rows(); r++)
		{
			String row = r + ": ";

			for (int c = 0; c < mat.cols(); c++)
			{
				if (c == 0)
					row += "[" + c + ": ";

				double[] values = mat.get(r, c);

				for (int v = 0; v < values.length; v++)
				{
					if (v == 0)
						row += "[" + v + ": ";

					row += values[v];

					if (v == values.length - 1)
						row += "]";
					else
						row += ", ";
				}

				if (c == mat.cols() - 1)
					row += "]";
				else
					row += ", ";
			}

			if (r != mat.rows() - 1)
				row += ", " + "r: " + r + ", mat.rows(): " + mat.rows();

			Log.d(LOGTAG, "displayMat: " + row);
		}
	}

	// click listeners from UI

	public static Mat getAbsDiff(Bitmap pic1, Bitmap pic2)
	{
		Mat mat_pic1 = new Mat();
		Mat mat_pic2 = new Mat();

		Utils.bitmapToMat(pic1, mat_pic1);      // getRequested() handles gray scale
		Utils.bitmapToMat(pic2, mat_pic2);

		Mat mat_difference = new Mat();
		Core.absdiff(mat_pic1, mat_pic2, mat_difference);

		// maybe add turn on / off features for the functions here
		// for filtering, blurring, gaussian blur, threshold ...

		return mat_difference;
	}

	// http://docs.opencv.org/2.4/doc/tutorials/imgproc/shapedescriptors/find_contours/find_contours.html
	public static Mat getContours(Mat src)
//	public static List<MatOfPoint> getContours(Mat src)
	{
		// Load source image and convert it to gray
		Mat src_gray = new Mat();

		// Convert image to gray and blur it (Canny Process)
		Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.blur(src_gray, src_gray, new Size(3, 3));

		// Prepare to get Contours
		Mat canny_output = new Mat();
//		Vector<Vector<Point>> contours = new Vector<>();
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// Detect edges using Canny
		// L2Gradient is fault by default in c++ - http://docs.opencv.org/2.4/modules/imgproc/doc/feature_detection.html
		int thresh = 100;
//		Imgproc.Canny(src_gray, canny_output, 20, 60, 3, false);
		Imgproc.Canny(src_gray, canny_output, thresh, thresh * 2, 3, false);

		// Find Contours
		Imgproc.findContours(canny_output, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

//		return contours;

		// return drawing
		Mat drawing = Mat.zeros(canny_output.size(), CvType.CV_8UC3);
		for (int i = 0; i < contours.size(); i++)
		{
			int random = (i * 37) % 255;           // random(0, 255)
			Scalar color = new Scalar(random, random, random);
			Imgproc.drawContours(drawing, contours, i, color, 2, 8, hierarchy, 0, new Point());
		}


		return drawing;
	}

	// http://stackoverflow.com/questions/10176184/with-opencv-try-to-extract-a-region-of-a-picture-described-by-arrayofarrays
	public static List<Mat> getSubRegionFromContours(Mat src, List<MatOfPoint> contours)
	{
		//

		// Load source image and convert it to gray
		Mat src_gray = new Mat();

		// Convert image to gray and blur it (Canny Process)
		Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
		Imgproc.blur(src_gray, src_gray, new Size(3, 3));

		//


		List<Mat> subregions = new ArrayList<>();

		for (int i = 0; i < contours.size(); i++)
		{
			// Get bounding box for contour
			Rect roi = Imgproc.boundingRect(contours.get(i)); // This is a OpenCV function

			// Create a mask for each contour to mask out that region from image.
			Mat mask = Mat.zeros(src.size(), CvType.CV_8UC1);
			Imgproc.drawContours(mask, contours, i, new Scalar(255), Core.FILLED); // This is a OpenCV function

			// At this point, mask has value of 255 for pixels within the contour and value of 0 for those not in contour.

			// Extract region using mask for region
			Mat contourRegion = new Mat();
			Mat imageROI = new Mat();
			src_gray.copyTo(imageROI, mask); // 'src' is the image you used to compute the contours.
			contourRegion = imageROI.submat(roi);   // contourRegion = imageROI(roi);
			// Mat maskROI = mask(roi); // Save this if you want a mask for pixels within the contour in contourRegion.

			// Store contourRegion. contourRegion is a rectangular image the size of the bounding rect for the contour
			// BUT only pixels within the contour is visible. All other pixels are set to (0,0,0).
			subregions.add(contourRegion);
		}

		return subregions;
	}

	// http://answers.opencv.org/question/6054/what-is-the-difference-between-vecmat-and-mat/
	public static Mat concatImage(List<Mat> subregions)
	{
		Mat imageConcat = new Mat();
		for (Mat m : subregions)
			imageConcat.push_back(m);
		return imageConcat;
	}
}
