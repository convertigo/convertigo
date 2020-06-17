import {Input, AfterViewInit, Component, forwardRef, ElementRef, Renderer2, OnInit, ViewChild} from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor }  from '@angular/forms';
import SignaturePad from 'signature_pad';

@Component({
  selector: 'c8o-signature',
  templateUrl: './SignatureComponent.html',
  styleUrls: ['./SignatureComponent.scss'],
  providers: [
     {
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => SignatureComponent),
        multi: true,
     }
    ]
})
export class SignatureComponent implements ControlValueAccessor, OnInit, AfterViewInit {
  @Input() public options: Object = {};
  @ViewChild('sPad', {static: true}) signaturePadElement;
  
  signaturePad: SignaturePad;
  
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
      //console.log('signature data :');
      //console.log(this.signaturePad.toData());
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
  
  ngOnInit(): void {
      
  }

  ngAfterViewInit(): void {
    this.signaturePad = new SignaturePad(this.signaturePadElement.nativeElement, this.options);
    this.signaturePad.onBegin = () => {};
    this.signaturePad.onEnd = () => {
        this.signature = this.signaturePad.toDataURL('image/png', 0.5);
    }
    
    this.resize();
    this.clear();
  }

  public resize(): void {
      const parent = this.renderer.parentNode(this.elRef.nativeElement);
      let width = parent.clientWidth;
      let height = parent.clientHeight;

      let canvasWidth = (<any>this.options)['canvasWidth'];
      let canvasHeight = (<any>this.options)['canvasHeight'];
      
      let canvas = this.signaturePadElement.nativeElement;
      canvas.width = canvasWidth == null ? canvas.width : canvasWidth ;
      canvas.height = canvasHeight == null ? canvas.height : canvasHeight ;
  }
  
  clear() {
    this.signaturePad.clear();
    this.signature = '';
  }

  undo() {
    const data = this.signaturePad.toData();
    if (data) {
      data.pop(); // remove the last dot or line
      this.signaturePad.fromData(data);
      if (this.signaturePad.isEmpty()) {
          this.signature = ''
      } else {
          this.signature = this.signaturePad.toDataURL('image/png', 0.5);
      }
    }
  }

  changeColor() {
      const r = Math.round(Math.random() * 255);
      const g = Math.round(Math.random() * 255);
      const b = Math.round(Math.random() * 255);
      const color = 'rgb(' + r + ',' + g + ',' + b + ')';
      this.signaturePad.penColor = color;
  }

  download(dataURL, filename) {
    if (navigator.userAgent.indexOf('Safari') > -1 && navigator.userAgent.indexOf('Chrome') === -1) {
        window.open(dataURL);
    } else {
        const blob = this.dataURLToBlob(dataURL);
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;

        document.body.appendChild(a);
        a.click();

        window.URL.revokeObjectURL(url);
    }
  }

  dataURLToBlob(dataURL) {
    // Code taken from https://github.com/ebidel/filer.js
      const parts = dataURL.split(';base64,');
      const contentType = parts[0].split(':')[1];
      const raw = window.atob(parts[1]);
      const rawLength = raw.length;
      const uInt8Array = new Uint8Array(rawLength);
      for (let i = 0; i < rawLength; ++i) {
          uInt8Array[i] = raw.charCodeAt(i);
      }
      return new Blob([uInt8Array], { type: contentType });
  }

  savePNG() {
    if (this.signaturePad.isEmpty()) {
        alert('Please provide a signature first.');
    } else {
        const dataURL = this.signaturePad.toDataURL();
        this.download(dataURL, 'signature.png');
    }
  }

  saveJPG() {
    if (this.signaturePad.isEmpty()) {
        alert('Please provide a signature first.');
    } else {
        const dataURL = this.signaturePad.toDataURL('image/jpeg');
        this.download(dataURL, 'signature.jpg');
    }
  }

  saveSVG() {
    if (this.signaturePad.isEmpty()) {
        alert('Please provide a signature first.');
    } else {
        const dataURL = this.signaturePad.toDataURL('image/svg+xml');
        this.download(dataURL, 'signature.svg');
    }
  }
}