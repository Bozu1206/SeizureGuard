#!/usr/bin/env python
"""
Test script to verify that refactored utility functions work correctly.
This tests the new utility functions without breaking existing functionality.
"""
import os
import sys
import torch

# Add the parent directory to the path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from utils.models import FCN2 as Net
from utils.tools import (
    load_model,
    load_data_and_create_loader,
    print_metrics,
    export_model_to_onnx,
    validate
)

def test_load_model():
    """Test the load_model utility function."""
    print("Testing load_model()...")
    model_path = "models/base_pat_02.pth"
    
    if not os.path.exists(model_path):
        print(f"  SKIP: Model file {model_path} not found")
        return False
    
    try:
        model = load_model(Net, model_path, device="cpu", in_channels=18)
        assert model is not None
        assert isinstance(model, Net)
        print("  PASS: Model loaded successfully")
        return True
    except Exception as e:
        print(f"  FAIL: {e}")
        return False

def test_load_data_and_create_loader():
    """Test the load_data_and_create_loader utility function."""
    print("Testing load_data_and_create_loader()...")
    data_file = "data/data_20.bin"
    
    if not os.path.exists(data_file):
        print(f"  SKIP: Data file {data_file} not found")
        return False
    
    try:
        dataloader, data, labels = load_data_and_create_loader(
            data_file, batch_size=32, shuffle=False
        )
        assert dataloader is not None
        assert data is not None
        assert labels is not None
        assert len(data) == len(labels)
        print(f"  PASS: Loaded {len(data)} samples")
        return True
    except Exception as e:
        print(f"  FAIL: {e}")
        return False

def test_print_metrics():
    """Test the print_metrics utility function."""
    print("Testing print_metrics()...")
    try:
        test_metrics = {
            'precision': 0.8542,
            'recall': 0.9123,
            'fpr': 0.0234
        }
        test_f1 = 0.8820
        print_metrics(test_f1, test_metrics, prefix="Test ")
        print("  PASS: Metrics printed successfully")
        return True
    except Exception as e:
        print(f"  FAIL: {e}")
        return False

def test_export_model_to_onnx():
    """Test the export_model_to_onnx utility function."""
    print("Testing export_model_to_onnx()...")
    model_path = "models/base_pat_02.pth"
    onnx_path = "/tmp/test_model.onnx"
    
    if not os.path.exists(model_path):
        print(f"  SKIP: Model file {model_path} not found")
        return False
    
    try:
        model = load_model(Net, model_path, device="cpu", in_channels=18)
        export_model_to_onnx(model, onnx_path, device="cpu", input_shape=(1, 18, 1024))
        assert os.path.exists(onnx_path)
        print("  PASS: Model exported to ONNX successfully")
        # Clean up
        os.remove(onnx_path)
        return True
    except Exception as e:
        print(f"  FAIL: {e}")
        return False

def test_validate_with_refactored_code():
    """Test that validate still works with refactored data loading."""
    print("Testing validate() with refactored code...")
    model_path = "models/base_pat_02.pth"
    data_file = "data/data_20.bin"
    
    if not os.path.exists(model_path) or not os.path.exists(data_file):
        print("  SKIP: Required files not found")
        return False
    
    try:
        model = load_model(Net, model_path, device="cpu", in_channels=18)
        dataloader, _, _ = load_data_and_create_loader(
            data_file, batch_size=32, shuffle=False
        )
        
        # Run validation on a subset (first batch only for speed)
        model.eval()
        f1_score, metrics = validate(dataloader, model, device="cpu")
        
        assert 0 <= f1_score <= 1, f"F1 score {f1_score} out of range"
        assert 0 <= metrics['precision'] <= 1, "Precision out of range"
        assert 0 <= metrics['recall'] <= 1, "Recall out of range"
        assert 0 <= metrics['fpr'] <= 1, "FPR out of range"
        
        print(f"  PASS: Validation completed - F1={f1_score:.4f}")
        return True
    except Exception as e:
        print(f"  FAIL: {e}")
        return False

def main():
    """Run all tests."""
    print("\n" + "="*60)
    print("Testing Refactored Utility Functions")
    print("="*60 + "\n")
    
    tests = [
        test_load_model,
        test_load_data_and_create_loader,
        test_print_metrics,
        test_export_model_to_onnx,
        test_validate_with_refactored_code,
    ]
    
    results = []
    for test in tests:
        result = test()
        results.append(result)
        print()
    
    # Summary
    print("="*60)
    passed = sum(1 for r in results if r)
    total = len(results)
    print(f"Tests Passed: {passed}/{total}")
    print("="*60)
    
    return 0 if all(results) else 1

if __name__ == "__main__":
    sys.exit(main())
