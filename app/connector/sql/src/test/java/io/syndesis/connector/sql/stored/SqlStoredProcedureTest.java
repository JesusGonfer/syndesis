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
package io.syndesis.connector.sql.stored;

import java.util.HashMap;
import java.util.Map;

import io.syndesis.connector.sql.SqlSupport;
import io.syndesis.connector.sql.common.SqlTest;
import io.syndesis.connector.sql.common.SqlTest.ConnectionInfo;
import io.syndesis.connector.sql.common.SqlTest.Setup;
import io.syndesis.connector.sql.common.SqlTest.Teardown;
import io.syndesis.connector.sql.common.stored.StoredProcedureMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SqlTest.class)
@Setup(SampleStoredProcedures.DERBY_DEMO_ADD_SQL)
@Teardown("DROP PROCEDURE DEMO_ADD")
public class SqlStoredProcedureTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlStoredProcedureTest.class);

    @Test
    public void listAllStoredProcedures(final ConnectionInfo info) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("user", info.username);
        parameters.put("password", info.password);
        parameters.put("url", info.url);

        Map<String, StoredProcedureMetadata> storedProcedures = SqlSupport.getStoredProcedures(parameters);
        assertThat(storedProcedures.isEmpty()).isFalse();
        // Find 'demo_add'
        assertThat(storedProcedures.keySet().contains("DEMO_ADD")).isTrue();

        for (String storedProcedureName : storedProcedures.keySet()) {
            StoredProcedureMetadata md = storedProcedures.get(storedProcedureName);
            LOGGER.info("{}:{}", storedProcedureName, md.getTemplate());
        }

        // Inspect demo_add
        StoredProcedureMetadata metaData = storedProcedures.get("DEMO_ADD");
        assertThat(metaData.getTemplate()).isEqualTo("DEMO_ADD(INTEGER ${body[A]}, INTEGER ${body[B]}, OUT INTEGER C)");
    }

}
