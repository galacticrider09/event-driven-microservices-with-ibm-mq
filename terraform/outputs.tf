output "alb_dns_name" {
  description = "The DNS name of the ALB"
  value       = aws_lb.main.dns_name
}

output "mq_private_ip" {
  description = "The internal IP address of the IBM MQ instance"
  value       = aws_instance.mq.private_ip
}

output "mq_public_ip" {
  description = "The public IP address of the IBM MQ instance (for Admin Console access)"
  value       = aws_instance.mq.public_ip
}

output "pgadmin_public_ip" {
  description = "The public IP address of the pgAdmin instance"
  value       = aws_instance.pgadmin.public_ip
}

output "rds_endpoints" {
  description = "Endpoints for all RDS instances"
  value = {
    for k, v in aws_db_instance.rds : k => v.endpoint
  }
}

output "instructions" {
  value = <<EOF
Terraform has provisioned the infrastructure.

Next steps:
1. Ensure your CI/CD pipeline builds the Docker images and pushes them to ECR.
2. Update the ECS Task Definitions via the pipeline with the correct ECR image URIs.
3. Access the APIs using the ALB DNS Name: http://${aws_lb.main.dns_name}/api/...
4. Access IBM MQ Admin Console at: https://${aws_instance.mq.public_ip}:9443
5. Access pgAdmin Web UI at: http://${aws_instance.pgadmin.public_ip}:5050
EOF
}
