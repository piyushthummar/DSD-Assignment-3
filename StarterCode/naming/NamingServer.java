package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rmi.*;
import common.*;
import storage.*;

/** Naming server. 

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
	private Skeleton<Registration> registrationSkeleton;
	private InetSocketAddress registrationAddress;
	
	//Note : Addresses for registration and service are given in NamingStubs.java
	
	private Skeleton<Service> serviceSkeleton;
	private InetSocketAddress serviceAddress;
	
	//For Registration Test of NamingServer
	ArrayList<LeafNode> leavesOfTree = new ArrayList<>();
	
    /** Creates the naming server object.
        <p>
        The naming server is not started.
     */
    public NamingServer()
    {  	
    	//For Storage package Tests
        registrationAddress = new InetSocketAddress(6001);
        registrationSkeleton = new Skeleton<Registration>(Registration.class, this, registrationAddress);
        
        //For Naming Package Tests
        serviceAddress = new InetSocketAddress(6000);
        serviceSkeleton = new Skeleton<Service>(Service.class, this, serviceAddress);
    }

    public Skeleton<Registration> getRegistrationSkeleton() {
		return registrationSkeleton;
	}

	public void setRegistrationSkeleton(Skeleton<Registration> registrationSkeleton) {
		this.registrationSkeleton = registrationSkeleton;
	}

	/** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        registrationSkeleton.start();
        serviceSkeleton.start();
    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        registrationSkeleton.stop();
        serviceSkeleton.stop();
        stopped(null);
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        if(path == null) {
        	throw new NullPointerException("Given path is null");
        }
        if(!doesPathExistsOnNamingServer(path)) {
        	throw new FileNotFoundException("Given directory does not exist");
        }
        for(LeafNode leafNode : leavesOfTree) {
        	for(String itemOfStorageFile : leafNode.storageFiles.keySet()) {
        		if(itemOfStorageFile.equals(path.toString())) {
        			if(leafNode.storageFiles.get(itemOfStorageFile).equals("File")) {
        				return false;
        			} else {
        				return true;
        			}
        		}
        		if(itemOfStorageFile.startsWith(path.toString())) {
        			if(path.isRoot()) {
        				return true;
        			}
        			
        			String subString = itemOfStorageFile.substring(path.toString().length());
        			if(subString.charAt(0) == '/') {
        				return true;
        			}
        		}
        	}
        }

        return true;
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
    	if(directory == null) {
        	throw new NullPointerException("Given path is null");
        }
//    	File newFile =  new File(directory.path);
//        if(!newFile.exists()) {
//        	throw new FileNotFoundException("File is not found in given path");
//        }
//        if(newFile.isFile()) {
//        	throw new FileNotFoundException("Given path is of file not directory to list files");
//        }
        if(!doesPathExistsOnNamingServer(directory)) {
        	throw new FileNotFoundException("Given directory does not exist");
        }
    	
        Set<String> listOfFilesToReturn = new HashSet();
        for(LeafNode leafNode : leavesOfTree) {
        	for(String itemOfStorageFile : leafNode.storageFiles.keySet()) {
        		if(itemOfStorageFile.equals(directory.toString())) {
        			//Given directory itself is in the list of files resides in leafNode
        			throw new FileNotFoundException();
        		}
        		if(itemOfStorageFile.startsWith(directory.toString())) {
        			if(directory.isRoot()) {
        				// if directory is root, add all paths of file and directory after root
        				String[] split = itemOfStorageFile.split("/");
        				listOfFilesToReturn.add(split[1]);
        				continue;
        			}
        			String subString = itemOfStorageFile.substring(directory.toString().length());
        			if(subString.charAt(0) == '/') {
        				//add files after the path of directory is given
        				listOfFilesToReturn.add(subString.substring(1));
        			}
        		}
        	}
        }
        if(listOfFilesToReturn.size() == 0) {
        	throw new FileNotFoundException("No list for given directory");
        }
        return listOfFilesToReturn.stream().toArray(String[] :: new);
    }

    private boolean doesPathExistsOnNamingServer(Path path) {
    	if(path.isRoot()) {
    		return true;
    	}
    	for(LeafNode leafNode : leavesOfTree) {
    		for(String itemFromStorageFiles : leafNode.storageFiles.keySet()) {
    			if(itemFromStorageFiles.startsWith(path.toString())) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if(client_stub == null || command_stub == null) {
        	throw new NullPointerException("client_stub or command_stub is null");
        }
        if(files == null) {
        	throw new NullPointerException("File list is null");
        }
        
        Set<Path> duplicateFiles = new HashSet<>();
        for(Path item : files) {
        	if(item.isRoot()) {
        		return duplicateFiles.stream().toArray(Path[] :: new);
        	}
        }
        
        for(LeafNode leaf : leavesOfTree) {
        	if(leaf.command.equals(command_stub) || leaf.storage.equals(client_stub)) {
        		throw new IllegalStateException("leaf is present in tree");
        	}
        }
      
        //Checking for duplicate files
        
        for(LeafNode leafItem : leavesOfTree) {
        	for(String pathItemOfLeaf : leafItem.storageFiles.keySet()) {
        		for(Path pathItemFromInput : files) {
        			//delete file if it is present in storageFileList of Leaf Node
        			if(pathItemFromInput.toString().equals(pathItemOfLeaf)){
        				duplicateFiles.add(pathItemFromInput);
        			}
        			if(pathItemOfLeaf.startsWith(pathItemFromInput.toString())) {
        				duplicateFiles.add(pathItemFromInput);
        			}
        			if(pathItemFromInput.isRoot()) {
        				return duplicateFiles.stream().toArray(Path[] :: new);
        			}
        		}
        	}
        }
        
        //remove the files that are duplicate in the list
        List<Path> uniqueFiles = new ArrayList<>();
        for(Path item : files) {
        	uniqueFiles.add(item);
        }
        for(Path item : duplicateFiles) {
        	uniqueFiles.remove(item);
        }
        
        files = uniqueFiles.stream().toArray(Path[] :: new);
        
        //Create a new leaf with unique files
        LeafNode leafNode = new LeafNode();
        for(Path item : files) {
        	leafNode.storageFiles.put(item.toString(), "File");
        }
        leafNode.command = command_stub;
        leafNode.storage = client_stub;
        leavesOfTree.add(leafNode);
        
        
        // If duplicate files are found, return the list of files to delete from local storage
        if(duplicateFiles.size() > 0) {
        	return duplicateFiles.stream().toArray(Path[] :: new);
        }
        return null;
    }
}
