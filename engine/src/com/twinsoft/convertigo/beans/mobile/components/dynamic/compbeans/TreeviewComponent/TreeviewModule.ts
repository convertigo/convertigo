import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';

import { TreeviewComponent } from './TreeviewComponent';

@NgModule({
  //declarations: [ TreeviewComponent ],
  //exports: [ TreeviewComponent ],
  imports: [ CommonModule ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})
export class TreeviewModule {}
