# Decodable Resources

This folder contains the resources that can be deployed in your Decodable account. It includes:

- `streams/` contains the definition of two streams - for input and output.

- `connections/` contains the definition of a Kafka source connection that connects to the local Kafka cluster that can be spun up by using the deploy.sh script under `../docker` This connection writes to the input topic defined in #1.

- `custom/` contains a Java project with a Java pipeline that consumes from the input stream and writes to the output stream defined in #1.

- `pipelines/` contains the definition of the custom pipeline defined above and a SQL pipeline that also reads from and writes to the same streams.

## Build

The `deploy.sh` script here builds and deploys the resources in each of the folders. 





