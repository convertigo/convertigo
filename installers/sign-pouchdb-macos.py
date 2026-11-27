"""Sign the macOS PouchDB addon and seal the identical payload in the provider JAR."""

import hashlib
import json
from pathlib import Path
import stat
import subprocess
import sys
from tempfile import TemporaryDirectory
from zipfile import ZipFile


ADDON = "prebuilds/darwin-x64+arm64/node.napi.node"
MANIFEST = "bundle-info.json"


def sign(web_inf, lib, identity):
    bundle = web_inf / "pouchdb"
    if not bundle.exists():
        return
    (provider,) = lib.glob("convertigo-provider-couchdb-*.jar")
    manifest = (bundle / MANIFEST).read_bytes()
    info = json.loads(manifest)
    addon = bundle / ADDON
    original_addon = addon.read_bytes()

    with TemporaryDirectory(dir=lib) as work:
        signed_addon = Path(work) / addon.name
        signed_addon.write_bytes(original_addon)
        updated = Path(work) / provider.name
        with ZipFile(provider) as source:
            if (source.read("pouchdb/" + MANIFEST) != manifest
                    or source.read("pouchdb/" + ADDON) != original_addon
                    or info["files"][ADDON] != hashlib.sha256(original_addon).hexdigest()):
                raise ValueError("PouchDB payload differs from the provider before signing")
            command = ["codesign", "--force", "--options", "runtime", "--sign", identity]
            if identity != "-":
                command.append("--timestamp")
            subprocess.run([*command, signed_addon], check=True)
            native = signed_addon.read_bytes()
            info["files"][ADDON] = hashlib.sha256(native).hexdigest()
            manifest = (json.dumps(info, indent=2, ensure_ascii=False) + "\n").encode()
            replacement = {"pouchdb/" + ADDON: native, "pouchdb/" + MANIFEST: manifest}
            with ZipFile(updated, "w") as target:
                for entry in source.infolist():
                    data = replacement.get(entry.filename)
                    target.writestr(entry, source.read(entry) if data is None else data)
        updated.chmod(stat.S_IMODE(provider.stat().st_mode))
        updated.replace(provider)
        addon.write_bytes(native)
        (bundle / MANIFEST).write_bytes(manifest)


if __name__ == "__main__":
    sign(Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3])
