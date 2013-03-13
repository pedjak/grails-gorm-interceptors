package org.grails.plugins.gorm.interceptors

class G {

    String value
    static boolean doGet

    static def gormBeforeGet(args) {
        new E(value: '123')
    }

    static boolean gormDoGet() {
        doGet
    }
}
