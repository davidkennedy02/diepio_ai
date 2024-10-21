import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.List;

public class CircleDetectionUtils {

    public static String classifyColor(Mat bgrColor) {
        // Convert BGR to HSV
        Mat hsvColor = new Mat();
        Imgproc.cvtColor(bgrColor, hsvColor, Imgproc.COLOR_BGR2HSV);

        // Get HSV values
        double[] hsv = hsvColor.get(0, 0);

        // Define HSV ranges for color classification
        double[] lowerBlue = {90, 150, 50};
        double[] upperBlue = {140, 255, 255};
        double[] darkLowerRed = {0, 100, 100};
        double[] darkUpperRed = {10, 255, 255};
        double[] brightLowerRed = {160, 100, 100};
        double[] brightUpperRed = {180, 255, 255};

        // Check for Blue
        if (isWithinRange(hsv, lowerBlue, upperBlue)) {
            return "Blue";
        }
        // Check for Red
        else if (isWithinRange(hsv, darkLowerRed, darkUpperRed) ||
                isWithinRange(hsv, brightLowerRed, brightUpperRed)) {
            return "Red";
        }

        return "Unknown";
    }

    private static boolean isWithinRange(double[] hsv, double[] lower, double[] upper) {
        return (hsv[0] >= lower[0] && hsv[0] <= upper[0]) &&
                (hsv[1] >= lower[1] && hsv[1] <= upper[1]) &&
                (hsv[2] >= lower[2] && hsv[2] <= upper[2]);
    }

    public static List<Detection> detectCircles(Mat frame, List<Detection> detections) {
        // Convert frame to grayscale
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(grayFrame, grayFrame, new Size(9, 9), 2, 2);

        // Get screen center coordinates
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenCenterX = screenSize.width / 4;
        int screenCenterY = screenSize.height / 2;

        // Define tolerance for how far the circle can be from the center
        int tolerance = 100;  // You can adjust this value

        // Use Hough Circle Transform to detect circles
        Mat circles = new Mat();
        Imgproc.HoughCircles(
                grayFrame,
                circles,
                Imgproc.HOUGH_GRADIENT,
                1.0,
                (double) grayFrame.rows() / 16,
                80,
                30,
                2,
                40
        );

        // If some circles are detected, draw them
        if (circles.cols() > 0) {
            for (int i = 0; i < circles.cols(); i++) {
                double[] circleData = circles.get(0, i);
                if (circleData == null) continue;

                int x = (int) Math.round(circleData[0]);
                int y = (int) Math.round(circleData[1]);
                int radius = (int) Math.round(circleData[2]);

                // Average color from a small patch around the circle center
                int patchSize = 5; // Small patch around the circle center
                Rect roi = new Rect(Math.max(0, x - patchSize), Math.max(0, y - patchSize), patchSize * 2, patchSize * 2);
                Mat colorPatch = new Mat(frame, roi);

                // Get average color from the patch
                Scalar averageColorScalar = Core.mean(colorPatch);
                String bgrColorString = String.format("BGR: (%.0f, %.0f, %.0f)", averageColorScalar.val[0], averageColorScalar.val[1], averageColorScalar.val[2]);

                if (20 <= radius && radius <= 50) {
                    String colorName = classifyColor(colorPatch);

                    if ("Blue".equals(colorName) && (Math.abs(x - screenCenterX) <= tolerance && Math.abs(y - screenCenterY) <= tolerance)) {
                        drawCircle(frame, x, y, radius, "Self: " + bgrColorString, new Scalar(255, 0, 0)); // Blue color for detected circle
                        detections.add(new Detection("self", new Point(x, y)));
                    } else if ("Red".equals(colorName)) {
                        drawCircle(frame, x, y, radius, "Enemy tank: " + bgrColorString, new Scalar(0, 0, 255)); // Red color for detected circle
                        detections.add(new Detection("enemy_tank", new Point(x, y)));
                    }
                } else if (2 <= radius && radius <= 24) {
                    String colorName = classifyColor(colorPatch);

                    if ("Red".equals(colorName)) {
                        drawCircle(frame, x, y, radius, "Enemy bullet: " + radius, new Scalar(0, 0, 255)); // Red color for detected circle
                        detections.add(new Detection("enemy_bullet", new Point(x, y)));
                    }
//                    else if ("Blue".equals(colorName)) {
//                        drawCircle(frame, x, y, radius, "Friendly bullet: " + bgrColorString, new Scalar(0, 0, 255)); // Red color for detected circle
//                        detections.add(new Detection("friendly_bullet", new Point(x, y)));
//                    }
                }
            }
        }

        return detections;
    }

    private static void drawCircle(Mat frame, int x, int y, int radius, String label, Scalar color) {
        Imgproc.circle(frame, new Point(x, y), radius, color, 2);
        Imgproc.circle(frame, new Point(x, y), 2, new Scalar(0, 0, 255), 3);
        Imgproc.putText(frame, label, new Point(x - 20, y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
    }
}
