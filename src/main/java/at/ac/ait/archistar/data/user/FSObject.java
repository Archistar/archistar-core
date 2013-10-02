package at.ac.ait.archistar.data.user;

import java.util.Map;

/**
 * this is the base interface for all user-supplied data fragments
 */
public interface FSObject {

  Map<String, String> getMetadata();

  String setMetaData(String key, String value);
  
  String getPath();
};
