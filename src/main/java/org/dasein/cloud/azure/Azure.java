/**
 * Copyright (C) 2013-2014 Dell, Inc
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

import org.apache.commons.codec.binary.Base64;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.network.AzureNetworkServices;
import org.dasein.cloud.azure.platform.AzurePlatformServices;
import org.dasein.cloud.azure.storage.AzureStorageServices;
import org.dasein.cloud.azure.storage.model.CreateStorageServiceInputModel;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

/**
 * Core cloud provider implementation for the Microsoft Azure cloud.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class Azure extends AbstractCloud {
    static private final Logger logger = Azure.getLogger(Azure.class);
	
    static private @Nonnull String getLastItem(@Nonnull String name) {
        int idx = name.lastIndexOf('.');

        if( idx < 0 ) {
            return name;
        }
        else if( idx == (name.length()-1) ) {
            return "";
        }
        return name.substring(idx + 1);
    }

    static public @Nonnull Logger getLogger(@Nonnull Class<?> cls) {
        String pkg = getLastItem(cls.getPackage().getName());

        if( pkg.equals("azure") ) {
            pkg = "";
        }
        else {
            pkg = pkg + ".";
        }
        return Logger.getLogger("dasein.cloud.azure.std." + pkg + getLastItem(cls.getName()));
    }

    static public @Nonnull Logger getWireLogger(@Nonnull Class<?> cls) {
        return Logger.getLogger("dasein.cloud.azure.wire." + getLastItem(cls.getPackage().getName()) + "." + getLastItem(cls.getName()));
    }


    public Azure() { }


    static private final Random random = new Random();
    
    public @Nonnull String generateToken(int minLength, int maxLength) {
        if( minLength < 0 ) {
            minLength = 0;
        }
        if( maxLength < minLength ) {
            if( minLength == 0 ) {
                return "";
            }
            maxLength = minLength;
        }

        //generate token that meets most password complexity requirements - Uppercase and lowercase letters, digits and special chars
        String token = new String(RandomPasswordGenerator.generatePassword(minLength, maxLength, 2, 2, 2));
        return token;
    }
  
    @Override
    public @Nonnull String getCloudName() {
        return "Azure";
    }

    @Override
    public @Nonnull ContextRequirements getContextRequirements() {
        return new ContextRequirements(
                new ContextRequirements.Field("apiKey", "The API Keypair", ContextRequirements.FieldType.KEYPAIR, ContextRequirements.Field.X509, true),
                new ContextRequirements.Field("proxyHost", "Proxy host", ContextRequirements.FieldType.TEXT, false),
                new ContextRequirements.Field("proxyPort", "Proxy port", ContextRequirements.FieldType.TEXT, false)
        );
    }

    public String getDataCenterId(String regionId){
    	return regionId;
    }


    @Override
    public @Nonnull AzureComputeServices getComputeServices() {
        return new AzureComputeServices(this);
    }

    @Override
    public @Nonnull AzureLocation getDataCenterServices() {
        return new AzureLocation(this);
    }

    public @Nonnull AzurePlatformServices getPlatformServices(){ return new AzurePlatformServices(this);}
    
    @Override
    public @Nonnull AzureNetworkServices getNetworkServices() {
    	//return null;
    	//Not ready yet
    	return new AzureNetworkServices(this);
    }

    private transient String storageEndpoint;

    public @Nullable String getStorageEndpoint() throws CloudException, InternalException {
        if( storageEndpoint == null ) {
            ProviderContext ctx = getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No configuration was set for this request");
            }
            AzureMethod method = new AzureMethod(this);

            Document xml = method.getAsXML(ctx.getAccountNumber(), "/services/storageservices");

            if( xml == null ) {
                throw new CloudException("Unable to identify the blob endpoint");
            }
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            try {
                XPathExpression expr = xpath.compile("(/StorageServices/StorageService/StorageServiceProperties[GeoPrimaryRegion='"+ctx.getRegionId()+"']/Endpoints/Endpoint[contains(.,'.blob.')])[1]");
                storageEndpoint = expr.evaluate(xml).trim();
            } catch (XPathExpressionException e) {
                throw new CloudException("Invalid blob endpoint search expression");
            }

            if( storageEndpoint == null || storageEndpoint.isEmpty())
                storageEndpoint = null;
        }
        return storageEndpoint;
    }

    private transient String storageService;

    public @Nullable String getStorageService() throws CloudException, InternalException {
        if( storageService == null ) {
            ProviderContext ctx = getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No configuration was set for this request");
            }
            AzureMethod method = new AzureMethod(this);

            Document xml = method.getAsXML(ctx.getAccountNumber(), "/services/storageservices");

            if( xml == null ) {
                throw new CloudException("Unable to identify the storage service");
            }
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();

            try {
                XPathExpression expr = xpath.compile("(/StorageServices/StorageService/StorageServiceProperties[GeoPrimaryRegion='"+ctx.getRegionId()+"']/../ServiceName)[1]");
                storageService = expr.evaluate(xml).trim();
            } catch (XPathExpressionException e) {
                throw new CloudException("Failed to find storage service in the current region: " + ctx.getRegionId());
            }

            if( storageService == null || storageService.isEmpty())
                storageService = null;
        }
        return storageService;
    }

    public void createDefaultStorageService() throws CloudException, InternalException {
        ProviderContext ctx = getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No configuration was set for this request");
        }

        String randomSufix = UUID.randomUUID().toString().replace("-", "").substring(0, 13);
        String serviceName = "portalvhds" + randomSufix;
        CreateStorageServiceInputModel createStorageServiceInputModel = new CreateStorageServiceInputModel();
        createStorageServiceInputModel.setServiceName(serviceName);
        createStorageServiceInputModel.setDescription("Implicitly created storage service");
        createStorageServiceInputModel.setLabel(new String(Base64.encodeBase64(randomSufix.getBytes())));
        createStorageServiceInputModel.setLocation(ctx.getRegionId());
        createStorageServiceInputModel.setGeoReplicationEnabled("true");

        String requestId = null;
        AzureMethod azureMethod = new AzureMethod(this);
        try {
            requestId = azureMethod.post("/services/storageservices", createStorageServiceInputModel);
        }
        catch (JAXBException e)
        {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }

        if (requestId != null) {
            int httpCode = azureMethod.getOperationStatus(requestId);
            while (httpCode == -1) {
                try {
                    Thread.sleep(15000L);
                } catch (InterruptedException ignored) {
                }
                httpCode = azureMethod.getOperationStatus(requestId);
            }
            if (httpCode == HttpServletResponse.SC_OK) {
                storageEndpoint = "https://" + serviceName + ".blob.core.windows.net/";
            }
        }
    }

    @Override
    public @Nonnull AzureStorageServices getStorageServices() {
        return new AzureStorageServices(this);
    }
    
    
    @Override
    public @Nonnull String getProviderName() {
        return "Microsoft";
    }

    public @Nonnull String getVMProductsResource() {
        ProviderContext ctx = getContext();
        String value;

        if( ctx == null ) {
            value = null;
        }
        else {
            Properties p = ctx.getCustomProperties();

            if( p == null ) {
                value = null;
            }
            else {
                value = p.getProperty("vmproducts");
            }
        }
        if( value == null ) {
            value = System.getProperty("azure.vmproducts");
        }
        if( value == null ) {
            value = "/org/dasein/cloud/azure/vmproducts.json";
        }
        return value;
    }

    public long parseTimestamp(@Nullable String time) throws CloudException {
        if( time == null ) {
            return 0L;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        if( time.length() > 0 ) {
            try {
                return fmt.parse(time).getTime();
            }
            catch( ParseException e ) {
                fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try {
                    return fmt.parse(time).getTime();
                }
                catch( ParseException encore ) {
                    throw new CloudException("Could not parse date: " + time);
                }
            }
        }
        return 0L;
    }

    @Override
    public @Nullable String testContext() {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + Azure.class.getName() + ".testContext()");
        }
        try {
            try {
                AzureMethod method = new AzureMethod(this);
                ProviderContext ctx = getContext();
                
                if( ctx == null ) {
                    logger.error("No context was specified for a context test");
                    return null;
                }

               /* logger.debug("--------------Context-------------");
                logger.debug("Account number: "+ctx.getAccountNumber());
                logger.debug("X509 cert: "+new String(ctx.getX509Cert(), "utf-8"));
                logger.debug("X509 key: "+new String(ctx.getX509Key(), "utf-8"));
                logger.debug("--------------Context-------------");
                */
                if( method.getAsStream(ctx.getAccountNumber(), "/locations") == null ) {
                    logger.warn("Account number was invalid for context test: " + ctx.getAccountNumber());
                    return null;
                }
                if( logger.isDebugEnabled() ) {
                    logger.debug("Valid account: " + ctx.getAccountNumber());
                }
                return ctx.getAccountNumber();
            }
            catch( Exception e ) {
                logger.error("Failed to test context: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + Azure.class.getName() + ".testContext()");
            }
        }
    }

    public HttpClientBuilder getAzureClientBuilder() throws CloudException {
        try {
            HttpClientBuilder builder = HttpClientBuilder.create();
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", new AzureSSLSocketFactory(new AzureX509(this)))
                    .build();
            HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
            builder.setConnectionManager(ccm);
            return builder;
        } catch (Exception e) {
            throw new CloudException(e.getMessage());
        }
    }
}

class RandomPasswordGenerator {
    private static final String ALPHA_CAPS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHA   = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUM     = "0123456789";
    private static final String SPECIAL = "!@#$%^*_=+-/";

    public static char[] generatePassword(int minLen, int maxLen, int noOfCAPSAlpha,
                                          int noOfDigits, int noOfSpecialChars) {
        if(minLen > maxLen)
            throw new IllegalArgumentException("Min. Length > Max. Length!");
        if( (noOfCAPSAlpha + noOfDigits + noOfSpecialChars) > minLen )
            throw new IllegalArgumentException
                    ("Min. Length should be atleast sum of (CAPS, DIGITS, SPL CHARS) Length!");
        Random rnd = new Random();
        int len = rnd.nextInt(maxLen - minLen + 1) + minLen;
        char[] pswd = new char[len];
        int index = 0;
        for (int i = 0; i < noOfCAPSAlpha; i++) {
            index = getNextIndex(rnd, len, pswd);
            pswd[index] = ALPHA_CAPS.charAt(rnd.nextInt(ALPHA_CAPS.length()));
        }
        for (int i = 0; i < noOfDigits; i++) {
            index = getNextIndex(rnd, len, pswd);
            pswd[index] = NUM.charAt(rnd.nextInt(NUM.length()));
        }
        for (int i = 0; i < noOfSpecialChars; i++) {
            index = getNextIndex(rnd, len, pswd);
            pswd[index] = SPECIAL.charAt(rnd.nextInt(SPECIAL.length()));
        }
        for(int i = 0; i < len; i++) {
            if(pswd[i] == 0) {
                pswd[i] = ALPHA.charAt(rnd.nextInt(ALPHA.length()));
            }
        }
        return pswd;
    }

    private static int getNextIndex(Random rnd, int len, char[] pswd) {
        int index = rnd.nextInt(len);
        while(pswd[index = rnd.nextInt(len)] != 0);
        return index;
    }
}