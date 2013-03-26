import grails.util.Metadata
import javassist.ClassPool
import javassist.CtField
import javassist.CtMethod
import javassist.CtConstructor
import javassist.CtClass

class GormInterceptorsGrailsPlugin {
    def version = "0.1"
    def grailsVersion = "1.2 > *"
    def loadAfter = ['domainClass', 'hibernate']
    def pluginExcludes = [
        "grails-app/domain/**"
    ]

    def issueManagement = [system: 'github', url: 'https://github.com/pedjak/grails-gorm-interceptors/issues']
    def scm = [url: "http://github.com/pedjak/grails-gorm-interceptors"]
    def documentation = "https://github.com/pedjak/grails-gorm-interceptors/blob/master/README.md"
    def licence = "APACHE"

    def author = "Predrag Knezevic"
    def authorEmail = "pedjak@gmail.com"
    def title = "GORM Interceptors Plugin"
    def description = 'Interceptors for GORM methods (excluding find*, countBy*, addTo*, removeFrom* methods)'

    private interceptorNamePattern = ~/gorm(Before|After)(.+)/
    private static final def classPool = ClassPool.default
    private static final def OBJECT_CLASS = classPool.get(Object.name)
    private static final def METAMETHOD_CLASS = classPool.get(MetaMethod.name)
    private static final def CLOSURE_CLASS = classPool.get(Closure.name)
    
    def doWithApplicationContext = { ctx ->
        def plugin = delegate
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
                        mc.'static'."${gm.name}" = interceptMethod(gm, beforeInvokeMethod, afterInvokeMethod, doInvokeMethod)
                    }
                }
            }
        }
    }

    private interceptMethod(method, beforeInvoke, afterInvoke, doInvoke) {        
        // create closure at runtime
        def className = "${method.declaringClass.theClass.name}_${method.name}_interceptor_${method.signature.hashCode()}"
        def clazz = classPool.makeClass(className, CLOSURE_CLASS)
        
        ['beforeInvoke', 'afterInvoke', 'method', 'doInvoke'].each { clazz.addField(new CtField(METAMETHOD_CLASS, it, clazz)) }
        
        // add constructor
        def constructor = new CtConstructor(([OBJECT_CLASS] + [METAMETHOD_CLASS] * 4) as CtClass[], clazz)
        constructor.setBody("""
{
super(\$1);
this.beforeInvoke = \$2;
this.afterInvoke = \$3;
this.method = \$4;
this.doInvoke = \$5;
}
""")
        clazz.addConstructor(constructor)
        
        // implement getParameterTypes
        def m = new CtMethod(clazz.methods.find { it.name == 'getParameterTypes' }, clazz, null)
        m.setBody("""
{
return method.getNativeParameterTypes();
}
""")
        clazz.addMethod(m)
        
        // implement getMaximumNumberOfParameters
        m = new CtMethod(clazz.methods.find { it.name == 'getMaximumNumberOfParameters' }, clazz, null)
        m.setBody("""
{
return ${method.nativeParameterTypes?.size() ?: 0 };
}
""")
        clazz.addMethod(m)
        
        // implement call(Object... args)
        def w = new StringWriter()

        // do we have interceptors that want to inspect method parameters
        def interceptorsWithParams = [beforeInvoke, afterInvoke, doInvoke].any { it?.nativeParameterTypes?.size() > 0 } 
        // create closure
        w << """{
Object result; 
${interceptorsWithParams ? 'Object[] methodArgs = new Object[] { \$1 };' : ''}
"""
        int i = 0
        
        if (beforeInvoke) {
            w << """

    result = beforeInvoke.invoke(getDelegate(), ${beforeInvoke.nativeParameterTypes?.size() > 0 ? 'methodArgs' : '(Object[])null'});

"""
        }
        if (doInvoke) {
            w << """
    Object invokeMethod = doInvoke.invoke(getDelegate(), ${doInvoke.nativeParameterTypes?.size() > 0 ? 'methodArgs' : '(Object[])null'});
    if (invokeMethod instanceof Boolean && ((Boolean)invokeMethod).equals(Boolean.TRUE)) {
      result = method.invoke(getDelegate(), \$1);
    }
"""
        } else {
        w << """
result = method.invoke(getDelegate(), \$1);
"""
        }
        if (afterInvoke) {
            w << """
Object[] afterInvokeArgs = ${afterInvoke.nativeParameterTypes?.size() > 0 ? 'new Object[] { \$1, result }': 'null' };
Object resultAfterInvoke = afterInvoke.invoke(getDelegate(), afterInvokeArgs);
if (afterInvoke.getReturnType() != Void.TYPE) result = resultAfterInvoke;
"""
        }
        w << """
            return result;
}
"""
        m = new CtMethod(clazz.methods.find { it.name == 'call' && it.parameterTypes.size() == 1 && it.parameterTypes[0].isArray() }, clazz, null)
        m.setBody(w.toString())
        clazz.addMethod(m)
        
        // instantiate it
        clazz.toClass().getConstructor(Object, MetaMethod, MetaMethod, MetaMethod, MetaMethod).newInstance(this, beforeInvoke, afterInvoke, method, doInvoke)
    }
}
