package org.dasein.cloud.azure;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Handles connectivity to Microsoft Azure Storage services.
 * @author George Reese (george.reese@imaginary.com)
 * @author Qunying Huang (qunying.huang@enstratus.com)
 * @since 2012.07.1
 * @version 2012.07.1
 */
public class AzureStorageMethod {
    static private final Logger logger = Azure.getLogger(AzureStorageMethod.class);
    static private final Logger wire = Azure.getWireLogger(AzureStorageMethod.class);

    static public final String VERSION = "2009-09-19";
    //static public final String VERSION = "2012-02-12";

    private String Header_Prefix_MS = "x-ms-";
    
    public static final String  Storage_OPERATION_DELETE = "DELETE";
    public static final String  Storage_OPERATION_PUT = "PUT";
    public static final String  Storage_OPERATION_GET = "GET";

    private Azure  provider;

    public AzureStorageMethod(Azure azure) throws AzureConfigException {
        provider = azure;
        ProviderContext ctx = provider.getContext();
        
        if( ctx == null ) {
            throw new AzureConfigException("No context was provided for this request");
        }        
    }

    private void fetchKeys() throws CloudException, InternalException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was set for this request");
        }

        if( ctx.getStoragePrivate() == null ) {
            AzureMethod method = new AzureMethod(provider);

            Document doc = method.getAsXML(ctx.getAccountNumber(), "/services/storageservices/" + provider.getStorageService() + "/keys");

            if( doc == null ) {
                throw new CloudException("Unable to identify the storage keys for this account");
            }
            NodeList keys = doc.getElementsByTagName("StorageServiceKeys");

            for( int i=0; i<keys.getLength(); i++ ) {
                Node key = keys.item(i);

                if( key.getNodeName().equalsIgnoreCase("StorageServiceKeys") && key.hasChildNodes() ) {
                    NodeList parts = key.getChildNodes();
                    String p = null, s = null;

                    for( int j=0; j<parts.getLength(); j++ ) {
                        Node part = parts.item(j);

                        if( part.getNodeName().equalsIgnoreCase("primary") && part.hasChildNodes() ) {
                            p = part.getFirstChild().getNodeValue().trim();
                        }
                        else if( part.getNodeName().equalsIgnoreCase("secondary") && part.hasChildNodes() ) {
                            s = part.getFirstChild().getNodeValue().trim();
                        }
                    }
                    if( p != null ) {
                        try {
                            ctx.setStoragePrivate(p.getBytes("utf-8"));
                        }
                        catch( UnsupportedEncodingException e ) {
                            logger.error("UTF-8 not supported: " + e.getMessage());
                            throw new InternalException(e);
                        }
                        break;
                    }
                    else if( s != null ) {
                        try {
                            ctx.setStoragePrivate(s.getBytes("utf-8"));
                        }
                        catch( UnsupportedEncodingException e ) {
                            logger.error("UTF-8 not supported: " + e.getMessage());
                            throw new InternalException(e);
                        }
                        break;
                    }
                }
            }
        }
    }

    private String getStorageAccount() throws CloudException, InternalException {
        return provider.getStorageService();
    }

    private String calculatedSharedKeyLiteSignature(@Nonnull HttpRequestBase method, @Nonnull Map<String, String> queryParams) throws  CloudException, InternalException {
        fetchKeys();

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        Header h = method.getFirstHeader("content-type");
        String contentType = (h == null ? null : h.getValue());

        if( contentType == null ) {
            contentType = "";
        }
        StringBuilder stringToSign = new StringBuilder();

        stringToSign.append(method.getMethod().toUpperCase()).append("\n");
        stringToSign.append("\n"); // content-md5
        stringToSign.append(contentType).append("\n");
        stringToSign.append(method.getFirstHeader("date").getValue()).append("\n");

        Header[] headers = method.getAllHeaders();
        TreeSet<String> keys = new TreeSet<String>();

        for( Header header  : headers ) {
            if( header.getName().startsWith(Header_Prefix_MS) ) {
                keys.add(header.getName().toLowerCase());
            }
        }

        for( String key : keys ) {
            Header header = method.getFirstHeader(key);

            if( header != null ) {
                Header[] all = method.getHeaders(key);

                stringToSign.append(key.toLowerCase().trim()).append(":");
                if( all != null && all.length > 0 ) {
                    for( Header current : all ) {
                        String v = (current.getValue() != null ? current.getValue() : "");

                        stringToSign.append(v.trim().replaceAll("\n", " ")).append(",");
                    }
                }
                stringToSign.deleteCharAt(stringToSign.lastIndexOf(","));
            }
            else {
                stringToSign.append(key.toLowerCase().trim()).append(":");
            }
            stringToSign.append("\n");
        }

        stringToSign.append("/").append(getStorageAccount()).append(method.getURI().getPath());

        keys.clear();
        for( String key : queryParams.keySet() ) {
            if( key.equalsIgnoreCase("comp") ) {
                key = key.toLowerCase();
                keys.add(key);
            }
        }
        if( !keys.isEmpty() ) {
            stringToSign.append("?");
            for( String key : keys ) {
                String value = queryParams.get(key);

                if( value == null ) {
                    value = "";
                }
                stringToSign.append(key).append("=").append(value).append("&");
            }
            stringToSign.deleteCharAt(stringToSign.lastIndexOf("&"));
        }
        try {
            if( logger.isDebugEnabled() ) {
                logger.debug("BEGIN STRING TO SIGN");
                logger.debug(stringToSign.toString());
                logger.debug("END STRING TO SIGN");
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.decodeBase64(ctx.getStoragePrivate()), "HmacSHA256"));

            String signature = new String(Base64.encodeBase64(mac.doFinal(stringToSign.toString().getBytes("UTF-8"))));

            if( logger.isDebugEnabled() ) {
                logger.debug("signature=" + signature);
            }
            return signature;
        }
        catch( UnsupportedEncodingException e ) {
            logger.error("UTF-8 not supported: " + e.getMessage());
            throw new InternalException(e);
        }
        catch( NoSuchAlgorithmException e ) {
            logger.error("No such algorithm: " + e.getMessage());
            throw new InternalException(e);
        }
        catch( InvalidKeyException e ) {
            logger.error("Invalid key: " + e.getMessage());
            throw new InternalException(e);
        }
    }

    /* SharedKey
	private String calculatedSharedKeySignature(HttpRequestBase method,  String serviceResource, Map<String, String> queries, String contentLength) throws  CloudException, InternalException {
        fetchKeys();

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        Header h = method.getFirstHeader("content-type");
        String contentType = "", encoding = "";

        if( h != null && h.getValue() != null ) {
            contentType = h.getValue();
            if( contentType == null ) {
                contentType = "";
            }
        }
        h = method.getFirstHeader("content-encoding");
        if( h != null ) {
            encoding = h.getValue();
            if( encoding == null ) {
                encoding = "";
            }
        }
        if( contentLength == null ) {
            contentLength = "";
        }
        StringBuilder stringToSign = new StringBuilder();

        stringToSign.append(method.getMethod().toUpperCase()).append("\n");
        stringToSign.append("\n"); // content encoding
        stringToSign.append("\n"); // content-language
        stringToSign.append("").append("\n"); // content-length
        stringToSign.append("\n"); // content-md5
        stringToSign.append(contentType).append("\n");
        stringToSign.append("\n"); // date ignored since we specify x-ms-date
        stringToSign.append("\n"); // if-modified-since
        stringToSign.append("\n"); // if-match
        stringToSign.append("\n"); // if-none-match
        stringToSign.append("\n"); // if-unmodified-since
        stringToSign.append("\n"); // range

		Header[] headers = method.getAllHeaders();
        TreeSet<String> keys = new TreeSet<String>();

        for( Header header  : headers ) {
            if( header.getName().startsWith(Header_Prefix_MS) ) {
                keys.add(header.getName().toLowerCase());
            }
        }
        for( String key : keys ) {
            Header header = method.getFirstHeader(key);

            if( header != null ) {
                Header[] all = method.getHeaders(key);

                stringToSign.append(key.toLowerCase().trim()).append(":");
                if( all != null && all.length > 0 ) {
                    for( Header current : all ) {
                        String v = (current.getValue() != null ? current.getValue() : "");

                       stringToSign.append(v.trim().replaceAll("\n", " ")).append(",");
                    }
                }
                stringToSign.deleteCharAt(stringToSign.lastIndexOf(","));
            }
            else {
                stringToSign.append(key.toLowerCase().trim()).append(":");
            }
            stringToSign.append("\n");
        }

        stringToSign.append("/").append(getStorageAccount()).append(method.getURI().getPath()).append("\n");

        keys.clear();
        for( String key : queries.keySet() ) {
            String value = queries.get(key);

            queries.remove(key);
            key = key.toLowerCase();
            queries.put(key, value);
            keys.add(key);
        }
        for( String key : keys ) {
            String value = queries.get(key);

            if( value == null ) {
                value = "";
            }
            stringToSign.append(key).append(":").append(value).append("\n");
		}
	
		try {
            System.out.println("String to sign:\n---\n" + stringToSign.toString() + "---");



            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.decodeBase64(ctx.getStoragePrivate()), "HmacSHA256"));

            return new String(Base64.encodeBase64(mac.doFinal(stringToSign.toString().getBytes("UTF-8"))));
        }
        catch (UnsupportedEncodingException e) {
			throw new InternalException(e.getMessage());
		}
		catch (NoSuchAlgorithmException e) {
			throw new InternalException(e.getMessage());
		}
		catch (InvalidKeyException e) {
			throw new InternalException(e.getMessage());
		}	
	}

	public String createSignatureString(Map<String, String> signatureMap)  throws CloudException, InternalException{
		fetchKeys();
		String StringToSign =  signatureMap.get("signedpermissions")  + "\n"
				 + signatureMap.get("signedstart")  + "\n"
				 + signatureMap.get("signedexpiry")   + "\n"
				 + signatureMap.get("canonicalizedresource")   + "\n"
	             + signatureMap.get("signedidentifier")    + "\n"
	             + signatureMap.get("signedversion");		
		try {	
			
			 Mac mac = Mac.getInstance("HmacSHA256");
		        
			 mac.init(new SecretKeySpec(provider.getContext().getStoragePrivate(), "HmacSHA256"));
		    
			 return new String(Base64.encodeBase64(mac.doFinal(StringToSign.getBytes("utf-8"))));

		} catch (UnsupportedEncodingException e) {
			throw new InternalException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new InternalException(e.getMessage());
		} catch (InvalidKeyException e) {
			throw new InternalException(e.getMessage());
		}
	}
		*/

    public static Document createDoc() throws InternalException{
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new InternalException(e);
		}		
	}
	
	/**
	 *  Convert a xml document to string
	 */
	@SuppressWarnings("unused")
	public static String convertDomToString( Document doc)  throws CloudException, InternalException{
		try {
			 if(doc == null) return null;
			 StringWriter stw = new StringWriter();
	         Transformer serializer = TransformerFactory.newInstance().newTransformer();
	         serializer.transform(new DOMSource(doc), new StreamResult(stw));
	        if(stw != null){
	        	return stw.toString();
	        }
	        return null;
		} catch (TransformerConfigurationException e) {			
			throw new InternalException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new InternalException(e);
		} catch (TransformerException e) {
			throw new InternalException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public Document getAsDoc(@Nonnull String httpVerb, @Nonnull String resource, @Nullable Map<String, String> queries, @Nullable String body, @Nullable Map<String, String> headerMap, boolean authorization) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureStorageMethod.class.getName() + "." + httpVerb + "(" + getStorageAccount() + "," + resource + ")");
        }
        String endpoint = provider.getStorageEndpoint();

        if( wire.isDebugEnabled() ) {
            wire.debug(httpVerb + "--------------------------------------------------------> " + endpoint + getStorageAccount() + resource);
            wire.debug("");
        }
        try {
            HttpClient client =  getClient();

            if( headerMap == null ) {
                headerMap = new HashMap<String,String>();
            }

            HttpRequestBase method = getMethod(httpVerb, buildUrl(resource, queries), queries, headerMap, authorization);

            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            
            // If it is post or put
            if(method instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
            	
	            if (body != null) {
					entityEnclosingMethod.setEntity(new StringEntity(body, "application/xml", "utf-8"));
	            }           	
            }           
          
            HttpResponse response ;
            StatusLine status;
            
            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                logger.error("GET(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("GET(): HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();
            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            
            if( status.getStatusCode() == HttpServletResponse.SC_NOT_FOUND ) {
                return null;
            }
            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION ) {
                logger.error(httpVerb + "(): Expected OK for " + httpVerb + "request, got " + status.getStatusCode());
                
                HttpEntity entity = response.getEntity();
                String result;
                               
                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                	result = EntityUtils.toString(entity);           
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(result);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), result);

                if( items == null ) {
                    return null;
                }

                logger.error(httpVerb + "(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
            else {

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    return null;
                }
                InputStream input;

                try {                	
                    input = entity.getContent();
                }
                catch( IOException e ) {
                    logger.error(httpVerb + "(): Failed to read response error due to a cloud I/O error: " + e.getMessage());
                    if( logger.isTraceEnabled() ) {
                        e.printStackTrace();
                    }
                    throw new CloudException(e);
                }
                return parseResponse(input, true);
            }
        } catch (UnsupportedEncodingException e) {			
        	throw new CloudException(e);
		}
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " );
            }
        } 
	}
	       
	
	
    public @Nullable InputStream getAsStream(@Nonnull String strMethod, @Nonnull String resource, @Nonnull Map<String, String> queries, @Nullable String body,  @Nullable Map<String, String> headerMap, boolean authorization) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureStorageMethod.class.getName() + "." + strMethod + "(" + getStorageAccount() + "," + resource + ")");
        }
        String endpoint = provider.getStorageEndpoint();
        if( wire.isDebugEnabled() ) {
            wire.debug(strMethod + "--------------------------------------------------------> " + endpoint + getStorageAccount() + resource);
            wire.debug("");
        }
        try {

            HttpClient client =  getClient();
            
            String contentLength = null;
            if(body != null){        	
            	contentLength = String.valueOf(body.length());            	
            }else{
            	contentLength = "0";        	
            }

            HttpRequestBase method = getMethod(strMethod, buildUrl(resource, queries),queries, headerMap, authorization);
  
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            
            // If it is post or put
            if(method instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
            	
	            if (body != null) {
					entityEnclosingMethod.setEntity(new StringEntity(body, "application/xml", "utf-8"));
	            }            	
            }           
          
            HttpResponse response ;
            StatusLine status;
            
            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                logger.error("post(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("post(): HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();
  
            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            
            if( status.getStatusCode() == HttpServletResponse.SC_NOT_FOUND ) {
                return null;
            }
            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION ) {
                logger.error(strMethod + "(): Expected OK for " + strMethod + "request, got " + status.getStatusCode());
                
                HttpEntity entity = response.getEntity();
                String result;
                               
                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                	result = EntityUtils.toString(entity);                
          }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(result);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), result);

                if( items == null ) {
                    return null;
                }
                
                logger.error(strMethod + "(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
            else {

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    return null;
                }
                InputStream input;

                try {                	
                    input = entity.getContent();
                }
                catch( IOException e ) {
                    logger.error(strMethod + "(): Failed to read response error due to a cloud I/O error: " + e.getMessage());
                    if( logger.isTraceEnabled() ) {
                        e.printStackTrace();
                    }
                    throw new CloudException(e);
                }
                return input;
            }
        } catch (UnsupportedEncodingException e) {			
        	throw new CloudException(e);
		}
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " );
            }
        } 
    }

    protected @Nonnull HttpClient getClient() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was defined for this request");
        }
        String endpoint = provider.getStorageEndpoint();
        boolean ssl = endpoint.startsWith("https");
        int targetPort;

        try {
        	
            URI uri = new URI(endpoint);
            targetPort = uri.getPort();
            
            if( targetPort < 1 ) {
                targetPort = (ssl ? 443 : 80);
            }
        }
        catch( URISyntaxException e ) {
            throw new AzureConfigException(e);
        }
        HttpParams params = new BasicHttpParams();
   
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
       
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        
        Properties p = ctx.getCustomProperties();

        if( p != null ) {
            String proxyHost = p.getProperty("proxyHost");
            String proxyPort = p.getProperty("proxyPort");

            if( proxyHost != null ) {
                int port = 0;

                if( proxyPort != null && proxyPort.length() > 0 ) {
                    port = Integer.parseInt(proxyPort);
                }
                params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, port, ssl ? "https" : "http"));
            }
        }
        return new DefaultHttpClient (params);
        
    }


    public @Nonnull Document parseResponse(@Nonnull String responseBody, boolean withWireLogging) throws CloudException, InternalException {
        try {
            if( wire != null && wire.isDebugEnabled() ) {
                String[] lines = responseBody.split("\n");

                if( lines.length < 1 ) {
                    lines = new String[] { responseBody };
                }
                for( String l : lines ) {
                    wire.debug(l);
                }
                wire.debug("");
            }
            ByteArrayInputStream bas = new ByteArrayInputStream(responseBody.getBytes());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.parse(bas);

            bas.close();
            return doc;
        }
        catch( IOException e ) {
            throw new CloudException(e);
        }
        catch( ParserConfigurationException e ) {
            throw new CloudException(e);
        }
        catch( SAXException e ) {
            throw new CloudException(e);
        }
    }
    
    public @Nonnull Document parseResponse(@Nonnull InputStream responseBodyAsStream, boolean withWireLogging) throws CloudException, InternalException {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(responseBodyAsStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;

            while( (line = in.readLine()) != null ) {
                sb.append(line);
                sb.append("\n");
            }
            in.close();          
            // The response may start with ? instead of <
            String strResponse= sb.toString();
                           
            int index = strResponse.indexOf("<");
            
            if(index > 0){
            	return parseResponse(strResponse.substring(index), withWireLogging);
            }else{
            	return parseResponse(strResponse, withWireLogging);
            }
            
        }
        catch( IOException e ) {
            throw new CloudException(e);
        }
    }
    
	public String buildUrl(String resource, Map<String, String> queries) throws InternalException, CloudException {
        String endpoint = provider.getStorageEndpoint();

        StringBuilder str = new StringBuilder();       
        str.append(endpoint);
        
        if(!endpoint.endsWith("/")){
        	str.append("/");
        }
        if(resource != null && !resource.equalsIgnoreCase("null")){
        	str.append(resource);	
        }        
    
        if(queries != null && queries.size() > 0 ){
        	str.append("?");
        }
        
        boolean firstPara = true;
        if(queries != null){
        	//comp key should be put first
        	if( queries.containsKey("comp")){
    		    str.append("comp");
                str.append("=");
                str.append(queries.get("comp"));
                firstPara = false;
        	}
        	
            for(String key: queries.keySet()){
            	if( key.equals("comp")) continue;
            	
            	if(firstPara ){ 
        		    str.append(key);
                    str.append("=");
                    str.append(queries.get(key));
                    firstPara = false;
        		}else{
    			   str.append("&");
    			   str.append(key);
                   str.append("=");
                   str.append(queries.get(key));
        		}
        	}            
        } 
        return str.toString();        
    }
	
	
    public String getBlobProperty(@Nonnull String strMethod, @Nonnull String resource, @Nonnull Map<String, String> queries, String body, @Nullable Map<String, String> headerMap, boolean authorization, String propertyName) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureStorageMethod.class.getName() + "." + strMethod + "(" + getStorageAccount() + "," + resource + ")");
        }
        String endpoint = provider.getStorageEndpoint();

        if( wire.isDebugEnabled() ) {
            wire.debug(strMethod + "--------------------------------------------------------> " + endpoint + getStorageAccount() + resource);
            wire.debug("");
        }
        try {

            HttpClient client =  getClient();

            if( headerMap == null ) {
                headerMap = new HashMap<String, String>();
            }

            HttpRequestBase method = getMethod(strMethod, buildUrl(resource, queries), queries, headerMap, authorization );
     	
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            
            // If it is post or put
            if(method instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
            	
	            if (body != null) {
					entityEnclosingMethod.setEntity(new StringEntity(body, "application/xml", "utf-8"));
	            }           	
            }           
          
            HttpResponse response ;
            StatusLine status;
            
            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                logger.error("post(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("get(): HTTP Status " + status);
            }

            if( wire.isDebugEnabled() ) {
                Header[] headers = response.getAllHeaders();
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
            if( status.getStatusCode() == HttpServletResponse.SC_NOT_FOUND ) {
                return null;
            }
            if((status.getStatusCode() != HttpServletResponse.SC_CREATED
            		&& status.getStatusCode() != HttpServletResponse.SC_ACCEPTED 
            		&& status.getStatusCode() != HttpServletResponse.SC_OK ) 
            		&& status.getStatusCode() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION ) {
                logger.error(strMethod + "(): Expected OK for " + strMethod + "request, got " + status.getStatusCode());
                
                HttpEntity entity = response.getEntity();
                String result;
                               
                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                	result = EntityUtils.toString(entity);
                	
                    int index = result.indexOf("<");                    
                    // The result may not be a stardard xml format
                    if(index > 0){
                    	result = result.substring(index);                    	
                    }     
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(result);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), result);
                logger.error(strMethod + "(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
            else{
            	Header header = response.getFirstHeader(propertyName);
            	if(header != null){
            		return header.getValue();          		
            	}else{
            		return null;
            	}
            }
        } catch (UnsupportedEncodingException e) {			
        	throw new CloudException(e);
		}
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " );
            }
        } 
    }
    
    public void invoke(@Nonnull String strMethod, @Nonnull String resource, @Nonnull Map<String, String> queries, @Nullable String body, @Nullable Map<String, String> headerMap, boolean authorization) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureStorageMethod.class.getName() + "." + strMethod + "(" + getStorageAccount() + "," + resource + ")");
        }
        String endpoint = provider.getStorageEndpoint();

        if( wire.isDebugEnabled() ) {
            wire.debug(strMethod + "--------------------------------------------------------> " + endpoint + getStorageAccount() + resource);
            wire.debug("");
        }
        try {
            HttpClient client =  getClient();
            
            if( headerMap == null ) {
                headerMap = new HashMap<String, String>();
            }

            HttpRequestBase method = getMethod(strMethod, buildUrl(resource, queries), queries, headerMap, authorization );
     	
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            
            // If it is post or put
            if(method instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
            	
	            if (body != null) {
					entityEnclosingMethod.setEntity(new StringEntity(body, "application/xml", "utf-8"));
	            }           	
            }           
          
            HttpResponse response ;
            StatusLine status;
            
            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                logger.error("post(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("post(): HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();
        
            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
 
            if((status.getStatusCode() != HttpServletResponse.SC_CREATED
            		&& status.getStatusCode() != HttpServletResponse.SC_ACCEPTED 
            		&& status.getStatusCode() != HttpServletResponse.SC_OK ) 
            		&& status.getStatusCode() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION ) {
                logger.error(strMethod + "(): Expected OK for " + strMethod + "request, got " + status.getStatusCode());
                
                HttpEntity entity = response.getEntity();
                String result;
                               
                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                	result = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(result);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), result);

                if( items != null ) {
                    logger.error(strMethod + "(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                    throw new AzureException(items);
                }
                else {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), "UnknownError", result);
                }
            }
        } catch (UnsupportedEncodingException e) {			
        	throw new CloudException(e);
		}
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " );
            }
        } 
    }
    
    public void putWithFile(@Nonnull String strMethod, @Nonnull String resource, Map<String, String> queries, File file, Map<String, String> headerMap, boolean authorization) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureStorageMethod.class.getName() + "." + strMethod + "(" + getStorageAccount() + "," + resource + ")");
        }
        String endpoint = provider.getStorageEndpoint();

        if( wire.isDebugEnabled() ) {
            wire.debug(strMethod + "--------------------------------------------------------> " + endpoint + getStorageAccount() + resource);
            wire.debug("");
        }
        
        long begin = System.currentTimeMillis();
        try {

            HttpClient client =  getClient();
            
            String contentLength = null;
            
            if(file != null){        	
            	contentLength = String.valueOf(file.length());            	
            }else{
            	contentLength = "0";        	
            }

            HttpRequestBase method = getMethod(strMethod, buildUrl(resource, queries), queries, headerMap, authorization );
     	
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( file != null ) {
                    wire.debug(file);
                    wire.debug("");
                }
            }
            
            // If it is post or put
            if(method instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
            	
	            if (file != null) {
					entityEnclosingMethod.setEntity(new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM));
	            }           	
            }           
          
            HttpResponse response ;
            StatusLine status;
            
            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                logger.error("post(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                
                long end =  System.currentTimeMillis();
                logger.debug("Totoal time -> " + (end - begin));
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("post(): HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();
           
            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
 
            if((status.getStatusCode() != HttpServletResponse.SC_CREATED
            		&& status.getStatusCode() != HttpServletResponse.SC_ACCEPTED 
            		&& status.getStatusCode() != HttpServletResponse.SC_OK ) 
            		&& status.getStatusCode() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION ) {
                logger.error(strMethod + "(): Expected OK for " + strMethod + "request, got " + status.getStatusCode());
                
                HttpEntity entity = response.getEntity();
                String result;
                               
                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                	result = EntityUtils.toString(entity);           
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(result);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), result);
                logger.error(strMethod + "(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " );
            }
        } 
    }
    
  
    public void putWithBytes(@Nonnull String strMethod, @Nonnull String resource, Map<String, String> queries, byte[] body, Map<String, String> headerMap, boolean authorization) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureStorageMethod.class.getName() + "." + strMethod + "(" + getStorageAccount() + "," + resource + ")");
        }
        String endpoint = provider.getStorageEndpoint();

        if( wire.isDebugEnabled() ) {
            wire.debug(strMethod + "--------------------------------------------------------> " + endpoint + getStorageAccount() + resource);
            wire.debug("");
        }
        try {

            HttpClient client =  getClient();
            
            String contentLength = null;
            if(body != null){        	
            	contentLength = String.valueOf(body.length);            	
            }else{
            	contentLength = "0";        	
            }

            HttpRequestBase method = getMethod(strMethod, buildUrl(resource, queries), queries, headerMap, authorization );
     	
            if( wire.isDebugEnabled() ) {
                wire.debug(method.getRequestLine().toString());
                for( Header header : method.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            
            // If it is post or put
            if(method instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) method;
            	
	            if (body != null) {
					entityEnclosingMethod.setEntity(new ByteArrayEntity(body));
	            }           	
            }           
          
            HttpResponse response ;
            StatusLine status;
            
            try {
                response = client.execute(method);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                logger.error("post(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("post(): HTTP Status " + status);
            }
            Header[] headers = response.getAllHeaders();
           
            if( wire.isDebugEnabled() ) {
                wire.debug(status.toString());
                for( Header h : headers ) {
                    if( h.getValue() != null ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    else {
                        wire.debug(h.getName() + ":");
                    }
                }
                wire.debug("");
            }
 
            if((status.getStatusCode() != HttpServletResponse.SC_CREATED
            		&& status.getStatusCode() != HttpServletResponse.SC_ACCEPTED 
            		&& status.getStatusCode() != HttpServletResponse.SC_OK ) 
            		&& status.getStatusCode() != HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION ) {
                
            	logger.error(strMethod + "(): Expected OK for " + strMethod + "request, got " + status.getStatusCode());
                
                HttpEntity entity = response.getEntity();
                String result;
                               
                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                	result = EntityUtils.toString(entity);                	
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(result);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), result);
                logger.error(strMethod + "(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " );
            }
        } 
    }
       
       
    protected HttpRequestBase getMethod(@Nonnull String httpMethod, @Nonnull String endpoint, @Nonnull Map<String, String> queryParams, @Nullable Map<String, String> headers, boolean authorization) throws CloudException, InternalException {
    	HttpRequestBase method;

        if( httpMethod.equals("GET") ) {
        	method = new HttpGet(endpoint);
        }
        else if(httpMethod.equals("POST")){
        	 method = new HttpPost(endpoint);
        }
        else if(httpMethod.equals("PUT")){
            method = new HttpPut(endpoint);
        }
        else if(httpMethod.equals("DELETE")){
        	method = new HttpDelete(endpoint);
        }
        else if(httpMethod.equals("HEAD")){
        	 method = new HttpHead(endpoint);
        }
        else if(httpMethod.equals("OPTIONS")){
        	 method = new HttpOptions(endpoint);
        }
        else if(httpMethod.equals("HEAD")){
        	method = new HttpTrace(endpoint);
        }
        else {
        	method = new HttpGet(endpoint);
        }        
        if( !authorization ) {
        	return method;
        }
        if(headers == null) {
            headers = new TreeMap<String, String>();
        }

        String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
		DateFormat rfc1123Format = new SimpleDateFormat(RFC1123_PATTERN);

		rfc1123Format.setTimeZone(TimeZone.getTimeZone("GMT"));
        headers.put("Date", rfc1123Format.format(new Date()));
        headers.put(Header_Prefix_MS + "version", VERSION);
        for(String key: headers.keySet() ){
            method.addHeader(key, headers .get(key));
		}
        
        if(method.getFirstHeader("content-type") == null && !httpMethod.equals("GET") ) {
        	method.addHeader("content-type", "application/xml;charset=utf-8");
        }
        method.addHeader("Authorization", "SharedKeyLite " + getStorageAccount() + ":" + calculatedSharedKeyLiteSignature(method, queryParams));
        return method;
    }
}
