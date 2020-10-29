import { Component }																	from '@angular/core';
import { Router, ActivatedRoute } 														from '@angular/router';
import { DomSanitizer }                 												from '@angular/platform-browser';
import { NavParams, NavController, LoadingController, MenuController, Platform}			from '@ionic/angular';
import { AlertController, ActionSheetController, ModalController }						from '@ionic/angular';
import { PopoverController, ToastController }											from '@ionic/angular';
import { C8oPage, C8oPageBase, C8oRouter, C8oCafUtils }                      			from 'c8ocaf';
import { C8oNetworkStatus }                                 							from 'c8osdkangular';
import { ChangeDetectorRef, ChangeDetectionStrategy, InjectionToken, Injector, Type}	from "@angular/core";
import { TranslateService }                                 							from '@ngx-translate/core';
import { ActionBeans } 																	from '../../services/actionbeans.service';
import { Events } 																		from '../../services/events.service';

/*
	You can customize your page class by writing code between the :
   		Begin_c8o_XXXX and
   		End_c8o_XXXX
   	Comments.
   	Any code placed outside these these comments will be lost when the application is generated
*/
/*=c8o_PageImports*/

/*Begin_c8o_PageImport*/
/*End_c8o_PageImport*/

@Component({selector: /*=c8o_PageSelector*/, templateUrl: /*=c8o_PageTplUrl*/, styleUrls: [/*=c8o_PageStyleUrls*/], changeDetection: /*=c8o_PageChangeDetection*/})
export class /*=c8o_PageName*/  extends C8oPage {
	/*=c8o_PageDeclarations*/

	public navParams : NavParams;
	public events : Events;
	public subscriptions = {};
	public actionBeans: ActionBeans;
	public static nameStatic: string = "/*=c8o_PageName*/";
	/*Begin_c8o_PageDeclaration*/
	/*End_c8o_PageDeclaration*/

	constructor(routerProvider: C8oRouter, private route: ActivatedRoute, private angularRouter: Router, loadingCtrl: LoadingController, sanitizer: DomSanitizer, ref: ChangeDetectorRef, injector: Injector, menuCtrl: MenuController, public translate: TranslateService){
		super(routerProvider, loadingCtrl, sanitizer, ref, injector, menuCtrl);
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
		
		/*=c8o_PageConstructors*/
		
		/*Begin_c8o_PageConstructor*/
		/*End_c8o_PageConstructor*/
		
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
	
	/*Begin_c8o_PageFunction*/
	/*End_c8o_PageFunction*/
	
	/*=c8o_PageFunctions*/
}
