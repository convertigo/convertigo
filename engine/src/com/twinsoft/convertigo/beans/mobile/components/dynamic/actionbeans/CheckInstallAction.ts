    /**
     * Function UninstallAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CheckInstallAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileOpener : FileOpener = page.getInstance(FileOpener);
            const packageID: string = props.packageID;
            if(packageID == null || packageID == "") {
                reject("packageID must be set");
            }
            fileOpener.appIsInstalled(packageID)
            .then((resp) => {
                let installed: boolean;
                if(resp.status === 0){
                    installed = false;
                    page.router.c8o.log.debug("[MB] CheckInstallAction: " + packageID + " is not installed");
                }
                else{
                    installed = true;
                    page.router.c8o.log.debug("[MB] CheckInstallAction: " + packageID + " is installed");
                }
                resolve(installed);
            })
            .catch((e) =>{
                if(e == "cordova_not_available"){
                    if(!props.mockedResponse){
                        page.router.c8o.log.debug("[MB] CheckInstallAction: cordova isn't available: using mocked response: " + packageID + " is not installed");
                    }
                    else{
                        page.router.c8o.log.debug("[MB] CheckInstallAction: cordova isn't available: using mocked response: " + packageID + " is installed");
                    }
                    resolve(props.mockedResponse);
                } 
                else{
                    page.router.c8o.log.error("[MB] CheckInstallAction: Error" , e);
                    reject(e);
                }
            });
        });
    }