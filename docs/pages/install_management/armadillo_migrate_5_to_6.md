# Migrate Armadillo 5 to Armadillo 6

## 1. Get the update script

## 2. Run the update script

## 3. Make backup of system config

## 4. Stop Armadillo

## 5. Run the migration script

In Armadillo 6, "Profiles" have been renamed to "Containers" throughout the application:

- **UI**: The "Profiles" tab is now called "Containers"
- **Config file**: `profiles.json` → `containers.json`
- **application.yml**: The old `profiles:` section can be removed; new `container.defaults` section is optional (see `application.template.yml` for reference)
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

The script migrates `profiles.json` to `containers.json`. Pass the Armadillo data directory:

```bash
python3 migrate-containers-json.py /usr/share/armadillo/data
```

This will:
- Create `containers.json` from `profiles.json` (if `containers.json` doesn't already exist)

The script is safe to run multiple times — it skips steps that are already done.

### 5.3 Verify and clean up

Check that the migrated containers look correct:

```bash
cat /usr/share/armadillo/data/system/containers.json | python3 -m json.tool | head -50
```

Once you've confirmed your containers work in Armadillo 6, remove the old file:

```bash
rm /usr/share/armadillo/data/system/profiles.json
```

You can also remove the old `profiles:` section from your `application.yml` — it is no longer used by Armadillo 6.

## 6. Link new version

## 8. Restart Armadillo

## 9. Verify the upgrade

## Troubleshooting
