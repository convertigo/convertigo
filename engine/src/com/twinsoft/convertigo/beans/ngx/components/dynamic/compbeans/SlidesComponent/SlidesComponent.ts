import {Input, Output, Component, ElementRef, AfterViewInit, OnInit, ViewChild, EventEmitter} from '@angular/core';

import { register } from 'swiper/element/bundle';
import Swiper from 'swiper';
//import { SwiperOptions } from 'swiper/types';

register();

@Component({
  selector: 'c8o-slides',
  templateUrl: './SlidesComponent.html',
  styleUrls: ['./SlidesComponent.scss']
})
export class SlidesComponent implements OnInit, AfterViewInit {
	@Input() public options: Object = {};
	@Output() private onSwiperActiveIndexChange = new EventEmitter<any>;
	@Output() private onSwiperAfterInit = new EventEmitter<any>;
	@Output() private onSwiperBeforeDestroy = new EventEmitter<any>;
	@Output() private onSwiperBeforeInit = new EventEmitter<any>;
	@Output() private onSwiperBeforeLoopFix = new EventEmitter<any>;
	@Output() private onSwiperBeforeResize = new EventEmitter<any>;
	@Output() private onSwiperBeforeSlideChangeStart = new EventEmitter<any>;
	@Output() private onSwiperBeforeTransitionStart = new EventEmitter<any>;
	@Output() private onSwiperBreakpoint = new EventEmitter<any>;
	@Output() private onSwiperChangeDirection = new EventEmitter<any>;
	@Output() private onSwiperClick = new EventEmitter<any>;
	@Output() private onSwiperDestroy = new EventEmitter<any>;
	@Output() private onSwiperDoubleClick = new EventEmitter<any>;
	@Output() private onSwiperDoubleTap = new EventEmitter<any>;
	@Output() private onSwiperFromEdge = new EventEmitter<any>;
	@Output() private onSwiperInit = new EventEmitter<any>;
	@Output() private onSwiperLock = new EventEmitter<any>;
	@Output() private onSwiperLoopFix = new EventEmitter<any>;
	@Output() private onSwiperMomentumBounce = new EventEmitter<any>;
	@Output() private onSwiperObserverUpdate = new EventEmitter<any>;
	@Output() private onSwiperOrientationchange = new EventEmitter<any>;
	@Output() private onSwiperProgress = new EventEmitter<any>;
	@Output() private onSwiperReachBeginning = new EventEmitter<any>;
	@Output() private onSwiperReachEnd = new EventEmitter<any>;
	@Output() private onSwiperRealIndexChange = new EventEmitter<any>;
	@Output() private onSwiperResize = new EventEmitter<any>;
	@Output() private onSwiperSetTransition = new EventEmitter<any>;
	@Output() private onSwiperSetTranslate = new EventEmitter<any>;
	@Output() private onSwiperSlideChange = new EventEmitter<any>;
	@Output() private onSwiperSlideChangeTransitionEnd = new EventEmitter<any>;
	@Output() private onSwiperSlideChangeTransitionStart = new EventEmitter<any>;
	@Output() private onSwiperSlideNextTransitionEnd = new EventEmitter<any>;
	@Output() private onSwiperSlideNextTransitionStart = new EventEmitter<any>;
	@Output() private onSwiperSlidePrevTransitionEnd = new EventEmitter<any>;
	@Output() private onSwiperSlidePrevTransitionStart = new EventEmitter<any>;
	@Output() private onSwiperSlideResetTransitionEnd = new EventEmitter<any>;
	@Output() private onSwiperSlideResetTransitionStart = new EventEmitter<any>;
	@Output() private onSwiperSliderFirstMove = new EventEmitter<any>;
	@Output() private onSwiperSliderMove = new EventEmitter<any>;
	@Output() private onSwiperSlidesGridLengthChange = new EventEmitter<any>;
	@Output() private onSwiperSlidesLengthChange = new EventEmitter<any>;
	@Output() private onSwiperSlidesUpdated = new EventEmitter<any>;
	@Output() private onSwiperSnapGridLengthChange = new EventEmitter<any>;
	@Output() private onSwiperSnapIndexChange = new EventEmitter<any>;
	@Output() private onSwiperTap = new EventEmitter<any>;
	@Output() private onSwiperToEdge = new EventEmitter<any>;
	@Output() private onSwiperTouchEnd = new EventEmitter<any>;
	@Output() private onSwiperTouchMove = new EventEmitter<any>;
	@Output() private onSwiperTouchMoveOpposite = new EventEmitter<any>;
	@Output() private onSwiperTouchStart = new EventEmitter<any>;
	@Output() private onSwiperTransitionEnd = new EventEmitter<any>;
	@Output() private onSwiperTransitionStart = new EventEmitter<any>;
	@Output() private onSwiperUnlock = new EventEmitter<any>;
	@Output() private onSwiperUpdate = new EventEmitter<any>;
	
  	@ViewChild('swiperRef') swiperRef: ElementRef | undefined;
  	
  	swiper: Swiper;
	el;
	
  	ngOnInit(): void {
    	
  	}

  	ngAfterViewInit(): void {
		// configure swiper
    	Object.assign(this.swiperRef?.nativeElement, this.options);
    	
    	// initialize swiper
		setTimeout(() => {
			this.swiperRef?.nativeElement.initialize();
			
			this.el = this.swiperRef?.nativeElement;
    		this.swiper = this.swiperRef?.nativeElement.swiper;
    		
			let that = this;
			that.swiper.on('activeIndexChange', function (event) {that.onSwiperActiveIndexChange.emit(event);});
			that.swiper.on('afterInit', function (event) {that.onSwiperAfterInit.emit(event);});
			that.swiper.on('beforeDestroy', function (event) {that.onSwiperBeforeDestroy.emit(event);});
			that.swiper.on('beforeInit', function (event) {that.onSwiperBeforeInit.emit(event);});
			that.swiper.on('beforeLoopFix', function (event) {that.onSwiperBeforeLoopFix.emit(event);});
			that.swiper.on('beforeResize', function (event) {that.onSwiperBeforeResize.emit(event);});
			that.swiper.on('beforeSlideChangeStart', function (event) {that.onSwiperBeforeSlideChangeStart.emit(event);});
			that.swiper.on('beforeTransitionStart', function (event) {that.onSwiperBeforeTransitionStart.emit(event);});
			that.swiper.on('breakpoint', function (event) {that.onSwiperBreakpoint.emit(event);});
			that.swiper.on('changeDirection', function (event) {that.onSwiperChangeDirection.emit(event);});
			that.swiper.on('click', function (event) {that.onSwiperClick.emit(event);});
			that.swiper.on('destroy', function (event) {that.onSwiperDestroy.emit(event);});
			that.swiper.on('doubleClick', function (event) {that.onSwiperDoubleClick.emit(event);});
			that.swiper.on('doubleTap', function (event) {that.onSwiperDoubleTap.emit(event);});
			that.swiper.on('fromEdge', function (event) {that.onSwiperFromEdge.emit(event);});
			that.swiper.on('init', function (event) {that.onSwiperInit.emit(event);});
			that.swiper.on('lock', function (event) {that.onSwiperLock.emit(event);});
			that.swiper.on('loopFix', function (event) {that.onSwiperLoopFix.emit(event);});
			that.swiper.on('momentumBounce', function (event) {that.onSwiperMomentumBounce.emit(event);});
			that.swiper.on('observerUpdate', function (event) {that.onSwiperObserverUpdate.emit(event);});
			that.swiper.on('orientationchange', function (event) {that.onSwiperOrientationchange.emit(event);});
			that.swiper.on('progress', function (event) {that.onSwiperProgress.emit(event);});
			that.swiper.on('reachBeginning', function (event) {that.onSwiperReachBeginning.emit(event);});
			that.swiper.on('reachEnd', function (event) {that.onSwiperReachEnd.emit(event);});
			that.swiper.on('realIndexChange', function (event) {that.onSwiperRealIndexChange.emit(event);});
			that.swiper.on('resize', function (event) {that.onSwiperResize.emit(event);});
			that.swiper.on('setTransition', function (event) {that.onSwiperSetTransition.emit(event);});
			that.swiper.on('setTranslate', function (event) {that.onSwiperSetTranslate.emit(event);});
			that.swiper.on('slideChange', function (event) {that.onSwiperSlideChange.emit(event);});
			that.swiper.on('slideChangeTransitionEnd', function (event) {that.onSwiperSlideChangeTransitionEnd.emit(event);});
			that.swiper.on('slideChangeTransitionStart', function (event) {that.onSwiperSlideChangeTransitionStart.emit(event);});
			that.swiper.on('slideNextTransitionEnd', function (event) {that.onSwiperSlideNextTransitionEnd.emit(event);});
			that.swiper.on('slideNextTransitionStart', function (event) {that.onSwiperSlideNextTransitionStart.emit(event);});
			that.swiper.on('slidePrevTransitionEnd', function (event) {that.onSwiperSlidePrevTransitionEnd.emit(event);});
			that.swiper.on('slidePrevTransitionStart', function (event) {that.onSwiperSlidePrevTransitionStart.emit(event);});
			that.swiper.on('slideResetTransitionEnd', function (event) {that.onSwiperSlideResetTransitionEnd.emit(event);});
			that.swiper.on('slideResetTransitionStart', function (event) {that.onSwiperSlideResetTransitionStart.emit(event);});
			that.swiper.on('sliderFirstMove', function (event) {that.onSwiperSliderFirstMove.emit(event);});
			that.swiper.on('sliderMove', function (event) {that.onSwiperSliderMove.emit(event);});
			that.swiper.on('slidesGridLengthChange', function (event) {that.onSwiperSlidesGridLengthChange.emit(event);});
			that.swiper.on('slidesLengthChange', function (event) {that.onSwiperSlidesLengthChange.emit(event);});
			that.swiper.on('slidesUpdated', function (event) {that.onSwiperSlidesUpdated.emit(event);});
			that.swiper.on('snapGridLengthChange', function (event) {that.onSwiperSnapGridLengthChange.emit(event);});
			that.swiper.on('snapIndexChange', function (event) {that.onSwiperSnapIndexChange.emit(event);});
			that.swiper.on('tap', function (event) {that.onSwiperTap.emit(event);});
			that.swiper.on('toEdge', function (event) {that.onSwiperToEdge.emit(event);});
			that.swiper.on('touchEnd', function (event) {that.onSwiperTouchEnd.emit(event);});
			that.swiper.on('touchMove', function (event) {that.onSwiperTouchMove.emit(event);});
			that.swiper.on('touchMoveOpposite', function (event) {that.onSwiperTouchMoveOpposite.emit(event);});
			that.swiper.on('touchStart', function (event) {that.onSwiperTouchStart.emit(event);});
			that.swiper.on('transitionEnd', function (event) {that.onSwiperTransitionEnd.emit(event);});
			that.swiper.on('transitionStart', function (event) {that.onSwiperTransitionStart.emit(event);});
			that.swiper.on('unlock', function (event) {that.onSwiperUnlock.emit(event);});
			that.swiper.on('update', function (event) {that.onSwiperUpdate.emit(event);});
		});
  	}
}