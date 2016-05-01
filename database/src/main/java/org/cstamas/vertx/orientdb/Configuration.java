package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import io.vertx.core.json.JsonObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration of OrientDB integration. Allows to selectively enable OrientServer (to be able to connect to OrientDB
 * remotely or not, and to set orient DB Home is (if relative, is resolved against process CWD). For server, rest of
 * configuration is expected to come from standard OrientDB facilities read from {@code
 * &lt;orientHome&gt;/config/orientdb-server-config.xml} (as per OrientDB documentation).
 * <p/>
 * Default configuration is created if one is not found (copied from OrientDB distribution) but it is NOT MEANT
 * for production, just for toying.
 *
 * Example configuration value (these are default values):
 * <ttyl>
 * {
 * "serverEnabled" : "true",
 * "orientHome" : "orientdb"
 * }
 * </ttyl>
 *
 * @see <a href="http://orientdb.com/docs/2.1/DB-Server.html">OrientDB Server</a>
 */
public class Configuration
{
  private final boolean serverEnabled;

  private final String orientHome;

  public Configuration(boolean serverEnabled, final String orientHome) {
    this.serverEnabled = serverEnabled;
    this.orientHome = checkNotNull(orientHome);
  }

  public boolean isServerEnabled() {
    return serverEnabled;
  }

  public String getOrientHome() {
    return orientHome;
  }

  public static Configuration fromJsonObject(@Nullable final JsonObject config) {
    boolean serverEnabled = true;
    String orientHome = "orientdb";
    if (config != null) {
      serverEnabled = config.getBoolean("serverEnabled", serverEnabled);
      orientHome = config.getString("orientHome", orientHome);
    }
    return new Configuration(serverEnabled, orientHome);
  }
}
