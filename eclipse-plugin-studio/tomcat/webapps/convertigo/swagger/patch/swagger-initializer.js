window.onload = function() {
  const defaultResponseContentTypePlugin = function() {
    return {
      statePlugins: {
        oas3: {
          wrapSelectors: {
            responseContentType: (original, system) => (state, path, method) => {
              const selected = original(state, path, method);
              if (selected || !system.specSelectors.isOAS3()) {
                return selected;
              }
              // Derive the default without relying on the response control mounting.
              const responseMap = system.specSelectors.specJsonWithResolvedSubtrees()
                .getIn(["paths", path, method, "responses"]);
              const responses = Object.entries(responseMap?.toJS() || {});
              const hasContent = ([, response]) => Object.keys(response?.content || {}).length > 0;
              const controlling = responses.find(entry => entry[0].startsWith("2") && hasContent(entry))
                || responses.find(entry => entry[0] === "default" && hasContent(entry));
              if (!controlling) {
                return selected;
              }
              const [code, response] = controlling;
              const mediaTypes = Object.keys(response.content);
              const perCode = system.oas3Selectors.responseCodeContentType?.(path, method, code);
              const displayed = system.specSelectors.currentProducesFor([path, method]);
              return [perCode, displayed].find(value => mediaTypes.includes(value)) || mediaTypes[0];
            }
          }
        }
      }
    };
  };
  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    //url: "https://petstore.swagger.io/v2/swagger.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl,
      defaultResponseContentTypePlugin
    ],
    layout: "StandaloneLayout",
    queryConfigEnabled: true
  });

  //</editor-fold>
};
