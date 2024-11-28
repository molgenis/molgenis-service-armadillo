# Developer Guidelines
The Armadillo and DataSHIELD community both are very welcoming to anyone who wants to contribute in any sort of form.
One way of doing that is by helping with the development of Armadillo. To help you get started, we've put together some
information to get you started and help you get familiarised with our code and way of working. 

=== ":fontawesome-solid-circle-info: General information"

    <h2>Commits and automatic releases</h2>

    Versionnumbers are updated according to [semantic versioning](https://semver.org/),
    using [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/).
    
    It is especially important to correctly identify and commit user-facing changes. These being: bugfixes and new
    features. The correct prefix to a commit will trigger an automatic release. Automatic releases are set to be
    pre-releases.
    
    Each commit with `!` just before the colon `:` is a major update, indicating a breaking change. So use it wisely. 
    You can also add `BREAKING CHANGE:` in the long commit message format.
    
    - Use `feat!: ...` or `fix!: ...` for a major upgrade, indicating a breaking change.
      - Use `feat: ...` for a minor upgrade, indicating a new feature.
      - Use `fix: ...` for a patch update, indicating a bugfix.

    <h2>Releases</h2>
    As mentioned above, the automatic releases in Armadillo are pre-releases. Normal releases are done manually whenever
    deemed necessary. Of course a new release can be requested anytime.

    <h2>Testing and continuous integration</h2>
    All our repositories contain unittests. Although we usually aim for a coverage of 80%, sometimes we make an
    exception. Tests are ran as part of our continuous integration, on each pull request and each merge with the master.

    <h2>Pull request</h2>
    We encourage everyone to contribute to MOLGENIS armadillo. We do however have a couple of requirements for a pull
    requests, mostly because of our automatic processes.

    1. Start the title of your PR with prefix using conventional commits (see general information tab)
    2. When you're fixing an issue, include "closing #issuenumber" or "fixes #issuenumber" in a commit in the PR
    3. Describe what your PR does shortly and how to test it.

    <h2>Armadillo API and Swagger</h2>
    Armadillo can be controlled using it's REST API. To see and test all available endpoints, you can visit its
    swaggerpage. This page can be found on /swagger-ui/index.html of each Armadillo instance. An example can be found
    on [our demo server](https://armadillo-demo.molgenis.net/swagger-ui/index.html).

=== ":fontawesome-brands-java: Java"

    The serverside code of MOLGENIS Armadillo 
    ([molgenis-service-armadillo](https://github.com/molgenis/molgenis-service-armadillo)) is written in Java. In order
    to run it locally you will need the following prerequisites:

    - Java 17 (unless running with Docker)
    - Docker

    <h2>Project Structure</h2>
    If you look at the java code in [our repository](https://github.com/molgenis/molgenis-service-armadillo/), you might
    notice there are two parts to it:

    - [Armadillo](https://github.com/molgenis/molgenis-service-armadillo/tree/master/armadillo)
    - [R](https://github.com/molgenis/molgenis-service-armadillo/tree/master/r)

    The Armadillo part is the main application that contains all basic funcitonality and APIs. It uses the R part to
    communicate with R, using DataSHIELD logic. 
 
    <h2>Tools</h2>
    <h3>pre-commit</h3>
    This repository uses `pre-commit` to manage commit hooks. An installation guide can be found
    [here](https://pre-commit.com/index.html#1-install-pre-commit). To install the hooks, run `pre-commit install` from the root folder of this repository. Now
    your code will be automatically formatted whenever you commit. The pre-commit hooks we've set, will reformat new
    code upon commit, so that it matches our code style. 

    <h2>Running</h2>
    There are several ways to run armadillo. What you're planning on doing with armadillo determines the best way to go.
    If you just want to test how it works, runnig via the jar or even using docker is very suitable. If you're planning
    on writing code yourself, it's better to clone the project, build it yourself and run it, either using your IDE (we
    use IntelliJ), or running the jar you built locally.
    
    <h3>Jar</h3>

    1. Download the jar from our
    [releases page](https://github.com/molgenis/molgenis-service-armadillo/releases). 
    2. Copy paste the contents of 
    [application-template.yml](https://github.com/molgenis/molgenis-service-armadillo/blob/master/application.template.yml)
    and paste it in a file called `application.yml`, in the same folder as the downloaded jar. 
    3. To start the application, run `java -jar molgenis-armadillo-x.yy.zz.jar`.
    4. Go to `http://localhost:8080` to see the Armadillo UI.

    <h3>Docker</h3>
    For testing without having to installing Java you can run using docker:

    1. Install [docker-compose](https://docs.docker.com/compose/install/)
    2. Download this [docker-compose.yml](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/refs/heads/master/docker-compose.yml).
    3. Execute `docker-compose up`
    4. Once it says 'Started', go to http://localhost:8080 to see your Armadillo running.

    The command must run in the same directory as the downloaded docker file. 
    We made docker available via 'docker.sock' so we can start/stop DataSHIELD profiles. 
    Alternatively you must include the datashield profiles into this docker-compose. 
    You can override all application.yml settings via environment variables (see commented code in docker-compose file).

    <h3>IntelliJ</h3>
    We develop Armadillo using IntelliJ. To do so:
    
    1. Clone our project:
    ```shell
    git clone https://github.com/molgenis/molgenis-service-armadillo.git
    ```
    2. Open the project in Intellij. 
    3. Make sure the right version of Java is set in `File> Project structure...`
    4. Build the project: select the gradle button (elephant symbol, usually on the right of your intellij screen),
    select `clean` and then `build`. 
    5. Go to the `ArmadilloServiceApplication` class (press shift-shift and search for it).
    6. Click on the play button in front of the main function. You might have to press "Run" again if a popup with the
    run configuration appears.

    <h2>Testing</h2>
    Most of our code is covered by unittests. Although we usually aim for a coverage of 80%, sometimes we make
    exceptions because we value quality of tests over their quantity. We recognise that for some pieces of code it's
    hard to write meaningful unittests (half of some functions would have to be mocked, usually causing tests to
    lose their purpose). This is especially the case in our Java repository. Of course we don't want
    these pieces untested, therefore we have
    [a set of release test scripts](https://github.com/molgenis/molgenis-service-armadillo/tree/master/scripts/release).
    This test is made to test basic functionality from Datamanager and Researcher perspective using the most commonly
    used DataSHIELD packages. This test script is used before each release to ensure Armadillo's quality. The script
    also runs partly (without OIDC) in our continuous integration test on each pull request, as well as the unittests.

=== ":material-vuejs: JavaScript/VueJS"
    
    The user interface (UI) of MOLGENIS Armadillo is written in JavaScript, using VueJS as framework. We use `yarn` to 
    compile the code and develop in VSCode. Our setup is as follows:
    
    1. Open the armadillo repository, and build and run it in InteliJ (as described in Java Developer guide)
    2. Open the ui folder of the repository in VSCode.
    3. Install using:
    ```shell
    yarn install
    ```
    4. Run dev server using:
    ```
    yarn dev
    ```
    You can now login on http://localhost:8080 and then switch to http://localhost:8081 to directly see the changes
    you're changing the UI code. 

=== ":simple-r: R"

    We maintain several R packages:
    
    - [MolgenisArmadillo](https://github.com/molgenis/molgenis-r-armadillo)
    - [DSMolgenisArmadillo](https://github.com/molgenis/molgenis-r-datashield)
    - [DSUpload](https://github.com/lifecycle-project/ds-upload)
    - [DSTidyVerse](- [DSTidyverse](https://github.com/molgenis/ds-tidyverse))
    - [DSTidyverseClient](https://github.com/molgenis/ds-tidyverse-client)

    We aim to release all our packages (excluding DSUpload) to CRAN, to increase visibility and compatibility. This
    means that documentation and vignettes should be updated in every pull request that changes or updates 
    functionality.