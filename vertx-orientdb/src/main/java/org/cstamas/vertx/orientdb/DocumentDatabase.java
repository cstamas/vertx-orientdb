package org.cstamas.vertx.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/**
 * OrientDB pooled document database instance.
 */
public interface DocumentDatabase
    extends Database<DocumentDatabase, ODatabaseDocumentTx>
{
}
