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
