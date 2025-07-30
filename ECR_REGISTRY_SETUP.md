# ECR Registry Configuration Guide

## Overview

This guide explains the proper configuration of ECR registry URLs in GitHub secrets to ensure correct Docker image naming and deployment.

## Understanding ECR Repository URLs

### Structure of ECR URLs

An ECR repository URL consists of:
```
[AWS_ACCOUNT_ID].dkr.ecr.[REGION].amazonaws.com/[REPOSITORY_NAME]
```

For example:
```
123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/techwikiplus-user-service
```

## Required GitHub Secrets Configuration

### 1. ECR_REGISTRY

**IMPORTANT**: The `ECR_REGISTRY` secret should contain the **full repository URL**, including the repository name.

**Correct format**:
```
123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/techwikiplus-user-service
```

**Incorrect format** (missing repository name):
```
123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
```

### 2. AWS Credentials

Ensure these secrets are properly configured:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` (e.g., `ap-northeast-2`)

### 3. EC2 Deployment Secrets

For deployment to EC2:
- `EC2_HOST`: EC2 instance IP or hostname
- `EC2_USERNAME`: SSH username (e.g., `ubuntu`, `ec2-user`)
- `EC2_SSH_KEY`: Private SSH key for EC2 access
- `EC2_DEPLOY_PATH`: Deployment directory path on EC2

## Creating the ECR Repository

If the ECR repository doesn't exist yet, create it using AWS CLI:

```bash
# Create the repository
aws ecr create-repository \
    --repository-name techwikiplus-user-service \
    --region ap-northeast-2

# Get the repository URI
aws ecr describe-repositories \
    --repository-names techwikiplus-user-service \
    --region ap-northeast-2 \
    --query 'repositories[0].repositoryUri' \
    --output text
```

## Verifying the Configuration

### 1. Check Current ECR_REGISTRY Value

To verify your current setup, you can temporarily add this debug step to your workflow:

```yaml
- name: Debug ECR Registry
  run: |
    echo "ECR_REGISTRY value: ${{ secrets.ECR_REGISTRY }}"
    echo "Length: ${#ECR_REGISTRY}"
```

### 2. Test ECR Authentication

```bash
# Get ECR login token
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin \
  [YOUR_ECR_REGISTRY_URL]
```

### 3. Verify Repository Exists

```bash
# List all repositories
aws ecr describe-repositories --region ap-northeast-2

# Check specific repository
aws ecr describe-repositories \
    --repository-names techwikiplus-user-service \
    --region ap-northeast-2
```

## Troubleshooting

### Error: "repository not found: name unknown"

This error typically means:
1. The repository name in the URL is incorrect
2. The repository doesn't exist in ECR
3. The ECR_REGISTRY secret has an incorrect format

### Common Issues and Solutions

1. **Double repository name in path**
   - **Symptom**: Error shows path like `techwikiplus/server/user-service/techwikiplus-user-service`
   - **Cause**: ECR_REGISTRY already contains the repository name, but code appends it again
   - **Solution**: Ensure ECR_REGISTRY contains the full repository URL

2. **Authentication failures**
   - **Symptom**: "no basic auth credentials" error
   - **Cause**: ECR login failed or expired
   - **Solution**: Check AWS credentials and permissions

3. **Repository doesn't exist**
   - **Symptom**: "repository with name 'xxx' does not exist"
   - **Cause**: ECR repository hasn't been created
   - **Solution**: Create the repository using AWS CLI or console

## Best Practices

1. **Use consistent naming**: Keep repository names consistent across all environments
2. **Document the format**: Clearly document the expected format of each secret
3. **Use validation**: Add validation steps in workflows to check secret formats
4. **Regular audits**: Periodically verify that all secrets are correctly configured

## Summary

The key to avoiding ECR repository naming issues is to:
1. Store the complete repository URL in `ECR_REGISTRY` secret
2. Use this secret directly without appending additional path components
3. Ensure all deployment scripts use the same naming convention