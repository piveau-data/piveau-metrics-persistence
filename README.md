# Metrics Hub to PostgreSQL

Temporary service that fetches linked data for storage in a the old MQA PostgreSQL database

## Setup

1. Install all of the following software
        
* Java JDK >= 1.8
* Git >= 2.17
  
2. Clone the directory and enter it
        
3. Edit the environment variables in the `Dockerfile` to your liking. Variables and their purpose are listed below:
   
| Key | Description | Default |
| :--- | :--- | :--- |
| PORT | Port this service will run on | 8087 |
| HOST | Host name this service will run on, required for URL check to work (callbacks) | null |
| URL_CHECK_ENDPOINT | Full endpoint to which URL check requests shall be sent | null |
| PIVEAU_HUB_HOST | Host name of Piveau Hub |  |
| PIVEAU_HUB_PORT | Port number of Piveau Hub |  |
| PIVEAU_HUB_PAGE_SIZE | Number of elements fetched at once from endpoints supporting pagination | 100 |
| PGSQL_SERVER_HOST | PostgreSQL server address, including port and database instance | jdbc:postgresql://localhost:5432/mqa |
| PGSQL_USERNAME | PostgreSQL user instance | postgres | 
| PGSQL_PASSWORD | PostgreSQL password | postgres |
| PIVEAU_PIPE_LOG_LEVEL | Log level | INFO |

        
## Run

### Production

Build the project by using the provided Maven wrapper. This ensures everyone this software is provided to can use the exact same version of the maven build tool.
The generated _fat-jar_ can then be found in the `target` directory.

* Linux
    
        ./mvnw clean package
        java -jar target/hub-to-postgres-1.0-fat.jar

* Windows

        mvnw.cmd clean package
        java -jar target/hub-to-postgres-1.0-fat.jar
      
* Docker

    1. Start your docker daemon 
    2. Build the application as described in Windows or Linux
    3. Adjust the port number (`EXPOSE` in the `Dockerfile`)
    4. Build the image: `docker build -t piveau/hub_to_postgres .`
    5. Run the image, adjusting the port number as set in step _iii_: `docker run -i -p 8087:8087 piveau/hub_to_postgres`
    6. Configuration can be changed without rebuilding the image by overriding variables: `-e PORT=8088`


### Development

For use in development two scripts are provided in the project's root folder. These enable hot deployment (dynamic recompiling when changes are made to the source code).
Linux users should run the `redeploy.sh` and Windows users the `redeploy.bat` file.


## API

A formal OpenAPI 3 specification can be found in the `src/main/resources/webroot/openapi.yaml` file.
A visually more appealing version is available at `{url}:{port}` once the application has been started.