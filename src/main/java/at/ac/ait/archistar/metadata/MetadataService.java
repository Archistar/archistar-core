package at.ac.ait.archistar.metadata;

import java.util.Dictionary;
import java.util.Set;

import at.ac.ait.archistar.data.fragments.Fragment;
import at.ac.ait.archistar.data.user.FSObject;

/**
 * The Metadata-Service is responsible for all filesystem/object
 * metadata operations including: file listings, file versions,
 * file deletion.
 *
 * @author andy
 */
public interface MetadataService {

  /** connect and bootstrap to servers */
  int connect();

  /** disconnect from all servers */
  int disconnect();

  /** returns a matching for a given path */
  Set<Fragment> getDistributionFor(String path);

  /** makes sure that everything is synchronized */
  int synchronize();
  
  /** List files in correspondence to a path expression */
  Set<String> list(String path);

  /** removes an object from the index */
  int delete(FSObject obj);

  /** retrieves stat information about an object */
  Dictionary<String, String> stat(String path);
}
