import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

;

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
    //relative paths so you can serve anywhere
    base: '/ui/',
    server: {
        port: "8081",
        proxy: {
            "^/access/.*": {
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
            "^/storage/.*": {
                target: {
                    protocol: "http:",
                    host: "localhost",
                    port: 8080,
                },
            },
            "^/ds-profile": {
                target: {
                    protocol: "http:",
                    host: "localhost",
                    port: 8080,
                },
            },
            "^/basic-login": {
                target: {
                    protocol: "http:",
                    host: "localhost",
                    port: 8080,
                },
            },
            "^/oauth2": {
                target: {
                    protocol: "http:",
                    host: "localhost",
                    port: 8080,
                },
            },
            "^/login/oauth2/.*": {
                target: {
                    protocol: "http:",
                    host: "localhost",
                    port: 8080,
                },
            },
            "^/logout": {
                target: {
                    protocol: "http:",
                    host: "localhost",
                    port: 8080,
                },
            },
        },
    },
});
