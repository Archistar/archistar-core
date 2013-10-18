# Control/Data Flow


## Write Operation

The data flow in the write-case is:

 1. User application uses SimpleFileInterface to communicate with Engine
 2. Engine uses MetadataService to gain information on which server under which filenames the encrypted fragments should be stored
 3. CryptoEngine gets the original data, performs encryption and puts encrypted data into the placeholders (provided by MetadataService)
 4. Distributor gets the encrypted data and forwards the request to the different backend servers
 5. Within the backend server the ExecutionHandler is called which forwards the storage operation to StorageServer
 6. StorageServer persists the data

## Read Operation

The data flow for the read-case is similar:

 1. User application uses SimpleFileInterface to communicate with Engine
 2. Engine uses MetadataService to gain informaiton on which server under which filenames the encrypted fragments should be stored
 3. Distributor gets the read request and forwards the information through the BFT network to the backend servers
 4. Within the backend server the ExecutionHandler is called to perform the storage operation through StorageServer
 5. StorageServer retrieves the data (might be a network operation in case of S3) and returns it to the Distributor
 6. After the Distributor has collected 2f+1 results the result is forwarded to the CryptoEngine
 7. CryptoEngine reconstructs the original data
 8. Engine returns the original (unencrypted) data to the caller
