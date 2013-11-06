# Archistar [![Build Status](https://travis-ci.org/Archistar/archistar-core.png?branch=master)](https://travis-ci.org/Archistar/archistar-core)

_"hackable secure multi-cloud solution (or at least a prototype)"_

Archistar is a  multi-cloud prototype written during the *tada* Archistar project. The goal is to distribute user data to different (private) clouds in a way that allows no single cloud provider to read or alter an user's data.

## Some background

- secure cloud-based storage
- BFT network for total ordering
- uses [archistar-smc](https://github.org/archistar/archistar-smc) for secret sharing, currently test-cases utilize Sharmir's Secret Sharing
- Multiple backend storage options (memory-only, file-backed, S3-backed)
- TLS authentication and encryption between communication partners -- key validation is not implemented yet

## Design Goals

- well-written code
- hackability: other projects should be able to use archistar as starting point
- k.i.s.s.

## Running archistar

This example uses the ftp demostrator which consists of four locally started storage servers. Each of the storage servers uses a local filesystem backend for data storage (which can be found under `/tmp/test-ftp-filesystem/1..4`).

First start the fake-FTP server frontend:

```bash
$ mvn compile
   ...
$ mvn exec:java -Dexec.mainClass="at.ac.ait.archistar.bin.ArchistarFTP"

[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building archistar 1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] >>> exec-maven-plugin:1.2.1:java (default-cli) @ archistar >>>
[INFO] 
[INFO] <<< exec-maven-plugin:1.2.1:java (default-cli) @ archistar <<<
[INFO] 
[INFO] --- exec-maven-plugin:1.2.1:java (default-cli) @ archistar ---
[at.ac.ait.archistar.bin.ArchistarFTP.main()] INFO at.ac.ait.archistar.bin.ArchistarFTP - Starting archistar storage engine
[Thread-6] INFO at.ac.ait.archistar.bft.ozymandias.server.OzymandiasServer - successful transactions: 0.0
[Thread-6] INFO at.ac.ait.archistar.bft.ozymandias.server.OzymandiasServer - server: 0 transaction length: 0ms
[at.ac.ait.archistar.bin.ArchistarFTP.main()] WARN at.ac.ait.archistar.metadata.SimpleMetadataService - creating and syncing a new database
[at.ac.ait.archistar.bin.ArchistarFTP.main()] INFO at.ac.ait.archistar.bin.ArchistarFTP - Starting FTP server on port 30022
[Thread-9] INFO org.mockftpserver.stub.StubFtpServer - Starting the server on port 30022
```

Now you can use a standard ftp program to access the server. Please note that there's currenlty no user authentication so you can pass anything you want as username and password:

```bash
$ ftp localhost 30022
Connected to localhost (127.0.0.1).
220 Service ready for new user. (MockFtpServer 2.4; see http://mockftpserver.sourceforge.net)
Name (localhost:andy): 
331 User name okay, need password.
Password:
501 Syntax error in parameters or arguments.
Login failed.
Remote system type is "WINDOWS".
ftp> ls
227 Entering Passive Mode (127,0,0,1,163,241).
202
ftp> put /etc/passwd tmp
local: /etc/passwd remote: tmp
227 Entering Passive Mode (127,0,0,1,218,64).
150 File status okay; about to open data connection.
226 Closing data connection. Requested file action successful.
2508 bytes sent in 9.5e-05 secs (26400.00 Kbytes/sec)
ftp> get tmp some-file.txt
local: some-file.txt remote: tmp
227 Entering Passive Mode (127,0,0,1,169,160).
150 File status okay; about to open data connection.
226 Closing data connection. Requested file action successful.
2508 bytes received in 0.103 secs (24.27 Kbytes/sec)
ftp> ls
227 Entering Passive Mode (127,0,0,1,204,163).
202 tmp
ftp> 
```

## Developing archistar

0. [read the documentation](docs/overview.md)
1. fork it
2. work on your new feature
3. run the testcases with `mvn test`
4. send me a pull request

## Contributors

- Andreas Happe, AIT <andreashappe@snikt.net>
- Christian Hanser, TU Graz
- Daniel Slamanig, TU Graz
- Konrad Lanz, TU Graz
- Peter Lipp, TU Graz
- Thomas Loruenser, AIT <thomas.loruenster@ait.ac.at>

## License

This project is licensed under the GPLv2. If you want to use Archistar under a different license please contact Thomas Loruenser <thomas.loruenser@ait.ac.at>.
