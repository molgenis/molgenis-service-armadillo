# Armadillo for OPS

To monitor and control Armadillo and its DataShield docker containers we need to use the Armadillo API.

## Armadillo Controller

With `armadilloctl` you can control the DataShield docker containers.

To make this possible Armadillo must have an admin user set.

### env.dist

The file [`env.dist`](./env.dist) lists the required environment variables.

- Copy this over to a location of choice ie `/etc/armadillo/acc.env`

### run

Make sure to `source` the file in your current `bash` or through a new shell.

```bash
bash -c "source /etc/armadillo/acc.env ; ./armadilloctl.bash"
```

### examples

- `./armadillo.bash statusAll`
- `./armadillo.bash autoStart`
- `./armadillo.bash stop default`
- `./armadillo.bash startAll`

### Test script (WIP)

You can test `armadilloctl.bash` using `test_armadilloctl.bash`.

We source the environment variables to not contaminate our current shell.

```bash
bash -c "source /etc/armadillo/acc.env ; ./test_armadilloctl.bash" || echo FAILED
```
