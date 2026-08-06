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

# 3. Mount EBS for IBM MQ persistence (Idempotent)
blkid /dev/xvdb || mkfs -t xfs /dev/xvdb
mkdir -p /mnt/mqm
if ! mountpoint -q /mnt/mqm; then
  mount /dev/xvdb /mnt/mqm
  grep -q '/mnt/mqm' /etc/fstab || echo '/dev/xvdb /mnt/mqm xfs defaults,nofail 0 2' >> /etc/fstab
fi
mkdir -p /mnt/mqm/data
chown -R 1001:1001 /mnt/mqm

# 4. Fetch MQ passwords from Parameter Store
MQ_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_PASSWORD" --with-decryption --region ${aws_region} --query "Parameter.Value" --output text)
MQ_ADMIN_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_ADMIN_PASSWORD" --with-decryption --region ${aws_region} --query "Parameter.Value" --output text)

# 5. Run IBM MQ container
docker run -d --name ibm-mq --restart unless-stopped \
  -e LICENSE=accept \
  -e MQ_QMGR_NAME=QM1 \
  -e MQ_APP_PASSWORD="$MQ_PASS" \
  -e MQ_ADMIN_PASSWORD="$MQ_ADMIN_PASS" \
  -p 1414:1414 -p 9443:9443 \
  -v /mnt/mqm:/mnt/mqm \
  icr.io/ibm-messaging/mq:latest

# 6. Wait for MQ to start then create queues
cat << 'MQSC' > /tmp/queues.mqsc
${mqsc_content}
MQSC

(
  until docker exec ibm-mq dspmq | grep -qi "status(running)"; do
    sleep 5
  done
  docker exec -i ibm-mq runmqsc QM1 < /tmp/queues.mqsc
) &
