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

# Use existing resources via data sources
data "aws_ecr_repository" "app" {
  name = var.project_name
}

data "aws_lb" "main" {
  name = "${var.project_name}-alb"
}

data "aws_ecs_cluster" "main" {
  cluster_name = "${var.project_name}-cluster"
}

# Try to get existing service, create if not found
data "aws_ecs_service" "app" {
  service_name = "${var.project_name}-service"
  cluster_arn  = data.aws_ecs_cluster.main.arn
}

# Outputs for the pipeline
output "load_balancer_dns" {
  value = data.aws_lb.main.dns_name
}

output "ecr_repository_url" {
  value = data.aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = data.aws_ecs_cluster.main.cluster_name
}

output "ecs_service_name" {
  value = data.aws_ecs_service.app.service_name
}

output "database_endpoint" {
  value = null
}