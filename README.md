# SCPB


A simple REST service to register new clients on a prometheus server. Also a proxy for path server requests and the 
speed cam server.

## Usage
`java -jar SCBP.jar PARAMETERS `

Possible parameters are:
+ `-pc` or `--prometheus-config`: Path to the prometheus configuration (for example prometheus.yml).
+ `-pu` or `--prometheus-url`: The URL of the prometheus server to access the web hook to reload configuration. 
+ `-p` or `--port`: The port the REST service listens to. Default is 7543.

`-pc` and `-pu` must be both set or otherwise it will create an exception on start. If you do not set any of them, SCPB will run without interacting with a Prometheus server.

## Requirements
- Java 8

Prometheus in 2.X only when SCPB shall update the prometheus.yml file. The server must be started with `--web.enable-lifecycle`.

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
