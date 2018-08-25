
### Local API Endpoint
 - /api/v1/user

### How to build
`./mvnw clean package`

### How to run
`java -jar target/REISystems-USCIS-ODOS2-user-0.0.1.jar`

### How to test
`./mvnw test`

### How to run code sonar analysis 
`./mvnw sonar:sonar`

### Building docker image
`docker build -t uscis-odos/user`

### Deploy through to openshift
`cd openshift`
`oc process -f deploy-template.yaml --param-file deploy-params-dev | oc create -f -`
