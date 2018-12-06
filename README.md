![# Header](https://i.imgur.com/NNwBkWb.png)

[<img src="https://ci.appveyor.com/api/projects/status/e9h6l9fu137jr5ek?retina=true" height="20.74px"></img>](https://ci.appveyor.com/project/dscalzi/zipextractor) [![](https://img.shields.io/github/license/dscalzi/ZipExtractor.svg)](https://github.com/dscalzi/ZipExtractor/blob/master/LICENSE.txt) ![](https://img.shields.io/badge/Spigot-1.8.x--1.13.x-orange.svg) ![](https://img.shields.io/badge/Java-8+-ec2025.svg) [![](https://discordapp.com/api/guilds/211524927831015424/widget.png)](https://discordapp.com/invite/Fcrh6PT)

ZipExtractor is an administrative utility plugin allowing the compression/extraction of archived files through minecraft command. This plugin is extremely useful for dealing with archives over FTP, which does not provide support for neither compression nor extraction. While many safeguards are in place, please note that **there is no undo button**. Overridden files **cannot** be recovered.

The source and destination file paths are saved inside of the config.yml. This means that only one can be set at a time. If you edit these values directly in the config.yml you must reload the plugin for the new values to take effect.

---

# Feature List

* Extraction of **ZIP**, **RAR**, **JAR**, **PACK**, and **XZ** archives.
* Compression of any file or directory into the **ZIP** format.
* Compression of any **JAR** file to the **PACK** format, and any non-directory file to the **XZ** format.
* Queueable operations if you have many extractions/compressions to perform.
* Configurable [Thread Pool Executor][thread_pools] allowing you to set a maximum queue size and maximum number of threads to run at once. Incase of an emergency the Thread Pool can be shutdown at anytime.
* Option to be warned if an extraction/compression would result in files being overriden.
    * If enabled, users will require an additional permission in order to proceed with the process.
    * For extractions, you can view every file which would be overriden prior to proceeding with the process.
* Metrics by [bStats][bStats]

You can find more extensive details on the [wiki][wiki].

***

# Contributing

If you would like to contribute to this project, feel free to submit a pull request. The project does not use a specific code style, but please keep to the conventions used throughout the code.

You can build ZipExtractor using [Gradle][gradle]. Clone the repository and run the following command.

```console
$ gradlew build
```

Since the main purpose of this plugin deals with archive manipulation, the plugin uses a provider system so that new formats can be easily supported. If you need support for a specific file extension you can create an issue and request it or submit a pull request which adds the provider. The *TypeProvider* class is documented in the code and implementations already exist if you need examples. A reference to each provider is saved in the *TypeProvider* class.

***

# Links
* [Spigot Resource Page][spigot]
* [Dev Bukkit Page][devbukkit]
* [Sponge Ore Page][spongeore]
* [Suggest Features or Report Bugs][issues]

[thread_pools]: http://tutorials.jenkov.com/java-util-concurrent/threadpoolexecutor.html "Thread Pool Information"
[bStats]: https://bstats.org/plugin/bukkit/ZipExtractor "bStats page"
[wiki]: https://github.com/dscalzi/ZipExtractor/wiki "Wiki page"
[gradle]: https://gradle.org/ "Gradle"
[spigot]: https://www.spigotmc.org/resources/zipextractor.43482/ "Spigot"
[devbukkit]: https://dev.bukkit.org/projects/zipextractor "DevBukkit"
[spongeore]: https://ore.spongepowered.org/TheKraken7/ZipExtractor "Sponge Ore"
[issues]: https://github.com/dscalzi/ZipExtractor/issues "Issue Tracker"