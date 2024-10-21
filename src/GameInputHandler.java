import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.opencv.core.Point;

public class GameInputHandler {

    private final Robot robot;
    private boolean wPressed = false;
    private boolean aPressed = false;
    private boolean sPressed = false;
    private boolean dPressed = false;

    // Track the number of times each number key (1-8) is pressed
    private final Map<Integer, Integer> keyPressCount;
    private final Random random;

    public GameInputHandler() throws AWTException {
        // Initialize the Robot instance
        robot = new Robot();
        keyPressCount = new HashMap<>();
        random = new Random();

        // Initialize the map with keys 1-8 and set their press counts to 0
        for (int i = KeyEvent.VK_1; i <= KeyEvent.VK_8; i++) {
            keyPressCount.put(i, 0);
        }

    }

    // Method to apply movement
    public void applyMovement(double moveX, double moveY) {
        // Vertical movement: Up (W), Down (S)
        wPressed = handleKey(KeyEvent.VK_W, moveY < 0, wPressed);
        sPressed = handleKey(KeyEvent.VK_S, moveY > 0, sPressed);

        // Horizontal movement: Left (A), Right (D)
        aPressed = handleKey(KeyEvent.VK_A, moveX < 0, aPressed);
        dPressed = handleKey(KeyEvent.VK_D, moveX > 0, dPressed);
    }

    // Helper method to handle key press/release and return updated key press status
    private boolean handleKey(int keyCode, boolean shouldPress, boolean keyPressed) {
        if (shouldPress && !keyPressed) {
            robot.keyPress(keyCode);
            return true;  // Update keyPressed to true
        } else if (!shouldPress && keyPressed) {
            robot.keyRelease(keyCode);
            return false;  // Update keyPressed to false
        }
        return keyPressed;  // Return the original state if nothing changes
    }

    // Method to aim and fire at the target (mouse control)
    public void applyFire(Point target) {
        if (target != null) {
            // Move the mouse to the target position on the screen
            robot.mouseMove((int) target.x, (int) target.y);

            // Simulate a left mouse click to fire
            robot.mousePress(KeyEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(KeyEvent.BUTTON1_DOWN_MASK);
        }
    }

    // Function to press a weighted random number key from 1 to 8
    public void upgradeTank() {
        // Create a list of eligible keys (not pressed 8 or more times)
        List<Integer> eligibleKeys = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : keyPressCount.entrySet()) {
            if (entry.getValue() < 8) {
                eligibleKeys.add(entry.getKey());
            }
        }

        // If no keys are eligible, do nothing
        if (eligibleKeys.isEmpty()) {
            System.out.println("All number keys have been pressed 8 or more times.");
            return;
        }

        // Define weights for keys 1-8
        Map<Integer, Double> keyWeights = getKeyWeights();

        // Filter the eligible keys and calculate total weight
        double totalWeight = 0.0;
        List<Double> cumulativeWeights = new ArrayList<>();
        for (Integer key : eligibleKeys) {
            totalWeight += keyWeights.getOrDefault(key, 1.0);  // Use 1.0 as default weight
            cumulativeWeights.add(totalWeight);
        }

        // Generate a random number in the range of [0, totalWeight)
        double randomWeight = random.nextDouble() * totalWeight;

        // Select the key based on weighted probability
        int selectedKey = eligibleKeys.get(0);  // Fallback in case of any issues
        for (int i = 0; i < eligibleKeys.size(); i++) {
            if (randomWeight < cumulativeWeights.get(i)) {
                selectedKey = eligibleKeys.get(i);
                break;
            }
        }

        // Press the selected key
        robot.keyPress(selectedKey);
        robot.keyRelease(selectedKey);

        // Increment the press count for that key
        keyPressCount.put(selectedKey, keyPressCount.get(selectedKey) + 1);

        // Output the key pressed and the number of times it has been pressed
        System.out.println("Pressed key: " + (selectedKey - KeyEvent.VK_0) + " (pressed " + keyPressCount.get(selectedKey) + " times)");
    }

    private static Map<Integer, Double> getKeyWeights() {
        Map<Integer, Double> keyWeights = new HashMap<>();
        keyWeights.put(KeyEvent.VK_1, 1.0);  // Health regen - High priority
        keyWeights.put(KeyEvent.VK_2, 1.0);  // Max health - High priority
        keyWeights.put(KeyEvent.VK_3, 0.1);  // Body damage - Low priority
        keyWeights.put(KeyEvent.VK_4, 0.5);  // Bullet speed - Medium priority
        keyWeights.put(KeyEvent.VK_5, 1.0);  // Bullet penetration - High priority
        keyWeights.put(KeyEvent.VK_6, 1.0);  // Bullet damage - High priority
        keyWeights.put(KeyEvent.VK_7, 0.5);  // Reload - Medium priority
        keyWeights.put(KeyEvent.VK_8, 0.5);  // Movement speed - Medium priority
        return keyWeights;
    }

}
