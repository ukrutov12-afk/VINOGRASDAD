import os
import sys

import moderngl

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "fashion", "shaders", "core")
PAIRS = [("ui.vsh", "ui.fsh"), ("fullscreen.vsh", "blur_down.fsh"), ("fullscreen.vsh", "blur_up.fsh")]

ctx = moderngl.create_standalone_context(require=330)
ok = True
for v, f in PAIRS:
    try:
        ctx.program(vertex_shader=open(os.path.join(ROOT, v)).read(), fragment_shader=open(os.path.join(ROOT, f)).read())
        print("ok", v, f)
    except Exception as e:
        ok = False
        print("FAIL", v, f, e)
sys.exit(0 if ok else 1)
