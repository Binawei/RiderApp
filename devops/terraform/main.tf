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

# Check for existing infrastructure
data "aws_vpc" "existing" {
  filter {
    name   = "tag:Name"
    values = ["${var.project_name}-vpc"]
  }
}

data "aws_subnets" "existing" {
  filter {
    name   = "vpc-id"
    values = [local.vpc_id]
  }
  filter {
    name   = "tag:Name"
    values = ["${var.project_name}-public-*"]
  }
}

data "aws_lb" "existing" {
  name = "${var.project_name}-alb"
}

data "aws_ecs_cluster" "existing" {
  cluster_name = "${var.project_name}-cluster"
}

locals {
  vpc_exists     = try(data.aws_vpc.existing.id, null) != null
  cluster_exists = try(data.aws_ecs_cluster.existing.arn, null) != null
  alb_exists     = try(data.aws_lb.existing.arn, null) != null
  vpc_id         = local.vpc_exists ? data.aws_vpc.existing.id : aws_vpc.main[0].id
  subnet_ids     = length(data.aws_subnets.existing.ids) > 0 ? data.aws_subnets.existing.ids : aws_subnet.public[*].id
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

# ALB - create only if doesn't exist
resource "aws_lb" "main" {
  count              = local.alb_exists ? 0 : 1
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = local.subnet_ids
  
  tags = {
    Name = "${var.project_name}-alb"
  }
}

resource "aws_lb_target_group" "app" {
  name        = "${var.project_name}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = local.vpc_id
  target_type = "ip"
  
  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 2
  }
  
  tags = {
    Name = "${var.project_name}-tg"
  }
}

resource "aws_lb_listener" "app" {
  count             = local.alb_exists ? 0 : 1
  load_balancer_arn = local.alb_exists ? data.aws_lb.existing.arn : aws_lb.main[0].arn
  port              = "80"
  protocol          = "HTTP"
  
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

# ECS Cluster - create only if doesn't exist
resource "aws_ecs_cluster" "main" {
  count = local.cluster_exists ? 0 : 1
  name  = "${var.project_name}-cluster"
  
  tags = {
    Name = "${var.project_name}-cluster"
  }
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 7
  
  tags = {
    Name = "${var.project_name}-logs"
  }
}

# ECS Task Definition - always update
resource "aws_ecs_task_definition" "app" {
  family                   = var.project_name
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  
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
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
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

# ECS Service - create only if doesn't exist
resource "aws_ecs_service" "app" {
  name            = "${var.project_name}-service"
  cluster         = local.cluster_exists ? data.aws_ecs_cluster.existing.arn : aws_ecs_cluster.main[0].id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  
  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = local.subnet_ids
    assign_public_ip = true
  }
  
  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = var.project_name
    container_port   = 8080
  }
  
  depends_on = [aws_lb_listener.app]
  
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }
  
  tags = {
    Name = "${var.project_name}-service"
  }
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.project_name}-ecs-task-execution-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_ecr_repository" "app" {
  name = var.project_name
}

# Outputs
output "load_balancer_dns" {
  value = local.alb_exists ? data.aws_lb.existing.dns_name : aws_lb.main[0].dns_name
}

output "ecr_repository_url" {
  value = data.aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = local.cluster_exists ? data.aws_ecs_cluster.existing.cluster_name : aws_ecs_cluster.main[0].name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "database_endpoint" {
  value = null
}