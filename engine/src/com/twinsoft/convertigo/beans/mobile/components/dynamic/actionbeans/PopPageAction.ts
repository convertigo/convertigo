    /**
     * Function PopPageAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PopPageAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            page.routerProvider.pop()
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }