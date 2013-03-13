package org.grails.plugins.gorm.interceptors

class E {

    String value

    static def gormAfterGet(args, result) {
        new E()
    }
}
