import os
import argparse
import asyncio
import subprocess
import time
import json
import matplotlib.pyplot as plt # type: ignore
import numpy as np # type: ignore
from pathlib import Path
import shutil

SERVERS = {
    "base64": {"port": 8765, "script": "server_base64.py"},
    "binary": {"port": 8766, "script": "server_binary.py"},
    "matrix": {"port": 8767, "script": "server_matrix.py"},
    "stream": {"port": 8768, "script": "server_stream.py"}
}

CLIENTS = {
    "base64": {"script": "client_base64.py"},
    "binary": {"script": "client_binary.py"},
    "matrix": {"script": "client_matrix.py"},
    "stream": {"script": "client_stream.py"}
}

results = {}

def get_python_command():
    python_commands = ['python3', 'python']
    for cmd in python_commands:
        if shutil.which(cmd):
            return cmd
    raise RuntimeError("Python command not found. Ensure Python is installed and in the PATH.")

def start_server(method):
    print(f"Starting server {method}...")
    python_cmd = get_python_command()
    server_process = subprocess.Popen([python_cmd, SERVERS[method]["script"]])
    time.sleep(2)
    return server_process

def stop_server(server_process):
    server_process.terminate()
    server_process.wait()

async def run_client(method, image_dir, iterations):
    print(f"Running client {method}...")
    
    python_cmd = get_python_command()
    cmd = [python_cmd, CLIENTS[method]["script"], image_dir, "--iterations", str(iterations)]
    
    process = await asyncio.create_subprocess_exec(
        *cmd,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE
    )
    
    stdout, stderr = await process.communicate()
    
    if process.returncode != 0:
        print(f"Error executing client {method}:")
        print(stderr.decode())
        return None
    
    output = stdout.decode()
    
    metrics = {}
    try:
        lines = output.split("\n")
        summary_section = False
        for line in lines:
            
            if any(line.startswith(header) for header in [
                "Benchmark Base64 Summary:",
                "Benchmark Binary Summary:",
                "Benchmark Matrix Summary:",
                "Benchmark Stream Summary:"
            ]):
                summary_section = True
                continue
            
            if summary_section and line.strip():
                if ":" in line:
                    key, value = line.split(":", 1)
                    key = key.strip()
                    value = value.strip()
                    
                    try:
                        if "MB" in value:
                            value = float(value.split()[0])
                        elif "ms" in value:
                            value = float(value.split()[0])
                        elif "img/s" in value:
                            value = float(value.split()[0])
                        else:
                            try:
                                value = float(value)
                            except:
                                pass
                    except:
                        pass
                    
                    metrics[key] = value
    except Exception as e:
        print(f"Error extracting metrics: {e}")
    
    return {
        "output": output,
        "metrics": metrics
    }

async def run_benchmark(method, image_dir, iterations):
    server_process = start_server(method)
    
    try:
        result = await run_client(method, image_dir, iterations)
        if result:
            results[method] = result
            print(f"Benchmark {method} completed successfully!")
        else:
            print(f"Benchmark {method} failed.")
    finally:
        stop_server(server_process)

def parse_metric_value(value):
    if isinstance(value, (int, float)):
        return value
    if isinstance(value, str):
        try:
            value = value.split()[0]
            return float(value)
        except:
            return 0
    return 0

def generate_charts():
    methods = list(results.keys())
    
    throughputs = []
    latencies = []
    bandwidths = []
    total_times = []
    
    for method in methods:
        metrics = results[method]["metrics"]
        throughputs.append(parse_metric_value(metrics.get("Throughput", 0)))
        latencies.append(parse_metric_value(metrics.get("Average transmission time", 0)))
        bandwidths.append(parse_metric_value(metrics.get("Bandwidth", 0)))
        total_times.append(parse_metric_value(metrics.get("Total time", 0)))
    
    fig, axs = plt.subplots(2, 2, figsize=(15, 10))
    
    axs[0,0].bar(methods, throughputs, color='skyblue')
    axs[0,0].set_title('Throughput (img/s)')
    axs[0,0].set_ylabel('Images per second')
    
    axs[0,1].bar(methods, latencies, color='salmon')
    axs[0,1].set_title('Latency (ms)')
    axs[0,1].set_ylabel('Milliseconds')
    
    axs[1,0].bar(methods, bandwidths, color='lightgreen')
    axs[1,0].set_title('Bandwidth (MB/s)')
    axs[1,0].set_ylabel('MB/s')
    
    axs[1,1].bar(methods, total_times, color='orange')
    axs[1,1].set_title('Total time (s)')
    axs[1,1].set_ylabel('Seconds')
    
    plt.tight_layout()
    
    plt.savefig('benchmark_comparison.png')
    print("Graph saved as benchmark_comparison.png")

def generate_report():
    if not results:
        print("No results to generate the report.")
        return
    
    print("\n" + "="*80)
    print("COMPARATIVE REPORT OF TRANSMISSION METHODS")
    print("="*80)
    
    headers = ["Method", "Throughput (img/s)", "Latency (ms)", "Bandwidth (MB/s)", "Total time (s)"]
    print(f"{headers[0]:<10} {headers[1]:<20} {headers[2]:<15} {headers[3]:<20} {headers[4]:<15}")
    print("-"*85)
    
    for method, result in results.items():
        metrics = result["metrics"]
        throughput = metrics.get("Throughput", "N/A")
        latency = metrics.get("Average transmission time", "N/A")
        bandwidth = metrics.get("Bandwidth", "N/A")
        total_time = metrics.get("Total time", "N/A")
        
        print(f"{method:<10} {throughput:<20} {latency:<15} {bandwidth:<20} {total_time:<15}")
    
    print("="*85 + "\n")
    
    with open("benchmark_results.json", "w") as f:
        json.dump(results, f, indent=2)
    
    print("Results saved in benchmark_results.json")
    
    generate_charts()

async def main():
    parser = argparse.ArgumentParser(description="Benchmark Runner for image transmission via WebSocket")
    parser.add_argument("image_dir", help="Directory containing images for the benchmark")
    parser.add_argument("--iterations", type=int, default=3, help="Number of times each image is sent")
    parser.add_argument("--methods", nargs="+", choices=["base64", "binary", "matrix", "stream", "all"], 
                        default=["all"], help="Methods to test")
    
    args = parser.parse_args()
    
    methods_to_run = list(SERVERS.keys()) if "all" in args.methods else args.methods
    
    print(f"Running benchmarks for methods: {', '.join(methods_to_run)}")
    print(f"Image directory: {args.image_dir}")
    print(f"Iterations per image: {args.iterations}")
    
    for method in methods_to_run:
        await run_benchmark(method, args.image_dir, args.iterations)
    
    generate_report()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nBenchmark interrupted by user.")