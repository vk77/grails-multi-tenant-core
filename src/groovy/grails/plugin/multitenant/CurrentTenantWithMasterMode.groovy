package grails.plugin.multitenant

import grails.plugin.multitenant.core.CurrentTenantThreadLocal

class CurrentTenantWithMasterMode extends CurrentTenantThreadLocal {

    static ThreadLocal<Boolean> masterMode = new ThreadLocal<Boolean>()

	def isMasterMode(){
		if(masterMode.get() == null){
			masterMode.set(false)
		}
		masterMode.get()
	}
	
	def setMasterMode(gm) {
		masterMode.set(gm as Boolean)
	}

}