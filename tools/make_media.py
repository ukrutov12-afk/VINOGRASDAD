import glob
import os
import sys

import imageio.v2 as imageio
from PIL import Image

src = sys.argv[1]
dst = sys.argv[2]
os.makedirs(dst, exist_ok=True)
for seq in sorted(glob.glob(os.path.join(src, "seq_*"))):
    name = os.path.basename(seq)[4:]
    files = sorted(glob.glob(os.path.join(seq, "*.png")))
    if not files:
        continue
    frames = [imageio.imread(f) for f in files]
    writer = imageio.get_writer(os.path.join(dst, name + ".mp4"), fps=60, codec="libx264", quality=9, pixelformat="yuv420p", macro_block_size=2)
    for fr in frames:
        writer.append_data(fr[:, :, :3])
    writer.close()
    small = [Image.fromarray(fr[:, :, :3]).resize((fr.shape[1] * 3 // 4, fr.shape[0] * 3 // 4), Image.LANCZOS) for fr in frames[::2]]
    small[0].save(os.path.join(dst, name + ".gif"), save_all=True, append_images=small[1:], duration=33, loop=0, optimize=False)
    print(name, len(frames))
