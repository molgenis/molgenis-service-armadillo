# Migrate Armadillo 5 to Armadillo 6

## 1. Get the update script

## 2. Run the update script

## 3. Make backup of system config

## 4. Stop Armadillo

## 5. Migrate containers.json

In Armadillo 6, "Profiles" have been renamed to "Containers" throughout the application:

- **UI**: The "Profiles" tab is now called "Containers"
- **Config file**: `profiles.json` → `containers.json`
- **Schema changes**: The configuration schema has been updated to support different container types

### Key changes

- **File renamed**: `profiles.json` → `containers.json`
- **New `type` field**: Containers now have a `type` to distinguish between:
  - `ds` - DataSHIELD containers (with packageWhitelist, functionBlacklist, datashieldROptions)
  - `vanilla` - Generic Docker containers (no DataSHIELD-specific configuration)
- **Field renames**:
  - `options` → `datashieldROptions`
  - `InstallDate` → `installDate`
  - `CreationDate` → `creationDate`

### 5.1 Download the migration script

```bash
# Change the version number v6.x.y
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v6.x.y/scripts/migrate-containers-json.py
```

### 5.2 Preview the migration

First, preview what the migrated file will look like (without modifying the original):

```bash
python3 migrate-containers-json.py /usr/share/armadillo/data/system/containers.json
```

Review the output to ensure all your containers are present and have the correct `type`:

- `ds` - Containers with packageWhitelist, functionBlacklist, or datashieldROptions set
- `vanilla` - Containers with none of these fields set

### 5.3 Run the migration

Once satisfied with the preview, run the migration in-place:

```bash
python3 migrate-containers-json.py /usr/share/armadillo/data/system/containers.json --in-place
```

This creates a backup at `containers.json.bak` and updates the original file.

### 5.4 Verify the migration

```bash
cat /usr/share/armadillo/data/system/containers.json | python3 -m json.tool | head -50
```

Ensure all containers are present and have the new schema fields.

## 6. Update application.yml

## 7. Link new version

## 8. Restart Armadillo

## 9. Verify the upgrade

## Troubleshooting
