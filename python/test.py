import torch
import torchvision

model = torchvision.models.mobilenet_v2(
    weights=torchvision.models.MobileNet_V2_Weights.IMAGENET1K_V2
)

# The original model is trained on imagenet which has 1000 classes.
# For our image classification scenario, we need to classify among 4 categories.
# So we need to change the last layer of the model to have 4 outputs.
model.classifier[1] = torch.nn.Linear(1280, 4)

# Export the model to ONNX.
model_name = "mobilenetv2"
torch.onnx.export(
    model,
    torch.randn(1, 3, 224, 224),
    f"training_artifacts/{model_name}.onnx",
    input_names=["input"],
    output_names=["output"],
    dynamic_axes={"input": {0: "batch"}, "output": {0: "batch"}},
)

import onnx

# Load the onnx model.
onnx_model = onnx.load(f"training_artifacts/{model_name}.onnx")

# Define the parameters that require their gradients to be computed
# (trainable parameters) and those that do not (frozen/non trainable parameters).
requires_grad = ["classifier.1.weight", "classifier.1.bias"]
frozen_params = [
    param.name
    for param in onnx_model.graph.initializer
    if param.name not in requires_grad
]


from onnxruntime.training import artifacts

# Generate the training artifacts.
artifacts.generate_artifacts(
    onnx_model,
    requires_grad=requires_grad,
    frozen_params=frozen_params,
    loss=artifacts.LossType.CrossEntropyLoss,
    optimizer=artifacts.OptimType.AdamW,
    artifact_directory="training_artifacts",
)
