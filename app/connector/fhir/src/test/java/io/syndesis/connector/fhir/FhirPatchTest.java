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
package io.syndesis.connector.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import io.syndesis.common.model.integration.Step;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.okXml;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class FhirPatchTest extends FhirTestBase {

    @Override
    protected List<Step> createSteps() {
        return Arrays.asList(newSimpleEndpointStep(
            "direct",
            builder -> builder.putConfiguredProperty("name", "start")),
            newFhirEndpointStep("io.syndesis:fhir-patch-connector", builder -> {
                builder.putConfiguredProperty("resourceType", "Patient");
                builder.putConfiguredProperty("id", "1");
            }));
    }

    @Test
    public void patchTest() {
        stubFhirRequest(patch(urlEqualTo("/Patient/1")).willReturn(okXml(toXml(new OperationOutcome()))));

        template().requestBody("direct:start",
            "[{\"op\":\"replace\", \"path\":\"active\", \"value\":true}]", MethodOutcome.class);
    }


}
