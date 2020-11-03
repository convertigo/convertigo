import { Component }                                                                    from '@angular/core';
import { ChangeDetectorRef, ChangeDetectionStrategy, InjectionToken, Injector, Type}    from "@angular/core";
import { DomSanitizer }                                                                 from '@angular/platform-browser';
import { Router, ActivatedRoute }                                          				from '@angular/router';
import { NavParams, NavController, LoadingController, MenuController, Platform}         from '@ionic/angular';
import { AlertController, ActionSheetController, ModalController }                      from '@ionic/angular';
import { PopoverController, ToastController }                                           from '@ionic/angular';
import { SplashScreen }                                                                 from '@ionic-native/splash-screen/ngx';
import { StatusBar }                                                                    from '@ionic-native/status-bar/ngx';
import { TranslateService }                                                             from '@ngx-translate/core';

//Convertigo CAF Imports
import { C8oRouter }                                        from 'c8ocaf';
//import { C8oRoute, C8oRouteOptions, C8oRouteListener}       from 'c8ocaf'
import { C8oPage, C8oPageBase, C8oCafUtils}                 from "c8ocaf";
import { C8o, C8oSettings, C8oLogLevel,C8oProgress }        from "c8osdkangular";
import { C8oNetworkStatus }                                 from "c8osdkangular";

import { ActionBeans }                                      from './services/actionbeans.service';
import { Events }                                           from './services/events.service';

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

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss']
})
export class AppComponent extends C8oPageBase {
	rootPage : any = /*=c8o_RootPage*/;
	public appPages : /*=c8o_PageArrayDef*/;
    pagesKeyValue: any;
	public navParams : NavParams;
	public events: Events;
	public subscriptions = {};
    public actionBeans: ActionBeans;
	public selectedPath = '';
    /*=c8o_AppDeclarations*/
    
    /*Begin_c8o_AppDeclaration*/
    /*End_c8o_AppDeclaration*/
	
//  	constructor(private platform: Platform, private splashScreen: SplashScreen, private statusBar: StatusBar) {
    constructor(private platform: Platform, private splashScreen: SplashScreen, private statusBar: StatusBar, routerProvider: C8oRouter, private route: ActivatedRoute, private angularRouter: Router, loadingCtrl: LoadingController, sanitizer: DomSanitizer, ref: ChangeDetectorRef, injector: Injector, menuCtrl: MenuController, public translate: TranslateService){
        super(injector, routerProvider, loadingCtrl, ref);
        this.events = this.getInstance(Events);
        this.actionBeans = this.getInstance(ActionBeans);
		try {
			// for PopoverController, ModalController
			this.navParams = new NavParams(this.getInstance(NavParams).data)
		} catch (e) {
			// for NavController (based on angular router)
			let params = {}
			this.merge(params, this.route.snapshot.params)
			this.merge(params, this.route.snapshot.queryParams)
			this.navParams = new NavParams(params)
		}

		this.angularRouter.events.subscribe((event: any) => {
			if (event && event.urlAfterRedirects) {
				this.selectedPath = event.urlAfterRedirects
			}
		})

		this.appPages = [/*=c8o_PagesVariables*/];
        this.pagesKeyValue = {/*=c8o_PagesVariablesKeyValue*/}
        this.routerProvider.pagesArray = this.appPages;
        this.routerProvider.pagesKeyValue = this.pagesKeyValue;
		
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
           
        this.platform.ready().then(() => {
            this.statusBar.styleDefault();
            this.splashScreen.hide();
            
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
    
    instance() {
        return this;
    }

	public merge(firstObj: Object, secondObj): Object{
	    return Object.assign(firstObj, secondObj);
	}
	
	public log(val) {
	    console.log(val);
	}
	
	public navigate(url: string, data: any) {
	    this.angularRouter.navigate([url], { queryParams: data });
	}
	
	public navigateByUrl(url: string){
	    this.angularRouter.navigateByUrl(url);
	}
	    
    /*Begin_c8o_AppFunction*/
    /*End_c8o_AppFunction*/
    
    /*=c8o_AppFunctions*/
}
