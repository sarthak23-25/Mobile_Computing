# README
## Image Classification Program with Native Neural Networks API
### Overview
This program addresses the lack of native support for running neural networks in Kotlin on Android by leveraging Android's native neural networks API for image classification tasks. The program allows users to collect a set of images and employs a convolutional neural network (CNN) to classify them into predefined categories.

### Functionality
Native Neural Networks API Integration: The program utilizes Android's native neural networks API to perform image classification tasks efficiently.
Image Collection: Users can load images into the program, either from the device's camera or from existing files.
Convolutional Neural Network Classification: The program runs a convolutional neural network model to classify the loaded images into predefined categories.
Prediction: The CNN model predicts the categories of the loaded images, providing proper output without requiring extremely high accuracy.

### Implementation Details
Native Code Integration: The program incorporates native code to utilize Android's native neural networks API for efficient image classification.
Image Input: Users have the option to input images either from the device's camera or from existing files.
Convolutional Neural Network Model: A CNN model is trained and deployed for image classification tasks.
Activity for Image Loading: The program features an activity dedicated to loading images, providing a user-friendly interface.