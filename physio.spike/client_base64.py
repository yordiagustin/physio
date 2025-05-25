import asyncio
import websockets # type: ignore
import base64
import json
import os
import time
import argparse
from pathlib import Path
from PIL import Image # type: ignore
import io

client_metrics = {
    "total_images": 0,
    "total_bytes": 0,
    "start_time": 0,
    "transmission_times": []
}

def resize_image(image_path, max_size_mb=0.5, max_dimension=1024):
    """Image was resized to 1024x1024"""
    max_size_bytes = max_size_mb * 1024 * 1024  # Convertir MB a bytes
    
    with Image.open(image_path) as img:
        # Convert to RGB if necessary
        if img.mode in ('RGBA', 'P'):
            img = img.convert('RGB')
        
        # Resize the image if it's larger than max_dimension
        if max(img.size) > max_dimension:
            ratio = max_dimension / max(img.size)
            new_size = tuple(int(dim * ratio) for dim in img.size)
            img = img.resize(new_size, Image.Resampling.LANCZOS)
        
        # Calculate Initial Quality
        quality = 85
        output = io.BytesIO()
        
        # Compress the image with reduced quality until it's smaller than max_size_bytes
        while True:
            output.seek(0)
            output.truncate()
            img.save(output, format='JPEG', quality=quality)
            if output.tell() <= max_size_bytes or quality <= 5:
                break
            quality -= 5
        
        return output.getvalue()

async def send_image(websocket, image_path):
    """Send Base64 image to the server and receive the response"""
    try:
        start_time = time.time()
        
        image_binary = resize_image(image_path)
        base64_data = base64.b64encode(image_binary).decode('utf-8')
        
        message = json.dumps({
            "image": base64_data,
            "timestamp": time.time(),
            "filename": os.path.basename(image_path)
        })
        
        message_size = len(message)
        client_metrics["total_bytes"] += message_size
        
        await websocket.send(message)
        
        response = await websocket.recv()
        
        transmission_time = time.time() - start_time
        client_metrics["transmission_times"].append(transmission_time)
        
        print(f"Image: {os.path.basename(image_path)}")
        print(f"Message size: {message_size / 1024:.2f} KB")
        print(f"Transmission time: {transmission_time * 1000:.2f} ms")
        response_data = json.loads(response)
        print(f"Response: {response_data}")
        print("-" * 50)
        
        return response_data
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo {image_path}")
        raise
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON: {str(e)}")
        raise
    except websockets.exceptions.ConnectionClosed as e:
        print(f"WebSocket connection error: {str(e)}")
        raise
    except Exception as e:
        print(f"Unexpected error processing image {image_path}: {str(e)}")
        raise

async def benchmark(image_dir, iterations=1):
    """Execute the benchmark by sending all images in a directory"""
    uri = "ws://localhost:8765"
    
    image_paths = []
    for ext in ['*.jpg', '*.jpeg', '*.png']:
        image_paths.extend(Path(image_dir).glob(ext))
    
    if not image_paths:
        print(f"No images found in {image_dir}")
        return
    
    print(f"Found {len(image_paths)} images for the benchmark")
    
    try:
        async with websockets.connect(uri) as websocket:
            print(f"Connected to {uri}")
            
            client_metrics["start_time"] = time.time()
            
            for _ in range(iterations):
                for img_path in image_paths:
                    try:
                        print(f"Sending image: {img_path}")
                        await send_image(websocket, str(img_path))
                        client_metrics["total_images"] += 1
                    except Exception as e:
                        print(f"Error processing image {img_path}: {str(e)}")
                        continue
            
            if client_metrics["total_images"] > 0:
                duration = time.time() - client_metrics["start_time"]
                avg_time = sum(client_metrics["transmission_times"]) / len(client_metrics["transmission_times"]) * 1000
                
                print("\nBenchmark Base64 Summary:")
                print(f"Total images sent: {client_metrics['total_images']}")
                print(f"Total data sent: {client_metrics['total_bytes'] / (1024*1024):.2f} MB")
                print(f"Average transmission time: {avg_time:.2f} ms")
                print(f"Total time: {duration:.2f} s")
                print(f"Throughput: {client_metrics['total_images']/duration:.2f} img/s")
                print(f"Bandwidth: {(client_metrics['total_bytes'] / duration) / (1024*1024):.2f} MB/s")
            else:
                print("Could not process any image correctly")
    except websockets.exceptions.ConnectionClosed as e:
        print(f"Connection error: {str(e)}")
    except Exception as e:
        print(f"Unexpected error: {str(e)}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="WebSocket Client for Base64 Image Benchmark")
    parser.add_argument("image_dir", help="Image Directory")
    parser.add_argument("--iterations", type=int, default=1, help="Number of iterations to send each image")
    
    args = parser.parse_args()
    
    asyncio.run(benchmark(args.image_dir, args.iterations))