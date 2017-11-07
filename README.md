# SCPB


A simple REST service to register new clients on a prometheus server. 

## Usage
`java -jar SCBP.jar [PATH_TO_PROMETHEUS_DIR] [TARGET_IPv4] (PORT)`

+ TARGET_IPv4: The targets(client) IPv4 Prometheus will pull from
+ Port: The port the REST service listens to

## Requirements
- Java 8
- Prometheus 1.X

## License
MIT

## Language
Java