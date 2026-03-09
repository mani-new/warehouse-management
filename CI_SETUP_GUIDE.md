# CI/CD Pipeline Setup Guide

This guide explains how to set up continuous integration and deployment for the warehouse-management project.

## 🚀 Quick Start

### Option 1: GitHub Actions (Recommended)

The project now includes a comprehensive GitHub Actions workflow at `.github/workflows/ci-pipeline.yml`.

**What's included:**
- ✅ Multi-OS testing (Ubuntu, Windows)
- ✅ Multiple Java versions (17, 21)
- ✅ Automated testing with coverage reports
- ✅ Code quality checks
- ✅ Docker image building
- ✅ Test result artifacts

**To enable:**
1. Push the `.github/workflows/ci-pipeline.yml` file to your repository
2. Go to your GitHub repository → Settings → Secrets and variables → Actions
3. Add the following secrets (if using Docker):
   - `DOCKER_USERNAME`: Your Docker Hub username
   - `DOCKER_PASSWORD`: Your Docker Hub password

### Option 2: Jenkins

A Jenkins pipeline file `Jenkinsfile` is also provided.

**Setup steps:**
1. Install Jenkins with the following plugins:
   - Pipeline
   - JaCoCo
   - JUnit
   - Maven Integration
   - Docker Pipeline

2. Configure Jenkins tools:
   - JDK 17 (named 'OpenJDK 17')
   - Maven 3.9.x (named 'Maven 3.9.x')

3. Create a new Pipeline job:
   - Pipeline script from SCM
   - Repository URL: your git repository
   - Script Path: `Jenkinsfile`

## 📋 Pipeline Features

### Build & Test
- **Maven Wrapper**: Uses `./mvnw` for consistent builds
- **Test Execution**: Runs all unit and integration tests
- **Coverage Reports**: Generates JaCoCo coverage reports
- **Test Results**: Publishes JUnit test results

### Quality Assurance
- **Code Formatting**: Spotless Maven plugin (if configured)
- **Static Analysis**: Checkstyle (if configured)
- **Security Scanning**: OWASP Dependency Check
- **Multi-Platform**: Tests on Ubuntu and Windows

### Docker Integration
- **Image Building**: Builds JVM Docker image
- **Registry Push**: Pushes to Docker Hub (when configured)
- **Multi-Stage**: Supports different build stages

## 🔧 Configuration Options

### Environment Variables

Add these to your CI environment:

```bash
# Maven options for large builds
MAVEN_OPTS=-Xmx3072m

# Test configuration
QUARKUS_TEST_PROFILE=test
```

### Customizing the Pipeline

#### GitHub Actions

Edit `.github/workflows/ci-pipeline.yml`:

```yaml
# Change Java versions
java-version: [17, 21]  # Add or remove versions

# Change trigger branches
branches: [ main, master, develop ]

# Add more quality checks
- name: Run additional checks
  run: ./mvnw sonar:sonar -B
```

#### Jenkins

Edit `Jenkinsfile`:

```groovy
// Add more test stages
stage('Integration Tests') {
    steps {
        sh './mvnw verify -B'
    }
}

// Add deployment stage
stage('Deploy') {
    when {
        branch 'main'
    }
    steps {
        sh 'kubectl apply -f k8s/'
    }
}
```

## 📊 Monitoring & Reporting

### Coverage Reports

- **GitHub Actions**: Uploaded as artifacts and to Codecov
- **Jenkins**: Published as HTML reports in the build

### Test Results

- **JUnit XML**: Parsed by CI platforms
- **HTML Reports**: Available in build artifacts
- **Coverage Trends**: Track coverage over time

## 🚢 Deployment Integration

### Docker Deployment

The pipeline builds and pushes Docker images. To deploy:

1. **Kubernetes**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: warehouse-management
spec:
  replicas: 3
  selector:
    matchLabels:
      app: warehouse-management
  template:
    metadata:
      labels:
        app: warehouse-management
    spec:
      containers:
      - name: app
        image: your-repo/warehouse-management:latest
        ports:
        - containerPort: 8080
```

2. **Docker Compose**:
```yaml
version: '3.8'
services:
  app:
    image: your-repo/warehouse-management:latest
    ports:
      - "8080:8080"
    environment:
      - QUARKUS_PROFILE=prod
```

### Cloud Platforms

#### AWS
```yaml
# .github/workflows/deploy-aws.yml
name: Deploy to AWS
on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - name: Deploy to ECS
      run: |
        aws ecs update-service --cluster my-cluster --service my-service --force-new-deployment
```

#### Azure
```yaml
# azure-pipelines.yml
trigger:
- main

pool:
  vmImage: 'ubuntu-latest'

steps:
- task: Maven@3
  inputs:
    mavenPomFile: 'pom.xml'
    goals: 'clean test'
    publishJUnitResults: true
    testResultsFiles: '**/surefire-reports/TEST-*.xml'
    codeCoverageTool: 'JaCoCo'
    javaHomeOption: 'JDKVersion'
    jdkVersionOption: '1.17'
```

## 🔍 Troubleshooting

### Common Issues

1. **Maven Wrapper Permission Denied**:
   ```bash
   chmod +x mvnw
   ```

2. **Docker Build Fails**:
   - Ensure Dockerfile paths are correct
   - Check Docker Hub credentials

3. **Test Timeouts**:
   - Increase timeout in workflow
   - Check for slow database connections

4. **Memory Issues**:
   - Adjust MAVEN_OPTS
   - Use larger runner instances

### Debugging

- **View Logs**: Check the "Actions" tab in GitHub
- **Download Artifacts**: Test results and coverage reports
- **Local Testing**: Run `./mvnw clean test jacoco:report` locally

## 📈 Best Practices

1. **Branch Protection**: Require CI to pass before merging
2. **Coverage Gates**: Set minimum coverage requirements
3. **Security Scanning**: Regular dependency vulnerability checks
4. **Performance Monitoring**: Track build times and test durations
5. **Artifact Management**: Store build artifacts for deployment

## 🔐 Security Considerations

- Store secrets in CI platform's secret management
- Use read-only tokens for external services
- Scan for vulnerabilities in dependencies
- Limit deployment permissions

## 📞 Support

For issues with the CI pipeline:
1. Check the CI logs for error messages
2. Verify all required secrets are configured
3. Test locally with the same commands
4. Review the workflow/pipeline configuration

---

**Last Updated**: March 9, 2026
**CI Platforms Supported**: GitHub Actions, Jenkins, GitLab CI, Azure DevOps
