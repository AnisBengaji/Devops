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

```
Checkout → Gitleaks → Build (Maven) → Grype (SCA) → SonarQube (SAST)
→ Build Image → Trivy (Image) → Checkov (IaC) → Push DockerHub
→ Run Containers → OWASP ZAP (DAST) → Upload to DefectDojo
→ Security Gate → Deploy to Kubernetes
```

Each push triggers Jenkins to run the whole thing automatically. Every scanner sends its report to DefectDojo, and the **Security Gate** asks DefectDojo one question: *are there any active Critical/High vulnerabilities (CVSS ≥ 7)?* If yes, the build fails and the deploy is refused. A Slack notification is sent on every run.

---

## Shift-left (before the pipeline even runs)

Security also runs on the developer's machine, so problems are caught before a commit is even pushed:

- **SonarLint** in the IDE, connected to the SonarQube server (same rules as the pipeline)
- **pre-commit hooks** running **Gitleaks** + **Semgrep** — a commit containing a secret is blocked locally

---

## Running locally with Docker

```bash
docker-compose up -d
```

> The MySQL password is no longer hardcoded in `docker-compose.yml`. It's read from a git-ignored `.env` file (and injected from a Jenkins credential in the pipeline), so no secret lives in the repo.

---

## Kubernetes

```bash
kubectl apply -f mysql-deployment.yaml -n devops
kubectl apply -f spring-deployment.yaml -n devops
kubectl get pods -n devops
minikube service spring-service -n devops --url
```

---

## Handling vulnerabilities

The pipeline doesn't chase "zero vulnerabilities" (which isn't realistic). It follows a risk-based approach:

- **Fix what's fixable** — e.g. bumping Spring Boot and pinning a newer Tomcat cleared most of the Critical/High CVEs.
- **Risk-accept the rest** — vulnerabilities with no upstream fix are formally accepted in DefectDojo, with a justification and an expiry date. They stay documented, never silently ignored.

---

Made by **Anis Bengaji** — ESPRIT
