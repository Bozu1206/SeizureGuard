<p align="left">
  <img src="features.png" alt="SeizureGuard Logo" title="SeizureGuard">
</p>

# SeizureGuard

> [!Warning]
> This is a student project that is not longer maintained. You may encounter weird behavior / bugs. Keep your expectations low.


SeizureGuard is an open-source Android application that leverages on-device machine learning to detect seizures in real time. Built using the ONNX framework, this native app is designed not only as a proof of concept but also as a foundation for future development in medical monitoring and emergency response systems.

Developed during Fall 2024 at EPFL as part of the course *"Lab on App Development for Tablets and Smartphones"*, SeizureGuard demonstrates how advanced algorithms can be deployed directly on mobile devices for critical health applications.


## Key Features

- **Real-Time Seizure Detection:** Continuously monitors EEG signals and triggers immediate alerts when a seizure is detected.
- **On-Device Inference and Training:** Uses the ONNX framework to both deploy and refine the machine learning model without relying on cloud services.
- **Emergency Notifications:** Provides high-priority alerts that include emergency call shortcuts, safety guidelines, and seizure event logging.
- **Cross-Device Alerts:** Supports a Parent Mode that sends notifications to secondary devices, allowing caregivers to respond promptly.
- **Flexible Sensor Integration:** Easily integrates with companion EEG sensor apps or utilizes artificial data in debug mode for testing purposes.


## Getting Started

### Prerequisites

- **Android Studio:** Latest version recommended.
- **Android Device:** A smartphone or tablet with BLE (Bluetooth Low Energy) support.
- **EEG Sensor or Simulator:** Use the companion app *"EEG app Android"* on another device, or the *"EEG app Nordic Board"* on a Nordic nRF5340-DK board. Alternatively, enable debug mode for simulated EEG data.

### Installation

1. **Clone the Repository:**
   - Open a terminal and run:
     ```
     git clone https://github.com/yourusername/seizureguard.git
     cd seizureguard
     ```

2. **Open the Project in Android Studio:**
   - Launch Android Studio.
   - Open the project folder you just cloned.

3. **Configure API Keys:**
   - Navigate to the `APP` folder.
   - Create a file named `local.properties` if it doesn't exist.
   - Add your API keys in the following format:
     ```
     GOOGLE_WALLET_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----your_google_wallet_private_key_here-----END PRIVATE KEY-----
     MAPS_API_KEY=your_google_maps_api_key_here
     ```
   - **Note:** This file is excluded from version control (check `.gitignore`) to ensure your keys remain secure.

4. **Build the Project:**
   - Rebuild the project in Android Studio to apply the configuration changes.


## Using SeizureGuard

### Demo Account

> [!NOTE]  
> For those who wish to test the app without providing personal information, a demo account is available:
> - **Email:** demo@epfl.ch
> - **Password:** epfl

### Step-by-Step Usage

1. **Profile Setup:** Install the SeizureGuard app, accept all permissions, and create your user profile.
2. **Sensor Integration:**
   - Install the companion *"EEG app Android"* on a secondary device or the *"EEG app Nordic Board"* for sensor simulation.
   - Alternatively, enable debug mode in settings to simulate EEG data.
3. **Device Connection:**
   - Navigate to the **Monitor** screen and tap **"Look for Devices."**
   - Once a device connects, the indicator turns green, confirming a successful connection.
4. **Real-Time Inference:**
   - The app’s inference service runs in the background even if the main interface is closed.
5. **Model Training:**
   - After 100 samples are collected, tap **"Train Model"** to update the machine learning parameters. Accuracy and F1 score metrics will update automatically.
6. **Seizure Detection & Alerts:**
   - When a seizure is detected, a high-priority notification will appear.
   - Tap the notification to view emergency options, guidelines, and log the event.
7. **Profile & History:**
   - Edit your profile information on the **Profile** page.
   - View historical data and statistics on the **History** page.
8. **Parent Mode:**
   - Log in on another device using the same profile and activate Parent Mode to receive real-time notifications and location data for seizure events.


## API Key Setup

SeizureGuard requires API keys to function correctly. To set up your environment:

1. **Create `local.properties`:**
   - In the `APP` folder, create the file if it does not exist.
2. **Add Your API Keys:**
   - Insert the following lines, replacing the placeholder text with your actual keys:
     ```
     GOOGLE_WALLET_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----your_google_wallet_private_key_here-----END PRIVATE KEY-----
     MAPS_API_KEY=your_google_maps_api_key_here
     ```
3. **Rebuild the Project:**
   - Rebuild the project in Android Studio to apply these changes.

For more details on obtaining these keys, see:
- [Google Wallet API Guide](https://developers.google.com/wallet)
- [Google Maps API Getting Started](https://developers.google.com/maps/gmp-get-started)


## Contributing

We welcome contributions from developers, researchers, and enthusiasts! To contribute:

1. **Fork the Repository:** Create your own copy of the project.
2. **Create a Branch:** Develop your feature or bug fix on a new branch.
3. **Submit a Pull Request:** Once your changes are ready, open a pull request for review.

For further details, please refer to our [CONTRIBUTING.md](CONTRIBUTING.md).


## Authors & Acknowledgements

- **Authors:**
  - [Francesco Poluzzi](https://people.epfl.ch/francesco.poluzzi)
  - [François Dumoncel](https://people.epfl.ch/francois.dumoncel-kessler)
- **Supervisor:**
  - [Amirhossein Shahbazinia](https://people.epfl.ch/amirhossein.shahbazinia)

SeizureGuard was developed as a collaborative project at EPFL. The machine learning model and supporting data were provided by [Amirhossein Shahbazinia](https://people.epfl.ch/amirhossein.shahbazinia). For further inquiries or collaboration opportunities, please contact the project maintainers.

## License

This project is licensed under the [MIT License](LICENSE). Please see the LICENSE file for more details.

## Additional Resources

- **Project Slides:** For more detailed information, view the [presentation slides](./slides/).

## Contact

For questions, feature requests, or bug reports, please open an issue on GitHub or contact one of the maintainers directly.
