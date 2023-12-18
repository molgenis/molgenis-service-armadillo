# Armadillo UI

## Test as a UI developer

From the terminal:
- `cd ui/`
- make sure you have `yarn` installed.
- `yarn test --watch`

Do your develop stuff and watch your test fail or succeed on the go. That is:
- change files in `src/**`
- change files in `tests/unit/**`

## Run the UI locally

Make sure Armadillo is running on 8080 as it needs an endpoint to talk with.

Then run `npm run dev` and start developing the UI.


## How this project was generated

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)

## Can't log in in Armadillo UI

Remove the session data / cookies from the swagger or the armadillo server on port 80, then try again