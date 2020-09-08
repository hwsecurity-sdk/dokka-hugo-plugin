# Dokka Hugo Plugin

Extends [Dokka](https://github.com/Kotlin/dokka) with a Hugo Plugin.

## Build

``./gradlew build``

## Try Local Build

``./gradlew publishToMavenLocal``


## Dependency

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

```
tasks.register("dokkaHugo", DokkaTask) {
    dependencies {
        dokkaHugoPlugin 'com.github.cotechde:dokka-hugo-plugin:master-SNAPSHOT'
    }
}
```

## Requirements

* Currently uses the Hugo academic theme
* Requires special docs template called "reference"
* Requires shortcode to insert markdown in html. Put `md.html` in your "shortcodes" folder with the content:
```
{{ .Inner }}
```
