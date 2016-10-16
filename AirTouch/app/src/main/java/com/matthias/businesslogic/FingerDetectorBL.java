package com.matthias.businesslogic;

import com.matthias.domain.NamedMat;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matthias on 16-04-23.
 */
public class FingerDetectorBL
{
	private final static String LOGTAG = "FingerDetectorBL";

	// hand gesture fields
	public final static int HAND_GESTURE_UNDEFINED = -1;
	public final static int HAND_GESTURE_CLICK = 1;

	// fields for users to return - they are declared in detector methods
	private List<NamedMat> images_debug;
	private Mat img_handSegmentation_8uc1;      // only hand is white, all others black
	private MatOfPoint mop_handContour;         // used for img_handSegmentation_8uc1, found in detectHand() - the largest blob

	// after all done - AirVideo needs them
	private Mat img_handSegmentationWithFingerPoint_8uc3;      // hand is white, red clicking point
	private Mat img_allWithFingerPoint_8uc3;      // hand is white, red clicking point
	private Point highestPoint;

	// private fields - all of them will be available after constructor is called
	private Mat img_background_8uc3;
	private Mat img_background_8uc1;
	private Mat img_target_8uc3;
	private Mat img_target_8uc1;

	// convex hull detection
	private boolean isUsingLowestHeight;

	/**
	 * Once this constructor is called, all required info is obtained and saved to private fields of this instance.
	 * All required fields are calculated and obtained using background and target images.
	 * Users only need to use getters.
	 * If it is required to change the target frame, create a new instance.
	 *
	 * @param img_background_8uc3 background frame with BGR channel
	 * @param img_target_8uc3 target frame that contains with BGR channel
	 */
	public FingerDetectorBL(Mat img_background_8uc3, Mat img_target_8uc3)
	{
		this.img_background_8uc3    = img_background_8uc3;
		this.img_target_8uc3        = img_target_8uc3;

		// prep BW images
		this.img_background_8uc1    = new Mat();
		this.img_target_8uc1        = new Mat();
		Imgproc.cvtColor(img_background_8uc3, img_background_8uc1, Imgproc.COLOR_BGR2GRAY);
		Imgproc.cvtColor(img_target_8uc3, img_target_8uc1, Imgproc.COLOR_BGR2GRAY);

		// prep getters
		images_debug = new ArrayList<>();

		// prep for Convex Hull
		isUsingLowestHeight = true;
		mop_handContour = null;		        // null if no contour is found - to be checked in detectConvexHull

		// detect all potentially required info, in order of process 1 -> N
		detectHandSegmentation();

		if (mop_handContour == null)
		{
			img_handSegmentationWithFingerPoint_8uc3 = new Mat(img_background_8uc3.rows(), img_background_8uc3.cols(), CvType.CV_8UC3);
		}
		else
		{
			detectConvexHull();
		}
	}

	// ============================================================================================================
	// Getter Methods
	// All required information will be available after constructor exits.
	// ============================================================================================================

	/** Return Mat 8uc3 */
	public Mat getFinalViewFrame()
	{ return img_allWithFingerPoint_8uc3; }

	public Mat getHandSegmentationWithHighestPoint()
	{ return img_handSegmentationWithFingerPoint_8uc3; }

	public Point getHighestPoint()
	{ return highestPoint; }

	public Mat getHandSegmentation()
	{ return img_handSegmentation_8uc1; }

	/**
	 * Defection Points are points that "breaks" convex hull.
	 * @param img_convexHull see getConvexHull()
	 * @return
	 */
	public List<Point> getNumConvexDefectPoints(Mat img_convexHull)
	{
		return null;
	}

	public List<Point> getNumConvexHullPoints(Mat img_convexHull)
	{
		return null;
	}

	public int getHandGesture()
	{ return HAND_GESTURE_UNDEFINED; }

	public List<NamedMat> getDebugImages()
	{ return images_debug; }

	// ============================================================================================================
	// Processor Methods -  Methods to process the required fields
	// The following Method Blocks have to be executed in the order of Process Number, from 1 -> N.
	// This is because some blocks may require fields defined by previous blocks.
	// All the following detector methods should only be called by constructor.
	// All required fields should properly define all fields for getters.
	// Not following the above may result in unexpected behaviour, and/or a fatal error of the programme.
	// ============================================================================================================

	// ============================================================================================================
	// Detector Method Block 1. Hand Segmentation with BackgroundSubtractor
	// ============================================================================================================

	/**
	 * Use OpenCV.BackgroundSubtractor
	 * @return White area for hand, black for others.
	 */
	private void detectHandSegmentation()
	{
		Mat fgMask = detectFrameDifference();
		Mat handMask = detectHand(fgMask);

		img_handSegmentation_8uc1 = handMask;
	}

	private Mat detectFrameDifference()
	{
		// Prepare memory
		System.gc();

		double learningRate = 0.1;

		//		BackgroundSubtractorMOG mog = new BackgroundSubtractorMOG();            // cause insufficient memory errors on android
		BackgroundSubtractorMOG2 mog = new BackgroundSubtractorMOG2();
		Mat fgMask = new Mat();

		// works better on BW > BGR - source ? said in somewhere ...
		mog.apply(img_background_8uc1, fgMask, learningRate);
		mog.apply(img_target_8uc1, fgMask, learningRate);

		// make it white vs black using masks - make all non-blacks white
		Core.inRange(fgMask, new Scalar(1), new Scalar(256), fgMask);

		// debug - save image
		images_debug.add(new NamedMat("Hand Seg - Original", fgMask));

		// remove noise
		Mat kernel1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
		Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10));
		//		Imgproc.morphologyEx(fgMask, fgMask, kernel1);
		Imgproc.erode(fgMask, fgMask, kernel1);
		Imgproc.dilate(fgMask, fgMask, kernel2);

		// debug - save image
		images_debug.add(new NamedMat("Hand Seg - Noise Reduced", fgMask));

		// wrap up
		kernel1.release();
		kernel2.release();
		return fgMask;
	}

	/**
	 * http://harismoonamkunnu.blogspot.ca/2013/06/opencv-find-biggest-contour-using-c.html
	 * Find contour of Hand by finding the largest contour from the image
	 * And draw the contour of hand only.
	 * @param mask Hand + Noises are white in this
	 * @return Image with hand only, displayed in white in binary image
	 */
	private Mat detectHand(Mat mask)
	{
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

		// find largest
		int largest_index = -1;
		double largest_area = -1;
		for (int i = 0; i < contours.size(); i++)
		{
			double area = Imgproc.contourArea(contours.get(i));

			if (area > largest_area)
			{
				largest_area = area;
				largest_index = i;
			}
		}

		Scalar black = Scalar.all(0);
		Scalar white = Scalar.all(255);

		Mat dest = new Mat(mask.rows(), mask.cols(), CvType.CV_8UC1, black);

		// if background looks equal to target, no contours will be found
		if (largest_index >= 0)
		{
			Imgproc.drawContours(dest, contours, largest_index, white, Core.FILLED);

			// debug - save image
			mop_handContour = contours.get(largest_index);
			images_debug.add(new NamedMat("Hand Seg - Largest Blob", dest));
		}

		// wrap up
//		for (MatOfPoint matOfPoint : contours)
//			matOfPoint.release();
		return dest;
	}


	// ============================================================================================================
	// Detector Method Block 2. Convex Hull related methods, using Hand BW Mat
	// ============================================================================================================

	/**
	 *
	 * @return
	 */
	private Mat detectConvexHull()
	{
		// find biggest contours
		MatOfInt hull = new MatOfInt();
		Imgproc.convexHull(mop_handContour, hull, false);

		MatOfPoint mopOut = new MatOfPoint();
		mopOut.create((int) hull.size().height, 1, CvType.CV_32SC2);        // 32 bits signed 2 channels

		for (int i = 0; i < hull.size().height; i++)
		{
			int index = (int) hull.get(i, 0)[0];
			double[] point = new double[] {mop_handContour.get(index, 0)[0], mop_handContour.get(index, 0)[1] };
			mopOut.put(i, 0, point);
		}

		// prepare to do stuff
		List<Point> convexPoints = mopOut.toList();

		// Plot Points
		Mat img_convexPoints = new Mat(img_background_8uc1.rows(), img_background_8uc1.cols(), CvType.CV_8UC3);
		Scalar green = new Scalar(0, 255, 0);
		for (Point point : convexPoints)
			Core.circle(img_convexPoints, point, 10, green, Core.FILLED);
		images_debug.add(new NamedMat("Convex Points", img_convexPoints));

		// Connect Points with lines
		Mat img_convexPointsConnected = img_convexPoints.clone();
		Scalar gray = new Scalar(220, 210, 230);
		for (int i = 0; i < convexPoints.size() - 1; i++)
			Core.line(img_convexPointsConnected, convexPoints.get(i), convexPoints.get(i + 1), gray, 10);
		images_debug.add(new NamedMat("Convex Points Connected", img_convexPointsConnected));

		// show it with hand segmentation
		Mat img_convexPointsWithHand = img_convexPointsConnected.clone();
		Scalar white = new Scalar(255, 255, 255);
		List<MatOfPoint> contours = new ArrayList<>();
		contours.add(mop_handContour);
		Imgproc.drawContours(img_convexPointsWithHand, contours, 0, white, Core.FILLED);
		images_debug.add(new NamedMat("Hand with Convex Hull", img_convexPointsWithHand));

		// emphasise the highest point
		Mat img_handWithHighestConvexPoint = img_convexPointsWithHand.clone();
		Point highestPoint = findHighestPoint(convexPoints);
		Scalar red = new Scalar(0, 0, 255);
		Core.circle(img_handWithHighestConvexPoint, highestPoint, 40, red, Core.FILLED);
		images_debug.add(new NamedMat("Hand with Highest Point", img_handWithHighestConvexPoint));
		img_handSegmentationWithFingerPoint_8uc3 = img_handWithHighestConvexPoint;
		this.highestPoint = highestPoint;

		// prep for the final view
		img_allWithFingerPoint_8uc3 = new Mat(img_background_8uc3.rows(), img_background_8uc3.cols(), CvType.CV_8UC3);
		img_target_8uc3.copyTo(img_allWithFingerPoint_8uc3);
		for (int i = 0; i < convexPoints.size() - 1; i++)
			Core.line(img_allWithFingerPoint_8uc3, convexPoints.get(i), convexPoints.get(i + 1), gray, 10);
		Core.circle(img_allWithFingerPoint_8uc3, highestPoint, 40, red, Core.FILLED);
		images_debug.add(new NamedMat("Final View", img_allWithFingerPoint_8uc3));

		return null;
	}

	/**
	 *
	 * @param points
	 * @return a point that has highest y-value
	 */
	private Point findHighestPoint(List<Point> points)
	{
		if (points.size() == 0)
			return null;

		int highest_index = 0;
		int lowest_index = 0;

		// not valid - convex hull points do not give the number of points proportional to number of points at the outline
		// maybe yes for bigger blobs tho
		int similarity = 10;
		int numSimilarHigh = 0;
		int numSimilarLow  = 0;

		for (int i = 0; i < points.size(); i++)
		{
			double diff_high = points.get(i).y - points.get(highest_index).y;
			double diff_low = points.get(i).y - points.get(lowest_index).y;

			if (diff_high > 0)
			{
				highest_index = i;
			}
			if (diff_low < 0)
				lowest_index  = i;

		}

		Point clickPointAtTopFingerTip;
		if (isUsingLowestHeight)    // lowest y-value is considered the highest finger tip  - what i'm using now
			clickPointAtTopFingerTip = points.get(lowest_index);
		else    // highest y-value is considered the highest finger tip - might have to support change
			clickPointAtTopFingerTip = points.get(highest_index);

		return clickPointAtTopFingerTip;
	}

	// ============================================================================================================
	// Detector Method Block 3. Algorithms to recognise user interaction from Convex Hull
	// Hand Gestures can be recognised by checking the number of Defect Points
	// ============================================================================================================

}
