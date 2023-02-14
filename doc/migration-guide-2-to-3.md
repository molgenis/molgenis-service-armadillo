# Instructions for migrating a 2.x Armadillo to 3.x
The release of Armadillo 3.0 introduces some breaking changes. These instructions will guide you 
through the steps needed to migrate your old Armadillo service to the new version.

## Configuration
This section is about changes in the `application.yml` configuration file. For a full
example you can look at [the application.yml in the code](/armadillo/src/main/resources/application.yml).

### OIDC
Besides accepting JWTs from a trusted authentication provider, you can now enable OIDC authentication.
This will make it possible for admins to log in with their institute account in the UI.

To enable this, add the following properties:

```
spring:
  security:
    oauth2:
      client:
        registration:
          molgenis:
            client-id: <client_id>
            client-secret: <client_secret>
```

### Armadillo Settings
The `datashield` property has been renamed to `armadillo`, and some new settings have been
introduced:

```
armadillo:
  oidc-permission-enabled: false
  oidc-admin-user: user@yourinstitute.org
  docker-management-enabled: true
```

These settings are explained in more detail below.

#### OIDC Permission Enabled
By default, roles and permissions are managed in Armadillo itself. However, you can still accept 
roles from your authentication provider by setting `oidc-permission-enabled` to `true`. This is
not recommended.

#### OIDC Admin User
You can configure a default OIDC admin user by setting the `oidc-admin-user` property. Armadillo
will add this user when the application starts. This admin will then immediately be able to login
with their institute account.

#### Docker Management Enabled
Armadillo can manage the Docker containers used for profiles. To enable, set `docker-management-enabled`
to `true`. Keep in mind that Armadillo needs to be able to access a local Docker instance.

### Profiles
Profiles can now be created and managed at runtime in the UI or via the profiles API. However,
it is still possible to configure one or more profiles, which will be created for you when
the application starts.

Before 3.0, profiles and R environments had to be defined separately:

```
rserve:
  environments:
    - name: default
      host: localhost
      port: 6311

datashield:
  profiles:
    - name: default
      environment: default
      whitelist:
        - dsBase
      options:
        datashield:
          seed: 342325352
```

This is no longer the case. You now have to configure everything inside a profile:

```
armadillo:
  profiles:
    - name: default
      image: datashield/armadillo-rserver
      host: localhost
      port: 6311
      whitelist:
        - dsBase
      options:
        datashield:
          seed: 342325352
```

Note that the root `datashield` property has been renamed to `armadillo`. 

Also note the `image` property: this one is optional and only necessary when you have enabled 
[Docker Management](#docker-management). The image should be an image available on DockerHub. 

### Storage
You can now choose where Armadillo should store data: on a MinIO server (like before) or on the local file system.

If you want to keep using MinIO, you don't need to change anything. The old configuration will still work:

```
minio:
  url: http://localhost
  port: 9000
  access-key: <access-key>
  secret-key: <secret-key>
```

To use the local file system, set the following parameter:

```
storage:
  root-dir: <path_to_data_folder>
```

MinIO has precedence over local file storage, so keep that in mind when you have both configured.

## Migrating users and data

Since users and permissions are now managed in Armadillo instead of the authentication server, this
information needs to be migrated. And if you choose to host the data on the local file system
instead of a MinIO server, the data needs to be migrated as well. For both scenarios we have migration
scripts.

### User migration script: Fusion Auth to Armadillo
To migrate users from Fusion Auth to Armadillo, you can use the script found [here](/scripts/migrate-auth.py).
More information on how to run it can be found in the script or by calling it with the `-h` flag. Make
sure Armadillo is NOT running when you run the script. After the script is done you need to start 
Armadillo and it will automatically create the projects based on the folders that were copied.

### Data migration script: MinIO to local file system
To migrate data from MinIO to the local file system, you can use the script found [here](/scripts/migrate-minio.py).
More information on how to run it can be found in the script or by calling it with the `-h` flag. Make
sure Armadillo is running when you run the script.

