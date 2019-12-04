package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{
	public String path = null;
    /** Creates a new path which represents the root directory. */
    public Path()
    {
        this.path = "/";
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
    	if(component.contains(":") || component.contains("/") || component.isEmpty()) {
    		throw new IllegalArgumentException("component contains : or / or component is empty");
    	} else {
    		String newPath = null;
    		if(path.isRoot()) {
    			newPath = path.toString() + component;
    		} else {
    			newPath = path.toString() + "/" + component;
    		}
    		
    		this.path = newPath;
    	}
        //throw new UnsupportedOperationException("not implemented");
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if(!path.startsWith("/") || path.contains(":")) {
        	throw new IllegalArgumentException("");
        } else {
        	String[] pathComponents = path.split("/");
        	String shrinkedPath = new String();
        	for(String item : pathComponents) {
        		if(item.equals(null) || item.equals("")) {
        			continue;
        		} else {
        			shrinkedPath += ("/" + item);
        		}
        	}
        	if(shrinkedPath.equals(null) || shrinkedPath.equals("")) {
        		this.path = "/";
        	} else {
            	this.path = shrinkedPath;
        	}
        }
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
    	String[] splittedPath = this.path.split("/");
    	ArrayList<String> pathComponents = new ArrayList<>();
    	
    	for(String item : splittedPath) {
    		if(!item.isEmpty()) {
    			pathComponents.add(item);
    		}
    	}
        Iterator<String> pathIterator = new Iterator<String>() {
			int index = 0;
			@Override
			public String next() {
				if(index >= pathComponents.size()) {
					throw new NoSuchElementException();
				} else {
					return pathComponents.get(index++);
				}
			}
			
			@Override
			public boolean hasNext() {
				if(index < pathComponents.size() && (pathComponents.get(index) != null)) {
					return true;
				} else {
					return false;
				}
			}
			
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
        return pathIterator;
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        if(!directory.exists()) {
        	throw new FileNotFoundException("No such directory exists");
        }
        if(!directory.isDirectory()) {
        	throw new IllegalArgumentException(directory + " is not a directory");
        }
        
        ArrayList<Path> pathList = new ArrayList<>();
        File[] listOfFiles = directory.listFiles();
        ArrayList<File> files = new ArrayList<>();
        
        for(File file : listOfFiles) {
        	files.add(file);
        }
        while(!files.isEmpty()) {
        	File temporaryFile = files.remove(0);
        	if(temporaryFile.isFile()) {
        		pathList.add(new Path(getLocalPath(directory, temporaryFile)));
        	}
        	if(temporaryFile.isDirectory()) {
        		for(File subFile : temporaryFile.listFiles()) {
        			files.add(subFile);
        		}
        	}
        } 
        return pathList.stream().toArray(Path[] :: new);
    }

    public static String getLocalPath(File directory, File temporaryFile) {
    	String relativePath = temporaryFile.getAbsolutePath();
    	relativePath = relativePath.replace(directory.getAbsolutePath(), "");
    	relativePath = relativePath.replace("\\", "/");
    	return relativePath;
    }
    
    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        if(this.path.equals("/")) {
        	return true;
        } else {
        	return false;
        }
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if(this.path.equals("/")) {
        	throw new IllegalArgumentException("");
        } else {
        	String[] pathComponents = this.path.split("/");
        	String parentPath = "";
        	for(int index = 0; index < pathComponents.length - 1; index++) {
        		parentPath += ("/" + pathComponents[index]);
        	}
        	return new Path(parentPath);
        }
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        String[] pathComponents = this.path.split("/");
        int size = pathComponents.length;
        if(this.path.equals("/")) {
        	throw new IllegalArgumentException("");
        } else {
        	return pathComponents[size-1];
        }
        
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
    	
    	String[] subpathComponents = other.toString().split("/");
    	String[] pathComponents = this.path.split("/");
    	 
    	boolean result = true;
    	int count = 0;
    	if(other.isRoot()) {
    		result = true;
    	} else {
    		if(pathComponents.length < subpathComponents.length) {
    			result = false;
    		} else {
    			for(int index = 1; index < subpathComponents.length; index++ ) {
    				if(subpathComponents[index] != "" && pathComponents[index] != "" && subpathComponents[index] != null && pathComponents[index] != null) {
    					if(subpathComponents[index].equals(pathComponents[index]) ) {
//            				System.out.println(pathComponents.length + " -- "+ subpathComponents.length);
//            				System.out.println("inequals=" + subpathComponents[index] +"<->"+pathComponents[index]+".");
            				count++;
            			} else {
            				break;
            			}
    				}			
        		}
        		if(count == 0) {
        			result = false;
        		} else {
//        			System.out.println(count + " : "+this.path + " ---- " + other.path);
        			result = true;
        		}
    		}		
    	}
    	return result;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
    	Path comparePath = (Path) other;
    	if(this.path.equals(comparePath.path)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        int hashCode = 0;
        if(this.path == null) {
        	return -999;
        } else {
        	hashCode = (int)this.path.charAt(0) * 7;
        	return hashCode;
        }
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        return this.path;
    }
}
