import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { TranslateModule } from '@ngx-translate/core';
/*=c8o_ModuleTsImports*/
/*=c8o_PageImport*/

@NgModule({
  declarations: [/*Begin_c8o_NgDeclarations*/
	/*=c8o_PageName*/
  /*End_c8o_NgDeclarations*/],
  imports: [/*Begin_c8o_NgModules*/
  	CommonModule,
	FormsModule,
	ReactiveFormsModule,
	IonicModule,
	TranslateModule.forChild(),
	/*=c8o_PageRoutingModuleName*/,
  /*End_c8o_NgModules*/],
  exports: [RouterModule],
  entryComponents: [/*Begin_c8o_NgComponents*/
  /*End_c8o_NgComponents*/],
  providers: [/*Begin_c8o_NgProviders*/
  /*End_c8o_NgProviders*/]
})
export class /*=c8o_PageModuleName*/ {}