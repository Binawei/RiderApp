#!/bin/bash

echo "Terminating current instances and resetting ASG..."

# Set desired capacity to 0 to terminate all instances
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name "RiderApp-asg" \
  --desired-capacity 0 \
  --min-size 0

echo "ASG set to 0 instances. Instances will terminate shortly."
echo "Run the pipeline again to create new instances with proper SSM configuration."