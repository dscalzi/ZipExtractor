![# Header](http://i.imgur.com/NNwBkWb.png)

[![](http://ci.aventiumsoftworks.com/jenkins/job/ZipExtractor/badge/icon)](http://ci.aventiumsoftworks.com/jenkins/job/ZipExtractor/) [![](https://img.shields.io/badge/license-AGPL-blue.svg)](https://bitbucket.org/AventiumSoftworks/zip-extractor/src/3b445af5293f3638493cccf50c11f38f1eaf9902/src/com/dscalzi/zipextractor/resources/License.txt) ![](https://img.shields.io/badge/Spigot-1.8--1.12-orange.svg) ![](https://img.shields.io/badge/Java-8+-ec2025.svg) [![](https://discordapp.com/api/guilds/211524927831015424/widget.png)](https://discordapp.com/invite/MkmRnhd)

ZipExtractor is an administrative utility plugin allowing the compression/extraction of archived files through minecraft command. This plugin is extremely useful for dealing with archives over FTP, which does not provide support for neither compression nor extraction. While using this plugin please note that **there is no undo button**. Overridden files **cannot** be recovered.

The source and destination file paths are saved inside of the config.yml. This means that only one can be set at a time. If you edit these values directly in the config.yml you must reload the plugin for the new values to take effect.

---

#Feature List

* Extraction of **ZIP**, **RAR**, and **JAR** archives.
* Compression of any file into the **ZIP** format.
* Queueable operations if you have many extractions/compressions to perform.
* Configurable [Thread Pool Executor][thread_pools] allowing you to set a maximum queue size and maximum number of threads to run at once. Incase of an emergency the Thread Pool can be shutdown at anytime.
* Metrics by [bStats][bStats]

You can find more extensive details on the [wiki][wiki].

***

#Links
* [Spigot Resource Page][spigot]
* [Dev Bukkit Page][devbukkit]
* [Suggest Features or Report Bugs][issues]

[thread_pools]: http://tutorials.jenkov.com/java-util-concurrent/threadpoolexecutor.html "Thread Pool Information"
[bStats]: https://bstats.org/plugin/bukkit/ZipExtractor "bStats page"
[wiki]: https://bitbucket.org/AventiumSoftworks/zip-extractor/wiki/Home "Wiki page"
[spigot]: https://www.spigotmc.org/resources/zipextractor.43482/ "Spigot"
[devbukkit]: https://dev.bukkit.org/projects/zipextractor "DevBukkit"
[issues]: https://bitbucket.org/AventiumSoftworks/zip-extractor/issues?status=new&status=open "Issue Tracker"