package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Standardizes .whitelist and .blacklist files
 * 
 * @author PKelaita
 * @version 1.0
 */
public class ParseList extends AbstractParser {

	/**
	 * Standardizes input File into a Map of keys and values.
	 * 
	 * @param input
	 *            the File to be standardized
	 */
	public void standardize(File input) {
		String name = input.getAbsolutePath();

		try {
			FileReader fr = new FileReader(input);
			BufferedReader br = new BufferedReader(fr);
			
			String key;
			while ((key = br.readLine()) != null) {
				if (key.length() == 0 || key.charAt(0) == '#') {
					continue;
				}
				
				data.put(key, "true");
			}

			br.close();
		} catch (IOException e) {
			System.err.println("\n[DATABASE ERROR] " + name + " is not in correct .whitelist/.blacklist format.\n");
			error = true;
		}

	}

}
