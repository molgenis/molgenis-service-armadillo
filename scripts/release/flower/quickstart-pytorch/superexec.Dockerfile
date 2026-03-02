FROM flwr/superexec:1.23.0

WORKDIR /app

COPY pyproject.toml .
COPY pytorchexample/ ./pytorchexample/

RUN sed -i 's/.*flwr\[simulation\].*//' pyproject.toml \
   && python -m pip install -U --no-cache-dir .

ENTRYPOINT [ "flower-superexec" ]
