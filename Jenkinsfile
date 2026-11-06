pipeline {
    agent any
    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }
    environment {
        DD_URL = 'http://localhost:8081'
        DD_ENGAGEMENT = '1'
    }
    stages {
        stage('Secrets Scan - Gitleaks') {
            steps {
                sh '''
                    docker run --rm -v "$WORKSPACE:/repo" zricethezav/gitleaks:latest \
                      detect --source /repo \
                      --report-format json --report-path /repo/gitleaks-report.json -v || true
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

        stage('SCA - Grype (dependencies)') {
            steps {
                sh '''
                    docker run --rm -v "$WORKSPACE:/repo" anchore/grype:latest \
                      /repo/target/student-management-0.0.1-SNAPSHOT.jar \
                      -o json --file /repo/grype-report.json || true
                '''
            }
            post {
                always { archiveArtifacts artifacts: 'grype-report.json', allowEmptyArchive: true }
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
                      anisbengaji/student-management:latest || true
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-image-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('IaC Scan - Checkov') {
    steps {
        sh '''
            rm -rf "$WORKSPACE/checkov-report"
            mkdir -p "$WORKSPACE/checkov-report"
            docker run --rm -v "$WORKSPACE:/repo" bridgecrew/checkov:latest \
              -d /repo --framework dockerfile kubernetes \
              --compact -o cli -o json --output-file-path console,/repo/checkov-report || true
        '''
    }
    post {
        always {
            archiveArtifacts artifacts: 'checkov-report/results_json.json', allowEmptyArchive: true
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

        stage('DAST - OWASP ZAP') {
            steps {
                sh '''
                    echo "Waiting for the app to be ready..."
                    sleep 40
                    mkdir -p "$WORKSPACE/zap-out"
                    chmod 777 "$WORKSPACE/zap-out"
                    docker run --rm --network host \
                      -v "$WORKSPACE/zap-out:/zap/wrk:rw" \
                      ghcr.io/zaproxy/zaproxy:stable zap-baseline.py \
                      -t http://localhost:8089 \
                      -J zap-report.json -r zap-report.html -x zap-report.xml || true
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'zap-out/zap-report.*', allowEmptyArchive: true
                }
            }
        }

        stage('Upload to DefectDojo') {
    steps {
        withCredentials([string(credentialsId: 'defectdojo-api-key', variable: 'DD_TOKEN')]) {
            sh '''
                URL="$DD_URL/api/v2/import-scan/"
                push() {  # $1=scan_type  $2=file  $3=test_title
                  echo "--> $3"
                  [ -f "$2" ] || { echo "   (fichier absent: $2)"; return; }
                  code=$(curl -s -o /tmp/dd_resp.json -w "%{http_code}" -X POST "$URL" \
                    -H "Authorization: Token $DD_TOKEN" \
                    -F "scan_type=$1" -F "engagement=$DD_ENGAGEMENT" -F "file=@$2" \
                    -F "test_title=$3" -F "active=true" -F "verified=false" \
                    -F "close_old_findings=true")
                  echo "   HTTP $code"
                  [ "$code" = "201" ] || cat /tmp/dd_resp.json
                }
                push "Gitleaks Scan"  "$WORKSPACE/gitleaks-report.json"             "Gitleaks"
                push "Anchore Grype"  "$WORKSPACE/grype-report.json"                "Grype - Dependencies"
                push "Trivy Scan"     "$WORKSPACE/trivy-image-report.json"          "Trivy - Image"
                push "ZAP Scan"       "$WORKSPACE/zap-out/zap-report.xml"           "OWASP ZAP"
                push "Checkov Scan"   "$WORKSPACE/checkov-report/results_json.json" "Checkov"
            '''
        }
    }
}

        stage('Security Gate - DefectDojo') {
            steps {
                withCredentials([string(credentialsId: 'defectdojo-api-key', variable: 'DD_TOKEN')]) {
                    sh '''
                        API="$DD_URL/api/v2/findings/"
                        count() {
                          curl -s -G "$API" -H "Authorization: Token $DD_TOKEN" \
                            --data-urlencode "test__engagement=$DD_ENGAGEMENT" \
                            --data-urlencode "active=true" \
                            --data-urlencode "severity=$1" \
                            | python3 -c "import sys,json;print(json.load(sys.stdin)['count'])"
                        }
                        CRIT=$(count Critical)
                        HIGH=$(count High)
                        echo "=============== SECURITY GATE ==============="
                        echo " Findings Critical : $CRIT"
                        echo " Findings High     : $HIGH"
                        echo " Seuil de blocage  : CVSS >= 7 (Critical + High)"
                        echo "============================================="
                        if [ "$CRIT" -gt 0 ] || [ "$HIGH" -gt 0 ]; then
                          echo " GATE: BLOQUE - deploiement interdit"
                          exit 1
                        fi
                        echo " GATE: PASSE"
                    '''
                }
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
