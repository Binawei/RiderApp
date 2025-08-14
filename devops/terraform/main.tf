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

# Check for existing infrastructure with error handling
data "aws_vpcs" "existing" {
  filter {
    name   = "tag:Name"
    values = ["${var.project_name}-vpc"]
  }
}

data "aws_vpc" "main" {
  count = length(data.aws_vpcs.existing.ids) > 0 ? 1 : 0
  id    = data.aws_vpcs.existing.ids[0]
}

data "aws_subnets" "existing" {
  count = length(data.aws_vpcs.existing.ids) > 0 ? 1 : 0
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main[0].id]
  }
  filter {
    name   = "tag:Name"
    values = ["${var.project_name}-public-*"]
  }
}

locals {
  vpc_exists = length(data.aws_vpcs.existing.ids) > 0
  vpc_id     = local.vpc_exists ? data.aws_vpc.main[0].id : aws_vpc.main[0].id
  subnet_ids = local.vpc_exists && length(data.aws_subnets.existing) > 0 ? data.aws_subnets.existing[0].ids : aws_subnet.public[*].id
}

# Create VPC only if it doesn't exist
resource "aws_vpc" "main" {
  count                = local.vpc_exists ? 0 : 1
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  count  = local.vpc_exists ? 0 : 1
  vpc_id = local.vpc_id
  
  tags = {
    Name = "${var.project_name}-igw"
  }
}

resource "aws_subnet" "public" {
  count = local.vpc_exists ? 0 : 2
  
  vpc_id                  = local.vpc_id
  cidr_block              = "10.0.${count.index + 1}.0/24"
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true
  
  tags = {
    Name = "${var.project_name}-public-subnet-${count.index + 1}"
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_route_table" "public" {
  count  = local.vpc_exists ? 0 : 1
  vpc_id = local.vpc_id
  
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main[0].id
  }
  
  tags = {
    Name = "${var.project_name}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  count = local.vpc_exists ? 0 : 2
  
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public[0].id
}

# Security Groups - always create new ones for updates
resource "aws_security_group" "alb" {
  name_prefix = "${var.project_name}-alb-"
  vpc_id      = local.vpc_id
  
  ingress {
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
  
  tags = {
    Name = "${var.project_name}-alb-sg"
  }
}

resource "aws_security_group" "ecs_tasks" {
  name_prefix = "${var.project_name}-ecs-tasks-"
  vpc_id      = local.vpc_id
  
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = "${var.project_name}-ecs-tasks-sg"
  }
}

# Use existing ALB as data source
data "aws_lb" "existing" {
  name = "${var.project_name}-alb"
}

# Use existing target group as data source
data "aws_lb_target_group" "fargate" {
  name = "${var.project_name}-fargate-tg"
}



# ECS Cluster - always manage
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"
  
  tags = {
    Name = "${var.project_name}-cluster"
  }
}

# Use existing log group as data source
data "aws_cloudwatch_log_group" "existing" {
  name = "/ecs/${var.project_name}"
}

# ECS Task Definition - always update
resource "aws_ecs_task_definition" "app" {
  family                   = var.project_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.ecs_task_execution_role.arn
  
  container_definitions = jsonencode([
    {
      name  = var.project_name
      image = var.image_uri
      
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
        }
      ]
      
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = data.aws_cloudwatch_log_group.existing.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
      
      essential = true
    }
  ])
  
  lifecycle {
    ignore_changes = [container_definitions]
  }
  
  tags = {
    Name = "${var.project_name}-task-definition"
  }
}

# ECS Service - always manage
resource "aws_ecs_service" "app" {
  name            = "${var.project_name}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  
  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = local.subnet_ids
    assign_public_ip = true
  }
  
  load_balancer {
    target_group_arn = data.aws_lb_target_group.fargate.arn
    container_name   = var.project_name
    container_port   = 8080
  }
  
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }
  
  tags = {
    Name = "${var.project_name}-service"
  }
}

# Use existing IAM role
data "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.project_name}-ecs-task-execution-role"
}

data "aws_ecr_repository" "app" {
  name = var.project_name
}

# Outputs
output "load_balancer_dns" {
  value = data.aws_lb.existing.dns_name
}

output "ecr_repository_url" {
  value = data.aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "database_endpoint" {
  value = null
}