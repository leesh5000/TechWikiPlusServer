# CD Pipeline Configuration Guide

## Overview
This guide explains the Continuous Deployment (CD) pipeline setup for the TechWikiPlus User Service.

## Required GitHub Secrets

Before the CD pipeline can run successfully, you need to configure the following secrets in your GitHub repository:

### AWS Credentials
- `AWS_ACCESS_KEY_ID`: Your AWS access key ID
- `AWS_SECRET_ACCESS_KEY`: Your AWS secret access key
- `ECR_REGISTRY`: Your ECR registry URL (e.g., `123456789012.dkr.ecr.ap-northeast-2.amazonaws.com`)

### EC2 Configuration
- `EC2_SSH_PRIVATE_KEY`: The private SSH key to connect to your EC2 instance
- `EC2_USER`: The username for SSH connection (e.g., `ec2-user`, `ubuntu`)

### Notifications (Optional)
- `SLACK_WEBHOOK_URL`: Slack webhook URL for deployment notifications

## EC2 Instance Setup

### 1. Tag Your EC2 Instance
Your EC2 instance must have the following tags:
```
Service: TechWikiPlus
Environment: production (or staging)
```

### 2. Install Required Software on EC2
```bash
# Install Docker
sudo yum update -y  # For Amazon Linux
sudo yum install docker -y
sudo service docker start
sudo usermod -a -G docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Configure AWS CLI (for ECR login)
sudo yum install aws-cli -y
aws configure  # Enter your AWS credentials
```

### 3. Create Application Directory
```bash
mkdir -p ~/techwikiplus/server/user
```

## Pipeline Triggers

The CD pipeline triggers in two ways:

1. **Automatic**: When the CI pipeline completes successfully on the main branch
2. **Manual**: Via workflow dispatch with environment selection

## Pipeline Flow

1. **Deploy Job**:
   - Retrieves EC2 instance information using AWS tags
   - Transfers deployment files (deploy.sh, docker-compose files)
   - Executes the deployment script
   - Verifies the deployment
   - Sends notifications
   - Creates GitHub deployment record

2. **Rollback Job** (on failure):
   - Automatically triggers if deployment fails
   - Attempts to rollback to the previous version
   - Sends rollback notifications

## Manual Deployment

To trigger a manual deployment:

1. Go to Actions tab in GitHub
2. Select "User Service CD" workflow
3. Click "Run workflow"
4. Select the environment (production/staging)
5. Click "Run workflow" button

## Monitoring Deployments

### GitHub Deployments
View deployment history in the repository's "Deployments" section.

### Slack Notifications
If configured, you'll receive notifications for:
- Deployment start
- Deployment success/failure
- Rollback initiation

### EC2 Logs
SSH into your EC2 instance and check:
```bash
cd ~/techwikiplus/server/user
docker-compose -f docker-compose.base.yml -f docker-compose.prod.yml logs -f
```

## Troubleshooting

### SSH Connection Failed
- Verify EC2 instance is running
- Check security group allows SSH (port 22)
- Verify SSH key in GitHub secrets matches EC2 key pair

### ECR Login Failed
- Verify AWS credentials have ECR permissions
- Check ECR registry URL is correct
- Ensure EC2 instance has proper IAM role/permissions

### Deployment Script Failed
- Check deploy.sh has execute permissions
- Verify Docker and Docker Compose are installed
- Check for port conflicts on EC2

### Container Won't Start
- Check environment variables in docker-compose files
- Verify image exists in ECR
- Check container logs for specific errors

## Security Best Practices

1. **Rotate Secrets Regularly**: Update AWS keys and SSH keys periodically
2. **Use IAM Roles**: Consider using EC2 IAM roles instead of AWS keys
3. **Restrict SSH Access**: Use security groups to limit SSH access
4. **Enable CloudTrail**: Monitor AWS API calls for security
5. **Use Secrets Management**: Consider AWS Secrets Manager for sensitive data

## Version Management

The deployment script maintains version history:
- Current version stored in `.current_version`
- Previous version stored in `.previous_version`
- Enables automatic rollback capability

## Support

For issues or questions:
1. Check GitHub Actions logs
2. Review EC2 instance logs
3. Verify all secrets are correctly configured
4. Check AWS service status