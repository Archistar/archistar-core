![High-Level Component Diagram](https://raw.github.com/archistar/archistar-core/master/docs/architecture/components_rough.png "High-Level Component Diagram")

The components can be structured in three big parts: frontend, middleware and backend.

# Frontend

This package provides a user-interface to client programs. It utilizes a (middleware) Engine instance for communicating with the network. Current client frontends include a FTP server and a disk-based sync utility.

# Middleware

The middleware is responsible for receiving messages from the frondend (via the SimpleUserInterface), splits and encrypts those messages and forwards them to the different backend servers.

## SimpleUserInterface

This is the application interface to the Archistar system. It should provide an easy interface for application developers, thus it provides rather standard get/put/delete/list methods.


## MetadataService

The metadata service is reponsible for mapping the end-user (unencrypted) filenames to (encrypted) blob ids on the the different storage servers. Through this it also defines which servers will be responsible for storing data fragments (== encrypted user data).
  
In addition the MetadataService is responsible for providing metadata for files as well as handle delete operations (as the storage server themselves do not delete data for now) by querying or modifying their data index.

To achieve it's storage operations the MetadataService is utilizing the Distributor component.

Versioning is another feature which would be implemented by this component.

## CryptoEngine

The CryptoEngine is responsible for en/decryption. In the encryption case it is provided the original data as well as placeholders for the resulting encrypted data (otherwise it would not know how many fragments to create). The decryption operation works in the opposite direction: it is provided multiple data fragments and should reconstruct the original data.

## Distributor

The distributor is responsible for distributing the encrypted data fragments to the differnet server (in the write-case) as well as responsible for gathering the different encrypted fragments in the read-case. To achieve error resilence the BFTDistributor utilizes a BFT algorithm.

## ExecutionHandler

Distributor uses a BFT network for sending operations to servers. The servers in turn contain a storage backend (StorageServer) which will be used to persist and query data. The ExecutionHandler interface is an callback interface which forwards requests from the BFT server to the storage system. To minimize requirements on the storage system currenlty only read and write operations are performed. The API was inspired by Key-Value-Stores.

## Serializer

The Serializer converts a end-user FileObject (as provided by the SimpleFile interface) and converts it into a byte array. The deserialize routine performs this vice-versa.

## ServerConfiguration

The ServerConfiguration interface is used to store information about all connected servers (and currently about the used StorageServer backend per BFT server). It is used by the MetadataService as well as by the Distributor to gain information about the current system configuration.

If later versions support dynamic server configurations most of its imlpementation will be at the ServerConfiguration level.

## SecurityMonitor

This is a centralized singleton component where all (assumed) security breaches are reported. It in turn is reponsible for policing and (potentially) escalating a security notification into a system shutdown.

# Backend

The backend package ipmlements active backend servers. They accept commands from the middleware (via the bft network) and forward the ordered requests to the included StorageServer.

## StorageServer

The StorageServer interface represents a storage system where encrypted fragments (BLOBs acutally) will be stored. Currently implementations include Memory, Filesystem or S3-based storage engines.

# Bft

This includes the abstract bft state machine which will be executed within each backend server.

Future releases might move the bft package either into the backend package or into a library of it's own.

# TrustManager

Global trust manager that manages the certificates needed for establishing secure connections between replicas. Future releases might merge the ServerConfiguration object and the TrustManager.
