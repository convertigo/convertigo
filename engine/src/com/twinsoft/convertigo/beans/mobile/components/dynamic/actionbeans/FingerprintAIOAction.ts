    /**
     * Function FingerprintAIOAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    FingerprintAIOAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            const fingerprint : FingerprintAIO  = page.getInstance(FingerprintAIO);
            let params : any =  {"clientId": ""};
            if(props.clientID == null) {
                reject("[MB] FingerprintAIOAction: client ID must be set");
            }
            else{
                params["clientId"] = props.clientID;
            }
            if(props.clientSecret != null){
                params["clientSecret"] = props.clientSecret;
            }
            if(props.disableBackup != null){
                params["disableBackup"] = props.disableBackup;
            }
            if(props.localizedFallbackTitle != null){
                params["localizedFallbackTitle"] = props.localizedFallbackTitle;
            }
            if(props.localizedReason != null){
                params["localizedReason"] = props.localizedReason;
            }
            
            fingerprint.isAvailable()
            .then((response)=>{
                fingerprint.show(params)
                .then((result: any) => {
                    page.router.c8o.log.debug("[MB] FingerprintAIOAction: ", result);
                    resolve(result);
                })
                .catch((error: any) => {
                    page.router.c8o.log.error("[MB] FingerprintAIOAction: ", error);
                    reject(error);
                });
            })
            .catch((err)=>{
                page.router.c8o.log.debug("[MB] FingerprintAIOAction: ", err);
                reject(error);
            });             
        });
    }