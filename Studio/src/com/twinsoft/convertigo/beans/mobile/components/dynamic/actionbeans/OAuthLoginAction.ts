    /**
     * Function OAuthLoginAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    OAuthLoginAction(page: C8oPage, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            /*
            let r: string = props.requestable; let m: string = props.marker;
            let rm: string = r + ( m != '' ? '#' : '' ) + m;
            */
            
            page.routerProvider.doOAuthLogin('https://login.microsoftonline.com/common/oauth2/v2.0/authorize?' +     // Authorize URL
                'client_id=d26692c4-4181-45e5-856c-7cae5fc78c4d' +                                  // Client ID as registred in the app portal
                '&response_type=id_token+token' +                                                   // We ask for implicit flow
                '&scope=openid' +                                                                   // Scopes...
                '%20https%3A%2F%2Fgraph.microsoft.com%2FUser.Read' +
                '%20Files.ReadWrite' +
                '&response_mode=fragment&state=12345&nonce=678910',                                 // Implicit flow
                'https://login.live.com/oauth20_desktop.srf',                                           // the call back URL to check (As declared in the app portal)
                'lib_OAuth.loginAzureAdWithAccessToken',                                                // The server sequence to be launched to check the access token
                'lib_OAuth.checkAccessToken'                                                             // The sequence to execute to check access token
            ).then((response: any )=>{
                resolve( response )
            })
            .catch((error: any) => {
                reject( error )
            })
            
            /*
            page.call( rm, page.merge( { __localCache_priority: props.cachePolicy, __localCache_ttl: props.cacheTtl }, vars ), null, 500 )
                .then(( res: any ) => {
                    resolve( res )
                } )
                .catch(( error: any ) => {
                    reject( error )
                } )
            */
        });
    }