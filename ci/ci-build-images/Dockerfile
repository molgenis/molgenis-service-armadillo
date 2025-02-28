FROM molgenis/ci-build:1.2.4 


RUN echo "deb https://cran.r-project.org/bin/linux/ubuntu jammy-cran40/" >> /etc/apt/sources.list
RUN wget -qO- https://cran.r-project.org/bin/linux/ubuntu/marutter_pubkey.asc | tee -a /etc/apt/trusted.gpg.d/cran_ubuntu_key.asc

#Install R
RUN apt update && apt -y install r-base r-base-dev 

#install deps for building 
RUN apt -y install libpcre2-dev libdeflate-dev liblzma-dev libbz2-dev zlib1g-dev libzstd-dev libicu-dev libfftw3-dev curl ssh libcurl4-openssl-dev libssl-dev build-essential ninja-build cmake jq  
RUN apt install -y \
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
    libtiff5 \ 
    libtiff5-dev \
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
RUN install2.r --skipinstalled --repo https://cran.r-project.org arrow future RCurl cli
RUN install2.r --skipinstalled --repo https://cran.r-project.org DSI resourcer MolgenisArmadillo DSMolgenisArmadillo

RUN mkdir -p /cicd/armadillo/scripts/release
COPY scripts/release/install_release_script_dependencies.R /cicd/scripts/release/


RUN /cicd/scripts/release/install_release_script_dependencies.R
