package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import io.vertx.core.json.JsonObject;

import static java.util.Objects.requireNonNull;

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
 * "useEventLoop" : "false", // how OrientDB is accessed: in execute blocking block or directly on event loop
 * "serverEnabled" : "true"
 * }
 * </ttyl>
 *
 * Note: accessing OrientDB <b>on eventloop</b> is wrong as it involves IO, but in some cases aggressive caching
 * of OrientDB might actually allow this use. Consider the default setting as <b>hard recommendation</b>, but
 * feel free to experiment with it. Vert.x will yell, if eventloop is being blocked for more than tolerable time,
 * so watch logs!
 *
 * @see <a href="http://orientdb.com/docs/2.2/DB-Server.html">OrientDB Server</a>
 */
public class ManagerOptions
{
  private final String orientHome;

  private final boolean useEventLoop;

  private final boolean serverEnabled;

  public ManagerOptions(final String orientHome, final boolean useEventLoop, boolean serverEnabled) {
    this.orientHome = requireNonNull(orientHome);
    this.useEventLoop = useEventLoop;
    this.serverEnabled = serverEnabled;
  }

  public String getOrientHome() {
    return orientHome;
  }

  public boolean isUseEventLoop() {
    return useEventLoop;
  }

  public boolean isServerEnabled() {
    return serverEnabled;
  }

  public static ManagerOptions fromJsonObject(@Nullable final JsonObject config) {
    String orientHome = "orientdb";
    boolean useEventLoop = false;
    boolean serverEnabled = true;
    if (config != null) {
      orientHome = config.getString("orientHome", orientHome);
      useEventLoop = config.getBoolean("useEventLoop", useEventLoop);
      serverEnabled = config.getBoolean("serverEnabled", serverEnabled);
    }
    return new ManagerOptions(orientHome, useEventLoop, serverEnabled);
  }
}
