    /**
     * Function ModalAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ModalAction(page: C8oPageBase, props, vars) : Promise<any> {
        
        function toString(data) {
            if (data) {
                try {
                    return JSON.stringify(data);
                } catch(e) {
                    return data.toString();
                }
            } else {
               return "no data"; 
            }
        }
        
        return new Promise((resolve, reject) => {
            let q:string = props.page; // qname of page
            let p:string = q.substring(q.lastIndexOf('.')+1);
            let version:string = props.tplVersion ? props.tplVersion : '';
            let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.7.0.2", version) : version.localeCompare("7.7.0.2");
            let v:any = greater ? p : page.getPageByName(p);
            let data = props.data
        
            let Modal = page.getInstance(ModalController);
            let modal = Modal.create(v, data, {
                showBackdrop            : props.showBackdrop,
                enableBackdropDismiss   : props.enableBackdropDismiss,
                cssClass                : props.cssClass
            });
            
            let modals = page.router.sharedObject["ModalPages"];
            if (modals == undefined) {
                page.router.sharedObject["ModalPages"] = [];
            }
            
            modal.onDidDismiss((data) => {
                // case view not dismissed using CloseModal
                let index = page.router.sharedObject["ModalPages"].indexOf(modal);
                if (index != -1) {
                    page.router.sharedObject["ModalPages"] = page.router.sharedObject["ModalPages"].splice(index,1);
                }
                
                page.c8o.log.debug("[MB] Modal Page '"+p+"' dismissed: " + toString(data));
                if (props.blockWhileDisplayed) {
                    resolve(data)
                }
            })
            
            modal.present().then((data) => {
                page.router.sharedObject["ModalPages"].push(modal);
                
                page.c8o.log.debug("[MB] Modal Page '"+p+"' displayed: " + toString(data));
                if (!props.blockWhileDisplayed) {
                    resolve();
                }
            })
        });
    }