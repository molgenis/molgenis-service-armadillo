import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  test: {
    environment: "jsdom",
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  plugins: [vue()],
  server: {
    port: "8081",
    proxy: {
      "^/admin/.*": {
        target: {
          protocol: "http:",
          host: "localhost",
          port: 8080,
        },
      },
      "^/my/.*": {
        target: {
          protocol: "http:",
          host: "localhost",
          port: 8080,
        },
      },
    },
  },
});
