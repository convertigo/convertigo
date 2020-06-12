	/*
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ClosePopoverAction(page: C8oPageBase, props, vars) : Promise<any> {
        const closePopover = async () => {
            let popoverCtrl = page.getInstance(PopoverController)
            await popoverCtrl.dismiss(props.data);
        }
        
        return new Promise((resolve, reject) => {
            /*try {
                let view = page.getInstance(ViewController)
                view.dismiss().then(() => {
                    page.c8o.log.debug("[MB] popover Page closed");
                    resolve();
                });
            } catch (e) {
                page.c8o.log.warn("[MB] Unable to close popover", e.message);
                resolve();
            }*/
            
            Promise.resolve(closePopover())
            .then((data) => {
                resolve(data);
            }).catch((error:any) => {reject(error)})
      	});
    }
