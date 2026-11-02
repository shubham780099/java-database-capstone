// ESLint flat config for the browser JavaScript under app/src/main/resources/static/js.
// Kept deliberately small: it checks syntax and a few correctness rules without needing any
// plugin packages, so `npm install -g eslint` is all the CI job requires.
export default [
  {
    files: ["**/*.js"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module"
    },
    rules: {
      "no-dupe-keys": "error",
      "no-dupe-args": "error",
      "no-unreachable": "error",
      "no-redeclare": "error",
      "no-self-assign": "error",
      "use-isnan": "error"
    }
  }
];
