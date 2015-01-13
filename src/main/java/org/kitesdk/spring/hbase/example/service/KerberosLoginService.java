/*
 * Copyright 2015 joey.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kitesdk.spring.hbase.example.service;

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KerberosLoginService {

  private static final Logger LOG
      = LoggerFactory.getLogger(KerberosLoginService.class);

  public KerberosLoginService(String applicationPrincipal,
      String applicationKeytab) throws IOException {

    LOG.debug("application.kerberos.principal=" + applicationPrincipal);
    LOG.debug("application.kerberos.keytab=" + applicationKeytab);

    if (UserGroupInformation.isSecurityEnabled()) {
      Preconditions.checkNotNull(applicationPrincipal,
          "Setting the application.kerberos.principal in hbase-prod.properties "
          + "is required when security is enabled.");

      Preconditions.checkNotNull(applicationKeytab,
          "Setting the application.kerberos.keytab in hbase-prod.properties is "
          + "required when security is enabled.");

      LOG.info("Logging in user {} using keytab {}.", new Object[]{
        applicationPrincipal, applicationKeytab});

      UserGroupInformation.loginUserFromKeytab(applicationPrincipal,
          applicationKeytab);
    }
  }

}
