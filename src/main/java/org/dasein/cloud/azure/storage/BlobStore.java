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
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.NameRules;
import org.dasein.cloud.ProviderContext;
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

	/*

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
	public void moveFile(String sourceDirectory, String object, String toDirectory)throws InternalException, CloudException {
		//First copy 
		copyFile(sourceDirectory, object, object, toDirectory);		
		//Then delete old
		this.removeFile(sourceDirectory, object, false);
		
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
	protected void put(String directory, String fileName, String content) throws  IOException, CloudException {
	

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
            throw new CloudException("No bucket was specified for this request");
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
            throw new CloudException("No bucket was specified for this request");
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
