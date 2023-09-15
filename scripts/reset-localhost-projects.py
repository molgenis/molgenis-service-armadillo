import os
import argparse
from getpass import getpass

import requests


def main():
    user = CommandLineParser().parse()
    API(user).parse()


class API:
    def __init__(self, user):
        self.target = "http://localhost:8080/"
        self.user = user
        self.header = self._create_header()

    def parse(self):
        projects = self._parse_projects()
        for project in projects:
            if project != "lifecycle":
                self._delete_project(project)
        users = self._parse_users()
        for user in users:
            if user != self.user:
                self._delete_user(user)

    def _parse_projects(self):
        projects = []
        localhost_project_dict = self._get_projects()
        for project in localhost_project_dict:
            projects.append(project["name"])
        return projects

    def _parse_users(self):
        users = []
        localhost_user_dict = self._get_users()
        for user in localhost_user_dict:
            users.append(user["email"])
        return users

    @staticmethod
    def _create_header():
        token = getpass("Token: ")
        return {"Authorization": "Bearer " + token}

    def _delete_project(self, project_name):
        response = requests.delete(f"{self.target}access/projects/{project_name}", headers=self.header)
        response.raise_for_status()

    def _delete_user(self, user_email):
        response = requests.delete(f"{self.target}access/users/{user_email}", headers=self.header)
        response.raise_for_status()

    def _get_projects(self):
        response = requests.get(f"{self.target}access/projects", headers=self.header).json()
        return response

    def _get_users(self):
        response = requests.get(f"{self.target}access/users", headers=self.header).json()
        return response


class CommandLineParser:
    def __init__(self):
        self.parser = CommandLineInterface()

    def parse(self):
        su = self._parse_su()
        return su

    def _parse_su(self):
        return self.parser.get_argument("user")


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
            "-u",
            "--user",
            type=str,
            required=True,
            help="The single user that is registered on the localhost that needs to remain."
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
