# 8. As a researcher you are able to load multiple RData files

Date: 2020-04-23

## Status

Accepted

## Context
We want to give researchers as much freedom as possible when they're selecting and loading data.

## Decision

The `/load-tables` endpoint supports loading multiple .RData files at once. These files can be in different folders.

## Consequences
- Users with multiple roles can load data from multiple folders into their workspace. This will make
  sharing workspaces between users difficult (because they may not have the same roles). 