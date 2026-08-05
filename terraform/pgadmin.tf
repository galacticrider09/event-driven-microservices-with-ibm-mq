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
    yum update -y && yum install -y docker
    systemctl enable docker && systemctl start docker

    # Fetch MQ password from Parameter Store (used for pgadmin login)
    MQ_ADMIN_PASS=$(aws ssm get-parameter --name "/order-saga/MQ_ADMIN_PASSWORD" --with-decryption --region ${var.aws_region} --query "Parameter.Value" --output text)

    # Run pgAdmin
    docker run -d --name pgadmin --restart unless-stopped \
      -e PGADMIN_DEFAULT_EMAIL=admin@ordersaga.com \
      -e PGADMIN_DEFAULT_PASSWORD=$MQ_ADMIN_PASS \
      -p 5050:80 \
      dpage/pgadmin4:latest
  EOF

  tags = { Name = "order-saga-pgadmin" }
}
