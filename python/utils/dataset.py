import torch
from torch.utils.data import Dataset


class SeizureDataset(Dataset):
    """
    Custom Dataset for seizure data and labels.
    Expects data of shape (num_samples, dim1, dim2)
    and labels of shape (num_samples,).
    """

    def __init__(self, data, labels, transform=None):
        """
        Args:
            data (numpy.ndarray): NumPy array of data samples (num_samples, dim1, dim2).
            labels (numpy.ndarray): NumPy array of labels (num_samples,).
            transform (callable, optional): Optional transform to be applied
                                            on a sample.
        """
        self.data = data
        self.labels = labels
        self.transform = transform

    def __len__(self):
        # Returns the total number of samples
        return len(self.data)

    def __getitem__(self, idx):
        # Retrieve the sample and its corresponding label at index `idx`
        sample = self.data[idx]
        label = self.labels[idx]

        # If a transform is defined, apply it to the sample
        if self.transform:
            sample = self.transform(sample)

        # Convert the sample to a PyTorch tensor (if necessary)
        sample = torch.tensor(sample, dtype=torch.float32)
        label = torch.tensor(label, dtype=torch.long)

        return sample, label
