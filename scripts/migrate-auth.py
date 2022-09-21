"""
Migration script for Armadillo 2.x -> 3.0

This script copies users and their roles from Fusion Auth to Armadillo. It automatically
creates users, projects, and permissions. Credentials are requested during execution of
the script.
"""

import getopt
import json
import sys
from getpass import getpass

import requests
from fusionauth.fusionauth_client import FusionAuthClient
from fusionauth.rest_client import ClientResponse
from requests import Session
from simple_term_menu import TerminalMenu

help_string = "python migrate-auth.py --fusion-auth auth_server --armadillo " \
              "armadillo_server "
armadillo_url = ""
auth_url = ""


def main(argv):
    global armadillo_url
    global auth_url

    try:
        opts, args = getopt.getopt(argv, "hf:a:", ["fusion-auth=", "armadillo="])
    except getopt.GetoptError:
        print(help_string)
        sys.exit(2)

    for opt, arg in opts:
        if opt == "-h":
            print(help_string)
            sys.exit()
        elif opt in ("-f", "--fusion-auth"):
            auth_url = arg
        elif opt in ("-a", "--armadillo"):
            armadillo_url = arg.rstrip("/")

    if not auth_url or not armadillo_url:
        print("All arguments are required: ")
        print(help_string)
        sys.exit(2)

    migrate()


def migrate():
    # 1. Set up clients
    armadillo_client = create_armadillo_client()
    fusion_client = create_fusion_client()

    # 2. Get production apps that have users
    users = get_active_registered_users(fusion_client)
    apps = get_production_applications_with_users(fusion_client, users)

    # 3. Let user pick which app to migrate to this Armadillo server
    app_to_migrate = choose_application(apps)

    # 4. Copy the app's users to Armadillo
    migrate_application(app_id=app_to_migrate,
                        app_name=apps[app_to_migrate],
                        users=users,
                        armadillo_client=armadillo_client)


# noinspection DuplicatedCode
def create_armadillo_client():
    username = input("Armadillo username:")
    password = getpass("Armadillo password:")

    session = requests.Session()
    session.auth = (username, password)
    session.headers.update({'Content-Type': 'application/json'})

    response = session.get(armadillo_url + "/my/principal")
    response.raise_for_status()

    return session


def create_fusion_client():
    api_key = getpass("Fusion Auth API Key:")

    # Remove trailing / to work around an issue in the FusionAuth
    # See https://github.com/FusionAuth/fusionauth-python-client/issues/12
    url = auth_url.rstrip("/")

    client = FusionAuthClient(api_key, url)

    print()
    return client


def choose_application(app_names_by_id: dict) -> str:
    print("Choose which applications to migrate")
    menu = TerminalMenu(app_names_by_id.values())
    choice_index = menu.show()

    print()
    return list(app_names_by_id.keys())[choice_index]


def migrate_application(app_id: str, app_name: str, users: list,
                        armadillo_client: Session):
    print(f"Migrating users from {app_name} to {armadillo_url}")
    for user in users:
        regs = user["registrations"]
        for reg in regs:
            if reg["applicationId"] != app_id:
                continue

            migrate_user(armadillo_client, reg, user)
    print()


def migrate_user(armadillo_client: Session, reg: dict, user: dict):
    print(f" - {user['email']}")

    roles = reg["roles"]
    is_admin = "SU" in roles
    researcher_roles = filter(lambda role: role.endswith("_RESEARCHER"), roles)
    projects = list(map(lambda role: role.rstrip("_RESEARCHER").lower(),
                        researcher_roles))
    json_user = json.dumps({
        "email": user["email"],
        "firstName": user.get("firstName", None),
        "lastName": user.get("lastName", None),
        "institution": user.get(""),
        "admin": is_admin,
        "projects": projects
    })
    response = armadillo_client.put(armadillo_url + "/admin/users",
                                    data=json_user)
    response.raise_for_status()


def get_production_applications_with_users(client: FusionAuthClient,
                                           users: list) -> dict:
    """
    Returns a dict of application id:name pairs. Only returns production applications
    that have more than one user.
    """
    response = client.retrieve_applications()
    result = get_result_or_exit(response)

    # map app ids to app names
    apps = result["applications"]
    app_names_by_id = {app["id"]: app["name"] for app in apps}

    # iterate over users and collect applications they are registered to
    apps_with_users = dict()
    for user in users:
        regs = user["registrations"]
        for reg in regs:
            app_id = reg["applicationId"]
            apps_with_users[app_id] = app_names_by_id[app_id]

    # only return PROD applications
    return {id_: name
            for id_, name in apps_with_users.items()
            if name.startswith("[PROD]")}


def get_active_registered_users(client: FusionAuthClient) -> list:
    response = client.search_users_by_query({
          "search": {
            "queryString": "*"
          }
        })
    result = get_result_or_exit(response)
    users = result["users"]
    active_users = filter(lambda user: user["active"] is True, users)
    return list(filter(lambda user: "registrations" in user, active_users))


def get_result_or_exit(response: ClientResponse):
    if response.was_successful():
        return response.success_response
    else:
        res = response.response
        res.raise_for_status()
        exit(1)


if __name__ == '__main__':
    main(sys.argv[1:])
