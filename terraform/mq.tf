data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

resource "aws_instance" "mq" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.micro"

  # Deploy in public subnet for simple access and no NAT
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.mq.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_mq.name

  # Ensure you create this key pair in AWS if you want SSH access
  # key_name = var.key_name 

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
    encrypted   = true
  }

  ebs_block_device {
    device_name = "/dev/xvdb"
    volume_size = 10
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = <<-EOF
    #!/bin/bash
    yum update -y && yum install -y docker
    systemctl enable docker && systemctl start docker

    # Create 2GB swap file to prevent OOM on t3.micro
    dd if=/dev/zero of=/swapfile bs=1M count=2048
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile swap swap defaults 0 0' >> /etc/fstab

    # Mount EBS for IBM MQ persistence
    mkfs -t xfs /dev/xvdb
    mkdir -p /mnt/mqm
    mount /dev/xvdb /mnt/mqm
    echo '/dev/xvdb /mnt/mqm xfs defaults,nofail 0 2' >> /etc/fstab
    mkdir -p /mnt/mqm/data
    chown -R 1001:1001 /mnt/mqm

    # Fetch MQ password from Parameter Store
    MQ_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_PASSWORD" --with-decryption --region ${var.aws_region} --query "Parameter.Value" --output text)
    MQ_ADMIN_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_ADMIN_PASSWORD" --with-decryption --region ${var.aws_region} --query "Parameter.Value" --output text)

    # Run IBM MQ
    docker run -d --name ibm-mq --restart unless-stopped \
      -e LICENSE=accept -e MQ_QMGR_NAME=QM1 \
      -e MQ_APP_PASSWORD=$MQ_PASS \
      -e MQ_ADMIN_PASSWORD=$MQ_ADMIN_PASS \
      -p 1414:1414 -p 9443:9443 \
      -v /mnt/mqm:/mnt/mqm \
      icr.io/ibm-messaging/mq:latest

    # Wait for MQ to start and create queues automatically
    cat << 'MQSC' > /tmp/queues.mqsc
DEFINE QLOCAL(DEV.ORDER.QUEUE) REPLACE
DEFINE QLOCAL(DEV.ORDER.FAILED.QUEUE) REPLACE
DEFINE QLOCAL(DEV.ORDER.COMPLETED.QUEUE) REPLACE
DEFINE QLOCAL(DEV.PAYMENT.QUEUE) REPLACE
DEFINE QLOCAL(DEV.NOTIFICATION.QUEUE) REPLACE
DEFINE QLOCAL(DEV.INVENTORY.COMPENSATE.QUEUE) REPLACE
MQSC

    (
      until docker exec ibm-mq dspmq | grep -qi "status(running)"; do
        sleep 5
      done
      docker exec -i ibm-mq runmqsc QM1 < /tmp/queues.mqsc
    ) &

  EOF

  tags = { Name = "order-saga-mq" }
}
