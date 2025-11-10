# %%
# %load_ext autoreload
# %autoreload 2
# %%
import torch
import os
from utils.models import FCN2 as Net
from utils.tools import validate, load_model, load_data_and_create_loader, print_metrics, export_model_to_onnx


# %%
def main():
    device = "cpu"
    checkpoint_dir = "models/"

    data_file = "data/data_20.bin"
    # Use utility function to load data
    seizure_dataloader, _, _ = load_data_and_create_loader(data_file, batch_size=32, shuffle=False)

    # Use utility function to load model
    model_path = os.path.join(checkpoint_dir, "base_pat_02.pth")
    model = load_model(Net, model_path, device=device, in_channels=18)

    model.train()
    # **Export the model to ONNX**
    onnx_model_path = "models/base_pat_02-new.onnx"
    export_model_to_onnx(model, onnx_model_path, device=device, input_shape=(1, 18, 1024), opset_version=11)

    print(f"Testing the model")
    
    f1_score, metrics = validate(seizure_dataloader, model, device=device)
    print_metrics(f1_score, metrics)


# %%
if __name__ == "__main__":
    main()
# %%
