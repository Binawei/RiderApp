variable "enable_database" {
  description = "Enable RDS database"
  type        = bool
  default     = false
}

variable "database_type" {
  description = "Database engine type"
  type        = string
  default     = "postgres"
  validation {
    condition     = contains(["postgres", "mysql", "mariadb"], var.database_type)
    error_message = "Database type must be postgres, mysql, or mariadb."
  }
}

variable "database_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

# Database subnet group
resource "aws_db_subnet_group" "main" {
  count      = var.enable_database ? 1 : 0
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

# Private subnets for database
resource "aws_subnet" "private" {
  count             = var.enable_database ? 2 : 0
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.project_name}-private-${count.index + 1}"
  }
}

# Database security group
resource "aws_security_group" "database" {
  count       = var.enable_database ? 1 : 0
  name_prefix = "${var.project_name}-db-"
  vpc_id      = aws_vpc.main.id

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

  tags = {
    Name = "${var.project_name}-db-sg"
  }
}

# RDS Instance
resource "aws_db_instance" "main" {
  count = var.enable_database ? 1 : 0

  identifier = "${var.project_name}-db"
  
  engine         = var.database_type == "postgres" ? "postgres" : "mysql"
  engine_version = var.database_type == "postgres" ? "15.4" : "8.0"
  instance_class = var.database_instance_class
  
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp2"
  storage_encrypted     = true
  
  db_name  = replace(var.project_name, "-", "_")
  username = "admin"
  password = random_password.db_password[0].result
  
  vpc_security_group_ids = [aws_security_group.database[0].id]
  db_subnet_group_name   = aws_db_subnet_group.main[0].name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = true
  deletion_protection = false

  tags = {
    Name = "${var.project_name}-database"
  }
}

# Random password for database
resource "random_password" "db_password" {
  count   = var.enable_database ? 1 : 0
  length  = 16
  special = true
}

# Store database credentials in AWS Systems Manager
resource "aws_ssm_parameter" "db_host" {
  count = var.enable_database ? 1 : 0
  name  = "/${var.project_name}/database/host"
  type  = "String"
  value = aws_db_instance.main[0].endpoint
}

resource "aws_ssm_parameter" "db_name" {
  count = var.enable_database ? 1 : 0
  name  = "/${var.project_name}/database/name"
  type  = "String"
  value = aws_db_instance.main[0].db_name
}

resource "aws_ssm_parameter" "db_username" {
  count = var.enable_database ? 1 : 0
  name  = "/${var.project_name}/database/username"
  type  = "String"
  value = aws_db_instance.main[0].username
}

resource "aws_ssm_parameter" "db_password" {
  count = var.enable_database ? 1 : 0
  name  = "/${var.project_name}/database/password"
  type  = "SecureString"
  value = random_password.db_password[0].result
}

# Database outputs
output "database_endpoint" {
  value = var.enable_database ? aws_db_instance.main[0].endpoint : null
}

output "database_name" {
  value = var.enable_database ? aws_db_instance.main[0].db_name : null
}