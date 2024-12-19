import onnx

# Load your ONNX model
model = onnx.load('models/base_pat_02.onnx')

# Check the model for consistency
onnx.checker.check_model(model)
print("The ONNX model is valid!")