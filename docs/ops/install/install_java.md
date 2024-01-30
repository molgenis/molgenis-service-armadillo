## Run Armadillo using java commandline
Software developers often run Armadillo as java jar file: 

1. Install Java and Docker (for the DataSHIELD profiles)
2. Download Armadillo jar file from [releases](https://github.com/molgenis/molgenis-service-armadillo/releases), for example:
[molgenis-armadillo-3.3.0.jar](https://github.com/molgenis/molgenis-service-armadillo/releases/download/V3.3.0/)
3. Run armadillo using ```java -jar molgenis-armadillo-3.3.0.jar```
4. Go to http://localhost:8080 to see your Armadillo running.

Default Armadillo will start with only 'basic-auth' and user 'admin' with password 'admin'. You can enable 'oidc' for connecting more users. You can change 
by providing and editing [application.yaml](application.template.yml) file
in your working directory and then run command above again.

