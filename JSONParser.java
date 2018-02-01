package ...;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;


public class JSONParser {
	// Converting XML to JSON
	private static JSONObject convert(String xml) {
		if(xml == null || xml.isEmpty()) {
			return null;
		}
		return XML.toJSONObject(xml);
	}

	public static JSONObject convert(NodeList nodeList) throws TransformerException
	{
		String xml = nodeListToString(nodeList);
		return convert(xml);
	}

	private static String nodeListToString(NodeList nodes) throws TransformerException
	{
		DOMSource source = new DOMSource();
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

		for (int i = 0; i < nodes.getLength(); ++i) {
			source.setNode(nodes.item(i));
			transformer.transform(source, result);
		}
		return writer.toString();
	}

	public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {
		JSONObject mergedJSON = new JSONObject();
		if(json1.length()<=0 && json2.length()<=0){

		}else if(json1.length()<=0)
		{
			mergedJSON = json2;
		}else	if(json2.length()<=0)
		{
			mergedJSON = json1;
		}else{
			try {
				mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
				for (String crunchifyKey : JSONObject.getNames(json2)) {
					mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
				}

			} catch (JSONException e) {
				throw new RuntimeException("JSON Exception" + e);
			}
		}

		return mergedJSON;
	}

	public static JSONObject createJSON(NodeList nodeList)
	{
		JSONObject object = null;
		if(null != nodeList && nodeList.getLength() > 0){
			try
			{
				object = JSONParser.convert(nodeList);
			}
			catch (TransformerException e)
			{
				e.printStackTrace();
			}
		}
		return object;
	}

}