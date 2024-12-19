import torch.nn as nn


class FCN2(nn.Module):
    def __init__(self, in_channels=22):
        super(FCN2, self).__init__()
        # first convolutional block
        n_filters = 128
        self.conv1 = nn.Conv1d(
            in_channels, n_filters, kernel_size=3, padding=1
        )
        self.bn1 = nn.BatchNorm1d(n_filters)
        self.relu1 = nn.ReLU()
        # first pooling
        self.pool1 = nn.MaxPool1d(kernel_size=4, padding=0)

        # second convolutional block
        self.conv2 = nn.Conv1d(n_filters, n_filters, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm1d(n_filters)
        self.relu2 = nn.ReLU()
        # second pooling
        self.pool2 = nn.MaxPool1d(kernel_size=4, padding=0)

        # third convolutional block
        self.conv3 = nn.Conv1d(n_filters, n_filters, kernel_size=3, padding=1)
        self.bn3 = nn.BatchNorm1d(n_filters)
        self.relu3 = nn.ReLU()
        # second pooling
        self.pool3 = nn.MaxPool1d(kernel_size=4, padding=0)

        # Fully connected layers within the classifier sequential module
        self.classifier = nn.Sequential(
            nn.Conv1d(n_filters, 100, kernel_size=16, padding=0),
            nn.Conv1d(100, 2, kernel_size=1, padding=0),
        )

    def get_features(self, x):
        out = self.relu1(self.bn1(self.conv1(x)))
        out = self.pool1(out)
        out = self.relu2(self.bn2(self.conv2(out)))
        out = self.pool2(out)
        out = self.relu3(self.bn3(self.conv3(out)))
        out = self.pool3(out)
        return out

    def forward(self, x):
        features = self.get_features(x)
        out = self.classifier(features)
        out = out.transpose(0, 1)  # nxbxt
        n, b, t = out.size()
        out = out.contiguous()
        out = out.view(-1, t * b)  # nx(t*b)
        out = out.t()  # (t*b)xn

        return out
