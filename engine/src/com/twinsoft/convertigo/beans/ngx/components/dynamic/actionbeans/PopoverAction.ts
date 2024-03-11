    /**
     * Function PopoverAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PopoverAction(page: C8oPageBase, props, vars) : Promise<any> {
        let q:string = props.page; // qname of page
        let p:string = q.substring(q.lastIndexOf('.')+1);
        let version:string = props.tplVersion ? props.tplVersion : '';
        //let greater: any = typeof page["compare"]!== "undefined" ? page["compare"]("7.7.0.2", version) : version.localeCompare("7.7.0.2");
        //let v:any = greater ? p : page.getPageByName(p);
        //let data = props.data ? props.data: {} 

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
        
        const openPopover = async (resolve) => {
            let popoverCtrl = page.getInstance(PopoverController)
            let pop = await popoverCtrl.create({
                mode              : props.mode ? props.mode : undefined,
                component         : props.component,
                componentProps    : props.data,
                keyboardClose     : props.keyboardClose,
                showBackdrop      : props.showBackdrop,
                backdropDismiss   : props.enableBackdropDismiss,
                animated          : props.animated,
                enterAnimation    : props.enterAnimation ? props.enterAnimation : undefined,
                leaveAnimation    : props.leaveAnimation ? props.leaveAnimation : undefined,
                cssClass          : props.cssClass ? props.cssClass : undefined,
                event             : props.event,
                translucent       : props.translucent,
                
                id				: props.id ? props.id : undefined,
                alignment    	: props.alignment ? props.alignment : undefined,
                arrow    		: props.arrow ? props.arrow : undefined,
                dismissOnSelect	: props.dismissOnSelect ? props.dismissOnSelect : false,
                htmlAttributes  : props.htmlAttributes ? props.htmlAttributes : undefined,
                reference    	: props.reference ? props.reference : undefined,
                side    		: props.side ? props.side : undefined,
                size    		: props.size ? props.size : undefined
            })
            
            pop.onDidDismiss().then((data) => {
                page.c8o.log.debug("[MB] Popover '"+p+"' dismissed: " + toString(data));
                resolve(data)
            })
            
            return await pop.present();
        }
        
        return new Promise((resolve, reject) => {
            Promise.resolve(openPopover(resolve))
            .then(() => {
                page.c8o.log.debug("[MB] Popover Page '"+p+"' displayed: " + toString(props.data));
            }).catch((error:any) => {reject(error)})
        });
    }