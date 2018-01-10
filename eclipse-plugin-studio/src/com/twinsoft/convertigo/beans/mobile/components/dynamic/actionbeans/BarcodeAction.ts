    /**
     * Function BarcodeAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    BarcodeAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            const barcode : BarcodeScanner = page.getInstance(BarcodeScanner);
            barcode.scan(props).then((barcodeData) => {
                page.router.c8o.log.debug("[MB] BarcodeAction Success: " + JSON.stringify(barcodeData));
                resolve(barcodeData)
            })
            .catch((e) => {
                if(e == "cordova_not_available"){
                    page.router.c8o.log.debug("[MB] BarcodeAction :" + e);
                } 
                else{
                    page.router.c8o.log.error("[MB] BarcodeAction: ", e);
                    reject(e); 
                }
            });
        });
    }