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

package com.google.domain.registry.flows.session;

import static com.google.common.truth.Truth.assertThat;
import static com.google.domain.registry.testing.DatastoreHelper.deleteResource;
import static com.google.domain.registry.testing.DatastoreHelper.persistResource;

import com.google.domain.registry.flows.EppException.UnimplementedExtensionException;
import com.google.domain.registry.flows.EppException.UnimplementedObjectServiceException;
import com.google.domain.registry.flows.EppException.UnimplementedProtocolVersionException;
import com.google.domain.registry.flows.FlowTestCase;
import com.google.domain.registry.flows.session.LoginFlow.AlreadyLoggedInException;
import com.google.domain.registry.flows.session.LoginFlow.BadRegistrarClientIdException;
import com.google.domain.registry.flows.session.LoginFlow.BadRegistrarPasswordException;
import com.google.domain.registry.flows.session.LoginFlow.PasswordChangesNotSupportedException;
import com.google.domain.registry.flows.session.LoginFlow.RegistrarAccountNotActiveException;
import com.google.domain.registry.flows.session.LoginFlow.TooManyFailedLoginsException;
import com.google.domain.registry.flows.session.LoginFlow.UnsupportedLanguageException;
import com.google.domain.registry.model.registrar.Registrar;
import com.google.domain.registry.model.registrar.Registrar.State;
import com.google.domain.registry.testing.ExceptionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Unit tests for {@link LoginFlow}. */
public abstract class LoginFlowTestCase extends FlowTestCase<LoginFlow> {

  @Rule
  public final ExceptionRule thrown = new ExceptionRule();

  Registrar registrar;
  Registrar.Builder registrarBuilder;

  @Before
  public void initRegistrar() {
    sessionMetadata.setClientId(null);  // Don't implicitly log in (all other flows need to).
    registrar = Registrar.loadByClientId("NewRegistrar");
    registrarBuilder = registrar.asBuilder();
  }

  // Can't inline this since it may be overridden in subclasses.
  protected Registrar.Builder getRegistrarBuilder() {
    return registrarBuilder;
  }

  // Also called in subclasses.
  void doSuccessfulTest(String xmlFilename) throws Exception {
    setEppInput(xmlFilename);
    assertTransactionalFlow(false);
    runFlowAssertResponse(readFile("login_response.xml"));
  }

  // Also called in subclasses.
  void doFailingTest(String xmlFilename, Class<? extends Exception> exception)
      throws Exception {
    thrown.expect(exception);
    setEppInput(xmlFilename);
    runFlow();
  }

  @Test
  public void testSuccess() throws Exception {
    doSuccessfulTest("login_valid.xml");
    assertThat(sessionMetadata.isSuperuser()).isFalse();
  }

  @Test
  public void testSuccess_superuser() throws Exception {
    persistResource(getRegistrarBuilder().setIanaIdentifier(9999L).build());
    doSuccessfulTest("login_valid.xml");
    assertThat(sessionMetadata.isSuperuser()).isTrue();
  }

  @Test
  public void testSuccess_notSuperuser() throws Exception {
    persistResource(getRegistrarBuilder().setIanaIdentifier(15L).build());
    doSuccessfulTest("login_valid.xml");
    assertThat(sessionMetadata.isSuperuser()).isFalse();
  }

  @Test
  public void testSuccess_suspendedRegistrar() throws Exception {
    persistResource(getRegistrarBuilder().setState(State.SUSPENDED).build());
    doSuccessfulTest("login_valid.xml");
  }

  @Test
  public void testSuccess_missingTypes() throws Exception {
    // We don't actually care if you list all the right types, as long as you don't add wrong ones.
    doSuccessfulTest("login_valid_missing_types.xml");
  }

  @Test
  public void testFailure_invalidVersion() throws Exception {
    doFailingTest("login_invalid_version.xml", UnimplementedProtocolVersionException.class);
  }

  @Test
  public void testFailure_invalidLanguage() throws Exception {
    doFailingTest("login_invalid_language.xml", UnsupportedLanguageException.class);
  }

  @Test
  public void testFailure_invalidExtension() throws Exception {
    doFailingTest("login_invalid_extension.xml", UnimplementedExtensionException.class);
  }

  @Test
  public void testFailure_invalidTypes() throws Exception {
    doFailingTest("login_invalid_types.xml", UnimplementedObjectServiceException.class);
  }

  @Test
  public void testFailure_newPassword() throws Exception {
    doFailingTest("login_invalid_newpw.xml", PasswordChangesNotSupportedException.class);
  }

  @Test
  public void testFailure_unknownRegistrar() throws Exception {
    deleteResource(getRegistrarBuilder().build());
    doFailingTest("login_valid.xml", BadRegistrarClientIdException.class);
  }

  @Test
  public void testFailure_pendingRegistrar() throws Exception {
    persistResource(getRegistrarBuilder().setState(State.PENDING).build());
    doFailingTest("login_valid.xml", RegistrarAccountNotActiveException.class);
  }

  @Test
  public void testFailure_incorrectPassword() throws Exception {
    persistResource(getRegistrarBuilder().setPassword("diff password").build());
    doFailingTest("login_valid.xml", BadRegistrarPasswordException.class);
  }

  @Test
  public void testFailure_tooManyFailedLogins() throws Exception {
    persistResource(getRegistrarBuilder().setPassword("diff password").build());
    doFailingTest("login_valid.xml", BadRegistrarPasswordException.class);
    doFailingTest("login_valid.xml", BadRegistrarPasswordException.class);
    doFailingTest("login_valid.xml", BadRegistrarPasswordException.class);
    doFailingTest("login_valid.xml", TooManyFailedLoginsException.class);
  }

  @Test
  public void testFailure_alreadyLoggedIn() throws Exception {
    sessionMetadata.setClientId("something");
    doFailingTest("login_valid.xml", AlreadyLoggedInException.class);
  }
}