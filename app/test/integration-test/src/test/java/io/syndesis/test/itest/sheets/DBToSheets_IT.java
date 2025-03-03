/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.syndesis.test.itest.sheets;

import java.util.Arrays;

import io.syndesis.test.SyndesisTestEnvironment;
import io.syndesis.test.container.integration.SyndesisIntegrationRuntimeContainer;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.runner.TestRunner;

/**
 * @author Christoph Deppisch
 */
@Testcontainers
public class DBToSheets_IT extends GoogleSheetsTestSupport {

    /**
     * Integration periodically retrieves all contacts from the database and maps the entries (first_name, last_name, company) to a spreadsheet on a Google Sheets account.
     * The integration uses collection to collection data mapping and passes all values in one single operation to Google Sheets.
     */
    @Container
    public static final SyndesisIntegrationRuntimeContainer INTEGRATION_CONTAINER = new SyndesisIntegrationRuntimeContainer.Builder()
            .name("db-to-sheets")
            .fromExport(DBToSheets_IT.class.getResource("DBToSheets-export"))
            .customize("$..configuredProperties.schedulerExpression", "5000")
            .customize("$..rootUrl.defaultValue",
                        String.format("http://%s:%s", GenericContainer.INTERNAL_HOST_HOSTNAME, GOOGLE_SHEETS_SERVER_PORT))
            .build()
            .withNetwork(getSyndesisDb().getNetwork())
            .waitingFor(Wait.defaultWaitStrategy().withStartupTimeout(SyndesisTestEnvironment.getContainerStartupTimeout()));

    @Test
    @CitrusTest
    public void testDBToSheets(@CitrusResource TestRunner runner) {
        runner.sql(builder -> builder.dataSource(sampleDb())
                .statements(Arrays.asList("insert into contact (first_name, last_name, company, lead_source) values ('Joe','Jackson','Red Hat','google-sheets')",
                                          "insert into contact (first_name, last_name, company, lead_source) values ('Joanne','Jackson','Red Hat','google-sheets')")));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                        .receive()
                        .put()
                        .payload("{\"majorDimension\":\"ROWS\",\"values\":[[\"Joe\",\"Jackson\",\"Red Hat\"],[\"Joanne\",\"Jackson\",\"Red Hat\"]]}"));

        runner.http(builder -> builder.server(googleSheetsApiServer)
                        .send()
                        .response(HttpStatus.OK));
    }
}
