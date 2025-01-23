# import serial
# import time
# import struct

# # Configuration
# COM_PORT = "COM13"    # Replace with the correct COM port
# BAUD_RATE = 115200     # UART baud rate
# SEND_INTERVAL = 1.0    # Interval in seconds between sending samples
# DATA_FILE = "data.bin" # Path to your data file

# def load_data(filename):
#     with open(filename, "rb") as f:
#         # Read header: 4 int32 (little-endian)
#         header_bytes = f.read(16)  # 4 integers * 4 bytes each
#         if len(header_bytes) < 16:
#             raise ValueError("Invalid file: Header too short")
        
#         # Parse header
#         numArrays, dim1, dim2, labelsPresent = struct.unpack('<iiii', header_bytes)
#         print(f"numArrays: {numArrays}, dim1: {dim1}, dim2: {dim2}, labelsPresent: {labelsPresent}")

#         totalFloats = numArrays * dim1 * dim2
#         dataSize = totalFloats * 4  # float32 is 4 bytes
#         data_bytes = f.read(dataSize)
#         if len(data_bytes) < dataSize:
#             raise ValueError("Invalid file: Not enough data bytes")

#         # Convert to floats (little-endian float32)
#         float_data = struct.unpack('<' + ('f' * totalFloats), data_bytes)

#         # Read labels if present
#         if labelsPresent == 1:
#             labelsSize = numArrays * 4  # int32 * numArrays
#             labels_bytes = f.read(labelsSize)
#             if len(labels_bytes) < labelsSize:
#                 raise ValueError("Invalid file: Not enough label bytes")
#             labels = struct.unpack('<' + ('i' * numArrays), labels_bytes)
#         else:
#             # No labels present, set all to -1
#             labels = [-1] * numArrays

#         # Construct data samples
#         dataSamples = []
#         float_index = 0
#         for i in range(numArrays):
#             sample_data = float_data[float_index:float_index + (dim1 * dim2)]
#             float_index += (dim1 * dim2)
#             label = labels[i]
#             dataSamples.append((sample_data, label))

#         return dataSamples

# def main():
#     dataSamples = load_data(DATA_FILE)
#     numSamples = len(dataSamples)
#     sampleIndex = 0

#     try:
#         with serial.Serial(COM_PORT, BAUD_RATE, timeout=1) as ser:
#             print(f"Connected to {COM_PORT} at {BAUD_RATE} baud.")

#             while True:
#                 # Get the current sample
#                 sample_data, label = dataSamples[sampleIndex]

#                 # Prepare the data string: floats separated by space, then label
#                 # Example: "f0 f1 f2 ... fN label\n"
#                 sample_str = " ".join(f"{x:.6f}" for x in sample_data) + f" {label}\n"
                
#                 # Send data
#                 ser.write(sample_str.encode('utf-8'))
#                 print(f"Sent sample {sampleIndex}: label={label}, length={len(sample_data)}")

#                 # Move to the next sample
#                 sampleIndex = (sampleIndex + 1) % numSamples

#                 # Wait before sending the next sample
#                 time.sleep(SEND_INTERVAL)

#     except serial.SerialException as e:
#         print(f"Serial Error: {e}")
#     except KeyboardInterrupt:
#         print("Exiting.")
#     except ValueError as ve:
#         print(f"Data Loading Error: {ve}")

# if __name__ == "__main__":
#     main()

