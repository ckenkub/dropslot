#!/bin/bash

# 🎯 DropSlot Prometheus + Grafana Monitoring Setup Script
# This script sets up and tests the complete monitoring stack

echo "🚀 DropSlot Prometheus + Grafana Monitoring Setup"
echo "=================================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "📊 1. Starting Monitoring Stack"
echo "--------------------------------"

cd /home/kritchanartl/projects/dropslot/backend

echo "🔄 Starting all services including Prometheus & Grafana..."
docker-compose -f compose.e2e.yml up -d

echo "⏳ Waiting for services to be ready..."
sleep 30

echo -e "\n📋 2. Service Health Checks"
echo "-----------------------------"

services=("api-gateway:8080" "user-service:8081" "store-service:8082" "prometheus:9090" "grafana:3000")
for service in "${services[@]}"; do
    name=$(echo $service | cut -d':' -f1)
    port=$(echo $service | cut -d':' -f2)
    
    if curl -s http://localhost:$port > /dev/null; then
        echo "✅ $name is running on port $port"
    else
        echo "❌ $name is not accessible on port $port"
    fi
done

echo -e "\n🎯 3. Prometheus Configuration Test"
echo "------------------------------------"

echo "🔍 Checking Prometheus targets:"
TARGETS=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null | jq -r '.data.activeTargets[].labels.job' 2>/dev/null | sort | uniq)
if [[ -n "$TARGETS" ]]; then
    echo "✅ Active Prometheus targets:"
    echo "$TARGETS" | sed 's/^/   - /'
else
    echo "❌ No Prometheus targets found"
fi

echo -e "\n📊 4. Metrics Verification"
echo "---------------------------"

echo "🔍 Testing Kafka metrics availability:"
KAFKA_METRICS=$(curl -s "http://localhost:9090/api/v1/label/__name__/values" 2>/dev/null | jq -r '.data[] | select(. | contains("kafka"))' 2>/dev/null | head -5)
if [[ -n "$KAFKA_METRICS" ]]; then
    echo "✅ Kafka metrics available in Prometheus:"
    echo "$KAFKA_METRICS" | sed 's/^/   - /'
else
    echo "⚠️  No Kafka metrics found yet (may need time to collect)"
fi

echo -e "\n🎨 5. Grafana Dashboard Setup"
echo "------------------------------"

echo "🔍 Checking Grafana accessibility:"
if curl -s http://admin:admin@localhost:3000/api/health > /dev/null; then
    echo "✅ Grafana is accessible"
    echo "📊 Dashboard URL: http://localhost:3000"
    echo "🔑 Login: admin / admin"
else
    echo "❌ Grafana is not accessible"
fi

echo -e "\n🧪 6. Generate Test Data"
echo "-------------------------"

echo "🔄 Triggering Kafka events to populate metrics..."
for i in {1..3}; do
    echo "📨 Sending test event $i..."
    curl -s -X POST http://localhost:8080/api/auth/register \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"monitoring.test$i@example.com\",
        \"password\": \"password123\",
        \"name\": \"Monitoring Test User $i\"
      }" > /dev/null
    sleep 2
done

echo "⏳ Waiting for metrics to be collected..."
sleep 10

echo -e "\n📈 7. Final Metrics Check"
echo "--------------------------"

PROCESSED_COUNT=$(curl -s http://localhost:8082/actuator/metrics/kafka.messages.processed 2>/dev/null | jq -r '.measurements[0].value // "N/A"')
echo "✅ Messages Processed: $PROCESSED_COUNT"

echo -e "\n🎉 Monitoring Setup Complete!"
echo "=============================="
echo ""
echo "🔗 Access URLs:"
echo "   • Grafana Dashboard:  http://localhost:3000 (admin/admin)"
echo "   • Prometheus:         http://localhost:9090"
echo "   • API Gateway:        http://localhost:8080"
echo ""
echo "📊 Pre-configured Dashboards:"
echo "   • DropSlot Kafka Monitoring (auto-loaded)"
echo ""
echo "🎯 Next Steps:"
echo "   1. Open Grafana at http://localhost:3000"
echo "   2. Login with admin/admin"
echo "   3. Navigate to Dashboards → DropSlot Kafka Monitoring"
echo "   4. Watch real-time metrics as you use the application"
echo ""
echo "💡 To generate more test data:"
echo "   ./scripts/kafka-monitoring-test.sh"
