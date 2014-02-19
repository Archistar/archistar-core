# Archistar [![Build Status](https://travis-ci.org/Archistar/archistar-core.png?branch=master)](https://travis-ci.org/Archistar/archistar-core)

_"hackable secure multi-cloud solution (or at least a prototype)"_

Archistar is a  multi-cloud prototype written during the *tada* Archistar project. The goal is to distribute user data to different (private) clouds in a way that allows no single cloud provider to read or alter an user's data.

## Some background

- secure cloud-based storage
- uses [archistar-bft](https://github.com/archistar/archistar-bft) BFT library for achieving total ordering
- uses [netty.io](http://netty.io) as network transport
- uses [archistar-smc](https://github.com/archistar/archistar-smc) for secret sharing, currently test-cases utilize Sharmir's Secret Sharing
- Multiple backend storage options (memory-only, file-backed, S3-backed)
- TLS authentication and encryption between communication partners -- key validation is not implemented yet
- client-side S3 interface

## Design Goals

- well-written code
- hackability: other projects should be able to use archistar as starting point
- k.i.s.s.

## Running archistar

This example uses the ftp demostrator which consists of four locally started storage servers. Each of the storage servers uses a local filesystem backend for data storage (which can be found under `/var/spool/archistar/test-s3/1..4`).

First start the fake-S3 server frontend:

```bash
$ mvn exec:java -Dexec.mainClass="at.ac.ait.archistar.frontend.ArchistarS3"
[INFO] Scanning for projects...
[INFO] Searching repository for plugin with prefix: 'exec'.
[INFO] ------------------------------------------------------------------------
[INFO] Building archistar
[INFO]    task-segment: [exec:java]
[INFO] ------------------------------------------------------------------------
[INFO] Preparing exec:java
[INFO] No goals needed for project - skipping
[INFO] [exec:java {execution: default-cli}]
[at.ac.ait.archistar.frontend.ArchistarS3.main()] INFO at.ac.ait.archistar.frontend.ArchistarS3 - Starting archistar storage engine
[Thread-7] INFO at.ac.ait.archistar.bft.BftEngine - successful transactions: 0.0
[Thread-7] INFO at.ac.ait.archistar.bft.BftEngine - server: 0 transaction length: 0ms
```

To use the s3cmd program to access the server you'll need to adopt your /etc/hosts configuration file. The s3cmd uses hard-coded amazon server urls so we redirect these to our local server by adding the following lines to `/etc/hosts` (an example configuration file can be found at docs/examples/hosts):

```
127.0.0.1 s3.amazonaws.com
127.0.0.1 s3.localhost
```

s3cmd needs a configuration file, an example file can be found at docs/examples/s3cfg.

Now you can use s3cmd to access the server (note that currently fake_bucket is hard coded, also I already had two files in my bucket):

``` bash
$ s3cmd -c docs/examples/s3cfg ls s3://fake_bucket
2006-02-03 16:41       140   s3://fake_bucket/iptables-rule.txt
2006-02-03 16:41      1151   s3://fake_bucket/link-list.txt

$ echo "testdata" > testfile
$ md5sum testfile
73d643ec3f4beb9020eef0beed440ad0  testfile

$ s3cmd -c docs/examples/s3cfg put testfile s3://fake_bucket/testfile
testfile -> s3://fake_bucket/testfile  [1 of 1]
 9 of 9   100% in    0s    48.76 B/s  done

$ s3cmd -c docs/examples/s3cfg ls s3://fake_bucket
2006-02-03 16:41       140   s3://fake_bucket/iptables-rule.txt
2006-02-03 16:41      1151   s3://fake_bucket/link-list.txt
2006-02-03 16:41         9   s3://fake_bucket/testfile

$ s3cmd -c docs/examples/s3cfg get s3://fake_bucket/testfile testfile.2
s3://fake_bucket/testfile -> testfile.2  [1 of 1]
 9 of 9   100% in    0s   416.49 B/s  done

$ md5sum testfile.2 
73d643ec3f4beb9020eef0beed440ad0  testfile.2
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
