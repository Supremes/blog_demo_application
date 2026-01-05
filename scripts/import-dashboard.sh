#!/bin/bash

# Grafana Dashboard 导入脚本
# 使用方法: ./import-dashboard.sh

GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin"
GRAFANA_PASSWORD="admin"
DASHBOARD_FILE="grafana-dashboards/business-metrics-dashboard.json"

echo "📊 正在导入 Business Metrics Dashboard..."

# 检查文件是否存在
if [ ! -f "$DASHBOARD_FILE" ]; then
    echo "❌ 错误: 找不到文件 $DASHBOARD_FILE"
    exit 1
fi

# 创建导入 payload
IMPORT_PAYLOAD=$(cat <<EOF
{
  "dashboard": $(cat $DASHBOARD_FILE),
  "overwrite": true,
  "message": "Imported via script"
}
EOF
)

# 导入到 Grafana
RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -u "$GRAFANA_USER:$GRAFANA_PASSWORD" \
  "$GRAFANA_URL/api/dashboards/db" \
  -d "$IMPORT_PAYLOAD")

# 检查结果
if echo "$RESPONSE" | grep -q '"status":"success"'; then
    URL=$(echo "$RESPONSE" | grep -o '"url":"[^"]*"' | cut -d'"' -f4)
    echo "✅ Dashboard 导入成功!"
    echo "   访问地址: $GRAFANA_URL$URL"
    echo ""
    echo "🎉 你现在可以在浏览器中打开以下地址查看 Dashboard:"
    echo "   $GRAFANA_URL$URL"
else
    echo "❌ 导入失败"
    echo "$RESPONSE"
    exit 1
fi
