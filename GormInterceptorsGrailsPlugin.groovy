import grails.util.Metadata

class GormInterceptorsGrailsPlugin {
    def version = "0.0.2-SNAPSHOT"
    def grailsVersion = "1.2 > *"
    def loadAfter = ['domainClass', 'hibernate']
    def pluginExcludes = [
        "grails-app/domain/**"
    ]

    def scm = [url: "http://github.com/pedjak/grails-gorm-interceptors"]
    def documentation = "http://github.com/pedjak/grails-gorm-interceptors"
    def licence = "APACHE"

    def author = "Predrag Knezevic"
    def authorEmail = "pedjak@gmail.com"
    def title = "GORM Interceptors Plugin"
    def description = 'Interceptors for GORM methods (excluding queries)'

    private interceptorNamePattern = ~/gorm(Before|After)(.+)/

    def doWithApplicationContext = { ctx ->
        application.domainClasses.each { dc ->
            def clazz = dc.clazz
            boolean gormMethodsInitialized = !Metadata.current.getGrailsVersion().startsWith('1.')
            def mc = clazz.metaClass
            def interceptedMethods = []

            mc.methods.each { m ->
                String methodName = m.name
                def matcher = interceptorNamePattern.matcher(methodName)
                if (matcher.matches()) {
                    if (!gormMethodsInitialized) {
                        clazz.count()
                        gormMethodsInitialized = true
                    }

                    String type = matcher[0][1].toLowerCase()
                    String gormMethodName = matcher[0][2].toLowerCase()

                    if (gormMethodName in interceptedMethods) return
                    interceptedMethods << gormMethodName

                    // find interceptors
                    def afterInvokeMethod = type == 'after' ? m : mc.methods.find { it.name == "gormAfter${matcher[0][2]}".toString() }
                    def beforeInvokeMethod = type == 'before' ? m : mc.methods.find { it.name == "gormBefore${matcher[0][2]}".toString() }
                    def doInvokeMethod = mc.methods.find { it.name == "gormDo${matcher[0][2]}".toString() }

                    // find all gorm methods
                    mc.methods.findAll { it.name == gormMethodName }.each { gm ->
                        mc.'static'."$gm.name" = interceptMethod(gm, beforeInvokeMethod, afterInvokeMethod, doInvokeMethod)
                    }
                }
            }
        }
    }

    private Closure interceptMethod(method, beforeInvoke, afterInvoke, doInvoke) {

        new Closure(this) {

            Class[] getParameterTypes() {
                method.nativeParameterTypes
            }

            int getMaximumNumberOfParameters() {
                method.nativeParameterTypes ? method.nativeParameterTypes.size() : 0
            }

            def call(Object... args) {
                def result

                if (beforeInvoke) {
                    result = beforeInvoke.invoke(delegate, beforeInvoke.nativeParameterTypes ? [args] as Object[] : null)
                }

                if (doInvoke) {
                    def invokeResult = doInvoke.invoke(delegate, doInvoke.nativeParameterTypes ? [args] as Object[] : null)
                    if (invokeResult) {
                        result = method.invoke(delegate, args)
                    }
                }
                else {
                    result = method.invoke(delegate, args)
                }

                if (afterInvoke) {
                    def resultAfterInvoke = afterInvoke.invoke(delegate, afterInvoke.nativeParameterTypes ? [args, result] as Object[] : null)
                    if (afterInvoke.returnType != Void.TYPE) result = resultAfterInvoke
                }

                result
            }
        }
    }
}
