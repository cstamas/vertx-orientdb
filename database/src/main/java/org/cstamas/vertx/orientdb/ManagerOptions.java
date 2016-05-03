package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import io.vertx.core.json.JsonObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Options of OrientDB integration. Allows to selectively enable OrientServer (to be able to connect to OrientDB
 * remotely or not, and to set OrientDB home (if relative, is resolved against process CWD). For server, rest of
 * configuration is expected to come from standard OrientDB facilities, read from {@code
 * &lt;orientHome&gt;/config/orientdb-server-config.xml} (as per OrientDB documentation).
 * <p/>
 * Default configuration is created if one is not found (copied from OrientDB distribution) but it is NOT MEANT
 * for production, just for toying.
 *
 * Example configuration value (these are default values):
 * <ttyl>
 * {
 * "orientHome" : "orientdb",
 * "serverEnabled" : "true"
 * }
 * </ttyl>
 *
 * @see <a href="http://orientdb.com/docs/2.1/DB-Server.html">OrientDB Server</a>
 */
public class ManagerOptions
{
  private final String orientHome;

  private final boolean serverEnabled;

  public ManagerOptions(final String orientHome, boolean serverEnabled) {
    this.orientHome = checkNotNull(orientHome);
    this.serverEnabled = serverEnabled;
  }

  public String getOrientHome() {
    return orientHome;
  }

  public boolean isServerEnabled() {
    return serverEnabled;
  }

  public static ManagerOptions fromJsonObject(@Nullable final JsonObject config) {
    String orientHome = "orientdb";
    boolean serverEnabled = true;
    if (config != null) {
      orientHome = config.getString("orientHome", orientHome);
      serverEnabled = config.getBoolean("serverEnabled", serverEnabled);
    }
    return new ManagerOptions(orientHome, serverEnabled);
  }
}
