# Armadillo Local Quickstart Setup

Welcome! This guide shows how to run **Armadillo**, **RServer**, and **Keycloak** locally with Docker Compose.

!!! Note 
    The preferred way to install Armadillo in production is via [systemd](../armadillo_install). 
    Feel free to use our docker setup.

---

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)  
- [Docker Compose](https://docs.docker.com/compose/)  
- [Git](https://git-scm.com/)

---

## Quickstart

Clone this repository:

```bash
git clone https://github.com/molgenis/molgenis-service-armadillo.git
cd molgenis-service-armadillo/docker/quickstart
```

Start the stack

```bash
docker compose up
```


This starts:

- Armadillo → http://localhost:8080
- Keycloak → http://localhost:8081
 (admin console)
- RServer → localhost:6311

Logs and data are stored in ./logs and ./data.



## Keycloak Setup

After starting with `docker compose up`, Keycloak is available at:

- Admin console: [http://localhost:8081](http://localhost:8081)  
- Logini credentials: `admin / admin`

---

### Import Realm

The repo provides a realm config under `./keycloak/realms`. It is automatically imported on startup.

---

### Add a Test User

1. Open the **Keycloak Admin Console**  
2. Go to **Realms → Armadillo**  
3. Go to **Users → Add User**  
   - Username: `demo@example.org`  
   - Email verified: ✅  
4. Go to **Credentials → Set Password**  
   - Password: `demo12345`  
   - Temporary: OFF  

---

## Armadillo Configuration

1. Add the previously added [user](../../basic_usage/armadillo_ui/#users) in keycloak also to Armadillo
2. Add a [project](../../basic_usage/armadillo_ui/#projects)
3. Connect as a [researcher](../../quick_start/#researcher) to the service. Use **http://localhost:8000** as the Armadillo URL.

## Troubleshooting

### Keycloak healthcheck fails
- Ensure port `9000` is free on your system (used for readiness checks).

### [invalid_user_info_response] 403 Forbidden
- Confirm that the user exists in Keycloak and has a password set. This user also needs to be added to Armadillo.

### Reset stack
```bash
docker compose down -v
docker compose up --build
```

## Kubernetes

We have [helm](https://github.com/molgenis/molgenis-service-armadillo/tree/master/helm-chart) charts available for Kubernetes setup. 
Profiles are maintained bij DataShield. We have helm charts available in our private repository. 
If you need help or want to contribute please give us a message via support@molgenis.org or you can find us in the #Armadillo-support [Slack](https://join.slack.com/t/datashieldespacio/shared_invite/zt-3c52ci6id-_W~w5a_alDeFKX~icn4ttw) channel 
