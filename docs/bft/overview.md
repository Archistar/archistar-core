# BFT / Distributor / Ozzymandias

## Overview

- we're using BFT as message transport layer
- BFT provides total ordered multicast and through that consensus between replicas (servers)
- based upon [PBFT](http://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=2&cad=rja&ved=0CDoQFjAB&url=http%3A%2F%2Fpmg.csail.mit.edu%2Fpapers%2Fosdi99.pdf&ei=o55iUqGuFYrdtAbdwIHYAQ&usg=AFQjCNFd4C-1HEEfle_N2jBAoK74B9A1gg&bvm=bv.54934254,d.Yms). If you have only time to read one paper, please read [Aardvark](http://www.cs.utexas.edu/~aclement/aardvark-tr.pdf)
- not optimized for performance (yet)
- message-based, uses [netty.io](http://netty.io) for message transport
- *Distributor* is the Archistar server-side component (which in turn is a client within the BFT network).
- *Ozzymandias* ([*"My name is Ozymandias, king of kings: Look on my works, ye Mighty, and despair!"*](http://en.wikipedia.org/wiki/Ozymandias)) is the internal name for the BFT-Network. Thus OzzymandiasServer and OzzymandiasClient.
- An operation-in-progress is called *Transaction*
- The *ServerServerCommunication* class is a 'virtual' client which is instantiated at each server. The servers use this to send/broadcast commands to all replicas.

## Assumptions / Changes vs. PBFT

- we use TLS for server authentication as well as for verifying message authenticity -- BUT we're currently bypassing identity verification (look into TrustManager and SecureKeyStore if you want to improve this situation). So we're suffering the performance hit (which makes our prototype's performance comparable to full-blown solutions) but are not reaping MAC benefits
- initial data transfer from the client to the replicas differs from BFT: as we are planing of implementing a secret-sharing network passing all data to one replica doesn't make too much sense security-wise. This also means that currenly byzantine clients can deliver garbage to the replicas: future versions will employ verifiy-able secret sharing to solve this drawback.
- view-change protocol is minimal and untested for now
- no catch-up messages (which would improve performance in case of errors) for now
- no time-bounded state transfer. In case of error we notify the SecurityManager component. Which in turn just shuts down the network

## Implementation notes: Uglyness ahead

- An instance of the *Transaction* class includes all state information for one Operation. Alas this makes this class rather large.
- Transactions are stored in two collections: one TreeMap which orders Transactions by client-id, one TreeMap which uses the BFT-network-internal Id. If we wouldn't care about performance we would just use a in-memory SQL-database to make the queries easier to understand.
- We need those two Collections as the initial communication between client and a replica does not contain the internal transaction-id. Both (transaction-id and client-id) are only known for sure after retrieving a PREPARE command.
- Transaction assembly is somehow split between *OzzymandiasServer*, *OzymandiasCommandHandler* and *Transaction*. This is ugly.
- asynchronous vs synchronous communication patterns: the server are using asynchronous message-passing (good), the clients expect typical UNIX synchronous message patterns (meh.). This leads to clients using a WaitCondition for each sent command/request

