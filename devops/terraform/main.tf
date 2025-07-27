variable "project_name" { type = string }
variable "app_type" { type = string }
variable "aws_region" { type = string }
variable "instance_type" { type = string }
variable "min_instances" { type = number }
variable "max_instances" { type = number }
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

provider "aws" {
  region = var.aws_region
}

# Try to get existing ECR repository, create if not found
data "aws_ecr_repository" "existing_app" {
  name = var.project_name
}

locals {
  ecr_repository_url = data.aws_ecr_repository.existing_app.repository_url
}

# Use Default VPC (avoids VPC limit)
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Use existing default VPC resources
locals {
  vpc_id     = data.aws_vpc.default.id
  subnet_ids = data.aws_subnets.default.ids
}

# Security Groups
resource "aws_security_group" "app" {
  name_prefix = "${var.project_name}-app-"
  vpc_id      = local.vpc_id

  dynamic "ingress" {
    for_each = var.app_type == "react-frontend" ? [80] : [8080]
    content {
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Check if Load Balancer exists
data "aws_lb" "existing_main" {
  count = 1
  name  = "${var.project_name}-alb"
}

# Create Load Balancer only if it doesn't exist
resource "aws_lb" "main" {
  count              = length(data.aws_lb.existing_main) == 0 ? 1 : 0
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = local.subnet_ids
}

# Use existing or created Load Balancer
locals {
  load_balancer_arn = length(data.aws_lb.existing_main) > 0 ? data.aws_lb.existing_main[0].arn : aws_lb.main[0].arn
  load_balancer_dns = length(data.aws_lb.existing_main) > 0 ? data.aws_lb.existing_main[0].dns_name : aws_lb.main[0].dns_name
}

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
}

# Check if IAM Role exists
data "aws_iam_role" "existing_ec2_role" {
  count = 1
  name  = "${var.project_name}-ec2-role"
}

# Create IAM Role only if it doesn't exist
resource "aws_iam_role" "ec2_role" {
  count = length(data.aws_iam_role.existing_ec2_role) == 0 ? 1 : 0
  name  = "${var.project_name}-ec2-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "parameter_store_policy" {
  count = length(data.aws_iam_role.existing_ec2_role) == 0 ? 1 : 0
  name  = "${var.project_name}-parameter-store-policy"
  role  = aws_iam_role.ec2_role[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath",
        "ssm:UpdateInstanceInformation",
        "ssm:SendCommand",
        "ec2messages:*",
        "ssmmessages:*",
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ]
      Resource = "*"
    }]
  })
}

resource "aws_iam_instance_profile" "ec2_profile" {
  count = length(data.aws_iam_role.existing_ec2_role) == 0 ? 1 : 0
  name  = "${var.project_name}-ec2-profile"
  role  = length(data.aws_iam_role.existing_ec2_role) > 0 ? data.aws_iam_role.existing_ec2_role[0].name : aws_iam_role.ec2_role[0].name
}

# Use existing or created IAM Instance Profile
locals {
  instance_profile_name = length(data.aws_iam_role.existing_ec2_role) > 0 ? "${var.project_name}-ec2-profile" : aws_iam_instance_profile.ec2_profile[0].name
}

# Check if Auto Scaling Group exists
data "aws_autoscaling_group" "existing_app" {
  count = 1
  name  = "${var.project_name}-asg"
}

# Launch Template
resource "aws_launch_template" "app" {
  count         = length(data.aws_autoscaling_group.existing_app) == 0 ? 1 : 0
  name_prefix   = "${var.project_name}-"
  image_id      = data.aws_ami.amazon_linux.id
  instance_type = var.instance_type

  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile {
    name = local.instance_profile_name
  }

  user_data = base64encode(templatefile("${path.module}/user_data.sh", {
    project_name = var.project_name
    app_type     = var.app_type
    aws_region   = var.aws_region
    enable_database = var.enable_database
    ecr_registry = local.ecr_repository_url
  }))
}

# Create Auto Scaling Group only if it doesn't exist
resource "aws_autoscaling_group" "app" {
  count               = length(data.aws_autoscaling_group.existing_app) == 0 ? 1 : 0
  name                = "${var.project_name}-asg"
  vpc_zone_identifier = local.subnet_ids
  target_group_arns   = [data.aws_lb_target_group.app.arn]
  health_check_type   = "EC2"
  health_check_grace_period = 300
  wait_for_capacity_timeout = "15m"

  min_size         = 1
  max_size         = var.max_instances
  desired_capacity = 1

  launch_template {
    id      = aws_launch_template.app[0].id
    version = "$Latest"
  }
}

# Target Group (use existing)
data "aws_lb_target_group" "app" {
  name = "${var.project_name}-tg"
}

resource "aws_lb_listener" "app" {
  load_balancer_arn = local.load_balancer_arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = data.aws_lb_target_group.app.arn
  }
}

# Data sources
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

# Check if DB Subnet Group exists
data "aws_db_subnet_group" "existing_main" {
  count = var.enable_database ? 1 : 0
  name  = "${var.project_name}-db-subnet-group-new"
}

# Create DB Subnet Group only if it doesn't exist
resource "aws_db_subnet_group" "main" {
  count      = var.enable_database && length(data.aws_db_subnet_group.existing_main) == 0 ? 1 : 0
  name       = "${var.project_name}-db-subnet-group-new"
  subnet_ids = local.subnet_ids
  tags = { Name = "${var.project_name}-db-subnet-group-new" }
}

# Use existing or created DB Subnet Group
locals {
  db_subnet_group_name = var.enable_database ? (
    length(data.aws_db_subnet_group.existing_main) > 0 ? 
    data.aws_db_subnet_group.existing_main[0].name : 
    aws_db_subnet_group.main[0].name
  ) : null
}

# Database Security Group
resource "aws_security_group" "database" {
  count       = var.enable_database ? 1 : 0
  name_prefix = "${var.project_name}-db-"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = var.database_type == "postgres" ? 5432 : 3306
    to_port         = var.database_type == "postgres" ? 5432 : 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Check if RDS instance exists
data "aws_db_instance" "existing_main" {
  count                  = var.enable_database ? 1 : 0
  db_instance_identifier = "${var.project_name}-db"
}

# Create RDS Instance only if it doesn't exist
resource "aws_db_instance" "main" {
  count                  = var.enable_database && length(data.aws_db_instance.existing_main) == 0 ? 1 : 0
  identifier             = "${var.project_name}-db"
  engine                 = var.database_type == "postgres" ? "postgres" : "mysql"
  engine_version         = var.database_type == "postgres" ? "15.7" : "8.0"
  instance_class         = var.database_instance_class
  allocated_storage      = 20
  max_allocated_storage  = 100
  storage_encrypted      = true
  
  db_name  = replace(var.project_name, "-", "")
  username = "dbadmin"
  password = random_password.db_password[0].result
  
  vpc_security_group_ids = [aws_security_group.database[0].id]
  db_subnet_group_name   = local.db_subnet_group_name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = true
  deletion_protection = false
  
  tags = { Name = "${var.project_name}-database" }
}

# Database Password
resource "random_password" "db_password" {
  count   = var.enable_database ? 1 : 0
  length  = 16
  special = true
}

# Use existing or created RDS instance
locals {
  db_endpoint = var.enable_database ? (
    length(data.aws_db_instance.existing_main) > 0 ? 
    data.aws_db_instance.existing_main[0].endpoint : 
    aws_db_instance.main[0].endpoint
  ) : null
  
  db_name = var.enable_database ? (
    length(data.aws_db_instance.existing_main) > 0 ? 
    data.aws_db_instance.existing_main[0].db_name : 
    aws_db_instance.main[0].db_name
  ) : null
}

# Store DB credentials in Parameter Store
resource "aws_ssm_parameter" "db_host" {
  count     = var.enable_database ? 1 : 0
  name      = "/${var.project_name}/database/host"
  type      = "String"
  value     = local.db_endpoint
  overwrite = true
}

resource "aws_ssm_parameter" "db_name" {
  count     = var.enable_database ? 1 : 0
  name      = "/${var.project_name}/database/name"
  type      = "String"
  value     = local.db_name
  overwrite = true
}

resource "aws_ssm_parameter" "db_username" {
  count     = var.enable_database ? 1 : 0
  name      = "/${var.project_name}/database/username"
  type      = "String"
  value     = "dbadmin"
  overwrite = true
}

resource "aws_ssm_parameter" "db_password" {
  count     = var.enable_database ? 1 : 0
  name      = "/${var.project_name}/database/password"
  type      = "SecureString"
  value     = random_password.db_password[0].result
  overwrite = true
}

# Outputs
output "load_balancer_dns" {
  value = local.load_balancer_dns
}

output "ecr_repository_url" {
  value = local.ecr_repository_url
}

output "database_endpoint" {
  value = var.enable_database ? aws_db_instance.main[0].endpoint : null
}