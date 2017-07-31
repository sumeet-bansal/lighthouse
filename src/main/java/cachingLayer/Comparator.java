package cachingLayer;

import java.io.*;
import java.text.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.*;

/**
 * Pulls queried data from MongoDB and compares key values.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class Comparator {
	private ArrayList<Document[]> queryList = new ArrayList<Document[]>();
	private ArrayList<Document> blockList = new ArrayList<Document>();
	private ArrayList<String[]> table = new ArrayList<String[]>();
	private ArrayList<String> names = new ArrayList<String>();
	private Integer[] altData = new Integer[2];
	private MongoCollection<Document> col;

	@SuppressWarnings("resource")
	public Comparator() {
		col = new MongoClient("localhost", 27017).getDatabase("ADS_DB").getCollection("ADS_COL");
	}

	/**
	 * Getter method for table.
	 * 
	 * @return 2D representation of CSV
	 */
	public ArrayList<String[]> getTable() {
		return table;
	}

	/**
	 * Getter method for altData.
	 * 
	 * @return the numbers of key and value discrepancies in the query
	 */
	public Integer[] getAltData() {
		if (!(altData[0] == null && altData[1] == null)) {
			Integer[] fullAltData = { altData[0], altData[1], (altData[0] + altData[1]) };
			return fullAltData;
		} else {
			Integer[] fullAltData = { -1, -1, -1 };
			return fullAltData;
		}
	}

	/**
	 * Given path inputs, verifies the validity of the inputs and generates filters
	 * for the inputs.
	 * 
	 * @param path1
	 *            the first path being compared
	 * @param path2
	 *            the other path being compared
	 * @return the generated filters
	 */
	private Document[] query(String path1, String path2) {
		String[] arr1 = path1.split("/");
		String[] arr2 = path2.split("/");
		if (arr1.length != arr2.length) {
			System.err.println("ERROR: Paths must be at the same specified level!");
			return null;
		}
		Document filter1 = new Document();
		Document filter2 = new Document();
		String[] pathFilters = { "environment", "fabric", "node", "filename" };
		System.out.println();
		for (int i = 0; i < arr1.length; i++) {
			if (!arr1[i].equals("*")) {
				filter1.append(pathFilters[i], arr1[i]);
			}
		}
		for (int i = 0; i < arr2.length; i++) {
			if (!arr2[i].equals("*")) {
				filter2.append(pathFilters[i], arr2[i]);
			}
		}
		Document[] filters = { filter1, filter2 };
		return filters;
	}

	/**
	 * Adds path inputs to the internal queryList.
	 * 
	 * @param path1
	 *            the first path being compared
	 * @param path2
	 *            the other path being compared
	 */
	public void addQuery(String path1, String path2) {
		System.out.println();
		Document[] filters = query(path1, path2);
		if (filters == null) {
			return;
		}
		try {

			// adds query filters to queryList
			queryList.add(filters);
			System.out.println("Looking for files with attributes:");
			System.out.println("\t" + filters[0].toJson());
			System.out.println("\t" + filters[1].toJson());

			// adds file names or lowest filepath specification to CSV name
			String[] pathFilters = { "filename", "node", "fabric", "environment" };

			for (String filter : pathFilters) {
				try {
					String name1 = filters[0].getString(filter);
					int end1 = name1.length();
					if (name1.contains(".")) {
						end1 = name1.lastIndexOf(".");
					}
					String add1 = name1.substring(0, end1);
					if (!names.contains(add1)) {
						names.add(add1);
					}

					String name2 = filters[1].getString(filter);
					int end2 = name2.length();
					if (name2.contains(".")) {
						end2 = name2.lastIndexOf(".");
					}
					String add2 = name2.substring(0, end2);
					if (!names.contains(add2)) {
						names.add(add2);
					}
				} catch (NullPointerException e) {
					continue;
				}
				break;
			}

		} catch (Exception e) {
			System.err.println("Unable to add files with attributes:");
			for (Document doc : filters) {
				System.err.println("\t" + doc.toJson());
			}
		}
		System.out.println();
	}

	/**
	 * Blocks queried files with certain attributes from being compared.
	 * 
	 * @param path
	 *            the path of the file being blocked
	 */
	public void blockQuery(String path) {
		try {
			String[] arr = path.split("/");
			Document filter = new Document();
			String[] pathFilters = { "environment", "fabric", "node", "filename" };

			for (int i = 0; i < arr.length; i++) {
				if (!arr[i].equals("*")) {
					filter.append(pathFilters[i], arr[i]);
				} else {
					filter.append(pathFilters[i], "*");
				}
			}
			blockList.add(filter);
			System.out.println("Blocking files with attributes:");
			System.out.println("\t" + filter.toJson());

		} catch (Exception e) {
			System.out.println("Invalid block input!");
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Clears the internal queryList and blockList.
	 */
	public void clearQuery() {
		int size = queryList.size() * 2;
		queryList.clear();
		blockList.clear();
		for (int i = 0; i < altData.length; i++) {
			altData[i] = 0;
		}
		System.out.println("Cleared " + size + " entries from query\n");
	}

	/**
	 * Retrieves filtered files from the MongoDB database, blocks files as
	 * appropriate, compares the remaining queried files, and adds the results to a
	 * CSV file.
	 */
	public void compare() {
		if (queryList.isEmpty()) {
			return;
		}

		// creates Documents as specified in the query
		ArrayList<Document> docs1 = new ArrayList<Document>();
		ArrayList<Document> docs2 = new ArrayList<Document>();
		int queried = 0;
		int blocked = 0;

		for (Document[] filter : queryList) {

			// Create iterators for added files
			MongoCursor<Document> cursor1 = col.find(filter[0]).iterator();
			MongoCursor<Document> cursor2 = col.find(filter[1]).iterator();

			// finds and compares all unblocked files
			while (cursor1.hasNext()) {

				Document doc = cursor1.next();

				// checks if files on the left side of the query are blocked
				boolean checkEnv = true;
				boolean checkFab = true;
				boolean checkNode = true;
				boolean checkFile = true;

				for (Document block : blockList) {
					if (block.getString("environment").equals(doc.getString("environment"))
							|| block.getString("environment").equals("*")) {
						checkEnv = false;
					}
					if (block.getString("fabric").equals(doc.getString("fabric"))
							|| block.getString("fabric").equals("*")) {
						checkFab = false;
					}
					if (block.getString("node").equals(doc.getString("node")) || block.getString("node").equals("*")) {
						checkNode = false;
					}
					if (block.getString("filename").equals(doc.getString("filename"))
							|| block.getString("filename").equals("*")) {
						checkFile = false;
					}
				}

				if (checkEnv || checkFab || checkNode || checkFile) {
					docs1.add(doc);
				} else {
					blocked++;
				}
				queried++;
			}

			while (cursor2.hasNext()) {

				Document doc = cursor2.next();

				// checks if files on the right side of the query are blocked
				boolean checkEnv = true;
				boolean checkFab = true;
				boolean checkNode = true;
				boolean checkFile = true;

				for (Document block : blockList) {
					if (block.getString("environment").equals(doc.getString("environment"))
							|| block.getString("environment").equals("*")) {
						checkEnv = false;
					}
					if (block.getString("fabric").equals(doc.getString("fabric"))
							|| block.getString("fabric").equals("*")) {
						checkFab = false;
					}
					if (block.getString("node").equals(doc.getString("node")) || block.getString("node").equals("*")) {
						checkNode = false;
					}
					if (block.getString("filename").equals(doc.getString("filename"))
							|| block.getString("filename").equals("*")) {
						checkFile = false;
					}
				}

				if (checkEnv || checkFab || checkNode || checkFile) {
					docs2.add(doc);
				} else {
					blocked++;
				}
				queried++;
			}

		}
		System.out.println("\nFound " + queried + " properties and blocked " + blocked + " properties matching query");

		// if single query, sets column names to query comparison
		String file1 = "File 1";
		String file2 = "File 2";
		if (queryList.size() == 1) {
			Document comp1 = queryList.get(0)[0];
			Document comp2 = queryList.get(0)[1];

			String[] pathKeys = { "environment", "fabric", "node", "filename" };
			String name1 = "";
			String name2 = "";
			boolean change = false;
			for (String key : pathKeys) {
				String val1 = comp1.getString(key);
				String val2 = comp2.getString(key);
				if (val1 != null && val2 != null && !val1.equals(val2)) {
					name1 = val1;
					name2 = val2;
					change = true;
				}
			}
			if (change) {
				file1 = name1;
				file2 = name2;
			}

		}

		// compare Documents and add header to CSV table representation
		table = createTable(docs1, docs2);
		String[] header = { file1, "Key 1", "Value 1", file2, "Key 2", "Value 2", "Key Status", "Value Status" };
		table.add(0, header);
	}

	/**
	 * Compares Documents and adds the comparison outcomes to the table.
	 * 
	 * @param props1
	 *            a List of Documents representing every property in the left side
	 *            of the query
	 * @param props2
	 *            a List of Documents representing every property in the right side
	 *            of the query
	 * @return the table
	 */
	private ArrayList<String[]> createTable(ArrayList<Document> props1, ArrayList<Document> props2) {

		// generates key set
		ArrayList<ArrayList<Document>> comparable = new ArrayList<ArrayList<Document>>();
		comparable.add(props1);
		comparable.add(props2);
		Set<String> keyset = new TreeSet<>();
		for (ArrayList<Document> props : comparable) {
			for (Document prop : props) {
				keyset.add(prop.getString("key"));
			}
		}
		keyset.remove("_id"); // auto-generated by MongoDB

		// sets up row information
		ArrayList<String[]> table = new ArrayList<String[]>();
		int keyAlt = 0;
		int valAlt = 0;

		for (String key : keyset) {
			Document doc1 = new Document();
			Document doc2 = new Document();

			// finds appropriate property from the keyset
			for (Document prop : props1) {
				if (prop.getString("key").equals(key)) {
					doc1 = prop;
				}
			}
			for (Document prop : props2) {
				if (prop.getString("key").equals(key)) {
					doc2 = prop;
				}
			}

			// writes paths from property metadata
			String path1 = doc1.getString("environment") + "/" + doc1.getString("fabric") + "/" + doc1.getString("node")
					+ "/" + doc1.getString("filename");
			String path2 = doc2.getString("environment") + "/" + doc2.getString("fabric") + "/" + doc2.getString("node")
					+ "/" + doc2.getString("filename");

			// copies property values to Strings
			Object val1 = doc1.get("value");
			Object val2 = doc2.get("value");
			String v1 = "null";
			String v2 = "null";
			if (val1 != null) {
				v1 = val1.toString();
			}
			if (val2 != null) {
				v2 = val2.toString();
			}

			// compares and generates diff report
			if (!(key.equals("path") || key.equals("environment") || key.equals("fabric") || key.equals("node")
					|| key.equals("filename"))) {
				if (v1 != "null" && v2 != "null" && !v1.equals(v2)) {
					String[] row = { path1, key, v1, path2, key, v2, "Same", "Different" };
					table.add(row);
					valAlt++;
				} else if (v1 == "null") {
					String[] row = { "null", "null", "null", path2, key, v2, "Missing in file 1", "Missing in file 1" };
					table.add(row);
					keyAlt++;
				} else if (v2 == "null") {
					String[] row = { path1, key, v1, "null", "null", "null", "Missing in file 2", "Missing in file 2" };
					table.add(row);
					keyAlt++;
				} else {
					String[] row = { path1, key, v1, path2, key, v2, "Same", "Same" };
					table.add(row);
				}
			}

			altData[0] = keyAlt;
			altData[1] = valAlt;
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
		if (queryList.size() == 0) {
			System.err.println("\nUnable write CSV because query list is empty\n");
			return;
		}
		if (table.size() == 1) {
			System.err.println("\nUnable to write CSV because no docuemnts were found matching your query\n");
			return;
		}
		String path = directory + "/" + filename + ".csv";
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			String content = "";
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
			fw = new FileWriter(path);
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
	 * provided in the query
	 * 
	 * @return default CSV name
	 */
	public String getDefaultName() {
		String defaultName = "ADS-Report";
		DateFormat nameFormat = new SimpleDateFormat("_yyyy-MM-dd_HH.mm.ss");
		Date date = new Date();
		defaultName += nameFormat.format(date);
		for (String name : names) {
			if (defaultName.length() < 100) {
				defaultName += "_" + name;
			}
		}
		return defaultName;
	}
}
