# SCPB


A simple REST service to register new clients on a prometheus server. Also a proxy for path server requests and the 
speed cam server.

## Usage
`java -jar SCBP.jar [PATH_TO_PROMETHEUS_DIR] [PROMETHEUS_URL] (PORT) `

+ Path: Path to the prometheus directory
+ PrometheusUrl: Accessible prometheus URL to reload the configuration. 
+ Port: The port the REST service listens to

## Requirements
- Java 8
- Prometheus 2.X with enabled web management (`--web.enable-lifecycle`) 

## License
MIT

## Language
Java, Gradle

## API

### /prometheusClient
Add or receive prometheus client information

#### GET

Receive a list of registered prometheus client

#### POST

Post this json to register or modify clients:

`
        
        {
            "CREATE": {
                "br1-17-1": {
                    "ip": "192.168.0.1",
                    "port": "12",
                    "targetIsdAs": "1-18"
                }
            },            
            "UPDATE": {
                "br1-17-1": {
                    "ip": "192.168.0.1",
                    "port": "12",
                    "targetIsdAs": "1-18"
                }
            },            
            "REMOVE": {
                "br1-17-1": {
                    "ip": "192.168.0.1",
                    "port": "12",
                    "targetIsdAs": "1-18"
                }
            }
        }
`

### /pathServerRequests
Add or receive all path requests (for SpeedCam)

#### GET
Receive a list of URLS

#### POST

POST a JSON array of strings of path requests (e.x. 1->23->24)
