# Application Load Balancer
resource "aws_lb" "main" {
  name               = "order-saga-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [for s in aws_subnet.public : s.id]

  enable_deletion_protection = false

  tags = { Name = "order-saga-alb" }
}

# HTTP Listener (Redirect to HTTPS or serve HTTP for POC)
# Note: Since POC doesn't include an ACM cert by default, we'll configure HTTP.
# To use HTTPS, you would need to provision an ACM cert first.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "text/plain"
      message_body = "Order Saga API"
      status_code  = "404"
    }
  }
}

# Target Groups
resource "aws_lb_target_group" "ecs" {
  for_each = {
    producer     = var.app_port_producer
    inventory    = var.app_port_inventory
    payment      = var.app_port_payment
    notification = var.app_port_notification
  }

  name        = "tg-${each.key}"
  port        = each.value
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/api/${each.key}/health"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200"
  }
}

# Listener Rules
resource "aws_lb_listener_rule" "api_rules" {
  for_each = {
    producer     = { priority = 10, path = "/api/producer/*" }
    inventory    = { priority = 20, path = "/api/inventory/*" }
    payment      = { priority = 30, path = "/api/payment/*" }
    notification = { priority = 40, path = "/api/notification/*" }
  }

  listener_arn = aws_lb_listener.http.arn
  priority     = each.value.priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ecs[each.key].arn
  }

  condition {
    path_pattern {
      values = [each.value.path]
    }
  }
}
