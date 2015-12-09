package org.juurlink.atagone;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.juurlink.atagone.domain.OneInfo;

public class AtagOneAppTest {

	private AtagOneApp atagOneApp;

	@Before
	public void setUp() {
		atagOneApp = new AtagOneApp();
	}

	@Test
	@Ignore("Cannot test this in a Junit test")
	public void testSearchOnes() throws Exception {
		final OneInfo oneInfo = atagOneApp.searchOnes();
		System.out.println("oneInfo = " + oneInfo);
	}
}
