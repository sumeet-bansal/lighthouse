package zookeeperModule;

import java.util.*;
import java.io.*;

/**
 * Generates .properties files for all fabrics in an environment.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ZKGenerator {
	
	private ZKClientManager zkmanager = new ZKClientManager();
	private String basepath = "/alcatrazproperties/2.5", fabric;
	private Map<String, String> map;
	
	/**
	 * Runs the .properties file generator.
	 */
	public void generate() {
		
		// gets all fabrics in environment
		List<String> fabrics = zkmanager.getZNodeChildren(basepath);
		
		for (int i = 0; i < fabrics.size(); i++) {
			
			// generates directory structure
			new File("environment/" + fabrics.get(i) + "/common").mkdirs();
			
			// generates .properties files
			map = new LinkedHashMap<>();
			fabric =  fabrics.get(i);
			recursive(basepath + "/" + fabric);
			write(fabric, map);
			System.out.println("generated .properties file for " + fabric);
		}
	}	
	
	/**
	 * Recursively traverses ZNodes to generate full path keys.
	 * @param path the path of the current ZNode
	 */
	private void recursive(String path) {
		
		// base case: current ZNode is leaf
		if (zkmanager.getZNodeStats(path).getNumChildren() == 0) {
			String key = path.substring(basepath.length()+1);
			String data = (String) zkmanager.getZNodeData(path, false);
			map.put(key.substring(fabric.length() + 1), data);
			return;
		}
		
		// recursive case: iterates through all children
		List<String> children = zkmanager.getZNodeChildren(path);
		for (int i = 0; i < children.size(); i++) {
			recursive(path + "/" + children.get(i));
		}
	}
	
	/**
	 * Writes fabric data to .properties files.
	 * @param fabric the current fabric
	 * @param map a Map of root-to-leaf keys and respective data from fabric
	 */
	private void write(String server, Map<String, String> map) {
		try {
			String directory = "environment/" + server + "/common/";
			
			// 'blacklist' branch gets separate .properties for UI readability
			ArrayList<String> blacklist = new ArrayList<>();
			
			Writer writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(directory + "server.properties"),
			          "utf-8"));
			for (Map.Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (entry.getKey().startsWith("blacklist")) {
				    blacklist.add(key + "=" + value + "\n");
				} else {
					writer.write(key + "=" + value + "\n");
				}
			}
		    writer.close();
			
			if (blacklist.size() > 0) {
				writer = new BufferedWriter(new OutputStreamWriter(
				          new FileOutputStream(directory +
				          "server.blacklist.properties"), "utf-8"));
				for (int i = 0; i < blacklist.size(); i++) {
					writer.write(blacklist.get(i));
				}
				writer.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
