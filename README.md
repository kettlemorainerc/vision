# Starting
If you use intellij you can use the internal terminal and run `git update-index --assume-unchanged VIDEO_DATA/*`
to prevent arbitrary updates that mean nothing from being caught by git.

You will need to clone, build, and copy the `build/libs/april-tags-2077.jar` from team-2077's
[April Tag JNI wrapper](https://github.com/kettlemorainerc/april-tags-jni) to a folder called
`april-tag` in the repository root. Alternatively, receive the jar from someone who's already
built it (and the related, necessary, native lib) for your architecture/OS.

In order to start you will want to run the `run` task. It should set everything up.

## run
should be under the "application" tasks group in the IntelliJ Gradle menu.

Otherwise, you can run the terminal commands. Depending on the terminal either `./gradlew run` or `gradlew run` should work.
