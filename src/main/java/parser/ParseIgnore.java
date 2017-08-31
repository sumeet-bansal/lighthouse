package parser;

import java.io.*;

/**
 * Standardizes hosts files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ParseIgnore extends AbstractParser {

	/**
	 * Standardizes input File into a Map of keys and values.
	 * 
	 * @param input
	 *            the File to be standardized
	 */
	public void standardize(File input) {

		try {
			String line = new String();
			BufferedReader br = new BufferedReader(new FileReader(input));
			while ((line = br.readLine()) != null) {

				// ignores comments and blank lines
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}

				data.put(line, true);

			}
			br.close();

		} catch (IOException e) {
			String name = input.getAbsolutePath();
			System.err
					.println("\n[DATABASE ERROR] " + name + " is not in correct .ignore format.\n");
			error = true;
		}

	}
}