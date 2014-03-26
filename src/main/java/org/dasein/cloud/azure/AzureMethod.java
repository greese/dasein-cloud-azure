/**
 * Copyright (C) 2012 enStratus Networks Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.azure;

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
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
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
 * @version 2012.07 initial version
 * @since 2012.07
 */
public class AzureMethod {
    static private final Logger logger = Azure.getLogger(AzureMethod.class);
    static private final Logger wire   = Azure.getWireLogger(AzureMethod.class);

    static public class AzureResponse {
        public int httpCode;
        public Object body;
    }
    
    private String endpoint;
    private Azure provider;
    
    public AzureMethod(Azure azure) throws CloudException {
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
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureMethod.class.getName() + ".get(" + account + "," + resource + ")");
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
                logger.error("get(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                e.printStackTrace();
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("get(): HTTP Status " + status);
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
                logger.error("get(): Expected OK for GET request, got " + status.getStatusCode());

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
                logger.error("get(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
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
                    logger.error("get(): Failed to read response error due to a cloud I/O error: " + e.getMessage());
                    e.printStackTrace();
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
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
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
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureMethod.class.getName() + ".get(" + account + "," + uri + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("--------------------------------------------------------> " + uri.toASCIIString());
            wire.debug("");
        }
        try {
            HttpClient client = getClient();
            HttpUriRequest get = new HttpGet(uri);

            //get.addHeader("Content-Type", "application/xml");
            if (uri.toString().indexOf("/services/images") > -1) {
                get.addHeader("x-ms-version", "2012-08-01");
            }
            else {
                get.addHeader("x-ms-version", "2012-03-01");
            }
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
                logger.error("get(): Failed to execute HTTP request due to a cloud I/O error: " + e.getMessage());
                if( logger.isTraceEnabled() ) {
                    e.printStackTrace();
                }
                throw new CloudException(e);
            }
            if( logger.isDebugEnabled() ) {
                logger.debug("get(): HTTP Status " + status);
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
                logger.error("get(): Expected OK for GET request, got " + status.getStatusCode());

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
                logger.error("get(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
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
                    logger.error("get(): Failed to read response error due to a cloud I/O error: " + e.getMessage());
                    if( logger.isTraceEnabled() ) {
                        e.printStackTrace();
                    }
                    throw new CloudException(e);
                }
                return parseResponse(input, true);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".getStream()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("--------------------------------------------------------> " + uri.toASCIIString());
            }
        }
    }
    
    protected @Nonnull HttpClient getClient() throws CloudException, InternalException {
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
            registry.register(new Scheme(ssl ? "https" : "http", targetPort, new AzureSSLSocketFactory(new AzureX509(provider))));
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
        params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 300000);

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

    public String post(@Nonnull String account, @Nonnull String resource, @Nonnull String body) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureMethod.class.getName() + ".post(" + account + "," + resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            wire.debug("");
        }
        String requestId = null;
        try {
            HttpClient client = getClient();
            String url = endpoint + account + resource;

            HttpPost post = new HttpPost(url);
          
            post.addHeader("x-ms-version", "2012-03-01");
            
            //If it is networking configuration services
            if(url.endsWith("/services/networking/media")){
            	post.addHeader("Content-Type", "text/plain;charset=UTF-8");
            }else{
            	post.addHeader("Content-Type", "application/xml;charset=UTF-8");
            }
            
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
                	 if(url.endsWith("/services/networking/media")){
                		 post.setEntity(new StringEntity(body, "text/plain", "utf-8"));                     	
                     }else{
                    	 post.setEntity(new StringEntity(body, "application/xml", "utf-8")); 
                     }
                    
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
            }
            for( Header h : headers ) {
                if( h.getValue() != null ) {
                    if( wire.isDebugEnabled() ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    if (h.getName().equalsIgnoreCase("x-ms-request-id")) {
                        requestId = h.getValue().trim();
                    }
                }
                else {
                    if( wire.isDebugEnabled() ) {
                        wire.debug(h.getName() + ":");
                    }
                }
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
            }

            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_CREATED && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                logger.error("post(): Expected OK for GET request, got " + status.getStatusCode());

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
                logger.error("post(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".post()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            }
        }
        return requestId;
    }
    
    protected HttpRequestBase getMethod(String httpMethod,String url) {
    	HttpRequestBase method = null;
        if(httpMethod.equals("GET")){
        	method = new HttpGet(url);
        }else if(httpMethod.equals("POST")){
        	 method = new HttpPost(url);
        }else if(httpMethod.equals("PUT")){
            method = new HttpPut(url);	        	
        }else if(httpMethod.equals("DELETE")){
        	method = new HttpDelete(url);
        }else if(httpMethod.equals("HEAD")){
        	 method = new HttpHead(url);
        }else if(httpMethod.equals("OPTIONS")){
        	 method = new HttpOptions(url);
        }else if(httpMethod.equals("HEAD")){
        	method = new HttpTrace(url);
        }else{
        	method = new HttpGet(url);
        }
        return method;
    }
    
    public String invoke(@Nonnull String method, @Nonnull String account, @Nonnull String resource, @Nonnull String body) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureMethod.class.getName() + ".post(" + account + "," + resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            wire.debug("");
        }
        String requestId = null;
        try {
            HttpClient client = getClient();
            String url = endpoint + account + resource;

            HttpRequestBase httpMethod = getMethod(method, url);

            //If it is networking configuration services
            if (httpMethod instanceof HttpPut) {
                if(url.endsWith("/services/networking/media")){
                    httpMethod.addHeader("Content-Type", "text/plain");
                }else{
                    httpMethod.addHeader("Content-Type", "application/xml;charset=UTF-8");
                }
            }
            else {
                httpMethod.addHeader("Content-Type", "application/xml;charset=UTF-8");
            }

            //dmayne version is older for anything to do with images and for disk deletion
            if (url.indexOf("/services/images") > -1 || (httpMethod instanceof HttpDelete && url.indexOf("/services/disks") > -1)) {
                httpMethod.addHeader("x-ms-version", "2012-08-01");
            }
            else {
                httpMethod.addHeader("x-ms-version", "2012-03-01");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug(httpMethod.getRequestLine().toString());
                for( Header header : httpMethod.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }
            
            
            if(httpMethod instanceof HttpEntityEnclosingRequestBase ){
            	
            	HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) httpMethod;
            	
	            if (body != null) {
					try {
						entityEnclosingMethod.setEntity(new StringEntity(body, "application/xml", "utf-8"));
					} catch (UnsupportedEncodingException e) {
                        throw new CloudException(e);
					}
	            }           	
            }          
                      
            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(httpMethod);
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
            }
            for( Header h : headers ) {
                if( h.getValue() != null ) {
                    if( wire.isDebugEnabled() ) {
                        wire.debug(h.getName() + ": " + h.getValue().trim());
                    }
                    if (h.getName().equalsIgnoreCase("x-ms-request-id")) {
                        requestId = h.getValue().trim();
                    }
                }
                else {
                    if( wire.isDebugEnabled() ) {
                        wire.debug(h.getName() + ":");
                    }
                }
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
            }
            if (status.getStatusCode() == HttpServletResponse.SC_TEMPORARY_REDIRECT) {
                logger.warn("Expected OK, got "+status.getStatusCode());

                String responseBody = "";

                HttpEntity entity = response.getEntity();

                if( entity == null ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), "An error was returned without explanation");
                }
                try {
                    responseBody = EntityUtils.toString(entity);
                }
                catch( IOException e ) {
                    throw new AzureException(CloudErrorType.GENERAL, status.getStatusCode(), status.getReasonPhrase(), e.getMessage());
                }
                logger.debug(responseBody);
                logger.debug("https: char "+responseBody.indexOf("https://"));
                logger.debug("account number: char "+responseBody.indexOf(account));
                String tempEndpoint = responseBody.substring(responseBody.indexOf("https://"), responseBody.indexOf(account)-responseBody.indexOf("https://"));
                logger.debug("temp redirect location: "+tempEndpoint);
                tempRedirectInvoke(tempEndpoint, method, account, resource, body);
            }
            else if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_CREATED && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                logger.error("post(): Expected OK for GET request, got " + status.getStatusCode());

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
                logger.error("post(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".post()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            }
        }
        return requestId;
    }

    public void tempRedirectInvoke(@Nonnull String tempEndpoint, @Nonnull String method, @Nonnull String account, @Nonnull String resource, @Nonnull String body) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("enter - " + AzureMethod.class.getName() + ".post(" + account + "," + resource + ")");
        }
        if( wire.isDebugEnabled() ) {
            wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            wire.debug("");
        }
        try {
            HttpClient client = getClient();
            String url = tempEndpoint + account + resource;

            HttpRequestBase httpMethod = getMethod(method, url);

            //If it is networking configuration services
            if (httpMethod instanceof HttpPut) {
                if(url.endsWith("/services/networking/media")){
                    httpMethod.addHeader("Content-Type", "text/plain");
                }else{
                    httpMethod.addHeader("Content-Type", "application/xml;charset=UTF-8");
                }
            }
            else {
                httpMethod.addHeader("Content-Type", "application/xml;charset=UTF-8");
            }

            //dmayne version is older for anything to do with images and for disk deletion
            if (url.indexOf("/services/images") > -1 || (httpMethod instanceof HttpDelete && url.indexOf("/services/disks") > -1)) {
                httpMethod.addHeader("x-ms-version", "2012-08-01");
            }
            else {
                httpMethod.addHeader("x-ms-version", "2012-03-01");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug(httpMethod.getRequestLine().toString());
                for( Header header : httpMethod.getAllHeaders() ) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
                if( body != null ) {
                    wire.debug(body);
                    wire.debug("");
                }
            }


            if(httpMethod instanceof HttpEntityEnclosingRequestBase ){

                HttpEntityEnclosingRequestBase entityEnclosingMethod = (HttpEntityEnclosingRequestBase) httpMethod;

                if (body != null) {
                    try {
                        entityEnclosingMethod.setEntity(new StringEntity(body, "application/xml", "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            HttpResponse response;
            StatusLine status;

            try {
                response = client.execute(httpMethod);
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
            if( status.getStatusCode() != HttpServletResponse.SC_OK && status.getStatusCode() != HttpServletResponse.SC_CREATED && status.getStatusCode() != HttpServletResponse.SC_ACCEPTED ) {
                logger.error("post(): Expected OK for GET request, got " + status.getStatusCode());

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
                logger.error("post(): [" + status.getStatusCode() + " : " + items.message + "] " + items.details);
                throw new AzureException(items);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("exit - " + AzureMethod.class.getName() + ".post()");
            }
            if( wire.isDebugEnabled() ) {
                wire.debug("");
                wire.debug("POST --------------------------------------------------------> " + endpoint + account + resource);
            }
        }
    }

    public @Nonnull int getOperationStatus(String requestID) throws CloudException, InternalException {
        ProviderContext ctx = provider.getContext();
        Document doc = getAsXML(ctx.getAccountNumber(),"/operations/"+requestID);

        if (doc == null) {
            return -2;
        }

        NodeList entries = doc.getElementsByTagName("Operation");
        Node entry = entries.item(0);

        NodeList s = entry.getChildNodes();

        String status = "";
        String httpCode = "";

        for (int i =0; i<s.getLength(); i++) {
            Node attribute = s.item(i);
            if( attribute.getNodeType() == Node.TEXT_NODE) {
                continue;
            }
            if( attribute.getNodeName().equalsIgnoreCase("status") && attribute.hasChildNodes() ) {
                status = attribute.getFirstChild().getNodeValue().trim();
                continue;
            }
            if (status.length() > 0 && !status.equalsIgnoreCase("inProgress")) {
                if( attribute.getNodeName().equalsIgnoreCase("httpstatuscode") && attribute.hasChildNodes() ) {
                    httpCode = attribute.getFirstChild().getNodeValue().trim();
                }
            }
        }

        if (status.equalsIgnoreCase("succeeded")) {
           return HttpServletResponse.SC_OK;
        }
        else if (status.equalsIgnoreCase("failed")) {
            String errMsg = checkError(s, httpCode);
            throw new CloudException(errMsg);
        }
        return -1;
    }

    private String checkError(NodeList s, String httpCode) throws CloudException, InternalException {
        String errMsg = httpCode+": ";
        for (int i=0; i<s.getLength(); i++) {
            Node attribute = s.item(i);
            if( attribute.getNodeType() == Node.TEXT_NODE) {
                continue;
            }
            if( attribute.getNodeName().equalsIgnoreCase("Error") && attribute.hasChildNodes() ) {
                NodeList errors = attribute.getChildNodes();
                for (int error = 0; error < errors.getLength(); error++) {
                    Node node = errors.item(error);
                    if (node.getNodeName().equalsIgnoreCase("code") && node.hasChildNodes()) {
                        errMsg = errMsg + node.getFirstChild().getNodeValue().trim();
                        continue;
                    }
                    if (node.getNodeName().equalsIgnoreCase("message") && node.hasChildNodes()) {
                        errMsg = errMsg + ". reason: " + node.getFirstChild().getNodeValue().trim();
                    }
                }
            }
        }
        return errMsg;
    }
}
