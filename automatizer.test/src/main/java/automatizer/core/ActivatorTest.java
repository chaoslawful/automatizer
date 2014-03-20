package automatizer.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import automatizer.core.AutomatizerActivator;

/**
* Sample integration test. In Eclipse, right-click > Run As > JUnit-Plugin. <br/>
* In Maven CLI, run "mvn integration-test".
*/
public class ActivatorTest {

	@Test
	public void veryStupidTest() {
		assertEquals("automatizer.core",AutomatizerActivator.PLUGIN_ID);
		assertTrue("Plugin should be started", AutomatizerActivator.getDefault().started);
	}
}