# Armadillo UI

As we use Gradle for building Armadillo we have node related versions defined
in UI [build.gradel](./build.gradle).

After the first build these specific versions appear in UI [.gradle/](./.gradle/) and can be used.

You could ie run the same yarn as CI will run.

```
.gradle/yarn/yarn-v1.22.19/bin/yarn run dev

.gradle/yarn/yarn-v1.22.19/bin/yarn why ts-node

# etc
```

## Work on the docs

This is a little weird as the docs do not belong to the UI project but configuring
it here makes is useable in CI (as an exercise for now).

```bash
.gradle/yarn/yarn-v1.22.19/bin/yarn docs
```

## Test as a UI developer

From the terminal:

```bash
cd ui/
```

```bash
.gradle/yarn/yarn-v1.22.19/bin/yarn test --watch
```

Do your develop stuff and watch your test fail or succeed on the go. That is:
- change files in `src/**`
- change files in `tests/unit/**`

## Run the UI locally

Make sure Armadillo is running on 8080 as it needs an endpoint to talk with.

Then run `npm run dev` and start developing the UI.

## Adding or updating dependencies

As the [UI build file](./build.gradle) has settings for node, npm and **yarn** you must use `yarn` to add dependencies.

You may notice warning when running `../gradlew clean :ui:build` afterwards.

In case of problems

```bash
rm yarn.lock
../gradlew :ui:build
# Yes twice because the addition could cause a new dependency
../gradlew :ui:build
```

## How this project was generated

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)

## Can't log in in Armadillo UI

Remove the session data / cookies from the swagger or the armadillo server on port 80, then try again