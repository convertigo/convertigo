	/*
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    CloseModalAction(page: C8oPageBase, props, vars) : Promise<any> {
        const closeModal = async () => {
            let modalController = page.getInstance(ModalController);
            await modalController.dismiss(props.data);
        }
        
        return new Promise((resolve, reject) => {
            /*let modals = page.routerProvider.sharedObject["ModalPages"];
            if (modals != undefined) {
                let view = page.routerProvider.sharedObject["ModalPages"].pop();
                if (view != undefined) {
                    view.dismiss().then(() => {
                        page.c8o.log.debug("[MB] Modal Page closed");
                        resolve();
                    });
                } else {
                    page.c8o.log.debug("[MB] Invalid Modal Page");
                    resolve();
                }
            } else {
                page.c8o.log.debug("[MB] No Modal Page");
                resolve();
            }*/
            
            Promise.resolve(closeModal())
            .then((data) => {
                resolve(data);
            }).catch((error:any) => {reject(error)})
        });
    }
    