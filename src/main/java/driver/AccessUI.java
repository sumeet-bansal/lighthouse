package driver;

import java.io.File;
import java.util.ArrayList;

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
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Invalid input! Regular syntax is as follows:\n"
					+ "<files to query> block <files to block within query>\n"
					+ "please note: blocking files within query is optional");
			System.out.println();
			e.printStackTrace();
		}
	}
}