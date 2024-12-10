# %%
# %load_ext autoreload
# %autoreload 2
# %%
import torch
import os
from utils.models import FCN2 as Net
from utils.tools import validate, load_arrays_and_labels_from_bin, train
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

    # Split the dataset into training and validation sets
    seizure_train, seizure_val = torch.utils.data.random_split(
        seizure_dataset,
        [
            int(0.8 * len(seizure_dataset)),
            len(seizure_dataset) - int(0.8 * len(seizure_dataset)),
        ],
    )

    train_loader = DataLoader(seizure_train, batch_size=32, shuffle=True)
    val_loader = DataLoader(seizure_val, batch_size=32, shuffle=False)

    model_path = os.path.join(checkpoint_dir, "base_pat_02.pth")
    model = Net(in_channels=18)
    model.to(device)
    model.load_state_dict(
        torch.load(model_path, map_location=torch.device("cpu"))["state_dict"]
    )

    # # Training the model
    # train(
    #     train_loader, val_loader, model, device=device, epochs=20, patience=7
    # )

    print(f"Testing the model")
    test_file = "data/data_21.bin"
    data, labels = load_arrays_and_labels_from_bin(test_file)

    # Instantiate the dataset
    seizure_dataset = SeizureDataset(data=data, labels=labels)
    test_loader = DataLoader(seizure_dataset, batch_size=32, shuffle=False)

    # Loading the best model
    model.load_state_dict(
        torch.load("models/base_pat_02.pth", map_location=torch.device("cpu"))[
            "state_dict"
        ]
    )
    f1_score, metrics = validate(
        test_loader,
        model,
        device=device,
    )

    print(
        f"F1 = {f1_score:.4f},"
        f"Precision = {metrics['precision']:.4f}, "
        f"Recall = {metrics['recall']:.4f}, FPR = {metrics['fpr']:.4f}"
    )

    model.load_state_dict(
        torch.load("models/best_model.pth", map_location=torch.device("cpu"))[
            "state_dict"
        ]
    )
    f1_score, metrics = validate(
        test_loader,
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
