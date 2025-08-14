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

# Only use ECR data source (this works)
data "aws_ecr_repository" "app" {
  name = var.project_name
}

# Create initial task definition for pipeline to update
resource "aws_ecs_task_definition" "app" {
  family                   = var.project_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = "arn:aws:iam::964191654598:role/riderapp-ecs-execution-role"
  task_role_arn           = "arn:aws:iam::964191654598:role/riderapp-ecs-task-role"

  container_definitions = jsonencode([
    {
      name  = var.project_name
      image = var.image_uri
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]
      
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/${var.project_name}"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
      
      essential = true
    }
  ])
  
  tags = { Name = "${var.project_name}-task-definition" }
}

# Hardcode outputs since we can't read ECS resources
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