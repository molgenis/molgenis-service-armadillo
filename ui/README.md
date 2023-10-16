# Vue 3 + Vite

This template should help get you started developing with Vue 3 in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)

## Can't log in in Armadillo UI

Remove the session data / cookies from the swagger or the armadillo server on port 80, then try again

## Updating tests

- `yarn test --watch`

## Views

We have several views which use generic [Table](./src/components/Table.vue) component

- [Profiles](./src/views/Profiles.vue)
- [Projects](./src/views/Projects.vue)
- [Users](./src/views/Users.vue)

### Change a single profile, project or user

Check for

- `defineComponent()` in the `.vue`
- visit the REST endpoint for seeing the data ie http://localhost:8080/ds-profiles`
