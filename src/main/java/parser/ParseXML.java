package parser;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Standardizes hazelcast.xml files and cluster.xml files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-07-27
 */
public class ParseXML extends AbstractParser{
	
	/**
	 * Standardizes input files by calling the recursive method parseXML.
	 * @param input the File to be standardized 
	 */
	public void standardize(File input) {
		try {
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = dBuilder.parse(input);
			ArrayList<String> tag = new ArrayList<String>();
			doc.getDocumentElement().normalize();
			tag.add(doc.getDocumentElement().getNodeName());
			if (doc.hasChildNodes()) {
				parseXML(doc.getChildNodes(),tag);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * A Recursive method that inserts keys and values into a Map.
	 * @param nodeList the NodeList from the File to be standardized
	 * @param tag the ArrayList of all the parents for adding to the key names
	 */
	public void parseXML(NodeList nodeList,ArrayList<String> tag) {
		ArrayList<String> tags=tag;
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			// make sure it's element node.
			if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
				// get node name and value
				if ((((Element) tempNode).getAttribute("name")) != "") {
					tags.add(tempNode.getNodeName()+"."+((Element) tempNode).getAttribute("name"));
				} else {
					tags.add(tempNode.getNodeName());
				}
				String parents = "";
				for (int i = 0; i < tags.size(); i++) {
					parents += tags.get(i) + ".";
				}
				parents = parents.substring(0, parents.length() - 1);
				if (tempNode.hasAttributes()) {
					NamedNodeMap nodeMap = tempNode.getAttributes();
					for (int i = 0; i < nodeMap.getLength(); i++) {
						Node node = nodeMap.item(i);
						data.put(parents + "." + node.getNodeName(), node.getNodeValue());
					}
				}
				Node copyNode = tempNode;
				NodeList nodes = tempNode.getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					if(nodes.item(i).getChildNodes().getLength()<=0) {
						copyNode.removeChild(nodes.item(i));
						i--;
					}
				}
				NodeList nodes2 = copyNode.getChildNodes();
				for (int i = 0; i < nodes2.getLength(); i++) {
					if(!nodes2.item(i).getTextContent().contains("\n")) {
						data.put(parents + "." + nodes2.item(i).getNodeName(), nodes2.item(i).getTextContent());
					}
				}
				if (tempNode.hasChildNodes()) {
					parseXML(tempNode.getChildNodes(),tags);
				}
				tags.remove(tags.size() - 1);
			}
		}
	}

}