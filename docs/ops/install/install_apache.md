# Apache

It is possible to run Molgenis Armadillo using Apache, however we do not provide support with this configuration.

## Encoding

Apache requires some additional configuration to get the `/storage/projects/{project}/objects/{object}` to work. When this endpoint doesn't work:

- tables cannot be assigned
- subsets cannot be created
- resources cannot be used.

## Tell Armadillo about https

We need to tell Armadillo server how to building URLs.

## Changes to your site-enabled configuration

Your configuration probably should look like this.

```conf
ProxyPreserveHost On

ProxyPass / http://localhost:8080/ nocanon

AllowEncodedSlashes On

RequestHeader set X-Forwarded-Proto https
RequestHeader set X-Forwarded-Port 443
```

After setting this don't forget to restart Apache.
