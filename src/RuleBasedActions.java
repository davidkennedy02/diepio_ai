import java.util.List;
import java.util.Random;
import org.opencv.core.Point;

public class RuleBasedActions {

    // Constants
    private static final double TARGET_APPROACH_THRESHOLD = 10000;
    private static final double BLOCK_AVOID_DISTANCE = 150;
    private static final double BULLET_SAFETY_DISTANCE = 2000;  // New threshold for "safe" bullet distance
    private static final Random random = new Random();

    // Class variables for random movement
    private static double randomMoveX = 0;
    private static double randomMoveY = 0;
    private static long lastRandomMoveTime = 0;
    private static final long RANDOM_MOVE_DURATION = 3000;

    public static MoveAction ruleBasedActions(List<Detection> detections, Point playerPosition) {
        // Default actions
        double moveX;
        double moveY;
        Point target = null;

        double avoidanceX = 0;
        double avoidanceY = 0;

        Detection closestTarget = null;
        double closestTargetDistance = Double.MAX_VALUE;

        // Loop through detections
        for (Detection obj : detections) {
            String objType = obj.type();
            Point objPosition = obj.position();

            // Calculate distance from player's position
            double dist = distanceBetweenPoints(playerPosition, objPosition);

            // Avoid bullets, enemy tanks, and enemy drones
            if ("enemy_bullet".equals(objType) || "enemy_tank".equals(objType) || "enemy_drone".equals(objType)) {
                double[] avoidanceVector = calculateAvoidanceVector(playerPosition, objPosition, dist, objType);
                avoidanceX += avoidanceVector[0];
                avoidanceY += avoidanceVector[1];
            }

            // Find the closest non-enemy target (e.g., block)
            if (!objType.startsWith("enemy") && !objType.equals("upgrade") && dist < TARGET_APPROACH_THRESHOLD && dist > 10) {
                int priority = getPriority(objType);

                if (closestTarget == null) {
                    closestTarget = obj;
                    closestTargetDistance = dist;
                } else {
                    int currentTargetPriority = getPriority(closestTarget.type());
                    if (priority > currentTargetPriority || (priority == currentTargetPriority && dist < closestTargetDistance)) {
                        closestTarget = obj;
                        closestTargetDistance = dist;
                    }
                }
            }
        }

        // If threats exist (bullets, tanks, drones), move away from them
        if (avoidanceX != 0 || avoidanceY != 0) {
            double[] normalized = normalizeMovement(avoidanceX, avoidanceY);
            moveX = normalized[0];
            moveY = normalized[1];
        }
        // Otherwise, approach the closest non-threat target
        else if (closestTarget != null) {
            Point targetPosition = closestTarget.position();
            double targetDistance = distanceBetweenPoints(playerPosition, targetPosition);

            if (targetDistance <= BLOCK_AVOID_DISTANCE) {
                moveX = 0;
                moveY = 0;
            } else {
                moveX = targetPosition.x - playerPosition.x;
                moveY = targetPosition.y - playerPosition.y;

                // Avoid non-target blocks while moving towards the target
                for (Detection obj : detections) {
                    if (obj.type().startsWith("block") && obj != closestTarget) {
                        Point blockPosition = obj.position();
                        double blockDistance = distanceBetweenPoints(playerPosition, blockPosition);

                        // If a block is too close, adjust the movement to avoid it
                        if (blockDistance < BLOCK_AVOID_DISTANCE) {
                            double deltaX = playerPosition.x - blockPosition.x;
                            double deltaY = playerPosition.y - blockPosition.y;
                            double avoidFactor = BLOCK_AVOID_DISTANCE - blockDistance;

                            moveX += deltaX * avoidFactor;
                            moveY += deltaY * avoidFactor;
                        }
                    }
                }

                double[] normalized = normalizeMovement(moveX, moveY);
                moveX = normalized[0];
                moveY = normalized[1];
            }
        }
        // Perform random movement if no target or bullet is detected
        else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRandomMoveTime > RANDOM_MOVE_DURATION || (randomMoveX == 0 && randomMoveY == 0)) {
                randomMoveX = (random.nextDouble() * 2) - 1;
                randomMoveY = (random.nextDouble() * 2) - 1;

                double[] normalized = normalizeMovement(randomMoveX, randomMoveY);
                randomMoveX = normalized[0];
                randomMoveY = normalized[1];
                lastRandomMoveTime = currentTime;
            }
            moveX = randomMoveX;
            moveY = randomMoveY;
        }

        if (closestTarget != null) {
            target = closestTarget.position();
        }

        return new MoveAction(moveX, moveY, target);
    }

    // Avoidance vector calculation for drones, bullets, and tanks
    private static double[] calculateAvoidanceVector(Point playerPosition, Point enemyPosition, double distance, String objType) {
        double deltaX = playerPosition.x - enemyPosition.x;
        double deltaY = playerPosition.y - enemyPosition.y;

        // Avoid bullets with reduced force if far away
        double force;
        if ("enemy_bullet".equals(objType)) {
            if (distance > BULLET_SAFETY_DISTANCE) {
                // Bullets beyond safety distance exert less avoidance force
                force = 0.1 / Math.max(distance, 1);
            } else {
                // Closer bullets exert stronger avoidance force
                force = 1 / Math.max(distance, 1);
            }
        } else {
            // For other enemies (tanks, drones), use normal avoidance
            force = 1 / Math.max(distance, 1);  // Prevent division by zero
        }

        deltaX *= force;
        deltaY *= force;

        return new double[]{deltaX, deltaY};
    }

    private static double[] normalizeMovement(double dx, double dy) {
        double magnitude = Math.sqrt(dx * dx + dy * dy);
        if (magnitude == 0) {
            return new double[]{0, 0};
        }
        return new double[]{dx / magnitude, dy / magnitude};
    }

    private static double distanceBetweenPoints(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public record MoveAction(double moveX, double moveY, Point target) {}

    private static int getPriority(String objType) {
        return switch (objType) {
            case "enemy_tank", "enemy_drone" -> 4;
            case "block_purple" -> 3;
            case "block_red" -> 2;
            case "block_yellow" -> 1;
            default -> 0;
        };
    }
}

