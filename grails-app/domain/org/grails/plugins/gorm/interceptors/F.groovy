package org.grails.plugins.gorm.interceptors

class F {

    String value

    static def gormAfterGet(args, result) {
        result.value = 'xxx'
        result
    }

    static def gormBeforeGet(args) {
        new E(value: '123')
    }

    static boolean gormDoGet() {
        false
    }
}
