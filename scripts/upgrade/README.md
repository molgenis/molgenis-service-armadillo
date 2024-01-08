# Upgrade Armadillo installs for developers

See [docs](../docs/upgrade-2-3.md) for ops info.

This project uses `pipenv` for managing dependencies as a developer.

When in need for adding or upgrading a dependency use `pipenv`.

Install pipenv by your package manager

- `apt install pipenv`
- `brew install pipenv`

## Running scripts locally

```bash
pipenv shell
```

then run your scripts as documented in the upgrade documents.

## Check for updates

```bash
pipenv check
pipenv update
pipenv check
```

## Update requirements

As `requirements.txt` is used by the upgrade documentation make sure to update it as well.

```bash
pipenv requirements > requirements.txt
```
