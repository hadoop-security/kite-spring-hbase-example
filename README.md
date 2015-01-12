Hadoop Security - Interactive HBase Web Application Case Study
=========================

This case study is built on a modified [example Kite SDK web application]
(https://github.com/kite-sdk/kite-spring-hbase-example) that uses Spring MVC
and HBase.  This application is a web caching app that can be used to fetch web
pages and store their content in a local HBase cluster. The cached web page can
be viewed, and metadata about that page, like size, time to fetch, and outlinks
can be queried.

Download
=========================

Download the source code for this project by cloning the Git repository:

```bash
git clone https://github.com/hadoop-security/kite-spring-hbase-example.git
cd kite-spring-hbase-example
```

Download the Kite SDK CLI tool:

```bash
wget http://central.maven.org/maven2/org/kitesdk/kite-tools-cdh5/0.17.1/kite-tools-cdh5-0.17.1.tar.gz
tar -zxf kite-tools-cdh5-0.17.1.tar.gz
```

Configuration
=========================

Before building and deploying the application you need to configure your
HBase cluster with the necessary security settings. In particular, you need to:

1. Enable Kerberos authentication for Hadoop (HDFS and YARN)
2. Enable Kerberos authentication for HBase
3. Enable HBase table ACLs
4. Create the HBase datasets:
    ```bash
    kite-tools-cdh5-0.17.1/bin/kite-dataset create dataset:hbase:quickstart.cloudera:2181/webpagesnapshots.WebPageSnapshotModel -s src/main/avro/hbase-models/WebPageSnapshotModel.avsc
    kite-tools-cdh5-0.17.1/bin/kite-dataset create dataset:hbase:quickstart.cloudera:2181/webpageredirects.WebPageRedirectModel -s src/main/avro/hbase-models/WebPageRedirectModel.avsc
    ```
5. Grant Alice and Bob access to their columns:
    ```bash
    hbase shell
    grant 'alice', 'RW', 'webpagesnapshots', 'content', 'alice'
    grant 'bob', 'RW', 'webpagesnapshots', 'content', 'bob'
    ```
6. Step six

Building
=========================

There are two build profiles in the application: dev and prod.

The default build profile is dev, and in that mode, it will be built so that an
in-process HBase cluster is launched and configured on startup. That cluster
will re-use the same data directory across restarts, so data remains persistent.
This enables us to quickly build a web application on this framework without
having to install a Hadoop and HBase for dev purposes.

The prod build profile will construct a WAR that won't launch an in-process
HBase cluster on startup. Before deployment, you must modify the configuration 
file src/main/resources/hbase-prod.properties with the appropriate properties.

You can build the production WAR file using the following command:

```bash
mvn -Pprod clean install
```

Running
==========================

To run locally in dev mode, simply run the following maven command, which
launches an in process Tomcat to run the app in (we have pretty high memory
settings since not only is this running Tomcat, but it's also launching an
HDFS and HBase cluster in the app):

```bash
env MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=256m" mvn clean install tomcat7:run
```

Once launched, you can view the web application in your browser by going to
the appropriate URL. For example:

[http://localhost:8080/home](http://localhost:8080/home)

Once there, you can take snapshots, and view older snapshots of web pages.

Running in Production
===========================

Deploy the built WAR file (webapps/ROOT.war) to your production Tomcat instance.
