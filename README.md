# tinyIOC
a tiny IOC framework for java

# Usage
```
public class Main {
    // create an instance of container
    // you may alse initialize multiple containers as many as you like.
    static IOCManager iocManager = new IOCManager();

    public static void main(String[] args) {
        // scan the packages
        iocManager.load("com.demo.tinyioc");
    }
}

// Here define a class 
package com.demo.tinyioc;

@AutoManage // let the container manage the instance.
public class ClassA {
    ...
}

// Inject a instance.
public class ClassB {
    // using like springboot
    @AutoInject
    ClassA classA;
}

```

# By the author
This is a  realy realy simple ioc framework,using in a lite system. It may not be a nice choise using in a Complex system.