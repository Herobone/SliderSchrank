# Contributing

source: https://www.conventionalcommits.org/en/v1.0.0/#summary

## Commits
* follow the scheme:  `((feat|fix|build|chore|ci|docs|style|refactor|perf|test)([optional: scope, bspw. API/Camera/etc])): descriptive text`
* example: `feat(API): add API Connection timeout`
* example: `feat: add CI Configuration for tests`

## Code Style
This project uses Spotless to enforce code style. A pre-commit hook is configured to automatically run `spotlessApply` before each commit. This ensures that all committed code is formatted correctly.

To install the pre-commit hook, run the following command in your terminal:

```
./gradlew installGitHook
```

Spotless enforces the Android Kotlin Style guide, which can be found [here](https://developer.android.com/kotlin/style-guide)

## Branches

* follow the scheme: `(feature|bugfix|hotfix|release|documentation)/what_are_you_doing`
* example: `feature/add-photo-button`
