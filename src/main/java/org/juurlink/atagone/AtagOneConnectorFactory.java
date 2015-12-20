package org.juurlink.atagone;

import java.io.IOException;

import org.juurlink.atagone.domain.Configuration;

/**
 * Create Local or Remote connector, based cn configuration.
 */
public class AtagOneConnectorFactory {

	public AtagOneConnectorInterface getInstance(final Configuration configuration) throws IOException {
		return configuration.isLocal() ? new AtagOneLocalConnector(configuration) : new AtagOneRemoteConnector(configuration);
	}
}
