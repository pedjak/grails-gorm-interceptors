package org.grails.plugins.gorm.interceptors

class A {
    
    
    String value
    
    def beforeSaveInvoked
    
    def afterSaveInvoked
    
    def doSave
    
    void gormBeforeSave() {
        beforeSaveInvoked = true
    }
    
    boolean gormDoSave() {
        doSave = true
        true
    }
    
    void gormAfterSave() {
        afterSaveInvoked = true
    }
}