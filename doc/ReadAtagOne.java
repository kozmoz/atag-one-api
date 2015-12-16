import java.io.IOException;
import java.util.Map;

import org.juurlink.atagone.AtagOneConnectorInterface;
import org.juurlink.atagone.AtagOneLocalConnector;
import org.juurlink.atagone.AtagOneRemoteConnector;
import org.juurlink.atagone.domain.PortalCredentials;

/**
 * Example class how to use the ATAG One API library in Java.
 */
public class ReadAtagOne {

	/**
	 * Main start method.
	 */
	public static void main(String[] args) throws IOException {

		// Decide if we connect to thermostat in local network org to ATAG One Portal.
		boolean localConnector = true;

		// ATAG One Portal credentials.
		PortalCredentials portalCredentials = PortalCredentials.builder()
			.emailAddress("p6ssw0rd")
			.password("p6ssw0rd")
			.build();

		// Create local or remote connector.
		AtagOneConnectorInterface atagOneConnector = (localConnector) ? new AtagOneLocalConnector() : new AtagOneRemoteConnector(portalCredentials);

		// First login, both local and remote.
		atagOneConnector.login();

		// Get diagnostics.
		final Map<String, Object> diagnostics = atagOneConnector.getDiagnostics();

		// Print results.
		for (Map.Entry<String, Object> entry : diagnostics.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
	}
}
