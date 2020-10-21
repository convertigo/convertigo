import { NgModule, ErrorHandler }		                                      from '@angular/core';
import { HttpClient, HttpClientModule }                                       from '@angular/common/http';
import { BrowserModule }                                                      from '@angular/platform-browser';
import { BrowserAnimationsModule }                                            from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule }   								  from '@angular/forms';
import { ServiceWorkerModule }                                                from '@angular/service-worker';

import { IonicApp, IonicModule, IonicErrorHandler, DeepLinkConfig }           from 'ionic-angular';
import { StatusBar }                                                          from '@ionic-native/status-bar';
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
  declarations: [/*Begin_c8o_NgDeclarations*/
    MyApp,
    /*=c8o_PagesDeclarations*/
  /*End_c8o_NgDeclarations*/],
  imports: [/*Begin_c8o_NgModules*/
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
	FormsModule,
	ReactiveFormsModule,
	TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: (createTranslateLoader),
          deps: [HttpClient]
        }
	}),
    ServiceWorkerModule.register('ngsw-worker.js', { enabled: /*=c8o_RegisterWorker*/ }),
    IonicModule.forRoot(MyApp, {preloadModules: true})
  /*End_c8o_NgModules*/],
  bootstrap: [IonicApp],
  entryComponents: [/*Begin_c8o_NgComponents*/
    MyApp,
    /*=c8o_PagesDeclarations*/
  /*End_c8o_NgComponents*/],
  providers: [/*Begin_c8o_NgProviders*/
    StatusBar,
    C8o,
    C8oRouter,
    ActionBeans,
    {provide: ErrorHandler, useClass: IonicErrorHandler}
  /*End_c8o_NgProviders*/]
})

export class AppModule {}