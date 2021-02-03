# Dokka Hugo Plugin

Extends [Dokka](https://github.com/Kotlin/dokka) with a Hugo Plugin.

## Build

``./gradlew build``

## Try Local Build

``./gradlew publishToMavenLocal``


## Dependency

```
buildScript{
    repositories{
        ...
        jcenter()
    }
    dependencies{
        ...
        classpath "org.jetbrains.dokka:dokka-gradle-plugin:1.4.20"
    }
}

allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

It is necessary to create a task to run the dokka with the hugo-plugin after included the dependencies

## Task without customizations
```
tasks.register("dokkaHugo", org.jetbrains.dokka.gradle.DokkaTask) {
    dependencies {
        dokkaHugoPlugin 'com.github.cotechde:dokka-hugo-plugin:$version'
    }
}
```

## Task with customizations
You can customize the title or name showed on the menu and if this names will be capitalize. To configure this, your
build.gradle file needs to be a build.gradle.kts, then, you can do like below:

```
tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHugo") {
    dependencies {
        plugins("de.cotech:dokka-hugo-plugin:$version")
    }
    pluginConfiguration<org.jetbrains.dokka.hugo.HugoPlugin, org.jetbrains.dokka.hugo.HugoConfiguration> {
        titleReplace = hashMapOf("com.test.sample." to "", "com.test." to "", "." to " ")
        titleCapitalize = true
        linkTitleReplace = hashMapOf("com.test.sample." to "", "com.test." to "", "." to " ")
        linkTitleCapitalize = true
    }
}
```

The titleReplace is a hashMap that will get the keys and replace for the respective values for the title generated.

The titleCapitalize is a Boolean, if true, the title will be capitalize, else, the title will not be capitalize

The linkTitleReplace is a hashMap that will get the keys and replace for the respective values for the linkTitle 
generated.

The linkTitleCapitalize is a Boolean, if true, the linkTitle will be capitalize, else, the 
linkTitle will not be capitalize

## Requirements

* Currently uses the Hugo academic theme
* Requires special docs template called "reference"
* Requires shortcode to insert markdown in html. Put `md.html` in your "shortcodes" folder with the content:
```
{{ .Inner }}
```
