import asyncio
import websockets # type: ignore
import time
import json
import numpy as np # type: ignore
from PIL import Image # type: ignore
import io

metrics = {
    "total_messages": 0,
    "total_bytes": 0,
    "start_time": 0,
    "processing_times": []
}

async def process_image_matrix(image_data):
    """Simulates image processing (pose estimation) from matrix"""
    start = time.time()
    
    height = image_data['height']
    width = image_data['width']
    channels = image_data['channels']
    flat_data = image_data['data']
    
    matrix = np.array(flat_data, dtype=np.uint8).reshape(height, width, channels)
    
    image = Image.fromarray(matrix)
    
    process_time = time.time() - start
    metrics["processing_times"].append(process_time)
    
    return {
        "width": width,
        "height": height,
        "channels": channels,
        "process_time_ms": process_time * 1000
    }

async def handle_connection(websocket):
    """Handles the WebSocket connection with the client"""
    print(f"Client connected from {websocket.remote_address}")
    metrics["start_time"] = time.time()
    
    try:
        async for message in websocket:
            try:
                metrics["total_messages"] += 1
                metrics["total_bytes"] += len(message)
                
                data = json.loads(message)
                
                result = await process_image_matrix(data)
                
                await websocket.send(json.dumps({
                    "status": "processed",
                    "image_info": result
                }))
                
            except json.JSONDecodeError as e:
                print(f"Error decoding JSON: {str(e)}")
                await websocket.send(json.dumps({
                    "status": "error",
                    "message": "Invalid JSON message"
                }))
            except Exception as e:
                print(f"Error processing message: {str(e)}")
                await websocket.send(json.dumps({
                    "status": "error",
                    "message": "Internal server error"
                }))
            
    except websockets.exceptions.ConnectionClosed:
        print("Connection closed")
    except Exception as e:
        print(f"Unexpected error: {str(e)}")
    finally:
        if metrics["total_messages"] > 0:
            duration = time.time() - metrics["start_time"]
            avg_process = sum(metrics["processing_times"]) / len(metrics["processing_times"]) * 1000
            print(f"\nMatrix performance metrics:")
            print(f"Total images processed: {metrics['total_messages']}")
            print(f"Total bytes received: {metrics['total_bytes'] / (1024*1024):.2f} MB")
            print(f"Average processing time: {avg_process:.2f} ms")
            print(f"Total processing time: {duration:.2f} s")
            print(f"Throughput: {metrics['total_messages']/duration:.2f} img/s")
            print(f"Bandwidth: {(metrics['total_bytes'] / duration) / (1024*1024):.2f} MB/s")

async def main():
    # Configure the server with a larger message limit (2MB)
    server = await websockets.serve(
        handle_connection,
        "localhost",
        8767,
        max_size=2 * 1024 * 1024  # 2MB
    )
    print("Matrix WebSocket Server started on: ws://localhost:8767")
    
    await server.wait_closed()

if __name__ == "__main__":
    asyncio.run(main())