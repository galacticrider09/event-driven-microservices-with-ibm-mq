locals {
  ecr_repos = toset(["producer", "inventory", "payment", "notification"])
}

resource "aws_ecr_repository" "app" {
  for_each = local.ecr_repos

  name                 = "order-saga-${each.key}"
  image_tag_mutability = "MUTABLE"

  force_delete = true # Useful for POC teardown

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = "order-saga-${each.key}-ecr" }
}

output "ecr_repository_urls" {
  description = "The URLs of the ECR repositories"
  value = {
    for k, v in aws_ecr_repository.app : k => v.repository_url
  }
}
