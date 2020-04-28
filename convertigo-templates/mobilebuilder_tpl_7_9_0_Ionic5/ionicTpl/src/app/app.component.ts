import { Component } from '@angular/core';

import { Platform } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';

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
export class AppComponent {
	public selectedIndex = 0;
	public appPages : /*=c8o_PageArrayDef*/;
	
  	constructor(private platform: Platform, private splashScreen: SplashScreen, private statusBar: StatusBar) {
	
		this.appPages = [/*=c8o_PagesVariables*/];
		
    	this.initializeApp();
  	}

  	initializeApp() {
    	this.platform.ready().then(() => {
      		this.statusBar.styleDefault();
      		this.splashScreen.hide();
    	});
  	}
}
