#!/bin/bash

# Change to the terraform directory to get outputs
cd terraform || exit 1

# Fetch dynamic values from Terraform outputs
ALB_DOMAIN=$(terraform output -raw alb_dns_name)
ALB_URL="http://$ALB_DOMAIN"
MQ_IP=$(terraform output -raw mq_public_ip)

echo "==========================================="
echo "   DYNAMIC SAGA PATTERN URLS"
echo "==========================================="

echo -e "\n1. PRODUCER SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/producer/health"
echo "Send Order   : $ALB_URL/api/producer/send?product=Laptop&quantity=2"
echo "All Orders   : $ALB_URL/api/producer/orders"
echo "Swagger UI   : $ALB_URL/api/producer/swagger-ui/index.html"
echo "API Docs     : $ALB_URL/api/producer/v3/api-docs"

echo -e "\n2. INVENTORY SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/inventory/health"
echo "Catalog      : $ALB_URL/api/inventory/catalog"
echo "Inventory    : $ALB_URL/api/inventory/records"
echo "Swagger UI   : $ALB_URL/api/inventory/swagger-ui/index.html"
echo "API Docs     : $ALB_URL/api/inventory/v3/api-docs"

echo -e "\n3. PAYMENT SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/payment/health"
echo "Transactions : $ALB_URL/api/payment/transactions"
echo "Swagger UI   : $ALB_URL/api/payment/swagger-ui/index.html"
echo "API Docs     : $ALB_URL/api/payment/v3/api-docs"

echo -e "\n4. NOTIFICATION SERVICE"
echo "-----------------------------------"
echo "Health Check : $ALB_URL/api/notification/health"
echo "Notifications: $ALB_URL/api/notification/records"
echo "Swagger UI   : $ALB_URL/api/notification/swagger-ui/index.html"
echo "API Docs     : $ALB_URL/api/notification/v3/api-docs"

echo -e "\n==========================================="
echo "   INFRASTRUCTURE URLS"
echo "==========================================="
echo "IBM MQ Console: https://$MQ_IP:9443/ibmmq/console/"
echo "IBM MQ Login  : admin / passw0rd (systems manager)"
echo ""
echo "==========================================="
echo "   DATABASE ACCESS (pgAdmin UI)"
echo "==========================================="
echo "pgAdmin Web UI: http://$MQ_IP:5050"
echo "Login Email   : admin@ordersaga.com"
echo "Login Password: MQ_ADMIN_PASSWORD - check systems manager"
echo ""
echo "Database Hostnames (to enter when adding a server in pgAdmin):"
PRODUCER_DB=$(terraform output -json rds_endpoints | jq -r '.producer')
INVENTORY_DB=$(terraform output -json rds_endpoints | jq -r '.inventory')
PAYMENT_DB=$(terraform output -json rds_endpoints | jq -r '.payment')
NOTIFICATION_DB=$(terraform output -json rds_endpoints | jq -r '.notification')

echo "- Producer DB     : ${PRODUCER_DB%:*}"
echo "- Inventory DB    : ${INVENTORY_DB%:*}"
echo "- Payment DB      : ${PAYMENT_DB%:*}"
echo "- Notification DB : ${NOTIFICATION_DB%:*}"
echo ""
echo "(Port is 5432, Username is postgres, Password is postgres)"
echo "==========================================="
