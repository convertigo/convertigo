    /**
     * Function OAuthLoginAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    OAuthLoginAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let clientid = props.clientid;
            let provider = props.provider;
            let tenantid = props.tenantid ? props.tenantid:"common";
            let scope, response_mode, response_type, callbackurl, oAuthUrl
            let loginRequestable
            let checkAccessTokenRequestable
            if (clientid && provider) {
                page.getInstance(Platform).ready().then(() => {                                         // Wait for CDV Plugins to be initialized                
                    switch(provider) {
                        case 'azure':
                            scope = props.scope ? props.scope :'openid' +                               // Scopes...
                                    '%20https%3A%2F%2Fgraph.microsoft.com%2FUser.Read' +
                                    '%20Files.ReadWrite'
                            response_mode = 'fragment&state=12345&nonce=678910'                         // Ask implicitflow
                            response_type = 'id_token+token'

                            if (props.callbackurl) {
                                callbackurl   = props.callbackurl 
                            } else {
                                callbackurl = window["cordova"] != undefined ?
                                        'https://login.live.com/oauth20_desktop.srf' :
                                        page.c8o.endpointConvertigo + "/projects/lib_OAuth/getToken.html"   // the call back URL to check (As declared in the app portal)
                            }
                                
                            oAuthUrl = 'https://login.microsoftonline.com/' + tenantid + '/oauth2/v2.0/authorize?' +     
                                'client_id=' + clientid +                                             
                                '&response_type='+ response_type +                                    
                                '&scope=' + scope +
                                '&response_mode=' + response_mode
                                
                            loginRequestable = props.loginRequestable ? props.loginRequestable : "lib_OAuth.loginAzureAdWithAccessToken"
                            checkAccessTokenRequestable = props.checkAccessTokenRequestable ? props.checkAccessTokenRequestable : "lib_OAuth.checkAccessToken"
                            break
                            
                        case "linkedin":
                            scope = props.scope ? props.scope : 'r_basicprofile'
                            response_type = 'code'
                                    
                            oAuthUrl = 'https://www.linkedin.com/oauth/v2/authorization?' +     
                                'client_id=' + clientid +                                             
                                '&response_type='+ response_type +                                    
                                '&scope=' + scope +
                                '&state=c8ocsrf'
                        
                            loginRequestable = props.loginRequestable ? props.loginRequestable : "lib_OAuth.loginLinkedInWithCode"
                            checkAccessTokenRequestable = props.checkAccessTokenRequestable ? props.checkAccessTokenRequestable : "lib_OAuth.checkAccessTokenLinkedIn"
                            if (props.callbackurl) {
                                callbackurl   = props.callbackurl 
                            } else {
                                callbackurl   = window["cordova"] != undefined ? 
                                    'https://www.convertigo.com/authorize':
                                    page.c8o.endpointConvertigo + "/projects/lib_OAuth/getTokenLinkedIn.html"
                            }
                            break

                        case "openid":
                            scope = props.scope ? props.scope : 'openid'
                            response_type = props.response_type ?  props.response_type:'id_token+token'
                                    
                            if (!props.authorization_endpoint) {
                                page.c8o.log.error("[MB] OAuth login, Authorization endpoint no set for OpenID provider")
                                reject("[MB] OAuth login, Authorization endpoint no set for OpenID provider")
                                return
                            }    

                            
                            if (!props.callbackurl) {
                                page.c8o.log.error("[MB] OAuth login, redirect URI  no set for OpenID provider")
                                reject("[MB] OAuth login, redirect URI  no set for OpenID provider")
                                return
                            }    
                            callbackurl = props.callbackurl
                            oAuthUrl = props.authorization_endpoint + '?' +
                                'client_id=' + clientid +                                             
                                '&response_type='+ response_type +                                    
                                '&scope=' + scope +
                                '&state=c8ocsrf' +
                                '&nonce=' + Date.now();
                        
                            loginRequestable = props.loginRequestable ? props.loginRequestable : "lib_OAuth.loginOpenID"
                            checkAccessTokenRequestable = props.checkAccessTokenRequestable ? props.checkAccessTokenRequestable : "lib_OAuth.checkAccessOpenID"
                            break
                            
                        default:
                            page.c8o.log.error("[MB] OAuth login, invalid provider type")
                    }
                        
                    page.routerProvider.doOAuthLogin(
                        oAuthUrl, 
                        callbackurl,
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
                reject("[MB] OAuth login, Missing Parameters")
            }
        });
    }
