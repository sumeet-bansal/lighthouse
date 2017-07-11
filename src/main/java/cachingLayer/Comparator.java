package cachingLayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

/**
 * Pulls queried data from mongoDB and compares key values
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017/07/10
 */
public class Comparator {
	private DbFeeder feeder = new DbFeeder();
	private MongoCollection<Document> col;
	private String root;
	private ArrayList<Document[]> queryList = new ArrayList<Document[]>();
	private String[][] table;

	public Comparator(String root) {
		feeder.feedDocs(root);
		this.root = root.substring(3);
		System.out.println("Root folder: " + this.root + "\n\n");
		this.col = feeder.getCol();
	}

	public String[][] getTable() {
		return table;
	}

	/**
	 * Takes in two path inputs, makes sure the inputs are valid, generates
	 * filter documents to find the files to compare within those parameters
	 * 
	 * @param path1
	 * @param path2
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
		} else if (!arr1[0].equals(root.split("/")[0])) {
			System.err.println("Paths must be in the specified root file");
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
		String[] filterKeys = { "environment", "fabric", "node", "filename" };
		for (int i = 1; i < arr1.length; i++) {
			if (!arr1[i].equals("*")) {
				filter1.append(filterKeys[i - 1], arr1[i]);
			}
		}
		for (int i = 1; i < arr2.length; i++) {
			if (!arr2[i].equals("*")) {
				filter2.append(filterKeys[i - 1], arr2[i]);
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
		Document[] filters = query(path1, path2);
		try {
			queryList.add(filters);
			System.out.println("Added files with attributes:");
			System.out.println("\t" + filters[0]);
			System.out.println("\t" + filters[1]);
		} catch (Exception e) {
			System.err.println("Unable to add files with attributes:");
			for (Document doc : filters) {
				System.err.println("\t" + doc);
			}
		}
	}

	/**
	 * Removes filters from queryList
	 * 
	 * @param path1
	 * @param path2
	 */
	public void removeQuery(String path1, String path2) {
		Document[] filters = query(path1, path2);
		try {
			int count = 0;
			for (int i = 0; i < queryList.size(); i++) {
				Document[] arr = queryList.get(i);
				if (arr[0].equals(filters[0]) && arr[1].equals(filters[1])) {
					queryList.remove(i);
					if (count == 0) {
						System.out.println("Removed files with attributes:");
						System.out.println("\t" + filters[0]);
						System.out.println("\t" + filters[1]);
					}
					count++;
				}
			}
			if (count == 0) {
				System.err.println("Unable to remove files with attributes:");
				for (Document doc : filters) {
					System.err.println("\t" + doc);
				}
			}
		} catch (Exception e) {
			System.err.println("Unable to remove files with attributes:");
			for (Document doc : filters) {
				System.err.println("\t" + doc);
			}
		}
	}

	/**
	 * Clears queryList
	 */
	public void clearQuery() {
		int size = queryList.size();
		queryList.clear();
		System.out.println("Cleared " + size + " file(s) from query\n");
	}

	/**
	 * Pulls files from MongoDB matching the filter docs in queryList
	 */
	public void compare() {
		ArrayList<String[]> data;
		if (queryList.isEmpty()) {
			System.out.println("query is empty");
			return;
		}

		// Create query-specified documents
		Document compare1 = new Document();
		Document compare2 = new Document();
		for (Document[] filter : queryList) {
			Document filter1 = filter[0];
			Document filter2 = filter[1];
			FindIterable<Document> iter1 = col.find(filter1);
			FindIterable<Document> iter2 = col.find(filter2);
			MongoCursor<Document> cursor1 = iter1.iterator();
			MongoCursor<Document> cursor2 = iter2.iterator();
			System.out.println("valid: " + (cursor1.hasNext() && cursor2.hasNext()));
			while (cursor1.hasNext() && cursor2.hasNext()) {
				compare1.putAll(cursor1.next());
				compare2.putAll(cursor2.next());
			}
		}

		try {
			// Compare query-specified documents
			data = compareAll(compare1, compare2);

			// Convert ArrayList<String[]> to 2D array suitable for CSV export
			String[][] dataTable = new String[data.size() + 1][6];
			String[] header = { "Key 1", "Value 1", "Key 2", "Value 2", "Key Status", "Value Status" };
			dataTable[0] = header;
			for (int i = 0; i < data.size(); i++) {
				dataTable[i + 1] = data.get(i);
			}
			table = dataTable;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stores key and value information in an arraylist, compares them, and adds
	 * the outcomes of the comparisons to the arrayList
	 * 
	 * @param set
	 *            Set of keys to be printed out
	 * @param doc1
	 *            Document with values to be printed out
	 * @param doc2
	 *            Document with values to be printed out
	 */
	private ArrayList<String[]> createTable(Set<String> set, Document doc1, Document doc2) {
		ArrayList<String[]> table = new ArrayList<String[]>();
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Object val1 = doc1.get(key);
			Object val2 = doc2.get(key);
			// MongoDB does not allow dots in key names the database, so they
			// are converted to the string "```" in order to be stored, and
			// are converted back to dots to be displayed
			key = key.replace("```", ".");
			// diff in values
			if (val1 != null && val2 != null && !val1.equals(val2)) {
				String[] row = createRow(key, val1, key, val2, "Same", "Different");
				table.add(row);
			} else if (val1 == null) { // missing value
				String[] row = createRow("null", "null", key, val2, "Missing in file 1", "Missing in file 1");
				table.add(row);
			} else if (val2 == null) { // missing value
				String[] row = createRow(key, val1, "null", "null", "Missing in file 2", "Missing in file 2");
				table.add(row);
			} else { // everything is the same
				String[] row = createRow(key, val1, key, val2, "Same", "Same");
				table.add(row);
			}
		}
		return table;
	}

	/**
	 * Converts values to Strings and adds keys, values, and statuses to an
	 * Array representing one row in a CSV file
	 * 
	 * @param key1
	 * @param val1
	 * @param key2
	 * @param val2
	 * @param keyStatus
	 *            Same, Different, Missing in file 1, or Missing in file 2
	 * @param valStatus
	 *            Same, Different, Missing in file 1, or Missing in file 2
	 * @return row
	 */
	private String[] createRow(String key1, Object val1, String key2, Object val2, String keyStatus, String valStatus) {
		String[] row = { key1, val1.toString(), key2, val2.toString(), keyStatus, valStatus };
		return row;
	}

	/**
	 * Compares all keys between two Documents in database as per UI
	 * specifications.
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
	 * Compares keys with different values (including null) between two
	 * Documents in database as per UI specifications.
	 * 
	 * @param doc1
	 *            Document to be compared
	 * @param doc2
	 *            Document to be compared
	 */
	@SuppressWarnings("unused")
	private void compareDiffs(Document doc1, Document doc2) {
		// remove path identifiers
		doc1.remove("environment");
		doc1.remove("fabric");
		doc1.remove("node");
		doc1.remove("filename");
		doc2.remove("environment");
		doc2.remove("fabric");
		doc2.remove("node");
		doc2.remove("filename");

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
			System.err.println("Cannot write CSV; query list is empty");
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
}
