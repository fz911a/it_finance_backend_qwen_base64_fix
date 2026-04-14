本版修复：
1. 人脸库示例图片从 /face/zhangsan.jpg 改为 /uploads/demo_face_zhangsan.png
2. 后端增加 /face/** 静态映射兼容旧路径
3. 重新导入 schema.sql 后人脸库可预览

注意：
- 小程序真机/局域网请把前端 BASE_URL 改成电脑局域网IP:8081
- localhost 只适合同机浏览器/模拟器部分场景
