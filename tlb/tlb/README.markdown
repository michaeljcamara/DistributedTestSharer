# Rationale:
A typical problem that teams doing CI (continuous integration) try and solve is to get the build time to an acceptable amount so that frequent checkins are possible. However, with a steady increase in the number of tests, the time taken to run these tests on every checkin keeps increasing. Solving this problem in the build is almost always non trivial. This is where parallelizing builds comes handy. Throwing hardware at this problem is one of the potential solutions to get build time under acceptable limits.

**TestLoadBalancer (TLB)** aims at splitting your entire test suite into mutually exclusive units such that each of the unit can be executed in parallel. Assuming that tests are written independent of each other, which is a best practice in writing tests, the tests can be ordered and arranged in anyway and *TLB* leverages this fact in order to split the test suite and reorder the tests.

# Build from Source

TLB is a Java project that uses *ant* for building, *ivy* for dependency management and *junit* for tests.

Follow these steps to get TLB building locally

* Clone the code base
* Make sure you have:
    * JDK 1.6
    * ant 1.7 or above
    * ant-junit libraries in the ant lib directory
* TLB uses ivy to manage dependencies. Hence, the build will need to be able to download all the dependent libraries from a maven repo.
* In the checkout directory, run `ant`.  This will _download all the deps_ (only the first time), _compile_ and _run all tests_ for all the modules and _build_ the packages.

Checkout the _build.xml_ file to see all the build targets.

# Documentation:
Detailed documentation of TLB concepts and configuration options is available in the [TLB Website](http://test-load-balancer.github.com).

# Supported Frameworks:
Please check the list of supported frameworks on the [TLB Website](http://test-load-balancer.github.com).

# Contributors:
### Core Team:
  * Pavan K Sudarshan [http://github.com/itspanzi](http://github.com/itspanzi "Github Page")
  * Janmejay Singh [http://codehunk.wordpress.com](http://codehunk.wordpress.com "Blog")

### Other Contributors:
  * Chris Turner [http://github.com/BestFriendChris](http://github.com/BestFriendChris "Github Page")

# History:
  *TLB* started in Jan 2010 as an attempt to enhance another similar project called TestLoadBalancer(hosted on code.google.com and github.com). 
  However due to some other issues inheriting the codebase for reuse/enhancement was not possible, besides the direction planned for the new codebase was not aligned with the structure of TestLoadBalancer codebase, so we decided to build *TLB* from scratch. The old TestLoadBalancer originally implemented the idea of load balancing tests based on count, and inspired the creation of *TLB*. The project is not developed or maintained anymore and is not hosted publicly. 

### People behind TestLoadBalancer(the old project)
  * Li Yanhui [http://whimet.blogspot.com](http://whimet.blogspot.com "Blog")
  * Hu Kai [http://iamhukai.com](http://iamhukai.com "Blog")
  * Derek Yang [http://dyang.github.com/](http://dyang.github.com/ "Github Page")
