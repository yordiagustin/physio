import asyncio
import websockets # type: ignore
import time
import json
from PIL import Image # type: ignore
import io
import struct

metrics = {
    "total_messages": 0,
    "total_bytes": 0,
    "start_time": 0,
    "processing_times": []
}

class ImageStreamProcessor:
    def __init__(self):
        self.reset()
    
    def reset(self):
        self.buffer = bytearray()
        self.expected_size = None
        self.image_id = None
        self.fragments_received = 0
    
    def add_fragment(self, fragment_data):
        if self.expected_size is None and len(fragment_data) >= 12:
            self.image_id, self.expected_size = struct.unpack("!QI", fragment_data[:12])
            self.buffer.extend(fragment_data[12:])
        else:
            self.buffer.extend(fragment_data)
        
        self.fragments_received += 1
        
        return len(self.buffer) >= self.expected_size if self.expected_size is not None else False
    
    def get_image_data(self):
        return self.buffer[:self.expected_size], self.image_id, self.fragments_received

async def process_image(image_bytes):
    """Simulates image processing (pose estimation)"""
    start = time.time()
    
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

async def handle_connection(websocket):
    """Handles the WebSocket connection with the client"""
    print(f"Client connected from {websocket.remote_address}")
    metrics["start_time"] = time.time()
    
    stream_processor = ImageStreamProcessor()
    
    try:
        async for message in websocket:
            try:
                if isinstance(message, bytes):
                    metrics["total_bytes"] += len(message)
                    
                    is_complete = stream_processor.add_fragment(message)
                    
                    if is_complete:
                        image_data, image_id, fragments = stream_processor.get_image_data()
                        metrics["total_messages"] += 1
                        
                        result = await process_image(image_data)
                        result["fragments_received"] = fragments
                        result["image_id"] = image_id
                        
                        await websocket.send(json.dumps({
                            "status": "processed",
                            "image_info": result
                        }))
                        
                        stream_processor.reset()
                    else:
                        await websocket.send(json.dumps({
                            "status": "fragment_received",
                            "fragments": stream_processor.fragments_received
                        }))
                else:
                    print(f"Received non-binary message: {type(message)}")
                    await websocket.send(json.dumps({
                        "status": "error",
                        "message": "Expected a binary message"
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
            print(f"\nStream performance metrics:")
            print(f"Total images processed: {metrics['total_messages']}")
            print(f"Total bytes received: {metrics['total_bytes'] / (1024*1024):.2f} MB")
            print(f"Average processing time: {avg_process:.2f} ms")
            print(f"Total time: {duration:.2f} s")
            print(f"Throughput: {metrics['total_messages']/duration:.2f} img/s")
            print(f"Bandwidth: {(metrics['total_bytes'] / duration) / (1024*1024):.2f} MB/s")

async def main():
    server = await websockets.serve(handle_connection, "localhost", 8768)
    print("Stream WebSocket Server started on: ws://localhost:8768")
    
    await server.wait_closed()

if __name__ == "__main__":
    asyncio.run(main())