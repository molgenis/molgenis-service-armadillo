# Frequently Asked Questions

??? question "Docker gives a `java.socket` error"
    You might need to enable Docker socket. On Docker desktop you can find that under 'settings' and 'advanced'.

??? question "Can I use docker compose to start DataSHIELD profiles?"
    Instead of making Armadillo start/stop DataSHIELD profiles you can also use docker compose. See commented section in docker-compose.yml file.

??? question "Can I pass environment or commandline variables instead of application.yml?"
    Yes, it is standard spring.

??? question "Can I run Armadillo with oauth2 config offline?"
    Yes, you can run in 'offline' profile
    ```bash
    ./gradlew run -Dspring.containers.active=offline
    ```

??? question "How to run Armadillo version 2?"
    For armadillo 2.x you can follow [instructions](https://github.com/molgenis/molgenis-service-armadillo/tree/armadillo-service-2.2.3) for testing we use docker compose. For production environment we are using [Ansible](https://galaxy.ansible.com/molgenis/armadillo)

??? question "How to run Armadillo as developer?"
    We develop Armadillo using IntelliJ.

??? question "How to build Armadillo"
    Run following command in the github root:
    ```./gradlew build```

    To execute in 'dev' run following command in the github root:
    ```./gradlew run```

    **Setting up development tools**

    This repository uses `pre-commit` to manage commit hooks. An installation guide can be found [here](https://pre-commit.com/index.html#1-install-pre-commit). To install the hooks, run `pre-commit install` from the root folder of this repository. Now your code will be automatically formatted whenever you commit.

    **How to change data directory**

    Data is automatically stored in the `data` folder in this repository. You can choose another location in `application.yml` by changing the `storage.root-dir` setting.

    When you run Armadillo locally for the first time, the `lifecycle` project has not been added to the system metadata yet. To add it automatically, see Application properties.
    Alternatively you can add it manually:
        
      - Go to the Swagger UI (`http://localhost:8080/swagger-ui/index.html`)
      - Go to the `PUT /access/projects` endpoint
      - Add the project `lifecycle`
    
    **Working with resources in development mode**

    When developing locally, docker has trouble connecting to localhost. This problem becomes clear when working with
    resources. Luckily there is a quick fix for the problem. Instead of defining a resource as for example
    `http://localhost:8080/storage/projects/omics/objects/test%2Fgse66351_1.rda`, rewrite it to:
    `http://host.docker.internal:8080/storage/projects/omics/objects/test%2Fgse66351_1.rda`. Here's some example R code
    for uploading resources:

    ```R
    ## Uploading resources
    library(MolgenisArmadillo)
    library(resourcer)

    token <- armadillo.get_token("http://localhost:8080/")

    resGSE1 <- resourcer::newResource(
      name = "GSE66351_1",
      secret = token,
      url = "http://host.docker.internal:8080/storage/projects/omics/objects/test%2Fgse66351_1.rda",
      format = "ExpressionSet"
    )

    armadillo.login("http://localhost:8080/")
    armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE1, name = "GSE66351_1")
    ```
    And for using them:
    ```R
    library(DSMolgenisArmadillo)
    library(dsBaseClient)

    token <- armadillo.get_token("http://localhost:8080/")

    builder <- DSI::newDSLoginBuilder()
    builder$append(
      server = "local",
      url = "http://localhost:8080/",
      token = token,
      driver = "ArmadilloDriver",
      profile = "uniform",
      resource = "omics/ewas/GSE66351_1"
    )

    login_data <- builder$build()
    conns <- DSI::datashield.login(logins = login_data, assign = TRUE)

    datashield.resources(conns = conns)
    datashield.assign.resource(conns, resource="omics/ewas/GSE66351_1", symbol="eSet_0y_EUR")
    ds.class('eSet_0y_EUR', datasources = conns)
    datashield.assign.expr(conns, symbol = "methy_0y_EUR",expr = quote(as.resource.object(eSet_0y_EUR)))
    ```
