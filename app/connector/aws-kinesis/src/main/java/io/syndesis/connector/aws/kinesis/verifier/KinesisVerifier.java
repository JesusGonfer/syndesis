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
package io.syndesis.connector.aws.kinesis.verifier;

import java.util.Map;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;

import com.amazonaws.services.kinesis.model.ListStreamsResult;


import com.amazonaws.util.StringUtils;
import io.syndesis.connector.support.verifier.api.ComponentVerifier;
import org.apache.camel.CamelContext;
import org.apache.camel.component.aws.kinesis.KinesisComponentVerifierExtension;
import org.apache.camel.component.aws.kinesis.KinesisConfiguration;
import org.apache.camel.component.extension.verifier.ResultBuilder;
import org.apache.camel.component.extension.verifier.ResultErrorBuilder;

/**
 * Factory to create the verifiers for the connection.
 *
 * @author JesusGonfer
 */
public class KinesisVerifier extends ComponentVerifier {
    public KinesisVerifier() {
        super("aws-kinesis", KinesisComponentVerifierExtension.class);
    }

    @Override
    protected KinesisComponentVerifierExtension resolveComponentVerifierExtension(CamelContext context, String scheme) {
        KinesisComponentVerifierExtension ext = new KinesisComponentVerifierExtension(scheme) {

            @Override
            protected Result verifyConnectivity(Map<String, Object> parameters) {
                ResultBuilder builder = ResultBuilder.withStatusAndScope(Result.Status.OK, Scope.CONNECTIVITY);

                try {
                    KinesisConfiguration configuration = setProperties(new KinesisConfiguration(), parameters);

                    AWSCredentials credentials = new BasicAWSCredentials(configuration.getAccessKey(), configuration.getSecretKey());
                    AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
                    AmazonKinesis client = AmazonKinesisClientBuilder.standard().withCredentials(credentialsProvider).withRegion(Regions.valueOf(configuration.getRegion())).build();
                    ListStreamsResult streams = client.listStreams();
                    String stream = configuration.getStreamName();
                    boolean streamExist = !StringUtils.isNullOrEmpty(stream) && streams.getStreamNames().parallelStream().filter(t -> stream.equalsIgnoreCase(t)).count() > 0;

                    if (!streamExist) {
                        ResultErrorBuilder errorBuilder = ResultErrorBuilder.withCodeAndDescription(VerificationError.StandardCode.ILLEGAL_PARAMETER,
                                                                                                    "Stream does not exist. Check stream name and region.")
                                                                                                    .detail("aws_kinesis_exception_message", "Stream does not exist. Check stream name and region.");
                        builder.error(errorBuilder.build());
                    }
                } catch (SdkClientException e) {
                    ResultErrorBuilder errorBuilder =
                        ResultErrorBuilder.withCodeAndDescription(VerificationError.StandardCode.AUTHENTICATION,
                            e.getMessage())
                            .detail("aws_kinesis_exception_message", e.getMessage()).detail(VerificationError.ExceptionAttribute.EXCEPTION_CLASS, e.getClass().getName())
                            .detail(VerificationError.ExceptionAttribute.EXCEPTION_INSTANCE, e);

                    builder.error(errorBuilder.build());
                } catch (Exception e) {
                    builder.error(ResultErrorBuilder.withException(e).build());
                }
                return builder.build();
            }
        };
        ext.setCamelContext(context);
        return ext;
    }
}
