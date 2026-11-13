pipeline {
    agent any
    
     triggers {
        pollSCM('H/2 * * * *')
    }
    
    tools {
        jdk 'JAVA_HOME'
        maven 'M2_HOME'
    }
    environment {
        DD_URL = 'http://localhost:8081'
        DD_ENGAGEMENT = '1'
    }
    stages {
        // Scan de secrets : Gitleaks cherche mots de passe, cles API et tokens dans le code
        // et l'historique Git. Non bloquant ici (|| true), la decision est prise par la Security Gate.
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

        // Compilation Maven : produit le JAR Spring Boot, necessaire a l'analyse SCA qui suit.
        stage('Build & Package') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        // SCA : Grype analyse les dependances embarquees dans le JAR (Tomcat, Jackson, Spring...)
        // et signale les CVE connues. Remplace Trivy, qui ne lisait pas le fat jar.
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

        // SAST : SonarQube analyse le code source Java (failles, bugs, qualite).
        // Les resultats sont consultables dans l'interface SonarQube.
        stage('MVN SONARQUBE') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -DskipTests'
                }
            }
        }

        // Construction de l'image Docker a partir du Dockerfile durci (non-root, HEALTHCHECK).
        stage('Build Docker Image') {
            steps {
                sh 'docker build -t anisbengaji/student-management:latest .'
            }
        }

        // Scan d'image : Trivy cherche les CVE Critical/High dans l'image et son OS de base.
        // Le volume trivy-cache evite de retelecharger la base de vulnerabilites a chaque build.
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

        // Scan IaC : Checkov verifie les mauvaises configurations du Dockerfile et des manifests
        // Kubernetes. Premier appel pour l'affichage dans les logs, second pour l'export JSON.
         stage('IaC Scan - Checkov') {
    steps {
        sh '''
            docker run --rm -v "$WORKSPACE:/repo" bridgecrew/checkov:latest \
              -d /repo --framework dockerfile kubernetes secrets --compact -o cli || true
            docker run --rm -v "$WORKSPACE:/repo" bridgecrew/checkov:latest \
              -d /repo --framework dockerfile kubernetes secrets -o json > "$WORKSPACE/checkov-results.json" 2>/dev/null || true
        '''
    }
    post {
        always {
            archiveArtifacts artifacts: 'checkov-results.json', allowEmptyArchive: true
        }
    }
}

      

        // Recupere l'image MySQL utilisee par docker compose.
        stage('Pull MySQL Image') {
            steps {
                sh 'docker pull mysql:8.0'
            }
        }

   

        // Demarre l'application et sa base MySQL (docker compose) pour pouvoir la tester dynamiquement.
        // Les anciens conteneurs sont supprimes avant.
        stage('Run Containers') {
            steps {
                sh '''
                    docker rm -f mysql-db student-management || true
                    docker compose down || true
                    docker compose up -d
                '''
            }
        }

        // DAST : OWASP ZAP teste l'application en cours d'execution (scan baseline : en-tetes,
        // cookies, etc.). Le sleep laisse le temps a l'application de demarrer.
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

        // Centralisation : envoie chaque rapport de scan a DefectDojo via son API (un appel par outil).
        // La cle API est un credential Jenkins masque.
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
                push "Checkov Scan"   "$WORKSPACE/checkov-results.json"  "Checkov"
            '''
        }
    }
}

        // Quality gate : interroge DefectDojo pour compter les findings actifs Critical et High.
        // S'il en reste au moins un (CVSS >= 7), le pipeline est bloque (exit 1) et rien n'est deploye.
        // Les vulnerabilites acceptees via Risk Acceptance sont inactives et ne sont pas comptees.
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
        
         // Publication de l'image sur DockerHub, uniquement si la Security Gate est passee.
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

        // Deploiement sur Kubernetes (namespace devops) : MySQL puis l'application.
        // Dernier stage, atteint seulement si tous les controles precedents sont passes.
        //al
        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                    kubectl apply -f mysql-deployment.yaml -n devops
                    kubectl apply -f spring-deployment.yaml -n devops
                '''
            }
        }
    }
        post {
        failure {
            withCredentials([string(credentialsId: 'slack-webhook', variable: 'HOOK')]) {
                sh '''
                    curl -s -X POST -H 'Content-type: application/json' \
                      --data "{\\"text\\":\\":red_circle: Pipeline ${JOB_NAME} #${BUILD_NUMBER} BLOQUE par le Security Gate (vulnerabilites critiques/hautes). ${BUILD_URL}\\"}" \
                      "$HOOK"
                '''
            }
        }
        success {
            withCredentials([string(credentialsId: 'slack-webhook', variable: 'HOOK')]) {
                sh '''
                    curl -s -X POST -H 'Content-type: application/json' \
                      --data "{\\"text\\":\\":white_check_mark: Pipeline ${JOB_NAME} #${BUILD_NUMBER} reussi - deploiement autorise. ${BUILD_URL}\\"}" \
                      "$HOOK"
                '''
            }
        }
    }
}
