

#FROM molgenis/ci-build:1.2.4

# docker file for the ci
# to build and publish (n.b. on apple silicon you need to set --platform)
# docker buildx build -t molgenis/ci-build:1.2.2 --platform linux/amd64 .
# docker push molgenis/ci-build:1.2.2

FROM gradle:jdk21-noble
USER root

ENV DEBIAN_FRONTEND=noninteractive
RUN useradd -m molgenis

RUN echo "deb https://cran.r-project.org/bin/linux/ubuntu noble-cran40/" >> /etc/apt/sources.list
RUN wget -qO- https://cran.r-project.org/bin/linux/ubuntu/marutter_pubkey.asc | tee -a /etc/apt/trusted.gpg.d/cran_ubuntu_key.asc


RUN apt update && apt -y upgrade
RUN apt-get install -y gnupg lsb-release ca-certificates curl apt-transport-https
RUN install -m 0755 -d /etc/apt/keyrings

##docker
RUN curl -sSL https://get.docker.com/ | sh
RUN mkdir ~/.docker
RUN curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh -s 

# kubectl
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl

# Helm (binary install – works on noble)
RUN curl -fsSL https://get.helm.sh/helm-v3.14.4-linux-amd64.tar.gz \
  | tar -xz \
 && mv linux-amd64/helm /usr/local/bin/helm \
 && chmod +x /usr/local/bin/helm \
 && rm -rf linux-amd64
#

# Azure CLI Tools
RUN mkdir -p /etc/apt/keyrings
RUN curl -sLS https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor | tee /etc/apt/keyrings/microsoft.gpg > /dev/null
RUN chmod go+r /etc/apt/keyrings/microsoft.gpg
RUN  AZ_DIST=$(lsb_release -cs) && echo "deb [arch=`dpkg --print-architecture` signed-by=/etc/apt/keyrings/microsoft.gpg] https://packages.microsoft.com/repos/azure-cli/ $AZ_DIST main" | tee /etc/apt/sources.list.d/azure-cli.list
RUN apt-get update
RUN apt-get install azure-cli -y




#install deps for building 
RUN apt  install -y r-base \
    r-base-dev \
    libpcre2-dev \
    libdeflate-dev \
    liblzma-dev \
    libbz2-dev \
    zlib1g-dev \
    libzstd-dev \
    libicu-dev \
    libfftw3-dev \
    curl \
    ssh \
    libcurl4-openssl-dev \
    libssl-dev \
    build-essential \
    ninja-build \
    cmake \
    jq \
    fonts-roboto \
    ghostscript \
    hugo \
    less \
    libglpk-dev \
    libgmp3-dev \
    libfribidi-dev \
    libharfbuzz-dev \
    libhunspell-dev \
    libicu-dev \
    liblzma-dev \
    libopenmpi-dev \
    libpcre2-dev \
    libssl-dev \
    libv8-dev \
    libxml2-dev \
    libxslt1-dev \
    libzmq3-dev \
    libfontconfig-dev \
    libfontconfig1-dev \
    libtiff6 \ 
    libtiff5-dev \
    libudunits2-dev \
    libgdal-dev \
    qpdf \
    texinfo \
    software-properties-common \
    vim \
    wget

#Install littler
RUN Rscript -e "install.packages(c('littler', 'docopt'), repos='https://cran.r-project.org')"

RUN ln -sf /usr/local/lib/R/site-library/littler/bin/r /usr/local/bin
RUN ln -sf /usr/local/lib/R/site-library/littler/examples/install2.r /usr/local/bin

RUN install2.r --skipinstalled --repo https://cran.r-project.org devtools diffobj getPass getopt triebeard
RUN install2.r --skipinstalled --repo https://cran.r-project.org qqconf mutoss fftw 
RUN install2.r --skipinstalled --repo https://cran.r-project.org metap jsonlite urltools vroom
RUN install2.r --skipinstalled --repo https://cran.r-project.org arrow future RCurl cli sf
RUN install2.r --skipinstalled --repo https://cran.r-project.org DSI resourcer MolgenisArmadillo DSMolgenisArmadillo
#RUN install2.r --skipinstalled --repo https://cloud.r-project.org  BiocManager
#RUN Rscript -e "BiocManager::install('multtest', ask = FALSE, update = FALSE)"

RUN mkdir -p /cicd/armadillo/scripts/release
COPY scripts/release/install_release_script_dependencies.R /cicd/scripts/release/


RUN /cicd/scripts/release/install_release_script_dependencies.R
