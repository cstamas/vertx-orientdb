package org.cstamas.vertx.orientdb;

import javax.annotation.Nullable;

import io.vertx.core.json.JsonObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OrientDB configuration. Sets do you enable OrientServer (to be able to connect to OrientDB remotely or not, and
 * where orient DB Home is (if relative, is resolved against process CWD). The rest comes from standard OrientDB
 * configuration facilities (if server enabled).
 *
 * <ttyl>
 * {
 * "serverEnabled" : "true",
 * "orientHome" : "orientdb"
 * }
 * </ttyl>
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
