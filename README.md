# Lighthouse Diagnostic Suite
Lighthouse is a suite of diagnostic tools developed by the 2017 summer engineering interns of Actiance's Alcatraz division for company-wide use. Lighthouse facilitates multi-team coordination by ensuring parity across server configurations and is a scalable, extensible framework for highly functional diagnostic tools.

# Set Up
__Lighthouse requires having [MongoDB](https://www.mongodb.com/) and JRE 1.8 installed__. It can be run as a command-line executable:
1. Ensure [MongoDB](https://www.mongodb.com/) and JRE 1.8 is installed on the machine.
2. Run `mongod.exe` to ensure a working MongoDB connection.
3. Navigate to the directory containing the executable.
4. Run the following command: `java -jar lighthouse-1.2.jar`. From here, the application help pages are sufficiently detailed to run Lighthouse.

# Functionality (as of v1.2)
Lighthouse standardizes and parses various server configuration files (e.g. .properties, .config, .yaml) from development environments and caches everything in a MongoDB database. The database cache can then be queried for files that can be directly compared for configuration differences via the command line. Lighthouse has flexible queries that allow for comparisons between different directories, comparisons within the same directory (e.g. all nodes within a single fabric against each other), exclusions for specific files and directories (e.g. system information files or irrelevant nodes), and  property searches within the database to see the locations and values of that property. The command-line application features full-fledged help pages with detailed information about each individual command (e.g. purpose, usage). More specific information on the usage of each modular component of Lighthouse can be found within the help pages of the command-line application or [the v1.2 demo presentation](https://docs.google.com/presentation/d/1711yUaoKp8omFTRhEUGPJnlApA5UuHwN2WRJ2gde3c0/pub?start=true&loop=false&delayms=3000). More specific information on the architecture and modular program design can be found below.

# Architecture and Pipeline
Lighthouse was developed entirely within Java and utilizes the MongoDB API significantly.

### Crawler
The `crawler` program acts as a complement to the 'lighthouse' diagnostic suite and can be found [here](https://github.com/sumeet-bansal/crawler).

### Parsing Files
The modular design philosophy behind Lighthouse also shapes how the file parsing system works. Just as Lighthouse serves as a framework into which diagnostic tools can be plugged into, the file parsing system serves as a framework into which different types of file parsers can be plugged into. As of v1.2, Lighthouse currently supports .properties, .yaml, .config, .xml, and hosts files, as well as any variations of those (e.g. .prop, .yml).

The files must be within a compatible directory structure:
```
dev
  > fabric
    > node
      - file
```

The file parser works by separating each property within a given file into a key and a value. It then writes each key-pair value to a MongoDB Document, which represents a single property, and attaches file metadata to the Document.

For example, a line from the `server.properties` file (environment: RWC-Dev, fabric: hazelcast, node: common)
```
report.kibana.index=.kibananew
```
is parsed into the following MongoDB Document:
```
"_id"       :   ObjectId("some_id"),
"key"       :   "report.kibana.new",
"value"     :   ".kibananew",
"filename"  :   "server.properties",
"node"      :   "common",
"fabric"    :   "hazelcast",
"environment"   :   "RWC-Dev",
"path"      :   "RWC-Dev/hazelcast/common/server.properties",
"extension" :   "properties"
```

#### Adding Support for New File Types

To create a parser for a new file type, extend the `AbstractParser` class, which contains supporting methods for any given parser. Within the new parser, the only code to be written is the `standardize()` method, which varies from file type to file type. Once this new parser is up and running, the parser and its file types must be added to the part of the `FileParser` class which delegates functionality to different parsers by file type. When this is all completed, the new parser will instantly be functional and Lighthouse as a whole will be able to parse the new file type, cache and query all files of that type in and from the database.

Currently supported file types:
+ .properties
+ .config
+ .yaml
+ .xml (beta)
+ hosts

### Editing the Database
#### Populating the Database
Lighthouse offers basic functionality for editing the MongoDB database: a database can be cleared or populated, and info about a database can be printed to the command line. Within MongoDB itself, the database is listed as `LH_DB` and collection as `LH_COL`. Detailed usage statements can be found in the application's help pages, but as an example, this command populates the database with a compatible root directory (as outlined in [the Parsing Files section](#parsing-files)):

```
lighthouse-v1.2: db $ populate /user/root
Added 1017 properties to database.
```

#### Verifying the Database
The results of the previous command can be verified as such:

```
lighthouse-v1.2: db $ info
Database Info:

Properties      1017
Files           423
Nodes           21
Fabrics         9

Environments:
 - dev1
 - dev2
 - dev3
```

This outputs the number of properties, files, nodes, and fabrics within the database and lists the available environments. Lighthouse also supports finding specific properties within the database, as outlined in the ['Querying the Database' section](#querying-the-database). Since it is a basic MongoDB database, it can also easily be searched or modified through the Mongo application itself or some equivalent (e.g. [Robo 3T, formerly RoboMongo](https://robomongo.org/)). In order to further verify the structure of the database, Lighthouse supports a flexible 'list' command. This command allows the user to see the database folder structure at a specified lowest level between 1 and 4, where 1 represents the file level and 4 represents the environment level. For example, the following command details the database structure down to the node level:

```
lighthouse-v1.2: db $ list 2

DATABASE STRUCTURE @ NODE LEVEL

dev1
  > karaf
    - common
    - h1
    - h2
  > hazelcast
    - common
    - h1
dev2
  > karaf
    ...
  > storm
    - common
    - h1
    - h2
    - h3
    - h4
```

### Querying the Database
The most robust and useful feature of Lighthouse is its advanced query function, which allows for efficient querying of the MongoDB database. By default, query results are written to CSVs in the working directory. The query function currently has two modes, comparing two levels and comparing a single level internally (i.e. comparing all its subdirectories against each other). "Levels" refers to the directory structure of the root directory and, by extension, the database.

#### Basic Queries
Functionally, the advanced query function can compare not only files but also directories. For example, the same command can compare not only files but also fabrics of environments:

```
lighthouse-v1.2: query $ compare dev1/storm/common/server.properties dev2/storm/common/server.properties
Looking for files with attributes:
        { "environment" : "dev1", "fabric" : "storm", "node" : "common", "filename" : "server.properties" }
        { "environment" : "dev2", "fabric" : "storm", "node" : "common", "filename" : "server.properties" }

Found 1290 properties matching query.

Key discrepancies:      14
Value discrepancies:    0
Total discrepancies:    14

Use default CSV file name lighthouse-report_2017-07-28_17.14.44_server.csv? (y/n): y
Successfully wrote lighthouse-report_2017-07-28_17.14.44_server.csv to /user/Documents/lighthouse-reports

lighthouse-v1.2: query $ compare dev1 dev2
Looking for files with attributes:
        { "environment" : "dev1" }
        { "environment" : "dev2" }

Found 2378 properties matching query.

Key discrepancies:      10
Value discrepancies:    12
Total discrepancies:    22

Use default CSV file name lighthouse-report_2017-07-28_17.14.16_dev1_dev2.csv? (y/n): y
Successfully wrote lighthouse-report_2017-07-28_17.14.16_dev1_dev2.csv to /user/Documents/lighthouse-reports
```

Both are equally valid commands but can be as precise or broad as needed.

#### Wildcards
Additionally, Lighthouse supports wildcards (`*`) which allow for comparing all subdirectories or files in a directory and can be used to further finetune queries. For example, the following command can be used to exclusively compare all `server.properties` files across all fabrics of an environment:

```
lighthouse-v1.2: query $ compare dev1/*/common/server.properties dev2/*/common/server.properties
Looking for files with attributes:
        { "environment" : "dev1", "node" : "common", "filename" : "server.properties" }
        { "environment" : "dev2", "node" : "common", "filename" : "server.properties" }

Found 2071 properties matching query.

Key discrepancies:      14
Value discrepancies:    0
Total discrepancies:    14

Use default CSV file name lighthouse-report_2017-07-28_17.14.44_server.csv? (y/n): y
Successfully wrote lighthouse-report_2017-07-28_17.15.07_server.csv to /user/Documents/lighthouse-reports
```

#### Internal Queries
Lighthouse additionally supports "internal queries"--queries within the same directory (i.e. comparing all its subdirectories against each other). For example, the following command internally compares a fabric, i.e. compares all nodes within a single fabric against each other:

```
lighthouse-v1.2: query $ compare RWC-Dev/storm

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n4" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n3" }

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n4" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n2" }

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n4" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n1" }

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n3" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n2" }

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n3" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n1" }

Looking for properties with attributes:
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n2" }
        { "environment" : "RWC-Dev", "fabric" : "storm", "node" : "n1" }

Found 12752 properties and excluded 0 properties matching query.

Key discrepancies       21
Value discrepancies     51
Total discrepancies     72

Use default CSV file name lighthouse-report_2017-08-03_13.43.05_n4_n3_n2_n1? (y/n): y
Successfully wrote lighthouse-report_2017-08-03_13.43.10_n4_n3_n2_n1.csv to /user/Documents/lighthouse-reports
```

#### Exclusions
To further finetune queries, specific files or directories can be excluded. For example, the following command can be used to compare two environments but exclude dev-specific information (found within `system.properties`):

```
lighthouse-v1.2: query $ compare dev1 dev2 exclude */*/*/system.properties
Looking for files with attributes:
        { "environment" : "dev1" }
        { "environment" : "dev2" }

Excluding properties with attributes:
        { "filename" : "server.properties" }

Found 2071 properties and excluded 516 properties matching query.

Key discrepancies:      14
Value discrepancies:    0
Total discrepancies:    14

Use default CSV file name lighthouse-report_2017-08-03_13.08.07_dev1_dev2? (y/n): y
Successfully wrote lighthouse-report_2017-08-03_13.09.05_dev1_dev2.csv to /user/Documents/lighthouse-reports
```

#### `find` and `grep`
Lighthouse can also find all instances of a property within the database and supports a custom version of the 'grep' command to find specific property keys or values from just fragments of the key or value name. The `-k` and -`v` flags can be used to if the search is for keys or values. Both commands have optional location flags `-l` to narrow down the query. For example, the following commands find a specific property from the key fragment `report` and then search for all instances of that property within a specific location (in this case, `dev1/karaf`):

```
lighthouse-v1.2: query $ grep -k report

Found 11 matching property keys:
report.kibana.index
report.kibana.buildNum
report.port
report/domain/name
report/kibana/index
report/rollup/retry/limit
report/kibana/buildNum
report.rollup.batch.size
report.savedsearch.scroll.page.size
report.rollup.retry.limit
report/http_protocol

lighthouse-v1.2: query $ find -k report.kibana.index -l dev1/karaf

Location: dev1/karaf

Found 2 instances of property "report.kibana.index":
PATH: dev1/karaf/h1/server.properties/         VALUE: .kibananew
PATH: dev1/karaf/h2/server.properties/         VALUE: .kibanaold
```

# Planned Updates
- more options for database modification
    - advanced property editing
    - auto-updating database cache
- more configurable CSV diagnostic reports
    
Further suggestions welcome. For information on contacting developers, please see ['Developers' section](#developers).

# Code and Build
The code can be found online on [the GitHub page for Lighthouse](https://github.com/sumeet-bansal/lighthouse) and each release of Lighthouse can be found online on [the GitHub releases page for Lighthouse](https://github.com/sumeet-bansal/lighthouse/releases). Lighthouse is currently on release v1.2 with future releases under development. The repo is set up as a Maven project that can be easily compiled and built.

# Developers
+ Sumeet Bansal (sbansal@actiance.com)
+ Pierce Kelaita (pkelaita@actiance.com)
+ Gagan Gupta (ggupta@actiance.com)