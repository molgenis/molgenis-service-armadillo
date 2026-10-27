module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  moduleFileExtensions:  ["vue", "js", "jsx", "ts", "tsx", "json", "mjs"],
  testRegex: "/tests/unit/.*/.*.spec.ts$",
  testEnvironmentOptions: {
    customExportConditions: ["node", "node-addons"],
  },
  moduleNameMapper: {
    "^@/(.*)$": "<rootDir>/src/$1",
  },
  transform: {
    "^.+\\.vue$": "@vue/vue3-jest",
    "^.+\\js$": "babel-jest",
    "^.+\\.ts$": "ts-jest",
    "^.+\\.tsx?$": "ts-jest",
    "\\.mjs$": "babel-jest",
  },
  transformIgnorePatterns: ["/node_modules/(?!(nostics)/)"],
};
