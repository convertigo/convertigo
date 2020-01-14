import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SignaturePadModule } from 'angular2-signaturepad';
import { SignatureComponent } from './SignatureComponent';

@NgModule({
  declarations: [ SignatureComponent ],
  exports: [ SignatureComponent ],
  imports: [ CommonModule, SignaturePadModule ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})
export class SignatureModule {}
