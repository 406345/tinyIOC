# tinyIOC
a tiny IOC framework for java

# Usage
```
public class Main {
    // create an instance of container
    // you may alse initialize multiple containers as many as you like.
    static IOCManager iocManager = new IOCManager();

    public static void main(String[] args) {
        // scan the package
        iocManager.load("com.demo.tinyioc");
        iocManager.load("com.demo.tinyioc.xxxx");
    }
}

// Here define a class 
package com.demo.tinyioc;

@AutoManage // let the container manage the instance.
public class ClassA {
    ...
}

// use an instance in the container.

@AutoManage //  ClassB MUST be managed by container, so that the ClassA can be injected.
public class ClassB {
    // using like springboot
    @AutoInject
    ClassA classA;
}

```

# By the author
This is a  realy realy simple ioc framework,using in a lite system. It may not be a nice choise using in a complex system.
