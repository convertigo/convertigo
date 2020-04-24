    /**
     * Function CloseLoadingAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CloseLoadingAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                if (page.global["_c8o_loaders"] != undefined) {
                    page.global["_c8o_loaders"].dismiss();
                    delete page.global["_c8o_loaders"];
                }
                resolve();
            }
            catch(err) {
                reject(err)
            }
        });
    }