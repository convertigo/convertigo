var DOMUtils = {
    createDOM: function (rootElement) {
        return document.implementation.createDocument("", rootElement, null);
    },
    domToString2: function (xmlNode) {
        try {
            // Gecko- and Webkit-based browsers (Firefox, Chrome), Opera.
            return (new XMLSerializer()).serializeToString(xmlNode);
        }
        catch (e) {
            try {
                // Internet Explorer.
                return xmlNode.xml;
            }
            catch (e) {
                // Other browsers without XML Serializer
                alert('Xmlserializer not supported');
            }
        }
        return false;
    }
};
