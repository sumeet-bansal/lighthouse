package cachingLayer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

public class Comparator {
	private DbFeeder feeder = new DbFeeder();
	private MongoCollection<Document> col;
	private String root;

	public Comparator(String root) {
		feeder.feedDocs(root);
		this.root = root.substring(3);
		System.out.println("Root folder: " + this.root);
		this.col = feeder.getCol();
	}

	// EXAMPLE PATH: rootName/envName/fabName/nodeName/fileName

	/*
	 * EXAMPLE DOC: key1 : val1 key2 : val2 ... keyN : valN environment :
	 * envName fabric : fabName node : nodeName file : fileName
	 */

	public void query(String path1, String path2) {
		if (path1.equals(path2)) {
			System.err.println("Paths cannot be the same");
			return;
		}
		String[] arr1 = path1.split("/");
		String[] arr2 = path2.split("/");
		if (!arr1[0].equals(arr2[0])) {
			System.err.println("Paths must be in the same root file");
			return;
		} else if (!arr1[0].equals(root.split("/")[0])) {
			System.err.println("Paths must be in the specified root file");
			return;
		} else if (arr1.length != arr2.length) {
			System.err.println("Paths must be at the same specified level");
			return;
		} else if (arr1.length > 5 || arr1.length < 2) {
			System.err.println("Paths must be at environment, fabric, node, or file level");
			return;
		}
		switch (arr1.length) {
		case 2:
			compareEnv(arr1, arr2);
			break;
		case 3:
			compareFab(arr1, arr2);
			break;
		case 4:
			compareNode(arr1, arr2);
			break;
		case 5:
			compareFile(arr1, arr2);
			break;
		}
	}

	// EXAMPLE PATH: Root/Environment/Node/Fabric/File

	private void compareEnv(String[] path1, String[] path2) {
		Document filter1 = new Document().append("environment", path1[1]);
		Document filter2 = new Document().append("environment", path2[1]);
		findFiles(filter1, filter2);
	}

	private void compareFab(String[] path1, String[] path2) {
		Document filter1 = new Document().append("environment", path1[1]);
		filter1.append("fabric", path1[2]);
		Document filter2 = new Document().append("environment", path2[1]);
		filter2.append("fabric", path2[2]);
		findFiles(filter1, filter2);
	}

	private void compareNode(String[] path1, String[] path2) {
		Document filter1 = new Document().append("environment", path1[1]);
		filter1.append("fabric", path1[2]);
		filter1.append("node", path1[3]);
		Document filter2 = new Document().append("environment", path2[1]);
		filter2.append("fabric", path2[2]);
		filter2.append("node", path2[3]);
		findFiles(filter1, filter2);
	}

	private void compareFile(String[] path1, String[] path2) {
		Document filter1 = new Document().append("environment", path1[1]);
		filter1.append("fabric", path1[2]);
		filter1.append("node", path1[3]);
		filter1.append("filename", path1[4]);
		Document filter2 = new Document().append("environment", path2[1]);
		filter2.append("fabric", path2[2]);
		filter2.append("node", path2[3]);
		filter2.append("filename", path2[4]);
		findFiles(filter1, filter2);
	}

	private void findFiles(Document filter1, Document filter2) {
		FindIterable<Document> iter1 = col.find(filter1);
		FindIterable<Document> iter2 = col.find(filter2);
		MongoCursor<Document> cursor1 = iter1.iterator();
		MongoCursor<Document> cursor2 = iter2.iterator();
		System.out.println(filter1);
		System.out.println(filter2);
		System.out.println(cursor1.hasNext());
		System.out.println(cursor2.hasNext());
		while (cursor1.hasNext() && cursor2.hasNext()) {
			compareAll(cursor1.next(), cursor2.next());
			compareDiffs(cursor1.next(), cursor2.next());
		}
	}

	/**
	 * Helper method to iterate through key set and print out list of keys and
	 * values from respective Documents.
	 * 
	 * @param set
	 *            Set of keys to be printed out
	 * @param doc1
	 *            Document with values to be printed out
	 * @param doc2
	 *            Document with values to be printed out
	 */
	private void printIter(Set<String> set, Document doc1, Document doc2) {
		Iterator<String> iter = set.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			String val1 = doc1.get(key).toString();// != null ? doc1.get(key) :
													// "";
			String val2 = doc2.get(key).toString();// != null ? doc2.get(key) :
													// "";
			key = backtickToDot(key);
			if (val1 != null && val2 != null && !val1.equals(val2)) {
				// tabs may cause output to not print
				System.out.println(key + "\t" + val1 + "\t" + key + "\t" + val2 + "\t\t\tdiff");
			} else if (val1 == null) {
				System.out.println("\t\t" + key + "\t" + val2 + "\tmissing in doc1\t");
			} else if (val2 == null) {
				System.out.println(key + "\t" + val1 + "\t\t" + "\tmissing in doc2\t");
			} else {
				System.out.println(key + "\t" + val1 + "\t" + key + "\t" + val2);
			}
		}
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
	public void compareAll(Document doc1, Document doc2) {
		// generates key set
		Set<String> setAmalgam = new TreeSet<>(doc1.keySet());
		setAmalgam.addAll(new TreeSet<String>(doc2.keySet()));
		setAmalgam.remove("_id"); // auto-generated by MongoDB
		/*
		 * // manual check for key set System.out.println("doc1 key set: " +
		 * doc1.keySet()); System.out.println("doc2 key set: " + doc2.keySet());
		 * System.out.println("complete key set " + setAmalgam);
		 */
		// list all keys and values from both Documents
		System.out.println();
		System.out.println("------------------------list------------------------");
		System.out.println();
		printIter(setAmalgam, doc1, doc2);
	}

	/**
	 * Compares keys with different values (including null) between two
	 * Documents in database as per UI specifications.
	 * 
	 * @param doc1
	 *            Document to be compared
	 * @param doc2
	 *            Document to be compared
	 */
	public void compareDiffs(Document doc1, Document doc2) {
		// generates key sets
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
		System.out.println();
		System.out.println("------------------------diff------------------------");
		System.out.println();
		Iterator<String> iter = overlapSet.iterator();
		TreeSet<String> diffSet = new TreeSet<>();
		while (iter.hasNext()) {
			String key = iter.next();
			if (!doc1.get(key).equals(doc2.get(key))) {
				diffSet.add(key);
			}
		}
		printIter(new TreeSet<String>(doc1unique), doc1, doc2);
		printIter(new TreeSet<String>(doc2unique), doc1, doc2);
		printIter(new TreeSet<String>(diffSet), doc1, doc2);
	}

	/**
	 * Due to MongoDB constraints, all dot characters in the key field were
	 * converted to an infrequently used substring--three backticks (```)--and
	 * this converts those key Strings back to their original form with dots.
	 * 
	 * @param key
	 *            the key whose three-backtick sets are being converted
	 * @return the converted key, with dots instead of three-backtick sets
	 */
	private String backtickToDot(String key) {
		key.replace("```", ".");
		return key;
	}

	public static void main(String[] args) {
		Comparator c = new Comparator("C:/Users/Pkelaita/Documents/Comparable");
		String c1 = "Users/Pkelaita/Documents/Comparable/Diffs";
		String c2 = "Users/Pkelaita/Documents/Comparable/ParserResources";
		c.query(c1, c2);
	}

}
