import getopt
import sys
from pathlib import Path

from minio import Minio

help_string = "migrate_minio -t /target/directory"


def main(argv):
    try:
        opts, args = getopt.getopt(argv, "ht:", ["target="])
    except getopt.GetoptError:
        print(help_string)
        sys.exit(2)

    target = None
    for opt, arg in opts:
        if opt == "-h":
            print(help_string)
            sys.exit()
        elif opt in ("-t", "--target"):
            target = arg

    target_dir = Path(target)
    if not target_dir.is_dir():
        print(f"Not a valid directory: {target}")
        sys.exit(1)

    download_all_buckets(target_dir)


def download_all_buckets(target_dir: Path):
    client = Minio(
        "localhost:9000",
        access_key="molgenis",
        secret_key="molgenis",
        secure=False
    )

    buckets = client.list_buckets()
    for bucket in buckets:
        print(bucket.name)

        bucket_dir = target_dir.joinpath(bucket.name)
        obj_count = download_objects(bucket.name, bucket_dir, client)

        if obj_count == 0:
            # just create an empty bucket
            bucket_dir.mkdir()

        # TODO: create project in admin API

        print()


def download_objects(bucket_name: str, bucket_dir: Path, client: Minio) -> int:
    objects = client.list_objects(bucket_name, recursive=True)
    obj_count = 0
    for obj in objects:
        print("> " + obj.object_name)
        target_file = bucket_dir.joinpath(obj.object_name)
        client.fget_object(
            bucket_name,
            obj.object_name,
            file_path=str(target_file)
        )
        obj_count += 1
    return obj_count


if __name__ == '__main__':
    main(["-t", "/Users/tommy/PycharmProjects/armadillo-migration/data/"])
    # main(sys.argv[1:])
