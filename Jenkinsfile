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
            sh 'mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -DskipTests'
        }
    }
}
       stage('Build Docker Image') {
            steps {
                sh 'docker build -t anisbengaji/student-management:latest .'
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
            docker-compose down || true
            docker-compose up -d
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
