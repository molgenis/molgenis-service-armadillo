FROM --platform=linux/amd64 rocker/verse:4.1.1

LABEL maintainer="Clemens Tolboom <molgenis@umcg.nl>"
LABEL version="0.0.1"
LABEL description="Image build to run release-test.R"

RUN apt update

RUN mkdir -p /cicd/armadillo/scripts/release

# Copy slow changing file only
COPY armadillo/scripts/release/install_release_script_dependencies.R /cicd/scripts/release/

RUN /cicd/scripts/release/install_release_script_dependencies.R

# Copy rest from of scripts
COPY armadillo/scripts/release/* /cicd/scripts/release/

COPY armadillo /cicd/

#RUN adduser armadillo

#RUN chown -R armadillo:armadillo /armadillo/

#USER armadillo

CMD ["bash"]
