# Default StAX providers

Woodstox Core is no longer bundled, including its transitive contribution through Axiom. On the default classpath, StAX input/output factories fall back to the JDK implementations; the event factory remains Xerces through its service descriptor.

StAX2 API is retained for Axiom compatibility, as are the SOAP and JAXB runtimes. Explicit custom providers or code depending on Woodstox implementation classes are separate compatibility surfaces. No factory is hard-coded to replace application or project configuration.

Regression checks exercise XML read/write, namespaces and special characters, SOAP 1.1/1.2 through SAAJ/Axiom, local WSDL import/request generation and Hibernate XML configuration. Full qualification must also cover real schemas/policies, large XML inputs, SOAP attachments and XML/PDF/SVG transformations; provider selection and serialization details may differ even when the XML is equivalent.
