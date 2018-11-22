import { Component, ViewChild}                              from '@angular/core';

import { Platform, Nav, App, LoadingController}             from 'ionic-angular';
import { StatusBar }                                        from '@ionic-native/status-bar';
import { TranslateService }                                 from '@ngx-translate/core';

// Convertigo CAF Imports
import { C8oRouter }                                        from 'c8ocaf';
import { C8oRoute, C8oRouteOptions, C8oRouteListener}       from 'c8ocaf'
import { C8oPage}                                           from "c8ocaf";
import { C8o, C8oSettings, C8oLogLevel,C8oProgress }        from "c8osdkangular";

/*
	You can customize your application class by writing code between the :

   		Begin_c8o_XXXX and
   		End_c8o_XXXX
   		
   	Comments.
   	
   	Any code placed outside these these comments will be lost when the application is generated
*/

/*Begin_c8o_AppImport*/
/*End_c8o_AppImport*/

/*=c8o_PagesImport*/ 


/**
 * Disable comments to run in prod mode
 */
/*import {enableProdMode} from '@angular/core';
 enableProdMode();*/


@Component({
    templateUrl: 'app.html'
})
export class MyApp {
    @ViewChild(Nav) nav: Nav;
    rootPage = /*=c8o_RootPage*/;
    pages : Array<{title: string, icon: string, component: any, includedInAutoMenu?: boolean}>;
    pagesKeyValue: any;
    private imgCache : Object = new Object();
	
	/*Begin_c8o_AppDeclaration*/
	/*End_c8o_AppDeclaration*/

    constructor(platform: Platform, statusBar: StatusBar, private c8o: C8o, private router: C8oRouter, private loader: LoadingController, private app: App, private translate: TranslateService) {
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
            .setLogLevelLocal(C8oLogLevel.DEBUG);

        /**
         * Then we assign C8oSettings to our c8o Object with the init method
         */
        this.c8o.init(settings);

        
        /* ============================================================================================================
             End of Convertigo Angular Framework (CAF) initialization...
           ============================================================================================================*/
		   
		/*Begin_c8o_AppConstructor*/
		/*End_c8o_AppConstructor*/
		   
        platform.ready().then(() => {
            statusBar.styleDefault();
            /**
             * Then we finalize initialization
             */
            this.c8o.finalizeInit().then(()=>{
                this.resetImageCache();
                /*Begin_c8o_AppInitialization*/
                /*End_c8o_AppInitialization*/
            });

        });

    }

	getRootNav() {
		let rootNavs = this.app.getRootNavs();
		return rootNavs.length > 0 ? rootNavs[0]:null;
	}
	
    openPage(page) {
		let rootNav = this.getRootNav();
		if (rootNav) {
			rootNav.setRoot(page.component);
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
    
    /**
     * Get attachment data url a requestable response to be displayed
     *
     * @param id             the DocumentID to get the attachment from
     * @param attachmentName  name of the attachment to display (eg: image.jpg)
     * @param placeholderURL  the url to display while we get the attachment (This is an Async process)
     * @param databaseName    the Qname of a FS database (ex project.fsdatabase) to get the attachment from.
     *
     */
    public getAttachmentUrl(id: string, attachmentName: string, placeholderURL : string, databaseName?: string): Object{
        return this.router.getAttachmentUrl(id, attachmentName, placeholderURL, this.imgCache, databaseName);
    }
    
    /**
     * Reset Image Cache.
     *
     * @param cacheEntry : the name of the Entry to clear. If not provided, clears all the entries
     *
     */
    public resetImageCache(cacheEntry: string= null ) {
        if (cacheEntry) {
            delete this.imgCache[cacheEntry]
            return;
        }
        this.imgCache = []
    }
    
    /**
     * Gets the data from previous called requestable list. can be used in an Angular 2 directive such as
     *
     *   *ngFor="let category of listen(['fs://.view']).rows" or
     *   *ngFor="let Page2 of listen(['fs://.view', 'fs://.view#search']).rows"
     *
     * The data for the first requestable to match is returned
     *
     * @return the data for one of the requestables in the list.
     */
    public listen(requestables : string[]) : any {
        return this.router.getResponseForView(this.constructor.name, requestables);
        //this.router.getResponseForView('_C80_GeneralView', ['fs://fs_monmobile.view');
    }

	/*Begin_c8o_AppFunction*/
	/*End_c8o_AppFunction*/
}