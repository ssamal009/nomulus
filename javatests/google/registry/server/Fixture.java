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

package com.google.domain.registry.server;

import static com.google.domain.registry.model.domain.DesignatedContact.Type.ADMIN;
import static com.google.domain.registry.model.domain.DesignatedContact.Type.BILLING;
import static com.google.domain.registry.model.domain.DesignatedContact.Type.TECH;
import static com.google.domain.registry.testing.DatastoreHelper.createTlds;
import static com.google.domain.registry.testing.DatastoreHelper.newContactResource;
import static com.google.domain.registry.testing.DatastoreHelper.newDomainResource;
import static com.google.domain.registry.testing.DatastoreHelper.persistActiveHost;
import static com.google.domain.registry.testing.DatastoreHelper.persistResource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.domain.registry.model.contact.ContactAddress;
import com.google.domain.registry.model.contact.ContactResource;
import com.google.domain.registry.model.contact.PostalInfo;
import com.google.domain.registry.model.domain.DesignatedContact;
import com.google.domain.registry.model.domain.ReferenceUnion;
import com.google.domain.registry.model.ofy.Ofy;
import com.google.domain.registry.model.registrar.Registrar;
import com.google.domain.registry.testing.FakeClock;
import com.google.domain.registry.testing.InjectRule;

import org.joda.time.DateTime;

/**
 * Datastore fixtures for the development webserver.
 *
 * <p><b>Warning:</b> These fixtures aren't really intended for unit tests, since they take upwards
 * of a second to load.
 */
public enum Fixture {

  INJECTED_FAKE_CLOCK {
    @Override
    public void load() {
      new InjectRule()
          .setStaticField(Ofy.class, "clock", new FakeClock(DateTime.parse("2000-01-01TZ")));
    }
  },

  /** Fixture of two TLDs, three contacts, two domains, and six hosts. */
  BASIC {
    @Override
    public void load() {
      createTlds("xn--q9jyb4c", "example");

      ContactResource google = persistResource(newContactResource("google")
          .asBuilder()
          .setLocalizedPostalInfo(new PostalInfo.Builder()
              .setType(PostalInfo.Type.LOCALIZED)
              .setName("Mr. Google")
              .setOrg("Google Inc.")
              .setAddress(new ContactAddress.Builder()
                  .setStreet(ImmutableList.of("111 8th Ave", "4th Floor"))
                  .setCity("New York")
                  .setState("NY")
                  .setZip("10011")
                  .setCountryCode("US")
                  .build())
              .build())
            .build());

      ContactResource justine = persistResource(newContactResource("justine")
          .asBuilder()
          .setLocalizedPostalInfo(new PostalInfo.Builder()
              .setType(PostalInfo.Type.LOCALIZED)
              .setName("Justine Bean")
              .setOrg("(✿◕ ‿◕ )ノ Incorporated")
              .setAddress(new ContactAddress.Builder()
                  .setStreet(ImmutableList.of("123 Fake St."))
                  .setCity("Stratford")
                  .setState("CT")
                  .setZip("06615")
                  .setCountryCode("US")
                  .build())
              .build())
            .build());

      ContactResource robert = persistResource(newContactResource("robert")
          .asBuilder()
          .setLocalizedPostalInfo(new PostalInfo.Builder()
              .setType(PostalInfo.Type.LOCALIZED)
              .setName("Captain Robert")
              .setOrg("Ancient World")
              .setAddress(new ContactAddress.Builder()
                  .setStreet(ImmutableList.of(
                      "A skeleton crew is what came back",
                      "And once in port he filled his sack",
                      "With bribes and cash and fame and coin"))
                  .setCity("Things to make a new crew join")
                  .setState("NY")
                  .setZip("10011")
                  .setCountryCode("US")
                  .build())
              .build())
            .build());

      persistResource(
          newDomainResource("love.xn--q9jyb4c", justine).asBuilder()
              .setContacts(ImmutableSet.of(
                  DesignatedContact.create(ADMIN, ReferenceUnion.create(robert)),
                  DesignatedContact.create(BILLING, ReferenceUnion.create(google)),
                  DesignatedContact.create(TECH, ReferenceUnion.create(justine))))
              .setNameservers(ImmutableSet.of(
                  ReferenceUnion.create(
                      persistActiveHost("ns1.love.xn--q9jyb4c")),
                  ReferenceUnion.create(
                      persistActiveHost("ns2.love.xn--q9jyb4c"))))
              .build());

      persistResource(
          newDomainResource("moogle.example", justine).asBuilder()
              .setContacts(ImmutableSet.of(
                  DesignatedContact.create(ADMIN, ReferenceUnion.create(robert)),
                  DesignatedContact.create(BILLING, ReferenceUnion.create(google)),
                  DesignatedContact.create(TECH, ReferenceUnion.create(justine))))
              .setNameservers(ImmutableSet.of(
                  ReferenceUnion.create(persistActiveHost("ns1.linode.com")),
                  ReferenceUnion.create(persistActiveHost("ns2.linode.com")),
                  ReferenceUnion.create(persistActiveHost("ns3.linode.com")),
                  ReferenceUnion.create(persistActiveHost("ns4.linode.com")),
                  ReferenceUnion.create(persistActiveHost("ns5.linode.com"))))
              .build());

      persistResource(
          Registrar.loadByClientId("TheRegistrar").asBuilder()
              .setAllowedTlds(ImmutableSet.of("example", "xn--q9jyb4c"))
              .setBillingMethod(Registrar.BillingMethod.BRAINTREE)
              .build());
    }
  };

  /** Loads this fixture into the datastore. */
  public abstract void load();
}