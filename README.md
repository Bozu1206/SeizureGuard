# Simple Inference APP
 
- In the `APP` folder, there is a simple example on how to perform inference with the seizure detection model with Kotlin. The app has a button thats starts the inference coroutine when pressed, and the validation  metrics get prined on the screen as soon as the inference finishes. Also, all the outputs of the model get printed in the logcat.
- The model gets converted to ONNX format in `inference.py` and saved in `models/pase_pat_02.onnx`.
- `test_inference_output.py` logs the model outputs and prints the metrics for comparing the output with the kotlin version.
- `test_onnx_model.py` tests the validity of the ONNX model