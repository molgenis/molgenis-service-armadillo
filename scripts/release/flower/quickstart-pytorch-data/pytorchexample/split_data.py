"""Prepare CIFAR10 subsets for upload to Armadillo storage."""

import torch
from torchvision.datasets import CIFAR10
from torchvision.transforms import Compose, Normalize, ToTensor

transform = Compose([
    ToTensor(),
    Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5)),
])


def dataset_to_tensors(ds, indices):
    imgs, labels = [], []
    for i in indices:
        x, y = ds[i]
        imgs.append(x)
        labels.append(y)
    return torch.stack(imgs), torch.tensor(labels)


def main():
    """Create small CIFAR10 subsets and save as .pt files."""
    full_train = CIFAR10(root="data", train=True, download=True, transform=transform)
    n = len(full_train)  # 50_000
    n20 = n // 5  # 10_000 (20%)

    # Train splits
    x1, y1 = dataset_to_tensors(full_train, range(0, n20))
    torch.save({"images": x1, "labels": y1}, "cifar10_train.pt")
    print(f"Saved cifar10_train.pt ({n20} samples)")

    # Test splits
    full_test = CIFAR10(root="data", train=False, download=True, transform=transform)
    n_test = len(full_test)  # 10_000
    n20_test = n_test // 5  # 2_000

    x1e, y1e = dataset_to_tensors(full_test, range(0, n20_test))
    torch.save({"images": x1e, "labels": y1e}, "cifar10_test.pt")
    print(f"Saved cifar10_test.pt ({n20_test} samples)")


if __name__ == "__main__":
    main()
