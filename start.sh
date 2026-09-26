#!/bin/bash

set -e

echo "=========================================="
echo " 产业园地下车库照明灯组分区批量划转管理系统"
echo "=========================================="
echo ""

echo "[1/5] 检查端口占用..."
PORTS=(8232 8332 3532 6632)
for PORT in "${PORTS[@]}"; do
    if lsof -Pi ":$PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "ERROR: 端口 $PORT 被占用!"
        exit 1
    fi
done
echo "端口检查通过"
echo ""

echo "[2/5] 验证后端编译..."
cd backend
if ! mvn compile -q; then
    echo "ERROR: 后端编译失败!"
    exit 1
fi
echo "后端编译通过"
cd ..
echo ""

echo "[3/5] 验证前端构建..."
cd frontend
if ! npm ci; then
    echo "ERROR: 前端依赖安装失败!"
    exit 1
fi
if ! npm run build; then
    echo "ERROR: 前端构建失败!"
    exit 1
fi
echo "前端构建通过"
cd ..
echo ""

echo "[4/5] Docker 构建启动..."
docker compose up -d --build
echo "Docker 服务启动中..."
sleep 30
echo ""

echo "[5/5] 服务验证..."
echo "验证前端 (localhost):"
if curl -s "http://localhost:8232" | grep -q "照明灯组"; then
    echo "  ✓ 前端服务正常"
else
    echo "  ✗ 前端服务异常"
    exit 1
fi

echo "验证前端 (127.0.0.1):"
if curl -s "http://127.0.0.1:8232" | grep -q "照明灯组"; then
    echo "  ✓ 前端服务正常"
else
    echo "  ✗ 前端服务异常"
    exit 1
fi

echo "验证后端 API:"
if curl -s "http://localhost:8332/api/zone/list" | grep -q "zone"; then
    echo "  ✓ 后端服务正常"
else
    echo "  ✗ 后端服务异常"
    exit 1
fi

echo ""
echo "=========================================="
echo " 系统启动完成！"
echo ""
echo " 前端地址: http://localhost:8232"
echo " 后端地址: http://localhost:8332"
echo "=========================================="
