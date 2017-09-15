# Lighthouse Diagnostic Suite
Lighthouse is a suite of diagnostic tools developed by the 2017 summer engineering interns of Actiance's Alcatraz division for company-wide use. Lighthouse facilitates multi-team coordination by ensuring parity across server (mis)configurations between product sites/environments (e.g. Alcatraz-QA, Alcatraz-Staging) and is a modular, extensible framework for highly functional diagnostic tools.

## Table of Contents
+ [Functionality as of (v1.4.0)](#functionality-as-of-v140)
+ [Architecture and Pipeline](#architecture-and-pipeline)
+ [Set Up](#set-up)
+ [Generating Configuration Files](#generating-configuration-files)
	+ [Generating ZooKeeper Files](#generating-zookeeper-files)
	+ [Generating Dependency Data](#generating-dependency-data)
	+ [Adding More Extraction Scripts](#adding-more-extraction-scripts)
+ [Parsing Files](#parsing-files)
	+ [Meta Files](#meta-files)
	+ [Adding Support for New File Types](#adding-support-for-new-file-types)
	+ [Currently Supported File Types](#currently-supported-file-types)
+ [General Application Usage](#general-application-usage)
	+ [Switching between Modules](#switching-between-modules)
	+ [Chaining Commands](#chaining-commands)
	+ [Exiting Lighthouse](#exiting-lighthouse)
+ [the Database Module](#the-database-module)
	+ [Populating the Database](#populating-the-database)
	+ [Verifying the Database](#verifying-the-database)
	+ [`ignore`](#ignore)
+ [the Query Module](#the-query-module)
	+ [Basic Queries](#basic-queries)
	+ [Wildcards](#wildcards)
	+ [Internal Queries](#internal-queries)
	+ [Exclusions](#exclusions)
	+ [`find` and `grep`](#find-and-grep)
+ [Planned Updates](#planned-updates)
+ [Code and Build](#code-and-build)
	+ [Useful Maven Commands](#useful-maven-commands)
+ [Developers](#developers)

## Functionality (as of v1.4.0)
Lighthouse standardizes and parses various server configuration files (e.g. .properties, .config, .yaml) from development environments and caches everything in a SQLite database. The database cache can then be queried for files that can be directly compared for configuration differences via the command line. Lighthouse has
+ flexible queries that allow for comparisons between different directories
+ comparisons within the same directory (e.g. all nodes within a single fabric against each other)
+ exclusions for specific files and directories (e.g. system information files or irrelevant nodes)
+ the ability to ignore dev-specific properties to reduce false-positives
+ property searches within the database to see the locations and values of that property
+ an extremely scalable and extensible program architecture

The command-line application features full-fledged help pages with detailed information about each individual command (e.g. purpose, usage). More specific information on the usage of each modular component of Lighthouse can be found within the help pages of the command-line application or [the v1.4.0 demo presentation](https:/docs.google.com/presentation/d/1711yUaoKp8omFTRhEUGPJnlApA5UuHwN2WRJ2gde3c0/pub?start=true&loop=false&delayms=3000). More specific information on the architecture and modular program design can be found below.

## Architecture and Pipeline
Lighthouse was developed entirely within Java and utilizes the [SQLite-JDBC API](https://github.com/xerial/sqlite-jdbc) significantly. The complementary [`crawler` collection](https:/github.com/sumeet-bansal/crawler) was developed as a collection of both regular scripts and wrappers for Java applications and heavily utilizes the ZooKeeper API.

## Set Up
__Lighthouse requires having JRE 1.8 installed__. It can be run as a command-line executable:
1. Ensure JRE 1.8 is installed on the machine.
2. Navigate to the directory containing the executable.
3. Run the following command: `java -jar lighthouse-1.4.0.jar`. From here, the application help pages are sufficiently detailed to run Lighthouse.

The [`prereq-installer.sh` script](https:/github.com/sumeet-bansal/lighthouse/blob/master/scripts/prereq-installer.sh) for \*Nix systems can ensure all Lighthouse prerequisites are installed on stable LTS releases of [Ubuntu](https:/www.ubuntu.com/). The [`addon-installer.sh` script](https:/github.com/sumeet-bansal/lighthouse/blob/master/scripts/addon-installer.sh) installs useful programs to enhance Lighthouse, e.g. [tabview](https:/github.com/TabViewer/tabview), a command-line CSV viewer.

## Generating Configuration Files
The `crawler` program acts as a complement to the 'lighthouse' diagnostic suite and can be found [here](https:/github.com/sumeet-bansal/crawler). Crawler retrieves and generates various server configuration files (e.g. .properties, .config, .yaml) from individual dev environments and populates user-specified root directories.

### Generating ZooKeeper Files
__Generating ZooKeeper files requires having JRE 1.8 installed.__
Crawler works with the ZooKeeper API to generate full `.properties` files for all fabrics within a given dev environment. It generates a directory for the given dev environment within the root directory, and then, given a path within ZooKeeper to follow, it creates nested directories for each fabric within that ZooKeeper path. Within those fabric directories, it creates another directory named `common`, under which the `.properties` files can be found for each fabric. Furthermore, certain branches can be excepted from the general `server.properties` file for readability&mdash;these excepted branches are then given their own file named `server.<branch>.properties`. Detailed usage statements can be found in the application help pages, but as an example, this command creates a directory `dev` within `C:/Users/user/root` and populates it with the fabrics found within `/alcatrazproperties/2.5` of the host IP `127.0.0.1`:

```
~$ java -jar ADS_v1.1.jar zk generate 127.0.0.1 /alcatrazproperties/2.5 C:/Users/user/root dev blacklist purge
generated .properties file(s) for dev/fabric1
generated .properties file(s) for dev/fabric2
generated .properties file(s) for dev/fabric3
```

For each fabric within that environment, the command will then create some file `root/dev/fabric/server.properties` and, if there is a `blacklist` branch but no `purge` branches within the fabric, the command will additionally create a file `root/dev/fabric/server.blacklist.properties`.

However, since crawler is designed as a collection of scripts, a Shell wrapper for ZooKeeper generation is available: [`zk-wrapper.sh`](https:/github.com/sumeet-bansal/crawler/blob/master/scripts/zk-wrapper.sh). To use this wrapper, first specify each environment and IP within the associative array of the script. For example, to add `dev1` with environment `127.0.0.1` and `dev3` with environment `127.0.0.3`, modify the script as such:

Before:
```bash
#!/usr/bin/env bash

declare -A IPTable

# to add a dev and their environment, simply add another element to the associative array
IPTable[dev]=env
...
```

After:
```bash
#!/usr/bin/env bash

declare -A IPTable

# to add a dev and their environment, simply add another element to the associative array
IPTable[dev1]=127.0.0.1
IPTable[dev3]=127.0.0.3
...
```

Assuming the [`zk-crawler.jar`](https:/github.com/sumeet-bansal/crawler/blob/master/scripts/zk-crawler.jar) file is still in the same directory as the wrapper, [`zk-wrapper.sh`](https:/github.com/sumeet-bansal/crawler/blob/master/scripts/zk-wrapper.sh) can then be run as a normal Shell script:
```
~$ chmod u+x zk-wrapper.sh
~$ ./zk-wrapper.sh
generated .properties file(s) for dev1/fabric1
generated .properties file(s) for dev1/fabric2
generated .properties file(s) for dev1/fabric3
generated .properties file(s) for dev3/fabric1
generated .properties file(s) for dev3/fabric2
generated .properties file(s) for dev3/fabric3
```

### Generating Dependency Data
Crawler has three scripts that work in tandem to generate dependency data for a node or set of nodes: [`jar-generator.sh`](https:/github.com/sumeet-bansal/crawler/blob/master/scripts/jar-generator.sh), [`jar-organizer.sh`](https:/github.com/sumeet-bansal/crawler/blob/master/scripts/jar-organizer.sh), and [`RWC-jar-generator.sh`](https:/github.com/sumeet-bansal/crawler/blob/master/scripts/RWC-jar-generator.exp). The first, `jar-generator.sh`, must be run from within an environment. Assuming a standard fabric-naming scheme, `jar-generator.sh` detects which fabric it's being run from and comes hardcoded with the locations of each `lib/` folder for that specific fabric. It then generates a `.jars` file with the structure:
```
dependency1.jar=size1
dependency2.jar=size2
dependency3.jar=size3
```
For example, to generate a `.jars` file for the node `fab-eng02-haz-n2`:
```
sysops@fab-eng02-haz-n2:~$ ./jar-generator.sh
generated ~/fab-eng02-haz-n2.lib.jars
generated ~/fab-eng02-haz-n2.apclib.jars
```
The second, `jar-organizer.sh`, takes a collection of `.jars` files and organizes them in a data store Lighthouse can use. This data store takes the form of a directory structure within a given root directory. For example, to organize all the `.jars` files within the working directory:
```
~$ ./jar-organizer.sh root/RWC-Dev
organized root/RWC-Dev/hazelcast/n2/fab-eng02-haz-n2.lib.jars
organized root/RWC-Dev/hazelcast/n2/fab-eng02-haz-n2.apclib.jars
```
The third, `RWC-jar-generator.sh`, works specifically with the Redwood City dev environments, cycling through them and generating a complete inventory of the dependencies in each node of a fabric. `RWC-jar-generator.sh` can be easily modified to do the same for any set of environments.
```
~$ ./RWC-jar-generator.sh
generated ~/fab-eng02-haz-n1.lib.jars
generated ~/fab-eng02-haz-n1.apclib.jars
organized root/RWC-Dev/hazelcast/n2/fab-eng02-haz-n1.lib.jars
...
generated ~/fab-eng02-stm-n4.lib.jars
organized root/RWC-Dev/storm/n4/fab-eng02-stm-n4.lib.jars
```

### Adding More Extraction Scripts
Since most of these scripts are standalone, they can be added as needed.

## Parsing Files
The modular design philosophy behind Lighthouse also shapes how the file parsing system works. Just as Lighthouse serves as a framework into which diagnostic tools can be plugged into, the file parsing system serves as a framework into which different types of file parsers can be plugged into. As of v1.4.0, Lighthouse supports [a number of files and their variants](#currently-supported-file-types).

The files must be within a compatible directory structure:
```
dev
  > fabric
    > node
      - file
```

The file parser works by separating each property within a given file into a key and a value. It then writes each key-pair value to a SQLite row, which represents a single property, and attaches file metadata to the row.

For example, a line from the `server.properties` file (environment: `RWC-Dev`, fabric: `hazelcast`, node: `common`)
```
report.kibana.index=.kibananew
```
is parsed into the following SQLite row:
```
"key"       :   "report.kibana.new",
"value"     :   ".kibananew",
"filename"  :   "server.properties",
"node"      :   "common",
"fabric"    :   "hazelcast",
"environment"   :   "RWC-Dev",
"path"      :   "RWC-Dev/hazelcast/common/server.properties",
"extension" :   "properties"
```

### Meta Files
Besides just configuration files, Lighthouse can also read "meta" files&mdash;files that affect how Lighthouse works with properties. Currently, there is only one such supported file type: `.ignore`. The `.ignore` file type is structured as a list of properties that are to be "ignored" during queries to reduce false positives (e.g. dev-specific information). The `.ignore` file can be placed anywhere within the root directory and will be interpreted as a meta file that automatically ignores all properties within the same level as it. For example, if a `hazelcast.ignore` file is placed with the `hazelcast` fabric of the `RWC-Dev` environment, all properties in the `RWC-Dev/hazelcast` fabric that match a property key from the .ignore file will be set to be ignored. For example, if the property `server.dr.mongo.host.port` within `RWC-Dev` was generating false positives within a query report, it could be ignored by writing this `.ignore` file:
```
# RWC-Dev/hazelcast/hazelcast.ignore
server.dr.mongo.host.port
```

and placing that file within the appropriate fabric. All properties with keys matching `server.dr.mongo.host.port` within that fabric would then be set to be ignored during queries.

### Adding Support for New File Types

To create a parser for a new file type, extend the `AbstractParser` class, which contains supporting methods for any given parser. Within the new parser, the only code to be written is the `standardize()` method, which varies from file type to file type. Once this new parser is up and running, the parser and its file types must be added to the `instantiateParser()` method of the `FileParser` class, which delegates functionality to different parsers by file type. A more detailed guide to added a new file type can be found [here](https:/github.com/sumeet-bansal/lighthouse/blob/master/src/main/java/parser/ParserGuide.md). When this is all completed, the new parser will be completely functional and Lighthouse as a whole will be able to parse the new file type, cache and query all files of that type in and from the database.

### Currently Supported File Types:
+ `.properties`
+ `.env`
+ `.jars`
+ `.config`
+ `.yaml`
+ `.xml`
+ `.info`
+ `.whitelist`
+ `.blacklist`
+ `.keyring`
+ `.gateway`
+ `.ignore`
+ `hosts`

and all variants of the above (e.g. `.prop`, `.cfg`, `.yml`).

## General Application Usage

As an executable JAR file, Lighthouse can be easily run (assuming JRE 1.8 are both installed):
```
~$ java -jar lighthouse-1.4.0.jar



                                                                  _-^-_
                                            -            -    --   |@|   --    -         -
 _  _         _      _    _                                       =====
| |(_)  __ _ | |__  | |_ | |__    ___   _   _  ___   ___           |\|
| || | / _` || '_ \ | __|| '_ \  / _ \ | | | |/ __| / _ \          |\|
| || || (_| || | | || |_ | | | || (_) || |_| |\__ \|  __/          |\|
|_||_| \__, ||_| |_| \__||_| |_| \___/  \__,_||___/ \___|        ,/::|.._              ___
       |___/                                                  __,./::: ...^ ~ ~~~~~~~ / ~~

version 1.4.0 -- developed by Sumeet Bansal, Pierce Kelaita, and Gagan Gupta

HOME PAGE -- POSSIBLE COMMANDS
'help'
        goes to the help page for the general diagnostic tool
        Usage: ~$ help
'db'
        switches to the database module to access functions that directly edit the database
        Usage: ~$ db
'query'
        switches to the query module to access functions that analyze the contents of the database
        Usage: ~$ query

lighthouse-v1.4.0: home $
```

### Switching between Modules
Lighthouse is designed as a modular program: it has a "home" module and 2 functional modules. The home module serves as a launchpad for the rest of the program and the true functionality of Lighthouse is split between the "database" and "query" modules. All functionality related to directly working with the database (e.g. clearing data, populating data, viewing data) can be found within the database module. Similarly, all functionality related to querying the database (e.g. finding information about specific properties, generating diagnostic analyses and reports) can be found within the query module. These modules can be easily switched between by inputting the appropriate keyword for each module: `db` for the database module and `query` for the query module.

### Chaining Commands
Lighthouse commands can easily be chained together to execute multiple commands through a single statement as long as they are delimited by a semicolon(`;`). These commands can even be from different modules, so long as each command is preceded by the appropriate module keyword or a properly formatted and executed command from the same module. For example, the following command clears the database, repopulates it with a root directory labeled `root` in the same location, and then searches for all instances of a property named `report/port` within the new database:
```
~$ lighthouse-v1.4.0: home $ db clear --yes; populate root; query find report/port
```
Each of these commands will be expanded on in later sections and each has its own entry in the appropriate module's help page (accessibly through the `help` or `man` commands).

### Exiting Lighthouse
To exit Lighthouse, a simple `exit` or `quit` command closes the application.

## the Database Module
Lighthouse offers basic functionality for editing the SQLite database: a database can be cleared or populated, and info about a database can be printed to the command line. Within SQLite itself, the database  has a single table `properties`. Detailed usage statements can be found in the application's help pages (shown below). To enter the database module, input `db` (the module keyword):

```
lighthouse-v1.4.0: home $ db

DATABASE MODULE -- POSSIBLE COMMANDS
'help'
        goes to the help page for 'db'
        Usage: ~$ help
'populate'
        populates the database with the given files
        Usage: ~$ populate <root directory>
'info'
        provides info about the contents of the database
        Usage: ~$ info
'list'
        prints the structure of the database at optional branches and levels
        Usage: ~$ list [path] [level (1+)]
        Note: the higher the level, the deeper the list.
'ignore'
        provides info about ignored properties, can additionally ignore further properties
        Usage: ~$ ignore [toggle] [-l path] [property] ... [property]
        toggles:
                -t, --true      to ignore the following properties
                                alt.: -i, --ignore
                -f, --false     to acknowledge the following properties
                                alt.: -a, --acknowledge
'clear'
        clears the database
        Usage: ~$ clear
Type the name of another module to switch modules. Available modules: home, db, query.

lighthouse-v1.4.0: db $
```

### Populating the Database
As an example, this command populates the database with a compatible root directory (as outlined in [the Parsing Files section](#parsing-files)):

```
lighthouse-v1.4.0: db $ populate /user/root
Added 17965 properties to database.
```

### Verifying the Database
The results of the previous command can be verified as such:

```
lighthouse-v1.4.0: db $ info

There are currently 17965 properties in the database.

environments    5 (see below)
  > fabrics     21
    - nodes     56
      - files   245

Environments:
1. dev1
2. dev2
...
5. dev5
```

This outputs the number of properties, files, nodes, and fabrics within the database and lists the available environments. Lighthouse also supports finding specific properties within the database, as outlined in the ['Query Module' section](#the-query-module). Since it is a SQLite database, it can also easily be searched or modified through the [SQLite shell](https://sqlite.org/cli.html) or some equivalent (e.g. [SQLite Browser](https://sqlitebrowser.org/)). In order to further verify the structure of the database, Lighthouse supports a flexible `list` command. This command allows the user to see the database directory structure at a specified branch and relative depth. For example, the following command details the database structure down to the node level:

```
lighthouse-v1.4.0: db $ list 3

dev1
 | karaf
 |  | common
 |  | n1
 |  | n2
 | hazelcast
 |  | common
 |  | n1
dev2
 | karaf
...
 | storm
 |  | common
 |  | n1
 |  | n2
 |  | n3
 |  | n4
```

The `list` command can additionally support greater depths or specific directories within the system. For example, the following command details the database structure down to the file level for the `RWC-Dev/storm` path:

```
lighthouse-v1.4.0: db $ list RWC-Dev/storm 2
storm
 | n1
 |  | compression.whitelist
 |  | compression.blacklist
 |  | product-build.info
 |  | server.properties
 |  | storm.yaml
 | ...
 | n4
 |  | compression.blacklist
 |  | product-build.info
 |  | server.properties
 |  | storm.yaml
```
Running the `list` command without any parameters gives a complete scope of the database, which may be impractical at scale.

### `ignore`
Besides being able to populate and verify the database, the database module can also directly edit properties within the database. Properties can be "ignored" during queries to reduce false positives (e.g. dev-specific information). These properties can easily be set via `.ignore` files (elaborated upon in the ["Meta Files" section](#meta-files)) or through the `ignore` command, which provides info about the properties currently set to be ignored in the database and can further ignore or acknowledge properties. Passing in no arguments outputs the properties currently set to be ignored, and any properties given to the command as arguments are set to be ignored by default.

For example, the following commands check what properties are currently being ignored and then sets the property `server.dr.mongo.host.port` within the environment `RWC-Dev` to be ignored:
```
lighthouse-v1.4.0: db $ ignore

2 properties set to be ignored:
 - export/dedup/fieldlist
 - export/output/chunk/size/bytes

lighthouse-v1.4.0: db $ ignore -l RWC-Dev server.dr.mongo.host.port

3 properties set to be ignored:
 - export/dedup/fieldlist
 - export/output/chunk/size/bytes
 - server.dr.mongo.host.port
```

This can be reversed to acknowledge previously ignored properties using the `-a` flag (or the equivalent `-f` flag). For example, the following command acknowledges the previously ignored `export/dedup/fieldlist`:
```
lighthouse-v1.4.0: db $ ignore -a export/dedup/fieldlist

2 properties set to be ignored:
 - export/output/chunk/size/bytes
 - server.dr.mongo.host.port
```

### Clearing the Database
Clearing the database is a fairly straightforward task with the `clear` command:
```
lighthouse-v1.4.0: db $ clear

Clear entire database? (y/n): y
Cleared 17965 properties from database.
```

The `clear` command additionally supports "auto-yes" flags as well; using either the `-y` flag or the `--yes` flag skips the prompts and immediately clears the database:
```
lighthouse-v1.4.0: db $ clear -y
Cleared 17965 properties from database.
```

## the Query Module
The most robust and useful feature of Lighthouse is its advanced query function, which efficiently queries the SQLite database. By default, query results are written to `.csv` spreadsheets in a folder labeled `lighthouse-reports` within the working directory. The query function has two modes: comparing two directories or files at the same level and comparing a single directory internally (i.e. comparing all its subdirectories or files against each other). "Levels" refers to the depth of the the root directory's structure and, by extension, the database's directory structure. To enter the query module, input `query` (the module keyword):

```
lighthouse-v1.4.0: db $ query

QUERY MODULE -- POSSIBLE COMMANDS
'help'
        goes to the help page for 'query'
        Usage: ~$ help
'compare'
        compares the selected root directories and generates appropriate CSVs
        Usage: ~$ compare <path1> <path2>
'exclude'
        excludes selected files or directories from the query
        must be used in conjunction with the 'compare' command
        Usage: ~$ compare <path1> <path2> exclude <path> <path> ... <path>
'grep'
        finds every property key or value in the database matching a given pattern
        Usage: ~$ grep [toggle] <pattern>
        toggles:
                -k      to find matching keys
                -v      to find matching values
'find'
        prints the locations and values of a key/value (can be toggled) within an optional location
        Usage: ~$ find [toggle] [-l path] <pattern>
        toggles:
                -k, --key       to find matching keys
                -v, --value     to find matching values
Type the name of another module to switch modules. Available modules: home, db, query.

lighthouse-v1.4.0: query $
```

### Basic Queries
Functionally, the advanced query function can compare not only files but also directories. For example, the same command can compare not only files but also fabrics:

```
lighthouse-v1.4.0: query $ compare dev1/storm/common/server.properties dev2/storm/common/server.properties
Looking for files with attributes:
        { "environment" : "dev1", "fabric" : "storm", "node" : "common", "filename" : "server.properties" }
        { "environment" : "dev2", "fabric" : "storm", "node" : "common", "filename" : "server.properties" }

Found 1520 properties matching query.

Key discrepancies      14
Value discrepancies     0
Total discrepancies    14
Ignored properties      0

Use default CSV file name lighthouse-report_2017-09-08_17.14.44_server.csv? (y/n): y
Successfully wrote /user/Documents/lighthouse-reports/lighthouse-report_2017-07-28_17.14.44_server.csv

lighthouse-v1.4.0: query $ compare dev1 dev2
Looking for files with attributes:
        { "environment" : "dev1" }
        { "environment" : "dev2" }

Found 4784 properties matching query.

Key discrepancies      444
Value discrepancies    301
Total discrepancies    745
Ignored properties      22

Use default CSV file name lighthouse-report_2017-07-28_17.14.16_dev1_dev2.csv? (y/n): y
Successfully wrote /user/Documents/lighthouse-reports/lighthouse-report_2017-07-28_17.14.16_dev1_dev2.csv
```

Both are equally valid commands so queries can be as precise or broad as needed.

### Wildcards
Additionally, Lighthouse supports wildcards (`*`) which allow for comparing all subdirectories or files in a directory and can be used to further finetune queries. For example, the following command can be used to exclusively compare all `server.properties` files across all fabrics of an environment:

```
lighthouse-v1.4.0: query $ compare dev1/*/common/server.properties dev2/*/common/server.properties
Looking for files with attributes:
        { "environment" : "dev1", "node" : "common", "filename" : "server.properties" }
        { "environment" : "dev2", "node" : "common", "filename" : "server.properties" }

Found 2738 properties matching query.

Key discrepancies      14
Value discrepancies     3
Total discrepancies    17
Ignored properties      5

Use default CSV file name lighthouse-report_2017-07-28_17.14.44_server.csv? (y/n): y
Successfully wrote /user/Documents/lighthouse-reports/lighthouse-report_2017-07-28_17.15.07_server.csv
```

### Internal Queries
Lighthouse additionally supports "internal queries"&mdash;queries within the same directory (i.e. comparing all its subdirectories against each other). For example, the following command internally compares the fabric `RWC-Dev/storm`, i.e. compares all nodes within that single fabric against each other:

```
lighthouse-v1.4.0: query $ compare RWC-Dev/storm

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n1" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n2" }

        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n1" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n3" }

        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n1" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n4" }

        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n2" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n3" }

        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n2" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n4" }

        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n3" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n4" }

Found 20676 properties matching query.

Key discrepancies       13
Value discrepancies     25
Total discrepancies     38
Ignored properties      22

Use default CSV file name lighthouse-report_2017-09-08_13.43.10_n1_n2_n3_n4? (y/n): y
Successfully wrote /user/Documents/lighthouse-reports/lighthouse-report_2017-09-08_13.43.10_n1_n2_n3_n4.csv
```

### Exclusions
To further finetune queries, specific files or directories can be excluded. For example, the following command compares two environments but excludes dev-specific information (in this case, found within `system.properties`):

```
lighthouse-v1.4.0: query $ compare dev1 dev2 exclude */*/*/system.properties
Looking for files with attributes:
        { "environment" : "dev1" }
        { "environment" : "dev2" }

Excluding properties with attributes:
        { "filename" : "server.properties" }

Found 4784 properties and excluded 28 properties matching query.

Key discrepancies      195
Value discrepancies      7
Total discrepancies    202
Ignored properties      22

Use default CSV file name lighthouse-report_2017-08-03_13.08.07_dev1_dev2? (y/n): y
Successfully wrote /user/Documents/lighthouse-reports/lighthouse-report_2017-08-03_13.09.05_dev1_dev2.csv
```

### `find` and `grep`
Lighthouse can also find all instances of a property within the database and supports a custom version of the `grep` command to find specific property keys or values from just fragments of the key or value name. The `find` command finds the properties themselves, given a full key or value name, and details the property path and key or value (key if the value name is given, vice versa). The `-k` and -`v` flags can be used to specify if the search is for keys or values, but the search will default to keys. The `find` command has an optional location flag `-l` to narrow down the query. For example, the following commands find a specific property from the key fragment `lfs/ingestion` and then search for all instances of that property within a specific location (in this case, `dev2/storm`):

```
lighthouse-v1.4.0: query $ grep lfs/ingestion

Found 4 matching property keys:
- lfs/ingestion/large-file/chunk/size
- lfs/ingestion/large-file-event/min/size
- lfs/ingestion/topics
- lfs/ingestion/consumer/group

lighthouse-v1.4.0: query $ find -k -l dev2/storm lfs/ingestion/large-file/chunk/size

Found 1 instance of key "lfs/ingestion/large-file/chunk/size":
 PATH: dev2/storm/common/server.properties/             VALUE: 5000000
```

For an example that searches for a value instead, the following commands find a specific property from the value fragment `elastic` and then search for all instances of that property within a specific location (in this case, `RWC-Dev/`):

```
lighthouse-v1.4.0: query $ grep -v elastic

Found 5 matching property keys:
 - /data/elastic/data
 - /data/elastic/logs
 - elasticsearch
 - org.elasticsearch.action
 - org.elasticsearch.deprecation

lighthouse-v1.4.0: query $ find -v -l RWC-Dev/ elasticsearch

Found 8 instance(s) of value "elasticsearch":
 PATH: RWC-Dev/hazelcast/n1/server.properties/          KEY: es/settings/metrics/cluster/name
 PATH: RWC-Dev/hazelcast/n2/server.properties/          KEY: es/settings/metrics/cluster/name
 PATH: RWC-Dev/karaf/n1/bamboo.server.properties/       KEY: server.primary.es.host.cluster.name
 PATH: RWC-Dev/karaf/n2/bamboo.server.properties/       KEY: server.primary.es.host.cluster.name
 PATH: RWC-Dev/karaf/n3/bamboo.server.properties/       KEY: server.primary.es.host.cluster.name
 PATH: RWC-Dev/storm/n1/server.properties/              KEY: es/settings/metrics/cluster/name
 PATH: RWC-Dev/storm/n2/server.properties/              KEY: es/settings/metrics/cluster/name
 PATH: RWC-Dev/storm/n3/server.properties/              KEY: es/settings/metrics/cluster/name
 PATH: RWC-Dev/storm/n4/server.properties/              KEY: es/settings/metrics/cluster/name
```
The `grep` command additionally supports wildcards (`*`) within key or value fragments. For example, the following command finds all property instances where the key matches `lfs*size`:

```
lighthouse-v1.4.0: query $ grep lfs*size

Found 3 matching property keys:
- lfs/indexable/file/max/size
- lfs/ingestion/large-file/chunk/size
- lfs/ingestion/large-file-event/min/size
```

## Planned Updates
- more options for database modification
    - advanced property editing
    - auto-updating database cache
- validating IP addresses and ports
    
Further suggestions welcome. For information on contacting developers, please see ['Developers' section](#developers).

## Code and Build
The code can be found online on [the GitHub page for Lighthouse](https:/github.com/sumeet-bansal/lighthouse) and each release of Lighthouse can be found online on [the GitHub releases page for Lighthouse](https:/github.com/sumeet-bansal/lighthouse/releases). Detailed documentation can be found [here](https://cdn.rawgit.com/sumeet-bansal/lighthouse/v1.4/documentation/main/index.html). Lighthouse is currently on release v1.4.0 with future releases under development. The repository is set up as a Maven project that can be easily compiled and built.

### Useful Maven Commands
Specific behavior for each of these commands has been defined in the [POM](https:/github.com/sumeet-bansal/lighthouse/blob/master/pom.xml).

To purge the local dependency cache (e.g. in case of corrupted dependencies):
```
~$ mvn dependency:purge-local-repository
```

To clean out the javadocs and compiled source:
```
~$ mvn clean
```

To compile the main and test source code:
```
~$ mvn compile test-compile
```

To run through the JUnit tests:
```
~$ mvn test
```

To assemble a single JAR packaged with the necessary dependencies:
```
~$ mvn assembly:single
```
Note: the `assembly:single` goal must be run in conjunction with the `compile` phase but is already bound to the `package` phase, which includes additionally compilation and unit testing.

To generate the `main` and `test` class javadocs:
```
~$ mvn javadoc:javadoc javadoc:test-javadoc
```
Note: the generated documentation can be found under `documentation/main/` and `documentation/test/`, for the `main` and `test` classes respectively&mdash;they are manually renamed to match the standard repo structure.

These commands can be chained together as well. For example, to compile and assemble the latest executable JAR:
```
~$ mvn clean compile assembly:single
```
or alternatively,
```
~$ mvn clean package
```
which additionally runs through the unit tests to ensure a working build.

## Developers
+ Sumeet Bansal&ensp;&ensp;sumeetbansal@gmail.com
+ Pierce Kelaita&ensp;&ensp;&thinsp;&thinsp;&thinsp;pierce@kelaita.com
+ Gagan Gupta&ensp;&ensp;&thinsp;&thinsp;&thinsp;gagangup@outlook.com