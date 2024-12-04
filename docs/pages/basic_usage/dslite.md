# DSLite

Relevant for: :fontawesome-solid-laptop-code:{title="Developers"}

DataSHIELD Lite ([DSLite](https://github.com/datashield/DSLite)) is a serverless [DataSHIELD Interface (DSI)](https://datashield.github.io/DSI/) implementation which purpose is to mimic the behavior of a distant (virtualized or barebone) data repository server like Armadillo or Opal. The datasets that are being analyzed are fully accessible in the local environment. The DataSHIELD configuration (set of allowed aggregation and assignment functions) is discovered at runtime by inspecting the DataSHIELD server-side R packages installed locally. This configuration can also be amended or provided explicitly.

DSLite can be used to:

- speed up development and testing cycle when developing new DataSHIELD functions (both at server and client side): no need to deploy a data repository infrastructure.
- allow DataSHIELD analysis with combined datasets, some of them being accessible remotely in secure data repositories, others being privately accessible (in a governmental institution for instance).
