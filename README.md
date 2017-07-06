# Alcatraz Diagnostic Suite

This is a suite of diagnostic tools being developed by the summer engineering interns of Actiance for the Alcatraz platform.

# Comparator

Currently under development. Retrieves various server configuration files (e.g. .properties, .config, .yaml) from source control, standardizes each file, parses it for keys and values, and caches it in a MongoDB database. From that database cache, the files can then be directly compared for configuration differences via the web UI, and that cache can be queried for specific differences as well.
