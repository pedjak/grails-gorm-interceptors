# Motivation

There are cases when you would like to intercept calls to a Gorm method, in
order to perform additional actions just right before or after, or even to
prevent executing it.

The already existing Gorm approach to declare beforeXXX or afterXXX (XXX
== a Gorm method) methods or closures does not help in cases when interceptor
logic needs to execute queries or update/persist/delete additional objects
because the interceptor is executed at session's flush happening
usually at the session end. This might be problematic, if you need to:

* Execute queries, you need to do this in a new session - all
created/modified objects will be invisible or having old values.

* Modify DB state - you need to do it in a new session, outside
trasaction that has triggered the interceptor at the first place. In case
that the transaction needs to be rolled back, the changes done in the new
session could be eventually persisted.

# Usage

Intercepting a Gorm method is controlled via three additional methods
declared on a domain class:

* gormBeforeXXX
* gormAfterXXX
* gormDoXXX

where XXX is the name of the Gorm method we wish to intercept.

If we need to intercept a static Gorm method, then the interceptor methods
must be declared as static as well.

## gormBeforeXXX

It executes the given code before the Gorm method is invoked. The following
forms are available:

* performing an action not being interested in Gorm method's parameters:

```groovy
    void gormBeforeXXX() {
      // do something
    }
```

* performing an action depending on Gorm method's parameters:

```groovy
    // args is an instance of Object[]
    void gormBeforeXXX(args) {
      // do something
    }
```

* performing an action and returning a value that might be returned to the
method caller:

```groovy
    def gormBeforeXXX() {
      // do something
    }
```

or

```groovy
    // args is an instance of Object[]
    def gormBeforeXXX(args) {
      // do something
    }
```

## gormDoXXX

It is optional and if declared, controls if the interecepted method will be
invoked at all. Two declaration forms are possible:

```groovy
    boolean gormDoDelete() {
       // return true if the intercepted method
       // should be invoked
    }
```

or

```groovy
    // args is an instance of Object[]
    boolean gormDoDelete(args) {
       // return true if the intercepted method
       // should be invoked
    }
```

## gormAfterXXX

This interceptor is optional and if defined, executes the code after the
intercepted method has completed. The following scenarios are supported:

* perform some action not being interested in Gorm method's parameters and
the return value:

```groovy
    void gormAfterDelete() {
      // perform some action
    }
```

* perform some action depending on Gorm method's parameters and
the return value:

```groovy
    // args is an instance of Object[]
    // result is an instance of Object, returned by the intercepted method
    void gormAfterDelete(args, result) {
      // perform some action
    }
```

* replace the returned value with somthing else:

```groovy
    // args is an instance of Object[]
    // result is an instance of Object, returned by the intercepted method
    static def gormAfterGet(args, result) {
      // replace result with something else
    }
```
