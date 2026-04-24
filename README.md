This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

## Prerequisites for development

### Docker

You're gonna need it for the local development. I'd recommend
downloading [Docker Desktop](https://www.docker.com/products/docker-desktop/).

### Supabase CLI

This is needed for modifying the db schema and having a proper local development db as well. How to install: you can
check their [website](https://supabase.com/docs/guides/local-development/cli/getting-started), but tldr:

#### MacOS or Linux

Use Homebrew:

```shell
brew install supabase/tap/supabase
```

#### Windows

They recommend using Scoop, but that's another dependency. Use whatever works for y'all.

### IntelliJ Idea or Android Studio

We use IntelliJ Idea, but Android Studio works just as good (apart from being somewhat slower in my experience).

## Local development

1 - Start Docker Desktop

2 - Start the local Supabase environment:

```shell
supabase start
```

3 - (Optional) If you want to reset the DB to use the seed data:

```shell
supabase db reset
```

Then, you can visit the Studio link in your console, and navigate to the Table Editor to view your local data (for me,
it's http://127.0.0.1:54323/project/default/editor/)

# Supported Platforms

Must have:

These platforms are the primary focus and must remain fully supported.

- Web (WasmJS)

Nice to have:

These platforms are currently working, but they are secondary because the web app is available to everyone. If support
breaks or becomes too expensive to maintain, it is acceptable to drop the platform. When that happens, update this file
to clearly mark the platform as unsupported or broken.

- Desktop (JVM)
- Android

# Business Logic Decisions

e.g ledgers, editing events after they happened, etc

TBD

## Miscellaneous

### Supabase Environments

The shared Supabase client is configured at build time from Gradle properties or environment variables. Local Supabase
is the default so development builds do not accidentally write to the hosted project.

Default local configuration:

- URL: `http://127.0.0.1:54321`
- Key: the Supabase CLI default local `anon` key

### I don't even know

Build or run against local Supabase:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Psupabase.env=local
```

Build or run against production only when intentional:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun -Psupabase.env=prod
```

Supported overrides:

- `-Psupabase.env=local|prod` or `SUPABASE_ENV`
- `-Psupabase.local.url=...` or `SUPABASE_LOCAL_URL`
- `-Psupabase.local.key=...` or `SUPABASE_LOCAL_KEY`
- `-Psupabase.prod.url=...` or `SUPABASE_PROD_URL`
- `-Psupabase.prod.key=...` or `SUPABASE_PROD_KEY`

Use the public `anon` or publishable key in client apps. Never use a `service_role` or secret key in Web, Android,
iOS, or Desktop builds.
