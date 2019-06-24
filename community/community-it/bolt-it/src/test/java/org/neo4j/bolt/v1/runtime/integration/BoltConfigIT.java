/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bolt.v1.runtime.integration;

import org.junit.Rule;
import org.junit.Test;

import org.neo4j.bolt.AbstractBoltTransportsTest;
import org.neo4j.bolt.v1.messaging.request.InitMessage;
import org.neo4j.bolt.v1.transport.integration.Neo4jWithSocket;
import org.neo4j.bolt.v1.transport.socket.client.TransportConnection;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.internal.helpers.HostnamePort;
import org.neo4j.test.rule.SuppressOutput;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.neo4j.bolt.v1.transport.integration.Neo4jWithSocket.DEFAULT_CONNECTOR_KEY;
import static org.neo4j.bolt.v1.transport.integration.TransportTestUtil.eventuallyDisconnects;
import static org.neo4j.configuration.SettingValueParsers.TRUE;
import static org.neo4j.configuration.connectors.BoltConnector.EncryptionLevel.REQUIRED;

public class BoltConfigIT extends AbstractBoltTransportsTest
{
    private static final String ANOTHER_CONNECTOR_KEY = "1";

    @Rule
    public Neo4jWithSocket server = new Neo4jWithSocket( getClass(),
            settings ->
            {
                settings.put( BoltConnector.group( DEFAULT_CONNECTOR_KEY ).enabled, TRUE );
                settings.put( BoltConnector.group( DEFAULT_CONNECTOR_KEY ).advertised_address, "localhost:0" );
                settings.put( BoltConnector.group( ANOTHER_CONNECTOR_KEY ).enabled, TRUE );
                settings.put( BoltConnector.group( ANOTHER_CONNECTOR_KEY ).advertised_address, "localhost:0" );
                settings.put( BoltConnector.group( ANOTHER_CONNECTOR_KEY ).encryption_level, REQUIRED.name() );
            } );
    @Rule
    public SuppressOutput suppressOutput = SuppressOutput.suppressAll();

    @Test
    public void shouldSupportMultipleConnectors() throws Throwable
    {
        HostnamePort address0 = server.lookupConnector( DEFAULT_CONNECTOR_KEY );
        assertConnectionAccepted( address0, newConnection() );

        HostnamePort address1 = server.lookupConnector( ANOTHER_CONNECTOR_KEY );
        assertConnectionRejected( address1, newConnection() );
    }

    private void assertConnectionRejected( HostnamePort address, TransportConnection client ) throws Exception
    {
        client.connect( address )
                .send( util.defaultAcceptedVersions() );

        assertThat( client, eventuallyDisconnects() );
    }

    private void assertConnectionAccepted( HostnamePort address, TransportConnection client ) throws Exception
    {
        client.connect( address )
                .send( util.defaultAcceptedVersions() )
                .send( util.chunk( new InitMessage( "TestClient/1.1", emptyMap() ) ) );
        assertThat( client, util.eventuallyReceivesSelectedProtocolVersion() );
    }
}
