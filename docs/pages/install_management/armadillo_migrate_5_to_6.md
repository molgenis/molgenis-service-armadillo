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
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v6.x.y/scripts/upgrade/migration_5_6/migrate-containers-json.py
```

### 5.2 Run the migration

The script reads `profiles.json` and creates `containers.json` with the new schema. If `containers.json` already exists, it does nothing.

```bash
python3 migrate-containers-json.py /usr/share/armadillo/data/system
```

### 5.3 Verify and clean up

Check that the migrated file looks correct:

```bash
cat /usr/share/armadillo/data/system/containers.json | python3 -m json.tool | head -50
```

Once you've confirmed your containers work in Armadillo 6, remove the old file:

```bash
rm /usr/share/armadillo/data/system/profiles.json
```

## 6. Update application.yml

## 7. Link new version

## 8. Restart Armadillo

## 9. Verify the upgrade

## Troubleshooting
