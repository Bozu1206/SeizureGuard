# %%
# %load_ext autoreload
# %autoreload 2
# %%
import torch
import os
from utils.models import FCN2 as Net


import onnx
from onnxruntime.training import artifacts


def main():
    device = "cpu"
    checkpoint_dir = "models/"
    model_path = os.path.join(checkpoint_dir, "base_pat_02.pth")
    model = Net(in_channels=18)
    model.load_state_dict(
        torch.load(model_path, map_location=torch.device("cpu"))["state_dict"]
    )

    # **Export the model to ONNX**
    dummy_input = torch.randn(1, 18, 1024, device=device)
    onnx_model_path = "training_artifacts/base_pat_02.onnx"
    torch.onnx.export(
        model,
        dummy_input,
        onnx_model_path,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={
            "input": {0: "batch_size"},
            "output": {0: "batch_size"},
        },
        export_params=True,
        do_constant_folding=True,
        verbose=True,
        verify=True
    )

    onnx_model = onnx.load(f"training_artifacts/base_pat_02.onnx")
    for param in onnx_model.graph.initializer:
        print(param.name)
    
    requires_grad = [
        "classifier.0.weight",
        "classifier.0.bias",
        "classifier.1.weight",
        "classifier.1.bias",
        # "onnx::Conv_57",
        # "onnx::Conv_58",
        # "onnx::Conv_60",
        # "onnx::Conv_61",
        # "onnx::Conv_63",
        # "onnx::Conv_64",
    ]

    frozen_params = [
        param.name
        for param in onnx_model.graph.initializer
        if param.name not in requires_grad
    ]

    print(frozen_params)

    artifacts.generate_artifacts(
        onnx_model,
        requires_grad=requires_grad,
        frozen_params=frozen_params,
        loss=artifacts.LossType.CrossEntropyLoss,
        optimizer=artifacts.OptimType.AdamW,
        artifact_directory="training_artifacts",
    )


main()
