class GormInterceptorsGrailsPlugin {
    // the plugin version
    def version = "0.0.2-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    
    def loadAfter = ['domainClass', 'hibernate']
    
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            "grails-app/domain/**",
            "test/**"
    ]

    def scm = [url: "http://github.com/pedjak/grails-gorm-interceptors"]
    def documentation = "http://github.com/pedjak/grails-gorm-interceptors"
    def licence = "APACHE"

    def author = "Predrag Knezevic"
    def authorEmail = "pedjak@gmail.com"
    def title = "Interceptors for GORM methods (excluding queries)"
    def description = '''
'''

    def interceptorNamePattern = ~/gorm(Before|After)(.+)/
    
    def doWithApplicationContext = { ctx ->
        def plugin = delegate
        ctx.grailsApplication.domainClasses.each { dc ->
            def clazz = dc.clazz
            boolean gormMethodsInitialized = false
            def mc = clazz.metaClass
            def interceptedMethods = []
            
            mc.methods.each { m ->
                def methodName = m.name
                def matcher = interceptorNamePattern.matcher(methodName)
                if (matcher.matches()) {
                    if (!gormMethodsInitialized) {
                        clazz.count()
                        gormMethodsInitialized = true
                    }
                    
                    def type = matcher[0][1].toLowerCase()
                    def gormMethodName = matcher[0][2].toLowerCase()
                    
                    if (gormMethodName in interceptedMethods) return
                    interceptedMethods << gormMethodName
                    
                    // find interceptors
                    def afterInvokeMethod = type == 'after' ? m : mc.methods.find { it.name == "gormAfter${matcher[0][2]}".toString() } 
                    def beforeInvokeMethod = type == 'before' ? m : mc.methods.find { it.name == "gormBefore${matcher[0][2]}".toString() }
                    def doInvokeMethod = mc.methods.find { it.name == "gormDo${matcher[0][2]}".toString() }
                    
                    // find all gorm methods
                    mc.methods.findAll { it.name == gormMethodName }.each { gm ->                        
                        mc.'static'."${gm.name}" = interceptMethod(gm, beforeInvokeMethod, afterInvokeMethod, doInvokeMethod)   
                    } 
                    
                } 
            }
        }
    }
    
    private def interceptMethod(method, beforeInvoke, afterInvoke, doInvoke) {
        def paramTypes = method.nativeParameterTypes
        def w = new StringWriter()
        
        // create closure
        w << '{ '
        int i = 0
        w << paramTypes.collect { "${it.name} a${i++}" }.join(', ')
        i = 0 
        w << """ ->
def result
def args = ${'[' + paramTypes.collect { "a${i++}" }.join(', ') + '] as Object[]'}
"""
        if (beforeInvoke) {
            w << """
                                    
    result = beforeInvoke.invoke(delegate, beforeInvoke.nativeParameterTypes.size() > 0 ? [args] as Object[] : null)

"""
        }
        if (doInvoke) {
            w << """
    def invokeMethod = doInvoke.invoke(delegate, doInvoke.nativeParameterTypes.size() > 0 ? [args] as Object[] : null)
    if (invokeMethod) {
      result = method.invoke(delegate, args)
    }
"""
        } else {
        w << """
result = method.invoke(delegate, args)
"""
        }
        if (afterInvoke) {
            w << """

def resultAfterInvoke = afterInvoke.invoke(delegate, afterInvoke.nativeParameterTypes.size() > 0 ? [args, result] as Object[] : null)
if (afterInvoke.returnType != Void.TYPE) result = resultAfterInvoke
"""
        }
        w << """
            result
}
"""
        new GroovyShell(new Binding([beforeInvoke: beforeInvoke, afterInvoke: afterInvoke, doInvoke: doInvoke, method: method])).evaluate(w.toString())
    }

}
