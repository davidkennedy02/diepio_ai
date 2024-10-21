import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BlockDetectionUtils {

    private static final int THREAD_COUNT = 4; // One for each color

    public static List<Detection> detectObjects(Mat frame, List<Detection> detections) {
        // Convert the frame to HSV
        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV);

        // Define color ranges
        Scalar lowerPurple = new Scalar(100, 50, 50);
        Scalar upperPurple = new Scalar(160, 255, 255);
        Scalar lowerRed = new Scalar(0, 50, 50);
        Scalar upperRed = new Scalar(10, 255, 255);
        Scalar lowerYellow = new Scalar(20, 100, 100);
        Scalar upperYellow = new Scalar(30, 255, 255);

        try (ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT)) {
            List<Future<List<Detection>>> futures = new ArrayList<>();

            // Create tasks for each color mask
            futures.add(executorService.submit(() -> detectWithMask(hsvFrame, lowerRed, upperRed, frame, "block_red")));
            futures.add(executorService.submit(() -> detectWithMask(hsvFrame, lowerYellow, upperYellow, frame, "block_yellow")));
            futures.add(executorService.submit(() -> detectWithMask(hsvFrame, lowerPurple, upperPurple, frame, "block_purple")));

            // Collect results from all futures
            for (Future<List<Detection>> future : futures) {
                try {
                    detections.addAll(future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            executorService.shutdown();
        }
        return detections;
    }


    private static List<Detection> detectWithMask(Mat hsvFrame, Scalar lowerBound, Scalar upperBound, Mat frame, String type) {
        List<Detection> detections = new ArrayList<>();
        Mat mask = new Mat();

        // Create mask for the specific color
        Core.inRange(hsvFrame, lowerBound, upperBound, mask);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Get coordinates of bottom 20th of screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenBottomY = (screenSize.height / 20) * 19;
        int screenTopThirdY = (screenSize.height / 3);

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            boolean possible_death = false;
            if (area > 20) {
                double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), approx, 0.04 * perimeter, true);

                Rect boundingRect = Imgproc.boundingRect(contour);
                String label = "";

                if (boundingRect.y > screenBottomY && (Objects.equals(type, "block_yellow"))) {
                    continue;
                }

                if (approx.total() == 3) {
                    if (450 <= area && area <= 750 && (Objects.equals(type, "block_red"))) {
                        label = "Block (Triangle)";
                    } else if (50 <= area && area <= 450) {
                        label = "Enemy Drone";
                    }
                } else if (approx.total() == 4) {
                    if ((600 <= area && area <= 1100) && (Objects.equals(type, "block_yellow"))) {
                        label = "Block (Square)";
                    } else if (Objects.equals(type, "block_purple") && boundingRect.y <= screenTopThirdY && area > 8700 &&
                            (220 < boundingRect.width && 240 > boundingRect.width) &&
                            (boundingRect.height > 40 && boundingRect.height < 50)){
                        label = "Possible death screen";
                        possible_death = true;
                    }
                } else if (approx.total() == 5 && (1400 <= area && area <= 2300) && (Objects.equals(type, "block_purple"))) {
                    label = "Block (Pentagon)";
                }

                if (!label.isEmpty()) {
                    Imgproc.rectangle(frame, boundingRect.tl(), boundingRect.br(), new Scalar(0, 255, 255), 2);
                    Imgproc.putText(frame, label + ": " + (int) area + " : " + boundingRect.width + " : " + boundingRect.height, boundingRect.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 255), 2);
                    detections.add(new Detection((possible_death ? "possible_death" : label), new Point(boundingRect.x + (double) boundingRect.width / 2, boundingRect.y + (double) boundingRect.height / 2)));
                }
            }
        }

        return detections;
    }
}
