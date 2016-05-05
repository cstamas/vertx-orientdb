package org.cstamas.vertx.orientdb;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Orient DB connection options.
 */
public class ConnectionOptions
{
  /**
   * The connection name. Should be URL safe name, and databases and connections are keyed by this property.
   */
  private final String name;

  /**
   * The connection URI.
   */
  private final String uri;

  /**
   * Username to be used with connection.
   */
  private final String username;

  /**
   * Password to be used with connection.
   */
  private final String password;

  /**
   * See {@link OPartitionedDatabasePool}.
   */
  private final int maxPartitionSize;

  /**
   * See {@link OPartitionedDatabasePool}.
   */
  private final int maxPoolSize;

  public ConnectionOptions(final String name,
                           final String uri,
                           final String username,
                           final String password,
                           final int maxPartitionSize,
                           final int maxPoolSize)
  {
    this.name = checkNotNull(name);
    this.uri = checkNotNull(uri);
    this.username = checkNotNull(username);
    this.password = checkNotNull(password);
    this.maxPartitionSize = maxPartitionSize;
    this.maxPoolSize = maxPoolSize;
  }

  public String name() {
    return name;
  }

  public String uri() {
    return uri;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public int maxPartitionSize() {
    return maxPartitionSize;
  }

  public int maxPoolSize() {
    return maxPoolSize;
  }

  public static class Builder
  {
    private static final String ADMIN_USER = "admin";

    private static final String ADMIN_PASSWORD = "admin";

    private final String name;

    private final String uri;

    private String username = ADMIN_USER;

    private String password = ADMIN_PASSWORD;

    private int maxPartitionSize = 64;

    private int maxPoolSize = 64;

    public Builder(final String name, final String uri) {
      this.name = checkNotNull(name);
      this.uri = checkNotNull(uri);
    }

    public Builder setUsernamePassword(final String username, final String password) {
      this.username = username;
      this.password = password;
      return this;
    }

    public Builder setMaxPartitionSize(final int maxPartitionSize) {
      this.maxPartitionSize = maxPartitionSize;
      return this;
    }

    public Builder setMaxPoolSize(final int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
      return this;
    }

    public ConnectionOptions build() {
      return new ConnectionOptions(name, uri, username, password, maxPartitionSize, maxPoolSize);
    }
  }
}
