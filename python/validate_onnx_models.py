import numpy as np
from torch.utils.data import DataLoader
from utils.dataset import SeizureDataset
from utils.tools import load_arrays_and_labels_from_bin
import onnxruntime.training.api as orttraining

from onnxruntime import InferenceSession
from onnxruntime.capi import _pybind_state as C

from utils.tools import compute_metrics

def main():
    data_file = "data/data_20.bin"
    data, labels = load_arrays_and_labels_from_bin(data_file)

    seizure_dataset = SeizureDataset(data=data, labels=labels)
    train_loader = DataLoader(seizure_dataset, batch_size=32, shuffle=True)

    # Instantiate the training session by defining the checkpoint state, module, and optimizer
    # The checkpoint state contains the state of the model parameters at any given time.
    checkpoint_state = orttraining.CheckpointState.load_checkpoint(
        "training_artifacts/checkpoint"
    )

    model = orttraining.Module(
        "training_artifacts/training_model.onnx",
        checkpoint_state,
        "training_artifacts/eval_model.onnx",
    )

    optimizer = orttraining.Optimizer("training_artifacts/optimizer_model.onnx", model)

    print(f"Fine-tuning on {data_file}")
    for epoch in range(0):
        model.train()
        loss = 0
        for data, target in train_loader:
            data = data.numpy()
            target = target.numpy()
            loss += model(data, target)
            optimizer.step()
            model.lazy_reset_grad()

        print(f"Epoch {epoch}: Loss: {loss}")

    model.export_model_for_inferencing("inference_artifacts/inference.onnx", ["output"])

    session = InferenceSession("inference_artifacts/inference.onnx", providers=C.get_available_providers())

    test_file = "data/data_21.bin"
    data, labels = load_arrays_and_labels_from_bin(test_file)

    seizure_dataset = SeizureDataset(data=data, labels=labels)
    test_loader = DataLoader(seizure_dataset, batch_size=32, shuffle=False)
    
    all_preds = []
    all_targets = []
    print(f"Running inference on {test_file}")
    for sample, target in test_loader:
        sample = sample.numpy()
        target = target.numpy()
        logits = session.run(["output"], {"input": sample})[0]

        preds = np.argmax(logits, axis=1)
        all_preds.append(preds)
        all_targets.append(target)

    all_preds = np.concatenate(all_preds)
    all_targets = np.concatenate(all_targets)

    metrics = compute_metrics(all_targets, all_preds)
    val_f1 = metrics["f1"]

    print(f"F1 = {val_f1:.4f}, Precision = {metrics['precision']:.4f}, Recall = {metrics['recall']:.4f}, FPR = {metrics['fpr']:.4f}")


if __name__ == "__main__":
    main()
