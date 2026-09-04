resource "aws_instance" "pgadmin" {
  ami           = data.aws_ami.amazon_linux_2023.id
  instance_type = "t3.micro"

  # Deploy in public subnet for simple access and no NAT
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.pgadmin.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_mq.name

  root_block_device {
    volume_size = 8
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = <<-EOF
    #!/bin/bash
    set -x

    # 1. Create 2GB swap file FIRST to prevent OOM
    if [ ! -f /swapfile ]; then
      dd if=/dev/zero of=/swapfile bs=1M count=2048
      chmod 600 /swapfile
      mkswap /swapfile
      swapon /swapfile
      echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
    fi

    # 2. Install Docker and PostgreSQL client tools
    yum install -y docker postgresql15
    systemctl enable docker && systemctl start docker

    # 3. Fetch credentials from Parameter Store
    REGION="${var.aws_region}"
    MQ_ADMIN_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_ADMIN_PASSWORD" --with-decryption --region $REGION --query "Parameter.Value" --output text)
    PG_USER=$(aws ssm get-parameter --name "/order-saga/POSTGRES_USER" --with-decryption --region $REGION --query "Parameter.Value" --output text)
    PG_PASS=$(aws ssm get-parameter --name "/order-saga/POSTGRES_PASSWORD" --with-decryption --region $REGION --query "Parameter.Value" --output text)

    # 4. Create the 4 logical databases on the single RDS instance
    #    order_db is already created by RDS as the default db, so we create the other 3.
    RDS_HOST="${aws_db_instance.rds.address}"
    export PGPASSWORD="$PG_PASS"

    for DB_NAME in inventory_db payment_db notification_db; do
      psql -h "$RDS_HOST" -U "$PG_USER" -d order_db -tc \
        "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1 \
        || psql -h "$RDS_HOST" -U "$PG_USER" -d order_db -c "CREATE DATABASE $DB_NAME;"
    done

    unset PGPASSWORD

    # 5. Run pgAdmin
    docker run -d --name pgadmin --restart unless-stopped \
      -e PGADMIN_DEFAULT_EMAIL=admin@ordersaga.com \
      -e PGADMIN_DEFAULT_PASSWORD=$MQ_ADMIN_PASS \
      -p 5050:80 \
      dpage/pgadmin4:latest
  EOF

  depends_on = [aws_db_instance.rds]

  tags = { Name = "order-saga-pgadmin" }
}
