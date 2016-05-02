# Vert.x OrientDB Integration

Integrates [OrientDB](http://orientdb.com/docs/2.1/index.html) with Vert.x.

[![wercker status](https://app.wercker.com/status/ba9343552def99973ea803d929ba7c51/m "wercker status")](https://app.wercker.com/project/bykey/ba9343552def99973ea803d929ba7c51)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.cstamas.vertx.orientdb/vertx-orientdb/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.cstamas.vertx.orientdb/vertx-orientdb)

Integration configuration:

```
{
  "orientHome" : "orient",
  "serverEnabled" : true
}
```

The `orientHome` path should point to a directory (if not exists, will be created) where OrientDB Home is. OrientDB
"home" directory is where it's configuration, databases, etc. reside.

The `serverEnabled` boolean sets whether to enable OrientDB Server on startup, hence, allow *incoming remote
connections* to Vert.x manager OrientDB Server or not. When server enabled, the OrientDB servier configuration is
searched for on path `$orientHome/config/orientdb-server-config.xml` and all the "usual" applies how OrientDB
configures itself (see OrientDB documentation). If server disabled, the integration still allows to access
local, in-memory or remote databases, but no incoming OrientDB connection (database or console) will be possible.
In that case, databases are placed in `$orientHome/databases` directory.

If server enabled, but no configuration provided, this integration with copy the "default" configuration to it's place
and use that, but that mode is not recommended for production use (configuration is copied from default OrientDB
distribution).

# Using it

To use the OrientDB in Vert.x, you need to include following dependency to your project:

```
    <dependency>
      <groupId>org.cstamas.vertx.orientdb</groupId>
      <artifactId>database</artifactId>
      <version>1.0.0</version>
    </dependency>

```

To use it in your code, you must perform these steps:
* instantiate manager
* start (open) manager (with large databases this may be lengthy operation)
* create/open a named database instance
* use the database

Both, the database instance and manager implement `io.vertx.core.Closeable`. Closing database closes only the given
database, while closing manager closes all opened databases and the manager.

Once a named database is created, a service is being also published (in flux, just toying with it).

Examples found in the `examples` subproject.


TBD:
* threading: orient DBTx instance heavily relies on ThreadLocals, so thread performing db.acquire (hence blocking)
will be the one binding it. This means something must be done with it, as currently passes in Handler will NOT
run on it's context!
* remote connection still has problems, might be that service is not suited for it?
* too much to enlist here :)

Have fun!
~t~

