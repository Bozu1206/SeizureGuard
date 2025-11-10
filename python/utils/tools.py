import numpy as np
import torch
import os
from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
)
from torch.utils.data import DataLoader


def compute_metrics(true_labels, pred_labels):
    """
    Compute evaluation metrics such as Precision, Recall, F1-Score, and False Positive Rate (FPR).
    """
    # Calculate Precision, Recall, and F1-Score
    precision = precision_score(true_labels, pred_labels, zero_division=0)
    recall = recall_score(true_labels, pred_labels, zero_division=0)
    f1 = f1_score(true_labels, pred_labels, zero_division=0)

    # Compute confusion matrix
    tn, fp, fn, tp = confusion_matrix(true_labels, pred_labels).ravel()

    # Calculate False Positive Rate (FPR)
    if fp + tn > 0:
        fpr = fp / (fp + tn)
    else:
        fpr = 0.0  # Handle the case where there are no true negatives or false positives

    # Return metrics, with f1 as the primary metric for evaluation
    return {
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "fpr": fpr,  # False Positive Rate
    }


def train(train_loader, val_loader, model, device, epochs, patience=5):
    """
    Train the model on the training dataset.

    Args:
        train_loader (DataLoader): DataLoader for the training dataset.
        model (torch.nn.Module): Model to be trained.
        device: Device to train on (e.g. 'cpu' or 'cuda').

    Returns:
        torch.nn.Module: Trained model.
    """
    criterion = torch.nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=1e-4)
    prev_f1 = 0.0
    for i in range(epochs):
        train_loss = train_one_epoch(
            train_loader, model, criterion, optimizer, device
        )
        val_f1, metrics = validate(val_loader, model, device)
        print(
            f"Epoch {i+1}, Train Loss = {train_loss:.6f}, "
            f"Validation F1 = {val_f1:.4f}, "
            f"Precision = {metrics['precision']:.4f}, "
            f"Recall = {metrics['recall']:.4f}, FPR = {metrics['fpr']:.4f}"
        )
        if prev_f1 < val_f1:
            prev_f1 = val_f1
            patience = 5
            print(f"reset: {patience}")
        patience -= 1
        if prev_f1 == val_f1:
            torch.save(
                {
                    "state_dict": model.state_dict(),
                    "optimizer": optimizer.state_dict(),
                },
                f"models/best_model.pth",
            )
            print(f"saving,  {patience}")

        if patience == 0:
            break


def train_one_epoch(train_loader, model, criterion, optimizer, device):
    """
    Train the model for one epoch on the training dataset.

    Args:
        train_loader (DataLoader): DataLoader for the training dataset.
        model (torch.nn.Module): Model to be trained.
        criterion: Loss function.
        optimizer: Optimizer for the model.
        device: Device to train on (e.g. 'cpu' or 'cuda').

    Returns:
        float: Average loss for the epoch.
    """
    model.train()
    train_loss = 0.0

    for data, target in train_loader:
        data, target = data.to(device), target.to(device)
        optimizer.zero_grad()
        output = model(data)
        loss = criterion(output, target)
        loss.backward()
        optimizer.step()
        train_loss += loss.item()

    return train_loss / len(train_loader)


def validate(val_loader, model, device):
    """
    Validate the model on the validation dataset, returning F1 score and additional metrics.

    Args:
        val_loader (DataLoader): DataLoader for the validation dataset.
        model (torch.nn.Module): Model to be validated.
        device: Device to validate on (e.g. 'cpu' or 'cuda').

    Returns:
        tuple: Validation loss, F1 score, and a dictionary of other metrics.
    """
    model.eval()
    all_preds = []
    all_targets = []

    with torch.no_grad():
        for data, target in val_loader:
            data, target = data.to(device), target.to(device)
            output = model(data)

            preds = output.argmax(dim=1)
            all_preds.append(preds.cpu().numpy())
            all_targets.append(target.cpu().numpy())

    all_preds = np.concatenate(all_preds)
    all_targets = np.concatenate(all_targets)

    # Compute the metrics (F1, precision, recall, FPR)
    metrics = compute_metrics(all_targets, all_preds)
    val_f1 = metrics["f1"]

    return val_f1, metrics


import numpy as np


def save_arrays_and_labels_to_bin(array_list, labels_list, filename):
    """
    Saves a list of NumPy arrays and corresponding labels to a binary file with a header.

    Parameters:
    - array_list: List of NumPy arrays, each array should have the same shape and dtype.
    - labels_list: List or NumPy array of labels corresponding to each array in array_list.
    - filename: Name of the binary file to save the data to.
    """
    # Check that array_list and labels_list are not empty and have the same length
    if len(array_list) != len(labels_list):
        raise ValueError(
            "array_list and labels_list must have the same length"
        )

    # Verify that all arrays have the same shape and dtype
    first_shape = array_list[0].shape
    first_dtype = array_list[0].dtype
    for idx, arr in enumerate(array_list):
        if arr.shape != first_shape:
            raise ValueError(
                f"All arrays must have the same shape. Array at index {idx} has shape {arr.shape}, expected {first_shape}"
            )
        if arr.dtype != first_dtype:
            raise ValueError(
                f"All arrays must have the same dtype. Array at index {idx} has dtype {arr.dtype}, expected {first_dtype}"
            )

    # Convert array_list to a single NumPy array
    data = np.stack(array_list)  # Shape will be (num_arrays, dim1, dim2)

    # Convert labels_list to a NumPy array
    labels = np.array(labels_list, dtype=np.int32)

    # Get the dimensions
    num_arrays, dim1, dim2 = data.shape

    # Prepare header: [num_arrays, dim1, dim2, labels_present (1 for True)], stored as int32
    header = np.array(
        [num_arrays, dim1, dim2, 1], dtype=np.int32
    )  # The '1' indicates labels are present

    # Open file in binary write mode
    with open(filename, "wb") as f:
        # Write header
        header.tofile(f)
        # Write data
        data.tofile(f)
        # Write labels
        labels.tofile(f)

    print(f"Data and labels saved to {filename}")


def load_arrays_and_labels_from_bin(filename):
    """
    Loads data and labels from a binary file with a header.

    Parameters:
    - filename: Name of the binary file to read the data from.

    Returns:
    - data: NumPy array of shape (num_arrays, dim1, dim2)
    - labels: NumPy array of shape (num_arrays,) containing the labels
    """
    with open(filename, "rb") as f:
        # Read header
        header = np.fromfile(f, dtype=np.int32, count=4)
        if len(header) < 4:
            raise ValueError("Header is incomplete or file is corrupted.")
        num_arrays, dim1, dim2, labels_present = header

        # Read data
        num_floats = num_arrays * dim1 * dim2
        data = np.fromfile(f, dtype="<f4", count=num_floats)
        if data.size < num_floats:
            raise ValueError("Data is incomplete or file is corrupted.")
        data = data.reshape((num_arrays, dim1, dim2))

        # Read labels if present
        if labels_present == 1:
            labels = np.fromfile(f, dtype="<i4", count=num_arrays)
            if labels.size < num_arrays:
                raise ValueError("Labels are incomplete or file is corrupted.")
        else:
            labels = None  # Or set default labels if necessary

    return data, labels


def load_model(model_class, model_path, device="cpu", in_channels=18):
    """
    Load a PyTorch model from a checkpoint file.
    
    Args:
        model_class: The model class to instantiate
        model_path (str): Path to the model checkpoint (.pth file)
        device (str): Device to load the model on ('cpu' or 'cuda')
        in_channels (int): Number of input channels for the model
        
    Returns:
        torch.nn.Module: Loaded model
    """
    model = model_class(in_channels=in_channels)
    model.to(device)
    model.load_state_dict(
        torch.load(model_path, map_location=torch.device(device))["state_dict"]
    )
    return model


def load_data_and_create_loader(data_file, batch_size=32, shuffle=False):
    """
    Load data from a binary file and create a DataLoader.
    
    Args:
        data_file (str): Path to the binary data file
        batch_size (int): Batch size for the DataLoader
        shuffle (bool): Whether to shuffle the data
        
    Returns:
        tuple: (DataLoader, data, labels)
    """
    from utils.dataset import SeizureDataset
    
    data, labels = load_arrays_and_labels_from_bin(data_file)
    seizure_dataset = SeizureDataset(data=data, labels=labels)
    dataloader = DataLoader(seizure_dataset, batch_size=batch_size, shuffle=shuffle)
    
    return dataloader, data, labels


def print_metrics(f1_score, metrics, prefix=""):
    """
    Print evaluation metrics in a consistent format.
    
    Args:
        f1_score (float): F1 score
        metrics (dict): Dictionary containing precision, recall, and fpr
        prefix (str): Optional prefix for the output
    """
    print(
        f"{prefix}F1 = {f1_score:.4f}, "
        f"Precision = {metrics['precision']:.4f}, "
        f"Recall = {metrics['recall']:.4f}, FPR = {metrics['fpr']:.4f}"
    )


def export_model_to_onnx(model, onnx_path, device="cpu", input_shape=(1, 18, 1024), opset_version=11):
    """
    Export a PyTorch model to ONNX format.
    
    Args:
        model: PyTorch model to export
        onnx_path (str): Path where the ONNX model will be saved
        device (str): Device the model is on
        input_shape (tuple): Shape of the input tensor
        opset_version (int): ONNX opset version
    """
    dummy_input = torch.randn(*input_shape, device=device)
    torch.onnx.export(
        model,
        dummy_input,
        onnx_path,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={
            "input": {0: "batch_size"},
            "output": {0: "batch_size"}
        },
        opset_version=opset_version,
    )
    print(f"Model exported to {onnx_path}")
