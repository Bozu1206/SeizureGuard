# %%
# %load_ext autoreload
# %autoreload 2
# %%
import torch
import os
from utils.models import FCN2 as Net
from utils.tools import validate, load_model, load_data_and_create_loader, print_metrics, train
from torch.utils.data import DataLoader


# %%
def main():
    device = "cpu"
    checkpoint_dir = "models/"

    data_file = "data/data_20.bin"
    # Use utility function to load data and create dataset
    full_loader, data, labels = load_data_and_create_loader(data_file, batch_size=32, shuffle=False)
    
    from utils.dataset import SeizureDataset
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

    # Use utility function to load model
    model_path = os.path.join(checkpoint_dir, "base_pat_02.pth")
    model = load_model(Net, model_path, device=device, in_channels=18)

    # # Training the model
    train(
        train_loader, val_loader, model, device=device, epochs=20, patience=7
    )

    print(f"Testing the model")
    test_file = "data/data_21.bin"
    # Use utility function to load test data
    test_loader, _, _ = load_data_and_create_loader(test_file, batch_size=32, shuffle=False)

    # Loading the best model - base_pat_02
    model = load_model(Net, "models/base_pat_02.pth", device=device, in_channels=18)
    f1_score, metrics = validate(test_loader, model, device=device)
    print_metrics(f1_score, metrics)

    # Loading the best model - best_model
    model = load_model(Net, "models/best_model.pth", device=device, in_channels=18)
    f1_score, metrics = validate(test_loader, model, device=device)
    print_metrics(f1_score, metrics)


# %%
if __name__ == "__main__":
    main()
# %%
