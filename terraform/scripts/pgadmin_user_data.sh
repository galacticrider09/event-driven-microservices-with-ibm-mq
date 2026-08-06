#!/bin/bash
set -x
exec > /var/log/user-data.log 2>&1

# 1. Create 2GB swap file FIRST to prevent OOM on t3.micro
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
fi

# 2. Install and start Docker
yum install -y docker
systemctl enable docker && systemctl start docker

# 3. Fetch admin password from Parameter Store
MQ_ADMIN_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_ADMIN_PASSWORD" --with-decryption --region ${aws_region} --query "Parameter.Value" --output text)

# 4. Run pgAdmin container
docker run -d --name pgadmin --restart unless-stopped \
  -e PGADMIN_DEFAULT_EMAIL=admin@ordersaga.com \
  -e PGADMIN_DEFAULT_PASSWORD="$MQ_ADMIN_PASS" \
  -p 5050:80 \
  dpage/pgadmin4:latest
