package org.jmxtrans.embedded;

import jakarta.annotation.Resource;

/**
 * <p>Embedded Jmx Trans Launcher.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
public class EmbeddedJmxTransLauncher {
	 
	@Resource
	protected EmbeddedJmxTrans jmxtrans;
	/** Gets the jmxtrans. */
	
	public EmbeddedJmxTrans getJmxtrans() {
		return jmxtrans;
	}
	/** Sets the jmxtrans. */

	public void setJmxtrans(EmbeddedJmxTrans jmxtrans) {
		this.jmxtrans = jmxtrans;
	}
	
}
