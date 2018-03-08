# SCPB


A simple REST service to register new clients on a prometheus server. Also a proxy for path server requests and the 
speed cam server.

## Usage
`java -jar SCBP.jar [PATH_TO_PROMETHEUS_DIR] (PORT) (RETENTION_TIME)`

+ Port: The port the REST service listens to
+ Retention time: The time span before old data will be deleted on the prometheus server

## Requirements
- Java 8
- Prometheus 2.X

## License
MIT

## Language
Java, Gradle
