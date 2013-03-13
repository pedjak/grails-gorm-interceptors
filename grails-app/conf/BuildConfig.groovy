grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()
	}

	plugins {
		test 'org.grails.plugins:spock:0.5-groovy-1.7'
	}
}
