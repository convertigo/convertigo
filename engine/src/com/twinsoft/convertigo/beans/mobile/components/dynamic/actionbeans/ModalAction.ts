    /**
     * Function ModalAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ModalAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let q:string = props.page; // qname of page
            let p:string = q.substring(q.lastIndexOf('.')+1);
            let data = props.data
        
            let Modal = page.getInstance(ModalController);
            let modal = Modal.create(page.getPageByName(p), data, {
                showBackdrop            : props.showBackdrop,
                enableBackdropDismiss   : props.enableBackdropDismiss,
                cssClass                : props.cssClass
            });
            if(props.blockWhileDisplayed){
                modal.onDidDismiss(data => {
                    let dataStr = "";
                    try{
                        dataStr = JSON.Stringify(data);
                    }
                    catch(err){
                        // Do nothing
                    }
                    page.c8o.log.debug("[MB] Modal page Did dismiss: " + dataStr);
                    resolve(data);
                  });
            }
            
            modal.present();
            page.c8o.log.debug("[MB] Modal Page displayed");
            if(!props.blockWhileDisplayed){
                resolve();    
            }
            
        });
    }