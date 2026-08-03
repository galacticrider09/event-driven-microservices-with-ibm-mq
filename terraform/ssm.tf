# These act as placeholders. In a real environment, you'd populate the values 
# manually or via a separate script to avoid keeping secrets in Terraform state.
# Here we use dummy values so ECS tasks can start up.

resource "aws_ssm_parameter" "postgres_user" {
  name  = "/order-saga/POSTGRES_USER"
  type  = "SecureString"
  value = "postgres"
  lifecycle { ignore_changes = [value] }
}

resource "aws_ssm_parameter" "postgres_password" {
  name  = "/order-saga/POSTGRES_PASSWORD"
  type  = "SecureString"
  value = "postgres"
  lifecycle { ignore_changes = [value] }
}

resource "aws_ssm_parameter" "mq_user" {
  name  = "/order-saga/MQ_USER"
  type  = "String"
  value = "app"
  lifecycle { ignore_changes = [value] }
}

resource "aws_ssm_parameter" "mq_password" {
  name  = "/order-saga/MQ_PASSWORD"
  type  = "SecureString"
  value = "passw0rd"
  lifecycle { ignore_changes = [value] }
}

resource "aws_ssm_parameter" "mq_admin_password" {
  name  = "/order-saga/MQ_ADMIN_PASSWORD"
  type  = "SecureString"
  value = "passw0rd"
  lifecycle { ignore_changes = [value] }
}
