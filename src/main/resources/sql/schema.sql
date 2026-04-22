CREATE DATABASE IF NOT EXISTS it_finance_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE it_finance_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ai_chat_log;
DROP TABLE IF EXISTS recognition_record;
DROP TABLE IF EXISTS face_profile;
DROP TABLE IF EXISTS expense_record;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS tax_rule;
DROP TABLE IF EXISTS salary_project_allocation;
DROP TABLE IF EXISTS salary_record;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS payment_invoice;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS invoice;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(100) NOT NULL,
    project_code VARCHAR(50),
    manager_id BIGINT,
    customer_name VARCHAR(100),
    budget_amount DECIMAL(12,2),
    start_date DATE,
    end_date DATE,
    status VARCHAR(30),
    remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE invoice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_no VARCHAR(100) NOT NULL,
    invoice_date DATE,
    amount DECIMAL(12,2),
    tax_amount DECIMAL(12,2),
    customer_name VARCHAR(100),
    project_id BIGINT,
    file_url VARCHAR(255),
    ocr_result TEXT,
    description VARCHAR(255),
    paid_amount DECIMAL(12,2) DEFAULT 0,
    unpaid_amount DECIMAL(12,2) DEFAULT 0,
    status VARCHAR(30),
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    payment_date DATE,
    amount DECIMAL(12,2),
    method VARCHAR(20),
    remark VARCHAR(255),
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE payment_invoice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT,
    invoice_id BIGINT,
    allocated_amount DECIMAL(12,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE employee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    position_name VARCHAR(50),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE salary_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    pay_period VARCHAR(20),
    gross_salary DECIMAL(12,2),
    personal_tax DECIMAL(12,2),
    actual_salary DECIMAL(12,2),
    pay_date DATE,
    remark VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE salary_project_allocation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    salary_id BIGINT,
    project_id BIGINT,
    allocation_type VARCHAR(20),
    ratio DECIMAL(5,2),
    amount DECIMAL(12,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE tax_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(100),
    tax_type VARCHAR(50),
    calc_type VARCHAR(20),
    rate DECIMAL(8,4),
    ladder_json TEXT,
    scope_type VARCHAR(20),
    scope_id BIGINT,
    status TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    module_name VARCHAR(50),
    operation_type VARCHAR(50),
    content TEXT,
    ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE face_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT,
    employee_name VARCHAR(50),
    face_image_url VARCHAR(255),
    face_embedding TEXT,
    project_id BIGINT,
    status VARCHAR(20),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE recognition_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recognition_type VARCHAR(30),
    source_file_url VARCHAR(255),
    result_json TEXT,
    confidence_score DECIMAL(5,2),
    operator_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE expense_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT,
    employee_id BIGINT,
    expense_type VARCHAR(50),
    amount DECIMAL(12,2),
    expense_date DATE,
    merchant_name VARCHAR(100),
    receipt_url VARCHAR(255),
    ai_summary VARCHAR(255),
    status VARCHAR(30) DEFAULT '待提交',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE ai_chat_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    module_name VARCHAR(50),
    prompt_text TEXT,
    result_text TEXT,
    parsed_json TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO sys_user (id, username, password, real_name, role, status, create_time) VALUES
    (1, 'admin', '123456', '系统管理员', 'ADMIN', 1, '2026-04-20 08:30:00'),
    (2, 'finance', '123456', '财务人员', 'FINANCE', 1, '2026-04-20 08:31:00'),
    (3, 'manager1', '123456', '项目负责人', 'PROJECT_MANAGER', 1, '2026-04-20 08:32:00');

INSERT INTO project (id, project_name, project_code, manager_id, customer_name, budget_amount, start_date, end_date, status, remark) VALUES
    (1, '智慧运维平台', 'IT2026001', 3, '星河科技', 300000.00, '2026-01-01', '2026-12-31', '进行中', '重点项目'),
    (2, '企业数据中台', 'IT2026002', 3, '远航信息', 500000.00, '2026-01-15', '2026-11-30', '进行中', '企业级平台'),
    (3, '移动审批系统', 'IT2026003', 3, '海纳软件', 180000.00, '2026-02-01', '2026-10-31', '进行中', '移动端项目');

INSERT INTO invoice (id, invoice_no, invoice_date, amount, tax_amount, customer_name, project_id, file_url, ocr_result, description, paid_amount, unpaid_amount, status, create_by, create_time) VALUES
    (1, 'FP20260401001', '2026-04-01', 50000.00, 3000.00, '星河科技', 1, '/uploads/demo_invoice1.png', '{"invoiceNo":"FP20260401001","amount":50000,"projectName":"智慧运维平台"}', '阶段一回款发票', 32000.00, 18000.00, '部分回款', 1, '2026-04-01 10:00:00'),
    (2, 'FP20260315006', '2026-03-15', 80000.00, 4800.00, '远航信息', 2, '/uploads/demo_invoice2.png', '{"invoiceNo":"FP20260315006","amount":80000,"projectName":"企业数据中台"}', '全额回款发票', 80000.00, 0.00, '已回款', 1, '2026-03-15 11:20:00'),
    (3, 'FP20260309003', '2026-03-09', 35000.00, 2100.00, '海纳软件', 3, '/uploads/demo_invoice3.png', '{"invoiceNo":"FP20260309003","amount":35000,"projectName":"移动审批系统"}', '待催收发票', 0.00, 35000.00, '未回款', 1, '2026-03-09 09:45:00'),
    (4, 'FP20260418002', '2026-04-18', 24000.00, 1440.00, '星河科技', 1, '/uploads/demo_invoice4.png', '{"invoiceNo":"FP20260418002","amount":24000,"projectName":"智慧运维平台"}', '项目二阶段发票', 20000.00, 4000.00, '部分回款', 1, '2026-04-18 16:15:00'),
    (5, 'FP20260506007', '2026-05-06', 10000.00, 600.00, '海纳软件', 3, '/uploads/demo_invoice5.png', '{"invoiceNo":"FP20260506007","amount":10000,"projectName":"移动审批系统"}', '里程碑发票', 10000.00, 0.00, '已回款', 1, '2026-05-06 14:00:00');

INSERT INTO payment (id, project_id, payment_date, amount, method, remark, create_by, create_time) VALUES
    (1, 1, '2026-04-03', 32000.00, '银行转账', '第一阶段回款', 1, '2026-04-03 15:30:00'),
    (2, 2, '2026-03-20', 80000.00, '银行转账', '全额回款', 1, '2026-03-20 10:00:00'),
    (3, 1, '2026-04-20', 20000.00, '银行转账', '第二阶段回款', 1, '2026-04-20 11:40:00'),
    (4, 3, '2026-05-08', 10000.00, '银行转账', '里程碑回款', 1, '2026-05-08 09:25:00');

INSERT INTO payment_invoice (payment_id, invoice_id, allocated_amount) VALUES
    (1, 1, 32000.00),
    (2, 2, 80000.00),
    (3, 4, 20000.00),
    (4, 5, 10000.00);

INSERT INTO employee (id, name, position_name, phone, status) VALUES
    (1, '张三', '前端开发', '13800000001', 1),
    (2, '李四', '后端开发', '13800000002', 1),
    (3, '王五', '测试工程师', '13800000003', 1),
    (4, '赵六', '产品经理', '13800000004', 1);

INSERT INTO salary_record (id, employee_id, pay_period, gross_salary, personal_tax, actual_salary, pay_date, remark) VALUES
    (1, 1, '2026-03', 12000.00, 360.00, 11640.00, '2026-03-31', '3月工资'),
    (2, 2, '2026-03', 15000.00, 450.00, 14550.00, '2026-03-31', '3月工资'),
    (3, 3, '2026-03', 9000.00, 270.00, 8730.00, '2026-03-31', '3月工资'),
    (4, 4, '2026-04', 13000.00, 390.00, 12610.00, '2026-04-30', '4月工资');

INSERT INTO salary_project_allocation (salary_id, project_id, allocation_type, ratio, amount) VALUES
    (1, 1, '按比例', 100.00, 12000.00),
    (2, 2, '按比例', 100.00, 15000.00),
    (3, 3, '按比例', 100.00, 9000.00),
    (4, 1, '按比例', 100.00, 13000.00);

INSERT INTO tax_rule (rule_name, tax_type, calc_type, rate, ladder_json, scope_type, scope_id, status) VALUES
    ('个人所得税', '人员', '阶梯', NULL, '3%~45%', '按人员配置', NULL, 1),
    ('增值税', '项目', '比例', 6.0000, NULL, '按项目配置', 1, 1),
    ('企业所得税', '全局', '比例', 25.0000, NULL, '全局配置', NULL, 1);

INSERT INTO operation_log (id, user_id, module_name, operation_type, content, ip, create_time) VALUES
    (1, 1, 'auth', 'login-success', '账号登录成功：admin', '127.0.0.1', '2026-04-20 09:00:00'),
    (2, 2, 'auth', 'login-fail', '账号登录失败：finance，密码错误', '127.0.0.1', '2026-04-20 09:02:00'),
    (3, 1, 'face', 'face-enroll', '人员录入：张三', '127.0.0.1', '2026-04-20 09:12:00'),
    (4, 1, 'face', 'face-recognize', '人脸识别通过：张三', '127.0.0.1', '2026-04-20 09:18:00'),
    (5, 1, 'report', 'export', '导出报表：project-profit csv', '127.0.0.1', '2026-04-20 09:25:00');

INSERT INTO face_profile (id, employee_id, employee_name, face_image_url, face_embedding, project_id, status, create_time) VALUES
    (1, 1, '张三', '/uploads/demo_face_zhangsan.png', 'demo-vector-001', 1, '已启用', '2026-04-20 09:11:00'),
    (2, 2, '李四', '/uploads/demo_face_lisi.png', 'demo-vector-002', 2, '已录入', '2026-04-20 09:14:00'),
    (3, 3, '王五', '/uploads/demo_face_wangwu.png', 'demo-vector-003', 3, '已停用', '2026-04-20 09:15:00'),
    (4, 4, '赵六', '/uploads/demo_face_zhaoliu.png', 'demo-vector-004', 1, '已录入', '2026-04-20 09:16:00');

INSERT INTO recognition_record (id, recognition_type, source_file_url, result_json, confidence_score, operator_id, create_time) VALUES
    (1, 'invoice', '/uploads/demo_invoice1.png', '{"invoiceNo":"FP20260401001","customerName":"星河科技","projectName":"智慧运维平台"}', 98.50, 1, '2026-04-20 09:20:00'),
    (2, 'receipt', '/receipt/r1.jpg', '{"merchantName":"星海酒店","amount":560,"expenseType":"差旅费"}', 92.30, 2, '2026-04-20 09:21:00'),
    (3, 'face', '/uploads/demo_face_zhangsan.png', '{"employeeName":"张三","projectId":1,"match":"张三"}', 96.10, 1, '2026-04-20 09:22:00'),
    (4, 'face', '/uploads/demo_face_lisi.png', '{"employeeName":"李四","projectId":2,"match":"李四"}', 89.40, 1, '2026-04-20 09:23:00');

INSERT INTO expense_record (id, project_id, employee_id, expense_type, amount, expense_date, merchant_name, receipt_url, ai_summary, status, create_time) VALUES
    (1, 1, 1, '差旅费', 560.00, '2026-04-02', '星海酒店', '/receipt/r1.jpg', 'AI识别为差旅费小票', '待审核', '2026-04-02 10:20:00'),
    (2, 2, 2, '软件服务费', 1280.00, '2026-04-08', '云桥科技', '/receipt/r2.jpg', '云服务器及软件订阅', '待提交', '2026-04-08 14:10:00'),
    (3, 3, 3, '招待费', 3200.00, '2026-05-03', '海纳餐厅', '/receipt/r3.jpg', '商务招待费用', '已报销', '2026-05-03 18:30:00'),
    (4, 1, 4, '办公费', 980.00, '2026-05-12', '星海办公', '/receipt/r4.jpg', '办公耗材采购', '已报销', '2026-05-12 09:05:00');

INSERT INTO ai_chat_log (id, user_id, module_name, prompt_text, result_text, parsed_json, create_time) VALUES
    (1, 1, 'report', '分析项目1的4月经营情况', '项目1 4月回款良好，但仍有部分未收账款，需要关注', '{"projectId":1,"income":52000,"expense":1540}', '2026-04-20 09:30:00'),
    (2, 2, 'risk', '判断项目3回款风险', '项目3存在未回款发票，建议优先催收', '{"projectId":3,"riskLevel":"高"}', '2026-04-20 09:31:00'),
    (3, 1, 'ocr', '识别上传的发票图片', '已提取发票号码和金额信息', '{"invoiceNo":"FP20260401001","amount":50000}', '2026-04-20 09:32:00');

SET FOREIGN_KEY_CHECKS = 1;
