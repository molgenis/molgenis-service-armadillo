"""
Migration script for Armadillo 2.x -> 3.0

This script copies the complete structure of a MinIO server to local file storage. Run
this script on the server that will store the data. Projects will automatically be
created when you start Armadillo. Credentials are requested during execution of the
script.
"""

import getopt
import sys
from getpass import getpass
from pathlib import Path

from minio import Minio
from minio.datatypes import Bucket

help_string = "python migrate-minio.py --minio http://localhost:9000/ --target /target/directory"
minio_url = ""


def main(argv):
    global minio_url

    try:
        opts, args = getopt.getopt(argv, "hm:t:a:", ["minio=", "target="])
    except getopt.GetoptError:
        print(help_string)
        sys.exit(2)

    target = None
    for opt, arg in opts:
        if opt == "-h":
            print(help_string)
            sys.exit()
        elif opt in ("-m", "--minio"):
            minio_url = arg
        elif opt in ("-t", "--target"):
            target = arg

    if not minio_url or not target:
        print("All arguments are required: ")
        print(help_string)
        sys.exit(2)

    target_dir = Path(target)
    if not target_dir.is_dir():
        print(f"Not a valid directory: {target}")
        sys.exit(1)

    download_all_buckets(target_dir)


def download_all_buckets(target_dir: Path):
    minio_client = create_minio_client()

    buckets = minio_client.list_buckets()
    for bucket in buckets:
        print(f"Downloading {bucket.name}...")
        download_bucket(minio_client, bucket, target_dir)
        print()


def download_bucket(minio_client: Minio, bucket: Bucket,
                    target_dir: Path):
    bucket_dir = target_dir.joinpath(bucket.name)
    obj_count = download_objects(bucket.name, bucket_dir, minio_client)

    if obj_count == 0:
        print("> No files found")
        if not bucket_dir.exists():
            # just create an empty folder
            bucket_dir.mkdir()


def create_minio_client():
    username = input("MinIO username:")
    password = getpass("MinIO password:")

    secure = not minio_url.startswith("http://")
    url = minio_url.lstrip("http://").lstrip("https://")

    client = Minio(
        url,
        access_key=username,
        secret_key=password,
        secure=secure
    )

    print()
    return client


def download_objects(bucket_name: str, bucket_dir: Path, client: Minio) -> int:
    objects = client.list_objects(bucket_name, recursive=True)
    obj_count = 0
    for obj in objects:
        print("- " + obj.object_name)
        target_file = bucket_dir.joinpath(obj.object_name)
        client.fget_object(
            bucket_name,
            obj.object_name,
            file_path=str(target_file)
        )
        obj_count += 1
    return obj_count


if __name__ == '__main__':
    main(sys.argv[1:])
