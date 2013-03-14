package org.grails.plugins.gorm.interceptors

class D {

    String value

    def afterSaveInvoked

    void gormAfterSave(args, result) {
        afterSaveInvoked = [args, result]
    }
}
