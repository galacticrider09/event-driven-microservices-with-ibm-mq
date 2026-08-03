resource "aws_db_subnet_group" "rds" {
  name       = "order-saga-db-subnet-group"
  subnet_ids = [for s in aws_subnet.private_db : s.id]
  tags       = { Name = "order-saga-db-subnet-group" }
}

locals {
  databases = toset(["producer", "inventory", "payment", "notification"])
}

resource "aws_db_instance" "rds" {
  for_each = local.databases

  identifier        = "order-saga-${each.key}-db"
  allocated_storage = 20
  storage_type      = "gp3"
  engine            = "postgres"
  engine_version    = "15"
  instance_class    = "db.t3.micro"

  db_name  = "${each.key}_db"
  username = aws_ssm_parameter.postgres_user.value
  password = aws_ssm_parameter.postgres_password.value

  db_subnet_group_name   = aws_db_subnet_group.rds.name
  vpc_security_group_ids = [aws_security_group.rds[each.key].id]

  skip_final_snapshot = true
  multi_az            = false
  publicly_accessible = false
  storage_encrypted   = true

  backup_retention_period = 0

  tags = { Name = "order-saga-${each.key}-db" }
}
