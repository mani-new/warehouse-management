pipeline {
    agent any

    tools {
        maven 'Maven 3.9.x'
        jdk 'OpenJDK 17'
    }

    environment {
        MAVEN_OPTS = '-Xmx3072m'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                sh './mvnw clean compile test-compile'
            }
        }

        stage('Run Tests') {
            steps {
                sh './mvnw test jacoco:report -B -V'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    jacoco execPattern: 'target/jacoco.exec',
                           classPattern: 'target/classes',
                           sourcePattern: 'src/main/java',
                           exclusionPattern: 'src/test/*'
                }
            }
        }

        stage('Quality Checks') {
            steps {
                sh './mvnw compile checkstyle:check -B'
            }
        }

        stage('Package') {
            steps {
                sh './mvnw package -DskipTests -B'
            }
        }

        stage('Build Docker Image') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                script {
                    def imageTag = "${env.BUILD_NUMBER}"
                    if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master') {
                        imageTag = 'latest'
                    }
                    sh "docker build -f src/main/docker/Dockerfile.jvm -t ${env.JOB_NAME}:${imageTag} ."
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            publishHTML target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'JaCoCo Coverage Report'
            ]
        }
        success {
            echo 'Build and tests passed!'
        }
        failure {
            echo 'Build or tests failed!'
        }
    }
}
