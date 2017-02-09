# Vert.x OrientDB Integration

Integrates [OrientDB 2.x](http://orientdb.com/docs/2.2/index.html) with Vert.x. It
supports `plocal`, `remote` and `memory` databases, and OrientGraph for graph/Tinkerpop 2.x
uses.

[![wercker status](https://app.wercker.com/status/ba9343552def99973ea803d929ba7c51/m "wercker status")](https://app.wercker.com/project/bykey/ba9343552def99973ea803d929ba7c51)

[![Maven Central](https://img.shields.io/maven-central/v/org.cstamas.vertx.orientdb/vertx-orientdb.svg)](https://img.shields.io/maven-central/v/org.cstamas.vertx.orientdb/vertx-orientdb.svg)

# Using it

To use the OrientDB in Vert.x, you need to include following dependency to your project:

```
    <dependency>
      <groupId>org.cstamas.vertx.orientdb</groupId>
      <artifactId>database</artifactId>
      <version>4.0.1</version>
    </dependency>

```

To use it in your code, you must perform these steps:
* instantiate manager
* open manager (with large databases this may be lengthy operation)
* create/open a named database instance
* use the database

Both, the database instance and manager implement `io.vertx.core.Closeable`. Closing database closes only the given
database, while closing manager closes all opened databases and the manager itself.

Examples found in the `vertx-orientdb-examples` subproject.

Integration configuration:

```
{
  "orientHome" : "orient",
  "serverEnabled" : false
}
```

The `orientHome` path should point to a directory (if not exists, will be created) where OrientDB Home is. OrientDB
"home" directory is where it's configuration, databases, etc. reside.

The `serverEnabled` boolean sets whether to enable OrientDB Server on startup, hence, allow *incoming remote
connections* to Vert.x managed OrientDB Server or not. When server enabled, the OrientDB server configuration is
searched on path `$orientHome/config/orientdb-server-config.xml` and all the "usual business" applies how OrientDB
configures itself (see OrientDB documentation). If server disabled, the integration still allows to access
local, in-memory or remote databases, but no incoming OrientDB connection (database or console) will be possible.
In that case, databases are placed in `$orientHome/databases` directory.

If server enabled, but no configuration provided, this integration will copy the "default" configuration to it's place
and use that, but that mode is not recommended for production use (configuration is copied from default OrientDB
distribution).

## Branches and building

* master - uses latest OrientDB 2.2.x and Vert.x 3.3

Note: build depends on https://github.com/vert-x3/vertx-codegen/issues/81 as it uses Takari Lifecycle
with JDT compiler! Hence, must use Vert.x 3.3.2+ if you are building this project.


Have fun!  
~t~
