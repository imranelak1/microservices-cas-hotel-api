"""Charger les images AMD64 du TP dans Kind, avec un flux binaire sous Windows."""
import argparse
import re
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("cluster", nargs="?", default="course7")
cluster = parser.parse_args().cluster
if not re.fullmatch(r"[a-z0-9][a-z0-9-]*", cluster):
    parser.error("Nom de cluster Kind invalide")

images = [
    "course7-hotel-rest:1.0.0",
    "course7-hotel-soap:1.0.0",
    "course7-hotel-grpc:1.0.0",
    "course7-hotel-graphql:1.0.0",
    "course7-hotel-frontend:1.0.0",
    "postgres:16.15",
]
nodes = subprocess.check_output(
    ["docker", "ps", "--filter", "label=io.x-k8s.kind.cluster=" + cluster,
     "--format", "{{.Names}}"], text=True
).splitlines()
if not nodes:
    raise SystemExit("Le cluster Kind doit être démarré : " + cluster)

for node in nodes:
    # Sélectionner AMD64 évite les manifestes d'autres architectures absents du cache.
    # Le raccordement direct des pipes préserve les octets sous PowerShell.
    export = subprocess.Popen(
        ["docker", "image", "save", "--platform", "linux/amd64", *images],
        stdout=subprocess.PIPE,
    )
    try:
        imported = subprocess.Popen(
            ["docker", "exec", "-i", node, "ctr", "-n", "k8s.io", "images",
             "import", "--platform", "linux/amd64", "-"],
            stdin=export.stdout,
        )
        export.stdout.close()
        imported_code = imported.wait()
        exported_code = export.wait()
        if imported_code or exported_code:
            raise SystemExit("Échec du chargement dans " + node)
    finally:
        if export.poll() is None:
            export.terminate()
            export.wait()
    print("Images chargées dans " + node)
