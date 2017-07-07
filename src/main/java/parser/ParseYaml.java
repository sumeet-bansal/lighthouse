package parser;

import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Standardizes .yaml files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-27
 */
public class ParseYaml extends AbstractParser {
	
	/**
	 * Standardizes input File into separate ArrayLists for keys and values.
	 * @param input File to be standardized
	 */
	public void standardize(File input) {
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {

			// converts YAML file to LinkedHashMap
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, ArrayList<String>> map = mapper.readValue(input, LinkedHashMap.class);

			// stores HashMap data in separate ArrayLists for keys and values
			for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
				keys.add(entry.getKey());
				vals.add(entry.getValue());
			}
		} catch (Exception e) {
			System.err.println("error: file must be in .yaml format.");
			error = true;
		}
	}

}