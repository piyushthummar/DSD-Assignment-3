/**
 * 
 */
package naming;

import java.util.HashMap;
import java.util.Map;

import storage.Command;
import storage.Storage;

/**
 * @author PIYUSH
 *
 */
public class LeafNode extends Node{
	Storage storage;
	Command command;
	Map<String, String> storageFiles = new HashMap<>();
}
