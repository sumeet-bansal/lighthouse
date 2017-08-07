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

	private ArrayList<Document[]> queryPairs = new ArrayList<>();
	private Set<Document> excludedProps = new HashSet<>();
	private ArrayList<ArrayList<String[]>> tables = new ArrayList<>();

	private ArrayList<String> filenames = new ArrayList<>();

	private Integer[] discrepancies = new Integer[2];

	/**
	 * Constructor.
	 */
	public QueryFunctions() {
		MongoManager.connectToDatabase();
		for (int i = 0; i < discrepancies.length; i++) {
			discrepancies[i] = 0;
		}
	}

	/**
	 * Getter method for table.
	 * 
	 * @return 2D representation of CSV
	 */
	public List<ArrayList<String[]>> getTables() {
		return tables;
	}

	/**
	 * Getter method for discrepancies.
	 * 
	 * @return the discrepancy statistics of a query
	 */
	public Integer[] getDiscrepancies() {
		if (!(discrepancies[0] == null && discrepancies[1] == null)) {
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
	 * Private helper method. Given path inputs, verifies the validity of the inputs
	 * and generates filters for the inputs.
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
			if (!arrL[i].equals("*")) {
				filterL.append(genericPath[i], arrL[i]);
			}
		}
		for (int i = 0; i < arrR.length; i++) {
			if (!arrR[i].equals("*")) {
				filterR.append(genericPath[i], arrR[i]);
			}
		}
		Document[] filters = { filterL, filterR };
		return filters;
	}

	/**
	 * Gets all the files matching a given extension.
	 * 
	 * @param env
	 * @param fabric
	 * @param node
	 * @param wildcard
	 * @return Matching files
	 */
	private ArrayList<Document> getFiles(String env, String fabric, String node, String extension) {
		ArrayList<Document> files = new ArrayList<>();

		// set up filter with given metadata
		Document filter = new Document();
		String[] keys = { env, fabric, node };
		for (int i = 0; i < 3; i++) {
			if (!(keys[i] == null)) {
				filter.append(genericPath[i], keys[i]);
			}
		}

		// query database for files with extension equal to given extension
		Set<String> names = new TreeSet<>();
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String filename = doc.getString("filename");
			String colExt = filename.substring(filename.indexOf('.') + 1);
			if (extension.equalsIgnoreCase(colExt)) {
				names.add(filename);
			}
		}

		// copy data to new document and add to files list
		for (String name : names) {
			Document element = new Document();
			for (int i = 0; i < 3; i++) {
				if (!(keys[i] == null)) {
					element.append(genericPath[i], keys[i]);
				}
			}
			element.append("filename", name);
			files.add(element);
			filter.remove("filename");
		}

		return files;
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

			// if filenames are wildcard/extension, find all files with specified extension
			boolean isWildcard = false;
			ArrayList<Document> left = new ArrayList<>();
			ArrayList<Document> right = new ArrayList<>();
			ArrayList<Document[]> pairs = new ArrayList<>();

			for (int i = 0; i < filters.length; i++) {
				Document doc = filters[i];
				if (doc.getString("filename") != null && doc.getString("filename").startsWith("*.")) {
					isWildcard = true;
					String env = doc.getString("environment");
					String fabric = doc.getString("fabric");
					String node = doc.getString("node");
					String extension = doc.getString("filename").substring(2);

					if (i == 0) {
						left = getFiles(env, fabric, node, extension);
					} else {
						right = getFiles(env, fabric, node, extension);
					}
				}
			}

			// adds query filters to queryPairs
			System.out.println("Looking for properties with attributes:");
			if (!isWildcard) {
				queryPairs.add(filters);
				System.out.println("\t" + filters[0].toJson());
				System.out.println("\t" + filters[1].toJson());
			} else {
				// pair up matching filenames
				ArrayList<String> usedL = new ArrayList<>();
				ArrayList<String> usedR = new ArrayList<>();
				for (Document docL : left) {
					for (Document docR : right) {
						Document[] pair = new Document[2];
						if (docL.getString("filename").equals(docR.getString("filename"))) {
							pair[0] = docL;
							pair[1] = docR;
							pairs.add(pair);
							usedL.add(docL.getString("filename"));
							usedR.add(docR.getString("filename"));
						}
					}
				}

				// print matches to CLI
				for (Document[] pair : pairs) {
					System.out.println("\t" + pair[0].toJson());
					System.out.println("\t" + pair[1].toJson());
					System.out.println();
				}

				// pair the leftovers to a null document holding no properties
				Document nullDoc = new Document();
				for (String str : genericPath) {
					nullDoc.append(str, null);
				}

				if (left.size() != 0 && right.size() != 0) {
					System.out.println("  ----- The following attributes exist on only one side of the query -----\n");
				}

				outerloop: for (Document docL : left) {
					for (String used : usedL) {
						if (docL.getString("filename").equals(used)) {
							continue outerloop;
						}
					}
					Document[] pair = { docL, nullDoc };
					System.out.println("\t" + docL.toJson());
					pairs.add(pair);
				}
				outerloop: for (Document docR : right) {
					for (String used : usedR) {
						if (docR.getString("filename").equals(used)) {
							continue outerloop;
						}
					}
					Document[] pair = { nullDoc, docR };
					System.out.println("\t" + docR.toJson());
					pairs.add(pair);
				}

				// add all pairs to query
				for (Document[] pair : pairs) {
					queryPairs.add(pair);
				}
			}

			// adds file filenames or lowest filepath specification to CSV name
			for (String filter : reversePath) {
				try {
					String name1 = filters[0].getString(filter);
					if (name1.startsWith("*.")) {
						continue;
					}
					int end1 = name1.length();
					if (name1.contains(".")) {
						end1 = name1.lastIndexOf(".");
					}
					String add1 = name1.substring(0, end1);
					if (!filenames.contains(add1)) {
						filenames.add(add1);
					}

					String name2 = filters[1].getString(filter);
					if (name2.startsWith("*.")) {
						continue;
					}
					int end2 = name2.length();
					if (name2.contains(".")) {
						end2 = name2.lastIndexOf(".");
					}
					String add2 = name2.substring(0, end2);
					if (!filenames.contains(add2)) {
						filenames.add(add2);
					}
				} catch (NullPointerException e) {
					continue;
				}
				break;
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
		System.out.println("Cleared " + size + " entries from query.");
	}

	/**
	 * Retrieves filtered files from the MongoDB database, excludes files as
	 * appropriate, compares the remaining queried files, and adds the results to a
	 * CSV file.
	 */
	public void compare() {
		if (queryPairs.isEmpty()) {
			return;
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
	}

	/**
	 * Compares Documents and adds the comparison outcomes to the table.
	 * 
	 * @param propsL
	 *            a List of Documents representing every property in the left side
	 *            of the query
	 * @param propsR
	 *            a List of Documents representing every property in the right side
	 *            of the query
	 * @return the table
	 */
	private ArrayList<String[]> createTable(ArrayList<Document> propsL, ArrayList<Document> propsR) {

		// generates key set
		Set<String> keyset = new LinkedHashSet<>();
		for (Document prop : propsL) {
			keyset.add(prop.getString("key"));
		}
		for (Document prop : propsR) {
			keyset.add(prop.getString("key"));
		}
		keyset.remove("_id"); // auto-generated by MongoDB

		// sets up row information
		ArrayList<String[]> table = new ArrayList<>();
		int keyDiffs = 0;
		int valDiffs = 0;

		for (String key : keyset) {
			Document propL = new Document();
			Document propR = new Document();

			// finds appropriate property from the keyset
			for (Document prop : propsL) {
				if (prop.getString("key").equals(key)) {
					propL = prop;
				}
			}
			for (Document prop : propsR) {
				if (prop.getString("key").equals(key)) {
					propR = prop;
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
	 * Writes stored data to a CSV file with a user-specified name and directory.
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
			System.out.println("\nSuccessfully wrote CSV file " + filename + ".csv to " + directory + "\n");
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
	 * Creates a default name for the CSV file based on the lowest-level metadata
	 * provided in the query.
	 * 
	 * @return the default CSV name
	 */
	public String getDefaultName() {
		String defaultName = "ADS-Report";
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
	 * Takes in a single path input and generates a filter document to find the
	 * files to compare within those parameters.
	 * 
	 * @param path
	 *            the path containing the subdirectories being compared against each
	 *            other
	 */
	public void internalQuery(String path) {

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

		// query each unique pair of files within List
		for (int i = 0; i < subdirs.size() - 1; i++) {
			for (int j = i + 1; j < subdirs.size(); j++) {
				addQuery(subdirs.get(i), subdirs.get(j));
			}
		}
	}

	/**
	 * Queries the database for a user-given key and returns location(s) and
	 * values(s) of the key.
	 * 
	 * @param key
	 *            the key being found
	 * @param location
	 *            a specific path within which to find the key
	 * @return a List of Strings representing each key location and value
	 */
	public static ArrayList<String> findProp(String key, String location) {

		// sets up filter for given key
		Document filter = new Document().append("key", key);
		if (location != null) {
			filter = generatePathFilter(location);
		}

		ArrayList<String> props = new ArrayList<>();
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			Document prop = cursor.next();

			String path = "PATH: ";
			for (int i = 0; i < genericPath.length; i++) {
				path += prop.getString(genericPath[i]) + "/";
			}

			// lines up path with value
			int numSpaces;
			if (path.length() < 50) {
				numSpaces = 50 - path.length();
			} else {
				numSpaces = 5;
			}
			String spaces = "";
			for (int i = 0; i < numSpaces; i++) {
				spaces += " ";
			}

			// adds value to path output
			props.add(path + spaces + "VALUE: " + prop.getString("value"));
		}
		return props;
	}

	/**
	 * Finds every key in the database that contains a user-given pattern.
	 * 
	 * @param pattern
	 *            substring being searched for
	 * @return a Set of property keys that contain the pattern
	 */
	public static Set<String> grep(String pattern, String location) {

		// set up filter for given key
		Document filter = new Document();
		if (location != null) {
			filter = generatePathFilter(location);
		}

		/**
		 * TODO advanced grep logic if no wildcards, check if property contains
		 * elem if wildcards present if charAt(0) != * check that string starts
		 * with 0->1stindexOf(*) if charAt(legnth-1) != * check that string ends
		 * with last(*)->lastChar split up by * for all in split, check if index
		 * of each elem in array is greater than the last
		 */

		Set<String> keyset = new HashSet<>();
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			String key = cursor.next().getString("key");
			if (key.contains(pattern)) {
				keyset.add(key);
			}
		}
		return keyset;
	}

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
