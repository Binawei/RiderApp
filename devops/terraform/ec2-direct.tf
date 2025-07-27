# Direct EC2 instances instead of ASG to avoid terminating loop
resource "aws_instance" "app" {
  count                  = 1
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  subnet_id              = local.subnet_ids[0]
  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = local.instance_profile_name

  user_data = base64encode(templatefile("${path.module}/user_data_simple.sh", {
    project_name = var.project_name
    aws_region   = var.aws_region
    ecr_registry = local.ecr_repository_url
  }))

  tags = {
    Name = "${var.project_name}-instance"
  }
}

# Register instance with target group
resource "aws_lb_target_group_attachment" "app" {
  count            = 1
  target_group_arn = data.aws_lb_target_group.app.arn
  target_id        = aws_instance.app[0].id
  port             = 8080
}