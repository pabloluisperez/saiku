/*
 * Copyright 2014 OSBI Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.saiku.datasources.connection.encrypt;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;

/**
 * SecureDataSourceFactory.
 */
class SecureDataSourceFactory extends BasicDataSourceFactory {

  private static final String PROP_DEFAULTAUTOCOMMIT = "defaultAutoCommit";
  private static final String PROP_DEFAULTREADONLY = "defaultReadOnly";
  private static final String PROP_DEFAULTTRANSACTIONISOLATION = "defaultTransactionIsolation";
  private static final String PROP_DEFAULTCATALOG = "defaultCatalog";
  private static final String PROP_DRIVERCLASSNAME = "driverClassName";
  private static final String PROP_MAXACTIVE = "maxActive";
  private static final String PROP_MAXIDLE = "maxIdle";
  private static final String PROP_MINIDLE = "minIdle";
  private static final String PROP_INITIALSIZE = "initialSize";
  private static final String PROP_MAXWAIT = "maxWait";
  private static final String PROP_TESTONBORROW = "testOnBorrow";
  private static final String PROP_TESTONRETURN = "testOnReturn";
  private static final String PROP_TIMEBETWEENEVICTIONRUNSMILLIS = "timeBetweenEvictionRunsMillis";
  private static final String PROP_NUMTESTSPEREVICTIONRUN = "numTestsPerEvictionRun";
  private static final String PROP_MINEVICTABLEIDLETIMEMILLIS = "minEvictableIdleTimeMillis";
  private static final String PROP_TESTWHILEIDLE = "testWhileIdle";
  private static final String PROP_PASSWORD = "password";
  private static final String PROP_URL = "url";
  private static final String PROP_USERNAME = "username";
  private static final String PROP_VALIDATIONQUERY = "validationQuery";
  private static final String PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED = "accessToUnderlyingConnectionAllowed";
  private static final String PROP_REMOVEABANDONED = "removeAbandoned";
  private static final String PROP_REMOVEABANDONEDTIMEOUT = "removeAbandonedTimeout";
  private static final String PROP_LOGABANDONED = "logAbandoned";
  private static final String PROP_POOLPREPAREDSTATEMENTS = "poolPreparedStatements";
  private static final String PROP_MAXOPENPREPAREDSTATEMENTS = "maxOpenPreparedStatements";
  private static final String PROP_CONNECTIONPROPERTIES = "connectionProperties";
  //@formatter:off
  private static final String[] ALL_PROPERTIES = { PROP_DEFAULTAUTOCOMMIT,
    PROP_DEFAULTREADONLY,
    PROP_DEFAULTTRANSACTIONISOLATION,
    PROP_DEFAULTCATALOG,
    PROP_DRIVERCLASSNAME,
    PROP_MAXACTIVE,
    PROP_MAXIDLE,
    PROP_MINIDLE,
    PROP_INITIALSIZE,
    PROP_MAXWAIT,
    PROP_TESTONBORROW,
    PROP_TESTONRETURN,
    PROP_TIMEBETWEENEVICTIONRUNSMILLIS,
    PROP_NUMTESTSPEREVICTIONRUN,
    PROP_MINEVICTABLEIDLETIMEMILLIS,
    PROP_TESTWHILEIDLE,
    PROP_PASSWORD,
    PROP_URL,
    PROP_USERNAME,
    PROP_VALIDATIONQUERY,
    PROP_ACCESSTOUNDERLYINGCONNECTIONALLOWED,
    PROP_REMOVEABANDONED,
    PROP_REMOVEABANDONEDTIMEOUT,
    PROP_LOGABANDONED,
    PROP_POOLPREPAREDSTATEMENTS,
    PROP_MAXOPENPREPAREDSTATEMENTS,
    PROP_CONNECTIONPROPERTIES };
  //@formatter:on

  // -------------------------------------------------- ObjectFactory Methods

  /**
   * <p> Create and return a new <code>BasicDataSource</code> instance. If no instance can be created, return
   * <code>null</code> instead. </p>
   *
   * @param obj         The possibly null object containing location or reference information that can be used in
   *                    creating an object
   * @param name        The name of this object relative to <code>nameCtx</code>
   * @param nameCts     The context relative to which the <code>name</code> parameter is specified, or <code>null</code>
   *                    if <code>name</code> is relative to the default initial context
   * @param environment The possibly null environment that is used in creating this object
   * @throws Exception if an exception occurs creating the instance
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public Object getObjectInstance(@Nullable Object obj, Name name, Context nameCtx, Hashtable environment)
      throws Exception {

    // We only know how to deal with <code>javax.naming.Reference</code>s
    // that specify a class name of "javax.sql.DataSource"
    if (obj == null || !(obj instanceof Reference)) {
      return null;
    }
    Reference ref = (Reference) obj;
    if (!"javax.sql.DataSource".equals(ref.getClassName())) {
      return null;
    }

    Properties properties = new Properties();
    for (String propertyName : ALL_PROPERTIES) {
      RefAddr ra = ref.get(propertyName);
      if (ra != null) {
        String propertyValue = ra.getContent().toString();
        properties.setProperty(propertyName, propertyValue);
      }
    }

    decryptPassword(properties);

    return createDataSource(properties);
  }

  private void decryptPassword(@NotNull Properties properties) {
    String pwd = properties.getProperty(PROP_PASSWORD);
    if (pwd != null) {
      String newPwd = CryptoUtil.decrypt(pwd);
      properties.setProperty(PROP_PASSWORD, newPwd);
    }
  }

}
