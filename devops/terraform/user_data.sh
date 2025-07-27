#!/bin/bash
yum update -y
yum install -y docker

# Start Docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Install AWS CLI
yum install -y aws-cli

# Start and enable SSM agent
systemctl start amazon-ssm-agent
systemctl enable amazon-ssm-agent

# Install CloudWatch agent
yum install -y amazon-cloudwatch-agent

# Create application directory
mkdir -p /opt/${project_name}
cd /opt/${project_name}

# Configure Docker to use ECR (after creating directory)
aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_registry} || echo "ECR login failed, will retry later"

# Create systemd services for the application
cat > /etc/systemd/system/${project_name}.service << EOF
[Unit]
Description=${project_name} Service
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/opt/${project_name}/start.sh
ExecStop=/opt/${project_name}/stop.sh
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

# Create start script
cat > /opt/${project_name}/start.sh << 'EOF'
#!/bin/bash
aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_registry}
docker pull ${ecr_registry}/${project_name}:latest
docker run -d --name ${project_name} -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e ENABLE_DATABASE=${enable_database} \
  -e PROJECT_NAME=${project_name} \
  -e AWS_REGION=${aws_region} \
  --restart unless-stopped \
  ${ecr_registry}/${project_name}:latest
EOF

# Create stop script
cat > /opt/${project_name}/stop.sh << 'EOF'
#!/bin/bash
docker stop ${project_name} || true
docker rm ${project_name} || true
EOF

chmod +x /opt/${project_name}/*.sh

# Enable service but don't start it immediately (let deployment handle it)
systemctl enable ${project_name}

# Signal that user data completed successfully
echo "User data script completed successfully" > /var/log/user-data-complete.log