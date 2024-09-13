## Run Armadillo via docker compose
For testing without having to installing Java you can run using docker:

1. Install [docker-compose](https://docs.docker.com/compose/install/)
2. Download this [docker-compose.yml](docker-compose.yml).
3. Execute ```docker-compose up```
4. Once it says 'Started' go to http://localhost:8080 to see your Armadillo running.

The command must run in the same directory as the downloaded docker file. We made docker available via 'docker.sock' so we can start/stop DataSHIELD profiles. Alternatively you must include the datashield profiles into this docker-compose. You can override all application.yaml settings via environment variables 
(see commented code in docker-compose file).
