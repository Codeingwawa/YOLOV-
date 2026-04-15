# 智慧停车管理系统

基于 YOLOv8 + PaddleOCR 的智能车牌识别停车场管理系统

## 项目简介

本项目是一个完整的智慧停车场管理系统，包含车牌识别、车辆管理、车位管理、实时监控、统计报表等功能。

### 技术栈

- **前端**: Vue 3 + Vite + Element Plus + ECharts
- **后端**: Spring Boot 3.2 + Spring Security + JWT
- **车牌识别**: Python Flask + YOLOv8 + PaddleOCR
- **数据库**: MySQL 8.0

## 项目结构

```
parking-management/
├── parking-backend/           # Spring Boot 后端
│   ├── src/main/java/         # Java 源码
│   ├── src/main/resources/    # 配置文件
│   └── pom.xml                # Maven 依赖
├── parking-frontend/          # Vue.js 前端
│   ├── src/                   # 前端源码
│   ├── package.json           # npm 依赖
│   └── vite.config.js         # Vite 配置
├── plate-recognition-service/ # Python 车牌识别服务
│   ├── app.py                 # Flask 应用
│   ├── models/                # YOLOv8 模型文件
│   ├── fonts/                 # 字体文件
│   └── requirements.txt       # Python 依赖
├── database/                  # 数据库脚本
│   └── init.sql               # 初始化脚本
└── start-all.bat              # 一键启动脚本
```

## 功能特性

- 车牌识别: 支持图片、视频、摄像头实时识别
- 车辆管理: 车辆信息录入、查询、管理
- 车位管理: 车位状态监控、自动分配、超时释放
- 实时监控: 摄像头实时检测、结果展示
- 统计报表: 收入统计、趋势分析、数据可视化
- 用户认证: JWT 登录认证、权限管理

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- Python 3.8+
- MySQL 8.0+
- Maven 3.8+

### 1. 克隆项目

```bash
git clone https://github.com/Codeingwawa/YOLOV-.git
cd YOLOV-
```

### 2. 初始化数据库

```bash
mysql -u root -p < database/init.sql
```

### 3. 配置后端

修改 `parking-backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/parking_management
    username: root
    password: your_password
```

### 4. 安装前端依赖

```bash
cd parking-frontend
npm install
```

### 5. 安装 Python 依赖

```bash
cd plate-recognition-service
pip install -r requirements.txt
```

### 6. 启动服务

**方式一: 一键启动 (Windows)**

双击运行 `start-all.bat`

**方式二: 分别启动**

```bash
# 1. 启动后端
cd parking-backend
mvn spring-boot:run

# 2. 启动前端
cd parking-frontend
npm run dev

# 3. 启动车牌识别服务
cd plate-recognition-service
python app.py
```

### 7. 访问系统

- 前端地址: http://localhost:3000
- 后端API: http://localhost:8080
- 车牌识别服务: http://localhost:5000

默认管理员账号: `admin` / `admin123`

## API 接口

### 车牌识别服务 (端口 5000)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/recognize` | POST | 图片识别 |
| `/api/recognize-with-image` | POST | 识别并返回标注图片 |
| `/api/video/detect` | POST | 视频检测 |

### 后端服务 (端口 8080)

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录 |
| `/api/vehicle-info` | GET | 车辆列表 |
| `/api/parking-space/list` | GET | 车位列表 |
| `/api/statistics/dashboard` | GET | 仪表盘数据 |

## 注意事项

1. 首次运行需要下载 PaddleOCR 模型，可能需要较长时间
2. 建议使用 GPU 运行车牌识别服务以获得更好性能
3. 生产环境请修改 JWT 密钥和数据库密码

## 许可证

MIT License
