import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SignatureComponent } from './SignatureComponent';

@NgModule({
  declarations: [ SignatureComponent ],
  exports: [ SignatureComponent ],
  imports: [ CommonModule ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})
export class SignatureModule {}
