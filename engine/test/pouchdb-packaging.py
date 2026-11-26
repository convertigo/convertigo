"""Check the embedded PouchDB payload of a WAR or packaged Eclipse Studio plugin."""

from argparse import ArgumentParser
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile


def verify(archive, without_bundle):
    with ZipFile(archive) as artifact:
        if archive.suffix == ".war":
            web_inf = "WEB-INF/"
            lib = "WEB-INF/lib/"
        else:
            web_inf = "tomcat/webapps/convertigo/WEB-INF/"
            lib = "lib/"
        (provider,) = [name for name in artifact.namelist()
                       if name.startswith(lib + "convertigo-provider-couchdb-") and name.endswith(".jar")]
        with ZipFile(BytesIO(artifact.read(provider))) as jar:
            expected = {name for name in jar.namelist() if name.startswith("pouchdb/") and not name.endswith("/")}
            actual = {name.removeprefix(web_inf) for name in artifact.namelist()
                      if name.startswith(web_inf + "pouchdb/") and not name.endswith("/")}
            if without_bundle:
                assert not actual and not expected, "The distribution still contains the PouchDB bundle"
                print(f"{archive.name}: PouchDB files are absent from the webapp and provider JAR")
                return
            assert "pouchdb/pouchdb-server.cjs" in expected, "The provider JAR has no PouchDB payload"
            assert actual == expected, "Installed PouchDB file list differs from the provider JAR"
            for name in expected:
                assert artifact.read(web_inf + name) == jar.read(name), f"Installed file differs from the provider JAR: {name}"
    print(f"{archive.name}: the {len(expected)} PouchDB files match the provider JAR")


if __name__ == "__main__":
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("--without-bundle", action="store_true")
    parser.add_argument("archives", nargs="+", type=Path, metavar="WAR_OR_PLUGIN_JAR")
    args = parser.parse_args()
    for archive in args.archives:
        verify(archive, args.without_bundle)
