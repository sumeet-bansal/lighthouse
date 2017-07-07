package parser;

import java.io.*;
import java.util.*;

/**
 * Standardizes hosts files
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-30
 */
public class ParseHost extends AbstractParser {
	
	/**
	 * Standardizes input File into separate ArrayLists for keys and values.
	 * @param input File to be standardized
	 */
	public void standardize(File input) {
		
		ArrayList<String> tempKeys = new ArrayList<String>();
		ArrayList<String> tempVals = new ArrayList<String>();
		
		try {
			FileReader fr = new FileReader(input);
			BufferedReader br = new BufferedReader(fr);
			
			String line = new String();
			while ((line = br.readLine()) != null) {
				String key = new String();
				String val = new String();
				for (int i = 0; i < line.length()-1; i++) {

					// true if space, else false
					boolean curr = line.charAt(i) == ' ';
					boolean next = line.charAt(i + 1) == ' ';

					// if !curr && next, curr is the end of the substring
					// if curr && !next, next is the start of the substring
					if (!curr && next) {
						val = line.substring(0, i+1);
					} else if (curr && !next) {
						key = line.substring(i+1, line.length());
						break;
					}
				}
				
				if (line.length() > 0 && line.charAt(0) != '#') {
					tempKeys.add(key);
					tempVals.add(val);
				}
			}
			
			fr.close();
			
		} catch (IOException e) {
			error = true;
		}
		
		// split up keys with spaces and copy values
		ArrayList<String> tempVals2 = new ArrayList<>();
		for (String val : tempVals) {
			tempVals2.add(val);
		}
		for (int keyIndex = tempKeys.size() - 1; keyIndex >= 0; keyIndex--) {
			String line = tempKeys.get(keyIndex);
			int count = 0;
			for (int i = 0; i < line.length() - 1; i++) {
				boolean c1 = line.charAt(i) == ' ';
				boolean c2 = line.charAt(i + 1) == ' ';
				if (c1 && !c2) {
					count++;
				}
			}
			for (int j = 0; j < count; j++) {
				tempVals2.add(keyIndex + 1, (String) tempVals.get(keyIndex));
			}
			for (String key : line.split(" ")) {
				if (key.length() > 0) {
					keys.add(key);
				}
			}
		}
		for (Object val : tempVals2) {
			vals.add(val);
		}
		Collections.reverse(keys);

	}
}