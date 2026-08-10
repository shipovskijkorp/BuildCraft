# Independent BuildCraft build generations

`legacy` and `modern` are separate Gradle/Stonecutter roots. Each directory owns
its wrapper, settings, controller, target matrix and loader build scripts.

Run a generation from its own directory:

```text
cd builds/legacy
./gradlew buildAndCollect
```

```text
cd builds/modern
./gradlew buildAndCollect
```

The wrappers are intentionally independent. Upgrading the modern wrapper must
not require upgrading the legacy ForgeGradle build. Use the repository-level
`build-all` scripts only as orchestration; they launch both wrappers as separate
Gradle processes.
