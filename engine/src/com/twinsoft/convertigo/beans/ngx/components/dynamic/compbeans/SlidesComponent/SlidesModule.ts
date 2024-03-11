import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SlidesComponent } from './SlidesComponent';

@NgModule({
  declarations: [ SlidesComponent ],
  exports: [ SlidesComponent ],
  imports: [ CommonModule ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})
export class SlidesModule {}
