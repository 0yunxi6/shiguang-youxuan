#!/bin/bash

echo "========================================="
echo "  拾光 - 一键启动"
echo "========================================="

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误: Docker 未运行，请先启动 Docker Desktop"
    exit 1
fi

# 检查 .env 文件是否存在
if [ ! -f .env ]; then
    echo "警告: .env 文件不存在，正在创建默认配置..."
    cp .env.example .env 2>/dev/null || true
    echo "请编辑 .env 文件配置数据库密码等敏感信息"
    exit 1
fi

# 构建并启动所有服务
echo "正在构建并启动服务..."
docker-compose up -d --build

# 等待服务启动
echo "等待服务启动..."
sleep 5

# 检查服务状态
echo ""
echo "服务状态:"
docker-compose ps

echo ""
echo "========================================="
echo "  启动完成！"
echo "========================================="
echo "前端地址: http://localhost"
echo "后端地址: http://localhost:8080"
echo ""
echo "查看日志: docker-compose logs -f"
echo "停止服务: ./stop.sh"
echo "========================================="
