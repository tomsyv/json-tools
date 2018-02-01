package ...;

import com.sun.org.apache.xerces.internal.dom.DeepNodeListImpl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.json.Json.createObjectBuilder;

public class JSONConverter
{

	public static final String EMPTY_NODE = "#text";
	private static String AMOUNT_REGEX = ".*Amount.*|.*Price.*";
	private static String INTEGER_REGEX = ".*Seq.*|.*Qty.*";

	public static JsonObject convertXMLToJson(NodeList nodeList, Set nodesToIgnore)
	{
		JsonObjectBuilder mainBuilder = createObjectBuilder();
		if(nodeList!=null){
			for (int i = 0; i < nodeList.getLength(); i++)
			{
				if(!nodeList.item(i).getNodeName().equals(EMPTY_NODE)){
					if(nodeList.getLength()>1){
						mainBuilder = buildJsonObjectFromNodeList(nodeList, nodesToIgnore);
					}else{
						if(nodeList.item(i).getChildNodes().getLength()>1){
							mainBuilder.add(nodeList.item(i).getNodeName(), buildJsonObjectFromNodeList(nodeList.item(i).getChildNodes(), nodesToIgnore).build());
						}else{
							addValueToBuilder(mainBuilder, nodeList.item(i), nodeList.item(i).getNodeName());
						}
					}
				}
			}
		}
		return mainBuilder.build();
	}

	public static JsonObject convertXMLToJson(Node node, Set nodesToIgnore)
	{
		JsonObjectBuilder mainBuilder = createObjectBuilder();
		populateBuilder(node, nodesToIgnore, mainBuilder);

		return mainBuilder.build();
	}

	protected static void populateBuilder(Node node, Set nodesToIgnore, JsonObjectBuilder mainBuilder)
	{
		if(node!=null){
			NodeList childNodes = node.getChildNodes();
			if(childNodes.getLength()>1)
			{
				JsonObjectBuilder childObjectBuilder = buildJsonObjectFromNodeList(childNodes, nodesToIgnore);
				mainBuilder.add(node.getNodeName(), childObjectBuilder.build());
			}
			else if(!node.getTextContent().trim().equals("")){
				addValueToBuilder(mainBuilder, node, node.getNodeName());
			}

		}
	}

	public static JsonObjectBuilder convertXMLToJsonBuilder(NodeList nodeList, Set nodesToIgnore)
	{
		JsonObjectBuilder mainBuilder = createObjectBuilder();
		if(nodeList!=null){
			for (int i = 0; i < nodeList.getLength(); i++){
				if(!nodeList.item(i).getNodeName().equals(EMPTY_NODE)){
					if(nodeList.getLength()>1){
						mainBuilder = buildJsonObjectFromNodeList(nodeList, nodesToIgnore);
					}else{
						if (nodeList.item(i).getChildNodes().getLength() > 1){
							mainBuilder.add(nodeList.item(i).getNodeName(), buildJsonObjectFromNodeList(nodeList.item(i).getChildNodes(), nodesToIgnore));
						}
						else{
							addValueToBuilder(mainBuilder, nodeList.item(i), nodeList.item(i).getNodeName());
						}
					}
				}
			}
		}
		return mainBuilder;
	}

	public static JsonObjectBuilder convertXMLToJsonBuilder(Node node, Set nodesToIgnore)
	{
		JsonObjectBuilder mainBuilder = createObjectBuilder();
		populateBuilder(node, nodesToIgnore, mainBuilder);
		return mainBuilder;
	}

	private static JsonObjectBuilder buildJsonObjectFromNodeList(NodeList nodeList, Set nodesToIgnore)
	{
		JsonObjectBuilder childObjectBuilder = createObjectBuilder();
		Set<String> alreadyAdded = new HashSet<>();
		for (int j = 0; j < nodeList.getLength(); j++)
		{
			Node item = nodeList.item(j);
			String nodeName = item.getNodeName();
			if((null!=nodesToIgnore && nodesToIgnore.contains(nodeName)) || alreadyAdded.contains(nodeName))
				continue;
			if(nodeList instanceof DeepNodeListImpl || nodeListHasMultipleEqualElements((Element) nodeList, nodeName)){
				createJsonArray(nodeList, nodesToIgnore, childObjectBuilder, alreadyAdded, nodeName);
			}else{
				addValuesOrTraverseNode(nodesToIgnore, childObjectBuilder, item, nodeName);
			}
		}
		return childObjectBuilder;
	}

	private static void addValuesOrTraverseNode(Set nodesToIgnore, JsonObjectBuilder childObjectBuilder, Node item, String nodeName)
	{
		if(item.getChildNodes().getLength()>1){
			NodeList childNodes = item.getChildNodes();
			childObjectBuilder.add(nodeName, buildJsonObjectFromNodeList(childNodes, nodesToIgnore).build());
		}else if(item.getChildNodes().getLength()<=1){
				addValueToBuilder(childObjectBuilder, item, nodeName);
		}
	}

	private static void createJsonArray(NodeList nodeList, Set nodesToIgnore, JsonObjectBuilder childObjectBuilder, Set<String> alreadyAdded, String nodeName)
	{
		List<Node> listOfNodes = new ArrayList<>();
		for (int k = 0; k < nodeList.getLength(); k++)
		{
			if(nodeList.item(k).getNodeName().equals(nodeName)){
				listOfNodes.add(nodeList.item(k));
			}
		}
		childObjectBuilder.add(nodeName, buildJsonArray(listOfNodes, nodesToIgnore).build());
		alreadyAdded.add(nodeName);
	}

	private static boolean nodeListHasMultipleEqualElements(Element nodeElement, String nodeName)
	{
		return nodeElement.getElementsByTagName(nodeName).getLength()>1;
	}


	private static void addValueToBuilder(JsonObjectBuilder objectBuilder, Node item, String nodeName)
	{
		if(!item.getTextContent().trim().equals("")){
			if(nodeName.matches(AMOUNT_REGEX) || nodeName.matches(INTEGER_REGEX)){
				addNumericToBuilder(objectBuilder, item, nodeName);
			}else{
				objectBuilder.add(nodeName, item.getTextContent());
			}
		}
	}

	private static void addNumericToBuilder(JsonObjectBuilder childObjectBuilder, Node item, String nodeName)
	{
		try{
			parseNumericValuesAndAddToBuilder(childObjectBuilder, item, nodeName, nodeName.matches(AMOUNT_REGEX));
		}catch (NumberFormatException e){
			childObjectBuilder.add(nodeName, item.getTextContent());
		}
	}
	private static void parseNumericValuesAndAddToBuilder(JsonObjectBuilder childObjectBuilder, Node item, String nodeName, boolean matches)
	{
		if (matches){
			BigDecimal decimalValue = new BigDecimal(item.getTextContent());
			childObjectBuilder.add(nodeName, decimalValue.setScale(2, BigDecimal.ROUND_HALF_UP));
		}else{
			childObjectBuilder.add(nodeName, Integer.parseInt(item.getTextContent()));
		}
	}


	private static JsonArrayBuilder buildJsonArray(List nodeList, Set nodesToIgnore)
	{
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (int k = 0; k < nodeList.size(); k++)
		{
			Node node = (Node) nodeList.get(k);
			NodeList childNodes = node.getChildNodes();
			JsonObjectBuilder arrayObjectBuilder = buildJsonObjectFromNodeList(childNodes, nodesToIgnore);
			arrayBuilder.add(arrayObjectBuilder);
		}
		return arrayBuilder;
	}
}
