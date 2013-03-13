package org.grails.plugins.gorm.interceptors

class H {

    String value

    static def gormBeforeGet(args) {
        throw new RuntimeException()
    }
}
