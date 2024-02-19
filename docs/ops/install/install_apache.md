# Apache

> We do however not provide any support regarding this configuration.

It is possible to run Molgenis Armadillo using Apache.

## Infrastructure related

Below are some hints found.

### https into the Armadillo server

When https is done until reaching Armadillo server you need to tell Armadillo building URLs.

```conf
ProxyPreserveHost On

RequestHeader set X-Forwarded-Proto https
RequestHeader set X-Forwarded-Port 443
```

## Encoding

Apache requires some additional configuration to get the `/storage/projects/{project}/objects/{object}` to work. When this endpoint doesn't work:

- tables cannot be assigned
- subsets cannot be created
- resources cannot be used.

This basically means Armadillo is not usable. 

Issues might be resolved with the following settings in the `ssl.conf`:

```apache
ProxyPass / http://localhost:8080/ nocanon
AllowEncodedSlashes On
```

After setting this, don't forget to restart Apache.
