# Quick Start 
MOLGENIS Armadillo facilitates federated analysis using <a href="https://datashield.org/" target="_blank">DataSHIELD</a>. 
To learn more about DataSHIELD, visit their website or our <a href="/pages/basic_concepts/" >Basic Concepts page</a>.

First we need to determine what kind of user you are:

1. :material-file-table: [Datamanager](#data-manager)

2. :material-server: [System Operator](#system-operator)

3. :material-layers-search: [Researcher](#researcher)

4. :fontawesome-solid-laptop-code: [Developer](#developer)

## Data manager
Data management can be done in different ways: the Armadillo User Interface, the armadillo R client, or using DsUpload. 

### User interface
In the armadillo user interface, data managers can login and manage users, projects, profiles and see logs from there.
The user interface is especially useful for managing users and viewing logs. 
To get to know more about the UI, visit the [Usage examples page](examples_usage.md).
![ui-projects.png](../img/ui-projects.png)

Please note that the user interface is admin only. Users without admin permissions, will get the following message:

![Warning message for non admin users](../img/ui-non-admin-message.png){ width="700" }
/// caption
You are logged in but you don't have permission to access the Armadillo user interface.
///

### Armadillo R client
Data can also be managed using the Armadillo R client. The following code block is an example of how to create a project
and upload data. 

```R
library('MolgenisArmadillo')
# Login
armadillo.login("https://armadillo-url-example.org")

# Load the iris dataset to upload as test
library(datasets)

# Create a project called "project"
armadillo.create_project("project")

# Upload the data in a folder called "folder"
armadillo.upload_table("project", "folder", iris)
```

Data is organised in projects. These projects can be compared to folders on the filesystem of your computer. 
Users can be granted access to specific projects. Within those projects, data has to be organised in folders.

![Project file structure](../img/project-file-structure.png){ width="500" }
/// caption
A typical project structure looks like this.
///


### DsUpload
[DsUpload](https://lifecycle-project.github.io/ds-upload/) is an R package that aids data managers in the data uploading
process. Data uploaded using this package has to fit the 
[DsDictionaries](ttps://github.com/lifecycle-project/ds-dictionaries/blob/master/README.md) format. 

## System Operator
System Operators are the ones that install the software (Molgenis Armadillo) on the server. Although installing 
armadillo can be done rather quickly, we would like to refer you to our [Install Guide](install_management.md), 
as we keep that one as concise and straightforward as possible. 

## Researcher
If you're doing research with Armadillo, you will need to have access to the Armadillo instance of the cohort that hosts
the data you want to work with. This guide assumes you have access to the data and that you have the URL of the
armadillo instance. 

Research can be done using R. The R package [DSMolgenisArmadillo](https://molgenis.github.io/molgenis-r-datashield/)
is used connect to data hosted using MOLGENIS Armadillo. The cohort that hosts the Armadillo instance will provide you
with a URL. Simply install the package using:
```R
install.packages("DSMolgenisArmadillo")
```
To be able to do basic research with DataSHIELD, we also need `DsBaseClient`: the DataSHIELD R package that provides
basic functionality required for research with DataSHIELD. You can install it using:
```R
install.packages('dsBaseClient', repos=c(getOption('repos'), 'http://cran.obiba.org'), dependencies=TRUE)
```
Now you can load the libraries:
```R
library(dsBaseClient)
library(DSMolgenisArmadillo)
```

With these libraries, you can now login to armadillo:
```R
url <- "https://armadillo-demo.molgenis.net/"
token <- armadillo.get_token(url)
builder <- DSI::newDSLoginBuilder()

builder$append(
  server = "armadillo",
  url = url,
  token = token,
  driver = "ArmadilloDriver",
  profile = "xenon")
  
logindata <- builder$build()
conns <- DSI::datashield.login(logins = logindata)
```
This example assumes you're using our demo server as armadillo URL, but of course this can be any armadillo server.

Now you can assign your data, an example of doing that looks like this:
```R
datashield.assign.table(conns, "mtcars", "project/data/cars")
```
To see the data assigned in your workspace use:
```R
ds.ls()
```
If all of this succeeds, your access to Armadillo is setup correctly. For more extensive documentation, please visit
our documentation for [`DSMolgenisArmadillo`](https://molgenis.github.io/molgenis-r-datashield/).

## Developer
We heavily encourage fellow developers to help us with new features or bugfixes in armadillo, or help us with our R 
packages. To run armadillo locally, first clone the git repository:
```shell
git clone https://github.com/molgenis/molgenis-service-armadillo.git
```
You first need to configure armadillo in the `application.yml`. To do that, you first will need to create this file. 
The easiest way to do that is by copying the 
[application.template.yml](https://github.com/molgenis/molgenis-service-armadillo/blob/master/application.template.yml)
and name it `application.yml`. 

If you're using IntelliJ, open the `ArmadilloServiceApplication` class (press shift twice and type the class name to go
there), and press the play button on the main function. You might have to set the Java version (java 17). Armadillo 
will start up without oAuth configured this way, so you can login using the username `admin` and the password set in 
`application.yml` (default: `admin`). 

For more information, see our [Developer guides](dev_guides.md) and [License](license.md). 