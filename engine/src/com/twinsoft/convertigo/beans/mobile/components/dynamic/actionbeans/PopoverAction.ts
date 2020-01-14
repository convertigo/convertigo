    /**
     * Function PopoverAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    PopoverAction(page: C8oPageBase, props, vars) : Promise<any> {
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
            let data = props.data ? props.data: {} 
        
            let PopoverCtrl = page.getInstance(PopoverController)
            let pop = PopoverCtrl.create(v, data, {
                showBackdrop            : props.showBackdrop,
                enableBackdropDismiss   : props.enableBackdropDismiss,
                cssClass                : props.cssClass
            })
            
            pop.onDidDismiss((data) => {
                page.c8o.log.debug("[MB] Popover '"+p+"' dismissed: " + toString(data));
                resolve(data)
            })
            
            pop.present({ev: props.event}).then((data) => {
                page.c8o.log.debug("[MB] Popover Page '"+p+"' displayed: " + toString(data));
            })
        });
    }