You have two eclipse projects: Server and Studio.

* The Studio project contains all resources needed in order to execute
  the C-EMS Studio component.
* The Server project contains all resources needed to launch the C-EMS
  server into a web application server (e.g. Tomcat...)

Be aware that the Server project contains lots of SVN externals links to
the Studio project (especially to Java source code and web content),
which explains that the server project seems having no code activity!