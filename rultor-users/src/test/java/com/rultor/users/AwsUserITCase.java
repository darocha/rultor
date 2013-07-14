/**
 * Copyright (c) 2009-2013, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.users;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.jcabi.dynamo.Credentials;
import com.jcabi.dynamo.Region;
import com.jcabi.dynamo.TableMocker;
import com.jcabi.urn.URN;
import com.rultor.spi.User;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration case for {@link AwsUser}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
public final class AwsUserITCase {

    /**
     * AWS key.
     */
    private static final String KEY = System.getProperty("failsafe.dynamo.key");

    /**
     * Region to work with.
     */
    private transient Region region;

    /**
     * Table mocker to work with.
     */
    private transient TableMocker table;

    /**
     * Assume we're online.
     * @throws Exception If fails
     */
    @Before
    public void prepare() throws Exception {
        Assume.assumeNotNull(AwsUserITCase.KEY);
        final String prefix = System.getProperty("failsafe.dynamo.prefix");
        this.region = new Region.Prefixed(
            new Region.Simple(
                new Credentials.Simple(
                    AwsUserITCase.KEY,
                    System.getProperty("failsafe.dynamo.secret")
                )
            ),
            prefix
        );
        this.table = new TableMocker(
            this.region,
            new CreateTableRequest()
                .withTableName(String.format("%sunits", prefix))
                .withProvisionedThroughput(
                    new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
                )
                .withAttributeDefinitions(
                    new AttributeDefinition()
                        .withAttributeName(AwsUnit.KEY_OWNER)
                        .withAttributeType(ScalarAttributeType.S),
                    new AttributeDefinition()
                        .withAttributeName(AwsUnit.KEY_NAME)
                        .withAttributeType(ScalarAttributeType.S)
                )
                .withKeySchema(
                    new KeySchemaElement()
                        .withAttributeName(AwsUnit.KEY_OWNER)
                        .withKeyType(KeyType.HASH),
                    new KeySchemaElement()
                        .withAttributeName(AwsUnit.KEY_NAME)
                        .withKeyType(KeyType.RANGE)
                )
        );
        this.table.create();
    }

    /**
     * Assume we're online.
     */
    @After
    public void drop() {
        Assume.assumeNotNull(AwsUserITCase.KEY);
        this.table.drop();
    }

    /**
     * AwsUser can work with real data.
     * @throws Exception If some problem inside
     */
    @Test
    public void worksWithRealDynamoDb() throws Exception {
        final URN urn = new URN("urn:github:66");
        final User user = new AwsUser(this.region, urn);
        MatcherAssert.assertThat(user.urn(), Matchers.equalTo(urn));
        final String name = "simple-unit";
        if (user.units().contains(name)) {
            user.remove(name);
        }
        user.create(name);
        MatcherAssert.assertThat(user.units(), Matchers.hasItem(name));
    }

}
