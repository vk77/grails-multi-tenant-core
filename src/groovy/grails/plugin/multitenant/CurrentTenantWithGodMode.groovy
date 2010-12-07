package grails.plugin.multitenant

import grails.plugin.multitenant.core.CurrentTenantThreadLocal

class CurrentTenantWithGodMode extends CurrentTenantThreadLocal {

    static ThreadLocal<Boolean> godMode = new ThreadLocal<Boolean>()

	def isGodMode(){
		if(godMode.get() == null){
			godMode.set(false)
		}
		godMode.get()
	}
	
	def setGodMode(gm) {
		godMode.set(gm as Boolean)
	}

}