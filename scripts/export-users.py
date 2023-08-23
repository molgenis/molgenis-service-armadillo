"""
User export script to aid in migration

This script gets users and their assigned cohorts from Fusion Auth to Armadillo.
It will group each user by cohort and will export it as TSV to a given export location, so that upon request
this TSV can be used to set users to their appropriate cohorts.
"""

import os
import argparse
import warnings
from pathlib import Path
from getpass import getpass

import requests
from fusionauth.fusionauth_client import FusionAuthClient
from fusionauth.rest_client import ClientResponse
from requests import Session
import pandas as pd


def main():
    fusion_url, output = CommandLineParser().parse()
    cohort_data = FusionClientParser(fusion_url).parse()
    Exporter(output).export_cohorts(cohort_data)


class Exporter:
    def __init__(self, output_path):
        self.output_path = output_path

    def export_cohorts(self, cohorts):
        for cohort_name, cohort_users in cohorts.items():
            cohort_users.to_csv(os.path.join(self.output_path, cohort_name + '.tsv.gz'), index=False, sep='\t')


class FusionClientParser:
    def __init__(self, fusion_url):
        self.client = FusionClient(fusion_url)
        self.data_object = {}

    def parse(self):
        users = self.client.get_users()
        cohorts = self.client.get_cohorts()
        # TODO: get all cohorts
        # TODO: maybe loop through all cohorts and get users per cohort?
        return self.data_object


class FusionClient:
    def __init__(self, fusion_url):
        api_key = getpass("Fusion Auth API Key:")
        self.client = FusionAuthClient(api_key, fusion_url)

    def get_users(self):
        response = self.client.search_users_by_query({
            "search": {
                "queryString": "*",
                "startRow": 0,
                "numberOfResults": 10000
            }
        })
        result = self._get_result_or_exit(response)
        users = pd.DataFrame(result["users"])
        # TODO: ask Mariska if we only want active and registered users
        return users

    def get_cohorts(self):
        response = self.client.retrieve_applications()
        result = self._get_result_or_exit(response)
        applications = pd.DataFrame(result["applications"])
        return applications

    @staticmethod
    def _get_result_or_exit(response):
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
        fusion_url = self._parse_url(self.interface.get_argument("fusion_auth"))
        output = self._parse_output(Path(self.interface.get_argument("output")))
        return fusion_url, output

    @staticmethod
    def _parse_url(url_argument):
        if url_argument.endswith("/"):
            url_argument = url_argument.rstrip("/")
        if not url_argument.startswith("http"):
            warnings.warn("CLI supplied argument did not contain scheme, adding https://")
            url_argument = "https://" + url_argument
        return url_argument

    @staticmethod
    def _parse_output(output_argument):
        # Absolute so that it works with ./output or similar
        output_argument = output_argument.absolute()
        if not os.path.isdir(output_argument):
            if not os.path.isdir(output_argument.parent):
                raise OSError(f"Cannot create output directory {output_argument}, parent does not exist.")
            else:
                os.mkdir(output_argument)
        return output_argument


class CommandLineInterface:
    def __init__(self):
        parser = self._create_argument_parser()
        self.arguments = parser.parse_args()

    @staticmethod
    def _create_argument_parser():
        parser = argparse.ArgumentParser(
            prog="Export cohort users",
            description="Utilitarian script of the Armadillo migration process to export users grouped by cohorts."
        )
        required = parser.add_argument_group("Required arguments")
        # TODO: check with Mariska if fusion auth is all I need
        # required.add_argument(
        #     "-a",
        #     "--armadillo-url",
        #     type=str,
        #     required=True,
        #     help="The Armadillo URL"
        # )
        required.add_argument(
            "-f",
            "--fusion-auth",
            type=str,
            required=True,
            help="The fusion auth URL"
        )
        required.add_argument(
            "-o",
            "--output",
            type=str,
            required=True,
            help="The output directory in which the cohort TSV files should be placed."
        )
        return parser

    def get_argument(self, argument_key):
        if argument_key in self.arguments:
            return getattr(self.arguments, argument_key)
        else:
            raise KeyError(f"Argument {argument_key} invalid, not found in CLI arguments.")


if __name__ == "__main__":
    main()
