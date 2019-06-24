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
package org.neo4j.ssl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileNotFoundException;

import org.neo4j.configuration.Config;
import org.neo4j.configuration.ssl.PemSslPolicyConfig;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.ssl.config.SslPolicyLoader;
import org.neo4j.test.extension.Inject;
import org.neo4j.test.extension.TestDirectoryExtension;
import org.neo4j.test.rule.TestDirectory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.neo4j.configuration.Config.newBuilder;
import static org.neo4j.configuration.GraphDatabaseSettings.neo4j_home;

@ExtendWith( TestDirectoryExtension.class )
class PemSslPolicyLoaderTest
{
    @Inject
    private TestDirectory testDirectory;

    private File home;
    private File publicCertificateFile;
    private File privateKeyFile;

    @BeforeEach
    void setup() throws Exception
    {
        home = testDirectory.directory( "home" );
        File baseDir = new File( home, "certificates/default" );
        publicCertificateFile = new File( baseDir, "public.crt" );
        privateKeyFile = new File( baseDir, "private.key" );

        new PkiUtils().createSelfSignedCertificate(
                publicCertificateFile, privateKeyFile, "localhost" );

        File trustedDir = new File( baseDir, "trusted" );
        trustedDir.mkdir();
        FileUtils.copyFile( publicCertificateFile, new File( trustedDir, "public.crt" ) );
        new File( baseDir, "revoked" ).mkdir();
    }

    @Test
    void shouldLoadBaseCryptographicObjects() throws Exception
    {
        // given
        PemSslPolicyConfig policyConfig = PemSslPolicyConfig.group( "default" );

        Config config = newBuilder()
                .set( neo4j_home, home.getAbsolutePath() )
                .set( policyConfig.base_directory, "certificates/default" )
                .build();

        // when
        SslPolicyLoader sslPolicyLoader = SslPolicyLoader.create( config, NullLogProvider.getInstance() );

        // then
        SslPolicy sslPolicy = sslPolicyLoader.getPolicy( "default" );
        assertNotNull( sslPolicy );
        assertNotNull( sslPolicy.privateKey() );
        assertNotNull( sslPolicy.certificateChain() );
        assertNotNull( sslPolicy.nettyClientContext() );
        assertNotNull( sslPolicy.nettyServerContext() );
    }

    @Test
    void shouldComplainIfMissingPrivateKey()
    {
        shouldComplainIfMissingFile( privateKeyFile );
    }

    @Test
    void shouldComplainIfMissingPublicCertificate()
    {
        shouldComplainIfMissingFile( publicCertificateFile );
    }

    private void shouldComplainIfMissingFile( File file )
    {
        // given
        FileUtils.deleteFile( file );

        PemSslPolicyConfig policyConfig = PemSslPolicyConfig.group( "default" );

        Config config = newBuilder()
                .set( neo4j_home, home.getAbsolutePath() )
                .set( policyConfig.base_directory, "certificates/default" )
                .build();

        // when
        Exception exception = assertThrows( Exception.class, () -> SslPolicyLoader.create( config, NullLogProvider.getInstance() ) );
        assertThat( exception.getCause(), instanceOf( FileNotFoundException.class ) );
    }

    @Test
    void shouldThrowIfPolicyNameDoesNotExist()
    {
        // given
        PemSslPolicyConfig policyConfig = PemSslPolicyConfig.group( "default" );

        Config config = newBuilder()
                .set( neo4j_home, home.getAbsolutePath() )
                .set( policyConfig.base_directory, "certificates/default" )
                .build();

        SslPolicyLoader sslPolicyLoader = SslPolicyLoader.create( config, NullLogProvider.getInstance() );

        // when
        assertThrows( IllegalArgumentException.class, () -> sslPolicyLoader.getPolicy( "unknown" ) );
    }

    @Test
    void shouldReturnNullPolicyIfNullRequested()
    {
        // given
        SslPolicyLoader sslPolicyLoader = SslPolicyLoader.create( Config.defaults(), NullLogProvider.getInstance() );

        // when
        SslPolicy sslPolicy = sslPolicyLoader.getPolicy( null );

        // then
        assertNull( sslPolicy );
    }
}
