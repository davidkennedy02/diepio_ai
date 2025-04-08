# Diep.io AI Bot

This project is an automated bot for playing [diep.io](https://diep.io), a free multiplayer online game where players control tanks and eliminate obstacles and other players.

## Game Overview

Diep.io is a geometric browser game where players control tanks in an arena. The goal is to gain experience points by destroying shapes and other players, which allows you to level up and upgrade your tank. As you progress, you can choose different tank classes and enhance various attributes like health, damage, and movement speed.

## Project Description

This bot uses computer vision (via OpenCV) to analyze the game screen and employs a rule-based AI system to:
- Detect and track the player's tank
- Identify shapes (blocks) of different colors and values
- Recognize enemy tanks and projectiles
- Identify upgrade opportunities
- Make movement decisions based on detected objects
- Automatically upgrade the tank with weighted priorities

## How It Works

The system operates through several key components:
1. **Screen Capture**: Captures the game screen in real-time
2. **Object Detection**: Uses computer vision to detect game elements by color and shape
3. **Decision Making**: Applies rule-based logic to determine optimal actions
4. **Input Automation**: Controls the game through programmatic keyboard and mouse inputs

## Limitations and Shortcomings

As my first attempt at computer vision and rule-based AI, this project has several limitations:

- **Detection Reliability**: The object detection isn't always accurate, especially in crowded game scenarios
- **Performance Impact**: The screen capture and processing can cause lag on some systems
- **Limited Strategy**: The rule-based approach lacks the sophistication of more advanced AI techniques
- **Upgrade Path**: The tank upgrade decisions follow fixed probabilities rather than adapting to game situations
- **Collision Handling**: Sometimes struggles with proper collision avoidance
- **Game Changes**: Updates to the diep.io game may break detection mechanisms

## Technologies Used

- Java
- OpenCV for computer vision
- AWT Robot for input simulation

## Requirements

- Java Development Kit (JDK)
- OpenCV library
- A system capable of running diep.io in a browser window

## Usage Notes

This project is intended as an educational exploration of computer vision and rule-based AI systems rather than a competitive game bot. Use responsibly and in accordance with the game's terms of service.
