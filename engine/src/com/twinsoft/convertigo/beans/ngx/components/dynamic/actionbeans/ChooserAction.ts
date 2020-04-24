    /**
     * Function ChooserAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ChooserAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const chooserI : Chooser = page.getInstance(Chooser);
            let mimeType = props.mimeType != null ? props.mimeType : null;
            chooserI.getFile(mimeType)
            .then((file)=>{
                resolve(file);
            })
            .catch((error:any)=>{
                if(error == "cordova_not_available"){
                    page.router.c8o.log.debug("[MB] ChooserAction: cordova isn't available: using mocked response: " + props.mockedResponse);
                    resolve(props.mockedResponse);
                }
                else{
                    page.router.c8o.log.error("[MB] FileChooserAction :", error);
                    reject(error);
                }
            });
        });
    }