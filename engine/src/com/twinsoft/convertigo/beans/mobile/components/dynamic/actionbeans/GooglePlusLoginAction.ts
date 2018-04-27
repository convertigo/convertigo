    /**
     * Function GooglePlusLoginAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    GooglePlusLoginAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            page.getInstance(Platform).ready().then(() => {
                // If Action selected is Login
                if(props.actionGoogle == "Login"){
                    page.getInstance(GooglePlus).login({
                        "webClientId" : props.webClientId
                    })
                    .then((res: any) => {
                        page.c8o.log.debug("[MB] GooglePlusLoginAction: logged in: " + JSON.stringify(res));
                        resolve(res);
                    })
                    .catch((err) => {
                        if(err == "cordova_not_available"){
                            let resp : any = {}
                            resp.email = props.mockedResponseE;
                            resp.userId = props.mockedResponseU;
                            resp.displayName = props.mockedResponseD;
                            resp.familyName = props.mockedResponseF;
                            resp.givenName = props.mockedResponseG;
                            resp.imageUrl = props.mockedResponseImg;
                            resp.idToken = props.mockedResponseId;
                            resp.serverAuthCode = props.mockedResponseS;
                            resp.accessToken = props.mockedResponseA;
                            page.router.c8o.log.debug("[MB] GooglePlusLoginAction: cordova isn't available: using mocked response for logged in: " + JSON.stringify(resp));
                            resolve(resp);
                        }
                        else{
                            page.c8o.log.error("[MB] GooglePlusLoginAction: ",err);
                            reject(err);
                        }
                        
                    })
                }
                //If Action selected is Logout
                else if(props.actionGoogle == "Logout"){
                    page.getInstance(GooglePlus).logout()
                    .then((res: any) => {
                        page.c8o.log.debug("[MB] GooglePlusLoginAction: logged out: " + JSON.stringify(res));
                        resolve(res);
                    })
                    .catch((err) => {
                        if(err == "cordova_not_available"){
                            let resp : any = {}
                            page.router.c8o.log.debug("[MB] GooglePlusLoginAction: cordova isn't available: using mocked response for logged out: " + props.mockedResponseLogout);
                            resolve(props.mockedResponseLogout);
                        }
                        else{
                            page.c8o.log.error("[MB] GooglePlusLoginAction: ",err);
                            reject(err);
                        }
                    })
                }
                else{
                    reject("[MB] GooglePlusLoginAction: The Action selected does not exists. Please choose one from the listbox.")
                }
            });
        });
    }