# User Service CD (Continuous Deployment) Pipeline Guide

## Overview

The User Service CD pipeline automates the deployment process to EC2 instances after successful CI builds. It transfers deployment files and executes the deployment script on the target server.

## Workflow Triggers

The CD pipeline is triggered in two ways:

1. **Automatic Deployment**: When the CI pipeline completes successfully on the `main` branch
2. **Manual Deployment**: Via GitHub Actions UI using workflow dispatch

## Prerequisites

### Required AWS Environment Secrets

The following secrets must be configured in the GitHub repository's AWS Environment:

| Secret Name | Description | Example |
|------------|-------------|---------|
| `EC2_HOST` | EC2 instance IP address or hostname | `52.79.xxx.xxx` or `ec2-52-79-xxx-xxx.ap-northeast-2.compute.amazonaws.com` |
| `EC2_USERNAME` | SSH username for EC2 instance | `ubuntu` (for Ubuntu) or `ec2-user` (for Amazon Linux) |
| `EC2_SSH_KEY` | Private SSH key for EC2 access | The full private key content (including `-----BEGIN RSA PRIVATE KEY-----`) |
| `EC2_DEPLOY_PATH` | Target directory on EC2 for deployment | `/home/ubuntu/techwikiplus/server/user` |

### Existing AWS Environment Secrets (from CI)

These secrets are already configured for CI and are also used by CD:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `ECR_REGISTRY`

### EC2 Requirements

1. **SSH Access**: The EC2 instance must allow SSH connections from GitHub Actions IP ranges
2. **Docker**: Docker and Docker Compose should be installed (the deployment script will check)
3. **Directory Permissions**: The deployment user must have permissions to create directories and run Docker commands

## Deployment Process

### 1. SSH Connection Setup
- Configures SSH key from secrets
- Tests connection to EC2 instance
- Adds host to known_hosts

### 2. File Transfer
The following files are transferred to the EC2 instance:
- `service/user/deploy.sh` - Main deployment script
- `service/user/docker/docker-compose.base.yml` - Base Docker Compose configuration
- `service/user/docker/docker-compose.prod.yml` - Production-specific configuration

### 3. Deployment Execution
- Grants execute permission to `deploy.sh`
- Sets environment variables (AWS_REGION, ECR_REGISTRY, DOCKER_REGISTRY)
- Executes the deployment script
- Captures and reports deployment output

### 4. Post-Deployment Verification
- Checks running containers
- Provides deployment status summary
- Uploads deployment logs as artifacts

## Environment Files on EC2

The deployment script expects these files to exist on the EC2 instance:
- `.env` - Base environment variables
- `.env.prod` - Production-specific environment variables

These files should be manually created on the EC2 instance before the first deployment.

Example `.env` file:
```bash
# MySQL Configuration
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DATABASE=techwikiplus
MYSQL_USER=techwikiuser
MYSQL_PASSWORD=your_mysql_password
MYSQL_ROOT_PASSWORD=your_root_password

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# Server Configuration
SERVER_PORT=9000

# Health Check
HEALTH_CHECK_PATH=health
```

Example `.env.prod` file:
```bash
# Production-specific settings
JPA_SHOW_SQL=false
JPA_HIBERNATE_DDL_AUTO=none

# Additional production configs
LOG_LEVEL=INFO
```

## Manual Deployment

To trigger a manual deployment:

1. Go to Actions tab in GitHub
2. Select "User Service CD" workflow
3. Click "Run workflow"
4. Select branch (main)
5. Optionally add a deployment message
6. Click "Run workflow"

## Monitoring and Logs

### GitHub Actions Summary
Each deployment generates a detailed summary including:
- Deployment trigger information
- SSH connection status
- File transfer status
- Deployment script output
- Container status

### Deployment Artifacts
- `deployment-logs`: Contains the full output from the deployment script

### EC2 Deployment Logs
On the EC2 instance, deployment logs are saved with timestamps:
- Location: `$EC2_DEPLOY_PATH/deploy_YYYYMMDD_HHMMSS.log`

## Troubleshooting

### SSH Connection Failed
1. Verify `EC2_HOST` is correct
2. Check `EC2_USERNAME` matches the EC2 instance user
3. Ensure `EC2_SSH_KEY` contains the correct private key
4. Verify EC2 security group allows SSH (port 22) from GitHub Actions

### File Transfer Failed
1. Check if the deployment path exists and has proper permissions
2. Verify the source files exist in the repository
3. Ensure sufficient disk space on EC2

### Deployment Script Failed
1. Check Docker and Docker Compose are installed on EC2
2. Verify `.env` and `.env.prod` files exist on EC2
3. Check ECR authentication is working
4. Review deployment logs for specific errors

### Container Health Check Failed
1. Ensure the application is configured correctly
2. Check if required services (MySQL, Redis) are running
3. Verify environment variables are set correctly
4. Review application logs: `docker-compose logs user`

## Security Considerations

1. **SSH Keys**: Keep SSH private keys secure and rotate regularly
2. **Environment Variables**: Never commit `.env` files to the repository
3. **Network Security**: Restrict EC2 security group to necessary ports only
4. **ECR Access**: Use IAM roles with minimal required permissions

## Integration with CI Pipeline

The CD pipeline is designed to work seamlessly with the CI pipeline:

1. CI builds and tests the application during PR
2. CI pushes Docker images to ECR during PR checks
3. CD triggers automatically after PR is merged and CI completes
4. CD uses the Docker images that were built and pushed during the PR

This ensures that:
- Only tested and validated code is deployed to production
- Docker images are pre-built during PR review, speeding up deployment
- The exact same image tested in PR is deployed to production