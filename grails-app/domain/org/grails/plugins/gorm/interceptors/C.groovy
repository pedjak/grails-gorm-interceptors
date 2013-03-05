package org.grails.plugins.gorm.interceptors

class C {
    
    
    String value
    
    def beforeSaveInvoked
    
    
    void gormBeforeSave(args) {
        beforeSaveInvoked = args
    }
    
    
}