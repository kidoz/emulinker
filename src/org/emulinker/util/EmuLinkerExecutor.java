package org.emulinker.util;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class EmuLinkerExecutor extends ThreadPoolExecutor
{
	//private static Log	log	= LogFactory.getLog(EmuLinkerXMLConfig.class);

	//public EmuLinkerExecutor(Configuration config, BlockingQueue queue) throws NoSuchElementException
	@Autowired
	public EmuLinkerExecutor(Configuration config) throws NoSuchElementException
	{
		// super(config.getInt("threadPool.coreSize"), config.getInt("threadPool.maxSize"), config.getLong("threadPool.keepAlive"), TimeUnit.SECONDS, queue);
		// super((config.getInt("server.maxUsers")*2)+10, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		// super.prestartAllCoreThreads();
		super(5, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}
}
