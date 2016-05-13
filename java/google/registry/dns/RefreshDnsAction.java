// Copyright 2016 The Domain Registry Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.domain.registry.dns;

import static com.google.domain.registry.model.EppResourceUtils.loadByUniqueId;

import com.google.domain.registry.dns.DnsConstants.TargetType;
import com.google.domain.registry.model.EppResource;
import com.google.domain.registry.model.domain.DomainResource;
import com.google.domain.registry.model.host.HostResource;
import com.google.domain.registry.request.Action;
import com.google.domain.registry.request.HttpException.BadRequestException;
import com.google.domain.registry.request.HttpException.NotFoundException;
import com.google.domain.registry.request.Parameter;
import com.google.domain.registry.util.Clock;

import javax.inject.Inject;

/** Action that manually triggers refresh of DNS information. */
@Action(path = "/_dr/dnsRefresh", automaticallyPrintOk = true)
public final class RefreshDnsAction implements Runnable {

  @Inject Clock clock;
  @Inject DnsQueue dnsQueue;
  @Inject @Parameter("name") String domainOrHostName;
  @Inject @Parameter("type") TargetType type;
  @Inject RefreshDnsAction() {}

  @Override
  public void run() {
    if (!domainOrHostName.contains(".")) {
      throw new BadRequestException("URL parameter 'name' must be fully qualified");
    }

    boolean domainLookup;
    Class<? extends EppResource> clazz;
    switch (type) {
      case DOMAIN:
        domainLookup = true;
        clazz = DomainResource.class;
        break;
      case HOST:
        domainLookup = false;
        clazz = HostResource.class;
        break;
      default:
        throw new BadRequestException("Unsupported type: " + type);
    }

    EppResource eppResource = loadByUniqueId(clazz, domainOrHostName, clock.nowUtc());
    if (eppResource == null) {
      throw new NotFoundException(
          String.format("%s %s not found", type, domainOrHostName));
    }

    if (domainLookup) {
      dnsQueue.addDomainRefreshTask(domainOrHostName);
    } else {
      if (((HostResource) eppResource).getSuperordinateDomain() == null) {
        throw new BadRequestException(
            String.format("%s isn't a subordinate hostname", domainOrHostName));
      } else {
        // Don't enqueue host refresh tasks for external hosts.
        dnsQueue.addHostRefreshTask(domainOrHostName);
      }
    }
  }
}