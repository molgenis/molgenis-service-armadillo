## Creating data subsets on the Armadillo MiNIO file server
# When researchers request access to your data they may in many cases not be granted access to the whole data set, 
# but only to a subset. On the MinIO file server access is regulated on the project level, 
# so you will need to create a new project using a subset of the data. 
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
# In order to access the files on the MinIO fileserver you need to log in using the URLs of the Armadillo server and the MinIO fileserver. 
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

# You want to share data from the cohort you just uploaded (1_MolgenisArmadillo.r). 
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
