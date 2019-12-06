/**
 * 
 */
package naming;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import storage.Command;
import storage.Storage;

/**
 * @author PIYUSH
 *
 */
public class LeafNode extends Node{
	Storage storage;
	Command command;
//	Map<String, String> storageFiles = new HashMap<>();
	ConcurrentSkipListMap<String, String> storageFiles = new ConcurrentSkipListMap<>();
}
