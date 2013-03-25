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

where XXX is the name of the Gorm method we wish to intercept - all methods
expect find*, countBy, addTo*, removeFrom* are supported.

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
# Examples

## Adding/removing roles in spring security model

Grails spring security plugin introduces the following model:

```groovy
   class User {
   }

   class Authority {
   }

   class UserAuthority {
     User user
     Authority role
   }
```

Let's add 'admin' property to User - it will be rendered as a checkbox in a
GUI form. However, setting/cleaning it should be translated into
adding/removing a proper UserAuthority item. Trying to solve it via the
Gorm afterInsert/afterUpdate event wont work - they are triggered at session
flush which usually happens at its end where further database modifications are
not allowed. A possible workaround is to perform these modifications in a
new session, but this could lead to an inconsistent data, in case that actions
performed in the original session must be rolled back, changes requested in
the new session can still be persisted.

Declaring an after interceptor on Gorm delete method solves the issue:

```groovy
   class User {
     static transients = ['admin']
     boolean admin

     void gormAfterDelete() {
       updateRoles()
     }

     private void updateRoles() {
       def role = Authority.ADMIN
       if (admin) {
         UserAuthority.findByUserAndRole(this, role)?.delete()
       } else {
         new UserAuthority(user: this, role: role).save()
       }
     }
   }
```

## Hiding domain object's source

Sometimes is very useful to hide from the rest of the code the source of
object, and to persist it only if it needs to be associated with some other
entities. Imagine that you have a list of countries defined in an XML config
file and the entity is modeled as follows:

```groovy
   class Country {
     String id // iso code
     String name
   }
```

It would be good that the rest of the application does not know if entries
are comming from database or not. You could make CountryService for that, but
it would be more in the spirit of Gorm to define a few after interceptors:


```groovy
   class Country {
     String id // iso code
     String name

     static def gormAfterList(args, result) {
       // read all entries not appearing in result
       // from the config file and return them all
     }

     static def gormAfterGet(args, result) {
        if (!result) {
           // result = get the entry from the config file using
           // the provided key in args[0]
        }
        result
     }
   }
```
