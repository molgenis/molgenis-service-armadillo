# Release test

In this folder you can run the release test script.

It needs R requirements and a dot-env file

## Once

You need to run

```bash
./install_release_script_dependencies.R
```

to install the required dependencies.

## Run

You can now run

```bash
./release-test.R
```

### Use .env file

To prevent all the questions:

- copy the `dev.dist.env` to `.env`
- fill in some or all parts
- toggle the `interactive` to `n` or `y` to let the script wait for manual checks.
- If you want to test as a researcher (not just in admin) ensure that you have added your email address to 'OIDC_EMAIL'
### CICD 
- Note CICD only runs test as admin as we cannot connect to an OIDC account.
- Using `armadillo.get_token` will run locally but fail at CICD.
