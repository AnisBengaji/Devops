# devsecops — Secure CI/CD Pipeline

![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)

![Gitleaks](https://img.shields.io/badge/Gitleaks-secrets-critical?style=for-the-badge)
![SonarQube](https://img.shields.io/badge/SonarQube-SAST-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white)
![Grype](https://img.shields.io/badge/Grype-SCA-blue?style=for-the-badge)
![Trivy](https://img.shields.io/badge/Trivy-Image_Scan-1904DA?style=for-the-badge&logo=aqua&logoColor=white)
![Checkov](https://img.shields.io/badge/Checkov-IaC-6C43DC?style=for-the-badge)
![OWASP ZAP](https://img.shields.io/badge/OWASP_ZAP-DAST-000000?style=for-the-badge&logo=owasp&logoColor=white)
![DefectDojo](https://img.shields.io/badge/DefectDojo-Quality_Gate-E6522C?style=for-the-badge)

This started as a plain DevOps pipeline (Spring Boot + MySQL, built and deployed with Jenkins, Docker and Kubernetes). I extended it into a full **DevSecOps** pipeline: security is now checked automatically at every stage, from the developer's machine all the way to the Kubernetes deploy — and a vulnerable build is **blocked** before it ever reaches production.

---

## What changed vs. the old pipeline

The old pipeline worked, but it was blind to security: no secret scanning, no dependency checks, no image scanning, and nothing stopped a vulnerable build from shipping. The new one adds six security controls plus a central gate.

| Control | Tool | What it checks |
|---|---|---|
| Secrets | **Gitleaks** | Hardcoded passwords, keys, tokens |
| SAST | **SonarQube** | Vulnerabilities in the source code |
| SCA | **Grype** | Vulnerable dependencies in the JAR |
| Image scan | **Trivy** | CVEs in the Docker image + base OS |
| IaC | **Checkov** | Misconfigs in Dockerfile & Kubernetes |
| DAST | **OWASP ZAP** | The running app (port 8089) |
| Aggregation + gate | **DefectDojo** | Centralizes everything, blocks on Critical/High |

---

## Pipeline
