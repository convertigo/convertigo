    /**
     * Function GooglePlusLoginAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    GooglePlusLoginAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            page.getInstance(Platform).ready().then(() => {
                // If Action selected is Login
                if(props.actionGoogle == "Login"){
                    page.getInstance(GooglePlus).login({
                        "webClientId" : props.webClientId
                    })
                    .then((res: any) => {
                        page.c8o.log.debug(JSON.stringify(res))
                        resolve( res )
                    })
                    .catch((err) => {
                        page.c8o.log.error(JSON.stringify(err))
                        reject( err )
                    })
                }
                //If Action selected is Logout
                else if(props.actionGoogle == "Logout"){
                    page.getInstance(GooglePlus).logout()
                    .then((res: any) => {
                        page.c8o.log.debug(JSON.stringify(res))
                        resolve( res )
                    })
                    .catch((err) => {
                        page.c8o.log.error(JSON.stringify(err))
                        reject( err )
                    })
                }
                else{
                    reject("[MB] GooglePlusLoginAction: No valid action selected");
                }
            });
        });
    }