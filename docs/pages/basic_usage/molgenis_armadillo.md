# MolgenisArmadillo

Relevant for: :material-file-table:{title="Data Managers"}

MolgenisArmadillo is a R library that can be used by data managers to share datasets on a MOLGENIS Armadillo server. Researchers can then analyse these datasets and datasets shared on other servers using DataSHIELD. Researchers will only be able to access aggregate information and cannot see individual rows.

For more detailed information please visit [MolgenisArmadillo](https://molgenis.github.io/molgenis-r-armadillo/index.html).

Below some basic operations to get you started using MolgenisArmadillo:

=== "Prerequisite"

    ???+ example

        ```r linenums="1" title="Prerequisite.r"
        # install the following packages
        install.packages("MolgenisArmadillo")
        install.packages("dplyr")
        install.packages("arrow")
        # After installation make sure you can load the libraries
        library(MolgenisArmadillo)
        library(dplyr)
        library(arrow)
        # verify the loaded libraries (see: other attached packages)
        sessionInfo()
        ```
=== "Basic usage"

    ???+ example

        ```r linenums="1" title="MolgenisArmadillo.r"
        ## Prerequisites
        #
        # - Run the code in Prerequisite.r
        #
        library(MolgenisArmadillo)

        ## 
        # To share your data using Armadillo, you first need to login

        ## Login
        #
        # In order to access the files as a data manager, you need to log in. 
        # The login method needs the URLs of the Armadillo server.
        # It will open a browser window where you can identify yourself with the ID provider.
        # 
        # If you are unsure how this login function (or any function) works ask R to provide documentation
        ?armadillo.login

        armadillo.login(
            armadillo = "https://armadillo.test.molgenis.org"
        )
        #token <- armadillo.get_token("https://armadillo.test.molgenis.org")

        # armadillo.login will create a session and store the credentials in the environment.

        ## Structure
        # To share data via Armadillo you can have a nested structure to save you data.

        # We distinguish:
        # - projects
        # - folders
        # - tables

        ### Projects
        # Projects are root-folders you can give persons permissions on. 
        # you can imagine that you will use a separate project for each study you need to support. 
        # This way you make sure people can not see each others variables.

        ### Folders
        # Folder objects can be used to version the different tables you want to share in Armadillo. 
        # This is not mandatory and are free to use the folder level as you see fit. 
        # In our examples we will go into the versioning part a bit deeper.

        ### Tables 
        # Tables contain the data you want to share. 
        # This can be all the data on a certain subject, mostly used in consortia or a specific study you want to expose.

        ## Sharing data
        # Assume you are in a consortia which has core-variables and outcome-variables. 
        # You want to share and version the whole dataset to all researchers which applied to access your data.

        # First we will create the project. In our case "ipec".

        cohort <- "workshop2"
        armadillo.create_project(cohort) 

        # Secondly we will load the table(s) we want to upload to Armadillo in the R-environment. 
        # We have test data which is in `arrow` format, the upload will take any object that has a table like structure to upload into the Armadillo. 
        # This can be SPSS, STATA, SAS or R-based data as well.

        library(arrow)

        # load the core data (make sure you are working in the correct directory)
        nonrep <- arrow::read_parquet("data/nonrep.parquet")
        yearlyrep <- arrow::read_parquet("data/yearlyrep.parquet")

        # explore your data (sanity check)
        View(nonrep)
        dim(nonrep)
        names(nonrep)
        table(nonrep$recruit_age)

        # The third step is determining the second level, which contains in this case the datamodel-version the type of variables and the data-version.

        # y_y-#variable-type#-x_x
        
        # y_y = datamodel version
        # x_x = data version

        # upload the core variables
        armadillo.upload_table(cohort, "2_1-core-1_0", nonrep)
        armadillo.upload_table(cohort, "2_1-core-1_0", yearlyrep)

        ## Looking at the data
        # There are helper functions to help you determine what is in the storage server. 
        # You can list projects and tables to what's in the storage.

        # list of projects
        armadillo.list_projects()

        # listing tables per project
        armadillo.list_tables(cohort)

        # You can download the data in the R-environment as well.

        # download table to local R environment
        download_nonrep <- armadillo.load_table(cohort, "2_1-core-1_0", "nonrep")

        # check the column names from the local environment
        colnames(download_nonrep)

        # check if local data and uploaded data are equal (optional)
        setequal(nonrep, download_nonrep)

        # Now you can also take a look at the files in the user interface of the MinIO file server
        # open the MinIO server URL in a browser window (used in armadillo.login).

        # > !IMPORTANT: run this part after subsetting the data

        ## Deleting the data 
        # To delete the data you need to throw away the contents first.

        # throw away the core tables
        armadillo.delete_table(cohort, "2_1-core-1_0", "nonrep")
        armadillo.delete_table(cohort, "2_1-core-1_0", "yearlyrep")

        # Now you can delete the project.
        armadillo.delete_project(cohort)
        ```
=== "Create subsets"

    ???+ example

        ```r linenums="1" title="Creating_data_subsets_Armadillo.r"
        ## Creating data subsets on the Armadillo server
        # When researchers request access to your data they may in many cases not be granted access to the whole data set, but only to a subset. 
        # Here you can see the different relevant steps you need to take to create these subsets.
        #
        # - Setting up the environment
        # - Logging into the servers
        # - Exploring the data
        # - Making subsets of the data
        # - Upload data subsets
        #
        # - Delete data subsets

        ## Setting up the environment
        # - Run the code in 0_Prerequisite.r

        # load required libraries
        library(MolgenisArmadillo)
        library(dplyr)

        ## Logging in to the servers
        # In order to access the files you need to log in using the URLs of the Armadillo server. 
        # A browser window will be opened where you can identify yourself with the ID provider.

        armadillo.login(
            armadillo = "https://armadillo.test.molgenis.org"
        )
        # A session will be created and the credentials are stored in the environment.

        ## Explore the data
        # Let's assume you are in a consortium which has core-variables and outcome-variables. 
        # You want to share a subset of the whole data set with certain researchers that applied for access to your data.

        # List projects on the Armadillo server.
        armadillo.list_projects()

        # Next create a study, here called 'subset1'.
        # NOTE: change the name of the subset for your own practice run

        study <- "study1"
        armadillo.create_project(study)

        # List the tables in a project

        # You want to share data from the cohort you just uploaded (MolgenisArmadillo.r). 
        # Change the cohort_name to your own cohort name here:
        cohort = "workshop1"

        # List the available tables within this project.
        armadillo.list_tables(cohort)

        # Subset the core variables
        # Download the relevant core tables to the local environment

        nonrep <- armadillo.load_table(cohort, "2_1-core-1_0", "nonrep")
        yearlyrep <- armadillo.load_table(cohort, "2_1-core-1_0", "yearlyrep")

        # List their variables

        colnames(nonrep)
        colnames(yearlyrep)

        # Subset the variables that were requested per table.

        subset_core_nonrep <- nonrep %>% select(child_id, asthma_m, preg_cig, preg_fever, preg_alc)
        subset_core_yearlyrep <- yearlyrep %>% select(child_id, cohab_, smk_exp)

        ## Uploading the data subset
        # Check the variables in the data subset before uploading

        colnames(subset_core_nonrep)
        colnames(subset_core_yearlyrep)

        # Upload the data subsets
        armadillo.upload_table(study, "2_1-core-1_0", subset_core_nonrep, "nonrep")
        armadillo.upload_table(study, "2_1-core-1_0", subset_core_yearlyrep, "yearlyrep")

        # See if tables are uploaded
        armadillo.list_tables(study)

        # Now you can also take a look at the files in the user interface of the Armadillo server. In this case: https://armadillo.test.molgenis.org/#/projects

        # > !IMPORTANT: run this part after subsetting the data

        ## Deleting the data 
        # To delete the data you need to throw away the contents first.

        # throw away the core tables
        armadillo.delete_table(study, "2_1-core-1_0", "nonrep")
        armadillo.delete_table(study, "2_1-core-1_0", "yearlyrep")

        # Now you can delete the project.

        armadillo.delete_project(study)
        ```

!!! note
    The example files used in the examples above can be found here: [nonrep.parquet](../../data/nonrep.parquet), [yearlyrep.parquet](../../data/yearlyrep.parquet)
