/**
 * 
 */
define(
	function(){
		function EventCenter( mode ){
			this.mode = mode;
			this.eventHandlers = [];
			this.fire = function(src, srcTag,  args ){
				this.log( src +  ";"+ srcTag );
				var resultData = [];
				for(var i=0; i<this.eventHandlers.length; i++){
					if(  !this.eventHandlers[i].exports ) continue;
					//this.log("EventCenter: -->" + this.eventHandlers[i].name );
					var processFun = this.eventHandlers[i].exports[srcTag];	//判断组件是否响应这个事件
					if( !!processFun ){
						this.log("EventCenter: " + src +'['+ srcTag + "] -->" + this.eventHandlers[i].name );
						var rst = this.eventHandlers[i].exports[srcTag]( args );	//处理事件并返回结果
						resultData.push( { handler: this.eventHandlers[i].name, rst: rst } );
					}
				}
				
				return resultData;
			}
			
			this.addHandler = function( handler ){
				
				if( !!handler.name && !!handler.ec){
					for(var i=0; i<this.eventHandlers.length; i++){
						if(this.eventHandlers[i].name == handler.name ){
							return;
						}
					}
					this.eventHandlers.push( handler );
				}else
					this.log("no name or ec!");
				
			}

			this.registEventBind = function( handler ){
				if(handler.container == null) return;
				
				var doms = handler.container.find('[fire]');
				
				for(var i=0; i<doms.length; i++){
					var dom = doms.eq(i);
					if( !!dom.attr('firebind') ) continue;	//如果已经绑定就不会再绑

					var fireEventType = dom.attr('fire'); 	//只允许绑定一个事件
					var srcTag = dom.attr("srcTag");		//得到事件处理标签

					if( fireEventType.trim() != ""){
						dom.on( 
							fireEventType, null,
							{ handler: handler },
							handler.eventHandlers[fireEventType][srcTag]
						);
						dom.attr('firebind', 'true');
					}
				}
			}
			
			this.log = function( obj ){
				if(this.mode == 'debug'){
					console.log( obj );
				};
			}
		}
		
		return EventCenter;
	}
);