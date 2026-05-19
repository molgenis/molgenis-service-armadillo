# Authentication server

Relevant for: :material-server:{title="System Operator"}

Authentication server: [https://lifecycle-auth.molgenis.org/](https://lifecycle-auth.molgenis.org/)

In order for OIDC to work on armadillo, an authentication server needs to be configured. In most cases, we will provide
you with OIDC credentials (email support@molgenis.org), otherwise you can configure your own authentication server.
Armadillo functions with FusionAuth, as well as KeyCloak. To configure this in armadillo, specify the following in the 
application.yml:

```yml
spring:
    oauth2:
      client:
        provider:
          molgenis:
            issuer-uri: https://the-auth-server
        registration:
          molgenis:
            client-id: the-client-id
            client-secret: the-client-secret
            authorization-grant-type:
              - authorization_code
              - refresh_token
      resourceserver:
        jwt:
          issuer-uri: https://the-auth-server # can be different one
        opaquetoken:
          client-id: the-device-flow-client-id
```
Note that both the opaquetoken and the client-id are specified here. 
In FusionAuth the client-id and opaguetoken client-id can be the same one. 
Device flow can be setup in the same configuration as regular login, in KeyCloak however, this is impossible. That means
that an additional configuration has to be set up to allow for device flow (as is required for login via DataSHIELD).

## Setup in FusionAuth
To configure a client in FusionAuth, add a configuration in Applications on your auth server. 
The Client Id and Client secret in this application configuration, as well as your auth server will need to be added
in above application.yml.

### oAuth tab
- Require authentication set to true
- Generate Refresh Tokens set to true
- Authorized redirect URLs: http://your-armadillo-url/login/oauth2/code/molgenis
- Enabled grants: Authorization Code, Device, Implicit, Refresh Token
- Device verification URL: https://auth.molgenis.org/oauth2/device

### JWT
- Enabled set to true
- JWT duration: 3600
- Refresh Token duration: 43200

### Security
- Require an API key set to true
- Generate Refresh Tokens set to true
- Enable JWT refresh set to true
- Authentication Tokens: Enabled set to true

## Setup in KeyCloak
### Regular Client ID
Create a new client to use as regular oidc client. Use the Client ID you configure in here as `molgenis.client-id` in 
above setup in the `application.yml`. For the secret, go to the Credentials tab and get the Client Secret there. Apply
the settings below.

#### Settings 
- Set Root, Home URLs, Web origins and Admin URL to the URL of your armadillo server
- Set valid redirect URLs: http://your-armadillo-url/login/oauth2/code/molgenis
- Set Valid post logout redirect URIs:  http://your-armadillo-url/logout
- Client authentication set to On
- Autorization set to off
- Authentication flow: Standard flow, Direct access grants, OAuth 2.0 Device Authorization Grant
- Login Settings: all off
- Logout settings: Front channel logout + Backchannel logout session required to on, 
Backchannel logout revoke offline sessions to off. 

#### Advanced
- Use refresh tokens set to On, all other OpenID Connect Compatibility Modes to Off. 

### Device flow client
Create a new client for this. Use the Client ID you configure in here as `opaquetoken.client-id` in above setup in the
`application.yml`.

#### Settings
- All URL's empty
- Client authentication and Authorization set to off
- Authentication flow: OAuth 2.0 Device Authorization Grant

#### Advanced
- Use refresh tokens set to On, all other OpenID Connect Compatibility Modes to Off. 
