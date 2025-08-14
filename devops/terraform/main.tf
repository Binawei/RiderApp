variable "project_name" {
  type = string
}

variable "app_type" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "enable_database" {
  type    = bool
  default = false
}

variable "database_type" {
  type    = string
  default = "postgres"
}

variable "database_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "image_uri" {
  type        = string
  description = "Docker image URI for ECS task"
}

provider "aws" {
  region = var.aws_region
}

# Onnly use ECR data source
data "aws_ecr_repository" "app" {
  name = var.project_name
}

# Hardcode all values - let pipeline handle ECS entirely
locals {
  load_balancer_dns = "riderapp-alb-305274337.us-east-1.elb.amazonaws.com"
  ecs_cluster_name  = "${var.project_name}-cluster"
  ecs_service_name  = "${var.project_name}-service"
}

# Outputs for the pipeline
output "load_balancer_dns" {
  value = local.load_balancer_dns
}

output "ecr_repository_url" {
  value = data.aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = local.ecs_cluster_name
}

output "ecs_service_name" {
  value = local.ecs_service_name
}

output "database_endpoint" {
  value = null
}