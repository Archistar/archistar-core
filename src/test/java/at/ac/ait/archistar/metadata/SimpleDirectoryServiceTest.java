package at.ac.ait.archistar.metadata;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.HashSet;

import org.junit.BeforeClass;
import org.junit.Test;

import at.ac.ait.archistar.backendserver.fragments.Fragment;
import at.ac.ait.archistar.backendserver.storageinterface.StorageServer;
import at.ac.ait.archistar.engine.crypto.ArchistarCryptoEngine;
import at.ac.ait.archistar.engine.crypto.PseudoMirrorCryptoEngine;
import at.ac.ait.archistar.engine.distributor.Distributor;
import at.ac.ait.archistar.engine.distributor.ServerConfiguration;
import at.ac.ait.archistar.engine.metadata.MetadataService;
import at.ac.ait.archistar.engine.metadata.SimpleMetadataService;

public class SimpleDirectoryServiceTest {

    private static Distributor distributor;
    private static MetadataService theService;
    private static StorageServer server1;
    private static StorageServer server2;
    private static ServerConfiguration config;

    @BeforeClass
    public static void prepareTestData() {
        distributor = mock(Distributor.class);
        config = mock(ServerConfiguration.class);

        Set<StorageServer> servers = new HashSet<>();

        server1 = mock(StorageServer.class);
        when(server1.isConnected()).thenReturn(true);
        servers.add(server1);

        server2 = mock(StorageServer.class);
        when(server2.isConnected()).thenReturn(true);
        servers.add(server2);

        ArchistarCryptoEngine crypto = new PseudoMirrorCryptoEngine();

        Set<Fragment> result = new HashSet<>();
        Fragment frag1 = mock(Fragment.class);
        when(frag1.getStorageServer()).thenReturn(server1);
        when(frag1.getFragmentId()).thenReturn("fragement-1");
        result.add(frag1);
        Fragment frag2 = mock(Fragment.class);
        when(frag2.getStorageServer()).thenReturn(server2);
        when(frag2.getFragmentId()).thenReturn("fragement-2");
        result.add(frag2);

        when(config.getOnlineStorageServerCount()).thenReturn(2);
        when(config.getOnlineStorageServers()).thenReturn(servers);

        theService = new SimpleMetadataService(config, distributor, crypto);
        theService.connect();
    }

    @Test
    public void testIfAllServersAreIncludedInResult() {

        Set<Fragment> result = theService.getDistributionFor("/some-test-path");
        assertThat(result).hasSize(config.getOnlineStorageServerCount());

        Set<StorageServer> choosenServers = new HashSet<>();
        for (Fragment f : result) {
            choosenServers.add(f.getStorageServer());
        }
        assertThat(choosenServers).contains(server1, server2);
    }

    @Test
    public void testIfRepeatedCallsGenerateSameFragmentIds() {

        Set<Fragment> result1 = theService.getDistributionFor("/some-test-path");
        Set<Fragment> result2 = theService.getDistributionFor("/some-test-path");

        // equivalent to result1.map(&:getStorageServer()) == result2.map(&:getStorageServer())
        Set<String> fragmentIds1 = new HashSet<>();
        for (Fragment f : result1) {
            fragmentIds1.add(f.getFragmentId());
        }

        Set<String> fragmentIds2 = new HashSet<>();

        for (Fragment f : result2) {
            fragmentIds2.add(f.getFragmentId());
        }

        assertThat(fragmentIds1).containsAll(fragmentIds2);
        assertThat(fragmentIds2).containsAll(fragmentIds1);
    }

    @Test
    public void testIfRepeatedCallsGenerateSameServers() {

        Set<Fragment> result1 = theService.getDistributionFor("/some-test-path");
        Set<Fragment> result2 = theService.getDistributionFor("/some-test-path");

        // equivalent to result1.map(&:getStorageServer()) == result2.map(&:getStorageServer())
        Set<StorageServer> servers1 = new HashSet<>();
        for (Fragment f : result1) {
            servers1.add(f.getStorageServer());
        }

        Set<StorageServer> servers2 = new HashSet<>();

        for (Fragment f : result2) {
            servers2.add(f.getStorageServer());
        }
        assertThat(servers1).containsAll(servers2);
        assertThat(servers2).containsAll(servers1);
    }
}
