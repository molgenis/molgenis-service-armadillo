FROM flwr/superexec:1.23.0

# git needed for pip install from GitHub
USER root
RUN apt-get update && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*
USER app

WORKDIR /app

# Install molgenis-flwr-armadillo from source (copied in by build context)
COPY molgenis_flwr_armadillo/ ./molgenis_flwr_armadillo/
RUN pip install --no-cache-dir ./molgenis_flwr_armadillo

# Install the Flower app
COPY pyproject.toml .
COPY pytorchexample/ ./pytorchexample/

# Remove deps already installed or not needed in container
RUN sed -i 's/.*flwr\[simulation\].*//' pyproject.toml \
    && sed -i 's/.*molgenis-flwr-armadillo.*//' pyproject.toml \
    && pip install --no-cache-dir -U .

# Create data directory for push-data endpoint
USER root
RUN mkdir -p /tmp/armadillo_data && chmod 777 /tmp/armadillo_data
USER app

ENTRYPOINT [ "flower-superexec" ]
