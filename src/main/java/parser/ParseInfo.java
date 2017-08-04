package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Standardizes .info files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ParseInfo extends AbstractParser {
	
	/**
	 * Standardizes input File into a Map of keys and values.
	 * @param input the File to be standardized
	 */
	public void standardize(File input) {
		String name = input.getAbsolutePath();

		try {
			FileReader fr = new FileReader(input);
			BufferedReader br = new BufferedReader(fr);

			int num = 0;
			String str;
			while ((str = br.readLine()) != null) {
				num++;
				if (str.length() == 0 || str.charAt(0) == ';') {
					continue;
				}

				String[] arr = str.split("=");

				if (arr.length != 2 && str.charAt(str.length() - 1) != '=') {
					System.out.println("\n[DATABASE ERROR] " + name + ": line " + num
							+ " was not parsed due to incorrect format.\n");
					continue;
				}

				String key = arr[0];
				Object val;
				if (arr.length > 1) {
					val = arr[1];
				} else {
					val = "";
				}
				data.put(key, val);
			}

			br.close();
		} catch (IOException e) {
			System.err.println("\n[DATABASE ERROR] " + name + " is not in correct .info format.\n");
			error = true;
		}

	}
}
