-- 数据库迁移脚本：更新车辆分类枚举
-- 将旧的分类（SEDAN, SUV, MOTORCYCLE）合并为 FUEL（油车）

USE parking_management;

-- 1. 先修改为VARCHAR类型
ALTER TABLE vehicle_info MODIFY COLUMN category VARCHAR(20) COMMENT '车辆分类';

-- 2. 更新旧分类数据
UPDATE vehicle_info SET category = 'FUEL' WHERE category IN ('SEDAN', 'SUV', 'MOTORCYCLE');

-- 3. 修改回ENUM类型
ALTER TABLE vehicle_info MODIFY COLUMN category ENUM('NEW_ENERGY', 'FUEL', 'TRUCK', 'BUS', 'OTHER') DEFAULT 'FUEL' COMMENT '车辆分类';
