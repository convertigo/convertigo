import { NgModule, ErrorHandler }		                                      from '@angular/core';
import { HttpClient, HttpClientModule }                                       from '@angular/common/http';
import { BrowserModule }                                                      from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule }   								  from '@angular/forms';

import { IonicApp, IonicModule, IonicErrorHandler, DeepLinkConfig }           from 'ionic-angular';
import { StatusBar }                                                          from '@ionic-native/status-bar';
import { SuperTabsModule }                                                    from 'ionic2-super-tabs';
import { AgmCoreModule }                                                      from '@agm/core';
import { ChartsModule }                                                       from 'ng2-charts';
import { TranslateModule, TranslateLoader }                                   from '@ngx-translate/core';
import { TranslateHttpLoader }                                                from '@ngx-translate/http-loader';
/*=c8o_ModuleTsImports*/

import { C8o }                                                                from "c8osdkangular";
import { C8oRouter } 			                                              from 'c8ocaf';
import { ActionBeans }                                                        from '../services/actionbeans.service';

import { MyApp } 				                                              from './app.component';
/*=c8o_PagesImport*/


/**
 * Deep links to your pages so that the app can rout directly to the page url
 */
export const deepLinkConfig: DeepLinkConfig = {
  links: [/*=c8o_PagesLinks*/]
};


/**
 * Customize the ngx-translate loader for assets/i18n
 */
export function createTranslateLoader(http: HttpClient) {
    return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

@NgModule({
  declarations: [
    MyApp,/*=c8o_PagesDeclarations*/
  ],
  imports: [/*Begin_c8o_NgModules*/
    BrowserModule,
    HttpClientModule,
	FormsModule,
	ReactiveFormsModule,
	ChartsModule,
	TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: (createTranslateLoader),
          deps: [HttpClient]
        }
	}),
	SuperTabsModule.forRoot(),
	AgmCoreModule.forRoot({
	      apiKey: 'AIzaSyB0Nl1dX0kEsB5QZaNf6m-tnb1N-U5dpXs'
	}),
    IonicModule.forRoot(MyApp, {}, deepLinkConfig)
  /*End_c8o_NgModules*/],
  bootstrap: [IonicApp],
  entryComponents: [MyApp,/*=c8o_PagesDeclarations*/],
  providers: [/*Begin_c8o_NgProviders*/
    StatusBar,
    C8o,
    C8oRouter,
    ActionBeans,
    {provide: ErrorHandler, useClass: IonicErrorHandler}
  /*End_c8o_NgProviders*/]
})

export class AppModule {}