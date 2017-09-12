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
	 * 
	 * @throws SQLException
	 *             if a database access error occurs or the url is null
	 */
	public static void connectToDatabase() throws SQLException {
		DriverManager.setLoginTimeout(30);
		connection = DriverManager.getConnection("jdbc:sqlite:" + database);
		Statement statement = connection.createStatement();
		String sql = "CREATE TABLE IF NOT EXISTS properties (\n	key text,\n	value text,\n"
				+ "	filename text,\n	node text,\n	fabric text,\n	environment text,\n"
				+ "	path text,\n	extension text,\n	ignore text\n);";
		statement.execute(sql);
	}

	/**
	 * Clears all rows from database table.
	 * 
	 * @return the number of properties cleared from the database
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on a closed connection
	 */
	public static long clear() throws SQLException {
		long size = getSize();
		Statement statement = connection.createStatement();
		statement.execute("DELETE FROM " + table + ";");
		return size;
	}

	/**
	 * Getter method for the SQL connection.
	 * 
	 * @return the connection to the SQLite database
	 */
	public static Connection getConnection() {
		return connection;
	}

	/**
	 * Returns the size of the properties table.
	 * 
	 * @return the size of the properties table
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on a closed connection
	 */
	public static long getSize() throws SQLException {
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table + ";");
		result.next();
		return result.getInt(1);
	}

	/**
	 * Extracts data from the database.
	 * 
	 * @param sql
	 *            the full SQLite command
	 * @return the extracted data as a {@link java.sql.ResultSet}
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on a closed connection
	 */
	public static ResultSet select(String sql) throws SQLException {
		Statement statement = connection.createStatement();
		return statement.executeQuery(sql);
	}

	/**
	 * Updates data in a database.
	 * 
	 * @param sql
	 *            the full SQLite command
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on a closed connection
	 */
	public static void update(String sql) throws SQLException {
		Statement statement = connection.createStatement();
		statement.executeUpdate(sql);
	}

	/**
	 * Inserts new data into a database.
	 * 
	 * @param key
	 *            the key of the property being inserted
	 * @param value
	 *            the value of the property being inserted
	 * @param metadata
	 *            a Map<String, String> of the property metadata (i.e. environment, fabric, node,
	 *            filename, path, extension)
	 * @throws SQLException
	 *             if a database access error occurs or this method is called on a closed connection
	 */
	public static void insert(String key, String value, Map<String, String> metadata) throws SQLException {
		String sql = "";
		sql = "INSERT INTO " + table + "(key, value, " + generateSQLSet(metadata.keySet(), false) + ", ignore) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
		PreparedStatement ps = connection.prepareStatement(sql);
		int i = 1;
		ps.setString(i++, key);
		ps.setString(i++, value);
		for (String val : metadata.values()) {
			ps.setString(i++, val);
		}
		ps.setString(i++, "false");
		ps.executeUpdate();
	}

	/**
	 * Given a {@link java.util.Collection} of Strings, generates a SQL-compatible set.
	 * 
	 * @param collection
	 *            a Collection of Strings
	 * @param full
	 *            true if the collection contains all the properties of the intended set, else false
	 * @return the SQL-compatible set of properties
	 */
	public static String generateSQLSet(Collection<String> collection, boolean full) {
		String sql = "";
		if (collection == null || collection.size() == 0) {
			return sql;
		}
		Iterator<String> iter = collection.iterator();
		while (iter.hasNext()) {
			String item = iter.next();
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
		try {
			
			// connects to database
			System.out.println("[DATABASE MESSAGE] Connecting to database...");
			connectToDatabase();
			System.out.println("[DATABASE MESSAGE] Database connection successful.\n");
			
			// tests insertion
			Map<String, String> m;
			m = new LinkedHashMap<>();
			m.put("filename", "sth2.prop");
			m.put("node", "h2");
			m.put("fabric", "storm");
			m.put("environment", "Redwood-City");
			m.put("path", m.get("environment") + "/" + m.get("fabric") + "/" + m.get("node") + "/" + m.get("filename"));
			m.put("extension", "properties");
			insert("testk", "testv", m);

			m.put("filename", "esh3.prop");
			m.put("node", "h3");
			m.put("fabric", "elastic");
			m.put("environment", "developer1");
			m.put("path", m.get("environment") + "/" + m.get("fabric") + "/" + m.get("node") + "/" + m.get("filename"));
			m.put("extension", "prop");
			insert("port", "8080", m);
			
			ResultSet r = null;
			String sql = "";
			
			// retrieves all rows
			sql = "SELECT * FROM properties;";
			r = select(sql);
			for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
				System.out.print(r.getMetaData().getColumnName(i) + "\t\t");
			}
			System.out.println();
			while (r.next()) {
				for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
					System.out.print(r.getString(i) + "\t\t");
				}
				System.out.println();
			}
			System.out.println();
			
			// retrieves key, value, and ignore rows from Redwood-City
			sql = "SELECT key, value, ignore FROM properties WHERE environment = 'Redwood-City';";
			r = select(sql);
			for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
				System.out.print(r.getMetaData().getColumnName(i) + "\t\t");
			}
			System.out.println();
			while (r.next()) {
				for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
					System.out.print(r.getString(i) + "\t\t");
				}
				System.out.println();
			}
			
			// checks count and clears
			System.out.println("\nCount: " + getSize() + " rows of properties.");
			System.out.println("Cleared " + clear() + " rows. Remaining: " + getSize());
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("[DATABASE ERROR] A database access error occurred. Exiting with error code 1.");
			System.exit(1);
		}
	}
}