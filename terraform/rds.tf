resource "aws_db_subnet_group" "rds" {
  name       = "order-saga-db-subnet-group"
  subnet_ids = [for s in aws_subnet.private_db : s.id]
  tags       = { Name = "order-saga-db-subnet-group" }
}

resource "aws_db_instance" "rds" {
  identifier        = "order-saga-db"
  allocated_storage = 20
  storage_type      = "gp3"
  engine            = "postgres"
  engine_version    = "15"
  instance_class    = "db.t3.micro"

  db_name  = "order_db"
  username = aws_ssm_parameter.postgres_user.value
  password = aws_ssm_parameter.postgres_password.value

  db_subnet_group_name   = aws_db_subnet_group.rds.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  skip_final_snapshot = true
  multi_az            = false
  publicly_accessible = false
  storage_encrypted   = true

  backup_retention_period = 0

  tags = { Name = "order-saga-db" }
}
