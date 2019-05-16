    /**
     * Function ShowLoadingAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ShowLoadingAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                if(page.global["_c8o_loaders"] != undefined){
                    resolve();
                }
                else{
                    let content = props.content != undefined ? props.content: "";
                    page.global["_c8o_loaders"] = page.loadingCtrl.create({content: content});
                    page.global["_c8o_loaders"].present();
                    resolve();
                }
            }
            catch(err) {
                reject(err)
            }
        });
    }