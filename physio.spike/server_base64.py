import asyncio
import websockets # type: ignore
import base64
import time
import json
from PIL import Image # type: ignore
import io

metrics = {
    "total_messages": 0,
    "total_bytes": 0,
    "start_time": 0,
    "processing_times": []
}

async def process_image(image_data):
    """Emulates image processing (pose estimation)"""
    try:
        start = time.time()
        
        image_bytes = base64.b64decode(image_data)
        image = Image.open(io.BytesIO(image_bytes))
        
        width, height = image.size
        format = image.format
        mode = image.mode

        process_time = time.time() - start
        metrics["processing_times"].append(process_time)
        
        return {
            "width": width,
            "height": height,
            "format": format,
            "mode": mode,
            "process_time_ms": process_time * 1000
        }
    except base64.binascii.Error:
        raise ValueError("Invalid base64 image data")
    except Exception as e:
        raise ValueError(f"Error processing image: {str(e)}")

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
                if "image" not in data:
                    await websocket.send(json.dumps({
                        "status": "error",
                        "message": "No 'image' field found in the message"
                    }))
                    continue
                
                image_data = data["image"]
                result = await process_image(image_data)
                
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
            print(f"\nBase64 performance metrics:")
            print(f"Total images processed: {metrics['total_messages']}")
            print(f"Total bytes received: {metrics['total_bytes'] / (1024*1024):.2f} MB")
            print(f"Average processing time: {avg_process:.2f} ms")
            print(f"Total processing time: {duration:.2f} s")
            print(f"Throughput: {metrics['total_messages']/duration:.2f} img/s")
            print(f"Bandwidth: {(metrics['total_bytes'] / duration) / (1024*1024):.2f} MB/s")

async def main():
    server = await websockets.serve(handle_connection, "localhost", 8765)
    print("Base64 WebSocket Server started on: ws://localhost:8765")
    await server.wait_closed()

if __name__ == "__main__":
    asyncio.run(main())