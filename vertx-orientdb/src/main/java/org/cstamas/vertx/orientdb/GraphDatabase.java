package org.cstamas.vertx.orientdb;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * OrientDB pooled graph database instance.
 */
public interface GraphDatabase
    extends Database<GraphDatabase, OrientGraph>
{
}
