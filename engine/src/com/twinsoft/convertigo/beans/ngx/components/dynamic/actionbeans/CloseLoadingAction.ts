    /**
     * Function CloseLoadingAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CloseLoadingAction(page: C8oPageBase, props, vars) : Promise<any> {
        
        const closeLoading = async () => {
            let loadingController = page.getInstance(LoadingController)
            await loadingController.dismiss(props.data);
        }
        
        return new Promise((resolve, reject) => {
            /*try {
                if (page.global["_c8o_loaders"] != undefined) {
                    page.global["_c8o_loaders"].dismiss();
                    delete page.global["_c8o_loaders"];
                }
                resolve();
            }
            catch(err) {
                reject(err)
            }*/
            Promise.resolve(closeLoading())
            .then((data) => {
                resolve(data);
            }).catch((error:any) => {reject(error)})
        });
    }