Release procedure

1. Update CHANGELOG.
2. Update README if needed.
3. Bump version number in `plugin-best-practices-plugin/build.gradle` to next stable version.
4. `git commit -am "Prepare for release x.y.z."`.
5. Publish: `./gradlew :plugin-best-practices-plugin:publishPlugins --no-configuration-cache`
   (this will automatically run all the tests, and won't publish if any fail).
6. `git tag -a vx.y.z -m "Version x.y.z".`
7. Update version number `build.gradle` to next snapshot version (x.y.z-SNAPSHOT).
8. `git commit -am "Prepare next development version."`
9. `git push && git push --tags`

nb: if there are ever any issues with publishing to the Gradle Plugin Portal, open an issue on
https://github.com/gradle/plugin-portal-requests/issues and email plugin-portal-support@gradle.com.