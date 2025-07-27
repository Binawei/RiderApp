#!/bin/bash

# Fetch database credentials from AWS Systems Manager Parameter Store
if [ "$ENABLE_DATABASE" = "true" ]; then
    export DB_HOST=$(aws ssm get-parameter --name "/riderapp/database/host" --query 'Parameter.Value' --output text --region ${AWS_REGION:-us-east-1})
    export DB_NAME=$(aws ssm get-parameter --name "/riderapp/database/name" --query 'Parameter.Value' --output text --region ${AWS_REGION:-us-east-1})
    export DB_USERNAME=$(aws ssm get-parameter --name "/riderapp/database/username" --query 'Parameter.Value' --output text --region ${AWS_REGION:-us-east-1})
    export DB_PASSWORD=$(aws ssm get-parameter --name "/riderapp/database/password" --with-decryption --query 'Parameter.Value' --output text --region ${AWS_REGION:-us-east-1})
    
    # Set Spring Boot database properties
    export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}"
    export SPRING_DATASOURCE_USERNAME="${DB_USERNAME}"
    export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
fi

# Start the application
exec java -jar /app/app.jar