import { Component, Input, ViewChild, forwardRef, ElementRef, Renderer2} 	from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor } 							from '@angular/forms';
import { SignaturePad }                            							from 'angular2-signaturepad/signature-pad';

@Component({
  selector: 'c8o-signature',
  templateUrl: 'SignatureComponent.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SignatureComponent),
      multi: true,
    },
  ],
})
export class SignatureComponent implements ControlValueAccessor {
  @Input() public options: Object = {};
  @ViewChild(SignaturePad) public signaturePad: SignaturePad;

  public _signature: any = null;

  public propagateChange: Function = null;

  constructor(private elRef: ElementRef, private renderer: Renderer2) {
	  
  }
  
  get signature(): any {
    return this._signature;
  }

  set signature(value: any) {
    this._signature = value;
    //console.log('set signature to ' + this._signature);
    console.log('signature data :');
    console.log(this.signaturePad.toData());
    if (this.propagateChange) {
        this.propagateChange(this.signature);
    }
  }

  public writeValue(value: any): void {
    if (!value) {
	  try {
		  this.clear();
	  } catch (e) {}
      return;
    }
    this._signature = value;
    this.signaturePad.fromDataURL(this.signature);
  }

  public registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  public registerOnTouched(): void {
    // no-op
  }

  public ngAfterViewInit(): void {
    this.resize();
	this.signaturePad.clear();
  }

  public resize(): void {
	const parent = this.renderer.parentNode(this.elRef.nativeElement);
	if (parent != null) {
	    if (!(<any>this.options)['canvasWidth'] && parent.clientWidth) {
	        this.signaturePad.set('canvasWidth', parent.clientWidth);
	    }
	    if (!(<any>this.options)['canvasHeight'] && parent.clientHeight) {
	        this.signaturePad.set('canvasHeight', parent.clientHeight);
	    }
	}
  }
  
  public drawBegin(): void {
    
  }

  public drawComplete(): void {
    this.signature = this.signaturePad.toDataURL('image/png', 0.5);
    //console.log('signature completed: ' + this._signature);
  }

  public clear(): void {
    this.signaturePad.clear();
    this.signature = '';
  }
}