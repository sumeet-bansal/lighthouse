# Alcatraz Diagnostic Suite (ADS)
This is a suite of diagnostic tools being developed for the Alcatraz platform by the 2017 summer engineering interns of Actiance's Alcatraz division. ADS facilitates multi-team coordination by ensuring parity across server configurations and is a scalable, extensible framework for highly functional diagnostic tools.

# Set Up
ADS can be run as a command-line executable:
1. Ensure [MongoDB](https://www.mongodb.com/) is installed on the machine.
1. Run `mongod.exe` to ensure a working MongoDB connection.
2. Navigate to the directory containing the executable.
3. Run the following command: `java -jar ADS_v1.1.jar`. From here, the application help pages are sufficiently detailed to run ADS.

# Functionality (as of v1.1)
ADS retrieves various server configuration files (e.g. .properties, .config, .yaml) from individual dev environments, populates user-specified root directories, standardizes and parses each file, and caches everything in a MongoDB database. The database cache can then be queried for files that can be directly compared for configuration differences via the command line. ADS has flexible queries that allow for comparisons between different directories, comparisons within the same directory (e.g. all nodes within a single fabric against each other), exclusions for specific files and directories (e.g. system information files or irrelevant nodes), and  property searches within the database to see the locations and values of that property. The command-line application features full-fledged help pages with detailed information about each individual command (e.g. purpose, usage). More specific information on the usage of each modular component of ADS can be found within the help pages of the command-line application or [the v1.1 demo presentation](https://docs.google.com/presentation/d/1711yUaoKp8omFTRhEUGPJnlApA5UuHwN2WRJ2gde3c0/pub?start=true&loop=false&delayms=3000). More specific information on the architecture and modular program design can be found below.

# Architecture and Pipeline
ADS was developed entirely within Java and utilizes the ZooKeeper and MongoDB APIs significantly.

### Generating Directories
ADS works with the ZooKeeper API to generate full .properties files for all fabrics within a given dev environment. It generates a directory for the given dev environment within the root directory, and then, given a path within ZooKeeper to follow, it creates nested directories for each fabric within that ZooKeeper path. Within those fabric directories, it creates another directory named `common`, under which the .properties files can be found for each fabric. Furthermore, certain branches can be excepted from the general `server.properties` file for readability--these excepted branches are then given their own file named `server.<branch>.properties`. Detailed usage statements can be found in the application help pages, but as an example, this command creates a directory `dev` within `C:/Users/user/root` and populates it with the fabrics found within `/alcatrazproperties/2.5` of the host IP `127.0.0.1`:

```
~$ java -jar ADS_v1.1.jar zk generate 127.0.0.1 /alcatrazproperties/2.5 C:/Users/user/root dev blacklist purge
generated .properties file(s) for fabric1
generated .properties file(s) for fabric2
generated .properties file(s) for fabric3
``` 

For each fabric within that environment, the command will then create some file `root/dev/fabric/server.properties` and, if there is a `blacklist` branch but no `purge` branches within the fabric, the command will additionally create a file `root/dev/fabric/server.blacklist.properties`.

### Parsing Files
The modular design philosophy behind ADS also shapes how the file parsing system works. Just as ADS serves as a framework into which diagnostic tools can be plugged into, the file parsing system serves as a framework into which different types of file parsers can be plugged into. As of v1.1, ADS currently supports .properties, .yaml, .config, .xml, and hosts files, as well as any variations of those (e.g. .prop, .yml).

The file parser works by separating each property within a given file into a key and a value. It then writes each key-pair value to a MongoDB Document, which represents a single property, and attaches file metadata to the Document.

To create a parser for a new file type, extend the `AbstractParser` class, which contains supporting methods for any given parser. Within the new parser, the only code to be written is the `standardize()` method, which varies from file type to file type. Once this new parser is up and running, the parser and its file types must be added to the part of the `FileParser` class which delegates functionality to different parsers by file type. When this is all completed, the new parser will instantly be functional and ADS as a whole will be able to parse the new file type, cache and query all files of that type in and from the database.

Currently supported file types:
+ .properties
+ .config
+ .yaml
+ .xml (beta)
+ hosts

### Editing the Database
ADS offers basic functionality for editing the MongoDB database: a database can be cleared or populated, and info about a database can be printed to the command line. Detailed usage statements can be found in the application's help pages, but as an example, this command populates the database with a root directory:

```
~$ java -jar ADS_v1.1.jar db populate C:/Users/user/root
Added 1017 properties to database.
```

The results of the previous command can be verified as such:

```
~$ java -jar ADS_v1.1.jar db info
Count: 
1017 properties currently in database.
```

This outputs the number of properties within the database. ADS also supports finding specific properties within the database, as outlined in the ['Querying the Database' section](#querying-the-database). Since it is a basic MongoDB database, it can also easily be searched or modified through the Mongo application itself or some equivalent (e.g. [Robo 3T, formerly RoboMongo](https://robomongo.org/)).

### Querying the Database
The most robust and useful feature of ADS is its advanced query function, which allows for efficient querying of the MongoDB database. The query function currently has two smode, comparing two levels and comparing a single level internally (i.e. comparing all its subdirectories against each other). "Levels" refers to the directory structure of the root directory and, by extension, the database. What this means functionally is that the advanced query function can compare not only files but also directories like nodes, fabrics, and entire environments. For example, the same command can compare not only files but also fabrics or environments:

```
~$ java -jar ADS_v1.1.jar query compare dev1/fabric/common/server.properties dev2/fabric/common/server.properties
Looking for files with attributes:
        { "environment" : "dev1", "fabric" : "storm", "node" : "common", "filename" : "server.properties" }
        { "environment" : "dev2", "fabric" : "storm", "node" : "common", "filename" : "server.properties" }

Found 1290 properties matching query.
Key discrepancies:      14
Value discrepancies:    0
Total discrepancies:    14
Use default CSV file name? (y/n): y
Successfully wrote CSV file diffreport_2017-07-28_17.14.44_server.csv to C:\Users\user\root\ADS reports

~$ java -jar ADS_v1.1.jar query compare dev1 dev2
Looking for files with attributes:
        { "environment" : "dev1" }
        { "environment" : "dev2" }

Found 2378 properties matching query.
Key discrepancies:      10
Value discrepancies:    12
Total discrepancies:    22
Use default CSV file name? (y/n): y
Successfully wrote CSV file diffreport_2017-07-28_17.14.16_dev1_dev2.csv to C:\Users\user\root\ADS reports
```

Both are equally valid commands but can be as precise or broad as needed. Additionally, ADS supports wildcards (`*`) which allow for comparing all subdirectories or files in a directory and can be used to further finetune queries. For example, the following command can be used to exclusively compare all `server.properties` files across all fabrics of an environment:

```
~$ java -jar ADS_v1.1.jar query compare dev1/*/common/server.properties dev2/*/common/server.properties
Looking for files with attributes:
        { "environment" : "dev1", "node" : "common", "filename" : "server.properties" }
        { "environment" : "dev2", "node" : "common", "filename" : "server.properties" }

Found 2071 properties matching query.
Key discrepancies:      14
Value discrepancies:    0
Total discrepancies:    14
Use default CSV file name? (y/n): y
Successfully wrote CSV file diffreport_2017-07-28_17.15.07_server.csv to C:\Users\user\root\ADS reports
```

ADS additionally supports "internal queries"--queries within the same directory (i.e. comparing all its subdirectories against each other). For example, the following command internally compares a fabric, i.e. compares all nodes within a single fabric against each other:

```
~$ java -jar ADS_v1.1.jar query compare RWC-Dev/storm

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

Use default CSV file name ADS-Report_2017-08-03_13.43.05_n4_n3_n2_n1? (y/n): y
Successfully wrote CSV file ADS-Report_2017-08-03_13.43.10_n4_n3_n2_n1.csv to C:\Users\user\root\ADS Reports
```

To further finetune queries, specific files or directories can be excluded. For example, the following command can be used to compare two environments but exclude dev-specific information (found within `system.properties`):

```
~$ java -jar ADS_v1.1.jar query compare dev1 dev2 exclude */*/*/system.properties
Looking for files with attributes:
        { "environment" : "dev1" }
        { "environment" : "dev2" }

Excluding properties with attributes:
        { "filename" : "server.properties" }

Found 2071 properties and excluded 516 properties matching query.
Key discrepancies:      14
Value discrepancies:    0
Total discrepancies:    14
Use default CSV file name ADS-Report_2017-08-03_13.08.07_dev1_dev2? (y/n): y
Successfully wrote CSV file ADS-Report_2017-08-03_13.09.05_dev1_dev2.csv to C:\Users\user\root\ADS reports
```

ADS can also find all instances of a property within the database and supports a custom version of the 'grep' command to find specific property keys from just fragments of the key name. Both of these commands have optional location parameters to narrow down the query. For example, the following commands find a specific property from the key fragment `report` and then search for all instances of that property within a specific location (for this example, `dev1/karaf`):

```
~$ java -jar ADS_v1.1.jar query grep report

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

~$ java -jar ADS_v1.1.jar query find report.kibana.index dev1/karaf

Found 2 instances of property "report.kibana.index":
PATH: dev1/karaf/h1/server.properties/         VALUE: .kibananew
PATH: dev1/karaf/h2/server.properties/         VALUE: .kibanaold
```

# Planned Updates
- more support for file types
	- .info
	- .erb
- more options for directory generation
	- automated directory population directly from dev VMs
	- greater ZooKeeper support
- more options for database modification
	- advanced property editing
	- auto-updating database cache
- more configurable CSV diagnostic reports
	
Further suggestions welcome. For information on contacting developers, please see ['Developers' section](#developers).

# Accessibility
The code can be found online on [the GitHub page for ADS](https://github.com/sbansal21/AlcatrazDiagnosticSuite) and each release of ADS can be found online on [the GitHub releases page for ADS](https://github.com/sbansal21/AlcatrazDiagnosticSuite/releases). ADS is currently on release v1.1 with future releases under development. 

# Developers
+ Sumeet Bansal (sbansal@actiance.com)
+ Pierce Kelaita (pkelaita@actiance.com)
+ Gagan Gupta (ggupta@actiance.com)