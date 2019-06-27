import { Component, OnInit , OnDestroy, OnChanges, SimpleChange, Input, Output, EventEmitter } from '@angular/core';
import { ViewChild, forwardRef, ElementRef, Renderer2, ViewEncapsulation } from '@angular/core';
import { C8oPageBase } from 'c8ocaf';

@Component({
    selector: 'c8o-scroll-infinite',
    templateUrl: 'AutoScrollComponent.html'
  })
export class AutoScrollComponent implements OnInit, OnDestroy, OnChanges {
    @Input() public c8oPage: C8oPageBase = null;
    @Input() public c8oItems: any = [];
    @Input() public c8oSParams: any = {};
    @Input() public c8oVParams: any = {};
    @Input() public scrollParams: any = {};
    
    public scroll_position: string = "'bottom'";
    public scroll_threshold: string = "'15%'";
    public scroll_isEnabled: boolean = true;
    
    public _requestable: string = null;
    public _threshold: number = 500;
    public _timeout: number = 500;
    public _options: any = {};
    
    constructor(private elRef: ElementRef, private renderer: Renderer2) {
        
    }
    
    ngOnInit() {
        this.initialize();
    }
    
    ngOnDestroy() {
        
    }
    
    ngOnChanges(changes: {[propKey: string]: SimpleChange}) {
        if (changes["c8oItems"]) {
            this.scroll_isEnabled = true;
        }
    }
    
    public isEnabled() : boolean {
        return this.scroll_isEnabled;
    }
    
    public doInfinite(scroll) : Promise<any> {
        return new Promise((resolve, reject)=> {
            this.scroll_isEnabled = false;
            if (this._requestable != null) {
                let limit = this._options["limit"];
                let l1 = this._options["skip"] = this.c8oItems.length;
                this.c8oPage.c8o.log['debug']("AutoScroll: requesting... (limit="+limit+", skip="+l1+")");
                
                this.c8oPage.call(this._requestable, this._options, null, this._threshold)
                .then((res:any) => {
                    //let length:number = res[Object.keys(res)[0]].length;
                    //this.c8oPage.c8o.log['debug'](JSON.stringify(res));
                    let l2 = this.c8oItems.length;
                    let delta = l2 - l1;
                    let done = delta != this._options["limit"] ? true:false;
                    this.scroll_isEnabled = !done;
                    this.c8oPage.c8o.log['debug']("AutoScroll: retrieved "+l2+" items (done="+done+")");
                })
                .catch((error:any) => {
                    this.c8oPage.c8o.log['warn'](error);
                    this.scroll_isEnabled = true;
                })
                scroll.complete();
                resolve(true);
            } else {
                this.c8oPage.c8o.log['error']("Invalid 'requestable' or 'fsview' property for Auto Scroll!");
                scroll.complete();
                resolve(false);
            }
        });
    }
    
    private initialize() {
        this.scroll_position = this.scrollParams.position;
        if (this.scrollParams.threshold != null && this.scrollParams.threshold != "") {
            this.scroll_threshold = this.scrollParams.threshold;
        }
        
        if (this._requestable == null) {
            this.c8oPage.c8o.log['debug']("AutoScroll: initializing...");
            let isSequence = this.c8oSParams && this.c8oSParams.requestable != null && this.c8oSParams.requestable != "";
            let isView = this.c8oVParams && this.c8oVParams.fsview != null && this.c8oVParams.fsview != "";
            let params = isSequence ? this.c8oSParams : (isView ? this.c8oVParams : null);
            
            if (params != null) {
                let tokens = isSequence ? params.requestable.split('.') : params.fsview.split('.');
                
                let r:string = isSequence ? params.requestable : "fs://" + tokens[1] + ".view";
                let m:string = params.marker != null ? params.marker : "";
                this._requestable = r + (m != "" ? "#":"")+ m;
                
                this._timeout = params.timeout || 500;
                this._threshold = params.threshold || 500;
                
                if (isView) {
                    this._options.ddoc = tokens[2];
                    this._options.view = tokens[3];
                }
                
                for (let key in params) {
                    if (key != "marker" && key != "requestable" && key != "fsview") {
                        if (params[key] != null) {
                            if (params[key] == "true") { 
                                this._options[key] = true;
                            } else if (params[key] == "false") { 
                                this._options[key] = false;
                            } else {
                                this._options[key] = params[key];
                            }
                        } else {
                            if (key == "limit") {
                                this._options[key] = 500;
                            }
                        }
                    }
                }
            }
            this.c8oPage.c8o.log['debug']("AutoScroll: requestable: "+ this._requestable);
            this.c8oPage.c8o.log['debug']("AutoScroll: options: "+ JSON.stringify(this._options));
        }
    }
}