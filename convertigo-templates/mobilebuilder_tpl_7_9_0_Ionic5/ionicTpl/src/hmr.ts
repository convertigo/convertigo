import { NgModuleRef, ApplicationRef } from '@angular/core';
import { createNewHosts } from '@angularclass/hmr';

export const hmrBootstrap = (module: any, bootstrap: () => Promise<NgModuleRef<any>>) => {
  let ngModule: NgModuleRef<any>;
  module.hot.accept();
  bootstrap().then(mod => ngModule = mod);
  module.hot.dispose(() => {
    const appRef: ApplicationRef = ngModule.injector.get(ApplicationRef);
    const elements = appRef.components.map(c => c.location.nativeElement);
    const makeVisible = createNewHosts(elements);
    ngModule.destroy();
    makeVisible();
  });
  module.hot.addStatusHandler(status => {
	  // React to the current status...
	  if (window["HmrBuildStatus"] == undefined || window["HmrBuildStatus"] != "idle") {
		window["HmrBuildStatus"] = status;
		if (status == "check") {
		  	console.log("*** HMR Building =" + status)
			var buildDiv = document.createElement('span')
			buildDiv.id = "BuildDiv"
			buildDiv.innerHTML = `
				<span style="
					font-size: 25px;
				    color: white;
				    background-color: #222222;
				    z-index: 100;
				    position: absolute;
				    top: 50%;
				    left: 50%;
				    transform: translate(-50%,-50%);					
					border: solid 1px white;
					padding: 5px;
					border-radius: 5px">Building...
				</span>"
			`
			document.body.appendChild(buildDiv)
		}
	  } else {
		window["HmrBuildStatus"] = status;
		console.log("*** HMR Build done")
		var buildDiv = document.getElementById("BuildDiv")
		if (buildDiv) {
			buildDiv.parentNode.removeChild(buildDiv)
		}
	 }
  });
};
