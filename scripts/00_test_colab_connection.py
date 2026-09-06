# pyright: reportMissingImports=false
import torch  # type: ignore
import sys

print("Python version:", sys.version)
print("CUDA Available:", torch.cuda.is_available())
if torch.cuda.is_available():
    print("Device Name:", torch.cuda.get_device_name(0))
    print("Device Count:", torch.cuda.device_count())
    print("Allocated Memory:", torch.cuda.memory_allocated(0))
