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
 * @version 1.3
 */
public class QueryEngine extends MongoManager {

	// ArrayList of Document pairs {left BSON filter, right BSON filter}
	private ArrayList<Document[]> queryPairs = new ArrayList<>();

	// Set of properties excluded from query--generated in excludeQuery()
	private Set<Document> excludedProps = new HashSet<>();

	/*
	 * String[]: CSV row, formatted {file, key, value, file, key, value, key diff, value diff}
	 * 
	 * ArrayList<String[]>: a single table containing the entirety of a comparison between queries
	 * 
	 * ArrayList<ArrayList<String>>: multiple tables, necessary due to how internal queries generate
	 * several tables for each comparison between fabrics/nodes
	 */
	private ArrayList<ArrayList<String[]>> tables = new ArrayList<>();

	private Set<String> filenames = new TreeSet<>();
	private Map<String, Integer> discrepancies = new HashMap<>();

	/**
	 * Constructor.
	 */
	public QueryEngine() {
		discrepancies.put("key", 0);
		discrepancies.put("value", 0);
		discrepancies.put("ignored", 0);
	}

	/**
	 * Getter method for table.
	 * 
	 * @return a 2D representation of CSV as a series of tables, with each table representing a
	 *         single comparison
	 */
	public List<ArrayList<String[]>> getTables() {
		return tables;
	}

	/**
	 * Getter method for the discrepancy statistics of a query.
	 * 
	 * @return the discrepancy statistics as a Map where the entry "key" corresponds to the total
	 *         number of differences in the keys of a query, the entry "value" corresponds to the
	 *         total number of differences in the values of a query, and the entry "ignored"
	 *         corresponds to the total number of properties that were ignored by the QueryEngine.
	 */
	public Map<String, Integer> getDiscrepancies() {
		return discrepancies;
	}

	/**
	 * Takes in a single path input and generates a series of queries between the subdirectories of
	 * the specified path.
	 * <dl>
	 * <dt>example path parameters:
	 * <dd>dev1/fabric2
	 * </dl>
	 * <dl>
	 * <dt>example queries:
	 * <dd>dev1/fabric2/node1
	 * <dd>dev1/fabric2/node2
	 * <dd>dev1/fabric2/node1
	 * <dd>dev1/fabric2/node3
	 * <dd>dev1/fabric2/node2
	 * <dd>dev1/fabric2/node3
	 * </dl>
	 * 
	 * @param path
	 *            the path containing the subdirectories being compared against each other
	 * @return a String representing the status of the query: null if successful, else error message
	 */
	public String generateInternalQueries(String path) {

		// cleans up path input for processing
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if (path.charAt(path.length() - 1) == '/') {
			path = path.substring(0, path.length() - 1);
		}

		// generates filter and verifies that a directory is being queried
		ArrayList<String> subdirs = new ArrayList<>();
		Document filter = generateFilter(path);
		if (filter.get("filename") != null) {
			return "[ERROR] Internal queries must be at a directory level" + "(i.e. environment, fabric, or node).\n";
		}

		// rebuilds path (in case of wildcard extensions)
		String loc = "";
		for (int j = 0; j < genericPath.length; j++) {
			if (filter.get(genericPath[j]) != null) {
				loc += filter.getString(genericPath[j]) + "/";
			}
		}

		// retrieves all the subdirectories of the path
		MongoCursor<String> cursor = null;
		for (int i = 0; i < reversePath.length; i++) {
			if (filter.get(reversePath[i]) != null) {
				cursor = collection.distinct(reversePath[i - 1], filter, String.class).iterator();
				break;
			}
		}

		// adds all individual paths to dirSet
		while (cursor.hasNext()) {
			String subdir = loc + cursor.next();
			if (filter.get("extension") != null) {
				for (int i = subdir.split("/").length - 1; i < genericPath.length; i++) {
					subdir += "/*";
				}
				subdir = subdir.substring(0, subdir.length());
				subdir += "." + filter.get("extension");
			}
			subdirs.add(subdir);
		}

		if (subdirs.isEmpty()) {
			return "\n[ERROR] No matching properties found.\n";
		}
		if (subdirs.size() < 2) {
			return "\n[ERROR] Directory must contain at least 2 files or subdirectories."
					+ "\n\tOnly matching subdirectory found: " + subdirs.get(0) + "\n";
		}

		// query each unique pair of files within List
		String status = "";
		for (int i = 0; i < subdirs.size() - 1; i++) {
			for (int j = i + 1; j < subdirs.size(); j++) {
				status += addQuery(subdirs.get(i), subdirs.get(j));
			}
		}
		if (status.length() == 0) {
			status = null;
		}
		return status;
	}

	/**
	 * Adds path inputs to the internal queryPairs List.
	 * 
	 * @param pathL
	 *            the first path being compared
	 * @param pathR
	 *            the other path being compared
	 * 
	 * @return a String containing any filters throwing exceptions (empty if none)
	 */
	public String addQuery(String pathL, String pathR) {

		if (pathL.split("/").length != pathR.split("/").length) {
			return "[ERROR] Paths must be at the same specified level.";
		}

		Document[] filters = { generateFilter(pathL), generateFilter(pathR) };
		String status = "";
		try {

			// adds query filters to queryPairs
			status += "\n\t" + filters[0].toJson();
			status += "\n\t" + filters[1].toJson();
			status += "\n";
			queryPairs.add(filters);

			// adds lowest-level path difference to CSV header
			String[] splitL = pathL.split("/");
			String[] splitR = pathR.split("/");
			for (int i = splitL.length - 1; i >= 0; i--) {
				if (!splitL[i].equals(splitR[i])) {
					filenames.add(splitL[i]);
					filenames.add(splitR[i]);
					break;
				}
			}

		} catch (Exception e) {
			status += "\nUnable to add properties with attributes:";
			for (Document doc : filters) {
				status += "\t" + doc.toJson();
			}
		}
		return status;
	}

	/**
	 * Excludes queried files with certain attributes from being compared.
	 * 
	 * @param path
	 *            the path of the file being blocked
	 * @return a String containing all exclusion filters (empty if none)
	 */
	public String exclude(String path) {

		// generates filter for exclusion
		Document filter = generateFilter(path);

		// generates status message
		String status = "\t" + filter.toJson();

		// adds all exclusions to a Set to be cross-referenced during comparison
		MongoCursor<Document> cursor = collection.find(filter).iterator();
		while (cursor.hasNext()) {
			excludedProps.add(cursor.next());
		}

		return status;
	}

	/**
	 * Clears the internal queryPairs and excludedProps Lists.
	 */
	public void clearQuery() {
		int size = queryPairs.size() * 2;
		queryPairs.clear();
		excludedProps.clear();
		for (Map.Entry<String, Integer> entry : discrepancies.entrySet()) {
			discrepancies.put(entry.getKey(), 0);
		}
		System.out.println("Cleared " + size + " entries from query.\n");
	}

	/**
	 * Retrieves filtered files from the MongoDB database, excludes files as appropriate, compares
	 * the remaining queried files, and adds the results to a CSV file.
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

			// creates Document lists as specified in the query
			// Maps used to rapidly hash properties and corresponding Documents for constant lookup
			Map<String, Document> docsL = new HashMap<>();
			Map<String, Document> docsR = new HashMap<>();
			MongoCursor<Document> cursor;

			// finds all unblocked properties on left side of query
			cursor = collection.find(queryPair[0]).iterator();
			while (cursor.hasNext()) {
				Document doc = cursor.next();
				if (excludedProps.contains(doc)) {
					excluded++;
				} else {
					docsL.put(doc.getString("key"), doc);
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
					docsR.put(doc.getString("key"), doc);
				}
				queried++;
			}

			// compares sides of a query
			ArrayList<String[]> table = new ArrayList<>();
			table = createTable(docsL, docsR);
			tables.add(table);

		}

		if (queried == 0) {
			System.out.println("[ERROR] No matching properties found\n");
			return false;
		}

		System.out.println("Found " + queried + " properties and excluded " + excluded + " properties matching query.");

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
	 *            a List of Documents representing every property in the left side of the query
	 * @param propsR
	 *            a List of Documents representing every property in the right side of the query
	 * @return the table as an ArrayList of String[] containing the entirety of a comparison between
	 *         queries, with each String[] representing a CSV row
	 */
	private ArrayList<String[]> createTable(Map<String, Document> propsL, Map<String, Document> propsR) {

		// generates key set
		Set<String> keyAmalgam = new LinkedHashSet<>();
		keyAmalgam.addAll(propsL.keySet());
		keyAmalgam.addAll(propsR.keySet());
		keyAmalgam.remove("_id"); // auto-generated by MongoDB

		// sets up row information
		ArrayList<String[]> table = new ArrayList<>();

		for (String key : keyAmalgam) {

			// finds appropriate property from the keyset
			Document propL = propsL.get(key);
			Document propR = propsR.get(key);

			// copies property values to Strings
			String pathL = propL != null ? propL.getString("path") : "";
			String pathR = propR != null ? propR.getString("path") : "";
			String valueL = propL != null ? propL.getString("value") : "";
			String valueR = propR != null ? propR.getString("value") : "";

			// compares and generates diff report
			String keyStatus, valueStatus;
			if (propL == null) {
				keyStatus = valueStatus = "missing in left";
				discrepancies.put("key", discrepancies.get("key") + 1);
			} else if (propR == null) {
				keyStatus = valueStatus = "missing in right";
				discrepancies.put("key", discrepancies.get("key") + 1);
			} else if (propL.getString("ignore").equals("true") || propL.getString("ignore").equals("true")) {
				keyStatus = valueStatus = "ignored";
				discrepancies.put("ignored", discrepancies.get("ignored") + 1);
			} else if (propL.getString("key").equals(propL.getString("key")) && !valueL.equals(valueR)) {
				keyStatus = "same";
				valueStatus = "different";
				discrepancies.put("value", discrepancies.get("value") + 1);
			} else if (propL.getString("key").equals(propL.getString("key"))) {
				keyStatus = valueStatus = "same";
			} else {
				keyStatus = valueStatus = "";
			}
			String[] row = { pathL, key, valueL, pathR, key, valueR, keyStatus, valueStatus };
			table.add(row);
		}
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
	 * Writes data to CSV file with a default name if the name is not user-specified.
	 * 
	 * @param directory
	 *            the directory the CSV is being written to
	 */
	public void writeToCSV(String directory) {
		String defaultName = getDefaultName();
		writeToCSV(defaultName, directory);
	}

	/**
	 * Creates a default name for the CSV file based on the lowest-level metadata provided in the
	 * query.
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

}
