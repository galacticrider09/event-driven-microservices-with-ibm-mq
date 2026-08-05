# VPC
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = { Name = "order-saga-vpc" }
}

# Internet Gateway
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "order-saga-igw" }
}

data "aws_availability_zones" "available" {
  state = "available"
}

# Public Subnets (for ALB, ECS Fargate with public IPs, and EC2 IBM MQ)
resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index + 1)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true
  tags                    = { Name = "order-saga-public-subnet-${count.index + 1}" }
}

# Private Subnets (for RDS isolated databases)
resource "aws_subnet" "private_db" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 20)
  availability_zone = data.aws_availability_zones.available.names[count.index]
  tags              = { Name = "order-saga-private-db-subnet-${count.index + 1}" }
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
  tags = { Name = "order-saga-public-rt" }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ----------------- SECURITY GROUPS -----------------

# ALB Security Group
resource "aws_security_group" "alb" {
  name        = "order-saga-alb-sg"
  description = "Allow inbound HTTPS/HTTP"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ECS Security Groups (One for each service)
resource "aws_security_group" "ecs" {
  for_each = {
    producer     = var.app_port_producer
    inventory    = var.app_port_inventory
    payment      = var.app_port_payment
    notification = var.app_port_notification
  }

  name        = "order-saga-ecs-${each.key}-sg"
  description = "Security group for ${each.key} service"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Allow HTTP traffic from ALB"
    from_port       = each.value
    to_port         = each.value
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# IBM MQ Security Group
resource "aws_security_group" "mq" {
  name        = "order-saga-mq-sg"
  description = "Security group for IBM MQ on EC2"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "JMS connections from all ECS services"
    from_port   = 1414
    to_port     = 1414
    protocol    = "tcp"
    security_groups = [
      aws_security_group.ecs["producer"].id,
      aws_security_group.ecs["inventory"].id,
      aws_security_group.ecs["payment"].id,
      aws_security_group.ecs["notification"].id
    ]
  }

  ingress {
    description = "IBM MQ Admin Console"
    from_port   = 9443
    to_port     = 9443
    protocol    = "tcp"
    cidr_blocks = [var.your_ip]
  }

  ingress {
    description = "pgAdmin Web UI"
    from_port   = 5050
    to_port     = 5050
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.your_ip]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# RDS Security Groups (One for each DB)
resource "aws_security_group" "rds" {
  for_each = {
    producer     = aws_security_group.ecs["producer"].id
    inventory    = aws_security_group.ecs["inventory"].id
    payment      = aws_security_group.ecs["payment"].id
    notification = aws_security_group.ecs["notification"].id
  }

  name        = "order-saga-rds-${each.key}-sg"
  description = "Security group for ${each.key} database"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from ${each.key} service"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [each.value]
  }

  ingress {
    description     = "PostgreSQL from MQ Bastion host"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.mq.id]
  }
}
