/**
 * 
 */

define(
	['uploader', 'widget', 'eventcenter', 'datacenter', 'msgbox' ],
	function( uploader, wd, ec, dc, msgbox){
		function Test( container ){
			this.name = 'TEST';
			this.ec = new ec('debug');
			this.ec.addHandler( this ); 
			this.messages = null;
			this.container = container;
			this.wd = new wd();
			
			var _that = this;
			

			this.init = function(){
				this.msgbox = new msgbox(
					'en', 
					this.ec, 
					$('#messagebox'), 
					[{name: 'tsp', email: 'hjhaohj@126.com' }],
					[{name: 'hanjinhao', email: 'hjhaohj@126.com'}],
					'hello world',
					'dasf.......=======-----------'
				);
				this.msgbox.init();

				this.wd.datepicker('#test1');

				// this.uploader = new uploader(
				// 	container.find("#uploader"), 	//选择文件按键
				// 	this.ec,
				// 	null,
				// 	container.find("#uploadConfirm"),  //确定上传按键
				// 	'zh'
				// ).init();

				// _that.messages = JSON.parse(_that.container.find('p[i18n]').attr('i18n'));

			}

			// this.uploadProcess = function( paras ){
			// 	console.log(paras);
			// 	if( !!paras ){
			// 		_that.wd.msgbox(
			// 			_that.messages['uploaded-title'], 
			// 			_that.messages['uploaded-suc'], 
			// 			_that.messages['uploaded-buts'], 
			// 			function(){}
			// 		);
			// 	}else{
			// 		_that.wd.msgbox(
			// 			_that.messages['uploaded-title'], 
			// 			_that.messages['uploaded-fail'], 
			// 			_that.messages['uploaded-buts'], 
			// 			function(){}
			// 		);
			// 	}
			// }

			// this.exports = {
				
			// }
		}
		return Test;
	}
	
)