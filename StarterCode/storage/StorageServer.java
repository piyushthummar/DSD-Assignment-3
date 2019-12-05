package storage;

import java.io.*;
import java.net.*;
import java.nio.file.Files;

import common.*;
import rmi.*;
import naming.*;
import org.apache.commons.io.FileUtils;
/**
 * Storage server.
 * 
 * <p>
 * Storage servers respond to client file access requests. The files accessible
 * through a storage server are those accessible under a given directory of the
 * local filesystem.
 */
public class StorageServer implements Storage, Command {
	private Skeleton<Storage> storageSkeleton;
	private Storage client_stub;
	private Skeleton<Command> commandSkeleton;
	private Command command_stub;
	private String root;
	private Path[] expectedFiles;

	private Path[] deleteFiles;

	/**
	 * Creates a storage server, given a directory on the local filesystem.
	 * 
	 * @param root
	 *            Directory on the local filesystem. The contents of this directory
	 *            will be accessible through the storage server.
	 * @throws NullPointerException
	 *             If <code>root</code> is <code>null</code>.
	 */
	public StorageServer(File root) throws FileNotFoundException {
		this.root = root.getAbsolutePath();
		expectedFiles = Path.list(root);
		storageSkeleton = new Skeleton<Storage>(Storage.class, this);
		commandSkeleton = new Skeleton<Command>(Command.class, this);
	}

	/**
	 * Starts the storage server and registers it with the given naming server.
	 * 
	 * @param hostname
	 *            The externally-routable hostname of the local host on which the
	 *            storage server is running. This is used to ensure that the stub
	 *            which is provided to the naming server by the <code>start</code>
	 *            method carries the externally visible hostname or address of this
	 *            storage server.
	 * @param naming_server
	 *            Remote interface for the naming server with which the storage
	 *            server is to register.
	 * @throws UnknownHostException
	 *             If a stub cannot be created for the storage server because a
	 *             valid address has not been assigned.
	 * @throws FileNotFoundException
	 *             If the directory with which the server was created does not exist
	 *             or is in fact a file.
	 * @throws RMIException
	 *             If the storage server cannot be started, or if it cannot be
	 *             registered.
	 */
	public synchronized void start(String hostname, Registration naming_server)
			throws RMIException, UnknownHostException, FileNotFoundException {
		storageSkeleton.start();
		commandSkeleton.start();

		this.client_stub = Stub.create(Storage.class, storageSkeleton);
		this.command_stub = Stub.create(Command.class, commandSkeleton);
		deleteFiles = naming_server.register(client_stub, command_stub, expectedFiles);

		for (Path deleteFile : deleteFiles) {
			String deletePath = deleteFile.toString();

			// Converting relative path to local absolute path
			deletePath = deletePath.replace("/", "\\");
			String pathToDeleteFile = this.root + deletePath;

			if (pathToDeleteFile.contains("prune")) {
				int lastIndex = pathToDeleteFile.lastIndexOf("prune");
				String pathToDeleteDirectory = pathToDeleteFile.substring(0, lastIndex) + "prune";
//				System.out.println("-> " + pathToDeleteFile);
//				System.out.println(pathToDeleteDirectory + " *");
				
				File deleteDirectory = new File(pathToDeleteDirectory);
				//reference :- https://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
				try {
					FileUtils.deleteDirectory(new File(pathToDeleteDirectory));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
//				System.out.println(" -- " + pathToDeleteFile);
				
				File fileToDelete = new File(pathToDeleteFile);
				fileToDelete.delete();
			}
		}
	}

	/**
	 * Stops the storage server.
	 * 
	 * <p>
	 * The server should not be restarted.
	 */
	public void stop() {
		storageSkeleton.stop();
		commandSkeleton.stop();
	}

	/**
	 * Called when the storage server has shut down.
	 * 
	 * @param cause
	 *            The cause for the shutdown, if any, or <code>null</code> if the
	 *            server was shut down by the user's request.
	 */
	protected void stopped(Throwable cause) {
	}

	// The following methods are documented in Storage.java.
	@Override
	public synchronized long size(Path file) throws FileNotFoundException {
		if(file == null) {
			throw new NullPointerException("Given path is null");
		}
		File newFile = new File(this.root + file.path);
		if(!newFile.exists()) {
			throw new FileNotFoundException("File does not exists");
		}
		if(newFile.isDirectory()) {
			throw new FileNotFoundException("Given path is a Directory not a file");
		}
		return newFile.length();
	}

	@Override
	public synchronized byte[] read(Path file, long offset, int length) throws FileNotFoundException, IOException {
		if(file == null) {
			throw new NullPointerException("Given path is null");
		}
		File newFile = new File(this.root + file.path);
		if(!newFile.exists()) {
			throw new FileNotFoundException("File does not exists");
		} 
		if(newFile.isDirectory()) {
			throw new FileNotFoundException("Given path is a Directory not a file");
		}
		if(offset < 0 || length < 0) {
			throw new IndexOutOfBoundsException("offset or length is negative");
		}
		if(offset > newFile.length()) {
			throw new IndexOutOfBoundsException("offset greater than or equal to file length");
		}
		if(length > newFile.length()) {
			throw new IndexOutOfBoundsException("given length is greater than file size");
		}
		// reference :-> https://netjs.blogspot.com/2015/11/how-to-convert-file-to-byte-array-java.html
		byte[] result = new byte[(int) newFile.length()];
		
		FileInputStream fileInputStream = new FileInputStream(newFile);
		fileInputStream.read(result,(int) offset, length);
		fileInputStream.close();
		
		return result;
	}

	@Override
	public synchronized void write(Path file, long offset, byte[] data) throws FileNotFoundException, IOException {
		if(file == null) {
			throw new NullPointerException("Given path is null");
		}
		File newFile = new File(this.root + file.path);
		if(!newFile.exists()) {
			throw new FileNotFoundException("File does not exists");
		}
		if(newFile.isDirectory()) {
			throw new FileNotFoundException("Given path is a Directory not a file");
		}
		if(data == null) {
			throw new NullPointerException("Data to write is null");
		}
		if(offset < 0) {
			throw new IndexOutOfBoundsException("offset is negative");
		}
		if(offset > 0) {
			// reference :-> http://www.java2s.com/Tutorials/Java/IO_How_to/write/Append_byte_array_to_a_file.htm
			FileOutputStream fileOutputStream = new FileOutputStream(newFile, true);
			fileOutputStream.write(" ".getBytes()); //can be changed
			fileOutputStream.write(data);
			fileOutputStream.close();
		} else {
			FileOutputStream fileOutputStream = new FileOutputStream(newFile);
			fileOutputStream.write(data,(int) offset, data.length);
			fileOutputStream.close();
		}
		
	}

	// The following methods are documented in Command.java.
	@Override
	public synchronized boolean create(Path file) {
		if(file == null) {
			throw new NullPointerException();
		}

		File newFile = new File(this.root + file.path);
		
		if(file.isRoot()) {
			return false;
		}
		if(newFile.exists()) {
//			System.out.println(file.path+" - file exists" + newFile.getName());
			return false;
		}
		boolean result = false;
		try {
			//reference :-> https://stackoverflow.com/questions/2833853/create-whole-path-automatically-when-writing-to-a-new-file
			newFile.getParentFile().mkdirs();
			result = newFile.createNewFile();
		} catch (IOException e) {
			result = false;
		}
		return result;
	}

	@Override
	public synchronized boolean delete(Path path) {
		if(path == null) {
			throw new NullPointerException("Given path is null");
		}
		if(path.isRoot()) {
			return false;
		}
		File newFile = new File(this.root + path.path);
		if(!newFile.exists()) {
			return false;
		}
		boolean result = false;
		if(newFile.isDirectory()) {
			try {
				// reference :-> https://stackoverflow.com/questions/20281835/how-to-delete-a-folder-with-files-using-java
				FileUtils.deleteDirectory(newFile);
				result = true;
			} catch (IOException e) {
				result = false;
			}
		} else {
			result = newFile.delete();
		}
		return result;
	}
}
