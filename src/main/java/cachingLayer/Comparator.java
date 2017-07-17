package cachingLayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
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
	 * Clears queryList
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
		Document compare1 = new Document();
		Document compare2 = new Document();
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
					compare1.putAll(doc);
				} else {
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
					compare2.putAll(doc);
				} else {
					blockCount++;
				}
				count++;
			}

		}
		System.out.println("\nFound " + count + " files and blocked " + blockCount + " file(s) matching query");

		// Compare query-specified documents and add header to CSV table representation
		try {
			table = compareAll(compare1, compare2);
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
	private ArrayList<String[]> createTable(Set<String> set, Document doc1, Document doc2) {
		// set up row information
		ArrayList<String[]> table = new ArrayList<String[]>();
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Object val1 = doc1.get(key);
			Object val2 = doc2.get(key);
			key = key.replace("```", ".");

			// valPath arrays represent a value and a path, as a result of splitting
			// the String stored in vals at the separator "\0"
			String[] valPath1 = new String[2];
			String[] valPath2 = new String[2];
			if (val1 != null) {
				valPath1 = val1.toString().split("@@@");
			}
			if (val2 != null) {
				valPath2 = val2.toString().split("@@@");
			}

			// generate diff report
			if (val1 != null && val2 != null && !valPath1[0].equals(valPath2[0])) {
				// diff in value
				String[] row = { valPath1[1], key, valPath1[0], valPath2[1], key, valPath2[0], "Same", "Different" };
				table.add(row);
			} else if (val1 == null) { // missing value
				String[] row = { "null", "null", "null", valPath2[1], key, valPath2[0], "Missing in file 1",
						"Missing in file 1" };
				table.add(row);
			} else if (val2 == null) { // missing value
				String[] row = { valPath1[1], key, valPath1[0], "null", "null", "null", "Missing in file 2",
						"Missing in file 2" };
				table.add(row);
			} else { // everything is the same
				String[] row = { valPath1[1], key, valPath1[0], valPath2[1], key, valPath2[0], "Same", "Same" };
				table.add(row);
			}
		}
		return table;
	}

	/**
	 * Compares all keys between two Documents in database as per UI specifications.
	 * 
	 * @param doc1
	 *            Document to be compared
	 * @param doc2
	 *            Document to be compared
	 */
	private ArrayList<String[]> compareAll(Document doc1, Document doc2) {

		// remove path identifiers
		doc1.remove("environment");
		doc1.remove("fabric");
		doc1.remove("node");
		doc1.remove("filename");
		doc2.remove("environment");
		doc2.remove("fabric");
		doc2.remove("node");
		doc2.remove("filename");

		// generate key set
		Set<String> setAmalgam = new TreeSet<>(doc1.keySet());
		setAmalgam.addAll(new TreeSet<String>(doc2.keySet()));
		setAmalgam.remove("_id"); // auto-generated by MongoDB

		return createTable(setAmalgam, doc1, doc2);
	}

	// In progress
	/**
	 * Compares keys with different values (including null) between two Documents in
	 * database as per UI specifications.
	 * 
	 * @param doc1
	 *            Document to be compared
	 * @param doc2
	 *            Document to be compared
	 */
	@SuppressWarnings("unused")
	private void compareDiffs(Document doc1, Document doc2) {
		// generate key sets
		Set<String> doc1unique = new HashSet<>(doc1.keySet());
		Set<String> doc2unique = new HashSet<>(doc2.keySet());
		Set<String> overlapSet = new HashSet<>(doc1.keySet());
		doc1unique.removeAll(new HashSet<String>(doc2.keySet()));
		doc2unique.removeAll(new HashSet<String>(doc1.keySet()));
		overlapSet.retainAll(new HashSet<String>(doc2.keySet()));
		overlapSet.remove("_id"); // auto-generated by MongoDB
		/*
		 * // manual check for key sets System.out.println("doc1 key set " +
		 * doc1.keySet()); System.out.println("doc2 key set " + doc2.keySet());
		 * System.out.println("unique in doc1: " + doc1unique);
		 * System.out.println("unique in doc2: " + doc2unique);
		 * System.out.println("overlap in d12: " + overlapSet);
		 */
		// gets differences between all files
		System.out.println("------------------------diff------------------------");
		Iterator<String> iter = overlapSet.iterator();
		TreeSet<String> diffSet = new TreeSet<>();
		while (iter.hasNext()) {
			String key = iter.next();
			if (!doc1.get(key).equals(doc2.get(key))) {
				diffSet.add(key);
			}
		}
		createTable(new TreeSet<String>(diffSet), doc1, doc2);
		createTable(new TreeSet<String>(doc1unique), doc1, doc2);
		createTable(new TreeSet<String>(doc2unique), doc1, doc2);
		System.out.println();
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
			} catch (IOException ex) {
				ex.printStackTrace();
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
