#!/bin/bash
yum update -y
yum install -y docker aws-cli

# Start Docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Start SSM agent
systemctl start amazon-ssm-agent
systemctl enable amazon-ssm-agent

# Create app directory
mkdir -p /opt/${project_name}

# Signal completion
echo "Simple user data completed" > /var/log/user-data-complete.log