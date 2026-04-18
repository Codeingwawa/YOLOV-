-- 数据库迁移脚本：更新车辆分类枚举
-- 分类只保留 NEW_ENERGY（新能源）和 FUEL（油车）

USE parking_management;

-- 1. 先修改为VARCHAR类型
ALTER TABLE vehicle_info MODIFY COLUMN category VARCHAR(20) COMMENT '车辆分类';

-- 2. 更新所有非新能源的分类为油车
UPDATE vehicle_info SET category = 'FUEL' WHERE category != 'NEW_ENERGY';

-- 3. 修改回ENUM类型
ALTER TABLE vehicle_info MODIFY COLUMN category ENUM('NEW_ENERGY', 'FUEL') DEFAULT 'FUEL' COMMENT '车辆分类(新能源/油车)';
