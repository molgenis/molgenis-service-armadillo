# V6-Bridge Security Considerations

## Docker Socket Access

The v6-bridge container requires access to the Docker socket
(`/var/run/docker.sock`) to launch vantage6 algorithm containers.
This grants significant privileges and should be handled carefully
in production.

### Risk

A container with Docker socket access can:
- Create, start, stop, and remove any container on the host
- Mount any host directory into a new container
- Potentially escalate to full host access

### Mitigations

#### Option 1: Docker Socket Proxy (Recommended for Production)

Use [Tecnativa/docker-socket-proxy](https://github.com/Tecnativa/docker-socket-proxy)
to restrict which Docker API calls the bridge can make.

```yaml
services:
  docker-proxy:
    image: tecnativa/docker-socket-proxy
    environment:
      CONTAINERS: 1
      IMAGES: 1
      NETWORKS: 1
      VOLUMES: 1
      POST: 1
      # Deny dangerous operations
      BUILD: 0
      COMMIT: 0
      EXEC: 0
      SWARM: 0
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    ports:
      - "2375"

  v6-bridge:
    environment:
      DOCKER_HOST: tcp://docker-proxy:2375
    # No direct Docker socket mount needed
```

#### Option 2: AppArmor/SELinux Profile

Create a custom AppArmor or SELinux profile that restricts the
bridge container's Docker API calls to only those required for
algorithm container management.

#### Option 3: Rootless Docker

Run Docker in rootless mode to limit the impact of Docker socket
access. See [Docker rootless documentation](https://docs.docker.com/engine/security/rootless/).

## Algorithm Container Isolation

Vantage6 isolates algorithm containers by default:
- **Isolated Docker network**: No internet access unless explicitly whitelisted
- **Read-only data mounts**: Algorithms can read but not modify data
- **Container-specific JWT**: Each algorithm gets a scoped token
- **Squid proxy**: Controlled external access via whitelist

These isolation mechanisms are inherited from vantage6 and apply
unchanged in the Armadillo integration.

## Credential Management

### API Key

The vantage6 node API key is passed as an environment variable.
In production:
- Use Docker secrets or a secrets manager
- Rotate API keys periodically
- Monitor for unauthorized node registrations on the v6 server

### Encryption Key

The RSA private key for end-to-end encryption is mounted as a
read-only file. In production:
- Store the key with restricted file permissions (600)
- Consider using a hardware security module (HSM)
- Back up the key securely — losing it means losing access to
  encrypted results

### Access Tokens

Armadillo-issued access tokens control which projects are visible
to vantage6 collaborations. These tokens:
- Are stored in Armadillo's metadata (v6-tokens.json)
- Should be treated as sensitive credentials
- Can be revoked via the REST API at `/api/v6/tokens/{id}`

## Network Security

- The v6-bridge needs outbound connectivity to the vantage6 server
- Algorithm containers should NOT have outbound internet access
- Use firewall rules to restrict the bridge's network access to
  only the vantage6 server endpoint

## Audit

All v6 token operations are audited via Armadillo's
AuditEventPublisher. Events include:
- `V6_CREATE_TOKEN` — token created
- `V6_DELETE_TOKEN` — token revoked
- `V6_UPDATE_TOKEN_PROJECTS` — project access changed
- `V6_LIST_TOKENS` — tokens listed
- `V6_GET_TOKEN` — token retrieved
- `V6_GET_TOKEN_PERMISSIONS` — permissions queried
