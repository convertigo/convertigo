    /**
     * Function OAuthLoginAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    OAuthLoginAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let clientid = props.clientid;
            let provider = props.provider;
            let scope, response_mode, response_type, callbackurl, oAuthUrl
            let loginRequestable
            let checkAccessTokenRequestable
            if (clientid && provider) {
                page.getInstance(Platform).ready().then(() => {                                 // Wait for CDV Plugins to be initialized                
                    switch(provider) {
                        case 'azure':
                            scope = 'openid' +                                                  // Scopes...
                                    '%20https%3A%2F%2Fgraph.microsoft.com%2FUser.Read' +
                                    '%20Files.ReadWrite'
                            response_mode = 'fragment&state=12345&nonce=678910'                 // Ask implicitflow
                            response_type = 'id_token+token'
                            callbackurl = page.c8o.endpointConvertigo + "/projects/lib_OAuth/getToken.html"
                            oAuthUrl = 'https://login.microsoftonline.com/common/oauth2/v2.0/authorize'
                            loginRequestable = "lib_OAuth.loginAzureAdWithAccessToken"
                            checkAccessTokenRequestable = "lib_OAuth.checkAccessToken"
                            break
                            
                        default:
                            page.c8o.log.error("[MB] OAuth login, invalid provider type")
                    }
                        
                    page.routerProvider.doOAuthLogin(oAuthUrl + '?' +                           // Authorize URL
                        'client_id=' + clientid +                                               // Client ID as registred in the app portal
                        '&response_type='+ response_type +                                      // We ask for implicit flow
                        '&scope=' + scope +
                        '&response_mode=' + response_mode,                                      // Implicit flow
                        callbackurl,                                                            // the call back URL to check (As declared in the app portal)
                        loginRequestable,                                                       // The server sequence to be launched to login
                        checkAccessTokenRequestable,                                            // The server sequence to be launched to check the access token
                    ).then((response: any )=>{
                        resolve( response )
                    })
                    .catch((error: any) => {
                        page.c8o.log.error("[MB] OAuth login, Login error" + error)
                        reject( error )
                    })
                })
            }
            else {
                page.c8o.log.error("[MB] OAuth login, Missing Parameters")
            }
        });
    }
