    /**
     * Function ModalAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    ModalAction(page: C8oPageBase, props, vars) : Promise<any> {
        let q:string = props.page; // qname of page
        let p:string = q.substring(q.lastIndexOf('.')+1);
        let version:string = props.tplVersion ? props.tplVersion : '';
        //let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.7.0.2", version) : version.localeCompare("7.7.0.2");
        //let v:any = greater ? p : page.getPageByName(p);
        
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

        const openModal = async (resolve) => {
            let modalController = page.getInstance(ModalController);
            let modal = await modalController.create({
              mode              : props.mode ? props.mode : undefined,
              component         : props.component,
              componentProps    : props.data,
              keyboardClose     : props.keyboardClose,
              showBackdrop      : props.showBackdrop,
              swipeToClose      : props.swipeToClose,
              backdropDismiss   : props.enableBackdropDismiss,
              animated          : props.animated,
              enterAnimation    : props.enterAnimation ? props.enterAnimation : undefined,
              leaveAnimation    : props.leaveAnimation ? props.leaveAnimation : undefined,
              presentingElement : props.presentingElement ? props.presentingElement : undefined,
              cssClass          : props.cssClass ? props.cssClass : ''
            });

            /*let modals = page.routerProvider.sharedObject["ModalPages"];
            if (modals == undefined) {
                page.routerProvider.sharedObject["ModalPages"] = [];
            }*/
            
            modal.onDidDismiss().then((data) => {
                // case view not dismissed using CloseModal
                /*let index = page.routerProvider.sharedObject["ModalPages"].indexOf(modal);
                if (index != -1) {
                    page.routerProvider.sharedObject["ModalPages"] = page.routerProvider.sharedObject["ModalPages"].splice(index,1);
                }*/
                
                page.c8o.log.debug("[MB] Modal Page '"+p+"' dismissed: " + toString(data));
                if (props.blockWhileDisplayed) {
                    resolve(data)
                }
            })

            return await modal.present();
        }
        
        return new Promise((resolve, reject) => {
            Promise.resolve(openModal(resolve))
            .then(() => {
                //page.routerProvider.sharedObject["ModalPages"].push(modal);
                page.c8o.log.debug("[MB] Modal Page '"+p+"' displayed: " + toString(props.data));
                if (!props.blockWhileDisplayed) {
                    resolve();
                }
            }).catch((error:any) => {reject(error)})
        });
    }