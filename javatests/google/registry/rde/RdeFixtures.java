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

package com.google.domain.registry.rde;

import static com.google.common.io.BaseEncoding.base16;
import static com.google.domain.registry.testing.DatastoreHelper.generateNewContactHostRoid;
import static com.google.domain.registry.testing.DatastoreHelper.generateNewDomainRoid;
import static com.google.domain.registry.testing.DatastoreHelper.persistResource;
import static com.google.domain.registry.testing.DatastoreHelper.persistResourceWithCommitLog;
import static com.google.domain.registry.testing.DatastoreHelper.persistSimpleResource;
import static com.google.domain.registry.util.DateTimeUtils.END_OF_TIME;
import static org.joda.money.CurrencyUnit.USD;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.InetAddresses;
import com.google.domain.registry.model.billing.BillingEvent;
import com.google.domain.registry.model.billing.BillingEvent.Flag;
import com.google.domain.registry.model.billing.BillingEvent.Reason;
import com.google.domain.registry.model.contact.ContactAddress;
import com.google.domain.registry.model.contact.ContactPhoneNumber;
import com.google.domain.registry.model.contact.ContactResource;
import com.google.domain.registry.model.contact.PostalInfo;
import com.google.domain.registry.model.domain.DesignatedContact;
import com.google.domain.registry.model.domain.DomainAuthInfo;
import com.google.domain.registry.model.domain.DomainResource;
import com.google.domain.registry.model.domain.GracePeriod;
import com.google.domain.registry.model.domain.ReferenceUnion;
import com.google.domain.registry.model.domain.rgp.GracePeriodStatus;
import com.google.domain.registry.model.domain.secdns.DelegationSignerData;
import com.google.domain.registry.model.eppcommon.AuthInfo.PasswordAuth;
import com.google.domain.registry.model.eppcommon.StatusValue;
import com.google.domain.registry.model.eppcommon.Trid;
import com.google.domain.registry.model.host.HostResource;
import com.google.domain.registry.model.poll.PollMessage;
import com.google.domain.registry.model.poll.PollMessage.Autorenew;
import com.google.domain.registry.model.reporting.HistoryEntry;
import com.google.domain.registry.model.transfer.TransferData;
import com.google.domain.registry.model.transfer.TransferData.TransferServerApproveEntity;
import com.google.domain.registry.model.transfer.TransferStatus;
import com.google.domain.registry.testing.FakeClock;
import com.google.domain.registry.util.Idn;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

import org.joda.money.Money;
import org.joda.time.DateTime;

/** Utility class for creating {@code EppResource} entities that'll successfully marshal. */
final class RdeFixtures {

  static DomainResource makeDomainResource(FakeClock clock, String tld) {
    DomainResource domain = new DomainResource.Builder()
        .setFullyQualifiedDomainName("example." + tld)
        .setRepoId(generateNewDomainRoid(tld))
        .build();
    HistoryEntry historyEntry =
        persistResource(new HistoryEntry.Builder().setParent(domain).build());
    clock.advanceOneMilli();
    BillingEvent.OneTime billingEvent = persistResourceWithCommitLog(
        new BillingEvent.OneTime.Builder()
            .setReason(Reason.CREATE)
            .setTargetId("example." + tld)
            .setClientId("TheRegistrar")
            .setCost(Money.of(USD, 26))
            .setPeriodYears(2)
            .setEventTime(DateTime.parse("1910-01-01T00:00:00Z"))
            .setBillingTime(DateTime.parse("1910-01-01T00:00:00Z"))
            .setParent(historyEntry)
            .build());
    domain = domain.asBuilder()
        .setAuthInfo(DomainAuthInfo.create(PasswordAuth.create("secret")))
        .setContacts(ImmutableSet.of(
            DesignatedContact.create(DesignatedContact.Type.ADMIN, ReferenceUnion.create(
                makeContactResource(clock, "5372808-IRL",
                    "be that word our sign in parting", "BOFH@cat.みんな"))),
            DesignatedContact.create(DesignatedContact.Type.TECH, ReferenceUnion.create(
                makeContactResource(clock, "5372808-TRL",
                    "bird or fiend!? i shrieked upstarting", "bog@cat.みんな")))))
        .setCreationClientId("TheRegistrar")
        .setCurrentSponsorClientId("TheRegistrar")
        .setCreationTimeForTest(clock.nowUtc())
        .setDsData(ImmutableSet.of(DelegationSignerData.create(
              123, 200, 230, base16().decode("1234567890"))))
        .setFullyQualifiedDomainName(Idn.toASCII("love." + tld))
        .setLastTransferTime(DateTime.parse("1910-01-01T00:00:00Z"))
        .setLastEppUpdateClientId("IntoTheTempest")
        .setLastEppUpdateTime(clock.nowUtc())
        .setIdnTableName("extended_latin")
        .setNameservers(ImmutableSet.of(
            ReferenceUnion.create(
                makeHostResource(clock, "bird.or.devil.みんな", "1.2.3.4")),
            ReferenceUnion.create(
                makeHostResource(
                    clock, "ns2.cat.みんな", "bad:f00d:cafe::15:beef"))))
        .setRegistrant(ReferenceUnion.create(
            makeContactResource(clock,
                "5372808-ERL", "(◕‿◕) nevermore", "prophet@evil.みんな")))
        .setRegistrationExpirationTime(DateTime.parse("1930-01-01T00:00:00Z"))
        .setGracePeriods(ImmutableSet.of(
            GracePeriod.forBillingEvent(GracePeriodStatus.RENEW,
                persistResource(
                    new BillingEvent.OneTime.Builder()
                        .setReason(Reason.RENEW)
                        .setTargetId("love." + tld)
                        .setClientId("TheRegistrar")
                        .setCost(Money.of(USD, 456))
                        .setPeriodYears(2)
                        .setEventTime(DateTime.parse("1920-01-01T00:00:00Z"))
                        .setBillingTime(DateTime.parse("1920-01-01T00:00:00Z"))
                        .setParent(historyEntry)
                        .build())),
            GracePeriod.create(
                GracePeriodStatus.TRANSFER, DateTime.parse("1920-01-01T00:00:00Z"), "foo", null)))
        .setSubordinateHosts(ImmutableSet.of("home.by.horror.haunted"))
        .setStatusValues(ImmutableSet.of(
            StatusValue.CLIENT_DELETE_PROHIBITED,
            StatusValue.CLIENT_RENEW_PROHIBITED,
            StatusValue.CLIENT_TRANSFER_PROHIBITED,
            StatusValue.SERVER_UPDATE_PROHIBITED))
        .setAutorenewBillingEvent(
            Ref.create(persistResource(
                new BillingEvent.Recurring.Builder()
                    .setReason(Reason.RENEW)
                    .setFlags(ImmutableSet.of(Flag.AUTO_RENEW))
                    .setTargetId(tld)
                    .setClientId("TheRegistrar")
                    .setEventTime(END_OF_TIME)
                    .setRecurrenceEndTime(END_OF_TIME)
                    .setParent(historyEntry)
                    .build())))
        .setAutorenewPollMessage(
            Ref.create(persistSimpleResource(
                new PollMessage.Autorenew.Builder()
                    .setTargetId(tld)
                    .setClientId("TheRegistrar")
                    .setEventTime(END_OF_TIME)
                    .setAutorenewEndTime(END_OF_TIME)
                    .setMsg("Domain was auto-renewed.")
                    .setParent(historyEntry)
                    .build())))
        .setTransferData(new TransferData.Builder()
            .setExtendedRegistrationYears(1)
            .setGainingClientId("gaining")
            .setLosingClientId("losing")
            .setPendingTransferExpirationTime(DateTime.parse("1925-04-20T00:00:00Z"))
            .setServerApproveBillingEvent(Ref.create(billingEvent))
            .setServerApproveAutorenewEvent(
                Ref.create(persistResource(
                    new BillingEvent.Recurring.Builder()
                        .setReason(Reason.RENEW)
                        .setFlags(ImmutableSet.of(Flag.AUTO_RENEW))
                        .setTargetId("example." + tld)
                        .setClientId("TheRegistrar")
                        .setEventTime(END_OF_TIME)
                        .setRecurrenceEndTime(END_OF_TIME)
                        .setParent(historyEntry)
                        .build())))
            .setServerApproveAutorenewPollMessage(Ref.create(persistResource(
                new Autorenew.Builder()
                    .setTargetId("example." + tld)
                    .setClientId("TheRegistrar")
                    .setEventTime(END_OF_TIME)
                    .setAutorenewEndTime(END_OF_TIME)
                    .setMsg("Domain was auto-renewed.")
                    .setParent(historyEntry)
                    .build())))
            .setServerApproveEntities(ImmutableSet.<Key<? extends TransferServerApproveEntity>>of(
                Ref.create(billingEvent).getKey()))
            .setTransferRequestTime(DateTime.parse("1919-01-01T00:00:00Z"))
            .setTransferStatus(TransferStatus.PENDING)
            .setTransferRequestTrid(Trid.create("client trid"))
            .build())
        .build();
    clock.advanceOneMilli();
    return persistResourceWithCommitLog(domain);
  }

  static ContactResource makeContactResource(
      FakeClock clock, String id, String name, String email) {
    clock.advanceOneMilli();
    return persistResourceWithCommitLog(
        new ContactResource.Builder()
            .setContactId(id)
            .setRepoId(generateNewContactHostRoid())
            .setEmailAddress(email)
            .setStatusValues(ImmutableSet.of(StatusValue.OK))
            .setCurrentSponsorClientId("GetTheeBack")
            .setCreationClientId("GetTheeBack")
            .setCreationTimeForTest(clock.nowUtc())
            .setInternationalizedPostalInfo(new PostalInfo.Builder()
                .setType(PostalInfo.Type.INTERNATIONALIZED)
                .setName(name)
                .setOrg("DOGE INCORPORATED")
                .setAddress(new ContactAddress.Builder()
                    .setStreet(ImmutableList.of("123 Example Boulevard"))
                    .setCity("KOKOMO")
                    .setState("BM")
                    .setZip("31337")
                    .setCountryCode("US")
                    .build())
                .build())
            .setVoiceNumber(
                new ContactPhoneNumber.Builder()
                .setPhoneNumber("+1.5558675309")
                       .build())
            .setFaxNumber(
                new ContactPhoneNumber.Builder()
                .setPhoneNumber("+1.5558675310")
                .build())
            .build());
  }

  static HostResource makeHostResource(FakeClock clock, String fqhn, String ip) {
    clock.advanceOneMilli();
    return persistResourceWithCommitLog(
        new HostResource.Builder()
            .setRepoId(generateNewContactHostRoid())
            .setCreationClientId("LawyerCat")
            .setCreationTimeForTest(clock.nowUtc())
            .setCurrentSponsorClientId("BusinessCat")
            .setFullyQualifiedHostName(Idn.toASCII(fqhn))
            .setInetAddresses(ImmutableSet.of(InetAddresses.forString(ip)))
            .setLastTransferTime(DateTime.parse("1910-01-01T00:00:00Z"))
            .setLastEppUpdateClientId("CeilingCat")
            .setLastEppUpdateTime(clock.nowUtc())
            .setStatusValues(ImmutableSet.of(
                StatusValue.OK,
                StatusValue.PENDING_UPDATE))
            .build());
  }

  private RdeFixtures() {}
}