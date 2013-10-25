package org.dasein.cloud.test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: sdorzak
 * Date: 06/09/2013
 * Time: 14:04
 * To change this template use File | Settings | File Templates.
 */
public class TestHelpers {
    public static Document getMockXmlResponse(String responsePath) {
        InputStream input = AzureProviderTest.class.getResourceAsStream(responsePath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = null;
        Document document = null;
        try {
            parser = factory.newDocumentBuilder();
            document = parser.parse(input);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return document;
    }

    public static String getStringXmlRequest(String requestPath)    {

        BufferedReader bufferedReader = null;
        InputStream input = AzureProviderTest.class.getResourceAsStream(requestPath);

        StringBuffer stringBuffer = new StringBuffer();
        String line = null;

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            while((line =bufferedReader.readLine())!=null){

                stringBuffer.append(line).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return stringBuffer.toString();
    }

    public static String getStringXmlRequestAsSingleLine(String requestPath){
        return getStringXmlRequest(requestPath).replaceAll("\n|\r", "");
    }
}
