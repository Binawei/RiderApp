#!/bin/bash

echo "Resetting Auto Scaling Group to fix terminating loop..."

# Set ASG to 0 capacity and wait
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name "RiderApp-asg" \
  --desired-capacity 0 \
  --min-size 0

echo "Waiting for all instances to terminate..."
sleep 60

# Check if instances are gone
for i in {1..10}; do
  INSTANCES=$(aws autoscaling describe-auto-scaling-groups \
    --auto-scaling-group-names "RiderApp-asg" \
    --query 'AutoScalingGroups[0].Instances[].InstanceId' \
    --output text)
  
  if [ -z "$INSTANCES" ] || [ "$INSTANCES" = "None" ]; then
    echo "All instances terminated successfully"
    break
  fi
  
  echo "Attempt $i: Still have instances: $INSTANCES"
  sleep 30
done

echo "ASG reset complete. Run pipeline again to create fresh instances."