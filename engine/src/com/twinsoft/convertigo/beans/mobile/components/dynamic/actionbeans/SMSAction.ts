    /**
     * Function SMSAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    SMSAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject)=> {
            const sms : SMS = page.getInstance(SMS);
            let options: any = {android:{}};
            if(props.replaceLineBreaks != null){
                options.replaceLineBreaks = props.replaceLineBreaks;
            }
            if(props.intent != null){
                props.intent == true ? options.android.intent = 'INTENT' : null; 
                
            }
            sms.send(props.phoneNumber, props.message)
            .then((buttonIndex: number) => {
                page.router.c8o.log.debug("[MB] SMSAction: " + buttonIndex);
                resolve(buttonIndex);
            })
            .catch((err)=>{
                page.router.c8o.log.error("[MB] SMSAction: ", err);
                reject(err);
            })
        });
    }