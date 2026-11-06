pipeline {
    agent any
    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }
    stages {
        stage('Secrets Scan - Gitleaks') {
            steps {
                sh '''
                    docker run --rm -v "$WORKSPACE:/repo" zricethezav/gitleaks:latest \
                      detect --source /repo \
                      --report-format json --report-path /repo/gitleaks-report.json -v
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('Build & Package') {
    steps {
        sh 'mvn clean package -DskipTests'
    }
}

        stage('SCA - Trivy (dependencies)') {
    steps {
        sh '''
            docker run --rm -v "$WORKSPACE:/repo" aquasec/trivy:latest \
              fs --scanners vuln --severity CRITICAL,HIGH \
              --format json -o /repo/trivy-fs-report.json /repo/target
        '''
    }
    post {
        always {
            archiveArtifacts artifacts: 'trivy-fs-report.json', allowEmptyArchive: true
        }
    }
}

        stage('MVN SONARQUBE') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -DskipTests'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t anisbengaji/student-management:latest .'
            }
        }
        
         stage('Image Scan - Trivy') {
             steps {
                 sh '''
                    docker run --rm \
                      -v /var/run/docker.sock:/var/run/docker.sock \
                      -v trivy-cache:/root/.cache/trivy \
                      -v "$WORKSPACE:/repo" aquasec/trivy:latest \
                      image --scanners vuln --severity CRITICAL,HIGH \
                      --timeout 15m \
                      --format json -o /repo/trivy-image-report.json \
                      anisbengaji/student-management:latest
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-image-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('Pull MySQL Image') {
            steps {
                sh 'docker pull mysql:8.0'
            }
        }

        stage('Push to DockerHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh '''
                        docker login -u $USERNAME -p $PASSWORD
                        docker push anisbengaji/student-management:latest
                    '''
                }
            }
        }

        stage('Run Containers') {
            steps {
                sh '''
                    docker rm -f mysql-db student-management || true
                    docker compose down || true
                    docker compose up -d
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    kubectl apply -f mysql-deployment.yaml -n devops
                    kubectl apply -f spring-deployment.yaml -n devops
                '''
            }
        }
    }
}
