import { ApplicationRef, Injector, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, RouterStateSnapshot, CanActivate, CanDeactivate, UrlTree } from '@angular/router';
import { Observable} from 'rxjs';

import { C8oPage } from 'c8ocaf';

@Injectable({
    providedIn: 'root'
})
export class GuardsService implements CanActivate, CanDeactivate<C8oPage> {
  appRef : ApplicationRef;

  constructor(private injector: Injector) {
	this.appRef = this.injector.get(ApplicationRef);
  }
	
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean|UrlTree>|Promise<boolean|UrlTree>|boolean|UrlTree {
	let app = this.appRef.components[0].instance
	if (typeof app["canActivate"] !== "undefined") {
		return app["canActivate"]({pageName: route.component["nameStatic"], route: route, state: state})
	} else {
		return true
	}
  }

  canDeactivate(component: C8oPage, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | boolean {
	let app = this.appRef.components[0].instance
	if (typeof app["canDeactivate"] !== "undefined") {
		return app["canDeactivate"]({pageName: route.component["nameStatic"], component: component, route: route, state: state})
	} else {
		return true
	}
  }
}
