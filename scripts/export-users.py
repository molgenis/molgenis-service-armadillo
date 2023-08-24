"""
User export script to aid in migration

This script gets users and their assigned cohorts from Fusion Auth to Armadillo.
It will group each user by cohort and will export it as TSV to a given export location, so that upon
request this TSV can be used to set users to their appropriate cohorts.
"""

import os
import argparse
import warnings
from pathlib import Path
from getpass import getpass
from datetime import datetime

import numpy as np
import pandas as pd
from fusionauth.fusionauth_client import FusionAuthClient


def main():
    fusion_url, output = CommandLineParser().parse()
    cohort_data = FusionClientParser(fusion_url).parse()
    Exporter(output).export_cohorts(cohort_data)


class Exporter:
    def __init__(self, output_path):
        self.output_path = output_path

    def export_cohorts(self, cohorts):
        for cohort_identifier, cohort_data_object in cohorts.items():
            cohort_data_object.data.to_csv(
                os.path.join(
                    self.output_path,
                    self._make_output_filename(cohort_data_object.name)
                ),
                index=False,
                sep='\t'
            )

    def _make_output_filename(self, cohort_data_object_name):
        """
        Classifies whenever the object name is None, which should return the uncategorized users
        or not None, in which case the filename should be whatever the cohort name is.
        """
        if cohort_data_object_name is None:
            return "uncategorized_users.tsv"
        else:
            return self._make_output_filename_from_object_name(cohort_data_object_name)

    @staticmethod
    def _make_output_filename_from_object_name(cohort_data_object_name):
        """
        Function to make an output filename from the cohort name. Replaces spaces with underscores,
        replaces the / (or backslash) with an or and adds .tsv to the end.
        """
        return cohort_data_object_name.rstrip(
            " "
        ).replace(
            " ", "_"
        ).replace(
            "/", "or"
        ).replace(
            "\\", "or"
        ) + '.tsv'


class FusionClientParser:
    def __init__(self, fusion_url):
        self.client = FusionClient(fusion_url)
        self.cohort_dict = {}
        self.users_not_parsed = []
        self.columns_of_interest = ['active', 'email', 'firstName', 'fullName', 'lastName']

    def parse(self):
        """
        Caller function to call all the obtaining, parsing of cohorts.
        Also calls to obtain, parse and process of the users, which in turn maps the users to
        cohorts.
        """
        self._process_cohorts(self.client.get_cohorts())
        self._parse_users(self.client.get_users())
        self._warn_empty_cohorts()
        return self.cohort_dict

    def _warn_empty_cohorts(self):
        """
        Verbose function to warn user about empty cohorts and their names.
        """
        empty_cohorts = []
        for cohort_id, cohort_data_object in self.cohort_dict.items():
            if cohort_data_object.is_empty():
                empty_cohorts.append(cohort_data_object.name)
        if len(empty_cohorts) > 0:
            warnings.warn(
                f"There are {len(empty_cohorts)} cohorts without users present: "
                f"{', '.join(empty_cohorts)}"
            )

    def _warn_unparsed_users(self):
        """
        Verbose function to warn datamanager about users that are in cohorts, but could not be
        mapped to the known cohorts (these users ARE registered in 1 or more cohorts,
        since "registrations" is not NaN).
        """
        if len(self.users_not_parsed) > 0:
            warnings.warn(
                f"There are {len(self.users_not_parsed)} users that could not be "
                f"mapped to a cohort (but they ARE registered in 1 or multiple cohorts!): "
                f"{', '.join(self.users_not_parsed)}"
            )

    def _process_cohorts(self, cohorts):
        """
        Simple caller function to process all cohorts. Also adds the NaN cohort for all users that
        are not in an Application.
        """
        self.cohort_dict['nan'] = CohortUserDataObject(None)
        for identifier in cohorts['id']:
            name = cohorts.loc[cohorts[cohorts['id'] == identifier].index, "name"].values[0]
            self.cohort_dict[identifier] = CohortUserDataObject(name)

    def _parse_users(self, users):
        """
        Bread and butter, core function of the script. First calls to parse unregistered users,
        then loops through all user IDs, maps back the user to the ID,
        then obtains all registrations for that user,
        checks if the applicationId is already known by the class,
        if so then adds this single user to that particular data object.
        Nothing happens if a user cannot be mapped to an already known Application.
        """
        self._parse_unregistered_users(users)
        user_mail = False
        for user in users['id'].values:
            current_user = users.loc[users[users['id'] == user].index, :]
            user_mail = current_user.iloc[0, :]['email']
            user_registrations = list(current_user['registrations'].values[0])
            for registration in user_registrations:
                identifier = registration['applicationId']
                if identifier in self.cohort_dict.keys():
                    # Setting user_name to false, so that it doesn't get added to unparsed users
                    user_mail = False
                    if "roles" not in registration.keys():
                        roles = np.nan
                    else:
                        roles = self._process_roles(registration['roles'])
                    self.cohort_dict[identifier].add_user(current_user[self.columns_of_interest])
                    self.cohort_dict[identifier].add_user_roles(
                        current_user['email'].values[0],
                        roles
                    )
        if user_mail:
            self.users_not_parsed.append(user_mail)

    @staticmethod
    def _process_roles(roles):
        """
        Simple function to prevent Pandas from raising IndexError when supplied with a list for
        the roles property of a user.
        """
        if isinstance(roles, list):
            return ', '.join(roles)
        else:
            return roles

    def _parse_unregistered_users(self, users):
        """
        Required function to parse all users that have NaN in the "registrations" column of the
        Users API call, since this does not cooperate well when obtaining "ApplicationId" from
        registrations if NaN is present.
        """
        unregistered_users = users.loc[users[users['registrations'].isna()].index, :]
        users.drop(index=users[users['registrations'].isna()].index, inplace=True)
        self.cohort_dict['nan'].add_data(unregistered_users[self.columns_of_interest])
        self.cohort_dict['nan'].data['roles'] = np.nan


class CohortUserDataObject:
    """
    Data Object resting on Pandas to store information regarding cohorts.
    To be used as storing in a dictionary, with its key the "Application ID"  and its value this
    data object. Within this data object the name of that Application ID can also be set, but also
    a complete user set (for instance, all users without Applications) or each user individually.
    Roles for user email addresses can also be set.
    """
    def __init__(self, name):
        self.name = name
        self.data = pd.DataFrame()

    def add_data(self, user_data):
        """
        Function to add a complete dataset to the data object, in case of all
        users without Applications for instance.
        """
        self.data = user_data.copy(deep=True)

    def add_user(self, user):
        """
        Function to add each user individually.
        Simple if statement to see if the data property is already set or needs to be set.
        """
        if self.data.shape[1] == 0:
            self.data = user.copy(deep=True)
        else:
            self.data = pd.concat([self.data, user])

    def add_user_roles(self, user_email, roles):
        """
        Function to add a role to a user email address.
        """
        self.data.loc[self.data[self.data['email'] == user_email].index, 'roles'] = roles

    def is_empty(self):
        """
        Simple function to get if the data property is empty or not.
        """
        if self.data.shape[0] == 0:
            return True
        else:
            return False


class FusionClient:
    def __init__(self, fusion_url):
        api_key = getpass("Fusion Auth API Key:")
        self.client = FusionAuthClient(api_key, fusion_url)

    def get_users(self):
        """
        FusionClient API call to retrieve all users from the Fusion URL (and their Applications)
        """
        response = self.client.search_users_by_query({
            "search": {
                "queryString": "*",
                "startRow": 0,
                "numberOfResults": 10000
            }
        })
        result = self._get_result_or_exit(response)
        users = pd.DataFrame(result["users"])
        return users

    def get_cohorts(self):
        """
        FusionClient API call to retrieve the "Applications" (AKA, cohorts)
        """
        response = self.client.retrieve_applications()
        result = self._get_result_or_exit(response)
        applications = pd.DataFrame(result["applications"])
        return applications

    @staticmethod
    def _get_result_or_exit(response):
        """
        Function obtained from migration script to return a successful response, but to raise, fail
        and exit(1) if the response is not successful.
        """
        if response.was_successful():
            return response.success_response
        else:
            res = response.response
            res.raise_for_status()
            exit(1)


class CommandLineParser:
    def __init__(self):
        self.interface = CommandLineInterface()

    def parse(self):
        """
        Parse function for the Command Line Interface. Gets the CLA from the CLI class,
        validates and then processes the CLA.

        Returns:
            tuple:
                Returns a tuple containing [0] the parsed fusion url and [1] the absolute output
                directory.
        """
        fusion_url = self._parse_url(self.interface.get_argument("fusion_auth"))
        output = self._parse_output(Path(self.interface.get_argument("output")))
        return fusion_url, output

    @staticmethod
    def _parse_url(url_argument):
        """
        Parser function for the fusion URL. Includes stripping the / at the end of a URL (
        See https://github.com/FusionAuth/fusionauth-python-client/issues/12 ) and adding a scheme
        to the URL if not supplied.
        """
        if url_argument.endswith("/"):
            url_argument = url_argument.rstrip("/")
        if not url_argument.startswith("http"):
            warnings.warn("CLI supplied argument did not contain scheme, adding https://")
            url_argument = "https://" + url_argument
        return url_argument

    @staticmethod
    def _parse_output(output_argument):
        """
        Parser for the output directory.
        Will make the output path absolute and try to create the output folder if it does not exist.
        Will error if the parent of the output folder does not exist.
        Will also make a folder inside the output folder, named after the date of today in format:
        YYYY-MM-DD to keep better track of exports.
        """
        # Absolute so that it works with ./output or similar
        output_argument = output_argument.absolute()
        if not os.path.isdir(output_argument):
            if not os.path.isdir(output_argument.parent):
                raise OSError(
                    f"Cannot create output directory {output_argument}, parent does not exist."
                )
            else:
                os.mkdir(output_argument)
        output_folder_name = str(datetime.now().strftime("%Y_%m_%d"))
        full_output = os.path.join(output_argument, output_folder_name)
        if not os.path.isdir(full_output):
            os.mkdir(full_output)
        return full_output


class CommandLineInterface:
    def __init__(self):
        parser = self._create_argument_parser()
        self.arguments = parser.parse_args()

    @staticmethod
    def _create_argument_parser():
        """
        Creation of the Command Line Arguments, and whenever they should be displayed as
        "required" or not.
        """
        parser = argparse.ArgumentParser(
            prog="Export cohort users",
            description="Utilitarian script of the Armadillo migration "
                        "process to export users grouped by cohorts."
        )
        required = parser.add_argument_group("Required arguments")
        required.add_argument(
            "-f",
            "--fusion-auth",
            type=str,
            required=True,
            help="The fusion auth URL (URL to export the users per cohort from)"
        )
        required.add_argument(
            "-o",
            "--output",
            type=str,
            required=True,
            help="The output directory in which the cohort TSV files should be placed. "
                 "Please note that a folder of the current date in format YYYY_MM_DD will be made "
                 "in this output folder."
        )
        return parser

    def get_argument(self, argument_key):
        """
        Small getter function for CLI, including some error handling.
        """
        if argument_key in self.arguments:
            return getattr(self.arguments, argument_key)
        else:
            raise KeyError(f"Argument {argument_key} invalid, not found in CLI arguments.")


if __name__ == "__main__":
    main()
