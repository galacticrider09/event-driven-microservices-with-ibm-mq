variable "aws_region" {
  description = "AWS region for the deployment"
  type        = string
  default     = "ap-south-1"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "your_ip" {
  description = "Your IP address for SSH and IBM MQ Admin Console access (e.g. 203.0.113.1/32)"
  type        = string
  default     = "0.0.0.0/0" # Change to your actual IP for better security
}

variable "key_name" {
  description = "Name of the EC2 Key Pair for SSH access to the IBM MQ instance"
  type        = string
  default     = "poc-key-pair"
}

variable "app_port_producer" { default = 8080 }
variable "app_port_inventory" { default = 8081 }
variable "app_port_payment" { default = 8082 }
variable "app_port_notification" { default = 8083 }
