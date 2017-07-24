package driver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import cachingLayer.Comparator;

public class AccessUI {

	/**
	 * Passes command-line arguments to query the database, compares files specified
	 * by query, and writes a CSV file to C:/test diffs (will change later). Syntax
	 * is as follows: <root directory (optional, any level)> <files to query
	 * (include root directory if not specified)> block (optional) <files to block
	 * (include root directory if not specified)>
	 * 
	 * TODO Configure UI to pass command line arguments to driver classes
	 * 
	 * @param args
	 */
	public static void run(String[] args) {

		// disable mongo logging
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
		rootLogger.setLevel(Level.OFF);

		try {
			ArrayList<String> list = new ArrayList<String>();
			ArrayList<String> addList = new ArrayList<String>();
			ArrayList<String> blockList = new ArrayList<String>();

			// copy command line arguments to arrayLists

			for (String str : args) {
				list.add(str);
			}

			int r = -1;
			if (list.contains("block")) {
				r = list.indexOf("block");
				if (r < 2) {
					System.err.println(
							"Invalid input, please specify at least one full query to add before removing files from query");
					return;
				}
				for (int i = r + 1; i < list.size(); i++) {
					blockList.add(list.get(i));
				}
				for (int i = 0; i < r; i++) {
					addList.add(list.get(i));
				}
			} else {
				for (String str : list) {
					addList.add(str);
				}
			}

			// check if args list is odd or even and append terms with root folder
			// accordingly
			if (!(addList.size() % 2 == 0)) {
				String root = addList.get(0) + "/";
				addList.remove(0);
				for (int i = 0; i < addList.size(); i++) {
					addList.set(i, root + addList.get(i));
				}
				for (int i = 0; i < blockList.size(); i++) {
					blockList.set(i, root + blockList.get(i));
				}
			}

			// add groups of 2 paths given by the order of args
			Comparator c = new Comparator();
			for (int i = 0; i < addList.size(); i += 2) {
				c.addQuery(addList.get(i), addList.get(i + 1));
			}
			if (r != -1) {
				for (int i = 0; i < blockList.size(); i++) {
					c.blockQuery(blockList.get(i));
				}
			}

			// compare files, build CSV file and clear current query
			c.compare();
			String writePath = System.getProperty("user.home") + File.separator + "Documents" + File.separator
					+ "Diagnostic reports";
			new File(writePath).mkdirs();
			c.writeToCSV(writePath);
			c.clearQuery();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Please specify files to query");
		} catch (Exception e) {
			System.err.println("Invalid input! Regular syntax is as follows:\n"
					+ "<root directory> <files to query> block <files to block within query>\n"
					+ "please note: specifying root directory and blocking files within query are optional");
			System.out.println();
			e.printStackTrace();
		}
	}
}