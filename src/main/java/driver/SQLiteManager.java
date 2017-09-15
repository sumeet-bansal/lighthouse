package driver;

import java.sql.*;
import java.util.*;

public class SQLiteManager {

	private static final String database = "lighthouse.db";
	private static final String table = "properties";

	public static String[] genericPath = { "environment", "fabric", "node", "filename" };
	public static String[] reversePath = { "filename", "node", "fabric", "environment" };

	private static Connection connection;

	/**
	 * Connects to the database and creates the table if necessary.
	 */
	public static void connectToDatabase() {
		try {
			DriverManager.setLoginTimeout(30);
			connection = DriverManager.getConnection("jdbc:sqlite:" + database);
			Statement statement = connection.createStatement();
			String sql = "CREATE TABLE IF NOT EXISTS properties (\n	key text,\n	value text,\n"
					+ "	filename text,\n	node text,\n	fabric text,\n	environment text,\n"
					+ "	path text,\n	extension text,\n	ignore text\n);";
			statement.execute(sql);
		} catch (SQLException e) {
			exit(e);
		}
	}

	/**
	 * Given path inputs, generates filters in a format standard across the entire project.
	 * <dl>
	 * <dt>example path parameters:
	 * <dd>dev1/fabric2
	 * </dl>
	 * <dl>
	 * <dt>example filter:
	 * <dd>{environment: "dev1", fabric: "fabric2"}
	 * </dl>
	 * 
	 * @param path
	 *            the path for which a filter is being generated
	 * @return the generated filter
	 */
	public static Map<String, String> generatePathFilter(String path) {

		// cleans up the path
		while (path.contains("\\")) {
			path = path.replace("\\", "/");
		}
		while (path.contains("//")) {
			path = path.replace("//", "/");
		}

		path = path.length() > 0 && path.charAt(0) == '/' ? path.substring(1) : path;
		if (path.length() == 0) {
			return new LinkedHashMap<>();
		}

		// splits the path by delimiter and adds metadata to filter
		String[] split = path.split("/");
		Map<String, String> filter = new LinkedHashMap<>();
		for (int i = 0; i < split.length; i++) {
			if (split[i].charAt(0) != ('*')) {
				filter.put(genericPath[i], split[i]);
			} else if (split[i].startsWith("*.")) {
				filter.put("extension", split[i].substring("*.".length()));
			}
		}
		return filter;
	}

	/**
	 * Given a {@link java.util.Collection} of Strings, generates a SQL-compatible set.
	 * 
	 * @param collection
	 *            a {@link java.util.Collection} of Strings
	 * @param full
	 *            true if the collection contains all the properties of the intended set, else false
	 * @param prepared
	 *            true if the collection is intended to be used in a prepared SQL statement as
	 *            placeholders for values, else false
	 * @return the SQL-compatible set of properties
	 */
	public static String generateSQLSet(Collection<String> collection, boolean full, boolean prepared) {
		String sql = "";
		if (collection == null || collection.isEmpty()) {
			return sql;
		}
		Iterator<String> iter = collection.iterator();
		while (iter.hasNext()) {
			String item = iter.next();
			if (prepared) {
				item = "?";
			}
			sql += item;
			if (iter.hasNext()) {
				sql += ", ";
			}
		}
		return full ? "(" + sql + ")" : sql;
	}

	/**
	 * Generates a SQL command-style filter for a given Map and optional set of keys.
	 * <dl>
	 * <dt>example filter:
	 * <dd>{environment=RWC-Dev, fabric=hazelcast}
	 * </dl>
	 * <dl>
	 * <dt>generated filter:
	 * <dd>" WHERE (environment = 'RWC-Dev') AND (fabric = 'hazelcast')"
	 * </dl>
	 * <dl>
	 * <dt>example filter and set of keys:
	 * <dd>{environment=RWC-Dev, fabric=hazelcast}
	 * <dd>{'lfs/ingestion/large-file/chunk/size', 'lfs/ingestion/topics', 'report/kibana/version'}
	 * </dl>
	 * <dl>
	 * <dt>generated filter:
	 * <dd>" WHERE (environment = 'RWC-Dev') AND (fabric = 'hazelcast') AND (key IN
	 * ('lfs/ingestion/large-file/chunk/size', 'lfs/ingestion/topics', 'report/kibana/version'))"
	 * </dl>
	 * 
	 * @param filter
	 *            the filter, as a Map with String keys and values
	 * @param keys
	 *            an optional Set of keys for the IN operator
	 * @return a SQL command-style filter
	 */
	public static String generateSQLFilter(Map<String, String> filter, Set<String> keys) {
		String sql = "";
		if (filter != null && !filter.isEmpty()) {
			sql += " WHERE ";
			for (Map.Entry<String, String> entry : filter.entrySet()) {
				sql += "(" + entry.getKey() + " = '" + entry.getValue() + "') AND ";
			}
			sql = sql.substring(0, sql.length() - " AND ".length());
		}

		if (keys != null && !keys.isEmpty()) {
			sql = filter == null || filter.isEmpty() ? sql + " WHERE " : sql + " AND ";
			sql += "key IN (";
			for (String key : keys) {
				sql += "'" + key + "', ";
			}
			sql = sql.substring(0, sql.length() - ", ".length()) + ")";
		}
		return sql;
	}

	/**
	 * Returns the name of the database table.
	 * 
	 * @return the name of the database table
	 */
	public static String getTable() {
		return table;
	}

	/**
	 * Returns the size of the properties table.
	 * 
	 * @return the size of the properties table
	 */
	public static long getSize() {
		try {
			Statement statement = connection.createStatement();
			ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table + ";");
			result.next();
			return result.getInt(1);
		} catch (SQLException e) {
			exit(e);
		}
		return -1;
	}

	/**
	 * Gets all distinct instances of the specified field name.
	 * 
	 * @param field
	 *            the field name
	 * @param filter
	 *            the query filter
	 * @return a Set of all distinct instances of the field name
	 */
	public static Set<String> getDistinct(String field, Map<String, String> filter) {
		Set<String> distinct = new LinkedHashSet<>();
		try {
			String sql = "SELECT DISTINCT " + field + " FROM " + table + generateSQLFilter(filter, null);
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			Iterator<Map<String, String>> distinctMaps = parseResultSet(rs).iterator();
			while (distinctMaps.hasNext()) {
				distinct.add(distinctMaps.next().get(field));
			}
		} catch (SQLException e) {
			exit(e);
		}
		return distinct;
	}

	/**
	 * Clears all rows from the database table.
	 * 
	 * @return the number of properties cleared from the database
	 */
	public static long clear() {
		long size = -1;
		try {
			size = getSize();
			Statement statement = connection.createStatement();
			statement.execute("DELETE FROM " + table + ";");
		} catch (SQLException e) {
			exit(e);
		}
		return size;
	}

	/**
	 * Extracts data from the database.
	 * 
	 * @param sql
	 *            the full SQLite command
	 * @return the extracted data as a List of Maps, each of which represent a single property, or
	 *         row within the SQL table
	 */
	public static List<Map<String, String>> select(String sql) {
		try {
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			return parseResultSet(rs);
		} catch (SQLException e) {
			exit(e);
		}
		return null;
	}

	/**
	 * Inserts new data into a database.
	 * 
	 * @param property
	 *            a Map representing the property as a series of key-value pairs (e.g. "environment"
	 *            : "RWC-Dev", "key" : "some key")
	 */
	public static void insert(Map<String, String> property) {
		try {
			String sql = "INSERT INTO " + table + generateSQLSet(property.keySet(), true, false) + " VALUES "
					+ generateSQLSet(property.keySet(), true, true) + ";";
			PreparedStatement ps = connection.prepareStatement(sql);
			int i = 1;
			for (String value : property.values()) {
				ps.setString(i++, value);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			exit(e);
		}
	}

	/**
	 * Updates data in a database.
	 * 
	 * @param updated
	 *            a Map of fields to be updated
	 * @param filter
	 *            a Map of fields with which to filter the updates
	 * @param keys
	 *            a Set containing the keys of each property to be updated
	 */
	public static void update(Map<String, String> updated, Map<String, String> filter, Set<String> keys) {
		if (updated == null || updated.isEmpty()) {
			return;
		}

		// generates the appropriate SQL command
		String sql = "";
		sql = "UPDATE " + table + " SET ";
		for (String key : updated.keySet()) {
			sql += key + " = ? , ";
		}
		sql = sql.substring(0, sql.length() - " , ".length());

		if (filter != null && !filter.isEmpty()) {
			sql += " WHERE ";
			for (String key : filter.keySet()) {
				sql += "(" + key + " = ?) AND ";
			}
			sql = sql.substring(0, sql.length() - " AND ".length());
		}

		if (keys != null && !keys.isEmpty()) {
			sql = filter == null || filter.isEmpty() ? sql + " WHERE " : sql + " AND ";
			sql += "key IN (";
			for (int i = 0; i < keys.size() - 1; i++) {
				sql += "?,";
			}
			sql += "?)";
		}
		sql += ";";

		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			int i = 1;
			for (String val : updated.values()) {
				ps.setString(i++, val);
			}
			if (filter != null && !filter.isEmpty()) {
				for (String val : filter.values()) {
					ps.setString(i++, val);
				}
			}
			if (keys != null && !keys.isEmpty()) {
				for (String key : keys) {
					ps.setString(i++, key);
				}
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			exit(e);
		}
	}

	/**
	 * Deletes data from a database.
	 * 
	 * @param filter
	 *            a Map containing the filtered fields (e.g. environment, fabric)
	 */
	public static void delete(Map<String, String> filter) {
		String sql = "";
		sql = "DELETE FROM " + table + " WHERE ";
		for (String key : filter.keySet()) {
			sql += "(" + key + " = ?) AND ";
		}
		sql = sql.substring(0, sql.length() - " AND ".length()) + ";";
		try {
			PreparedStatement ps = connection.prepareStatement(sql);
			int i = 1;
			for (String value : filter.values()) {
				ps.setString(i++, value);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			exit(e);
		}
	}

	/**
	 * Batch inserts new data into a database. Significantly more efficient for groups of documents
	 * than individual insertion.
	 * 
	 * @param properties
	 *            a Collection of properties (represented by the standard Map)
	 */
	public static void insertBatch(Collection<Map<String, String>> properties) {
		if (properties == null || properties.isEmpty()) {
			return;
		}

		try {
			connection.setAutoCommit(false);
			PreparedStatement ps = null;

			Iterator<Map<String, String>> iter = properties.iterator();
			while (iter.hasNext()) {
				Map<String, String> property = iter.next();
				if (ps == null) {
					String sql = "INSERT INTO " + table + generateSQLSet(property.keySet(), true, false) + " VALUES "
							+ generateSQLSet(property.keySet(), true, true) + ";";
					ps = connection.prepareStatement(sql);
				}
				int i = 1;
				for (String value : property.values()) {
					ps.setString(i++, value);
				}
				ps.addBatch();

			}
			ps.executeBatch();

			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			exit(e);
		}
	}

	/**
	 * Batch deletes data from a database. Significantly more efficient for groups of documents than
	 * individual deletion.
	 * 
	 * @param filters
	 *            a {@link java.util.Collection} of filters (represented by the standard Map)
	 */
	public static void deleteBatch(Collection<Map<String, String>> filters) {
		if (filters == null || filters.isEmpty()) {
			return;
		}

		try {
			connection.setAutoCommit(false);
			PreparedStatement ps = null;

			Iterator<Map<String, String>> iter = filters.iterator();
			while (iter.hasNext()) {
				Map<String, String> filter = iter.next();
				if (ps == null) {
					String sql = "DELETE FROM " + table + " WHERE ";
					for (String key : filter.keySet()) {
						sql += "(" + key + " = ?) AND ";
					}
					sql = sql.substring(0, sql.length() - " AND ".length()) + ";";
					ps = connection.prepareStatement(sql);
				}
				int i = 1;
				for (String value : filter.values()) {
					ps.setString(i++, value);
				}
				ps.addBatch();

			}
			ps.executeBatch();

			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			exit(e);
		}
	}

	/**
	 * Reformats the ResultSet SQLite returns in response to SQL commands as Maps with String keys
	 * and values, to maintain a standard data structure for properties throughout the project.
	 * 
	 * @param rs
	 *            the ResultSet SQLite returns in response to SQL commands
	 * @return a {@link java.util.List} of Maps with String keys and values
	 */
	private static List<Map<String, String>> parseResultSet(ResultSet rs) {
		LinkedList<Map<String, String>> result = new LinkedList<>();
		try {
			Set<String> metadata = new LinkedHashSet<>();
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				metadata.add(rs.getMetaData().getColumnName(i));
			}
			while (rs.next()) {
				Map<String, String> row = new LinkedHashMap<>();
				for (String field : metadata) {
					row.put(field, rs.getString(field));
				}
				result.add(row);
			}
		} catch (SQLException e) {
			exit(e);
		}
		return result;
	}

	/**
	 * Terminates the JVM upon a SQLException. Useful for quickly modifying the behavior of any
	 * SQLiteManager function during SQLExceptions.
	 * 
	 * @param e
	 *            the SQLException
	 */
	private static void exit(SQLException e) {
		System.err.println("[DATABASE ERROR] A database access error occurred. Exiting with error code 1.");
		System.exit(1);
	}

}