package org.dasein.cloud.azure.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureStorageMethod;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.storage.AbstractBlobStoreSupport;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.FileTransfer;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.Jiterator;
import org.dasein.util.JiteratorPopulator;
import org.dasein.util.PopulatorThread;
import org.dasein.util.uom.storage.*;
import org.dasein.util.uom.storage.Byte;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlobStore extends AbstractBlobStoreSupport {
    static private final Logger logger = Azure.getLogger(BlobStore.class);

    static public final int                                       MAX_BUCKETS     = 100;
    static public final int                                       MAX_OBJECTS     = -1;
    static public final Storage<org.dasein.util.uom.storage.Byte> MAX_OBJECT_SIZE = new Storage<org.dasein.util.uom.storage.Byte>(5000000000L, Storage.BYTE);

    private Azure provider = null;

    public BlobStore(Azure provider) {
        this.provider = provider;
    }

    @Override
    public boolean allowsNestedBuckets() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsRootObjects() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean allowsPublicSharing() throws CloudException, InternalException {
        return false;
    }

    private void commitBlocks(@Nonnull String bucket, @Nonnull String object, @Nonnull Collection<String> blockIds) throws InternalException, CloudException {
        String resource = bucket + "/" + object ;

        TreeMap <String, String> headers = new TreeMap <String, String>();
        TreeMap <String, String> queries = new TreeMap <String, String>();

        queries.put("comp", "blocklist");

        //Create post body
        Document doc = AzureStorageMethod.createDoc();
        Element blockList = doc.createElement("BlockList");

        for(String id: blockIds){
            Element uncommitted = doc.createElement("Uncommitted");
            uncommitted.setTextContent(id);
            blockList.appendChild(uncommitted);
        }
        doc.appendChild(blockList);

        AzureStorageMethod method = new AzureStorageMethod(provider);

        method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, resource, queries, AzureStorageMethod.convertDomToString(doc), headers, true);
    }

    public void copyFile(@Nullable String sourceBucket, @Nonnull String sourceObject, @Nullable String targetBucket, @Nonnull String targetObject) throws InternalException, CloudException {
        logger.debug("ENTER - " + BlobStore.class.getName() + ".copyFile(" + sourceBucket + "," + sourceObject + "," + targetBucket + "," + targetObject + ")");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            String regionId = ctx.getRegionId();

            if( regionId == null ) {
                throw new AzureConfigException("No region ID was specified for this request");
            }
            HashMap<String,String> headers = new HashMap<String,String>();

            headers.put("x-ms-copy-source", "/" + provider.getStorageService() + "/" + sourceBucket + "/" + sourceObject);
            TreeMap <String, String> queryParams = new TreeMap <String, String>();
            AzureStorageMethod method = new AzureStorageMethod(provider);

            method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, targetBucket + "/" + targetObject, queryParams, null, headers, true);

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 30L);

            while( timeout > System.currentTimeMillis() ) {
                try {
                    Blob blob = getObject(targetBucket, targetObject);

                    if( blob != null ) {
                        return;
                    }
                }
                catch( Throwable ignore ) {
                    // ignore
                }
                try { Thread.sleep(60000L); }
                catch( InterruptedException ignore ) { }
            }
        }
        finally {
            logger.debug("EXIT - " + BlobStore.class.getName() + ".copyFile()");
        }
    }

    @Override
    public @Nonnull Blob createBucket(@Nonnull String bucketName, boolean findFreeName) throws InternalException, CloudException {
        logger.debug("ENTER - " + BlobStore.class.getName() + ".createBucket(" + bucketName + "," + findFreeName);
        if (bucketName.contains("/")) {
            throw new OperationNotSupportedException("Nested buckets not supported");
        }

        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            String regionId = ctx.getRegionId();

            if( regionId == null ) {
                throw new AzureConfigException("No region ID was specified for this request");
            }
            TreeMap <String, String> queries = new TreeMap <String, String>();
            AzureStorageMethod method = new AzureStorageMethod(provider);

            queries.put("restype", "container");

            if( findFreeName ) {
                String name = bucketName;
                int idx = 1;

                while( exists(name) ) {
                    name = bucketName + "-" + (idx++);
                }
                bucketName = name;
            }
            method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, bucketName, queries, null, null, true);
            Blob bucket = getBucket(bucketName);

            if( bucket == null ) {
                logger.error("Unable to find newly created bucket: " + bucket);
                throw new CloudException("Unable to find newly created bucket: " + bucket);
            }
            return bucket;
        }
        finally {
            logger.debug("exit - createRootContainer(String)");
        }
    }

    @Override
    public boolean exists(@Nonnull String bucketName) throws InternalException, CloudException {
        TreeMap <String, String> queries = new TreeMap <String, String>();
        AzureStorageMethod method = new AzureStorageMethod(provider);

        queries.put("comp", "list");

        Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET, "", queries, null, null, true);
        NodeList matches = doc.getElementsByTagName("Container");

        if( matches != null ){
            for( int i=0; i<matches.getLength(); i++ ) {
                Node bucket = matches.item(i);

                if( bucket.hasChildNodes() ) {
                    NodeList attributes = bucket.getChildNodes();

                    for( int j=0; j<attributes.getLength(); j++ ) {
                        Node attr = attributes.item(j);

                        if( attr.getNodeName().equalsIgnoreCase("name") && attr.hasChildNodes() && attr.getFirstChild().getNodeValue().trim().equals(bucketName) ) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private @Nonnull Collection<String> getBlocks(@Nonnull String bucket, @Nonnull String object, @Nonnull String blocklistType, @Nonnull String blockTypeTag) throws  InternalException, CloudException{
        TreeMap<String, String> queries = new TreeMap <String, String>();
        ArrayList<String> idList = new ArrayList<String>();
        String resource = bucket + "/" + object;

        queries.put("comp", "blocklist");
        // committed, uncommitted, or all ; default committed
        queries.put("blocklisttype", blocklistType);

        try {

            AzureStorageMethod method = new AzureStorageMethod(provider);

            Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET,
                    resource, queries, null, null, true);

            NodeList matches = doc.getElementsByTagName(blockTypeTag);

            if(matches != null){
                Node block = matches.item(0);
                NodeList blockAttributes = block.getChildNodes();
                for( int i=0; i<blockAttributes.getLength(); i++ ) {
                    Node node = blockAttributes.item(i);
                    if(node.getNodeType() == Node.TEXT_NODE) continue;
                    if(!node.getNodeName().equals("Block")) continue;
                    NodeList attributes = node.getChildNodes();
                    for( int j=0; j<attributes.getLength(); j++ ) {
                        Node attribute = attributes.item(j);
                        if( attribute.getNodeName().equalsIgnoreCase("Name") ) {
                            idList.add(attribute.getFirstChild().getNodeValue());
                        }
                    }
                }
            }
        } catch (AzureConfigException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        }
        return idList;
    }

    @Override
    public Blob getBucket(@Nonnull String bucketName) throws InternalException, CloudException {
        for( Blob blob : list(null) ) {
            if( blob.isContainer() ) {
                String name = blob.getBucketName();

                if( name != null && name.equals(bucketName) ) {
                    return blob;
                }
            }
        }
        return null;
    }

    @Override
    public Blob getObject(@Nullable String bucketName, @Nonnull String objectName) throws InternalException, CloudException {
        if( bucketName == null ) {
            return null;
        }
        for( Blob blob : list(bucketName) ) {
            String name = blob.getObjectName();

            if( name != null && name.equals(objectName) ) {
                return blob;
            }
        }
        return null;
    }

    @Override
    public @Nullable Storage<org.dasein.util.uom.storage.Byte> getObjectSize(@Nullable String bucket, @Nullable String object) throws InternalException, CloudException {
        String resource = bucket + "/" + object;

        AzureStorageMethod method = new AzureStorageMethod(provider);
        String blobProperty = "Content-Length";

        String result = method.getBlobProperty(AzureStorageMethod.Storage_OPERATION_GET, resource, new HashMap<String, String>(), null, null, true, blobProperty);

        if( result != null ) {
            return new Storage<org.dasein.util.uom.storage.Byte>(Long.valueOf(result), Storage.BYTE);
        }
        return null;
    }

    @Override
    public int getMaxBuckets() throws CloudException, InternalException {
        return MAX_BUCKETS;
    }

    @Override
    protected void get(@Nullable String bucket, @Nonnull String object, @Nonnull File toFile, @Nullable FileTransfer transfer) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + BlobStore.class.getName() + ".get(" + bucket + "," + object + "," + toFile + "," + transfer + ")");
        }
        try {
            if( bucket == null ) {
                throw new CloudException("No bucket was specified");
            }
            StringBuilder resource = new StringBuilder();

            resource.append(bucket);
            resource.append("/");
            resource.append(object);

            AzureStorageMethod method = new AzureStorageMethod(provider);

            InputStream input = method.getAsStream(AzureStorageMethod.Storage_OPERATION_GET, resource.toString(), new HashMap<String, String>(), null, null, true);

            if( input == null ) {
                throw new CloudException("No such file: " + bucket + "/" + object);
            }
            try {
                copy(input, new FileOutputStream(toFile), transfer);
            }
            catch( FileNotFoundException e ) {
                logger.error("Could not find target file to fetch to " + toFile + ": " + e.getMessage());
                throw new InternalException(e);
            }
            catch( IOException e ) {
                logger.error("Could not fetch file to " + toFile + ": " + e.getMessage());
                throw new CloudException(e);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + BlobStore.class.getName() + ".get()");
            }
        }
    }

    @Override
    public Storage<org.dasein.util.uom.storage.Byte> getMaxObjectSize() {
        return MAX_OBJECT_SIZE;
    }

    @Override
    public int getMaxObjectsPerBucket() throws CloudException, InternalException {
        return MAX_OBJECTS;
    }

    @Override
    public @Nonnull String getProviderTermForBucket(@Nonnull Locale locale) {
        return "bucket";
    }

    @Override
    public @Nonnull String getProviderTermForObject(@Nonnull Locale locale) {
        return "object";
    }

    @Override
    public boolean isPublic(@Nullable String bucket, @Nullable String object) throws CloudException, InternalException {
        AzureStorageMethod method = new AzureStorageMethod(provider);
        TreeMap <String, String> queries = new TreeMap<String,String>();
        String resource;

        if( object != null ) {
            if( bucket == null ) {
                return false;
            }
            resource = bucket + "/" + object;
        }
        else if( bucket == null ) {
            return false;
        }
        else {
            queries.put("restype", "container");
            resource = bucket;
        }

        InputStream input = method.getAsStream(AzureStorageMethod.Storage_OPERATION_GET, resource, queries, null, null, false);

        return (input != null);
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + BlobStore.class.getName() + ".isSubscribed()");
        }
        try {
            return (provider.getStorageService() != null);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + BlobStore.class.getName() + ".isSubscribed()");
            }
        }
    }

    @Override
    public @Nonnull Collection<Blob> list(final @Nullable String bucket) throws CloudException, InternalException {
        final ProviderContext ctx = provider.getContext();
        PopulatorThread <Blob> populator;

        if( ctx == null ) {
            throw new CloudException("No context was specified for this request");
        }
        final String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new CloudException("No region ID was specified");
        }
        provider.hold();
        populator = new PopulatorThread<Blob>(new JiteratorPopulator<Blob>() {
            public void populate(@Nonnull Jiterator<Blob> iterator) throws CloudException, InternalException {
                try {
                    list(regionId, bucket, iterator);
                }
                finally {
                    provider.release();
                }
            }
        });
        populator.populate();
        return populator.getResult();
    }

    private void list(@Nonnull String regionId, @Nullable String bucket, @Nonnull Jiterator<Blob> iterator) throws CloudException, InternalException {
        if( bucket == null ) {
            loadBuckets(regionId, iterator);
        }
        else {
            loadObjects(regionId, bucket, iterator);
        }
    }

    private void loadBuckets(@Nonnull String regionId, @Nonnull Jiterator<Blob> iterator) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + BlobStore.class.getName() + ".listBuckets()");
        }
        try {
            TreeMap <String, String> queries = new TreeMap <String, String>();
            AzureStorageMethod method = new AzureStorageMethod(provider);

            queries.put("comp", "list");

            Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET, "", queries, null, null, true);
            NodeList matches = doc.getElementsByTagName("Container");

            if( matches != null ){
                for( int i=0; i<matches.getLength(); i++ ) {
                    Blob bucket = toBlob(regionId, matches.item(i), "/", true);

                    if( bucket != null ) {

                        iterator.push(bucket);
                    }
                }
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + BlobStore.class.getName() + ".listBuckets()");
            }
        }
    }

    private void loadObjects(@Nonnull String regionId, @Nonnull String bucket, @Nonnull Jiterator<Blob> iterator) throws CloudException, InternalException {
        TreeMap <String, String> queries = new TreeMap <String, String>();
        AzureStorageMethod method = new AzureStorageMethod(provider);

        queries.put("restype", "container");
        queries.put("comp", "list");


        Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET, bucket, queries, null, null, true);
        if(doc == null) return;

        NodeList matches = doc.getElementsByTagName("Blob");

        if(matches != null){
            for( int i=0; i<matches.getLength(); i++ ) {
                Blob file = toBlob(regionId, matches.item(i), bucket, false);

                if( file != null ) {
                    iterator.push(file);
                }
            }
        }
    }

    @Override
    public void makePublic(@Nonnull String bucket) throws InternalException, CloudException {
        makePublic(bucket, null);
    }

    @Override
    public void makePublic(@Nullable String bucket, @Nullable String object) throws InternalException, CloudException {
        if( bucket == null && object == null ) {
            throw new CloudException("No such object: null/null");
        }
        TreeMap <String, String> queries = new TreeMap <String, String>();
        TreeMap <String, String> headers = new TreeMap <String, String>();
        String resource = (object == null ? bucket : (bucket + "/" + object));
        AzureStorageMethod method = new AzureStorageMethod(provider);

        queries.put("restype", "container");
        queries.put("comp", "acl");

        headers.put("x-ms-blob-public-access","container");

        method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, resource, queries,null,headers, true);
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    @Override
    public void move(@Nullable String sourceBucket, @Nullable String object, @Nullable String targetBucket) throws InternalException, CloudException {
        if( sourceBucket == null ) {
            throw new CloudException("No source bucket was specified");
        }
        if( targetBucket == null ) {
            throw new CloudException("No target bucket was specified");
        }
        if( object == null ) {
            throw new CloudException("No source object was specified");
        }
        copy(sourceBucket, object, targetBucket, object);
        removeObject(sourceBucket, object);
    }

    @Override
    protected void put(@Nullable String bucket, @Nonnull String object, @Nonnull File file) throws CloudException, InternalException {
        if( bucket == null ) {
            throw new CloudException("No bucket was specified");
        }

        try {
            InputStream input;

            try {
                input =  new FileInputStream(file);
            }
            catch( IOException e ) {
                logger.error("Error reading input file " + file + ": " + e.getMessage());
                throw new InternalException(e);
            }

            int fileSize = input.available();

            if( fileSize > (63 * 1024 * 1024) ) {
                putBlocks(bucket, object, input);
            }
            else {
                TreeMap <String, String> queries = new TreeMap <String, String>();
                TreeMap <String, String> headers = new TreeMap <String, String>();
                AzureStorageMethod method = new AzureStorageMethod(provider);
                String resource = bucket + "/" + object ;

                queries.put("timeout", "600");

                headers.put("x-ms-blob-type", "BlockBlob");
                headers.put("content-type", "application/octet-stream");
                method.putWithFile(AzureStorageMethod.Storage_OPERATION_PUT, resource, queries, file, headers, true);
            }
        }
        catch( IOException e ) {
            logger.error("Error uploading file " + file + ": " + e.getMessage());
            throw new CloudException(e);
        }


    }

    @Override
    protected void put(@Nullable String bucket, @Nonnull String object, @Nonnull String content) throws CloudException, InternalException {
        TreeMap <String, String> headers = new TreeMap <String, String>();

        headers.put("x-ms-blob-type", "BlockBlob");
        headers.put("content-type", "application/octet-stream");

        AzureStorageMethod method = new AzureStorageMethod(provider);

        method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, bucket + "/" + object, new HashMap<String, String>(), content, headers, true);
    }

    private void putBlocks(@Nonnull String bucket, @Nonnull String object, @Nonnull InputStream input) throws  InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER - " + BlobStore.class.getName() + ".putBlocks(" + bucket + "," + object + ",<<INPUT STREAM>>)");
        }
        try {
            int basicId = 1000;
            int blockSize = 4 * 1024 * 1024;
            byte[] bytes = new byte[blockSize];
            int read;

            try{
                try {
                    while ((read = input.read(bytes)) != -1) {
                        String blockId = Base64.encodeBase64String(String.valueOf(basicId).getBytes());

                        if( read < blockSize ) {
                            byte [] subArray = Arrays.copyOfRange(bytes, 0, read);

                            putBlocks(bucket, object, subArray, blockId);
                        }
                        else {
                            putBlocks(bucket, object, bytes, blockId);
                        }
                        basicId ++;
                    }
                }
                catch( IOException e ) {
                    throw new CloudException(e);
                }
            }
            finally{
                try { input.close(); }
                catch( Throwable ignore ) { }

                ArrayList<String> blockIds = (ArrayList<String>)getBlocks(object, bucket, "all", "UncommittedBlocks");

                commitBlocks(object, bucket, blockIds);
            }
        }
        finally{
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT - " + BlobStore.class.getName() + ".putBlocks()");
            }
        }
    }

    private void putBlocks(@Nonnull String bucket, @Nonnull String object, @Nonnull byte[] content, @Nonnull String blockId) throws  InternalException, CloudException {
        TreeMap <String, String> queries = new TreeMap <String, String>();
        TreeMap <String, String> headers = new TreeMap <String, String>();
        AzureStorageMethod method = new AzureStorageMethod(provider);
        String resource = bucket + "/" + object;

        queries.put("blockid", blockId);
        queries.put("comp", "block");

        headers.put("x-ms-blob-type", "BlockBlob");
        headers.put("content-type", "text/plain");


        method.putWithBytes(AzureStorageMethod.Storage_OPERATION_PUT, resource, queries, content, headers, true);
    }

    @Override
    public void removeBucket(@Nonnull String bucket) throws CloudException, InternalException {
        TreeMap <String, String> queries = new TreeMap <String, String>();
        AzureStorageMethod method = new AzureStorageMethod(provider);

        queries.put("restype", "container");
        method.invoke(AzureStorageMethod.Storage_OPERATION_DELETE, bucket, queries, null, null, true);
    }

    @Override
    public void removeObject(@Nullable String bucket, @Nonnull String name) throws CloudException, InternalException {
        if( bucket == null ) {
            throw new CloudException("No bucket was specified for this request");
        }
        AzureStorageMethod method = new AzureStorageMethod(provider);
        String resource = bucket + "/" + name;

        method.invoke(AzureStorageMethod.Storage_OPERATION_DELETE, resource, new HashMap<String, String>(), null, null, true);
    }

    @Override
    public @Nonnull String renameBucket(@Nonnull String oldName, @Nonnull String newName, boolean findFreeName) throws CloudException, InternalException {
        Blob bucket = createBucket(newName, findFreeName);

        for( Blob file : list(oldName) ) {
            int retries = 10;

            while( true ) {
                retries--;
                try {
                    move(oldName, file.getObjectName(), bucket.getBucketName());
                    break;
                }
                catch( CloudException e ) {
                    if( retries < 1 ) {
                        throw e;
                    }
                }
                try { Thread.sleep(retries * 10000L); }
                catch( InterruptedException ignore ) { }
            }
        }
        boolean ok = true;
        for( Blob file : list(oldName ) ) {
            if( file != null ) {
                ok = false;
            }
        }
        if( ok ) {
            removeBucket(oldName);
        }
        return newName;
    }

    @Override
    public void renameObject(@Nullable String bucket, @Nonnull String object, @Nonnull String newName) throws CloudException, InternalException {
        if( bucket == null ) {
            throw new CloudException("No bucket was specified");
        }
        copy(bucket, object, bucket, newName);
        removeObject(bucket, object);
    }


    @Override
    public @Nonnull Blob upload(@Nonnull File source, @Nullable String bucket, @Nonnull String fileName) throws CloudException, InternalException {
        if( bucket == null ) {
            throw new OperationNotSupportedException("Root objects not supported in cloud");
        }
        if( !exists(bucket) ) {
            createBucket(bucket, false);
        }
        put(bucket, fileName, source);
        return getObject(bucket, fileName);
    }

    @Override
    public @Nonnull NameRules getBucketNameRules() throws CloudException, InternalException {
        return NameRules.getInstance(1, 255, false, true, true, new char[] { '-', '.' });
    }

    @Override
    public @Nonnull NameRules getObjectNameRules() throws CloudException, InternalException {
        return NameRules.getInstance(1, 255, false, true, true, new char[] { '-', '.', ',', '#', '+' });
    }

    private @Nullable Blob toBlob(@Nonnull String regionId, @Nullable Node node, @Nonnull String bucket, boolean isContainer) {
        if( node == null ) {
            return null;
        }
        NodeList attributes = node.getChildNodes();
        String object = null, location = null;
        long size = -1L, creationDate = 0L;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeType() == Node.TEXT_NODE ) {
                continue;
            }
            String aname = attribute.getNodeName().toLowerCase();
            String value;

            if( attribute.getChildNodes().getLength() > 0 ) {
                value = attribute.getFirstChild().getNodeValue();
            }
            else {
                continue;
            }
            if( aname.equalsIgnoreCase("Name") ) {
                object = value;
            }
            else if( aname.equalsIgnoreCase("Url") ) {
                location = value;
            }
            else if( aname.equalsIgnoreCase("Properties") ) {
                NodeList propertyAttributes  = attribute.getChildNodes();

                for(int j=0;j<propertyAttributes.getLength();j++ ){
                    Node property = propertyAttributes.item(j);
                    String propertyName = property.getNodeName();
                    String propertyValue;

                    if( property.getChildNodes().getLength() > 0 ) {
                        propertyValue = property.getFirstChild().getNodeValue();
                    }
                    else{
                        continue;
                    }

                    if( propertyName.equalsIgnoreCase("Content-Length") ) {
                        size = Long.valueOf(propertyValue);
                    }
                    else if( propertyName.equalsIgnoreCase("Last-Modified") ) {
                        String RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
                        DateFormat rfc1123Format = new SimpleDateFormat(RFC1123_PATTERN);

                        rfc1123Format.setTimeZone(TimeZone.getTimeZone("GMT"));
                        try {
                            creationDate = rfc1123Format.parse(propertyValue).getTime();
                        }
                        catch (ParseException e) {
                            logger.warn("Invalid date: " + propertyValue);
                        }
                    }
                }
            }
        }
        if( isContainer ) {
            return Blob.getInstance(regionId, location, object, creationDate);
        }
        else {
            return Blob.getInstance(regionId, location, bucket, object, creationDate, new Storage<Byte>(size, Storage.BYTE));
        }
    }
}
