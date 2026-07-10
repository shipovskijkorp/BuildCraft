# Optional development mod jars

The files in this directory are development-only dependencies. They are not bundled into the BuildCraft jar.

## IC2 Classic compatibility

Compile-time API jar:

```text
IC2Classic-1.19.2-2.1.3.3.jar
```

The IC2 API is added to `compileOnly`, so normal BuildCraft releases do not require IC2.

For complete IC2 API resolution and to launch `runClient` or `runServer` with IC2 enabled, also add its mandatory dependency:

```text
CarbonConfig-1.19.2-2.0.0.jar
```

Then enable the runtime dependencies with either:

```properties
enable_ic2_runtime=true
```

in `gradle.properties`, or pass the property on the command line:

```text
./gradlew runClient -Penable_ic2_runtime=true
```

Change `ic2_classic_version` or `carbon_config_version` in `gradle.properties` when the local jar versions change.
