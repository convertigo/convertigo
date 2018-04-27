    /**
     * Function UninstallAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    UninstallAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileOpener : FileOpener = page.getInstance(FileOpener);
            const packageID: string = props.packageID;
            if(packageID == null) {
                reject("packageID must be set");
            }
            fileOpener.uninstall(packageID)
            .then((resp) => {
                page.router.c8o.log.debug("[MB] UninstallAction: Uninstaller for " + packageID + " has launched");
                resolve(true);
            })
            .catch((e) =>{
                if(e == "cordova_not_available"){
                    page.router.c8o.log.debug("[MB] UninstallAction: cordova isn't available: using mocked response: " + props.mockedResponse);
                    resolve(props.mockedResponse);
                }
                else if(e.status == 9){
                    page.router.c8o.log.debug("[MB] UninstallAction: Failed to uninstall " + packageID, e);
                    resolve(false);
                }
                else{
                    page.router.c8o.log.error("[MB] UninstallAction: Failed to uninstall " + packageID, e);
                    reject(e);
                }
            });
        });
    }