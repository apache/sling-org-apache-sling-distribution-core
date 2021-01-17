/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.transport.impl;

import java.net.URI;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.ObjectName;

import org.apache.sling.distribution.monitor.impl.UserCredentialsDistributionTransportSecretMBean;
import org.apache.sling.distribution.monitor.impl.UserCredentialsDistributionTransportSecretMBeanImpl;
import org.apache.sling.distribution.transport.DistributionTransportSecret;
import org.apache.sling.distribution.transport.DistributionTransportSecretProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service=DistributionTransportSecretProvider.class,
        properties= {
                "webconsole.configurationFactory.nameHint=Secret provider name: {name}"
        })
@Designate(ocd=UserCredentialsDistributionTransportSecretProvider.Config.class, factory = true)
public class UserCredentialsDistributionTransportSecretProvider implements
        DistributionTransportSecretProvider {
    
    @ObjectClassDefinition(name="Apache Sling Distribution Transport Credentials - User Credentials based DistributionTransportSecretProvider")
    public @interface Config {
        @AttributeDefinition(name="Name")
        String name();
        
        @AttributeDefinition(name="User Name", description = "The name of the user used to perform remote actions.")
        String username();
        
        @AttributeDefinition(name="Password", description = "The clear text password to perform authentication. Warning: storing clear text passwords is not safe.")
        String password();
    }

    private String username;
    private String password;

    private ServiceRegistration<UserCredentialsDistributionTransportSecretMBean> mbeanServiceRegistration;

    @Activate
    protected void activate(BundleContext context, Config conf) {
        username = conf.username().trim();
        password = conf.password().trim();

        String id = String.valueOf(username.hashCode());

        Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
        mbeanProps.put("jmx.objectname", "org.apache.sling.distribution:type=transport,id=" + ObjectName.quote(id));

        UserCredentialsDistributionTransportSecretMBean mbean =
                        new UserCredentialsDistributionTransportSecretMBeanImpl(username);
        mbeanServiceRegistration =
                        context.registerService(UserCredentialsDistributionTransportSecretMBean.class, mbean, mbeanProps);
    }

    @Deactivate
    protected void deactivate() {
        if (mbeanServiceRegistration != null) {
            mbeanServiceRegistration.unregister();
        }
        mbeanServiceRegistration = null;
    }

    public DistributionTransportSecret getSecret(URI uri) {
        return new DistributionTransportSecret() {
            public Map<String, String> asCredentialsMap() {
                Map<String, String> map = new HashMap<String, String>();
                map.put("username", username);
                map.put("password", password);
                return Collections.unmodifiableMap(map);
            }
        };
    }
}
