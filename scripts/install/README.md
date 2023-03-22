# Armadillo as a service installation

This guide leads you step by step through the process of installing armadillo 3 as a service and configuring the components.

## Requirements
This script requires an systemd service based linux operating system. Armadillo 3 is tested on the latest LTS release of Ubuntu. For questions on other linux release you can email molgenis-operations@umcg.nl
> ## Server resources
>
> You need a server or virtual machine to deploy the Armadillo stack. The specifications of the resource are the following, depending on the participant size of the cohort you are running.
>
> | Participants  | Memory (in GB) | Diskspace (in GB) | CPU cores |
> | ------------- | -------------- | ----------------- | --------- |
> | 0-20.000      | 8              | 100               | 4         |
> | 20.000-70.000 | 16             | 100               | 4         |
> | 70.000 >      | 32             | 150               | 8         |

In case of using dsOmics this setup can be rather bigger. Please contact molgenis-operations@umcg.nl for the latest specifications.

## Software requirements

* Java 17 JRE or JDK
* Docker

In addition to these, there are other optional components you may wish to install, such as setting up nginx as a reverse proxy. 

## Domain
An domain or an hostname is required to run armadillo 3. This domain should be used for installation, for example: cohort.armadillo.domain.org

### Authentication
Before we start with the deployment of Armadillo you will need to register your domain that you are going to use with your Armadillo on the DataSHIELD authentication server. This allows you to delegate the authentication and user management. The authorization will still be under the control of the Data Manager(who gets access and who don't get access) within your armadillo installation. To registrate you will need to send a mail to `molgenis-support@umcg.nl` with the [chosen domains](#domain) and the e-mail adres of the Data Manager who is granted admin permissions in Armadillo. Also add to the mail that you want to register for the the DataSHIELD authentication server and if you belong to a project like Lifecycle, Athlete or Longitools. When the Armadillo is registrerd you will get an mail back with data that need to be inserted in step 2.


##### Step 1
Download setup script
```bash
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh
```
##### Step 2
The installation script requires some arguments:
| Argument                                    | Application   |
| ------------------------------------------- | ------------- |
| admin-user                                  | Local armadillo admin user            |
| admin-password                              | Secure password for the admin user    |
| datadir                                     | The location where the data is stored. This directory should be have enough diskspace en could be backuped (Default &rarr; /usr/share/armadillo/data)|
| domain                                      | The URL where armadillo is listening on. For example: cohort.armadillo.domain.org  |
|||
|oidc                                       | Enable OIDC, see [authentication](#Authentication) |
|oidc_url                                   | Given oidc URL |
|oidc_clientid | Given client ID|
|oidc_clientsecret | Given secret ID|

```bash
bash armadillo-setup.sh --admin-user admin --admin-password xxxxxxxx --domain armadillo.cohort.study.com --oidc --oidc_url https://lifecycle-auth.molgenis.org --oidc_clientid xxxxx --oidc_clientsecret xxxx'
```

#### Step 3
After installation armadillo is listening on port 8080. 
Test the setup on http://domain:8080 or on the localhost http://localhost:8080

# ProxyPass  SSL / certificates
Armadillo 3 is a standalone application wich is listening on port 8080. 
You can set up a front-end proxy if you'd like to proxypass to this port. This can be a simple HTTP server, something like Apache HTTPD or nginx, These can be useful for managing multiple URLs or sites through a single server machine, configuring HTTPS with SSL certificates without involving Armadillo. This is completely optional.

We have an example for nginx: [Example](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/conf/armadillo-nginx.conf)


### Updates
The above setup script is creating an update check. Every week (cron) armadillo checks and installing the latest updates of armadillo.

### Backups
A good start for backuping data is the /usr/share/armadillo and /etc/armadillo. If you gave another datadir as setup option you also should backup this directory. For disaster backups you should contact your IT department.

## What's next?

* [For the server owner or data manager who need to put data on to the server](https://molgenis.github.io/molgenis-r-armadillo/)
* [For the researcher who want to start analyzing the data on the server](https://molgenis.github.io/molgenis-r-datashield/)
