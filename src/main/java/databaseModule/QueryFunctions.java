package databaseModule;

import java.io.*;
import java.text.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.client.*;

/**
 * Pulls queried data from MongoDB and compares key values.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class QueryFunctions extends MongoManager {

	// ArrayList of Document pairs {left BSON filter, right BSON filter}
	private ArrayList<Document[]> queryPairs = new ArrayList<>();

	// Set of properties excluded from query--generated in excludeQuery()
	private Set<Document> excludedProps = new HashSet<>();

	/*
	 * String[]: CSV row, formatted {file, key, value, file, key, value, key
	 * diff, value diff}
	 * 
	 * ArrayList<String>: a single table containing the entirety of a comparison
	 * between queries
	 * 
	 * ArrayList<ArrayList<String>>: multiple tables, necessary due to how
	 * internal queries generate several tables for each comparison between
	 * fabrics/nodes
	 */
	private ArrayList<ArrayList<String[]>> tables = new ArrayList<>();

	private Set<String> filenames = new TreeSet<>();
	private Integer[] discrepancies = new Integer[2];

	/**
	 * Constructor.
	 */
	public QueryFunctions() {
		for (int i = 0; i < discrepancies.length; i++) {
			discrepancies[i] = 0;
		}
	}

	/**
	 * Getter method for table.
	 * 
	 * @return a 2D representation of CSV as a series of tables, with each table
	 *         representing a single comparison
	 */
	public List<ArrayList<String[]>> getTables() {
		return tables;
	}

	/**
	 * Getter method for the discrepancy statistics of a query.
	 * 
	 * @return the discrepancy statistics as an Integer[] where Integer[0] is
	 *         the total number of differences in the keys of a query and
	 *         Integer[1] is the total number of differences in the values of a
	 *         query
	 */
	public Integer[] getDiscrepancies() {
		if (discrepancies[0] != null && discrepancies[1] != null) {
			Integer diffkey = discrepancies[0];
			Integer diffval = discrepancies[1];
			Integer[] report = { diffkey, diffval, diffkey + diffval };
			return report;
		} else {
			Integer[] report = { -1, -1, -1 };
			return report;
		}
	}

	/**
	 * Takes in a single path input and generates a filter document to find the
	 * files to compare within those parameters.
	 * <p>
	 * <dl>
	 * <dt>example path parameters:
	 * <dd>dev1/fabric2
	 * </dl>
	 * </p>
	 * <p>
	 * <dl>
	 * <dt>example queries:<p>
	 * <dd>dev1/fabric2/node1
	 * <dd>dev1/fabric2/node2
	 * </p><p>
	 * <dd>dev1/fabric2/node1
	 * <dd>dev1/fabric2/node3
	 * </p><p>
	 * <dd>dev1/fabric2/node2
	 * <dd>dev1/fabric2/node3
	 * </dl>
	 * </p>
	 * 
	 * @param path
	 *            the path containing the subdirectories being compared against
	 *            each other
	 */
	public void generateInternalQueries(String path) {

		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if (path.charAt(path.length() - 1) == '/') {
			path = path.substring(0, path.length() - 1);
		}

		// given directory, populates List with all sub-directories
		Set<String> dirSet = new HashSet<>();
		String[] arr = path.split("/");
		Document filter = new Document();
		for (int i = 0; i < arr.length; i++) {
			if (!arr[i].equals("*")) {
				filter.append(genericPath[i], arr[i]);
			}
		}
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String loc = "";
			for (int i = 0; i <= arr.length; i++) {
				loc += doc.getString(genericPath[i]) + "/";
			}
			dirSet.add(loc.substring(0, loc.length() - 1));
		}
		ArrayList<String> subdirs = new ArrayList<>();
		for (String subdir : dirSet) {
			subdirs.add(subdir);
		}
		if (subdirs.size() < 2) {
			System.err.println("\n[ERROR] Directory must contain at least 2 files or subdirectories.\n");
			return;
		}

		// query each unique pair of files within List
		for (int i = 0; i < subdirs.size() - 1; i++) {
			for (int j = i + 1; j < subdirs.size(); j++) {
				addQuery(subdirs.get(i), subdirs.get(j));
			}
		}
	}

	/**
	 * Adds path inputs to the internal queryPairs List.
	 * 
	 * @param pathL
	 *            the first path being compared
	 * @param pathR
	 *            the other path being compared
	 */
	public void addQuery(String pathL, String pathR) {
		Document[] filters = generateFilters(pathL, pathR);
		if (filters == null) {
			return;
		}
		try {

			// adds query filters to queryPairs
			System.out.println("Looking for properties with attributes:");
			queryPairs.add(filters);
			System.out.println("\t" + filters[0].toJson());
			System.out.println("\t" + filters[1].toJson());

			// adds lowest-level path difference to CSV header
			String[] splitL = pathL.split("/");
			String[] splitR = pathR.split("/");
			for (int i = splitL.length-1; i >= 0; i--) {
				if (!splitL[i].equals(splitR[i])) {
					filenames.add(splitL[i]);
					filenames.add(splitR[i]);
					break;
				}
			}

		} catch (Exception e) {
			System.err.println("Unable to add properties with attributes:");
			for (Document doc : filters) {
				System.err.println("\t" + doc.toJson());
			}
			e.printStackTrace();
		}
	}

	/**
	 * Private helper method. Given path inputs, verifies the validity of the
	 * inputs and generates filters for the inputs.
	 * <p>
	 * <dl>
	 * <dt>example path parameters:
	 * <dd>dev1/fabric2
	 * <dd>dev2/fabric2
	 * </dl>
	 * </p>
	 * <p>
	 * <dl>
	 * <dt>example filters:
	 * <dd>{environment: "dev1", fabric: "fabric2"}
	 * <dd>{environment: "dev2", fabric: "fabric2"}
	 * </dl>
	 * </p>
	 * 
	 * @param pathL
	 *            the first path being compared
	 * @param pathR
	 *            the other path being compared
	 * @return the generated filters
	 */
	private Document[] generateFilters(String pathL, String pathR) {
		String[] arrL = pathL.split("/");
		String[] arrR = pathR.split("/");
		if (arrL.length != arrR.length) {
			System.err.println("ERROR: Paths must be at the same specified level.");
			return null;
		}
		Document filterL = new Document();
		Document filterR = new Document();
		System.out.println();
		for (int i = 0; i < arrL.length; i++) {
			if (arrL[i].charAt(0) != ('*')) {
				filterL.append(genericPath[i], arrL[i]);
			} else if (arrL[i].startsWith("*.")) {
				filterL.append("extension", arrL[i].substring(2));
			}
		}
		for (int i = 0; i < arrR.length; i++) {
			if (arrR[i].charAt(0) != ('*')) {
				filterR.append(genericPath[i], arrR[i]);
			} else if (arrR[i].startsWith("*.")) {
				filterR.append("extension", arrR[i].substring(2));
			}
		}
		Document[] filters = { filterL, filterR };
		return filters;
	}

	/**
	 * Excludes queried files with certain attributes from being compared.
	 * 
	 * @param path
	 *            the path of the file being blocked
	 */
	public void exclude(String path) {
		try {
			String[] arr = path.split("/");
			Document filter = new Document();

			for (int i = 0; i < arr.length; i++) {
				if (!arr[i].equals("*")) {
					filter.append(genericPath[i], arr[i]);
				}
			}

			System.out.println("\nExcluding properties with attributes:");
			System.out.println("\t" + filter.toJson());

			MongoCursor<Document> cursor = collection.find(filter).iterator();
			while (cursor.hasNext()) {
				excludedProps.add(cursor.next());
			}

		} catch (Exception e) {
			System.out.println("Invalid 'exclude' input.");
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Clears the internal queryPairs and excludedProps Lists.
	 */
	public void clearQuery() {
		int size = queryPairs.size() * 2;
		queryPairs.clear();
		excludedProps.clear();
		for (int i = 0; i < discrepancies.length; i++) {
			discrepancies[i] = 0;
		}
		System.out.println("Cleared " + size + " entries from query.\n");
	}

	/**
	 * Retrieves filtered files from the MongoDB database, excludes files as
	 * appropriate, compares the remaining queried files, and adds the results
	 * to a CSV file.
	 * 
	 * @return true if query is valid, false if not
	 */
	public boolean compare() {
		if (queryPairs.isEmpty()) {
			return false;
		}

		int queried = 0;
		int excluded = 0;

		// adds properties matching both sides of query
		for (Document[] queryPair : queryPairs) {

			// creates Documents as specified in the query
			ArrayList<Document> docsL = new ArrayList<>();
			ArrayList<Document> docsR = new ArrayList<>();
			MongoCursor<Document> cursor;

			// finds all unblocked properties on left side of query
			cursor = collection.find(queryPair[0]).iterator();
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				if (excludedProps.contains(doc)) {
					excluded++;
				} else {
					docsL.add(doc);
				}
				queried++;
			}

			// finds all unblocked properties on right side of query
			cursor = collection.find(queryPair[1]).iterator();
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				if (excludedProps.contains(doc)) {
					excluded++;
				} else {
					docsR.add(doc);
				}
				queried++;
			}

			// compares sides of a query
			ArrayList<String[]> table = new ArrayList<>();
			table = createTable(docsL, docsR);
			tables.add(table);

		}
		System.out.println(
				"\nFound " + queried + " properties and " + "excluded " + excluded + " properties matching query.");

		// if single query, sets column filenames to query comparison
		String left = "root";
		String right = "root";
		if (queryPairs.size() == 1) {
			Document comp1 = queryPairs.get(0)[0];
			Document comp2 = queryPairs.get(0)[1];

			String[] pathKeys = { "environment", "fabric", "node", "filename" };
			for (String key : pathKeys) {
				String val1 = comp1.getString(key);
				String val2 = comp2.getString(key);
				if (val1 != null && val2 != null && !val1.equals(val2)) {
					left = val1;
					right = val2;
				}
			}

		}

		String[] header = { left, "left key", "left value", right, "right key", "right value", "key status",
				"value status" };
		ArrayList<String[]> tableHeader = new ArrayList<>();
		tableHeader.add(header);
		tables.add(0, tableHeader);
		return true;
	}

	/**
	 * Compares Documents and adds the comparison outcomes to the table.
	 * 
	 * @param propsL
	 *            a List of Documents representing every property in the left
	 *            side of the query
	 * @param propsR
	 *            a List of Documents representing every property in the right
	 *            side of the query
	 * @return the table
	 */
	private ArrayList<String[]> createTable(ArrayList<Document> propsL, ArrayList<Document> propsR) {

		// generates key set
		Set<String> keyAmalgam = new LinkedHashSet<>();
		for (Document prop : propsL) {
			keyAmalgam.add(prop.getString("key"));
		}
		for (Document prop : propsR) {
			keyAmalgam.add(prop.getString("key"));
		}
		keyAmalgam.remove("_id"); // auto-generated by MongoDB

		// sets up row information
		ArrayList<String[]> table = new ArrayList<>();
		int keyDiffs = 0;
		int valDiffs = 0;

		for (String key : keyAmalgam) {
			Document propL = new Document();
			Document propR = new Document();

			// finds appropriate property from the keyset
			for (Document prop : propsL) {
				if (prop.getString("key").equals(key)) {
					propL = prop;
					break;
				}
			}
			for (Document prop : propsR) {
				if (prop.getString("key").equals(key)) {
					propR = prop;
					break;
				}
			}

			// writes paths from property metadata
			String pathL = propL.getString("environment") + "/" + propL.getString("fabric") + "/"
					+ propL.getString("node") + "/" + propL.getString("filename");
			String pathR = propR.getString("environment") + "/" + propR.getString("fabric") + "/"
					+ propR.getString("node") + "/" + propR.getString("filename");

			// copies property values to Strings
			String valueL = "null";
			String valueR = "null";
			if (propL.get("value") != null) {
				valueL = propL.get("value").toString();
			}
			if (propR.get("value") != null) {
				valueR = propR.get("value").toString();
			}

			// compares and generates diff report
			if (valueL != "null" && valueR != "null" && !valueL.equals(valueR)) {
				String[] row = { pathL, key, valueL, pathR, key, valueR, "same", "different" };
				table.add(row);
				valDiffs++;
			} else if (valueL == "null") {
				String[] row = { "null", "null", "null", pathR, key, valueR, "missing in left", "missing in left" };
				table.add(row);
				keyDiffs++;
			} else if (valueR == "null") {
				String[] row = { pathL, key, valueL, "null", "null", "null", "missing in right", "missing in right" };
				table.add(row);
				keyDiffs++;
			} else {
				String[] row = { pathL, key, valueL, pathR, key, valueR, "same", "same" };
				table.add(row);
			}
		}
		discrepancies[0] += keyDiffs;
		discrepancies[1] += valDiffs;
		return table;
	}

	/**
	 * Writes stored data to a CSV file with a user-specified name and
	 * directory.
	 * 
	 * @param filename
	 *            the user-specified filename
	 * @param directory
	 *            the user-specified directory
	 */
	public void writeToCSV(String filename, String directory) {
		if (queryPairs.size() == 0) {
			System.err.println("\nUnable to write CSV because query list is empty.\n");
			return;
		}
		if (tables.get(0).size() == 1 && tables.size() == 1) {
			System.err.println("\nUnable to write CSV because no documents were found matching your query.\n");
			return;
		}
		String path = directory + "/" + filename + ".csv";
		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			String content = "";
			for (ArrayList<String[]> table : tables) {
				for (String[] arr : table) {
					for (String str : arr) {
						if (str.equals("null")) {
							content += ("\"\"" + ",");
						} else {
							str = str.replace("\"", "'");
							content += ("\"" + str + "\"" + ",");
						}
					}
					content += "\n";
				}
			}
			fw = new FileWriter(path, true);
			bw = new BufferedWriter(fw);
			bw.write(content);
			System.out.println("\nSuccessfully wrote " + filename + ".csv to " + directory + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writes data to CSV file with a default name if the name is not
	 * user-specified.
	 * 
	 * @param directory
	 *            the directory the CSV is being written to
	 */
	public void writeToCSV(String directory) {
		String defaultName = getDefaultName();
		writeToCSV(defaultName, directory);
	}

	/**
	 * Creates a default name for the CSV file based on the lowest-level
	 * metadata provided in the query.
	 * 
	 * @return the default CSV name
	 */
	public String getDefaultName() {
		String defaultName = "lighthouse-report";
		DateFormat nameFormat = new SimpleDateFormat("_yyyy-MM-dd_HH.mm.ss");
		Date date = new Date();
		defaultName += nameFormat.format(date);
		for (String filename : filenames) {
			if (defaultName.length() < 100) {
				defaultName += "_" + filename;
			}
		}
		return defaultName;
	}

	/**
	 * Queries the database for a user-given key and returns location(s) and
	 * values(s) of the key.
	 * 
	 * @param input
	 *            the key or value being found
	 * @param location
	 *            a specific path within which to find the key
	 * @return a List of Strings representing each key location and value
	 */
	public static ArrayList<String> findProp(String input, String location, int type) {
		// determine search type (key or value)
		String searchFor;
		if (type == 0) {
			searchFor = "key";
		} else {
			searchFor = "value";
		}

		// sets up filter for given property
		Document filter = new Document();
		if (location != null) {
			filter = generatePathFilter(location);
		}
		filter.append(searchFor, input);

		ArrayList<String> props = new ArrayList<>();
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			Document prop = cursor.next();
			String path = "PATH: ";
			for (int i = 0; i < genericPath.length; i++) {
				path += prop.getString(genericPath[i]) + "/";
			}

			// set up spacing for CLI output
			String key = prop.getString("key");
			String value = prop.getString("value");
			final int PATH_MAX_SPACING = 80;

			// lines up path with key
			int numSpaces1;
			if (path.length() < PATH_MAX_SPACING) {
				numSpaces1 = PATH_MAX_SPACING - path.length();
			} else {
				numSpaces1 = 5;
			}
			String spaces1 = "";
			for (int i = 0; i < numSpaces1; i++) {
				spaces1 += " ";
			}

			// output line with path, key, and value
			String line = path + spaces1;
			if (type == 1) {
				line += "Key: " + key;
			} else {
				line += "Value: " + value;
			}
			props.add(line);
		}
		return props;
	}

	/**
	 * Finds every key or value in the database that contains a user-given
	 * pattern.
	 * 
	 * @param pattern
	 *            substring being searched for
	 * @return a Set of property keys or values that contain the pattern
	 */
	public static Set<String> grep(String pattern, int type) {
		// determine search type (key or value)
		String searchFor;
		if (type == 0) {
			searchFor = "key";
		} else {
			searchFor = "value";
		}

		// set up filter for given key
		Document filter = new Document();

		Set<String> dataset = new HashSet<>();
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			String data = cursor.next().getString(searchFor);
			if (data.contains(pattern)) {
				dataset.add(data);
			}
		}
		return dataset;
	}

	/**
	 * Private helper method. Generates mongo filters for a given path
	 * 
	 * @param path
	 * @return
	 */
	private static Document generatePathFilter(String path) {
		Document filter = new Document();
		String[] metadata = path.split("/");
		for (int i = 0; i < metadata.length; i++) {
			if (!metadata[i].equals("*")) {
				filter.append(genericPath[i], metadata[i]);
			}
		}
		return filter;
	}

}
