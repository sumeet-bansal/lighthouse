package parser;

import java.io.*;
import java.util.*;

/**
 * Standardizes hosts files
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class ParseHost extends AbstractParser {

	/**
	 * Standardizes input File into a Map of keys and values.
	 * @param input the File to be standardized
	 */
	public void standardize(File input) {
		
		try {
			
			FileReader fr = new FileReader(input);
			BufferedReader br = new BufferedReader(fr);
			
			String line = new String();
			while ((line = br.readLine()) != null) {
				
				// ignores comments and blank lines
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				
				// takes all keys and values as non-space blocks of characters
				ArrayList<String> charBlocks = new ArrayList<>();
				for (String str : line.split(" ")) {
					if (str.length() > 0) {
						charBlocks.add(str);
					}
				}
				
				// first non-space block must be value, rest must be keys
				String val = charBlocks.remove(0);
				for (String key : charBlocks) {
					data.put(key, val);
				}
				
			}
			fr.close();
			
		} catch (IOException e) {
			error = true;
		}

	}
}