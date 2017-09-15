package queryModule;

import java.util.*;
import driver.SQLiteManager;

/**
 * Pulls queried data from the SQLite database for non-comparison searches.
 * 
 * @author ActianceEngInterns
 * @version 1.4.0
 */
public class QueryFunctions {

	/**
	 * Finds every key or value in the database that contains a user-given pattern.
	 * 
	 * @param pattern
	 *            substring being searched for
	 * @param toggle
	 *            0 for key, 1 for value
	 * @return a Set of property keys or values that contain the pattern
	 */
	public static Set<String> grep(String pattern, int toggle) {

		// determine search type (key or value)
		String type = toggle == 0 ? "key" : "value";

		// replaces regular wildcards with SQL-style wildcards
		while (pattern.contains("*")) {
			pattern = pattern.replace("*", "%");
		}

		// sets up SQL statement to get all unique matches
		String sql = "SELECT DISTINCT " + type + " FROM " + SQLiteManager.getTable() + " WHERE " + type + " LIKE '%"
				+ pattern + "%';";
		Iterator<Map<String, String>> iter = SQLiteManager.select(sql).iterator();

		// iterates through and formats all matches
		Set<String> matches = new HashSet<>();
		while (iter.hasNext()) {
			matches.add(iter.next().get(type));
		}
		return matches;
	}

	/**
	 * Queries the database for a user-given key and returns location(s) and values(s) of the key.
	 * 
	 * @param pattern
	 *            the key or value being found
	 * @param location
	 *            a specific path within which to find the key
	 * @param toggle
	 *            0 for key, 1 for value
	 * @return a List of Maps, each of which represents a single matching property and contains the
	 *         key, value, and path of the matching property instance
	 */
	public static List<Map<String, String>> findProp(String pattern, String location, int toggle) {

		// determine search type (key or value)
		String type = toggle == 0 ? "key" : "value";

		// sets up filter for given property
		Map<String, String> filter = null;
		filter = location != null ? SQLiteManager.generatePathFilter(location) : new LinkedHashMap<>();
		filter.put(type, pattern);

		// sets up SQL statement to get all key/value/path for each match
		String table = SQLiteManager.getTable();
		String sql = "SELECT key, value, path FROM " + table + SQLiteManager.generateSQLFilter(filter, null) + ";";
		return SQLiteManager.select(sql);
	}

}
