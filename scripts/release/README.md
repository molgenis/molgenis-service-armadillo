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
- Copy the `dev.dist.env` to `.env`
- Fill in some or all parts
- Toggle the `interactive` to `n` or `y` to let the script wait for manual checks.
- If you want to test as a researcher (not just in admin) ensure that you have added your email address to 'OIDC_EMAIL'
- Specify tests you want to skip by adding them to the SKIP_TESTS in the .env file.
Specify the test name to skip, separated by commas with no spaces.
Eg: SKIP_TESTS=dsMediation,dsSurvival

Valid test names: dm-login, create-test-project, upload-data, setup-resources,
manual-test, set-researcher-access, researcher-login, verify-profile, assigning,
verify-resources, dsBase, dsMediation, dsSurvival, dsMTLBase, dsExposome, dsOmics,
dsTidyverse, remove-data, basic-auth

### CICD 
- Note CICD only runs test as admin as we cannot connect to an OIDC account.
- Using `armadillo.get_token` will run locally but fail at CICD.
