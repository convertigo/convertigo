//** Site Logo/ Watermark Script- (c) Dynamic Drive DHTML code library: http://www.dynamicdrive.com.
//** Available/ usage terms at http://www.dynamicdrive.com
//** v2.0 (April 19th, 09')


var ddsitelogo={
	setting: {orientation:4, visibleduration:0, fadeduration:[1000, 500]}, //orientation=1|2|3|4, duration=millisec or 0, fadedurations=millisecs
	offsets: {x:10, y:10},//offset of logo relative to window corner
	//logoHTML: '<a href="http://www.dynamicdrive.com" title="Dynamic Drive"><img src="http://i40.tinypic.com/e7xmvm.gif" style="width:50px; height:47px; border:0" /></a>', //HTML for logo, which is auto wrapped in DIV w/ ID="mysitelogo"
	logoHTML: '<div  class="floatingButton trans "><Button id="pageBeg" OnClick="window.location.href=\'#\'" >Go to the beginning of the page</Button><Button id="pageEnd" OnClick="window.location.href=\'#endP\'">Go to the end of the page</Button><Button id="first"  >First</Button><Button id="previous" >Previous</Button><Button id="next" >Next</Button><Button id="last"  >Last</Button></div>',	
		
		
	coord: {},

	keepfixed:function(){
		if (!this.cssfixedsupport){
			var $window=jQuery(window)
			var is1or3=/^[13]$/.test(this.setting.orientation)
			var is1or2=/^[12]$/.test(this.setting.orientation)
			var x=$window.scrollLeft() + (is1or3? this.offsets.x : $window.width()-this.$control.width()-this.offsets.x)
			var y=$window.scrollTop() + (is1or2? this.offsets.y : $window.height()-this.$control.height()-this.offsets.y)
			this.$control.css({left:x+'px', top:y+'px'})
		}
	},

	showlogo:function(){
		var mainobj=ddsitelogo
		this.$control.animate({opacity:1}, this.setting.fadeduration[0])
		if (this.setting.visibleduration>0){
			setTimeout(function(){
				mainobj.$control.stop().animate({opacity:0}, mainobj.setting.fadeduration[1], function(){
					jQuery(window).unbind('scroll.fixed resize.fixed')
				})
			}, this.setting.visibleduration+this.setting.fadeduration[0])
		}
	},
	
	init:function(){
		{
			
			var mainobj=ddsitelogo
			var iebrws=document.all
			mainobj.cssfixedsupport=!iebrws || iebrws && document.compatMode=="CSS1Compat" && window.XMLHttpRequest //not IE or IE7+ browsers in standards mode
			if (mainobj.cssfixedsupport){
				mainobj.coord[(/^[13]$/.test(mainobj.setting.orientation))? 'left' : 'right']=mainobj.offsets.x
				mainobj.coord[(/^[12]$/.test(mainobj.setting.orientation))? 'top' : 'bottom']=mainobj.offsets.y
			}
			mainobj.$control=$('<div id="logNavigationBar">'+mainobj.logoHTML+'</div>')
				.css({position:mainobj.cssfixedsupport? 'fixed' : 'absolute', opacity:0})
				.css(mainobj.coord)
				.appendTo('body')
			if (document.all && !window.XMLHttpRequest && mainobj.$control.text()!='') //loose check for IE6 and below, plus whether control contains any text
				mainobj.$control.css({width:mainobj.$control.width()}) //IE6- seems to require an explicit width on a DIV containing text
			mainobj.keepfixed()
			mainobj.showlogo()
			$(window).bind('scroll.fixed resize.fixed', function(){mainobj.keepfixed()})
		}
	}
}



