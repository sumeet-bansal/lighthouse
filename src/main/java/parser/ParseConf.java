package parser;

import java.io.*;

/**
 * Standardizes .config files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-27
 */
public class ParseConf extends AbstractParser {

	/**
	 * Standardizes input File into a Map of keys and values.
	 * @param input the File to be standardized
	 */
	public void standardize(File input) {
		
		try {
			
			FileReader fr = new FileReader(input);
			BufferedReader br = new BufferedReader(fr);
			
			String str;
			String grouping = "";
			
			while ((str = br.readLine()) != null) {
				if ((str.charAt(0) == '[')) {
					grouping = str.substring(str.indexOf('[') + 1,
											 str.indexOf(']'));
				} else {
					
					// finds last non-space character of key
					int keyplace = str.indexOf('=');
					while (str.charAt(keyplace-1) == ' ') {
						keyplace--;
					}
					
					// finds first non-space character of value
					int valplace = str.indexOf('=');
					while (str.charAt(valplace+1) == ' ') {
						valplace++;
					}
					
					String key = grouping + "." + str.substring(0, keyplace);
					String val;
					if (str.substring(valplace+1).indexOf(',') != -1) {
						val = "[" + str.substring(valplace+1) + "]";
					} else {
						val = str.substring(valplace+1);
					}
					data.put(key, val);

				}
			}
			
			br.close();
			
		} catch (IOException e) {
			System.err.println("error: file must be in .config format.");
			error = true;
		}
	}

}