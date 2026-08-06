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

  user_data = templatefile("${path.module}/scripts/pgadmin_user_data.sh", {
    aws_region = var.aws_region
  })

  tags = { Name = "order-saga-pgadmin" }
}
