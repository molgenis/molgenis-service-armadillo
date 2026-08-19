FROM flwr/supernode:1.23.0

USER root
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*
USER app

WORKDIR /app

COPY pyproject.toml .
COPY pytorchexample/ ./pytorchexample/

RUN sed -i 's/.*flwr\[simulation\].*//' pyproject.toml \
   && python -m pip install -U --no-cache-dir .

ENTRYPOINT [ "flower-supernode" ]
