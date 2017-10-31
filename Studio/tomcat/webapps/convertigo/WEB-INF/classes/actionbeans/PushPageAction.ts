    /**
     * Function PushPageAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PushPageAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let q:string = props.page; // qname of page
            let p:string = q.substring(q.lastIndexOf('.')+1);
            page.routerProvider.push(page.getPageByName(p),{},{animate: true, duration: 250})
            .then((res:any) => {resolve(res)}).catch((error:any) => {reject(error)})
        });
    }