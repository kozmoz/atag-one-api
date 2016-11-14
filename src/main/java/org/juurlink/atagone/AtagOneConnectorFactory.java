package org.juurlink.atagone;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.juurlink.atagone.domain.Configuration;

import lombok.NonNull;

/**
 * Create Local or Remote connector, based cn configuration.
 */
public class AtagOneConnectorFactory {

    public AtagOneConnectorInterface getInstance(final @Nonnull @NonNull Configuration configuration) throws IOException {
        return configuration.isLocal() ?
            new AtagOneLocalConnector(configuration) :
            new AtagOneRemoteConnector(configuration);
    }
}
