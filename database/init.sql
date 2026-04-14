-- 停车场管理系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS parking_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE parking_management;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    role ENUM('ADMIN', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER' COMMENT '角色',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    status ENUM('ACTIVE', 'DISABLED') DEFAULT 'ACTIVE' COMMENT '状态',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 车位表
CREATE TABLE IF NOT EXISTS parking_space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_number VARCHAR(20) NOT NULL UNIQUE COMMENT '车位编号',
    status ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE') DEFAULT 'AVAILABLE' COMMENT '车位状态',
    type ENUM('STANDARD', 'LARGE', 'DISABLED', 'VIP') DEFAULT 'STANDARD' COMMENT '车位类型',
    occupied_time DATETIME COMMENT '占用时间',
    current_plate VARCHAR(20) COMMENT '当前停放车牌',
    INDEX idx_status (status),
    INDEX idx_space_number (space_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车位表';

-- 车辆进出记录表
CREATE TABLE IF NOT EXISTS vehicle_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    entry_time DATETIME NOT NULL COMMENT '入场时间',
    exit_time DATETIME COMMENT '出场时间',
    entry_image VARCHAR(500) COMMENT '入场图片路径',
    exit_image VARCHAR(500) COMMENT '出场图片路径',
    status ENUM('PARKED', 'EXITED') DEFAULT 'PARKED' COMMENT '状态',
    fee DECIMAL(10, 2) COMMENT '停车费用',
    vehicle_type VARCHAR(50) COMMENT '车辆类型',
    plate_color VARCHAR(20) COMMENT '车牌颜色',
    parking_duration INT COMMENT '停车时长(分钟)',
    INDEX idx_plate_number (plate_number),
    INDEX idx_entry_time (entry_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆进出记录表';

-- 月卡/会员表
CREATE TABLE IF NOT EXISTS membership (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    owner_name VARCHAR(50) COMMENT '车主姓名',
    owner_phone VARCHAR(20) COMMENT '车主电话',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    type ENUM('MONTHLY', 'QUARTERLY', 'YEARLY') DEFAULT 'MONTHLY' COMMENT '会员类型',
    fee DECIMAL(10, 2) COMMENT '会员费用',
    status ENUM('ACTIVE', 'EXPIRED', 'CANCELLED') DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plate_number (plate_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    description VARCHAR(200) COMMENT '配置描述',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '操作用户ID',
    operation VARCHAR(100) NOT NULL COMMENT '操作类型',
    content TEXT COMMENT '操作内容',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始化管理员账户 (密码: admin123, 实际使用时应该加密)
INSERT INTO sys_user (username, password, real_name, role, status) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'ADMIN', 'ACTIVE');

-- 初始化系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('hourly_rate', '5.0', '每小时停车费'),
('max_daily_fee', '50.0', '每日最高停车费'),
('free_time', '15', '免费停车时间(分钟)'),
('night_start', '22:00', '夜间开始时间'),
('night_end', '06:00', '夜间结束时间'),
('night_rate', '10.0', '夜间停车费');

-- 初始化车位数据 (示例：100个车位)
DELIMITER //
CREATE PROCEDURE init_parking_spaces(IN count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= count DO
        INSERT INTO parking_space (space_number, status, type) 
        VALUES (CONCAT('A', LPAD(i, 3, '0')), 'AVAILABLE', 'STANDARD');
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 调用存储过程初始化100个车位
CALL init_parking_spaces(100);

-- 删除存储过程
DROP PROCEDURE IF EXISTS init_parking_spaces;

-- 车辆信息表
CREATE TABLE IF NOT EXISTS vehicle_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL UNIQUE COMMENT '车牌号',
    plate_color VARCHAR(20) COMMENT '车牌颜色',
    vehicle_type VARCHAR(50) COMMENT '车型',
    vehicle_brand VARCHAR(50) COMMENT '车辆品牌',
    vehicle_color VARCHAR(20) COMMENT '车辆颜色',
    image_url VARCHAR(500) COMMENT '车辆图片路径',
    thumbnail_url VARCHAR(500) COMMENT '缩略图路径',
    confidence DECIMAL(5, 2) COMMENT '识别置信度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
    updated_at DATETIME COMMENT '更新时间',
    category ENUM('SEDAN', 'SUV', 'TRUCK', 'BUS', 'MOTORCYCLE', 'NEW_ENERGY', 'OTHER') DEFAULT 'SEDAN' COMMENT '车辆分类',
    remarks VARCHAR(500) COMMENT '备注',
    INDEX idx_plate_number (plate_number),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆信息表';

-- 检测记录表
CREATE TABLE IF NOT EXISTS detection_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plate_number VARCHAR(20) NOT NULL COMMENT '车牌号',
    plate_color VARCHAR(20) COMMENT '车牌颜色',
    plate_type VARCHAR(50) COMMENT '车型',
    confidence DECIMAL(5, 2) COMMENT '识别置信度',
    source VARCHAR(20) COMMENT '检测来源',
    detection_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    image_url VARCHAR(500) COMMENT '检测图片路径',
    saved BOOLEAN DEFAULT FALSE COMMENT '是否已保存到车辆库',
    INDEX idx_plate_number (plate_number),
    INDEX idx_detection_time (detection_time),
    INDEX idx_saved (saved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测记录表';
