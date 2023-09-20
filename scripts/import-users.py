#!/usr/bin/env python3

import os
import re
import base64
import argparse
import warnings
from pathlib import Path
from getpass import getpass

import requests
import pandas as pd


def main():
    server, data_directory = CommandLineParser().parse()
    api = API(server)
    cohorts = ProjectDataParser().parse(data_directory)
    ProjectProcessor(api).parse(cohorts)


class ProjectDataParser:
    def parse(self, directory):
        """
        Method to parse all TSVs in a user supplied directory, assuming all those TSVs are export
        TSVs from the export script. Returns a dictionary with a (parsed and sanitized) cohort name
        as key and it's user data as value.

        Args:
            directory:
                Absolute path to the directory containing the user cohort export TSVs.

        Returns:
            dict:
                Returns a dictionary containing (key) cohort name, parsed to adhere to AWS
                bucket standards and (value) cohort user data.
        """
        return_dict = {}
        files = os.listdir(directory)
        for file in files:
            if not file.endswith((".tsv", ".tsv.gz")):
                continue
            full_path = os.path.join(directory, file)
            project_name = self._parse_filename(file)
            return_dict[project_name] = self._load_tsv(full_path)
        return return_dict

    @staticmethod
    def _parse_filename(filename):
        """
        Function to parse the filename to the project name.

        Args:
            filename:
                The string of the filename of the TSV.
        Returns:
            string:
                Returns a string that is AWS bucket naming compliant.
        """
        # First, break off file extension
        filename = filename.replace(".tsv.gz", "")
        filename = filename.replace(".tsv", "")
        # Then create an intermediate with only valid characters
        name = "".join(
            [c for c in filename.replace("-", "") if c.isalnum() or c == "_"]
        ).strip()
        # Then clean up starting underscores
        name = re.sub(r"^_*", "", name)
        # Replacing one or more underscores with -
        name = re.sub(r"_+", "-", name)
        return name.lower()

    @staticmethod
    def _load_tsv(path):
        """
        Method to load in the pandas TSV, even if the TSV is just headers or not even headers.
        Args:
            path:
                Absolute path to the (gzipped) TSV file.
        Returns:
            dataframe:
                Returns a pandas.DataFrame containing the columns of a loaded in pandas TSV or
                empty dataframe containing the correct columns.
        """
        try:
            data = pd.read_csv(
                path,
                sep='\t',
                dtype={
                    "active": bool,
                    "email": str,
                    "firstName": str,
                    "fullName": str,
                    "lastName": str,
                    "roles": str
                }
            ).fillna("")
        except pd.errors.EmptyDataError:
            data = pd.DataFrame(
                columns=["active", "email", "firstName", "fullName", "lastName", "roles"]
            )
        return data


class ProjectProcessor:
    def __init__(self, api):
        self.api = api
        self.superusers = []
        self.current_superusers = self._obtain_current_superusers(api)

    def parse(self, cohort_data):
        """
        Bread and butter of the script. This function parses the cohort data dictionary into the
        API, creating new projects and users alike. Also sets users admin rights accordingly,
        as they were according to the export TSVs.

        Args:
            cohort_data:
                Dictionary containing (key) the cohort name and (value) its user data as a pandas
                dataframe.
        """
        for cohort_name, cohort_users in cohort_data.items():
            # Skip users that are not categorized into a cohort
            if cohort_name == "uncategorized-users":
                continue
            self._obtain_su(cohort_users)
            self._put_project(name=cohort_name, users=cohort_users['email'].values.tolist())
        self._process_uncategorized_users(cohort_data['uncategorized-users'])
        self._add_admin_rights()

    @staticmethod
    def _obtain_current_superusers(api):
        """
        Method to obtain the already present superusers / admins from the target URL, to make sure
        that current admins do not lose their privilege.

        Args:
            api:
                Initiated class of the API present in this file.
        Returns:
            list:
                Returns a list containing the email address of all users currently marked as admin
                in the target armadillo URL.
        """
        super_users = []
        users = api.get_users()
        for user in users:
            if user["admin"]:
                super_users.append(user['email'])
        return super_users

    def _put_user(self, email, admin, firstname="", lastname=""):
        """
        Method primarily designed to update a given user their email.

        Args:
            email:
                String of the users email address.
            admin:
                Boolean whenever the user should become an admin or not.
            firstname:
                [Optional] String of the user's first name.
            lastname:
                [Optional] String of the user's last name.
        """
        self.api.put_user({
              "email": email,
              "firstName": firstname,
              "lastName": lastname,
              "admin": admin
        })

    def _put_project(self, name, users):
        """
        Method to call the put command
        Args:
            name:
            users:

        Returns:

        """
        self.api.put_project({"name": name, "users": users})

    def _obtain_su(self, cohort_users):
        """
        Method to obtain all marked superusers / admins from a cohort and store them, to at the end of parsing re-add
        admin rights to the users that used to have it. According to the roles "ADMIN", "admin" and "SU", the user will
        be marked to become admin in armadillo 3. The reason it adds to an init variable instead of return the list is
        because of the uncategorized users, which have to be processed separately, but still added to the admin list
        to make sure they don't lose or not obtain their admin rights.

        Args:
            cohort_users:
                Pandas.dataframe object of the users of the cohort_data. Can be empty.
        """
        su = cohort_users.loc[
                cohort_users[cohort_users['roles'].isin(("SU", "ADMIN", "admin"))].index,
                "email"
            ]
        for user in su:
            if user not in self.superusers:
                self.superusers.append(user)

    def _process_uncategorized_users(self, uncategorized_users):
        """
        Method to add the "uncategorized_users.tsv" users that are registered, but are not assigned to any cohort
        to the armadillo 3 server.

        Args:
            uncategorized_users:
                Pandas.dataframe object of the users of key "uncategorized-users" of the cohort_data dictionary.
        """
        self._obtain_su(uncategorized_users)
        uncategorized_users.apply(
            lambda x: self._put_user(
                email=x['email'],
                admin=x['email'] in self.current_superusers,
                firstname=x['firstName'],
                lastname=x['lastName']
            ), axis=1
        )

    def _add_admin_rights(self):
        """
        Method to be used last after all users and cohorts have been added, to give back admin rights to the users
        that used to have it.
        """
        for user in self.superusers:
            self._put_user(email=user, admin=True)


class API:
    def __init__(self, server):
        self.target = server
        self.header = self._create_authorazation_header()

    @staticmethod
    def _create_authorazation_header():
        """
        Method to create the authentication header and content-type header, based on whenever the
        user supplies a Token (from a user that is admin ofcourse) or basic auth.

        Returns:
            json:
                JSON containing the 'Content-Type': 'application/json' and the 'Authorization'
                based on user input.
        """
        token = getpass("Armadillo 3 token:")
        if token == "":
            warnings.warn("Token not supplied, requiring basic auth password!")
            password = getpass("Basic auth password:")
            auth_header = "Basic " + base64.b64encode(("admin:" + password).encode('UTF-8')).decode("UTF-8")
        else:
            auth_header = "Bearer " + token
        return {"Content-Type": "application/json", "Authorization": auth_header}

    def put_user(self, data):
        """
        Requests function to execute the curl 'PUT' command to access/users.

        Args:
            data:
                JSON of the user that needs to be added or updated.
                See swagger UI for format.
                (URL: <url of armadillo>/swagger-ui/index.html)
        Raises:
            HTTPError:
                HTTPError is raised when HTTP error codes 400+ are obtained as a response.
        """
        response = requests.put(self.target + "access/users", json=data, headers=self.header)
        response.raise_for_status()

    def put_project(self, project):
        """
        Requests function to execute the curl 'PUT' command to access/projects.

        Args:
            project:
                JSON of the project that needs to be added or updated (very likely added).
                See swagger UI for format.
                (URL: <url of armadillo>/swagger-ui/index.html)
        Raises:
            HTTPError:
                HTTPError is raised when HTTP error codes 400+ are obtained as a response.
        """
        response = requests.put(self.target + "access/projects", json=project, headers=self.header)
        response.raise_for_status()

    def get_users(self):
        """
        Requests function to execute the curl 'GET' command to obtain all users from target.

        Returns:
            json:
                Returns a json containing the users response.
                See swagger UI for format.
                (URL: <url of armadillo>/swagger-ui/index.html)
        """
        response = requests.get(self.target + "access/users", headers=self.header)
        response.raise_for_status()
        return response.json()


class CommandLineParser:
    def __init__(self):
        self.parser = CommandLineInterface()

    def parse(self):
        """
        Parser function of CommandLineParser. Will obtain the CLA, checks their validity and
        mutates them to be compliant in the rest of the code.

        Returns:
            tuple:
                Returns a tuple containing [0] the server url and [1] the absolute path to the TSV
                directory.
        """
        server = self._parse_server()
        directory = self._parse_data_directory()
        return server, directory

    def _parse_server(self):
        """
        Function to parse the user CLA supplied target server. Adds http:// if missing and a / at
        the end if missing.

        Returns:
            url:
                Fully API compliant url including scheme and a slash at the end.
        """
        server_argument = self.parser.get_argument("server")
        if not server_argument.startswith("http"):
            warnings.warn("CLI supplied argument did not contain scheme, adding https://")
            server_argument = "http://" + server_argument
        if not server_argument.endswith("/"):
            server_argument = server_argument + "/"
        return server_argument

    def _parse_data_directory(self):
        """
        Function to check if TSVs are present in the user supplied directory
        Returns:
            path:
                Returns the absolute path of the user supplied command line argument if (gzipped)
                TSVs are present.
        Raises:
            IOError:
                IOError is raised when the user supplied CLA is not a directory or does not contain
                any TSVs.
        """
        directory = Path(self.parser.get_argument("user_data")).absolute()
        if not os.path.isdir(directory):
            raise IOError("Given user data argument is not a directory")
        contains_tsv = False
        files = os.listdir(directory)
        for file in files:
            if file.endswith((".tsv", ".tsv.gz")):
                contains_tsv = True
                break
        if not contains_tsv:
            raise IOError("Given user data directory does not contain the user export TSVs")
        return directory


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
            prog="Import cohort users",
            description="Utilitarian script of the Armadillo migration "
                        "process to import users and their rights into the correct cohorts."
        )
        required = parser.add_argument_group("Required arguments")
        required.add_argument(
            "-s",
            "--server",
            type=str,
            required=True,
            help="The server into which the users and their rights should be imported to."
        )
        required.add_argument(
            "-d",
            "--user-data",
            type=str,
            required=True,
            help="The folder in which all export TSV's are located for "
                 "each cohort as they are obtained from export-users.py."
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
