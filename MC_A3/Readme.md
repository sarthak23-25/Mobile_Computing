# README
## Smartphone Accelerometer Monitoring App

### Overview

This Android application is designed to monitor the accelerometer data of a smartphone in real-time. It features two main functionalities: real-time accelerometer data display and logging of accelerometer data over time into a local database. Additionally, the app allows users to export the collected data for further analysis. Furthermore, it implements a prediction algorithm to forecast future accelerometer values based on historical data.

### Functionality

Real-time Accelerometer Data Display: The app continuously reads accelerometer data to display the orientation of the smartphone in terms of three angles (pitch, roll, and yaw) in real-time.

## Data Logging and Visualization: 
Accelerometer data is logged into a local database, enabling users to visualize the historical accelerometer trends through three graphs.
Data Export: Users can export the collected accelerometer data from the smartphone to their computer for external analysis.
Prediction Algorithm: The app treats the historical accelerometer data as a time series and utilizes it to predict the next 10 seconds of accelerometer values.
Sensing Intervals: Users have the option to change the sensing intervals to observe their impact on prediction accuracy.
Implementation Details
SensorManager Integration: The app utilizes the SensorManager class to collect accelerometer data from the smartphone.
Two Activities: The app consists of two activities - one for real-time accelerometer data display and data logging, and another for visualizing historical data through graphs.
Database Management: SQLite database is employed for local data storage, including the creation of a schema to organize accelerometer data.
Data Export Functionality: Users can export accelerometer data from the smartphone to their computer via a simple export feature.
Prediction Algorithm: A time series analysis approach is implemented to forecast future accelerometer values based on historical data.