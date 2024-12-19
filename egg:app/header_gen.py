import struct
import os
import numpy as np

# Configuration
DATA_FILE = r"C:\ncs\eeg_sensor_2\data.bin"    # Path to your data file
OUTPUT_HEADER = "src/eeg_data.h"         # Output C header file
MAX_SAMPLES = 8192                              # Number of samples to embed (~600 kB)

def load_data(filename, max_samples):
    if not os.path.isfile(filename):
        raise FileNotFoundError(f"The file {filename} does not exist.")
    
    with open(filename, "rb") as f:
        # Read header: 4 int32 (little-endian)
        header_bytes = f.read(16)  # 4 integers * 4 bytes each
        if len(header_bytes) < 16:
            raise ValueError("Invalid file: Header too short")
        
        # Parse header
        numArrays, dim1, dim2, labelsPresent = struct.unpack('<iiii', header_bytes)
        print(f"numArrays: {numArrays}, dim1: {dim1}, dim2: {dim2}, labelsPresent: {labelsPresent}")

        # Validate dimensions
        if dim1 != 18:
            raise ValueError(f"Invalid dim1: Expected 18, got {dim1}")
        if dim2 != 1024:
            raise ValueError(f"Invalid dim2: Expected 1024, got {dim2}")
        if labelsPresent != 1:
            raise ValueError(f"Invalid labelsPresent: Expected 1, got {labelsPresent}")

        # Calculate total number of floats
        totalFloats = numArrays * dim1 * dim2
        dataSize = totalFloats * 4  # float32 is 4 bytes

        print("Reading EEG data...")
        # Read all EEG data
        eeg_data_bytes = f.read(dataSize)
        if len(eeg_data_bytes) < dataSize:
            raise ValueError("Invalid file: Not enough EEG data bytes")

        # Convert to NumPy array for efficient processing
        eeg_data = np.frombuffer(eeg_data_bytes, dtype='<f4')  # Little-endian float32
        eeg_data = eeg_data.reshape((numArrays, dim2, dim1))  # Shape: (numArrays, dim2, dim1)
        print("EEG data loaded successfully.")

        # Read all labels
        labelsSize = numArrays * 4  # int32 * numArrays
        labels_bytes = f.read(labelsSize)
        if len(labels_bytes) < labelsSize:
            raise ValueError("Invalid file: Not enough label bytes")
        
        labels = struct.unpack('<' + ('i' * numArrays), labels_bytes)
        labels = np.array(labels)
        print("Labels loaded successfully.")

        # Find the first array with label == 1
        target_indices = np.where(labels == 1)[0]
        if len(target_indices) == 0:
            raise ValueError("No array with label 1 found in the data.")
        
        target_array_index = target_indices[0]
        print(f"First array with label 1 found at index: {target_array_index}")

        # Calculate starting array index (5 before)
        start_array_index = max(0, target_array_index - 5)
        print(f"Starting from array index: {start_array_index}")

        # Calculate how many arrays to collect to reach MAX_SAMPLES
        # Each array has dim2=1024 samples
        arrays_needed = int(np.ceil(MAX_SAMPLES / dim2))
        end_array_index = start_array_index + arrays_needed
        if end_array_index > numArrays:
            end_array_index = numArrays
            print("Adjusted end array index to fit within total arrays.")

        # Adjust MAX_SAMPLES based on available arrays
        actual_arrays_collected = end_array_index - start_array_index
        actual_samples = actual_arrays_collected * dim2
        if actual_samples > MAX_SAMPLES:
            actual_samples = MAX_SAMPLES

        print(f"Collecting {actual_samples} samples from arrays {start_array_index} to {end_array_index - 1}.")

        # Collect the samples
        collected_samples = []
        for array_idx in range(start_array_index, end_array_index):
            for sample_idx in range(dim2):
                if len(collected_samples) >= MAX_SAMPLES:
                    break
                channels = eeg_data[array_idx, sample_idx, :].tolist()
                label = labels[array_idx]
                collected_samples.append((channels, label))
            if len(collected_samples) >= MAX_SAMPLES:
                break

        print(f"Total samples collected: {len(collected_samples)}")

        return collected_samples

def generate_c_header(dataSamples, output_filename):
    numSamples = len(dataSamples)
    
    with open(output_filename, "w") as f:
        # Write header guards and includes
        f.write("#ifndef EEG_DATA_GENERATED_H\n")
        f.write("#define EEG_DATA_GENERATED_H\n\n")
        f.write("#include <stdint.h>\n\n")
        
        # Write struct definition
        f.write("// Define the EEG Sample Struct\n")
        f.write("typedef struct {\n")
        f.write("    float channels[18];    // 18 EEG channel values\n")
        f.write("    int32_t label;          // Label for the sample\n")
        f.write("} eeg_sample_t;\n\n")
        
        # Write the data array
        f.write("// Declare the EEG Data Array\n")
        f.write(f"const eeg_sample_t eeg_data_array[{numSamples}] = {{\n")
        
        for sample in dataSamples:
            channels, label = sample
            # Format floats with 6 decimal places
            channels_str = ", ".join(f"{x:.6f}f" for x in channels)
            f.write(f"    {{ {{ {channels_str} }}, {label} }},\n")
        
        f.write("};\n\n")
        
        # Write the size of the data array
        f.write(f"const size_t eeg_data_size = {numSamples};\n\n")
        
        # Close header guard
        f.write("#endif /* EEG_DATA_GENERATED_H */\n")
    
    print(f"C header file '{output_filename}' generated with {numSamples} samples.")

def main():
    try:
        dataSamples = load_data(DATA_FILE, MAX_SAMPLES)
        generate_c_header(dataSamples, OUTPUT_HEADER)
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
