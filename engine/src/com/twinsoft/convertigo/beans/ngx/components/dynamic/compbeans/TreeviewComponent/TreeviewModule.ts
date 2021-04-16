import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';

import { TreeModule } from '@circlon/angular-tree-component';
import { TreeviewComponent } from './TreeviewComponent';

@NgModule({
  declarations: [ TreeviewComponent ],
  exports: [ TreeviewComponent ],
  imports: [ CommonModule, TreeModule ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ]
})
export class TreeviewModule {}
