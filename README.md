# Parsdroid - Parsley Telemetry Dashboard on Android

Parsdroid is an Android application designed for real-time monitoring of live telemetry data received via USB Serial.

## Features

- **Automatic USB Connectivity**: Automatically detects and connects to USB debug when plugged in.
- **Integrated Parsley Parsing**: Uses [Chaquopy](https://chaquo.com/chaquopy/) to run [Parsley](https://github.com/waterloo-rocketry/parsley)
- **Live Telemetry Dashboard**:
    - **GPS Tracking**: Real-time Latitude, Longitude, and Satellite count.
    - **Power Monitoring**: Battery Voltage and Current draw from the POWER board.
    - **Signal Quality**: Live RSSI (Signal Strength) monitoring.
    - **Message Log**: A scrolling, terminal-like log of all incoming raw messages with system timestamps.
- **User Conveniences**:
    - **Copy to Clipboard**: Tap any telemetry card to copy its value.
    - **Keep Screen On**: Toggle switch to prevent the device from sleeping during a launch.

## System Architecture

1.  **USB Layer**: Uses `usb-serial-for-android` to handle low-level serial communication.
2.  **Service Layer**: A Foreground Service (`UsbSerialService`) manages the serial connection and buffers incoming data into complete lines.
3.  **Processing Layer**: Lines are passed to a Python script (`processor.py`) which uses Parsley to parse USB debug message into JSON
4.  **UI Layer**: A state-driven Dashboard built with Jetpack Compose and Material 3.

## Build Requirements
[rustup](https://github.com/rust-lang/rustup) to cross compile pydantic-core, which is required by parsley.

## Dashboard Preview

![Dashboard Preview](https://github.com/waterloo-rocketry/parsdroid/raw/main/docs/dashboard_preview.png)
