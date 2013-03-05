package org.grails.plugins.gorm.interceptors

class B {
    
    
    String value
    
    def beforeSaveInvoked
    
    def afterSaveInvoked
    
    def doSave
    
    void gormBeforeSave(args) {
        beforeSaveInvoked = args
    }
    
    boolean gormDoSave(args) {
        doSave = args
        true
    }
    
    void gormAfterSave(args, result) {
        afterSaveInvoked = [args, result]
    }
}