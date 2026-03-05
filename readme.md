# devops — CI/CD Pipeline

![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)

This is a Spring Boot + MySQL app deployed through a full CI/CD pipeline built with Jenkins, Docker and Kubernetes. The goal was to automate everything from compiling the code to deploying it on a Kubernetes cluster.

---

## Pipeline
```
Compile → SonarQube → Build Image → push to DockerHub → Run Containers → Deploy to Kubernetes
```

Each push to the repo triggers Jenkins to run the full pipeline automatically.

- **Compile** — Maven builds the project
- **SonarQube** — scans the code for bugs and quality issues
- **Docker** — builds the Spring Boot image,push to DockerHub repo and spins up both the app and MySQL with docker-compose
- **Kubernetes** — deploys everything to a local Minikube cluster in the `devops` namespace

---

## Running locally with Docker
```bash
docker-compose up -d
```

---

## Kubernetes
```bash
kubectl apply -f mysql-deployment.yaml -n devops
kubectl apply -f spring-deployment.yaml -n devops
kubectl get pods -n devops
minikube service spring-service -n devops --url
```

---

Made by **Anis Bengaji** — ESPRIT
