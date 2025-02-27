FROM molgenis/ci-build:1.2.4 


RUN echo "deb https://cloud.r-project.org/bin/linux/ubuntu jammy-cran40/" >> /etc/apt/sources.list
RUN wget -qO- https://cloud.r-project.org/bin/linux/ubuntu/marutter_pubkey.asc | tee -a /etc/apt/trusted.gpg.d/cran_ubuntu_key.asc

RUN apt update && apt -y install r-base r-base-dev r-cran-littler r-cran-docopt r-cran-metap libfftw3-dev curl ssh libcurl4-openssl-dev libssl-dev build-essential ninja-build cmake jq  
RUN cp /usr/lib/R/site-library/littler/examples/install2.r /usr/local/bin


RUN install2.r --skipinstalled --repo https://cloud.r-project.org devtools diffobj getPass
RUN install2.r --skipinstalled --repo https://cloud.r-project.org arrow future RCurl cli
RUN install2.r --repo https://cloud.r-project.org DSI resourcer MolgenisArmadillo DSMolgenisArmadillo
