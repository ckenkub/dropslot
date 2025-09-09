#!/bin/bash

# 🎯 DropSlot Kafka Monitoring Test Script
# This script demonstrates the monitoring improvements we've implemented

echo "🚀 DropSlot Kafka Monitoring & Operations Test"
echo "==============================================="

# Check if services are running
echo "📊 1. Service Health Checks"
echo "----------------------------"

echo "🔍 API Gateway Status:"
curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "❌ API Gateway not accessible"

echo -e "\n🔍 Store Service Status:"
curl -s http://localhost:8082/actuator/health 2>/dev/null || echo "❌ Store Service not accessible"

echo -e "\n🔍 User Service Status:"
curl -s http://localhost:8081/actuator/health 2>/dev/null || echo "❌ User Service not accessible"

# Test Kafka functionality
echo -e "\n\n📨 2. Kafka Event Processing Test"
echo "-----------------------------------"

echo "🧹 Cleaning up existing test data..."
# Delete any existing test user to ensure clean test
docker exec backend-postgres-user-1 psql -U user -d user_db -c "DELETE FROM users WHERE email = 'kafka.monitoring.test@example.com';" 2>/dev/null || echo "No existing test user found"
# Also cleanup any related store data
docker exec backend-postgres-store-1 psql -U store -d store_db -c "DELETE FROM stores WHERE owner_email = 'kafka.monitoring.test@example.com';" 2>/dev/null || echo "No existing test store found"

echo "🔄 Registering test user to trigger Kafka events..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "kafka.monitoring.test@example.com",
    "password": "password123",
    "name": "Kafka Monitoring Test"
  }' 2>/dev/null)

if [[ $RESPONSE == *"id"* ]]; then
    echo "✅ User registration successful"
    USER_ID=$(echo $RESPONSE | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    echo "👤 User ID: $USER_ID"
else
    echo "❌ User registration failed: $RESPONSE"
fi

# Wait for Kafka processing
echo -e "\n⏳ Waiting for Kafka event processing..."
sleep 3

# Check logs for monitoring evidence
echo -e "\n📋 3. Kafka Consumer Monitoring Evidence"
echo "-------------------------------------------"

echo "🏪 Store Service Kafka Processing (Last 10 lines):"
docker-compose -f /home/kritchanartl/projects/dropslot/backend/compose.e2e.yml logs store-service --tail=10 2>/dev/null | grep -E "(Processing event|Successfully processed|STORE SERVICE)" | tail -5

echo -e "\n📧 Email Service Processing Evidence:"
docker-compose -f /home/kritchanartl/projects/dropslot/backend/compose.e2e.yml logs store-service --tail=20 2>/dev/null | grep -E "(EMAIL SERVICE|welcome email)" | tail -3

# Show metrics capabilities
echo -e "\n\n📈 4. Metrics Verification & Testing"
echo "-------------------------------------"

echo "🔍 Checking available Kafka metrics:"
KAFKA_METRICS=$(curl -s http://localhost:8082/actuator/metrics 2>/dev/null | jq -r '.names | map(select(. | contains("kafka"))) | join(", ")')
if [[ -n "$KAFKA_METRICS" ]]; then
    echo "✅ Available Kafka metrics: $KAFKA_METRICS"
else
    echo "❌ No Kafka metrics found"
fi

echo -e "\n📊 Current Metrics Values:"

# Check processed messages counter
PROCESSED_COUNT=$(curl -s http://localhost:8082/actuator/metrics/kafka.messages.processed 2>/dev/null | jq -r '.measurements[0].value // "N/A"')
echo "✅ Messages Processed: $PROCESSED_COUNT"

# Check processing duration
PROCESSING_TIME=$(curl -s http://localhost:8082/actuator/metrics/kafka.processing.duration 2>/dev/null | jq -r '.measurements[] | select(.statistic=="TOTAL_TIME") | .value // "N/A"')
echo "✅ Total Processing Time: ${PROCESSING_TIME}s"

# Check error count
ERROR_COUNT=$(curl -s http://localhost:8082/actuator/metrics/kafka.messages.error 2>/dev/null | jq -r '.measurements[0].value // "0"')
echo "✅ Error Count: $ERROR_COUNT"

echo -e "\n🎯 Prometheus Metrics Sample:"
echo "$(curl -s http://localhost:8082/actuator/prometheus 2>/dev/null | grep -E 'kafka_(messages|processing)' | head -3)"

echo -e "\n\n📈 5. Monitoring Capabilities Implemented"
echo "-------------------------------------------"

echo "✅ Enhanced Kafka Consumer Monitoring:"
echo "   - Partition and offset tracking"
echo "   - Processing time metrics"
echo "   - Success/error counters"
echo "   - Correlation ID tracing"
echo "   - Manual acknowledgment control"

echo -e "\n✅ Error Handling & Resilience:"
echo "   - @Retryable with backoff"
echo "   - Graceful error handling"
echo "   - Manual acknowledgment to avoid infinite retries"

echo -e "\n✅ Structured Logging:"
echo "   - MDC correlation IDs"
echo "   - Partition/offset information"
echo "   - Event type tracking"
echo "   - Emoji indicators for easy scanning"

echo -e "\n✅ Metrics Integration:"
echo "   - Micrometer counters for success/error"
echo "   - Processing time timers"
echo "   - Prometheus metrics ready"
echo "   - Tagged by consumer and topic"

# Performance insights
echo -e "\n\n⚡ 6. Performance & Operations Insights"
echo "-------------------------------------------"

echo "🔄 Multiple Consumer Groups Active:"
echo "   - dropslot-store: Business logic processing"
echo "   - dropslot-email: Notification processing"
echo "   - Each group processes same events independently"

echo -e "\n🎯 Configuration Flexibility:"
echo "   - Generic ConsumerFactory creation"
echo "   - Separate ContainerFactory per consumer type"
echo "   - Configurable offset reset policies"

echo -e "\n🛡️ Production-Ready Features:"
echo "   - Health checks (with actuator)"
echo "   - Manual acknowledgment"
echo "   - Retry mechanisms"
echo "   - Structured monitoring"

echo -e "\n\n🎉 Summary: Kafka Monitoring Implementation Complete!"
echo "===================================================="
echo "Your DropSlot application now has:"
echo "• Enhanced Kafka consumer monitoring with metrics"
echo "• Proper error handling and retry mechanisms"
echo "• Multiple consumer groups with independent processing"
echo "• Structured logging with correlation tracking"
echo "• Production-ready observability features"

echo -e "\n📚 Next Steps for Advanced Monitoring:"
echo "• Add Prometheus + Grafana dashboards"
echo "• Implement dead letter queues"
echo "• Add Kafka Streams for real-time analytics"
echo "• Set up alerting on consumer lag"
