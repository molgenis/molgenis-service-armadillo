module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  moduleFileExtensions: ["js", "ts", "json", "vue"],
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
  },
  transformIgnorePatterns: ["/node_modules/"],
};
