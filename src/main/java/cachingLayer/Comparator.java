package cachingLayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.*;

/**
 * Pulls queried data from MongoDB and compares key values.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * 
 *          TODO Address bug throwing NullPointerException when blocking
 *          directories higher up in the filepath than those specified by the
 *          add query. Also do more QA on block and wildcard functions
 */
public class Comparator {
	private ArrayList<Document[]> queryList = new ArrayList<Document[]>();
	private ArrayList<Document> blockList = new ArrayList<Document>();
	private ArrayList<String[]> table = new ArrayList<String[]>();
	private ArrayList<String> names = new ArrayList<String>();
	private MongoCollection<Document> col;

	@SuppressWarnings("resource")
	public Comparator() {
		col = new MongoClient("localhost", 27017).getDatabase("ADS_DB").getCollection("ADS_COL");
	}

	public ArrayList<String[]> getTable() {
		return table;
	}

	/**
	 * Takes in two path inputs, makes sure the inputs are valid, generates filter
	 * documents to find the files to compare within those parameters
	 * 
	 * @param path1
	 * @param path2
	 * @return filters
	 */
	private Document[] query(String path1, String path2) {
		if (path1.equals(path2)) {
			System.err.println("Paths cannot be the same");
			return null;
		}

		String[] arr1 = path1.split("/");
		String[] arr2 = path2.split("/");
		if (!arr1[0].equals(arr2[0])) {
			System.err.println("Paths must be in the same root file");
			return null;
		} else if (arr1.length != arr2.length) {
			System.err.println("Paths must be at the same specified level");
			return null;
		} else if (arr1.length > 5 || arr1.length < 2) {
			System.err.println("Paths must be at environment, fabric, node, or file level");
			return null;
		}
		Document filter1 = new Document();
		Document filter2 = new Document();
		String[] pathFilters = { "environment", "fabric", "node", "filename" };
		for (int i = 1; i < arr1.length; i++) {
			if (!arr1[i].equals("*")) {
				filter1.append(pathFilters[i - 1], arr1[i]);
			}
		}
		for (int i = 1; i < arr2.length; i++) {
			if (!arr2[i].equals("*")) {
				filter2.append(pathFilters[i - 1], arr2[i]);
			}
		}
		Document[] filters = { filter1, filter2 };
		return filters;
	}

	/**
	 * Adds filters to queryList
	 * 
	 * @param path1
	 * @param path2
	 */
	public void addQuery(String path1, String path2) {
		System.out.println();
		Document[] filters = query(path1, path2);
		try {
			// add query filters to queryList
			queryList.add(filters);
			System.out.println("Looking for files with attributes:");
			System.out.println("\t" + filters[0].toJson());
			System.out.println("\t" + filters[1].toJson());

			// add file names or lowest filepath specification to CSV name
			String[] pathFilters = { "filename", "node", "fabric", "environment" };

			for (String filter : pathFilters) {
				try {
					String name1 = filters[0].getString(filter);
					int end1 = name1.length();
					if (name1.contains(".")) {
						end1 = name1.lastIndexOf(".");
					}
					if (!names.contains(name1.substring(0, end1))) {
						names.add(name1.substring(0, end1));
					}

					String name2 = filters[1].getString(filter);
					int end2 = name1.length();
					if (name2.contains(".")) {
						end2 = name2.lastIndexOf(".");
					}
					if (!names.contains(name2.substring(0, end2))) {
						names.add(name2.substring(0, end2));
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
			e.printStackTrace();
		}
		System.out.println();
	}

	/**
	 * Blocks files with certain attributes in the query from being compared.
	 * 
	 * @param path
	 */
	public void blockQuery(String path) {
		try {
			String[] arr = path.split("/");
			if (arr.length > 5 || arr.length < 2) {
				System.err.println("\nInvalid block input! Path must be at environment, fabric, node, or file level\n");
				return;
			}
			Document filter = new Document();
			String[] pathFilters = { "environment", "fabric", "node", "filename" };

			for (int i = 1; i < arr.length; i++) {
				if (!arr[i].equals("*")) {
					filter.append(pathFilters[i - 1], arr[i]);
				} else {
					filter.append(pathFilters[i - 1], "*");
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
	 * Clears queryList and blockList
	 */
	public void clearQuery() {
		int size = queryList.size() * 2;
		queryList.clear();
		blockList.clear();
		System.out.println("Cleared " + size + " entries from query\n");
	}

	/**
	 * Pulls files from MongoDB matching the filter docs in queryList. Check's each
	 * documents filepath indicators to see if they match with the indicators
	 * specified by the user-given block query. Files that do not competely match
	 * block queries are compared and added to the CSV table
	 */
	public void compare() {
		if (queryList.isEmpty()) {
			return;
		}

		// Create query-specified documents
		ArrayList<Document> docs1 = new ArrayList<Document>();
		ArrayList<Document> docs2 = new ArrayList<Document>();
		int count = 0;
		int blockCount = 0;

		for (Document[] filter : queryList) {

			// Create iterators for added files
			Document filter1 = filter[0];
			Document filter2 = filter[1];
			FindIterable<Document> iter1 = col.find(filter1);
			FindIterable<Document> iter2 = col.find(filter2);
			MongoCursor<Document> cursor1 = iter1.iterator();
			MongoCursor<Document> cursor2 = iter2.iterator();

			// Find and compare all files that are not blocked

			while (cursor1.hasNext()) {

				Document doc = cursor1.next();

				// Check if files on the left side of the query are blocked
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
					// file is blocked
					blockCount++;
				}
				count++;
			}
			while (cursor2.hasNext()) {

				Document doc = cursor2.next();

				// Check if files on the right side of the query are blocked
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
					// file is blocked
					blockCount++;
				}
				count++;
			}

		}
		System.out.println("\nFound " + count + " properties and blocked " + blockCount + " properties matching query");

		// Compare query-specified documents and add header to CSV table representation
		try {
			table = createTable(docs1, docs2);
			String[] header = { "File 1", "Key 1", "Value 1", "File 2", "Key 2", "Value 2", "Key Status",
					"Value Status" };
			table.add(0, header);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stores key and value information in an arraylist, compares them, and adds the
	 * outcomes of the comparisons to the arrayList
	 * 
	 * @param set
	 *            Set of keys to be printed out
	 * @param doc1
	 *            Document with values to be printed out
	 * @param doc2
	 *            Document with values to be printed out
	 */
	private ArrayList<String[]> createTable(ArrayList<Document> props1, ArrayList<Document> props2) {
		
		// generate key set
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

		// set up row information
		ArrayList<String[]> table = new ArrayList<String[]>();

		for (String key : keyset) {
			Document doc1 = new Document();
			Document doc2 = new Document();

			// Find documets in input lists with the key given by keyset
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

			// Write paths from property metadata
			String path1 = doc1.getString("environment") + "/" + doc1.getString("fabric") + "/" + doc1.getString("node")
					+ "/" + doc1.getString("filename");
			String path2 = doc2.getString("environment") + "/" + doc2.getString("fabric") + "/" + doc2.getString("node")
					+ "/" + doc2.getString("filename");

			// Copy property values to strings
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

			// Compare and generate diff report
			if (!(key.equals("path") || key.equals("environment") || key.equals("fabric") || key.equals("node")
					|| key.equals("filename"))) {
				if (v1 != "null" && v2 != "null" && !v1.equals(v2)) { // values are different
					String[] row = { path1, key, v1, path2, key, v2, "Same", "Different" };
					table.add(row);
				} else if (v1 == "null") { // missing key
					String[] row = { "null", "null", "null", path2, key, v2, "Missing in file 1", "Missing in file 1" };
					table.add(row);
				} else if (v2 == "null") { // missing key
					String[] row = { path1, key, v1, "null", "null", "null", "Missing in file 2", "Missing in file 2" };
					table.add(row);
				} else { // everything is the same
					String[] row = { path1, key, v1, path2, key, v2, "Same", "Same" };
					table.add(row);
				}
			}
		}
		return table;
	}

	/**
	 * Writes stored data to a CSV file with a user-specified name and directory
	 * 
	 * @param fileName
	 * @param directory
	 */
	public void writeToCSV(String fileName, String directory) {
		if (queryList.size() == 0) {
			System.err.println("\nUnable write CSV because query list is empty\n");
			return;
		}
		if (table.size() == 1) {
			System.err.println("\nUnable to write CSV because no docuemnts were found matching your query\n");
			return;
		}
		String path = directory + "/" + fileName + ".csv";
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
			System.out.println("\nSuccessfully wrote CSV file " + fileName + ".csv to " + directory + "\n");
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
	 * Writes data to CSV file with a default name if the name is not user-specified
	 * 
	 * @param directory
	 */
	public void writeToCSV(String directory) {
		String defaultName = "diffreport";
		DateFormat nameFormat = new SimpleDateFormat("_yyyy-MM-dd_HH.mm.ss");
		Date date = new Date();
		defaultName += nameFormat.format(date);
		for (String name : names) {
			if (defaultName.length() < 100) {
				defaultName += "_" + name;
			}
		}
		writeToCSV(defaultName, directory);
	}
}
