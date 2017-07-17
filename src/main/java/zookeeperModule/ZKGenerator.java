package zookeeperModule;

import java.util.*;
import java.io.*;

public class ZKGenerator {
	
	private static ZKClientManager zkmanager = new ZKClientManager();
	private static String basepath = "/alcatrazproperties/2.5/storm";
	
	public static void main(String[] args) {
		recursive(basepath);
	}	
	
	private static void recursive(String path) {
		if (zkmanager.getZNodeStats(path).getNumChildren() == 0) {
			System.out.println(path.replace("/", ".").substring(basepath.length() + 1)
					+ "=" + (String) zkmanager.getZNodeData(path, false));
		}
		List<String> children = zkmanager.getZNodeChildren(path);
		for (int i = 0; i < children.size(); i++) {
			recursive(path + "/" + children.get(i));
		}
	}
	
}
