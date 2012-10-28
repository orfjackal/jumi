// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.launcher;

import fi.jumi.actors.*;
import fi.jumi.actors.eventizers.dynamic.DynamicEventizerProvider;
import fi.jumi.actors.listeners.*;
import fi.jumi.core.network.*;
import fi.jumi.core.util.PrefixedThreadFactory;
import fi.jumi.launcher.daemon.*;
import fi.jumi.launcher.process.*;
import fi.jumi.launcher.remote.*;
import org.apache.commons.io.output.NullOutputStream;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

@NotThreadSafe
public class JumiLauncherBuilder {

    private boolean debugLogging = false;

    public JumiLauncher build() {
        final ExecutorService actorsThreadPool = createActorsThreadPool();
        ProcessStarter processStarter = createProcessStarter();
        final NetworkServer networkServer = createNetworkServer();
        OutputStream daemonOutputListener = createDaemonOutputListener();

        Actors actors = new MultiThreadedActors(
                actorsThreadPool,
                new DynamicEventizerProvider(),
                new PrintStreamFailureLogger(System.out),
                new NullMessageListener()
        );
        final ActorThread actorThread = startActorThread(actors);

        ActorRef<DaemonSummoner> daemonSummoner = actorThread.bindActor(DaemonSummoner.class, new ProcessStartingDaemonSummoner(
                new DirBasedSteward(new EmbeddedDaemonJar(), getSettingsDirectory()),
                processStarter,
                networkServer,
                daemonOutputListener
        ));
        final ActorRef<SuiteLauncher> suiteLauncher = actorThread.bindActor(SuiteLauncher.class, new RemoteSuiteLauncher(actorThread, daemonSummoner));

        @NotThreadSafe
        class ExternalResources implements Closeable {
            @Override
            public void close() throws IOException {
                networkServer.close();
                actorThread.stop();
                actorsThreadPool.shutdown();
                try {
                    actorsThreadPool.awaitTermination(1, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return new JumiLauncher(suiteLauncher, new ExternalResources());
    }

    // configuration parameters

    public JumiLauncherBuilder enableDebugLogging() {
        debugLogging = true;
        return this;
    }

    protected Path getSettingsDirectory() {
        return Paths.get(".jumi"); // TODO: put into user home; also update fi.jumi.launcher.daemon.DirBasedSteward.settingsDir
    }


    // dependencies

    protected ExecutorService createActorsThreadPool() {
        return Executors.newCachedThreadPool(new PrefixedThreadFactory("jumi-launcher-"));
    }

    protected ActorThread startActorThread(Actors actors) {
        return actors.startActorThread(); // in an overridable method for testing purposes
    }

    protected ProcessStarter createProcessStarter() {
        return new SystemProcessStarter();
    }

    protected NetworkServer createNetworkServer() {
        return new NettyNetworkServer(debugLogging);
    }

    protected OutputStream createDaemonOutputListener() {
        return new NullOutputStream();
    }
}