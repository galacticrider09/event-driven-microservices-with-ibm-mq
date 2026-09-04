# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "order-saga-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    base              = 1
    weight            = 100
    capacity_provider = "FARGATE"
  }
}

# CloudWatch Log Groups for ECS
resource "aws_cloudwatch_log_group" "ecs" {
  for_each = {
    producer     = "producer"
    inventory    = "inventory"
    payment      = "payment"
    notification = "notification"
  }

  name              = "/ecs/order-saga-${each.key}"
  retention_in_days = 7
}

# Task Definitions
locals {
  services = {
    producer = {
      cpu    = 512
      memory = 1024
      port   = var.app_port_producer
    }
    inventory = {
      cpu    = 512
      memory = 1024
      port   = var.app_port_inventory
    }
    payment = {
      cpu    = 256
      memory = 512
      port   = var.app_port_payment
    }
    notification = {
      cpu    = 256
      memory = 512
      port   = var.app_port_notification
    }
  }
}

resource "aws_ecs_task_definition" "app" {
  for_each = local.services

  family                   = "order-saga-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = each.value.cpu
  memory                   = each.value.memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  # For POC, we'll use a placeholder image if ECR images aren't pushed yet.
  # Once images are pushed via CI/CD, this will be replaced.
  # To prevent Terraform from overwriting CI/CD updates, we can ignore image changes.
  container_definitions = jsonencode([
    {
      name      = "order-saga-${each.key}"
      image     = "${aws_ecr_repository.app[each.key].repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = each.value.port
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "MQ_HOST", value = aws_instance.mq.private_ip },
        { name = "DB_HOST", value = aws_db_instance.rds.address },
        { name = "DB_PORT", value = "5432" }
      ]

      secrets = [
        { name = "POSTGRES_USER", valueFrom = aws_ssm_parameter.postgres_user.arn },
        { name = "POSTGRES_PASSWORD", valueFrom = aws_ssm_parameter.postgres_password.arn },
        { name = "MQ_USER", valueFrom = aws_ssm_parameter.mq_user.arn },
        { name = "MQ_PASSWORD", valueFrom = aws_ssm_parameter.mq_password.arn }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs[each.key].name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

# ECS Services
resource "aws_ecs_service" "app" {
  for_each = local.services

  name            = "order-saga-${each.key}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [for s in aws_subnet.public : s.id]
    security_groups  = [aws_security_group.ecs[each.key].id]
    assign_public_ip = true # Crucial for NO NAT Gateway setup
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.ecs[each.key].arn
    container_name   = "order-saga-${each.key}"
    container_port   = each.value.port
  }

  health_check_grace_period_seconds = 300

  depends_on = [
    aws_lb_listener.http
  ]
}
