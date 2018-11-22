import { Component }																from '@angular/core';
import { DomSanitizer }                 											from '@angular/platform-browser';
import { NavParams, LoadingController, MenuController, Platform }					from 'ionic-angular';
import { C8oRouter }                    											from 'c8ocaf';
import { C8oPage }                      											from 'c8ocaf';
import { ActionBeans } 																from '../../services/actionbeans.service';
import {ChangeDetectorRef, ChangeDetectionStrategy, InjectionToken, Injector, Type} from "@angular/core";


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

@Component({
  selector: /*=c8o_PageSelector*/,
  templateUrl: /*=c8o_PageTplUrl*/,
})
export class /*=c8o_PageName*/ extends C8oPage  {
	/*=c8o_PageDeclarations*/

	public actionBeans: ActionBeans;
	public static nameStatic: string = "/*=c8o_PageName*/";
	/*Begin_c8o_PageDeclaration*/
	/*End_c8o_PageDeclaration*/

	constructor(routerProvider : C8oRouter, navParams: NavParams, loadingCtrl: LoadingController, sanitizer: DomSanitizer, ref: ChangeDetectorRef, injector: Injector, menuCtrl: MenuController){
		super(routerProvider, navParams, loadingCtrl, sanitizer, ref, injector, menuCtrl);
		this.actionBeans = this.getInstance(ActionBeans);
		/*=c8o_PageConstructors*/
		
		/*Begin_c8o_PageConstructor*/
		/*End_c8o_PageConstructor*/
		
    }
	
	/*Begin_c8o_PageFunction*/
	/*End_c8o_PageFunction*/
	
	/*=c8o_PageFunctions*/
}
