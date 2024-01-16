### Apache
It is possible to run Molgenis Armadillo using Apache. We do however not provide any support regarding this configuration. Apache requires some additional configuration to get the `/storage/projects/{project}/objects/{object}` to work. When this endpoint doesn't work, tables cannot be assigned, subsets cannot be created and resources cannot be used. This basically means Armadillo is not usable. 

Issues might be resolved with the following settings in the `ssl.conf`:
```
ProxyPass / http://localhost:8080/ nocanon
AllowEncodedSlashes On
```
After setting this, don't forget to restart Apache. 