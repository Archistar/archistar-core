package at.ac.ait.archistar.middleware.frontend;

/**
 * TODO: is the path metadata or are objects complete different from files
 */
interface FSFile extends FSObject {

  byte[] getData();
};
