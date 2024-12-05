# %%
# %load_ext autoreload
# %autoreload 2
# %%
import torch
import os
from utils.models import FCN2 as Net
from utils.tools import validate, load_arrays_and_labels_from_bin
from utils.dataset import SeizureDataset
from torch.utils.data import DataLoader


# %%
def main():
    device = "cpu"
    checkpoint_dir = "models/"

    data_file = "data/data_20.bin"
    data, labels = load_arrays_and_labels_from_bin(data_file)

    # Instantiate the dataset
    seizure_dataset = SeizureDataset(data=data, labels=labels)

    # Create a DataLoader
    seizure_dataloader = DataLoader(
        seizure_dataset, batch_size=32, shuffle=False
    )

    model_path = os.path.join(checkpoint_dir, "base_pat_02.pth")
    model = Net(in_channels=18)
    model.to(device)
    model.load_state_dict(
        torch.load(model_path, map_location=torch.device("cpu"))["state_dict"]
    )


    # **Export the model to ONNX**
    # Create a dummy input tensor with batch size 1
    dummy_input = torch.randn(1, 18, 1024, device=device)
    # Define the path where you want to save the ONNX model
    onnx_model_path = "models/base_pat_02.onnx"
    # Export the model
    torch.onnx.export(
        model,                       # The model being exported
        dummy_input,                 # An example input tensor
        onnx_model_path,             # Where to save the ONNX model
        input_names=["input"],       # The model's input names
        output_names=["output"],     # The model's output names
        dynamic_axes={
            "input": {0: "batch_size"},    # Variable batch size for input
            "output": {0: "batch_size"}    # Variable batch size for output
        },
        opset_version=11             # ONNX opset version (adjust if necessary)
    )

    print(f"Testing the model")
    

    f1_score, metrics = validate(
        seizure_dataloader,
        model,
        device=device,
    )

    print(
        f"F1 = {f1_score:.4f},"
        f"Precision = {metrics['precision']:.4f}, "
        f"Recall = {metrics['recall']:.4f}, FPR = {metrics['fpr']:.4f}"
    )


# %%
if __name__ == "__main__":
    main()
# %%
