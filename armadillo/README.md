# Armadillo (java)

This is the core application next to [R (java)](../r) and [UI (vuejs)](../ui/README.md).

Armadillo relays R / RStudio data requests from data analysts through DSI (Data Shield Interface) to running docker
Rock images which has R processes running to fulfill the data request from the data analyst.

Armadillo relies on authentication through the OICD landscape.

## Static code analysis

### Annotations

As we use lots of annotations it helps to see which file uses what.

#### What annotation with values

```bash
grep --recursive --no-filename --only-matching --extended-regexp '@\w+\([^)]*\)' * | sort | uniq | sort
```

#### Where what annotation with values

```bash
grep --recursive --only-matching --extended-regexp '@\w+\([^)]*\)' * | sort | uniq | sort
```

#### Count with same value

```bash
grep --recursive --no-filename --only-matching --extended-regexp '@\w+\([^)]*\)' * | sort -n | uniq -c | sort
# | grep -i put # to find a method
```

#### Count annotation occurences

```bash
grep --recursive --no-filename --only-matching --extended-regexp '@\w+\(' * | sort -n | uniq -c | sort
```
