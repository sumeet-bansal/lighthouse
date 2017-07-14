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

import com.mongodb.client.*;

/**
 * Pulls queried data from MongoDB and compares key values.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class Comparator {
	private MongoCollection<Document> col;
	private ArrayList<Document[]> queryList = new ArrayList<Document[]>();
	private ArrayList<String[]> table = new ArrayList<String[]>();
	private ArrayList<String> names = new ArrayList<String>();

	public Comparator(MongoCollection<Document> col) {
		this.col = col;
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
		Document[] filters = query(path1, path2);
		try {
			queryList.add(filters);
			System.out.println("Adding files with attributes:");
			System.out.println("\t" + filters[0].toJson());
			System.out.println("\t" + filters[1].toJson());

			String name1 = filters[0].getString("filename");

			int end = name1.length();
			if (name1.contains(".")) {
				end = name1.lastIndexOf(".");
			}
			names.add(name1.substring(0, end));

		} catch (Exception e) {
			System.err.println("Unable to add files with attributes:");
			for (Document doc : filters) {
				System.err.println("\t" + doc);
			}
			e.printStackTrace();
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
		int size = queryList.size()*2;
		queryList.clear();
		System.out.println("Cleared " + size + " file(s) from query\n");
	}

	/**
	 * Pulls files from MongoDB matching the filter docs in queryList
	 */
	public void compare() {
		if (queryList.isEmpty()) {
			System.out.println("query is empty");
			return;
		}

		// Create query-specified documents
		Document compare1 = new Document();
		Document compare2 = new Document();
		int count = 0;
		for (Document[] filter : queryList) {
			Document filter1 = filter[0];
			Document filter2 = filter[1];
			FindIterable<Document> iter1 = col.find(filter1);
			FindIterable<Document> iter2 = col.find(filter2);
			MongoCursor<Document> cursor1 = iter1.iterator();
			MongoCursor<Document> cursor2 = iter2.iterator();
			while (cursor1.hasNext()) {
				compare1.putAll(cursor1.next());
				count ++;
			}
			while (cursor2.hasNext()) {
				compare2.putAll(cursor2.next());
				count++;
			}
		}
		System.out.println("Found " + count + " file(s) matching query");
		
		try {
			// Compare query-specified documents and add header to CSV table representation
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
