package org.jmxtrans.embedded;

import java.util.Map;
import java.util.concurrent.Callable;

import org.jmxtrans.embedded.util.network.MacAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ExtendedResultNameStrategy extends ResultNameStrategy {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Map<String, Callable<String>> _expressionEvaluators = null;
    
	public ExtendedResultNameStrategy() {
		super();
		try {

			String macAddress = MacAddressUtils.getMacAddress();
			// getMacAddress() can return null in environments without a detectable
			// mac address (containers, some CI boxes). Fall back to a placeholder
			// so that downstream #mac_address# expressions do not throw NPEs.
			if (macAddress == null) {
				macAddress = "unknown";
			}
            registerExpressionEvaluator("mac_address", macAddress);
            registerExpressionEvaluator("escaped_mac_address", macAddress.replaceAll("\\:", "_"));

        } catch (Exception e) {
            logger.error("Exception resolving localhost, expressions like #hostname#, #canonical_hostname# or #hostaddress# will not be available", e);
        }
	}
	 
}
