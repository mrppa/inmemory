package com.mrppa.inmemory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.mrppa.inmemory.dataloader.DataLoader;
import com.mrppa.inmemory.maintainer.Maintainer;

/**
 * This class service the memory requests
 * 
 * @author Pasindu Ariyarathna
 *
 */
public class InMemory {

	private static InMemory inMemoryInstance = null;
	private static Logger log = Logger.getLogger(InMemory.class.getName());

	private static HashMap<String, CacheSet> memoryMap = null;
	
	
	private InMemory() {
	}

	/**
	 * To Get the single instance Of InMemeory
	 * 
	 * @return single instance Of InMemory Object
	 * @throws IOException
	 */
	public static synchronized InMemory getInstance() {
		if (inMemoryInstance == null) {
			log.info("InMemory New Instance");
			inMemoryInstance = new InMemory();
			memoryMap = new HashMap<String, CacheSet>();
			inMemoryInstance.initAll();
		}
		return inMemoryInstance;
	}

	/**
	 * To get the memory value of given keys
	 * 
	 * @param cacheSetId
	 *            set id given in configuration
	 * @param key
	 *            key parameters
	 * @return the respective memory value
	 * @throws DataNotReady
	 */
	public String getDataValue(String cacheSetId, String... keys) {
		CacheSet cacheSet=this.findCacheSetByID(cacheSetId);
		if (cacheSet.isLocked()) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return getDataValue(cacheSetId, keys);
		}

		CacheKey ckeySet = new CacheKey();
		for (String key : keys) {
			ckeySet.getKeys().add(key);
		}
		String value = cacheSet.getDataMap().get(ckeySet);
		return value;
	}

	/**
	 * print the memory value of a set
	 * 
	 * @param setId
	 */
	public void printMemory(String cacheSetId) {
		CacheSet cacheSet=this.findCacheSetByID(cacheSetId);
		log.info("PRINT MEMORY-" + cacheSet.getCacheId());
		Iterator<Entry<CacheKey, String>> it = cacheSet.getDataMap().entrySet().iterator();
		while(it.hasNext()) {
			String record="";
			CacheKey cacheKey=it.next().getKey();
			for (String key:cacheKey.getKeys()) {
				record+=key + "\t";
			}
			record+="\t-\t"+cacheSet.getDataMap().get(cacheKey);
			log.info(record);
		}
		log.info("END PRINT MEMORY-" + cacheSet.getCacheId());
	}
	
	private CacheSet findCacheSetByID(String cacheSetId) {
		return memoryMap.get(cacheSetId);
	}

	private void initAll() {
		int setCount = InMemoryProperties.getInstance().getIntPropertyValue("InMemory.TotSetIdCount");
		// Load for each dataset Configuration
		for (int i = 0; i < setCount; i++) {
			this.initDataSet(i+1);
		}

	}

	private void initDataSet(int setIdVal) {
		try {
			String setIdPropVal = "InMemory.SetId_" + String.valueOf(setIdVal);
			String setId = InMemoryProperties.getInstance().getPropertyValue(setIdPropVal);
			if (setId != null) {
				log.info(new StringBuffer("set-").append(setId));
				
				//Create CacheSet
				CacheSet cacheset = new CacheSet();
				cacheset.setCacheId(setId);

				// Load Maintainer Class
				String maintainerClassName = InMemoryProperties.getInstance().getPropertyValue("InMemory." + setId + ".maintainer");
				Maintainer maintainer = null;
				try {
					maintainer = (Maintainer) Maintainer.class.getClassLoader().loadClass(maintainerClassName).newInstance();
				} catch (InstantiationException e) {
					log.fatal("Error instanciate " + maintainerClassName, e);
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					log.fatal("Error instanciate " + maintainerClassName, e);
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					log.fatal("Error instanciate " + maintainerClassName, e);
					e.printStackTrace();
				}
				cacheset.setMaintainer(maintainer);

				// Load Data Loader Class
				String dataLoaderClassName = InMemoryProperties.getInstance().getPropertyValue("InMemory." + setId + ".dataloader");
				DataLoader dataLoader = null;
				try {
					dataLoader = (DataLoader) Maintainer.class.getClassLoader().loadClass(dataLoaderClassName)
							.newInstance();
				} catch (InstantiationException e) {
					log.fatal("Error instanciate " + dataLoaderClassName, e);
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					log.fatal("Error instanciate " + dataLoaderClassName, e);
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					log.fatal("Error instanciate " + dataLoaderClassName, e);
					e.printStackTrace();
				}
				cacheset.setDataLoader(dataLoader);
				
				this.memoryMap.put(setId, cacheset);

				//Start Maintaining
				maintainer.doMaintain(cacheset);
			}
		} catch (Exception e) {
			log.fatal("Error while initializing", e);
		}
	}

}
