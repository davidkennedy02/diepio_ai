import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class UpgradeDetectionUtils {
    public static List<Detection> detectUpgradePossibility(Mat frame, List<Detection> detections) {
        // Convert the frame to HSV
        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV);

        // Define HSV range for the green bar (reload upgrade)
        Scalar lowerReload = new Scalar(50, 50, 50);  // Green
        Scalar upperReload = new Scalar(80, 255, 255);

        // Limit detection area to bottom left (where the upgrades are)
        Rect roi = new Rect(0, frame.rows() - (frame.rows() / 4), frame.cols() / 4, frame.rows() / 4);
        Mat roiFrame = new Mat(hsvFrame, roi);  // Crop the region of interest (ROI)

        // Detect the upgrade bar
        detectUpgradeBar(roiFrame, lowerReload, upperReload, frame, detections, roi);

        return detections;
    }

    private static void detectUpgradeBar(Mat hsvFrame, Scalar lowerBound, Scalar upperBound, Mat frame, List<Detection> detections, Rect roi) {
        // Create mask for the specific color
        Mat mask = new Mat();
        Core.inRange(hsvFrame, lowerBound, upperBound, mask);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > 50) {  // Adjust area threshold if needed
                Rect boundingRect = Imgproc.boundingRect(contour);

                // Draw rectangle around the upgrade bar
                Imgproc.rectangle(frame,
                        new Point(boundingRect.x + roi.x, boundingRect.y + roi.y),
                        new Point(boundingRect.x + boundingRect.width + roi.x, boundingRect.y + boundingRect.height + roi.y),
                        new Scalar(0, 255, 0), 2);

                // Add detection to the list
                detections.add(new Detection("upgrade",
                        new Point(boundingRect.x + boundingRect.width / 2.0 + roi.x, boundingRect.y + boundingRect.height / 2.0 + roi.y)));
            }
        }
    }
}

