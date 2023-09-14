import os
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
    processor = ProjectProcessor(api)
    processor.parse(cohorts)


class ProjectDataParser:
    def parse(self, directory):
        return_dict = {}
        files = os.listdir(directory)
        for file in files:
            if not file.endswith((".tsv", ".tsv.gz")):
                continue
            full_path = os.path.join(directory, file)
            project_name = self._parse_filename(file)
            if project_name == "uncategorized users":
                return_dict["NaN"] = self._load_tsv(full_path)
            else:
                return_dict[project_name] = self._load_tsv(full_path)
        return return_dict

    @staticmethod
    def _parse_filename(filename):
        filename = filename.replace("_or_", "_/_")
        filename = filename.replace("_", " ")
        filename = filename.replace()
        return filename

    @staticmethod
    def _load_tsv(path):
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
            warnings.warn(f"Obtaining {path} resulted in empty dataframe!")
            data = pd.DataFrame(
                columns=["active", "email", "firstName", "fullName", "lastName", "roles"]
            )
        return data


class ProjectProcessor:
    def __init__(self, api):
        self.api = api
        self.su = []

    def parse(self, cohort_data):
        for cohort_name, cohort_users in cohort_data.items():
            if cohort_name == "NaN":
                continue
            self._obtain_su(cohort_users)
            self._put_project(name=cohort_name, users=cohort_users['email'].values.tolist())
        self._process_uncategorized_users(cohort_data['NaN'])
        self._add_admin_rights()

    def _put_user(self, email, firstName, lastName):
        self.api.put_user({
              "email": email,
              "firstName": firstName,
              "lastName": lastName,
              "admin": False,
        })

    def _put_project(self, name, users):
        self.api.put_project({"name": name, "users": users})

    def _obtain_su(self, project_data):
        su = project_data.loc[
                project_data[project_data['roles'].isin(("SU", "ADMIN", "admin"))].index,
                "email"
            ]
        for user in su:
            if user not in self.su:
                self.su.append(user)

    def _process_uncategorized_users(self, uncategorized_users):
        self._obtain_su(uncategorized_users)
        uncategorized_users.apply(
            lambda x: self._put_user(
                email=x['email'],
                firstName=x['firstName'],
                lastName=x['lastName']
            )
        )

    def _add_admin_rights(self):
        for user in self.su:
            self.api.put_user({"email": user, "admin": True})


class API:
    def __init__(self, server):
        self.target = server
        self.header = self._create_authorazation_header()

    @staticmethod
    def _create_authorazation_header():
        token = getpass("Armadillo 3 token:")
        if token == "":
            warnings.warn("Token not supplied, requiring basic auth password!")
            password = getpass("Basic auth password:")
            auth_header = "Basic " + base64.b64encode(("admin:" + password).encode('ascii')).decode("UTF-8")
        else:
            auth_header = "Bearer " + token
        return {"Content-Type": "application/json", "Authorization": auth_header}

    def put_user(self, data):
        response = requests.put(self.target + "access/users", json=data, headers=self.header)
        response.raise_for_status()

    def put_project(self, project):
        response = requests.put(self.target + "access/projects", json=project, headers=self.header)
        response.raise_for_status()


class CommandLineParser:
    def __init__(self):
        self.parser = CommandLineInterface()

    def parse(self):
        server = self._parse_server()
        directory = self._parse_data_directory()
        return server, directory

    def _parse_server(self):
        server_argument = self.parser.get_argument("server")
        if not server_argument.startswith("http"):
            warnings.warn("CLI supplied argument did not contain scheme, adding https://")
            server_argument = "http://" + server_argument
        if not server_argument.endswith("/"):
            server_argument = server_argument + "/"
        return server_argument

    def _parse_data_directory(self):
        directory = Path(self.parser.get_argument("user_data"))
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
            help="The folder in which all export TSV's are located for each cohort as they are obtained from "
                 "export-users.py."
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
