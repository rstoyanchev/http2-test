
Jetty requires ALPN to be in the boot classloader, see [Staring the JVM](http://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-starting).

The jar alpn-boot jar is available in the `lib/` sub-directory. Add the following VM option:
-Xbootclasspath/p:lib/alpn-boot-7.1.0.v20141016.jar
