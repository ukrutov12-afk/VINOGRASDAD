import glob
import os
import sys

import numpy as np
from PIL import Image

root = sys.argv[1]
for seq in sorted(glob.glob(os.path.join(root, "seq_*"))):
    files = sorted(glob.glob(os.path.join(seq, "*.png")))
    prev = None
    diffs = []
    for f in files:
        a = np.asarray(Image.open(f).convert("L"), dtype=np.float32)
        if prev is not None:
            diffs.append(float(np.abs(a - prev).mean()))
        prev = a
    d = np.array(diffs)
    if len(d) < 3:
        continue
    med = np.convolve(d, np.ones(5) / 5, mode="same")
    spikes = [(i + 1, round(v, 2), round(m, 2)) for i, (v, m) in enumerate(zip(d, med)) if v > 2.2 * m + 0.4]
    line = " ".join(f"{v:.1f}" for v in d)
    print(f"{os.path.basename(seq):14s} n={len(files):3d} spikes={spikes}")
    print("   ", line)
