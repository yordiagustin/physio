import asyncio
import websockets # type: ignore
import json
import numpy as np # type: ignore
import os
import time
import argparse
from PIL import Image # type: ignore
from pathlib import Path
import io

client_metrics = {
    "total_images": 0,
    "total_bytes": 0,
    "start_time": 0,
    "transmission_times": []
}

def resize_image(image_path, max_size_mb=0.5, max_dimension=1024):
    """Resize the image to not exceed the specified maximum size in MB"""
    max_size_bytes = max_size_mb * 1024 * 1024  # Convert MB to bytes
    
    with Image.open(image_path) as img:
        # Convert to RGB if necessary
        if img.mode in ('RGBA', 'P'):
            img = img.convert('RGB')
        
        # Resize the image if it's larger than max_dimension
        if max(img.size) > max_dimension:
            ratio = max_dimension / max(img.size)
            new_size = tuple(int(dim * ratio) for dim in img.size)
            img = img.resize(new_size, Image.Resampling.LANCZOS)
        
        # Calculate initial quality
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
        
        output.seek(0)
        return np.array(Image.open(output), dtype=np.uint8)

async def send_image(websocket, image_path):
    """Send an image through WebSocket as a matrix"""
    try:
        start_time = time.time()
        
        matrix = resize_image(image_path)
        
        height, width, channels = matrix.shape if len(matrix.shape) == 3 else (*matrix.shape, 1)
        
        flat_data = matrix.flatten().tolist()
        
        message = json.dumps({
            "height": height,
            "width": width,
            "channels": channels,
            "data": flat_data,
            "filename": os.path.basename(image_path),
            "timestamp": time.time()
        })
        
        message_size = len(message)
        if message_size > 900 * 1024:  # If the message is larger than 900KB
            print(f"Warning: Message too large ({message_size/1024:.2f}KB), trying to compress more...")
            matrix = resize_image(image_path, max_size_mb=0.2, max_dimension=128)
            height, width, channels = matrix.shape if len(matrix.shape) == 3 else (*matrix.shape, 1)
            flat_data = matrix.flatten().tolist()
            message = json.dumps({
                "height": height,
                "width": width,
                "channels": channels,
                "data": flat_data,
                "filename": os.path.basename(image_path),
                "timestamp": time.time()
            })
            message_size = len(message)
        
        client_metrics["total_bytes"] += message_size
        
        await websocket.send(message)
        
        response = await websocket.recv()
        
        transmission_time = time.time() - start_time
        client_metrics["transmission_times"].append(transmission_time)
        
        print(f"Image: {os.path.basename(image_path)}")
        print(f"Dimensions: {width}x{height}x{channels}")
        print(f"Message size: {message_size / 1024:.2f} KB")
        print(f"Transmission time: {transmission_time * 1000:.2f} ms")
        response_data = json.loads(response)
        print(f"Response: {response_data}")
        print("-" * 50)
        
        return response_data
    except FileNotFoundError:
        print(f"Error: File {image_path} not found")
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
    uri = "ws://localhost:8767"
    
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
                
                print("\nBenchmark Matrix Summary:")
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
    parser = argparse.ArgumentParser(description="WebSocket Client for Image Transmission Benchmark as Matrix")
    parser.add_argument("image_dir", help="Directory containing images for the benchmark")
    parser.add_argument("--iterations", type=int, default=1, help="Number of times each image is sent")
    
    args = parser.parse_args()
    
    asyncio.run(benchmark(args.image_dir, args.iterations))