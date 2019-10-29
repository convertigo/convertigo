import { Component }																from '@angular/core';
import { DomSanitizer }                 											from '@angular/platform-browser';
import { IonicPage,
		 NavParams,
		 LoadingController,
		 MenuController,
		 Platform,
		 Events,
		 AlertController,
		 ActionSheetController,
		 ModalController }															from 'ionic-angular';
import { C8oPage,
		 C8oPageBase,
		 C8oRouter, 
		 C8oCafUtils }                      										from 'c8ocaf';
import { C8oNetworkStatus }                                 						from 'c8osdkangular';
import { ChangeDetectorRef,
		 ChangeDetectionStrategy,
		 InjectionToken,
		 Injector,
		 Type} 																		from "@angular/core";
import { ActionBeans } 																from '../../services/actionbeans.service';


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

@IonicPage({
  priority: /*=c8o_PagePriority*/,
  segment: /*=c8o_PageSegment*/,
  defaultHistory: /*=c8o_PageHistory*/
})
@Component({
  selector: /*=c8o_PageSelector*/,
  templateUrl: /*=c8o_PageTplUrl*/,
})
export class /*=c8o_PageName*/ extends C8oPage  {
	/*=c8o_PageDeclarations*/

	public events : Events;
	public actionBeans: ActionBeans;
	public static nameStatic: string = "/*=c8o_PageName*/";
	/*Begin_c8o_PageDeclaration*/
	/*End_c8o_PageDeclaration*/

	constructor(routerProvider : C8oRouter, navParams: NavParams, loadingCtrl: LoadingController, sanitizer: DomSanitizer, ref: ChangeDetectorRef, injector: Injector, menuCtrl: MenuController){
		super(routerProvider, navParams, loadingCtrl, sanitizer, ref, injector, menuCtrl);
		this.events = this.getInstance(Events);
		this.actionBeans = this.getInstance(ActionBeans);
		
		/*=c8o_PageConstructors*/
		
		/*Begin_c8o_PageConstructor*/
		/*End_c8o_PageConstructor*/
		
    }
	
	instance() {
		return this;
	}
	
	/*Begin_c8o_PageFunction*/
	/*End_c8o_PageFunction*/
	
	/*=c8o_PageFunctions*/
}
