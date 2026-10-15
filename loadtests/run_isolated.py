"""One real server, database and bounded load generator per disposable runner."""
import collections
import csv
import gzip
import json
import math
import os
from pathlib import Path
import platform
import subprocess
import threading
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "run-output"
OUT.mkdir(exist_ok=True)
PROTOCOL = os.environ.get("PROTOCOL", "rest")
PORT = {"rest":19601, "soap":19602, "graphql":19603, "grpc":19604}[PROTOCOL]
SMOKE = os.environ.get("SMOKE", "false").lower() == "true"
STOP = threading.Event()


def command(*args, **kwargs):
    return subprocess.check_output(list(args), text=True, **kwargs).strip()


def monitor():
    ticks = int(command("docker", "exec", "hotel-measured", "getconf", "CLK_TCK"))
    previous = None
    with (OUT / "resources.csv").open("w", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(["timestamp_ms", "protocol", "cpu_percent_one_core", "rss_bytes", "read_bytes", "write_bytes"])
        while not STOP.is_set():
            try:
                raw = command("docker", "exec", "hotel-measured", "sh", "-c", "cat /proc/1/stat /proc/1/status /proc/1/io")
                lines = raw.splitlines()
                stat = lines[0].split(")", 1)[1].split()
                cpu = (int(stat[11]) + int(stat[12])) / ticks
                values = {line.split(":", 1)[0]:line.split(":", 1)[1].strip() for line in lines[1:] if ":" in line}
                now = time.time()
                percent = 0 if previous is None else 100 * (cpu - previous[1]) / (now - previous[0])
                writer.writerow([round(now * 1000), PROTOCOL, round(percent, 4),
                    int(values["VmRSS"].split()[0]) * 1024, int(values["read_bytes"]), int(values["write_bytes"])])
                output.flush()
                previous = now, cpu
            except (OSError, subprocess.CalledProcessError, ValueError) as error:
                print("Resource sample unavailable:", type(error).__name__, flush=True)
            STOP.wait(2)


def percentile(values, fraction):
    return values[max(0, math.ceil(len(values) * fraction) - 1)]


def aggregate():
    phases = {}
    with (OUT / "raw.csv.phases.csv").open(newline="") as source:
        for row in csv.DictReader(source):
            phases[(row["size_kib"], row["concurrency"], row["repetition"])] = row
    groups = collections.defaultdict(lambda:{"latencies":[], "errors":0, "request":0, "response":0})
    with (OUT / "raw.csv").open(newline="") as source:
        for row in csv.DictReader(source):
            key = row["size_kib"], row["concurrency"], row["repetition"], row["operation"]
            for target in [key, key[:3] + ("ALL",)]:
                group = groups[target]
                group["latencies"].append(float(row["elapsed_ms"]))
                group["errors"] += row["success"].lower() != "true"
                group["request"] += int(row["request_bytes"])
                group["response"] += int(row["response_bytes"])
    with (OUT / "summary.csv").open("w", newline="") as output:
        writer = csv.writer(output)
        writer.writerow(["protocol", "size_kib", "concurrency", "repetition", "operation", "attempts", "errors", "error_percent", "mean_ms", "p50_ms", "p95_ms", "p99_ms", "throughput_rps", "mean_request_body_bytes", "mean_response_body_bytes"])
        for key, group in groups.items():
            values = sorted(group["latencies"])
            phase = phases[key[:3]]
            writer.writerow([PROTOCOL, *key, len(values), group["errors"], round(100 * group["errors"] / len(values), 4),
                round(sum(values) / len(values), 4), percentile(values, .5), percentile(values, .95), percentile(values, .99),
                round(len(values) / float(phase["duration_seconds"]), 4), round(group["request"] / len(values), 3), round(group["response"] / len(values), 3)])
    assert len(phases) == (1 if SMOKE else 36), f"Incomplete phases: {len(phases)}"
    if SMOKE:
        actual = sum(len(group["latencies"]) for key,group in groups.items() if key[3]=="ALL")
        errors = sum(group["errors"] for key,group in groups.items() if key[3]=="ALL")
        assert actual==8 and errors==0, f"Actual smoke CRUD: {actual} calls, {errors} failures"
    for name in ["raw.csv", "resources.csv"]:
        source = OUT / name
        with source.open("rb") as data, gzip.open(str(source)+".gz", "wb", compresslevel=6) as target:
            while chunk := data.read(1024*1024):
                target.write(chunk)
        source.unlink()


def main():
    for name in ["hotel-generator","hotel-measured","hotel-postgres"]:
        if subprocess.run(["docker","container","inspect",name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0:
            raise RuntimeError("Refusing to touch existing container: "+name)
    metadata = {"protocol":PROTOCOL,"mode":"smoke" if SMOKE else "full", "github_run_id":os.environ.get("GITHUB_RUN_ID"),
        "commit":os.environ.get("GITHUB_SHA"),"platform":platform.platform(),"visible_cpus":os.cpu_count(),
        "cpu_budget":4,"memory_budget_bytes":8*1024**3,"server_limit_bytes":1024**3,
        "database_limit_bytes":1024**3,"generator_limit_bytes":6*1024**3,
        "server_java_heap":"160m","server_node_heap":"160m","generator_java_heap":"512m",
        "java_server":"17","java_generator":"21.0.8+9","postgres":"16.15","shared_database":False,
        "metrics":"real process /proc/1 CPU ticks and VmRSS every2s; one core=100%",
        "payload":"canonical business JSON 1/10/100KiB; recorded bytes exclude transport headers/framing",
        "started_unix":time.time()}
    (OUT / "environment.json").write_text(json.dumps(metadata, indent=2))
    thread = None
    try:
        command("docker","run","-d","--name","hotel-postgres","--memory=1g","--cpus=4",
            "-p","127.0.0.1:19632:5432","-e","POSTGRES_USER=lab","-e","POSTGRES_PASSWORD=labpass","-e","POSTGRES_DB=hotel",
            "-v",str(ROOT/"database/init.sql")+":/docker-entrypoint-initdb.d/init.sql:ro","postgres:16.15")
        deadline = time.monotonic() + 180
        while subprocess.run(["docker","exec","hotel-postgres","pg_isready","-h","127.0.0.1","-U","lab","-d","hotel"],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode:
            if time.monotonic()>deadline:raise RuntimeError("PostgreSQL TCP did not become ready")
            time.sleep(2)
        counts=command("docker","exec","hotel-postgres","psql","-U","lab","-d","hotel","-Atc","SELECT (SELECT count(*) FROM client),(SELECT count(*) FROM chambre),(SELECT count(*) FROM reservation)")
        assert counts=="100|1000|1",counts
        (OUT/"dataset-initial.csv").write_text("clients,chambres,reservations\n"+counts.replace("|",",")+"\n")
        command("docker","run","-d","--name","hotel-measured","--network","host","--memory=1g","--cpus=4",
            "-e","DB_URL=jdbc:postgresql://localhost:19632/hotel","-e","DATABASE_URL=postgresql://lab:labpass@localhost:19632/hotel",
            "-e","BENCHMARK=true","-e","LOGGING_LEVEL_ROOT=WARN","hotel-measured:1.0.0")
        path="/health" if PROTOCOL=="graphql" else "/actuator/health"
        deadline=time.monotonic()+180
        while True:
            try:
                with urllib.request.urlopen(f"http://localhost:{PORT}{path}",timeout=3) as response:
                    assert json.load(response)["status"]=="UP"
                break
            except (OSError,AssertionError):
                if time.monotonic()>deadline:raise RuntimeError("Measured API did not become ready")
                time.sleep(2)
        print("RUNNER_HEALTHY",PROTOCOL,"database100/1000/1 + targetAPIUP",flush=True)
        (OUT/"containers.json").write_text(command("docker","inspect","hotel-measured","hotel-postgres"))
        thread=threading.Thread(target=monitor);thread.start()
        args=["docker","run","--name","hotel-generator","--network","host","--memory=6g","--cpus=4",
            "-v",str(ROOT)+":/work:ro","-v",str(OUT)+":/results","-w","/work",
            "eclipse-temurin:21.0.8_9-jre-jammy","java","-Xmx512m","-Xss512k",
            "-Dloader.main=ma.imrane.hotel.benchmark.BenchmarkClient","-Doutput=/results/raw.csv",
            "-Dprotocols="+PROTOCOL,"-Dlogin.port="+str(PORT)]
        if SMOKE:args += ["-Dusers=2","-Dsizes=1","-Drepetitions=1"]
        args += ["-cp","grpc-api/target/grpc-api-1.0.0.jar","org.springframework.boot.loader.launch.PropertiesLauncher"]
        with (OUT/"client.log").open("w") as log:subprocess.run(args,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
        STOP.set();thread.join(15)
        aggregate()
        metadata["finished_unix"]=time.time()
        metadata["completed_phases"]=1 if SMOKE else 36
        (OUT/"environment.json").write_text(json.dumps(metadata,indent=2))
        print("COMPLETE",PROTOCOL,metadata["completed_phases"],"phases",flush=True)
    finally:
        STOP.set()
        if thread:thread.join(15)
        for container in ["hotel-measured","hotel-postgres","hotel-generator"]:
            with (OUT/(container+".log")).open("w") as log:subprocess.run(["docker","logs",container],stdout=log,stderr=subprocess.STDOUT)
            subprocess.run(["docker","rm","-f",container],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)


if __name__=="__main__":main()
