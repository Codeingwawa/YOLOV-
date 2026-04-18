import os
import sys
import time
import base64
import json
import threading
from io import BytesIO
from flask import Flask, request, jsonify, Response, stream_with_context
from flask_cors import CORS
from ultralytics import YOLO
from paddleocr import PaddleOCR
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import torch
import logging
from queue import Queue

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_PATH = os.path.join(BASE_DIR, 'models')
MODEL_PATH = os.path.join(MODELS_PATH, 'best.pt')
FONT_PATH = os.path.join(BASE_DIR, 'fonts', 'platech.ttf')

device = 'cuda' if torch.cuda.is_available() else 'cpu'
logger.info(f"Using device: {device}")

logger.info("Loading YOLO model...")
model = YOLO(MODEL_PATH, task='detect')
model(np.zeros((48, 48, 3)).astype(np.uint8), device=device)
logger.info("YOLO model loaded successfully")

logger.info("Loading vehicle detection model...")
vehicle_model = YOLO('yolov8n.pt', task='detect')
vehicle_model(np.zeros((48, 48, 3)).astype(np.uint8), device=device)
logger.info("Vehicle detection model loaded successfully")

logger.info("Loading PaddleOCR model...")
ocr = PaddleOCR(use_angle_cls=True, lang="ch", det=True)
logger.info("PaddleOCR model loaded successfully")

PLATE_COLORS = {
    0: '绿牌',
    1: '蓝牌'
}

PLATE_TYPES = {
    0: '新能源',
    1: '普通'
}

VEHICLE_CLASSES = {
    1: '自行车',
    2: '轿车',
    3: '摩托车',
    5: '公交车',
    7: '卡车'
}

VEHICLE_CATEGORY_MAP = {
    '轿车': 'SEDAN',
    '公交车': 'BUS',
    '卡车': 'TRUCK',
    '摩托车': 'MOTORCYCLE',
    '自行车': 'OTHER'
}

camera_streams = {}
camera_lock = threading.Lock()


def detect_plate_color(crop_img):
    if crop_img is None or crop_img.size == 0:
        return '蓝牌', '普通'
    
    hsv = cv2.cvtColor(crop_img, cv2.COLOR_BGR2HSV)
    
    green_lower = np.array([35, 43, 46])
    green_upper = np.array([77, 255, 255])
    green_mask = cv2.inRange(hsv, green_lower, green_upper)
    green_ratio = np.sum(green_mask > 0) / (crop_img.shape[0] * crop_img.shape[1])
    
    blue_lower = np.array([100, 43, 46])
    blue_upper = np.array([124, 255, 255])
    blue_mask = cv2.inRange(hsv, blue_lower, blue_upper)
    blue_ratio = np.sum(blue_mask > 0) / (crop_img.shape[0] * crop_img.shape[1])
    
    yellow_lower = np.array([26, 43, 46])
    yellow_upper = np.array([34, 255, 255])
    yellow_mask = cv2.inRange(hsv, yellow_lower, yellow_upper)
    yellow_ratio = np.sum(yellow_mask > 0) / (crop_img.shape[0] * crop_img.shape[1])
    
    if green_ratio > 0.15:
        return '绿牌', '新能源'
    elif yellow_ratio > 0.15:
        return '黄牌', '大型车'
    else:
        return '蓝牌', '普通'


def detect_vehicle_type(image_array):
    results = vehicle_model(image_array, conf=0.25, iou=0.7, classes=[1, 2, 3, 5, 7])[0]
    
    vehicle_types = []
    for box in results.boxes:
        cls_id = int(box.cls[0])
        conf = float(box.conf[0])
        x1, y1, x2, y2 = map(int, box.xyxy[0])
        
        vehicle_name = VEHICLE_CLASSES.get(cls_id, '未知')
        vehicle_types.append({
            'type': vehicle_name,
            'category': VEHICLE_CATEGORY_MAP.get(vehicle_name, 'OTHER'),
            'confidence': round(conf * 100, 2),
            'location': [x1, y1, x2, y2]
        })
    
    if vehicle_types:
        return max(vehicle_types, key=lambda x: x['confidence'])
    return None


def recognize_plate(image_array):
    results = model(image_array, conf=0.3, iou=0.7)[0]
    
    location_list = results.boxes.xyxy.tolist()
    location_list = [list(map(int, e)) for e in location_list]
    
    cls_list = results.boxes.cls.tolist()
    cls_list = [int(i) for i in cls_list]
    
    vehicle_info = detect_vehicle_type(image_array)
    
    plate_results = []
    
    for idx, (location, cls) in enumerate(zip(location_list, cls_list)):
        x1, y1, x2, y2 = location
        crop_img = image_array[y1:y2, x1:x2]
        
        plate_color, plate_type = detect_plate_color(crop_img)
        
        ocr_result = ocr.ocr(crop_img, cls=True)
        
        if ocr_result and ocr_result[0]:
            plate_number, confidence = ocr_result[0][0][1]
            if '·' in plate_number:
                plate_number = plate_number.replace('·', '')
        else:
            plate_number = "无法识别"
            confidence = 0.0
        
        result = {
            'plateNumber': plate_number,
            'confidence': round(confidence * 100, 2),
            'plateColor': plate_color,
            'plateType': plate_type,
            'location': location
        }
        
        if vehicle_info:
            result['vehicleType'] = vehicle_info['type']
            result['vehicleCategory'] = vehicle_info['category']
            result['vehicleConfidence'] = vehicle_info['confidence']
        else:
            result['vehicleType'] = '未知'
            result['vehicleCategory'] = 'OTHER'
            result['vehicleConfidence'] = 0
        
        plate_results.append(result)
    
    return plate_results


def draw_results(image_array, results):
    img = Image.fromarray(cv2.cvtColor(image_array, cv2.COLOR_BGR2RGB))
    draw = ImageDraw.Draw(img)
    
    try:
        font = ImageFont.truetype(FONT_PATH, 30)
        small_font = ImageFont.truetype(FONT_PATH, 20)
    except:
        font = ImageFont.load_default()
        small_font = font
    
    for result in results:
        x1, y1, x2, y2 = result['location']
        plate_number = result['plateNumber']
        vehicle_type = result.get('vehicleType', '')
        
        draw.rectangle([x1, y1, x2, y2], outline=(255, 0, 0), width=2)
        draw.text((x1, y1 - 35), plate_number, fill=(255, 0, 0), font=font)
        
        if vehicle_type and vehicle_type != '未知':
            draw.text((x1, y2 + 5), f'车型: {vehicle_type}', fill=(0, 128, 0), font=small_font)
    
    return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)


def generate_frames(camera_id=0):
    cap = cv2.VideoCapture(camera_id)
    if not cap.isOpened():
        logger.error(f"Cannot open camera {camera_id}")
        return
    
    frame_count = 0
    detection_interval = 5
    
    while True:
        success, frame = cap.read()
        if not success:
            break
        
        frame_count += 1
        
        if frame_count % detection_interval == 0:
            results = recognize_plate(frame)
            if results:
                frame = draw_results(frame, results)
                with camera_lock:
                    if camera_id not in camera_streams:
                        camera_streams[camera_id] = {'results': results}
                    else:
                        camera_streams[camera_id]['results'] = results
        
        ret, buffer = cv2.imencode('.jpg', frame)
        frame_bytes = buffer.tobytes()
        
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')
    
    cap.release()


def generate_video_frames(video_path):
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        logger.error(f"Cannot open video {video_path}")
        return
    
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = 0
    detection_interval = max(1, int(fps / 2))
    
    all_results = []
    
    while True:
        success, frame = cap.read()
        if not success:
            break
        
        frame_count += 1
        
        if frame_count % detection_interval == 0:
            results = recognize_plate(frame)
            if results:
                frame = draw_results(frame, results)
                for r in results:
                    r['frame'] = frame_count
                    r['timestamp'] = frame_count / fps
                    all_results.append(r)
        
        ret, buffer = cv2.imencode('.jpg', frame)
        frame_bytes = buffer.tobytes()
        
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')
    
    cap.release()


@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'healthy',
        'device': device,
        'model': 'YOLOv8 + PaddleOCR'
    })


@app.route('/api/recognize', methods=['POST'])
def recognize():
    start_time = time.time()
    
    if 'image' not in request.files:
        return jsonify({'error': 'No image provided'}), 400
    
    file = request.files['image']
    
    try:
        file_bytes = np.frombuffer(file.read(), np.uint8)
        image = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
        
        if image is None:
            return jsonify({'error': 'Invalid image'}), 400
        
        results = recognize_plate(image)
        
        process_time = time.time() - start_time
        
        if results:
            return jsonify({
                'plateNumber': results[0]['plateNumber'],
                'confidence': results[0]['confidence'],
                'plateColor': results[0]['plateColor'],
                'plateType': results[0]['plateType'],
                'vehicleType': results[0].get('vehicleType', '未知'),
                'vehicleCategory': results[0].get('vehicleCategory', 'OTHER'),
                'vehicleConfidence': results[0].get('vehicleConfidence', 0),
                'allResults': results,
                'processTime': round(process_time, 3)
            })
        else:
            return jsonify({
                'plateNumber': None,
                'confidence': 0,
                'message': 'No plate detected',
                'processTime': round(process_time, 3)
            })
            
    except Exception as e:
        logger.error(f"Recognition error: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/recognize-base64', methods=['POST'])
def recognize_base64():
    start_time = time.time()
    
    data = request.get_json()
    
    if not data or 'image' not in data:
        return jsonify({'error': 'No image provided'}), 400
    
    try:
        image_data = data['image']
        if ',' in image_data:
            image_data = image_data.split(',')[1]
        
        image_bytes = base64.b64decode(image_data)
        nparr = np.frombuffer(image_bytes, np.uint8)
        image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if image is None:
            return jsonify({'error': 'Invalid image'}), 400
        
        results = recognize_plate(image)
        
        process_time = time.time() - start_time
        
        if results:
            return jsonify({
                'plateNumber': results[0]['plateNumber'],
                'confidence': results[0]['confidence'],
                'plateColor': results[0]['plateColor'],
                'plateType': results[0]['plateType'],
                'vehicleType': results[0].get('vehicleType', '未知'),
                'vehicleCategory': results[0].get('vehicleCategory', 'OTHER'),
                'vehicleConfidence': results[0].get('vehicleConfidence', 0),
                'allResults': results,
                'processTime': round(process_time, 3)
            })
        else:
            return jsonify({
                'plateNumber': None,
                'confidence': 0,
                'message': 'No plate detected',
                'processTime': round(process_time, 3)
            })
            
    except Exception as e:
        logger.error(f"Recognition error: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/recognize-with-image', methods=['POST'])
def recognize_with_image():
    start_time = time.time()
    
    if 'image' not in request.files:
        return jsonify({'error': 'No image provided'}), 400
    
    file = request.files['image']
    
    try:
        file_bytes = np.frombuffer(file.read(), np.uint8)
        image = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
        
        if image is None:
            return jsonify({'error': 'Invalid image'}), 400
        
        results = recognize_plate(image)
        
        result_image = draw_results(image, results)
        
        _, buffer = cv2.imencode('.jpg', result_image)
        result_image_base64 = base64.b64encode(buffer).decode('utf-8')
        
        process_time = time.time() - start_time
        
        if results:
            return jsonify({
                'plateNumber': results[0]['plateNumber'],
                'confidence': results[0]['confidence'],
                'plateColor': results[0]['plateColor'],
                'plateType': results[0]['plateType'],
                'vehicleType': results[0].get('vehicleType', '未知'),
                'vehicleCategory': results[0].get('vehicleCategory', 'OTHER'),
                'vehicleConfidence': results[0].get('vehicleConfidence', 0),
                'allResults': results,
                'resultImage': result_image_base64,
                'processTime': round(process_time, 3)
            })
        else:
            return jsonify({
                'plateNumber': None,
                'confidence': 0,
                'message': 'No plate detected',
                'resultImage': result_image_base64,
                'processTime': round(process_time, 3)
            })
            
    except Exception as e:
        logger.error(f"Recognition error: {str(e)}")
        return jsonify({'error': str(e)}), 500


@app.route('/api/camera/stream/<int:camera_id>')
def camera_stream(camera_id):
    return Response(generate_frames(camera_id),
                   mimetype='multipart/x-mixed-replace; boundary=frame')


@app.route('/api/camera/results/<int:camera_id>')
def get_camera_results(camera_id):
    with camera_lock:
        if camera_id in camera_streams:
            return jsonify(camera_streams[camera_id]['results'])
    return jsonify([])


@app.route('/api/video/stream', methods=['POST'])
def video_stream():
    if 'video' not in request.files:
        return jsonify({'error': 'No video provided'}), 400
    
    video_file = request.files['video']
    temp_path = os.path.join('temp_videos', video_file.filename)
    os.makedirs('temp_videos', exist_ok=True)
    video_file.save(temp_path)
    
    return Response(generate_video_frames(temp_path),
                   mimetype='multipart/x-mixed-replace; boundary=frame')


@app.route('/api/video/detect', methods=['POST'])
def video_detect():
    if 'video' not in request.files:
        return jsonify({'error': 'No video provided'}), 400
    
    video_file = request.files['video']
    temp_path = os.path.join('temp_videos', video_file.filename)
    os.makedirs('temp_videos', exist_ok=True)
    video_file.save(temp_path)
    
    cap = cv2.VideoCapture(temp_path)
    if not cap.isOpened():
        return jsonify({'error': 'Cannot open video'}), 400
    
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    frame_count = 0
    detection_interval = max(1, int(fps / 2))
    
    all_results = []
    detected_plates = set()
    
    while True:
        success, frame = cap.read()
        if not success:
            break
        
        frame_count += 1
        
        if frame_count % detection_interval == 0:
            results = recognize_plate(frame)
            for r in results:
                plate_number = r['plateNumber']
                if plate_number not in detected_plates and plate_number != "无法识别":
                    result_frame = draw_results(frame, [r])
                    _, buffer = cv2.imencode('.jpg', result_frame)
                    result_image_base64 = base64.b64encode(buffer).decode('utf-8')
                    
                    r['frame'] = frame_count
                    r['timestamp'] = round(frame_count / fps, 2)
                    r['resultImage'] = result_image_base64
                    all_results.append(r)
                    detected_plates.add(plate_number)
    
    cap.release()
    
    return jsonify({
        'totalFrames': total_frames,
        'processedFrames': frame_count,
        'detectionCount': len(all_results),
        'uniquePlates': len(detected_plates),
        'results': all_results
    })


@app.route('/api/video/detect-frame', methods=['POST'])
def video_detect_frame():
    if 'frame' not in request.files:
        return jsonify({'error': 'No frame provided'}), 400
    
    file = request.files['frame']
    
    try:
        file_bytes = np.frombuffer(file.read(), np.uint8)
        image = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
        
        if image is None:
            return jsonify({'error': 'Invalid frame'}), 400
        
        results = recognize_plate(image)
        
        result_image = draw_results(image, results)
        _, buffer = cv2.imencode('.jpg', result_image)
        result_image_base64 = base64.b64encode(buffer).decode('utf-8')
        
        return jsonify({
            'results': results,
            'resultImage': result_image_base64
        })
            
    except Exception as e:
        logger.error(f"Frame detection error: {str(e)}")
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    logger.info("Starting Plate Recognition Service on port 5000...")
    app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)
