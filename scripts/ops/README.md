# Armadillo for OPS

To monitor and control Armadillo and its DataShield docker containers we need to use the Armadillo API.

## Armadillo Controller

With `armadilloctl` you can control the DataShield docker containers.

To make this possible Armadillo must have an admin user set.

### env.dist

The file [`env.dist`](./env.dist) lists the required environment variables.

- Copy this over to a location of choice ie `/etc/armadillo/acc.env`

### run

Make sure to `source` the file in your current `zsh` or through a new shell.

```zsh
zsh -c "source /etc/armadillo/acc.env ; ./armadilloctl.zsh"
```

### examples

- `./armadillo.zsh statusAll`
- `./armadillo.zsh autoStart`
- `./armadillo.zsh stop default`
- `./armadillo.zsh startAll`

### Test script (WIP)

You can test `armadilloctl.zsh` using `test_armadilloctl.zsh`.

```zsh
zsh -c "source /etc/armadillo/acc.env ; ./test_armadilloctl.zsh" || echo FAILED
```
