# Project 7.A - SeizureGuard

- **Course**: Lab on App Development for Tablets and Smartphones (EPFL)
- **Authors**: Francesco Poluzzi, François Dumoncel


## Testing the Application

This Android application aims to perform live AI-based epileptic seizure detection.

> **Note**
> If you want use the app without giving personal informations, you can use our demo account. Just log in with the following credentials:
> - email: demo@epfl.ch
> - password: epfl


### Steps for Using the App
1. Install the SeizureGuard app, accept all permissions, and create a profile. 
2. Install the "EEG app Android" app on another Android device or the "EEG app Nordic Board" app on a Nordic nRF5340-DK board to simulate the EEG sensor. If not possible, navigate to settings, set debug mode to true, and restart the app. This will read artificial EEG data from memory instead of taking it from a BLE sensor. 
3. In the app, navigate to "Monitor" and press "Look for Devices". When the device is connected, the button turns green, and inference can be started. Since inference runs in a foreground service, you can close the app, and the inference will continue working. 
4. To perform training, press "Train Model" when the number of samples collected reaches 100. Accuracy and F1 score values will update automatically. 
5. When a seizure is detected, a high-importance notification will be triggered. Click on it to open a page with shortcuts for emergency calls, some guidelines, and the possibility to save the seizure event.
6. On the "Profile" page, you can modify the profile fields. 
7. On the History page, you can see past seizures and some stats.
8. On another device logged into the same profile, you can set Parent Mode from settings to receive notifications of seizure events. Click on these notifications to open the location where the seizure happened.
