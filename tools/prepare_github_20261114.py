# -*- coding: utf-8 -*-
"""准备 2026-11-14 GitHub 发布内容：极简 README / About / Release 说明 + 脱敏源码树。"""
import json
import os
import re
import shutil

ROOT = r"D:\ccz_winlator_patch"
OUT = os.path.join(ROOT, "build", "_gh_publish_20261114")
UPLOAD = os.path.join(OUT, "files")

README = """# 曹操传加强版 v2.20 · Winlator 8.0 魔改

基于 [Winlator](https://github.com/brunodev85/winlator) 8.0 魔改，用于让《三国志曹操传》加强版 v2.20 在 Android 设备上运行。

## 下载

➡️ [Releases](../../releases/latest) — 懒人包：一个 APK 装完即用，不需要 adb、电脑或部署脚本。

**不含游戏本体**，游戏请自备并放到 `/sdcard/ccz`。

## 已知事项

- **预计启动时间：20–40 秒**（首次启动、冷启动会更久，视机型、图形驱动与温度浮动）。
- **音频**：容器默认 ALSA 后端可能无声或导致容器启动异常；建议在容器设置里改用 **PulseAudio**。
- **天玑 / 骁龙需自行配置容器参数**：骁龙 / Adreno 用 **Turnip**；天玑 / Mali 建好容器后把图形驱动改成 **Virgl**，box64 预设与其余参数按机型自行调整。

## 开源协议

- 本项目的 APK 是 [Winlator](https://github.com/brunodev85/winlator)（**GPLv3**）的修改版。分发修改后的二进制须遵守 GPLv3：提供对应源码、以同协议开源，不得附加额外限制。
- [Wine](https://www.winehq.org/) 遵循 **LGPL**，[box64](https://github.com/ptitSeb/box64) 遵循 **MIT**，[proot](https://github.com/proot-me/proot) 遵循 **GPLv2**。
- 仓库与 Release **不包含游戏本体**；请使用你自己合法拥有的《三国志曹操传》加强版副本。
- 本项目仅供个人研究与技术交流，与游戏权利方无关；若相关权利方有异议会立即下架。
"""

ABOUT = ("基于 Winlator 8.0 魔改，适配《三国志曹操传》加强版 v2.20。"
         "预计启动 20–40 秒；音频后端建议 PulseAudio；天玑与骁龙需自行配置容器参数。"
         "APK 为 GPLv3 衍生，源码与懒人包见仓库 / Releases。")

RELEASE_BODY = """懒人包：一个 APK 装完即用，不需要 adb、电脑或部署脚本。

- 资源：`曹操传Winlator懒人包_20261114.zip`（286.8 MB，md5 `7af5bf5b0c4bf1adc84950985200b719`）
- 包内：魔改 APK + 使用说明；**不含游戏本体**，游戏放到 `/sdcard/ccz`
- 已装过旧版且能运行的设备：先卸载或清数据再装，否则系统镜像不会重新解包

预计启动 20–40 秒。音频后端建议 PulseAudio；骁龙用 Turnip，天玑需改 Virgl。
详见仓库 README 与包内使用说明。
"""

RELEASE_TAG = "v1.2-20261114"
RELEASE_NAME = "懒人包 2026-11-14"
ASSET_NAME = "曹操传Winlator懒人包_20261114.zip"
ASSET_PATH = os.path.join(ROOT, "deliverables", "曹操传Winlator懒人包_20261114.zip")

# 源文件范围：日志、报告、二进制、APK、游戏资源一律不传
SRC_DIRS = ["src", "patch", "tools", "tests", "unpack_tools", "_publish"]
EXTS = {".py", ".sh", ".c", ".h", ".java", ".md", ".json", ".txt",
        ".cmake", ".make", ".smali", ".bat", ".ps1", ".ini", ".cfg",
        ".yml", ".yaml", ".xml", ".gradle", ".properties"}
MAX_SIZE = 3 * 1024 * 1024  # 单个文本文件上限 3 MB
EXCLUDE_NAME = re.compile(r"日志|攻坚|\.log$|token|credential|secret|\.github", re.I)
EXCLUDE_PATH = re.compile(r"(^|[\\/])(_归档_|artifacts|node_modules|\.git)([\\/]|$)", re.I)

REDACT = [
    ("PHONE_SERIAL_REDACTED", "PHONE_SERIAL_REDACTED"),
    ("PHONE_SERIAL_REDACTED", "PHONE_SERIAL_REDACTED"),
    ("TABLET_SERIAL_REDACTED", "TABLET_SERIAL_REDACTED"),
    ("TABLET_SERIAL_REDACTED", "TABLET_SERIAL_REDACTED"),
    ("TEST_SERIAL_REDACTED", "TEST_SERIAL_REDACTED"),
]


def target_path(rel):
    """_publish/ccz-winlator-patch/** -> 仓库根；其余保持原路径。"""
    p = rel.replace("\\", "/")
    prefix = "_publish/ccz-winlator-patch/"
    if p.startswith(prefix):
        return p[len(prefix):]
    return p


def main():
    os.makedirs(UPLOAD, exist_ok=True)
    for f in os.listdir(OUT):
        p = os.path.join(OUT, f)
        if os.path.isfile(p) and f != "files":
            os.remove(p)
    if os.path.isdir(UPLOAD):
        shutil.rmtree(UPLOAD)
    os.makedirs(UPLOAD)

    with open(os.path.join(OUT, "README.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write(README)
    with open(os.path.join(OUT, "about.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write(ABOUT)
    with open(os.path.join(OUT, "release_body.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write(RELEASE_BODY)
    meta = {"tag": RELEASE_TAG, "name": RELEASE_NAME,
            "asset": ASSET_NAME, "asset_path": ASSET_PATH, "about": ABOUT}
    with open(os.path.join(OUT, "meta.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    manifest, redacted = [], 0
    for top in SRC_DIRS:
        base = os.path.join(ROOT, top)
        if not os.path.isdir(base):
            continue
        for dirpath, dirnames, filenames in os.walk(base):
            dirnames[:] = [d for d in dirnames if not EXCLUDE_PATH.search(os.path.join(dirpath, d))]
            for fn in filenames:
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, ROOT).replace("\\", "/")
                if os.path.splitext(fn)[1].lower() not in EXTS:
                    continue
                if EXCLUDE_NAME.search(fn) or EXCLUDE_PATH.search(rel):
                    continue
                try:
                    size = os.path.getsize(full)
                    if size > MAX_SIZE:
                        continue
                    data = open(full, "rb").read()
                    text = data.decode("utf-8", "replace")
                except OSError:
                    continue
                new = text
                for a, b in REDACT:
                    new = new.replace(a, b)
                if new != text:
                    redacted += 1
                dst_rel = target_path(rel)
                dst = os.path.join(UPLOAD, dst_rel.replace("/", os.sep))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(dst, "w", encoding="utf-8", newline="\n") as f:
                    f.write(new)
                manifest.append(dst_rel)

    with open(os.path.join(OUT, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=0)
    total = sum(os.path.getsize(os.path.join(UPLOAD, p.replace("/", os.sep))) for p in manifest)
    print("待上传文件:", len(manifest), "| 总体积: %.1f MB" % (total / 1048576.0))
    print("脱敏文件:", redacted)
    print("README/About/Release 文案已生成于", OUT)


if __name__ == "__main__":
    main()
