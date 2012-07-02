package org.dasein.cloud.azure;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: greese
 * Date: 5/18/12
 * Time: 7:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class AzureException extends CloudException {
    static public class ExceptionItems {
        public CloudErrorType type;
        public int code;
        public String message;
        public String details;
    }

    static public @Nullable ExceptionItems parseException(@Nonnegative int code, @Nullable String xml) {
        if( xml == null ) {
            return null;
        }
        ExceptionItems items = new ExceptionItems();
        
        items.code = code;
        items.type = CloudErrorType.GENERAL;

        Document doc;
        
        try {
            ByteArrayInputStream bas = new ByteArrayInputStream(xml.getBytes());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            
            doc = parser.parse(bas);
            bas.close();
        }
        catch( IOException e ) {
            return null;
        }
        catch( ParserConfigurationException e ) {
            return null;
        }
        catch( SAXException e ) {
            return null;
        }
        NodeList errors = doc.getElementsByTagName("Error");

        items.message = "Unknown";
        items.details = xml;
        if( errors.getLength() > 0 ) {
            Node error = errors.item(0);
            NodeList attributes = error.getChildNodes();

            for( int i=0; i<attributes.getLength(); i++ ) {
                Node attribute = attributes.item(i);

                if( attribute.getNodeName().equalsIgnoreCase("code") && attribute.hasChildNodes() ) {
                    items.message = attribute.getFirstChild().getNodeValue().trim();
                }
                else if( attribute.getNodeName().equalsIgnoreCase("message") ) {
                    items.details = attribute.getFirstChild().getNodeValue().trim();
                }
            }
        }
        return items;
    }

    public AzureException(ExceptionItems items) {
        super(items.type, items.code, items.message, items.details);
    }

    public AzureException(CloudErrorType type, int code, String message, String details) {
        super(type, code, message, details);
    }
}
