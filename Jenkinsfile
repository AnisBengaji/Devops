pipeline {
    agent any
    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }
    stages {
        stage('Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('MVN SONARQUBE') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn clean verify sonar:sonar'
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t student-management:latest .'
            }
        }
        stage('Pull MySQL Image') {
            steps {
                sh 'docker pull mysql:8.0'
            }
        }
        stage('Run Containers') {
    steps {
        sh '''
            docker rm -f mysql-db student-management || true
            docker-compose down || true
            docker-compose up -d
        '''
    }
}
    }
}
