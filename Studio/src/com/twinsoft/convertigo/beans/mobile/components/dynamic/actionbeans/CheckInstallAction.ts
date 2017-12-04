    /**
     * Function UninstallAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CheckInstallAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fileOpener : FileOpener = page.getInstance(FileOpener);
            const packageID: string = props.packageID;
            if(packageID == null) {
                reject("packageID must be set");
            }
            fileOpener.appIsInstalled(packageID)
            .then((resp) => {
                page.router.c8o.log.debug("[MB] CheckInstallAction: " + packageID + "status is " + resp);
                resolve(resp);
            })
            .catch((e) =>{
                if(e == "cordova_not_available"){
                    page.router.c8o.log.debug("[MB] CheckInstallAction: " + e);
                } 
                else{
                    page.router.c8o.log.error("[MB] CheckInstallAction: Error" , e);
                    reject(e);
                }
            });
        });
    }