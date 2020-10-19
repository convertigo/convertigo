import { Component, ViewChild}                              from '@angular/core';
import { ChangeDetectorRef, Injector}                       from '@angular/core';
import { enableProdMode}                                    from '@angular/core';
import { SwUpdate }                                         from '@angular/service-worker';

import { AlertController }                                  from 'ionic-angular';
import { Platform, Nav, App, Events, LoadingController}     from 'ionic-angular';
import { StatusBar }                                        from '@ionic-native/status-bar';
import { TranslateService }                                 from '@ngx-translate/core';

// Convertigo CAF Imports
import { C8oRouter }                                        from 'c8ocaf';
import { C8oRoute, C8oRouteOptions, C8oRouteListener}       from 'c8ocaf'
import { C8oPage, C8oPageBase, C8oCafUtils}                 from "c8ocaf";
import { C8o, C8oSettings, C8oLogLevel,C8oProgress }        from "c8osdkangular";
import { C8oNetworkStatus }                                 from "c8osdkangular";

import { ActionBeans }                                      from '../services/actionbeans.service';

/*
	You can customize your application class by writing code between the :

   		Begin_c8o_XXXX and
   		End_c8o_XXXX
   		
   	Comments.
   	
   	Any code placed outside these these comments will be lost when the application is generated
*/
/*=c8o_AppImports*/

/*Begin_c8o_AppImport*/
/*End_c8o_AppImport*/

/*=c8o_PagesImport*/ 


/*=c8o_AppProdMode*/

@Component({
    templateUrl: 'app.html'
})
export class MyApp extends C8oPageBase {
    @ViewChild(Nav) nav: Nav;
    rootPage : any = /*=c8o_RootPage*/;
    pages : /*=c8o_PageArrayDef*/;
    pagesKeyValue: any;
    public actionBeans: ActionBeans;
    public events : Events;
    /*=c8o_AppDeclarations*/
    
	/*Begin_c8o_AppDeclaration*/
	/*End_c8o_AppDeclaration*/

    constructor(platform: Platform, statusBar: StatusBar, c8o: C8o, router: C8oRouter, loader: LoadingController, private app: App, private translate: TranslateService, ref: ChangeDetectorRef, injector: Injector) {
        
        super(injector, router, loader, ref);
        
        this.actionBeans = this.getInstance(ActionBeans);
        this.events = this.getInstance(Events);
        
        /**
         * declaring page to show in Menu
         */
        this.pages = [/*=c8o_PagesVariables*/];
        this.pagesKeyValue = {/*=c8o_PagesVariablesKeyValue*/}
        this.router.pagesArray = this.pages;
        this.router.pagesKeyValue = this.pagesKeyValue;


        /* ============================================================================================================
           Convertigo Angular Framework (CAF) initialization...
           ============================================================================================================
         * Thanks to Convertigo CAF router we can manage call and navigation :
         *
         * Create a C8orouteOptions in order to define basic and repetitive routes options that will be used in C8oRoute
         * We can define actions such as beforeCall that allow us to run code before the C8o Call
         */

        let tableOptions = new C8oRouteOptions()
            .setBeforeCall(() => {
                //Do what ever has to be done...
            })
            .setAfterCall(()=>{
                //Do what ever has to be done...
            })
            .setDidEnter((page: C8oPage, c8o: C8o) => {
                c8o.log.trace("DidEnter was called from the new routing table and with page : " + page.constructor.name)
            })
            .setDidLeave((page: C8oPage, c8o: C8o) => {
                c8o.log.trace("DidLeave was called from the new routing table and with page : " + page.constructor.name)
            })
            .setTargetAnimate(true)
            .setTargetDuration(250);

        /**
         * The generated Routing Table
         */
         /*=c8o_RoutingTable*/ 
        

        /**
         *  Define a C8oSettings Object in order to declare settings to be used in the C8oInit method
         */
        let settings: C8oSettings = new C8oSettings();
        settings
            .setLogRemote(true)
            .setLogC8o(true)
            .setLogLevelLocal(C8oLogLevel.DEBUG)
            .setKeepSessionAlive(true);
        /*Begin_c8o_AppSettings*/
        /*End_c8o_AppSettings*/
        
        /**
         * Then we assign C8oSettings to our c8o Object with the init method
         */
        this.c8o.init(settings);

        
        /* ============================================================================================================
             End of Convertigo Angular Framework (CAF) initialization...
           ============================================================================================================*/
        /*=c8o_AppConstructors*/
        
		/*Begin_c8o_AppConstructor*/
		/*End_c8o_AppConstructor*/
		   
        platform.ready().then(() => {
            statusBar.styleDefault();
            /**
             * Then we finalize initialization
             */
            this.c8o.finalizeInit().then(()=>{
                this.resetImageCache();
                
                let updates = this.getInstance(SwUpdate);
                let alertCtrl = this.getInstance(AlertController);
                let fu = ()=>{
                    this.c8o.log._debug("[SW] checking for updates each 60000 ms")
                    updates.checkForUpdate()
                    .then((res)=>{
                        this.c8o.log._debug("[SW] updates checked")
                    })
                    .catch((e)=>{
                        this.c8o.log._error("[SW] updates error")
                        console.log(JSON.stringify(e));
                    });
                    
                }
                setInterval(fu, 60000)
                
                updates.available.subscribe(event => {
                    this.c8o.log._debug("[SW] update available");
                    this.c8o.log._debug('new version is '+ event.current);
                    const prompt = alertCtrl.create({
                        title: 'Convertigo Update Service',
                        message: "A new version is available for for your app.",
                        buttons: [
                          {
                            text: 'Restart app',
                            handler: data => {
                                this.c8o.log._debug("update available we will reload app");
                                updates.activateUpdate().then(() => document.location.reload());
                            }
                          }
                        ]
                      });
                      prompt.present();
                  });
                
                /*Begin_c8o_AppInitialization*/
                /*End_c8o_AppInitialization*/
            });

        });

    }
    
    instance() {
        return this;
    }
    
	getRootNav() {
		let rootNavs = this.app.getRootNavs();
		return rootNavs.length > 0 ? rootNavs[0]:null;
	}
	
    openPage(page) {
		let rootNav = this.getRootNav();
		if (rootNav) {
			rootNav.setRoot(page.name);
		}
    }
	
    openPageWithName(name) {
		let rootNav = this.getRootNav();
		if (rootNav) {
			rootNav.setRoot(name);
		}
    }
    
    getPagesIncludedInAutoMenu(){
        let arrayIncluded: Array<any> = [];
        for (let p of this.pages){
            if (p["includedInAutoMenu"]) {
                arrayIncluded.push(p);
            }
        }
        return arrayIncluded;
    }
    
	/*Begin_c8o_AppFunction*/
	/*End_c8o_AppFunction*/
    
    /*=c8o_AppFunctions*/
}