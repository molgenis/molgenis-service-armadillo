module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  moduleFileExtensions: ["js", "mjs", "ts", "json", "vue"],
  testRegex: "/tests/unit/.*/.*.spec.ts$",
  testEnvironmentOptions: {
    customExportConditions: ["node", "node-addons"],
  },
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
    "perfect-debounce": "<rootDir>/tests/__mocks__/perfect-debounce.js",
  },
  transform: {
    "^.+\\.vue$": "@vue/vue3-jest",
    "^.+\\.m?js$": "babel-jest",
    "^.+\\.ts$": "ts-jest",
  },
  transformIgnorePatterns: [
    "/node_modules/(?!(perfect-debounce|@vue/devtools-kit|@vue/devtools-api|vue-router)/)",
  ],
};
