import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    private static final int MAX_FRAME_QUEUE_SIZE = 1;
    private static final LinkedBlockingQueue<BufferedImage> frameQueue = new LinkedBlockingQueue<>(MAX_FRAME_QUEUE_SIZE);
    private static final LinkedBlockingQueue<Mat> displayQueue = new LinkedBlockingQueue<>(MAX_FRAME_QUEUE_SIZE);

    // Minimum delay (2 seconds) between upgrade attempts
    private static final long UPGRADE_DELAY_MS = 2000;
    private static volatile long lastUpgradeTime = 0;

    private static volatile boolean running = true;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) throws AWTException {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screenRect = new Rectangle(0, 0, screenSize.width / 2, screenSize.height);
        int windowXPosition = screenSize.width / 2;
        int windowYPosition = 0;

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        GameInputHandler inputHandler = new GameInputHandler();

        Thread frameProducer = new Thread(() -> {
            try {
                Robot robot = new Robot();
                while (running) {
                    BufferedImage screenImage = robot.createScreenCapture(screenRect);
                    frameQueue.poll();
                    frameQueue.offer(screenImage);
                }
            } catch (AWTException e) {
                e.printStackTrace();
            }
        });

        Thread frameConsumer = new Thread(() -> {
            while (running) {
                try {
                    BufferedImage screenImage = frameQueue.take();
                    Mat matScreen = bufferedImageToMat(screenImage);
                    List<Detection> detections = new ArrayList<>();

                    Future<Void> circlesFuture = executorService.submit(() -> {
                        CircleDetectionUtils.detectCircles(matScreen, detections);
                        return null;
                    });

                    Future<Void> blocksFuture = executorService.submit(() -> {
                        BlockDetectionUtils.detectObjects(matScreen, detections);
                        return null;
                    });

                    Future<Void> upgradesFuture = executorService.submit(() -> {
                        UpgradeDetectionUtils.detectUpgradePossibility(matScreen, detections);
                        return null;
                    });

                    circlesFuture.get();
                    blocksFuture.get();
                    upgradesFuture.get();
                    displayQueue.offer(matScreen);

                    Point playerPosition = null;
                    boolean upgrade = false;
                    boolean possible_death = false;
                    for (Detection detection : detections) {
                        if ("self".equals(detection.type())) {
                            playerPosition = detection.position();
                        }

                        if ("upgrade".equals(detection.type())) {
                            upgrade = true;
                        }

                        if ("possible_death".equals(detection.type())) {
                            possible_death = true;
                        }
                    }

                    // Check for player death
                    if (playerPosition == null && possible_death) {
                        System.out.println("Tank has been killed! Stopping the program...");
                        running = false;
                        break;  // Exit the consumer thread
                    }

                    if (playerPosition != null) {
                        RuleBasedActions.MoveAction moveAction = RuleBasedActions.ruleBasedActions(detections, playerPosition);
                        inputHandler.applyMovement(moveAction.moveX(), moveAction.moveY());
                        inputHandler.applyFire(moveAction.target());
                    }

                    if (upgrade) {
                        long currentTime = System.currentTimeMillis();

                        // Check if 2 seconds have passed since the last upgrade attempt
                        if (currentTime - lastUpgradeTime >= UPGRADE_DELAY_MS) {
                            inputHandler.upgradeTank();  // Call upgradeTank immediately
                            lastUpgradeTime = currentTime;  // Update the last upgrade time
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        Thread frameDisplayThread = new Thread(() -> {
            while (running) {
                try {
                    Mat frameToDisplay = displayQueue.take();
                    HighGui.imshow("Detected Objects", frameToDisplay);
                    HighGui.moveWindow("Detected Objects", windowXPosition, windowYPosition);
                    if (HighGui.waitKey(1) == 'q') break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        frameProducer.start();
        frameConsumer.start();
        frameDisplayThread.start();
    }

    private static Mat bufferedImageToMat(BufferedImage img) {
        if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage convertedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = convertedImg.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = convertedImg;
        }
        Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, pixels);
        return mat;
    }
}
