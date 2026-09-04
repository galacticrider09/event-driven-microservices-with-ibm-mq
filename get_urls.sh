#!/bin/bash

# Change to the terraform directory to get outputs
cd terraform || exit 1

# Fetch dynamic values from Terraform outputs
ALB_DOMAIN=$(terraform output -raw alb_dns_name)
ALB_URL="http://$ALB_DOMAIN"
MQ_IP=$(terraform output -raw mq_public_ip)
PGADMIN_IP=$(terraform output -raw pgadmin_public_ip)

echo "==========================================="
echo "   DYNAMIC SAGA PATTERN URLS"
echo "==========================================="

echo -e "\n1. PRODUCER SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/producer/health"
echo "Swagger UI   : $ALB_URL/api/producer/swagger-ui/index.html"

echo -e "\n2. INVENTORY SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/inventory/health"
echo "Swagger UI   : $ALB_URL/api/inventory/swagger-ui/index.html"

echo -e "\n3. PAYMENT SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/payment/health"
echo "Swagger UI   : $ALB_URL/api/payment/swagger-ui/index.html"

echo -e "\n4. NOTIFICATION SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/notification/health"
echo "Swagger UI   : $ALB_URL/api/notification/swagger-ui/index.html"

echo -e "\n==========================================="
echo "   INFRASTRUCTURE URLS"
echo "==========================================="
echo "IBM MQ Console: https://$MQ_IP:9443/ibmmq/console/"
echo "IBM MQ Login  : admin / passw0rd (systems manager)"
echo ""
echo "==========================================="
echo "   DATABASE ACCESS (pgAdmin UI)"
echo "==========================================="
echo "pgAdmin Web UI: http://$PGADMIN_IP:5050"
echo "Login Email   : admin@ordersaga.com"
echo "Login Password: POSTGRES_PASSWORD (check systems manager) "
echo ""
echo "Database Hostname (single shared RDS instance — add ONE server in pgAdmin):"
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)

echo "- RDS Host        : ${RDS_ENDPOINT%:*}"
echo "- Port            : 5432"
echo "- Username        : postgres"
echo "- Password        : postgres (check systems manager)"
echo ""
echo "Databases inside this instance:"
echo "  • order_db        (Producer Service)"
echo "  • inventory_db    (Inventory Service)"
echo "  • payment_db      (Payment Service)"
echo "  • notification_db (Notification Service)"
echo "==========================================="

