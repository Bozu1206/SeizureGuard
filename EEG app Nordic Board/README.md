# EEG App for Nordic nRF5340DK

This project is a BLE-based EEG sensor simulation for use with the SeizureGuard application. It runs on the [Nordic Semiconductor nRF5340 DK](https://www.nordicsemi.com/Products/Development-hardware/nRF5340-DK) and connects via Bluetooth Low Energy (BLE) to the SeizureGuard app in the main repository to transmit EEG-like data.

## Requirements

- **Hardware**:
  - [Nordic Semiconductor nRF5340 DK](https://www.nordicsemi.com/Products/Development-hardware/nRF5340-DK)
- **Software & Tools**:
  - Nordic nRF Connect SDK
  - VS Code with Nordic Extensions

## Project Structure

- **`src/main.c`** – Main application source code for BLE communication with the SeizureGuard app
- **`src/eeg_data.h`** – Header file containing EEG sample data to be sent over BLE
- **`header_gen.py`** – Python script for generating EEG data header file (modifiable for different datasets)

## Building and Flashing the Application

1. **Set up the nRF Connect SDK**:

   - Follow the official [installation guide](https://developer.nordicsemi.com/nRF_Connect_SDK/doc/latest/nrf/installation/installing.html).

2. **Open the Project in VS Code**:

   - Use the [VS Code Nordic SDK extensions](https://www.nordicsemi.com/Products/Development-tools/nRF-Connect-for-VS-Code) to import and configure the project.

3. **Build the Project**:

   - Follow the official [build guide](https://docs.nordicsemi.com/bundle/ncs-latest/page/nrf/app_dev/config_and_build/building.html) to configure the build for the nRF5340 DK and compile using the nRF SDK.

4. **Flash the Firmware**:
- Use `west flash` to program the firmware onto the board:
```bash
west flash
```
- Make sure the board is connected via USB and properly detected.
- For more details, refer to the [west flash documentation](https://docs.nordicsemi.com/bundle/ncs-1.2.0/page/zephyr/guides/west/build-flash-debug.html).

## Modifying EEG Data

The `header_gen.py` script can be used to modify the EEG data that is transmitted over BLE:

```bash
python header_gen.py
```

This script generates a new `eeg_data.h` file that can be compiled into the application.

## License

This project follows the same licensing as the main **SeizureGuard** repository.

For more details, visit the [SeizureGuard main repository](https://github.com/Bozu1206/SeizureGuard).

