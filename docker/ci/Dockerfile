FROM --platform=linux/amd64 rocker/verse:4.1.1

LABEL maintainer="Clemens Tolboom <molgenis@umcg.nl>"
LABEL version="0.0.1"
LABEL description="Image build to run release-test.R"

RUN apt update
RUN apt --yes install git curl ssh libcurl4-openssl-dev libssl-dev build-essential ninja-build cmake jq

RUN install2.r --skipinstalled --repo https://cloud.r-project.org devtools diffobj getPass

RUN install2.r --skipinstalled --repo https://cloud.r-project.org arrow future RCurl cli

RUN install2.r --skipinstalled --repo https://cloud.r-project.org DSI resourcer MolgenisArmadillo DSMolgenisArmadillo

RUN mkdir -p /armadillo/ci/bin

COPY fake-tree/cicd/install_release_script_dependencies.R /armadillo/ci/bin

RUN /armadillo/ci/bin/install_release_script_dependencies.R

COPY fake-tree/cicd/release-test.R /armadillo/ci/bin

COPY ci.env /armadillo/ci/bin/.env

#RUN adduser armadillo

#RUN chown -R armadillo:armadillo /armadillo/

#USER armadillo

CMD ["bash"]
