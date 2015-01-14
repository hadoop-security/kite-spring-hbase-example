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

Download Tomcat:

```bash
wget http://apache.mesi.com.ar/tomcat/tomcat-7/v7.0.57/bin/apache-tomcat-7.0.57.tar.gz
tar -zxf apache-tomcat-7.0.57.tar.gz
```

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

Configuration and Setup
=========================

Before building and deploying the application you need to configure your HBase
cluster with the necessary security settings. In particular, you need to:

1. Enable Kerberos authentication for Hadoop (HDFS and YARN)
2. Enable Kerberos authentication for HBase
3. Enable HBase authorization
4. Create a Kerberos principal to perform HBase admin functions:

        kadmin
        addprinc hbase
        quit

5. Create a Kerberos principal for the application and export it to a keytab:

        kadmin
        addprinc web-page-snapshots
        ktadd -k app.keytab web-page-snapshots
        quit

6. Place the app.keytab file in the home directory of the user that will run
the app.
7. Grant the application principal create table permissions:

        kinit hbase
        hbase shell
        grant 'web-page-snapshots', 'RWXCA'
        quit

8. Create the HBase datasets:

        kinit -kt ~/app.keytab web-page-snapshots
        export KITE_USER_CLASSPATH=$HBASE_CONF_DIR
        kite-tools-cdh5-0.17.1/bin/kite-dataset create dataset:hbase:<ZK HOSTS>:<ZK PORT>/webpagesnapshots.WebPageSnapshotModel -s src/main/avro/hbase-models/WebPageSnapshotModel.avsc
        kite-tools-cdh5-0.17.1/bin/kite-dataset create dataset:hbase:<ZK HOSTS>:<ZK PORT>/webpageredirects.WebPageRedirectModel -s src/main/avro/hbase-models/WebPageRedirectModel.avsc

    Replace `<ZK HOSTS>` with the comma seperated list of ZooKeeper server
    hostnames and `<ZK PORT>` with the ZooKeeper port (typically 2181). Make sure
    `HBASE_CONF_DIR` is set to the location of your HBase client configuration
    files.

9. Grant Alice and Bob access to the public tables/columns:

        hbase shell
        grant 'alice', 'RW', 'webpagesnapshots', 'content', 'public'
        grant 'alice', 'RW', 'webpagesnapshots', '_s'
        grant 'alice', 'RW', 'webpagesnapshots', 'meta'
        grant 'alice', 'RW', 'webpagesnapshots', 'observable'
        grant 'alice', 'RW', 'webpageredirects'
        grant 'alice', 'RW', 'managed_schemas'
        grant 'bob', 'RW', 'webpagesnapshots', 'content', 'public'
        grant 'bob', 'RW', 'webpagesnapshots', '_s'
        grant 'bob', 'RW', 'webpagesnapshots', 'meta'
        grant 'bob', 'RW', 'webpagesnapshots', 'observable'
        grant 'bob', 'RW', 'webpageredirects'
        grant 'bob', 'RW', 'managed_schemas'
        quit

10. Grant Alice and Bob access to their private columns:

        hbase shell
        grant 'alice', 'RW', 'webpagesnapshots', 'content', 'alice'
        grant 'bob', 'RW', 'webpagesnapshots', 'content', 'bob'
        quit

11. Edit `~/apache-tomcat-7.0.57/conf/tomcat-users.xml` and add the following
before the `</tomcat-users>` closing tag:

          <user name="alice" password="secret" roles="user" />
          <user name="bob"  password="secret" roles="user"  />

12. Create `~/apache-tomcat-7.0.57/bin/setenv.sh` with the following content:

        #!/bin/bash
        
        export HADOOP_CONF_DIR=/etc/hadoop/conf
        export HBASE_CONF_DIR=/etc/hbase/conf
        export CLASSPATH=${HADOOP_CONF_DIR}:${HBASE_CONF_DIR}

    Replace `/etc/hadoop/conf` and `/etc/hbase/conf` with your Hadoop and HBase
    configuration directories if you use another location.

13. Edit `src/main/resources/hbase-prod.properties` and set the following
values:

        hbase.zk.host=<ZK HOSTS>
        hbase.zk.port=<ZK PORT>
        application.kerberos.principal=web-page-snapshots
        application.kerberos.keytab=/home/<USER>/app.keytab

    Replace `<ZK HOSTS>` with the comma seperated list of ZooKeeper server
    hostnames and `<ZK PORT>` with the ZooKeeper port (typically 2181). Also
    replace `<USER>` with the username that app will be running as.

14. Add the following parameters to `hbase-site.xml` on all of the HBase nodes
to enable user impersonation by the `web-page-snapshots` principal:

        <property>
          <name>hadoop.proxyuser.web-page-snapshots.groups</name>
          <value>*</value>
        </property>
        <property>
          <name>hadoop.proxyuser.web-page-snapshots.hosts</name>
          <value>*</value>
        </property>

15. Restart HBase

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

Running in Production
===========================

Deploy the built WAR file (webapps/ROOT.war) to your production Tomcat instance.

Go the appropriate URL, for example:

http://app.example.com:8080/home

Once there, you can take snapshots, and view older snapshots of web pages. By
default, the contents of snapshots will be made private and only visable to the
user who made the snapshot. You can check the "Make snapshot public?" checkbox
before taking a snapshot if you want a public snapshot.

When viewing a snapshot, you can always see the metadata. If you don't have
permissions to see the content, you'll see the message "Insufficient
privilleges to view snapshot" in the content pane of the page.

Running in Development Mode
===========================

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

Note: HBase security is not enabled when running in development mode. You
need to deploy to a cluster to test the security features.
