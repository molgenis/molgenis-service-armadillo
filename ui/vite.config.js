import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: "8081",
    proxy: {
      "^/metadata/.*": {
        target: {
          protocol: 'http:',
          host: "localhost",
          port: 8080,
        },
      },
      "^/my/.*": {
        target: {
          protocol: 'http:',
          host: "localhost",
          port: 8080,
        },
      },
    },
  },
});
