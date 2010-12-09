package grails.plugin.multitenant

import grails.plugin.multitenant.core.hibernate.TenantEventHandler
import org.hibernate.event.*
import org.hibernate.event.LoadEventListener.LoadType
import org.hibernate.tuple.StandardProperty

import org.hibernate.SessionFactory
import util.hibernate.HibernateEventUtil
import org.hibernate.event.InitializeCollectionEventListener
import grails.plugin.multitenant.TenantId
import grails.plugin.multitenant.core.util.TenantUtils

import org.codehaus.groovy.grails.commons.ConfigurationHolder

class TenantEventHandlerWithMasterMode extends TenantEventHandler{
	

	private Map<String, Class> reflectedCache = [:]
	
	public TenantEventHandlerWithMasterMode() {
  	}

	public boolean onPreInsert(PreInsertEvent preInsertEvent) {
		def shouldFail = false
		boolean hasAnnotation = TenantUtils.isAnnotated(preInsertEvent.getEntity().getClass())
		boolean hasSharedAnnotation = false
		if (ConfigurationHolder.config.tenant.mode != "singleTenant") {
			hasSharedAnnotation = TenantUtils.isAnnotatedAsShared(preInsertEvent.getEntity().getClass())
		}
		
		def setTenantId
		
		def findTenantIdIndex = { where ->
			int result = -1
			for(def property in where) {
				result++
				if (property.getName() == "tenantId") {
					break
				}
			}
			result
		}
		
		if(hasAnnotation){
			setTenantId = preInsertEvent.getEntity().tenantId
			
			if (setTenantId == 0 || setTenantId == null) {
				preInsertEvent.getEntity().tenantId = currentTenant.get()
				StandardProperty[] properties = preInsertEvent.getPersister().getEntityMetamodel().getProperties()
				int tenandIdIndex = findTenantIdIndex(properties)
				if (tenandIdIndex > -1) {
					preInsertEvent.getState()[tenandIdIndex] = currentTenant.get()
				}
			} else {
				if (setTenantId != currentTenant.get()) {
					shouldFail = true
					return shouldFail
				}
			}
		} else if(hasSharedAnnotation){
			setTenantId = preInsertEvent.getEntity().tenants*.tenantId
			if(!setTenantId){
				def tenantToAdd = TenantId.findByTenantId(currentTenant.get()) ?: new TenantId(tenantId: currentTenant.get())
				preInsertEvent.getEntity().addToTenants(tenantToAdd)

			} else {
				if (!(currentTenant.get() in setTenantId)) {
					shouldFail = true
					return shouldFail
				}
			}
		}
		return shouldFail;
	}



	public void onLoad(LoadEvent event, LoadType loadType) {
		if (ConfigurationHolder.config.tenant.withMasterMode && !currentTenant.isMasterMode() && annotated(event.getEntityClassName())) {
			Object result = event.getResult()
			if (result != null) {
				int currentTenant = currentTenant.get()
				def violates = attemptingTenantViolation(event.getEntityClassName(), result, currentTenant)
				if (violates && !event.isAssociationFetch()) {
					println "Trying to load record from a different app (should be ${currentTenant} but was ...)"
					event.setResult null
				} else if(!event.isAssociationFetch()){
					event.setResult result
				}
			}
		}
	}
	
	
  /**
   * Checks before deleting a record that the record is for the current tenant.  THrows an exception otherwise
   */
	public boolean onPreDelete(PreDeleteEvent event) {
		boolean shouldFail = attemptingTenantViolation(event.getEntity().getClass().getName(), event.getEntity(), currentTenant.get())
		if(shouldFail){
			println "Failed Delete Because TenantId Doesn't Match"
		}
		return shouldFail;
	}
	
	public boolean onPreUpdate(PreUpdateEvent preUpdateEvent) {
		boolean shouldFail = attemptingTenantViolation(preUpdateEvent.getEntity().getClass().getName(), preUpdateEvent.getEntity(), currentTenant.get())
		if(shouldFail){
			println "Failed Update Because TenantId Doesn't Match"
		}
		return shouldFail;
	}
	
	
	private Class getClassFromName(String className) {
		if (!reflectedCache.containsKey(className)) {
			Class aClass = this.class.classLoader.loadClass("${className}")
			reflectedCache.put(className, aClass)
		}
		return reflectedCache.get(className)
	}
	
	private annotated(entityClassName){
		boolean hasAnnotation = TenantUtils.isAnnotated(getClassFromName(entityClassName))
		boolean hasSharedAnnotation = false
		if (ConfigurationHolder.config.tenant.mode != "singleTenant") {
			hasSharedAnnotation = TenantUtils.isAnnotatedAsShared(getClassFromName(entityClassName))
		}
		
		hasAnnotation || hasSharedAnnotation
	}
	
	private attemptingTenantViolation(entityClassName, entity, currentTenantId){
		if (ConfigurationHolder.config.tenant.withMasterMode && !currentTenant.isMasterMode() && annotated(entityClassName)) {
			def loaded
			def hasAnnotation = TenantUtils.isAnnotated(getClassFromName(entityClassName))
			if(hasAnnotation){
				loaded = [entity.tenantId]
			} else {
				loaded = entity.tenants*.tenantId
			}
			return !(currentTenantId in loaded)
		}
		false
	}
	

}