package driver;

import java.sql.*;
import java.util.*;

import databaseModule.DbFunctions;

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
	 * Private helper method. Given path inputs, verifies the validity of the inputs and generates
	 * filters for the inputs.
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
		while (path.indexOf("\\") != -1) {
			path = path.replace("\\", "/");
		}
		while (path.indexOf("//") != -1) {
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

	public static Set<String> getDistinct(String field) {
		Set<String> distinct = new LinkedHashSet<>();
		try {
			String sql = "SELECT DISTINCT " + field + " FROM " + table;
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

	private static void exit(SQLException e) {
		e.printStackTrace();
		System.err.println("[DATABASE ERROR] A database access error occurred. Exiting with error code 1.");
		System.exit(1);
	}

	/**
	 * Clears all rows from database table.
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

	public static String getTable() {
		return table;
	}

	public static List<Map<String, String>> parseResultSet(ResultSet rs) {
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
	 * Updates data in a database.
	 * 
	 * @param updated
	 *            a Map<String, String> of fields to be updated
	 * @param filter
	 *            a Map<String, String> of fields with which to filter the updates
	 * @param keys
	 *            a Set containing the keys of each property to be updated
	 */
	public static void update(Map<String, String> updated, Map<String, String> filter, Set<String> keys) {
		if (updated == null || updated.isEmpty()) {
			return;
		}

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
	 * Inserts new data into a database.
	 * 
	 * @param property
	 *            a Map<String, String> representing the property as a series of key-value pairs
	 *            (e.g. "environment" : "RWC-Dev", "key" : "some key")
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
	 * Deletes data from a database.
	 * 
	 * @param filter
	 *            a Map<String, String> containing the filtered fields (e.g. environment, fabric)
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
	 * Given a {@link java.util.Collection} of Strings, generates a SQL-compatible set.
	 * 
	 * @param collection
	 *            a Collection of Strings
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
	 * Temporary testbed to verify all SQLiteManager methods working as intended.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {
		// connects to database
		System.out.println("[DATABASE MESSAGE] Connecting to database...");
		connectToDatabase();
		System.out.println("[DATABASE MESSAGE] Database connection successful.\n");

		// tests insertion
		Map<String, String> prop;
		prop = new LinkedHashMap<>();
		prop.put("key", "testk");
		prop.put("value", "testv");
		prop.put("filename", "sth2.prop");
		prop.put("node", "h2");
		prop.put("fabric", "storm");
		prop.put("environment", "Redwood-City");
		prop.put("path", prop.get("environment") + "/" + prop.get("fabric") + "/" + prop.get("node") + "/"
				+ prop.get("filename"));
		prop.put("extension", "properties");
		prop.put("ignore", "false");
		insert(prop);

		prop.put("key", "port");
		prop.put("value", "8080");
		prop.put("filename", "esh3.prop");
		prop.put("node", "h3");
		prop.put("fabric", "elastic");
		prop.put("environment", "developer1");
		prop.put("path", prop.get("environment") + "/" + prop.get("fabric") + "/" + prop.get("node") + "/"
				+ prop.get("filename"));
		prop.put("extension", "prop");
		prop.put("ignore", "false");
		insert(prop);

		prop.put("key", "mongo");
		prop.put("value", "db");
		prop.put("filename", "zkh1.prop");
		prop.put("node", "h1");
		prop.put("fabric", "zkepler");
		prop.put("environment", "developer8");
		prop.put("path", prop.get("environment") + "/" + prop.get("fabric") + "/" + prop.get("node") + "/"
				+ prop.get("filename"));
		prop.put("extension", "prop");
		prop.put("ignore", "false");
		insert(prop);

		List<Map<String, String>> res;
		String sql = "";

		// retrieves all rows
		sql = "SELECT * FROM properties;";
		res = select(sql);
		print(res);

		// retrieves key, value, and ignore rows from Redwood-City
		sql = "SELECT key, value, ignore FROM properties WHERE environment = 'Redwood-City';";
		res = select(sql);
		print(res);

		// checks count
		System.out.println("\nCount: " + getSize() + " rows of properties.");

		// deletes a row
		Map<String, String> d = new LinkedHashMap<>();
		d.put("environment", "Redwood-City");
		d.put("fabric", "storm");
		delete(d);
		System.out.println("Deleted rows with 'Redwood-City' environment, 'storm' fabric.\n");

		// updates row's 'ignore' field to true
		Map<String, String> u = new LinkedHashMap<>();
		u.put("ignore", "true");
		Map<String, String> f = new LinkedHashMap<>();
		f.put("environment", "developer1");
		Set<String> p = new HashSet<>();
		p.add("port");
		p.add("mongo");
		update(u, f, p);

		// retrieves all rows
		sql = "SELECT * FROM properties;";
		res = select(sql);
		print(res);

		// tests DbFunctions#populate
		System.out.println("Count: " + getSize() + " rows of properties.");
		System.out.println("Cleared " + clear() + " rows. Remaining: " + getSize());
		DbFunctions.populate(System.getProperty("user.home") + "/workspace/lighthouse/root");
		res = select("SELECT * FROM properties");
		print(res);
		System.out.println("\nCount: " + getSize() + " rows of properties.\n");

		res = select("SELECT * FROM properties WHERE key LIKE '%lfs/ingestion%'");
		print(res);

		res = select("SELECT * FROM properties WHERE ignore = 'true'");
		print(res);

		res = select("SELECT * FROM properties WHERE key = 'report/port'");
		print(res);
	}

	private static void print(List<Map<String, String>> res) {
		Iterator<Map<String, String>> iter = res.iterator();
		System.out.println();
		while (iter.hasNext()) {
			Map<String, String> property = iter.next();
			for (String value : property.values()) {
				System.out.print(value + "\t\t");
			}
			System.out.println();
		}
		System.out.println();
	}
}