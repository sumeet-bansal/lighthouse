## A Guide to Adding Parsers

Lighthouse's modular `parser` package makes adding parser for unsupported file types a simple and streamlined process.

To write a new parser, extend the `AbstractParser` class--it contains all necessary supporting methods to integrate the new parser into the `parser` package. Within the new parser, the only code to be written is the `standardize()` method, an abstract method inherited from `AbstractParser`. Within the code, the File must be parsed into `data`, a Map<String, Object> inherited from `AbstractParser`. Within the map `data`, each map entry represents a single property and the map represents the entirety of the file as a collection of properties. Below is some skeleton code for writing a new parser:
```java
package parser;

import java.io.*;		// necessary for reading in files

/**
 * Parses some type of file. Standard naming convention for the package is that the class
 * be named "Parse" and then whatever file type it's meant to parse, or an abbreviation
 * (e.g. "ParseProp" for .properties files or "ParseHosts" for hosts files).
 * 
 * @author ActianceEngIntern
 * @version 1.0
 */
public class ParseTemplate extends AbstractParser {

	/**
	 * Parses and standardizes input File into a Map of keys and values.
	 * 
	 * @param input
	 *		the File to be parsed and standardized
	 */
	public void standardize(File input) {

		// read in parameter 'input'
		try {
			String line = new String();
			BufferedReader reader = new BufferedReader(new FileReader(input));
			while ((line = br.readLine()) != null) {
				String key;
				Object value;

				// populate the key and value

				data.put(key, value);	// data is a Map<String, Object>

			}
			br.close();
		} catch (IOException e) {

			// standard error message for incorrectly formatted files
			String name = input.getAbsolutePath();
			System.err.println("\n[DATABASE ERROR] " + name + " is not in correct <type> format.\n");

			error = true;	// a boolean flag inherited from AbstractParser
		}
	}
```

Once the new parser class is functional, the method `instantiateParser()` of the `FileParser` class must be modified to integrate the new parser into the `parser` package. Below is the relevant snippet of `instantiateParser()`:
```java
/**
 * Determines the type of the File and instantiates the parser accordingly.
 */
public void instantiateParser() {
	try {

		// determines the extension (i.e. type) of a file
		String filepath = input.getAbsolutePath();
		String extension = filepath.substring(filepath.lastIndexOf('.') + 1).toLowerCase()
		extension = filepath.endsWith("hosts") ? "hosts" : extension;	// special exception

		// instantiates the appropriate parser for a file type and its variants
		switch (extension) {
		case "prop":
		case "properties":
			data = new ParseProp();
			break;
		case "cfg":
		case "config":
			data = new ParseConf();
			break;
		case "hosts":
			data = new ParseHosts();
			break;
		case "yml":
		case "yaml":
			data = new ParseYaml();
			break;
		...
		}
	}
}
```

The method works by determining the extension of a file and assigning the proper non-abstract parser subclass to the `AbstractParser data`. In some cases, such as with "hosts" files, the code to determine the extension may need to be modified, but in most cases, that won't be needed. Thus, to add the new parser, a new `case` (plus a `case` for each variant) must be added to the `switch` statement. To continue the previous example, the following lines within the `switch` add the `ParseTemplate` case to `FileParser`:
```java
		...
		switch(extension) {
		...
		case "templ":	// an example variant of .template that'll do the same as the following case
		case "template":
			data = new ParseTemplate();	// instantiates the AbstractParser data as a ParseTemplate
			break;
		...
		}
		...
```

After a successful compile, the new parser will be immediately functional. Since files are only parsed when the database is populated, the database may need to be repopulated to add the properties of the new file type.

### For Meta/Internal Files

If the file type to be parsed is a "meta" or "internal" file type, such as the `.ignore` file type (custom for Lighthouse operations), an additional line is required to declare the parser as one for internal file types:

```java
		...
		case "templ":
		case "template":
			data = new ParseTemplate();
			data.setInternal(true);		// declares a parser as one for internal file types
			break;
		...
```

The proper processing of an internal file necessitates further modification to the source code of Lighthouse since it modifies how Lighthouse inherently works with files and properties. Internal files will mostly likely have to modify some field in the database entry of a property, which would occur when the database is initially populated in the `populate(java.lang.String path)` method of class `DbFunctions` in package `databaseModule`. For a reference point, see how `.ignore` files are handled within the class and method.