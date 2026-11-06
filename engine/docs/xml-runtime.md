# XML runtime checks

The legacy JAXB XJC 2.2.11 schema-to-Java compiler is no longer bundled. This does not remove JAXB at runtime: Hibernate still brings Jakarta JAXB, also used by the engine's XSD type conversions. Project code that directly invokes the former compiler is a separate compatibility surface from the built-in XML operations.

The focused runtime check exercises engine DOM parsing/serialization, StAX factories and service discovery, Jakarta JAXB marshalling and XSD conversions, SOAP 1.1/1.2 through SAAJ/Axiom, a local self-contained WSDL import and sample SOAP request generation, and Hibernate XML configuration with an in-memory HSQLDB round trip:

```sh
./gradlew --max-workers=2 :engine:xmlRuntimeCompatibilityTest -x :engine:publish -x :engine:publishToMavenLocal
```

It does not publish SoapUI or require a remote SOAP endpoint or database server. Complete qualification must also cover real WSDL imports with included schemas and policies, SOAP transactions/attachments, application mappings and XML/PDF/SVG transformations.
