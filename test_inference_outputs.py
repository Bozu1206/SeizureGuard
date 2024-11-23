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

    data_file = "data/data.bin"
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

    log_file_path = './logs/outputs.log'
    os.makedirs(os.path.dirname(log_file_path), exist_ok=True)
    with open(log_file_path, 'w') as f:
        f.write("Model predictions:\n")

    # **Add the inference and output printing code here**
    # Set the model to evaluation mode
    model.eval()

    # Disable gradient calculation
    with torch.no_grad():
        # Iterate over the DataLoader
        batches = 0
        for data_batch, labels_batch in seizure_dataloader:
            # Move data to the device
            data_batch = data_batch.to(device)
            labels_batch = labels_batch.to(device)
            
            # Perform inference
            outputs = model(data_batch)
            
            # Convert outputs to NumPy arrays
            outputs_np = outputs.cpu().numpy()
            
            # Log the outputs to file
            log_model_outputs(outputs_np)
            
            # If you only want to process one batch, uncomment the following line
            batches += 1
            # if batches == 3:
                # break

    print(f"Testing the model")

    # Continue with your validation/testing code
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

def log_model_outputs(predictions, log_file_path='./logs/outputs.log'):
    # Ensure the logs directory exists
    os.makedirs(os.path.dirname(log_file_path), exist_ok=True)
    
    # Handle different shapes of predictions
    if len(predictions.shape) == 1:
        # 1D array, ensure size is divisible by 2
        if len(predictions) % 2 != 0:
            print("Predictions size is not divisible by 2")
            return
        num_rows = len(predictions) // 2
        output_lines = []
        output_lines.append("[\n")
        for i in range(num_rows):
            value1 = predictions[i * 2]
            value2 = predictions[i * 2 + 1]
            formatted_value1 = "{:10.6f}".format(value1)
            formatted_value2 = "{:10.6f}".format(value2)
            row_string = " [ {} {}]".format(formatted_value1, formatted_value2)
            output_lines.append(row_string)
            if i < num_rows - 1:
                output_lines.append("\n")
        output_lines.append("\n]")
        output = ''.join(output_lines)
    elif len(predictions.shape) == 2:
        # 2D array
        output_lines = []
        output_lines.append("[\n")
        for row_index, row in enumerate(predictions):
            formatted_values = ' '.join("{:10.6f}".format(value) for value in row)
            row_string = " [ {}]".format(formatted_values)
            output_lines.append(row_string)
            if row_index < predictions.shape[0] - 1:
                output_lines.append("\n")
        output_lines.append("\n]")
        output = ''.join(output_lines)
    else:
        print("Predictions have an unsupported number of dimensions.")
        return

    # Append the output to the log file
    with open(log_file_path, 'a') as f:
        f.write(output)
        f.write("\n")  # Add a newline for separation between batches

# %%
if __name__ == "__main__":
    main()
# %%
