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
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.NameRules;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureException;
import org.dasein.cloud.azure.AzureStorageMethod;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.storage.AbstractBlobStoreSupport;
import org.dasein.cloud.storage.Blob;
import org.dasein.cloud.storage.FileTransfer;
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

    private Azure provider = null;

    public BlobStore(Azure provider) {
        this.provider = provider;
    }

	/*
	static private final Logger logger = Logger.getLogger(BlobStore.class);
	
    static public final String SEPARATOR = "xzq";
    
    static private String toAbstract(String nameFromAzure) {
        return nameFromAzure.replace(SEPARATOR, ".");
    }
    
    static private final String toAzure(String abstractName) {
        return abstractName.replaceAll("\\.", SEPARATOR);
    }
    
    @Override
    protected String verifyName(String name, boolean container) throws CloudException {
        if( name == null ) {
            return null;
        }
        name = name.toLowerCase().trim();
        if( name.length() > 255 ) {
            String extra = name.substring(255);
            int idx = extra.indexOf(".");
            
            if( idx > -1 ) {
                throw new CloudException("Azure names are limited to 255 characters.");
            }
            name = name.substring(0,255);
        }
        while( name.indexOf("--") != -1 ) {
            name = name.replaceAll("--", "-");         
        }
        StringBuilder str = new StringBuilder();

        for( int i=0; i<name.length(); i++ ) {
            char c = name.charAt(i);
            
            if( Character.isLetterOrDigit(c) || c == '-' ) {
                if( container ) {
                    if( i == 0 && !Character.isLetter(c) ) {
                        throw new CloudException("Azure container names must start with a letter.");
                    }
                }
                str.append(c);
            }
            else if( c == '.' ) {
                str.append(c);
            }
        }
        name = str.toString();
        
        if( name.length() < 1 ) { 
            return "000";
        }
        while( name.charAt(name.length()-1) == '-' ) {
            name = name.substring(0,name.length()-1);
            if( name.length() < 1 ) { 
                return "000";
            }
        }
        if( name.length() < 1 ) { 
            return "000";
        }
        else if( name.length() == 1 ) {
            name = name + "00";
        }
        else if ( name.length() == 2 ) {
            name = name + "0";
        }
        return name;
    }
	


	@Override
	public String[] mapServiceAction(ServiceAction arg0) {
		// TODO Auto-generated method stub
		return new String[0];
	}

    //Delete a container and related children dir
	@Override
	public void clear(String directoryName) throws CloudException, InternalException {
		logger.debug("enter - clear(String)");		
        try {
           for( CloudStoreObject item : listFiles(directoryName) ) {
        	   if( item.isContainer() ) {
        		   clear(item.getName());
        	   }  
           }
           removeDirectory(directoryName);
        }
        finally {
            logger.debug("exit - clear(String)");
        }
	}
	
	
    public CloudStoreObject copy(CloudStoreObject file, CloudStoreObject toDirectory, String copyName) throws InternalException, CloudException {
        if( file.isContainer() ) {
            CloudStoreObject directory = new CloudStoreObject();
            String pathName;
            int idx;
             
            directory.setContainer(true);
            directory.setCreationDate(new Date());
            directory.setSize(0);
            if( file.getDirectory() != null ) {
                pathName = createDirectory(file.getDirectory() + "." + copyName, true);
            }
            else {
                pathName = createDirectory(copyName, true);
            }
            idx = pathName.lastIndexOf('.');
            String tmp = pathName;
            while( idx > -1 && idx == tmp.length()-1 ) {
                tmp = tmp.substring(0, idx);
                idx = tmp.lastIndexOf('.');
            }
            if( idx == -1 ) {
                directory.setDirectory(null);
                directory.setName(pathName);
            }
            else {
                directory.setDirectory(pathName.substring(0, idx));
                directory.setName(pathName.substring(idx+1));
            }
            for( CloudStoreObject f : listFiles(file.getDirectory()) ) {
                copy(f, directory, f.getName());
            }
            return directory;
        }
        else {
            return copyFile(file, toDirectory, copyName);
        }
    }
    
	// Create a container
	@Override
	public String createDirectory(String abstractDirectoryName, boolean findFreeName) throws InternalException, CloudException {
		logger.debug("enter - createDirectory(String, boolean)");
        try {
            try {
                String[] path = abstractDirectoryName.split("\\.");
                
                if( path == null || path.length < 1 ) {
                    path = new String[] { abstractDirectoryName };
                }
                for( int i=0; i<path.length; i++ ) {
                    String root = null;
                    
                    path[i] = verifyName(path[i], true);
                    if( i > 0 ) {
                        StringBuilder str = new StringBuilder();
                        
                        for( int j=0; j<i; j++ ) {
                            if( j > 0 ) {
                                str.append(".");
                            }
                            str.append(path[j]);
                        }
                        root = str.toString();
                    }
                    if( !exists(root + "." + path[i]) ) {
                        createDirectory(root, path[i]);
                    }
                    else if( i == path.length-1 ) {
                        if( !findFreeName ) {
                            throw new CloudException("The directory " + abstractDirectoryName + " already exists.");
                        }
                        else {
                            String tempName = path[i];
                            String suffix = "-";
                            char c = 'a';
                            
                            while( true ) {
                                path[i] = tempName + suffix + c;
                                if( exists(root + "." + path[i]) ) {
                                    break;
                                }
                                if( c == 'z' ) {
                                    suffix = suffix + "a";
                                    c = 'a';
                                }
                                else {
                                    c++;
                                }
                            }
                            createDirectory(root, path[i]);
                        }
                    }
                }
                return join(".", path);
            } 
            catch( CloudException e ) {
                logger.error(e);
                e.printStackTrace();
                throw e;
            } 
            catch(InternalException e ) {
                logger.error(e);
                e.printStackTrace();
                throw e;
            } 
            catch( RuntimeException e ) {
                logger.error(e);
                e.printStackTrace();
                throw new InternalException(e);
            }
        }
        finally {
            logger.debug("exit - createDirectory(String, boolean)");
        }

	}
	
    private boolean createDirectory(String parent, String name) throws InternalException, CloudException {
        logger.debug("enter - createDirectory(String)");
        try {
            try {
        		String resource = null;
        		if( parent == null || parent.equals("null")) {
        			//Create container
        			createRootContainer(name);
        		}
                else {
                    resource = toAzure(parent + "." + name);
                    createRootContainer(resource);
                }        		
        		return true;                
            }
            catch( RuntimeException e ) {
                logger.error("Could not create directory: " + e.getMessage());
                e.printStackTrace();
                throw new CloudException(e);
            }
        }
        finally {
            logger.debug("exit - createDirectory(String parent, String name)");
        }
    }
    
    private boolean createRootContainer(String containerName) throws InternalException, CloudException {
        logger.debug("enter - createRootContainer(String)");
        try {
            try {
            	String resource = containerName;
        	    TreeMap <String, String> queries = new TreeMap <String, String>();
        	    queries.put("restype", "container");
        	    
        		AzureStorageMethod method = new AzureStorageMethod(provider);
        				
        		method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
        				resource, queries, null, null, true);
        			
        		return true;                
            }
            catch( RuntimeException e ) {
                logger.error("Could not create container: " + e.getMessage());
                e.printStackTrace();
                throw new CloudException(e);
            }
        }
        finally {
            logger.debug("exit - createRootContainer(String)");
        }
    }
	
	
	
	private void createEmptyBlob(String abstractDirecotry, String blobName) throws  IOException, CloudException {
		logger.debug("enter - createEmptyBlob(String, String)");					
		
		try{
			String resource = toAzure(abstractDirecotry) + "/" + blobName ;
			
		    TreeMap <String, String> headers = new TreeMap <String, String>();
		
		    headers.put("x-ms-blob-type", "BlockBlob");	    
		    headers.put("content-type", "application/octet-stream");		    
			try {				
				AzureStorageMethod method = new AzureStorageMethod(provider);				
				method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
				resource, null, null, headers, true);
			
			} catch (AzureConfigException e) {
				e.printStackTrace();
			} catch (InternalException e) {
				e.printStackTrace();
			}
		}finally{
			logger.debug("exit - createEmptyBlob(String, String)");			
		}
	}


	@Override
	public long exists(String abstractDirectoryName, String object, boolean multiPart)throws InternalException, CloudException {
		
        logger.debug("enter - exists(String, String, boolean)");
        try {
            if( !multiPart ) {
                try {                                   
                    if( object == null ) {
                        int idx = abstractDirectoryName.lastIndexOf('.');
                        String dir;
                                                
                        if( idx == -1 ) {
                            dir = null;
                        }
                        else {
                            dir = abstractDirectoryName.substring(0, idx);
                        }
                        
                        String rootContainerName = null;
                        if(dir != null && dir.equals("null")){
                        	rootContainerName = abstractDirectoryName.substring(idx+1);
                        }
                        ArrayList<CloudStoreObject> containers = (ArrayList<CloudStoreObject>) filter(listRootContainers(), dir);
                        if(containers != null){
                            for( CloudStoreObject container : containers ) {
                            	if(dir != null && dir.equals("null")){
                            		//Root container
                            		if(rootContainerName.equals(container.getName())){
                            			return 0L;
                            		}                        		
                            	}
                                if( toAbstract(container.getName()).equals(abstractDirectoryName) ) {
                                    return 0L;
                                }
                            }
                        }
                        return -1L;
                    }
                    else {                    	
                    	return getBlobSize(toAzure(abstractDirectoryName), object);
                    }
                }               
                catch( RuntimeException e ) {
                    logger.error("Could not retrieve file info for " + abstractDirectoryName + "." + object + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new CloudException(e);
                }
            }
            else {
                if( exists(abstractDirectoryName, object + ".properties", false) == -1L ) {
                    return -1L;
                }
                Properties properties = new Properties();
                String str;
                
                try { 
                	InputStream input = getBlob(toAzure(abstractDirectoryName), object + ".properties");
        			if( input == null ) {
        	             throw new IOException("Can not load the property file " + abstractDirectoryName + "." + object + ".properties");
        	        }
        			
        			 try {
                         properties.load(input);
                     }
                     catch( IOException e ) {
                         logger.error("IO error loading file data for " + abstractDirectoryName + "." + object + ": " + e.getMessage());
                         e.printStackTrace();
                         throw new InternalException(e);
                     }
                	
                }
                catch( RuntimeException e ) {
                    logger.error("Could not retrieve file info for " + abstractDirectoryName + "." + object + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new CloudException(e);
                } catch (IOException e) {
					e.printStackTrace();
				}
                
                str = properties.getProperty("length");
                if( str == null ) {
                    return 0L;
                }
                else {
                    return Long.parseLong(str);
                }
            }
        }
        finally {
            logger.debug("exit - exists(String, String, boolean)");
        }
	}
	
	private long getBlobSize(String containerName, String blobName) throws CloudException, InternalException{
		String resource = containerName + "/" + blobName;
		 
		AzureStorageMethod method = new AzureStorageMethod(provider);
		String blobProperty = "Content-Length";
		try{
	 	    String result = method.getBlobProperty(AzureStorageMethod.Storage_OPERATION_GET, 
	 				resource, null, null, null, true, blobProperty);

	 	    if(result != null){
	 	    	return Long.valueOf(result);
	 	    }
	 	    
		}catch ( AzureException e){
			logger.debug(resource + " does not exist!");
			
		}		
 	    return -1L;
	}    

	@Override
	public long getMaxFileSizeInBytes() throws InternalException,CloudException {
		 return 5000000000L;
	}

	@Override
	public String getProviderTermForDirectory(Locale locale) {	
		return "Container";
	}

	@Override
	public String getProviderTermForFile(Locale locale) {

		return "Blob";
	}

	@Override
	public boolean isPublic(String directory, String file) throws CloudException,InternalException {
	
		logger.debug("enter - isPublic(directory,file)");		
		//   String resource = directory+ "/" + file ;
		String resource = directory;
		 
 	    TreeMap <String, String> queries = null;
 	    if(file != null){ 	    	
 	    	resource += "/" + file;
 	    	
 	    }else{ 	    	
 	    	queries = new TreeMap <String, String>(); 	    	
 	    	queries.put("restype", "container");
 	    }
 		
 	    AzureStorageMethod method = new AzureStorageMethod(provider);
 	    
 	    InputStream input = method.getAsStream(AzureStorageMethod.Storage_OPERATION_GET, 
			resource, queries, null, null, false);
 	   
 	    //If can be accessed without authorization, then public
 	    return input != null;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		logger.debug("enter - isSubscribed()");	
	    try{
			
			TreeMap <String, String> queries = new TreeMap <String, String>();
		    queries.put("comp", "list");
		    AzureStorageMethod method = new AzureStorageMethod(provider);
	
			Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET, 
			null, queries, null, null, true);  
			
			NodeList matches = doc.getElementsByTagName("EnumerationResults");
			if(matches != null){
				return true;
			}
	    }
		catch (Exception e){
			return false;
		}
	    return false;		
	}
	
	public Iterable<CloudStoreObject> listRootContainers() throws CloudException, InternalException {
		logger.trace("enter - listContainers()");	
		try {
			ArrayList<CloudStoreObject> list = new ArrayList<CloudStoreObject>();

    	    TreeMap <String, String> queries = new TreeMap <String, String>();
    	    queries.put("comp", "list");		
    	
    		AzureStorageMethod method = new AzureStorageMethod(provider);

    		Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET, 
    		null, queries, null, null, true);  
    		
    		NodeList matches = doc.getElementsByTagName("Container");
    		
    		if(matches != null){
                for( int i=0; i<matches.getLength(); i++ ) {
                    Node node = matches.item(i);                    
                    CloudStoreObject container = toCloudStoreObject(node, null, true);
                    if(container != null){
                    	list.add(container);
                    }                                    
                }
            } 
    		return list;
        }
        catch( RuntimeException e ) {
            logger.error("Could not list containers");
            e.printStackTrace();
            throw new CloudException(e);
        }
	}	

	@Override
	public Iterable<CloudStoreObject> listFiles(String parentDirectory) throws CloudException, InternalException {
        logger.debug("enter - listFiles(String)");
        try {
            PopulatorThread <CloudStoreObject> populator;
            final String dir = parentDirectory;
            
            populator = new PopulatorThread<CloudStoreObject>(new JiteratorPopulator<CloudStoreObject>() {
                public void populate(Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
                    listFiles(dir, iterator);
                }
            });
            populator.populate();
            return populator.getResult();
        }
        finally {
            logger.debug("exit - listFiles(String)");
        }
	}
	
	
    private void listFiles(String abstractDirectoryName, Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
    	loadDirectories(abstractDirectoryName, iterator);
    	if( abstractDirectoryName != null ) {
            loadFiles(abstractDirectoryName, iterator);
    	}
    }
    
    private Collection<CloudStoreObject> filter(Iterable<CloudStoreObject> pageSet, String prefix) {
        ArrayList<CloudStoreObject> filtered = new ArrayList<CloudStoreObject>();
        
        for( CloudStoreObject container : pageSet ) {

            if( container.isContainer()) {
                String name = container.getName();
                
                if( name == null && prefix == null ) {
                    filtered.add(container);
                }
                else if( prefix == null || prefix.equalsIgnoreCase("null")) {
                    if( name.indexOf(SEPARATOR) == -1 ) {
                        filtered.add(container);
                    }
                }
                else if( name != null && name.startsWith(prefix) && !name.equals(prefix) ) {
                    name = name.substring(prefix.length() + SEPARATOR.length());
                    if( name.indexOf(SEPARATOR) == -1 ) {
                    	container.setDirectory(prefix);
                        filtered.add(container);
                    }
                }
            }
        }
        return filtered;
    }
       
	private void loadFiles(String abstractDirectoryName, Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
		try {			
			String resource = null;			
			if( abstractDirectoryName != null ) {		
				resource = toAzure(abstractDirectoryName);
			}

    	    TreeMap <String, String> queries = new TreeMap <String, String>();
    	    queries.put("restype", "container");		
    	    queries.put("comp", "list");   		
    		
    		AzureStorageMethod method = new AzureStorageMethod(provider);

    		Document doc = method.getAsDoc(AzureStorageMethod.Storage_OPERATION_GET, 
    		resource, queries, null, null, true);  
    		if(doc == null) return;  		
    		
    		NodeList matches = doc.getElementsByTagName("Blob");
    		
    		if(matches != null){
                for( int i=0; i<matches.getLength(); i++ ) {
                    Node node = matches.item(i); 
                    
                    CloudStoreObject file = toCloudStoreObject(node, abstractDirectoryName, false);
                    if(file != null){
                    	iterator.push(file);                    	
                    }
                }
            } 
        }
        catch( RuntimeException e ) {
            logger.error("Could not list files in " + abstractDirectoryName + ": " + e.getMessage());
            e.printStackTrace();
            throw new CloudException(e);
        }
	}
	
	private void loadDirectories(String abstractDirectoryName, Jiterator<CloudStoreObject> iterator) throws CloudException, InternalException {
		ArrayList<CloudStoreObject> containers = null;
        try {
            if( abstractDirectoryName != null ) {
               containers = (ArrayList<CloudStoreObject>) filter(listRootContainers(), toAzure(abstractDirectoryName));
            }
            else {
            	containers = (ArrayList<CloudStoreObject>) listRootContainers();
            }
            if(containers != null){
            	for(CloudStoreObject container: containers){
            		iterator.push(container);
            	}
            }
        }
        catch( RuntimeException e ) {
            logger.error("Could not load directories in " + abstractDirectoryName + ": " + e.getMessage());
            e.printStackTrace();
            throw new CloudException(e);
        }
    }
	
	private void commitBlocks(String directory, String fileName, Collection<String> blockIds) throws  IOException, CloudException {
		
		String resource = toAzure(directory) + "/" + fileName ;		
		
		TreeMap <String, String> queries = new TreeMap <String, String>();
		queries.put("comp", "blocklist");
		
	    TreeMap <String, String> headers = new TreeMap <String, String>();
	
		try {			
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
			
			method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
			resource, queries,
			AzureStorageMethod.convertDomToString(doc), headers, true);
				
		} catch (InternalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@SuppressWarnings("deprecation")
	public String createAccessSharedKey(String resource, String signedidentifier){
		
        TreeMap<String, String> signatureMap = new TreeMap<String, String>();
        
        String accesspermission = "rwd";
        signatureMap.put("signedpermissions", accesspermission);
        
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); //2009-02-03T05:26:32.612278
        
        Date start = new Date();
        Date expiry = start;
        int currentHour = start.getHours();
        expiry.setHours(currentHour + 1) ;
        
        signatureMap.put("signedstart", df.format(start));
        signatureMap.put("signedexpiry", df.format(expiry));     
        signatureMap.put("canonicalizedresource", resource);
        signatureMap.put("signedidentifier", signedidentifier);
        signatureMap.put("signedversion", "");
    
        try {
			return AzureStorageMethod.createSignatureString(signatureMap);
		} catch (InternalException e) {
			e.printStackTrace();
		}
		return null; 
	}

	private void createACL(String abstractDirectoryName, String accessPermission) throws InternalException, CloudException{
		
		logger.debug("enter - setUpACL(String, String)");
		
	    TreeMap <String, String> queries = new TreeMap <String, String>();
	    queries.put("restype", "container");		
	    queries.put("comp", "acl");
	
		AzureStorageMethod method = new AzureStorageMethod(provider);
		
		String resource = abstractDirectoryName ;	
		
        //Create post body
        Document doc = AzureStorageMethod.createDoc();
        
        Element signedIdentifiers = doc.createElement("SignedIdentifiers");
        Element signedIdentifier = doc.createElement("SignedIdentifier");
               
        Element id = doc.createElement("Id");       
        Element accessPolicy = doc.createElement("AccessPolicy");
        Element start = doc.createElement("Start");
        Element expiry = doc.createElement("Expiry");
        
        
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //2009-02-03T05:26:32.612278
        
        Date startTime = new Date();
        //Set the expiry date after one year
        Date expiryTime = new Date();
        int currentYear = expiryTime.getYear();
        expiryTime.setYear(currentYear+ 1) ;
        
        //? identify can be any character unique-64-character-value<
        id.setTextContent("blob_acl_policy_" + startTime.getTime());
        start.setTextContent(df.format(startTime));
        expiry.setTextContent(df.format(expiryTime));
        
        String accesspermission = "rwd";
            
        Element permission = doc.createElement("Permission");
        permission.setTextContent(accesspermission);
        
        signedIdentifiers.appendChild(signedIdentifier);
        signedIdentifier.appendChild(id);
        signedIdentifier.appendChild(accessPolicy);
        accessPolicy.appendChild(start);
        accessPolicy.appendChild(expiry);
        accessPolicy.appendChild(permission);
        doc.appendChild(signedIdentifiers);
        
        TreeMap <String, String> headers = new TreeMap <String, String>();
 	    headers.put("x-ms-blob-public-access","container");		

		method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
				resource, queries, 
				AzureStorageMethod.convertDomToString(doc),headers, true);		
	}

	@Override
	public void makePublic(String abstractDirectoryName) throws InternalException,CloudException {
		
		logger.debug("enter - makePublic(String)");
		
		try{		
			String resource = toAzure(abstractDirectoryName) ;	
			
		    TreeMap <String, String> queries = new TreeMap <String, String>();
		    queries.put("restype", "container");		
		    queries.put("comp", "acl");
		
	        TreeMap <String, String> headers = new TreeMap <String, String>();
	 	    headers.put("x-ms-blob-public-access","container");
			
	 	    AzureStorageMethod method = new AzureStorageMethod(provider);
			 
			method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
					resource, queries,null,headers, true);
		
	    }finally {
            logger.debug("exit - removeDirectory(String)");
        }
	}

	@Override
	public void makePublic(String abstractDirectoryName, String fileName) throws InternalException,CloudException {
		//Only container control available
		makePublic(abstractDirectoryName);
	}

	@Override
	public void moveFile(String sourceDirectory, String object, String toDirectory)throws InternalException, CloudException {
		//First copy 
		copyFile(sourceDirectory, object, object, toDirectory);		
		//Then delete old
		this.removeFile(sourceDirectory, object, false);
		
	}

	@Override
	public void removeDirectory(String directoryName) throws CloudException,InternalException {
		
		logger.debug("enter - clear(String)");
	
	    try{
	    	TreeMap <String, String> queries = new TreeMap <String, String>();
		 
		    queries.put("restype", "container");		
		  
			String resource = toAzure(directoryName) ;
			
			AzureStorageMethod method = new AzureStorageMethod(provider);
	
			method.invoke(AzureStorageMethod.Storage_OPERATION_DELETE, 
			resource, queries, null, null, true);
			
	    }finally {
            logger.debug("exit - removeDirectory(String)");
        }

	}

	@Override
	public void removeFile(String directoryName, String name)throws CloudException, InternalException {
		
		logger.debug("enter - removeFile(String)");
	  
		try{			
			String resource = toAzure(directoryName) + "/" + name;
		
			AzureStorageMethod method = new AzureStorageMethod(provider);

			method.invoke(AzureStorageMethod.Storage_OPERATION_DELETE, 
					resource, null, null, null, true);
		}
		finally{
			logger.debug("exit - removeFile(String, String)");
		}
	}

	@Override
	public String renameDirectory(String oldName, String newName, boolean findFreeName) throws CloudException, InternalException {
		logger.debug("enter - renameDirectory(String, String, boolean)");
	  
        try {
            String nd = createDirectory(newName, findFreeName);
            
            // list all the old files, move them
            for( CloudStoreObject f : listFiles(oldName) ) {
                moveFile(oldName, f.getName(), nd);
            }
            // delete the old container/objects
            removeDirectory(oldName);       
            return nd;
        }
        finally {
            logger.debug("exit - renameDirectory(String, String, boolean)");
        }
	}

	
	public void copyFile(String sourceDir, String oldName, String newName, String toDir) throws CloudException, InternalException {
		
		logger.debug("enter - copyFile(String, String, String, String)");
		
		try{
			String source = "/" + this.provider.getContext().getStorageAccountNumber()
		
				 		+ "/" + toAzure(sourceDir) + "/" + oldName;
	    
			TreeMap <String, String> headers = new TreeMap <String, String>();
		    headers.put("x-ms-copy-source",source );		
		  
			String resource = toAzure(toDir) + "/" + newName;
			
			AzureStorageMethod method = new AzureStorageMethod(provider);
	
			method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
			resource, null, null, headers, true);
		}finally{
			logger.debug("enter - copyFile(String, String, String, String)");			
		}
	}

	
	@Override
	public void renameFile(String directory, String oldName, String newName) throws CloudException, InternalException {
		logger.debug("enter - renameFile(String, String, String)");
		//First: copy file with the newName
		copyFile(directory, oldName, newName, directory);
		//Second: delete old file
		removeFile(directory, oldName, false);
	}
	
	@Override
	public void upload(File source, String directory, String fileName, boolean multipart, Encryption encryption) throws CloudException, InternalException {
    	
		if( encryption != null ) {
    	    multipart = true;
    	}		
		try {
            if( multipart ) {
                try {
                    uploadMultipartFile(source, directory, fileName, encryption);
                }
                catch( InterruptedException e ) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new CloudException(e.getMessage());
                }
            }
            else {
                try {
                	put(directory, fileName, source);                	
                }
                catch( IOException e ) {
                    logger.error(e);
                    e.printStackTrace();
                    throw new CloudException(e.getMessage());
                }
            }        
        }
        finally {
            if( encryption != null ) {
                encryption.clear();
            }
        }
	}
	
	private CloudStoreObject toCloudStoreObject(Node node,String directory, boolean isContainer) {
		if( node == null ) {
			return null;
		}  
		CloudStoreObject cloudStoreObj = new CloudStoreObject();
				
		cloudStoreObj.setContainer(isContainer);
		
		cloudStoreObj.setDirectory(directory);
		
		cloudStoreObj.setProviderRegionId(provider.getContext().getRegionId());
	
        NodeList attributes = node.getChildNodes();
        
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String name = attribute.getNodeName().toLowerCase();
            String value;
            
            if( attribute.getChildNodes().getLength() > 0 ) {
                value = attribute.getFirstChild().getNodeValue();                
            }
            else {
               continue;
            }
            if( name.equalsIgnoreCase("Name") ) {
            	cloudStoreObj.setName(value);
            }
            else if( name.equalsIgnoreCase("Url") ) {      
            	cloudStoreObj.setLocation(value);               
            }
            else if( name.equalsIgnoreCase("Properties") ) {            	
           	 	NodeList propertyAttributes  = attribute.getChildNodes();
           	 	for(int j=0;j<propertyAttributes.getLength();j++ ){
           	 		Node property = propertyAttributes.item(j);
           	 		String propertyName = property.getNodeName();              
                    String propertyValue ;
                    if( property.getChildNodes().getLength() > 0 ) {
                    	propertyValue = property.getFirstChild().getNodeValue();
                    }else{
                    	continue;
                    }
           		 
                    if (propertyName.equalsIgnoreCase("Content-Length") ) {      
                    	cloudStoreObj.setSize(Long.valueOf(propertyValue));             
                    }
                    else if (propertyName.equalsIgnoreCase("Last-Modified")) { 
                    
                 		String RFC1123_PATTERN =
                		        "EEE, dd MMM yyyy HH:mm:ss z";
                		DateFormat rfc1123Format =
                		        new SimpleDateFormat(RFC1123_PATTERN);
                		rfc1123Format.setTimeZone(TimeZone.getTimeZone("GMT"));
                				
                		try {
							cloudStoreObj.setCreationDate(rfc1123Format.parse(propertyValue));
						} catch (ParseException e) {
							 logger.warn("Invalid date: " + propertyValue);							 
						}
                    }                    
           	 	}        	
            
           }
        }        
        return cloudStoreObj;
	}

	@Override
	protected void get(String abstractDirectory, String location, File toFile, FileTransfer transfer) throws IOException, CloudException {
		logger.debug("enter - String abstractDirectory, String location, File toFile, FileTransfer transfer)");
		try{
			if(toFile == null){			
				logger.debug("file entered is null");
				throw new IOException("file entered is null");	
			}
			
			InputStream input = this.getBlob(abstractDirectory, location);
			if( input == null ) {
	             throw new IOException("No such file: " + abstractDirectory + "." + location);
	        }
			copy(input, new FileOutputStream(toFile), transfer);
		}finally{
			logger.debug("exit - String abstractDirectory, String location, File toFile, FileTransfer transfer)");
		}		
	}
	
	private InputStream getBlob(String abstractDirectory, String location) throws IOException, CloudException {
		logger.debug("enter - getBlob(String abstractDirectory, String location)");
		
		try{		
				
			StringBuffer resource = new StringBuffer();
			if(abstractDirectory != null){			
				resource.append(toAzure(abstractDirectory));
				resource.append("/");			
			}
			resource.append(location);
	
			try {
				AzureStorageMethod method = new AzureStorageMethod(provider);
				
				InputStream input = method.getAsStream(AzureStorageMethod.Storage_OPERATION_GET, 
						resource.toString(), null, null, null, true);
				return input;
				 
			} catch (InternalException e) {
				e.printStackTrace();
			}
		}finally{
			logger.debug("exit - getBlob(String abstractDirectory, String location)");			
		}
		return null;
	}

	@Override
	protected void put(String container, String blobName, File file) throws  IOException, CloudException {
		logger.debug("enter - put(String, String, File)");
		
		try{
			
			if(blobName == null && file != null){
				blobName = file.getName();			
			}		
			InputStream inStream = null;
			
			if(file != null){
				inStream = new FileInputStream(file);
			}
			int fileSize = inStream.available();
			

			 * The maximum upload size for a block blob is 64 MB.
			 * If your blob is larger than 64 MB, 
			 * you must upload it as a set of blocks.

			if(fileSize > 63 * 1024 * 1024){			
				putBlocks(container, blobName, file);
			}else{
				
				String resource = toAzure(container) + "/" + blobName  ;//container + "/" + blobName ;
				
				TreeMap <String, String> queries = new TreeMap <String, String>();
				
				queries.put("timeout", "600");
				
			    TreeMap <String, String> headers = new TreeMap <String, String>();
			
			    headers.put("x-ms-blob-type", "BlockBlob");
			    headers.put("content-type", "application/octet-stream");
			   
				try {				
					AzureStorageMethod method = new AzureStorageMethod(provider);
					method.putWithFile(AzureStorageMethod.Storage_OPERATION_PUT, 
							resource, queries, file, headers, true);
				
				} catch (AzureConfigException e) {
					e.printStackTrace();
				} catch (InternalException e) {
					e.printStackTrace();
				}			
			}
		}finally{
			logger.debug("exit - put(String, String, File)");
		}
	}	

	private void putBlocks(String container, String blobName, File file) throws  IOException, CloudException {
		logger.debug("enter - putBlocks(String, String, File)");
		try{			
			try {
				if(exists(container, blobName, false) == -1L){
					createEmptyBlob(container,blobName );
				}
			} catch (InternalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			if(file == null){
				logger.debug("file entered is null");
				throw new IOException("file entered is null");				
			}
			if(blobName == null){
				blobName = file.getName();			
			}
			
			InputStream inputStream = new FileInputStream(file);
			
			//To enable encode the blockId
			int basicId = 1000;
			int blockSize = 4 * 1024 * 1024;
			byte[] bytes = new byte[blockSize];
			int read = 0;		
			try{			
				while ((read = inputStream.read(bytes)) != -1) {
	
					String blockId = Base64.encodeBase64String(String.valueOf(basicId).getBytes());
				
	    			if(read < blockSize ){
	    				byte [] subArray = Arrays.copyOfRange(bytes, 0, read);
	    				putBlocks(container, blobName,subArray, blockId);
	    			}else{		    				
	    				putBlocks(container, blobName,bytes, blockId);
	    			}
					basicId ++;
				}
					
			}finally{
				
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}		
				//Commit blocks			
				ArrayList <String> blockIds = (ArrayList<String>) getBlocks(container, blobName, "all", "UncommittedBlocks");
				commitBlocks(container, blobName, blockIds);			
			}
		}finally{
			logger.debug("exit - putBlocks(String, String, File)");
		}
	}

	private Collection<String> getBlocks(String directory, String fileName, String blocklistType, String blockTypeTag) throws  IOException, CloudException{
		
		ArrayList<String> idList = new ArrayList<String>(); 	

		String resource = toAzure(directory) + "/" + fileName;
		TreeMap<String, String> queries = new TreeMap <String, String>();
	    
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
	
	
	private void putBlocks(String directory, String fileName, byte[] content, String blockId) throws  IOException, CloudException {

		String resource = toAzure(directory) + "/" + fileName ;		
		
		TreeMap <String, String> queries = new TreeMap <String, String>();
	    
	    queries.put("blockid", blockId);
	    
	    queries.put("comp", "block");
	    
	    TreeMap <String, String> headers = new TreeMap <String, String>();
	
	    headers.put("x-ms-blob-type", "BlockBlob");
	    headers.put("content-type", "text/plain");
	
		try {
		
			AzureStorageMethod method = new AzureStorageMethod(provider);
			
			method.putWithBytes(AzureStorageMethod.Storage_OPERATION_PUT, 
			resource, queries, content, headers, true);
		
		} catch (AzureConfigException e) {
			e.printStackTrace();
		} catch (InternalException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void put(String directory, String fileName, String content) throws  IOException, CloudException {
	
		logger.debug("enter - put(String, String, File)");
					
		String resource = toAzure(directory) + "/" + fileName ; //directory + "/" + fileName ;
				
	    TreeMap <String, String> headers = new TreeMap <String, String>();
	
	    headers.put("x-ms-blob-type", "BlockBlob");
	    
	    headers.put("content-type", "application/octet-stream");
		
		try {			
			AzureStorageMethod method = new AzureStorageMethod(provider);
			
			method.invoke(AzureStorageMethod.Storage_OPERATION_PUT, 
			resource, null, content, headers, true);
		
		} catch (AzureConfigException e) {
			e.printStackTrace();
		} catch (InternalException e) {
			e.printStackTrace();
		}
	}
	
	class BlocksUploadThread extends Thread{

	    private String directory;
	    String blobName;
	    int startId;
	    int endId;
	    int basicId = 1000;
	    int blockSize ;
	    InputStream inputStream;
	    public BlocksUploadThread(String directory,String blobName, File file,int startId, int endId, int blockSize) throws FileNotFoundException
	    {
	    	this.directory = directory;
	    	//this.file = file;
	    	this.blobName = blobName;
	    	this.startId = startId;
	    	this.endId = endId;	
	    	this.inputStream = new FileInputStream(file);
	    	this.blockSize = blockSize;
	    }
	    
	    @Override
		public void run(){	        	
			try {       	
	    	
	    		byte[] bytes = new byte[blockSize ];
	    		inputStream.skip(startId * blockSize);
	    				    		
	    		int read = 0;
	    		while ((startId < endId) &&((read = inputStream.read(bytes)) != -1)){
				
	    			String blockId = Base64.encodeBase64String(String.valueOf(startId+ basicId).getBytes());
		    		
	    			if(read < blockSize ){
	    				byte [] subArray = Arrays.copyOfRange(bytes, 0, read);
	    				putBlocks(directory, blobName,subArray, blockId);
	    			}else{		    				
	    				putBlocks(directory, blobName,bytes, blockId);
	    			}						
					startId ++ ;
				}
	    		
	        } catch (IOException e) {
	
				e.printStackTrace();
			} catch (CloudException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 	
	    }
	}
    */

    @Override
    protected void get(@Nullable String bucket, @Nonnull String object, @Nonnull File toFile, @Nullable FileTransfer transfer) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void put(@Nullable String bucket, @Nonnull String objectName, @Nonnull File file) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void put(@Nullable String bucketName, @Nonnull String objectName, @Nonnull String content) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean allowsNestedBuckets() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean allowsRootObjects() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean allowsPublicSharing() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Blob createBucket(@Nonnull String bucket, boolean findFreeName) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean exists(@Nonnull String bucket) throws InternalException, CloudException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Blob getBucket(@Nonnull String bucketName) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Blob getObject(@Nullable String bucketName, @Nonnull String objectName) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Storage<Byte> getObjectSize(@Nullable String bucketName, @Nullable String objectName) throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getMaxBuckets() throws CloudException, InternalException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Storage<Byte> getMaxObjectSize() throws InternalException, CloudException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getMaxObjectsPerBucket() throws CloudException, InternalException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public NameRules getBucketNameRules() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public NameRules getObjectNameRules() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String getProviderTermForBucket(@Nonnull Locale locale) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String getProviderTermForObject(@Nonnull Locale locale) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isPublic(@Nullable String bucket, @Nullable String object) throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<Blob> list(@Nullable String bucket) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void makePublic(@Nonnull String bucket) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void makePublic(@Nullable String bucket, @Nonnull String object) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void move(@Nullable String fromBucket, @Nullable String objectName, @Nullable String toBucket) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeBucket(@Nonnull String bucket) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeObject(@Nullable String bucket, @Nonnull String object) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String renameBucket(@Nonnull String oldName, @Nonnull String newName, boolean findFreeName) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renameObject(@Nullable String bucket, @Nonnull String oldName, @Nonnull String newName) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Blob upload(@Nonnull File sourceFile, @Nullable String bucket, @Nonnull String objectName) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
