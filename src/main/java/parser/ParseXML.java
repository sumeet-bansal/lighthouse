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
			if (doc.hasChildNodes()) {
				parseXML(doc.getChildNodes());
			}
		} catch (IOException e) {
			System.err.println("error: file must be in .xml format.");
			error = true;
		}catch (ParserConfigurationException | SAXException e) {
			System.err.println("error: library exception, please check xml for formatting errors.");
			error = true;
		}
	}
	
	/**
	 * A Recursive method that inserts keys and values into a Map.
	 * @param nodeList the NodeList from the File to be standardized
	 */
	public void parseXML(NodeList nodeList) {
		ArrayList<String> tag = new ArrayList<String>();
		for (int count = 0; count < nodeList.getLength(); count++) {
			Node tempNode = nodeList.item(count);
			// make sure it's element node.
			if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
				// get node name to add to the parent node list for naming the keys
				if ((((Element) tempNode).getAttribute("name")) != "") {
					tag.add(tempNode.getNodeName()+"."+((Element) tempNode).getAttribute("name"));
				} else {
					tag.add(tempNode.getNodeName());
				}
				String parents = "";
				for (int i = 0; i < tag.size(); i++) {
					parents += tag.get(i) + ".";
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
					parseXML(tempNode.getChildNodes());
				}
				tag.remove(tag.size() - 1);
			}
		}
	}

}