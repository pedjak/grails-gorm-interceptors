package org.grails.plugins.gorm.interceptors

import grails.plugin.spock.IntegrationSpec

class InterceptorSpec extends IntegrationSpec {

    def "interceptor without params invoked"() {
        given:
        def a = new A(value: '123')

        when:
        def result = a.save()

        then:
        a.beforeSaveInvoked
        a.afterSaveInvoked
        a.doSave
        a.id
        result.id == a.id
    }

    def "interceptors with params invoked"() {
        given:
        def b = new B(value: '123')

        when:
        def result = b.save(true)

        then:
        b.beforeSaveInvoked == [true] as Object[]
        b.afterSaveInvoked == [[true] as Object[], b]
        b.doSave == [true] as Object[]
        b.id
        result.id == b.id
    }

    def "before interceptors with params invoked"() {
        given:
        def o = new C(value: '123')

        when:
        def result = o.save(true)

        then:
        o.beforeSaveInvoked == [true] as Object[]
        o.id
        result.id == o.id
    }

    def "after interceptors with params invoked"() {
        given:
        def o = new D(value: '123')

        when:
        def result = o.save(true)

        then:
        o.afterSaveInvoked == [[true] as Object[], o]
        o.id
        result.id == o.id
    }

    def "after interceptor changes the return value when result was null"() {

        expect:
        !E.get(1).value
    }

    def "after interceptor changes the return value when result was not null"() {
        given:
        def e = new E(value: '12')
        e.save()

        expect:
        !E.get(e.id).value
    }

    def "after interceptor changes the return value of before interceptor"() {

        expect:
        F.get(1).value == 'xxx'
    }

    def "return value fixed by before interceptor"() {
        given:
        G.doGet = false

        expect:
        G.get(1).value == '123'
    }

    def "return value of before interceptor overwritten by gorm method"() {
        given:
        G.doGet = true

        expect:
        !G.get(1)
    }

    def "before interceptor throws exception"() {
        when:
        H.get(1)

        then:
        thrown(RuntimeException)
    }
}
