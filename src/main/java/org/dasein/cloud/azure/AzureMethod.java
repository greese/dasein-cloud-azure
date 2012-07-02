package org.dasein.cloud.azure;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
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
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Properties;

/**
 * Handles connectivity to Microsoft Azure services.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureMethod {
    static public class AzureResponse {
        public int httpCode;
        public Object body;
    }
    
    private String endpoint;
    private Azure provider;
    
    public AzureMethod(Azure azure) throws InternalException {
        provider = azure;
        ProviderContext ctx = provider.getContext();
        
        if( ctx == null ) {
            throw new AzureConfigException("No context was provided for this request");
        }
        endpoint = ctx.getEndpoint();
        if( endpoint == null ) {
            throw new AzureConfigException("No endpoint was provided for this request");
        }
        if( !endpoint.endsWith("/") ) {
            endpoint = endpoint + "/";
        }
    }

    public @Nullable InputStream getAsStream(@Nonnull String account, @Nonnull String resource) throws CloudException, InternalException {
        Logger std = Azure.getLogger(AzureMethod.class, "std");
        Logger wire = Azure.getLogger(AzureMethod.class, "wire");

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + AzureMethod.class.getName() + ".get(" + account + "," + resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("--------------------------------------------------------> " + endpoint + account + resource);
            wire.debug("");
        }
        try {
            HttpClient client = getClient();
            HttpUriRequest get = new HttpGet(endpoint + account + resource);

            //get.addHeader("Content-Type", "application/xml");
            get.addHeader("x-ms-version", "2012-03-01");
            if( wire.isDebugEnabled() ) {
                wire.debug(get.getRequestLine().toString());
                for( Header header : get.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(get);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("get(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("get(): HTTP Status " + status);
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
                std.error("get(): Expected OK for GET request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();
                String body;

                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                    body = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(body);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), body);

                if( items == null ) {
                    return null;
                }
                std.error("get(): [" +  status.getStatusCode() + " : " + items.message + "] " + items.details);
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
                    std.error("get(): Failed to read response error due to a cloud I/O error: " + e.getMessage());
                    if( std.isTraceEnabled() ) {
                        e.printStackTrace();
                    }
                    throw new CloudException(e);
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug("---> Binary Data <---");
                }
                wire.debug("");
                return input;
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " + endpoint + account + resource);
            }
        }
    }

    public @Nullable Document getAsXML(@Nonnull String account, @Nonnull String resource) throws CloudException, InternalException {
        try {
            return getAsXML(account, new URI(endpoint + account + resource));
        }
        catch( URISyntaxException e ) {
            throw new InternalException("Endpoint misconfiguration (" + endpoint + account + resource + "): " + e.getMessage());
        }
    }

    public @Nullable Document getAsXML(@Nonnull String account, @Nonnull URI uri) throws CloudException, InternalException {    
        Logger std = Azure.getLogger(AzureMethod.class, "std");
        Logger wire = Azure.getLogger(AzureMethod.class, "wire");

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + AzureMethod.class.getName() + ".get(" + account + "," + uri + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("--------------------------------------------------------> " + uri.toASCIIString());
            wire.debug("");
        }
        try {
            HttpClient client = getClient();
            HttpUriRequest get = new HttpGet(uri);

            //get.addHeader("Content-Type", "application/xml");
            get.addHeader("x-ms-version", "2012-03-01");
            if( wire.isDebugEnabled() ) {
                wire.debug(get.getRequestLine().toString());
                for( Header header : get.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(get);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("get(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("get(): HTTP Status " + status);
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
                std.error("get(): Expected OK for GET request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();
                String body;

                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                    body = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(body);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), body);

                if( items == null ) {
                    return null;
                }
                std.error("get(): [" +  status.getStatusCode() + " : " + items.message + "] " + items.details);
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
                    std.error("get(): Failed to read response error due to a cloud I/O error: " + e.getMessage());
                    if( std.isTraceEnabled() ) {
                        e.printStackTrace();
                    }
                    throw new CloudException(e);
                }
                return parseResponse(input, true);
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " + uri.toASCIIString());
            }
        }
    }
    
    protected @Nonnull HttpClient getClient() throws InternalException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was defined for this request");
        }
        String endpoint = ctx.getEndpoint();

        if( endpoint == null ) {
            throw new AzureConfigException("No cloud endpoint was defined");
        }
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
        SchemeRegistry registry = new SchemeRegistry();


        try {
            registry.register(new Scheme(ssl ? "https" : "http", targetPort, new AzureSSLSocketFactory(new AzureX509(ctx))));
        }
        catch( KeyManagementException e ) {
            e.printStackTrace();
            throw new InternalException(e);
        }
        catch( UnrecoverableKeyException e ) {
            e.printStackTrace();
            throw new InternalException(e);
        }
        catch( NoSuchAlgorithmException e ) {
            e.printStackTrace();
            throw new InternalException(e);
        }
        catch( KeyStoreException e ) {
            e.printStackTrace();
            throw new InternalException(e);
        }

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUserAgent(params, "Dasein Cloud");

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

        ClientConnectionManager ccm = new ThreadSafeClientConnManager(registry);

        return new DefaultHttpClient(ccm, params);
    }

    public @Nonnull Document parseResponse(@Nonnull String responseBody, boolean withWireLogging) throws CloudException, InternalException {
        Logger wire = (withWireLogging ? Azure.getLogger(AzureMethod.class, "wire") : null);
        
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
            BufferedReader in = new BufferedReader(new InputStreamReader(responseBodyAsStream));
            StringBuilder sb = new StringBuilder();
            String line;

            while( (line = in.readLine()) != null ) {
                sb.append(line);
                sb.append("\n");
            }
            in.close();

            return parseResponse(sb.toString(), withWireLogging);
        }
        catch( IOException e ) {
            throw new CloudException(e);
        }
    }

    public void post(@Nonnull String account, @Nonnull String resource, @Nonnull String body) throws CloudException, InternalException {
        Logger std = Azure.getLogger(AzureMethod.class, "std");
        Logger wire = Azure.getLogger(AzureMethod.class, "wire");

        if( std.isTraceEnabled() ) {
            std.trace("enter - " + AzureMethod.class.getName() + ".post(" + account + "," + resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            wire.debug("");
        }
        try {
            HttpClient client = getClient();
            HttpPost post = new HttpPost(endpoint + account + resource);

            post.addHeader("Content-Type", "application/xml;charset=UTF-8");
            post.addHeader("x-ms-version", "2012-03-01");
            if( wire.isDebugEnabled() ) {
                wire.debug(post.getRequestLine().toString());
                for( Header header : post.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            if( body != null ) {
                try {
                    post.setEntity(new StringEntity(body, "application/xml", "utf-8"));
                }
                catch( UnsupportedEncodingException e ) {
                    throw new InternalException(e);
                }
            }
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(post);
                status = response.getStatusLine();
            }
            catch( IOException e ) {
                std.error("post(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( std.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( std.isDebugEnabled() ) {
                std.debug("post(): HTTP Status " + status);
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
            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_CREATED && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                std.error("post(): Expected OK for GET request, got " + status.getStatusCode());

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                    body = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                if( wire.isDebugEnabled() ) {
                    wire.debug(body);
                }
                wire.debug("");
                AzureException.ExceptionItems items = AzureException.parseException(status.getStatusCode(), body);

                if( items == null ) {
                    throw new CloudException(CloudErrorType.GENERAL, status.getStatusCode(), "Unknown", "Unknown");
                }
                std.error("post(): [" +  status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
        }
        finally {
            if( std.isTraceEnabled() ) {
                std.trace("exit - " + AzureMethod.class.getName() + ".post()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            }
        }
    }
}
