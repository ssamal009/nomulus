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

package com.google.domain.registry.monitoring.whitebox;

import static com.google.domain.registry.monitoring.whitebox.EntityIntegrityAlertsSchema.ENTITY_INTEGRITY_ALERTS_SCHEMA_FIELDS;
import static com.google.domain.registry.monitoring.whitebox.EntityIntegrityAlertsSchema.TABLE_ID;
import static com.google.domain.registry.monitoring.whitebox.EppMetrics.EPPMETRICS_SCHEMA_FIELDS;
import static com.google.domain.registry.monitoring.whitebox.EppMetrics.EPPMETRICS_TABLE_ID;
import static com.google.domain.registry.request.RequestParameters.extractRequiredParameter;
import static dagger.Provides.Type.MAP;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.domain.registry.request.Parameter;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.StringKey;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

/**
 * Dagger module for injecting common settings for Whitebox tasks.
 */
@Module
public class WhiteboxModule {

  @Provides(type = MAP)
  @StringKey(EPPMETRICS_TABLE_ID)
  static ImmutableList<TableFieldSchema> provideEppMetricsSchema() {
    return EPPMETRICS_SCHEMA_FIELDS;
  }

  @Provides(type = MAP)
  @StringKey(TABLE_ID)
  static ImmutableList<TableFieldSchema> provideEntityIntegrityAlertsSchema() {
    return ENTITY_INTEGRITY_ALERTS_SCHEMA_FIELDS;
  }

  @Provides
  @Parameter("tableId")
  static String provideTableId(HttpServletRequest req) {
    return extractRequiredParameter(req, "tableId");
  }

  @Provides
  @Parameter("insertId")
  static String provideInsertId(HttpServletRequest req) {
    return extractRequiredParameter(req, "insertId");
  }

  @Provides
  static Supplier<String> provideIdGenerator() {
    return new Supplier<String>() {
      @Override
      public String get() {
        return UUID.randomUUID().toString();
      }
    };
  }
}